package utils;

public class Configurazione {

    private int TCPPORT; //Porta TCP del server

    private String MULTICAST; //Indirizzo di multicast

    private int MCASTPORT; //Porta di multicast

    private String REGHOST; //Host su cui si trova il registry

    private int REGPORT; //Porta del registry RMI

    private int REWARD_TIME; //Timeout per invocare calcolo ricompensa

    private int EXCHANGE_TIME; //Timeout per cambiare il valore dell'exchange in Bitcoin

    private float PERCENTUALE_REWARD_AUTHOR; //Percentuale di ricompensa che aspetta all'autore del post


    //Costruttori

    public Configurazione() {
    }



    public Configurazione(String pathFileConfigurazione) {

        String config = Utils.fileToString(pathFileConfigurazione);
        Configurazione configurazione = Serializator.deserializioneConfigurazione(config);
        this.TCPPORT = configurazione.getTCPPORT();
        this.MULTICAST = configurazione.getMULTICAST();
        this.MCASTPORT = configurazione.getMCASTPORT();
        this.REGHOST = configurazione.getREGHOST();
        this.REGPORT = configurazione.getREGPORT();
        this.REWARD_TIME = configurazione.getREWARD_TIME();
        this.EXCHANGE_TIME = configurazione.getEXCHANGE_TIME();
        this.PERCENTUALE_REWARD_AUTHOR = configurazione.getPERCENTUALE_REWARD_AUTHOR();
    }



    public int getTCPPORT() {
        return TCPPORT;
    }

    public String getMULTICAST() {
        return MULTICAST;
    }

    public int getMCASTPORT() {
        return MCASTPORT;
    }

    public String getREGHOST() {
        return REGHOST;
    }

    public int getREGPORT() {
        return REGPORT;
    }

    public int getREWARD_TIME() {
        return REWARD_TIME;
    }

    public int getEXCHANGE_TIME() {
        return EXCHANGE_TIME;
    }

    public float getPERCENTUALE_REWARD_AUTHOR() {
        return PERCENTUALE_REWARD_AUTHOR;
    }

    @Override
    public String toString() {
        return "Configurazione{" +
                "TCPPORT=" + TCPPORT +
                ", MULTICAST='" + MULTICAST + '\'' +
                ", MCASTPORT=" + MCASTPORT +
                ", REGHOST='" + REGHOST + '\'' +
                ", REGPORT=" + REGPORT +
                ", REWARD_TIME=" + REWARD_TIME +
                ", EXCHANGE_TIME=" + EXCHANGE_TIME +
                ", PERCENTUALE_REWARD_AUTHOR=" + PERCENTUALE_REWARD_AUTHOR +
                '}';
    }
}
