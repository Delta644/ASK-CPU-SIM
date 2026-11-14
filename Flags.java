public class Flags {
    private boolean zero;     // wynik = 0
    private boolean negative; // ostatni bit wyniku = 1 (znak)
    private boolean carry;    // przekroczenie zakresu bez znaku
    private boolean overflow; // przekroczenie zakresu ze znakiem

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
        return String.format("[Z=%d, N=%d, C=%d, V=%d]",
                zero ? 1 : 0, negative ? 1 : 0, carry ? 1 : 0, overflow ? 1 : 0);
    }
}