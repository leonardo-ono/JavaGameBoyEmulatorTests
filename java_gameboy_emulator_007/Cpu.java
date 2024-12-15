public class Cpu {

    public static final int A = 0;
    public static final int B = 1;
    public static final int C = 2;
    public static final int D = 3;
    public static final int E = 4;
    public static final int H = 5;
    public static final int L = 6;
    public static final int AF = 7;
    public static final int BC = 8;
    public static final int DE = 9;
    public static final int HL = 10;
    public static final int SP = 11;
    public static final int _C_ = 12;
    public static final int _BC_ = 13;
    public static final int _DE_ = 14;
    public static final int _HL_ = 15;
    public static final int _HLi_ = 16;
    public static final int _HLd_ = 17;
    public static final int n16 = 18;
    public static final int a16 = 19;
    public static final int _a16_a = 20;
    public static final int _a16_sp = 21;
    public static final int e8 = 22;
    public static final int n8 = 23;
    public static final int _a8_ = 24;
    public static final int Z = 25;
    public static final int NZ = 26;
    public static final int NC = 27;

    public static int RA = 0;
    public static int RB = 0;
    public static int RC = 0;
    public static int RD = 0;
    public static int RE = 0;
    public static int RH = 0;
    public static int RL = 0;
    public static int RF = 0xb0;
    public static int RSP = 0xfffe;
    public static int RPC = 0x0100;

    // https://www.reddit.com/r/EmuDev/comments/7zx1om/gb_value_of_interrupt_master_enable_ime_at_startup/?rdt=40486
    public static int IME = 0; // interrupt master enable/disable
    
    public static boolean halted = false;
    public static boolean running = true;

    public static int tc = 0; // t-cycles
    
    public static String getTraceInfo() {
        int tmpOpcode = read(RPC - 1);
        int curPC = RPC;
        if (tmpOpcode == 0xCB) {
            curPC -= 1;
        }

        String addrStr = "0000" + Integer.toHexString(curPC);
        addrStr = addrStr.substring(addrStr.length() - 4, addrStr.length()).toUpperCase();
        String spStr = "0000" + Integer.toHexString(RSP);
        spStr = spStr.substring(spStr.length() - 4, spStr.length()).toUpperCase();
        
        String aStr = "00" + Integer.toHexString(RA);
        aStr = aStr.substring(aStr.length() - 2, aStr.length()).toUpperCase();
        String fStr = "00" + Integer.toHexString(RF);
        fStr = fStr.substring(fStr.length() - 2, fStr.length()).toUpperCase();
        String bStr = "00" + Integer.toHexString(RB);
        bStr = bStr.substring(bStr.length() - 2, bStr.length()).toUpperCase();
        String cStr = "00" + Integer.toHexString(RC);
        cStr = cStr.substring(cStr.length() - 2, cStr.length()).toUpperCase();
        String dStr = "00" + Integer.toHexString(RD);
        dStr = dStr.substring(dStr.length() - 2, dStr.length()).toUpperCase();
        String eStr = "00" + Integer.toHexString(RE);
        eStr = eStr.substring(eStr.length() - 2, eStr.length()).toUpperCase();
        String hStr = "00" + Integer.toHexString(RH);
        hStr = hStr.substring(hStr.length() - 2, hStr.length()).toUpperCase();
        String lStr = "00" + Integer.toHexString(RL);
        lStr = lStr.substring(lStr.length() - 2, lStr.length()).toUpperCase();
        
        //ROM00:0177BC=0013 DE=00D8 HL=0266 AF=01B0 SP=DFFE PC=0177
        return "ROM00:" + addrStr + "BC=" + bStr + cStr + " DE=" + dStr + eStr + " HL=" + hStr + lStr + " AF=" + aStr + fStr + " SP=" + spStr + " PC=" + addrStr;
    }

    public static void printRegs() {
        System.out.println("--- regs ---");
        System.out.printf("B=%s C=%s \n", Integer.toHexString(RB), Integer.toHexString(RC));
        System.out.printf("D=%s E=%s \n", Integer.toHexString(RD), Integer.toHexString(RE));
        System.out.printf("H=%s L=%s \n", Integer.toHexString(RH), Integer.toHexString(RL));
        System.out.printf("A=%s F=%s \n", Integer.toHexString(RA), Integer.toHexString(RF));
        System.out.printf("SP=%s \n", Integer.toHexString(RSP));
        System.out.printf("PC=%s \n", Integer.toHexString(RPC));
        System.out.println("--- ---");
    }

    public static int read(int addr) {
        return Bus.read(addr);
    }

    public static void write(int addr, int value) {
        Bus.write(addr, value);
    }
    
    public static void INVALID_DEST() {
        throw new RuntimeException("invalide dest op!");
    }

    public static void set(int dest, int value) {
        int tmp = 0;
        switch (dest) {
            case A: RA = value & 0xff; break;
            case B: RB = value & 0xff; break;
            case C: RC = value & 0xff; break;
            case D: RD = value & 0xff; break;
            case E: RE = value & 0xff; break;
            case H: RH = value & 0xff; break;
            case L: RL = value & 0xff; break;
            case AF: RA = (value >> 8) & 0xff; RF = value & 0xf0; break;
            case BC: RB = (value >> 8) & 0xff; RC = value & 0xff; break;
            case DE: RD = (value >> 8) & 0xff; RE = value & 0xff; break;
            case HL: RH = (value >> 8) & 0xff; RL = value & 0xff; break;
            case SP: RSP = value & 0xffff; break;
            case _C_: write(RC + 0xff00, value & 0xff); break;
            case _BC_: write((RB << 8) + RC, value & 0xff); break;
            case _DE_: write((RD << 8) + RE, value & 0xff); break;
            case _HL_: write((RH << 8) + RL, value & 0xff); break;
            case _HLi_: tmp = get(HL); write(tmp, value & 0xff); set(HL, tmp + 1); break;
            case _HLd_: tmp = get(HL); write(tmp, value & 0xff); set(HL, tmp - 1); break;
            case n16: INVALID_DEST(); break;
            case a16: INVALID_DEST(); break;
            case _a16_a: tmp = read(RPC++) + (read(RPC++) << 8); write(tmp, value & 0xff); break; // write 2 bytes -> LD [a16], A
            case _a16_sp: tmp = read(RPC++) + (read(RPC++) << 8); write(tmp, value & 0xff); write(tmp + 1, (value >> 8) & 0xff); break; // write 2 bytes -> LD [a16], SP
            case e8: INVALID_DEST(); break;
            case n8: INVALID_DEST(); break;
            case _a8_: write(read(RPC++) + 0xff00, value & 0xff); break;
            case Z: INVALID_DEST(); break;
            case NZ: INVALID_DEST(); break;
            case NC: INVALID_DEST(); break;   
            default: INVALID_DEST(); break;             
        }
    }

    public static void INVALID_GET_OP() {
        throw new RuntimeException("invalid get op!");
    }

    public static int get(int op) {
        int tmp = 0;
        switch (op) {
            case A: return RA;
            case B: return RB;
            case C: return RC;
            case D: return RD;
            case E: return RE;
            case H: return RH;
            case L: return RL;
            case AF: return (RA << 8) + RF;
            case BC: return (RB << 8) + RC;
            case DE: return (RD << 8) + RE;
            case HL: return (RH << 8) + RL;
            case SP: return RSP;
            case _C_: return read(RC + 0xff00);
            case _BC_: return read((RB << 8) + RC);
            case _DE_: return read((RD << 8) + RE);
            case _HL_: return read((RH << 8) + RL);
            case _HLi_: tmp = get(HL); set(HL, tmp + 1); return read(tmp);
            case _HLd_: tmp = get(HL); set(HL, tmp - 1); return read(tmp);
            case n16: return read(RPC++) + (read(RPC++) << 8);
            case a16: return read(RPC++) + (read(RPC++) << 8);
            case _a16_a: tmp = read(RPC++) + (read(RPC++) << 8); return read(tmp);
            case e8: return (byte) read(RPC++); // TODO ???
            case n8: return read(RPC++);
            case _a8_: return read((read(RPC++) & 0xff) + 0xff00);
            case Z: INVALID_GET_OP(); break;
            case NZ: INVALID_GET_OP(); break;
            case NC: INVALID_GET_OP(); break;     
            default: INVALID_GET_OP(); break;    
        }
        return 0;
    }
    
    public static int prefix = 0x0000;
    public static String lastMnemonic = "";

    public static void executeNextInstr() {
        int opcode = 0;

        if (!halted) {
            opcode = read(RPC++);
        }

        // bits oooooo_11111_22222_zznnhhcc_ttt
        switch (prefix + opcode) {
            case 0x0000: setFlags("----"); NOP(); tc += 4; lastMnemonic = "NOP"; break; 
            case 0x0001: setFlags("----"); LD(BC, n16); tc += 12; lastMnemonic = "LD BC, n16"; break; 
            case 0x0002: setFlags("----"); LD(_BC_, A); tc += 8; lastMnemonic = "LD [BC], A"; break; 
            case 0x0003: setFlags("----"); INC(BC); tc += 8; lastMnemonic = "INC BC"; break; 
            case 0x0004: setFlags("Z0H-"); INC(B); tc += 4; lastMnemonic = "INC B"; break; 
            case 0x0005: setFlags("Z1H-"); DEC(B); tc += 4; lastMnemonic = "DEC B"; break; 
            case 0x0006: setFlags("----"); LD(B, n8); tc += 8; lastMnemonic = "LD B, n8"; break; 
            case 0x0007: setFlags("000C"); RLCA(); tc += 4; lastMnemonic = "RLCA"; break; 
            case 0x0008: setFlags("----"); LD(_a16_sp, SP); tc += 20; lastMnemonic = "LD [a16], SP"; break; 
            case 0x0009: setFlags("-0HC"); ADD16(HL, BC); tc += 8; lastMnemonic = "ADD HL, BC"; break; 
            case 0x000a: setFlags("----"); LD(A, _BC_); tc += 8; lastMnemonic = "LD A, [BC]"; break; 
            case 0x000b: setFlags("----"); DEC(BC); tc += 8; lastMnemonic = "DEC BC"; break; 
            case 0x000c: setFlags("Z0H-"); INC(C); tc += 4; lastMnemonic = "INC C"; break; 
            case 0x000d: setFlags("Z1H-"); DEC(C); tc += 4; lastMnemonic = "DEC C"; break; 
            case 0x000e: setFlags("----"); LD(C, n8); tc += 8; lastMnemonic = "LD C, n8"; break; 
            case 0x000f: setFlags("000C"); RRCA(); tc += 4; lastMnemonic = "RRCA"; break; 
            case 0x0010: setFlags("----"); STOP(n8); tc += 4; lastMnemonic = "STOP n8"; break; 
            case 0x0011: setFlags("----"); LD(DE, n16); tc += 12; lastMnemonic = "LD DE, n16"; break; 
            case 0x0012: setFlags("----"); LD(_DE_, A); tc += 8; lastMnemonic = "LD [DE], A"; break; 
            case 0x0013: setFlags("----"); INC(DE); tc += 8; lastMnemonic = "INC DE"; break; 
            case 0x0014: setFlags("Z0H-"); INC(D); tc += 4; lastMnemonic = "INC D"; break; 
            case 0x0015: setFlags("Z1H-"); DEC(D); tc += 4; lastMnemonic = "DEC D"; break; 
            case 0x0016: setFlags("----"); LD(D, n8); tc += 8; lastMnemonic = "LD D, n8"; break; 
            case 0x0017: setFlags("000C"); RLA(); tc += 4; lastMnemonic = "RLA"; break; 
            case 0x0018: setFlags("----"); JR(e8); tc += 12; lastMnemonic = "JR e8"; break; 
            case 0x0019: setFlags("-0HC"); ADD16(HL, DE); tc += 8; lastMnemonic = "ADD HL, DE"; break; 
            case 0x001a: setFlags("----"); LD(A, _DE_); tc += 8; lastMnemonic = "LD A, [DE]"; break; 
            case 0x001b: setFlags("----"); DEC(DE); tc += 8; lastMnemonic = "DEC DE"; break; 
            case 0x001c: setFlags("Z0H-"); INC(E); tc += 4; lastMnemonic = "INC E"; break; 
            case 0x001d: setFlags("Z1H-"); DEC(E); tc += 4; lastMnemonic = "DEC E"; break; 
            case 0x001e: setFlags("----"); LD(E, n8); tc += 8; lastMnemonic = "LD E, n8"; break; 
            case 0x001f: setFlags("000C"); RRA(); tc += 4; lastMnemonic = "RRA"; break; 
            case 0x0020: setFlags("----"); JR(NZ, e8); tc += 8; lastMnemonic = "JR NZ, e8"; break; // obs: tc += 12 se cond==true, tc += 8 caso contrario
            case 0x0021: setFlags("----"); LD(HL, n16); tc += 12; lastMnemonic = "LD HL, n16"; break; 
            case 0x0022: setFlags("----"); LD(_HLi_, A); tc += 8; lastMnemonic = "LD [HLi], A"; break; 
            case 0x0023: setFlags("----"); INC(HL); tc += 8; lastMnemonic = "INC HL"; break; 
            case 0x0024: setFlags("Z0H-"); INC(H); tc += 4; lastMnemonic = "INC H"; break; 
            case 0x0025: setFlags("Z1H-"); DEC(H); tc += 4; lastMnemonic = "DEC H"; break; 
            case 0x0026: setFlags("----"); LD(H, n8); tc += 8; lastMnemonic = "LD H, n8"; break; 
            case 0x0027: DAA(); tc += 4; lastMnemonic = "DAA"; break;  // setFlags("Z-0C");
            case 0x0028: setFlags("----"); JR(Z, e8); tc += 8; lastMnemonic = "JR Z, e8"; break;  // obs: tc += 12 se cond==true, tc += 8 caso contrario
            case 0x0029: setFlags("-0HC"); ADD16(HL, HL); tc += 8; lastMnemonic = "ADD HL, HL"; break; 
            case 0x002a: setFlags("----"); LD(A, _HLi_); tc += 8; lastMnemonic = "LD A, [HLi]"; break; 
            case 0x002b: setFlags("----"); DEC(HL); tc += 8; lastMnemonic = "DEC HL"; break; 
            case 0x002c: setFlags("Z0H-"); INC(L); tc += 4; lastMnemonic = "INC L"; break; 
            case 0x002d: setFlags("Z1H-"); DEC(L); tc += 4; lastMnemonic = "DEC L"; break; 
            case 0x002e: setFlags("----"); LD(L, n8); tc += 8; lastMnemonic = "LD L, n8"; break; 
            case 0x002f: setFlags("-11-"); CPL(); tc += 4; lastMnemonic = "CPL"; break; 
            case 0x0030: setFlags("----"); JR(NC, e8); tc += 8; lastMnemonic = "JR NC, e8"; break;  // obs: tc += 12 se cond==true, tc += 8 caso contrario
            case 0x0031: setFlags("----"); LD(SP, n16); tc += 12; lastMnemonic = "LD SP, n16"; break; 
            case 0x0032: setFlags("----"); LD(_HLd_, A); tc += 8; lastMnemonic = "LD [HLd], A"; break; 
            case 0x0033: setFlags("----"); INC(SP); tc += 8; lastMnemonic = "INC SP"; break; 
            case 0x0034: setFlags("Z0H-"); INC(_HL_); tc += 12; lastMnemonic = "INC [HL]"; break; 
            case 0x0035: setFlags("Z1H-"); DEC(_HL_); tc += 12; lastMnemonic = "DEC [HL]"; break; 
            case 0x0036: setFlags("----"); LD(_HL_, n8); tc += 12; lastMnemonic = "LD [HL], n8"; break; 
            case 0x0037: setFlags("-001");  tc += 4; lastMnemonic = "SCF"; break; 
            case 0x0038: setFlags("----"); JR(C, e8); tc += 8; lastMnemonic = "JR C, e8"; break; // obs: tc += 12 se cond==true, tc += 8 caso contrario
            case 0x0039: setFlags("-0HC"); ADD16(HL, SP); tc += 8; lastMnemonic = "ADD HL, SP"; break; 
            case 0x003a: setFlags("----"); LD(A, _HLd_); tc += 8; lastMnemonic = "LD A, [HLd]"; break; 
            case 0x003b: setFlags("----"); DEC(SP); tc += 8; lastMnemonic = "DEC SP"; break; 
            case 0x003c: setFlags("Z0H-"); INC(A); tc += 4; lastMnemonic = "INC A"; break; 
            case 0x003d: setFlags("Z1H-"); DEC(A); tc += 4; lastMnemonic = "DEC A"; break; 
            case 0x003e: setFlags("----"); LD(A, n8); tc += 8; lastMnemonic = "LD A, n8"; break; 
            case 0x003f: setFlags("-00C"); CCF(); tc += 4; lastMnemonic = "CCF"; break; 
            case 0x0040: setFlags("----"); LD(B, B); tc += 4; lastMnemonic = "LD B, B"; break; 
            case 0x0041: setFlags("----"); LD(B, C); tc += 4; lastMnemonic = "LD B, C"; break; 
            case 0x0042: setFlags("----"); LD(B, D); tc += 4; lastMnemonic = "LD B, D"; break; 
            case 0x0043: setFlags("----"); LD(B, E); tc += 4; lastMnemonic = "LD B, E"; break; 
            case 0x0044: setFlags("----"); LD(B, H); tc += 4; lastMnemonic = "LD B, H"; break; 
            case 0x0045: setFlags("----"); LD(B, L); tc += 4; lastMnemonic = "LD B, L"; break; 
            case 0x0046: setFlags("----"); LD(B, _HL_); tc += 8; lastMnemonic = "LD B, [HL]"; break; 
            case 0x0047: setFlags("----"); LD(B, A); tc += 4; lastMnemonic = "LD B, A"; break; 
            case 0x0048: setFlags("----"); LD(C, B); tc += 4; lastMnemonic = "LD C, B"; break; 
            case 0x0049: setFlags("----"); LD(C, C); tc += 4; lastMnemonic = "LD C, C"; break; 
            case 0x004a: setFlags("----"); LD(C, D); tc += 4; lastMnemonic = "LD C, D"; break; 
            case 0x004b: setFlags("----"); LD(C, E); tc += 4; lastMnemonic = "LD C, E"; break; 
            case 0x004c: setFlags("----"); LD(C, H); tc += 4; lastMnemonic = "LD C, H"; break; 
            case 0x004d: setFlags("----"); LD(C, L); tc += 4; lastMnemonic = "LD C, L"; break; 
            case 0x004e: setFlags("----"); LD(C, _HL_); tc += 8; lastMnemonic = "LD C, [HL]"; break; 
            case 0x004f: setFlags("----"); LD(C, A); tc += 4; lastMnemonic = "LD C, A"; break; 
            case 0x0050: setFlags("----"); LD(D, B); tc += 4; lastMnemonic = "LD D, B"; break; 
            case 0x0051: setFlags("----"); LD(D, C); tc += 4; lastMnemonic = "LD D, C"; break; 
            case 0x0052: setFlags("----"); LD(D, D); tc += 4; lastMnemonic = "LD D, D"; break; 
            case 0x0053: setFlags("----"); LD(D, E); tc += 4; lastMnemonic = "LD D, E"; break; 
            case 0x0054: setFlags("----"); LD(D, H); tc += 4; lastMnemonic = "LD D, H"; break; 
            case 0x0055: setFlags("----"); LD(D, L); tc += 4; lastMnemonic = "LD D, L"; break; 
            case 0x0056: setFlags("----"); LD(D, _HL_); tc += 8; lastMnemonic = "LD D, [HL]"; break; 
            case 0x0057: setFlags("----"); LD(D, A); tc += 4; lastMnemonic = "LD D, A"; break; 
            case 0x0058: setFlags("----"); LD(E, B); tc += 4; lastMnemonic = "LD E, B"; break; 
            case 0x0059: setFlags("----"); LD(E, C); tc += 4; lastMnemonic = "LD E, C"; break; 
            case 0x005a: setFlags("----"); LD(E, D); tc += 4; lastMnemonic = "LD E, D"; break; 
            case 0x005b: setFlags("----"); LD(E, E); tc += 4; lastMnemonic = "LD E, E"; break; 
            case 0x005c: setFlags("----"); LD(E, H); tc += 4; lastMnemonic = "LD E, H"; break; 
            case 0x005d: setFlags("----"); LD(E, L); tc += 4; lastMnemonic = "LD E, L"; break; 
            case 0x005e: setFlags("----"); LD(E, _HL_); tc += 8; lastMnemonic = "LD E, [HL]"; break; 
            case 0x005f: setFlags("----"); LD(E, A); tc += 4; lastMnemonic = "LD E, A"; break; 
            case 0x0060: setFlags("----"); LD(H, B); tc += 4; lastMnemonic = "LD H, B"; break; 
            case 0x0061: setFlags("----"); LD(H, C); tc += 4; lastMnemonic = "LD H, C"; break; 
            case 0x0062: setFlags("----"); LD(H, D); tc += 4; lastMnemonic = "LD H, D"; break; 
            case 0x0063: setFlags("----"); LD(H, E); tc += 4; lastMnemonic = "LD H, E"; break; 
            case 0x0064: setFlags("----"); LD(H, H); tc += 4; lastMnemonic = "LD H, H"; break; 
            case 0x0065: setFlags("----"); LD(H, L); tc += 4; lastMnemonic = "LD H, L"; break; 
            case 0x0066: setFlags("----"); LD(H, _HL_); tc += 8; lastMnemonic = "LD H, [HL]"; break; 
            case 0x0067: setFlags("----"); LD(H, A); tc += 4; lastMnemonic = "LD H, A"; break; 
            case 0x0068: setFlags("----"); LD(L, B); tc += 4; lastMnemonic = "LD L, B"; break; 
            case 0x0069: setFlags("----"); LD(L, C); tc += 4; lastMnemonic = "LD L, C"; break; 
            case 0x006a: setFlags("----"); LD(L, D); tc += 4; lastMnemonic = "LD L, D"; break; 
            case 0x006b: setFlags("----"); LD(L, E); tc += 4; lastMnemonic = "LD L, E"; break; 
            case 0x006c: setFlags("----"); LD(L, H); tc += 4; lastMnemonic = "LD L, H"; break; 
            case 0x006d: setFlags("----"); LD(L, L); tc += 4; lastMnemonic = "LD L, L"; break; 
            case 0x006e: setFlags("----"); LD(L, _HL_); tc += 8; lastMnemonic = "LD L, [HL]"; break; 
            case 0x006f: setFlags("----"); LD(L, A); tc += 4; lastMnemonic = "LD L, A"; break; 
            case 0x0070: setFlags("----"); LD(_HL_, B); tc += 8; lastMnemonic = "LD [HL], B"; break; 
            case 0x0071: setFlags("----"); LD(_HL_, C); tc += 8; lastMnemonic = "LD [HL], C"; break; 
            case 0x0072: setFlags("----"); LD(_HL_, D); tc += 8; lastMnemonic = "LD [HL], D"; break; 
            case 0x0073: setFlags("----"); LD(_HL_, E); tc += 8; lastMnemonic = "LD [HL], E"; break; 
            case 0x0074: setFlags("----"); LD(_HL_, H); tc += 8; lastMnemonic = "LD [HL], H"; break; 
            case 0x0075: setFlags("----"); LD(_HL_, L); tc += 8; lastMnemonic = "LD [HL], L"; break; 
            case 0x0076: setFlags("----"); HALT(); tc += 4; lastMnemonic = "HALT"; break; 
            case 0x0077: setFlags("----"); LD(_HL_, A); tc += 8; lastMnemonic = "LD [HL], A"; break; 
            case 0x0078: setFlags("----"); LD(A, B); tc += 4; lastMnemonic = "LD A, B"; break; 
            case 0x0079: setFlags("----"); LD(A, C); tc += 4; lastMnemonic = "LD A, C"; break; 
            case 0x007a: setFlags("----"); LD(A, D); tc += 4; lastMnemonic = "LD A, D"; break; 
            case 0x007b: setFlags("----"); LD(A, E); tc += 4; lastMnemonic = "LD A, E"; break; 
            case 0x007c: setFlags("----"); LD(A, H); tc += 4; lastMnemonic = "LD A, H"; break; 
            case 0x007d: setFlags("----"); LD(A, L); tc += 4; lastMnemonic = "LD A, L"; break; 
            case 0x007e: setFlags("----"); LD(A, _HL_); tc += 8; lastMnemonic = "LD A, [HL]"; break; 
            case 0x007f: setFlags("----"); LD(A, A); tc += 4; lastMnemonic = "LD A, A"; break; 
            case 0x0080: setFlags("Z0HC"); ADD8(A, B); tc += 4; lastMnemonic = "ADD A, B"; break; 
            case 0x0081: setFlags("Z0HC"); ADD8(A, C); tc += 4; lastMnemonic = "ADD A, C"; break; 
            case 0x0082: setFlags("Z0HC"); ADD8(A, D); tc += 4; lastMnemonic = "ADD A, D"; break; 
            case 0x0083: setFlags("Z0HC"); ADD8(A, E); tc += 4; lastMnemonic = "ADD A, E"; break; 
            case 0x0084: setFlags("Z0HC"); ADD8(A, H); tc += 4; lastMnemonic = "ADD A, H"; break; 
            case 0x0085: setFlags("Z0HC"); ADD8(A, L); tc += 4; lastMnemonic = "ADD A, L"; break; 
            case 0x0086: setFlags("Z0HC"); ADD8(A, _HL_); tc += 8; lastMnemonic = "ADD A, [HL]"; break; 
            case 0x0087: setFlags("Z0HC"); ADD8(A, A); tc += 4; lastMnemonic = "ADD A, A"; break; 
            case 0x0088: setFlags("Z0HC"); ADC(A, B); tc += 4; lastMnemonic = "ADC A, B"; break; 
            case 0x0089: setFlags("Z0HC"); ADC(A, C); tc += 4; lastMnemonic = "ADC A, C"; break; 
            case 0x008a: setFlags("Z0HC"); ADC(A, D); tc += 4; lastMnemonic = "ADC A, D"; break; 
            case 0x008b: setFlags("Z0HC"); ADC(A, E); tc += 4; lastMnemonic = "ADC A, E"; break; 
            case 0x008c: setFlags("Z0HC"); ADC(A, H); tc += 4; lastMnemonic = "ADC A, H"; break; 
            case 0x008d: setFlags("Z0HC"); ADC(A, L); tc += 4; lastMnemonic = "ADC A, L"; break; 
            case 0x008e: setFlags("Z0HC"); ADC(A, _HL_); tc += 8; lastMnemonic = "ADC A, [HL]"; break; 
            case 0x008f: setFlags("Z0HC"); ADC(A, A); tc += 4; lastMnemonic = "ADC A, A"; break; 
            case 0x0090: setFlags("Z1HC"); SUB(A, B); tc += 4; lastMnemonic = "SUB A, B"; break; 
            case 0x0091: setFlags("Z1HC"); SUB(A, C); tc += 4; lastMnemonic = "SUB A, C"; break; 
            case 0x0092: setFlags("Z1HC"); SUB(A, D); tc += 4; lastMnemonic = "SUB A, D"; break; 
            case 0x0093: setFlags("Z1HC"); SUB(A, E); tc += 4; lastMnemonic = "SUB A, E"; break; 
            case 0x0094: setFlags("Z1HC"); SUB(A, H); tc += 4; lastMnemonic = "SUB A, H"; break; 
            case 0x0095: setFlags("Z1HC"); SUB(A, L); tc += 4; lastMnemonic = "SUB A, L"; break; 
            case 0x0096: setFlags("Z1HC"); SUB(A, _HL_); tc += 8; lastMnemonic = "SUB A, [HL]"; break; 
            case 0x0097: setFlags("1100"); SUB(A, A); tc += 4; lastMnemonic = "SUB A, A"; break; 
            case 0x0098: setFlags("Z1HC"); SBC(A, B); tc += 4; lastMnemonic = "SBC A, B"; break; 
            case 0x0099: setFlags("Z1HC"); SBC(A, C); tc += 4; lastMnemonic = "SBC A, C"; break; 
            case 0x009a: setFlags("Z1HC"); SBC(A, D); tc += 4; lastMnemonic = "SBC A, D"; break; 
            case 0x009b: setFlags("Z1HC"); SBC(A, E); tc += 4; lastMnemonic = "SBC A, E"; break; 
            case 0x009c: setFlags("Z1HC"); SBC(A, H); tc += 4; lastMnemonic = "SBC A, H"; break; 
            case 0x009d: setFlags("Z1HC"); SBC(A, L); tc += 4; lastMnemonic = "SBC A, L"; break; 
            case 0x009e: setFlags("Z1HC"); SBC(A, _HL_); tc += 8; lastMnemonic = "SBC A, [HL]"; break; 
            case 0x009f: setFlags("Z1H-"); SBC(A, A); tc += 4; lastMnemonic = "SBC A, A"; break; 
            case 0x00a0: setFlags("Z010"); AND(A, B); tc += 4; lastMnemonic = "AND A, B"; break; 
            case 0x00a1: setFlags("Z010"); AND(A, C); tc += 4; lastMnemonic = "AND A, C"; break; 
            case 0x00a2: setFlags("Z010"); AND(A, D); tc += 4; lastMnemonic = "AND A, D"; break; 
            case 0x00a3: setFlags("Z010"); AND(A, E); tc += 4; lastMnemonic = "AND A, E"; break; 
            case 0x00a4: setFlags("Z010"); AND(A, H); tc += 4; lastMnemonic = "AND A, H"; break; 
            case 0x00a5: setFlags("Z010"); AND(A, L); tc += 4; lastMnemonic = "AND A, L"; break; 
            case 0x00a6: setFlags("Z010"); AND(A, _HL_); tc += 8; lastMnemonic = "AND A, [HL]"; break; 
            case 0x00a7: setFlags("Z010"); AND(A, A); tc += 4; lastMnemonic = "AND A, A"; break; 
            case 0x00a8: setFlags("Z000"); XOR(A, B); tc += 4; lastMnemonic = "XOR A, B"; break; 
            case 0x00a9: setFlags("Z000"); XOR(A, C); tc += 4; lastMnemonic = "XOR A, C"; break; 
            case 0x00aa: setFlags("Z000"); XOR(A, D); tc += 4; lastMnemonic = "XOR A, D"; break; 
            case 0x00ab: setFlags("Z000"); XOR(A, E); tc += 4; lastMnemonic = "XOR A, E"; break; 
            case 0x00ac: setFlags("Z000"); XOR(A, H); tc += 4; lastMnemonic = "XOR A, H"; break; 
            case 0x00ad: setFlags("Z000"); XOR(A, L); tc += 4; lastMnemonic = "XOR A, L"; break; 
            case 0x00ae: setFlags("Z000"); XOR(A, _HL_); tc += 8; lastMnemonic = "XOR A, [HL]"; break; 
            case 0x00af: setFlags("1000"); XOR(A, A); tc += 4; lastMnemonic = "XOR A, A"; break; 
            case 0x00b0: setFlags("Z000"); OR(A, B); tc += 4; lastMnemonic = "OR A, B"; break; 
            case 0x00b1: setFlags("Z000"); OR(A, C); tc += 4; lastMnemonic = "OR A, C"; break; 
            case 0x00b2: setFlags("Z000"); OR(A, D); tc += 4; lastMnemonic = "OR A, D"; break; 
            case 0x00b3: setFlags("Z000"); OR(A, E); tc += 4; lastMnemonic = "OR A, E"; break; 
            case 0x00b4: setFlags("Z000"); OR(A, H); tc += 4; lastMnemonic = "OR A, H"; break; 
            case 0x00b5: setFlags("Z000"); OR(A, L); tc += 4; lastMnemonic = "OR A, L"; break; 
            case 0x00b6: setFlags("Z000"); OR(A, _HL_); tc += 8; lastMnemonic = "OR A, [HL]"; break; 
            case 0x00b7: setFlags("Z000"); OR(A, A); tc += 4; lastMnemonic = "OR A, A"; break; 
            case 0x00b8: setFlags("Z1HC"); CP(A, B); tc += 4; lastMnemonic = "CP A, B"; break; 
            case 0x00b9: setFlags("Z1HC"); CP(A, C); tc += 4; lastMnemonic = "CP A, C"; break; 
            case 0x00ba: setFlags("Z1HC"); CP(A, D); tc += 4; lastMnemonic = "CP A, D"; break; 
            case 0x00bb: setFlags("Z1HC"); CP(A, E); tc += 4; lastMnemonic = "CP A, E"; break; 
            case 0x00bc: setFlags("Z1HC"); CP(A, H); tc += 4; lastMnemonic = "CP A, H"; break; 
            case 0x00bd: setFlags("Z1HC"); CP(A, L); tc += 4; lastMnemonic = "CP A, L"; break; 
            case 0x00be: setFlags("Z1HC"); CP(A, _HL_); tc += 8; lastMnemonic = "CP A, [HL]"; break; 
            case 0x00bf: setFlags("1100"); CP(A, A); tc += 4; lastMnemonic = "CP A, A"; break; 
            case 0x00c0: setFlags("----"); RET(NZ); tc += 8; lastMnemonic = "RET NZ"; break;  // obs: tc += 20 se cond==true, tc += 8 caso contrario
            case 0x00c1: setFlags("----"); POP(BC); tc += 12; lastMnemonic = "POP BC"; break; 
            case 0x00c2: setFlags("----"); JP(NZ, a16); tc += 12; lastMnemonic = "JP NZ, a16"; break; // obs: tc += 16 se cond==true, tc += 12 caso contrario 
            case 0x00c3: setFlags("----"); JP(a16); tc += 16; lastMnemonic = "JP a16"; break; 
            case 0x00c4: setFlags("----"); CALL(NZ, a16); tc += 12; lastMnemonic = "CALL NZ, a16"; break; // obs: tc += 24 se cond==true, tc += 12 caso contrario 
            case 0x00c5: setFlags("----"); PUSH(BC); tc += 16; lastMnemonic = "PUSH BC"; break; 
            case 0x00c6: setFlags("Z0HC"); ADD8(A, n8); tc += 8; lastMnemonic = "ADD A, n8"; break; 
            case 0x00c7: setFlags("----"); RST(0x00); tc += 16; lastMnemonic = "RST $00"; break; 
            case 0x00c8: setFlags("----"); RET(Z); tc += 8; lastMnemonic = "RET Z"; break; // obs: tc += 20 se cond==true, tc += 8 caso contrario
            case 0x00c9: setFlags("----"); RET(); tc += 16; lastMnemonic = "RET"; break; 
            case 0x00ca: setFlags("----"); JP(Z, a16); tc += 12; lastMnemonic = "JP Z, a16"; break; // obs: tc += 16 se cond==true, tc += 12 caso contrario 
            case 0x00cb: setFlags("----"); PREFIX(); tc += 0; lastMnemonic = "PREFIX"; break; // tc += 4; obs: aparentemente as instruções com prefixo CB já está adicionada o valor to t-cycle do CB
            case 0x00cc: setFlags("----"); CALL(Z, a16); tc += 12; lastMnemonic = "CALL Z, a16"; break; // obs: tc += 24 se cond==true, tc += 12 caso contrario
            case 0x00cd: setFlags("----"); CALL(a16); tc += 24; lastMnemonic = "CALL a16"; break; 
            case 0x00ce: setFlags("Z0HC"); ADC(A, n8); tc += 8; lastMnemonic = "ADC A, n8"; break; 
            case 0x00cf: setFlags("----"); RST(0x08); tc += 16; lastMnemonic = "RST $08"; break; 
            case 0x00d0: setFlags("----"); RET(NC); tc += 8; lastMnemonic = "RET NC"; break;   // obs: tc += 20 se cond==true, tc += 8 caso contrario
            case 0x00d1: setFlags("----"); POP(DE); tc += 12; lastMnemonic = "POP DE"; break; 
            case 0x00d2: setFlags("----"); JP(NC, a16); tc += 12; lastMnemonic = "JP NC, a16"; break; // obs: tc += 16 se cond==true, tc += 12 caso contrario
            case 0x00d3: setFlags("----"); ILLEGAL(prefix + opcode); tc += 4; lastMnemonic = "ILLEGAL_D3"; break; 
            case 0x00d4: setFlags("----"); CALL(NC, a16); tc += 12; lastMnemonic = "CALL NC, a16"; break; // obs: tc += 24 se cond==true, tc += 12 caso contrario 
            case 0x00d5: setFlags("----"); PUSH(DE); tc += 16; lastMnemonic = "PUSH DE"; break; 
            case 0x00d6: setFlags("Z1HC"); SUB(A, n8); tc += 8; lastMnemonic = "SUB A, n8"; break; 
            case 0x00d7: setFlags("----"); RST(0x10); tc += 16; lastMnemonic = "RST $10"; break; 
            case 0x00d8: setFlags("----"); RET(C); tc += 8; lastMnemonic = "RET C"; break; // obs: tc += 20 se cond==true, tc += 8 caso contrario
            case 0x00d9: setFlags("----"); RETI(); tc += 16; lastMnemonic = "RETI"; break;
            case 0x00da: setFlags("----"); JP(C, a16); tc += 12; lastMnemonic = "JP C, a16"; break; // obs: tc += 16 se cond==true, tc += 12 caso contrario
            case 0x00db: setFlags("----"); ILLEGAL(prefix + opcode); tc += 4; lastMnemonic = "ILLEGAL_DB"; break; 
            case 0x00dc: setFlags("----"); CALL(C, a16); tc += 12; lastMnemonic = "CALL C, a16"; break; // obs: tc += 24 se cond==true, tc += 12 caso contrario 
            case 0x00dd: setFlags("----"); ILLEGAL(prefix + opcode); tc += 4; lastMnemonic = "ILLEGAL_DD"; break; 
            case 0x00de: setFlags("Z1HC"); SBC(A, n8); tc += 8; lastMnemonic = "SBC A, n8"; break; 
            case 0x00df: setFlags("----"); RST(0x18); tc += 16; lastMnemonic = "RST $18"; break; 
            case 0x00e0: setFlags("----"); LDH(_a8_, A); tc += 12; lastMnemonic = "LDH [a8], A"; break; 
            case 0x00e1: setFlags("----"); POP(HL); tc += 12; lastMnemonic = "POP HL"; break; 
            case 0x00e2: setFlags("----"); LD(_C_, A); tc += 8; lastMnemonic = "LD [C], A"; break; 
            case 0x00e3: setFlags("----"); ILLEGAL(prefix + opcode); tc += 4; lastMnemonic = "ILLEGAL_E3"; break; 
            case 0x00e4: setFlags("----"); ILLEGAL(prefix + opcode); tc += 4; lastMnemonic = "ILLEGAL_E4"; break; 
            case 0x00e5: setFlags("----"); PUSH(HL); tc += 16; lastMnemonic = "PUSH HL"; break; 
            case 0x00e6: setFlags("Z010"); AND(A, n8); tc += 8; lastMnemonic = "AND A, n8"; break; 
            case 0x00e7: setFlags("----"); RST(0x20); tc += 16; lastMnemonic = "RST $20"; break; 
            case 0x00e8: setFlags("00HC"); ADD_SP_e8(SP, e8); tc += 16; lastMnemonic = "ADD SP, e8"; break; 
            case 0x00e9: setFlags("----"); JP(HL); tc += 4; lastMnemonic = "JP HL"; break; 
            case 0x00ea: setFlags("----"); LD(_a16_a, A); tc += 16; lastMnemonic = "LD [a16], A"; break; 
            case 0x00eb: setFlags("----"); ILLEGAL(prefix + opcode); tc += 4; lastMnemonic = "ILLEGAL_EB"; break; 
            case 0x00ec: setFlags("----"); ILLEGAL(prefix + opcode); tc += 4; lastMnemonic = "ILLEGAL_EC"; break; 
            case 0x00ed: setFlags("----"); ILLEGAL(prefix + opcode); tc += 4; lastMnemonic = "ILLEGAL_ED"; break; 
            case 0x00ee: setFlags("Z000"); XOR(A, n8); tc += 8; lastMnemonic = "XOR A, n8"; break; 
            case 0x00ef: setFlags("----"); RST(0x28); tc += 16; lastMnemonic = "RST $28"; break; 
            case 0x00f0: setFlags("----"); LDH(A, _a8_); tc += 12; lastMnemonic = "LDH A, [a8]"; break; 
            case 0x00f1: setFlags("ZNHC"); POP(AF); tc += 12; lastMnemonic = "POP AF"; break; // obs.: apenas o nibble superior do registrador F é afetado, o nibble inferior é sempre 0b0000
            case 0x00f2: setFlags("----"); LD(A, _C_); tc += 8; lastMnemonic = "LD A, [C]"; break; 
            case 0x00f3: setFlags("----"); DI(); tc += 4; lastMnemonic = "DI"; break; 
            case 0x00f4: setFlags("----"); ILLEGAL(prefix + opcode); tc += 4; lastMnemonic = "ILLEGAL_F4"; break; 
            case 0x00f5: setFlags("----"); PUSH(AF); tc += 16; lastMnemonic = "PUSH AF"; break; // obs.: apenas o nibble superior do registrador F é afetado, o nibble inferior é sempre 0b0000
            case 0x00f6: setFlags("Z000"); OR(A, n8); tc += 8; lastMnemonic = "OR A, n8"; break; 
            case 0x00f7: setFlags("----"); RST(0x30); tc += 16; lastMnemonic = "RST $30"; break; 
            case 0x00f8: setFlags("00HC"); LD(HL, SP, e8); tc += 12; lastMnemonic = "LD HL, SP + e8"; break; 
            case 0x00f9: setFlags("----"); LD(SP, HL); tc += 8; lastMnemonic = "LD SP, HL"; break; 
            case 0x00fa: setFlags("----"); LD(A, _a16_a); tc += 16; lastMnemonic = "LD A, [a16]"; break; 
            case 0x00fb: setFlags("----"); EI(); tc += 4; lastMnemonic = "EI"; break; 
            case 0x00fc: setFlags("----"); ILLEGAL(prefix + opcode); tc += 4; lastMnemonic = "ILLEGAL_FC"; break; 
            case 0x00fd: setFlags("----"); ILLEGAL(prefix + opcode); tc += 4; lastMnemonic = "ILLEGAL_FD"; break; 
            case 0x00fe: setFlags("Z1HC"); CP(A, n8); tc += 8; lastMnemonic = "CP A, n8"; break; 
            case 0x00ff: setFlags("----"); RST(0x38); tc += 16; lastMnemonic = "RST $38"; break; 
            case 0xcb00: setFlags("Z00C"); RLC(B); tc += 8; prefix = 0; lastMnemonic = "RLC B"; break; 
            case 0xcb01: setFlags("Z00C"); RLC(C); tc += 8; prefix = 0; lastMnemonic = "RLC C"; break; 
            case 0xcb02: setFlags("Z00C"); RLC(D); tc += 8; prefix = 0; lastMnemonic = "RLC D"; break; 
            case 0xcb03: setFlags("Z00C"); RLC(E); tc += 8; prefix = 0; lastMnemonic = "RLC E"; break; 
            case 0xcb04: setFlags("Z00C"); RLC(H); tc += 8; prefix = 0; lastMnemonic = "RLC H"; break; 
            case 0xcb05: setFlags("Z00C"); RLC(L); tc += 8; prefix = 0; lastMnemonic = "RLC L"; break; 
            case 0xcb06: setFlags("Z00C"); RLC(_HL_); tc += 16; prefix = 0; lastMnemonic = "RLC [HL]"; break; 
            case 0xcb07: setFlags("Z00C"); RLC(A); tc += 8; prefix = 0; lastMnemonic = "RLC A"; break; 
            case 0xcb08: setFlags("Z00C"); RRC(B); tc += 8; prefix = 0; lastMnemonic = "RRC B"; break; 
            case 0xcb09: setFlags("Z00C"); RRC(C); tc += 8; prefix = 0; lastMnemonic = "RRC C"; break; 
            case 0xcb0a: setFlags("Z00C"); RRC(D); tc += 8; prefix = 0; lastMnemonic = "RRC D"; break; 
            case 0xcb0b: setFlags("Z00C"); RRC(E); tc += 8; prefix = 0; lastMnemonic = "RRC E"; break; 
            case 0xcb0c: setFlags("Z00C"); RRC(H); tc += 8; prefix = 0; lastMnemonic = "RRC H"; break; 
            case 0xcb0d: setFlags("Z00C"); RRC(L); tc += 8; prefix = 0; lastMnemonic = "RRC L"; break; 
            case 0xcb0e: setFlags("Z00C"); RRC(_HL_); tc += 16; prefix = 0; lastMnemonic = "RRC [HL]"; break; 
            case 0xcb0f: setFlags("Z00C"); RRC(A); tc += 8; prefix = 0; lastMnemonic = "RRC A"; break; 
            case 0xcb10: setFlags("Z00C"); RL(B); tc += 8; prefix = 0; lastMnemonic = "RL B"; break; 
            case 0xcb11: setFlags("Z00C"); RL(C); tc += 8; prefix = 0; lastMnemonic = "RL C"; break; 
            case 0xcb12: setFlags("Z00C"); RL(D); tc += 8; prefix = 0; lastMnemonic = "RL D"; break; 
            case 0xcb13: setFlags("Z00C"); RL(E); tc += 8; prefix = 0; lastMnemonic = "RL E"; break; 
            case 0xcb14: setFlags("Z00C"); RL(H); tc += 8; prefix = 0; lastMnemonic = "RL H"; break; 
            case 0xcb15: setFlags("Z00C"); RL(L); tc += 8; prefix = 0; lastMnemonic = "RL L"; break; 
            case 0xcb16: setFlags("Z00C"); RL(_HL_); tc += 16; prefix = 0; lastMnemonic = "RL [HL]"; break; 
            case 0xcb17: setFlags("Z00C"); RL(A); tc += 8; prefix = 0; lastMnemonic = "RL A"; break; 
            case 0xcb18: setFlags("Z00C"); RR(B); tc += 8; prefix = 0; lastMnemonic = "RR B"; break; 
            case 0xcb19: setFlags("Z00C"); RR(C); tc += 8; prefix = 0; lastMnemonic = "RR C"; break; 
            case 0xcb1a: setFlags("Z00C"); RR(D); tc += 8; prefix = 0; lastMnemonic = "RR D"; break; 
            case 0xcb1b: setFlags("Z00C"); RR(E); tc += 8; prefix = 0; lastMnemonic = "RR E"; break; 
            case 0xcb1c: setFlags("Z00C"); RR(H); tc += 8; prefix = 0; lastMnemonic = "RR H"; break; 
            case 0xcb1d: setFlags("Z00C"); RR(L); tc += 8; prefix = 0; lastMnemonic = "RR L"; break; 
            case 0xcb1e: setFlags("Z00C"); RR(_HL_); tc += 16; prefix = 0; lastMnemonic = "RR [HL]"; break; 
            case 0xcb1f: setFlags("Z00C"); RR(A); tc += 8; prefix = 0; lastMnemonic = "RR A"; break; 
            case 0xcb20: setFlags("Z00C"); SLA(B); tc += 8; prefix = 0; lastMnemonic = "SLA B"; break; 
            case 0xcb21: setFlags("Z00C"); SLA(C); tc += 8; prefix = 0; lastMnemonic = "SLA C"; break; 
            case 0xcb22: setFlags("Z00C"); SLA(D); tc += 8; prefix = 0; lastMnemonic = "SLA D"; break; 
            case 0xcb23: setFlags("Z00C"); SLA(E); tc += 8; prefix = 0; lastMnemonic = "SLA E"; break; 
            case 0xcb24: setFlags("Z00C"); SLA(H); tc += 8; prefix = 0; lastMnemonic = "SLA H"; break; 
            case 0xcb25: setFlags("Z00C"); SLA(L); tc += 8; prefix = 0; lastMnemonic = "SLA L"; break; 
            case 0xcb26: setFlags("Z00C"); SLA(_HL_); tc += 16; prefix = 0; lastMnemonic = "SLA [HL]"; break; 
            case 0xcb27: setFlags("Z00C"); SLA(A); tc += 8; prefix = 0; lastMnemonic = "SLA A"; break; 
            case 0xcb28: setFlags("Z00C"); SRA(B); tc += 8; prefix = 0; lastMnemonic = "SRA B"; break; 
            case 0xcb29: setFlags("Z00C"); SRA(C); tc += 8; prefix = 0; lastMnemonic = "SRA C"; break; 
            case 0xcb2a: setFlags("Z00C"); SRA(D); tc += 8; prefix = 0; lastMnemonic = "SRA D"; break; 
            case 0xcb2b: setFlags("Z00C"); SRA(E); tc += 8; prefix = 0; lastMnemonic = "SRA E"; break; 
            case 0xcb2c: setFlags("Z00C"); SRA(H); tc += 8; prefix = 0; lastMnemonic = "SRA H"; break; 
            case 0xcb2d: setFlags("Z00C"); SRA(L); tc += 8; prefix = 0; lastMnemonic = "SRA L"; break; 
            case 0xcb2e: setFlags("Z00C"); SRA(_HL_); tc += 16; prefix = 0; lastMnemonic = "SRA [HL]"; break; 
            case 0xcb2f: setFlags("Z00C"); SRA(A); tc += 8; prefix = 0; lastMnemonic = "SRA A"; break; 
            case 0xcb30: setFlags("Z000"); SWAP(B); tc += 8; prefix = 0; lastMnemonic = "SWAP B"; break; 
            case 0xcb31: setFlags("Z000"); SWAP(C); tc += 8; prefix = 0; lastMnemonic = "SWAP C"; break; 
            case 0xcb32: setFlags("Z000"); SWAP(D); tc += 8; prefix = 0; lastMnemonic = "SWAP D"; break; 
            case 0xcb33: setFlags("Z000"); SWAP(E); tc += 8; prefix = 0; lastMnemonic = "SWAP E"; break; 
            case 0xcb34: setFlags("Z000"); SWAP(H); tc += 8; prefix = 0; lastMnemonic = "SWAP H"; break; 
            case 0xcb35: setFlags("Z000"); SWAP(L); tc += 8; prefix = 0; lastMnemonic = "SWAP L"; break; 
            case 0xcb36: setFlags("Z000"); SWAP(_HL_); tc += 16; prefix = 0; lastMnemonic = "SWAP [HL]"; break; 
            case 0xcb37: setFlags("Z000"); SWAP(A); tc += 8; prefix = 0; lastMnemonic = "SWAP A"; break; 
            case 0xcb38: setFlags("Z00C"); SRL(B); tc += 8; prefix = 0; lastMnemonic = "SRL B"; break; 
            case 0xcb39: setFlags("Z00C"); SRL(C); tc += 8; prefix = 0; lastMnemonic = "SRL C"; break; 
            case 0xcb3a: setFlags("Z00C"); SRL(D); tc += 8; prefix = 0; lastMnemonic = "SRL D"; break; 
            case 0xcb3b: setFlags("Z00C"); SRL(E); tc += 8; prefix = 0; lastMnemonic = "SRL E"; break; 
            case 0xcb3c: setFlags("Z00C"); SRL(H); tc += 8; prefix = 0; lastMnemonic = "SRL H"; break; 
            case 0xcb3d: setFlags("Z00C"); SRL(L); tc += 8; prefix = 0; lastMnemonic = "SRL L"; break; 
            case 0xcb3e: setFlags("Z00C"); SRL(_HL_); tc += 16; prefix = 0; lastMnemonic = "SRL [HL]"; break; 
            case 0xcb3f: setFlags("Z00C"); SRL(A); tc += 8; prefix = 0; lastMnemonic = "SRL A"; break; 
            case 0xcb40: setFlags("Z01-"); BIT(0, B); tc += 8; prefix = 0; lastMnemonic = "BIT 0, B"; break; 
            case 0xcb41: setFlags("Z01-"); BIT(0, C); tc += 8; prefix = 0; lastMnemonic = "BIT 0, C"; break; 
            case 0xcb42: setFlags("Z01-"); BIT(0, D); tc += 8; prefix = 0; lastMnemonic = "BIT 0, D"; break; 
            case 0xcb43: setFlags("Z01-"); BIT(0, E); tc += 8; prefix = 0; lastMnemonic = "BIT 0, E"; break; 
            case 0xcb44: setFlags("Z01-"); BIT(0, H); tc += 8; prefix = 0; lastMnemonic = "BIT 0, H"; break; 
            case 0xcb45: setFlags("Z01-"); BIT(0, L); tc += 8; prefix = 0; lastMnemonic = "BIT 0, L"; break; 
            case 0xcb46: setFlags("Z01-"); BIT(0, _HL_); tc += 12; prefix = 0; lastMnemonic = "BIT 0, [HL]"; break; 
            case 0xcb47: setFlags("Z01-"); BIT(0, A); tc += 8; prefix = 0; lastMnemonic = "BIT 0, A"; break; 
            case 0xcb48: setFlags("Z01-"); BIT(1, B); tc += 8; prefix = 0; lastMnemonic = "BIT 1, B"; break; 
            case 0xcb49: setFlags("Z01-"); BIT(1, C); tc += 8; prefix = 0; lastMnemonic = "BIT 1, C"; break; 
            case 0xcb4a: setFlags("Z01-"); BIT(1, D); tc += 8; prefix = 0; lastMnemonic = "BIT 1, D"; break; 
            case 0xcb4b: setFlags("Z01-"); BIT(1, E); tc += 8; prefix = 0; lastMnemonic = "BIT 1, E"; break; 
            case 0xcb4c: setFlags("Z01-"); BIT(1, H); tc += 8; prefix = 0; lastMnemonic = "BIT 1, H"; break; 
            case 0xcb4d: setFlags("Z01-"); BIT(1, L); tc += 8; prefix = 0; lastMnemonic = "BIT 1, L"; break; 
            case 0xcb4e: setFlags("Z01-"); BIT(1, _HL_); tc += 12; prefix = 0; lastMnemonic = "BIT 1, [HL]"; break; 
            case 0xcb4f: setFlags("Z01-"); BIT(1, A); tc += 8; prefix = 0; lastMnemonic = "BIT 1, A"; break; 
            case 0xcb50: setFlags("Z01-"); BIT(2, B); tc += 8; prefix = 0; lastMnemonic = "BIT 2, B"; break; 
            case 0xcb51: setFlags("Z01-"); BIT(2, C); tc += 8; prefix = 0; lastMnemonic = "BIT 2, C"; break; 
            case 0xcb52: setFlags("Z01-"); BIT(2, D); tc += 8; prefix = 0; lastMnemonic = "BIT 2, D"; break; 
            case 0xcb53: setFlags("Z01-"); BIT(2, E); tc += 8; prefix = 0; lastMnemonic = "BIT 2, E"; break; 
            case 0xcb54: setFlags("Z01-"); BIT(2, H); tc += 8; prefix = 0; lastMnemonic = "BIT 2, H"; break; 
            case 0xcb55: setFlags("Z01-"); BIT(2, L); tc += 8; prefix = 0; lastMnemonic = "BIT 2, L"; break; 
            case 0xcb56: setFlags("Z01-"); BIT(2, _HL_); tc += 12; prefix = 0; lastMnemonic = "BIT 2, [HL]"; break; 
            case 0xcb57: setFlags("Z01-"); BIT(2, A); tc += 8; prefix = 0; lastMnemonic = "BIT 2, A"; break; 
            case 0xcb58: setFlags("Z01-"); BIT(3, B); tc += 8; prefix = 0; lastMnemonic = "BIT 3, B"; break; 
            case 0xcb59: setFlags("Z01-"); BIT(3, C); tc += 8; prefix = 0; lastMnemonic = "BIT 3, C"; break; 
            case 0xcb5a: setFlags("Z01-"); BIT(3, D); tc += 8; prefix = 0; lastMnemonic = "BIT 3, D"; break; 
            case 0xcb5b: setFlags("Z01-"); BIT(3, E); tc += 8; prefix = 0; lastMnemonic = "BIT 3, E"; break; 
            case 0xcb5c: setFlags("Z01-"); BIT(3, H); tc += 8; prefix = 0; lastMnemonic = "BIT 3, H"; break; 
            case 0xcb5d: setFlags("Z01-"); BIT(3, L); tc += 8; prefix = 0; lastMnemonic = "BIT 3, L"; break; 
            case 0xcb5e: setFlags("Z01-"); BIT(3, _HL_); tc += 12; prefix = 0; lastMnemonic = "BIT 3, [HL]"; break; 
            case 0xcb5f: setFlags("Z01-"); BIT(3, A); tc += 8; prefix = 0; lastMnemonic = "BIT 3, A"; break; 
            case 0xcb60: setFlags("Z01-"); BIT(4, B); tc += 8; prefix = 0; lastMnemonic = "BIT 4, B"; break; 
            case 0xcb61: setFlags("Z01-"); BIT(4, C); tc += 8; prefix = 0; lastMnemonic = "BIT 4, C"; break; 
            case 0xcb62: setFlags("Z01-"); BIT(4, D); tc += 8; prefix = 0; lastMnemonic = "BIT 4, D"; break; 
            case 0xcb63: setFlags("Z01-"); BIT(4, E); tc += 8; prefix = 0; lastMnemonic = "BIT 4, E"; break; 
            case 0xcb64: setFlags("Z01-"); BIT(4, H); tc += 8; prefix = 0; lastMnemonic = "BIT 4, H"; break; 
            case 0xcb65: setFlags("Z01-"); BIT(4, L); tc += 8; prefix = 0; lastMnemonic = "BIT 4, L"; break; 
            case 0xcb66: setFlags("Z01-"); BIT(4, _HL_); tc += 12; prefix = 0; lastMnemonic = "BIT 4, [HL]"; break; 
            case 0xcb67: setFlags("Z01-"); BIT(4, A); tc += 8; prefix = 0; lastMnemonic = "BIT 4, A"; break; 
            case 0xcb68: setFlags("Z01-"); BIT(5, B); tc += 8; prefix = 0; lastMnemonic = "BIT 5, B"; break; 
            case 0xcb69: setFlags("Z01-"); BIT(5, C); tc += 8; prefix = 0; lastMnemonic = "BIT 5, C"; break; 
            case 0xcb6a: setFlags("Z01-"); BIT(5, D); tc += 8; prefix = 0; lastMnemonic = "BIT 5, D"; break; 
            case 0xcb6b: setFlags("Z01-"); BIT(5, E); tc += 8; prefix = 0; lastMnemonic = "BIT 5, E"; break; 
            case 0xcb6c: setFlags("Z01-"); BIT(5, H); tc += 8; prefix = 0; lastMnemonic = "BIT 5, H"; break; 
            case 0xcb6d: setFlags("Z01-"); BIT(5, L); tc += 8; prefix = 0; lastMnemonic = "BIT 5, L"; break; 
            case 0xcb6e: setFlags("Z01-"); BIT(5, _HL_); tc += 12; prefix = 0; lastMnemonic = "BIT 5, [HL]"; break; 
            case 0xcb6f: setFlags("Z01-"); BIT(5, A); tc += 8; prefix = 0; lastMnemonic = "BIT 5, A"; break; 
            case 0xcb70: setFlags("Z01-"); BIT(6, B); tc += 8; prefix = 0; lastMnemonic = "BIT 6, B"; break; 
            case 0xcb71: setFlags("Z01-"); BIT(6, C); tc += 8; prefix = 0; lastMnemonic = "BIT 6, C"; break; 
            case 0xcb72: setFlags("Z01-"); BIT(6, D); tc += 8; prefix = 0; lastMnemonic = "BIT 6, D"; break; 
            case 0xcb73: setFlags("Z01-"); BIT(6, E); tc += 8; prefix = 0; lastMnemonic = "BIT 6, E"; break; 
            case 0xcb74: setFlags("Z01-"); BIT(6, H); tc += 8; prefix = 0; lastMnemonic = "BIT 6, H"; break; 
            case 0xcb75: setFlags("Z01-"); BIT(6, L); tc += 8; prefix = 0; lastMnemonic = "BIT 6, L"; break; 
            case 0xcb76: setFlags("Z01-"); BIT(6, _HL_); tc += 12; prefix = 0; lastMnemonic = "BIT 6, [HL]"; break; 
            case 0xcb77: setFlags("Z01-"); BIT(6, A); tc += 8; prefix = 0; lastMnemonic = "BIT 6, A"; break; 
            case 0xcb78: setFlags("Z01-"); BIT(7, B); tc += 8; prefix = 0; lastMnemonic = "BIT 7, B"; break; 
            case 0xcb79: setFlags("Z01-"); BIT(7, C); tc += 8; prefix = 0; lastMnemonic = "BIT 7, C"; break; 
            case 0xcb7a: setFlags("Z01-"); BIT(7, D); tc += 8; prefix = 0; lastMnemonic = "BIT 7, D"; break; 
            case 0xcb7b: setFlags("Z01-"); BIT(7, E); tc += 8; prefix = 0; lastMnemonic = "BIT 7, E"; break; 
            case 0xcb7c: setFlags("Z01-"); BIT(7, H); tc += 8; prefix = 0; lastMnemonic = "BIT 7, H"; break; 
            case 0xcb7d: setFlags("Z01-"); BIT(7, L); tc += 8; prefix = 0; lastMnemonic = "BIT 7, L"; break; 
            case 0xcb7e: setFlags("Z01-"); BIT(7, _HL_); tc += 12; prefix = 0; lastMnemonic = "BIT 7, [HL]"; break; 
            case 0xcb7f: setFlags("Z01-"); BIT(7, A); tc += 8; prefix = 0; lastMnemonic = "BIT 7, A"; break; 
            case 0xcb80: setFlags("----"); RES(0, B); tc += 8; prefix = 0; lastMnemonic = "RES 0, B"; break; 
            case 0xcb81: setFlags("----"); RES(0, C); tc += 8; prefix = 0; lastMnemonic = "RES 0, C"; break; 
            case 0xcb82: setFlags("----"); RES(0, D); tc += 8; prefix = 0; lastMnemonic = "RES 0, D"; break; 
            case 0xcb83: setFlags("----"); RES(0, E); tc += 8; prefix = 0; lastMnemonic = "RES 0, E"; break; 
            case 0xcb84: setFlags("----"); RES(0, H); tc += 8; prefix = 0; lastMnemonic = "RES 0, H"; break; 
            case 0xcb85: setFlags("----"); RES(0, L); tc += 8; prefix = 0; lastMnemonic = "RES 0, L"; break; 
            case 0xcb86: setFlags("----"); RES(0, _HL_); tc += 16; prefix = 0; lastMnemonic = "RES 0, [HL]"; break; 
            case 0xcb87: setFlags("----"); RES(0, A); tc += 8; prefix = 0; lastMnemonic = "RES 0, A"; break; 
            case 0xcb88: setFlags("----"); RES(1, B); tc += 8; prefix = 0; lastMnemonic = "RES 1, B"; break; 
            case 0xcb89: setFlags("----"); RES(1, C); tc += 8; prefix = 0; lastMnemonic = "RES 1, C"; break; 
            case 0xcb8a: setFlags("----"); RES(1, D); tc += 8; prefix = 0; lastMnemonic = "RES 1, D"; break; 
            case 0xcb8b: setFlags("----"); RES(1, E); tc += 8; prefix = 0; lastMnemonic = "RES 1, E"; break; 
            case 0xcb8c: setFlags("----"); RES(1, H); tc += 8; prefix = 0; lastMnemonic = "RES 1, H"; break; 
            case 0xcb8d: setFlags("----"); RES(1, L); tc += 8; prefix = 0; lastMnemonic = "RES 1, L"; break; 
            case 0xcb8e: setFlags("----"); RES(1, _HL_); tc += 16; prefix = 0; lastMnemonic = "RES 1, [HL]"; break; 
            case 0xcb8f: setFlags("----"); RES(1, A); tc += 8; prefix = 0; lastMnemonic = "RES 1, A"; break; 
            case 0xcb90: setFlags("----"); RES(2, B); tc += 8; prefix = 0; lastMnemonic = "RES 2, B"; break; 
            case 0xcb91: setFlags("----"); RES(2, C); tc += 8; prefix = 0; lastMnemonic = "RES 2, C"; break; 
            case 0xcb92: setFlags("----"); RES(2, D); tc += 8; prefix = 0; lastMnemonic = "RES 2, D"; break; 
            case 0xcb93: setFlags("----"); RES(2, E); tc += 8; prefix = 0; lastMnemonic = "RES 2, E"; break; 
            case 0xcb94: setFlags("----"); RES(2, H); tc += 8; prefix = 0; lastMnemonic = "RES 2, H"; break; 
            case 0xcb95: setFlags("----"); RES(2, L); tc += 8; prefix = 0; lastMnemonic = "RES 2, L"; break; 
            case 0xcb96: setFlags("----"); RES(2, _HL_); tc += 16; prefix = 0; lastMnemonic = "RES 2, [HL]"; break; 
            case 0xcb97: setFlags("----"); RES(2, A); tc += 8; prefix = 0; lastMnemonic = "RES 2, A"; break; 
            case 0xcb98: setFlags("----"); RES(3, B); tc += 8; prefix = 0; lastMnemonic = "RES 3, B"; break; 
            case 0xcb99: setFlags("----"); RES(3, C); tc += 8; prefix = 0; lastMnemonic = "RES 3, C"; break; 
            case 0xcb9a: setFlags("----"); RES(3, D); tc += 8; prefix = 0; lastMnemonic = "RES 3, D"; break; 
            case 0xcb9b: setFlags("----"); RES(3, E); tc += 8; prefix = 0; lastMnemonic = "RES 3, E"; break; 
            case 0xcb9c: setFlags("----"); RES(3, H); tc += 8; prefix = 0; lastMnemonic = "RES 3, H"; break; 
            case 0xcb9d: setFlags("----"); RES(3, L); tc += 8; prefix = 0; lastMnemonic = "RES 3, L"; break; 
            case 0xcb9e: setFlags("----"); RES(3, _HL_); tc += 16; prefix = 0; lastMnemonic = "RES 3, [HL]"; break; 
            case 0xcb9f: setFlags("----"); RES(3, A); tc += 8; prefix = 0; lastMnemonic = "RES 3, A"; break; 
            case 0xcba0: setFlags("----"); RES(4, B); tc += 8; prefix = 0; lastMnemonic = "RES 4, B"; break; 
            case 0xcba1: setFlags("----"); RES(4, C); tc += 8; prefix = 0; lastMnemonic = "RES 4, C"; break; 
            case 0xcba2: setFlags("----"); RES(4, D); tc += 8; prefix = 0; lastMnemonic = "RES 4, D"; break; 
            case 0xcba3: setFlags("----"); RES(4, E); tc += 8; prefix = 0; lastMnemonic = "RES 4, E"; break; 
            case 0xcba4: setFlags("----"); RES(4, H); tc += 8; prefix = 0; lastMnemonic = "RES 4, H"; break; 
            case 0xcba5: setFlags("----"); RES(4, L); tc += 8; prefix = 0; lastMnemonic = "RES 4, L"; break; 
            case 0xcba6: setFlags("----"); RES(4, _HL_); tc += 16; prefix = 0; lastMnemonic = "RES 4, [HL]"; break; 
            case 0xcba7: setFlags("----"); RES(4, A); tc += 8; prefix = 0; lastMnemonic = "RES 4, A"; break; 
            case 0xcba8: setFlags("----"); RES(5, B); tc += 8; prefix = 0; lastMnemonic = "RES 5, B"; break; 
            case 0xcba9: setFlags("----"); RES(5, C); tc += 8; prefix = 0; lastMnemonic = "RES 5, C"; break; 
            case 0xcbaa: setFlags("----"); RES(5, D); tc += 8; prefix = 0; lastMnemonic = "RES 5, D"; break; 
            case 0xcbab: setFlags("----"); RES(5, E); tc += 8; prefix = 0; lastMnemonic = "RES 5, E"; break; 
            case 0xcbac: setFlags("----"); RES(5, H); tc += 8; prefix = 0; lastMnemonic = "RES 5, H"; break; 
            case 0xcbad: setFlags("----"); RES(5, L); tc += 8; prefix = 0; lastMnemonic = "RES 5, L"; break; 
            case 0xcbae: setFlags("----"); RES(5, _HL_); tc += 16; prefix = 0; lastMnemonic = "RES 5, [HL]"; break; 
            case 0xcbaf: setFlags("----"); RES(5, A); tc += 8; prefix = 0; lastMnemonic = "RES 5, A"; break; 
            case 0xcbb0: setFlags("----"); RES(6, B); tc += 8; prefix = 0; lastMnemonic = "RES 6, B"; break; 
            case 0xcbb1: setFlags("----"); RES(6, C); tc += 8; prefix = 0; lastMnemonic = "RES 6, C"; break; 
            case 0xcbb2: setFlags("----"); RES(6, D); tc += 8; prefix = 0; lastMnemonic = "RES 6, D"; break; 
            case 0xcbb3: setFlags("----"); RES(6, E); tc += 8; prefix = 0; lastMnemonic = "RES 6, E"; break; 
            case 0xcbb4: setFlags("----"); RES(6, H); tc += 8; prefix = 0; lastMnemonic = "RES 6, H"; break; 
            case 0xcbb5: setFlags("----"); RES(6, L); tc += 8; prefix = 0; lastMnemonic = "RES 6, L"; break; 
            case 0xcbb6: setFlags("----"); RES(6, _HL_); tc += 16; prefix = 0; lastMnemonic = "RES 6, [HL]"; break; 
            case 0xcbb7: setFlags("----"); RES(6, A); tc += 8; prefix = 0; lastMnemonic = "RES 6, A"; break; 
            case 0xcbb8: setFlags("----"); RES(7, B); tc += 8; prefix = 0; lastMnemonic = "RES 7, B"; break; 
            case 0xcbb9: setFlags("----"); RES(7, C); tc += 8; prefix = 0; lastMnemonic = "RES 7, C"; break; 
            case 0xcbba: setFlags("----"); RES(7, D); tc += 8; prefix = 0; lastMnemonic = "RES 7, D"; break; 
            case 0xcbbb: setFlags("----"); RES(7, E); tc += 8; prefix = 0; lastMnemonic = "RES 7, E"; break; 
            case 0xcbbc: setFlags("----"); RES(7, H); tc += 8; prefix = 0; lastMnemonic = "RES 7, H"; break; 
            case 0xcbbd: setFlags("----"); RES(7, L); tc += 8; prefix = 0; lastMnemonic = "RES 7, L"; break; 
            case 0xcbbe: setFlags("----"); RES(7, _HL_); tc += 16; prefix = 0; lastMnemonic = "RES 7, [HL]"; break; 
            case 0xcbbf: setFlags("----"); RES(7, A); tc += 8; prefix = 0; lastMnemonic = "RES 7, A"; break; 
            case 0xcbc0: setFlags("----"); SET(0, B); tc += 8; prefix = 0; lastMnemonic = "SET 0, B"; break; 
            case 0xcbc1: setFlags("----"); SET(0, C); tc += 8; prefix = 0; lastMnemonic = "SET 0, C"; break; 
            case 0xcbc2: setFlags("----"); SET(0, D); tc += 8; prefix = 0; lastMnemonic = "SET 0, D"; break; 
            case 0xcbc3: setFlags("----"); SET(0, E); tc += 8; prefix = 0; lastMnemonic = "SET 0, E"; break; 
            case 0xcbc4: setFlags("----"); SET(0, H); tc += 8; prefix = 0; lastMnemonic = "SET 0, H"; break; 
            case 0xcbc5: setFlags("----"); SET(0, L); tc += 8; prefix = 0; lastMnemonic = "SET 0, L"; break; 
            case 0xcbc6: setFlags("----"); SET(0, _HL_); tc += 16; prefix = 0; lastMnemonic = "SET 0, [HL]"; break; 
            case 0xcbc7: setFlags("----"); SET(0, A); tc += 8; prefix = 0; lastMnemonic = "SET 0, A"; break; 
            case 0xcbc8: setFlags("----"); SET(1, B); tc += 8; prefix = 0; lastMnemonic = "SET 1, B"; break; 
            case 0xcbc9: setFlags("----"); SET(1, C); tc += 8; prefix = 0; lastMnemonic = "SET 1, C"; break; 
            case 0xcbca: setFlags("----"); SET(1, D); tc += 8; prefix = 0; lastMnemonic = "SET 1, D"; break; 
            case 0xcbcb: setFlags("----"); SET(1, E); tc += 8; prefix = 0; lastMnemonic = "SET 1, E"; break; 
            case 0xcbcc: setFlags("----"); SET(1, H); tc += 8; prefix = 0; lastMnemonic = "SET 1, H"; break; 
            case 0xcbcd: setFlags("----"); SET(1, L); tc += 8; prefix = 0; lastMnemonic = "SET 1, L"; break; 
            case 0xcbce: setFlags("----"); SET(1, _HL_); tc += 16; prefix = 0; lastMnemonic = "SET 1, [HL]"; break; 
            case 0xcbcf: setFlags("----"); SET(1, A); tc += 8; prefix = 0; lastMnemonic = "SET 1, A"; break; 
            case 0xcbd0: setFlags("----"); SET(2, B); tc += 8; prefix = 0; lastMnemonic = "SET 2, B"; break; 
            case 0xcbd1: setFlags("----"); SET(2, C); tc += 8; prefix = 0; lastMnemonic = "SET 2, C"; break; 
            case 0xcbd2: setFlags("----"); SET(2, D); tc += 8; prefix = 0; lastMnemonic = "SET 2, D"; break; 
            case 0xcbd3: setFlags("----"); SET(2, E); tc += 8; prefix = 0; lastMnemonic = "SET 2, E"; break; 
            case 0xcbd4: setFlags("----"); SET(2, H); tc += 8; prefix = 0; lastMnemonic = "SET 2, H"; break; 
            case 0xcbd5: setFlags("----"); SET(2, L); tc += 8; prefix = 0; lastMnemonic = "SET 2, L"; break; 
            case 0xcbd6: setFlags("----"); SET(2, _HL_); tc += 16; prefix = 0; lastMnemonic = "SET 2, [HL]"; break; 
            case 0xcbd7: setFlags("----"); SET(2, A); tc += 8; prefix = 0; lastMnemonic = "SET 2, A"; break; 
            case 0xcbd8: setFlags("----"); SET(3, B); tc += 8; prefix = 0; lastMnemonic = "SET 3, B"; break; 
            case 0xcbd9: setFlags("----"); SET(3, C); tc += 8; prefix = 0; lastMnemonic = "SET 3, C"; break; 
            case 0xcbda: setFlags("----"); SET(3, D); tc += 8; prefix = 0; lastMnemonic = "SET 3, D"; break; 
            case 0xcbdb: setFlags("----"); SET(3, E); tc += 8; prefix = 0; lastMnemonic = "SET 3, E"; break; 
            case 0xcbdc: setFlags("----"); SET(3, H); tc += 8; prefix = 0; lastMnemonic = "SET 3, H"; break; 
            case 0xcbdd: setFlags("----"); SET(3, L); tc += 8; prefix = 0; lastMnemonic = "SET 3, L"; break; 
            case 0xcbde: setFlags("----"); SET(3, _HL_); tc += 16; prefix = 0; lastMnemonic = "SET 3, [HL]"; break; 
            case 0xcbdf: setFlags("----"); SET(3, A); tc += 8; prefix = 0; lastMnemonic = "SET 3, A"; break; 
            case 0xcbe0: setFlags("----"); SET(4, B); tc += 8; prefix = 0; lastMnemonic = "SET 4, B"; break; 
            case 0xcbe1: setFlags("----"); SET(4, C); tc += 8; prefix = 0; lastMnemonic = "SET 4, C"; break; 
            case 0xcbe2: setFlags("----"); SET(4, D); tc += 8; prefix = 0; lastMnemonic = "SET 4, D"; break; 
            case 0xcbe3: setFlags("----"); SET(4, E); tc += 8; prefix = 0; lastMnemonic = "SET 4, E"; break; 
            case 0xcbe4: setFlags("----"); SET(4, H); tc += 8; prefix = 0; lastMnemonic = "SET 4, H"; break; 
            case 0xcbe5: setFlags("----"); SET(4, L); tc += 8; prefix = 0; lastMnemonic = "SET 4, L"; break; 
            case 0xcbe6: setFlags("----"); SET(4, _HL_); tc += 16; prefix = 0; lastMnemonic = "SET 4, [HL]"; break; 
            case 0xcbe7: setFlags("----"); SET(4, A); tc += 8; prefix = 0; lastMnemonic = "SET 4, A"; break; 
            case 0xcbe8: setFlags("----"); SET(5, B); tc += 8; prefix = 0; lastMnemonic = "SET 5, B"; break; 
            case 0xcbe9: setFlags("----"); SET(5, C); tc += 8; prefix = 0; lastMnemonic = "SET 5, C"; break; 
            case 0xcbea: setFlags("----"); SET(5, D); tc += 8; prefix = 0; lastMnemonic = "SET 5, D"; break; 
            case 0xcbeb: setFlags("----"); SET(5, E); tc += 8; prefix = 0; lastMnemonic = "SET 5, E"; break; 
            case 0xcbec: setFlags("----"); SET(5, H); tc += 8; prefix = 0; lastMnemonic = "SET 5, H"; break; 
            case 0xcbed: setFlags("----"); SET(5, L); tc += 8; prefix = 0; lastMnemonic = "SET 5, L"; break; 
            case 0xcbee: setFlags("----"); SET(5, _HL_); tc += 16; prefix = 0; lastMnemonic = "SET 5, [HL]"; break; 
            case 0xcbef: setFlags("----"); SET(5, A); tc += 8; prefix = 0; lastMnemonic = "SET 5, A"; break; 
            case 0xcbf0: setFlags("----"); SET(6, B); tc += 8; prefix = 0; lastMnemonic = "SET 6, B"; break; 
            case 0xcbf1: setFlags("----"); SET(6, C); tc += 8; prefix = 0; lastMnemonic = "SET 6, C"; break; 
            case 0xcbf2: setFlags("----"); SET(6, D); tc += 8; prefix = 0; lastMnemonic = "SET 6, D"; break; 
            case 0xcbf3: setFlags("----"); SET(6, E); tc += 8; prefix = 0; lastMnemonic = "SET 6, E"; break; 
            case 0xcbf4: setFlags("----"); SET(6, H); tc += 8; prefix = 0; lastMnemonic = "SET 6, H"; break; 
            case 0xcbf5: setFlags("----"); SET(6, L); tc += 8; prefix = 0; lastMnemonic = "SET 6, L"; break; 
            case 0xcbf6: setFlags("----"); SET(6, _HL_); tc += 16; prefix = 0; lastMnemonic = "SET 6, [HL]"; break; 
            case 0xcbf7: setFlags("----"); SET(6, A); tc += 8; prefix = 0; lastMnemonic = "SET 6, A"; break; 
            case 0xcbf8: setFlags("----"); SET(7, B); tc += 8; prefix = 0; lastMnemonic = "SET 7, B"; break; 
            case 0xcbf9: setFlags("----"); SET(7, C); tc += 8; prefix = 0; lastMnemonic = "SET 7, C"; break; 
            case 0xcbfa: setFlags("----"); SET(7, D); tc += 8; prefix = 0; lastMnemonic = "SET 7, D"; break; 
            case 0xcbfb: setFlags("----"); SET(7, E); tc += 8; prefix = 0; lastMnemonic = "SET 7, E"; break; 
            case 0xcbfc: setFlags("----"); SET(7, H); tc += 8; prefix = 0; lastMnemonic = "SET 7, H"; break; 
            case 0xcbfd: setFlags("----"); SET(7, L); tc += 8; prefix = 0; lastMnemonic = "SET 7, L"; break; 
            case 0xcbfe: setFlags("----"); SET(7, _HL_); tc += 16; prefix = 0; lastMnemonic = "SET 7, [HL]"; break; 
            case 0xcbff: setFlags("----"); SET(7, A); tc += 8; prefix = 0; lastMnemonic = "SET 7, A"; break;   
        }
    }
    
    public static final int FZ = 7;
    public static final int FN = 6;
    public static final int FH = 5;
    public static final int FC = 4;

    // set initial register values
    static {
        RA = 0x01;
        setFlag(FZ, 1);
        setFlag(FN, 0);
        setFlag(FH, 1); //?
        setFlag(FC, 1); //?
        RB = 0x00;
        RC = 0x13;
        RD = 0x00;
        RE = 0xD8;
        RH = 0x01;
        RL = 0x4D;
        RSP = 0xFFFE;
        RPC = 0x0100;
    }

    
    // bit = 0-7
    public static void setFlag(int bit, int value) {
        if (value == 1) {
            RF = RF | (1 << bit);
        }
        else if (value == 0) {
            RF = RF & ~(1 << bit);
        }
    }

    public static int getFlag(int bit) {
        return (RF & (1 << bit)) > 0 ? 1 : 0;
    }

    public static boolean FZA = false;
    public static boolean FNA = false;
    public static boolean FHA = false;
    public static boolean FCA = false;

    public static void setFlags(String flags) {
        //       Z N H C
        // FZ
        FZA = false;
        if (flags.charAt(0) == '0') setFlag(FZ,  0);
        else if (flags.charAt(0) == '1') setFlag(FZ,  1);
        else if (flags.charAt(0) == 'Z') FZA = true;
        // FN
        FNA = false;
        if (flags.charAt(1) == '0') setFlag(FN,  0);
        else if (flags.charAt(1) == '1') setFlag(FN,  1);
        else if (flags.charAt(1) == 'N') FNA = true;
        // FH
        FHA = false;
        if (flags.charAt(2) == '0') setFlag(FH,  0);
        else if (flags.charAt(2) == '1') setFlag(FH,  1);
        else if (flags.charAt(2) == 'H') FHA = true;
        // FC
        FCA = false;
        if (flags.charAt(3) == '0') setFlag(FC,  0);
        else if (flags.charAt(3) == '1') setFlag(FC,  1);
        else if (flags.charAt(3) == 'C') FCA = true;    
    }
    
    public static void printFlags() {
        String z = getFlag(FZ) == 1 ? "Z" : "-";
        String n = getFlag(FN) == 1 ? "N" : "-";
        String h = getFlag(FH) == 1 ? "H" : "-";
        String c = getFlag(FC) == 1 ? "C" : "-";
        System.out.printf("flags: %s%s%s%s \n", z, n, h, c);
    }

    public static void NOP() { 
        // do nothing    
    }

    public static void LD(int op1, int op2) { 
        //int a1 = get(op1);
        int a2 = get(op2);
        set(op1, a2);
    }
    
    // LD HL, SP + e8
    public static void LD(int op1, int op2, int op3) { 
        int a1 = get(op2);
        int a2 = get(op3);
        int n = a1 + a2;
        set(op1, n);
        if (FHA) setFlag(FH, (a1 & 0x0f) + (a2 & 0x0f) > 0x0f ? 1 : 0);
        if (FCA) setFlag(FC, (a1 & 0xff) + (a2 & 0xff) > 0xff ? 1 : 0);        
    }
    
    public static void LDH(int op1, int op2) { 
        set(op1, get(op2));
    }
    
    public static void INC(int op1) {
        int a1 = get(op1);
        int n = a1 + 1;
        set(op1, n);
        if (FZA) setFlag(FZ, (get(op1) == 0) ? 1 : 0);
        if (FHA) setFlag(FH, ((a1 & 0x0f) + 1) > 0x0f ? 1 : 0);
    }

    public static void DEC(int op1) { 
        int a1 = get(op1);
        int n = a1 - 1;
        set(op1, n);
        if (FZA) setFlag(FZ, (get(op1) == 0) ? 1 : 0);
        if (FHA) setFlag(FH, ((a1 & 0x0f) - 1) < 0x00 ? 1 : 0);
    }
        
    public static void RLCA() {
        int a1 = get(A);
        int c = (a1 & 0x80) >> 7;
        set(A, (a1 << 1) + c);
        setFlag(FC, c);
    }
    
    public static void CCF() { 
        setFlag(FC, getFlag(FC) == 1 ? 0 : 1);
    }

    public static void ADD8(int op1, int op2) { 
        int a1 = get(op1);
        int a2 = get(op2);
        int n = a1 + a2;
        set(op1, n);
        if (FZA) setFlag(FZ, (get(op1) == 0) ? 1 : 0);
        if (FHA) setFlag(FH, ((a1 & 0x0f) + (a2 & 0x0f)) > 0x0f ? 1 : 0);
        if (FCA) setFlag(FC, (a1 & 0xff) + (a2 & 0xff) > 0xff ? 1 : 0);        
    }

    // ADD SP, e8
    // TODO can be replaced by ADD8() ?
    public static void ADD_SP_e8(int op1, int op2) { 
        int a1 = get(op1);
        int a2 = get(op2);
        int n = a1 + a2;
        set(op1, n);
        if (FHA) setFlag(FH, (a1 & 0x0f) + (a2 & 0x0f) > 0x0f ? 1 : 0);
        if (FCA) setFlag(FC, (a1 & 0xff) + (a2 & 0xff) > 0xff ? 1 : 0);        
    }

    public static void ADD16(int op1, int op2) { 
        int a1 = get(op1);
        int a2 = get(op2);
        int n = a1 + a2;
        set(op1, n);
        if (FZA) setFlag(FZ, (get(op1) == 0) ? 1 : 0);
        if (FHA) setFlag(FH, ((a1 & 0x0fff) + (a2 & 0x0fff)) > 0x0fff ? 1 : 0);
        if (FCA) setFlag(FC, ((a1 & 0xffff) + (a2 & 0xffff)) > 0xffff ? 1 : 0);        
    }

    public static void RRCA() { 
        int a1 = get(A);
        int c = a1 & 0x01;
        set(A, (a1 >> 1) + (c << 7));
        setFlag(FC, c);
    }

    // TODO
    public static void STOP(int n) { 
        // apparently n is just ignored by hardware?
        System.out.println("stopping ... n = " + n);
        //System.exit(0);
        //running = false;
    }

    public static void RLA() { 
        int c = getFlag(FC);
        int a1 = get(A);
        int nc = (a1 & 0x80) >> 7;
        set(A, (a1 << 1) + c);
        setFlag(FC, nc);
    }

    public static void JR(int op1) { 
        int e = get(op1);
        RPC += e;
    }

    public static void RRA() { 
        int c = getFlag(FC);
        int a1 = get(A);
        int nc = a1 & 0x01;
        set(A, (a1 >> 1) + (c << 7));
        setFlag(FC, nc);
    }

    public static void JR(int cond, int op2) { 
        switch (cond) {
            case Z:  if (getFlag(FZ) == 1) { JR(op2); tc += 4; } else RPC++; break;
            case NZ: if (getFlag(FZ) == 0) { JR(op2); tc += 4; } else RPC++; break;
            case C:  if (getFlag(FC) == 1) { JR(op2); tc += 4; } else RPC++; break;
            case NC: if (getFlag(FC) == 0) { JR(op2); tc += 4; } else RPC++; break;
        }        
        // workaround: if cond==true tc += 4 
    }

    /**
     *  example:
     *  ld a, $15
     *  add a, $27 ; a = $3c
     *  daa ; a = $42
     */
    // refs
    // https://forums.nesdev.org/viewtopic.php?t=15944
    // dez/8/2024 nao funcionava porque eu alterava os flags antes atraves de setFlags()
    // https://www.ngemu.com/threads/little-help-with-my-gameboy-emulator.143814/
    public static void DAA() { 
        if (getFlag(FN) == 0) { // add op
            if ((getFlag(FC) == 1) || (RA > 0x99)) {
                RA = (RA + 0x60) & 0xff;
                setFlag(FC, 1);
            } 
            if ((getFlag(FH) == 1) || ((RA & 0x0F) > 0x09)) {
                RA = (RA + 0x06) & 0xff;
                setFlag(FH, 0);
            } 
        }
        else if (getFlag(FC) == 1 && getFlag(FH) == 1) { // sub op
            RA = (RA + 0x9A) & 0xff;
            setFlag(FH, 0);
        }
        else if (getFlag(FC) == 1) {
            RA = (RA + 0xA0) & 0xff;
        }
        else if (getFlag(FH) == 1) {
            RA = (RA + 0xFA) & 0xff;
            setFlag(FH, 0);
        } 
        setFlag(FZ, RA == 0 ? 1 : 0);
        setFlag(FH, 0);
    }

    public static void CPL() { 
        RA = (~RA) & 0xff;
    }
    
    public static void ADC(int op1, int op2) { 
        int a1 = get(op1);
        int a2 = get(op2);
        int n = a1 + a2 + getFlag(FC);
        set(op1, n);
        if (FZA) setFlag(FZ, (get(op1) == 0) ? 1 : 0);
        if (FHA) setFlag(FH, ((a1 & 0x0f) + (a2 & 0x0f) + getFlag(FC)) > 0x0f ? 1 : 0);
        if (FCA) setFlag(FC, n > 0xff ? 1 : 0);        
    }

    public static void SUB(int op1, int op2) { 
        int a1 = get(op1);
        int a2 = get(op2);
        int n = a1 - a2;
        set(op1, n);
        if (FZA) setFlag(FZ, (get(op1) == 0) ? 1 : 0);
        if (FHA) setFlag(FH, ((a1 & 0xf) - (a2 & 0xf)) < 0x00 ? 1 : 0);
        if (FCA) setFlag(FC, n < 0x00 ? 1 : 0);        
    }

    public static void SBC(int op1, int op2) { 
        int a1 = get(op1);
        int a2 = get(op2);
        int n = a1 - a2 - getFlag(FC);
        set(op1, n);
        if (FZA) setFlag(FZ, (get(op1) == 0) ? 1 : 0);
        if (FHA) setFlag(FH, ((a1 & 0xf) - (a2 & 0xf) - getFlag(FC)) < 0x00 ? 1 : 0);
        if (FCA) setFlag(FC, n < 0x00 ? 1 : 0);        
    }

    public static void AND(int op1, int op2) { 
        int a1 = get(op1);
        int a2 = get(op2);
        int n = a1 & a2;
        set(op1, n);
        if (FZA) setFlag(FZ, (get(op1) == 0) ? 1 : 0);
    }
    
    public static void XOR(int op1, int op2) { 
        int a1 = get(op1);
        int a2 = get(op2);
        int n = a1 ^ a2;
        set(op1, n);
        if (FZA) setFlag(FZ, (get(op1) == 0) ? 1 : 0);
    }

    public static void OR(int op1, int op2) { 
        int a1 = get(op1);
        int a2 = get(op2);
        int n = a1 | a2;
        set(op1, n);
        if (FZA) setFlag(FZ, (get(op1) == 0) ? 1 : 0);
    }
    
    public static void CP(int op1, int op2) { 
        int a1 = get(op1);
        int a2 = get(op2);
        int n = a1 - a2;
        if (FZA) setFlag(FZ, (n == 0) ? 1 : 0);
        if (FHA) setFlag(FH, ((a1 & 0x0f) - (a2 & 0x0f)) < 0x00 ? 1 : 0);
        if (FCA) setFlag(FC, ((a1 & 0xff) - (a2 & 0xff)) < 0x00 ? 1 : 0);        
    }
                
    public static void RET() { 
        int lsb = read(RSP++);
        int msb = read(RSP++);
        RPC = lsb + (msb << 8);
    }

    public static void RET(int cond) { 
        switch (cond) {
            case Z:  if (getFlag(FZ) == 1) { RET(); tc += 12; }; break;
            case NZ: if (getFlag(FZ) == 0) { RET(); tc += 12; }; break;
            case C:  if (getFlag(FC) == 1) { RET(); tc += 12; }; break;
            case NC: if (getFlag(FC) == 0) { RET(); tc += 12; }; break;
        }            
        // workaround: if cond==true tc += 4 (para dar 20 ao todo)
    }
    
    public static void CALL(int op1) { 
        int n = get(op1);
        RSP--;
        write(RSP--, (RPC >> 8) & 0xff);
        write(RSP, RPC & 0xff);
        RPC = n;        
    }
    
    public static void CALL(int cond, int op2) { 
        switch (cond) {
            case Z:  if (getFlag(FZ) == 1) { CALL(op2); tc += 12; } else RPC += 2; break;
            case NZ: if (getFlag(FZ) == 0) { CALL(op2); tc += 12; } else RPC += 2; break;
            case C:  if (getFlag(FC) == 1) { CALL(op2); tc += 12; } else RPC += 2; break;
            case NC: if (getFlag(FC) == 0) { CALL(op2); tc += 12; } else RPC += 2; break;
        }        
        // workaround: if cond==true tc += 12 (para dar 24 ao todo)    
    }
    
    public static void DI() { 
        IME = 0;
    }
    
    public static void JP(int op1) { 
        int n = get(op1);
        RPC = n;        
    }

    public static void JP(int cond, int op2) { 
        switch (cond) {
            case Z:  if (getFlag(FZ) == 1) { JP(op2); tc += 4; } else RPC += 2; break;
            case NZ: if (getFlag(FZ) == 0) { JP(op2); tc += 4; } else RPC += 2; break;
            case C:  if (getFlag(FC) == 1) { JP(op2); tc += 4; } else RPC += 2; break;
            case NC: if (getFlag(FC) == 0) { JP(op2); tc += 4; } else RPC += 2; break;
        }            
        // workaround: if cond==true tc += 4 (para dar 16 ao todo)
    }
    
    public static void RETI() { 
        EI();
        RET();
    }
    
    //TODO
    public static void HALT() { 
        //System.out.println("halting ...");
        //System.exit(0);
        //running = false;
        halted = true;
    }
    
    public static int IME_PENDING = 0;

    public static void EI() { 
        //IME = 1;
        IME_PENDING = 2;
    }
    
    public static void POP(int op1) { 
        int lsb = read(RSP++);
        int msb = read(RSP++);
        set(op1, lsb + (msb << 8));
    }
    
    public static void ILLEGAL(int opcode) { 
        // throw new RuntimeException("invoked illegal instruction ! opcode = " + opcode);
    }
    
    // n = 0x00, 0x08, 0x10, 0x18, 0x20, etc
    public static void RST(int n) { 
        RSP--;
        write(RSP--, (RPC >> 8) & 0xff);
        write(RSP, RPC & 0xff);
        RPC = n;
    }
    
    public static void PREFIX() { 
        prefix = 0xcb00;
    }
    
    public static void PUSH(int op1) { 
        int n = get(op1);
        RSP--;
        write(RSP, (n >> 8) & 0xff);
        RSP--;
        write(RSP, n & 0xff);
    }
    
    public static void RLC(int op1) {
        int a1 = get(op1);
        int c = (a1 & 0x80) >> 7;
        int n = ((a1 << 1) | c) & 0xff;
        set(op1, n);
        if (FZA) setFlag(FZ, get(op1) == 0 ? 1 : 0);
        setFlag(FC, c);
    }
    
    public static void RRC(int op1) {
        int a1 = get(op1);
        int c = a1 & 0x01;
        int n = ((a1 >> 1) | (c << 7)) & 0xff;
        set(op1, n);
        if (FZA) setFlag(FZ, get(op1) == 0 ? 1 : 0);
        setFlag(FC, c);
    }

    public static void RL(int op1) {
        int a1 = get(op1);
        int c = (a1 & 0x80) >> 7;
        int n = ((a1 << 1) | getFlag(FC)) & 0xff;
        set(op1, n);
        if (FZA) setFlag(FZ, get(op1) == 0 ? 1 : 0);
        setFlag(FC, c);
    }

    public static void RR(int op1) {
        int a1 = get(op1);
        int c = a1 & 0x01;
        int n = ((a1 >> 1) | (getFlag(FC) << 7)) & 0xff;
        set(op1, n);
        if (FZA) setFlag(FZ, get(op1) == 0 ? 1 : 0);
        setFlag(FC, c);
    }

    public static void SLA(int op1) {
        int a1 = get(op1);
        int c = (a1 & 0x80) >> 7;
        int n = (a1 << 1) & 0xff;
        set(op1, n);
        if (FZA) setFlag(FZ, get(op1) == 0 ? 1 : 0);
        setFlag(FC, c);
    }

    public static void SRA(int op1) {
        int a1 = get(op1);
        int msb = (a1 & 0x80);
        int c = a1 & 0x01;
        int n = ((a1 >> 1) | msb) & 0xff;
        set(op1, n);
        if (FZA) setFlag(FZ, get(op1) == 0 ? 1 : 0);
        setFlag(FC, c);
    }

    public static void SWAP(int op1) {
        int a1 = get(op1);
        int n = ((a1 << 4) & 0xf0) + ((a1 >> 4) & 0x0f);
        set(op1, n);
        if (FZA) setFlag(FZ, (get(op1) == 0) ? 1 : 0);
    }

    public static void SRL(int op1) {
        int a1 = get(op1);
        int c = a1 & 0x01;
        int n = (a1 >> 1) & 0xff;
        set(op1, n);
        if (FZA) setFlag(FZ, get(op1) == 0 ? 1 : 0);
        setFlag(FC, c);
    }

    public static void BIT(int b, int op) {
        int n = get(op) & (1 << b);
        setFlag(FZ, n == 0 ? 1 : 0);
    }

    public static void RES(int b, int op) {
        int n = get(op) & ~(1 << b);
        set(op, n);
    }

    public static void SET(int b, int op) {
        int n = get(op) | (1 << b);
        set(op, n);
    }
    
}
