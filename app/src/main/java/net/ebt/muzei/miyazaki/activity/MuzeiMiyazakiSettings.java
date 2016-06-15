package net.ebt.muzei.miyazaki.activity;


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import net.ebt.muzei.miyazaki.BuildConfig;
import net.ebt.muzei.miyazaki.R;
import net.ebt.muzei.miyazaki.app.MuzeiMiyazakiApplication;
import net.ebt.muzei.miyazaki.model.Artwork;
import net.ebt.muzei.miyazaki.service.MuzeiMiyazakiService;
import net.ebt.muzei.miyazaki.util.UiUtils;
import net.ebt.muzei.miyazaki.util.Utils;

import java.util.List;
import java.util.logging.Level;

import static net.ebt.muzei.miyazaki.Constants.ACTION_RELOAD;
import static net.ebt.muzei.miyazaki.Constants.CURRENT_PREF_NAME;
import static net.ebt.muzei.miyazaki.Constants.DEFAULT_INTERVAL;
import static net.ebt.muzei.miyazaki.Constants.INTERVALS;
import static net.ebt.muzei.miyazaki.Constants.MUZEI_COLOR;
import static net.ebt.muzei.miyazaki.Constants.MUZEI_FRAME;
import static net.ebt.muzei.miyazaki.Constants.MUZEI_INTERVAL;
import static net.ebt.muzei.miyazaki.Constants.MUZEI_WIFI;

public class MuzeiMiyazakiSettings extends FragmentActivity {

  private static final String TAG = "MuzeiMiyazakiSettings";
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

    int interval = settings.getInt(MUZEI_INTERVAL, DEFAULT_INTERVAL);

    final View colors = findViewById(R.id.colors);
    final SeekBar seekBar = (SeekBar) findViewById(R.id.muzei_interval);
    final CheckBox wifi = (CheckBox) findViewById(R.id.muzei_wifi);
    final TextView configLabel = (TextView) findViewById(R.id.muzei_config_label);
    final TextView label = (TextView) findViewById(R.id.muzei_label);
    configLabel.setText("Refresh every " + Utils.formatDuration(INTERVALS.get(interval)));

    seekBar.setMax(INTERVALS.size() - 1);
    seekBar.setProgress(interval);
    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        label.setText(Utils.formatDuration(INTERVALS.get(progress)).toUpperCase());
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
        configLabel.setVisibility(View.INVISIBLE);
        colors.setVisibility(View.GONE);
        label.setVisibility(View.VISIBLE);
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        label.setText(null);
        label.setVisibility(View.GONE);
        configLabel.setText("Refresh every " + Utils.formatDuration(INTERVALS.get(seekBar.getProgress())));
        configLabel.setVisibility(View.VISIBLE);
        colors.setVisibility(View.VISIBLE);

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(MUZEI_INTERVAL, seekBar.getProgress());
        editor.commit();

        Intent intent = new Intent(MuzeiMiyazakiService.ACTION_RESCHEDULE);
        intent.setClass(seekBar.getContext(), MuzeiMiyazakiService.class);
        startService(intent);
      }
    });

    wifi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(MUZEI_WIFI, isChecked);
        editor.commit();
      }
    });

    wifi.setChecked(settings.getBoolean(MUZEI_WIFI, true));
    label.setVisibility(View.GONE);

    findViewById(R.id.seeall).setVisibility(View.GONE);
    findViewById(R.id.seeall).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        try {
          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.setData(Uri.parse("http://muzeighibli.net?jsessionid=" + String.valueOf(System.currentTimeMillis()).hashCode() + "&a=" + Settings.Secure.getString(MuzeiMiyazakiSettings.this.getContentResolver(), Settings.Secure.ANDROID_ID).hashCode()));
          startActivity(intent);
          UiUtils.makeToast(MuzeiMiyazakiSettings.this, R.string.help_captions, Level.INFO);
        } catch (ActivityNotFoundException e) {
          UiUtils.makeToast(MuzeiMiyazakiSettings.this, R.string.install_chrome, Level.INFO);
        }
      }
    });
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
        settings.edit().remove(MUZEI_COLOR).commit();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "black").commit();
    } else if (view.getId() == R.id.grey) {
      if ("grey".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).commit();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "grey").commit();
    } else if (view.getId() == R.id.silver) {
      if ("silver".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).commit();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "silver").commit();
    } else if (view.getId() == R.id.maroon) {
      if ("maroon".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).commit();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "maroon").commit();
    } else if (view.getId() == R.id.olive) {
      if ("olive".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).commit();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "olive").commit();
    } else if (view.getId() == R.id.green) {
      if ("green".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).commit();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "green").commit();
    } else if (view.getId() == R.id.teal) {
      if ("teal".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).commit();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "teal").commit();
    } else if (view.getId() == R.id.navy) {
      if ("navy".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).commit();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "navy").commit();
    } else if (view.getId() == R.id.purple) {
      if ("purple".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).commit();
        remove = true;
      } else settings.edit().putString(MUZEI_COLOR, "purple").commit();
    }

    if (!remove) {
      Intent intent = new Intent(ACTION_RELOAD);
      intent.setClass(this, MuzeiMiyazakiService.class);
      startService(intent);
    }

    updateMatches(settings);

    if (!remove) {
      view.setAlpha(1.0f);
    }
  }

  private void updateMatches(SharedPreferences settings) {

    String frame = settings.getString(MUZEI_FRAME, null);
    String color = settings.getString(MUZEI_COLOR, null);
    int matches = 0;
    boolean ok;
    List<Artwork> artworks = MuzeiMiyazakiApplication.getInstance().getArtworks();
    if (artworks != null) {
      for (Artwork artwork : artworks) {
        ok = false;
        if (color == null || artwork.colors.get(color) > MuzeiMiyazakiApplication.getInstance().get(color))
          ok = true;
        if (ok && frame != null) {
          if ("portrait".equals(frame)) {
            ok = artwork.ratio < 1.0f;
          } else if ("ultra_wide".equals(frame)) {
            ok = artwork.ratio > 3.0f;
          } else if ("wide".equals(frame)) {
            ok = artwork.ratio >= 1.0f && artwork.ratio <= 3.0f;
          }
        }
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

  public void onFrameLayout(View view) {
    final SharedPreferences settings = getApplicationContext().getSharedPreferences(CURRENT_PREF_NAME, Context.MODE_PRIVATE);
    String frame = settings.getString(MUZEI_FRAME, null);
    if (view.getId() == R.id.frame_ultra_wide) {
      if ("ultra_wide".equals(frame)) settings.edit().remove(MUZEI_FRAME).commit();
      else settings.edit().putString(MUZEI_FRAME, "ultra_wide").commit();
    } else if (view.getId() == R.id.frame_portrait) {
      if ("portrait".equals(frame)) settings.edit().remove(MUZEI_FRAME).commit();
      else settings.edit().putString(MUZEI_FRAME, "portrait").commit();
    } else if (view.getId() == R.id.frame_wide) {
      if ("wide".equals(frame)) settings.edit().remove(MUZEI_FRAME).commit();
      else settings.edit().putString(MUZEI_FRAME, "wide").commit();
    }
    Intent intent = new Intent(ACTION_RELOAD);
    intent.setClass(this, MuzeiMiyazakiService.class);
    startService(intent);

    updateMatches(settings);
  }
}
