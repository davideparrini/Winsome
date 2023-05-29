package server;

import client.RMIClientWinsome;
import server.data_structure.Post;
import server.data_structure.Wallet;
import utils.Configurazione;
import utils.Serializator;
import utils.User;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

public class WinsomeServer extends RemoteServer implements RMIServerWinsome {

    //lista Path utilizzati per lo storage
    public static final String USERS_PATH = "server/filesystem/users";
    public static final String POST_PATH = "server/filesystem/posts";
    public static final String CREDENZIALI_PATH = "server/filesystem/credenziali.json";
    public static final String WALLETS_PATH = "server/filesystem/wallets.json";
    public static final String IDPOST_PATH = "server/filesystem/idpostcorrente.txt";
    public static final String CONFIG_PATH = "server/filesystem/configurazioneServer.json";
    public static final String FOLLOWERS_MAP_PATH = "server/filesystem/followersMap.json";
    public static final String FOLLOWED_MAP_PATH = "server/filesystem/followedMap.json";
    public static final String FOLLOWERS_OFFLINE_MAP_PATH = "server/filesystem/followerOfflineMap.json";
    public static final String UNFOLLOWERS_OFFLINE_MAP_PATH = "server/filesystem/unfollowerOffline.json";

//Inizializzazione strutture dati
    private static ConcurrentHashMap<String,String> credenziali; //username-password
    private static ConcurrentHashMap<String, User> profiliRegistrati; // username-profilo dedicato
    private static ConcurrentHashMap<String, CopyOnWriteArrayList<String>> utenteFollowers; // username- i suoi followers
    private static ConcurrentHashMap<String, CopyOnWriteArrayList<String>> utenteFollowed; // username - i suoi followed
    private static ConcurrentHashMap<String, CopyOnWriteArrayList<String>> utentiOfflile_follower; //utenti offline- follower to notify
    private static ConcurrentHashMap<String, CopyOnWriteArrayList<String>> utentiOfflile_unfollower; //utenti offline- unfollower to notify
    private static ConcurrentHashMap<String,CopyOnWriteArrayList<Post>> utentePostList; //utente - lista di post
    private static ConcurrentHashMap<Integer,Post> postList; // idPost-Post
    private static ConcurrentHashMap<String, Wallet> wallets; //Map di utente-proprio wallet
    public static ConcurrentHashMap<SocketAddress,String> connessioni; //Map connessioni attive    Ip/porta-username
    private final static BlockingQueue<TaskRequest> requestsList = new LinkedBlockingQueue<>();   //Lista richieste inoltrate dai client
    private static ConcurrentHashMap<String,RMIClientWinsome> rmiClientMap; //mappa degli stub-username
    private int idPostcorrente; // numero id corrente dell'ultimo post creato, solo crescente
    private double exchangeBitcoinValue; // valuta attuale del cambio Wincoin to Bitcoin
    private final float percentualeRewardAutore; // percentuale ricompensa che aspetta all' autore del post
    private final float percentualeRewardCuratori; /*percentuale ricompensa che aspetta ai curatori del post (ovviamente tale ricompensa è considerata "totale",
                                                      va spartita per ogni curatori del post)
                                                      es. percentuale = 30 % , 100 wincoin globale ricompensa, 30 wincoin ai curatori, il numero dei curatori è 3 , quindi ogni curatore avrà 10 wincoin */


    public WinsomeServer() throws RemoteException, AlreadyBoundException, NotBoundException {

        super();

        //configuro il mio server dal file configurazioneServer.json
        Configurazione configurazione = new Configurazione(CONFIG_PATH);

        //ripristino stato del server
        initializateFileSystem();
        percentualeRewardAutore = configurazione.getPERCENTUALE_REWARD_AUTHOR();
        percentualeRewardCuratori = 1 - percentualeRewardAutore;

        //Configurazione RMI
        RMIServerWinsome stub = (RMIServerWinsome) UnicastRemoteObject.exportObject(this,0);
        LocateRegistry.createRegistry(configurazione.getREGPORT());
        Registry registerRegistery = LocateRegistry.getRegistry(configurazione.getREGPORT());
        registerRegistery.bind(configurazione.getREGHOST(), stub);

        //Thread gestiore per aggiornamento dell' exchanger in Bitcoin
        Thread exchangerHandler = new Thread(new ExchangerHandler(configurazione.getEXCHANGE_TIME(),this));

        //Thread gestore delle connesioni TCP
        ConnectionHandlerServer connectionHandlerServer = new ConnectionHandlerServer(requestsList,configurazione.getTCPPORT(),this);
        Thread connectionHandler = new Thread(connectionHandlerServer);

        //Thread gestore/esecutore delle richieste dei client
        Thread requestHandler = new Thread(new RequestHandler(requestsList));

        //Thread gestore del Multicast per i rewarding
        Thread rewardHandler = new Thread( new RewardSendingHandler(configurazione.getMULTICAST(), configurazione.getMCASTPORT(), configurazione.getREWARD_TIME(),this));


        //Handler per il Ctrl+C per la terminazione del server
        TerminatoreServer terminator = new TerminatoreServer(connectionHandler,connectionHandlerServer,requestHandler,rewardHandler,exchangerHandler,this, registerRegistery,configurazione);

        //avvio il thread per gestire la connessione con i client
        connectionHandler.start();

        //avvio il thread per gestire ed eseguire le richieste dei client
        requestHandler.start();

        //avvio thread per i rewarding
        rewardHandler.start();

        //avvio thread per exchange
        exchangerHandler.start();

        System.out.println("Server in esecuzione\n\nPer fermare il l'esecuzione del server usare [Ctrl+C]");

        try {
            //aspetto che finiscano la loro esecuzione
            connectionHandler.join();
            requestHandler.join();
            rewardHandler.join();
            exchangerHandler.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    //Metodi inizializzaione e mantenimento dati server

    public void initializateFileSystem(){
        //inizializzo il filesystem del server, viene ricostruito lo stato del server

        //riottengo l'id dell'ultimo post salvato nel post id.txt
        idPostcorrente = updateIdCorrente();

        //Inizializzo le strutture dati che devo essere sempre resettate a ogni apertura del server
        connessioni = new ConcurrentHashMap<>();
        rmiClientMap = new ConcurrentHashMap<>();

        //Inizializzo le strutture dati che poi devo ricostruire
        profiliRegistrati = new ConcurrentHashMap<>();
        utentePostList = new ConcurrentHashMap<>();
        postList = new ConcurrentHashMap<>();

        //check path dove ci sono le credenziali
        if(!Files.exists(Path.of(CREDENZIALI_PATH))){
            //ovviamente se non esiste il file con le credenziali, significa che è il primo avvio del server
            credenziali = new ConcurrentHashMap<>();
            utenteFollowed = new ConcurrentHashMap<>();
            utenteFollowers = new ConcurrentHashMap<>();
            wallets = new ConcurrentHashMap<>();
            return;
        }

        //Ricostruisco lo stato del server

        //ottengo sotto forma di stringa tutto il contenuto del file passato
        String cred = Utils.fileToString(CREDENZIALI_PATH);
        //deserializzo dalla stringa l'oggetto corretto
        credenziali = Serializator.deserializioneCredenziali(cred);

        //File users è la cartella contenente tutti i file json riguardanti l'utenti, uno per ogni utente
        File users =  new File(USERS_PATH);
        for(File u : Objects.requireNonNull(users.listFiles())){
            User user = Serializator.deserializioneUtente(Utils.fileToString(USERS_PATH +"/"+ u.getName()));
            //deserializzo e inizializzo le mappe relative agli utenti
            profiliRegistrati.put(user.getUsername(), user);
            utentePostList.put(user.getUsername(),new CopyOnWriteArrayList<>());
        }

        //File posts è la cartella contenente tutti i file json riguardanti i post, uno per ogni post
        File posts = new File(POST_PATH);
        for(File p : Objects.requireNonNull(posts.listFiles())){
            Post post = Serializator.deserializionePost(Utils.fileToString(POST_PATH + "/" + p.getName()));
            //deserializzo e inizializzo le mappe relative dei post
            utentePostList.get(post.getAutor()).add(post);
            postList.put(post.getId(), post);
        }

        //inizializzo la mappa per notificare l'utenti offiline di nuovi follower/unfollower

        String utenteOfflineFollowerMap = Utils.fileToString(FOLLOWERS_OFFLINE_MAP_PATH);
        utentiOfflile_follower = Serializator.deserializioneFollowMap(utenteOfflineFollowerMap);

        String utenteOfflineUnFollowerMap = Utils.fileToString(UNFOLLOWERS_OFFLINE_MAP_PATH);
        utentiOfflile_unfollower = Serializator.deserializioneFollowMap(utenteOfflineUnFollowerMap);

        //inizializzo la mappa dei followers e follwed
        String followersMapS = Utils.fileToString(FOLLOWERS_MAP_PATH);
        utenteFollowers = Serializator.deserializioneFollowMap(followersMapS);

        String followedMapS = Utils.fileToString(FOLLOWED_MAP_PATH);
        utenteFollowed = Serializator.deserializioneFollowMap(followedMapS);

        //inizializzo la mappa dei wallet
        String walletMap = Utils.fileToString(WALLETS_PATH);
        wallets = Serializator.deserializioneWallets(walletMap);

    }

    //Metodo per salvare lo stato del server prima di chiudere il server, salvando tutte le strutture dati in file json
    public void saveStatusServer(){
        updateFileIdCorrente(idPostcorrente); //salvo nel file id.txt il numero id del file corrente
        for(String user : profiliRegistrati.keySet()){
            utentiOfflile_unfollower.putIfAbsent(user,new CopyOnWriteArrayList<>());
            utentiOfflile_follower.putIfAbsent(user,new CopyOnWriteArrayList<>());
        }
        Utils.stringToFile(Serializator.serializzazioneObject(credenziali),CREDENZIALI_PATH);
        Utils.stringToFile(Serializator.serializzazioneObject(utenteFollowers),FOLLOWERS_MAP_PATH);
        Utils.stringToFile(Serializator.serializzazioneObject(utenteFollowed),FOLLOWED_MAP_PATH);
        Utils.stringToFile(Serializator.serializzazioneObject(wallets),WALLETS_PATH);
        Utils.stringToFile(Serializator.serializzazioneObject(utentiOfflile_follower),FOLLOWERS_OFFLINE_MAP_PATH);
        Utils.stringToFile(Serializator.serializzazioneObject(utentiOfflile_unfollower),UNFOLLOWERS_OFFLINE_MAP_PATH);
        for(Post post : postList.values()){
            Utils.stringToFile(Serializator.serializzazioneObject(post),POST_PATH + "/post" + post.getId() + ".json");
        }
    }


//Metodo per recuperare dal file system il numero id dell'ultimo post scritto
    private int updateIdCorrente(){
        String s = null;
        try {
            s = Files.readString(Path.of(IDPOST_PATH));
        } catch (IOException e) {
            System.err.println("Problemi readFile per update IdCorrente post");
        }
        if (s == null) {
            System.err.println("Errore update idCorrente post");
            return -1;
        }
        return Integer.parseInt(s);
    }

    //Metodo per aggiornare il file id.txt riguardo all'id corrente
    private void updateFileIdCorrente(int idCorrente){
        try {
            Files.deleteIfExists(Path.of(IDPOST_PATH));
            Files.writeString(Path.of(IDPOST_PATH), String.valueOf(idCorrente),CREATE_NEW);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //Metodi RMI / CallBacks


    @Override
    public String register(String username, String password, String tagList) throws RemoteException{
        //reply è la risposta che avrò il client
        String reply = null;

        String[] tags = tagList.split(" ");
        try{

            if(credenziali.containsKey(username)){ //if true -> utente giò registato
                //"Username già utilizzato, riprova!"
                reply = "Username già utilizzato, riprova!";

            }
            else{ //utente non registrato, possibile registrarolo
                if(password.equals("")){
                    //"Password vuota, inserisci una password!"
                    reply = "Password vuota, inserisci una password!";
                }
                else{
                    //salvo l'utente in tutte le strutture dati, utilizzando putIfAbsent perchè è threadsafe
                    User u = new User(username,password,tags);
                    credenziali.putIfAbsent(username,password);
                    profiliRegistrati.putIfAbsent(username,u);
                    utenteFollowers.putIfAbsent(username,new CopyOnWriteArrayList<>());
                    utenteFollowed.putIfAbsent(username,new CopyOnWriteArrayList<>());
                    utentiOfflile_follower.putIfAbsent(username,new CopyOnWriteArrayList<>());
                    utentiOfflile_unfollower.putIfAbsent(username,new CopyOnWriteArrayList<>());
                    utentePostList.putIfAbsent(username,new CopyOnWriteArrayList<>());
                    wallets.putIfAbsent(username,new Wallet(0));
                    //salvo l' utente appena creato in un json nella cartella users
                    String utenteJson = Serializator.serializzazioneObject(u);
                    Utils.stringToFile(utenteJson,  USERS_PATH + "/" + username + ".json");

                    reply = "Registrazione completata!\nBevenuto in Winsome";
                }
            }
        }catch(NullPointerException e) {
            e.printStackTrace();
        }
        return reply;
    }

    //Metodo per iscriversi alle callBacks, utile per far arrivare notifiche per nuovi followers/unfollowers
    @Override
    public synchronized void registerForCallBack(String username,RMIClientWinsome client) throws RemoteException {
        //Inserisco nella map delle stub-utente online
        rmiClientMap.putIfAbsent(username,client);
        //uso la stub per aggiornare la lista dei followers dell'utente da lato client
        client.updateFollows(utenteFollowers.get(username),utentiOfflile_follower.get(username));
        client.updateUnfollows(utentiOfflile_unfollower.get(username));
        //rimuovo l'utente dalle mappe degli utenti offline
       utentiOfflile_follower.remove(username);
       utentiOfflile_unfollower.remove(username);
    }

    //metodo per disiscriversi alle callBack, il server non manda più notifiche al client, bensi memorizza le notifiche che dovevano essere mandate e le invia al momento di riconnessione del utente
    @Override
    public synchronized void unregisterForCallBack(String username,RMIClientWinsome client) throws RemoteException{
        //rimuovo la stub dalla map utentionline-stub
        rmiClientMap.remove(username,client);
        //aggiungo l'utente alle mappe degli utenti offline
        utentiOfflile_follower.putIfAbsent(username, new CopyOnWriteArrayList<>());
        utentiOfflile_unfollower.putIfAbsent(username, new CopyOnWriteArrayList<>());
    }

    //Metodo per notificare l'arrivo di un nuovo follower
    public synchronized void doCallBacksFollow(String username, String follower) throws RemoteException {
        RMIClientWinsome client = rmiClientMap.get(username);
        client.notifyFollow(follower);
    }

    //Metodo per notificare l'arrivo di un nuovo unfollower
    public synchronized void doCallBacksUnfollow(String username, String follower) throws RemoteException {
        RMIClientWinsome client = rmiClientMap.get(username);
        client.notifyUnfollow(follower);
    }


    // Metodi Multicast

    //metodo per calcolare le ricompense
    public void calcoloRewarding(){
        for(String u : profiliRegistrati.keySet()){
            for(Post p : utentePostList.get(u)){
                if(!p.isRewinPost()) {
                    double globalReward = p.rewardingCalculation();

                    double authorReward = globalReward * percentualeRewardAutore;
                    double curatoriReward = 0;
                    if(p.sizeCuratori() > 0){
                       curatoriReward = globalReward * percentualeRewardCuratori / p.sizeCuratori();
                    }
                    if(authorReward > 0)
                        wallets.get(u).addWincoins(authorReward);
                    if(curatoriReward > 0){
                        for(String curatorePost : p.getCurators()){
                            wallets.get(curatorePost).addWincoins(curatoriReward);
                        }
                    }

                }
            }
        }
    }

    //Metodi TCP
    //tutti i metodi "TCP" restituiscono una stringa contenente la risposta del server alla richiesta inviata

    //login dell'utente
    public String login(String username, String password, SocketAddress address) throws RemoteException {

        String reply;

        //check appartenenza map delle credenziali
        if(credenziali.get(username) != null && credenziali.get(username).equals(password)){
            if(connessioni.contains(username)){
                reply = "L'utente '" + username + "' è già collegato a Winsome da un altro dispositivo, se vuoi loggare su questo dispositivo devi prima fare logout!";
            }
            else{
                reply = "Benvenuto in Winsome '" + username +"'";

                //aggiungo alla mappa delle connessioni attive-utente
                connessioni.putIfAbsent(address,username);
            }

        }else{
            reply = "Username o password non corrette";
        }
        return reply;
    }

    //logout dell'utente
    public void logout(SocketAddress address){
        //Rimuovo daglli utenti online l'utente richiedente

        connessioni.remove(address);
    }

    //restituisce la lista utenti con almeno un tag in comune con l'utente che ha fatto la richiesta
    public String listUsers(SocketAddress address){
        String utenteRichiesta = connessioni.get(address);
        StringBuilder reply = new StringBuilder();
        reply.append("\tUtente\t|\tTag\t\n");
        reply.append("________________________________\n");
        User userReq =  profiliRegistrati.get(utenteRichiesta);

        //cerco fra i profili registrati chi ha almeno un tag in comune con l utente
        for(User u : profiliRegistrati.values()){
            if(u.equals(userReq)) continue;
            if(commonTag(userReq,u)){
                reply.append("\t").append(u.getUsername()).append("\t|\t");
                for(String t : u.getListTags()){
                    reply.append(t).append(" ");
                }
                reply.append("\n");
            }
        }
        return reply.toString();
    }

    //metodo di verifica tag in comune, restituisce true se l'utente u1 ha almeno un tag in comune con u2, false altrimenti
    private boolean commonTag(User u1, User u2){
        for(String tag : u1.getListTags()){
            if(u2.getListTags().contains(tag)) return true;
        }
        return false;
    }

    //metodo per far seguire al utente che ha fatto richiesta un altro utente toFollow
    public String follow(String toFollow, SocketAddress address) throws RemoteException {
            String utenteRichiesta = connessioni.get(address);

            //check se l'utente segue già toFollow
            if(utenteFollowed.get(utenteRichiesta).contains(toFollow)) return "Segui di già '" + toFollow + "'";

            //aggiungo toFollow alla lista dei seguiti dall'utente nella map relativa
            utenteFollowed.get(utenteRichiesta).add(toFollow);
            //e viceversa aggiungo utente alla lista di followers di toFollow
            utenteFollowers.get(toFollow).add(utenteRichiesta);

            //mando notifica di follow  all' utente che è stato seguito
            if(connessioni.contains(toFollow)) {
                doCallBacksFollow(toFollow, utenteRichiesta);
            }
            else {
                utentiOfflile_unfollower.get(toFollow).remove(utenteRichiesta);
                utentiOfflile_follower.get(toFollow).add(utenteRichiesta);
            }
            return "Ora segui '" + toFollow +"'";
    }

    //metodo per far smettere seguire al utente che ha fatto richiesta un altro utente toUnfollow
    public String unfollow(String toUnfollow, SocketAddress address) throws RemoteException {
        String utenteRichiesta = connessioni.get(address);
        //check se l'utente non segue già toUnFollow
        if(!utenteFollowed.get(utenteRichiesta).contains(toUnfollow)) return "Non puoi smettere di seguire una persona che non segui";

        utenteFollowed.get(utenteRichiesta).remove(toUnfollow);  //rimuovo toUnFollow dalla lista dei seguiti dall'utente nella map relativa

        utenteFollowers.get(toUnfollow).remove(utenteRichiesta);  //e viceversa rimuovo utente alla lista di followers di toFollow

        //mando notifica di follow  all' utente che è stato unfollowato
        if(connessioni.contains(toUnfollow)) {
            doCallBacksUnfollow(toUnfollow, utenteRichiesta);
        }
        else{
            utentiOfflile_follower.get(toUnfollow).remove(utenteRichiesta);
            utentiOfflile_unfollower.get(toUnfollow).add(utenteRichiesta);
        }
        return "Hai smesso di seguire '" + toUnfollow +"'";
    }

    //Metodo per far visualizzare all'utente il proprio blog
    public String viewBlog(SocketAddress address){
        String utenteRichiesta = connessioni.get(address);
        //se il blog dell utente è vuoto rispondi subito al client
        if(utentePostList.get(utenteRichiesta).isEmpty()){
            return "Non hai nessun post nel tuo blog!";
        }
        StringBuilder reply = new StringBuilder();
        reply.append("Il tuo blog :\n");
        reply.append("________________\n");
        //scandisco la lista di post dell'utente e inserisco i post sotto forma di stringa nella risposta al server
        for (Post p : utentePostList.get(utenteRichiesta)){
            reply.append("Id post: ").append(p.getId()).append("\n");
            reply.append(p);
            reply.append("\n");
            reply.append("--------------------\n");
        }
        return reply.toString();
    }

    //Metodo per far visualizzare all'utente la lista delle persone che segue
    public String listFollowing(SocketAddress address){
        String utenteRichiesta = connessioni.get(address);
        //se l'utente non segue nessuno, rispondi subito al client
        if(utenteFollowed.get(utenteRichiesta).isEmpty()){
            return "Non segui nessun utente!";
        }
        StringBuilder reply = new StringBuilder();
        reply.append("Tutte gli utenti che segui:\n");
        reply.append("____________________________\n");
        //scandisco la lista dei followed dall'utente e inserisco gli username sotto forma di stringa nella risposta al server
        for(String u : utenteFollowed.get(utenteRichiesta)){
            reply.append("'").append(u).append("'\n");
        }
        return reply.toString();
    }

    //Metodo per creare un post
    public String createPost(String title, String content, SocketAddress address){
        String utenteRichiesta = connessioni.get(address);
        StringBuilder reply = new StringBuilder();
        //creo post
        Post post = new Post(idPostcorrente++,utenteRichiesta ,title,content);
        //lo aggiungo alle strutture dati
        utentePostList.get(utenteRichiesta).add(post);
        postList.putIfAbsent(post.getId(),post);
        //serializzo il post e lo salvo nella cartella dei post sotto forma di json
        Utils.stringToFile(Serializator.serializzazioneObject(post),POST_PATH + "/post" + post.getId() + ".json");
        reply.append("Nuovo post creato (id = ").append(post.getId()).append(")\n");
        return reply.toString();
    }
    //Metodo per cancellazione di un post, invoca un altro metodo delete post per effettuare la cancellazione dei post "figli" in modo ricorsivo
    public String deletePostRequest(int idPost, SocketAddress address){
        String utenteRichiesta = connessioni.get(address);
        return deletePost(idPost,utenteRichiesta);
    }

    //Metodo cancellazione post ricorsiva
    private String deletePost(int idPost, String utente){
        //ottengo il post da rimuovere
        Post toRemove = postList.get(idPost);
        //controllo se è null o se l'autore del post non è lo stesso utente che ha fatto la richiesta e do errore
        if(toRemove == null ||!toRemove.getAutor().equals(utente)) {
            System.err.println("Errore deletePost");
            return "Impossibile rimuovere post " + idPost + "\nIl post non esiste o non è stato scritto da te!";
        }

        StringBuilder reply = new StringBuilder();
        //Se il post è  un rewin di un post dello stesso utente, gli fornisco l'id del post originale in modo che se vuole può cancellare l'originale
        if(toRemove.isRewinPost() && postList.get(toRemove.getIdPostRewinato()).getAutor().equals(utente)){
            reply.append("Questo post è un rewin di un tuo vecchio post, se vuoi cancellare anche il post originale digita, delete ").append(toRemove.getIdPostRewinato()).append("\n");
        }

        if(!toRemove.isRewinPost()){ //Se il post non è un rewin,quindi è un originale
            //controllo se ha dei "figli" e richiamo ricorsivamente la funzione delete sul post figlio, in modo da cancellare pure il post figlio
            if(toRemove.containsRewin()) {
                for(int id : toRemove.getListIdPostRewin()){
                    if(!deletePost(id,postList.get(id).getAutor()).equals("Impossibile rimuovere post " + id + "\nIl post non esiste o non è stato scritto da te!")){
                        reply.append("Rimosso rewin ").append(id).append(" del post ").append(idPost).append("\n");
                    }
                }
            }
        }else{
            //Se è un rewin, elimino il rewin dalla lista dei rewin del post originale
            toRemove.getRewined().getListIdPostRewin().remove((Integer) toRemove.getId());
        }

        //rimuovo il post dalle strutture dati
        utentePostList.get(utente).remove(toRemove);
        postList.remove(idPost);
        //elimino il file del post dal file system
        Utils.deleteFile(POST_PATH + "/post" + idPost + ".json");
        reply.append("Post ").append(idPost).append(" rimosso!");
        return reply.toString();
    }

    //metodo per fare visualizzare all utente un post che ha richiesto
    public String showPost(int idPost,SocketAddress address){
        //metto una bitmask all'inizio della stringa di risposta per dire al client se il post richiesto è stato scritto da lui o se il suo post è nel suo feed
        //Queste informazioni sono utili al client per fare eseguire comandi o specificare qualcosa all'utente, in modo tale da non dover richiedere al server queste informazioni

        //primo bit = proprietario post
        //secondo bit = apparteneza al feed

        String utenteRichiesta = connessioni.get(address);
        Post post = postList.get(idPost);
        if(post == null)
            return "00Non esiste il post : " + idPost;

        Integer bitProprieta = 0, bitAppartenenzaFeed = 0 ;
        //Se il post è stato scritto dall'utente della richiesta bitProprietò = 1
        if(post.getAutor().equals(utenteRichiesta)) bitProprieta = 1;
        //Se il post è nel proprio feed, quindi se l'utente della richiesta segue l'autore del post, bitAppartenenzaFeed = 1
        if(utenteFollowed.get(utenteRichiesta).contains(post.getAutor())) bitAppartenenzaFeed = 1;

        return bitProprieta.toString() + bitAppartenenzaFeed.toString() + post.toString();
    }


    //Metodo per far visualizzare all'utente richiedente il suo feed, composto quindi dai post scritti o rewinnati di tutte le persone che segue l'utente
    public String showFeed(SocketAddress address){
        String utenteRichiesta = connessioni.get(address);
        //Se non ci sono post nel feed del utente, rispondi subito al client
        if(utenteFollowed.get(utenteRichiesta).isEmpty()){
            return  "Non ci sono post nel tuo feed!";
        }
        StringBuilder reply = new StringBuilder();
        reply.append("Id post\t|\tAutore\t|\tTItolo\n");
        reply.append("_________________________\n");
        for(String followed : utenteFollowed.get(utenteRichiesta)){ //per ogni utente della lista dei seguiti dall'utente
            for(Post p : utentePostList.get(followed)){ //fai apparire ogni post
                reply.append(p.toStringFeed()).append("\n");
                reply.append("----------------------");
            }
        }
        return reply.toString();
    }

    //Metodo per far dare una valutazione ad un post da parte dell'utente richiedente
    public String ratePost(int idPost, int voto,SocketAddress address){
        String utenteRichiesta = connessioni.get(address);
        Post p = postList.get(idPost);
        //Controllo se il post esiste
        if(p == null ) return "Post " + idPost + " non esiste!";
        //Controllo se il post appartiene al feed del richiedente
        if(!utenteFollowed.get(utenteRichiesta).contains(p.getAutor()))
            return "Non puoi dare un voto all post "+ idPost + " di " + p.getAutor() + " , non appartiene al tuo feed";
        //Se il post è un rewin, il voto andrà al post originale
        if(p.isRewinPost()){
            int idPostOriginale = p.getIdPostRewinato();
            p = postList.get(idPostOriginale);
        }
        //Se l'utente ha gia dato una valutazione, riporta errore
        if(p.getPositiveRate().contains(utenteRichiesta) || p.getNegativeRate().contains(utenteRichiesta))
            return "Hai già dato un voto al post " + idPost;
        //dai il voto
        p.ratePost(utenteRichiesta,voto);
        return "Hai votato con '" + voto + "' il post "+ idPost;
    }

    //Metodo per far commentare l'utente un post del proprio feed o al proprio blog
    public String addComment(int idPost, String content,SocketAddress address){
        String utenteRichiesta = connessioni.get(address);
        Post p = postList.get(idPost);
        //controllo se esiste il post
        if(p == null ) return "Post " + idPost + " non esiste!";
        //controllo se appartiene al suo feed o al suo blog
        if(!utenteFollowed.get(utenteRichiesta).contains(p.getAutor()) && !p.getAutor().equals(utenteRichiesta))
            return "Non puoi commentare il post " + idPost + ", non appartiene o al tuo feed o al tuo blog";

        //Se il post è un rewin il commento va sull'originale
        if(p.isRewinPost()){
            int idPostOriginale = p.getIdPostRewinato();
            p = postList.get(idPostOriginale);
        }
        //aggiungo commento
        p.addComment(utenteRichiesta,content);
        return "Hai commentato il post " + idPost;
    }

    //Metodo per fare il rewin di un post
    public String rewinPost(int idPost, SocketAddress address){
        String utenteRichiesta = connessioni.get(address);
        Post p = postList.get(idPost);
        //Controllo se il post "padre" esiste
        if(p == null ) return "Il post " + idPost + " non esiste!";
        //Controllo se è un rewin, se lo è riporto errore
        if(p.isRewinPost()) return "Impossibile rewin del post " + idPost + ", non è possibile fare rewin di un rewin!";
        //Controllo se il post appartiene al proprio blog o feed
        if(!utenteFollowed.get(utenteRichiesta).contains(p.getAutor()) && !p.getAutor().equals(utenteRichiesta))
            return "Non puoi fare il rewin del post "+ idPost +", non appartiene o al tuo feed o al tuo blog";

        //Creo il rewin con l'opportuno costruttore
        Post rewin = new Post(idPostcorrente++,utenteRichiesta,p);

        //inserisco il rewin nelle strutture dati dei post
        utentePostList.get(utenteRichiesta).add(rewin);
        postList.putIfAbsent(rewin.getId(),rewin);

        return "Hai fatto il rewin del post " + p.getId();
    }
    //Metodo che restituisce il wallet dell'utente richiedente
    public String wallet(SocketAddress address){
        String utenteRichiesta = connessioni.get(address);
        return wallets.get(utenteRichiesta).toString();
    }

    //Metodo che restituisce il wallet dell'utente in Bitcoin/Satoshi
    public String walletBTC(SocketAddress address){
        String utenteRichiesta = connessioni.get(address);
        //Ottengo il valore in bitcoin del wallet tramite exchangeBitcoinValue
        double bitcoin = exchangeBitcoinValue * wallets.get(utenteRichiesta).getWincoins() / 1000000; //divido per 1'000'000 per rendere piu verosimile il valore in bitcoin
        if(bitcoin < 0.0001){ //Se il valore in Bitcoin del wallet è piccolo, ritorno anche il valore in Satoshi
            return "Il valore del tuo wallet è di: " + (int)Math.floor(bitcoin * 100000000)   +" Satoshi / "+Utils.arrotondaDoubleToString(bitcoin)+ " Bitcoin\n" +
                    "Exchanger value: " + exchangeBitcoinValue ;
        }
        //Senno riporto solo il valore in Bitcoin
        return "Il valore del tuo wallet in Bitcoin è di: " + Utils.arrotondaDoubleToString(bitcoin) + " Bitcoin\nExchanger value: " + exchangeBitcoinValue ;
    }

    //Metodo utilizzato dal thread che calcola l'exchanger
    //setta il valore del exchange quando viene aggiornato
    public void setExchangeBitcoinValue(double value){
        this.exchangeBitcoinValue = value;
    }

}
