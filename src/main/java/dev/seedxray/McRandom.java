package dev.seedxray;

/** Exact copy of Minecraft's legacy LCG (CheckedRandom / java.util.Random). */
final class McRandom {
    private static final long MASK = (1L << 48) - 1;
    private long seed;

    McRandom(long s) { setSeed(s); }

    void setSeed(long s) { seed = (s ^ 0x5DEECE66DL) & MASK; }

    private int next(int bits) {
        seed = (seed * 0x5DEECE66DL + 0xBL) & MASK;
        return (int) (seed >>> (48 - bits));
    }

    int nextInt(int bound) {
        if ((bound & (bound - 1)) == 0) return (int) ((bound * (long) next(31)) >> 31);
        int bits, val;
        do { bits = next(31); val = bits % bound; } while (bits - val + (bound - 1) < 0);
        return val;
    }

    long nextLong() { return ((long) next(32) << 32) + next(32); }
    float nextFloat() { return next(24) * 5.9604645E-8F; }
    double nextDouble() { return (((long) next(26) << 27) + next(27)) * 1.1102230246251565E-16; }
}
