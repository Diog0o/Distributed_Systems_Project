package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;


public class ClientMain {

    static final int numServers = 3;
    private static final String debugFlag = "-debug";
    private static boolean debug = false;

    public static void main(String[] args) {

        System.out.println(ClientMain.class.getSimpleName());

        if (args.length == 1) {
            if (debugFlag.equals(args[0])) {
				System.err.println("Debug mode enabled");
				debug = true;
			}
        }
        if (debug){
            ClientService.setDebug();
        }

        
        CommandProcessor commandProcessor = new CommandProcessor(new ClientService(ClientMain.numServers));

        if (debug) {
            System.err.println("Debug mode enabled");
        }

        if (debug) {
            System.err.printf("Starting command processor");
        }


        // Parse the input
        commandProcessor.parseInput();
        
        return;
    
    }

}
