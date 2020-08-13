import Bridge.Bridge;
import Devices.Type;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException{
        Bridge b = new Bridge(80);

        b.addDevice("Test0", () -> {}, Type.Dimmable);
        b.addDevice("Test1", () -> System.out.println("Hello world"), Type.ExtendedColor);

        b.start();
    }
}
