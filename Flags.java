public class Flags {
    private boolean zero; //wynik = 0
    private boolean negative; //ostatni bit wyniku = 1
    private boolean carry; //prekroczenie zakresu bez znaku
    private boolean overflow; //przekrozenie zakresu ze znakiem

    public boolean isZero() {
        return zero;
    }

    public void setZero(boolean zero) {
        this.zero = zero;
    }

    public boolean isCarry() {
        return carry;
    }

    public void setCarry(boolean carry) {
        this.carry = carry;
    }

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    public boolean isOverflow() {
        return overflow;
    }

    public void setOverflow(boolean overflow) {
        this.overflow = overflow;
    }


    @Override
    public String toString() {
        return "zero = " + zero + "\n" +
                "negative = " + negative + "\n" +
                "carry = " + carry + "\n" +
                "overflow = " + overflow;
    }
}