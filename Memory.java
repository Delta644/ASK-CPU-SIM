public class Memory {
    public static final int SIZE = 4096;
    private final byte[] cells = new byte[SIZE];

    //cells[addr] & 0xFF bitmask for 0-255 range
    //numer bloku << 8 | przsunięćie (m3m2m1m0 << 8 | p7p6p5p4p3p2p1p0)


    public int read(int addr) {
        return cells[addr] & 0xFF;
    }

    public void write(int addr, int value) {

        //test value
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException(
                    "Byte value out of range: " + value
            );
        }

        cells[addr] = (byte) (value & 0xFF);
    }


}
