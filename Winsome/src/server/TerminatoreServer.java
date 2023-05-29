package server;

import utils.Configurazione;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class TerminatoreServer {

    // Costruttore della classe.
    // Prende in input  un riferimento ai thread da interrompere
    // non appena si preme la combinazione di tasti Ctrl+C.

    public TerminatoreServer(Thread connectionHandler,ConnectionHandlerServer connectionHandlerServer, Thread requestHandler, Thread rewardHandler, Thread exchangeHandler, WinsomeServer server, Registry registry, Configurazione configurazione) {
        // Riferimento al thread da interrompere.


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Chiusura server");
            // Interrompo i thread.
            try {

                connectionHandlerServer.getServerChannel().close();
                System.out.println("Chiusura Thread ConnectionHandler TCP");
                if (connectionHandler.isAlive()) connectionHandler.interrupt();
                if (rewardHandler.isAlive()) rewardHandler.interrupt();
                if (requestHandler.isAlive()) requestHandler.interrupt();
                if (exchangeHandler.isAlive()) exchangeHandler.interrupt();

                registry.unbind(configurazione.getREGHOST());
                UnicastRemoteObject.unexportObject(server,true);

            } catch (NotBoundException | IOException e) {
                e.printStackTrace();
            }

            server.saveStatusServer();

            System.out.println("Server chiuso");

        }));
    }


}
