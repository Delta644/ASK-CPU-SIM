public class ControlUnit {
    private final Memory memory;
    private final Alu alu;
    private final Flags flags;

    // 4 rejestry uniwersalne 8-bitowe
    private final Register[] R = new Register[4];

    // Odpowiednia liczba rejestrów segmentowych
    // Używamy 4-bitowych masek dla segmentów, co w połączeniu z 8-bitowym przemieszczeniem da 12-bitowy adres (4096 słów)[cite: 5]
    private final Register CS; // Segment kodu programu[cite: 5]
    private final Register DS; // Segment danych[cite: 5]

    // Rejestr licznika rozkazów (PC/IP)[cite: 5] - z perspektywy asemblera jest tylko do odczytu[cite: 5]
    private final Register PC;

    private final Register SP;

    public ControlUnit(Memory memory, Alu alu, Flags flags) {
        this.memory = memory;
        this.alu = alu;
        this.flags = flags;

        // Inicjalizacja rejestrów uniwersalnych
        for (int i = 0; i < 4; i++) {
            R[i] = new Register("R" + i, 8);
        }

        // Inicjalizacja rejestrów segmentowych (4-bitowe)
        CS = new Register("CS", 4);
        DS = new Register("DS", 4);

        // Inicjalizacja liczników/wskaźników (8-bitowe przemieszczenie wewnątrz segmentu)
        PC = new Register("PC", 8);
        SP = new Register("SP", 8);
        SP.set(0xFF); // Stos domyślnie rośnie w dół od końca segmentu
    }

    // Obliczanie 12-bitowego adresu fizycznego (realizuje możliwość zaadresowania 4096 słów pamięci)[cite: 5]
    // Formuła z uwag do Memory: (numer bloku << 8 | przesunięcie)
    private int getPhysicalAddress(Register segmentRegister, int offset) {
        return (segmentRegister.get() << 8) | (offset & 0xFF);
    }

    // Cykl pobrania rozkazu (Fetch) - zawsze używa segmentu kodu (CS) i licznika rozkazów (PC)[cite: 5]
    public int fetch() {
        int physAddr = getPhysicalAddress(CS, PC.get());
        int data = memory.read(physAddr);

        // Sprzętowa inkrementacja licznika (dla programisty rejestr jest tylko do odczytu)[cite: 5]
        PC.set(PC.get() + 1);
        return data;
    }

    // Przykłady realizacji wsparcia dla wymaganych trybów adresowania[cite: 5]

    // 1. Adresowanie natychmiastowe[cite: 5] - pobranie kolejnego bajtu za kodem rozkazu
    public int fetchImmediate() {
        return fetch();
    }

    // 2. Adresowanie domyślne[cite: 5] - np. odczyt/zapis na stosie bazujący domyślnie na rejestrze SP
    public void push(int value) {
        SP.set(SP.get() - 1);
        int physAddr = getPhysicalAddress(DS, SP.get()); // Zakładamy stos w segmencie danych
        memory.write(physAddr, value);
    }

    // 3. Adresowanie bazowe z przemieszczeniem[cite: 5]
    public int readBaseWithDisplacement(Register baseRegister, int displacement) {
        // Obliczenie przemieszczenia bazującego na rejestrze (np. uniwersalnym) w segmencie danych[cite: 5]
        int offset = (baseRegister.get() + displacement) & 0xFF;
        int physAddr = getPhysicalAddress(DS, offset);
        return memory.read(physAddr);
    }

    // Szkielet metody wykonującej pojedynczy cykl procesora
    public void step() {
        int opcode = fetch();
        // Tutaj znajdzie się dekoder z wielkim switch-case lub drabinką if-else (decode & execute)
    }
}