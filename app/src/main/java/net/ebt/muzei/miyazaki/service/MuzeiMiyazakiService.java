package net.ebt.muzei.miyazaki.service;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.google.android.apps.muzei.api.UserCommand;

import net.ebt.muzei.miyazaki.app.MuzeiMiyazakiApplication;
import net.ebt.muzei.miyazaki.util.ExUtils;

import java.util.ArrayList;
import java.util.Collections;

import static net.ebt.muzei.miyazaki.Constants.ACTION_RELOAD;
import static net.ebt.muzei.miyazaki.Constants.CURRENT_PREF_NAME;
import static net.ebt.muzei.miyazaki.Constants.DEFAULT_INTERVAL;
import static net.ebt.muzei.miyazaki.Constants.INTERVALS;
import static net.ebt.muzei.miyazaki.Constants.MUZEI_COLOR;
import static net.ebt.muzei.miyazaki.Constants.MUZEI_FRAME;
import static net.ebt.muzei.miyazaki.Constants.MUZEI_INTERVAL;
import static net.ebt.muzei.miyazaki.Constants.MUZEI_WIFI;
import static net.ebt.muzei.miyazaki.Constants.SOURCE_NAME;

public class MuzeiMiyazakiService extends RemoteMuzeiArtSource {

  public static final String ACTION_RESCHEDULE = "kr.infli.muzei.InflikrMuzeiArtSource.ACTION_RESCHEDULE";
  private static final String TAG = "MuzeiMiyazakiService";
  public static final int COMMAND_ID_INFO = 10;

  public MuzeiMiyazakiService() {
    super(SOURCE_NAME);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (intent != null && ACTION_RESCHEDULE.equals(intent.getAction())) {
      scheduleNextUpdate();
    } else if (intent != null && ACTION_RELOAD.equals(intent.getAction())) {
      try {
        onTryUpdate(UPDATE_REASON_USER_NEXT);
      } catch (RetryException e) {
        Log.w(TAG, "Failed to reload", e);
      }
    } else super.onHandleIntent(intent);
  }

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  protected void onCustomCommand(int id) {
    if (id == COMMAND_ID_INFO) {
      try {
        if (getCurrentArtwork().getByline() != null && getCurrentArtwork().getByline().contains("-")) {
          String search = getCurrentArtwork().getByline().substring(0, getCurrentArtwork().getByline().indexOf("-") - 1).trim();
          Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
          intent.putExtra(SearchManager.QUERY, search);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(intent);
        }
      } catch (Exception e) {
        ExUtils.handle(e);
      }
    } else {
      super.onCustomCommand(id);
    }
  }

  @Override
  protected void onTryUpdate(int reason) throws RetryException {

    final SharedPreferences settings = getApplicationContext().getSharedPreferences(CURRENT_PREF_NAME, Context.MODE_PRIVATE);

    settings.edit().putBoolean("onboarding", true).apply();

    if (reason == UPDATE_REASON_SCHEDULED && abortIfNecessary()) {
      throw new RetryException();
    }

    String frame = settings.getString(MUZEI_FRAME, null);
    String color = settings.getString(MUZEI_COLOR, null);
    net.ebt.muzei.miyazaki.model.Artwork artwork;
    boolean ok;
    do {
      artwork = MuzeiMiyazakiApplication.getInstance().getArtworks().get(getNextArtworkIndex());
      ok = color == null || artwork.colors.get(color) > MuzeiMiyazakiApplication.class.cast(getApplication()).get(color);
      if (ok && frame != null) {
        if ("portrait".equals(frame)) {
          ok = artwork.ratio < 1.0f;
        } else if ("ultra_wide".equals(frame)) {
          ok = artwork.ratio > 3.0f;
        } else if ("wide".equals(frame)) {
          ok = artwork.ratio >= 1.0f && artwork.ratio <= 3.0f;
        }
      }
    } while (!ok);

    Log.i(TAG, "Publish " + artwork.url + " : " + artwork.caption);

    Artwork.Builder builder = new Artwork.Builder()
        .imageUri(Uri.parse(artwork.url))
        .token(artwork.hash)
        .title((artwork.caption == null ? "" : artwork.caption))
        .byline((artwork.subtitle == null ? "" : artwork.subtitle));

    if (artwork.subtitle != null && artwork.subtitle.contains("-")) {
      try {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, artwork.subtitle.substring(0, artwork.subtitle.indexOf("-") - 1).trim()); // query contains search string
        builder.viewIntent(intent);
        setUserCommands(new UserCommand(BUILTIN_COMMAND_ID_NEXT_ARTWORK), new UserCommand(COMMAND_ID_INFO, "More info"));
      } catch (Exception e) {
        ExUtils.handle(e);
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
      }
    } else {
      setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    publishArtwork(builder.build());

    if (reason != UPDATE_REASON_USER_NEXT) {
      scheduleNextUpdate();
    }
  }

  /**
   * Verify the connectivity/setting
   *
   * @return should abort
   */
  private boolean abortIfNecessary() {
    SharedPreferences settings = getApplicationContext().getSharedPreferences(CURRENT_PREF_NAME, Context.MODE_PRIVATE);
    boolean wifi = settings.getBoolean(MUZEI_WIFI, true);
    if (wifi) {
      ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo ni = cm.getActiveNetworkInfo();
      return ni == null || ni.getType() != ConnectivityManager.TYPE_WIFI;
    } else return false;
  }

  /**
   * Schedule next updated according the settings
   */
  private void scheduleNextUpdate() {
    SharedPreferences settings = getApplicationContext().getSharedPreferences(CURRENT_PREF_NAME, Context.MODE_PRIVATE);
    int interval = settings.getInt(MUZEI_INTERVAL, DEFAULT_INTERVAL);
    long nextInterval = INTERVALS.get(interval);
    scheduleUpdate(System.currentTimeMillis() + nextInterval);
  }

  /**
   * Current sequence is stored in a preferences
   * If there is no value then regenerate a sequence (shuffle)
   *
   * @return indexToShow
   */
  private int getNextArtworkIndex() {
    int indexToShow = 0;

    SharedPreferences prefs = getApplicationContext().getSharedPreferences(CURRENT_PREF_NAME, Context.MODE_PRIVATE);
    String sequence = prefs.getString(CURRENT_PREF_NAME, null);

    if (sequence == null) {

      // Reshuffle the array
      ArrayList<Integer> shuffled = new ArrayList<Integer>(MuzeiMiyazakiApplication.getInstance().getArtworks().size());
      for (int i = 0; i < MuzeiMiyazakiApplication.getInstance().getArtworks().size(); i++)
        shuffled.add(i);
      Collections.shuffle(shuffled);

      // Build show the first and build the next sequence
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < MuzeiMiyazakiApplication.getInstance().getArtworks().size(); i++) {
        if (i == 0) {
          indexToShow = shuffled.get(i);
        } else {
          builder.append(shuffled.get(i));
          builder.append(" ");
        }
      }
      sequence = builder.toString().trim();

    } else {

      if (sequence.contains(" ")) {
        // Remove and show the first from the sequence
        indexToShow = Integer.parseInt(sequence.substring(0, sequence.indexOf(" ")));
        sequence = sequence.substring(sequence.indexOf(" ") + 1);
      } else {
        // Show the latest and reset the sequence
        indexToShow = Integer.parseInt(sequence);
        sequence = null;
      }

    }

    if (sequence != null) {
      prefs.edit().putString(CURRENT_PREF_NAME, sequence).apply();
    } else {
      prefs.edit().remove(CURRENT_PREF_NAME).apply();
    }

    Log.i(TAG, "Show index " + indexToShow + " in [0-" + MuzeiMiyazakiApplication.getInstance().getArtworks().size() + "]");

    return indexToShow;
  }

}
