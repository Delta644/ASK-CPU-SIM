public class Memory {
    public static final int SIZE = 4096;
    private final byte[] cells = new byte[SIZE];

    //cells[addr] & 0xFF bitmask for 0-255 range


    public int read(int addr) {
        return cells[addr] & 0xFF;
    }

    public void write(int addr, int value) {
//
    }


}
