package ntx.note.bookshelf;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class DateNoteData {
    @SerializedName("bookTitle")
    private String bookTitle = "";

    @SerializedName("uuid")
    private UUID uuid;

    @SerializedName("create Time")
    private String createTime;

    @SerializedName("modify Time")
    private String modifyTime;

    public void setTitle(String title) {
        this.bookTitle = title;
    }

    public void setId(UUID uuid) {
        this.uuid = uuid;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public void setModifyTime(String modifyTime) {
        this.modifyTime = modifyTime;
    }

    public String getTitle() {
        return this.bookTitle;
    }

    public UUID getId() {
        return this.uuid;
    }

    public String getCreateTime() {
        return this.createTime;
    }

    public String getModifyTime() {
        return this.modifyTime;
    }
}
