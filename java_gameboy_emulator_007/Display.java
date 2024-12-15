import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public class Display extends Canvas implements Runnable, KeyListener {
        
    //private List<String> logs = new ArrayList<>();
    //private static PrintWriter logwriter;

    private BufferStrategy bs;
    private Thread mainThread;

    public Display() {
        
    }
    
    private void loadRom(String romfile) {
        try {
            String basePath = System.getProperty("user.dir") + "/";
            
            InputStream is = new FileInputStream(basePath + romfile);// getClass().getRjavaesourceAsStream("rom4.gb");
            int i = 0;
            int c = 0;
            while ((c = is.read()) >= 0) {
                //System.out.println("i = " + i);
                Bus.rom[i++] = c;
            }
            is.close();
        }
        catch (Exception e) {
            System.out.println("error loading rom !");
            System.exit(-1);
        }
    }

    private static final int BUFFER_SIZE = 1024;
    private static SourceDataLine dataLine;
    private byte[] mixedsample = new byte[1];

    private static void initializeAudio() {
        try {
            AudioFormat audioFormat = new AudioFormat(Apu.SAMPLE_RATE, 8, 1, false, false);
            dataLine = (SourceDataLine) AudioSystem.getSourceDataLine(audioFormat);
            dataLine.open(audioFormat, BUFFER_SIZE);
            dataLine.start();  
        }
        catch (Exception e) {
            System.out.println("could not initialize audio !");
        }
    }

    public void start(String romfile) {
        if (mainThread == null) {
            loadRom(romfile);
            initializeAudio();
            setIgnoreRepaint(true);
            createBufferStrategy(2);
            bs = getBufferStrategy();
            mainThread = new Thread(this);
            mainThread.start();
            addKeyListener(this);

            //try {
            //    String basePath = System.getProperty("user.dir") + "\\";
            //    logwriter = new PrintWriter(basePath + "debug.txt");
            //} catch (FileNotFoundException e) {
            //    e.printStackTrace();
            //}
        }
    }
    
    public void draw(Graphics2D g) {
        g.setColor(Color.RED);
        g.drawLine(0, 0, getWidth(), getHeight());
    }

    @Override
    public void run() {
        
        double processedApu = 0;
        double tcyclesPerSample = 4194304.0 / Apu.SAMPLE_RATE;
        
        // main loop
        while (Cpu.running) {
            //if (Input.printDebug) {
            //    logwriter.println("pc="+ Cpu.RPC + " " + Cpu.lastMnemonic);
            //}
            
            Irq.process();
            Cpu.executeNextInstr();
            Ppu.process(Cpu.tc);
            Timer.process(Cpu.tc);
            
            if (Ppu.framebufferUpdated) {
                Ppu.framebufferUpdated = false;
                //System.out.println("updating screen");
                Graphics2D g = (Graphics2D) bs.getDrawGraphics();
                g.drawImage(Ppu.framebuffer, 0, 0, getWidth(), getHeight(), null);       
                g.dispose();
                bs.show();
            }
    
            double remainingApu = Cpu.tc - processedApu;
            while (remainingApu > tcyclesPerSample) {
                processedApu += tcyclesPerSample;
                remainingApu = Cpu.tc - processedApu;
                mixedsample[0] = (byte) Apu.getNextMixedSample();
                dataLine.write(mixedsample, 0, mixedsample.length);                 
            }

        }         

    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT: Joypad.setFlagDPAD(Joypad.JOYP_BIT_RIGHT, 0); break;
            case KeyEvent.VK_LEFT: Joypad.setFlagDPAD(Joypad.JOYP_BIT_LEFT, 0); break;
            case KeyEvent.VK_UP: Joypad.setFlagDPAD(Joypad.JOYP_BIT_UP, 0); break;
            case KeyEvent.VK_DOWN: Joypad.setFlagDPAD(Joypad.JOYP_BIT_DOWN, 0); break;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_Z: Joypad.setFlagBUTT(Joypad.JOYP_BIT_A, 0); break;
            case KeyEvent.VK_X: Joypad.setFlagBUTT(Joypad.JOYP_BIT_B, 0); break;
            case KeyEvent.VK_ENTER: Joypad.setFlagBUTT(Joypad.JOYP_BIT_SELECT, 0); break;
            case KeyEvent.VK_BACK_SPACE: Joypad.setFlagBUTT(Joypad.JOYP_BIT_START, 0); break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT: Joypad.setFlagDPAD(Joypad.JOYP_BIT_RIGHT, 1); break;
            case KeyEvent.VK_LEFT: Joypad.setFlagDPAD(Joypad.JOYP_BIT_LEFT, 1); break;
            case KeyEvent.VK_UP: Joypad.setFlagDPAD(Joypad.JOYP_BIT_UP, 1); break;
            case KeyEvent.VK_DOWN: Joypad.setFlagDPAD(Joypad.JOYP_BIT_DOWN, 1); break;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_Z: Joypad.setFlagBUTT(Joypad.JOYP_BIT_A, 1); break;
            case KeyEvent.VK_X: {
                Joypad.setFlagBUTT(Joypad.JOYP_BIT_B, 1); 
                break; 
            }

            // debug todo remove later
            case KeyEvent.VK_Q: Joypad.printDebug = true; break;

            case KeyEvent.VK_ENTER: Joypad.setFlagBUTT(Joypad.JOYP_BIT_SELECT, 1); break;
            case KeyEvent.VK_BACK_SPACE: Joypad.setFlagBUTT(Joypad.JOYP_BIT_START, 1); break;
        }
    }


}
