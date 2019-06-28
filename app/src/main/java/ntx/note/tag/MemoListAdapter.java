package ntx.note.tag;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ntx.note.data.TagManager;
import ntx.note.data.TagManager.Tag;
import ntx.note.data.TagManager.TagSet;

public class MemoListAdapter extends BaseAdapter {
    private int mNumPerPage = 4;
    private List<Tag> mList = new LinkedList<>();
    private String mMainTagString = "";
    private int mTotalPage = 1;
    private int mCurrentPage = 1;

    public MemoListAdapter(String mainTag, TagSet tagSet, int numPerPage) {
        this.mMainTagString = mainTag;
        this.mList = getTagListExcludeQuickTag(tagSet);
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

    public void updateList(String mainTag, TagSet newTagSet) {
        this.mMainTagString = mainTag;
        this.mList = getTagListExcludeQuickTag(newTagSet);
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

    private List<Tag> getTagListExcludeQuickTag(TagSet tagSet) {
        List<Tag> outTagList = new LinkedList<>();
        Tag firstTag = tagSet.allTags().getFirst();
        for (Tag tag : tagSet.allTags()) {
            if (tag.toString().equals(TagManager.QUICK_TAG_NAME))
                continue;
            if (!tagSet.contains(tag))
                continue;
            if (tag.toString().equals(mMainTagString)) {
                firstTag = tag;
                continue;
            }
            outTagList.add(tag);
        }
        if (!mMainTagString.isEmpty())
            ((LinkedList<Tag>) outTagList).addFirst(firstTag);
        return outTagList;
    }
}
