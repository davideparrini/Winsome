package client;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.rmi.server.UnicastRemoteObject;

public class CtrlCHandlerClient {
    private final Thread shoutDownHook;

    //per eseguire correttamente il logout al server,
    public CtrlCHandlerClient(SocketChannel channel,RMIClientWinsome client) {
        this.shoutDownHook = new Thread(() -> {
            try {
                channel.close();
                UnicastRemoteObject.unexportObject(client,true);
            } catch (IOException e) {
                System.err.println("Errore chiusura channel");
            }
            System.out.println("Client chiuso!");
        });
        Runtime.getRuntime().addShutdownHook(shoutDownHook);
    }
    public void removeCtrlCHandler(){
        Runtime.getRuntime().removeShutdownHook(shoutDownHook);
    }
}
