package server;

import client.RMIClientWinsome;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIServerWinsome extends Remote {

    String register (String username, String password,String tagList) throws RemoteException;
    void registerForCallBack(String username,RMIClientWinsome client) throws RemoteException;
    void unregisterForCallBack(String username, RMIClientWinsome client)throws RemoteException;

}
