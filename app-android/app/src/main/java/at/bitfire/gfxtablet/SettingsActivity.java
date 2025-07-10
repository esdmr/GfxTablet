package at.bitfire.gfxtablet;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
	public static final String
		KEY_PREF_HOST = "host_preference",
		KEY_PREF_STYLUS_ONLY = "stylus_only_preference",
		KEY_DARK_CANVAS = "dark_canvas_preference",
		KEY_KEEP_DISPLAY_ACTIVE = "keep_display_active_preference",
		KEY_TEMPLATE_IMAGE = "key_template_image";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar supportActionBar = getSupportActionBar();
		if (supportActionBar != null) {
			supportActionBar.setDisplayHomeAsUpEnabled(true);
		}

		setContentView(R.layout.activity_settings);

		SettingsFragment prefFragment = new SettingsFragment();
		getSupportFragmentManager()
			.beginTransaction()
			.replace(R.id.fragment, prefFragment)
			.commit();
	}

}
