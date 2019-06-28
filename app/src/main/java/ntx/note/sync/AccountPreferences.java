package ntx.note.sync;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

import ntx.note.ActivityBase;
import ntx.note2.R;

public class AccountPreferences extends PreferenceActivity {
	private final static String TAG = "AccountPreferences";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			addPreferencesFromResource(R.xml.account_preferences);
		} catch (ClassCastException e) {
			Log.e(TAG, e.toString());
		}
	}
	
	
	@Override
	protected void onResume() {
        ActivityBase.quillIncRefcount();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
        ActivityBase.quillDecRefcount();
	}
}
