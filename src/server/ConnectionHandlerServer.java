package server;

import utils.Request;
import utils.Serializator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class ConnectionHandlerServer implements Runnable{

    private final BlockingQueue<TaskRequest> requestsList;
    private final int port;
    private final WinsomeServer server;
    private ServerSocketChannel serverChannel;
    public ConnectionHandlerServer(BlockingQueue<TaskRequest> requestsList, int port, WinsomeServer server) {
        this.requestsList = requestsList;
        this.port = port;
        this.server = server;
    }

    @Override
    public void run() {

        Selector selector = null;

        try {
            serverChannel = ServerSocketChannel.open();
            ServerSocket ss = serverChannel.socket();
            InetSocketAddress address = new InetSocketAddress("localhost",port);
            ss.bind(address);
            serverChannel.configureBlocking(false);
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException ex) {
            System.err.println("Problemi con apertura socketServerChannel");
        }

        while (true) {
            try {
                if (selector != null) {
                    selector.select();
                }
            } catch (IOException ex) {
                System.err.println("Problemi con selector");
                break;
            }
            Set<SelectionKey> readyKeys = Objects.requireNonNull(selector).selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                // rimuove la chiave dal Selected Set, ma non dal Registered Set
                try {
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        System.out.println("Accetto connessione dal client: " + client.getRemoteAddress());
                        client.configureBlocking(false);
                        client.register(selector,SelectionKey.OP_READ); // aspetto che il socketchannel sia readable

                    } else if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        System.out.println("Leggo dal client: " + client.getRemoteAddress());
                        //alloco un intBuffer dove otterrò dal client la dimensione della richiesta
                        ByteBuffer intBuffer = ByteBuffer.allocate(4);

                        if(client.read(intBuffer) == -1){ //se -1 è stato chiuso il channel di quel client, levo la key ed effettuo il logout correttamente
                            System.out.println("Chiusura client :" + client.getRemoteAddress());
                            server.logout(client.getRemoteAddress());
                            key.cancel();
                            key.channel().close();
                            continue;
                        }
                        intBuffer.flip(); //metto position a 0  e limit a position
                        //ottengo la dimensione della richiesta
                        int lenghtRequest = intBuffer.getInt();
                        //alloco un buffer con la dimensione giusta
                        ByteBuffer buffer = ByteBuffer.allocate(lenghtRequest);
                        //ricevo la richiesta
                        client.read(buffer);
                        //position 0, limit a position
                        buffer.flip();
                        //dentro buff c'è la Request serializzata con GSON
                        String reqSerializate = new String(buffer.array(),StandardCharsets.UTF_8).trim();
                        //deserializzo la richiesta
                        Request r = Serializator.deserializioneRequest(reqSerializate);
                        //stampo la richiesta
                        System.out.println("Richiesta di : \"" + r.getReqType() + "\" dal client :" + client.getRemoteAddress());
                        //creo un TaskRequest associando la Request con il channel di proveninza
                        TaskRequest task = new TaskRequest(r,client,server);
                        requestsList.put(task);
                    }
                } catch (IOException ex) {
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException ignored) {
                    }
                } catch (InterruptedException e) {
                    System.out.println("Chiusura Thread Connection");
                }
            }
        }
    }

    public ServerSocketChannel getServerChannel() {
        return serverChannel;
    }
}
