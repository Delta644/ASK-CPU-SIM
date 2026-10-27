


public class Main {
    public static void main(String[] args) {

        Register CS = new Register("CS", 8);
        //Output memory and rejisters values here
        System.out.println("hjeloo world");

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



    }


    static void test(String name, boolean passed) {
        if (passed) {
            System.out.println("[PASS] " + name);
        } else {
            System.out.println("[FAIL] " + name);
        }
    }


}

