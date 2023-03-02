package Jpeg.Segments;

import Jpeg.Marker;

import java.util.ArrayList;

public abstract class Segment {
    protected ArrayList<Marker> markers;
    protected Segment thumnail;
    protected byte[] segmentData;

    public void addMarker(Marker marker){
        markers.add(marker);
    }
    public void setSegmentData(byte[] data){
        segmentData = data;
    }
}
