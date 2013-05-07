package android.netinf.node;

import java.util.Map;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.netinf.R;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsActivity extends Activity {

    public static final String TAG = SettingsActivity.class.getSimpleName();

    public static int getPreferenceAsInt(String key) {
        return Integer.valueOf(getPreference(key));
    }

    public static String getPreference(String key) {
        return PreferenceManager.getDefaultSharedPreferences(Node.getContext()).getString(key, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load preferences
            addPreferencesFromResource(R.xml.preferences);

            // Set default summaries
            Map<String, ?> prefs = PreferenceManager.getDefaultSharedPreferences(getActivity()).getAll();
            for (String key : prefs.keySet()) {
                updateSummary(key);
            }

        }

        private void updateSummary(String key) {
            String value = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(key, "?");
            findPreference(key).setSummary(value);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            updateSummary(key);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }

}
