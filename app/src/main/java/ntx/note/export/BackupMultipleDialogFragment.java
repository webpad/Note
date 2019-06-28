package ntx.note.export;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.dropbox.android.Dropbox;
import com.dropbox.android.UploadDropboxListFilesTask;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import name.vbraun.filepicker.AsyncTaskResult;
import name.vbraun.filepicker.SearchFileByExtensionAsyncTask;
import name.vbraun.lib.pen.Hardware;
import ntx.note.Global;
import ntx.note.data.Bookshelf;
import ntx.note2.R;
import utility.CustomDialogFragment;

import static android.content.Context.MODE_PRIVATE;
import static ntx.note.Global.STRING_GB;
import static ntx.note.Global.STRING_KB;
import static ntx.note.Global.STRING_MB;

public class BackupMultipleDialogFragment extends CustomDialogFragment {
    private static final String ARGUMENT_KEY_BACKUP_UUID_LIST = "backup_uuid_list";
    private static final String ARGUMENT_KEY_DELETE_AFTER_BACKUP = "delete_after_backup";
    private static final String INTERNAL_PATH = Global.DIRECTORY_SDCARD_NOTE;
    private static final String EXTERNAL_PATH = Global.DIRECTORY_EXTERNALSD_NOTE;
    private static final String SEARCH_FILE_TYPE = ".note";
    private static final String TYPE_DROPBOX = "dropbox";

    public @interface BackupSaveVia {
        int INTERNAL = 0;
        int EXTERNAL = 1;
        int DROPBOX = 2;
        int EMAIL = 3;
    }

    private Spinner mSpinnerSaveFileVia;
    private Button mBtnBackupOk;
    private TextView mTvSelectedSize;
    private LinearLayout mLayoutDropboxAccount;
    private TextView mTvDropboxAccount;
    private TextView mTvFreeSpace;
    private LinearLayout mLayoutSpaceInfo;
    private LinearLayout mLayoutNotEnoughSpaceHint;
    private TextView mTvEmailCheckWifiHint;
    private LinearLayout mLayoutEmailTempNotEnoughSpaceHint;
    private TextView mTvDropboxSignIn;
    private TextView mTvDropboxSignOut;

    private String mBackupPath;

    private ArrayList<UUID> mBackupNoteUuidList;
    private List<String> mInternalExistedFileNameList = new ArrayList<>();
    private List<String> mExternalExistedFileNameList = new ArrayList<>();
    private List<String> mDropboxFileNameList = new ArrayList<>();
    private ArrayList<UUID> mConflictUuidList = new ArrayList<>();
    private Dropbox dbx;

    private GetSelectedBackupFileSizeTask mGetSelectedBackupFileSizeTask;
    private String mDropboxAccountStr;
    private boolean mIsCalculating = true;
    private boolean mIsDropboxSyncing = true;
    private boolean mIsGotAccountFreeSpace = false;
    private boolean mDeleteAfterBackup = false;
    private long mAccountFreeSpace = 0;
    private long mSelectedSize = 0;


    public static BackupMultipleDialogFragment newInstance(ArrayList<UUID> uuids, boolean deleteAfterBackup) {
        BackupMultipleDialogFragment fragment = new BackupMultipleDialogFragment();

        Bundle bundle = new Bundle();
        bundle.putBoolean(ARGUMENT_KEY_DELETE_AFTER_BACKUP, deleteAfterBackup);
        bundle.putSerializable(ARGUMENT_KEY_BACKUP_UUID_LIST, uuids);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBackupNoteUuidList = new ArrayList<>();
        mDeleteAfterBackup = getArguments().getBoolean(ARGUMENT_KEY_DELETE_AFTER_BACKUP);
        mBackupNoteUuidList.addAll((ArrayList<UUID>) getArguments().getSerializable(ARGUMENT_KEY_BACKUP_UUID_LIST));
        dbx = new Dropbox(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.file_backup_multiple_dialog, container, false);
        initView(v);
        mGetSelectedBackupFileSizeTask = new GetSelectedBackupFileSizeTask(mBackupNoteUuidList);
        mGetSelectedBackupFileSizeTask.asyncTaskResult = new AsyncTaskResult<Long>() {
            @Override
            public void taskFinish(Long result) {
                mIsCalculating = false;
                mSelectedSize = result;
                updateStorageInfo();
            }
        };
        mIsCalculating = true;
        mGetSelectedBackupFileSizeTask.execute();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSpinnerSaveFileVia.setSelection(BackupSaveVia.INTERNAL);
        updateStorageInfo();
        initStorageFileList();
    }

    private void initView(View v) {
        mLayoutDropboxAccount = (LinearLayout) v.findViewById(R.id.layout_dropbox_account);
        mTvDropboxAccount = (TextView) v.findViewById(R.id.tv_dropbox_account);
        mLayoutSpaceInfo = (LinearLayout) v.findViewById(R.id.layout_space_info);
        mTvFreeSpace = (TextView) v.findViewById(R.id.tv_free_space);
        mLayoutNotEnoughSpaceHint = (LinearLayout) v.findViewById(R.id.layout_space_not_enough_hint);
        mTvEmailCheckWifiHint = (TextView) v.findViewById(R.id.tv_email_check_wifi_hint);
        mLayoutEmailTempNotEnoughSpaceHint = (LinearLayout) v.findViewById(R.id.layout_email_temp_space_not_enough_hint);
        mTvDropboxSignIn = (TextView) v.findViewById(R.id.tv_dropbox_sign_in);
        mTvDropboxSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbx.logIn();
                dismiss();
            }
        });
        mTvDropboxSignOut = (TextView) v.findViewById(R.id.tv_dropbox_sign_out);
        mTvDropboxSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbx.logOut();
                dismiss();
            }
        });

        TextView txtMessage = (TextView) v.findViewById(R.id.txt_message);
        txtMessage.setText(getActivity().getString(R.string.message_export_total_file, mBackupNoteUuidList.size()));
        mTvSelectedSize = (TextView) v.findViewById(R.id.tv_selected_size);
        mTvSelectedSize.setText(String.format("(%s)",
                getActivity().getString(R.string.calculating)));

        mBtnBackupOk = (Button) v.findViewById(R.id.btn_backup_ok);
        mBtnBackupOk.setOnClickListener(onBtnClickListener);
        mBtnBackupOk.setEnabled(false);
        mBtnBackupOk.setAlpha(0.2f);
        Button btnBackupCancel = (Button) v.findViewById(R.id.btn_backup_cancel);
        btnBackupCancel.setOnClickListener(onBtnClickListener);

        mSpinnerSaveFileVia = (Spinner) v.findViewById(R.id.sp_save_file_via);

        String[] STRING_ARRAY_SAVE_FILE_VIA = Hardware.hasExternalSDCard()
                ? getResources().getStringArray(R.array.backup_via_entries)
                : getResources().getStringArray(R.array.backup_via_entries_no_extsd);
        LinkedList<String> via_values = new LinkedList<>();
        ArrayAdapter<CharSequence> saveFileViaValuesAdapter = new ArrayAdapter(getActivity(), R.layout.cinny_ui_spinner, via_values);
        saveFileViaValuesAdapter.addAll(STRING_ARRAY_SAVE_FILE_VIA);
        saveFileViaValuesAdapter.setDropDownViewResource(R.layout.cinny_ui_spinner_item);
        mSpinnerSaveFileVia.setAdapter(saveFileViaValuesAdapter);
        mSpinnerSaveFileVia.setOnItemSelectedListener(onSaveFileViaSpinnerItemSelectedListener);
    }

    private OnItemSelectedListener onSaveFileViaSpinnerItemSelectedListener = new OnItemSelectedListener() {

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case BackupSaveVia.INTERNAL:
                    mBackupPath = Global.DIRECTORY_SDCARD_NOTE;
                    break;
                case BackupSaveVia.EXTERNAL:
                    mBackupPath = Global.DIRECTORY_EXTERNALSD_NOTE;
                    break;
                case BackupSaveVia.EMAIL:
                    mBackupPath = Global.PATH_SDCARD + Global.MAIL_FILE_TEMP_DIR;
                    break;
                case BackupSaveVia.DROPBOX:
                    mBackupPath = TYPE_DROPBOX;
                    if (isWifiConnected()) {
                        if (!mIsGotAccountFreeSpace) {
                            mDropboxAccountStr = getString(R.string.sync_with_dropbox);
                            mIsDropboxSyncing = true;
                            dbx.trySignIn(new Dropbox.TrySignInFinishListener() {
                                @Override
                                public void onTryFinished(boolean isSignIn) {
                                    updateDropboxAccountInfo(isSignIn);
                                    mIsDropboxSyncing = false;
                                }
                            });
                        }
                    } else {
                        mDropboxAccountStr = getString(R.string.check_network);
                    }
                    break;
                default:
                    break;
            }
            updateStorageInfo();
        }
    };

    private Button.OnClickListener onBtnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_backup_cancel:
                    dismiss();
                    break;
                case R.id.btn_backup_ok:
                    if (!new File(mBackupPath).exists()) {
                        new File(mBackupPath).mkdir();
                    }
                    checkSelectedListIsExistInStorage();
                    if (mConflictUuidList.isEmpty()) {
                        backupSelectedList();
                    } else {
                        showConflictDialogFragment();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void updateDropboxAccountInfo(boolean isSignIn) {
        if (isSignIn)
            initDropboxFileList();
        else {
            mDropboxAccountStr = "";
            updateStorageInfo();
        }
    }

    private void initStorageFileList() {
        SearchFileByExtensionAsyncTask searchInternalTask = new SearchFileByExtensionAsyncTask();
        searchInternalTask.searchFinishCallback = new AsyncTaskResult<List<File>>() {
            @Override
            public void taskFinish(List<File> result) {
                mInternalExistedFileNameList.clear();
                for (File file : result) {
                    mInternalExistedFileNameList.add(file.getName());
                }
            }
        };
        searchInternalTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, INTERNAL_PATH, SEARCH_FILE_TYPE);

        SearchFileByExtensionAsyncTask searchExternalTask = new SearchFileByExtensionAsyncTask();
        searchExternalTask.searchFinishCallback = new AsyncTaskResult<List<File>>() {
            @Override
            public void taskFinish(List<File> result) {
                mExternalExistedFileNameList.clear();
                for (File file : result) {
                    mExternalExistedFileNameList.add(file.getName());
                }
            }
        };
        searchExternalTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, EXTERNAL_PATH, SEARCH_FILE_TYPE);
    }

    private void initDropboxFileList() {
        mDropboxFileNameList.clear();
        dbx.registerOnMetaFileListLoadedListener(new Dropbox.OnMetadataFileListLoadedListener() {
            @Override
            public void onMetaFileListLoaded(List<Metadata> metadataFileList) {
                for (Metadata metadata : metadataFileList) {
                    mDropboxFileNameList.add(metadata.getName());
                }
                SharedPreferences prefs = getActivity().getSharedPreferences(Global.ACCESS_KEY, MODE_PRIVATE);
                mDropboxAccountStr = prefs.getString(Global.ACCESS_USER_Email, "");
                mAccountFreeSpace = prefs.getLong(Global.ACCESS_USER_USED_FREE_SPACE, 0);
                mIsGotAccountFreeSpace = true;

                updateStorageInfo();
            }
        });
        dbx.getDropboxFileName();
    }

    private boolean isWifiConnected() {
        ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = null;
        if (connManager != null) {
            wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        }
        return wifi != null && wifi.isConnected();
    }

    private void backupSelectedList() {
        switch (mSpinnerSaveFileVia.getSelectedItemPosition()) {
            case BackupSaveVia.INTERNAL:
            case BackupSaveVia.EXTERNAL:
            case BackupSaveVia.EMAIL:
                new BackupMultipleAsyncTask(getActivity(), mBackupNoteUuidList, mDeleteAfterBackup).execute(mBackupPath);
                break;
            case BackupSaveVia.DROPBOX:
                if (!isWifiConnected())
                    return;

                new UploadDropboxListFilesTask(getActivity(), mBackupNoteUuidList, mDeleteAfterBackup).execute();
                break;
        }
        dismiss();
    }

    private void checkSelectedListIsExistInStorage() {
        mConflictUuidList.clear();
        switch (mSpinnerSaveFileVia.getSelectedItemPosition()) {
            case BackupSaveVia.INTERNAL:
                mConflictUuidList = getConflictUuidList(mInternalExistedFileNameList);
                break;
            case BackupSaveVia.EXTERNAL:
                mConflictUuidList = getConflictUuidList(mExternalExistedFileNameList);
                break;
            case BackupSaveVia.DROPBOX:
                mConflictUuidList = getConflictUuidList(mDropboxFileNameList);
                break;
            case BackupSaveVia.EMAIL:
            default:
                break;
        }
    }

    private ArrayList<UUID> getConflictUuidList(List<String> fileNameList) {
        ArrayList<UUID> conflictUuidList = new ArrayList<>();
        for (UUID uuid : mBackupNoteUuidList) {
            String fileName = Bookshelf.getInstance().getBook(uuid).getTitle() + ".note";
            if (fileNameList.contains(fileName)) {
                conflictUuidList.add(uuid);
            }
        }
        return conflictUuidList;
    }

    private void showConflictDialogFragment() {
        BackupConflictDialogFragment fragment = BackupConflictDialogFragment.newInstance(
                mSpinnerSaveFileVia.getSelectedItemPosition(),
                mBackupNoteUuidList,
                mConflictUuidList,
                mDeleteAfterBackup);

        FragmentTransaction fragmentTransaction = getActivity().getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.dialog_container, fragment, BackupConflictDialogFragment.class.getSimpleName()).commit();
    }

    private String getSizeString(double size) {
        String sizeStr = "< 1 " + STRING_KB;
        double sizef = 0.0f;
        if ((size / 1024f / 1024f / 1024f) > 1) {
            sizef = size / 1024f / 1024f / 1024f;
            sizeStr = String.format("%.2f", sizef) + " " + STRING_GB;
        } else if ((size / 1024f / 1024f) > 1) {
            sizef = size / 1024f / 1024f;
            sizeStr = String.format("%.2f", sizef) + " " + STRING_MB;
        } else if ((size / 1024f) > 1) {
            sizeStr = String.valueOf((int) (size / 1024f)) + " " + STRING_KB;
        }
        return sizeStr;
    }

    private double getStorageAvailableSpace(int storage) {
        StatFs stat;
        if (BackupSaveVia.INTERNAL == storage)
            stat = new StatFs(Global.PATH_SDCARD);
        else
            stat = new StatFs(Global.PATH_EXTERNALSD);
        return ((double) stat.getBlockSize() * (double) stat.getAvailableBlocks());
    }

    private void updateStorageInfo() {
        int dropboxAccountVisibility = View.GONE;
        mTvDropboxAccount.setText(mDropboxAccountStr);

        int dropboxSignInVisibility = View.GONE;
        int dropboxSignOutVisibility = View.GONE;

        int spaceInfoVisibility = View.INVISIBLE;
        double freeSpace = 0;
        String freeSpaceStr = "";
        int notEnoughSpaceVisibility = View.GONE;
        boolean btnBackupOkEnable = false;

        switch (mSpinnerSaveFileVia.getSelectedItemPosition()) {
            case BackupSaveVia.INTERNAL:
                dropboxAccountVisibility = View.GONE;
                dropboxSignInVisibility = View.GONE;
                dropboxSignOutVisibility = View.GONE;
                spaceInfoVisibility = View.VISIBLE;

                freeSpace = getStorageAvailableSpace(BackupSaveVia.INTERNAL);
                break;
            case BackupSaveVia.EXTERNAL:
                dropboxAccountVisibility = View.GONE;
                dropboxSignInVisibility = View.GONE;
                dropboxSignOutVisibility = View.GONE;
                spaceInfoVisibility = View.VISIBLE;

                freeSpace = getStorageAvailableSpace(BackupSaveVia.EXTERNAL);
                break;
            case BackupSaveVia.DROPBOX:
                dropboxAccountVisibility = View.VISIBLE;
                dropboxSignInVisibility = !mIsDropboxSyncing ? (mIsGotAccountFreeSpace ? View.GONE : View.VISIBLE) : View.GONE;
                dropboxSignOutVisibility = !mIsDropboxSyncing ? (mIsGotAccountFreeSpace ? View.VISIBLE : View.GONE) : View.GONE;
                spaceInfoVisibility = mIsGotAccountFreeSpace ? View.VISIBLE : View.INVISIBLE;

                if (mIsGotAccountFreeSpace)
                    freeSpace = mAccountFreeSpace;
                break;
            case BackupSaveVia.EMAIL:
                dropboxAccountVisibility = View.GONE;
                dropboxSignInVisibility = View.GONE;
                dropboxSignOutVisibility = View.GONE;
                spaceInfoVisibility = View.GONE;

                freeSpace = getStorageAvailableSpace(BackupSaveVia.INTERNAL);
                break;
        }
        mLayoutDropboxAccount.setVisibility(dropboxAccountVisibility);
        mTvDropboxSignIn.setVisibility(dropboxSignInVisibility);
        mTvDropboxSignOut.setVisibility(dropboxSignOutVisibility);

        freeSpaceStr = String.format("%s %s",
                getSizeString(freeSpace),
                getString(R.string.dropbox_free_space));
        mTvFreeSpace.setText(freeSpaceStr);
        mLayoutSpaceInfo.setVisibility(spaceInfoVisibility);

        if (mIsCalculating) {
            btnBackupOkEnable = false;
        } else {
            mTvSelectedSize.setText(String.format("(%s)", getSizeString(mSelectedSize)));

            if (freeSpace > mSelectedSize) {
                btnBackupOkEnable = true;
                notEnoughSpaceVisibility = View.GONE;
            } else {
                btnBackupOkEnable = false;
                notEnoughSpaceVisibility = View.VISIBLE;
            }
        }

        mLayoutNotEnoughSpaceHint.setVisibility(notEnoughSpaceVisibility);

        if (BackupSaveVia.EMAIL == mSpinnerSaveFileVia.getSelectedItemPosition()) {
            if (!isWifiConnected()) {
                mTvEmailCheckWifiHint.setVisibility(View.VISIBLE);
                mLayoutEmailTempNotEnoughSpaceHint.setVisibility(View.GONE);
                btnBackupOkEnable = false;
            } else {
                mTvEmailCheckWifiHint.setVisibility(View.GONE);
                mLayoutEmailTempNotEnoughSpaceHint.setVisibility(notEnoughSpaceVisibility);
            }
        }

        if (btnBackupOkEnable) {
            mBtnBackupOk.setAlpha(1.0f);
            mBtnBackupOk.setEnabled(true);
        } else {
            mBtnBackupOk.setAlpha(0.2f);
            mBtnBackupOk.setEnabled(false);
        }
    }

    @Override
    public void dismiss() {
        dbx.unregisterOnMetaFileListLoadedListener();
        dbx.unRegisterTrySignInFinishListener();
        mGetSelectedBackupFileSizeTask.cancel(true);
        super.dismiss();
    }
}
