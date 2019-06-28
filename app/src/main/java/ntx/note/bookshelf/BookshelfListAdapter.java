package ntx.note.bookshelf;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Checkable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ntx.note.data.Book;

public class BookshelfListAdapter extends BaseAdapter {
    private int mNumPerPage = 6;
    private List<CheckableBook> mList = new ArrayList<>();
    private int mTotalPage = 1;
    private int mCurrentPage = 1;


    public BookshelfListAdapter(List<Book> list) {
        for (Book book : list) {
            this.mList.add(new CheckableBook(book.getUUID()));
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
    }

    public Object getSelectedItem() {
        for (CheckableBook checkableBook : mList) {
            if (checkableBook.isChecked())
                return checkableBook;
        }
        return null;
    }

    public void setListItemCheck(int position) {
        for (CheckableBook checkableBook : mList) {
            checkableBook.setChecked(false);
        }
        mList.get(getRealPosition(position)).setChecked(true);
    }

    public void updateList(List<Book> newList) {
        this.mList.clear();
        for (Book book : newList) {
            this.mList.add(new CheckableBook(book.getUUID()));
        }
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

    public List<CheckableBook> getCurrentPageList() {
        List<CheckableBook> currentList = new ArrayList<>();
        currentList.clear();
        for (int i = 0; i < mNumPerPage; i++) {
            if (getRealPosition(i) >= mList.size())
                break;
            currentList.add(mList.get(getRealPosition(i)));
        }

        return currentList;
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

    public class CheckableBook extends Book implements Checkable {
        private boolean isChecked;

        CheckableBook(UUID uuid) {
            super(uuid, false);
            this.isChecked = false;
        }

        @Override
        public void toggle() {
            setChecked(!isChecked);
        }

        @Override
        public void setChecked(boolean checked) {
            isChecked = checked;
        }

        @Override
        public boolean isChecked() {
            return isChecked;
        }
    }
}
