import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')

import grpc
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
from NamingServerServiceImpl import NamingServerServiceImpl  
from concurrent import futures

# Define the port
PORT = 5001

if __name__ == '__main__':
    try:
        server = grpc.server(futures.ThreadPoolExecutor(max_workers=3))
        
        # Add service (use add_SequencerServicer_to_server for Sequencer service)
        pb2_grpc.add_NameServerServicer_to_server(NamingServerServiceImpl(), server)  # Corrected service name
        
        server.add_insecure_port('[::]:' + str(PORT))
        server.start()
        
        print("Server listening on port " + str(PORT))
        print("Press CTRL+C to terminate")
        
        server.wait_for_termination()

    except KeyboardInterrupt:
        print("Server stopped")
        exit(0)


