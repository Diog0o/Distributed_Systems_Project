package pt.ulisboa.tecnico.tuplespaces.server;

import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;

import io.grpc.stub.StreamObserver;

import java.util.List;



public class TupleSpacesImpl extends TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase {

    private final ServerState serverState;
    private final String qualifier;
    private final boolean debug;

    public TupleSpacesImpl(ServerState serverState, String qualifier, boolean debug) {
        this.serverState = serverState;
        this.qualifier = qualifier;
        this.debug = debug;
    }


    
    @Override
    public void put (PutRequest request, StreamObserver<PutResponse> responseObserver) {
        if (debug) System.err.println("Received put request");
        
        serverState.put(request.getNewTuple(), request.getSeqNumber());

        PutResponse response = PutResponse.newBuilder().build();
        responseObserver.onNext(response);
        if (debug) System.err.println("Sent put response");
        responseObserver.onCompleted();
    }

    @Override
    public void read (ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        if (debug) System.err.println("Received read request");
        String tuple = serverState.read(request.getSearchPattern());

        
        ReadResponse response = ReadResponse.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);
        if (debug) System.err.println("Sent read response");
        responseObserver.onCompleted();

    }

    @Override
    public void take (TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
        if (debug) System.err.println("Received take request");
        
        String tuple = serverState.take(request.getSearchPattern(), request.getSeqNumber());

        TakeResponse response = TakeResponse.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);

        if (debug) System.err.println("Sent take response");
        responseObserver.onCompleted();

    }

    @Override
    public void getTupleSpacesState (getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> responseObserver) {
        if (debug) System.err.println("Received getTupleSpacesState request");
        List<String> tuples = serverState.getTupleSpacesState();
        
        getTupleSpacesStateResponse response = getTupleSpacesStateResponse.newBuilder().addAllTuple(tuples).build();
        responseObserver.onNext(response);
        if (debug) System.err.println("Sent getTupleSpacesState response");
        responseObserver.onCompleted();

    }
}
