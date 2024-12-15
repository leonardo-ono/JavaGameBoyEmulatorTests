public class Dma {

    /*
     * Source:      $XX00-$XX9F   ;XX = $00 to $DF
     * Destination: $FE00-$FE9F
     */
    public static int DMA = 0xff; // DMA
    public static int dmaTransferRemainingDots = 0;

    public static void setDMA(int dma) {
        DMA = dma;
        dmaTransferRemainingDots = 640;
    }
    
    public static void process() {
        /*
        * DMA tranfer
        * Source:      $XX00-$XX9F   ;XX = $00 to $DF
        * Destination: $FE00-$FE9F
        */            
        if (dmaTransferRemainingDots > 0) {
            if (dmaTransferRemainingDots % 4 == 0) {
                int dmaOffset = (640 - dmaTransferRemainingDots) / 4;
                int s = Bus.read((DMA << 8) + dmaOffset);
                Bus.write(0xfe00 + dmaOffset, s);
            }
            dmaTransferRemainingDots--;
            //System.out.println("dma transfering ... " + dmaTransferRemainingDots);
        }
    }

}
