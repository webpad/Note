package ntx.note.data;

import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.UUID;

import ntx.note.Global;

public class BookDirectory extends DirectoryBase {
	private static final long serialVersionUID = -3424988863197267971L;
	private final static String TAG = "BookDir";

	public BookDirectory(Storage storage, UUID uuid) {
		super(storage, Storage.NOTEBOOK_DIRECTORY_PREFIX, uuid);
	}
	
	private FilenameFilter getPrefixFilter(final String prefix, final String suffix) {
		return new FilenameFilter() {
		    public boolean accept(File directory, String name) {
				return name.startsWith(prefix) && name.endsWith(suffix);
		    }};
	}
	
	protected LinkedList<UUID> listPages() {
		FilenameFilter filter = getPrefixFilter(Book.PAGE_FILE_PREFIX, Book.QUILL_DATA_FILE_SUFFIX);
		File[] entries = listFiles(filter);
		LinkedList<UUID> uuids = new LinkedList<UUID>();
		if (entries == null) return uuids;
		for (File page : entries) {
			String path = page.getAbsolutePath();
			int pos = path.lastIndexOf(Book.PAGE_FILE_PREFIX);
			pos += Book.PAGE_FILE_PREFIX.length();
			try {
				UUID uuid = UUID.fromString(path.substring(pos, pos+36));
				if (Global.isDebug)
				Log.d(TAG, "Found page: "+uuid);
				uuids.add(uuid);
			} catch (StringIndexOutOfBoundsException e) {
				page.delete();
				Log.e(TAG, "Malformed file name: "+uuid);
			}
		}
		return uuids;
	}
	
	/**
	 * List everything that is not Page ,index data and fast show image files
	 */
	protected LinkedList<UUID> listBlobs() {
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File directory, String name) {
		        return !name.startsWith(Book.PAGE_FILE_PREFIX) && 
		        		!name.startsWith(Book.INDEX_FILE) &&
						!name.startsWith(Book.AUTO) &&
						!name.startsWith(Global.PAGE_PREVIEW_BITMAP_FILE_NAME_PREFIX);
		    }}; 
		File[] entries = listFiles(filter);
		LinkedList<UUID> uuids = new LinkedList<UUID>();
		if (entries == null) return uuids;
		for (File blob : entries) {
			String path = blob.getName();
			try {
				UUID uuid = UUID.fromString(path.substring(0, 36));
				if (Global.isDebug)
				Log.d(TAG, "Found blob: "+uuid);
				uuids.add(uuid);
			} catch (StringIndexOutOfBoundsException e) {
				blob.delete();
				Log.e(TAG, "Malformed file name: "+uuid);
			}
		}
		return uuids;
	}

	protected LinkedList<File> listAutos() {
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File directory, String name) {
				return name.startsWith(Book.AUTO);
			}};
		File[] entries = listFiles(filter);
		LinkedList<File> auto = new LinkedList<>();
		for (File file : entries) {
			auto.add(file);
		}
		return auto;
	}

	/**
	 * Return the file with given UUID
	 * @param uuid
	 * @return A File object or null if it does not exist
	 */
	protected File getFile(UUID uuid) {
		final String uuidStr = uuid.toString();
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File directory, String name) {
		        return name.contains(uuidStr);
		    }};
		File[] entries = listFiles(filter);
		if (entries.length != 1)
			Log.e(TAG, "getFile() found "+entries.toString());
		if (entries.length < 1) 
			return null;
		return entries[0];
	}

	protected long getTotalSize(){
		long totalSize = 0;
		File[] entries = listFiles();
		for (File entry : entries) {
			totalSize += entry.length();
		}
		return totalSize;
	}
	
}


