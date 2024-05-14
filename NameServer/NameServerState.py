import sys
sys.path.insert(1, '../contract/target/generated-sources/protobuf/python')

from concurrent import futures
import grpc
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc



class serverEntry:
    def __init__(self, qualifier,address):
        self.qualifier = qualifier
        self.address = address

class ServiceEntry:
    def __init__(self, name):
        self.name = name
        self.servers = []
    
class NamingServer:
    def __init__(self):
        self.services_map = []

    def register_server(self, request):
        
        nameserver = request.nameserver
        qualifier = request.qualifier
        address = request.address
        
        serverEntry_aux = serverEntry(qualifier, address)
        serviceEntry_aux = ServiceEntry(nameserver)
        
        for serviceentry in self.services_map:
            if serviceentry.name == nameserver:
                for serverentry in serviceentry.servers:
                    if serverentry.qualifier == qualifier:
                        raise grpc.RpcError(grpc.StatusCode.NOT_FOUND, "Server already registered with the same qualifier")
                serviceentry.servers.append(serverEntry_aux)
                return
        serviceEntry_aux.servers.append(serverEntry_aux)
        self.services_map.append(serviceEntry_aux)
        
        for service in self.services_map:
            print("Service: ", service.name)
            for server in service.servers:
                print("Server: ", server.address, " ", server.qualifier)
        
        return
        
        
        
    def delete_server(self, request):
        service_name = request.service_name
        server_address = request.service_address
        

        for service in self.services_map:
            print("Service: ", service.name)
            if service.name == service_name:
                print("Service found")
                for server in service.servers:
                    if server.address == server_address:
                        print("Server found")
                        service.servers.remove(server)
                        if service.servers == []:
                            print("Service has no servers, removing service")
                            self.services_map.remove(service)
                        return

        # If server not found, raise an exception
        raise grpc.RpcError(grpc.StatusCode.NOT_FOUND, "Not possible to remove the server")
    
    def lookup_servers(self, request):
        service_name = request.servicename
        qualifier = request.qualifier
        res = []

        for service in self.services_map:
            if service.name == service_name:
                if qualifier == "":
                    # Case 1: No qualifier specified, return all servers
                    res.extend(server.address for server in service.servers)
                else:
                    # Case 2: Qualifier specified, return servers with matching qualifier
                    res.extend(server.address for server in service.servers if server.qualifier == qualifier)
                break  # Break once service is found
        return res

        
                    