package net.ebt.muzei.miyazaki.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.apps.muzei.api.provider.ProviderContract;

import net.ebt.muzei.miyazaki.BuildConfig;
import net.ebt.muzei.miyazaki.R;
import net.ebt.muzei.miyazaki.load.UpdateMuzeiWorker;

import static net.ebt.muzei.miyazaki.BuildConfig.GHIBLI_AUTHORITY;
import static net.ebt.muzei.miyazaki.Constants.CURRENT_PREF_NAME;
import static net.ebt.muzei.miyazaki.Constants.MUZEI_COLOR;

public class MuzeiMiyazakiSettings extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

  private static final float ALPHA_DEACTIVATED = 0.3f;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings);
    updateMatches(getApplicationContext().getSharedPreferences(CURRENT_PREF_NAME, Context.MODE_PRIVATE));
    LoaderManager.getInstance(this).initLoader(0, null, this);
  }

  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(final int id, @Nullable final Bundle arguments) {
    return new CursorLoader(this,
            ProviderContract.Artwork.getContentUri(GHIBLI_AUTHORITY),
            null, null, null, null);
  }

  @Override
  public void onLoadFinished(@NonNull final Loader<Cursor> loader, final Cursor data) {
    long count = data.getCount();
    if (BuildConfig.DEBUG) {
      int percentArtworkWithCaption = 0;
      if (count > 0) {
        while (data.moveToNext()) {
          String caption = data.getString(data.getColumnIndex(ProviderContract.Artwork.BYLINE));
          if (caption != null && !caption.isEmpty()) {
            percentArtworkWithCaption++;
          }
        }
        percentArtworkWithCaption *= 100;
        percentArtworkWithCaption /= count;
      }
      ((TextView) findViewById(R.id.matches)).setText("Using " + count + " artworks (" + percentArtworkWithCaption + "%)");
    } else {
      ((TextView) findViewById(R.id.matches)).setText("Using " + count + " artworks");
    }
  }

  @Override
  public void onLoaderReset(@NonNull final Loader<Cursor> loader) {
  }

  public void onColor(View view) {

    if (view instanceof FrameLayout) {
      view = FrameLayout.class.cast(view).getChildAt(0);
    }

    final SharedPreferences settings = getApplicationContext().getSharedPreferences(CURRENT_PREF_NAME, Context.MODE_PRIVATE);
    String color = settings.getString(MUZEI_COLOR, "");

    findViewById(R.id.black).setAlpha(ALPHA_DEACTIVATED);
    findViewById(R.id.maroon).setAlpha(ALPHA_DEACTIVATED);
    findViewById(R.id.navy).setAlpha(ALPHA_DEACTIVATED);
    findViewById(R.id.teal).setAlpha(ALPHA_DEACTIVATED);
    findViewById(R.id.green).setAlpha(ALPHA_DEACTIVATED);

    if (view.getId() == R.id.black) {
      if ("black".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
      } else settings.edit().putString(MUZEI_COLOR, "black").apply();
    } else if (view.getId() == R.id.grey) {
      if ("grey".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
      } else settings.edit().putString(MUZEI_COLOR, "grey").apply();
    } else if (view.getId() == R.id.silver) {
      if ("silver".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
      } else settings.edit().putString(MUZEI_COLOR, "silver").apply();
    } else if (view.getId() == R.id.maroon) {
      if ("maroon".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
      } else settings.edit().putString(MUZEI_COLOR, "maroon").apply();
    } else if (view.getId() == R.id.olive) {
      if ("olive".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
      } else settings.edit().putString(MUZEI_COLOR, "olive").apply();
    } else if (view.getId() == R.id.green) {
      if ("green".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
      } else settings.edit().putString(MUZEI_COLOR, "green").apply();
    } else if (view.getId() == R.id.teal) {
      if ("teal".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
      } else settings.edit().putString(MUZEI_COLOR, "teal").apply();
    } else if (view.getId() == R.id.navy) {
      if ("navy".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
      } else settings.edit().putString(MUZEI_COLOR, "navy").apply();
    } else if (view.getId() == R.id.purple) {
      if ("purple".equals(color)) {
        settings.edit().remove(MUZEI_COLOR).apply();
      } else settings.edit().putString(MUZEI_COLOR, "purple").apply();
    }

    updateMatches(settings);
    UpdateMuzeiWorker.Companion.enqueueUpdate();
  }

  private void updateMatches(SharedPreferences settings) {
    String color = settings.getString(MUZEI_COLOR, null);
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
