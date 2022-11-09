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
import org.springframework.web.client.RestTemplate;
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
    static HashMap<String, String> isArrayMaps = new HashMap<>();

    final String key2 = "originatorFieldName";
    final String key1 = "dataFieldName";

    public String generateCodeFiles(String mappingfile,String outputfolder, String outputPath) throws IOException {

        final Context context = new Context();

        RestTemplate restTemplate = new RestTemplate();
        List<String> apiNamesWithResponse = new ArrayList<String>();
        List<String> apiNamesWithoutResponse = new ArrayList<String>();

        JSONObject jsonObject = new JSONObject(mappingfile);


        JSONArray apiList = jsonObject.getJSONArray("consolidatedAPIs");

        JSONArray webhookList = jsonObject.getJSONArray("consolidatedWebhooks");

        apiList.putAll(webhookList);




        JsonParser p = new JsonParser();


        apiList.forEach(api -> {
            try {
                JSONObject jsonObj = new JSONObject(api.toString());
                System.out.println("jsonObj------->" + jsonObj);

                String api_name = "";

                if (jsonObj.has("api_name")) {
                    api_name = (String) jsonObj.get("api_name");
                } else if (jsonObj.has("webhook_name")) {
                    api_name = (String) jsonObj.get("webhook_name");
                }
                System.out.println("api_name-------->" + api_name);
                if(!(jsonObj.has("response_payload_attributes")))
                    apiNamesWithoutResponse.add(api_name);
                else if ((jsonObj.get("response_payload_attributes")).toString().equals("{}"))
                    apiNamesWithoutResponse.add(api_name);
                else
                    apiNamesWithResponse.add(api_name);


                maps = new HashMap<>();
                maps.put("api_name",api_name);
                JSONObject jsonBlock = jsonObj.getJSONObject("request_payload_attributes");
                Iterator<String> keys = jsonBlock.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    System.out.println(key + "++++++++++");
                    System.out.println(jsonBlock.get(key));
                    JSONObject jj = (JSONObject) jsonBlock.get(key);
                    System.out.println((jj.get("isArray")).equals(true));
                    if (!(key.equals("metadata"))) {
                        maps.put(key, key);
                        isArrayMaps = new HashMap<>();
                        check(api_name, true, p.parse(jj.toString()));
                        final Context context2 = new Context();
                        isArrayMaps.forEach((k, v) -> context2.setVariable(k, v));
                        String request_text1 = textTemplateEngine.process("/" + api_name + "/"+key, context2);
                        FileWriter request_file1 = new FileWriter(outputPath + "/" + api_name + key+".java");
                        request_file1.write(request_text1);
                        request_file1.close();
                    } else {
                        check(api_name, false, p.parse(jj.toString()));
                    }
                }
                if(apiNamesWithResponse.contains(api_name))
                    check(api_name,false,p.parse((jsonObj.getJSONObject("response_payload_attributes")).toString()));

                System.out.println(maps);

                System.out.println("*********\n");

                maps.forEach((k, v) -> context.setVariable(k, v));

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

    private void check(String api_name,Boolean isArray, JsonElement jsonElement) {

        String key = "",value ="";
        if (jsonElement.isJsonArray()) {
            for (JsonElement jsonElement1 : jsonElement.getAsJsonArray()) {
                check(api_name,isArray, jsonElement1);
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

                    if(isArray)
                        isArrayMaps.put(key, value);
                    else
                        maps.put(key, value);

                    check(api_name,isArray,entry.getValue());

                }
            }
        }

    }

}
