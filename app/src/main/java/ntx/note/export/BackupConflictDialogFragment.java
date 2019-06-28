package ntx.note.export;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.dropbox.android.UploadDropboxListFilesTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ntx.note.Global;
import ntx.note2.R;
import utility.CustomDialogFragment;

public class BackupConflictDialogFragment extends CustomDialogFragment {
    private static String ARGUMENT_KEY_BACKUP_STORAGE = "backup_storage";
    private static String ARGUMENT_KEY_BACKUP_UUID_LIST = "backup_uuid_list";
    private static String ARGUMENT_KEY_BACKUP_CONFLICT_UUID_LIST = "backup_conflict_uuid_list";
    private static String ARGUMENT_KEY_DELETE_AFTER_BACKUP = "delete_after_backup";

    private TextView mTvCurrentPage;
    private TextView mTvTotalPage;
    private ImageButton mBtnSkipAll;
    private ImageButton mBtnReplaceAll;
    private LinearLayout[] mLayoutItemArray = new LinearLayout[5];

    private int mBackupStorage;
    private ArrayList<UUID> mBackupNoteUuidList;
    private BackupConflictListAdapter mBackupConflictListAdapter;
    private boolean mDeleteAfterBackup = false;


    public static BackupConflictDialogFragment newInstance(int backupStorage, ArrayList<UUID> backupUuidList, ArrayList<UUID> backupConflictUuidList, boolean deleteAfterBackup) {
        BackupConflictDialogFragment fragment = new BackupConflictDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARGUMENT_KEY_BACKUP_STORAGE, backupStorage);
        bundle.putBoolean(ARGUMENT_KEY_DELETE_AFTER_BACKUP, deleteAfterBackup);
        bundle.putSerializable(ARGUMENT_KEY_BACKUP_UUID_LIST, backupUuidList);
        bundle.putSerializable(ARGUMENT_KEY_BACKUP_CONFLICT_UUID_LIST, backupConflictUuidList);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBackupStorage = getArguments().getInt(ARGUMENT_KEY_BACKUP_STORAGE);
        mDeleteAfterBackup = getArguments().getBoolean(ARGUMENT_KEY_DELETE_AFTER_BACKUP);
        mBackupNoteUuidList = (ArrayList<UUID>) getArguments().getSerializable(ARGUMENT_KEY_BACKUP_UUID_LIST);
        ArrayList<UUID> backupConflictUuidList = (ArrayList<UUID>) getArguments().getSerializable(ARGUMENT_KEY_BACKUP_CONFLICT_UUID_LIST);
        mBackupConflictListAdapter = new BackupConflictListAdapter(backupConflictUuidList);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateConflictListView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.backup_conflict_dialog, container, false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        TextView tvHeader = (TextView) v.findViewById(R.id.tv_backup_conflict_dialog_header);
        tvHeader.setText(getString(R.string.import_list_conflict_header, mBackupConflictListAdapter.getCount()));

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
                            mBackupConflictListAdapter.setItemSelect((int) group.getTag(), false);
                            break;
                        case R.id.btn_replace:
                            mBackupConflictListAdapter.setItemSelect((int) group.getTag(), true);
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
                    currentPage = mBackupConflictListAdapter.getCurrentPage();
                    currentPage--;
                    mBackupConflictListAdapter.setCurrentPage(currentPage);
                    break;
                case R.id.btn_conflict_list_page_down:
                    currentPage = mBackupConflictListAdapter.getCurrentPage();
                    currentPage++;
                    mBackupConflictListAdapter.setCurrentPage(currentPage);
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
                    backupSelectedList();
                    dismiss();
                    return;
                case R.id.btn_skip_all:
                    mBackupConflictListAdapter.setAllItemSelect(false);
                    break;
                case R.id.btn_replace_all:
                    mBackupConflictListAdapter.setAllItemSelect(true);
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

        List<BackupConflictItem> currentPageList = mBackupConflictListAdapter.getCurrentPageList();
        for (int i = 0; i < currentPageList.size(); i++) {
            TextView tvFileName = (TextView) mLayoutItemArray[i].findViewById(R.id.tv_backup_file_name);
            tvFileName.setText(currentPageList.get(i).getBackupFileName());
            RadioGroup radioGroupConflictOption = (RadioGroup) mLayoutItemArray[i].findViewById(R.id.radio_group_conflict_option);
            if (currentPageList.get(i).isItemSelected())
                radioGroupConflictOption.check(R.id.btn_replace);
            else
                radioGroupConflictOption.check(R.id.btn_skip);
            mLayoutItemArray[i].setVisibility(View.VISIBLE);
        }

        if (mBackupConflictListAdapter.isAllItemUnSelected()) {
            mBtnSkipAll.setSelected(true);
            mBtnReplaceAll.setSelected(false);
        } else if (mBackupConflictListAdapter.isAllItemSelected()) {
            mBtnSkipAll.setSelected(false);
            mBtnReplaceAll.setSelected(true);
        } else {
            mBtnSkipAll.setSelected(false);
            mBtnReplaceAll.setSelected(false);
        }
        updatePageInfo();
    }

    private void updatePageInfo() {
        mTvCurrentPage.setText(String.valueOf(mBackupConflictListAdapter.getCurrentPage()));
        mTvTotalPage.setText(String.valueOf(mBackupConflictListAdapter.getTotalPage()));
    }

    private void backupSelectedList() {
        for (BackupConflictItem item : mBackupConflictListAdapter.getUnSelectedList()) {
            mBackupNoteUuidList.remove(item.getBackupUuid());
        }

        switch (mBackupStorage) {
            case BackupMultipleDialogFragment.BackupSaveVia.INTERNAL:
                new BackupMultipleAsyncTask(getActivity(), mBackupNoteUuidList, mDeleteAfterBackup).execute(Global.DIRECTORY_SDCARD_NOTE);
                break;
            case BackupMultipleDialogFragment.BackupSaveVia.EXTERNAL:
                new BackupMultipleAsyncTask(getActivity(), mBackupNoteUuidList, mDeleteAfterBackup).execute(Global.DIRECTORY_EXTERNALSD_NOTE);
                break;
            case BackupMultipleDialogFragment.BackupSaveVia.DROPBOX:
                new UploadDropboxListFilesTask(getActivity(), mBackupNoteUuidList, mDeleteAfterBackup).execute();
                break;
        }
    }

}
