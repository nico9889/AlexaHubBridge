import Bridge.Bridge;
import Devices.Type;

public class Test {
    public static void main(String[] args) throws Exception{
        Bridge b = Bridge.createWithSelectNif(80);

        // You shouldn't try... trust me...
        for(int i=0;i<256;i++)
            b.addDevice("Light#"+i, (dev) -> System.out.println(dev.name + " " + dev.getColor()), Type.ExtendedColor);
        b.start();
    }
}
