package name.vbraun.filepicker;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ntx.note.bookshelf.ImportConflictList;

public class ImportConflictListAdapter extends BaseAdapter {
    private static final int NUM_PER_PAGE = 5; // for showing items per page

    private List<ImportItem> mConflictList = new ArrayList<>();
    private HashMap<String, String> mUuidTitleHashMap = new HashMap<>();
    private int mTotalPage = 1;
    private int mCurrentPage = 1;

    public ImportConflictListAdapter(ImportConflictList importConflictList) {
        this.mConflictList.clear();
        this.mConflictList.addAll(importConflictList.getConflictList());
        this.mUuidTitleHashMap = importConflictList.getUuidNoteTitleHashMap();
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

    public List<ImportItem> getResultList() {
        return mConflictList;
    }

    public List<ImportItem> getCurrentPageList() {
        List<ImportItem> currentList = new ArrayList<>();
        for (int i = 0; i < NUM_PER_PAGE; i++) {
            if (getRealPosition(i) >= mConflictList.size())
                break;
            currentList.add(mConflictList.get(getRealPosition(i)));
        }
        return currentList;
    }

    public String getItemMappingTitle(String uuid) {
        return mUuidTitleHashMap.get(uuid);
    }

    public boolean isItemSelected(int position) {
        return mConflictList.get(getRealPosition(position)).isItemSelected();
    }

    public boolean isAllItemSelected() {
        for (ImportItem item : mConflictList) {
            if (!item.isItemSelected())
                return false;
        }
        return true;
    }

    public boolean isAllItemUnSelected() {
        for (ImportItem item : mConflictList) {
            if (item.isItemSelected())
                return false;
        }
        return true;
    }

    public void setAllItemSelect(boolean isSelect) {
        for (ImportItem item : mConflictList) {
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
