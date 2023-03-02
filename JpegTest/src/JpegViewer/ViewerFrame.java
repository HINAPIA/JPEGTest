package JpegViewer;

import javax.swing.*;
import java.awt.*;

public class ViewerFrame extends JFrame {
    JSplitPane splitPane = null;
    PhotoPanel photoPane = null;
    DataOutPanel dataPane = null;
    ToolBar toolBar = null;

    String selectedPath=null;

    public ViewerFrame(){
       super("JEPG Viewr");
       this.setSize(1024, 768);
       this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

       splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
       photoPane = new PhotoPanel();
       splitPane.setDividerSize(0);
       // splotPane의 오른쪽에 사진을 띄우는 Photo Pane을 붙임
       splitPane.setLeftComponent(photoPane);
       splitPane.setDividerLocation(PhotoPanel.SCREEN_WIDTH);

       dataPane = new DataOutPanel();
        // splotPane의 왼쪽에 분석한 데이터를 띄우는 Data Pane을 붙임
       splitPane.setRightComponent(dataPane);
       this.getContentPane().add(splitPane, BorderLayout.CENTER);

       // toolbar 생성 및 붙이기
       toolBar = new ToolBar(this);
       this.getContentPane().add(toolBar, BorderLayout.NORTH);

       this.setResizable(false);
       this.setVisible(true);

    }

    //툴바에서 파일을 선택하면 이미지를 띄우고 분석하는 함수를 실행하는 함수
    public void analysis(String filePath){
        System.out.println(filePath);
        photoPane.viewImage(filePath);
    }
}

class PhotoPanel extends JScrollPane{
    public static final int SCREEN_WIDTH = 684;
    public static final int SCREEN_HEIGHT = 724;
    int drawImageWidth =0;
    int drawImageHeight= 0;

    private ImageIcon imageIcon = null;
    public PhotoPanel(){
        this.setLayout(null);
        this.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        this.setBackground(new Color(0xF4EEFF));
    }

    public void viewImage (String filePath){
        System.out.println("view Image");
        imageIcon = new ImageIcon(filePath);
        Image image = imageIcon.getImage();


        double imageWidth = image.getWidth(this);
        double imageHeight = image.getHeight(this);
        // panel의 사이즈보다 큰 경우 줄이는 작업
        if(imageHeight >= imageWidth){
            while(imageHeight > PhotoPanel.SCREEN_HEIGHT){
                double rate = imageHeight/ (double)imageWidth;
                imageHeight /=1.2;
                imageWidth =  (imageHeight/ rate);
            }
        }
       else{
            while(imageWidth > PhotoPanel.SCREEN_WIDTH){
                double rate = (double)imageWidth/ imageHeight;
                imageWidth /=1.2;
                imageHeight = imageWidth/ rate;
            }
        }
        drawImageWidth = (int)imageWidth;
        drawImageHeight = (int)imageHeight;
        repaint();
    }


    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if(imageIcon != null){
            Image image = imageIcon.getImage();
            g.drawImage(image, PhotoPanel.SCREEN_WIDTH/2 - drawImageWidth/2,
                    PhotoPanel.SCREEN_HEIGHT/2 -drawImageHeight/2,drawImageWidth, drawImageHeight, this);
        }
    }
}


