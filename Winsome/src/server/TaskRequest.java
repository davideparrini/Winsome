package server;

import utils.Request;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;

public class TaskRequest implements Runnable{

    private final Request r;
    private final SocketChannel channel;
    private WinsomeServer server;

    public TaskRequest(Request req, SocketChannel channel, WinsomeServer server) {
        this.r = req;
        this.channel = channel;
        this.server = server;
    }


    @Override
    public void run() {
        String reply = null; //risposta da inviare al client
        String[] arg = r.getArg(); //argomenti
        SocketAddress address = null; //address del richiedente della richiesta
        try {
           address = channel.getRemoteAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(address == null){
            System.exit(1);
        }
        switch (r.getReqType()){
            case LOGIN :
                try {
                    reply = server.login(arg[0],arg[1],address);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            break;

            case LOGOUT:
                reply = "Ti sei disconnesso da Winsome, a presto!";
                break;

            case LIST_USERS:
                reply = server.listUsers(address);
                break;

            case FOLLOW:
                try {
                    reply = server.follow(arg[0],address);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;

            case UNFOLLOW:
                try {
                    reply = server.unfollow(arg[0], address);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;

            case BLOG:
                reply = server.viewBlog(address);
                break;

            case LIST_FOLLOWING:
                reply = server.listFollowing(address);
                break;

            case POST:
                reply = server.createPost(arg[0],arg[1],address);
                break;

            case DELETE_POST:
                reply = server.deletePostRequest(Integer.parseInt(arg[0]),address);
                break;

            case SHOW_POST:
                reply =  server.showPost(Integer.parseInt(arg[0]),address);
                break;

            case SHOW_FEED:
                reply = server.showFeed(address);
                break;

            case RATE:
                reply = server.ratePost(Integer.parseInt(arg[0]),Integer.parseInt(arg[1]),address);
                break;

            case COMMENT:
                reply = server.addComment(Integer.parseInt(arg[0]),arg[1],address);
                break;

            case REWIN:
                reply = server.rewinPost(Integer.parseInt(arg[0]),address);
                break;

            case WALLET_BTC:
                reply = server.walletBTC(address);
                break;

            case WALLET:
                reply = server.wallet(address);
                break;
        }

        if(reply == null){
            System.err.println("Richiesta non eseguita correttamente, address -> " + address);
            return;
        }
        try {
            //creo un intBuff (buffer per int) per mandare al client la dimensione della mia risposta
            ByteBuffer intBuff = ByteBuffer.allocate(4);
            intBuff.putInt(reply.getBytes(StandardCharsets.UTF_8).length);
            intBuff.flip();
            channel.write(intBuff);
            //Scrivo su buffer la risposta e la invio al client
            ByteBuffer buffer = ByteBuffer.wrap(reply.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public WinsomeServer getServer() {
        return server;
    }

    public void setServer(WinsomeServer server) {
        this.server = server;
    }
}
