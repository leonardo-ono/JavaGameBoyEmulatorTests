

public class Apu {
    // refs.: 
    // https://gbdev.gg8.se/wiki/articles/Gameboy_sound_hardware

    /**
     * CH  Channel	    Sweep	Frequency	    Wave Form	Length Timer	Volume
     * +---+------------+-------+---------------+-----------+------+--------+---------
     *  1  Square 1	    Sweep	Period Counter	Duty	    Length Timer	Envelope
     *  2  Square 2		Period  Counter	        Duty	    Length Timer	Envelope
     *  3  Wave		    Period  Counter	        Wave	    Length Timer	Volume
     *  4  Noise		Period  Counter	        LFSR	    Length Timer	Envelope
     */    

     /**                               Envelope/
      *          Control    Frequency  Volume     Length     Sweep
      *             4          3          2          1          0
      *
      * Pulse A [TL---FFF] [FFFFFFFF] [VVVVAPPP] [DDLLLLLL] [-PPPNSSS]
      *            NR14       NR13       NR12       NR11       NR10
      *
      * Pulse B [TL---FFF] [FFFFFFFF] [VVVVAPPP] [DDLLLLLL] [--------]
      *            NR24       NR23       NR22       NR21       NR20
      *
      * DD = Duty Cycle
      *
      *    Wave [TL---FFF] [FFFFFFFF] [-VV-----] [LLLLLLLL] [E-------]
      *            NR34       NR33       NR32       NR31       NR30
      *                                00=mute               E=enable/disable
      *                                01=100%    L=tentei com valores diferentes mas nao senti diferença?
      *                                10=50%       ah agora funcionou, os valores sao de FF~FE sendo FE o mais curtinho, FF o mais longo depois vem o 00 que tb eh longo 01, 02 e assim por diante
      *                                11=25%
      *                     F = como os valores afetam a frequencia?
      *          T=trigger ok
      *          L=ativa/desativa length ok
      *
      * FF30 W0~W15 16 bytes register -> you can have 32 entries of 4 bits
      *
      *   Noise [TL------] [SSSSWDDD] [VVVVAPPP] [--LLLLLL] [--------]
      *            NR44       NR43       NR42       NR41       NR40
      * 
      * T=Trigger
      * L=desativa o som depois de algum tempo
      * 
      *                         [ALLLBRRR]
      *                            NR50
      *
      *                         [NW21NR21]
      *                            NR51
      *
      *                         [P---NW21]
      *                            NR52
      */

     // Audio Registers: NRxy x -> CH N N=0,1,2,3 or 4
     //                       y -> 0 channel specific feature
     //                            1 controls length timer
     //                            2 controls volume and envelope
     //                            3 controls period
     //                            4 channel's trigger and length timer enable bits etc


     public static final int SAMPLE_RATE = 44100; // <------------
          
     public static int NR52; // FF26 — NR52: Audio master control
     public static int NR51; // FF25 — NR51: Sound panning
     public static int NR50; // FF24 — NR50: Master volume & VIN panning

     // ---------------------------

     private static int[] CH1_PATTERN = new int[8];

     public static int NR10; // FF10 — NR10: Channel 1 sweep
     public static void setNR10(int value) {
        NR10 = value;

        ch1SweepPace = (NR10 & 0x70) >> 4;
        ch1SweepDir = (NR10 & 0x08) > 0 ? 1 : 0; 
        ch1SweepStep = NR10 & 0x07;
        ch1SweepTicks = 0;
     }

     private static int ch1SweepPace = 0;
     private static int ch1SweepDir = 0;
     private static int ch1SweepStep = 0;
     private static double ch1SweepTicks = 0;
     
     public static int NR11; // FF11 — NR11: Channel 1 length timer & duty cycle
     public static int NR12; // FF12 — NR12: Channel 1 volume & envelope

     public static void setNR12(int value) {
        NR12 = value;
        envCh1Vol = (NR12 & 0xF0) >> 4;
        envCh1Dir = (NR12 & 0x08) > 0 ? 1 : 0;
        envCh1SweepPace = (NR12 & 0x07); 
        envCh1Ticks = 0;
        //if (envCh1SweepPace == 0) { 
        //    envCh1Vol = 0; 
        //}
     }
     private static int envCh1Vol = 0;
     private static int envCh1Dir = 0; // 0= decrease / 1=increase
     private static int envCh1SweepPace = 0;
     private static double envCh1Ticks = 0;

     public static int NR13; // FF13 — NR13: Channel 1 period low [write-only]
     public static int NR14; // FF14 — NR14: Channel 1 period high & control
     
     public static void setNR14(int value) {
        NR14 = value;
        ch1Len = NR11 & 0x3f;
        if ((NR14 & 0x80) > 1) {
            setCHXDutyPattern(NR11, CH1_PATTERN);
            setNR10(NR10);
            setNR12(NR12);
            ch1Enabled = true;
        }
     }

     private static boolean ch1Enabled = false;
     private static double ch1Len = 0;

     public static void setCHXDutyPattern(int NRX1, int[] CHX_PATTERN) {
        int wavPattern = 0;
        switch ((NRX1 & 0xC0) >> 6) {
            case 0b00: wavPattern = 0b01111111; break; // 12.5%
            case 0b01: wavPattern = 0b00111111; break; // 25.0%
            case 0b10: wavPattern = 0b00001111; break; // 50.0%
            case 0b11: wavPattern = 0b00000011; break; // 75.0%
        }
        for (int i = 0; i < 8; i++) {
            CHX_PATTERN[i] = (wavPattern & (1 << i)) > 0 ? 0xff : 0x00;
        }
     } 

     private static double ch1SampleIndex = 0;

     // TODO next sample precisa ocorrer a cada 4 TCycles ???
     public static int getCH1NextSample() {
        // trigger disabled

        if (!ch1Enabled) {
            return 0;
        }

        // sweep
        if (ch1SweepPace > 0) {
            ch1SweepTicks += 128.0 / SAMPLE_RATE;
            while (ch1SweepTicks >= ch1SweepPace) {
                ch1SweepTicks -= ch1SweepPace;

                int currPeriod = ((NR14 & 0x07) << 8) + NR13;
                int newPeriod = ch1SweepDir == 0 ? currPeriod + (currPeriod / (1 << ch1SweepStep)) 
                                    : currPeriod - (currPeriod / (1 << ch1SweepStep));
                
                if (newPeriod < 0x000) {
                    newPeriod = 0x000;
                    //NR14 = (NR14 & ~0x80); // clear 'length enable' bit
                    ch1Enabled = false;
                }
                else if (newPeriod > 0x7FF) {
                    newPeriod = 0x7FF;
                    //NR14 = (NR14 & ~0x80); // clear 'length enable' bit
                    ch1Enabled = false;
                }

                NR13 = newPeriod & 0xff;
                NR14 = (NR14 & 0xF8) | ((newPeriod >> 8) & 0x07);
            }
        }

        // length
        if ((NR14 & 0x40) > 0) {
            ch1Len += 256.0 / SAMPLE_RATE;
            if (ch1Len >= 64.0) {
                ch1Len = 0;
                //NR14 = (NR14 & ~0x40); // clear 'length enable' bit
                //NR14 = (NR14 & ~0x80); // clear 'length enable' bit
                ch1Enabled = false;
            }
        }

        // envelope
        if (envCh1SweepPace > 0) {
            envCh1Ticks += 64.0 / SAMPLE_RATE;
            while (envCh1Ticks > envCh1SweepPace) {
                envCh1Ticks -= envCh1SweepPace;
                envCh1Vol = envCh1Dir == 0 ? envCh1Vol - 1 : envCh1Vol + 1;
                
                envCh1Vol = envCh1Vol < 0 ? 0 : envCh1Vol > 15 ? 15 : envCh1Vol;

                if (envCh1Vol == 0 || envCh1Vol == 15) {
                    envCh1SweepPace = 0;
                    break;
                }
            }
        }

        int period_value = ((NR14 & 0x07) << 8) + NR13; // 0b111_11111111;
        double wavSampleRate = 1048576.0 / (2048 - period_value);
        double sampleInc = wavSampleRate / SAMPLE_RATE;
        int si = ((int) ch1SampleIndex) % 8;
        ch1SampleIndex = ch1SampleIndex + sampleInc;
        return (int) ((envCh1Vol / 15.0) * CH1_PATTERN[si]); 
     }

     // --------------------
     


     // ---------------------------

     private static int[] CH2_PATTERN = new int[8];

     //public static int NR20; 
     public static int NR21; //NR21 ($FF16) → NR11
     public static int NR22; //NR22 ($FF17) → NR12
     public static void setNR22(int value) {
        NR22 = value;
        envCh2Vol = (NR22 & 0xF0) >> 4;
        envCh2Dir = (NR22 & 0x08) > 0 ? 1 : 0;
        envCh2SweepPace = (NR22 & 0x07); 
        envCh2Ticks = 0;
        //if (envCh2SweepPace == 0) envCh2Vol = 15; 
     }
     private static int envCh2Vol = 0;
     private static int envCh2Dir = 0; // 0= decrease / 1=increase
     private static int envCh2SweepPace = 0;
     private static double envCh2Ticks = 0;

     public static int NR23; //NR23 ($FF18) → NR13
     public static int NR24; //NR24 ($FF19) → NR14

     public static void setNR24(int value) {
        NR24 = value;
        ch2Len = NR21 & 0x3f;
        if ((NR24 & 0x80) > 1) {
            setCHXDutyPattern(NR21, CH2_PATTERN);
            setNR22(NR22);
            ch2Enabled = true;
        }        
     }
     private static boolean ch2Enabled = false;
     private static double ch2Len = 0;
     private static double ch2SampleIndex = 0;

     // TODO quando NR24 trigger eh ativado, canal canal precisa de um flag proprio indicando 
     //      se o canal esta habilitado ou nao

     // TODO next sample precisa ocorrer a cada 4 TCycles ???
     public static int getCH2NextSample() {
        // trigger disabled
        if (!ch2Enabled) {
            return 0;
        }

        // length
        if ((NR24 & 0x40) > 0) {
            ch2Len += 256.0 / SAMPLE_RATE;
            if (ch2Len >= 64.0) {
                ch2Len = 0;
                //NR24 = (NR24 & ~0x40); // clear 'length enable' bit
                //NR24 = (NR24 & ~0x80); // clear 'length enable' bit
                ch2Enabled = false;
            }
        }

        // envelope
        if (envCh2SweepPace > 0) {
            envCh2Ticks += 64.0 / SAMPLE_RATE;
            while (envCh2Ticks > envCh2SweepPace) {
                envCh2Ticks -= envCh2SweepPace;
                envCh2Vol = envCh2Dir == 0 ? envCh2Vol - 1 : envCh2Vol + 1;
                
                envCh2Vol = envCh2Vol < 0 ? 0 : envCh2Vol > 15 ? 15 : envCh2Vol;

                if (envCh2Vol == 0 || envCh2Vol == 15) {
                    envCh2SweepPace = 0;
                    break;
                }
            }
        }

        int period_value = ((NR24 & 0x07) << 8) + NR23; // 0b111_11111111;
        double wavSampleRate = 1048576.0 / (2048 - period_value);
        double sampleInc = wavSampleRate / SAMPLE_RATE;
        int si = ((int) ch2SampleIndex) % 8;
        ch2SampleIndex = ch2SampleIndex + sampleInc;
        return (int) ((envCh2Vol / 15.0) * CH2_PATTERN[si]); 
     }

     // --------------------
     
     

     // Sound Channel 3 — Wave output
     // o registrador NR34 + NR33 armazena o "period_value" de 11 bits
     // o que eu entendi, o divisor de periodo  deste canal eh 'clocked' em 2097152 Hz
     // ou seja para cada 2 dots do ppu, 1 sample pode ser processado se period_value=0b111_11111111 (= 2047 em decimal)
     // sample rate = 2097152 / (2048 - period_value) Hz
     // e como este canal possui 32 samples de 4 bits, a frequencia (ou pitch) pode ser determinada:
     // frequency (pitch) = (2097152 / 32) / (2048 - period_value) Hz
     // frequency (pitch) = 65536 / (2048 - period_value) Hz
     public static int NR30; // FF1A — NR30: Channel 3 DAC enable
     public static int NR31; // FF1B — NR31: Channel 3 length timer [write-only]
     public static int NR32; // FF1C — NR32: Channel 3 output level
     

     public static int NR33; // FF1D — NR33: Channel 3 period low [write-only]
     public static int NR34; // FF1E — NR34: Channel 3 period high & control

     public static void setNR34(int value) {
        NR34 = value;
        ch3Len = NR31 & 0xff;
        if ((NR34 & 0x80) > 1) {
            ch3Enabled = true;
        }            
     }
     private static boolean ch3Enabled = false;
     private static double ch3Len = 0;
     public static int[] WAV_PATTERN = new int[16]; //FF30–FF3F — Wave pattern RAM

     public static int getWavSample(int s) {
        int i = s / 2;
        int n = s % 2;
        return n == 0 ? (WAV_PATTERN[i] >> 4) & 0x0f : (WAV_PATTERN[i] & 0x0f);
     }

     private static double ch3SampleIndex = 0;

     // TODO next sample precisa ocorrer a cada 2 TCycles ???
     public static int getCH3NextSample() {
        // trigger disabled
        if ((NR30 & 0x80) == 0 || !ch3Enabled) { //} || (NR34 & 0x80) == 0) {
            return 0;
        }

        // length
        if ((NR34 & 0x40) > 0) {
            ch3Len += 256.0 / SAMPLE_RATE;
            if (ch3Len >= 256.0) {
                ch3Len = 0;
                //NR34 = (NR34 & ~0x40); // clear 'length enable' bit
                //NR34 = (NR34 & ~0x80); // clear 'length enable' bit
                ch3Enabled = false;
            }
        }
        
        int period_value = ((NR34 & 0x07) << 8) + NR33; // 0b111_11111111;
        double wavSampleRate = 2097152 / (2048 - period_value);
        double sampleInc = wavSampleRate / SAMPLE_RATE;
        int si = ((int) ch3SampleIndex) % 32;
        ch3SampleIndex = ch3SampleIndex + sampleInc;

        int ch3Vol = (NR32 & 0x60) >> 5;
        switch (ch3Vol) {
            case 0b00: ch3Vol = 0; break;
            case 0b01: ch3Vol = 15; break;
            case 0b10: ch3Vol = 8; break;
            case 0b11: ch3Vol = 4; break;
        }

        return (int) ((ch3Vol / 15.0) * (getWavSample(si) << 4)); 
     }
     
     // --- Noise Channel ---

     public static int NR41; // FF20 — NR41: Channel 4 length timer [write-only]
     public static int NR42; // FF21 — NR42: Channel 4 volume & envelope
     public static void setNR42(int value) {
        NR42 = value;
        envCh4Vol = (NR42 & 0xF0) >> 4;
        envCh4Dir = (NR42 & 0x08) > 0 ? 1 : 0;
        envCh4SweepPace = (NR42 & 0x07); 
        envCh4Ticks = 0;
        //if (envCh4SweepPace == 0) envCh4Vol = 15; 
     }
     private static int envCh4Vol = 0;
     private static int envCh4Dir = 0; // 0= decrease / 1=increase
     private static int envCh4SweepPace = 0;
     private static double envCh4Ticks = 0;


     public static int NR43; // FF22 — NR43: Channel 4 frequency & randomness

     public static int NR44; // FF23 — NR44: Channel 4 control

     public static void setNR44(int value) {
        NR44 = value;
        ch4Len = NR41 & 0x3f;
        if ((NR44 & 0x80) > 1) {
            setNR42(NR42);
            ch4Enabled = true;
            lfsr = 0xff;
        }
     }
     private static boolean ch4Enabled = false;
     private static double ch4SampleIndex = 0;
     private static double ch4Len = 0;
     private static int lfsr = 0xff;

     // The frequency at which the LFSR is clocked is 262144 / (divider × 2^shift) Hz.
     public static int getCH4NextSample() {
        // trigger disabled
        if (!ch4Enabled) {
            return 0;
        }

        // length
        if ((NR44 & 0x40) > 0) {
            ch4Len += 256.0 / SAMPLE_RATE;
            if (ch4Len >= 64.0) {
                ch4Len = 0;
                //NR44 = (NR44 & ~0x40); // clear 'length enable' bit
                //NR44 = (NR44 & ~0x80); // clear 'length enable' bit
                ch4Enabled = false;
            }
        }

        // envelope
        if (envCh4SweepPace > 0) {
            envCh4Ticks += 64.0 / SAMPLE_RATE;
            while (envCh4Ticks > envCh4SweepPace) {
                envCh4Ticks -= envCh4SweepPace;
                envCh4Vol = envCh4Dir == 0 ? envCh4Vol - 1 : envCh4Vol + 1;
                
                envCh4Vol = envCh4Vol < 0 ? 0 : envCh4Vol > 15 ? 15 : envCh4Vol;

                if (envCh4Vol == 0 || envCh4Vol == 15) {
                    envCh4SweepPace = 0;
                    break;
                }
            }
        }


        double divider = NR43 & 0x07;
        if (divider == 0) divider = 0.5;
        int shift = (NR43 & 0xf0) >> 4;
        double wavSampleRate = 262144 / (divider * (1 << shift));

        double sampleInc = wavSampleRate / SAMPLE_RATE;
        ch4SampleIndex = ch4SampleIndex + sampleInc;

        while (ch4SampleIndex >= 1.0) {
            ch4SampleIndex -= 1.0;

            lfsr = (lfsr >> 1) & 0xffff;
            int b0 = lfsr & 1;
            int b1 = (lfsr >> 1) & 1;
            int xor = b0 ^ b1;
            lfsr = lfsr | (xor << 15);

            if ((NR43 & 0x08) > 0) {
                lfsr = (lfsr & 0x7f) | (xor << 7);  
            }
            //System.out.println("lfsr " + lfsr);
        }

        return (int) ((envCh4Vol / 15.0) * (lfsr & 0xff)); 
     }
     
     public static int getNextMixedSample() {
        int mixedsample = 0;
        if ((Apu.NR52 & 0x80) > 0) {
            int ch1 = Apu.getCH1NextSample();
            int ch2 = Apu.getCH2NextSample();
            int ch3 = Apu.getCH3NextSample();
            int ch4 = Apu.getCH4NextSample();
            mixedsample = (byte) ((ch1 + ch2 + ch3 + ch4) >> 2);
        }
        else {
            mixedsample = 0;
        }
        return mixedsample;
     }
     
}
