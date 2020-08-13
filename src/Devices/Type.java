package Devices;

public enum Type{
    OnOff{
        @Override
        public String toString() {
            return "Light";
        }

        public String id(){
            return "Plug";
        }

        public int number(){
            return 0;
        }
    },
    Dimmable{
        @Override
        public String toString() {
            return "Dimmable Light";
        }

        public String id(){
            return "LWB010";
        }

        public int number(){
            return 1;
        }
    },
    Color{
        @Override
        public String toString() {
            return "Color light";
        }

        public String id(){
            return "LST001";
        }
        public int number(){
            return 3;
        }
    },
    ExtendedColor{
        @Override
        public String toString() {
            return "Extended color light";
        }

        public String id(){
            return "LCT015";
        }

        public int number(){
            return 4;
        }
    },
    WhiteSpectrum{
        @Override
        public String toString() {
            return "Color temperature light";
        }

        public String id(){
            return "LWT010";
        }

        public int number(){
            return 2;
        }
    };

    public abstract String id();
    public abstract int number();
}
