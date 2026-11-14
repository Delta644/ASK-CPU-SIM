public class ControlUnit {

    // Kody rozkazów (6-bitowe) zgodne z tabelą w PDF
    public static final int OP_START_HLT = 0b000000; // 0x00: Rozpoczęcie / zatrzymanie pracy
    public static final int OP_MOV_RN    = 0b000001; // 0x01: Przesyłanie rej-nat
    public static final int OP_MOV_RR    = 0b000010; // 0x02: Przesyłanie rej-rej
    public static final int OP_ADD_RN    = 0b000011; // 0x03: Dodawanie rej-nat
    public static final int OP_ADD_RR    = 0b000100; // 0x04: Dodawanie rej-rej
    public static final int OP_SUB_RN    = 0b000101; // 0x05: Odejmowanie rej-nat
    public static final int OP_SUB_RR    = 0b000110; // 0x06: Odejmowanie rej-rej
    public static final int OP_SIM_SUB_RR= 0b000111; // 0x07: Jednoczesne odejmowanie rej-rej
    public static final int OP_SIM_ADD_RR= 0b001000; // 0x08: Jednoczesne dodawanie rej-rej
    public static final int OP_SIM_SUB_RN= 0b001001; // 0x09: Jednoczesne odejmowanie rej-nat
    public static final int OP_SIM_ADD_RN= 0b001010; // 0x0A: Jednoczesne dodawanie rej-nat
    public static final int OP_CMP_RN    = 0b001011; // 0x0B: Porównywanie rej-nat
    public static final int OP_CMP_RR    = 0b001100; // 0x0C: Porównywanie rej-rej
    public static final int OP_AND_RN    = 0b001101; // 0x0D: AND rej-nat
    public static final int OP_OR_RN     = 0b001110; // 0x0E: OR rej-nat
    public static final int OP_XOR_RN    = 0b001111; // 0x0F: XOR rej-nat
    public static final int OP_NOT_RN    = 0b010000; // 0x10: NOT rej-nat
    public static final int OP_MOV_RM    = 0b010001; // 0x11: Przesyłanie rej-pam (odczyt/zapis)
    public static final int OP_ADD_RM    = 0b010010; // 0x12: Dodawanie rej-pam
    public static final int OP_SUB_RM    = 0b010011; // 0x13: Odejmowanie rej-pam
    public static final int OP_CALL      = 0b010100; // 0x14: Wywołanie podprogramu
    public static final int OP_RET       = 0b010101; // 0x15: Powrót z podprogramu
    public static final int OP_JMP       = 0b010110; // 0x16: Skok bezwarunkowy
    public static final int OP_JG        = 0b010111; // 0x17: Skok gdy > (większe)
    public static final int OP_JL        = 0b011000; // 0x18: Skok gdy < (mniejsze)
    public static final int OP_JE        = 0b011001; // 0x19: Skok gdy = (równe)
    public static final int OP_SET_DS    = 0b011010; // 0x1A: Zapis numeru do rejestru segmentowego DS

    // Specjalna klasa dla licznika rozkazów (tylko do odczytu z zewnątrz)
    public static class ReadOnlyPC extends Register {
        public ReadOnlyPC() {
            super("PC", 8);
        }

        @Override
        public void set(int v) {
            throw new UnsupportedOperationException("Rejestr licznika rozkazów (PC) jest tylko do odczytu!");
        }

        private void updateInternal(int v) {
            this.value = v & this.mask;
        }
    }

    private final Memory memory;
    private final Alu alu;
    private final Flags flags;

    // 4 rejestry uniwersalne (R0 - R3)
    private final Register[] regs = new Register[4];

    // Rejestry segmentowe (4-bitowe: wybierają 1 z 16 segmentów po 256 słów = 4096 słów)
    private final Register cs = new Register("CS", 4);
    private final Register ds = new Register("DS", 4);
    private final Register ss = new Register("SS", 4);

    // Wskaźnik stosu (8-bitowy) oraz Licznik Rozkazów (tylko do odczytu)
    private final Register sp = new Register("SP", 8);
    private final ReadOnlyPC pc = new ReadOnlyPC();

    private boolean running = false;
    private boolean started = false;
    private boolean verbose = true;

    public ControlUnit(Memory memory, Alu alu, Flags flags) {
        this.memory = memory;
        this.alu = alu;
        this.flags = flags;

        for (int i = 0; i < 4; i++) {
            regs[i] = new Register("R" + i, 8);
        }

        reset();
    }

    public void reset() {
        for (Register r : regs) {
            r.set(0);
        }
        cs.set(0x0); // Domyślny segment kodu = 0
        ds.set(0x1); // Domyślny segment danych = 1
        ss.set(0xF); // Domyślny segment stosu = 15 (0xF00 - 0xFFF)
        sp.set(0xFF); // Stos rośnie w dół od końca segmentu SS
        pc.updateInternal(0x00);
        running = false;
        started = false;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public Register getRegister(int idx) {
        return regs[idx & 0b11];
    }

    public Register getCS() { return cs; }
    public Register getDS() { return ds; }
    public Register getSS() { return ss; }
    public Register getSP() { return sp; }
    public Register getPC() { return pc; }

    // Pobranie pojedynczego bajtu z segmentu kodu (CS:PC) i inkrementacja PC
    public int fetch() {
        int addr = Memory.physicalAddress(cs.get(), pc.get());
        int byteVal = memory.read(addr);
        pc.updateInternal(pc.get() + 1);
        return byteVal;
    }

    // Operacje na stosie (obsługa wywołań podprogramów CALL/RET)
    public void push(int val) {
        memory.writeSegmented(ss.get(), sp.get(), val & 0xFF);
        sp.set(sp.get() - 1);
    }

    public int pop() {
        sp.set(sp.get() + 1);
        return memory.readSegmented(ss.get(), sp.get());
    }

    // Rozwiązanie adresu bazowego: jeśli M == 0x0F (lub ma ustawiony bit 0x80), użyj domyślnego rejestru DS/CS
    private int resolveSegment(int mByte, Register defaultSeg) {
        if ((mByte & 0x80) != 0) {
            return defaultSeg.get();
        }
        return mByte & 0x0F;
    }

    // Dekodowanie i wykonanie jednego rozkazu o zmiennej długości (1, 2 lub 3 bajty)
    public boolean step() {
        int instrAddr = Memory.physicalAddress(cs.get(), pc.get());
        int firstByte = fetch();

        int opcode = (firstByte >> 2) & 0b111111;
        int regA = firstByte & 0b11;

        execute(opcode, regA, instrAddr);
        return running;
    }

    private void execute(int opcode, int a, int instrAddr) {
        switch (opcode) {
            case OP_START_HLT -> { // 1 bajt
                if (!started) {
                    started = true;
                    running = true;
                    log(instrAddr, "START (Rozpoczęcie pracy procesora)");
                } else {
                    running = false;
                    log(instrAddr, "HLT (Zakończenie pracy procesora)");
                }
            }
            case OP_MOV_RN -> { // 2 bajty
                int imm = fetch();
                regs[a].set(imm);
                log(instrAddr, String.format("MOV R%d, %d (0x%02X)", a, imm, imm));
            }
            case OP_MOV_RR -> { // 2 bajty
                int b = fetch() & 0b11;
                regs[a].set(regs[b].get());
                log(instrAddr, String.format("MOV R%d, R%d", a, b));
            }
            case OP_ADD_RN -> { // 2 bajty
                int imm = fetch();
                regs[a].set(alu.add(regs[a].get(), imm));
                log(instrAddr, String.format("ADD R%d, %d -> R%d=%d", a, imm, a, regs[a].get()));
            }
            case OP_ADD_RR -> { // 2 bajty
                int b = fetch() & 0b11;
                regs[a].set(alu.add(regs[a].get(), regs[b].get()));
                log(instrAddr, String.format("ADD R%d, R%d -> R%d=%d", a, b, a, regs[a].get()));
            }
            case OP_SUB_RN -> { // 2 bajty
                int imm = fetch();
                regs[a].set(alu.sub(regs[a].get(), imm));
                log(instrAddr, String.format("SUB R%d, %d -> R%d=%d", a, imm, a, regs[a].get()));
            }
            case OP_SUB_RR -> { // 2 bajty
                int b = fetch() & 0b11;
                regs[a].set(alu.sub(regs[a].get(), regs[b].get()));
                log(instrAddr, String.format("SUB R%d, R%d -> R%d=%d", a, b, a, regs[a].get()));
            }
            case OP_SIM_SUB_RR -> { // 2 bajty: R[A]=R[A]-R[C], R[B]=R[B]-R[C]
                int second = fetch();
                int c = (second >> 2) & 0b11;
                int b = second & 0b11;
                int valC = regs[c].get();
                regs[a].set(alu.sub(regs[a].get(), valC));
                regs[b].set(alu.sub(regs[b].get(), valC));
                log(instrAddr, String.format("SIM_SUB R%d, R%d, R%d -> R%d=%d, R%d=%d",
                        a, b, c, a, regs[a].get(), b, regs[b].get()));
            }
            case OP_SIM_ADD_RR -> { // 2 bajty: R[A]=R[A]+R[C], R[B]=R[B]+R[C]
                int second = fetch();
                int c = (second >> 2) & 0b11;
                int b = second & 0b11;
                int valC = regs[c].get();
                regs[a].set(alu.add(regs[a].get(), valC));
                regs[b].set(alu.add(regs[b].get(), valC));
                log(instrAddr, String.format("SIM_ADD R%d, R%d, R%d -> R%d=%d, R%d=%d",
                        a, b, c, a, regs[a].get(), b, regs[b].get()));
            }
            case OP_SIM_SUB_RN -> { // 3 bajty: R[A]=R[A]-L, R[B]=R[B]-L
                int imm = fetch();
                int b = fetch() & 0b11;
                regs[a].set(alu.sub(regs[a].get(), imm));
                regs[b].set(alu.sub(regs[b].get(), imm));
                log(instrAddr, String.format("SIM_SUB_IMM R%d, R%d, %d -> R%d=%d, R%d=%d",
                        a, b, imm, a, regs[a].get(), b, regs[b].get()));
            }
            case OP_SIM_ADD_RN -> { // 3 bajty: R[A]=R[A]+L, R[B]=R[B]+L
                int imm = fetch();
                int b = fetch() & 0b11;
                regs[a].set(alu.add(regs[a].get(), imm));
                regs[b].set(alu.add(regs[b].get(), imm));
                log(instrAddr, String.format("SIM_ADD_IMM R%d, R%d, %d -> R%d=%d, R%d=%d",
                        a, b, imm, a, regs[a].get(), b, regs[b].get()));
            }
            case OP_CMP_RN -> { // 2 bajty
                int imm = fetch();
                alu.sub(regs[a].get(), imm);
                log(instrAddr, String.format("CMP R%d (%d), %d -> %s", a, regs[a].get(), imm, flags));
            }
            case OP_CMP_RR -> { // 2 bajty
                int b = fetch() & 0b11;
                alu.sub(regs[a].get(), regs[b].get());
                log(instrAddr, String.format("CMP R%d (%d), R%d (%d) -> %s", a, regs[a].get(), b, regs[b].get(), flags));
            }
            case OP_AND_RN -> { // 2 bajty
                int imm = fetch();
                regs[a].set(alu.and(regs[a].get(), imm));
                log(instrAddr, String.format("AND R%d, 0x%02X -> 0x%02X", a, imm, regs[a].get()));
            }
            case OP_OR_RN -> { // 2 bajty
                int imm = fetch();
                regs[a].set(alu.or(regs[a].get(), imm));
                log(instrAddr, String.format("OR R%d, 0x%02X -> 0x%02X", a, imm, regs[a].get()));
            }
            case OP_XOR_RN -> { // 2 bajty
                int imm = fetch();
                regs[a].set(alu.xor(regs[a].get(), imm));
                log(instrAddr, String.format("XOR R%d, 0x%02X -> 0x%02X", a, imm, regs[a].get()));
            }
            case OP_NOT_RN -> { // 2 bajty (R[A] = ~L)
                int imm = fetch();
                regs[a].set(alu.not(imm));
                log(instrAddr, String.format("NOT R%d, 0x%02X -> 0x%02X", a, imm, regs[a].get()));
            }
            case OP_MOV_RM -> { // 3 bajty: bit 4 w M określa kierunek (0 = odczyt z pam, 1 = zapis do pam)
                int mByte = fetch();
                int offset = fetch();
                boolean storeToMem = (mByte & 0x10) != 0;
                int seg = resolveSegment(mByte, ds);
                if (storeToMem) {
                    memory.writeSegmented(seg, offset, regs[a].get());
                    log(instrAddr, String.format("MOV [0x%X:0x%02X], R%d (%d)", seg, offset, a, regs[a].get()));
                } else {
                    int val = memory.readSegmented(seg, offset);
                    regs[a].set(val);
                    log(instrAddr, String.format("MOV R%d, [0x%X:0x%02X] -> %d", a, seg, offset, val));
                }
            }
            case OP_ADD_RM -> { // 3 bajty
                int mByte = fetch();
                int offset = fetch();
                int seg = resolveSegment(mByte, ds);
                int memVal = memory.readSegmented(seg, offset);
                regs[a].set(alu.add(regs[a].get(), memVal));
                log(instrAddr, String.format("ADD R%d, [0x%X:0x%02X] (%d) -> %d", a, seg, offset, memVal, regs[a].get()));
            }
            case OP_SUB_RM -> { // 3 bajty
                int mByte = fetch();
                int offset = fetch();
                int seg = resolveSegment(mByte, ds);
                int memVal = memory.readSegmented(seg, offset);
                regs[a].set(alu.sub(regs[a].get(), memVal));
                log(instrAddr, String.format("SUB R%d, [0x%X:0x%02X] (%d) -> %d", a, seg, offset, memVal, regs[a].get()));
            }
            case OP_CALL -> { // 3 bajty: odłożenie CS i PC na stos, skok do podprogramu
                int mByte = fetch();
                int offset = fetch();
                int targetSeg = resolveSegment(mByte, cs);
                push(cs.get());
                push(pc.get());
                cs.set(targetSeg);
                pc.updateInternal(offset);
                log(instrAddr, String.format("CALL 0x%X:0x%02X (SP=0x%02X)", targetSeg, offset, sp.get()));
            }
            case OP_RET -> { // 1 bajt: zdjęcie PC i CS ze stosu
                int retPc = pop();
                int retCs = pop();
                cs.set(retCs);
                pc.updateInternal(retPc);
                log(instrAddr, String.format("RET -> 0x%X:0x%02X (SP=0x%02X)", retCs, retPc, sp.get()));
            }
            case OP_JMP -> { // 3 bajty
                int mByte = fetch();
                int offset = fetch();
                int targetSeg = resolveSegment(mByte, cs);
                cs.set(targetSeg);
                pc.updateInternal(offset);
                log(instrAddr, String.format("JMP 0x%X:0x%02X", targetSeg, offset));
            }
            case OP_JG -> { // 3 bajty: Skok gdy większe (Z == 0 oraz N == 0)
                int mByte = fetch();
                int offset = fetch();
                int targetSeg = resolveSegment(mByte, cs);
                if (!flags.isZero() && !flags.isNegative()) {
                    cs.set(targetSeg);
                    pc.updateInternal(offset);
                    log(instrAddr, String.format("JG 0x%X:0x%02X [SKOK WYKONANY]", targetSeg, offset));
                } else {
                    log(instrAddr, String.format("JG 0x%X:0x%02X [POMINIĘTY]", targetSeg, offset));
                }
            }
            case OP_JL -> { // 3 bajty: Skok gdy mniejsze (N == 1)
                int mByte = fetch();
                int offset = fetch();
                int targetSeg = resolveSegment(mByte, cs);
                if (flags.isNegative()) {
                    cs.set(targetSeg);
                    pc.updateInternal(offset);
                    log(instrAddr, String.format("JL 0x%X:0x%02X [SKOK WYKONANY]", targetSeg, offset));
                } else {
                    log(instrAddr, String.format("JL 0x%X:0x%02X [POMINIĘTY]", targetSeg, offset));
                }
            }
            case OP_JE -> { // 3 bajty: Skok gdy równe (Z == 1)
                int mByte = fetch();
                int offset = fetch();
                int targetSeg = resolveSegment(mByte, cs);
                if (flags.isZero()) {
                    cs.set(targetSeg);
                    pc.updateInternal(offset);
                    log(instrAddr, String.format("JE 0x%X:0x%02X [SKOK WYKONANY]", targetSeg, offset));
                } else {
                    log(instrAddr, String.format("JE 0x%X:0x%02X [POMINIĘTY]", targetSeg, offset));
                }
            }
            case OP_SET_DS -> { // 2 bajty (rozkaz 011010 ze str. 5 PDF: zapis numeru do rejestru segmentowego)
                int seg = fetch() & 0x0F;
                ds.set(seg);
                log(instrAddr, String.format("SET_DS 0x%X", seg));
            }
            default -> {
                running = false;
                throw new IllegalStateException(String.format("Nieznany kod rozkazu: 0x%02X pod adresem 0x%03X", opcode, instrAddr));
            }
        }
    }

    public void run() {
        int maxSteps = 1000;
        int steps = 0;
        do {
            step();
            steps++;
            if (steps >= maxSteps) {
                System.out.println("[WARN] Przekroczono limit kroków symulacji!");
                break;
            }
        } while (running);
    }

    private void log(int addr, String msg) {
        if (verbose) {
            System.out.printf("[0x%03X] %-42s | R0=%3d R1=%3d R2=%3d R3=%3d | %s%n",
                    addr, msg, regs[0].get(), regs[1].get(), regs[2].get(), regs[3].get(), flags);
        }
    }

    public void printState() {
        System.out.println("=== STAN PROCESORA ===");
        System.out.printf("Rejestry uniwersalne: %s | %s | %s | %s%n", regs[0], regs[1], regs[2], regs[3]);
        System.out.printf("Rejestry segmentowe : %s | %s | %s%n", cs, ds, ss);
        System.out.printf("Rejestry specjalne  : %s | %s%n", pc, sp);
        System.out.printf("Znaczniki (Flagi)   : %s%n", flags);
    }
}