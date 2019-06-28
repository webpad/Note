package ntx.note.thumbnail;

import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import name.vbraun.view.write.Page;
import ntx.note.NoteWriterActivity;
import ntx.note.UndoManager;
import ntx.note.data.Book;
import ntx.note.data.Bookshelf;
import ntx.note.data.TagManager;
import ntx.note.data.TagManager.Tag;
import ntx.note.export.AlertDialogButtonClickListener;
import ntx.note.export.AlertDialogFragment;
import ntx.note2.R;
import utility.CustomDialogFragment;


public class ThumbnailDialogFragment extends CustomDialogFragment {
    private final static String ARGUMENT_SAVE_CURRENT_FIRST = "save_current_first";

    public @interface ThumbnailListStyle {
        int NORMAL_MODE = 1;
        int INSERT_MODE = 2;
    }

    private int mThumbnailListStyle = ThumbnailListStyle.NORMAL_MODE;

    private @interface TopLayoutMode {
        int SELECTION_MODE = 0;
        int CHECK_MODE = 1;
        int PASTE_MODE = 2;
        int MOVE_MODE = 3;
    }

    private int mTopLayoutMode = TopLayoutMode.SELECTION_MODE;

    private RelativeLayout mLayoutTopSelectionMode;
    private RelativeLayout mLayoutTopCheckMode;
    private RelativeLayout mLayoutTopPasteOrMoveMode;
    private TextView mTvTopLayoutTitle;
    private TextView mTvSelectedCounter;
    private ImageButton mBtnThumbnailManage;
    private LinearLayout mBtnThumbnailDelete;
    private LinearLayout mBtnThumbnailCopy;
    private LinearLayout mBtnThumbnailMove;
    private LinearLayout mLayoutTagList;
    private LinearLayout alert_dialog_container;
    private RelativeLayout mLayoutSelectedPageOperation;
    /*
    private TextView mTvTagListCurrentPage;
    private TextView mTvTagListTotalPage;
    private ImageButton mBtnTagListPageUp;
    private ImageButton mBtnTagListPageDown;
    private List<ThumbnailTagItem> mThumbnailTagItemList = new ArrayList<>();
    */
    private ThumbnailTagItem mStarTagItem;
    private EditText mEtSearchTag;
    private ImageButton mBtnSearchTagEnter;
    private TextView mTvSearchNotFoundHint;
    private LinearLayout mLayoutThumbnailListRow1;
    private LinearLayout mLayoutThumbnailListRow2;


    private TextView mTvThumbnailListCurrentPage;
    private TextView mTvThumbnailListTotalPage;
    private List<Page> mCurrentPageList;
    private List<ThumbnailItem> mThumbnailItemList = new ArrayList<>();
    private List<ImageButton> mInsertLocationList = new ArrayList<>();

    /*
    private TagListAdapter mTagListAdapter;
    */

    private ThumbnailListAdapter mThumbnailListAdapter;

    private Book mCurrentBook;
    private TagManager mCurrentBookTagManager;
    private boolean mIsSaveCurrentFirst = false;
    private Handler mHandler = new Handler();
    private Runnable saveCurrentNoteBookRunnable = new Runnable() {
        @Override
        public void run() {
            Bookshelf.getInstance().getCurrentBook().save();
            NoteWriterActivity.setNoteEdited(false);
            updateThumbnailListView(mThumbnailListStyle);
        }
    };

    public static ThumbnailDialogFragment newInstance(boolean isSaveCurrentFirst) {
        ThumbnailDialogFragment fragment = new ThumbnailDialogFragment();

        Bundle args = new Bundle();

        args.putBoolean(ARGUMENT_SAVE_CURRENT_FIRST, isSaveCurrentFirst);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCurrentBook = Bookshelf.getInstance().getCurrentBook();
        mCurrentBookTagManager = mCurrentBook.getTagManager();
        mThumbnailListAdapter = new ThumbnailListAdapter(mCurrentBook.getFilteredPages(), 6);
        int pageIndex = mThumbnailListAdapter.getPageIndexOfTheSpecificPageContent(mCurrentBook.currentPage());
        mThumbnailListAdapter.setCurrentPage(pageIndex);

        mIsSaveCurrentFirst = getArguments().getBoolean(ARGUMENT_SAVE_CURRENT_FIRST);
        if (mIsSaveCurrentFirst)
            mHandler.post(saveCurrentNoteBookRunnable);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.book_thumbnail_dialog, container, false);
        initView(v);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mIsSaveCurrentFirst)
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < mCurrentPageList.size(); j++) {
                        Page page = mCurrentPageList.get(j);
                        mThumbnailItemList.get(j).setTag(page);
                        mThumbnailItemList.get(j).updateIndex(getPageIndex(page));
                        mThumbnailItemList.get(j).updateCheckBox(mThumbnailListAdapter.isPageSelected(page.getUUID()));
                        mThumbnailItemList.get(j).updatePreview();
                    }
                }
            }, 100);
    }

    private void initView(View v) {
        mLayoutTopSelectionMode = (RelativeLayout) v.findViewById(R.id.layout_top_selection_mode);
        mLayoutTopCheckMode = (RelativeLayout) v.findViewById(R.id.layout_top_check_mode);
        mLayoutTopPasteOrMoveMode = (RelativeLayout) v.findViewById(R.id.layout_top_paste_or_move_mode);
        mTvTopLayoutTitle = (TextView) v.findViewById(R.id.tv_top_layout_mode_title);

        mTvSelectedCounter = (TextView) v.findViewById(R.id.tv_selected_counter);

        mLayoutTagList = (LinearLayout) v.findViewById(R.id.layout_tag_list);
        alert_dialog_container = (LinearLayout) v.findViewById(R.id.alert_dialog_container);
        mLayoutSelectedPageOperation = (RelativeLayout) v.findViewById(R.id.layout_selected_page_operation);

        /*
        mTvTagListCurrentPage = (TextView) v.findViewById(R.id.tv_tag_list_current_page);
        mTvTagListTotalPage = (TextView) v.findViewById(R.id.tv_tag_list_total_page);
        mBtnTagListPageUp = (ImageButton) v.findViewById(R.id.btn_tag_list_page_up);
        mBtnTagListPageDown = (ImageButton) v.findViewById(R.id.btn_tag_list_page_down);
        mBtnTagListPageUp.setOnClickListener(onPageBtnClickListener);
        mBtnTagListPageDown.setOnClickListener(onPageBtnClickListener);

        ThumbnailTagItem tagItem1 = (ThumbnailTagItem) v.findViewById(R.id.tag_list_item_1);
        ThumbnailTagItem tagItem2 = (ThumbnailTagItem) v.findViewById(R.id.tag_list_item_2);
        ThumbnailTagItem tagItem3 = (ThumbnailTagItem) v.findViewById(R.id.tag_list_item_3);
        ThumbnailTagItem tagItem4 = (ThumbnailTagItem) v.findViewById(R.id.tag_list_item_4);
        ThumbnailTagItem tagItem5 = (ThumbnailTagItem) v.findViewById(R.id.tag_list_item_5);
        ThumbnailTagItem tagItem6 = (ThumbnailTagItem) v.findViewById(R.id.tag_list_item_6);
        ThumbnailTagItem tagItem7 = (ThumbnailTagItem) v.findViewById(R.id.tag_list_item_7);
        ThumbnailTagItem tagItem8 = (ThumbnailTagItem) v.findViewById(R.id.tag_list_item_8);
        mThumbnailTagItemList.add(tagItem1);
        mThumbnailTagItemList.add(tagItem2);
        mThumbnailTagItemList.add(tagItem3);
        mThumbnailTagItemList.add(tagItem4);
        mThumbnailTagItemList.add(tagItem5);
        mThumbnailTagItemList.add(tagItem6);
        mThumbnailTagItemList.add(tagItem7);
        mThumbnailTagItemList.add(tagItem8);
        for (ThumbnailTagItem thumbnailTagItem : mThumbnailTagItemList) {
            thumbnailTagItem.setOnTagCheckedChangeListener(tagCheckChangedListener);
        }
        */
        mStarTagItem = (ThumbnailTagItem) v.findViewById(R.id.tag_list_star_item);
        mStarTagItem.setOnTagCheckedChangeListener(tagCheckChangedListener);
        mEtSearchTag = (EditText) v.findViewById(R.id.et_search_tag_input);
        mEtSearchTag.setOnKeyListener(onSearchEditTextKeyListener);
        mEtSearchTag.addTextChangedListener(searchEditTextWatcher);
        mEtSearchTag.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b)
                    showInputMethod();
                else
                    hideInputMethod(mEtSearchTag);
            }
        });
        ImageButton btnSearchTagResultClear = (ImageButton) v.findViewById(R.id.btn_search_tag_result_clear);
        mBtnSearchTagEnter = (ImageButton) v.findViewById(R.id.btn_search_tag_enter);
        mBtnSearchTagEnter.setAlpha(0.2f);
        mBtnSearchTagEnter.setEnabled(false);
        btnSearchTagResultClear.setOnClickListener(onSearchTagBtnClickListener);
        mBtnSearchTagEnter.setOnClickListener(onSearchTagBtnClickListener);
        mTvSearchNotFoundHint = (TextView) v.findViewById(R.id.tv_search_result_not_find_hint);
        mLayoutThumbnailListRow1 = (LinearLayout) v.findViewById(R.id.layout_thumbnail_list_row1);
        mLayoutThumbnailListRow2 = (LinearLayout) v.findViewById(R.id.layout_thumbnail_list_row2);

        ImageButton btnThumbnailListPageUp = (ImageButton) v.findViewById(R.id.btn_thumbnail_list_page_up);
        ImageButton btnThumbnailListPageDown = (ImageButton) v.findViewById(R.id.btn_thumbnail_list_page_down);
        btnThumbnailListPageUp.setOnClickListener(onPageBtnClickListener);
        btnThumbnailListPageDown.setOnClickListener(onPageBtnClickListener);
        mTvThumbnailListCurrentPage = (TextView) v.findViewById(R.id.tv_thumbnail_list_current_page);
        mTvThumbnailListTotalPage = (TextView) v.findViewById(R.id.tv_thumbnail_list_total_page);

        ImageButton insertLocation0 = (ImageButton) v.findViewById(R.id.ib_insert_location_0);
        ImageButton insertLocation1 = (ImageButton) v.findViewById(R.id.ib_insert_location_1);
        ImageButton insertLocation2 = (ImageButton) v.findViewById(R.id.ib_insert_location_2);
        ImageButton insertLocation3 = (ImageButton) v.findViewById(R.id.ib_insert_location_3);
        ImageButton insertLocation4 = (ImageButton) v.findViewById(R.id.ib_insert_location_4);
        ImageButton insertLocation5 = (ImageButton) v.findViewById(R.id.ib_insert_location_5);
        ImageButton insertLocation6 = (ImageButton) v.findViewById(R.id.ib_insert_location_6);
        ImageButton insertLocation7 = (ImageButton) v.findViewById(R.id.ib_insert_location_7);
        mInsertLocationList.add(insertLocation0);
        mInsertLocationList.add(insertLocation1);
        mInsertLocationList.add(insertLocation2);
        mInsertLocationList.add(insertLocation3);
        mInsertLocationList.add(insertLocation4);
        mInsertLocationList.add(insertLocation5);
        mInsertLocationList.add(insertLocation6);
        mInsertLocationList.add(insertLocation7);
        for (int i = 0; i < mInsertLocationList.size(); i++) {
            mInsertLocationList.get(i).setTag(i);
            mInsertLocationList.get(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int locationBtn = (int) v.getTag();
                    List<Page> currentPageList = mThumbnailListAdapter.getCurrentPageList();
                    int insertPosition;
                    int referencePageIndex;
                    if (locationBtn < 3) {
                        referencePageIndex = locationBtn;
                    } else if (locationBtn == 3) {
                        referencePageIndex = 2;
                    } else if (locationBtn == 7) {
                        referencePageIndex = 5;
                    } else {
                        referencePageIndex = locationBtn - 1;
                    }

                    Page referencePage;
                    if (referencePageIndex >= currentPageList.size()) { // prevent out of range
                        referencePage = currentPageList.get(currentPageList.size() - 1); // get last page
                        referencePageIndex = -1; // last location
                    } else
                        referencePage = currentPageList.get(referencePageIndex);

                    if (referencePageIndex >= 0) {
                        if (locationBtn == 3 || locationBtn == 7)
                            insertPosition = mCurrentBook.getPageNumber(referencePage);
                        else
                            insertPosition = mCurrentBook.getPageNumber(referencePage) - 1;
                    } else
                        insertPosition = mCurrentBook.getPageNumber(referencePage);

                    if (TopLayoutMode.PASTE_MODE == mTopLayoutMode)
                        copySelectedPageTo(insertPosition);
                    else if (TopLayoutMode.MOVE_MODE == mTopLayoutMode)
                        moveSelectedPageTo(insertPosition);
                }
            });
        }

        ThumbnailItem thumbnailItem1 = (ThumbnailItem) v.findViewById(R.id.thumbnail_list_item_1);
        ThumbnailItem thumbnailItem2 = (ThumbnailItem) v.findViewById(R.id.thumbnail_list_item_2);
        ThumbnailItem thumbnailItem3 = (ThumbnailItem) v.findViewById(R.id.thumbnail_list_item_3);
        ThumbnailItem thumbnailItem4 = (ThumbnailItem) v.findViewById(R.id.thumbnail_list_item_4);
        ThumbnailItem thumbnailItem5 = (ThumbnailItem) v.findViewById(R.id.thumbnail_list_item_5);
        ThumbnailItem thumbnailItem6 = (ThumbnailItem) v.findViewById(R.id.thumbnail_list_item_6);
        mThumbnailItemList.add(thumbnailItem1);
        mThumbnailItemList.add(thumbnailItem2);
        mThumbnailItemList.add(thumbnailItem3);
        mThumbnailItemList.add(thumbnailItem4);
        mThumbnailItemList.add(thumbnailItem5);
        mThumbnailItemList.add(thumbnailItem6);
        for (final ThumbnailItem thumbnailItem : mThumbnailItemList) {
            thumbnailItem.setBookUuid(mCurrentBook.getUUID());
            thumbnailItem.setOnItemClickListener(new ThumbnailItem.ItemClickListener() {
                @Override
                public void onClick(Object viewTag) {
                    if (TopLayoutMode.SELECTION_MODE == mTopLayoutMode) {
                        mCurrentBook.setCurrentPage((Page) viewTag);
                        ((NoteWriterActivity) getActivity()).switchToPage((Page) viewTag, true);
                        dismiss();
                    }
                }

                @Override
                public void onCheckedChange(Object viewTag, boolean b) {
                    mThumbnailListAdapter.setPageSelected(((Page) viewTag).getUUID(), b);
                    updateViewAtThumbBeSelected();
                }
            });

            thumbnailItem.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    mThumbnailListAdapter.setPageSelected(((Page) view.getTag()).getUUID(), true);
                    mBtnThumbnailManage.performClick();
                    return true;
                }
            });
        }

        ImageButton btnClose = (ImageButton) v.findViewById(R.id.btn_close);
        ImageButton btnClose2 = (ImageButton) v.findViewById(R.id.btn_close2);
        ImageButton btnClose3 = (ImageButton) v.findViewById(R.id.btn_close3);
        mBtnThumbnailManage = (ImageButton) v.findViewById(R.id.btn_thumbnail_manage);
        ImageButton btnExitCheckMode = (ImageButton) v.findViewById(R.id.btn_exit_check_mode);
        ImageButton btnExitPasteMode = (ImageButton) v.findViewById(R.id.btn_exit_paste_or_move_mode);
        mBtnThumbnailDelete = (LinearLayout) v.findViewById(R.id.btn_thumbnail_delete);
        mBtnThumbnailCopy = (LinearLayout) v.findViewById(R.id.btn_thumbnail_copy);
        mBtnThumbnailMove = (LinearLayout) v.findViewById(R.id.btn_thumbnail_move);
        btnClose.setOnClickListener(onBtnClickListener);
        btnClose2.setOnClickListener(onBtnClickListener);
        btnClose3.setOnClickListener(onBtnClickListener);
        mBtnThumbnailManage.setOnClickListener(onBtnClickListener);
        btnExitCheckMode.setOnClickListener(onBtnClickListener);
        btnExitPasteMode.setOnClickListener(onBtnClickListener);
        mBtnThumbnailDelete.setOnClickListener(onBtnClickListener);
        mBtnThumbnailCopy.setOnClickListener(onBtnClickListener);
        mBtnThumbnailMove.setOnClickListener(onBtnClickListener);

        initThumbnailListView();
        initTagList();
    }

    private ImageButton.OnClickListener onPageBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            /*
            int tagListCurrentPage = mTagListAdapter.getCurrentPage();
            */
            int thumbnailListCurrentPage = mThumbnailListAdapter.getCurrentPage();
            switch (view.getId()) {
                /*
                case R.id.btn_tag_list_page_up:
                case R.id.btn_tag_list_page_down:
                    if (R.id.btn_tag_list_page_up == view.getId())
                        tagListCurrentPage--;
                    else
                        tagListCurrentPage++;
                    mTagListAdapter.setCurrentPage(tagListCurrentPage);
                    updateTagListView();
                    break;
                */
                case R.id.btn_thumbnail_list_page_up:
                case R.id.btn_thumbnail_list_page_down:
                    if (R.id.btn_thumbnail_list_page_up == view.getId())
                        thumbnailListCurrentPage--;
                    else
                        thumbnailListCurrentPage++;
                    mThumbnailListAdapter.setCurrentPage(thumbnailListCurrentPage);
                    updateThumbnailListView(mThumbnailListStyle);
                    break;
            }
        }
    };

    private View.OnClickListener onBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_close:
                case R.id.btn_close2:
                case R.id.btn_close3:
                    dismiss();
                    break;
                case R.id.btn_thumbnail_manage:
                    updateTopLayoutView(TopLayoutMode.CHECK_MODE);

                    mThumbnailListStyle = ThumbnailListStyle.NORMAL_MODE;
                    updateViewAtThumbBeSelected();

                    for (int i = 0; i < mThumbnailListAdapter.getCurrentPageList().size(); i++) {
                        Page page = mThumbnailListAdapter.getCurrentPageList().get(i);
                        boolean isCheck = mThumbnailListAdapter.isPageSelected(page.getUUID());
                        mThumbnailItemList.get(i).updateCheckBox(isCheck);
                        mThumbnailItemList.get(i).setCheckBoxVisible(true);
                    }
                    break;
                case R.id.btn_exit_check_mode:
                    updateTopLayoutView(TopLayoutMode.SELECTION_MODE);

                    mThumbnailListAdapter.clearSelected();

                    for (ThumbnailItem thumbnailItem : mThumbnailItemList) {
                        thumbnailItem.updateCheckBox(false);
                        thumbnailItem.setCheckBoxVisible(false);
                    }
                    break;
                case R.id.btn_exit_paste_or_move_mode:
                    updateTopLayoutView(TopLayoutMode.CHECK_MODE);

                    /*
                    mThumbnailListAdapter.updateList(mCurrentBook.getFilteredPages());
                    */
                    mThumbnailListAdapter.updateList(mCurrentBook.getUnionFilteredPages(mStarTagItem.isTagCheck()));
                    mThumbnailListAdapter.notifyDataSetChanged();
                    mThumbnailListAdapter.setCurrentPage(1);

                    for (ThumbnailItem thumbnailItem : mThumbnailItemList) {
                        thumbnailItem.setCheckBoxVisible(true);
                    }
                    mThumbnailListStyle = ThumbnailListStyle.NORMAL_MODE;
                    updateThumbnailListView(mThumbnailListStyle);
                    break;
                case R.id.btn_thumbnail_delete:
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    String dialogTag = "delete_confirm";
                    String deleteConfirmMessage = getString(R.string.delete_selected_pages_hint);
                    AlertDialogFragment deleteDialogFragment = AlertDialogFragment.newInstance(deleteConfirmMessage, R.drawable.writing_ic_error, true, dialogTag);
                    deleteDialogFragment.setupNegativeButton(getString(android.R.string.no));
                    deleteDialogFragment.registerAlertDialogButtonClickListener(new AlertDialogButtonClickListener() {
                        @Override
                        public void onPositiveButtonClick(String fragmentTag) {
                            deleteSelectedPages();
                        }

                        @Override
                        public void onNegativeButtonClick(String fragmentTag) {
                        }
                    }, dialogTag);

                    ft.replace(R.id.alert_dialog_container, deleteDialogFragment, dialogTag)
                            .commit();
                    break;
                case R.id.btn_thumbnail_copy:
                    updateTopLayoutView(TopLayoutMode.PASTE_MODE);

                    mThumbnailListAdapter.updateList(mCurrentBook.getPages());
                    mThumbnailListAdapter.notifyDataSetChanged();
                    mThumbnailListAdapter.setCurrentPage(1);

                    for (ThumbnailItem thumbnailItem : mThumbnailItemList) {
                        thumbnailItem.setCheckBoxVisible(false);
                    }
                    mThumbnailListStyle = ThumbnailListStyle.INSERT_MODE;
                    updateThumbnailListView(mThumbnailListStyle);

                    break;
                case R.id.btn_thumbnail_move:
                    updateTopLayoutView(TopLayoutMode.MOVE_MODE);

                    mThumbnailListAdapter.updateList(mCurrentBook.getPages());
                    mThumbnailListAdapter.notifyDataSetChanged();
                    mThumbnailListAdapter.setCurrentPage(1);

                    for (ThumbnailItem thumbnailItem : mThumbnailItemList) {
                        thumbnailItem.setCheckBoxVisible(false);
                    }
                    mThumbnailListStyle = ThumbnailListStyle.INSERT_MODE;
                    updateThumbnailListView(mThumbnailListStyle);
                    break;
            }
        }
    };

    private ThumbnailTagItem.TagCheckChangedListener tagCheckChangedListener = new ThumbnailTagItem.TagCheckChangedListener() {
        @Override
        public void onCheckedChange(Object tag, boolean b) {
            if (mCurrentBook.getFilter().contains((Tag) tag))
                mCurrentBook.tagSetRemoveTag((Tag) tag);
            else
                mCurrentBook.tagSetAddTag((Tag) tag);

            mCurrentBook.filterChanged();
            /*
            mTagListAdapter.updateList(mCurrentBook.getFilter(), false);
            mThumbnailListAdapter.updateList(mCurrentBook.getFilteredPages());
            */
            if (mEtSearchTag.getText().toString().isEmpty())
                mThumbnailListAdapter.updateList(mCurrentBook.getFilteredPages());
            else
                mThumbnailListAdapter.updateList(mCurrentBook.getUnionFilteredPages(mStarTagItem.isTagCheck()));
            mThumbnailListAdapter.notifyDataSetChanged();
            mThumbnailListAdapter.setCurrentPage(1);
            updateThumbnailListView(mThumbnailListStyle);
        }
    };

    private EditText.OnKeyListener onSearchEditTextKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                utility.StringIllegal.checkFirstSpaceChar(mEtSearchTag);
                utility.StringIllegal.checkIllegalChar(mEtSearchTag);

                if (mEtSearchTag.getText().toString().equals("")) {
                    mBtnSearchTagEnter.setAlpha(0.2f);
                    mBtnSearchTagEnter.setEnabled(false);
                    return false;
                }

                String keyword = removeLastSpace(mEtSearchTag.getText().toString());
                mEtSearchTag.setText(keyword);
                mEtSearchTag.setSelection(keyword.length());
                mBtnSearchTagEnter.performClick();
            }
            return false;
        }
    };

    private TextWatcher searchEditTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.toString().equals("") || s.toString().isEmpty()) {
                mBtnSearchTagEnter.setAlpha(0.2f);
                mBtnSearchTagEnter.setEnabled(false);
                return;
            }

            utility.StringIllegal.checkFirstSpaceChar(mEtSearchTag);
            utility.StringIllegal.checkIllegalChar(mEtSearchTag);

            if (s.toString().equals(" ")) {
                mEtSearchTag.setText("");
                mBtnSearchTagEnter.setAlpha(0.2f);
                mBtnSearchTagEnter.setEnabled(false);
                return;
            }

            mBtnSearchTagEnter.setAlpha(1.0f);
            mBtnSearchTagEnter.setEnabled(true);
        }
    };

    private View.OnClickListener onSearchTagBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_search_tag_result_clear:
                    mEtSearchTag.setText("");
                    mCurrentBook.clearTagSetCheckedTag();
                    if (mStarTagItem.isTagCheck())
                        mCurrentBook.tagSetAddTag(mCurrentBookTagManager.findTag(TagManager.QUICK_TAG_NAME));
                    mCurrentBook.filterChanged();
                    mThumbnailListAdapter.updateList(mCurrentBook.getFilteredPages());
                    mThumbnailListAdapter.notifyDataSetChanged();
                    mThumbnailListAdapter.setCurrentPage(1);
                    updateThumbnailListView(mThumbnailListStyle);
                    break;
                case R.id.btn_search_tag_enter:
                    mEtSearchTag.setSelection(mEtSearchTag.getText().length());
                    hideInputMethod(mEtSearchTag);
                    mCurrentBook.clearTagSetCheckedTag();
                    LinkedList<Tag> searchTagList = mCurrentBookTagManager.findTagContain(mEtSearchTag.getText().toString());
                    for (Tag tag : searchTagList) {
                        mCurrentBook.tagSetAddTag(tag);
                    }
                    mThumbnailListAdapter.updateList(mCurrentBook.getUnionFilteredPages(mStarTagItem.isTagCheck()));
                    mThumbnailListAdapter.notifyDataSetChanged();
                    mThumbnailListAdapter.setCurrentPage(1);
                    updateThumbnailListView(mThumbnailListStyle);
                    break;
            }
        }
    };

    private void updateTopLayoutView(int topLayoutMode) {
        mTopLayoutMode = topLayoutMode;
        switch (mTopLayoutMode) {
            case TopLayoutMode.SELECTION_MODE:
                mLayoutTopSelectionMode.setVisibility(View.VISIBLE);
                mLayoutTopCheckMode.setVisibility(View.GONE);
                mLayoutTopPasteOrMoveMode.setVisibility(View.GONE);

                mLayoutTagList.setVisibility(View.VISIBLE);
                mLayoutSelectedPageOperation.setVisibility(View.GONE);
                break;
            case TopLayoutMode.CHECK_MODE:
                mLayoutTopSelectionMode.setVisibility(View.GONE);
                mLayoutTopCheckMode.setVisibility(View.VISIBLE);
                mLayoutTopPasteOrMoveMode.setVisibility(View.GONE);

                mLayoutTagList.setVisibility(View.GONE);
                mLayoutSelectedPageOperation.setVisibility(View.VISIBLE);
                break;
            case TopLayoutMode.PASTE_MODE:
                mLayoutTopSelectionMode.setVisibility(View.GONE);
                mLayoutTopCheckMode.setVisibility(View.GONE);
                mLayoutTopPasteOrMoveMode.setVisibility(View.VISIBLE);
                mTvTopLayoutTitle.setText(getString(R.string.paste_to));

                mLayoutTagList.setVisibility(View.GONE);
                mLayoutSelectedPageOperation.setVisibility(View.GONE);
                break;
            case TopLayoutMode.MOVE_MODE:
                mLayoutTopSelectionMode.setVisibility(View.GONE);
                mLayoutTopCheckMode.setVisibility(View.GONE);
                mLayoutTopPasteOrMoveMode.setVisibility(View.VISIBLE);
                mTvTopLayoutTitle.setText(getString(R.string.move_to));

                mLayoutTagList.setVisibility(View.GONE);
                mLayoutSelectedPageOperation.setVisibility(View.GONE);
                break;
        }
    }

    private void initTagList() {
        mCurrentBookTagManager.sort();
        /*
        mTagListAdapter = new TagListAdapter(mCurrentBook.getFilter(), 8, false);
        mTagListAdapter.setCurrentPage(1);
        */
        updateTagListView();
    }

    private void updateTagListView() {
        /*
        for (ThumbnailTagItem thumbnailTagItem : mThumbnailTagItemList) {
            thumbnailTagItem.setVisibility(View.INVISIBLE);
        }

        int currentPage = mTagListAdapter.getCurrentPage();
        int totalPage = mTagListAdapter.getTotalPage();
        mTvTagListCurrentPage.setText(String.valueOf(currentPage));
        mTvTagListTotalPage.setText(String.valueOf(totalPage));
        List<TagManager.Tag> currentTagList = mTagListAdapter.getCurrentPageList();

        if (currentTagList.isEmpty() && currentPage == 1 && totalPage == 1) {
            mBtnTagListPageUp.setEnabled(false);
            mBtnTagListPageDown.setEnabled(false);
        } else {
            mBtnTagListPageUp.setEnabled(true);
            mBtnTagListPageDown.setEnabled(true);
        }

        for (int i = 0; i < currentTagList.size(); i++) {
            mThumbnailTagItemList.get(i).setTagName(currentTagList.get(i).toString());
            mThumbnailTagItemList.get(i).setTagCheck(mCurrentBook.getFilter().contains(currentTagList.get(i)));
            mThumbnailTagItemList.get(i).setTag(currentTagList.get(i));
            mThumbnailTagItemList.get(i).setVisibility(View.VISIBLE);
        }
        */

        Tag quickTag = mCurrentBookTagManager.findTag(TagManager.QUICK_TAG_NAME);
        mStarTagItem.setTagName(quickTag.toString());
        mStarTagItem.setTagCheck(mCurrentBook.getFilter().contains(quickTag));
        mStarTagItem.setTag(quickTag);
    }

    private void initThumbnailListView() {
        int currentPage = mThumbnailListAdapter.getCurrentPage();
        int totalPage = mThumbnailListAdapter.getTotalPage();
        mTvThumbnailListCurrentPage.setText(String.valueOf(currentPage));
        mTvThumbnailListTotalPage.setText(String.valueOf(totalPage));
        mCurrentPageList = mThumbnailListAdapter.getCurrentPageList();

        for (ImageButton imageButton : mInsertLocationList) {
            imageButton.setVisibility(View.GONE);
        }

        mTvSearchNotFoundHint.setVisibility(View.GONE);
        mLayoutThumbnailListRow1.setVisibility(View.VISIBLE);
        mLayoutThumbnailListRow2.setVisibility(View.VISIBLE);

        for (int i = 0; i < mThumbnailItemList.size(); i++) {
            if (i == 2 || i == 5)
                mThumbnailItemList.get(i).setBorderStyle(ThumbnailItem.BorderStyle.BOTTOM);
            else
                mThumbnailItemList.get(i).setBorderStyle(ThumbnailItem.BorderStyle.RIGHT_BOTTOM);

            mThumbnailItemList.get(i).setupItemStyle(ThumbnailListStyle.NORMAL_MODE);
            mThumbnailItemList.get(i).setClickable(true);
            mThumbnailItemList.get(i).setVisibility(View.INVISIBLE);
        }

        for (int j = 0; j < mCurrentPageList.size(); j++) {
            mThumbnailItemList.get(j).setVisibility(View.VISIBLE);
        }
    }

    private void updateThumbnailListView(int pageListRows) {
        int currentPage = mThumbnailListAdapter.getCurrentPage();
        int totalPage = mThumbnailListAdapter.getTotalPage();
        mTvThumbnailListCurrentPage.setText(String.valueOf(currentPage));
        mTvThumbnailListTotalPage.setText(String.valueOf(totalPage));
        mCurrentPageList = mThumbnailListAdapter.getCurrentPageList();

        for (ImageButton imageButton : mInsertLocationList) {
            imageButton.setVisibility(View.GONE);
        }

        if (mCurrentPageList.isEmpty() && currentPage == 1 && totalPage == 1) {
            mTvSearchNotFoundHint.setVisibility(View.VISIBLE);
            mLayoutThumbnailListRow1.setVisibility(View.GONE);
            mLayoutThumbnailListRow2.setVisibility(View.GONE);
        } else {
            mTvSearchNotFoundHint.setVisibility(View.GONE);
            mLayoutThumbnailListRow1.setVisibility(View.VISIBLE);
            mLayoutThumbnailListRow2.setVisibility(View.VISIBLE);
        }

        for (int i = 0; i < mThumbnailItemList.size(); i++) {
            if (ThumbnailListStyle.NORMAL_MODE == pageListRows) {
                if (i == 2 || i == 5)
                    mThumbnailItemList.get(i).setBorderStyle(ThumbnailItem.BorderStyle.BOTTOM);
                else
                    mThumbnailItemList.get(i).setBorderStyle(ThumbnailItem.BorderStyle.RIGHT_BOTTOM);

                mThumbnailItemList.get(i).setupItemStyle(ThumbnailListStyle.NORMAL_MODE);
                mThumbnailItemList.get(i).setClickable(true);
            } else {
                mThumbnailItemList.get(i).setBorderStyle(ThumbnailItem.BorderStyle.FULL_AROUND);
                mThumbnailItemList.get(i).setupItemStyle(ThumbnailListStyle.INSERT_MODE);
                mThumbnailItemList.get(i).setClickable(false);
            }
            mThumbnailItemList.get(i).setVisibility(View.INVISIBLE);
        }

        for (int j = 0; j < mCurrentPageList.size(); j++) {
            Page page = mCurrentPageList.get(j);
            mThumbnailItemList.get(j).setTag(page);
            mThumbnailItemList.get(j).updateIndex(getPageIndex(page));
            mThumbnailItemList.get(j).updateCheckBox(mThumbnailListAdapter.isPageSelected(page.getUUID()));
            mThumbnailItemList.get(j).updatePreview();
            mThumbnailItemList.get(j).setVisibility(View.VISIBLE);

            if (ThumbnailListStyle.INSERT_MODE == pageListRows) {
                mInsertLocationList.get(j + 1).setVisibility(View.VISIBLE);
            }
            if (mTopLayoutMode == TopLayoutMode.CHECK_MODE) {
                mThumbnailItemList.get(j).setCheckBoxVisible(true);
            }
        }
        if (ThumbnailListStyle.INSERT_MODE == pageListRows) {
            mInsertLocationList.get(0).setVisibility(View.VISIBLE);
            if (mCurrentPageList.size() > 3)
                mInsertLocationList.get(mCurrentPageList.size() + 1).setVisibility(View.VISIBLE);
        }
    }

    private void updateViewAtThumbBeSelected() {
        int selectedCount = mThumbnailListAdapter.getSelectedPageList().size();
        mTvSelectedCounter.setText(String.valueOf(selectedCount));

        if (selectedCount > 0) {
            mBtnThumbnailDelete.setVisibility(View.VISIBLE);

            if (selectedCount == 1) {
                mBtnThumbnailCopy.setVisibility(View.VISIBLE);
                mBtnThumbnailMove.setVisibility(View.VISIBLE);
            } else {
                mBtnThumbnailCopy.setVisibility(View.GONE);
                mBtnThumbnailMove.setVisibility(View.GONE);
            }

        } else {
            mBtnThumbnailDelete.setVisibility(View.GONE);
            mBtnThumbnailCopy.setVisibility(View.GONE);
            mBtnThumbnailMove.setVisibility(View.GONE);
        }
    }

    private int getPageIndex(Page page) {
        return mCurrentBook.getPageNumber(page);
    }

    private void deleteSelectedPages() {
        UndoManager.getUndoManager().clearHistory();

        LinkedList<Page> selectedPageList = mThumbnailListAdapter.getSelectedPageList();
        mCurrentBook.deletePagesSet(selectedPageList);
        mHandler.removeCallbacks(saveCurrentNoteBookRunnable);
        mHandler.post(saveCurrentNoteBookRunnable);

        updateTopLayoutView(TopLayoutMode.SELECTION_MODE);

        mCurrentBook.clearTagSetCheckedTag();
        mCurrentBook.filterChanged();
        /*
        mTagListAdapter.updateList(mCurrentBook.getFilter(), false);
        */
        mEtSearchTag.setText("");
        mStarTagItem.setTagCheck(false);
        updateTagListView();

        mThumbnailListAdapter.clearSelected();

        for (ThumbnailItem thumbnailItem : mThumbnailItemList) {
            thumbnailItem.setCheckBoxVisible(false);
        }

        mThumbnailListStyle = ThumbnailListStyle.NORMAL_MODE;
        /*
        mThumbnailListAdapter.updateList(mCurrentBook.getFilteredPages());
        */
        mThumbnailListAdapter.updateList(mCurrentBook.getPages());
        mThumbnailListAdapter.notifyDataSetChanged();
    }

    private void copySelectedPageTo(int position) {
        UndoManager.getUndoManager().clearHistory();

        LinkedList<Page> selectedPageList = mThumbnailListAdapter.getSelectedPageList();
        Page p = selectedPageList.get(0);
        mCurrentBook.clonePageTo(p, position, true);
        mHandler.removeCallbacks(saveCurrentNoteBookRunnable);
        mHandler.post(saveCurrentNoteBookRunnable);

        updateTopLayoutView(TopLayoutMode.SELECTION_MODE);

        mCurrentBook.clearTagSetCheckedTag();
        mCurrentBook.filterChanged();
        /*
        mTagListAdapter.updateList(mCurrentBook.getFilter(), false);
        */
        mEtSearchTag.setText("");
        mStarTagItem.setTagCheck(false);
        updateTagListView();

        mThumbnailListAdapter.clearSelected();
        /*
        mThumbnailListAdapter.updateList(mCurrentBook.getFilteredPages());
        */
        mThumbnailListAdapter.updateList(mCurrentBook.getPages());
        mThumbnailListAdapter.notifyDataSetChanged();
        mThumbnailListStyle = ThumbnailListStyle.NORMAL_MODE;
    }

    private void moveSelectedPageTo(int position) {
        UndoManager.getUndoManager().clearHistory();

        LinkedList<Page> selectedPageList = mThumbnailListAdapter.getSelectedPageList();
        Page p = selectedPageList.get(0);
        mCurrentBook.clonePageTo(p, position, true);
        mCurrentBook.deletePagesSet(selectedPageList);
        mHandler.removeCallbacks(saveCurrentNoteBookRunnable);
        mHandler.post(saveCurrentNoteBookRunnable);

        updateTopLayoutView(TopLayoutMode.SELECTION_MODE);

        mCurrentBook.clearTagSetCheckedTag();
        mCurrentBook.filterChanged();
        /*
        mTagListAdapter.updateList(mCurrentBook.getFilter(), false);
        */
        mEtSearchTag.setText("");
        mStarTagItem.setTagCheck(false);
        updateTagListView();

        mThumbnailListAdapter.clearSelected();
        /*
        mThumbnailListAdapter.updateList(mCurrentBook.getFilteredPages());
        */
        mThumbnailListAdapter.updateList(mCurrentBook.getPages());
        mThumbnailListAdapter.notifyDataSetChanged();
        mThumbnailListStyle = ThumbnailListStyle.NORMAL_MODE;
    }

    private String removeLastSpace(String inputString) {
        while (inputString.substring(inputString.length() - 1).equals(" ") || inputString.substring(inputString.length() - 1).equals("　")) {
            inputString = inputString.substring(0, inputString.length() - 1);
        }
        return inputString;
    }

    private void showInputMethod() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    private void hideInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void dismiss() {
        mCurrentBook.clearTagSetCheckedTag();
        mCurrentBook.filterChanged();
        super.dismiss();
        ((NoteWriterActivity) getActivity()).updatePageNumber();
        ((NoteWriterActivity) getActivity()).switchToPage(mCurrentBook.currentPage(), true);
    }
}
