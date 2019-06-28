package ntx.note.thumbnail;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import name.vbraun.view.write.Page;

public class ThumbnailListAdapter extends BaseAdapter {
    private int mNumPerPage = 6;
    private List<Page> mList = new ArrayList<>();
    private Map<UUID, Boolean> isPageSelectedHashMap = new WeakHashMap<UUID, Boolean>();
    private int mTotalPage = 1;
    private int mCurrentPage = 1;

    public ThumbnailListAdapter(List<Page> pageList, int numPerPage) {
        this.mList = pageList;
        this.mNumPerPage = numPerPage;
        for (Page page : mList) {
            isPageSelectedHashMap.put(page.getUUID(), false);
        }
        setTotalPage();
    }

    @Override
    public int getCount() {
        return this.mList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.mList.get(position);
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

        if (mCurrentPage > mTotalPage)
            mCurrentPage = mTotalPage;
    }

    public void updateList(List<Page> newList) {
        this.mList = newList;
    }

    public int getTotalPage() {
        return this.mTotalPage;
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public int getPageIndexOfTheSpecificPageContent(Page specificPage) {
        int index = mList.indexOf(specificPage) + 1;
        if ((index % mNumPerPage) != 0)
            return ((index / mNumPerPage) + 1);
        else
            return (index / mNumPerPage);
    }

    public void setCurrentPage(int currentPage) {
        if (currentPage > mTotalPage) { // only one page
            currentPage = 1;
        } else if (currentPage <= 0) {
            currentPage = mTotalPage;
        }
        this.mCurrentPage = currentPage;
    }

    public List<Page> getCurrentPageList() {
        List<Page> currentList = new ArrayList<>();
        currentList.clear();
        for (int i = 0; i < mNumPerPage; i++) {
            if (getRealPosition(i) >= mList.size())
                break;
            currentList.add(mList.get(getRealPosition(i)));
        }

        return currentList;
    }

    public LinkedList<Page> getSelectedPageList() {
        LinkedList<Page> selectedList = new LinkedList<>();
        selectedList.clear();
        for (Page page : mList) {
            if (isPageSelected(page.getUUID())) {
                selectedList.add(page);
            }
        }

        return selectedList;
    }

    public void setPageSelected(UUID uuid, boolean isSelected) {
        isPageSelectedHashMap.put(uuid, isSelected);
    }

    public void clearSelected() {
        isPageSelectedHashMap.clear();
        for (Page page : mList) {
            isPageSelectedHashMap.put(page.getUUID(), false);
        }
    }

    public boolean isPageSelected(UUID uuid) {
        Boolean isSelected = isPageSelectedHashMap.get(uuid);
        if (isSelected == null)
            return false;
        return isSelected;
    }

    private int getRealPosition(int position) {
        return (((mCurrentPage - 1) * mNumPerPage) + position);
    }

    private void setTotalPage() {
        if (mList.size() == 0) {
            this.mTotalPage = 1;
        } else if (mList.size() % mNumPerPage != 0) {
            this.mTotalPage = (mList.size() / mNumPerPage) + 1;
        } else {
            this.mTotalPage = mList.size() / mNumPerPage;
        }
    }
}
