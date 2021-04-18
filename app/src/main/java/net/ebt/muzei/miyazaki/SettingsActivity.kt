package net.ebt.muzei.miyazaki

import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.google.android.apps.muzei.api.provider.ProviderContract
import net.ebt.muzei.miyazaki.databinding.SettingsBinding
import net.ebt.muzei.miyazaki.load.UpdateMuzeiWorker

class SettingsActivity : ComponentActivity(), LoaderManager.LoaderCallbacks<Cursor> {

    companion object {
        private const val ALPHA_DEACTIVATED = 0.3f
    }

    private val binding by lazy {
        SettingsBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateMatches()
        LoaderManager.getInstance(this).initLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, arguments: Bundle?): Loader<Cursor> {
        return CursorLoader(this,
                ProviderContract.getContentUri(GhibliArtProvider.AUTHORITY),
                null, null, null, null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        val count = data.count
        if (BuildConfig.DEBUG) {
            var percentArtworkWithCaption = 0
            if (count > 0) {
                // The position of the Cursor isn't reset after rotation
                // so reset the position before iterating through the Cursor
                data.moveToPosition(-1)
                while (data.moveToNext()) {
                    val caption = data.getString(data.getColumnIndex(ProviderContract.Artwork.BYLINE))
                    if (caption != null && caption.isNotEmpty()) {
                        percentArtworkWithCaption++
                    }
                }
                percentArtworkWithCaption *= 100
                percentArtworkWithCaption /= count
            }
            binding.matches.text = resources.getQuantityString(R.plurals.match_count_debug, count,
                    count, percentArtworkWithCaption)
        } else {
            binding.matches.text = resources.getQuantityString(R.plurals.match_count, count, count)
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
        binding.black.alpha = ALPHA_DEACTIVATED
        binding.maroon.alpha = ALPHA_DEACTIVATED
        binding.navy.alpha = ALPHA_DEACTIVATED
        binding.teal.alpha = ALPHA_DEACTIVATED
        binding.green.alpha = ALPHA_DEACTIVATED
        if ("black" == color) binding.black.alpha = 1.0f
        if ("maroon" == color) binding.maroon.alpha = 1.0f
        if ("navy" == color) binding.navy.alpha = 1.0f
        if ("teal" == color) binding.teal.alpha = 1.0f
        if ("green" == color) binding.green.alpha = 1.0f
    }
}
