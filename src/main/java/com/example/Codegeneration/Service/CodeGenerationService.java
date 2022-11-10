package com.example.Codegeneration.Service;


import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.yubi.oss.DCG.SDKServiceGrpc;
import com.yubi.oss.DCG.request1;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


@Service
public class CodeGenerationService {

    @GrpcClient("SDKService")
    public SDKServiceGrpc.SDKServiceBlockingStub SDKServiceStub;

    @Autowired
    private TemplateEngine textTemplateEngine;

    static HashMap<String, String> maps = new HashMap<>();
    static HashMap<String, String> metaMaps = new HashMap<>();

    final String key2 = "originatorFieldName";
    final String key1 = "dataFieldName";

    public String generateCodeFiles(String mappingfile,String outputfolder, String outputPath) throws IOException {

        final Context context = new Context();

        List<String> apiNamesWithResponse = new ArrayList<String>();
        List<String> apiNamesWithoutResponse = new ArrayList<String>();

        JSONObject jsonObject = new JSONObject(mappingfile);

        JSONArray apiList = jsonObject.getJSONArray("consolidatedAPIs");

        JSONArray webhookList = jsonObject.getJSONArray("consolidatedWebhooks");

        apiList.putAll(webhookList);


        JsonParser parser = new JsonParser();


        apiList.forEach(api -> {
            try {
                JSONObject jsonApiObj = new JSONObject(api.toString());

                String api_name = "";

                if (jsonApiObj.has("api_name")) {
                    api_name = (String) jsonApiObj.get("api_name");
                } else if (jsonApiObj.has("webhook_name")) {
                    api_name = (String) jsonApiObj.get("webhook_name");
                }
                System.out.println("api_name --------> " + api_name);
                if(!(jsonApiObj.has("response_payload_attributes")))
                    apiNamesWithoutResponse.add(api_name);
                else if ((jsonApiObj.get("response_payload_attributes")).toString().equals("{}"))
                    apiNamesWithoutResponse.add(api_name);
                else
                    apiNamesWithResponse.add(api_name);


                metaMaps = new HashMap<>();
                metaMaps.put("api_name",api_name);
                JSONObject requestBlock = jsonApiObj.getJSONObject("request_payload_attributes");
                Iterator<String> keys = requestBlock.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    System.out.println("key ---> "+key);
                    System.out.println("block -----> "+requestBlock.get(key));
                    JSONObject block = (JSONObject) requestBlock.get(key);
                    if (!(key.equals("metadata"))) {
                        metaMaps.put(key, key);
                        maps = new HashMap<>();
                        check(true, parser.parse(block.toString()));
                        final Context context2 = new Context();
                        maps.forEach((k, v) -> context2.setVariable(k, v));
                        String request_text1 = textTemplateEngine.process("/" + api_name + "/"+key, context2);
                        FileWriter request_file1 = new FileWriter(outputPath + "/" + api_name + key+".java");
                        request_file1.write(request_text1);
                        request_file1.close();
                    } else {
                        check(false, parser.parse(block.toString()));
                    }
                }
                if(apiNamesWithResponse.contains(api_name))
                    check(false,parser.parse((jsonApiObj.getJSONObject("response_payload_attributes")).toString()));

                System.out.println(metaMaps);


                metaMaps.forEach((k, v) -> context.setVariable(k, v));

                String request_text = textTemplateEngine.process("/"+api_name+"/YubiRequest", context);
                FileWriter request_file = new FileWriter(outputPath + "/"+api_name+"Request.java");
                request_file.write(request_text);
                request_file.close();

                if(apiNamesWithResponse.contains(api_name)) {
                    String response_text = textTemplateEngine.process("/" + api_name + "/YubiResponse", context);
                    FileWriter response_file = new FileWriter(outputPath + "/" + api_name + "Response.java");
                    response_file.write(response_text);
                    response_file.close();
                }


                System.out.println("\n\n");
            } catch (IOException e){
                System.out.println(e);
            }
        });

        final Context context1 = new Context();
        context1.setVariable("apiNamesWithResponses", apiNamesWithResponse);
        context1.setVariable("apiNamesWithoutResponses", apiNamesWithoutResponse);

        String loan_text = textTemplateEngine.process("/YUBILoan", context1);
        FileWriter loan_file = new FileWriter(outputPath + "/YUBILoan.java");
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

        return SDKServiceStub.getDownloadLink(request1.newBuilder().setFolderLocation(outputfolder).build()).getDownloadLink();
    }

    private void check(Boolean block, JsonElement jsonElement) {

        String key = "",value ="";
        if (jsonElement.isJsonArray()) {
            for (JsonElement jsonElement1 : jsonElement.getAsJsonArray()) {
                check(block, jsonElement1);
            }
        } else {
            if (jsonElement.isJsonObject()) {
                Set<Map.Entry<String, JsonElement>> entrySet = jsonElement.getAsJsonObject().entrySet();
                for (Map.Entry<String, JsonElement> entry : entrySet) {
                    String key6 = entry.getKey();
                    if (key6.equals(key2)) {
                        value = entry.getValue().getAsString();
                    }
                    if (key6.equals(key1)) {
                        key = entry.getValue().getAsString();
                    }

                    if(block)
                        maps.put(key, value);
                    else
                        metaMaps.put(key, value);

                    check(block,entry.getValue());
                }
            }
        }

    }

}
