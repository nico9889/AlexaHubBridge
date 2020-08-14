import Bridge.Bridge;
import Devices.Type;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException{
        Bridge b = new Bridge(80);

        b.addDevice("Luce1", (dev) -> System.out.println(dev.name + " " + dev.getColor()), Type.ExtendedColor);

        b.start();
    }
}
