package server;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class ExchangerHandler implements Runnable{

    private final int waitingTime; // tempo da aspettare prima di aggiornare il valore dell' exchanger in bitcoin
    private final WinsomeServer server;

    public ExchangerHandler(int waitingTime,WinsomeServer server) {
        this.waitingTime = waitingTime;
        this.server = server;
    }

    @Override
    public void run() {
        while(true){

            InputStream in = null;
            BufferedReader reader = null;
            double exchangeValue = 0;
            try{
                String encoding = "ISO-8859-1";
                URL u = new URL("https://www.random.org/decimal-fractions/?num=1&dec=5&col=1&format=plain&rnd=new");
                URLConnection uc = u.openConnection();
                String contentType = uc.getContentType();
                int encodingStart = contentType.indexOf("charset=");
                if (encodingStart != -1) {
                    encoding = contentType.substring(encodingStart + 8);
                }
                in = new BufferedInputStream(uc.getInputStream());
                reader =new BufferedReader(new InputStreamReader(in, encoding));
                exchangeValue = Double.parseDouble(reader.readLine());

            } catch (IOException e) {
                System.err.println("Errore exchager");
            }
            finally {
                try {
                    if(reader != null) reader.close();
                    if(in != null) in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(exchangeValue == 0){
                System.err.println("ERRORE EXCHANGE");
            }
            server.setExchangeBitcoinValue(exchangeValue);
            try {
                Thread.sleep(waitingTime);
            } catch (InterruptedException e) {
                System.out.println("Chiusura Thread exchanger");
            }

        }

    }

}
