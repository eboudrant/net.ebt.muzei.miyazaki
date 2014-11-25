package net.ebt.muzei.miyazaki;

import android.util.SparseArray;

/**
 * Created by eboudrant on 6/21/14.
 */
public class Constants {

  public static final String SOURCE_NAME = "MuzeiMiyazakiArtSource";
  public static final String CURRENT_PREF_NAME = "MuzeiGhibli.current";
  public static final SparseArray<Long> INTERVALS = new SparseArray<Long>();
  public static final String MUZEI_INTERVAL = "muzei_interval";
  public static final String MUZEI_WIFI = "muzei_wifi";
  public static final String MUZEI_COLOR = "muzei_color";
  public static final String MUZEI_FRAME = "muzei_frame";
  public static final String ACTION_RELOAD = "muzei_reload";


  public static final int DEFAULT_INTERVAL;
  private final static long MIN = 1000 * 60;
  private final static long HOUR = MIN * 60;
  private final static long DAY = HOUR * 24;

  static {
    int i = 0;
    if (BuildConfig.DEBUG) {
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
}
