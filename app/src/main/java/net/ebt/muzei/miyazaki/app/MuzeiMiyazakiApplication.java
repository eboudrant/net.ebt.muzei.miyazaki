package net.ebt.muzei.miyazaki.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.UUID;

public class MuzeiMiyazakiApplication extends Application {

  private static MuzeiMiyazakiApplication s_instance;
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
  }

  public static MuzeiMiyazakiApplication getInstance() {
    return s_instance;
  }

  @NonNull
  public String getUuid() {
    return uuid;
  }

}
