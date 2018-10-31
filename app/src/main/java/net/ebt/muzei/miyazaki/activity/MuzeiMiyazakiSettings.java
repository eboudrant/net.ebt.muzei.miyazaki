package net.ebt.muzei.miyazaki.activity;

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

public class MuzeiMiyazakiSettings extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

  private static final float ALPHA_DEACTIVATED = 0.3f;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings);
    updateMatches();
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

    if (view.getId() == R.id.black) {
      UpdateMuzeiWorker.Companion.toggleColor(this, "black");
    } else if (view.getId() == R.id.grey) {
      UpdateMuzeiWorker.Companion.toggleColor(this, "grey");
    } else if (view.getId() == R.id.silver) {
      UpdateMuzeiWorker.Companion.toggleColor(this, "silver");
    } else if (view.getId() == R.id.maroon) {
      UpdateMuzeiWorker.Companion.toggleColor(this, "maroon");
    } else if (view.getId() == R.id.olive) {
      UpdateMuzeiWorker.Companion.toggleColor(this, "olive");
    } else if (view.getId() == R.id.green) {
      UpdateMuzeiWorker.Companion.toggleColor(this, "green");
    } else if (view.getId() == R.id.teal) {
      UpdateMuzeiWorker.Companion.toggleColor(this, "teal");
    } else if (view.getId() == R.id.navy) {
      UpdateMuzeiWorker.Companion.toggleColor(this, "navy");
    } else if (view.getId() == R.id.purple) {
      UpdateMuzeiWorker.Companion.toggleColor(this, "purple");
    }

    updateMatches();
  }

  private void updateMatches() {
    String color = UpdateMuzeiWorker.Companion.getCurrentColor(this);
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
