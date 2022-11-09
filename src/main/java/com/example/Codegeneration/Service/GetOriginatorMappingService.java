package com.example.Codegeneration.Service;


import com.yubi.oss.DCG.mappingServiceGrpc;
import com.yubi.oss.DCG.request;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class GetOriginatorMappingService {

    @GrpcClient("mappingService")
    public mappingServiceGrpc.mappingServiceBlockingStub mappingStub;

    public String getOriginatorMappingFile(String originatorname){
        return mappingStub.getMappingFile(request.newBuilder().setOriginatorName(originatorname).build()).getMappingFile();
    }

}
