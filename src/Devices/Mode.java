package Devices;

public enum Mode{
    Hue{
        @Override
        public String toString() {
            return "hue";
        }
    },
    Temperature{
        @Override
        public String toString() {
            return "ct";
        }
    },
    XY{
        @Override
        public String toString() {
            return "xy";
        }
    }
}