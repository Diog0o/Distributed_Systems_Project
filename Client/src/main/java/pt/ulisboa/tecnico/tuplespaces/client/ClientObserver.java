package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.stub.StreamObserver;


public class ClientObserver<R> implements StreamObserver<R> {

    private ResponseCollector collector;

    public ClientObserver(ResponseCollector collector) {
        this.collector = collector;
    }
    
    
    @Override
    public void onNext(R r) {
        collector.collect(r);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Received error: " + throwable);
        collector.collect(throwable);
    }

    @Override
    public void onCompleted() {
    }


}
