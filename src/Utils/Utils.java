package Utils;

public class Utils {
    public static String macFormat(byte[] mac){
        StringBuilder sb = new StringBuilder(18);
        for (byte b : mac) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
