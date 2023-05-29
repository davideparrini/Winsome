package server.data_structure;

import utils.Utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


//Classe per rappresentare una trasazione

public class Transition implements Comparable<Transition> {
    private String timestamp; //data e ora della creazione della transazione
    private long time;  //tempo in mills delle creazione della transazione
    private double incremento; //incremento effettivo della transazione

    public Transition(){

    }
    public Transition(double incremento) {
        this.time = System.currentTimeMillis();
        this.timestamp = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").format(LocalDateTime.now());
        this.incremento = Math.round(100.0*incremento)/100.0;
    }


    public String getTimestamp() {
        return timestamp;
    }

    public double getIncremento() {
        return incremento;
    }

    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return " - timestamp: '" + timestamp + '\'' +
                ", incremento: " + Utils.arrotondaDoubleToString(incremento);
    }

    @Override
    public int compareTo(Transition o) {
        return (time - o.getTime()) > 0 ? 1 : -1;
    }
}
