package ntx.note.bookshelf;

import com.google.gson.annotations.SerializedName;

public class RecentlyBookData {
	@SerializedName("recentIndex")
	private int recentIndex = 0;
	
	@SerializedName("bookId")
	private long bookId = 0;
	
	@SerializedName("bookTitle")
	private String bookTitle = "";

	@SerializedName("bookAuthors")
	private String bookAuthors = "";
	
	@SerializedName("bookPath")
	private String bookPath = "";

	@SerializedName("bookSize")
	private long bookSize = 0;
	
	@SerializedName("bookEncoding")
	private String bookEncoding = "";
	
	@SerializedName("bookLanguage")
	private String bookLanguage = "";
	
	@SerializedName("bookHasCover")
	private boolean bookHasCover = false;

	@SerializedName("bookType")
	private String bookType = "";
	
	public RecentlyBookData(int index) {
		this.recentIndex = index;
	}
	
	public void setId(long id) {
		this.bookId = id;
	}
	
	public void setTitle(String title) {
		this.bookTitle = title;
	}

	public void setAuthors(String authors) {
		this.bookAuthors = authors;
	}
	
	public void setPath(String path) {
		this.bookPath = path;
	}

	public void setSize(long size) {
		this.bookSize = size;
	}
	
	public void setEncoding(String encoding) {
		this.bookEncoding = encoding;
	}
	
	public void setLanguage(String language) {
		this.bookLanguage = language;
	}
	
	public void setCover(boolean hasCover) {
		this.bookHasCover = hasCover;
	}

	public void setType(String type) {
		this.bookType = type;
	}
	
	public int getIndex() {
		return this.recentIndex;
	}
	
	public long getId() {
		return this.bookId;
	}
	
	public String getTitle() {
		return this.bookTitle;
	}

	public String getAuthors() {
		return this.bookAuthors;
	}
	
	public String getPath() {
		return this.bookPath;
	}

	public long getSize() {
		return this.bookSize;
	}
	
	public String getEncoding() {
		return this.bookEncoding;
	}
	
	public String getLanguage() {
		return this.bookLanguage;
	}
	
	public boolean hasCover() {
		return this.bookHasCover;
	}

	public String getType() {
		return this.bookType;
	}
}
