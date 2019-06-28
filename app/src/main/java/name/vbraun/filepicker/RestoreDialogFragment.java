package name.vbraun.filepicker;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import name.vbraun.lib.pen.Hardware;
import name.vbraun.lib.pen.PenEventNTX;
import ntx.note.Global;
import ntx.note.export.MySpinner;
import ntx.note2.R;
import utility.CustomDialogFragment;

import static ntx.note.Global.STRING_KB;
import static ntx.note.Global.STRING_MB;

public class RestoreDialogFragment extends CustomDialogFragment {
    private static final String ARGUMENT_KEY_UUID = "uuid";
    private static final String SEARCH_FILE_TYPE = ".note";
    private static final String INTERNAL_PATH = Global.DIRECTORY_SDCARD_NOTE;
    private static final String EXTERNAL_PATH = Global.DIRECTORY_EXTERNALSD_NOTE;
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    private static final int SORT_ASCENDING_TAG = 0;
    private static final int SORT_DESCENDING_TAG = 1;

    public @interface RestoreFileVia {
        int INTERNAL = 0;
        int EXTERNAL = 1;
    }

    public @interface RestoreListSort {
        int DATE_ASCENDING = 1;
        int DATE_DESCENDING = 2;
        int NAME_ASCENDING = 3;
        int NAME_DESCENDING = 4;
        int SIZE_ASCENDING = 5;
        int SIZE_DESCENDING = 6;
    }

    private LinearLayout mRestoreSelectionLayout;
    private LinearLayout mRestoreConfirmLayout;
    private MySpinner mSpinnerRestoreFileVia;

    private TextView mTvListEmptyHint;

    private List<LinearLayout> mRestoreListItemArray = new ArrayList<>();

    private ImageButton mBtnSortByName;
    private ImageButton mBtnSortByDate;
    private ImageButton mBtnSortBySize;
    private ImageButton mBtnPageUp;
    private ImageButton mBtnPageDown;
    private TextView mTvCurrentPage;
    private TextView mTvTotalPage;

    private String mUuidString;

    private List<RestoreItem> mCurrentRestoreList = new ArrayList<>();
    private List<RestoreItem> mInternalRestoreList = new ArrayList<>();
    private List<RestoreItem> mExternalRestoreList = new ArrayList<>();

    private int mCurrentPage;
    private int mTotalPage;
    private int mSortListProperty = RestoreListSort.DATE_ASCENDING;
    private RestoreItem mSelectedItem;

    private RestoreListAdapter mInternalAdapter;
    private RestoreListAdapter mExternalAdapter;

    private boolean mIsLoadingInternalRestoreFileList = true;
    private boolean mIsLoadingExternalRestoreFileList = true;

    private View mView;
    private Handler mHandler = new Handler();

    public static RestoreDialogFragment newInstance(UUID uuid) {
        RestoreDialogFragment fragment = new RestoreDialogFragment();
        Bundle args = new Bundle();

        args.putString(ARGUMENT_KEY_UUID, uuid.toString());

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUuidString = getArguments().getString(ARGUMENT_KEY_UUID, "");

        mInternalAdapter = new RestoreListAdapter(mInternalRestoreList);
        mExternalAdapter = new RestoreListAdapter(mExternalRestoreList);

        SearchFileByExtensionAsyncTask searchInternalTask = new SearchFileByExtensionAsyncTask();
        searchInternalTask.searchFinishCallback = new AsyncTaskResult<List<File>>() {
            @Override
            public void taskFinish(List<File> result) {
                List<RestoreItem> tempList = new ArrayList<>();
                for (File file : result) {
                    tempList.add(new RestoreItem(file.getPath(), file.getName(), file.lastModified(),
                            file.length()));
                }
                filterByUuid(tempList, mInternalRestoreList, mInternalAdapter, RestoreFileVia.INTERNAL);
            }
        };
        searchInternalTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, INTERNAL_PATH, SEARCH_FILE_TYPE);

        SearchFileByExtensionAsyncTask searchExternalTask = new SearchFileByExtensionAsyncTask();
        searchExternalTask.searchFinishCallback = new AsyncTaskResult<List<File>>() {
            @Override
            public void taskFinish(List<File> result) {
                List<RestoreItem> tempList = new ArrayList<>();
                for (File file : result) {
                    tempList.add(new RestoreItem(file.getPath(), file.getName(), file.lastModified(),
                            file.length()));
                }
                filterByUuid(tempList, mExternalRestoreList, mExternalAdapter, RestoreFileVia.EXTERNAL);
            }
        };
        searchExternalTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, EXTERNAL_PATH, SEARCH_FILE_TYPE);

    }

    @Override
    public void onResume() {
        super.onResume();
        mSortListProperty = RestoreListSort.DATE_DESCENDING;
        mInternalAdapter.sortByDateDescending();
        mSpinnerRestoreFileVia.setSelection(RestoreFileVia.INTERNAL);
        updateRestoreListView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.file_restore_dialog, container, false);
        initView(v);
        mView = v;
        return v;
    }

    private void initView(View v) {
        mRestoreSelectionLayout = (LinearLayout) v.findViewById(R.id.layout_restore_selection);
        mRestoreConfirmLayout = (LinearLayout) v.findViewById(R.id.layout_restore_confirm);
        mRestoreConfirmLayout.setVisibility(View.GONE);

        mTvListEmptyHint = (TextView) v.findViewById(R.id.tv_list_empty_hint);

        LinearLayout restoreListItem1 = (LinearLayout) v.findViewById(R.id.restore_list_item_1);
        LinearLayout restoreListItem2 = (LinearLayout) v.findViewById(R.id.restore_list_item_2);
        LinearLayout restoreListItem3 = (LinearLayout) v.findViewById(R.id.restore_list_item_3);
        LinearLayout restoreListItem4 = (LinearLayout) v.findViewById(R.id.restore_list_item_4);
        LinearLayout restoreListItem5 = (LinearLayout) v.findViewById(R.id.restore_list_item_5);
        mRestoreListItemArray.clear();
        mRestoreListItemArray.add(restoreListItem1);
        mRestoreListItemArray.add(restoreListItem2);
        mRestoreListItemArray.add(restoreListItem3);
        mRestoreListItemArray.add(restoreListItem4);
        mRestoreListItemArray.add(restoreListItem5);
        restoreListItem1.setOnClickListener(onListItemClickListener);
        restoreListItem2.setOnClickListener(onListItemClickListener);
        restoreListItem3.setOnClickListener(onListItemClickListener);
        restoreListItem4.setOnClickListener(onListItemClickListener);
        restoreListItem5.setOnClickListener(onListItemClickListener);

        mBtnSortByName = (ImageButton) v.findViewById(R.id.btn_sort_by_name);
        mBtnSortByDate = (ImageButton) v.findViewById(R.id.btn_sort_by_date);
        mBtnSortBySize = (ImageButton) v.findViewById(R.id.btn_sort_by_size);
        mBtnSortByName.setTag(new Integer(SORT_ASCENDING_TAG));
        mBtnSortByDate.setTag(new Integer(SORT_DESCENDING_TAG));
        mBtnSortBySize.setTag(new Integer(SORT_ASCENDING_TAG));
        mBtnSortByName.setImageLevel(SORT_ASCENDING_TAG);
        mBtnSortByDate.setImageLevel(SORT_DESCENDING_TAG);
        mBtnSortBySize.setImageLevel(SORT_ASCENDING_TAG);
        mBtnSortByName.setOnClickListener(onSortButtonClickListener);
        mBtnSortByDate.setOnClickListener(onSortButtonClickListener);
        mBtnSortBySize.setOnClickListener(onSortButtonClickListener);
        mBtnSortByDate.setSelected(true);

        mBtnPageUp = (ImageButton) v.findViewById(R.id.btn_restore_list_page_up);
        mBtnPageDown = (ImageButton) v.findViewById(R.id.btn_restore_list_page_down);
        mBtnPageUp.setOnClickListener(onPageButtonClickListener);
        mBtnPageDown.setOnClickListener(onPageButtonClickListener);

        mTvCurrentPage = (TextView) v.findViewById(R.id.tv_page_index);
        mTvTotalPage = (TextView) v.findViewById(R.id.tv_page_total);

        Button btnRestoreCancel = (Button) v.findViewById(R.id.btn_restore_cancel);
        btnRestoreCancel.setOnClickListener(onBtnClickListener);

        Button btnRestoreConfirmCancel = (Button) v.findViewById(R.id.btn_restore_confirm_cancel);
        btnRestoreConfirmCancel.setOnClickListener(onBtnClickListener);

        Button btnRestoreConfirmOk = (Button) v.findViewById(R.id.btn_restore_confirm_ok);
        btnRestoreConfirmOk.setOnClickListener(onBtnClickListener);


        mSpinnerRestoreFileVia = (MySpinner) v.findViewById(R.id.sp_restore_file_via);

        String[] STRING_ARRAY_SAVE_FILE_VIA = Hardware.hasExternalSDCard()
                ? getResources().getStringArray(R.array.backup_via_local_entries)
                : getResources().getStringArray(R.array.backup_via_local_entries_no_extsd);
        LinkedList<String> via_values = new LinkedList<String>();
        ArrayAdapter<CharSequence> restoreFileViaValuesAdapter = new ArrayAdapter(getActivity(), R.layout.cinny_ui_spinner, via_values);
        restoreFileViaValuesAdapter.addAll(STRING_ARRAY_SAVE_FILE_VIA);
        restoreFileViaValuesAdapter.setDropDownViewResource(R.layout.cinny_ui_spinner_item);
        mSpinnerRestoreFileVia.setAdapter(restoreFileViaValuesAdapter);
        mSpinnerRestoreFileVia.setOnItemSelectedEvenIfUnchangedListener(onRestoreFileViaSpinnerItemSelectedListener);

    }

    private void setListItemView(View itemView, RestoreItem item) {
        itemView.setTag(item);

        TextView tvFileName = (TextView) itemView.findViewById(R.id.tv_file_name);
        TextView tvFileDate = (TextView) itemView.findViewById(R.id.tv_file_date);
        TextView tvFileSize = (TextView) itemView.findViewById(R.id.tv_file_size);

        tvFileName.setText(item.getFileName());
        tvFileDate.setText(simpleDateFormat.format(item.getFileDate()));

        long fileSize = item.getFileSize();
        String sizeStr = "< 1 " + STRING_KB;
        if ((fileSize / 1024f / 1024f) > 1) {
            sizeStr = " " + (int) (fileSize / 1024f / 1024f) + " " + STRING_MB;
        } else if ((fileSize / 1024f) > 1) {
            sizeStr = " " + (int) (fileSize / 1024f) + " " + STRING_KB;
        }
        tvFileSize.setText(sizeStr);
    }

    private View.OnClickListener onSortButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            mBtnSortByDate.setSelected(false);
            mBtnSortByName.setSelected(false);
            mBtnSortBySize.setSelected(false);

            Integer viewTag = (Integer) view.getTag();

            switch (view.getId()) {
                case R.id.btn_sort_by_name:
                    if (SORT_ASCENDING_TAG == viewTag) {
                        if (RestoreListSort.NAME_ASCENDING == mSortListProperty) {
                            mSortListProperty = RestoreListSort.NAME_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mInternalAdapter.sortByNameDescending();
                            mExternalAdapter.sortByNameDescending();
                        } else {
                            mSortListProperty = RestoreListSort.NAME_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mInternalAdapter.sortByNameAscending();
                            mExternalAdapter.sortByNameAscending();
                        }
                    } else if (SORT_DESCENDING_TAG == viewTag) {
                        if (RestoreListSort.NAME_DESCENDING == mSortListProperty) {
                            mSortListProperty = RestoreListSort.NAME_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mInternalAdapter.sortByNameAscending();
                            mExternalAdapter.sortByNameAscending();
                        } else {
                            mSortListProperty = RestoreListSort.NAME_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mInternalAdapter.sortByNameDescending();
                            mExternalAdapter.sortByNameDescending();
                        }
                    }
                    break;
                case R.id.btn_sort_by_date:
                    if (SORT_ASCENDING_TAG == viewTag) {
                        if (RestoreListSort.DATE_ASCENDING == mSortListProperty) {
                            mSortListProperty = RestoreListSort.DATE_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mInternalAdapter.sortByDateDescending();
                            mExternalAdapter.sortByDateDescending();
                        } else {
                            mSortListProperty = RestoreListSort.DATE_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mInternalAdapter.sortByDateAscending();
                            mExternalAdapter.sortByDateAscending();
                        }
                    } else if (SORT_DESCENDING_TAG == viewTag) {
                        if (RestoreListSort.DATE_DESCENDING == mSortListProperty) {
                            mSortListProperty = RestoreListSort.DATE_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mInternalAdapter.sortByDateAscending();
                            mExternalAdapter.sortByDateAscending();
                        } else {
                            mSortListProperty = RestoreListSort.DATE_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mInternalAdapter.sortByDateDescending();
                            mExternalAdapter.sortByDateDescending();
                        }
                    }
                    break;
                case R.id.btn_sort_by_size:
                    if (SORT_ASCENDING_TAG == viewTag) {
                        if (RestoreListSort.SIZE_ASCENDING == mSortListProperty) {
                            mSortListProperty = RestoreListSort.SIZE_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mInternalAdapter.sortBySizeDescending();
                            mExternalAdapter.sortBySizeDescending();
                        } else {
                            mSortListProperty = RestoreListSort.SIZE_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mInternalAdapter.sortBySizeAscending();
                            mExternalAdapter.sortBySizeAscending();
                        }
                    } else if (SORT_DESCENDING_TAG == viewTag) {
                        if (RestoreListSort.SIZE_DESCENDING == mSortListProperty) {
                            mSortListProperty = RestoreListSort.SIZE_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mInternalAdapter.sortBySizeAscending();
                            mExternalAdapter.sortBySizeAscending();
                        } else {
                            mSortListProperty = RestoreListSort.SIZE_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mInternalAdapter.sortBySizeDescending();
                            mExternalAdapter.sortBySizeDescending();
                        }
                    }
                    break;
                default:
                    break;
            }
            view.setTag(viewTag);
            view.setSelected(true);
            ((ImageButton) view).setImageLevel(viewTag);
            mInternalAdapter.notifyDataSetChanged();
            mExternalAdapter.notifyDataSetChanged();
            updateRestoreListView();
        }
    };

    private View.OnClickListener onPageButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_restore_list_page_up:
                    mCurrentPage--;
                    break;
                case R.id.btn_restore_list_page_down:
                    mCurrentPage++;
                    break;
                default:
                    break;
            }

            switch (mSpinnerRestoreFileVia.getSelectedItemPosition()) {
                case RestoreFileVia.INTERNAL:
                    mInternalAdapter.setCurrentPage(mCurrentPage);
                    break;
                case RestoreFileVia.EXTERNAL:
                    mExternalAdapter.setCurrentPage(mCurrentPage);
                    break;
                default:
                    break;
            }

            updateRestoreListView();
        }
    };

    private View.OnClickListener onBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_restore_cancel:
                    dismiss();
                    break;
                case R.id.btn_restore_confirm_cancel:
                    showSelectionView();
                    break;
                case R.id.btn_restore_confirm_ok:
                    RestoreAsyncTask restoreAsyncTask = new RestoreAsyncTask(getActivity(), mSelectedItem.getFilePath(),
                            mSelectedItem.getPages());
                    restoreAsyncTask.execute();
                    dismiss();
                    break;
                default:
                    break;
            }
        }
    };

    private View.OnClickListener onListItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mSelectedItem = (RestoreItem) view.getTag();
            showConfirmView();
        }
    };

    private OnItemSelectedListener onRestoreFileViaSpinnerItemSelectedListener = new OnItemSelectedListener() {

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mInternalAdapter.setCurrentPage(1);
            mExternalAdapter.setCurrentPage(1);
            updateRestoreListView();
            mHandler.postDelayed(invalidateViewToCleanSpinnerGhosting, 500);
        }
    };

    private void updateRestoreListView() {
        mCurrentRestoreList.clear();

        for (LinearLayout layout : mRestoreListItemArray) {
            layout.setVisibility(View.INVISIBLE);
        }
        switch (mSpinnerRestoreFileVia.getSelectedItemPosition()) {
            case RestoreFileVia.INTERNAL:
                mCurrentRestoreList.addAll(mInternalAdapter.getCurrentPageList());
                mCurrentPage = mInternalAdapter.getCurrentPage();
                mTotalPage = mInternalAdapter.getTotalPage();
                if (mIsLoadingInternalRestoreFileList)
                    mTvListEmptyHint.setText(getString(R.string.loading_notebook));
                else
                    mTvListEmptyHint.setText(getString(R.string.restore_list_empty_hint));
                break;
            case RestoreFileVia.EXTERNAL:
                mCurrentRestoreList.addAll(mExternalAdapter.getCurrentPageList());
                mCurrentPage = mExternalAdapter.getCurrentPage();
                mTotalPage = mExternalAdapter.getTotalPage();
                if (mIsLoadingExternalRestoreFileList)
                    mTvListEmptyHint.setText(getString(R.string.loading_notebook));
                else
                    mTvListEmptyHint.setText(getString(R.string.restore_list_empty_hint));
                break;
            default:
                break;
        }

        mTvCurrentPage.setText(String.valueOf(mCurrentPage));
        mTvTotalPage.setText(String.valueOf(mTotalPage));

        if (mCurrentRestoreList.isEmpty() && mCurrentPage == 1 && mTotalPage == 1) {
            mTvListEmptyHint.setVisibility(View.VISIBLE);
            mBtnPageUp.setEnabled(false);
            mBtnPageDown.setEnabled(false);
        } else {
            mTvListEmptyHint.setVisibility(View.INVISIBLE);
            mBtnPageUp.setEnabled(true);
            mBtnPageDown.setEnabled(true);
        }

        for (int i = 0; i < mCurrentRestoreList.size(); i++) {
            RestoreItem item = mCurrentRestoreList.get(i);
            setListItemView(mRestoreListItemArray.get(i), item);
            mRestoreListItemArray.get(i).setVisibility(View.VISIBLE);
        }

    }

    Runnable invalidateViewToCleanSpinnerGhosting = new Runnable() {
        @Override
        public void run() {
            mView.invalidate(PenEventNTX.UPDATE_MODE_PARTIAL_GL16);
        }
    };

    private void filterByUuid(List<RestoreItem> inputList, final List<RestoreItem> outputList,
                              final RestoreListAdapter adapter, final int restoreFileVia) {
        outputList.clear();
        RestoreListFilterAsyncTask filterAsyncTask = new RestoreListFilterAsyncTask(inputList);
        filterAsyncTask.filterFinishCallback = new AsyncTaskResult<List<RestoreItem>>() {
            @Override
            public void taskFinish(List<RestoreItem> result) {
                outputList.addAll(result);
                adapter.notifyDataSetChanged();
                adapter.sortByDateDescending();

                if (RestoreFileVia.INTERNAL == restoreFileVia)
                    mIsLoadingInternalRestoreFileList = false;
                else
                    mIsLoadingExternalRestoreFileList = false;

                if (restoreFileVia == mSpinnerRestoreFileVia.getSelectedItemPosition()) {
                    /**
                     * 2019.5.8 Karote add try-catch
                     * Prevent the task finish callback but the dialog fragment is already dismiss.
                     */
                    try {
                        updateRestoreListView();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        filterAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mUuidString);
    }

    private void showConfirmView() {
        mRestoreSelectionLayout.setVisibility(View.GONE);
        mRestoreConfirmLayout.setVisibility(View.VISIBLE);
    }

    private void showSelectionView() {
        mRestoreConfirmLayout.setVisibility(View.GONE);
        mRestoreSelectionLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void dismiss() {
        mIsLoadingInternalRestoreFileList = true;
        mIsLoadingExternalRestoreFileList = true;
        mInternalRestoreList.clear();
        mExternalRestoreList.clear();
        mInternalAdapter.notifyDataSetChanged();
        mExternalAdapter.notifyDataSetChanged();
        mTvListEmptyHint.setText(getString(R.string.loading_notebook));
        super.dismiss();
    }

}
