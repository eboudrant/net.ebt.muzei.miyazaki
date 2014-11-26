package net.ebt.muzei.miyazaki.util;

import org.apache.commons.lang3.time.DurationFormatUtils;

public class Utils {

  public static String formatDuration(long millis) {
    return (" " + DurationFormatUtils.formatDurationWords(millis, true, true)).replace(" 1 ", " ").trim();
  }

}
