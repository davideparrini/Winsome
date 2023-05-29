package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;

import static java.nio.file.StandardOpenOption.CREATE_NEW;


public class Utils {


    public static void checkFileExists(Path pathName, String nameFuction){
        if(!Files.exists(pathName)) {
            System.err.println("File passato non esiste in" + nameFuction);
        }
    }


    public static String fileToString(String fileName){
        //Estraggo da un file "fileName" una string contenente tutto il suo contenuto
        Path pathFile = Path.of(fileName);
        checkFileExists(pathFile,"fileToString");
        String jsonContent = null;
        try {
            jsonContent = Files.readString(Path.of(fileName));
        } catch (IOException e) {
            System.err.println("Errore I/O fileToString " + fileName);
        }
        return jsonContent;
    }
    public static void stringToFile(String jsonString, String nameOfNewFile) {
        //Creo un file da una stringa
        //Se si tratta di un file JSON, jsonString deve essere già serializzata
        //se esiste gia un file di "nameOfNewFile" viene eliminato e sostituito con quello nuovo.
        try {
            Files.deleteIfExists(Path.of(nameOfNewFile));
            Files.writeString(Path.of(nameOfNewFile), jsonString, CREATE_NEW);
        } catch (IOException e) {
            System.err.println("Errore I/O stringToFile");
        }
    }
    public static void deleteFile(String fileToDelete){
        try {
            Files.deleteIfExists(Path.of(fileToDelete));
        } catch (IOException e) {
            System.err.println("Errore I/O deleteFile");
        }

    }
    //restituisce un booleano alla domanda, è un intero la stringa? true -> si, false -> no
    public static boolean isInteger(String s){
        try {
            Integer.parseInt(s);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }
    //restituisce un booleano alla domanda, è un double la stringa? true -> si, false -> no
    public static boolean isDouble(String s){
        try {
            Double.parseDouble(s);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }
    //restituisce un booleano alla domanda, la stringa è un numero? true -> si, false -> no
    public static boolean isNumeber(String s){
        return isInteger(s) || isDouble(s);
    }

    //Metodo per arrontondare un double sottoforma di stringa, la precisione massima equivale alla massima possibile divisione del bitcoin ovvero 1 * 10^-8
    public static String arrotondaDoubleToString(double number){
        DecimalFormat df;
        if(number > 0.01) df = new DecimalFormat("#.##");
        else if(number > 0.001) df = new DecimalFormat("#.###");
        else if(number > 0.0001) df = new DecimalFormat("#.####");
        else if(number > 0.00001) df = new DecimalFormat("#.#####");
        else if(number > 0.000001) df = new DecimalFormat("#.######");
        else if(number > 0.0000001) df = new DecimalFormat("#.#######");
        else df = new DecimalFormat("#.########");
        return df.format(number);
    }
}
