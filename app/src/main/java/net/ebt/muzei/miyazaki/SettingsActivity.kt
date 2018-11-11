package net.ebt.muzei.miyazaki

import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.google.android.apps.muzei.api.provider.ProviderContract
import net.ebt.muzei.miyazaki.BuildConfig.GHIBLI_AUTHORITY
import net.ebt.muzei.miyazaki.load.UpdateMuzeiWorker

class SettingsActivity : ComponentActivity(), LoaderManager.LoaderCallbacks<Cursor> {

    companion object {
        private const val ALPHA_DEACTIVATED = 0.3f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        updateMatches()
        LoaderManager.getInstance(this).initLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, arguments: Bundle?): Loader<Cursor> {
        return CursorLoader(this,
                ProviderContract.getContentUri(GHIBLI_AUTHORITY),
                null, null, null, null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        val count = data.count
        val matches: TextView = findViewById(R.id.matches)
        if (BuildConfig.DEBUG) {
            var percentArtworkWithCaption = 0
            if (count > 0) {
                // The position of the Cursor isn't reset after rotation
                // so reset the position before iterating through the Cursor
                data.moveToPosition(-1)
                while (data.moveToNext()) {
                    val caption = data.getString(data.getColumnIndex(ProviderContract.Artwork.BYLINE))
                    if (caption != null && !caption.isEmpty()) {
                        percentArtworkWithCaption++
                    }
                }
                percentArtworkWithCaption *= 100
                percentArtworkWithCaption /= count
            }
            matches.text = resources.getQuantityString(R.plurals.match_count_debug, count,
                    count, percentArtworkWithCaption)
        } else {
            matches.text = resources.getQuantityString(R.plurals.match_count, count, count)
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {}

    fun onColor(view: View) {
        val selectedView = if (view is FrameLayout) {
            view.getChildAt(0)
        } else {
            view
        }

        when(selectedView.id) {
            R.id.black -> UpdateMuzeiWorker.toggleColor(this, "black")
            R.id.maroon -> UpdateMuzeiWorker.toggleColor(this, "maroon")
            R.id.green -> UpdateMuzeiWorker.toggleColor(this, "green")
            R.id.teal -> UpdateMuzeiWorker.toggleColor(this, "teal")
            R.id.navy -> UpdateMuzeiWorker.toggleColor(this, "navy")
        }

        updateMatches()
    }

    private fun updateMatches() {
        val color = UpdateMuzeiWorker.getCurrentColor(this)
        findViewById<View>(R.id.black).alpha = ALPHA_DEACTIVATED
        findViewById<View>(R.id.maroon).alpha = ALPHA_DEACTIVATED
        findViewById<View>(R.id.navy).alpha = ALPHA_DEACTIVATED
        findViewById<View>(R.id.teal).alpha = ALPHA_DEACTIVATED
        findViewById<View>(R.id.green).alpha = ALPHA_DEACTIVATED
        if ("black" == color) findViewById<View>(R.id.black).alpha = 1.0f
        if ("maroon" == color) findViewById<View>(R.id.maroon).alpha = 1.0f
        if ("navy" == color) findViewById<View>(R.id.navy).alpha = 1.0f
        if ("teal" == color) findViewById<View>(R.id.teal).alpha = 1.0f
        if ("green" == color) findViewById<View>(R.id.green).alpha = 1.0f
    }
}
