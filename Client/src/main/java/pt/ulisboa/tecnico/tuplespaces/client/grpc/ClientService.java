package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import pt.ulisboa.tecnico.tuplespaces.client.ClientObserver;
import pt.ulisboa.tecnico.tuplespaces.client.ResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;


//NS
import pt.ulisboa.tecnico.NameServer.contract.NameServerGrpc;
import pt.ulisboa.tecnico.NameServer.contract.NameServerOuterClass.LookupRequest;
import pt.ulisboa.tecnico.NameServer.contract.NameServerOuterClass.LookupResponse;

//Sequencer
import pt.ulisboa.tecnico.sequencer.contract.SequencerGrpc;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass.GetSeqNumberRequest;

//Replica Contract
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc;

//Grpc Libraries
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

//Java Libraries





public class ClientService {
    //Constant Parameters
    private static final int MIN_TIME_BETWEEN_REQUESTS = 50; //ms between retrys of same request operations


    //replica stubs
    private TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[] asyncStubs;
    private TupleSpacesReplicaGrpc.TupleSpacesReplicaBlockingStub[] blockingStub;
    private ManagedChannel[] channels;

    private static boolean debug = false;
    private String host = "localhost";
    //NameServer
    private static int nsPort = 5001;
    private NameServerGrpc.NameServerBlockingStub stubNamingServer;
    private ManagedChannel channelNamingServer;
    //Sequencer
    private static int sequencerPort = 8080;
    private SequencerGrpc.SequencerBlockingStub stubSequencer;
    private ManagedChannel channelSequencer;


    private int numServers;


    OrderedDelayer delayer;
    

    public ClientService(int numServers) {
        
        //The delayer can be used to inject delays to the sending of requests to the
        //different servers, according to the per-server delays that have been set
        delayer = new OrderedDelayer(numServers);

        this.numServers = numServers;


        channels = new ManagedChannel[numServers];
        asyncStubs = new TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[numServers];
        blockingStub = new TupleSpacesReplicaGrpc.TupleSpacesReplicaBlockingStub[numServers];

        //sequencer channel initialization
        channelSequencer = ManagedChannelBuilder.forTarget(host + ":" + sequencerPort).usePlaintext().build();
        stubSequencer = SequencerGrpc.newBlockingStub(channelSequencer);
        
        
        for (int i = 0; i < numServers; i++) {
            String target = null;
            String qualifier;

            if (i == 0){
                qualifier = "A";
            }else if (i == 1){
                qualifier = "B";
            }else{
                qualifier = "C";
            }
        
            target = getIpFromNS(qualifier);
            if (target == null) {
                System.out.println("Error could not get the Server.java Ip.\nExiting...");
                System.exit(1);

                return;
            }
            this.channels[i] = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
            this.blockingStub[i] = TupleSpacesReplicaGrpc.newBlockingStub(channels[i]);
            this.asyncStubs[i] = TupleSpacesReplicaGrpc.newStub(channels[i]);
            
        }

    }

    // Get the server ip from the naming server with timeouts to wait for the server
    // to be up
    private String getIpFromNS(String qualifier) {
        int maxAttempts = 3;
        int currentAttempt = 0;
        String tempTarget = null;

        channelNamingServer = ManagedChannelBuilder.forTarget(host + ":" + nsPort).usePlaintext().build();
        stubNamingServer = NameServerGrpc.newBlockingStub(channelNamingServer);

        tempTarget = requestIpFromNS(qualifier);
        maxAttempts--;
        while (tempTarget == null && currentAttempt < maxAttempts) { //timeout and try again

            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            tempTarget = requestIpFromNS(qualifier);
            currentAttempt++;
        }

        channelNamingServer.shutdown();

        if (tempTarget != null) {
            return tempTarget;
        }

        return null;

    }

    // Send single request to the naming server
    private String requestIpFromNS(String qualifier) {
        LookupResponse response = null;
        LookupRequest request = LookupRequest.newBuilder().setServicename("TupleSpace").setQualifier(qualifier).build();
        if (debug) {
            System.out.println("Looking for server");
        }

        try {
            response = stubNamingServer.lookup(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getDescription().equals("io exception")) {
                System.out.println("Could not connect to the NameServer.");
                return null;
            } else {
                System.out.println(e.getStatus().getDescription());
            }
        }

        if (response == null) {
            return null;
        }

        if (debug) {
            System.out.println("Server found");
        }

        if (!response.getServersList().isEmpty()) {
            return response.getServersList().get(0);
        }

        return null;
    }

    public static void setDebug() {
        ClientService.debug = true;
    }



    //grows linear with the number of retries
    void sleepTimeout(int weight){
        final int delta = 100; //interval of randomness to let concurrent clients to not collide
        final int base = MIN_TIME_BETWEEN_REQUESTS; //starting time to wait
        weight = (weight+1); //avoid 0
        int min = base * weight/2;
        int max = min + delta;
        try{
            Thread.sleep(min + (int)(Math.random()*(max-min)) );} 
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //to avoid server and network congestion
    void sleepLittle(){
        try{
            Thread.sleep(MIN_TIME_BETWEEN_REQUESTS);}
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void put(String tuple) {

        ResponseCollector collector = new ResponseCollector();

        try {
            if (debug) {
                System.err.println("Starting put action");
            }
            int seqNumber = getSequenceNumber();
            PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).setSeqNumber(seqNumber).build();

            for (int i : delayer) {
                if (asyncStubs[i] == null) {
                    System.out.println("Server not found");
                    return;
                }
                asyncStubs[i].put(request, new ClientObserver<PutResponse>(collector));
            }

            try {
                collector.waitUntilAllReceived(numServers);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("OK");

            if (debug)
                System.err.println("Put action was successful");
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getDescription().equals("io exception")) {
                System.out.println("Lost connection to the Server.");
                
                return;

            } else {
                System.out.println(e.getStatus().getDescription());
            }
        }
    }

    public void read(String tuple) {
        ResponseCollector collector = new ResponseCollector();
        try {

            if (debug) {
                System.err.println("Starting read action");
            }



            ReadRequest request = ReadRequest.newBuilder().setSearchPattern(tuple).build();
            for (int i:delayer) {
                if (asyncStubs[i] == null) {
                    System.out.println("Server not found");
                    return;
                }
                asyncStubs[i].read(request, new ClientObserver<ReadResponse>(collector));
            }

            
            
            try {
                collector.waitUntilAllReceived(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ReadResponse response = (ReadResponse) collector.getItems().get(0);
            if (response.getResult().equals("")){
                System.out.println("OK\n" + "No tuple found");
            }
            else{
            System.out.println("OK\n" + response.getResult());
            }

            if (debug)
                System.err.println("Read action was successful");
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getDescription().equals("io exception")) {
                System.out.println(
                    "Lost connection to the Server. Did it moved?\nRequesting new ip from the NameServer.");

                return;

            } else {
                System.out.println(e.getStatus().getDescription());
            }
        }

    }



    public void take(String tuple) {

        ResponseCollector collector; 
        TakeResponse response = null;

        if (debug) System.err.println("Starting take action");
        
        try {
            collector = new ResponseCollector();
            // send request to all servers
            int seqNumber = getSequenceNumber();
            TakeRequest request1 = TakeRequest.newBuilder().setSearchPattern(tuple).setSeqNumber(seqNumber).build();
            for (int i : delayer) {
                if (asyncStubs[i] == null) {
                    System.out.println("Server not found");
                    return;
                }
                asyncStubs[i].take(request1, new ClientObserver<TakeResponse>(collector));
            }
            if(debug) System.err.println("Waiting for all servers response...");
            try {    
                collector.waitUntilAllReceived(numServers);}
            catch (InterruptedException e) {
                e.printStackTrace();
            }

            response = (TakeResponse) collector.getItems().get(0);
            System.out.println("OK\n" + response.getResult());
            
            if (debug)
                System.err.println("Take action was successful");


        } catch (StatusRuntimeException e) {
            if (e.getStatus().getDescription().equals("io exception")) {
                System.out.println(
                    "Lost connection to the Server.");
                return;

            } else {
                System.out.println(e.getStatus().getDescription());
            }
        }
    }

    public void getTupleSpacesState(String qualifier) {

        if (debug) {
            System.err.println("Starting getTupleSpacesState action");
        }

        try {
            // Create the request
            getTupleSpacesStateRequest request = getTupleSpacesStateRequest.newBuilder().build();
            int index = indexOfQualifier(qualifier);
            if (index == -1){
                System.out.println("Invalid Qualifier");
                return;
            }
            if (asyncStubs[index] == null) {
                System.out.println("Server not found");
                return;
            }

            getTupleSpacesStateResponse response = blockingStub[index].getTupleSpacesState(request);

            if (response.getTupleList().isEmpty()) {
                System.out.println("OK\nNo tuple found");
            } else {
                System.out.println("OK\n" + response.getTupleList());
            }

            if (debug)
                System.err.println("getTupleSpacesState action was successful");
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getDescription().equals("io exception")) {
                System.out.println(
                    "Lost connection to the Server.");
                return;

            } else {
                System.out.println(e.getStatus().getDescription());
            }
        }
    }



    // No final fechar o canal
    public void shutdown() {

        if (debug) {
            System.err.println("Shutting down client service");
        }

        for (int i = 0; i < numServers; i++) {
            channels[i].shutdown();
        }
        channelNamingServer.shutdown();
    
    }

    /*
     * This method allows the command processor to set the request delay assigned to
     * a given server
     */
    public void setDelay(int id, int delay) {
        delayer.setDelay(id, delay);
    }
    /*
     * Example: How to use the delayer before sending requests to each server
     * Before entering each iteration of this loop, the delayer has already
     * slept for the delay associated with server indexed by 'id'.
     * id is in the range 0..(numServers-1).
     * 
     * for (Integer id : delayer) {
     * stub[id].some_remote_method(some_arguments);
     * }
     */


    public String QualifierOfServerIndex(int index) {
        switch (index) {
            case 0:
                return "A";
            case 1:
                return "B";
            case 2:
                return "C";
            default:
                return null;
        }
    }

    private int indexOfQualifier(String qual){
        switch(qual){
            case "A":
                return 0;
            case "B":
                return 1;
            case "C":
                return 2;
            default:
                return -1;
        }
    }

    private int getSequenceNumber(){
        try{
            return stubSequencer.getSeqNumber(GetSeqNumberRequest.newBuilder().build()).getSeqNumber();
        }
        catch (StatusRuntimeException e) {
            if (e.getStatus().getDescription().equals("io exception")) {
                System.out.println(
                    "Lost connection to the Sequence Server. Did it stop?\n");

                return -1;

            } else {
                System.out.println(e.getStatus().getDescription());
                return -1;
            }
        }
    }
}
