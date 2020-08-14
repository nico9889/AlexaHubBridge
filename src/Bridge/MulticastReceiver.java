package Bridge;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.ArrayList;

import static Utils.Utils.macFormat;

public class MulticastReceiver extends Thread{
    private final MulticastSocket socket;
    private final ArrayList<InetAddress> addresses = new ArrayList<>();
    final InetAddress address;
    private final InetAddress group_address = InetAddress.getByName("239.255.255.250");
    final int port = 1900;
    private final String mac;
    private boolean running = true;

    MulticastReceiver(InetAddress address, NetworkInterface nif) throws IOException {
        this.socket = new MulticastSocket(port);
        this.address = address;
        this.mac = macFormat(nif.getHardwareAddress());
        socket.joinGroup(group_address);
        socket.setNetworkInterface(nif);
    }

    public void run(){
        System.out.println("Multicast Listening:");
        byte[] b = new byte[1024];
        DatagramPacket dgram = new DatagramPacket(b, b.length);
        try {
            while (running) {
                socket.receive(dgram);
                String data  = new String(b, 0, dgram.getLength());
                if((data.contains("upnp:rootdevice") || data.contains("asic:1") || data.contains("ssdp:all"))){
                    System.out.println("SSDP: " + dgram.getAddress());
                    String response = String.format("HTTP/1.1 200 OK\r\n" +
                            "EXT:\r\n" +
                            "CACHE-CONTROL: max-age=100\r\n" + // SSDP_INTERVAL
                            "LOCATION: http://%s:80/description.xml\r\n" +
                            "SERVER: FreeRTOS/6.0.5, UPnP/1.0, IpBridge/1.17.0\r\n" +
                            "hue-bridgeid: %s\r\n" +
                            "ST: urn:schemas-upnp-org:device:basic:1\r\n" +
                            "USN: uuid:2f402f80-da50-11e1-9b23-%s::ssdp:all\r\n" +
                            "\r\n", address, mac, mac);
                    DatagramPacket packet = new DatagramPacket(response.getBytes(), response.getBytes().length, dgram.getAddress(), dgram.getPort());
                    socket.send(packet);
                }
                dgram.setLength(b.length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            socket.close();
        }
    }
}
