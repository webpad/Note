package name.vbraun.filepicker;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImportListAdapter extends BaseAdapter {
    private static final int NUM_PER_PAGE = 8; // for showing items per page

    private List<ImportItem> mList = new ArrayList<>();
    private List<List<ImportItem>> mGroupItemList = new ArrayList<>();
    private List<ImportItem> mSingleItemList = new ArrayList<>();
    private int mTotalPage = 1;
    private int mCurrentPage = 1;
    private int mNumPerPage = NUM_PER_PAGE;

    public ImportListAdapter(List<ImportItem> list) {
        updateList(list);
        setTotalPage();
    }

    @Override
    public int getCount() {
        return this.mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
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
        updateMainList();
        setTotalPage();
    }

    public void updateList(List<ImportItem> list) {
        this.mList.clear();
        this.mList.addAll(list);
        updateSubLists();
    }

    public void setNumPerPage(int numPerPage) {
        this.mNumPerPage = numPerPage;
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

    public List<ImportItem> getCurrentPageList() {
        List<ImportItem> currentList = new ArrayList<>();
        for (int i = 0; i < NUM_PER_PAGE; i++) {
            if (getRealPosition(i) >= mList.size())
                break;
            currentList.add(mList.get(getRealPosition(i)));
        }
        return currentList;
    }

    public List<ImportItem> getFilterList(String keyword) {
        String keywordLowerCase = keyword.toLowerCase();
        String fileNameLowerCase;
        List<ImportItem> filterList = new ArrayList<>();
        List<List<ImportItem>> filterGroupList = new ArrayList<>();
        List<ImportItem> filterSingleList = new ArrayList<>();
        for (int i = 0; i < mGroupItemList.size(); i++) {
            List<ImportItem> emptyList = new ArrayList<>();
            filterGroupList.add(emptyList);
            for (ImportItem item : mGroupItemList.get(i)) {
                fileNameLowerCase = item.getFileName().toLowerCase();
                if (fileNameLowerCase.contains(keywordLowerCase)) {
                    filterGroupList.get(i).add(item);
                }
            }
        }
        for (List<ImportItem> itemList : filterGroupList) {
            if (itemList.size() <= 1) {
                if (!itemList.isEmpty())
                    filterSingleList.add(new ImportItem(itemList.get(0).getItemUuid(), itemList.get(0)));
                filterGroupList.remove(itemList);
            }
        }

        for (ImportItem item : mSingleItemList) {
            fileNameLowerCase = item.getFileName().toLowerCase();
            if (fileNameLowerCase.contains(keywordLowerCase))
                filterSingleList.add(item);
        }


        int groupHeaderIndex = 0;
        for (int i = 0; i < filterGroupList.size(); i++) {
            int size = filterGroupList.get(i).size();
            String uuid = filterGroupList.get(i).get(0).getItemUuid();
            filterList.add(new ImportItem(i, size, uuid));
            for (ImportItem item : filterGroupList.get(i)) {
                filterList.add(new ImportItem(i, size, uuid, item));
                if (item.isItemSelected()) {
                    filterList.get(groupHeaderIndex).setItemSelected(true);
                }
            }
            groupHeaderIndex = groupHeaderIndex + size + 1;
        }

        filterList.addAll(filterSingleList);

        return filterList;
    }

    public List<ImportItem> getSelectedList() {
        List<ImportItem> selectedList = new ArrayList<>();
        for (List<ImportItem> itemList : mGroupItemList) {
            for (ImportItem item : itemList) {
                if (item.isItemSelected())
                    selectedList.add(item);
            }
        }
        for (ImportItem item : mSingleItemList) {
            if (item.isItemSelected())
                selectedList.add(item);
        }
        return selectedList;
    }

    public int getSelectedCount() {
        int count = 0;
        for (List<ImportItem> itemList : mGroupItemList) {
            for (ImportItem item : itemList) {
                if (item.isItemSelected())
                    count++;
            }
        }
        for (ImportItem item : mSingleItemList) {
            if (item.isItemSelected())
                count++;
        }
        return count;
    }

    public boolean isAllItemSelected() {
        for (ImportItem item : mList) {
            if (!item.isItemSelected())
                return false;
        }
        return true;
    }

    public boolean isEveryItemSelected() {
        for (List<ImportItem> itemList : mGroupItemList) {
            for (int i = 0; i < itemList.size(); i++) {
                if (itemList.get(i).isItemSelected())
                    break;
                else if (i >= (itemList.size() - 1)) {
                    return false;
                }
            }
        }
        for (ImportItem item : mSingleItemList) {
            if (!item.isItemSelected())
                return false;
        }
        if (mGroupItemList.isEmpty() && mSingleItemList.isEmpty())
            return false;
        return true;
    }

    public void setAllItemSelect() {
        for (List<ImportItem> itemList : mGroupItemList) {
            for (ImportItem item : itemList) {
                item.setItemSelected(true);
            }
        }
        for (ImportItem item : mSingleItemList) {
            item.setItemSelected(true);
        }
        updateMainList();
    }

    public void setEveryItemSelected() {
        List<ImportItem> temp = new ArrayList<>();
        for (List<ImportItem> itemList : mGroupItemList) {
            temp.clear();
            temp.addAll(itemList);
            Collections.sort(temp, new DateComparator());
            Collections.reverse(temp);
            itemList.get(itemList.indexOf(temp.get(0))).setItemSelected(true);
        }
        for (ImportItem item : mSingleItemList) {
            item.setItemSelected(true);
        }
        updateMainList();
    }

    public void clearAllItemSelect() {
        for (List<ImportItem> itemList : mGroupItemList) {
            for (ImportItem item : itemList) {
                item.setItemSelected(false);
            }
        }
        for (ImportItem item : mSingleItemList) {
            item.setItemSelected(false);
        }
        updateMainList();
    }

    public void setGroupItemSelect(int groupIndex, int itemIndex, boolean isSelect) {
        mGroupItemList.get(groupIndex).get(itemIndex).setItemSelected(isSelect);
        updateMainList();
    }

    public void setGroupItemNewestSelect(int groupIndex) {
        List<ImportItem> temp = new ArrayList<>();
        temp.addAll(mGroupItemList.get(groupIndex));
        Collections.sort(temp, new DateComparator());
        Collections.reverse(temp);
        ImportItem newestItem = temp.get(0);
        int newestItemIndex = mGroupItemList.get(groupIndex).indexOf(newestItem);

        mGroupItemList.get(groupIndex)
                .get(newestItemIndex)
                .setItemSelected(true);

        updateMainList();
    }

    public void clearGroupItemSelect(int groupIndex) {
        for (ImportItem importItem : mGroupItemList.get(groupIndex)) {
            importItem.setItemSelected(false);
        }
        updateMainList();
    }

    public void setSingleItemSelect(ImportItem item, boolean isSelect) {
        mSingleItemList.get(mSingleItemList.indexOf(item)).setItemSelected(isSelect);
        updateMainList();
    }

    public int[] getGroupItemGroupIndex(ImportItem item) {
        int[] indexArray = new int[2];
        indexArray[0] = -1;
        indexArray[1] = -1;
        for (int i = 0; i < mGroupItemList.size(); i++) {
            if (mGroupItemList.get(i).contains(item)) {
                indexArray[0] = i;
                indexArray[1] = mGroupItemList.get(i).indexOf(item);
            }
        }
        return indexArray;
    }

    private void updateMainList() {
        mList.clear();
        int groupHeaderIndex = 0;
        for (int i = 0; i < mGroupItemList.size(); i++) {
            int size = mGroupItemList.get(i).size();
            String uuid = mGroupItemList.get(i).get(0).getItemUuid();
            mList.add(new ImportItem(i, size, uuid));
            for (ImportItem item : mGroupItemList.get(i)) {
                mList.add(item);
                if (item.isItemSelected()) {
                    mList.get(groupHeaderIndex).setItemSelected(true);
                }
            }
            groupHeaderIndex = groupHeaderIndex + size + 1;
        }

        mList.addAll(mSingleItemList);
    }

    private void updateSubLists() {
        this.mGroupItemList.clear();
        this.mSingleItemList.clear();
        for (ImportItem item : mList) {
            switch (item.getItemType()) {
                case ImportItem.ImportItemType.GROUP_HEADER:
                    List<ImportItem> emptyGroupList = new ArrayList<>();
                    mGroupItemList.add(emptyGroupList);
                    break;
                case ImportItem.ImportItemType.GROUP_ITEM:
                    mGroupItemList.get(item.getGroupIndex()).add(item);
                    break;
                case ImportItem.ImportItemType.SINGLE_ITEM:
                    mSingleItemList.add(item);
                    break;
            }
        }
    }

    private int getRealPosition(int position) {
        return (((mCurrentPage - 1) * mNumPerPage) + position);
    }

    private void setTotalPage() {
        int groupCount = mGroupItemList.size();
        int singleCount = mSingleItemList.size();
        int totalCount = groupCount + singleCount;

        if (totalCount == 0) {
            this.mTotalPage = 1;
            return;
        } else if (groupCount != 0) {
            for (List<ImportItem> groupItems : mGroupItemList) {
                groupCount += groupItems.size();
            }
        }
        totalCount = groupCount + singleCount;
        if (totalCount % mNumPerPage != 0)
            this.mTotalPage = (totalCount / mNumPerPage) + 1;
        else
            this.mTotalPage = totalCount / mNumPerPage;
    }


    public void sortByNameAscending() {
        for (List<ImportItem> restoreItems : mGroupItemList) {
            Collections.sort(restoreItems, new NameIgnoreCaseComparator());
        }
        Collections.sort(mSingleItemList, new NameIgnoreCaseComparator());
    }

    public void sortByNameDescending() {
        for (List<ImportItem> restoreItems : mGroupItemList) {
            Collections.sort(restoreItems, new NameIgnoreCaseComparator());
            Collections.reverse(restoreItems);
        }
        Collections.sort(mSingleItemList, new NameIgnoreCaseComparator());
        Collections.reverse(mSingleItemList);
    }

    public void sortByDateAscending() {
        for (List<ImportItem> restoreItems : mGroupItemList) {
            Collections.sort(restoreItems, new DateComparator());
        }
        Collections.sort(mSingleItemList, new DateComparator());
    }

    public void sortByDateDescending() {
        for (List<ImportItem> restoreItems : mGroupItemList) {
            Collections.sort(restoreItems, new DateComparator());
            Collections.reverse(restoreItems);
        }
        Collections.sort(mSingleItemList, new DateComparator());
        Collections.reverse(mSingleItemList);
    }

    public void sortBySizeAscending() {
        for (List<ImportItem> restoreItems : mGroupItemList) {
            Collections.sort(restoreItems, new SizeComparator());
        }
        Collections.sort(mSingleItemList, new SizeComparator());
    }

    public void sortBySizeDescending() {
        for (List<ImportItem> restoreItems : mGroupItemList) {
            Collections.sort(restoreItems, new SizeComparator());
            Collections.reverse(restoreItems);
        }
        Collections.sort(mSingleItemList, new SizeComparator());
        Collections.reverse(mSingleItemList);
    }

    private class NameIgnoreCaseComparator implements Comparator<RestoreItem> {
        public int compare(RestoreItem o1, RestoreItem o2) {
            return o1.getFileName().toLowerCase().compareTo(o2.getFileName().toLowerCase());
        }
    }

    private class DateComparator implements Comparator<RestoreItem> {
        public int compare(RestoreItem o1, RestoreItem o2) {
            return Long.valueOf(o1.getFileDate()).compareTo(Long.valueOf(o2.getFileDate()));
        }
    }

    private class SizeComparator implements Comparator<RestoreItem> {
        public int compare(RestoreItem o1, RestoreItem o2) {
            return Long.valueOf(o1.getFileSize()).compareTo(Long.valueOf(o2.getFileSize()));
        }
    }

}
