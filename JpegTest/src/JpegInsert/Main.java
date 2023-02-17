package JpegInsert;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        String sourcePaths [] = new String[10];
        int count = 2;
        String destPath= "src/JpegInsert/resource/1.jpg";
        sourcePaths[0]= "src/JpegInsert/resource/2.jpg";
        sourcePaths[1]= "src/JpegInsert/resource/3.jpg";

        JpegEdit jpeg = new JpegEdit();
        jpeg.insertFramesToJpeg(destPath, sourcePaths, count);
    }
}