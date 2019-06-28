package name.vbraun.filepicker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;

import android.os.AsyncTask;

public class RestoreListFilterAsyncTask extends AsyncTask<String, Void, Void> {
	private static final String NOTEBOOK_DIRECTORY_PREFIX = "notebook_";

	public AsyncTaskResult<List<RestoreItem>> filterFinishCallback = null;

	private List<RestoreItem> mList;
	private List<RestoreItem> filterResult = new ArrayList<RestoreItem>();
	private int mEntryCount;

	public RestoreListFilterAsyncTask(List<RestoreItem> list) {
		this.mList = list;
	}

	@Override
	protected Void doInBackground(String... arg0) {
		final String uuidStr = arg0[0];
		filterResult.clear();

		if (mList.isEmpty())
			return null;

		for (RestoreItem item : mList) {
			File file = new File(item.getFilePath());
			try {
				String fUuid = getUuidFromFile(file);
				if (fUuid.equals(uuidStr)) {
					item.setPages(mEntryCount - 1);
					filterResult.add(item);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		if (filterFinishCallback != null)
			filterFinishCallback.taskFinish(filterResult);
	}

	private String getUuidFromFile(File file) throws IOException {
		TarInputStream tis = null;
		UUID uuid = null;

		// check file and get uuid
		try {
			tis = new TarInputStream(new BufferedInputStream(new FileInputStream(file)));
			TarEntry entry;
			mEntryCount = 0;
			while ((entry = tis.getNextEntry()) != null) {
				if (entry.getName() == null)
					throw new IOException("Incorrect book archive file");
				if (uuid == null) {
					uuid = getBookUuidFromDirectoryName(entry.getName());
					if (uuid == null)
						throw new IOException("Incorrect book archive file");
				} else if (!uuid.equals(getBookUuidFromDirectoryName(entry.getName())))
					throw new IOException("Incorrect book archive file");

				mEntryCount++;
			}
		} catch (IOException e) {
			throw new IOException(e.getMessage());
		} finally {
			try {
				if (tis != null)
					tis.close();
			} catch (IOException e) {
				throw new IOException(e.getMessage());
			}
		}
		if (uuid == null)
			throw new IOException("No ID in book archive file.");

		return uuid.toString();
	}

	private UUID getBookUuidFromDirectoryName(String name) {
		if (!name.startsWith(NOTEBOOK_DIRECTORY_PREFIX))
			return null;
		int n = NOTEBOOK_DIRECTORY_PREFIX.length();
		String uuid = name.substring(n, n + 36);
		return UUID.fromString(uuid);
	}
}
