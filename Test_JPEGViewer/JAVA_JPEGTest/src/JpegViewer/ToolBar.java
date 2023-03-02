package JpegViewer;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToolBar extends JToolBar {
    public static final int WIDTH = 1024;
    public static final int HEIGHT = 44;
    JButton openBtn = new JButton(new ImageIcon("src/resource/img/open.jpg"));
    ViewerFrame frame;

    public ToolBar(ViewerFrame frame){
        super("toolBar");
        this.setSize(WIDTH, HEIGHT);
        this.setFloatable(false);
        this.setBackground(new Color(0x424874));

        this.frame = frame;
        openBtn.setBackground(new Color(0x424874));
        this.add(openBtn);
        openBtn.addActionListener(new OpenListener());

    }


    class OpenListener implements ActionListener {
        private JFileChooser chooser;
        public OpenListener() {
            String projectPath = System.getProperty("user.dir");
            chooser = new JFileChooser(projectPath);
        }
        @Override
        public void actionPerformed(ActionEvent e) {

            int ret = chooser.showOpenDialog(frame);

            FileNameExtensionFilter filter = new FileNameExtensionFilter("JPEG", "jpg");
            chooser.setFileFilter(filter);

            if (chooser.getSelectedFile() != null){
                String filePath = chooser.getSelectedFile().getPath();
                frame.analysis(filePath);
            }
        }
    }
}