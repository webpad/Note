package ntx.note.tag;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

import ntx.note.data.TagManager;
import ntx.note.data.TagManager.Tag;
import ntx.note.data.TagManager.TagSet;

public class TagListAdapter extends BaseAdapter {
    private int mNumPerPage = 5;
    private List<Tag> mList = new ArrayList<>();
    private int mTotalPage = 1;
    private int mCurrentPage = 1;

    public TagListAdapter(TagSet tagSet, int numPerPage, boolean excludeQuickTag) {
        if (excludeQuickTag)
            this.mList = getTagListExcludeQuickTag(tagSet.allTags());
        else
            this.mList = tagSet.allTags();

        this.mNumPerPage = numPerPage;
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

    public void updateList(TagSet newTagSet, boolean excludeQuickTag){
        if (excludeQuickTag)
            this.mList = getTagListExcludeQuickTag(newTagSet.allTags());
        else
            this.mList = newTagSet.allTags();
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

    public List<Tag> getCurrentPageList() {
        List<Tag> currentList = new ArrayList<>();
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

    private List<Tag> getTagListExcludeQuickTag(List<Tag> inTagList) {
        List<Tag> outTagList = new ArrayList<>();
        for (Tag tag : inTagList) {
            if (tag.toString().equals(TagManager.QUICK_TAG_NAME))
                continue;
            outTagList.add(tag);
        }
        return outTagList;
    }
}
