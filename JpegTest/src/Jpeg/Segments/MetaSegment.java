package Jpeg.Segments;

import Jpeg.Segments.Segment;

public class MetaSegment extends Segment {
    boolean isThumnailExist;

    public boolean changeThumnail(byte[] data){

        if (!isThumnailExist){
            System.out.println("Error: Thumnail이 존재하지 않음");
            return false;
        }

        //App1 데이터 부터 SOF0 ~ EOI 까지의 데이터를 교체해야함
        // 교체하는 코드
        // Marker로 부터 APP1의 시작 위치 알아내기
        // 원본의 APP1 시작 전까지부터 stream에 쓰고 그 뒤부터 새로운 APP1 옮기기!

        return true;
    }
    public void setThumnail(Segment thumnail){
        this.thumnail = thumnail;
    }
}
