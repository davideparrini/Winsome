package server;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;


public class RewardSendingHandler implements  Runnable{

    private InetAddress addr;
    private final int port;
    private final int rewardTime;
    private final WinsomeServer server;

    public RewardSendingHandler(String addr, int port,int rewardTime,WinsomeServer server) {
        try {
            this.addr = InetAddress.getByName(addr);
        } catch (UnknownHostException e) {
            System.err.println("Indirizzo multicast non valido");
            e.printStackTrace();
        }
        this.port = port;
        this.rewardTime = rewardTime;
        this.server = server;
    }


        //Getters and Setters


    @Override
    public void run() {

        //inizializzo il  DatagramPacket da inviare ogni volta ai client
        String rewardString = "Sono stati aggiornati i wallet!";
        byte[] buf = rewardString.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, addr, port);

        try (DatagramSocket socket = new DatagramSocket()) {
            while(true){
                Thread.sleep(rewardTime);
                server.calcoloRewarding();
                if (!WinsomeServer.connessioni.isEmpty()){
                    socket.send(packet);
                    System.out.println("Inviato reward notify");
                }else {
                    System.out.println("Calcolati rewarding, aggiornati wallet!");
                }

            }
        } catch (IOException e) {
            System.err.println("Errore invio rewarding notify");
        } catch (InterruptedException e) {
            System.out.println("Chiusura Thread reward");
        }
    }

}
