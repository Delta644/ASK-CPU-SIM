public class Alu {

    private final Flags flags;

    public Alu(Flags flags) {
        this.flags = flags;
    }

    public int add(int a, int b){
        a &= 0xFF;  b &= 0xFF;

        int full = a+b;
        int res = full & 0xFF;

        flags.setCarry(full > 0xFF);
        flags.setOverflow((~(a ^ b) & (a ^ res) & 0x80) != 0);
        flags.setZero(res == 0);
        flags.setNegative((res & 0x80) != 0);

        return res;

    }


    //carry = true przy wystąpieniu pozyczki
    public int sub(int a, int b) {

        a &= 0xFF;
        b &= 0xFF;

        int full = a - b;
        int res = full & 0xFF;

        flags.setCarry(full < 0xFF);
        flags.setOverflow(((a ^ b) & (a ^ res) & 0x80) != 0);

        flags.setZero(res == 0);
        flags.setNegative((res & 0x80) != 0);
        flags.setNegative((res & 0x80) != 0);
        return res;
    }

    public int and(int a, int b){
        a &= 0xFF;  b &= 0xFF;

        int res = a & b;
        flags.setZero(res == 0);
        flags.setNegative((res & 0x80) != 0);

        return res;
    }

    public int or(int a, int b){
        a &= 0xFF;  b &= 0xFF;

        int res = a | b;
        flags.setZero(res == 0);
        flags.setNegative((res & 0x80) != 0);

        return res;
    }

    public int xor(int a, int b){

        a &= 0xFF;  b &= 0xFF;

        int res = a ^ b;
        flags.setZero(res == 0);
        flags.setNegative((res & 0x80) != 0);

        return res;

    }

    public int not(int a){
        a &= 0xFF;

        int res = (~a) & 0xFF;
        flags.setZero(res == 0);
        flags.setNegative((res & 0x80) != 0);

        return res;
    }
}
