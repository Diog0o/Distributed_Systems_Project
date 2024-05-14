package pt.ulisboa.tecnico.tuplespaces.server;

import java.io.IOException;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.NameServer.contract.NameServerGrpc;
import pt.ulisboa.tecnico.tuplespaces.server.domain.NamingServerService;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ServerMain {

	private static final String debugFlag = "-debug";
	private static boolean debug = false;
	private static String qualifier;
	private static final String A = "A";
	private static final String B = "B";
	private static final String C = "C";
	private static int port;
	private static String host = "localhost";
	private static int nsPort = 5001;
	private static String serviceName = "TupleSpace";

	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length == 3) {
			if (debugFlag.equals(args[2])) {
				System.err.println("Debug mode enabled");
				debug = true;
			}
		}

		if (debug) {
			System.err.printf("Received %d arguments%n", args.length);
		}
		if (debug) {
			for (int i = 0; i < args.length; i++)
				System.err.printf("arg[%d] = %s%n", i, args[i]);
		}
		// check arguments
		if (args.length < 1) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", ServerMain.class.getName());
			return;
		}

		port = Integer.parseInt(args[0]);

		qualifier = args[1];

		if (!(qualifier.equals(A) || qualifier.equals(B) || qualifier.equals(C))) {
			if (debug) {
				System.err.println("Invalid qualifier");
			}
			return;
		}

		final ServerState serverState = new ServerState();
		final BindableService impl = new TupleSpacesImpl(serverState, qualifier, debug);

		//create a stub to connect to naming server
		ManagedChannel channelNamingServer = ManagedChannelBuilder.forTarget(host + ":" + nsPort).usePlaintext().build();
		NameServerGrpc.NameServerBlockingStub stubNamingServer = NameServerGrpc.newBlockingStub(channelNamingServer);

		//register server in naming server
		NamingServerService.register(serviceName, qualifier, port, stubNamingServer);
		

		Server server = ServerBuilder.forPort(port)
				.addService(impl)
				.build();

		if (debug) {
			System.err.println("Server created");
		}

		server.start();

		if (debug) {
			System.err.println("Server started");
		}

		String address = "localhost:" + port;

		// Server shutdown
		System.out.println("Server started, listening on " + port);
		System.out.println("Press <ENTER> to shutdown the server");

        System.in.read();
		NamingServerService.delete(serviceName, address, stubNamingServer);

		channelNamingServer.shutdown();
		server.shutdown();
        
	}
}
