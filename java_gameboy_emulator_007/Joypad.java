public class Joypad {
    
    public static boolean printDebug = false;

    // FF00 — P1/JOYP: Joypad register
    public static int JOYP = 0xcf;

    /**
     * bit 5 Select buttons	
     * bit 4 Select d-pad	
     * bit 3 Start / Down	
     * bit 2 Select / Up	
     * bit 1 B / Left	
     * bit 0 A / Right     * 
     */
    public static final int JOYP_BIT_SEL_BUTTONS = 5;
    public static final int JOYP_BIT_SEL_DPAD = 4;
    public static final int JOYP_BIT_START = 3;
    public static final int JOYP_BIT_SELECT = 2;
    public static final int JOYP_BIT_B = 1;
    public static final int JOYP_BIT_A = 0;
    public static final int JOYP_BIT_DOWN = 3;
    public static final int JOYP_BIT_UP = 2;
    public static final int JOYP_BIT_LEFT = 1;
    public static final int JOYP_BIT_RIGHT = 0;
    
    //private static boolean isFlagJOYP(int bit) {
    //    return (JOYP & (1 << bit)) > 0;
    //}

    // bit = 0-7
    public static void setFlag(int bit, int value) {
        if (value == 1) {
            JOYP = JOYP | (1 << bit);
        }
        else if (value == 0) {
            JOYP = JOYP & ~(1 << bit);
        }
    }

    public static int JOYP_DPAD = 0xcf;
    public static int JOYP_BUTT = 0xcf;

    public static void setFlagDPAD(int bit, int value) {
        if (value == 1) { // 1=not pressed
            JOYP_DPAD = JOYP_DPAD | (1 << bit);
        }
        else if (value == 0) { // 0=pressed
            boolean prevReleased = (JOYP_DPAD & (1 << bit)) > 0;
            JOYP_DPAD = JOYP_DPAD & ~(1 << bit);
            if (prevReleased) {
                Irq.setBitIF(Irq.IRQ_BIT_JOYPAD, 1);
            }
        }
    }    

    public static void setFlagBUTT(int bit, int value) {
        if (value == 1) { // 1=not pressed
            JOYP_BUTT = JOYP_BUTT | (1 << bit);
        }
        else if (value == 0) { // 0=pressed
            boolean prevReleased = (JOYP_BUTT & (1 << bit)) > 0;
            JOYP_BUTT = JOYP_BUTT & ~(1 << bit);
            if (prevReleased) {
                Irq.setBitIF(Irq.IRQ_BIT_JOYPAD, 1);
            }
        }
    }        

}
