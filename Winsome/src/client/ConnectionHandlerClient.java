package client;

import utils.Request;
import utils.RequestType;
import utils.Serializator;
import utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ConnectionHandlerClient implements Runnable{

    private final MyConsole console;
    private final SocketChannel channel;
    private final WinsomeClient client;
    private final String helpMsg = """
            In Winsome puoi fare tante cose!\tDigita:

            \t'list user' \t-> per vedere tutti gli utenti di Winsome con almeno un tag in comune al tuo!

            \t'list follwers' \t-> per vedere tutti i tuoi followers

            \t'list following' \t-> per vedere tutti gli utenti che segui!

            \t'follow' <username>  \t-> per seguire l'user digitato (potrai vedere i suoi post nel tuo feed!

            \t'unfollow' <username> \t-> per smettere di seguire l'user digitato

            \t'blog' \t-> per vedere il tuo blog, quindi tutti i tuoi post!

            \t'post' <titolo> <contenuto post> \t-> per creare un nuovo post nel tuo blog!

            \t'show feed' \t-> per vedere il tuo feed!

            \t'show post' <id Post> \t-> per vedere il post richiesto!

            \t'delete' <id Post> \t-> per eliminare il post richiesto, attenzione! Puoi eliminare un post solo se sei il suo legittimo autore!

            \t'rewin' <id Post> \t-> per ricondividere con i tuoi followers il post specificato di una persona che segui o di un tuo vecchio post!

            \t'rate' <id Post> <Voto> \t-> per dare un voto ad il post specificato appartenente al tuo feed, il voto può essere o 1 (positivo) o -1 (negativo)

            \t'comment' <id Post> <contenuto commento> \t-> per commentare il post specificato appartenente al tuo feed

            \t'wallet' \t-> per controllare il tuo portafoglio in Wincoin

            \t'wallet btc' \t-> per controllare il tuo portafoglio in Bitcoin, attenzione il valore di exchange in Bitcoin varia in continuazione!
            
            \t'logout' \t-> per uscire dall' account attualmente connesso!

            Divertiti in Winsome!""";
    private final String errDigitazioneMsg = "Hai sbagliato a digitare i comandi, riprova!";


    public ConnectionHandlerClient(SocketChannel channel, WinsomeClient client, MyConsole console) {
        this.client = client;
        this.channel = channel;
        this.console = console;
    }

    //nel metodo run avviene un parsing per la generazione della richiesta, viene messa in un oggetto Request, e
    // poi tradotta in una stringa tramite serializzazione Gson e mandata al server.
    //E ci si mette in attesa della risposta del server

    //Ogni qual volta che viene stampato qualcosa sullo schermo (risposta del server o indicazioni sui comandi possibili)
    //viene acquisito il "controllo" della console per non sovrapporre le stampe tramite un oggetto Console
    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        String line ;
        boolean logOut = false;
        System.out.println("\nServe aiuto? Ti ricordo che puoi digitare 'help' in ogni momento!");
        try {
            while (!logOut) {
                //LOCKARE DAL MOMENTO DI MANDATO COMANDO DA CONSOLE FINO A RISPOSTA DAL SERVER
                System.out.println("\n\nDigita un nuovo comando!");
                line = scanner.nextLine();
                Request r ;
                String[] cmd = line.split(" ");

                //Se sono dentro a questo thread significa che sono già loggato con un account
                if (cmd[0].equalsIgnoreCase("login")) {
                    System.out.println("Sei già collegato con un account, devi prima scollegarti dall'attuale account con il comando 'logout'");
                    continue;
                } else if (cmd[0].equalsIgnoreCase("list")) {
                    if (cmd[1].equalsIgnoreCase("users")) {
                        r = new Request(RequestType.LIST_USERS);
                    } else if (cmd[1].equalsIgnoreCase("followers")) {
                        //utilizzo il metodo listFollowers in locale, senza fare richieste al client , quindi vado avanti nel loop
                        listFollower();
                        continue;
                    } else if (cmd[1].equalsIgnoreCase("following")) {
                        r = new Request(RequestType.LIST_FOLLOWING);
                    } else {
                        //sono stati sbagliati i comandi di digitazione
                        console.takeConsole();
                        System.out.println(errDigitazioneMsg);
                        console.releaseConsole();
                        continue;
                    }

                } else if (cmd[0].equalsIgnoreCase("follow")) {
                    String[] arg = new String[1];
                    arg[0] = cmd[1];
                    r = new Request(RequestType.FOLLOW, arg);
                } else if (cmd[0].equalsIgnoreCase("unfollow")) {
                    String[] arg = new String[1];
                    arg[0] = cmd[1];
                    r = new Request(RequestType.UNFOLLOW, arg);
                } else if (cmd[0].equalsIgnoreCase("blog")) {
                    r = new Request(RequestType.BLOG);
                } else if (cmd[0].equalsIgnoreCase("post")) {
                    String[] arg = new String[2];
                    arg[0] = cmd[1];
                    arg[1] = (line.substring(cmd[0].length() + cmd[1].length() + 1)).trim();
                    r = new Request(RequestType.POST, arg);
                } else if (cmd[0].equalsIgnoreCase("show")) {
                    if (cmd[1].equalsIgnoreCase("feed")) {
                        r = new Request(RequestType.SHOW_FEED);
                    } else if (cmd[1].equalsIgnoreCase("post")) {
                        //devo verificare i parametri passati
                        if (cmd.length < 3) {
                            //sono stati sbagliati i comandi di digitazione
                            console.takeConsole();
                            System.out.println(errDigitazioneMsg);
                            console.releaseConsole();
                            continue;
                        }
                        //controllo se è stato passato un intero
                        if (!Utils.isInteger(cmd[2])) {
                            System.out.println("Attenzione, per eseguire il comando" + cmd[0] + " devi digitare come parametri dei numeri! (interi)");
                            continue;
                        }
                        //show post è un comando particolare, implica altri comandi a seconda del post che viene richiesto
                        //quindi la prima richiesta che viene fatta di vedere il post viene eseguita dentro questo if
                        //la  seconda richiesta (se c è) viene solo formulata nell'oggetto Request r come tutte le altre richiesta e mandata a client dopo il parsing
                        String[] arg = new String[1];
                        arg[0] = cmd[2];
                        r = new Request(RequestType.SHOW_POST, arg);
                        String requestSerializated = Serializator.serializzazioneObject(r);
                        console.takeConsole();
                        sendMessageTCP(requestSerializated, channel);
                        String received = receiveMessageTCP(channel);
                        int bitProprieta = Integer.parseInt(received.substring(0, 1));
                        int bitAppartenenzaFeed = Integer.parseInt(received.substring(1, 2));

                        System.out.println(received.substring(2));
                        console.releaseConsole();

                        if (bitProprieta == 1) {
                            System.out.println("Questo post è stato scritto da te, vuoi eliminarlo?\nDigitare 'si' o 'no'");
                            while (true) {
                                line = scanner.nextLine();
                                if (line.equalsIgnoreCase("si")) {
                                    r = new Request(RequestType.DELETE_POST, arg);
                                    break;
                                } else if (line.equalsIgnoreCase("no")) {
                                    break;
                                }
                            }
                        } else if (bitAppartenenzaFeed == 1) {
                            while (true) {
                                System.out.println("Vuoi votare o commentare questo post?\nDigitare 'commenta' o 'vota' o 'no'");
                                line = scanner.nextLine();
                                String[] argom = new String[2];
                                argom[0] = arg[0];
                                if (line.equalsIgnoreCase("commenta")) {
                                    System.out.println("Digitare il contenuto del commento:\n(premere [invio] per finire)\n");
                                    argom[1] = scanner.nextLine();
                                    r = new Request(RequestType.COMMENT, argom);
                                    break;
                                } else if (line.equalsIgnoreCase("vota")) {
                                    System.out.println("Digitare il voto: '1' o '-1' ?\n");
                                    argom[1] = scanner.nextLine();
                                    if (!argom[1].equals("1") && !argom[1].equals("-1")) {
                                        System.out.println("Attenzione, per votare devi digitare o 1 o -1");
                                        continue;
                                    }
                                    r = new Request(RequestType.RATE, argom);

                                    break;
                                } else if (line.equalsIgnoreCase("no")) {
                                    break;
                                }
                            }
                        } else continue;
                    } else {
                        console.takeConsole();
                        System.out.println(errDigitazioneMsg);
                        console.releaseConsole();
                        continue;
                    }
                } else if (cmd[0].equalsIgnoreCase("delete")) {
                    //controllo se è stato passato un intero
                    if (!Utils.isInteger(cmd[1])) {
                        System.out.println("Attenzione, per eseguire il comando" + cmd[0] + " devi digitare come parametri dei numeri! (interi)");
                        continue;
                    }
                    String[] arg = new String[1];
                    arg[0] = cmd[1];
                    r = new Request(RequestType.DELETE_POST, arg);
                } else if (cmd[0].equalsIgnoreCase("rewin")) {
                    //controllo se è stato passato un intero
                    if (!Utils.isInteger(cmd[1])) {
                        System.out.println("Attenzione, per eseguire il comando" + cmd[0] + " devi digitare come parametri dei numeri! (interi)");
                        continue;
                    }
                    String[] arg = new String[1];
                    arg[0] = cmd[1];
                    r = new Request(RequestType.REWIN, arg);
                } else if (cmd[0].equalsIgnoreCase("rate")) {
                    //controllo se è stati passati degli interi
                    if (!Utils.isInteger(cmd[1]) || !Utils.isInteger(cmd[2])) {
                        System.out.println("Attenzione, per eseguire il comando" + cmd[0] + " devi digitare come parametri dei numeri! (interi)");
                        continue;
                    }
                    String[] arg = new String[2];
                    arg[0] = cmd[1];
                    arg[1] = cmd[2];
                    r = new Request(RequestType.RATE, arg);
                } else if (cmd[0].equalsIgnoreCase("comment")) {
                    //controllo se è stato passato un intero
                    if (!Utils.isInteger(cmd[1])) {
                        System.out.println("Attenzione, per eseguire il comando" + cmd[0] + " devi digitare come parametri dei numeri! (interi)");
                        continue;
                    }
                    String[] arg = new String[2];
                    arg[0] = cmd[1];
                    arg[1] = cmd[2];
                    r = new Request(RequestType.COMMENT, arg);
                } else if (cmd[0].equalsIgnoreCase("wallet")) {
                    if (cmd.length < 2) {
                        r = new Request(RequestType.WALLET);
                    } else if (cmd[1].equalsIgnoreCase("btc")) {
                        r = new Request(RequestType.WALLET_BTC);
                    } else {
                        console.takeConsole();
                        System.out.println(errDigitazioneMsg);
                        console.releaseConsole();
                        continue;
                    }
                } else if (cmd[0].equalsIgnoreCase("logout")) {
                    r = new Request(RequestType.LOGOUT);
                    logOut = true; //esco dal while e chiudo il channel di comunicazione
                } else if (cmd[0].equalsIgnoreCase("help")) {
                    console.takeConsole();
                    System.out.println(helpMsg);
                    console.releaseConsole();
                    continue;
                } else {
                    console.takeConsole();
                    System.out.println(errDigitazioneMsg);
                    console.releaseConsole();
                    continue;
                }

                //Serializzo la Request costruita
                String requestSerializated = Serializator.serializzazioneObject(r);

                console.takeConsole();
                //invio la stringa serializzata al server
                sendMessageTCP(requestSerializated, channel);
                //aspetto la risposta del server
                String received = receiveMessageTCP(channel);
                //la stampo
                System.out.println(received);
                console.releaseConsole();

            }
        } catch (NoSuchElementException | IllegalStateException ignored){ //Exception generate dallo scanner
        }
            try {
                channel.close();
            } catch (IOException e) {
                System.err.println("Problemi Chiusura channel");
                System.exit(1);
            }

        }

    //Metodo utile per eseguire in locale il comando List Followers
    private void listFollower() {
        if(client.getListFollowers() == null || client.getListFollowers().isEmpty()){
            System.out.println("Non ci sono followers!");
            return;
        }
        System.out.println("Lista dei followers : ");
        System.out.println("______________________");
        for(String follower : client.getListFollowers()){
            System.out.println(follower);
        }
    }

    //Metodo per inviare una stringa tramite TCP
    public static void sendMessageTCP(String msg, SocketChannel channel) {
        try{
            //creo un intBuff (buffer per int) per mandare al server la dimensione della mia richiesta
            ByteBuffer intBuff = ByteBuffer.allocate(4);
            intBuff.putInt(msg.getBytes(StandardCharsets.UTF_8).length);
            intBuff.flip();
            channel.write(intBuff);
            //Scrivo su buffer la richiesta e la invio al server
            ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }

        } catch (IOException e) {
            System.err.println("Problemi sendMessage");
            System.exit(1);
        }

    }

        //Metodo per la ricezione di una stringa tramite TCP
    public static String receiveMessageTCP(SocketChannel channel){
        StringBuilder received = new StringBuilder();

        try {
            //intBuff è la dimesione della risposta del server
            ByteBuffer intBuff = ByteBuffer.allocate(4);
            channel.read(intBuff);
            intBuff.flip();
            int lenghtReply = intBuff.getInt();
            //ora che ho la dimensione della risposta, alloco un buffer di opportuna dimensione e ricevo la risposta
            ByteBuffer buffer = ByteBuffer.allocate(lenghtReply);
            channel.read(buffer);
            buffer.flip();
            //appendo al risposta nel mio StringBuilder
            received.append(StandardCharsets.UTF_8.decode(buffer));
        } catch (IOException e) {
            System.err.println("Problemi receiveMessage");
            System.exit(1);
        }
        return received.toString().trim();
    }


}
