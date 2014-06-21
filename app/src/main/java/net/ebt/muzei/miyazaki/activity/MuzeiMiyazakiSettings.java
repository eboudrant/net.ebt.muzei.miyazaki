package net.ebt.muzei.miyazaki.activity;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import net.ebt.muzei.miyazaki.R;
import net.ebt.muzei.miyazaki.service.MuzeiMiyazakiService;
import net.ebt.muzei.miyazaki.util.Utils;

import static net.ebt.muzei.miyazaki.Constants.*;

public class MuzeiMiyazakiSettings extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(null);

        setContentView(R.layout.settings);

        final SharedPreferences settings = getApplicationContext().getSharedPreferences(CURRENT_PREF_NAME, Context.MODE_PRIVATE);
        int interval = settings.getInt(MUZEI_INTERVAL, DEFAULT_INTERVAL);

        final ImageView icon = (ImageView) findViewById(R.id.muzei_icon);
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
                label.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                label.setText(null);
                label.setVisibility(View.GONE);
                configLabel.setText("Refresh every " + Utils.formatDuration(INTERVALS.get(seekBar.getProgress())));
                configLabel.setVisibility(View.VISIBLE);

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

        wifi.setChecked(settings.getBoolean(MUZEI_WIFI, false));
        label.setVisibility(View.GONE);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + getPackageName()));
                startActivity(intent);
            }
        });
    }

}
