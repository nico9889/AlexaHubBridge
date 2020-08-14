package Devices;

public class RGB{
    double r,g,b;

    RGB(double r, double g, double b){
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public String toString(){
        return String.format("[R: %f G: %f B: %f]",r,g,b);
    }
}
