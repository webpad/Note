package ntx.note.bookshelf;

import android.text.format.Time;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ntx.note.data.Book;


public class FreeStorageListAdapter extends BaseAdapter {
    private List<Book> mList = new ArrayList<>();
    private Map<UUID, Boolean> mIsBookSelectedHashMap = new HashMap<UUID, Boolean>();
    private int mNumPerPage;
    private int mCurrentPage = 1;
    private int mTotalPage = 1;

    public FreeStorageListAdapter(List<Book> list, int numPerPage) {
        this.mNumPerPage = numPerPage;
        updateList(list);
    }

    @Override
    public int getCount() {
        return mList.size();
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        return null;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        setTotalPage();

        if (mCurrentPage > mTotalPage)
            mCurrentPage = mTotalPage;
    }

    public void updateList(List<Book> newList) {
        this.mList.clear();
        this.mList.addAll(newList);
        this.mIsBookSelectedHashMap.clear();
        for (Book book : mList) {
            mIsBookSelectedHashMap.put(book.getUUID(), false);
        }
        notifyDataSetChanged();
    }

    public void setNumPerPage(int numPerPage) {
        this.mNumPerPage = numPerPage;
    }

    public void setCurrentPage(int currentPage) {
        if (currentPage > mTotalPage) { // only one page
            currentPage = 1;
        } else if (currentPage <= 0) {
            currentPage = mTotalPage;
        }
        this.mCurrentPage = currentPage;
    }

    public int getCurrentPage() {
        return this.mCurrentPage;
    }

    public int getTotalPage() {
        return this.mTotalPage;
    }

    public void setBookSelected(UUID uuid, boolean isSelected) {
        mIsBookSelectedHashMap.put(uuid, isSelected);
    }

    public void setListSelected(boolean isSelect) {
        mIsBookSelectedHashMap.clear();
        for (Book book : mList) {
            mIsBookSelectedHashMap.put(book.getUUID(), isSelect);
        }
    }

    public boolean isBookSelected(UUID uuid) {
        Boolean isSelected = mIsBookSelectedHashMap.get(uuid);
        if (isSelected == null)
            return false;

        return isSelected;
    }

    public List<Book> getCurrentPageList() {
        List<Book> currentPageList = new ArrayList<>();
        currentPageList.clear();
        for (int i = 0; i < mNumPerPage; i++) {
            if (getRealPosition(i) >= mList.size())
                break;
            currentPageList.add(mList.get(getRealPosition(i)));
        }
        return currentPageList;
    }

    public LinkedList<Book> getSelectedBookList() {
        LinkedList<Book> selectedList = new LinkedList<>();
        selectedList.clear();
        for (Book book : mList) {
            if (isBookSelected(book.getUUID())) {
                selectedList.add(book);
            }
        }

        return selectedList;
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

    public void sortByNameAscending() {
        Collections.sort(mList, new FreeStorageListAdapter.NameIgnoreCaseComparator());
    }

    public void sortByNameDescending() {
        Collections.sort(mList, new FreeStorageListAdapter.NameIgnoreCaseComparator());
        Collections.reverse(mList);
    }

    public void sortByDateAscending() {
        Collections.sort(mList, new FreeStorageListAdapter.DateComparator());
    }

    public void sortByDateDescending() {
        Collections.sort(mList, new FreeStorageListAdapter.DateComparator());
        Collections.reverse(mList);
    }

    public void sortBySizeAscending() {
        Collections.sort(mList, new FreeStorageListAdapter.SizeComparator());
    }

    public void sortBySizeDescending() {
        Collections.sort(mList, new FreeStorageListAdapter.SizeComparator());
        Collections.reverse(mList);
    }

    private class NameIgnoreCaseComparator implements Comparator<Book> {
        public int compare(Book o1, Book o2) {
            return o1.getTitle().toLowerCase().compareTo(o2.getTitle().toLowerCase());
        }
    }

    private class DateComparator implements Comparator<Book> {
        public int compare(Book o1, Book o2) {
            return Time.compare(o1.getMtime(), o2.getMtime());
        }
    }

    private class SizeComparator implements Comparator<Book> {
        public int compare(Book o1, Book o2) {
            return Long.compare(o1.getBookSizeInStorage(), o2.getBookSizeInStorage());
        }
    }

}
