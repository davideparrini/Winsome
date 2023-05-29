package client;

public class MyConsole {
    //Classe console serve a gestire la console dei client, per farla lavorare in modo sincrono
    //Evitando cosi che mandando una richiesta al server vengano notificate o eseguite altre azioni sulla console


    private boolean console; //true se la console è libera/ a disposizione
                             //false se la console è occupata

    public MyConsole() {
        this.console = true;
    }

    //Metodo per ottenere la console
    public synchronized void takeConsole(){
        while (!console){
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            console = false;
            this.notifyAll();
        }
    }
    //Metodo per rilasciare la console
    public synchronized void releaseConsole(){
        console = true;
        this.notifyAll();
    }

}
