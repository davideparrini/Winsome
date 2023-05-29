package client;

import server.RMIServerWinsome;
import utils.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class WinsomeClient implements RMIClientWinsome {


    private final String HELP_MESSAGE = """
            Per accedere ad un account digita ->  'login nickname password'
            Per registrare un nuovo account digita -> 'register nickname password

            Se invece vuoi terminare l'esecuzione dell'applicazione digita 'exit'""";

    public final String CONFIG_PATH = "client/configurazioneClient.json";
    private static Configurazione configurazione;
    private CopyOnWriteArrayList<String> listFollowers;
    private final MyConsole console;

    public WinsomeClient() throws RemoteException, NotBoundException, NoSuchAlgorithmException {
        super();
        //inizializzo Oggetto Configuazione
        configurazione = new Configurazione(CONFIG_PATH);

        //inizializzo Console, oggetto per sincronizzare la scrittura/lettura della console fra i thread
        console = new MyConsole();

        //RMI
        Registry registry = LocateRegistry.getRegistry(configurazione.getREGPORT());
        RMIServerWinsome server = (RMIServerWinsome) registry.lookup(configurazione.getREGHOST());
        RMIClientWinsome stub =  (RMIClientWinsome) UnicastRemoteObject.exportObject(this,0);

        //Inizializzo il channel sul quale il client si collegherà al server
        SocketChannel channel;

        //Inizializzo scanner per acquisire i comandi da console

        Scanner scanner = new Scanner(System.in);
        String line;

        //Avvio esecuzione del Client
        System.out.println("Winsome! Accedi ad un account o registrati!");
        System.out.println("\n.\n.\n.\nSe hai bisogno di aiuto digita 'help'!");
        try {

            while (!(line = scanner.nextLine()).equalsIgnoreCase("exit")) {
                //splitto i comandi e i suoi argomenti in un array di String
                String[] cmd = line.split(" ");

                //digitato 'help'
                if (cmd[0].equalsIgnoreCase("help")) {
                    System.out.println(HELP_MESSAGE);
                    continue;
                }
                //digitato 'register'
                if (cmd[0].equalsIgnoreCase("register")) {
                    //controllo se è stato passato il numero giusto di parametri
                    if (cmd.length < 4) {
                        System.out.println("Non hai messo abbastanza parametri per registrarti a Winsome!" +
                                " -> register [nickname] [password] [lista tag, minimo 1 massimo 5]");
                        continue;
                    }
                    //Ottengo la lista di tags per la registrazione, massimo 5, il resto vengo ignorati
                    String tags = getTagsFromArgs(cmd);
                    //uso rmi per registrarmi
                    String responseRegister = server.register(cmd[1], Hash.bytesToHex(Hash.sha256(cmd[2])), tags);
                    System.out.println(responseRegister);
                    //se la registrazione non è andata a buon fine vado avanti
                    if (!responseRegister.equals("Registrazione completata!\nBevenuto in Winsome")) {
                        continue;
                    }
                    //senno chiedo se vuole loggare l'account appena creato
                    System.out.println("Vuoi loggare l'account appena creato?\nDigitare 'si' o 'no'");
                    //entro in loop attendendo la risposta, deve essere si o no
                    while (true) {
                        line = scanner.nextLine();
                        if (line.equalsIgnoreCase("si")) {
                            channel = loggin(cmd[1], cmd[2], new InetSocketAddress("localhost",configurazione.getTCPPORT()));
                            //da loggin ottendo il channel di collegamento al server
                            if (channel != null) {
                                //se il channel è != null quindi il collegamento è andato a buon fine eseguo la funzione per la gestione dell' utente loggato
                                handleLogin(channel, server, stub, cmd[1]);
                            } else {
                                System.err.println("Errore acquisizione channel");
                                continue;
                            }
                            break;
                        } else if (line.equalsIgnoreCase("no")) {
                            System.out.println("Utente " + cmd[1] + " registrato!");
                            break;
                        }
                    }
                } else if (cmd[0].equalsIgnoreCase("login")) { //è stato digitato login
                    if (cmd.length < 3) { //check sul numero di argomenti passati
                        System.out.println("""
                                "Non hai messo abbastanza parametri per accedere a Winsome!"\s
                                                            " -> login [nickname] [password] "
                                """);
                        continue;
                    }
                    channel = loggin(cmd[1], cmd[2], new InetSocketAddress(configurazione.getTCPPORT()));
                    if (channel != null) {
                        handleLogin(channel, server, stub, cmd[1]);
                    } else {
                        System.err.println("Errore acquisizione channel");
                    }

                } else {
                    System.out.println("Errore digitazione comandi, MANNAGGIA RIPROVA , o prova ad utilizzare 'help' se cerchi aiuto");
                }

            }
            UnicastRemoteObject.unexportObject(this,true);

        }catch (NoSuchElementException | IllegalStateException e){ //Exception generate dallo scanner
            System.out.println("Arrivederci!");
            System.exit(0);
        }
    }




    //Metodo per acquisire gli argomenti da un array di stringhe, e li memorizzo tutti in LowerCase su uuna stringa
    private String getTagsFromArgs(String[] args) {

        if (args.length < 3) {
            System.err.println("Non ci sono tags!" +
                    "\nRiprova");
            return null;
        } else if (args.length > 8) {
            System.out.println("Hai specificato piu di 5 tags, gli altri verranno ignorati");
        }

        int n_tags = args.length - 3;
        int max_iter = 5;
        int i = 3;
        StringBuilder tagBuilder = new StringBuilder();
        while (n_tags > 0 && max_iter > 0) {
            tagBuilder.append(args[i].toLowerCase()).append(" ");
            i++;
            n_tags--;
            max_iter--;
        }

        return tagBuilder.toString();
    }


    //Metodi rmi per gestire le notifiche che arrivano dal server

    //notifica un nuovo follower
    @Override
    public void notifyFollow(String username) throws RemoteException {
        console.takeConsole();
        System.out.println("L' utente " + username + " ha iniziato a seguirti");
        console.releaseConsole();
        listFollowers.add(username);
    }

    //notifica un nuovo Unfollower
    @Override
    public void notifyUnfollow(String username) throws RemoteException {
        console.takeConsole();
        System.out.println("L'utente " + username + " ha smesso di seguirti");
        console.releaseConsole();
        listFollowers.remove(username);
    }

    //aggiorna la lista dei followers e notifica l'utente della presenza di nuovi followers durante il suo periodo di assenza
    @Override
    public void updateFollows(CopyOnWriteArrayList<String> followers,CopyOnWriteArrayList<String> newFollowers) throws RemoteException {
        listFollowers = followers; //do la lista di followers dell'utente, in modo di averla in locale
        if(newFollowers == null) return;
        if(newFollowers.isEmpty()) return;
        console.takeConsole();
        System.out.println("Mentre eri offline ti hanno iniziato a seguire :");
        for(String f : newFollowers){
            System.out.println(f);
            listFollowers.add(f);
        }
        System.out.println();
        console.releaseConsole();
    }

    //notifica all'utente gl'utenti che l'hanno unfollowato durante il suo periodo di assenza
    @Override
    public void updateUnfollows(CopyOnWriteArrayList<String> newUnfollowers) throws RemoteException {
        if(newUnfollowers == null) return;
        if(newUnfollowers.isEmpty()) return;
        console.takeConsole();
        System.out.println("Mentre eri offline ti hanno smesso di seguirti :");
        for(String f : newUnfollowers){
            System.out.println(f);
        }
        System.out.println();
        console.releaseConsole();
    }




    //Metodo per loggarsi sul server e stabilire una connessione con il server
    private SocketChannel loggin(String nickname, String password,SocketAddress address) {
        SocketChannel channel = null;

        try{
            //creo una connessione con il server
            channel = SocketChannel.open(address);
            //alloco username e password hashata in un array di string da passare al server
            String[] arg = new String[2];
            arg[0] = nickname;
            arg[1] = Hash.bytesToHex(Hash.sha256(password));
            //Elaboro l'oggetto request da serializzare in una stringa e passare al server
            Request request = new Request(RequestType.LOGIN,arg);
            //Serializzo
            String toSend = Serializator.serializzazioneObject(request);
            //invio al server la richiesta
            ConnectionHandlerClient.sendMessageTCP(toSend,channel);
            //aspetto la risposta
            String reply = ConnectionHandlerClient.receiveMessageTCP(channel);
            System.out.println(reply);
            //se entro nel if significa che la connessione non è andata a buon fine, chiudo il channel e restituisco null
            if(!reply.equals("Benvenuto in Winsome '" + nickname +"'")){
                channel.close();
                channel = null;
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Il server ha problemi!");
            try {
                if(channel == null) System.exit(1);
                channel.close();
                channel = null;
            } catch (IOException exception) {
                System.exit(1);
            }
        }
        return channel;
    }


    //metodo per la gestione della connessione
    private void handleLogin(SocketChannel channel,RMIServerWinsome server,RMIClientWinsome stub,String cmd) throws RemoteException {
        try {
            //Inizializzo il thread per l'elaborazione delle richieste TCP
            Thread connectionHandler = new Thread(new ConnectionHandlerClient(channel,this,console));
            //Inizializzo il thread del Multicast UDP aspettando notifiche dal client
            RewardMulticastReceivingHandler rewardMulticastReceivingHandler = new RewardMulticastReceivingHandler(configurazione.getMULTICAST(), configurazione.getMCASTPORT(),console);
            Thread rewardMulticast = new Thread(rewardMulticastReceivingHandler);
            //Avvio un handler di ctrlC per far disconnettere correttamente il client dal server, anche se si chiude il programma in modo "brusco"
            CtrlCHandlerClient ctrlCHandler = new CtrlCHandlerClient(channel,stub);

            //registro il client al servizio di notifica delle CallBacks
            server.registerForCallBack(cmd,stub);

            //avvio i thread
            connectionHandler.start();
            rewardMulticast.start();

            //aspetto che finiscano la loro esecuzione
            connectionHandler.join();
            server.unregisterForCallBack(cmd,stub);
            if(rewardMulticast.isAlive()){
                //Devo uscire dal gruppo da qua
                rewardMulticastReceivingHandler.getSocket().leaveGroup(InetAddress.getByName(configurazione.getMULTICAST()));
                rewardMulticast.interrupt();
            }
            //rimuovo l'handler del CtrlC
            ctrlCHandler.removeCtrlCHandler();


            console.takeConsole();
            System.out.println("Arrivederci " + cmd + "!");
            console.releaseConsole();

        } catch (InterruptedException e) {
            System.out.println("Logut in corso!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        console.takeConsole();
        System.out.println("\nVuoi continuare a navigare? ['si' o 'no']");
        console.releaseConsole();
        String line;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            line = scanner.nextLine();
            if (line.equalsIgnoreCase("si")) {
                System.out.println("\nDigita pure un nuovo comando:");
                break;
            }else if (line.equalsIgnoreCase("no")){
                System.out.println("A presto!");
                System.exit(0);
            }
        }
    }

    public List<String> getListFollowers() {
        return listFollowers;
    }
}
