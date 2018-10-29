package net.ebt.muzei.miyazaki.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import net.ebt.muzei.miyazaki.BuildConfig;
import net.ebt.muzei.miyazaki.model.Artwork;
import net.ebt.muzei.miyazaki.model.Artworks;
import net.ebt.muzei.miyazaki.util.ExUtils;
import net.ebt.muzei.miyazaki.util.ThUtils;
import net.ebt.muzei.miyazaki.util.VrUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.ebt.muzei.miyazaki.Constants.CURRENT_PREF_NAME;

public class MuzeiMiyazakiApplication extends Application {

  private static final String TAG = "MuzeiMiyazaki";
  private static final float RATIO = 1.0f;
  private static final long EXPIRATION = 1000 * 60 * 60 * 24 * 15;
  private static MuzeiMiyazakiApplication s_instance;
  private Map<String, Artwork> m_artworks = new HashMap<String, Artwork>();
  private Map<String, Float> m_avg = new HashMap<String, Float>();
  private Map<String, Float> m_min = new HashMap<String, Float>();
  private Map<String, Float> m_max = new HashMap<String, Float>();
  private Map<String, Integer> m_count = new HashMap<String, Integer>();
  private ArrayList<Artwork> m_artworksAsList = new ArrayList<Artwork>();
  private OkHttpClient mClient;
  private int m_percentWithCaption;
  private String uuid;

  public MuzeiMiyazakiApplication() {
    s_instance = this;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    final SharedPreferences sharedPreferences = getSharedPreferences("App", MODE_PRIVATE);
    if (!sharedPreferences.contains("uuid")) {
        sharedPreferences.edit()
                .putString("uuid", UUID.randomUUID().toString())
                .apply();
    }
    uuid = sharedPreferences.getString("uuid", "");

    MuzeiMiyazakiApplication.getInstance().loadDefinitivePayload(null);
    File cached = new File(getCacheDir() + "/.tmp");
    if (cached.exists()) {
      if (BuildConfig.DEBUG) Log.i(TAG, "Loading cached file");
      try {
        Reader reader = new InputStreamReader(new FileInputStream(cached));
        processPayload(reader);
      } catch (Throwable e) {
        ExUtils.handle(e);
        cached.delete();
        try {
          Reader reader = new InputStreamReader(getResources().getAssets().open("data.json"));
          processPayload(reader);
        } catch (IOException e1) {
          ExUtils.handle(e1);
        }
      }
    } else {
      try {
        if (BuildConfig.DEBUG) Log.i(TAG, "Loading assets file");
        Reader reader = new InputStreamReader(getResources().getAssets().open("data.json"));
        processPayload(reader);
      } catch (IOException e) {
        ExUtils.handle(e);
      }
    }
  }

  private void processPayload(Reader reader) {
    Gson gson = new Gson();
    int withCaption = 0;
    Artworks artworks = gson.fromJson(reader, Artworks.class);
    HashMap<String, Artwork> m = new HashMap<String, Artwork>(artworks.artworks.size());
    ArrayList<Artwork> l = new ArrayList<Artwork>(artworks.artworks.size());
    for (Artwork a : artworks.artworks) {
      if (a.caption != null && a.caption.length() > 0) withCaption++;
      l.add(a);
      a.done();
      m.put(a.hash, a);
      for (String c : a.colors.keySet()) {
        float v = a.colors.get(c);
        if (v != 0 && (m_max.get(c) == null || v > m_max.get(c))) {
          m_max.put(c, v);
        }
        if (v != 0 && (m_min.get(c) == null || v < m_min.get(c))) {
          m_min.put(c, v);
        }

        if (v != 0) {
          if (m_avg.get(c) == null) m_avg.put(c, v);
          else {
            m_avg.put(c, m_avg.get(c) + v);
          }
          if (m_count.get(c) == null) m_count.put(c, 1);
          else m_count.put(c, m_count.get(c) + 1);
        }
      }
    }
    for (String c : m_avg.keySet()) {
      m_avg.put(c, (m_avg.get(c) / m_count.get(c)) * RATIO);
      if (BuildConfig.DEBUG)
        Log.i(TAG, "avg:" + format(m_avg.get(c)) + " with " + m_count.get(c) + " for " + c + " -> min:" + format(m_min.get(c)) + " max:" + format(m_max.get(c)));
    }
    if (BuildConfig.DEBUG) Log.i(TAG, "loaded " + m.size() + " artworks");
    m_artworks = m;
    m_artworksAsList = l;
    m_percentWithCaption = (int) ((100.0f / (float) m_artworksAsList.size() * (float) withCaption));
  }

  public int getPercentWithCaption() {
    return m_percentWithCaption;
  }

  private String format(Float aFloat) {
    return aFloat == null ? "-" : new BigDecimal(aFloat).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
  }

  @Override
  public void onTerminate() {
    super.onTerminate();
  }

  public float get(String color) {
    return m_avg.get(color);
  }

  public static MuzeiMiyazakiApplication getInstance() {
    return s_instance;
  }

  @NonNull
  public String getUuid() {
    return uuid;
  }

  public List<Artwork> getArtworks() {
    return m_artworksAsList;
  }

  public void loadDefinitivePayload(final Runnable callback) {
    long lastLoaded = getSharedPreferences("App", MODE_PRIVATE).getLong("loadLoaded", 0);
    if (System.currentTimeMillis() - lastLoaded > EXPIRATION || BuildConfig.DEBUG) {
      if (mClient == null) {
        mClient = new OkHttpClient();
      }
      (new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... voids) {
          try {
            if (BuildConfig.DEBUG) Log.i(TAG, "Loading remote data");
            String url = "https://muzei-ghibli.appspot.com/list?sort=no&a=" + Settings.Secure.getString(MuzeiMiyazakiApplication.this.getContentResolver(), Settings.Secure.ANDROID_ID).hashCode();
            if (VrUtils.isNetworkConnectet(MuzeiMiyazakiApplication.this)) {
              long start = System.currentTimeMillis();
              Request request = new Request.Builder()
                  .url(url)
                  .build();

              Response resp = MuzeiMiyazakiApplication.getInstance().getHttpClient().newCall(request).execute();

              File tmp = new File(getCacheDir() + "/.tmpfile");
              VrUtils.copyFile(resp.body().byteStream(), tmp);
              Reader reader = new InputStreamReader(new FileInputStream(tmp));
              processPayload(reader);

              File cached = new File(getCacheDir() + "/.tmp");
              if (cached.exists()) {
                cached.delete();
              }
              tmp.renameTo(cached);
              getSharedPreferences("App", MODE_PRIVATE).edit().putLong("loadLoaded", System.currentTimeMillis()).apply();
              if (callback != null) {
                ThUtils.getMainHandler().post(callback);
              }
            }
          } catch (IOException e) {
            ExUtils.handle(e);
          } catch (Exception e) {
            ExUtils.handle(e);
          }
          return null;
        }
      }).execute();
    }
  }

  public OkHttpClient getHttpClient() {
    return mClient;
  }

  public void removePro() {
    if (!BuildConfig.DEBUG) {
      getSharedPreferences("App", MODE_PRIVATE).edit().remove("premium").apply();
      File cached = new File(getCacheDir() + "/.tmp");
      if (cached.exists()) {
        cached.delete();
      }
      SharedPreferences prefs = getApplicationContext().getSharedPreferences(CURRENT_PREF_NAME, Context.MODE_PRIVATE);
      prefs.edit().remove(CURRENT_PREF_NAME).apply();
    }
  }

}
