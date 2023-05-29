package client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.CopyOnWriteArrayList;

public interface RMIClientWinsome extends Remote {

    void notifyFollow(String username) throws RemoteException;
    void notifyUnfollow(String username) throws RemoteException;
    void updateFollows(CopyOnWriteArrayList<String> followers,CopyOnWriteArrayList<String> newFollowers) throws RemoteException;
    void updateUnfollows(CopyOnWriteArrayList<String> newUnfollowers) throws RemoteException;

}
