import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Display display = new Display();
            display.setPreferredSize(new Dimension(160 * 3, 144 * 3));
            JFrame frame = new JFrame("Java GameBoy Emulator - Test #01");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(display);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            display.requestFocus();

            String romfile = "rom/rom.gb";
            display.start(romfile);
        });
    }
        
}
