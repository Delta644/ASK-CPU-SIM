public class Memory {
    public static final int SIZE = 4096;
    private final byte[] cells = new byte[SIZE];

    //cells[addr] & 0xFF bitmask for 0-255 range
    //numer bloku << 8 | przsunięćie (m3m2m1m0 << 8 | p7p6p5p4p3p2p1p0)


    public int read(int addr) {
        checkAddr(addr);
        return cells[addr] & 0xFF;
    }

    public void write(int addr, int value) {
        checkAddr(addr);

        cells[addr] = (byte) (value & 0xFF);
    }

    private void checkAddr(int addr) {
        if (addr < 0 || addr >= SIZE) {
            throw new IllegalArgumentException("Adres poza zakresem: " + addr);
        }
    }

    public void load(int[] program, int startAddr) {

        if (startAddr < 0 || startAddr + program.length > SIZE) {
            throw new IllegalArgumentException("Brak miejsca na program w pamięci");
        }

        for (int i = 0; i < program.length; i++) {
            write(startAddr + i, program[i]);
        }
    }


}
