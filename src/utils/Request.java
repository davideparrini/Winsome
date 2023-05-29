package utils;

import java.util.Arrays;

public class Request{
    private RequestType reqType;
    private String[] arg;


    //Costruttori

    public Request(){
    }


    public Request(RequestType reqType, String[] arg) {
        this.reqType = reqType;
        this.arg = arg;
    }

    public Request(RequestType reqType) {
        this.reqType = reqType;
    }



//Getters and setters

    public RequestType getReqType() {
        return reqType;
    }

    public void setReqType(RequestType reqType) {
        this.reqType = reqType;
    }

    public String[] getArg() {
        return arg;
    }

    public void setArg(String[] arg) {
        this.arg = arg;
    }

    @Override
    public String toString() {
        return "Request{" +
                "reqType=" + reqType +
                ", arg=" + Arrays.toString(arg) +
                '}';
    }
}
