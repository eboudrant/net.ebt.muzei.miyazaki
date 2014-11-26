package net.ebt.muzei.miyazaki.util;

import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

import java.util.logging.Level;

/**
 * Created by eboudrant on 5/13/14.
 */
public class UiUtils {

  public static void makeToast(final Context context, final int resid, final Level level, final Object... args) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
      Toast.makeText(context, context.getResources().getString(resid, args), Toast.LENGTH_LONG).show();
    } else {
      ThUtils.getMainHandler().post(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(context, context.getResources().getString(resid, args), Toast.LENGTH_LONG).show();
        }
      });
    }
  }
}
