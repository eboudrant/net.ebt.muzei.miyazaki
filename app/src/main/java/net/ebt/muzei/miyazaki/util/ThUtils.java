package net.ebt.muzei.miyazaki.util;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by eboudrant on 5/14/14.
 */
public class ThUtils {

  public static Handler getMainHandler() {
    return Holder.sMainHandler;
  }

  private static class Holder {
    private final static Handler sMainHandler = new Handler(Looper.getMainLooper());
  }


}
