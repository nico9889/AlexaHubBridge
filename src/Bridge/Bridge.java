package Bridge;

import Devices.Callback;
import Devices.Device;
import Devices.Type;
import Handlers.Api;
import Handlers.Description;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import static Utils.Utils.macFormat;

public class Bridge {
    private final HttpServer httpServer;
    private final MulticastReceiver mri;
    private static byte[] mac;
    private final ArrayList<Device> devices = new ArrayList<>();

    public Bridge(int port) throws IOException {
        mri = new MulticastReceiver();
        httpServer = HttpServer.create();
        InetSocketAddress addr = new InetSocketAddress(mri.address, port);
        httpServer.bind(addr, 0);
        httpServer.createContext("/description.xml", new Description(addr.getAddress(), mri.nif));
        httpServer.createContext("/api", new Api(devices));
        mac = mri.nif.getHardwareAddress();
    }

    // FIXME: bad design
    public static int encodeLightId(int id){
        String mac = macFormat(Bridge.mac);
        int[] sub_mac = new int[6];
        for(int i = 0; i<mac.length(); i+=2){
            sub_mac[i/2] = Integer.parseInt(mac.substring(i, i+1), 16);
        }
        return (sub_mac[3] << 20) | (sub_mac[4] << 12) | (sub_mac[5] << 4) | (id & 0xF);
    }

    public static int decodeLightId(int id){
        return (id & 0xF);
    }

    public void start(){
        mri.start();
        httpServer.start();
    }


    public Device addDevice(String name, Callback callback, Type type){
        Device d = new Device(name, callback, type);
        devices.add(d);
        return d;
    }
}
