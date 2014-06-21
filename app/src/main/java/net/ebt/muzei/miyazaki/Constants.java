package net.ebt.muzei.miyazaki;

import android.util.SparseArray;

public class Constants {

    public static final String SOURCE_NAME = "MuzeiMiyazakiArtSource";
    public static final String CURRENT_PREF_NAME = "MuzeiMiyazakiArtSource.current";
    public static final String BASE_URL = "https://i.imgur.com/";
    public static final SparseArray<Long> INTERVALS = new SparseArray<Long>();
    public static final String MUZEI_INTERVAL = "muzei_interval";
    public static final String MUZEI_WIFI = "muzei_wifi";

    public static final int DEFAULT_INTERVAL;
    private final static long MIN = 1000 * 60;
    private final static long HOUR = MIN * 60;
    private final static long DAY = HOUR * 24;
    static {
        int i = 0;
        if(BuildConfig.DEBUG) {
            INTERVALS.put(i++, 1 * MIN);
        }
        INTERVALS.put(i++, 10 * MIN);
        INTERVALS.put(i++, 30 * MIN);
        INTERVALS.put(i++, 1 * HOUR);
        INTERVALS.put(i++, 2 * HOUR);
        INTERVALS.put(i++, 3 * HOUR);
        INTERVALS.put(i++, 4 * HOUR);
        INTERVALS.put(i++, 5 * HOUR);
        INTERVALS.put(i++, 6 * HOUR);
        INTERVALS.put(i++, 7 * HOUR);
        INTERVALS.put(i++, 8 * HOUR);
        INTERVALS.put(i++, 9 * HOUR);
        INTERVALS.put(i++, 10 * HOUR);
        INTERVALS.put(i++, 11 * HOUR);
        INTERVALS.put(i++, 12 * HOUR);
        INTERVALS.put(DEFAULT_INTERVAL = i++, 1 * DAY); // default is once a day
        INTERVALS.put(i++, 2 * DAY);
        INTERVALS.put(i++, 3 * DAY);
        INTERVALS.put(i++, 4 * DAY);
        INTERVALS.put(i++, 5 * DAY);
        INTERVALS.put(i++, 6 * DAY);
        INTERVALS.put(i++, 7 * DAY);
        INTERVALS.put(i++, 14 * DAY);
        INTERVALS.put(i++, 18 * DAY);
        INTERVALS.put(i++, 30 * DAY);
    }

    public static final String[] FILES = {
            "bYKUtAV.jpg",
            "5WVktYO.jpg",
            "ZLFpbxm.jpg",
            "dyXbuVK.jpg",
            "zEYl5Cp.jpg",
            "0byg3HK.jpg",
            "O8pQHDY.jpg",
            "yZxeGfq.png",
            "OVlKiK7.png",
            "KzUPWxF.jpg",
            "wCqGzTU.png",
            "Eh79054.jpg",
            "klGyEd6.png",
            "omNQMoZ.jpg",
            "RmPrDRL.jpg",
            "Vg3Ruyl.jpg",
            "CliUfqX.jpg",
            "N6tlFJc.jpg",
            "g5Q1NCa.jpg",
            "1ZmrLjK.jpg",
            "tLH51iW.jpg",
            "QQLEwzF.jpg",
            "w9fNuEX.jpg",
            "mUOUz2Y.jpg",
            "pYHoTtp.jpg",
            "rC7mw31.jpg",
            "i1Lv4CY.jpg",
            "hZZRnoR.jpg",
            "2hvl6qP.jpg",
            "9VopmwA.jpg",
            "CMTYkgj.jpg",
            "v9mnBmP.jpg",
            "Mly98OD.jpg",
            "NK4ZyuA.jpg",
            "BB1YAfb.jpg",
            "CpSFYwY.jpg",
            "DC4L7zI.jpg",
            "nxuSi9d.jpg",
            "MEej3vQ.jpg",
            "OKI3GDJ.jpg",
            "4OTXXx5.jpg",
            "09EwMmq.jpg",
            "xXpKKpU.jpg",
            "bNsIXto.jpg",
            "aodIouB.jpg",
            "3wWtNXJ.jpg",
            "530ygYW.jpg",
            "TieinE6.jpg",
            "vWSioof.jpg",
            "OLwHF9J.jpg",
            "NTN0jVS.jpg",
            "yIFFaRt.jpg",
            "sfb9PbR.jpg",
            "d3ca61p.jpg",
            "XU3xQNh.jpg",
            "yMSLTPj.jpg",
            "5Jz8lse.jpg",
            "ApzUMsW.jpg",
            "GXg3FUs.jpg",
            "cwWI4AG.jpg",
            "O1R2quB.jpg",
            "fnjieyM.jpg",
            "f9mYEvO.jpg",
            "FzrnX0c.jpg",
            "fF4PYMn.jpg",
            "LsUvp5g.jpg",
            "vQE6pwk.jpg",
            "oL3ETVL.jpg",
            "JiACR7N.jpg",
            "8GloK0A.jpg",
            "Zq7dJpw.jpg",
            "XcjkS7n.jpg",
            "qEalfZp.jpg"
    };
}
