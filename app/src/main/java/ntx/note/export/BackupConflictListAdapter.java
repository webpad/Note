package ntx.note.export;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ntx.note.data.Bookshelf;

public class BackupConflictListAdapter extends BaseAdapter {
    private static final int NUM_PER_PAGE = 5; // for showing items per page

    private List<BackupConflictItem> mConflictList = new ArrayList<>();
    private int mTotalPage = 1;
    private int mCurrentPage = 1;

    public BackupConflictListAdapter(List<UUID> backupConflictUuidList) {
        this.mConflictList.clear();
        for (UUID uuid : backupConflictUuidList) {
            String bookTitle = Bookshelf.getInstance().getBook(uuid).getTitle();
            this.mConflictList.add(new BackupConflictItem(uuid, bookTitle));
        }
        setTotalPage();
    }

    @Override
    public int getCount() {
        return this.mConflictList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.mConflictList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        setTotalPage();
    }

    public int getTotalPage() {
        return this.mTotalPage;
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public void setCurrentPage(int currentPage) {
        if (currentPage > mTotalPage) { // only one page
            currentPage = 1;
        } else if (currentPage <= 0) {
            currentPage = mTotalPage;
        }
        this.mCurrentPage = currentPage;
    }

    public List<BackupConflictItem> getUnSelectedList() {
        List<BackupConflictItem> unSelectedList = new ArrayList<>();
        for (BackupConflictItem item : mConflictList) {
            if (!item.isItemSelected())
                unSelectedList.add(item);
        }
        return unSelectedList;
    }

    public List<BackupConflictItem> getCurrentPageList() {
        List<BackupConflictItem> currentList = new ArrayList<>();
        for (int i = 0; i < NUM_PER_PAGE; i++) {
            if (getRealPosition(i) >= mConflictList.size())
                break;
            currentList.add(mConflictList.get(getRealPosition(i)));
        }
        return currentList;
    }

    public boolean isItemSelected(int position) {
        return mConflictList.get(getRealPosition(position)).isItemSelected();
    }

    public boolean isAllItemSelected() {
        for (BackupConflictItem item : mConflictList) {
            if (!item.isItemSelected())
                return false;
        }
        return true;
    }

    public boolean isAllItemUnSelected() {
        for (BackupConflictItem item : mConflictList) {
            if (item.isItemSelected())
                return false;
        }
        return true;
    }

    public void setAllItemSelect(boolean isSelect) {
        for (BackupConflictItem item : mConflictList) {
            item.setItemSelected(isSelect);
        }
    }

    public void setItemSelect(int position, boolean isSelected) {
        mConflictList.get(getRealPosition(position)).setItemSelected(isSelected);
    }

    private int getRealPosition(int position) {
        return (((mCurrentPage - 1) * NUM_PER_PAGE) + position);
    }

    private void setTotalPage() {
        if (mConflictList.size() == 0) {
            this.mTotalPage = 1;
        } else if (mConflictList.size() % NUM_PER_PAGE != 0) {
            this.mTotalPage = (mConflictList.size() / NUM_PER_PAGE) + 1;
        } else {
            this.mTotalPage = mConflictList.size() / NUM_PER_PAGE;
        }
    }
}
