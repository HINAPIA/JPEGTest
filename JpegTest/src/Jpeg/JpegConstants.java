package Jpeg;

import java.util.HashMap;
import java.util.HashSet;

public class JpegConstants {

    public static HashMap<Integer, String> nameHashMap = new HashMap<Integer,String>(){{
        put(480, "APP1");
        put(481, "APP2");
        put(482, "APP3");
        put(483, "APP4");
        put(492, "APP13");
        put(493, "APP14");
        put(494, "APP15");
        put(479, "JFIF");
        put(447, "SOF0");
        put(448, "SOF1");
        put(449, "SOF2");
        put(450, "SOF3");
        put(451, "DHT");
        put(452, "SOF5");
        put(453, "SOF6");
        put(454, "SOF7");
        put(455, "SOF8");
        put(456, "SOF9");
        put(457, "SOF10");
        put(458, "SOF11");
        put(459, "DAC");
        put(460, "SOF13");
        put(461, "SOF14");
        put(462, "SOF15");
        put(476, "DRI");
        put(463, "RST0");
        put(464, "RST1");
        put(465, "RST2");
        put(466, "RST3");
        put(467, "RST4");
        put(468, "RST5");
        put(469, "RST6");
        put(470, "RST7");
        put(471, "SOI");
        put(472, "EOI");
        put(473, "SOS");
        put(474, "DQT");

        put(475, "DNL");
        put(509, "COM");


        put(503,"SOM");
        put(496,"MEDIA1");
        put(497,"MEDIA2");
        put(504,"EOM");
    }};
    public static final int JPEG_SOM_MARKER =  0xff + 0xf8;
    public static final int JPEG_MEDIA1_MARKER =  0xff + 0xf1;
    public static final int JPEG_MEDIA2_MARKER =  0xff + 0xf2;
    public static final int JPEG_EOM_MARKER = 0xff + 0xf9;

    public static final int JPEG_APP1_MARKER =  0xff + 0xe1;
    public static final int JPEG_APP2_MARKER =  0xff + 0xe2;
    public static final int JPEG_APP3_MARKER =  0xff + 0xe3;
    public static final int JPEG_APP4_MARKER =  0xff + 0xe4;
    public static final int JPEG_APP13_MARKER = 0xff + 0xed;
    public static final int JPEG_APP14_MARKER =  0xff + 0xee;
    public static final int JPEG_APP15_MARKER =  0xff + 0xef;

    public static final int JFIF_MARKER =  0xff + 0xe0;
    public static final int SOF0_MARKER =  0xff + 0xc0;
    public static final int SOF1_MARKER = 0xff + 0xc1;
    public static final int SOF2_MARKER = 0xff + 0xc2;
    public static final int SOF3_MARKER = 0xff + 0xc3;
    public static final int DHT_MARKER = 0xff + 0xc4;
    public static final int SOF5_MARKER = 0xff + 0xc5;
    public static final int SOF6_MARKER = 0xff + 0xc6;
    public static final int SOF7_MARKER = 0xff + 0xc7;
    public static final int SOF8_MARKER = 0xff + 0xc8;
    public static final int SOF9_MARKER =0xff + 0xc9;
    public static final int SOF10_MARKER = 0xff + 0xca;
    public static final int SOF11_MARKER = 0xff + 0xcb;
    public static final int DAC_MARKER = 0xff + 0xcc;
    public static final int SOF13_MARKER = 0xff + 0xcd;
    public static final int SOF14_MARKER = 0xff + 0xce;
    public static final int SOF15_MARKER =  0xff + 0xcf;

    // marker for restart intervals
    public static final int DRI_MARKER = 0xff + 0xdd;
    public static final int RST0_MARKER =  0xff + 0xd0;
    public static final int RST1_MARKER =  0xff + 0xd1;
    public static final int RST2_MARKER = 0xff + 0xd2;
    public static final int RST3_MARKER =0xff + 0xd3;
    public static final int RST4_MARKER =0xff + 0xd4;
    public static final int RST5_MARKER = 0xff + 0xd5;
    public static final int RST6_MARKER = 0xff + 0xd6;
    public static final int RST7_MARKER = 0xff + 0xd7;

    public static final int SOI_MARKER = 0xff + 0xd8;
    public static final int EOI_MARKER =  0xff + 0xd9;
    public static final int SOS_MARKER = 0xff + 0xda;
    public static final int DQT_MARKER = 0xff + 0xdb;
    public static final int DNL_MARKER = 0xff + 0xdc;
    public static final int COM_MARKER = 0xff + 0xfe;

    public static final int [] MARKERS = {
            JFIF_MARKER,
            JPEG_APP1_MARKER,
            JPEG_APP2_MARKER,
            JPEG_APP3_MARKER,
            JPEG_APP4_MARKER,
            JPEG_APP13_MARKER,
            JPEG_APP14_MARKER,
            JPEG_APP15_MARKER,
            COM_MARKER,
            SOF0_MARKER,
            SOF1_MARKER,
            SOF2_MARKER,
            SOF3_MARKER,
            DHT_MARKER,
            SOF5_MARKER,
            SOF6_MARKER ,
            SOF7_MARKER ,
            SOF8_MARKER ,
            SOF9_MARKER ,
            SOF10_MARKER,
            SOF11_MARKER ,
            DAC_MARKER ,
            SOF13_MARKER ,
            SOF14_MARKER ,
            SOF15_MARKER ,
            // marker for restart intervals
            DRI_MARKER ,
            RST0_MARKER,
            RST1_MARKER,
            RST2_MARKER,
            RST3_MARKER,
            RST4_MARKER,
            RST5_MARKER ,
            RST6_MARKER,
            RST7_MARKER ,
            EOI_MARKER ,
            SOS_MARKER ,
            DQT_MARKER ,
            DNL_MARKER ,
            COM_MARKER,
            SOI_MARKER,
            JPEG_SOM_MARKER,
            JPEG_MEDIA1_MARKER,
            JPEG_MEDIA2_MARKER,
            JPEG_EOM_MARKER

    };

}
