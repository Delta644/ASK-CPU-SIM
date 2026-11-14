public class Memory {
    public static final int SIZE = 4096;
    private final byte[] cells = new byte[SIZE];

    public int read(int addr) {
        if (addr < 0 || addr >= SIZE) {
            throw new IndexOutOfBoundsException("Adres poza zakresem pamięci (0-4095): " + addr);
        }
        return cells[addr] & 0xFF;
    }

    public void write(int addr, int value) {
        if (addr < 0 || addr >= SIZE) {
            throw new IndexOutOfBoundsException("Adres poza zakresem pamięci (0-4095): " + addr);
        }
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Byte value out of range: " + value);
        }
        cells[addr] = (byte) (value & 0xFF);
    }

    // Wyliczenie 12-bitowego adresu fizycznego: (Segment << 8) | Przemieszczenie
    public static int physicalAddress(int segment, int offset) {
        return ((segment & 0x0F) << 8) | (offset & 0xFF);
    }

    public int readSegmented(int segment, int offset) {
        return read(physicalAddress(segment, offset));
    }

    public void writeSegmented(int segment, int offset, int value) {
        write(physicalAddress(segment, offset), value);
    }

    public void dumpSegment(int segment, int startOffset, int length) {
        System.out.printf("=== Zrzut pamięci: Segment 0x%X (od 0x%02X, %d słów) ===%n", segment, startOffset, length);
        for (int i = 0; i < length; i++) {
            int offset = (startOffset + i) & 0xFF;
            int addr = physicalAddress(segment, offset);
            int val = read(addr);
            System.out.printf("[0x%03X] = 0x%02X (%3d)%s", addr, val, val, ((i + 1) % 4 == 0) ? "\n" : "   ");
        }
        if (length % 4 != 0) {
            System.out.println();
        }
    }
}