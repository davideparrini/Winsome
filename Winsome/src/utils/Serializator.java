package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import server.data_structure.Post;
import server.data_structure.Wallet;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Serializator {

    //Metodi di serializzazione e deserializzazione di vari oggetti

    public static String serializzazioneObject(Object o){
        //serializzo in un Object
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(o);
    }

    public static ConcurrentHashMap<String,String> deserializioneCredenziali(String serializatedObj){
        //Deserializzo un oggetto ConcurrentHashMap<String,String>
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type MyClassObject = new TypeToken<ConcurrentHashMap<String,String>>(){}.getType();
        return gson.fromJson(serializatedObj,MyClassObject);
    }

    public static ConcurrentHashMap<String, CopyOnWriteArrayList<String>> deserializioneFollowMap(String serializatedObj){
        //Deserializzo un oggetto ConcurrentHashMap<String,CopyOnWriteArrayList<String>>
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type MyClassObject = new TypeToken<ConcurrentHashMap<String,CopyOnWriteArrayList<String>>>(){}.getType();
        return gson.fromJson(serializatedObj,MyClassObject);
    }

    public static ConcurrentHashMap<String, Wallet> deserializioneWallets(String serializatedObj){
        //Deserializzo un oggetto ConcurrentHashMap<String,Wincoin>
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type MyClassObject = new TypeToken<ConcurrentHashMap<String, Wallet>>(){}.getType();
        return gson.fromJson(serializatedObj,MyClassObject);
    }

    public static User deserializioneUtente(String serializatedObj){
        //Deserializzo un oggetto Utente
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type MyClassObject = new TypeToken<User>(){}.getType();
        return gson.fromJson(serializatedObj,MyClassObject);
    }

    public static Request deserializioneRequest(String serializatedObj){
        //Deserializzo un oggetto Request
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type MyClassObject = new TypeToken<Request>(){}.getType();
        return gson.fromJson(serializatedObj,MyClassObject);
    }

    public static Post deserializionePost(String serializatedObj){
        //Deserializzo un oggetto Post
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type MyClassObject = new TypeToken<Post>(){}.getType();
        return gson.fromJson(serializatedObj,MyClassObject);
    }


    public static Configurazione deserializioneConfigurazione(String serializatedObj){
        //Deserializzo un oggetto Configurazione
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type MyClassObject = new TypeToken<Configurazione>(){}.getType();
        return gson.fromJson(serializatedObj,MyClassObject);
    }

    public static ConcurrentHashMap<?,?> deserializioneGenericoConcurrentHashMap(String serializatedObj){
        //Deserializzo un oggetto ConcurrentHashMap<?,?> generica
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type MyClassObject = new TypeToken<ConcurrentHashMap<?,?>>(){}.getType();
        return gson.fromJson(serializatedObj,MyClassObject);
    }


    public static <T> Object deserializioneObject(String serializatedObj){
        //Deserializzo un oggetto Generico
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type MyClassObject = new TypeToken<T>(){}.getType();
        return gson.fromJson(serializatedObj,MyClassObject);
    }


}
