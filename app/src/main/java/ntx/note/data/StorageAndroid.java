package ntx.note.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import junit.framework.Assert;

import java.io.File;
import java.util.UUID;

public class StorageAndroid extends Storage {
	public final static String TAG = "StorageAndroid";

	private final Context context;

	public static void initialize(Context context) {
		if (instance != null) return;
		instance = new StorageAndroid(context);
		instance.postInitializaton();
	}

	private StorageAndroid(Context context) {
		Assert.assertNull(Storage.instance); // only construct once
		this.context = context.getApplicationContext();
	}

	@Override
    public File getFilesDir() {
		return context.getFilesDir();
	}

	@Override
    public File getExternalStorageDirectory() {
		return Environment.getExternalStorageDirectory();
	}

	@Override
    public String formatDateTime(long millis) {
		int fmt = DateUtils.FORMAT_SHOW_DATE + DateUtils.FORMAT_SHOW_TIME +
				DateUtils.FORMAT_SHOW_YEAR + DateUtils.FORMAT_SHOW_WEEKDAY;
		return  DateUtils.formatDateTime(context, millis, fmt);
	}


	public static final String KEY_CURRENT_BOOK_UUID = "current_book_uuid";

	@Override
    protected UUID loadCurrentBookUUID() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String s = settings.getString(KEY_CURRENT_BOOK_UUID, null);
        if (s == null)
        	return null;
        else
        	return UUID.fromString(s);
	}

	@Override
    protected void saveCurrentBookUUID(UUID uuid) {
        SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(KEY_CURRENT_BOOK_UUID, uuid.toString());
        editor.commit();
	}

	@Override
    public void LogMessage(String TAG, String message) {
		final String msg;
		if (message != null)
			msg = message;
		else
			msg = "No message details provided.";
		Log.d(TAG, msg);
		// showToast(message, Toast.LENGTH_LONG);
	}

	@Override
    public void LogError(String TAG, String message) {
		final String msg;
		if (message != null)
			msg = message;
		else
			msg = "Unknown error.";
		Log.e(TAG, msg);
	}

	public static final String KEY_AUTO_BACKUP = "backup_automatic";
	public static final String KEY_BACKUP_DIR  = "backup_directory";

	/**
	 * Return the backup directory
	 * @return File or null. The latter means it is not desired to make backups.
	 */
	@Override
    public File getBackupDir() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		boolean backup_automatic = settings.getBoolean(KEY_AUTO_BACKUP, true);
		if (!backup_automatic) return null;
		String backup_directory = settings.getString(KEY_BACKUP_DIR, getDefaultBackupDir().getAbsolutePath());
		File dir = new File(backup_directory);
		if (!dir.exists())
			dir.mkdir();
		return dir;
	}


}
