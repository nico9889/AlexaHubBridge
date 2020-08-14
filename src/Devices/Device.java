package Devices;

import Utils.Pair;
import org.jetbrains.annotations.NotNull;

import static Bridge.Bridge.encodeLightId;

public class Device {
    public final String name;
    private static int count=0;
    private final int id;
    private final Callback callback;
    private Mode mode;
    private double x=1.,y=1.;
    private int temperature = 500;
    private int hue, sat;
    private int value, lastvalue;
    @NotNull
    private RGB color;
    private final Type type;
    private int changed = 0;

    public Device(String name, Callback callback, Type type){
        this.name = name;
        this.type = type;
        this.id = ++count;
        this.callback = callback;
        this.mode = Mode.XY;
        color = new RGB(0,0,0);
    }

    private synchronized double rgbConvert(double value){
        if(value<0.0031308){
            return 12.92*value;
        }
        else{
            return (1.0+0.055) * Math.pow(value, (1.0 / 2.4)) - 0.055;
        }
    }

    private synchronized void setColor(){
        switch(mode){
            case Hue:
                double h = (double)(this.hue) / 65525.0;
                double s = (double)(this.sat) / 255.0;
                double i = Math.floor(h * 6);
                double f = h * 6.0 - i;
                double p = 255.0 * (1.0 - s);
                double q = 255.0 * (1.0 - f * s);
                double t = 255.0 * (1.0 - ( 1.0 - f) * s);
                int sw = (int)i % 6;
                switch (sw){
                    case 0:
                        color = new RGB(255, t, p);
                        break;
                    case 1:
                        color = new RGB(q, 255, p);
                        break;
                    case 2:
                        color = new RGB(p, 255, t);
                        break;
                    case 3:
                        color = new RGB(p, q, 255);
                        break;
                    case 4:
                        color = new RGB(t, p, 255);
                        break;
                    case 5:
                        color = new RGB(255, p , q);
                        break;
                }
                break;
            case Temperature:
                double temp = 10000.0/(double)this.temperature;
                color = new RGB(0,0,0);

                if (temp <= 66) {
                    color.r = 255;
                    color.g = temp;
                    color.g = 99.470802 * Math.log(color.g) - 161.119568;
                    if (temp <= 19)
                        color.b = 0;
                    else {
                        color.b = temp - 10;
                        color.b = 138.517731 * Math.log(color.b) - 305.044793;
                    }
                }
                else {
                    color.r = temp - 60.0;
                    color.r = 329.698727 * Math.pow(color.r, -0.13320476);
                    color.g = temp - 60.0;
                    color.g = 288.12217 * Math.pow(color.g, -0.07551485);
                    color.b = 255;
                }
                color.r = Math.max(Math.min(color.r, 255.1), 0.1);
                color.g = Math.max(Math.min(color.g, 255.1), 0.1);
                color.b = Math.max(Math.min(color.b, 255.1), 0.1);
                break;
            case XY:
                double Y = this.value;
                double X = (Y/this.y) * this.x;
                double Z = (Y / this.y) * (1 - this.x - this.y);
                color.r = X * 1.656492 - Y * 0.354851 - Z * 0.255038;
                color.g = -X * 0.707196 + Y * 1.655397 + Z * 0.036152;
                color.b = X * 0.051713 - Y * 0.121364 + Z * 1.011530;

                color.r = rgbConvert(color.r);
                color.g = rgbConvert(color.g);
                color.b = rgbConvert(color.b);

                color.r = Math.max(0, color.r);
                color.g = Math.max(0, color.g);
                color.b = Math.max(0, color.b);

                double max_component = Math.max(Math.max(color.r, color.g), color.b);

                if (max_component > 1) {
                    color.r = color.r / max_component;
                    color.g = color.g / max_component;
                    color.b = color.b / max_component;
                }

                color.r = color.r * 255;
                color.g = color.g * 255;
                color.b = color.b * 255;
                break;
        }
    }

    public synchronized void setColorTemperature(int temperature){
        this.temperature=temperature;
        this.mode = Mode.Temperature;
        this.setColor();
    }

    public synchronized void setColorXY(double x, double y){
        this.x = x;
        this.y = y;
        this.mode = Mode.XY;
        this.setColor();
    }

    public synchronized void setColorHue(int hue, int sat){
        this.hue = hue;
        this.sat = sat;
        this.mode = Mode.Hue;
        this.setColor();
    }

    public synchronized void callback(){
        callback.execute(this);
    }

    public synchronized String toJSON(){
        String json;

        boolean status = (this.value>0);

        json = "{\"state\":{\"on\":" + status;
        if (type != Type.OnOff) {
            json = json + ",\"bri\":" + (this.getLastValue() - 1);
            if (type == Type.Color || type == Type.ExtendedColor) {
                json = json + ",\"hue\":" + hue + ",\"sat\":" + sat;
                json = json + ",\"effect\":\"none\",\"xy\":[" + x + "," + y + "]";
            }
            if (type == Type.WhiteSpectrum || type == Type.ExtendedColor) {
                json = json + ",\"ct\":" + temperature;
            }
        }
        json = json + ",\"alert\":\"none";
        if(type==Type.WhiteSpectrum || type  == Type.Color || type == Type.ExtendedColor) {
            json = json + "\",\"colormode\":\"" + mode;
        }
        json = json + "\",\"mode\":\"homeautomation\",\"reachable\":true},";
        json = json + "\"type\":\"" + type;
        json = json + "\",\"name\":\"" + name;
        json = json + "\",\"modelid\":\"" + type.id();
        json = json + "\",\"manufacturername\":\"Philips\",\"productname\":\"E" + type.number();
        json = json + "\",\"uniqueid\":\"" + encodeLightId(id);
        json = json + "\",\"swversion\":\"AlexaHueBridge-1.0.0\"}";
        return json;
    }

    public synchronized int getLastValue() {
        if(this.lastvalue==0){
            return 255;
        }
        return this.lastvalue;
    }

    public synchronized @NotNull RGB getColor(){
        return color;
    }

    public synchronized void setPropertyChanged(int value) {
        this.changed = value;
    }

    public synchronized void setValue(int value) {
        if(this.value != 0)
            this.lastvalue = this.value;
        if(value != 0)
            this.lastvalue = value;
        this.value = value;
    }

    public synchronized int getChanged(){
        return changed;
    }

    public synchronized int getValue(){
        return value;
    }

    public synchronized int getLastvalue(){
        return lastvalue;
    }

    public synchronized Pair<Double, Double> getXY(){
        return new Pair<>(x, y);
    }

    public synchronized int getId(){
        return id;
    }

    public synchronized Pair<Integer, Integer> getHueSat(){
        return new Pair<>(hue, sat);
    }

    public synchronized int getTemperature(){
        return temperature;
    }

    public synchronized Mode getMode(){
        return mode;
    }

    @Override
    public synchronized String toString(){
        return String.format("Name: %s\n\tId: %d\n\tMode: %s\n\tValue: %d\n\tLastValue: %d\n\tRGB: %s\n\tType: %s\n", name, id, mode, value, lastvalue, color, type);
    }
}
