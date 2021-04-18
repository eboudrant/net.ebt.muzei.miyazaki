package net.ebt.muzei.miyazaki

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import com.google.android.apps.muzei.api.MuzeiContract.Sources.createChooseProviderIntent
import com.google.android.apps.muzei.api.MuzeiContract.Sources.isProviderSelected
import net.ebt.muzei.miyazaki.common.BuildConfig

/**
 * This activity's sole purpose is to redirect users to Muzei, which is where they should
 * activate Muzei and then select the Earth View source.
 *
 * You'll note the usage of the `enable_launcher` boolean resource value to only enable
 * this on API 29+ devices as it is on API 29+ that a launcher icon becomes mandatory for
 * every app.
 */
class RedirectActivity : ComponentActivity() {

    companion object {
        private const val MUZEI_PACKAGE_NAME = "net.nurik.roman.muzei"
        private const val PLAY_STORE_LINK = "https://play.google.com/store/apps/details?id=$MUZEI_PACKAGE_NAME"
    }

    private val redirectLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        // It doesn't matter what the result is, the important part is that the
        // user hit the back button to return to this activity. Since this activity
        // has no UI of its own, we can simply finish the activity.
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // First check whether Ghibli is already selected
        val launchIntent = packageManager.getLaunchIntentForPackage(MUZEI_PACKAGE_NAME)
        if (isProviderSelected(this, BuildConfig.GHIBLI_AUTHORITY)
                && launchIntent != null) {
            // Already selected so just open Muzei
            redirectLauncher.launch(launchIntent)
            return
        }
        // Ghibli isn't selected, so try to deep link into Muzei's Sources screen
        val deepLinkIntent = createChooseProviderIntent(BuildConfig.GHIBLI_AUTHORITY)
        if (tryStartIntent(deepLinkIntent, R.string.toast_enable)) {
            return
        }
        // createChooseProviderIntent didn't work, so try to just launch Muzei
        if (launchIntent != null && tryStartIntent(launchIntent, R.string.toast_enable_source)) {
            return
        }
        // Muzei isn't installed, so try to open the Play Store so that
        // users can install Muzei
        val playStoreIntent = Intent(Intent.ACTION_VIEW).setData(Uri.parse(PLAY_STORE_LINK))
        if (tryStartIntent(playStoreIntent, R.string.toast_muzei_missing_error)) {
            return
        }
        // Only if all Intents failed do we show a 'everything failed' Toast
        Toast.makeText(this, R.string.toast_play_store_missing_error, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun tryStartIntent(intent: Intent, @StringRes toastResId: Int): Boolean {
        return try {
            // Use startActivityForResult() so that we get a callback to
            // onActivityResult() if the user hits the system back button
            redirectLauncher.launch(intent)
            Toast.makeText(this, toastResId, Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            false
        }
    }
}
