package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class RewardMulticastReceivingHandler implements Runnable{

    private InetAddress group; //indirizzo del gruppo del multicast
    private final int port; //porta del Multicast
    private final int BUFSIZE = 1024;
    private final MyConsole console; // oggetto MyConsole per sincronizzazione delle stampe
    private MulticastSocket socket;

    //costruttore
    public RewardMulticastReceivingHandler(String addr, int port, MyConsole console) {
        try {
            this.group = InetAddress.getByName(addr);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.port = port;
        this.console = console;
    }

    @Override
    public void run() {
        try {
            socket = new MulticastSocket(port);
            //controllo se è un multicast address
            if (!group.isMulticastAddress()) {
                throw new IllegalArgumentException(
                        "Indirizzo multicast non valido: " + group.getHostAddress());
            }
            //unisco al gruppo il socket
            socket.joinGroup(group);
            //loop per mettermi sempre in attesa di notifiche
            while (true){
                //creo il packet
                byte[] buf = new byte[BUFSIZE];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                //aspetto di ricevere la notifica del server
                socket.receive(packet);
                console.takeConsole();
                //stampo la notifica ricevuta
                System.out.println(new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim());
                console.releaseConsole();
            }
        } catch(IOException exception) {
            System.err.println("Problemi Multicast");
        }
        finally {
            try {
                socket.leaveGroup(group);
                socket.close();
            } catch (IOException e) {
                System.err.println("Problemi chiusura multicast");
            }
        }
    }

    public MulticastSocket getSocket() {
        return socket;
    }
}
