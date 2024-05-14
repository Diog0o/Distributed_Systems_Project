import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')

import grpc
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc


def run():
    with grpc.insecure_channel('localhost:5001') as channel:
        stub = pb2_grpc.SequencerStub(channel)
        response = stub.register(pb2.RegisterRequest(nameserver="Nome do server",qualifier="qualifier",address="localhost:60myPort"))
        print("Response received: " + response)
        #response = stub.lookup(pb2.LookupRequest(servicename="Nome do server",qualifier="qualifier"))

if __name__ == '__main__':
    run()
