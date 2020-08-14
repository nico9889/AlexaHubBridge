package Bridge;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;

import static Utils.Utils.macFormat;

public class MulticastReceiver extends Thread{
    private final MulticastSocket socket;
    final NetworkInterface nif;
    private final ArrayList<InetAddress> addresses = new ArrayList<>();
    final InetAddress address;
    private final InetAddress group_address = InetAddress.getByName("239.255.255.250");
    final int port = 1900;
    private boolean running = true;

    MulticastReceiver() throws IOException {
        Scanner input = new Scanner(System.in);
        Enumeration<NetworkInterface> enumNif = NetworkInterface.getNetworkInterfaces();
        while(enumNif.hasMoreElements()){
            NetworkInterface actNif = enumNif.nextElement();
            System.out.println(actNif.getIndex() + " - " + actNif.getDisplayName());
        }
        this.nif = NetworkInterface.getByIndex(input.nextInt());
        Enumeration<InetAddress> enumInets = nif.getInetAddresses();
        int i = 0;
        while(enumInets.hasMoreElements()){
            InetAddress inet = enumInets.nextElement();
            addresses.add(inet);
            System.out.println(i++ + " - " + inet);
        }
        this.address = InetAddress.getByName(addresses.get(input.nextInt()).getHostAddress());
        this.socket = new MulticastSocket(port);
        socket.joinGroup(group_address);
        socket.setNetworkInterface(nif);
    }

    public void run(){
        System.out.println("Multicast Listening:");
        byte[] b = new byte[1024];
        DatagramPacket dgram = new DatagramPacket(b, b.length);
        try {
            String mac = macFormat(nif.getHardwareAddress());
            while (running) {
                socket.receive(dgram);
                String data  = new String(b, 0, dgram.getLength());
                if((data.contains("upnp:rootdevice") || data.contains("asic:1") || data.contains("ssdp:all"))){
                    System.out.println("SSDP: " + dgram.getAddress());
                    /*
                    System.out.println(dgram.getPort());
                    System.out.println(data);
                    System.out.println("Answering multicast");
                    */
                    String response = String.format("HTTP/1.1 200 OK\r\n" +
                            "EXT:\r\n" +
                            "CACHE-CONTROL: max-age=100\r\n" + // SSDP_INTERVAL
                            "LOCATION: http://%s:80/description.xml\r\n" +
                            "SERVER: FreeRTOS/6.0.5, UPnP/1.0, IpBridge/1.17.0\r\n" +
                            "hue-bridgeid: %s\r\n" +
                            "ST: urn:schemas-upnp-org:device:basic:1\r\n" +
                            "USN: uuid:2f402f80-da50-11e1-9b23-%s::ssdp:all\r\n" +
                            "\r\n", address, mac, mac);
                    // System.out.println(response);
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
