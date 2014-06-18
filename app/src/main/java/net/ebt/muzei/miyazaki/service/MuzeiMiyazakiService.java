package net.ebt.muzei.miyazaki.service;

import android.net.Uri;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.util.Random;

/**
 * Created by eboudrant on 6/17/14.
 */
public class MuzeiMiyazakiService extends RemoteMuzeiArtSource {

    private static final String TAG = "MuzeiMiyazakiService";
    private static final String SOURCE_NAME = "MuzeiMiyazakiArtSource";
    private static final String BASE_URL = "https://0cb23c22f059f72d8263a1f91d908328e0865e2e.googledrive.com/host/0B3idlWv4TifLckRUZmhydDZkREU/";
    private static final String[] FILES = {
            "01%20-%20bYKUtAV.jpg", "09%20-%20OVlKiK7.png", "17%20-%20CliUfqX.jpg", "25%20-%20pYHoTtp.jpg", "33%20-%20Mly98OD.jpg", "41%20-%204OTXXx5.jpg", "49%20-%20vWSioof.jpg", "57%20-%205Jz8lse.jpg", "65%20-%20fF4PYMn.jpg", "73%20-%20qEalfZp.jpg",
            "02%20-%205WVktYO.jpg", "10%20-%20KzUPWxF.jpg", "18%20-%20N6tlFJc.jpg", "26%20-%20rC7mw31.jpg", "34%20-%20NK4ZyuA.jpg", "42%20-%2009EwMmq.jpg", "50%20-%20OLwHF9J.jpg", "58%20-%20ApzUMsW.jpg", "66%20-%20LsUvp5g.jpg",
            "03%20-%20ZLFpbxm.jpg", "11%20-%20wCqGzTU.png", "19%20-%20g5Q1NCa.jpg", "27%20-%20i1Lv4CY.jpg", "35%20-%20BB1YAfb.jpg", "43%20-%20xXpKKpU.jpg", "51%20-%20NTN0jVS.jpg", "59%20-%20GXg3FUs.jpg", "67%20-%20vQE6pwk.jpg",
            "04%20-%20dyXbuVK.jpg", "12%20-%20Eh79054.jpg", "20%20-%201ZmrLjK.jpg", "28%20-%20hZZRnoR.jpg", "36%20-%20CpSFYwY.jpg", "44%20-%20bNsIXto.jpg", "52%20-%20yIFFaRt.jpg", "60%20-%20cwWI4AG.jpg", "68%20-%20oL3ETVL.jpg",
            "05%20-%20zEYl5Cp.jpg", "13%20-%20klGyEd6.png", "21%20-%20tLH51iW.jpg", "29%20-%202hvl6qP.jpg", "37%20-%20DC4L7zI.jpg", "45%20-%20aodIouB.jpg", "53%20-%20sfb9PbR.jpg", "61%20-%20O1R2quB.jpg", "69%20-%20JiACR7N.jpg",
            "06%20-%200byg3HK.jpg", "14%20-%20omNQMoZ.jpg", "22%20-%20QQLEwzF.jpg", "30%20-%209VopmwA.jpg", "38%20-%20nxuSi9d.jpg", "46%20-%203wWtNXJ.jpg", "54%20-%20d3ca61p.jpg", "62%20-%20fnjieyM.jpg", "70%20-%208GloK0A.jpg",
            "07%20-%20O8pQHDY.jpg", "15%20-%20RmPrDRL.jpg", "23%20-%20w9fNuEX.jpg", "31%20-%20CMTYkgj.jpg", "39%20-%20MEej3vQ.jpg", "47%20-%20530ygYW.jpg", "55%20-%20XU3xQNh.jpg", "63%20-%20f9mYEvO.jpg", "71%20-%20Zq7dJpw.jpg",
            "08%20-%20yZxeGfq.png", "16%20-%20Vg3Ruyl.jpg", "24%20-%20mUOUz2Y.jpg", "32%20-%20v9mnBmP.jpg", "40%20-%20OKI3GDJ.jpg", "48%20-%20TieinE6.jpg", "56%20-%20yMSLTPj.jpg", "64%20-%20FzrnX0c.jpg", "72%20-%20XcjkS7n.jpg"
    };
    private static final int ROTATE_TIME_MILLIS = 24 * 60 * 60 * 1000; // rotate every 3 hours

    public MuzeiMiyazakiService() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {

        Random random = new Random();
        int index = random.nextInt(FILES.length-1);
        String current = FILES[index];
        publishArtwork(new Artwork.Builder()
                .imageUri(Uri.parse(BASE_URL + current))
                .token(current)
                .build());

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }

}
