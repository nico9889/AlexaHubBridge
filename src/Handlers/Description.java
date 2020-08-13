package Handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;

import static Utils.Utils.macFormat;

public class Description implements HttpHandler {
    public final InetAddress address;
    public final NetworkInterface nif;

    public Description(InetAddress address, NetworkInterface nif){
        this.address = address;
        this.nif = nif;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        System.out.println("Handling description.xml");
        if(httpExchange.getRequestMethod().equals("GET")) {
            String ip = address.getHostAddress();
            String mac = macFormat(nif.getHardwareAddress());
            String description = String.format("<?xml version=\"1.0\" ?>\n" +
                    "<root xmlns=\"urn:schemas-upnp-org:device-1-0\">\n" +
                    "<specVersion><major>1</major><minor>0</minor></specVersion>\n " +
                    "<URLBase>http://%s:80/</URLBase>\n" +
                    "<device>\n" +
                    "<deviceType>urn:schemas-upnp-org:device:Basic:1</deviceType>\n" +
                    "<friendlyName>Espalexa (%s)</friendlyName>\n" +
                    "<manufacturer>Royal Philips Electronics</manufacturer>\n" +
                    "<manufacturerURL>http://www.philips.com</manufacturerURL>\n" +
                    "<modelDescription>Philips hue Personal Wireless Lighting</modelDescription>\n" +
                    "<modelName>Philips hue bridge 2012</modelName>\n" +
                    "<modelNumber>929000226503</modelNumber>\n" +
                    "<modelURL>http://www.meethue.com</modelURL>\n" +
                    "<serialNumber>%s</serialNumber>\n " +
                    "<UDN>uuid:2f402f80-da50-11e1-9b23-%s</UDN>\n" +
                    "<presentationURL>index.html</presentationURL>\n" +
                    "</device>\n" +
                    "</root>", ip, ip, mac, mac);
            httpExchange.sendResponseHeaders(200, description.getBytes().length);
            OutputStream response = httpExchange.getResponseBody();
            response.write(description.getBytes());
        }
        httpExchange.sendResponseHeaders(501,0);
    }
}
