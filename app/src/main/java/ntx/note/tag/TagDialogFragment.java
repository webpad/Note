package ntx.note.tag;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ntx.note.data.Book;
import ntx.note.data.Bookshelf;
import ntx.note.data.TagManager;
import ntx.note.data.TagManager.Tag;
import ntx.note.data.TagManager.TagSet;
import ntx.note2.R;
import utility.CustomDialogFragment;


public class TagDialogFragment extends CustomDialogFragment {

    private EditText mEtAddTag;
    private Button mBtnAddTag;

    private ImageButton mBtnTagSetting;
    private ImageButton mBtnTagSettingClose;

    private TextView mTvListEmptyHint;
    private TextView mTvCurrentPage;
    private TextView mTvTotalPage;
    private ImageButton mBtnTagListPageUp;
    private ImageButton mBtnTagListPageDown;
    private List<TagListItem> mTagListItemArray = new ArrayList<>();

    private TagManager mTagManager;
    private TagSet mCurrentPageTagSet;
    private TagListAdapter mTagListAdapter;

    private Book mCurrentBook;

    public static TagDialogFragment newInstance() {
        return new TagDialogFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mCurrentBook = Bookshelf.getInstance().getCurrentBook();
        View v = inflater.inflate(R.layout.tag_dialog, container, false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        mEtAddTag = (EditText) v.findViewById(R.id.et_add_tag);
        mEtAddTag.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if (mEtAddTag.getText().toString().equals("")) {
                        return true;
                    }
                    addNewTag();
                    return true;
                }

                return false;
            }
        });
        mEtAddTag.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                utility.StringIllegal.checkFirstSpaceChar(mEtAddTag);
                utility.StringIllegal.checkIllegalChar(mEtAddTag);

                if (s.toString().equals(" ")) {
                    mEtAddTag.setText("");
                    return;
                }
            }
        });
        mBtnAddTag = (Button) v.findViewById(R.id.btn_add_tag);
        mBtnAddTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEtAddTag.getText().toString().equals("")) {
                    return;
                }
                addNewTag();
                hideInputMethod();
            }
        });

        v.findViewById(R.id.btn_tag_close).setOnClickListener(onButtonClickListener);
        mBtnTagSetting = (ImageButton) v.findViewById(R.id.btn_tag_setting);
        mBtnTagSettingClose = (ImageButton) v.findViewById(R.id.btn_tag_setting_close);
        mBtnTagSetting.setOnClickListener(onButtonClickListener);
        mBtnTagSettingClose.setOnClickListener(onButtonClickListener);

        mTvListEmptyHint = (TextView) v.findViewById(R.id.tv_list_empty_hint);
        mTvCurrentPage = (TextView) v.findViewById(R.id.tv_page_index);
        mTvTotalPage = (TextView) v.findViewById(R.id.tv_page_total);
        mBtnTagListPageUp = (ImageButton) v.findViewById(R.id.btn_tag_list_page_up);
        mBtnTagListPageDown = (ImageButton) v.findViewById(R.id.btn_tag_list_page_down);
        mBtnTagListPageUp.setOnClickListener(onPageBtnClickListener);
        mBtnTagListPageDown.setOnClickListener(onPageBtnClickListener);

        TagListItem tagListItem1 = (TagListItem) v.findViewById(R.id.tag_list_item_1);
        TagListItem tagListItem2 = (TagListItem) v.findViewById(R.id.tag_list_item_2);
        TagListItem tagListItem3 = (TagListItem) v.findViewById(R.id.tag_list_item_3);
        TagListItem tagListItem4 = (TagListItem) v.findViewById(R.id.tag_list_item_4);
        TagListItem tagListItem5 = (TagListItem) v.findViewById(R.id.tag_list_item_5);
        mTagListItemArray.clear();
        mTagListItemArray.add(tagListItem1);
        mTagListItemArray.add(tagListItem2);
        mTagListItemArray.add(tagListItem3);
        mTagListItemArray.add(tagListItem4);
        mTagListItemArray.add(tagListItem5);
        for (TagListItem tagListItem : mTagListItemArray) {
            tagListItem.setOnListItemButtonClickListener(itemButtonClickListener);
        }
        initTagList();
    }

    private void initTagList() {
        mTagManager = mCurrentBook.getTagManager();
        mTagManager.sort();
        mCurrentPageTagSet = mCurrentBook.currentPage().tags;
        mTagListAdapter = new TagListAdapter(mCurrentPageTagSet, 5, true);
        mTagListAdapter.setCurrentPage(1);
        updateTagListView();
    }

    private void updateTagListView() {
        int currentPage = mTagListAdapter.getCurrentPage();
        int totalPage = mTagListAdapter.getTotalPage();
        mTvCurrentPage.setText(String.valueOf(currentPage));
        mTvTotalPage.setText(String.valueOf(totalPage));
        List<Tag> currentTagList = mTagListAdapter.getCurrentPageList();

        if (currentTagList.isEmpty() && currentPage == 1 && totalPage == 1) {
            mTvListEmptyHint.setVisibility(View.VISIBLE);
            mBtnTagListPageUp.setEnabled(false);
            mBtnTagListPageDown.setEnabled(false);
        } else {
            mTvListEmptyHint.setVisibility(View.GONE);
            mBtnTagListPageUp.setEnabled(true);
            mBtnTagListPageDown.setEnabled(true);
        }

        for (TagListItem tagListItem : mTagListItemArray) {
            tagListItem.setVisibility(View.INVISIBLE);
            tagListItem.setTag(null);
        }

        for (int i = 0; i < currentTagList.size(); i++) {
            mTagListItemArray.get(i).updateTagName(currentTagList.get(i).toString());
            mTagListItemArray.get(i).setTag(currentTagList.get(i));
            mTagListItemArray.get(i).setTagCheck(mCurrentPageTagSet.contains(currentTagList.get(i)));
            mTagListItemArray.get(i).setVisibility(View.VISIBLE);
        }
    }

    private void addNewTag() {
        Tag newTag = mTagManager.newTag(mEtAddTag.getText().toString());
        mCurrentPageTagSet.add(newTag);
        mTagManager.sort();
        mTagListAdapter.updateList(mCurrentPageTagSet, true);
        mTagListAdapter.notifyDataSetChanged();
        updateTagListView();
        mEtAddTag.setText("");

        mCurrentBook.currentPage().touch();
    }

    private ImageButton.OnClickListener onPageBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int currentPage = mTagListAdapter.getCurrentPage();
            switch (view.getId()) {
                case R.id.btn_tag_list_page_up:
                    currentPage--;
                    break;
                case R.id.btn_tag_list_page_down:
                    currentPage++;
                    break;
                default:
                    break;
            }
            mTagListAdapter.setCurrentPage(currentPage);
            updateTagListView();
        }
    };

    private TagListItem.ItemButtonClickListener itemButtonClickListener = new TagListItem.ItemButtonClickListener() {
        @Override
        public void onEditBtnClick(int index) {
            for (TagListItem tagListItem : mTagListItemArray) {
                tagListItem.updateListItemView(TagListItem.TagListMode.LOCK);
            }
            mTagListItemArray.get(index - 1).updateListItemView(TagListItem.TagListMode.EDITING);
        }

        @Override
        public void onDeleteBtnClick(Object tag) {
            if (tag == null)
                return;

            mCurrentPageTagSet.remove((Tag) tag);
            ((Tag) tag).rename("");
            mTagListAdapter.updateList(mCurrentPageTagSet, true);
            mTagListAdapter.notifyDataSetChanged();
            updateTagListView();

            mCurrentBook.currentPage().touch();
        }

        @Override
        public void onEditingCancelBtnClick() {
            for (TagListItem tagListItem : mTagListItemArray) {
                tagListItem.updateListItemView(TagListItem.TagListMode.SETTING);
            }
        }

        @Override
        public void onEditingOkBtnClick(Object tag, String newName) {
            if (tag == null)
                return;

            ((Tag) tag).rename(newName);
            for (TagListItem tagListItem : mTagListItemArray) {
                tagListItem.updateListItemView(TagListItem.TagListMode.SETTING);
            }
            mTagListAdapter.updateList(mCurrentPageTagSet, true);
            mTagListAdapter.notifyDataSetChanged();
            updateTagListView();

            mCurrentBook.currentPage().touch();
        }

        @Override
        public void onCheckedChange(Object tag, boolean isCheck) {
            if (tag == null)
                return;

            if (isCheck) {
                mCurrentPageTagSet.add((Tag) tag);
            } else {
                mCurrentPageTagSet.remove((Tag) tag);
            }

            mTagListAdapter.updateList(mCurrentPageTagSet, true);
            mTagListAdapter.notifyDataSetChanged();

            mCurrentBook.currentPage().touch();
        }
    };

    private View.OnClickListener onButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int tagListMode = 0;
            switch (view.getId()) {
                case R.id.btn_tag_setting:
                    tagListMode = TagListItem.TagListMode.SETTING;
                    break;
                case R.id.btn_tag_setting_close:
                    tagListMode = TagListItem.TagListMode.SELECTION;
                    break;
                case R.id.btn_tag_close:
                    tagListMode = TagListItem.TagListMode.SELECTION;
                    dismiss();
                    break;
                default:
                    break;
            }

            if (tagListMode == TagListItem.TagListMode.SETTING) {
                mBtnTagSetting.setVisibility(View.GONE);
                mBtnTagSettingClose.setVisibility(View.VISIBLE);
                mEtAddTag.setEnabled(false);
                mBtnAddTag.setTextColor(Color.GRAY);
                mBtnAddTag.setEnabled(false);
            } else {
                mBtnTagSetting.setVisibility(View.VISIBLE);
                mBtnTagSettingClose.setVisibility(View.GONE);
                mEtAddTag.setEnabled(true);
                mBtnAddTag.setTextColor(Color.BLACK);
                mBtnAddTag.setEnabled(true);
            }

            for (TagListItem itemView : mTagListItemArray) {
                itemView.updateListItemView(tagListMode);
            }
        }
    };

    private void hideInputMethod() {
        try {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mEtAddTag.getWindowToken(), 0);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        hideInputMethod();
        mEtAddTag.setText("");
    }
}
