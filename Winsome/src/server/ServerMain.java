package server;


import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class ServerMain {

    public static void main(String[] args) {
        try {
            WinsomeServer server = new WinsomeServer();

        } catch (RemoteException | AlreadyBoundException | NotBoundException e) {
            e.printStackTrace();
        }

    }

}
