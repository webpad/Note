package ntx.note.export;

import java.util.UUID;

/**
 * Created by karote on 2018/12/14.
 */

public class BackupConflictItem {
    private UUID mUuid;
    private String mFileName;
    private boolean mIsSelected;

    public BackupConflictItem(UUID uuid, String fileName) {
        this.mUuid = uuid;
        this.mFileName = fileName;
        this.mIsSelected = true;
    }

    public void setItemSelected(boolean isSelected) {
        this.mIsSelected = isSelected;
    }

    public String getBackupFileName() {
        return mFileName;
    }

    public UUID getBackupUuid() {
        return this.mUuid;
    }

    public boolean isItemSelected() {
        return mIsSelected;
    }
}
