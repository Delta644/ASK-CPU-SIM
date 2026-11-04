public class ControlUnit {
    private final Memory memory;
    private final Alu alu;
    private final Flags flags;


    private final Register[] R = new Register[4];


    private final Register CS; // Segment kodu programu
    private final Register DS; // Segment danych


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

        // Inicjalizacja rejestrów segmentowych
        CS = new Register("CS", 4);
        DS = new Register("DS", 4);

        // Inicjalizacja liczników/wskaźników
        PC = new Register("PC", 8);
        SP = new Register("SP", 8);
        SP.set(0xFF); // Stos domyślnie rośnie w dół od końca segmentu
    }


    // Formuła: (numer bloku << 8 | przesunięcie)
    private int getPhysicalAddress(Register segmentRegister, int offset) {
        return (segmentRegister.get() << 8) | (offset & 0xFF);
    }

    // Cykl pobrania rozkazu (Fetch) - używa CS i PC
    public int fetch() {
        int physAddr = getPhysicalAddress(CS, PC.get());
        int data = memory.read(physAddr);

        // Sprzętowa inkrementacja licznika
        PC.set(PC.get() + 1);
        return data;
    }

    //Przykłądy

    // Adresowanie natychmiastowe
    public int fetchImmediate() {
        return fetch();
    }

    // Adresowanie domyślne - np. odczyt/zapis na stosie bazujący domyślnie na rejestrze SP
    public void push(int value) {
        SP.set(SP.get() - 1);
        int physAddr = getPhysicalAddress(DS, SP.get()); // Zakładamy stos w segmencie danych
        memory.write(physAddr, value);
    }


    // Szkielet
    public void step() {
        int opcode = fetch();
        //System.out.printf("%X ", opcode);
        System.out.println(opcode);

        // Tutaj znajdzie się dekoder z switch-case
    }
}


// Adresowanie bazowe z przemieszczeniem
//    public int readBaseWithDisplacement(Register baseRegister, int displacement) {
//        int offset = (baseRegister.get() + displacement) & 0xFF;
//        int physAddr = getPhysicalAddress(DS, offset);
//        return memory.read(physAddr);
//    }