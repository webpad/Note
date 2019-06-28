package ntx.note.bookshelf;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import name.vbraun.filepicker.ImportItem;

/**
 * Created by karote on 2018/12/4.
 */

public class ImportConflictList implements Parcelable {
    private List<ImportItem> mConflictList = new ArrayList<>();
    private HashMap<String, String> mUuidNoteTitleHashMap = new HashMap<>();

    ImportConflictList(List<ImportItem> conflictList, HashMap<String, String> uuidTitleHashMap) {
        this.mConflictList = conflictList;
        this.mUuidNoteTitleHashMap = uuidTitleHashMap;
    }

    public List<ImportItem> getConflictList() {
        return mConflictList;
    }

    public HashMap<String, String> getUuidNoteTitleHashMap() {
        return mUuidNoteTitleHashMap;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(this.mConflictList);
        dest.writeSerializable(this.mUuidNoteTitleHashMap);
    }

    protected ImportConflictList(Parcel in) {
        this.mConflictList = new ArrayList<ImportItem>();
        in.readList(this.mConflictList, ImportItem.class.getClassLoader());
        this.mUuidNoteTitleHashMap = (HashMap<String, String>) in.readSerializable();
    }

    public static final Parcelable.Creator<ImportConflictList> CREATOR = new Parcelable.Creator<ImportConflictList>() {
        @Override
        public ImportConflictList createFromParcel(Parcel source) {
            return new ImportConflictList(source);
        }

        @Override
        public ImportConflictList[] newArray(int size) {
            return new ImportConflictList[size];
        }
    };
}
