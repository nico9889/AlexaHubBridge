package Handlers;

import Devices.Device;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import static Bridge.Bridge.decodeLightId;
import static Bridge.Bridge.encodeLightId;

public class Api implements HttpHandler {
    private final ArrayList<Device> devices;

    public Api(ArrayList<Device> devices){
        this.devices=devices;
    }

    @Override
    public void handle(HttpExchange httpExchange){
        System.out.println(httpExchange.getRequestURI());
        String request;
        JSONObject json = null;
        try {
            request = new String(httpExchange.getRequestBody().readAllBytes());
            json = new JSONObject(request);
            System.out.println("Body: " + json);
        }catch(Exception e) {
            System.out.println("Missing body");
        }
        try {
            handleAlexa(httpExchange, json);
        }catch(IOException io){
            System.out.println("Error when handling request");
        }

    }

    private void handleAlexa(HttpExchange httpExchange, JSONObject json) throws IOException {
        Headers headers = httpExchange.getResponseHeaders();
        headers.set("Content-Type", String.format("application/json; charset=%s", "UTF-8"));
        URI uri = httpExchange.getRequestURI();
        String path = uri.getRawPath();
        String body = "";
        OutputStream response = httpExchange.getResponseBody();
        if(json!=null) {
            if (json.has("devicetype")) {
                System.out.println("HTTP: Device Type");
                try {
                    body = "[{\"success\":{\"username\":\"2WLEDHardQrI3WHYTHoMcXHgEspsM8ZZRpSKtBQr\"}}]";
                    httpExchange.sendResponseHeaders(200, body.getBytes().length);
                    response.write(body.getBytes());
                }catch(Exception e){
                    e.printStackTrace();
                }finally{
                    response.close();
                }
            }
            if (path.indexOf("state") > 0) {
                System.out.println("HTTP: State");
                body = "[{\"success\":{\"/lights/1/state/\": true}}]";
                int devId;
                if (path.length() > path.indexOf("lights") + 7)
                    devId = Integer.parseInt(path.substring(path.indexOf("lights") + 7, path.indexOf("lights")+15));
                else
                    devId = 0;
                devId = decodeLightId(devId);
                devId--;
                if (devId >= devices.size()) return;
                devices.get(devId).setPropertyChanged(0);
                if (json.has("on") && !((boolean) json.get("on"))) {
                    devices.get(devId).setValue(0);
                    devices.get(devId).setPropertyChanged(2);
                    devices.get(devId).callback();
                }
                if (json.has("on") && ((boolean) json.get("on"))) {
                    devices.get(devId).setValue(devices.get(devId).getLastValue());
                    devices.get(devId).setPropertyChanged(1);
                }
                if (json.has("bri")) {
                    int bri = (int) json.get("bri");
                    if (bri == 255) {
                        devices.get(devId).setValue(255);
                    } else {
                        devices.get(devId).setValue(bri + 1);
                    }
                    System.out.println("Bri: " + bri);
                    devices.get(devId).setPropertyChanged(3);
                }
                if (json.has("xy")){
                    JSONArray xy = json.getJSONArray("xy");
                    double x = (double)xy.get(0);
                    double y = (double)xy.get(1);
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
                try {
                    httpExchange.sendResponseHeaders(200, body.getBytes().length);
                    response.write(body.getBytes());
                }catch(Exception e){
                    e.printStackTrace();
                }finally {
                    response.close();
                }
            }
        }

        int pos = path.indexOf("lights");
        if (pos > 0){
            System.out.println("HTTP: Lights");
            int devId = (path.length()>pos+7) ? Integer.parseInt(path.substring(pos + 7)):0;
            StringBuilder jsonTemp = new StringBuilder("{");

            if (devId == 0) {
                for (int i = 0; i < devices.size(); i++) {
                    jsonTemp.append("\"").append(encodeLightId(i + 1)).append("\":");
                    jsonTemp.append(devices.get(i).toJSON());
                    if (i < devices.size() - 1) jsonTemp.append(",");
                }
                jsonTemp.append("}");
                try{
                    httpExchange.sendResponseHeaders(200, jsonTemp.toString().getBytes().length);
                    response.write(jsonTemp.toString().getBytes());
                }catch(Exception e){
                    e.printStackTrace();
                }finally {
                    response.close();
                }
            }else{
                devId = decodeLightId(devId)-1;
                if(devId > devices.size())
                    body = "{}";
                else
                    body = devices.get(devId).toJSON();
                try {
                    httpExchange.sendResponseHeaders(200, body.getBytes().length);
                    response.write(body.getBytes());
                }catch(Exception e){
                    e.printStackTrace();
                }
                finally{
                    response.close();
                }
            }
        }
        System.out.println(body);
    }
}
