public class Registers {
    private final String name;
    private final int mask;
    private int value;

    public Registers(String name, int bits) {
        this.name = name;
        this.mask = (1 << bits) - 1;
    }

    public int get() { return value; }

    public String getName() {return name;}

    public void set(int v) {
        value = v & mask;
    }




   // @Override public String toString() {
   // }
}