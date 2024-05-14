# Import the generated classes
import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')

import grpc
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc

# Import the classes you created for managing server registration
from NameServerState import NamingServer, ServiceEntry, serverEntry

# Implement the NamingServerServiceImpl class extending pb2_grpc.Sequencer
class NamingServerServiceImpl(pb2_grpc.NameServerServicer):

    def __init__(self):
        self.naming_server = NamingServer()

    def register(self, request, context):
        response = self.naming_server.register_server(request)
        return pb2.RegisterResponse()

    def lookup(self, request, context):
        response = self.naming_server.lookup_servers(request)
        return pb2.LookupResponse(servers=response)

    def delete(self, request, context):
        try:
            self.naming_server.delete_server(request)
            return pb2.DeleteResponse()  # Return empty response for success
        except grpc.RpcError as e:
            # Handle RpcError and return appropriate response
            context.set_code(e.code())
            context.set_details(e.details())
        return pb2.DeleteResponse()
    