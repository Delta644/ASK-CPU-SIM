public class Main {
    private static int writePtr = 0;

    public static void main(String[] args) {
        System.out.println("=== 1. TESTY JEDNOSTKOWE ALU ===");
        Flags flags = new Flags();
        Alu alu = new Alu(flags);

        // ADD
        test("ADD 5 + 3", alu.add(5, 3) == 8
                && !flags.isZero()
                && !flags.isNegative()
                && !flags.isCarry()
                && !flags.isOverflow());

        test("ADD 200 + 100", alu.add(200, 100) == 44
                && !flags.isZero()
                && !flags.isNegative()
                && flags.isCarry()
                && !flags.isOverflow());

        test("ADD 0 + 0", alu.add(0, 0) == 0
                && flags.isZero()
                && !flags.isNegative()
                && !flags.isCarry()
                && !flags.isOverflow());

        // SUB
        test("SUB 10 - 5", alu.sub(10, 5) == 5
                && !flags.isZero()
                && !flags.isNegative()
                && flags.isCarry()
                && !flags.isOverflow());

        test("SUB 5 - 10", alu.sub(5, 10) == 251
                && !flags.isZero()
                && flags.isNegative()
                && !flags.isCarry()
                && !flags.isOverflow());

        test("SUB 7 - 7", alu.sub(7, 7) == 0
                && flags.isZero()
                && !flags.isNegative()
                && flags.isCarry()
                && !flags.isOverflow());

        // AND
        test("AND 1100 & 1010", alu.and(0b1100, 0b1010) == 0b1000
                && !flags.isZero()
                && !flags.isNegative());

        // OR
        test("OR 1100 | 0011", alu.or(0b1100, 0b0011) == 0b1111
                && !flags.isZero()
                && !flags.isNegative());

        // XOR
        test("XOR 1100 ^ 1010", alu.xor(0b1100, 0b1010) == 0b0110
                && !flags.isZero()
                && !flags.isNegative());

        // NOT
        test("NOT 00001111", alu.not(0b00001111) == 0b11110000
                && !flags.isZero()
                && flags.isNegative());

        test("NOT 11111111", alu.not(0xFF) == 0
                && flags.isZero()
                && !flags.isNegative());

        // Weryfikacja zabezpieczenia rejestru PC (tylko do odczytu)
        Memory memory = new Memory();
        ControlUnit cu = new ControlUnit(memory, alu, flags);
        boolean pcReadOnlyOk = false;
        try {
            cu.getPC().set(10);
        } catch (UnsupportedOperationException e) {
            pcReadOnlyOk = true;
        }
        test("Rejestr PC tylko do odczytu", pcReadOnlyOk);

        System.out.println("\n=== 2. SYMULACJA PROGRAMU DEMONSTRACYJNEGO ===");
        loadDemoProgram(memory);
        cu.run();

        System.out.println();
        cu.printState();
        System.out.println();
        memory.dumpSegment(0x2, 0x00, 8);
    }

    // Pomocnicze metody do ładowania rozkazów 1-, 2- i 3-bajtowych do pamięci
    private static void emit1(Memory m, int opcode, int regA) {
        m.write(writePtr++, ((opcode & 0x3F) << 2) | (regA & 0b11));
    }

    private static void emit2(Memory m, int opcode, int regA, int byte2) {
        emit1(m, opcode, regA);
        m.write(writePtr++, byte2 & 0xFF);
    }

    private static void emit3(Memory m, int opcode, int regA, int byte2, int byte3) {
        emit1(m, opcode, regA);
        m.write(writePtr++, byte2 & 0xFF);
        m.write(writePtr++, byte3 & 0xFF);
    }

    private static void loadDemoProgram(Memory m) {
        writePtr = 0x000; // Segment kodu CS = 0x0

        // 0x000: START procesora (1 bajt)
        emit1(m, ControlUnit.OP_START_HLT, 0);

        // 0x001: Ustawienie rejestru segmentu danych DS = 0x2 (2 bajty)
        emit2(m, ControlUnit.OP_SET_DS, 0, 0x02);

        // 0x003: Inicjalizacja rejestrów wartościami natychmiastowymi (po 2 bajty)
        emit2(m, ControlUnit.OP_MOV_RN, 0, 10); // R0 = 10
        emit2(m, ControlUnit.OP_MOV_RN, 1, 20); // R1 = 20
        emit2(m, ControlUnit.OP_MOV_RN, 2, 5);  // R2 = 5

        // 0x009: Jednoczesne dodawanie rej-rej: R0 = R0 + R2 (15), R1 = R1 + R2 (25) (2 bajty)
        emit2(m, ControlUnit.OP_SIM_ADD_RR, 0, (2 << 2) | 1);

        // 0x00B: Jednoczesne odejmowanie rej-nat: R0 = R0 - 3 (12), R1 = R1 - 3 (22) (3 bajty)
        emit3(m, ControlUnit.OP_SIM_SUB_RN, 0, 3, 1);

        // 0x00E: Zapis R0 i R1 do segmentu danych [0x2:0x10] oraz [0x2:0x11] (po 3 bajty)
        // Drugi bajt: 0x10 (flaga zapisu) | 0x02 (segment bazowy 2) = 0x12
        emit3(m, ControlUnit.OP_MOV_RM, 0, 0x12, 0x10);
        emit3(m, ControlUnit.OP_MOV_RM, 1, 0x12, 0x11);

        // 0x014: Odczyt z pamięci [0x2:0x10] do R3 i dodanie [0x2:0x11] (po 3 bajty)
        emit3(m, ControlUnit.OP_MOV_RM, 3, 0x02, 0x10); // R3 = 12
        emit3(m, ControlUnit.OP_ADD_RM, 3, 0x02, 0x11); // R3 = 12 + 22 = 34

        // 0x01A: Wywołanie podprogramu pod adresem 0x0:0x40 (3 bajty)
        emit3(m, ControlUnit.OP_CALL, 0, 0x00, 0x40);

        // 0x01D: Porównanie R3 (44) z wartością 40 i skok warunkowy JG do 0x0:0x25
        emit2(m, ControlUnit.OP_CMP_RN, 3, 40);
        emit3(m, ControlUnit.OP_JG, 0, 0x00, 0x25);

        // 0x022: Instrukcja pomijana przez skok JG (3 bajty: JMP 0x0:0x00)
        emit3(m, ControlUnit.OP_JMP, 0, 0x00, 0x00);

        // 0x025: Operacje logiczne na rejestrze R2 (po 2 bajty)
        emit2(m, ControlUnit.OP_AND_RN, 2, 0b00001111); // R2 = 5 & 15 = 5
        emit2(m, ControlUnit.OP_OR_RN,  2, 0b11110000); // R2 = 5 | 240 = 245 (0xF5)
        emit2(m, ControlUnit.OP_XOR_RN, 2, 0b11111111); // R2 = 245 ^ 255 = 10 (0x0A)

        // 0x02B: Zapis końcowych wyników R3 (44) pod [0x2:0x00] oraz R2 (10) pod [0x2:0x01]
        emit3(m, ControlUnit.OP_MOV_RM, 3, 0x12, 0x00);
        emit3(m, ControlUnit.OP_MOV_RM, 2, 0x12, 0x01);

        // 0x031: HLT - zatrzymanie procesora (1 bajt)
        emit1(m, ControlUnit.OP_START_HLT, 0);

        // === PODPROGRAM pod adresem 0x040 ===
        writePtr = 0x040;
        emit2(m, ControlUnit.OP_ADD_RN, 3, 10); // R3 = R3 + 10 (34 + 10 = 44)
        emit1(m, ControlUnit.OP_RET, 0);        // Powrót z podprogramu (1 bajt)
    }

    static void test(String name, boolean passed) {
        if (passed) {
            System.out.println("[PASS] " + name);
        } else {
            System.out.println("[FAIL] " + name);
        }
    }
}