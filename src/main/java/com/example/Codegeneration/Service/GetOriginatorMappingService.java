package com.example.Codegeneration.Service;


import com.example.sample.request;
import com.example.sample.sampleGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class GetOriginatorMappingService {

    @GrpcClient("sample")
    public sampleGrpc.sampleBlockingStub stub;

    public String getOriginatorMappingFile(String originatorname){
        return stub.func(request.newBuilder().setReq(originatorname).build()).getRes();
    }

}
