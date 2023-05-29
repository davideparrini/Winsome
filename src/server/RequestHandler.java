package server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RequestHandler implements Runnable {
    private final long terminationDelay = 5000;
    private final BlockingQueue<TaskRequest> requests;

    public RequestHandler(BlockingQueue<TaskRequest> requests) {
        this.requests = requests;
    }

    @Override
    public void run() {

        ExecutorService workers = Executors.newCachedThreadPool();
        while(true){
            try {
                workers.execute(requests.take());

            } catch (InterruptedException e) {
                workers.shutdown();
                System.out.println("Chiusura Thread pool workers");
                try {
                    if (!workers.awaitTermination(terminationDelay,
                            TimeUnit.MILLISECONDS)) workers.shutdownNow();
                }
                catch (InterruptedException exception) {
                    workers.shutdownNow();
                    System.out.println("Chiudo Thread pool workers");
                    break;
                }
            }
        }
    }

}
