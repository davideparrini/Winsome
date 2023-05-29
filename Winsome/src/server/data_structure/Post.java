package server.data_structure;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Post{
    private int id; // id del post
    private String title; // titolo del post
    private String autor; // autore del post
    private String date; // timestamp di quando è stato creato
    private String content; //contenuto del post
    private List<CommentPost> comments; //lista dei commenti del post
    private List<String> positiveRate; //lista degli utenti che hanno messo un voto positivo
    private List<String> negativeRate; //lista degli utenti che hanno messo un voto negativo 
    private List<Integer> listIdPostRewin; //lista dei "figli"  del post, ovvero tutti gli id dei post che hanno rewinato questo post ( rewin del post = figlio, post rewinnato = padre)
    private boolean isRewinPost; //boolean se è un rewin -> true, altrimenti  false
    private Post rewined; //id del post padre, ( rewin del post = figlio, post rewinnato = padre)

    private ConcurrentHashMap<String, Integer> counterComments_per_Commentator; // indica il numero di commenti che una certa persona p ha fatto
    private int n_iteractions; //numero volte post, sottoposto a conto ricompensa
    private List<String> curators; //lista dei curatori del post

    //Costruttore per deserializzazione
    public Post() {
    }
    //Costruttore nuovo post
    public Post(int id, String autor,String title, String content) {
        this.id = id;
        this.autor = autor;
        this.title = title;
        this.date = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").format(LocalDateTime.now());
        this.content = content;
        this.isRewinPost = false;
        this.listIdPostRewin = new CopyOnWriteArrayList<>();
        this.comments = new CopyOnWriteArrayList<>();
        this.positiveRate = new CopyOnWriteArrayList<>();
        this.negativeRate = new CopyOnWriteArrayList<>();
        this.counterComments_per_Commentator = new ConcurrentHashMap<>();
        this.curators = new CopyOnWriteArrayList<>();
        this.n_iteractions = 0;

    }

    //Costruttore post rewin
    public Post(int id, String autor,Post toRewin){
        this.id = id;
        this.autor = autor;
        this.title = "Rewin post " + toRewin.getId();
        this.date = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").format(LocalDateTime.now());
        this.content = toRewin.toStringRewin();
        this.rewined = toRewin;
        this.isRewinPost = true;
        toRewin.addRewin(id);
    }

    //Metodo per aggiungere un voto al post
    public void ratePost(String utente, int rate){
        if(rate > 0) positiveRate.add(utente);
        if(rate < 0) negativeRate.add(utente);
        if(!curators.contains(utente)) curators.add(utente);

    }

    //Metodo per aggiungere commento
    public void addComment(String userOfComment,String comment){
        //Creo commento
        CommentPost commentPost = new CommentPost(userOfComment,comment);
        comments.add(commentPost);
        //Aggiungo il commentatore alla lista dei curatori del post, se non è gia presente
        if(!curators.contains(userOfComment)) curators.add(userOfComment);
        
        //Aggiungo il commentatore alla mappa commentatore-Numero di commenti fatti
        //se userOfComment non è presente nella mappa lo aggiungo con valore 1, senno sommo 1 al contatore della map
        counterComments_per_Commentator.merge(userOfComment,1,Integer::sum);
    }
    //Metodo per aggiungere l'id di un figlio del post ( rewin del post = figlio, post rewinnato = padre)
    public void addRewin(int idPostRewin){
        listIdPostRewin.add(idPostRewin);
    }

    public boolean containsRewin(){
        return !listIdPostRewin.isEmpty();
    }

    //Metodo getter per recuperare l'id del post padre ( rewin del post = figlio, post rewinnato = padre) 
    public int getIdPostRewinato(){
        if(!isRewinPost){
            System.err.println("Se non è un rewin, non può ottenere l'id del post che ha rewinato");
            return -1;
        }
        return rewined.getId();
    }


    //Metodo per calcolo ricompensa

    public double rewardingCalculation(){
        n_iteractions++;
        double sommatoria2 = 0;
        if(counterComments_per_Commentator != null) {
            for (String s : counterComments_per_Commentator.keySet()) {
                sommatoria2 += (2 / (1 + Math.exp(-1 * (counterComments_per_Commentator.get(s) - 1))));
            }
        }
        double denominatore =(Math.log( Math.max(positiveRate.size() + negativeRate.size(),0) +1 ) + Math.log(sommatoria2 +1));
        return denominatore == 0 ? 0 : denominatore/ n_iteractions;
    }

//toString
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        //è un rewin
        if(isRewinPost){
            s.append("Titolo: \"" + title +
                    "\"\nContenuto: \t" + content +
                    "\nVoti: " + rewined.getPositiveRate().size() + " positivi, " + rewined.getNegativeRate().size() + " negativi\n" +
                    "Commenti: ");
            if(rewined.getComments().isEmpty()) s.append("0");
            else{
                s.append("\n");
                for(CommentPost c : rewined.comments){
                    s.append("\t");
                    s.append(c.toString());
                    s.append("\n");
                }
            }
            return s.toString();
        }
        //non è un rewin
        s.append("Titolo: \"" + title +
                "\"\nData e ora: \t\"" + date +
                "\"\nContenuto: \t\"" + content +
                "\"\nVoti: " + positiveRate.size() + " positivi, " + negativeRate.size() + " negativi\n" +
                "Commenti: ");
        if(comments.isEmpty()) s.append("0");
        else{
            s.append("\n");
            for(CommentPost c : comments){
                s.append("\t");
                s.append(c.toString());
                s.append("\n");
            }
        }
        return s.toString();
    }

    //toString utile per il comando Show Feed
    public String toStringFeed(){
        return id + "\t|\t" + autor + "\t|\t\"" + title + "\"";
    }

    //toString per post rewinato
    public String toStringRewin(){
        return "\n\tAutore: " + autor +
                "\n\tTitolo: \"" + title + "\"\n\tContenuto: \"" + content + "\" ";
    }




//Metodi getters and setters

    public int getId() {
        return id;
    }


    public String getAutor() {
        return autor;
    }


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<CommentPost> getComments() {
        return comments;
    }



    public List<String> getPositiveRate() {
        return positiveRate;
    }


    public List<String> getNegativeRate() {
        return negativeRate;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Integer> getListIdPostRewin() {
        return listIdPostRewin;
    }



    public boolean isRewinPost() {
        return isRewinPost;
    }


    public List<String> getCurators() {
        return curators;
    }

    public int sizeCuratori(){
        if(curators == null) return 0;
        return curators.size();
    }

    public Post getRewined() {
        return rewined;
    }

    public String getDate() {
        return date;
    }
}
