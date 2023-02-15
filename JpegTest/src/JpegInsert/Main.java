package JpegInsert;

import JpegInsert.Jpeg;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        String sourcePaths [] = new String[10];
        int count = 2;
        String destPath= "src/JpegInsert/resource/3.jpg";
        sourcePaths[0]= "src/JpegInsert/resource/1.jpg";
        sourcePaths[1]= "src/JpegInsert/resource/2.jpg";

        Jpeg jpeg = new Jpeg();
        jpeg.insertFramesToJpeg(destPath, sourcePaths, count);
    }
}