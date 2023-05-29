package server.data_structure;

import utils.Utils;

import java.util.Iterator;
import java.util.TreeSet;


public class Wallet {
    double wincoins;
    TreeSet<Transition> transazioni;

    public Wallet(){
    }

    public Wallet(double wincoins) {
        this.wincoins = wincoins;
        this.transazioni = new TreeSet<>();
    }


    public void addWincoins(double toAdd){
        wincoins += toAdd;
        transazioni.add(new Transition(toAdd));
    }
    public void subtractWincoins(double toAdd){
        wincoins -= toAdd;
    }

    public double getWincoins() {
        return wincoins;
    }

    public void setWincoins(double wincoins) {
        this.wincoins = wincoins;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(" Wallet attuale: ").append(Utils.arrotondaDoubleToString(wincoins)).append(" Wincoins\n\n");
        if(transazioni.isEmpty()){
            builder.append("Non ci sono state trasazioni\n");
            return builder.toString();
        }
        builder.append("Storico trasazioni:\n");
        Iterator<Transition> iterator = transazioni.iterator();
        int i = 1;
        while (iterator.hasNext()){
            Transition t = iterator.next();
            builder.append("Transazione ").append(i).append(t.toString()).append("\n");
            i++;
        }
        return  builder.toString();
    }
}
