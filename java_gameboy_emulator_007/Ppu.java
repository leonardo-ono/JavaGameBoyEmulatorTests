import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * $FF40	LCDC	LCD control	R/W	All
 * $FF41	STAT	LCD status	Mixed	All
 * $FF42	SCY	Viewport Y position	R/W	All
 * $FF43	SCX	Viewport X position	R/W	All
 * $FF44	LY	LCD Y coordinate	R	All
 * $FF45	LYC	LY compare	R/W	All
 * $FF46	DMA	OAM DMA source address & start	R/W	All
 * $FF47	BGP	BG palette data	R/W	DMG
 * $FF48	OBP0	OBJ palette 0 data	R/W	DMG
 * $FF49	OBP1	OBJ palette 1 data	R/W	DMG
 * $FF4A	WY	Window Y position	R/W	All
 * $FF4B	WX	Window X position plus 7	R/W	All
 */

public class Ppu {

    public static int[] vram = new int[0xffff]; // new int[0x2000]; // 8192 bytes
    //public static int[] oam = new int[0xa0]; // 160 bytes
    
    public static enum Mode { MODE0_HBLANK, MODE1_VBLANK, MODE2_OAM_SCAN, MODE3_DRAW_PIXELS }; 
    private static Mode ppuMode = Mode.MODE2_OAM_SCAN;

    private static int processedTCycles;
    private static int LX = 0; // LCD X coordinate (private)
    
    static {
        //for (int i = 0x8000; i < 0x9fff; i++) {
        //    vram[i] = (int) (Math.random() * 256);
        //}
    }
    // registers

    /**
     * bit 7: LCD & PPU enable: 0 = Off; 1 = On
     * bit 6: Window tile map area: 0 = 9800–9BFF; 1 = 9C00–9FFF
     * bit 5: Window enable: 0 = Off; 1 = On
     * bit 4: BG & Window tile data area: 0 = 8800–97FF; 1 = 8000–8FFF
     * bit 3: BG tile map area: 0 = 9800–9BFF; 1 = 9C00–9FFF
     * bit 2: OBJ size: 0 = 8×8; 1 = 8×16
     * bit 1: OBJ enable: 0 = Off; 1 = On
     * bit 0: BG & Window enable / priority [Different meaning in CGB Mode]: 0 = Off; 1 = On
     */    
    public static int LCDC = 0x91; // LCD control

    public static final int LCDC_FLAG_BG_ENABLED = 0;
    public static final int LCDC_FLAG_OBJ_ON = 1;
    public static final int LCDC_FLAG_OBJ_SIZE_8x16 = 2;
    public static final int LCDC_FLAG_BG_MAP_9C00 = 3;
    public static final int LCDC_FLAG_BG_WIN_DATA_8000 = 4;
    public static final int LCDC_FLAG_WIN_ENABLED = 5;
    public static final int LCDC_FLAG_WIN_MAP_9C00 = 6;
    public static final int LCDC_FLAG_ENABLED = 7;

    private static boolean getBitLCDCF(int bit) {
        return (LCDC & (1 << bit)) > 0;
    }

    public static int STAT = 0x81; // FF41 — STAT: LCD status

    // bit = 0-7
    public static void setBitSTAT(int bit, int value) {
        if (value == 1) {
            STAT = STAT | (1 << bit);
        }
        else if (value == 0) {
            STAT = STAT & ~(1 << bit);
        }    
    }

    public static int getBitSTAT(int bit) {
        return (STAT & (1 << bit)) > 0 ? 1 : 0;
    }

     /**
     * bit 6   LYC int select (Read/Write): If set, selects the LYC == LY condition for the STAT interrupt.
     * bit 5   Mode 2 int select (Read/Write): If set, selects the Mode 2 condition for the STAT interrupt.
     * bit 4   Mode 1 int select (Read/Write): If set, selects the Mode 1 condition for the STAT interrupt.
     * bit 3   Mode 0 int select (Read/Write): If set, selects the Mode 0 condition for the STAT interrupt.
     * bit 2   LYC == LY (Read-only): Set when LY contains the same value as LYC; it is constantly updated.
     * bit 0,1 PPU mode (Read-only): Indicates the PPU’s current status.
     */
    public static int STAT_BIT_LYC_EQ_LY = 2;
    public static int STAT_BIT_MODE0 = 3;
    public static int STAT_BIT_MODE1 = 4;
    public static int STAT_BIT_MODE2 = 5;
    public static int STAT_BIT_LYC = 6;

    public static int SCX = 0; // viewport scroll X
    public static int SCY = 0; // viewport scroll Y
    
    public static int LY = 0; // LCD Y coordinate
    public static int LYC = 0; // LY compare

    public static int BGP = 0xfc; // BG palette data
    public static int OBP0 = 0; // OBJ palette 0 data
    public static int OBP1 = 0; // OBJ palette 1 data
    
    public static int WX = 0; // Window Y position
    public static int WY = 0; // Window X position plus 7

    public static boolean framebufferUpdated = false;
    public static BufferedImage framebuffer = new BufferedImage(160, 144, BufferedImage.TYPE_INT_RGB);
    public static final int[][] pixelIndexBuffer = new int[144][160];
    
    private static int obpColor(int attr, int cindex) {
        int OBP = (attr & 0x10) > 0 ? OBP1 : OBP0;
        int s = cindex << 1;
        int c = (OBP & (0b11 << s)) >> s;
        return c;
    }

    public static void drawSprite(int spriteId) {
        int oamBase = 0xFE00 + 4 * spriteId; // FE00-FE9F
        int spriteY = vram[oamBase + 0];
        int spriteX = vram[oamBase + 1];
        int spriteTileIndex = vram[oamBase + 2];
        int spriteAttr = vram[oamBase + 3];

        int baseAddr = 0x8000;
        int tileAddr = baseAddr + spriteTileIndex * 16;
        
        final int FLIP_X = 5;
        final int FLIP_Y = 6;
        final int PRIORITY = 7;

        for (int y = 0; y < 8; y++) {
            int lineAddr = tileAddr + y * 2;
            for (int x = 0; x < 8; x++) {
                int tmpX = 7 - x;
                if ((spriteAttr & (1 << FLIP_X)) > 0) {
                    tmpX = x;
                }
                int b = (1 << tmpX);
                int cindex = ((vram[lineAddr] & b) >> tmpX) + (((vram[lineAddr + 1] & b) >> tmpX) << 1);
                if (cindex > 0) {
                    int pixelColor = PALETTE[obpColor(spriteAttr, cindex)];

                    int sprtX = spriteX + x - 8;
                    int sprtY = spriteY + y - 16;

                    if ((spriteAttr & (1 << FLIP_Y)) > 0) {
                        sprtY = spriteY + (8 - y) - 16;
                        if (getBitLCDCF(LCDC_FLAG_OBJ_SIZE_8x16)) {
                            sprtY += 8;
                        }
                    }

                    
                    if (sprtX >= 0 && sprtX < 160 && sprtY >= 0 & sprtY < 144) {
                        if ((spriteAttr & (1 << PRIORITY)) > 0) {
                            // draw only if background color index = 0 ?
                            if (pixelIndexBuffer[sprtY][sprtX] == 0) {
                                framebuffer.setRGB(sprtX, sprtY, pixelColor); 
                            }
                        }
                        // sprite on top
                        else {
                            framebuffer.setRGB(sprtX, sprtY, pixelColor); 
                        }
                    }

                }
            }
        }
    }

    // parte de baixo do sprite 8x16
    public static void drawSprite16(int spriteId) {
        int oamBase = 0xFE00 + 4 * spriteId; // FE00-FE9F
        int spriteY = vram[oamBase + 0] + 8;
        int spriteX = vram[oamBase + 1];
        int spriteTileIndex = vram[oamBase + 2];
        int spriteAttr = vram[oamBase + 3];

        int baseAddr = 0x8000;
        int tileAddr = baseAddr + (spriteTileIndex + 1) * 16;
        
        final int FLIP_X = 5;
        final int FLIP_Y = 6;
        final int PRIORITY = 7;

        for (int y = 0; y < 8; y++) {
            int lineAddr = tileAddr + y * 2;
            for (int x = 0; x < 8; x++) {
                int tmpX = 7 - x;
                if ((spriteAttr & (1 << FLIP_X)) > 0) {
                    tmpX = x;
                }
                int b = (1 << tmpX);
                int cindex = ((vram[lineAddr] & b) >> tmpX) + (((vram[lineAddr + 1] & b) >> tmpX) << 1);
                if (cindex > 0) {
                    int pixelColor = PALETTE[obpColor(spriteAttr, cindex)];

                    int sprtX = spriteX + x - 8;
                    int sprtY = spriteY + y - 16;

                    if ((spriteAttr & (1 << FLIP_Y)) > 0) {
                        sprtY = spriteY + (8 - y) - 16 - 8;
                    }

                    if (sprtX >= 0 && sprtX < 160 && sprtY >= 0 & sprtY < 144) {
                        if ((spriteAttr & (1 << PRIORITY)) > 0) {
                            // draw only if background color index = 0 ?
                            if (pixelIndexBuffer[sprtY][sprtX] == 0) {
                                framebuffer.setRGB(sprtX, sprtY, pixelColor); 
                            }
                        }
                        // sprite on top
                        else {
                            framebuffer.setRGB(sprtX, sprtY, pixelColor); 
                        }
                    }
                }
            }
        }
    }

    public static int getTilePixel(int tileId, int x, int y) {
        boolean is8000 = getBitLCDCF(LCDC_FLAG_BG_WIN_DATA_8000);

        if (!is8000) { // 8800
            if (tileId < 128) {
                tileId += 128;
            }
            else {
                tileId -= 128;
            }
        }

        //if (!is8000 && tileId > 127) {
        //    System.out.println();
        //}

        int baseAddr = is8000 ? 0x8000 : 0x8800;
        int tileAddr = baseAddr + tileId * 16;
        int lineAddr = tileAddr + y * 2;
        x = 7 - x;
        int b = (1 << x);
        int pixel = ((vram[lineAddr] & b) >> x) + (((vram[lineAddr + 1] & b) >> x) << 1);
        return pixel;
    }

    public static int getBkgPixel(int x, int y) {
        x = (x + SCX) & 0xff;
        y = (y + SCY) & 0xff;

        int px = x;
        int py = y;
        int col = px / 8;
        int row = py / 8;
        int mapAddr = col + (row * 32);
        int tileId = vram[mapAddr + (getBitLCDCF(LCDC_FLAG_BG_MAP_9C00) ? 0x9C00 : 0x9800)];

        int tx = px % 8;
        int ty = py % 8;
        return getTilePixel(tileId, tx, ty);
    }
    public static int getWinPixel(int x, int y) {
        x = x & 0xff;
        y = y & 0xff;

        int px = x;
        int py = y;
        int col = px / 8;
        int row = py / 8;
        int mapAddr = col + (row * 32);
        int tileId = vram[mapAddr + (getBitLCDCF(LCDC_FLAG_WIN_MAP_9C00) ? 0x9C00 : 0x9800)];

        int tx = px % 8;
        int ty = py % 8;
        return getTilePixel(tileId, tx, ty);
    }

    private static void refreshScreen() {
        if (!getBitLCDCF(LCDC_FLAG_ENABLED)) {
            Graphics g = framebuffer.getGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 160, 144);
        }
        
        // TODO prov draw sprites
        // for now, just drawing the 40 sprites at once xD
        if (getBitLCDCF(LCDC_FLAG_OBJ_ON)) {
            for (int s = 0; s < 40; s++) {
                drawSprite(s);
                if (getBitLCDCF(LCDC_FLAG_OBJ_SIZE_8x16)) {
                    drawSprite16(s);
                }
            }
        }

        framebufferUpdated = true;
    }

    private static void setMode(Mode mode) {
        ppuMode = mode;
        switch (ppuMode) {
            case MODE0_HBLANK:
                STAT = (STAT & 0xfc) | 0; 
                if (getBitSTAT(STAT_BIT_MODE0) == 1) {
                    Irq.setBitIF(Irq.IRQ_BIT_LCD, 1);
                }
                break; 
            
            case MODE1_VBLANK: 
                STAT = (STAT & 0xfc) | 1; 
                //if (getBitSTAT(STAT_BIT_MODE1) == 1) {
                    Irq.setBitIF(Irq.IRQ_BIT_VBLANK, 1);
                //}
                break;
            
            case MODE2_OAM_SCAN: 
                STAT = (STAT & 0xfc) | 2; 
                if (getBitSTAT(STAT_BIT_MODE2) == 1) {
                    Irq.setBitIF(Irq.IRQ_BIT_LCD, 1);
                }
                break;

            case MODE3_DRAW_PIXELS: 
                STAT = (STAT & 0xfc) | 3; 
                break;
        }
        //System.out.println("ppu: " + ppuMode);
    }

    private static final int[] PALETTE = { 
        //Color.BLACK.getRGB(), Color.DARK_GRAY.getRGB(), Color.GRAY.getRGB(), Color.WHITE.getRGB(), Color.RED.getRGB()
        Color.WHITE.getRGB(), Color.GRAY.getRGB(), Color.DARK_GRAY.getRGB(), Color.BLACK.getRGB(), Color.RED.getRGB()
    };

    private static int bgpColor(int cindex) {
        int s = cindex << 1;
        int c = (BGP & (0b11 << s)) >> s;
        return c;
    }

    //private static int dotCount = 0;

    public static void nextDot() {
        //System.out.println("LX = " + LX + " LY=" + LY);

        if (ppuMode == Mode.MODE3_DRAW_PIXELS) {
            if (LX - 80 >= 0 && LX - 80 < 160) {
                int dispX = LX - 80;
                int dispY = LY;
                int cindex = getBkgPixel(dispX, dispY);

                int wx = WX - 8;
                int wy = WY;
                if (getBitLCDCF(LCDC_FLAG_WIN_ENABLED) && dispX >= wx && dispY >= wy) {
                    cindex = getWinPixel(dispX - wx, dispY - wy);
                }
        
                int pixelColor = PALETTE[bgpColor(cindex)];
                framebuffer.setRGB(LX - 80, LY, pixelColor); 
                pixelIndexBuffer[LY][LX - 80] = cindex;
            }
        }

        processedTCycles++;
        //dotCount++;

        // update lx, ly
        LX = LX + 1;

        if (LX > 455) {
            LX = 0;

            LY = LY + 1;
            if (LY > 153) {
                LY = 0;

                // System.out.println("total dots: " + dotCount);
                //dotCount = 0;
            }
            
            if (LY == 144) {
                if (LX == 0) {
                    refreshScreen();
                }
                setMode(Mode.MODE1_VBLANK);
            }
    
            // set STAT STAT_BIT_LYC_EQ_LY bit accordingly
            setBitSTAT(STAT_BIT_LYC_EQ_LY, (LY == LYC) ? 1 : 0) ;
            if (LY == LYC) {
                if (getBitSTAT(STAT_BIT_LYC) == 1) {
                    Irq.setBitIF(Irq.IRQ_BIT_LCD, 1);
                }
            }

        }

        if (LY < 144) {
            if (LX == 0) {
                setMode(Mode.MODE2_OAM_SCAN);
            }
            else if (LX == 80) {
                setMode(Mode.MODE3_DRAW_PIXELS);
            }
            else if (LX == 252) {
                setMode(Mode.MODE0_HBLANK);
            }
        }
    }

    public static void process(int totalTCyles) {
        int remainingTCycles = totalTCyles - processedTCycles;
        while (remainingTCycles-- > 0) {
            nextDot();
            Dma.process();
        }
    }

}
