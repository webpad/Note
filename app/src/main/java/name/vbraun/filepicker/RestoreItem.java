package name.vbraun.filepicker;

public class RestoreItem {
	private String mFilePath;
	private String mFileName;
	private long mFileDate;
	private long mFileSize;
	private int mPages;

	public RestoreItem(String filePath, String fileName, long fileDate, long fileSize) {
		this.mFilePath = filePath;
		this.mFileName = fileName;
		this.mFileDate = fileDate;
		this.mFileSize = fileSize;
		this.mPages = 0;
	}

	public RestoreItem(String filePath, String fileName, long fileDate, long fileSize, int pages) {
		this.mFilePath = filePath;
		this.mFileName = fileName;
		this.mFileDate = fileDate;
		this.mFileSize = fileSize;
		this.mPages = pages;
	}

	public String getFilePath() {
		return this.mFilePath;
	}

	public String getFileName() {
		return this.mFileName;
	}

	public long getFileDate() {
		return this.mFileDate;
	}

	public long getFileSize() {
		return this.mFileSize;
	}

	public void setPages(int pages) {
		this.mPages = pages;
	}

	public int getPages() {
		return this.mPages;
	}

}
