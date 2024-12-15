public class Bus {
    
    /**
     * 0000-3FFF	16 KiB ROM bank 00	From cartridge, usually a fixed bank
     * 4000-7FFF	16 KiB ROM Bank 01–NN	From cartridge, switchable bank via mapper (if any)
     * 8000-9FFF	8 KiB Video RAM (VRAM)	In CGB mode, switchable bank 0/1
     * A000-BFFF	8 KiB External RAM	From cartridge, switchable bank if any
     * C000-CFFF	4 KiB Work RAM (WRAM)	
     * D000-DFFF	4 KiB Work RAM (WRAM)	In CGB mode, switchable bank 1–7
     * E000-FDFF	Echo RAM (mirror of C000–DDFF)	Nintendo says use of this area is prohibited.
     * FE00-FE9F	Object attribute memory (OAM)	
     * FEA0-FEFF	Not Usable	Nintendo says use of this area is prohibited.
     * FF00-FF7F	I/O Registers	
     * FF80-FFFE	High RAM (HRAM)	
     * FFFF-FFFF	Interrupt Enable register (IE)	     * 
     */

    //public static int[] rom = new int[0x8000];
    public static int[] rom = new int[0xfffff];
    public static int[] ram = new int[0x10000];
    
    public static int romBank = 1;

    public static int read(int addr) {
        if (addr >= 0x0000 && addr <= 0x3FFF) return rom[addr]; // rom
        else if (addr >= 0x4000 && addr <= 0x7FFF) return rom[addr + (romBank - 1) * 0x4000]; // rom test mbc1

        else if (addr >= 0x8000 && addr <= 0x9FFF) return Ppu.vram[addr]; // vram
        else if (addr >= 0xA000 && addr <= 0xBFFF) return ram[addr]; //	8 KiB External RAM)
        else if (addr >= 0xC000 && addr <= 0xDFFF) return ram[addr]; // ram
        else if (addr >= 0xE000 && addr <= 0xFDFF) return ram[addr - 0x2000]; // echo ram
        //else if (addr >= 0xFEA0 && addr <= 0xFEFF) return ram[addr]; //	Not Usable	Nintendo says use of this area is prohibited.
        else if (addr >= 0xFF00 && addr <= 0xFF7F) {
            switch (addr) {
                case 0xFF00: 
                    if ((Joypad.JOYP & 0x10) == 0) {
                        return (Joypad.JOYP_DPAD & 0x0f) | (Joypad.JOYP & 0xf0);
                    }
                    if ((Joypad.JOYP & 0x20) == 0) {
                        return (Joypad.JOYP_BUTT & 0x0f) | (Joypad.JOYP & 0xf0);
                    }
                    return Joypad.JOYP; // P1/JOYP register

                // Timer
                case 0xFF04: return Timer.DIV;  //FF04 — DIV: Divider register
                case 0xFF05: return Timer.TIMA; //FF05 — TIMA: Timer counter
                case 0xFF06: return Timer.TMA;  //FF06 — TMA: Timer modulo
                case 0xFF07: return Timer.TAC;  //FF07 — TAC: Timer control

                // LCD
                case 0xFF40: return Ppu.LCDC; // LCDC	LCD control	R/W	All
                case 0xFF41: return Ppu.STAT; // STAT	LCD status	Mixed	All
                case 0xFF42: return Ppu.SCY;  // SCY	Viewport Y position	R/W	All
                case 0xFF43: return Ppu.SCX;  // SCX	Viewport X position	R/W	All
                case 0xFF44: return Ppu.LY;   // LY	LCD Y coordinate	R	All
                case 0xFF45: return Ppu.LYC;  // LYC	LY compare	R/W	All
                case 0xFF46: return Dma.DMA;  // DMA	OAM DMA source address & start	R/W	All
                case 0xFF47: return Ppu.BGP;  // GP	BG palette data	R/W	DMG
                case 0xFF48: return Ppu.OBP0; // OBP0	OBJ palette 0 data	R/W	DMG
                case 0xFF49: return Ppu.OBP1; // OBP1	OBJ palette 1 data	R/W	DMG
                case 0xFF4A: return Ppu.WY;   // WY	Window Y position	R/W	All
                case 0xFF4B: return Ppu.WX;   // WX	Window X position plus 7	R/W	All       
                
                // APU
                case 0xFF26: return Apu.NR52; // FF26 — NR52: Audio master control
                case 0xFF25: return Apu.NR51; // FF25 — NR51: Sound panning
                case 0xFF24: return Apu.NR50; // FF24 — NR50: Master volume & VIN panning
                case 0xFF10: return Apu.NR10; // FF10 — NR10: Channel 1 sweep
                case 0xFF11: return Apu.NR11; // FF11 — NR11: Channel 1 length timer & duty cycle
                case 0xFF12: return Apu.NR12; // FF12 — NR12: Channel 1 volume & envelope
                case 0xFF13: return Apu.NR13; // FF13 — NR13: Channel 1 period low [write-only]
                case 0xFF14: return Apu.NR14; // FF14 — NR14: Channel 1 period high & control
                case 0xFF16: return Apu.NR21; //NR21 ($FF16) → NR11
                case 0xFF17: return Apu.NR22; //NR22 ($FF17) → NR12
                case 0xFF18: return Apu.NR23; //NR23 ($FF18) → NR13
                case 0xFF19: return Apu.NR24; //NR24 ($FF19) → NR14
                case 0xFF1A: return Apu.NR30; // FF1A — NR30: Channel 3 DAC enable
                case 0xFF1B: return Apu.NR31; // FF1B — NR31: Channel 3 length timer [write-only]
                case 0xFF1C: return Apu.NR32; // FF1C — NR32: Channel 3 output level
                case 0xFF1D: return Apu.NR33; // FF1D — NR33: Channel 3 period low [write-only]
                case 0xFF1E: return Apu.NR34; // FF1E — NR34: Channel 3 period high & control
                case 0xFF20: return Apu.NR41; // FF20 — NR41: Channel 4 length timer [write-only]
                case 0xFF21: return Apu.NR42; // FF21 — NR42: Channel 4 volume & envelope
                case 0xFF22: return Apu.NR43; // FF22 — NR43: Channel 4 frequency & randomness
                case 0xFF23: return Apu.NR44; // FF23 — NR44: Channel 4 control
                case 0xFF30: return Apu.WAV_PATTERN[0]; //FF30–FF3F — Wave pattern RAM
                case 0xFF31: return Apu.WAV_PATTERN[1]; //FF30–FF3F — Wave pattern RAM
                case 0xFF32: return Apu.WAV_PATTERN[2]; //FF30–FF3F — Wave pattern RAM
                case 0xFF33: return Apu.WAV_PATTERN[3]; //FF30–FF3F — Wave pattern RAM
                case 0xFF34: return Apu.WAV_PATTERN[4]; //FF30–FF3F — Wave pattern RAM
                case 0xFF35: return Apu.WAV_PATTERN[5]; //FF30–FF3F — Wave pattern RAM
                case 0xFF36: return Apu.WAV_PATTERN[6]; //FF30–FF3F — Wave pattern RAM
                case 0xFF37: return Apu.WAV_PATTERN[7]; //FF30–FF3F — Wave pattern RAM
                case 0xFF38: return Apu.WAV_PATTERN[8]; //FF30–FF3F — Wave pattern RAM
                case 0xFF39: return Apu.WAV_PATTERN[9]; //FF30–FF3F — Wave pattern RAM
                case 0xFF3A: return Apu.WAV_PATTERN[10]; //FF30–FF3F — Wave pattern RAM
                case 0xFF3B: return Apu.WAV_PATTERN[11]; //FF30–FF3F — Wave pattern RAM
                case 0xFF3C: return Apu.WAV_PATTERN[12]; //FF30–FF3F — Wave pattern RAM
                case 0xFF3D: return Apu.WAV_PATTERN[13]; //FF30–FF3F — Wave pattern RAM
                case 0xFF3E: return Apu.WAV_PATTERN[14]; //FF30–FF3F — Wave pattern RAM
                case 0xFF3F: return Apu.WAV_PATTERN[15]; //FF30–FF3F — Wave pattern RAM
                                          
            }
        }
        else if (addr >= 0xFE00 && addr <= 0xFE9F) return Ppu.vram[addr]; // oam
        else if (addr >= 0xFF80 && addr <= 0xFFFE) {
            return ram[addr]; // hram
        }
        
        else if (addr == 0xFF0F) return Irq.IF; // FF0F — IF: Interrupt flag
        else if (addr == 0xFFFF) return Irq.IE; // FFFF — IE: Interrupt enable

        else {

            //throw new RuntimeException("invalid read address " + addr + " !");
            //System.out.println("invalid read address " + addr + " !");
            return ram[addr] & 0xff;

        }

        return 0;
    }

    public static void write(int addr, int value) {
        //if (addr >= 0x0000 && addr <= 0x7FFF) {
        //    rom[addr] = value; // rom read only
        //}

        if (addr >= 0x2000 && addr <= 0x3FFF) { // select ROM Bank Number
            romBank = value & 0x1f;
        }
        else if (addr >= 0x8000 && addr <= 0x9FFF) {
            Ppu.vram[addr] = value; // vram
        }
        else if (addr >= 0xA000 && addr <= 0xBFFF) ram[addr] = value; // 8 KiB External RAM)
        else if (addr >= 0xC000 && addr <= 0xDFFF) ram[addr] = value; // ram
        else if (addr >= 0xE000 && addr <= 0xFDFF) ram[addr - 0x2000] = value; // echo ram
        //else if (addr >= 0xFEA0 && addr <= 0xFEFF) ram[addr] = value; //	Not Usable	Nintendo says use of this area is prohibited.
        else if (addr >= 0xFF00 && addr <= 0xFF7F) {
            switch (addr) {
                case 0xFF00: Joypad.JOYP = (Joypad.JOYP & 0x0f) | (value & 0xf0); break; // P1/JOYP register

                // Timer
                case 0xFF04: Timer.DIV = value; break;  //FF04 — DIV: Divider register
                case 0xFF05: Timer.TIMA = value; break; //FF05 — TIMA: Timer counter
                case 0xFF06: Timer.TMA = value; break;  //FF06 — TMA: Timer modulo
                case 0xFF07: Timer.TAC = value; break;  //FF07 — TAC: Timer control

                // LCD
                case 0xFF40: Ppu.LCDC = value; break; // LCDC	LCD control	R/W	All
                case 0xFF41: Ppu.STAT = value; break; // STAT	LCD status	Mixed	All
                case 0xFF42: Ppu.SCY = value; break;  // SCY	Viewport Y position	R/W	All
                case 0xFF43: Ppu.SCX = value; break;  // SCX	Viewport X position	R/W	All
                //case 0xFF44: Ppu.LY = value; break;   // LY	LCD Y coordinate	R	All
                case 0xFF45: Ppu.LYC = value; break;  // LYC	LY compare	R/W	All
                case 0xFF46: Dma.setDMA(value); break;  // DMA	OAM DMA source address & start	R/W	All
                case 0xFF47: Ppu.BGP = value; break;  // GP	BG palette data	R/W	DMG
                case 0xFF48: Ppu.OBP0 = value; break; // OBP0	OBJ palette 0 data	R/W	DMG
                case 0xFF49: Ppu.OBP1 = value; break; // OBP1	OBJ palette 1 data	R/W	DMG
                case 0xFF4A: Ppu.WY = value; break;   // WY	Window Y position	R/W	All
                case 0xFF4B: Ppu.WX = value; break;   // WX	Window X position plus 7	R/W	All   
                
                // APU
                case 0xFF26: Apu.NR52 = value; break; // FF26 — NR52: Audio master control
                case 0xFF25: Apu.NR51 = value; break; // FF25 — NR51: Sound panning
                case 0xFF24: Apu.NR50 = value; break; // FF24 — NR50: Master volume & VIN panning
                case 0xFF10: Apu.setNR10(value); break; //FF10 — NR10: Channel 1 sweep
                case 0xFF11: Apu.NR11 = value; break; //FF11 — NR11: Channel 1 length timer & duty cycle
                case 0xFF12: Apu.setNR12(value); break; //FF12 — NR12: Channel 1 volume & envelope
                case 0xFF13: Apu.NR13 = value; break; //FF13 — NR13: Channel 1 period low [write-only]
                case 0xFF14: Apu.setNR14(value); break; //FF14 — NR14: Channel 1 period high & control
                case 0xFF16: Apu.NR21 = value; break; //NR21 ($FF16) → NR11
                case 0xFF17: Apu.setNR22(value); break; //NR22 ($FF17) → NR12
                case 0xFF18: Apu.NR23 = value; break; //NR23 ($FF18) → NR13
                case 0xFF19: Apu.setNR24(value); break; //NR24 ($FF19) → NR14
                case 0xFF1A: Apu.NR30 = value; break; //FF1A — NR30: Channel 3 DAC enable
                case 0xFF1B: Apu.NR31 = value; break; //FF1B — NR31: Channel 3 length timer [write-only]
                case 0xFF1C: Apu.NR32 = value; break; //FF1C — NR32: Channel 3 output level
                case 0xFF1D: Apu.NR33 = value; break; //FF1D — NR33: Channel 3 period low [write-only]
                case 0xFF1E: Apu.setNR34(value); break; //FF1E — NR34: Channel 3 period high & control
                case 0xFF20: Apu.NR41 = value; break; //FF20 — NR41: Channel 4 length timer [write-only]
                case 0xFF21: Apu.setNR42(value); break; //FF21 — NR42: Channel 4 volume & envelope
                case 0xFF22: Apu.NR43 = value; break; //FF22 — NR43: Channel 4 frequency & randomness
                case 0xFF23: Apu.setNR44(value); break; //FF23 — NR44: Channel 4 control
                case 0xFF30: Apu.WAV_PATTERN[0] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF31: Apu.WAV_PATTERN[1] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF32: Apu.WAV_PATTERN[2] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF33: Apu.WAV_PATTERN[3] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF34: Apu.WAV_PATTERN[4] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF35: Apu.WAV_PATTERN[5] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF36: Apu.WAV_PATTERN[6] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF37: Apu.WAV_PATTERN[7] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF38: Apu.WAV_PATTERN[8] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF39: Apu.WAV_PATTERN[9] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF3A: Apu.WAV_PATTERN[10] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF3B: Apu.WAV_PATTERN[11] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF3C: Apu.WAV_PATTERN[12] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF3D: Apu.WAV_PATTERN[13] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF3E: Apu.WAV_PATTERN[14] = value; break; //FF30–FF3F — Wave pattern RAM
                case 0xFF3F: Apu.WAV_PATTERN[15] = value; break; //FF30–FF3F — Wave pattern RAM
                
            }
        }
        else if (addr >= 0xFE00 && addr <= 0xFE9F) {
            Ppu.vram[addr] = value; // oam
        }
        else if (addr >= 0xFF80 && addr <= 0xFFFE) {
            ram[addr] = value; // hram
        }
        else if (addr == 0xFF0F) {
            Irq.IF = value; // FF0F — IF: Interrupt flag
        }
        else if (addr == 0xFFFF) {
            Irq.IE = value; // FFFF — IE: Interrupt enable
        }

        else {
            //System.out.println("invalid write address " + addr + " !");
            //ram[addr] = value & 0xff;
            //throw new RuntimeException("invalid write address " + addr + " !");
        }
    }

}
