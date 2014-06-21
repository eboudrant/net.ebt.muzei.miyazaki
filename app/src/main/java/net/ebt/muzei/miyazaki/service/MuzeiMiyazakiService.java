package net.ebt.muzei.miyazaki.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.util.ArrayList;
import java.util.Collections;

import static net.ebt.muzei.miyazaki.Constants.*;

public class MuzeiMiyazakiService extends RemoteMuzeiArtSource {

    public static final String ACTION_RESCHEDULE = "kr.infli.muzei.InflikrMuzeiArtSource.ACTION_RESCHEDULE";

    public MuzeiMiyazakiService() {
        super(SOURCE_NAME);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && ACTION_RESCHEDULE.equals(intent.getAction())) {
                scheduleNextUpdate();
                return;
        } else super.onHandleIntent(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {

        if(reason == UPDATE_REASON_SCHEDULED && abortIfNecessary()) {
            throw new RetryException();
        }

        String current = FILES[getNextArtworkIndex()];
        publishArtwork(new Artwork.Builder()
                .imageUri(Uri.parse(BASE_URL + current))
                .token(current)
                .build());

        if(reason != UPDATE_REASON_USER_NEXT) {
            scheduleNextUpdate();
        }

    }

    /**
     * Verify the connectivity/setting
     * @return should abort
     */
    private boolean abortIfNecessary() {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(CURRENT_PREF_NAME, Context.MODE_PRIVATE);
        boolean wifi = settings.getBoolean(MUZEI_WIFI, false);
        if(wifi) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni.getType() != ConnectivityManager.TYPE_WIFI;
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

    private String string(int reason) {
        switch (reason) {
            case UPDATE_REASON_INITIAL:
                return "INITIAL";
            case UPDATE_REASON_SCHEDULED:
                return "SCHEDULED";
            case UPDATE_REASON_USER_NEXT:
                return "USER_NEXT";
            case UPDATE_REASON_OTHER:
                return "OTHER";
            default:
                return "?";
        }
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
            ArrayList<Integer> shuffled = new ArrayList<Integer>(FILES.length);
            for (int i = 0; i < FILES.length; i++)
                shuffled.add(i);
            Collections.shuffle(shuffled);

            // Build show the first and build the next sequence
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < FILES.length; i++) {
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
            prefs.edit().putString(CURRENT_PREF_NAME, sequence).commit();
        } else {
            prefs.edit().remove(CURRENT_PREF_NAME).commit();
        }

        return indexToShow;
    }

}
