package net.ebt.muzei.miyazaki.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.ebt.muzei.miyazaki.BuildConfig;
import net.ebt.muzei.miyazaki.R;
import net.ebt.muzei.miyazaki.app.MuzeiMiyazakiApplication;
import net.ebt.muzei.miyazaki.load.UpdateMuzeiWorker;
import net.ebt.muzei.miyazaki.model.Artwork;

import java.util.List;

import static net.ebt.muzei.miyazaki.Constants.CURRENT_PREF_NAME;
import static net.ebt.muzei.miyazaki.Constants.MUZEI_COLOR;

public class MuzeiMiyazakiSettings extends FragmentActivity {

  private static final float ALPHA_DEACTIVATED = 0.3f;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    final String muzeiPackageId = Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN ? "net.nurik.roman.muzei" : "net.nurik.roman.muzei.muik";
    final SharedPreferences settings = getApplicationContext().getSharedPreferences(CURRENT_PREF_NAME, Context.MODE_PRIVATE);

    if (getIntent() != null && getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_MAIN)) {
      try {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(muzeiPackageId);
        if (launchIntent != null) {

          if (!settings.getBoolean("onboarding", false)) {
            Toast.makeText(this, getResources().getString(R.string.setup_muzei), Toast.LENGTH_LONG).show();
          }
          launchIntent.addCategory(Intent.CATEGORY_DEFAULT);
          launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
          startActivity(launchIntent);
          finish();
          return;

        } else {
          Toast.makeText(this, getResources().getString(R.string.install_muzei), Toast.LENGTH_LONG).show();
          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.setData(Uri.parse("market://details?id=" + muzeiPackageId));
          startActivity(intent);
          finish();
          return;
        }
      } catch (Throwable e) {
        // No playstore
      }
    }

    setContentView(R.layout.settings);
  }

  @Override
  protected void onResume() {
    super.onResume();
    updateMatches(getApplicationContext().getSharedPreferences(CURRENT_PREF_NAME, Context.MODE_PRIVATE));
  }

  public void onColor(View view) {

    if (view instanceof FrameLayout) {
      view = FrameLayout.class.cast(view).getChildAt(0);
    }

    final SharedPreferences settings = getApplicationContext().getSharedPreferences(CURRENT_PREF_NAME, Context.MODE_PRIVATE);
    boolean remove = false;
    String color = settings.getString(MUZEI_COLOR, "");

    findViewById(R.id.black).setAlpha(ALPHA_DEACTIVATED);
    findViewById(R.id.maroon).setAlpha(ALPHA_DEACTIVATED);
    findViewById(R.id.navy).setAlpha(ALPHA_DEACTIVATED);
    findViewById(R.id.teal).setAlpha(ALPHA_DEACTIVATED);
    findViewById(R.id.green).setAlpha(ALPHA_DEACTIVATED);

    if (view.getId() == R.id.black) {
      if ("black".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "black").apply();
    } else if (view.getId() == R.id.grey) {
      if ("grey".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "grey").apply();
    } else if (view.getId() == R.id.silver) {
      if ("silver".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "silver").apply();
    } else if (view.getId() == R.id.maroon) {
      if ("maroon".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "maroon").apply();
    } else if (view.getId() == R.id.olive) {
      if ("olive".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "olive").apply();
    } else if (view.getId() == R.id.green) {
      if ("green".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "green").apply();
    } else if (view.getId() == R.id.teal) {
      if ("teal".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "teal").apply();
    } else if (view.getId() == R.id.navy) {
      if ("navy".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "navy").apply();
    } else if (view.getId() == R.id.purple) {
      if ("purple".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "purple").apply();
    }

    updateMatches(settings);
    UpdateMuzeiWorker.Companion.enqueueUpdate();

    if (!remove) {
      view.setAlpha(1.0f);
    }
  }

  private void updateMatches(SharedPreferences settings) {

    String frame = null;
    String color = settings.getString(MUZEI_COLOR, null);
    int matches = 0;
    boolean ok;
    List<Artwork> artworks = MuzeiMiyazakiApplication.getInstance().getArtworks();
    if (artworks != null) {
      for (Artwork artwork : artworks) {
        ok = false;
        if (color == null || artwork.colors.get(color) > MuzeiMiyazakiApplication.getInstance().get(color))
          ok = true;
          if (ok) matches++;
      }

      if (BuildConfig.DEBUG) {
        ((TextView) findViewById(R.id.matches)).setText("Using " + matches + " artworks (" + MuzeiMiyazakiApplication.getInstance().getPercentWithCaption() + "%)");
      } else {
        ((TextView) findViewById(R.id.matches)).setText("Using " + matches + " artworks");
      }

      findViewById(R.id.black).setAlpha(ALPHA_DEACTIVATED);
      findViewById(R.id.maroon).setAlpha(ALPHA_DEACTIVATED);
      findViewById(R.id.navy).setAlpha(ALPHA_DEACTIVATED);
      findViewById(R.id.teal).setAlpha(ALPHA_DEACTIVATED);
      findViewById(R.id.green).setAlpha(ALPHA_DEACTIVATED);
      if ("black".equals(color)) findViewById(R.id.black).setAlpha(1.0f);
      if ("maroon".equals(color)) findViewById(R.id.maroon).setAlpha(1.0f);
      if ("navy".equals(color)) findViewById(R.id.navy).setAlpha(1.0f);
      if ("teal".equals(color)) findViewById(R.id.teal).setAlpha(1.0f);
      if ("green".equals(color)) findViewById(R.id.green).setAlpha(1.0f);
    }
  }
}
