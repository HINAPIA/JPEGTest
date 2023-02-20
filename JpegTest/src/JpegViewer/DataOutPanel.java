package JpegViewer;

import javax.swing.*;
import java.awt.*;

public class DataOutPanel extends JPanel {
    public static final int SCREEN_WIDTH = 340;
    public static final int SCREEN_HEIGHT = 724;
    DataScrollPane dataScrollPane;
    DataInPanel dataInPanel;

    public DataOutPanel() {
        this.setLayout(null);
        this.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        this.setBackground(new Color(0xA6B1E1));
        dataInPanel = new DataInPanel();
        dataScrollPane = new DataScrollPane(dataInPanel);

        dataScrollPane.setLocation(21, 20);
        this.add(dataScrollPane);
        this.setVisible(true);
    }
}
class DataScrollPane extends JScrollPane {
    public static final int SCREEN_WIDTH = 286;
    public static final int SCREEN_HEIGHT = 650;
    public DataScrollPane(DataInPanel dataInPanel){
        super(dataInPanel);
        this.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        this.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    }
}

class DataInPanel extends JPanel{
    public static final int SCREEN_WIDTH = 286;
    public static final int SCREEN_HEIGHT = 2000;
    public DataInPanel(){
        this.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        this.setBackground(new Color(0xFFFFFF));
    }
}