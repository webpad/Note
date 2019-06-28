package name.vbraun.filepicker;

public class ImportItem extends RestoreItem {
    public @interface ImportItemType {
        int GROUP_HEADER = 0;
        int GROUP_ITEM = 1;
        int SINGLE_ITEM = 2;
    }

    private int mType;
    private int mGroupSize;
    private int mGroupIndex;
    private String mUuid;
    private boolean mIsSelected;

    // Group Header Constructor
    public ImportItem(int groupIndex, int groupSize, String uuid) {
        super("", "", 0, 0);
        this.mType = ImportItemType.GROUP_HEADER;
        this.mGroupIndex = groupIndex;
        this.mGroupSize = groupSize;
        this.mUuid = uuid;
        this.mIsSelected = false;
    }

    // Group RestoreItem Constructor
    public ImportItem(int groupIndex, int groupSize, String uuid, RestoreItem item) {
        super(item.getFilePath(), item.getFileName(), item.getFileDate(), item.getFileSize(), item.getPages());
        this.mType = ImportItemType.GROUP_ITEM;
        this.mGroupIndex = groupIndex;
        this.mGroupSize = groupSize;
        this.mUuid = uuid;
        this.mIsSelected = false;
    }

    // Single RestoreItem Constructor
    public ImportItem(String uuid, RestoreItem item) {
        super(item.getFilePath(), item.getFileName(), item.getFileDate(), item.getFileSize(), item.getPages());
        this.mType = ImportItemType.SINGLE_ITEM;
        this.mGroupSize = 0;
        this.mGroupIndex = -1;
        this.mUuid = uuid;
        this.mIsSelected = false;
    }

    public int getItemType() {
        return mType;
    }

    public int getGroupSize() {
        return mGroupSize;
    }

    public int getGroupIndex() {
        return mGroupIndex;
    }

    public String getItemUuid() {
        return mUuid;
    }

    public void setItemSelected(boolean isSelected) {
        this.mIsSelected = isSelected;
    }

    public boolean isItemSelected() {
        return this.mIsSelected;
    }
}
