package net.ebt.muzei.miyazaki.util;

import android.util.Log;

/**
 * Created by eboudrant on 5/14/14.
 */
public class ExUtils {

  private static final String LOG_TAG = "ExUtils";

  public static void handle(Throwable e) {
    Log.w(LOG_TAG, "ex: " + e, e);
  }
}
