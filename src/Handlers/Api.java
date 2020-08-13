package Handlers;

import Devices.Device;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;

import org.json.JSONObject;

import static Bridge.Bridge.decodeLightId;
import static Bridge.Bridge.encodeLightId;

public class Api implements HttpHandler {
    public final ArrayList<Device> devices;

    public Api(ArrayList<Device> devices){
        this.devices=devices;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException{
        System.out.println("Handling /api");
        System.out.println(httpExchange.getRequestURI());
        String request = null;
        JSONObject json = null;
        try {
            request = new String(httpExchange.getRequestBody().readAllBytes());
            json = new JSONObject(request);
        }catch(Exception e){
            System.out.println("Json non presente");
        }
        System.out.println("Request: " + request);
        System.out.println("JSON: " + json);
        handleAlexa(httpExchange, json);
    }

    public void handleAlexa(HttpExchange httpExchange, JSONObject json) throws IOException{
        Headers headers = httpExchange.getResponseHeaders();
        headers.set("Content-Type", String.format("application/json; charset=%s", "UTF-8"));
        URI uri = httpExchange.getRequestURI();
        String path = uri.getRawPath();
        System.out.println("Path:" + path);
        if(json!=null) {
            if (json.has("devicetype")) {
                try {
                    String body = "[{\"success\":{\"username\":\"2WLEDHardQrI3WHYTHoMcXHgEspsM8ZZRpSKtBQr\"}}]";
                    System.out.println(body);
                    httpExchange.sendResponseHeaders(200, body.getBytes().length);
                    OutputStream response = httpExchange.getResponseBody();
                    response.write(body.getBytes());
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            if (path.indexOf("state") > 0) {
                System.out.println("State answer");
                try{
                    String body = "[{\"success\":true}]";
                    int devId;
                    if (path.length() > path.indexOf("lights") + 7)
                        devId = Integer.parseInt(path.substring(path.indexOf("lights") + 7, path.indexOf("lights")+15));
                    else
                        devId = 0;
                    devId = decodeLightId(devId);
                    devId--;
                    if (devId >= devices.size() || devId < 0) return;
                    devices.get(devId).setPropertyChanged(0);
                    if (json.has("on") && !((boolean) json.get("on"))) { //OFF command
                        devices.get(devId).setValue(0);
                        devices.get(devId).setPropertyChanged(2);
                        devices.get(devId).callback();
                    }
                    if (json.has("on") && ((boolean) json.get("on"))) {
                        devices.get(devId).setValue(devices.get(devId).getLastValue());
                        devices.get(devId).setPropertyChanged(1);
                    }
                    if (json.has("bri")) {
                        int briL = (int) json.get("bri");
                        if (briL == 255) {
                            devices.get(devId).setValue(255);
                        } else {
                            devices.get(devId).setValue(briL + 1);
                        }
                        devices.get(devId).setPropertyChanged(3);
                    }
                    if (json.has("xy")){
                        double x = 0;
                        double y = 0;
                        devices.get(devId).setColorXY(x, y);
                        devices.get(devId).setPropertyChanged(6);
                    }
                    if (json.has("hue")) {
                        int hue = (int)json.get("hue");
                        int sat = (int)json.get("sat");
                        devices.get(devId).setColorHue(hue, sat);
                        devices.get(devId).setPropertyChanged(4);
                    }
                    if (json.has("ct")) {
                        int ct = (int)json.get("ct");
                        devices.get(devId).setColorTemperature(ct);
                        devices.get(devId).setPropertyChanged(5);
                    }
                    devices.get(devId).callback();
                    System.out.println("After callback");
                    httpExchange.sendResponseHeaders(200, body.getBytes().length);
                    OutputStream response = httpExchange.getResponseBody();
                    response.write(body.getBytes());
                }catch(Exception e){
                    e.printStackTrace();
                }
                System.out.println("Terminato cambio stato");
            }
        }

        int pos = path.indexOf("lights");
        if (pos > 0){
            System.out.println("Hadling lights");
            OutputStream response = httpExchange.getResponseBody();
            int devId = (path.length()>pos+7) ? Integer.parseInt(path.substring(pos + 7)):0;
            StringBuilder jsonTemp = new StringBuilder("{");

            if (devId == 0) {
                for (int i = 0; i < devices.size(); i++) {
                    jsonTemp.append("\"").append(encodeLightId(i + 1)).append("\":");
                    jsonTemp.append(devices.get(i).toJSON(i));
                    if (i < devices.size() - 1) jsonTemp.append(",");
                }
                jsonTemp.append("}");
                System.out.println("Light response: " + new JSONObject(jsonTemp));
                httpExchange.sendResponseHeaders(200, jsonTemp.toString().getBytes().length);
                response.write(jsonTemp.toString().getBytes());
            }else{
                devId = decodeLightId(devId)-1;
                System.out.println(devId);
                if(devId < devices.size()){
                    System.out.println("Getting devices" + devId);
                    String body = devices.get(devId).toJSON(devId);
                    System.out.println("Body luce con ID: " + body);
                    httpExchange.sendResponseHeaders(200, body.getBytes().length);
                    response.write(body.getBytes());
                }
            }
        }
    }
}
