package JpegInsert;

import JpegParsing.JpegParser;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
//        String sourcePaths [] = new String[10];
//        int count = 1;
          String destPath= "src/JpegInsert/resource/1.jpg";
//        sourcePaths[0]= "src/JpegInsert/resource/2.jpg";
//        //sourcePaths[1]= "src/JpegInsert/resource/3.jpg";
//
//        JpegEdit jpeg = new JpegEdit();
//        // 여러장의 사진을 한 장으로 합치지
//        jpeg.insertFramesToJpeg(destPath, sourcePaths, count);

        JpegParser jpegParser = new JpegParser();
        jpegParser.createMarkers(destPath);
        // 오디오 집어넣기
//        String destPath= "src/JpegInsert/resource/1.jpg";
//        String sourcePath = "src/JpegInsert/resource/audio.mp3";
//        jpeg.insertSegmentToJPEG(destPath, sourcePath);

        JpegEdit jpeg = new JpegEdit();
        //jpeg.insertFramesToJpeg(destPath, sourcePaths, count);

    }
}