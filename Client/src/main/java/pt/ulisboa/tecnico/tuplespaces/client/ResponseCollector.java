// package pt.tecnico.grpc.client;
package pt.ulisboa.tecnico.tuplespaces.client;

import java.util.ArrayList;

public class ResponseCollector {
    ArrayList<Object> collectedResponses;

    public ResponseCollector() {
        collectedResponses = new ArrayList<Object>();
    }

    synchronized public void collect(Object s) {
        collectedResponses.add(s);
        notifyAll();
    }

    synchronized public ArrayList<Object> getItems() {
        ArrayList<Object> res = new ArrayList<Object>();
        for (Object s : collectedResponses) {
            res.add(s);
        }
        return res;
    }
    
    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (collectedResponses.size() < n) 
            wait();
    }
    
}
