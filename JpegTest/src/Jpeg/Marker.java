package Jpeg;

public class Marker {

    private String name="";
    private int markerValue = 0;
    private int atIndex=0;

    public Marker(){}
    public Marker(int markerValue, int atIndex){
        this.markerValue = markerValue;
        this.atIndex = atIndex;
        this.name = JpegConstants.nameHashMap.get(markerValue);
        System.out.println("[MARKER] name " + name+ ", marker:" + this.markerValue + ", atIndex : "+ this.atIndex);
    }


}
