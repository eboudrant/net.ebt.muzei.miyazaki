package net.ebt.muzei.miyazaki.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by eboudrant on 5/15/14.
 */
public class VrUtils {

  public static boolean isNetworkConnectet(Context context) {
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo ni = cm.getActiveNetworkInfo();
    return ni != null && ni.isConnected();
  }

  public static void copyFile(InputStream in, File dest) throws IOException {
    if (!dest.exists()) {
      dest.createNewFile();
    }
    OutputStream out = null;
    try {
      out = new FileOutputStream(dest);

      // Transfer bytes from in to out
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
    } finally {
      if (in != null) {
        in.close();
      }
      if (out != null) {
        out.close();
      }
    }
  }

}
