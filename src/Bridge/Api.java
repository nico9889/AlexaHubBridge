package Bridge;

import Devices.Device;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;

import static Bridge.Bridge.decodeLightId;
import static Bridge.Bridge.encodeLightId;

class Api implements HttpHandler {
    private final ArrayList<Device> devices;

    public Api(ArrayList<Device> devices){
        this.devices=devices;
    }

    @Override
    public void handle(HttpExchange httpExchange){
        // System.out.println(httpExchange.getRequestURI());
        String request;
        JSONObject json = null;
        try {
            request = new String(httpExchange.getRequestBody().readAllBytes());
            json = new JSONObject(request);
            // System.out.println("Request body: " + json);
        }catch(Exception e) {
            // System.out.println("Request body: missing body");
        }
        try {
            handleAlexa(httpExchange, json);
        }catch(IOException io){
            // System.out.println("Error when handling request");
        }

    }

    private void handleAlexa(HttpExchange httpExchange, JSONObject json) throws IOException {
        Headers headers = httpExchange.getResponseHeaders();
        headers.set("Content-Type", String.format("application/json; charset=%s", "UTF-8"));
        URI uri = httpExchange.getRequestURI();
        String path = uri.getRawPath();
        String body = null;
        OutputStream response = httpExchange.getResponseBody();
        if(json!=null) {
            if (json.has("devicetype")) {
                // System.out.println("HTTP: Device Type");
                try {
                    // return a static username for the API call
                    body = "[{\"success\":{\"username\":\"2WLEDHardQrI3WHYTHoMcXHgEspsM8ZZRpSKtBQr\"}}]";
                    httpExchange.sendResponseHeaders(200, body.getBytes().length);
                    response.write(body.getBytes());
                }catch(Exception e){
                    e.printStackTrace();
                }finally{
                    response.close();
                }
            }
            else if (path.indexOf("state") > 0) {
                // System.out.println("HTTP: State");
                body = "[{\"success\":{\"/lights/1/state/\": true}}]";
                int devId;
                // We get the ID from the path
                if (path.length() > path.indexOf("lights") + 7)
                    devId = Integer.parseInt(path.substring(path.indexOf("lights") + 7, path.indexOf("lights")+15));
                else
                    return; // Invalid request, we stop here
                // Decode ID. We use that as ArrayList index (IDs starts from 1, need to shift them)
                devId = decodeLightId(devId)-1;
                // Invalid ID. Stop here
                if (devId >= devices.size()) return;
                devices.get(devId).setPropertyChanged(0);

                // Turn off check
                if (json.has("on") && !((boolean) json.get("on"))) {
                    devices.get(devId).setValue(0);
                    devices.get(devId).setPropertyChanged(2);
                // If the devices has been turned off, we skip to the end, otherwise we check all the other state change
                }else{
                    // Turn on check
                    if (json.has("on") && ((boolean) json.get("on"))) {
                        devices.get(devId).setValue(devices.get(devId).getLastValue());
                        devices.get(devId).setPropertyChanged(1);
                    }
                    // Brightness check
                    if (json.has("bri")) {
                        int bri = (int) json.get("bri");
                        if (bri == 255) {
                            devices.get(devId).setValue(255);
                        } else {
                            devices.get(devId).setValue(bri + 1);
                        }
                        devices.get(devId).setPropertyChanged(3);
                    }
                    // XY value check
                    if (json.has("xy")){
                        JSONArray xy = json.getJSONArray("xy");
                        double x = (double)xy.get(0);
                        double y = (double)xy.get(1);
                        devices.get(devId).setColorXY(x, y);
                        devices.get(devId).setPropertyChanged(6);
                    }
                    // Hue-saturation check
                    if (json.has("hue")) {
                        int hue = (int)json.get("hue");
                        int sat = (int)json.get("sat");
                        devices.get(devId).setColorHue(hue, sat);
                        devices.get(devId).setPropertyChanged(4);
                    }
                    // Color temperature check
                    if (json.has("ct")) {
                        int ct = (int)json.get("ct");
                        devices.get(devId).setColorTemperature(ct);
                        devices.get(devId).setPropertyChanged(5);
                    }
                }
                // User function callback
                devices.get(devId).callback();
                try {
                    // Send "success" in response
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
            // System.out.println("HTTP: Lights");
            // If the requests contains a light id, we parse it, otherwise it's zero
            int devId = (path.length()>pos+7) ? Integer.parseInt(path.substring(pos + 7)):0;

            // (id==0) Return all the virtual devices in a list
            if (devId == 0) {
                StringBuilder jsonTemp = new StringBuilder("{");
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
            // (id!=0) Return the requested virtual device status
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
        // System.out.println(body);
    }
}
