package com.example.Codegeneration;

import com.example.sample.request;
import com.example.sample.request1;
import com.example.sample.sample1Grpc;
import com.example.sample.sampleGrpc;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.codehaus.jettison.json.JSONException;

import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;

@RestController
public class RequestGenerator {
    @Autowired
    private TemplateEngine textTemplateEngine;

    @GrpcClient("sample")
    public sampleGrpc.sampleBlockingStub stub;

    @GrpcClient("sample1")
    public sample1Grpc.sample1BlockingStub stub1;

    static HashMap<String, String> maps = new HashMap<>();
    static List<String> apinames = new ArrayList<String>();

    @PostMapping("/generate")
    public String createRequest(@RequestBody Payload payload) throws JSONException, ParseException, IOException {

        System.out.println(payload);
        System.out.println(payload.getOriginator_name());
        String originatorname = payload.getOriginator_name();


        final Context context = new Context();

        RestTemplate restTemplate = new RestTemplate();


        //output path for storing the files
        String outputfolder = "/Users/manoj.kutala/Desktop/EFS/" + payload.getOriginator_name();


        File f1 = new File(outputfolder);
        boolean bool = f1.mkdir();
        if (bool) {
            System.out.println("Folder is created successfully");
        } else {
            System.out.println("Error Found!");
        }

        String outputPath = "/Users/manoj.kutala/Desktop/EFS/" + payload.getOriginator_name() + "/DCG";
        File f2 = new File(outputPath);
        bool = f2.mkdir();
        if (bool) {
            System.out.println("Folder is created successfully");
        } else {
            System.out.println("Error Found!");
        }


//        //getting other files of sdk
//        File src = new File("/Users/manoj.kutala/Desktop/DCG");
//        File dest = new File(outputPath);
//
//        try {
//            FileSystemUtils.copyRecursively(src, dest);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        String a = stub.func(request.newBuilder().setReq(originatorname).build()).getRes();

        JSONObject jsonObject = new JSONObject(a);


        JSONArray apiList = jsonObject.getJSONArray("consolidatedAPIs");

        JSONArray webhookList = jsonObject.getJSONArray("consolidatedWebhooks");

        apiList.putAll(webhookList);



        String key2 = "originatorFieldName";
        String key1 = "yubiFieldName";
        JsonParser p = new JsonParser();



//        Iterator<String> iterator = apiList.iterator();

        apiList.forEach(api -> {
            JSONObject jsonObj = null;
            try {
                jsonObj = new JSONObject(api.toString());
                System.out.println("jsonObj------->" + jsonObj);



                String api_name="";

                if(jsonObj.has("api_name")){
                    api_name = (String) jsonObj.get("api_name");
                } else if (jsonObj.has("webhook_name")) {
                    api_name = (String) jsonObj.get("webhook_name");
                }
                System.out.println("api_name-------->"+api_name);
                apinames.add(api_name);

                maps = new HashMap<>();
                check(api_name, key1, key2, p.parse(jsonObj.toString()));
                System.out.println(maps);

                System.out.println("*********\n");

                maps.forEach((k, v) -> context.setVariable(k, v));

                String request_text = textTemplateEngine.process("/"+api_name+"/YubiRequest", context);
                FileWriter request_file = new FileWriter(outputPath + "/"+api_name+"Request.java");
                request_file.write(request_text);
                request_file.close();

                String response_text = textTemplateEngine.process("/"+api_name+"/YubiResponse", context);
                FileWriter response_file = new FileWriter(outputPath + "/"+api_name+"Response.java");
                response_file.write(response_text);
                response_file.close();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        context.setVariable("apinames", apinames);

        String request_text = textTemplateEngine.process("/loan", context);
        FileWriter request_file = new FileWriter(outputPath + "/Loan.java");
        request_file.write(request_text);
        request_file.close();

        request_text = textTemplateEngine.process("/RequestBuilder", context);
        request_file = new FileWriter(outputPath + "/RequestBuilder.java");
        request_file.write(request_text);
        request_file.close();

        request_text = textTemplateEngine.process("/SendRequest", context);
        request_file = new FileWriter(outputPath + "/SendRequest.java");
        request_file.write(request_text);
        request_file.close();




//        //
//        Gson gson = new Gson();
//        HashMap<String, String> maps = gson.fromJson(String.valueOf(jsonObject), HashMap.class);
//        maps.forEach((k, v) -> context.setVariable(k, v));
//
//
//        String request_text = textTemplateEngine.process("YubiRequest", context);
//        FileWriter request_file = new FileWriter(outputPath + "/YubiRequest.java");
//        request_file.write(request_text);
//        request_file.close();
//
//        String response_text = textTemplateEngine.process("YubiResponse", context);
//        FileWriter response_file = new FileWriter(outputPath + "/YubiResponse.java");
//        response_file.write(response_text);
//        response_file.close();




        return stub1.func1(request1.newBuilder().setReq(outputfolder).build()).getRes();

    }



    private void check(String api_name, String key1, String key2, JsonElement jsonElement) {


        String key = "api_name",value =api_name;// "this.getClass().getPackage()";
        if (jsonElement.isJsonArray()) {
            for (JsonElement jsonElement1 : jsonElement.getAsJsonArray()) {
                check(api_name,key1,key2, jsonElement1);
            }
        } else {
            if (jsonElement.isJsonObject()) {
                Set<Map.Entry<String, JsonElement>> entrySet = jsonElement
                        .getAsJsonObject().entrySet();
                for (Map.Entry<String, JsonElement> entry : entrySet) {
                    String key6 = entry.getKey();
                    if (key6.equals(key2)) {
//                        list2.add(entry.getValue().toString());
                        value = entry.getValue().getAsString();
                    }
                    if (key6.equals(key1)) {
//                        list1.add(entry.getValue().toString());
                        key = entry.getValue().getAsString();

                    }

                    maps.put(key, value);
                    check(api_name,key1, key2, entry.getValue());

                }
            }
        }

    }

}


