package ntx.note.bookshelf;

import com.google.gson.annotations.SerializedName;

public class DropboxNoteData {

	@SerializedName(".tag")
	private String tag = "";

	@SerializedName("name")
	private String name = "";

	@SerializedName("id")
	private String id = "";

	@SerializedName("client_modified")
	private String client_modified = "";

	@SerializedName("server_modified")
	private String server_modified = "";

	@SerializedName("rev")
	private String rev = "";

	@SerializedName("size")
	private String size = "";

	@SerializedName("path_lower")
	private String path_lower = "";

	@SerializedName("path_display")
	private String path_display = "";

	@SerializedName("content_hash")
	private String content_hash = "";

	public void setTag(String tag) 					{ this.tag = tag; }
	public void setName(String name) 				{ this.name = name; }
	public void setId(String id) 					{ this.id = id; }
	public void setClientModified(String client)	{ this.client_modified = client; }
	public void setServerModified(String server) 	{ this.server_modified = server; }
	public void setRev(String rev) 					{ this.rev = rev; }
	public void setSize(String size) 				{ this.size = size; }
	public void setPathLower(String path_lower) 	{ this.path_lower = path_lower; }
	public void setPathDisplay(String path_display) { this.path_display = path_display; }
	public void setContentHash(String content_hash) { this.content_hash = content_hash; }

	public String getTag() 				{ return tag; }
	public String getName() 			{ return name; }
	public String getId() 				{ return id; }
	public String getClientModified()	{ return client_modified; }
	public String getServerModified() 	{ return server_modified; }
	public String getRev() 				{ return rev; }

	public long getSize(){

		return Long.parseLong(size);
	}

	public String getPathLower() 		{ return path_lower; }
	public String getPathDisplay() 		{ return path_display; }
	public String getContentHash() 		{ return content_hash; }

//	Example :
//	{
//		".tag" : "file",
//			"name" : "Note 2.note",
//			"id" : "id:jCKbvFxP8-IAAAAAAAAA1A",
//			"client_modified" : "2018-12-04T10:07:53Z",
//			"server_modified" : "2018-12-04T10:07:54Z",
//			"rev" : "015500000001099548d0",
//			"size" : 6144,
//			"path_lower" : "/note 2.note",
//			"path_display" : "/Note 2.note",
//			"content_hash" : "fd6fa96dd03a83ad61a3f7a1bb63cec12926e8d16df8aabc827319efc6ed9029"
//	}
}
