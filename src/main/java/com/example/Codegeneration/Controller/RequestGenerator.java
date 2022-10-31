package com.example.Codegeneration.Controller;

import com.example.Codegeneration.Entity.Payload;
import com.example.Codegeneration.Service.CodeGenerationService;
import com.example.Codegeneration.Service.GetOriginatorMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.codehaus.jettison.json.JSONException;
import org.json.simple.parser.ParseException;
import java.io.*;

@RestController
public class RequestGenerator {


    @Autowired
    private GetOriginatorMappingService getMappingService;

    @Autowired
    private CodeGenerationService codeGenerationService;


    @PostMapping("/generate")
    public String createRequest(@RequestBody Payload payload) throws JSONException, ParseException, IOException {

        String originatorname = payload.getOriginator_name();
        System.out.println(originatorname);

        //output path for storing the files
        String outputfolder = "/Users/manoj.kutala/Desktop/EFS/" + payload.getOriginator_name();

        File f1 = new File(outputfolder);
        boolean bool = f1.mkdir();
        if (bool) {
            System.out.println("Folder is created successfully");
        } else {
            System.out.println("Error Found!");
        }

        String outputPath = outputfolder + "/DCG";
        File f2 = new File(outputPath);
        bool = f2.mkdir();
        if (bool) {
            System.out.println("Folder is created successfully");
        } else {
            System.out.println("Error Found!");
        }


        String mappingjson = getMappingService.getOriginatorMappingFile(originatorname);

        String filelocation = codeGenerationService.generateCodeFiles(mappingjson,outputfolder,outputPath);

        return filelocation;
    }



}


