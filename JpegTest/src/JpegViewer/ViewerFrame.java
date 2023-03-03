package JpegViewer;

import JpegInsert.JpegEdit;
import JpegParsing.JpegParser;
import Jpeg.JPEGImage;
import Jpeg.JpegConstants;
import Jpeg.Marker;
import JpegInsert.JpegEdit;
import JpegParsing.JpegParser;

import javax.swing.*;
import java.awt.*;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

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
    private JLabel imageLabel;
    private JpegEdit jpegEdit= new JpegEdit();
    private ImageIcon imageIcon = null;
    private JPEGImage jpegImage;
    private JpegParser jpegParser = new JpegParser();
    private String imagePath;

    public PhotoPanel(){
        this.setLayout(null);
        imageLabel = new JLabel();
        imageLabel.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                // 오디오 시작 함수
                //startAudio();
                System.out.println("click");
            }
        });
        this.add(imageLabel);
        this.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        this.setBackground(new Color(0xF4EEFF));
    }
//    public void startAudio() throws IOException {
//        // JPEGImage 생성
//        ArrayList<Marker> markers;
//        markers = jpegParser.createMarkers(imagePath);
//        jpegImage = new JPEGImage(markers);
//        // 오디오 부분 떼서 .wav 파일로 저장
//        System.out.println("startAudio");
//        //Marker somMarker = jpegImage.getMaker(JpegConstants.JPEG_SOM_MARKER).get();
//        if(somMarker == null){
//            System.out.println("추가된 오디오 파일이 없습니다.");
//            return;
//        }
//        System.out.println("SOM : "+ somMarker.getAtIndex());
//        byte [] selectBytes = JpegEdit.getBytes(imagePath);
//        byte [] audioBytes = new byte[selectBytes.length-somMarker.getAtIndex() +2];
//        //System.arraycopy(selectBytes, somMarker.getAtIndex() +2, audioBytes, 0,selectBytes.length-somMarker.getAtIndex() +2);
//        File file = new File("src/resource/audio/play.wav");
//        FileOutputStream fos = new FileOutputStream(file);
//        fos.write(selectBytes, somMarker.getAtIndex() +2,selectBytes.length-(somMarker.getAtIndex()+2));
//        fos.close();
//
//        //오디오 재생
//        try {o'

//            Clip clip = AudioSystem.getClip();
//            clip.open(AudioSystem.getAudioInputStream(file));
//            // clip.loop(Clip.LOOP_CONTINUOUSLY);
//            clip.loop(Clip.LOOP_CONTINUOUSLY);
//            clip.start();
//        } catch (Exception e) {
//            System.err.println("Put the music.wav file in the sound folder if you want to play background music, only optional!");
//        }
//    }
    public void viewImage (String filePath){
        System.out.println("view Image");
        imagePath = filePath;
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
        // 크기 변경
        Image chageImage = image.getScaledInstance(drawImageWidth,drawImageHeight, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(chageImage));
        imageLabel.setSize(drawImageWidth, drawImageHeight);
        imageLabel.setLocation(PhotoPanel.SCREEN_WIDTH/2 - drawImageWidth/2,PhotoPanel.SCREEN_HEIGHT/2 -drawImageHeight/2);
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
//        if(imageIcon != null){
//            Image image = imageIcon.getImage();
//            g.drawImage(image, PhotoPanel.SCREEN_WIDTH/2 - drawImageWidth/2,
//                    PhotoPanel.SCREEN_HEIGHT/2 -drawImageHeight/2,drawImageWidth, drawImageHeight, this);
//        }

    }
}


