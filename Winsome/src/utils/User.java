package utils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class User {
   private String username;
   private String password;
   private List<String> listTags;


    public User() {
    }

    public User(User u){
        this.username = u.getUsername();
        this.password = u.getPassword();
        this.listTags = u.getListTags();;
    }

    public User(String username, String password, String[] listTags) {
        //nuovo utente e si inizializza tutti i campi
        this.username = username;
        this.password = password;
        this.listTags = new LinkedList<>();
        this.listTags.addAll(Arrays.asList(listTags));
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }



    //Getters

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getListTags() {
        return listTags;
    }


    //Setters


    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setListTags(List<String> listTags) {
        this.listTags = listTags;
    }

}

