public class Timer {

    //FF04 — DIV: Divider register
    public static int DIV;
    private static int subDIV;

    //FF05 — TIMA: Timer counter
    public static int TIMA;

    //FF06 — TMA: Timer modulo
    public static int TMA;

    //FF07 — TAC: Timer control
    public static int TAC;

    private static int subT;
    private static int incT;

    private static boolean getBitTAC(int bit) {
        return (TAC & (1 << bit)) > 0;
    }
    public static final int TAC_BIT_ENABLED = 2;

    public static int processedCycles = 0;

    private static void nextCycle() {
        // DIV incremented at a rate of 16384Hz always
        subDIV++;
        if (subDIV > 256 - 1) {
            DIV = (DIV + 1) & 0xff;
            subDIV = 0;
        }
        
        // update TIMA only if TAC bit 2 is enabled
        if (getBitTAC(TAC_BIT_ENABLED)) {
            subT++;
            setIncT();
            if (subT > incT - 1) {
                subT = 0;
                TIMA = TIMA + 1;
                if (TIMA > 0xff) { // overflow
                    TIMA = TMA;
                    
                    // sera que IF eh setado se IE ou IME estiver disabled?
                    Irq.setBitIF(Irq.IRQ_BIT_TIMER, 1);
                }
            }
        }

        processedCycles++;
    }

    public static void process(int totalCycles) {
        int remainingCycles = totalCycles - processedCycles;
        for (int i = 0; i < remainingCycles; i++) {
            nextCycle();
        }
    }

    public static void setIncT() {
        switch (TAC & 0x03) {
            case 0: incT = 1024; break;
            case 1: incT = 16; break;
            case 2: incT = 64; break;
            case 3: incT = 256; break;
        }
    }

}
