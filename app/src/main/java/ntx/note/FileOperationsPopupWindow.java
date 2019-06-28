package ntx.note;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import name.vbraun.view.write.Page;
import ntx.note.data.Book;
import ntx.note.data.Bookshelf;
import ntx.note.data.TagManager;
import ntx.note.tag.MemoListAdapter;
import ntx.note.tag.MemoListItem;
import ntx.note2.R;

public class FileOperationsPopupWindow extends RelativePopupWindow {
    private static FileOperationsPopupWindow mInstance;

    private Activity mContext;

    private EditText mEtAddMemo;

    private LinearLayout mLayoutMemoList;
    private ImageButton mBtnMemoPageDown;
    private List<MemoListItem> mMemoListItemArray = new ArrayList<>();

    private Book mCurrentBook;
    private TagManager mTagManager;
    private TagManager.TagSet mCurrentPageTagSet;
    private MemoListAdapter mMemoListAdapter;

    public static FileOperationsPopupWindow getInstance(Activity ctx, int width) {
        synchronized (FileOperationsPopupWindow.class) {
            if (mInstance == null) {
                mInstance = new FileOperationsPopupWindow(ctx, width);
            }
            return mInstance;
        }
    }

    private FileOperationsPopupWindow(Activity ctx, int width) {
        super(ctx);
        mContext = ctx;
        setContentView(LayoutInflater.from(ctx).inflate(R.layout.popupwindow_file_operations, null));
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setWidth(width);
        setOutsideTouchable(false);
        setFocusable(true);
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View popupView = getContentView();

        initView(popupView);
    }

    @Override
    public void dismiss() {
        mEtAddMemo.setText("");
        mEtAddMemo.setSelectAllOnFocus(false);
        mEtAddMemo.clearFocus();
        uninitTagList();
        super.dismiss();
    }

    @Override
    public void showOnAnchor(View anchor, int horizPos, int vertPos) {
        initTagList();
        super.showOnAnchor(anchor, horizPos, vertPos);
    }

    @Override
    public void showOnAnchor(View anchor, int horizPos, int vertPos, int offsetX, int offsetY) {
        initTagList();
        super.showOnAnchor(anchor, horizPos, vertPos, offsetX, offsetY);
    }

    @Override
    public void showOnAnchor(View anchor, int horizPos, int vertPos, int padL, int padT, int padR, int padB) {
        initTagList();
        super.showOnAnchor(anchor, horizPos, vertPos, padL, padT, padR, padB);
    }

    private void initView(View v) {
        Button btnFileRename = (Button) v.findViewById(R.id.btn_file_rename);
        Button btnFileCopyTo = (Button) v.findViewById(R.id.btn_file_copy_to);
        Button btnFileSave = (Button) v.findViewById(R.id.btn_file_save);
        Button btnFileConvertTo = (Button) v.findViewById(R.id.btn_file_convert_to);
        Button btnFileBackup = (Button) v.findViewById(R.id.btn_file_backup);
        Button btnFileRestore = (Button) v.findViewById(R.id.btn_file_restore);
        Button btnFileInfo = (Button) v.findViewById(R.id.btn_file_info);

        btnFileRename.setOnClickListener(onBtnClickListener);
        btnFileCopyTo.setOnClickListener(onBtnClickListener);
        btnFileSave.setOnClickListener(onBtnClickListener);
        btnFileConvertTo.setOnClickListener(onBtnClickListener);
        btnFileBackup.setOnClickListener(onBtnClickListener);
        btnFileRestore.setOnClickListener(onBtnClickListener);
        btnFileInfo.setOnClickListener(onBtnClickListener);


        mEtAddMemo = (EditText) v.findViewById(R.id.et_add_memo);
        mEtAddMemo.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if (mEtAddMemo.getText().toString().equals("")) {
                        return true;
                    }
                    addNewTag(mEtAddMemo.getText().toString());
                    return true;
                }

                return false;
            }
        });
        mEtAddMemo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                utility.StringIllegal.checkFirstSpaceChar(mEtAddMemo);
                utility.StringIllegal.checkIllegalChar(mEtAddMemo);

                if (s.toString().equals(" ")) {
                    mEtAddMemo.setText("");
                }
            }
        });

        Button btnAddMemo = (Button) v.findViewById(R.id.btn_add_memo);
        btnAddMemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEtAddMemo.getText().toString().equals("")) {
                    return;
                }
                addNewTag(mEtAddMemo.getText().toString());
                hideInputMethod();
            }
        });

        mLayoutMemoList = (LinearLayout) v.findViewById(R.id.layout_memo_list);
        mBtnMemoPageDown = (ImageButton) v.findViewById(R.id.btn_memo_page_down);
        mBtnMemoPageDown.setOnClickListener(onBtnClickListener);

        MemoListItem memoListItem1 = (MemoListItem) v.findViewById(R.id.memo_list_item_1);
        MemoListItem memoListItem2 = (MemoListItem) v.findViewById(R.id.memo_list_item_2);
        MemoListItem memoListItem3 = (MemoListItem) v.findViewById(R.id.memo_list_item_3);
        MemoListItem memoListItem4 = (MemoListItem) v.findViewById(R.id.memo_list_item_4);
        mMemoListItemArray.clear();
        mMemoListItemArray.add(memoListItem1);
        mMemoListItemArray.add(memoListItem2);
        mMemoListItemArray.add(memoListItem3);
        mMemoListItemArray.add(memoListItem4);
        for (MemoListItem memoListItem : mMemoListItemArray) {
            memoListItem.registerItemClickListener(memoItemClickListener);
        }
        initTagList();
    }

    private View.OnClickListener onBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_file_rename:
                    callEvent(CallbackEvent.RENAME_NOTE);
                    break;
                case R.id.btn_file_copy_to:
                    callEvent(CallbackEvent.COPY_CURRENT_PAGE_TO);
                    break;
                case R.id.btn_file_save:
                    callEvent(CallbackEvent.SAVE_NOTE);
                    break;
                case R.id.btn_file_convert_to:
                    callEvent(CallbackEvent.CONVERT_NOTE);
                    break;
                case R.id.btn_file_backup:
                    callEvent(CallbackEvent.BACKUP_NOTE);
                    break;
                case R.id.btn_file_restore:
                    callEvent(CallbackEvent.RESTORE_NOTE);
                    break;
                case R.id.btn_file_info:
                    callEvent(CallbackEvent.INFO_NOTE);
                    break;
                case R.id.btn_memo_page_down:
                    mMemoListAdapter.setCurrentPage(mMemoListAdapter.getCurrentPage() + 1);
                    updateMemoListView();
                    return;
            }
            dismiss();
        }
    };

    private MemoListItem.ItemClickListener memoItemClickListener = new MemoListItem.ItemClickListener() {
        @Override
        public void onItemClick(Object tag) {
            mCurrentBook.currentPage().setMainTag(((TagManager.Tag) tag).toString());
            uninitTagList();
            initTagList();
        }

        @Override
        public void onItemEditClick(int index) {
            for (int i = 0; i < mMemoListItemArray.size(); i++) {
                if (index == i + 1)
                    mMemoListItemArray.get(i).updateListItemView(MemoListItem.MemoListMode.EDITING);
                else
                    mMemoListItemArray.get(i).updateListItemView(MemoListItem.MemoListMode.LOCK);
            }
            showInputMethod();
        }

        @Override
        public void onItemEditCancelClick() {
            for (MemoListItem memoListItem : mMemoListItemArray) {
                memoListItem.updateListItemView(MemoListItem.MemoListMode.VIEW);
            }
            hideInputMethod();
        }

        @Override
        public void onItemEditOkClick(Object oldTag, String newTagString) {
            if (((TagManager.Tag) oldTag).toString().equals(mCurrentBook.currentPage().getMainTag()))
                mCurrentBook.currentPage().setMainTag("");
            mCurrentPageTagSet.remove((TagManager.Tag) oldTag);
            refineTagSet((TagManager.Tag) oldTag);
            addNewTag(newTagString);
            uninitTagList();
            initTagList();
            hideInputMethod();
        }

        @Override
        public void onItemDeleteClick(Object tag) {
            if (((TagManager.Tag) tag).toString().equals(mCurrentBook.currentPage().getMainTag())) {
                if (mMemoListAdapter.getCount() == 1) // Last one memo
                    mCurrentBook.currentPage().setMainTag("");
                else
                    mCurrentBook.currentPage().setMainTag(((TagManager.Tag) mMemoListAdapter.getItem(1)).toString());
            }
            mCurrentPageTagSet.remove((TagManager.Tag) tag);
            refineTagSet((TagManager.Tag) tag);
            uninitTagList();
            initTagList();
        }
    };

    private void initTagList() {
        mCurrentBook = Bookshelf.getInstance().getCurrentBook();
        mTagManager = mCurrentBook.getTagManager();
        mTagManager.sort();
        mCurrentPageTagSet = mCurrentBook.currentPage().tags;
        mMemoListAdapter = new MemoListAdapter(mCurrentBook.currentPage().getMainTag(), mCurrentPageTagSet, 4);
        mMemoListAdapter.setCurrentPage(1);
        updateMemoListView();
    }

    private void uninitTagList() {
        mCurrentBook = null;
        mTagManager = null;
        mCurrentPageTagSet = null;
        mMemoListAdapter = null;
    }

    private void addNewTag(String newTagString) {
        TagManager.Tag newTag = mTagManager.newTag(newTagString);
        if (mCurrentBook.currentPage().getMainTag().isEmpty()) {
            mCurrentBook.currentPage().setMainTag(newTag.toString());
        }
        mCurrentPageTagSet.add(newTag);
        mTagManager.sort();
        mMemoListAdapter.updateList(mCurrentBook.currentPage().getMainTag(), mCurrentPageTagSet);
        mMemoListAdapter.notifyDataSetChanged();
        updateMemoListView();
        mEtAddMemo.setText("");

        mCurrentBook.currentPage().touch();
    }

    private void updateMemoListView() {
        int currentPage = mMemoListAdapter.getCurrentPage();
        int totalPage = mMemoListAdapter.getTotalPage();
        List<TagManager.Tag> currentTagList = mMemoListAdapter.getCurrentPageList();

        if (currentTagList.isEmpty() && currentPage == 1 && totalPage == 1)
            mLayoutMemoList.setVisibility(View.GONE);
        else
            mLayoutMemoList.setVisibility(View.VISIBLE);


        for (MemoListItem memoListItem : mMemoListItemArray) {
            memoListItem.setVisibility(View.GONE);
            memoListItem.setTag(null);
        }

        for (int i = 0; i < currentTagList.size(); i++) {
            String tagString = currentTagList.get(i).toString();
            mMemoListItemArray.get(i).updateTagName(tagString);
            mMemoListItemArray.get(i).showFlag(mCurrentBook.currentPage().getMainTag().equals(tagString));
            mMemoListItemArray.get(i).updateListItemView(MemoListItem.MemoListMode.VIEW);
            mMemoListItemArray.get(i).setTag(currentTagList.get(i));
            mMemoListItemArray.get(i).setVisibility(View.VISIBLE);
        }

        if (totalPage > 1) {
            mBtnMemoPageDown.setVisibility(View.VISIBLE);
            for (MemoListItem memoListItem : mMemoListItemArray) {
                if (memoListItem.getVisibility() == View.GONE)
                    memoListItem.setVisibility(View.INVISIBLE);
            }
        } else {
            mBtnMemoPageDown.setVisibility(View.GONE);
        }
    }

    private void refineTagSet(TagManager.Tag tag) {
        for (Page page : mCurrentBook.getPages()) {
            if (page.tags.contains(tag))
                return;
        }
        tag.rename("");
    }

    private void showInputMethod() {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(mContext.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private void hideInputMethod() {
        try {
            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(mContext.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mEtAddMemo.getWindowToken(), 0);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void callEvent(String event) {
        CallbackEvent callbackEvent = new CallbackEvent();
        callbackEvent.setMessage(event);
        EventBus.getDefault().post(callbackEvent);
    }
}
