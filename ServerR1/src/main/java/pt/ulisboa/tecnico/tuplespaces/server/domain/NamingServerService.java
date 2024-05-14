package pt.ulisboa.tecnico.tuplespaces.server.domain;

import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.NameServer.contract.NameServerOuterClass.RegisterRequest;
import pt.ulisboa.tecnico.NameServer.contract.NameServerOuterClass.DeleteRequest;
import pt.ulisboa.tecnico.NameServer.contract.NameServerOuterClass.LookupRequest;
import pt.ulisboa.tecnico.NameServer.contract.NameServerOuterClass.LookupResponse;
import pt.ulisboa.tecnico.NameServer.contract.NameServerGrpc;
import java.util.Collections;
import java.util.List;

public class NamingServerService {


    public NamingServerService() {
    }

    public static void register(String serviceName, String qualifier, int port, NameServerGrpc.NameServerBlockingStub stub) {
        try {
            RegisterRequest request = RegisterRequest.newBuilder()
                    .setAddress("localhost:" + port).setNameserver(serviceName).setQualifier(qualifier).build();
            stub.register(request);
        }
        catch (StatusRuntimeException e) {
            System.out.println("Not possible to register the server\n");
        }
    }

    public static void delete(String serviceName, String address, NameServerGrpc.NameServerBlockingStub stub) {

            DeleteRequest request = DeleteRequest.newBuilder().setServiceAddress(address).setServiceName(serviceName).build();
            try{
                stub.delete(request);
            }catch (StatusRuntimeException e){
                System.out.println("Not possible to delete the server\n"); 
            }


    }

    public static List<String> lookup(String serviceName, String qualifier, NameServerGrpc.NameServerBlockingStub stub){

        LookupRequest request = LookupRequest.newBuilder().setServicename(serviceName).setQualifier(qualifier).build();
        LookupResponse response;
        try {
            response = stub.lookup(request);}
        catch (StatusRuntimeException e){
            System.out.println("Not possible to lookup the server\n");
            return Collections.emptyList();
        }


        if (!response.getServersList().isEmpty())
            return response.getServersList();
        else
            return Collections.emptyList();
    }
}