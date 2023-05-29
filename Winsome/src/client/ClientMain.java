package client;


import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;

public class ClientMain {

    public static void main(String[] args) {
        try {
            WinsomeClient client = new WinsomeClient();
        } catch (RemoteException | NotBoundException | NoSuchAlgorithmException e) {
            System.err.println("Problemi di remote");
            System.exit(1);
        }
    }
}
