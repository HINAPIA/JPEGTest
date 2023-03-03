package JpegInsert;
import Jpeg.JpegConstants;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;

import Jpeg.JpegConstants;
import Jpeg.Marker;

public class JpegEdit {
    HashMap<Integer, String> markerHashMap = JpegConstants.nameHashMap;
    public static final int SOF0 = JpegConstants.SOF0_MARKER;
    public static final int EOI = JpegConstants.EOI_MARKER;

    // Destination JPEG 파일에  Source 파일들의 Frame을 집어넣는 함수
    public void insertFramesToJpeg(String destPath, String[] sourcePaths , int sourceLegnth) throws IOException {
        byte[] destByte = null;
        byte[][] sourceBytes = new byte[sourceLegnth][];

        byte[][] extractSourceBytes = new byte[sourceLegnth][];
        byte [] resultBytes = null;
        // 1. 파일들의 바이트 배열 얻기
        destByte = getBytes(destPath);
        for(int index =0; index< sourceLegnth; index++){
            sourceBytes[index] = getBytes(sourcePaths[index]);
        }

        // 2. source files의 main frame을 추출
        for(int index = 0; index < sourceBytes.length;index++){
            extractSourceBytes[index] = extractAreaInJPEG(sourceBytes[index], SOF0, EOI);
        }
        //3. 합쳐질 사진에 추출한 프레임을 삽입
        resultBytes = injectFramesToJPEG(destByte,extractSourceBytes);

        //4. 파일로 저장
        writeFile(resultBytes, "src/JpegInsert/resource/result/result.jpg");
        bytesToText("src/JpegInsert/resource/result/result.jpg", "src/JpegInsert/resource/result/result.txt");
        //bytesToText("src/JpegInsert/resource/result/result.jpg", "src/JpegInsert/resource/result/result.txt");
    }
    // jpg 파일에 넣고싶은 데이터(오디오, 텍스트)를 넣는 함수
    public String addMediaToJPEG (String sourcePath, String mediaPath, String type) throws IOException {
        byte [] sourceBytes;
        byte [] mediaBytes;
        // 1. 바이트 배열 얻기
        sourceBytes = getBytes(sourcePath);
        mediaBytes = getBytes(mediaPath);
        // 2. 데이터 삽입
        // 2-1. source 파일 추가
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(sourceBytes);


        // 2-2. SOM 마커 추가
        outputStream.write((byte) -1);
        outputStream.write((byte) (JpegConstants.JPEG_SOM_MARKER - 255));
        // 2-3. 타입에 따라서 marker 추가 - 오디오 : ff a0, 텍스트 : ff a1
//        if(type.equals("audio")){
//            outputStream.write((byte) -1);
//            outputStream.write((byte) (JpegConstants.JPEG_MEDIA1_MARKER - 255));
//        } else if (type.equals("text")) {
//            outputStream.write((byte) -1);
//            outputStream.write((byte) (JpegConstants.JPEG_MEDIA2_MARKER - 255));
//        } else {
//            System.out.println("addMediaToJPEG error : 잘못된 type 입니다.");
//        }
        // 2-4. media 데이터 추가
        outputStream.write(mediaBytes);
        // 2-5. 미디어 테이터의 끝을 나타내는 EOM(ff a9) 마커 추가
//        outputStream.write((byte) -1);
//        outputStream.write((byte) (JpegConstants.JPEG_EOM_MARKER - 255));

        // 3. 저장
        byte[] resultBytes = outputStream.toByteArray();
        String savePath = sourcePath.substring(0, sourcePath.lastIndexOf('.'))  +"_audio.jpg";
        writeFile(resultBytes,savePath);
        // 4. 저장한 파일 경로 리턴
        return savePath;
    }

    //  (Frame이 여러개인 JPEG 대상) 메인 프레임을 바꾸는 함수
    public void changeMainFrame (String filePath, int SOFN) throws IOException { // frame이 여러 개인

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte [] resultBytes = null;
        byte[] sourceBytes = getBytes(filePath); // 바이트 배열로 바꾸기

        // Section1. 함수를 적용할 수 있는 JPEG 인지 확인 - isMultiFrameJpeg(target)


        // Section2. 메인 프레임과 대상 프레임 idx 추출
        //index 추출
        int[] mainFrameIdx = getFrameIdx(SOF0,EOI,sourceBytes); // 현재 메인프레임이 시작되는 위치(sof0Idx) 알아내기
        int[] targetFrameIdx = getFrameIdx(SOFN,EOI,sourceBytes);
        int subFrameIdx = mainFrameIdx[1]+2; // subframe 시작 idx - mainFrame 이 끝난 다음

        // Section3. 메인프레임으로 설정할 SOFn 의 데이터를 해당 위치(sof0Idx)로 옮기고 본 메인프레임은 맨 마지막으로 넣기

        for(int i=0; i< mainFrameIdx[0]; i++){ // main frame 전까지 outputStream에 쓰기
            outputStream.write(sourceBytes[i]);
        }

        // 바뀐 메인 프레임의 마커를 ff c0 으로 변경
        outputStream.write((byte)Integer.parseInt("ff", 16));
        outputStream.write((byte)Integer.parseInt("c0", 16));

        for(int i=targetFrameIdx[0]+2; i < targetFrameIdx[1]+2; i++){ // targetFrame 붙이기
            outputStream.write(sourceBytes[i]);
        }

        for (int i= subFrameIdx; i < sourceBytes.length; i++){ // 나머지 프레임들 붙이기
            if (!(i>=targetFrameIdx[0] && i <= targetFrameIdx[1]+1)){ // target 프레임이 아닌 프레임
                outputStream.write(sourceBytes[i]);
            }
        }

        for (int i=mainFrameIdx[0]; i<mainFrameIdx[1]+2;i++) // 기존 메인프레임 붙이기
            outputStream.write(sourceBytes[i]);

        resultBytes = outputStream.toByteArray();

        // Section4. 파일로 저장
        writeFile(resultBytes, "src/JpegInsert/resource/result/change.jpg");

    } // end of func changeMainFrame..

    public int[] getFrameIdx(int startMarker, int endMarker,byte[] jpegBytes){

        int[] result = new int[2];
        int startIndex =0;
        int endIndex = jpegBytes.length;
        int startCount =0;
        int endCount =0;

        int startMax =1;
        int endMax =1;

        boolean isFindStartMarker = false; // 시작 마커를 찾았는지 여부
        boolean isFindEndMarker = false; // 종료 마커를 찾았는지 여부

        //썸네일의 SOF0가 먼저 나와서 2번 해당 마커를 찾도록
        if(startMarker == SOF0) startMax = 2;

        int n1, n2;
        for (int i = 0; i < jpegBytes.length-1; i++) {
            if((n1 = Integer.valueOf((byte) jpegBytes[i]).intValue()) < 0){
                n1 += 256;
            }
            if((n2 = Integer.valueOf((byte) jpegBytes[i+1]).intValue()) < 0){
                n2 += 256;
            }

            int twoByteToNum = n1+n2;

            if(markerHashMap.containsKey(twoByteToNum) && n1== 255){ // n1 == ff

                //System.out.println("start hex string : " + hexString);
                if (twoByteToNum == startMarker) {
                    startCount++;
                    if(startCount == startMax){
                        startIndex = i;
                        result[0] = startIndex;
                        isFindStartMarker = true;
                    }
                }

                if (isFindStartMarker) { // 조건에 부합하는 start 마커를 찾은 후, end 마커 찾기
                    if (twoByteToNum == endMarker) {
                        endCount++;
                        if(endCount == endMax){
                            endIndex = i;
                            result[1] = endIndex;
                            isFindEndMarker = true;
                        }
                    }
                }

            }
        }

        if (!isFindStartMarker || !isFindEndMarker){
            System.out.println(isFindStartMarker);
            System.out.println(isFindEndMarker);
            System.out.println("Error: 찾는 마커가 존재하지 않음");
            return null;
        }

        return result;
    }

    // Dest 파일 바이너리 데이터에 Source 파일들의 메인 프레임(SOF0 ~ EOI) 바이너리 데이터를 넣는 함수
    public byte [] injectFramesToJPEG(byte[] destByte, byte[][] extractSourceBytes ) throws IOException {
        byte [] resultBytes;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // 1. Stream에 Dest파일의 바이너리 데이터를 write
        outputStream.write(destByte);
        for(int i=0; i< extractSourceBytes.length; i++){
            // 2. 추출한 Source 파일의 메
            byte [] frameByte = new byte [(extractSourceBytes[i].length) -2];
            //SOF0 마커 삭제하여
            System.arraycopy(extractSourceBytes[i], 2, frameByte, 0,extractSourceBytes[i].length -2);
            //SOFn 마커 삽입
            String marker = "c"+(i+1);
            outputStream.write((byte)Integer.parseInt("ff", 16));
            outputStream.write((byte)Integer.parseInt(marker, 16));
            //SOFn마커를 제외한 frame 데이터 write - EOI 포함
            outputStream.write(frameByte);
//EOI 삽입
            //outputStream.write((byte)Integer.parseInt("ff", 16));
            //outputStream.write((byte)Integer.parseInt("d9", 16));
        }


        resultBytes = outputStream.toByteArray();
        return resultBytes;
    }
    //JPEGFile에서 startMarker가 나오는 부분부터 endMarker가 나오기 전까지 추출하여 byte []로 리턴하는 함수
    public byte[] extractAreaInJPEG(byte [] jpegBytes ,int startMarker, int endMarker) throws IOException {
        int n1, n2;
        byte [] resultBytes;
        int startIndex =0;
        int endIndex = jpegBytes.length;
        int startCount =0;
        int endCount =0;
//EOI 삽입
        //outputStream.write((byte)Integer.parseInt("ff", 16));
        //outputStream.write((byte)Integer.parseInt("d9", 16));
        int startMax =1;
        int endMax =1;

        boolean isFindStartMarker = false; // 시작 마커를 찾았는지 여부
        boolean isFindEndMarker = false; // 종료 마커를 찾았는지 여부

        //썸네일의 SOF0가 먼저 나와서 2번 해당 마커를 찾도록
        if(startMarker == SOF0) startMax = 2;


        for (int i = 0; i < jpegBytes.length-1; i++) {

            if((n1 = Integer.valueOf((byte) jpegBytes[i]).intValue())  < 0){
                n1 += 256;
            }
            if((n2 = Integer.valueOf((byte) jpegBytes[i+1]).intValue())  < 0){
                n2 += 256;
            }

            int twoByteToNum = n1+n2;

            if(markerHashMap.containsKey(twoByteToNum) && n1== 255){
                System.out.println("마커 찾음 : "+ i+": "+twoByteToNum);
                System.out.println("n1 : "+ n1 + ", n2 :"+ n2) ;
                if (twoByteToNum == startMarker) {
                    startCount++;
                    if(startCount == startMax){
                        startIndex = i;
                        isFindStartMarker = true;
                    }
                }

                if (isFindStartMarker) { // 조건에 부합하는 start 마커를 찾은 후, end 마커 찾기
                    if (twoByteToNum == endMarker) {
                        endCount++;
                        if(endCount == endMax){
                            endIndex = i;
                            isFindEndMarker = true;
                        }
                    }
                }

            }
        }

        if (!isFindStartMarker || !isFindEndMarker){
            System.out.println("Error: 찾는 마커가 존재하지 않음");
            return null;
        }

        // 추출
        resultBytes = new byte[endIndex-startIndex+2];
        // start 마커부터 end 마커를 포함한 영역까지 복사해서 resultBytes에 저장
        System.arraycopy(jpegBytes, startIndex, resultBytes, 0,endIndex-startIndex+2);

        return resultBytes;
    }
    // byte 배열과 저장할 파일 이름을 입력 받아 해당 파일 이름으로 파일로 저장하는 함수
    public  void writeFile(byte[] bytes, String saveFilePath) {
        try {

            // 1. 파일 객체 생성
            File file = new File(saveFilePath);
            // 2. 파일 존재여부 체크 및 생성
            if (!file.exists()) {
                file.createNewFile();
            }
            // 3. Writer 생성
            FileOutputStream fos = new FileOutputStream(file);

            // 4. 파일에 쓰기
            fos.write(bytes);

            // 5. FileOutputStream close
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // filePath를 받아 해당 경로에 있는 파일을 읽고 데이터를 Hex 코드로 바꾸어 txt파일로 저장하는 함수
    public void bytesToText(String filePath, String SaveFilePath)  throws IOException {
        File sfile = new File(filePath);
        FileInputStream fileis = new FileInputStream(sfile);
        BufferedInputStream bis = new BufferedInputStream(fileis);
        //buffer 할당
        byte[] arrByte = new byte[bis.available()];
        StringBuilder data = new StringBuilder();
        int ioffs = 0;// 번지(라인)
        int iLine;// 길이
        String space = "   "; // 자리수 맞출 공백

        System.out.println("iLine:" + ((iLine = bis.read(arrByte))));

        if (iLine > 0) {

            int end = (int) (iLine / 16) + (iLine % 16 != 0 ? 1 : 0); //  ü  ټ        .
            System.out.println("end:" + end);

            for (int i = 0; i < end; i++) {

                //번지 출력
                //  System.out.format("%08X: ", ioffs); // Offset :
                //헥사구역
                for (int j = ioffs; j < ioffs + 16; j++) { //16
                    if (j < iLine) {
                        //System.out.format("%02X ", arrByte[j]); //        2      , %x 16
                        data.append(String.format("%02x ", arrByte[j]));
                    } else {
                        System.out.print(space);
                    }
                }// for
                ioffs += 16; //번지수 증가.
            }
        }

        bis.close();
        fileis.close();
        writeFile(data.toString().getBytes(), SaveFilePath);
        System.out.print("파일 저장 완료");

    }

    //파일경로를 받아 해당 파일을 바이트 배열로
    public static byte[] getBytes(String filePath) {

        System.out.println("getBytes [filePath] : " + filePath);
        byte[] byteFile = null;

        try {
            byteFile = Files.readAllBytes(new File(filePath).toPath());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return byteFile;


    }

    public static void readFile(String filePath) throws IOException{
        BufferedReader reader = new BufferedReader(
                new FileReader(filePath)
        );

        String str;
        while ((str = reader.readLine()) != null) {
            System.out.println(str);
        }

        reader.close();
    }
    public void stringTobytes (String hexString) {

        byte[] ans = new byte[hexString.length() / 2];
        for (int i = 0; i < ans.length; i++) {
            int index= 0;
            if(i != 0) {
                index = i * 3;
                if(index >= ans.length) break;
            }
            // Using parseInt() method of Integer class
            int val = Integer.parseInt(hexString.substring(index, index + 2), 16);
            ans[i] = (byte)val;
        }

        // Printing the required Byte Array
        System.out.print("Byte Array : ");
        System.out.print("파일 저장 완료");
        writeFile(ans, "src/resource/result/result test3.jpg");
    }

    public void saveFile(String filePath) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(
                new FileReader(filePath)
        );
        String data = "",str;

        System.out.println("읽기 전");
        while ((str = bufferedReader.readLine()) != null) {
            data += str;
            //System.out.print(data);
        }
        System.out.println("읽기 완료");

        stringTobytes(data);
    }
}