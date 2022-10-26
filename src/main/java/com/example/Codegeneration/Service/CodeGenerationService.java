package com.example.Codegeneration.Service;


import com.example.sample.request1;
import com.example.sample.sample1Grpc;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


@Service
public class CodeGenerationService {

    @GrpcClient("sample1")
    public sample1Grpc.sample1BlockingStub stub1;

    @Autowired
    private TemplateEngine textTemplateEngine;

    static HashMap<String, String> maps = new HashMap<>();

    public String generateCodeFiles(String mappingfile,String outputfolder, String outputPath) throws IOException {

        final Context context = new Context();

        RestTemplate restTemplate = new RestTemplate();
        List<String> apinames = new ArrayList<String>();

        JSONObject jsonObject = new JSONObject(mappingfile);


        JSONArray apiList = jsonObject.getJSONArray("consolidatedAPIs");

        JSONArray webhookList = jsonObject.getJSONArray("consolidatedWebhooks");

        apiList.putAll(webhookList);



        String key2 = "originatorFieldName";
        String key1 = "yubiFieldName";
        JsonParser p = new JsonParser();


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

        final Context context1 = new Context();
        context1.setVariable("apinames", apinames);

        String loan_text = textTemplateEngine.process("/loan", context1);
        FileWriter loan_file = new FileWriter(outputPath + "/Loan.java");
        loan_file.write(loan_text);
        loan_file.close();

        String requestbuilder_text = textTemplateEngine.process("/RequestBuilder", context1);
        FileWriter requestbuilder_file = new FileWriter(outputPath + "/RequestBuilder.java");
        requestbuilder_file.write(requestbuilder_text);
        requestbuilder_file.close();

        String sendrequest_text = textTemplateEngine.process("/SendRequest", context1);
        FileWriter sendrequest_file = new FileWriter(outputPath + "/SendRequest.java");
        sendrequest_file.write(sendrequest_text);
        sendrequest_file.close();



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
