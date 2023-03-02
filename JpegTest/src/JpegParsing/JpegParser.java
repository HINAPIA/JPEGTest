package JpegParsing;

import Jpeg.Marker;
import JpegInsert.JpegEdit;
import Jpeg.JpegConstants;

import java.util.HashSet;
import JpegInsert.JpegEdit;
import Jpeg.JpegConstants;

import java.util.ArrayList;
import java.util.HashSet;

public class JpegParser {

    HashSet<Integer> markersHashSet = new HashSet<Integer>();
    public JpegParser(){
        int [] markers = JpegConstants.MARKERS;
        for(int i=0; i< markers.length; i++){
            markersHashSet.add(markers[i]);
        }
    }
    public ArrayList<Marker> createMarkers(String filePath){
        ArrayList<Marker> markerArrayList = new ArrayList<>();
        byte[] bytes = null;
        int n1, n2;
        byte [] currentByte = new byte[2];
        bytes = JpegEdit.getBytes(filePath);
        boolean isDetect = true;
        Marker marker;
        bytes = JpegEdit.getBytes(filePath);
        //bytes.length-1
        for (int i = 0; i <bytes.length-1 ; i++){
            //byte 단위로 끊은 값은 -128~127 범위의 값으로 나옴
            // 값을 0~255 범위의 Int형으로 바꿈
            // -1 -> 255
            if((n1 = Integer.valueOf((byte) bytes[i]).intValue())  < 0){
                n1 += 256;
            }
            if((n2 = Integer.valueOf((byte) bytes[i+1]).intValue())  < 0){
                n2 += 256;
            }
            int twoByteToNum = n1+n2;
            // 마커는 ff 로 시작하는 특징이 있으므로 첫 번째 숫자가 255(ff)인 경우만 마커로 체크
            if(markersHashSet.contains(twoByteToNum) && n1== 255){
                //System.out.println("[find marker] marker : " + String.format("%02x %02x",  bytes[i], bytes[i+1]) +" ,Index : "+ i);
               // marker = new Marker(twoByteToNum, i);
//                if(twoByteToNum == JpegConstants.EOI_MARKER) {
//                    marker = new Marker(twoByteToNum, i);
//                    markerArrayList.add(marker);
//                    break;
//                }
                marker = new Marker(twoByteToNum, i);
                markerArrayList.add(marker);
            }

        }
        return markerArrayList;
    }
}
