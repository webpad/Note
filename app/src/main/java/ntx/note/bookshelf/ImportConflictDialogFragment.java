package ntx.note.bookshelf;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.List;

import name.vbraun.filepicker.ImportConflictListAdapter;
import name.vbraun.filepicker.ImportItem;
import ntx.note2.R;
import utility.CustomDialogFragment;

public class ImportConflictDialogFragment extends CustomDialogFragment {
    private static String ARGUMENT_KEY_IMPORT_CONFLICT_LIST = "import_conflict_list";

    private TextView mTvCurrentPage;
    private TextView mTvTotalPage;
    private ImageButton mBtnSkipAll;
    private ImageButton mBtnReplaceAll;
    private LinearLayout[] mLayoutItemArray = new LinearLayout[5];

    private ImportConflictListAdapter mImportConflictListAdapter;

    public interface OnFinishedConflictItemSelection {
        void onFinishedSelection(List<ImportItem> resultList);
    }

    private OnFinishedConflictItemSelection mCallback;

    public static ImportConflictDialogFragment newInstance(ImportConflictList importConflictList) {
        ImportConflictDialogFragment fragment = new ImportConflictDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARGUMENT_KEY_IMPORT_CONFLICT_LIST, importConflictList);
        fragment.setArguments(bundle);
        return fragment;
    }

    public void setOnFinishedConflictItemSelectionListener(OnFinishedConflictItemSelection listener) {
        this.mCallback = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImportConflictList importConflictList = getArguments().getParcelable(ARGUMENT_KEY_IMPORT_CONFLICT_LIST);
        mImportConflictListAdapter = new ImportConflictListAdapter(importConflictList);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateConflictListView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.import_conflict_dialog, container, false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        TextView tvHeader = (TextView) v.findViewById(R.id.tv_import_conflict_dialog_header);
        tvHeader.setText(getString(R.string.import_list_conflict_header, mImportConflictListAdapter.getCount()));

        mBtnSkipAll = (ImageButton) v.findViewById(R.id.btn_skip_all);
        mBtnReplaceAll = (ImageButton) v.findViewById(R.id.btn_replace_all);
        mBtnSkipAll.setOnClickListener(onBtnClickListener);
        mBtnReplaceAll.setOnClickListener(onBtnClickListener);

        mLayoutItemArray[0] = (LinearLayout) v.findViewById(R.id.conflict_list_item_1);
        mLayoutItemArray[1] = (LinearLayout) v.findViewById(R.id.conflict_list_item_2);
        mLayoutItemArray[2] = (LinearLayout) v.findViewById(R.id.conflict_list_item_3);
        mLayoutItemArray[3] = (LinearLayout) v.findViewById(R.id.conflict_list_item_4);
        mLayoutItemArray[4] = (LinearLayout) v.findViewById(R.id.conflict_list_item_5);
        for (int i = 0; i < mLayoutItemArray.length; i++) {
            mLayoutItemArray[i].setTag(i);
            mLayoutItemArray[i].findViewById(R.id.radio_group_conflict_option).setTag(i);
            ((RadioGroup) mLayoutItemArray[i].findViewById(R.id.radio_group_conflict_option)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch (checkedId) {
                        case R.id.btn_skip:
                            mImportConflictListAdapter.setItemSelect((int) group.getTag(), false);
                            break;
                        case R.id.btn_replace:
                            mImportConflictListAdapter.setItemSelect((int) group.getTag(), true);
                            break;
                    }
                    updateConflictListView();
                }
            });
        }

        mTvCurrentPage = (TextView) v.findViewById(R.id.tv_conflict_list_page_index);
        mTvTotalPage = (TextView) v.findViewById(R.id.tv_conflict_list_page_total);

        ImageButton btnConflictListPageUp = (ImageButton) v.findViewById(R.id.btn_conflict_list_page_up);
        ImageButton btnConflictListPageDown = (ImageButton) v.findViewById(R.id.btn_conflict_list_page_down);
        btnConflictListPageUp.setOnClickListener(onPageBtnClickListener);
        btnConflictListPageDown.setOnClickListener(onPageBtnClickListener);

        Button btnCancel = (Button) v.findViewById(R.id.btn_conflict_cancel);
        Button btnOk = (Button) v.findViewById(R.id.btn_conflict_ok);
        btnCancel.setOnClickListener(onBtnClickListener);
        btnOk.setOnClickListener(onBtnClickListener);
    }

    private Button.OnClickListener onPageBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int currentPage;
            switch (view.getId()) {
                case R.id.btn_conflict_list_page_up:
                    currentPage = mImportConflictListAdapter.getCurrentPage();
                    currentPage--;
                    mImportConflictListAdapter.setCurrentPage(currentPage);
                    break;
                case R.id.btn_conflict_list_page_down:
                    currentPage = mImportConflictListAdapter.getCurrentPage();
                    currentPage++;
                    mImportConflictListAdapter.setCurrentPage(currentPage);
                    break;
            }
            updateConflictListView();
        }
    };

    private Button.OnClickListener onBtnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_conflict_cancel:
                    dismiss();
                    return;
                case R.id.btn_conflict_ok:
                    if (mCallback != null)
                        mCallback.onFinishedSelection(mImportConflictListAdapter.getResultList());
                    dismiss();
                    return;
                case R.id.btn_skip_all:
                    mImportConflictListAdapter.setAllItemSelect(false);
                    break;
                case R.id.btn_replace_all:
                    mImportConflictListAdapter.setAllItemSelect(true);
                    break;
                default:
                    break;
            }
            updateConflictListView();
        }
    };

    private void updateConflictListView() {
        for (LinearLayout layout : mLayoutItemArray) {
            layout.setVisibility(View.INVISIBLE);
        }

        List<ImportItem> currentPageList = mImportConflictListAdapter.getCurrentPageList();
        for (int i = 0; i < currentPageList.size(); i++) {
            TextView tvFileName = (TextView) mLayoutItemArray[i].findViewById(R.id.tv_import_file_name);
            TextView tvOriginalTitle = (TextView) mLayoutItemArray[i].findViewById(R.id.tv_original_note_title);
            tvFileName.setText(currentPageList.get(i).getFileName());
            String originalStr = getString(R.string.original) + mImportConflictListAdapter.getItemMappingTitle(currentPageList.get(i).getItemUuid());
            tvOriginalTitle.setText(originalStr);
            RadioGroup radioGroupConflictOption = (RadioGroup) mLayoutItemArray[i].findViewById(R.id.radio_group_conflict_option);
            if (currentPageList.get(i).isItemSelected())
                radioGroupConflictOption.check(R.id.btn_replace);
            else
                radioGroupConflictOption.check(R.id.btn_skip);
            mLayoutItemArray[i].setVisibility(View.VISIBLE);
        }

        if (mImportConflictListAdapter.isAllItemUnSelected()) {
            mBtnSkipAll.setSelected(true);
            mBtnReplaceAll.setSelected(false);
        } else if (mImportConflictListAdapter.isAllItemSelected()) {
            mBtnSkipAll.setSelected(false);
            mBtnReplaceAll.setSelected(true);
        } else {
            mBtnSkipAll.setSelected(false);
            mBtnReplaceAll.setSelected(false);
        }
        updatePageInfo();
    }

    private void updatePageInfo() {
        mTvCurrentPage.setText(String.valueOf(mImportConflictListAdapter.getCurrentPage()));
        mTvTotalPage.setText(String.valueOf(mImportConflictListAdapter.getTotalPage()));
    }

}
