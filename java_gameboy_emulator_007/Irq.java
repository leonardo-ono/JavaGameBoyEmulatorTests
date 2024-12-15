public class Irq {

    // FFFF — IE: Interrupt enable
    public static int IE = 0;

    /**
     * bit 0 VBlank (Read/Write): Controls whether the VBlank interrupt handler may be called (see IF below).
     * bit 1 LCD (Read/Write): Controls whether the LCD interrupt handler may be called (see IF below).
     * bit 2 Timer (Read/Write): Controls whether the Timer interrupt handler may be called (see IF below).
     * bit 3 Serial (Read/Write): Controls whether the Serial interrupt handler may be called (see IF below).
     * bit 4 Joypad (Read/Write): Controls whether the Joypad interrupt handler may be called (see IF below).
     */

     // duvida: sera que IF eh setado pelos devices JOYPAD, TIMER, ETC mesmo se o IME ou IE estiver disabled?
     // FF0F — IF: Interrupt flag
     public static int IF = 0xe1;


     /**
      * bit 0 VBlank (Read/Write): Controls whether the VBlank interrupt handler is being requested.
      * bit 1 LCD (Read/Write): Controls whether the LCD interrupt handler is being requested.
      * bit 2 Timer (Read/Write): Controls whether the Timer interrupt handler is being requested.
      * bit 3 Serial (Read/Write): Controls whether the Serial interrupt handler is being requested.
      * bit 4 Joypad (Read/Write): Controls whether the Joypad interrupt handler is being requested.
      */
      public static final int IRQ_BIT_VBLANK = 0;
      public static final int IRQ_BIT_LCD = 1;
      public static final int IRQ_BIT_TIMER = 2;
      public static final int IRQ_BIT_SERIAL = 3;
      public static final int IRQ_BIT_JOYPAD = 4;

      // bit = 0-7
      public static void setBitIF(int bit, int value) {
        if (value == 1) {
            IF = IF | (1 << bit);
        }
        else if (value == 0) {
            IF = IF & ~(1 << bit);
        }
      }

      public static int getBitIF(int bit) {
        return (IF & (1 << bit)) > 0 ? 1 : 0;
      }

      public static void process() {

        if (Cpu.IME_PENDING == 2 && Cpu.IME == 0) {
            Cpu.IME_PENDING = 1;
        }
        else if (Cpu.IME_PENDING == 1 && Cpu.IME == 0) {
            Cpu.IME_PENDING = 0;
            Cpu.IME = 1;
        }
        

        // Cpu.prefix == 0 pois aparentemente precisa evitar que ocorra interrupcao 
        // bem no meio do 0xCB prefix, pois pode bagunçar tudo ao voltar da interrupção 
        // exemplo: opcode 0xCB ... ocorre a interrupcao ... e o primeiro opcode XX do interruption handler
        //          sera executado como CB XX (esse demorou para eu achar o problema)
        if (Cpu.prefix == 0 && Cpu.IME == 1 && ((IE & IF) > 0)) {
            Cpu.DI();
            
            Cpu.tc += 8; // Two wait states are executed (2 M-cycles pass while nothing happens; presumably the CPU is executing nops during this time).

            //Cpu.PUSH((Cpu.RPC >> 8) & 0xff);
            //Cpu.PUSH(Cpu.RPC & 0xff);
            Cpu.tc += 8; // The current value of the PC register is pushed onto the stack, consuming 2 more M-cycles.

            if ((IF & (1 << IRQ_BIT_VBLANK)) > 0) {
                setBitIF(IRQ_BIT_VBLANK, 0);
                Cpu.RST(0x40);
            }
            else if ((IF & (1 << IRQ_BIT_LCD)) > 0) {
                setBitIF(IRQ_BIT_LCD, 0);
                Cpu.RST(0x48);
            }
            else if ((IF & (1 << IRQ_BIT_TIMER)) > 0) {
                setBitIF(IRQ_BIT_TIMER, 0);
                Cpu.RST(0x50);
            }
            else if ((IF & (1 << IRQ_BIT_SERIAL)) > 0) {
                setBitIF(IRQ_BIT_SERIAL, 0);
                Cpu.RST(0x58);
            }
            else if ((IF & (1 << IRQ_BIT_JOYPAD)) > 0) {
                setBitIF(IRQ_BIT_JOYPAD, 0);
                Cpu.RST(0x60);
            }

            Cpu.tc += 4; //The PC register is set to the address of the handler (one of: $40, $48, $50, $58, $60). This consumes one last M-cycle.

            Cpu.halted = false;
      }


    }
    
}
