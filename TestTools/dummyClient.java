package testTools;


import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;





public class dummyClient {

	public static void main(String[] args) throws Exception {


		final String target = "localhost:5001"; 
        

		final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

		HelloWorldServiceGrpc.HelloWorldServiceBlockingStub stub = HelloWorldServiceGrpc.newBlockingStub(channel);
		HelloWorld.HelloRequest request = HelloWorld.HelloRequest.newBuilder().setName("friend").build();

		HelloWorld.HelloResponse response = stub.greeting(request);

		System.out.println(response);

		channel.shutdownNow();
	}

}
