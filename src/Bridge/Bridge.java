package Bridge;

import Devices.Callback;
import Devices.Device;
import Devices.Type;
import Utils.TooManyDevicesException;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;

import static Utils.Utils.macFormat;

public class Bridge {
    private final HttpServer httpServer;
    private final MulticastReceiver mri;
    private static byte[] mac;
    private final ArrayList<Device> devices = new ArrayList<>();

    public Bridge(InetAddress inet, NetworkInterface nif, int port) throws IOException {
        mri = new MulticastReceiver(inet, nif);
        httpServer = HttpServer.create();
        InetSocketAddress addr = new InetSocketAddress(mri.address, port);
        httpServer.bind(addr, 0);
        httpServer.createContext("/description.xml", new Description(addr.getAddress(), nif));
        httpServer.createContext("/api", new Api(devices));
        mac = nif.getHardwareAddress();
    }

    public Bridge (int port) throws IOException{
        this(InetAddress.getLocalHost(), NetworkInterface.getByInetAddress(InetAddress.getLocalHost()), port);
    }

    public static Bridge createWithSelectNif(int port) throws IOException{
        Scanner input = new Scanner(System.in);
        int i = 0;
        ArrayList<InetAddress> addresses = new ArrayList<>();
        Enumeration<NetworkInterface> enumNif = NetworkInterface.getNetworkInterfaces();
        while(enumNif.hasMoreElements()){
            NetworkInterface actNif = enumNif.nextElement();
            System.out.println(actNif.getIndex() + " - " + actNif.getDisplayName());
        }
        NetworkInterface nif = NetworkInterface.getByIndex(input.nextInt());
        Enumeration<InetAddress> enumInets = nif.getInetAddresses();

        while(enumInets.hasMoreElements()){
            InetAddress inet = enumInets.nextElement();
            addresses.add(inet);
            System.out.println(i++ + " - " + inet);
        }
        InetAddress address = InetAddress.getByName(addresses.get(input.nextInt()).getHostAddress());
        return new Bridge(address, nif, port);
    }

    public void stopSSDPHandler(){
        mri.terminate();
    }

    // FIXME: bad design
    public static int encodeLightId(int id){
        String mac = macFormat(Bridge.mac);
        int fixed0 = Integer.parseInt(mac.substring(8,9),16);
        int fixed1 = Integer.parseInt(mac.substring(10,11),16);
        // (MAC fourth couple) | (MAC fifth couple) | (light id) | (id truncated to fill)
        return (fixed0 << 20) | (fixed1 << 12) | ((id & 0xFF) << 4) | (id & 0xF);
    }

    public static int decodeLightId(int id){
        return id>>4 & (0xFF);
    }

    public void start(){
        mri.start();
        httpServer.start();
    }


    public Device addDevice(String name, Callback callback, Type type) throws Exception{
        Device d = new Device(name, callback, type);
        if(d.getCount()<=256) {
            devices.add(d);
            return d;
        }else{
            throw new TooManyDevicesException("Too many devices! (>256)");
        }
    }
}
