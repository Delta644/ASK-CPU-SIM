public class Register {
    private final String name;
    private final int bits;
    protected final int mask;
    protected int value;

    public Register(String name, int bits) {
        this.name = name;
        this.bits = bits;
        this.mask = (1 << bits) - 1;
    }

    public int get() {
        return value;
    }

    public String getName() {
        return name;
    }

    public void set(int v) {
        value = v & mask;
    }

    @Override
    public String toString() {
        if (bits <= 4) {
            return String.format("%s: 0x%01X (%d)", name, value, value);
        }
        return String.format("%s: 0x%02X (%3d)", name, value, value);
    }
}