package ntx.note.export;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.StatFs;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dropbox.android.Dropbox;
import com.dropbox.android.UploadDropboxSingleFileTask;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import name.vbraun.filepicker.AsyncTaskResult;
import name.vbraun.lib.pen.Hardware;
import name.vbraun.lib.pen.PenEventNTX;
import ntx.note.Global;
import ntx.note.data.Book;
import ntx.note2.R;
import utility.CustomDialogFragment;

import static android.content.Context.MODE_PRIVATE;
import static ntx.note.Global.STRING_GB;
import static ntx.note.Global.STRING_KB;
import static ntx.note.Global.STRING_MB;

public class BackupDialogFragment extends CustomDialogFragment {
    private static final String ARGUMENT_KEY_UUID = "uuid";
    private static final String ARGUMENT_KEY_DO_SAVE_CURRENT = "do_save_current";
    private static final String ARGUMENT_KEY_DELETE_AFTER_BACKUP = "delete_after_backup";

    public @interface BackupSaveVia {
        int INTERNAL = 0;
        int EXTERNAL = 1;
        int DROPBOX = 2;
        int EMAIL = 3;
    }

    private EditText mEtFileName;
    private TextView mTvNoteSize;
    private MySpinner mSpinnerSaveFileVia;
    private TextView mTvFilePath;
    private TextView mTvDropboxAccount;
    private TextView mTvDropboxSignIn;
    private TextView mTvDropboxSignOut;
    private LinearLayout mLayoutSpaceInfo;
    private TextView mTvFreeSpace;
    private LinearLayout mLayoutNotEnoughSpaceHint;
    private LinearLayout mLayoutEmailTempNotEnoughSpaceHint;
    private Button mBtnBackupOk;

    private Book mBackupNoteBook;

    private Dropbox dbx;
    private List<String> mDropboxFileNameList = new ArrayList<>();

    private String mBackupPath;
    private String mDropboxAccountStr;
    private boolean mDoSaveCurrent = false;
    private boolean mDeleteAfterBackup = false;
    private boolean mIsCalculating = true;
    private boolean mIsDropboxSyncing = true;
    private boolean mIsGotAccountFreeSpace = false;
    private long mAccountFreeSpace = 0;
    private long mNoteSize = 0;
    private GetSelectedBackupFileSizeTask mGetSelectedBackupFileSizeTask;

    private View mView;
    private Handler mHandler = new Handler();

    public static BackupDialogFragment newInstance(UUID saveBookUuid, boolean doSaveCurrent, boolean deleteAfterBackup) {
        BackupDialogFragment fragment = new BackupDialogFragment();

        Bundle args = new Bundle();
        args.putString(ARGUMENT_KEY_UUID, saveBookUuid.toString());
        args.putBoolean(ARGUMENT_KEY_DO_SAVE_CURRENT, doSaveCurrent);
        args.putBoolean(ARGUMENT_KEY_DELETE_AFTER_BACKUP, deleteAfterBackup);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String uuidString = getArguments().getString(ARGUMENT_KEY_UUID, "");
        mDoSaveCurrent = getArguments().getBoolean(ARGUMENT_KEY_DO_SAVE_CURRENT);
        mDeleteAfterBackup = getArguments().getBoolean(ARGUMENT_KEY_DELETE_AFTER_BACKUP);
        mBackupNoteBook = new Book(UUID.fromString(uuidString), false);
        dbx = new Dropbox(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.file_backup_dialog, container, false);
        initView(v);
        mView = v;
        List<UUID> tempList = new ArrayList<>();
        tempList.add(mBackupNoteBook.getUUID());
        mGetSelectedBackupFileSizeTask = new GetSelectedBackupFileSizeTask(tempList);
        mGetSelectedBackupFileSizeTask.asyncTaskResult = new AsyncTaskResult<Long>() {
            @Override
            public void taskFinish(Long result) {
                mIsCalculating = false;
                mNoteSize = result;
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

        mEtFileName.setText(mBackupNoteBook.getTitle());
        mEtFileName.requestFocus();
        mEtFileName.selectAll();
        showInputMethod();

        mSpinnerSaveFileVia.setSelection(BackupSaveVia.INTERNAL);
        updateStorageInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
        hideInputMethod();
    }

    private void initView(View v) {
        Button btnBackupCancel = (Button) v.findViewById(R.id.btn_backup_cancel);
        btnBackupCancel.setOnClickListener(onBtnClickListener);
        mBtnBackupOk = (Button) v.findViewById(R.id.btn_backup_ok);
        mBtnBackupOk.setOnClickListener(onBtnClickListener);
        mBtnBackupOk.setEnabled(false);
        mBtnBackupOk.setAlpha(0.2f);

        mEtFileName = (EditText) v.findViewById(R.id.et_backup_name);
        mTvNoteSize = (TextView) v.findViewById(R.id.tv_note_size);
        mTvNoteSize.setText(String.format("(%s)", getActivity().getString(R.string.calculating)));
        mSpinnerSaveFileVia = (MySpinner) v.findViewById(R.id.sp_save_file_via);
        mTvFilePath = (TextView) v.findViewById(R.id.tv_file_path);
        mTvDropboxAccount = (TextView) v.findViewById(R.id.tv_dropbox_account);
        mLayoutSpaceInfo = (LinearLayout) v.findViewById(R.id.layout_space_info);
        mTvFreeSpace = (TextView) v.findViewById(R.id.tv_free_space);
        mLayoutNotEnoughSpaceHint = (LinearLayout) v.findViewById(R.id.layout_space_not_enough_hint);
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


        mEtFileName.addTextChangedListener(edtFileNameTextWatcher);

        String[] STRING_ARRAY_SAVE_FILE_VIA = Hardware.hasExternalSDCard()
                ? getResources().getStringArray(R.array.backup_via_entries)
                : getResources().getStringArray(R.array.backup_via_entries_no_extsd);
        LinkedList<String> via_values = new LinkedList<>();
        ArrayAdapter<CharSequence> saveFileViaValuesAdapter = new ArrayAdapter(getActivity(), R.layout.cinny_ui_spinner, via_values);
        saveFileViaValuesAdapter.addAll(STRING_ARRAY_SAVE_FILE_VIA);
        saveFileViaValuesAdapter.setDropDownViewResource(R.layout.cinny_ui_spinner_item);
        mSpinnerSaveFileVia.setAdapter(saveFileViaValuesAdapter);
        mSpinnerSaveFileVia.setOnItemSelectedEvenIfUnchangedListener(onSaveFileViaSpinnerItemSelectedListener);

    }

    private TextWatcher edtFileNameTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            updateExportPath(s.toString());
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(final CharSequence s, int start, int before, int count) {
        }

    };

    private OnItemSelectedListener onSaveFileViaSpinnerItemSelectedListener = new OnItemSelectedListener() {

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (BackupSaveVia.DROPBOX == position) {
                if (isWifiConnected()) {
                    if (!mIsGotAccountFreeSpace) {
                        mDropboxAccountStr = getString(R.string.sync_with_dropbox);
                        mIsDropboxSyncing = true;
                        dbx.trySignIn(new Dropbox.TrySignInFinishListener() {
                            @Override
                            public void onTryFinished(boolean isSignIn) {
                                mIsDropboxSyncing = false;
                                updateDropboxAccountInfo(isSignIn);
                            }
                        });
                    }
                } else {
                    mDropboxAccountStr = getString(R.string.check_network);
                }
            }
            updateExportPath(mEtFileName.getText().toString());
            mHandler.postDelayed(invalidateViewToCleanSpinnerGhosting, 500);
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
                    hideInputMethod();
                    if (isFileExists()) {
                        FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
                        String overWriteHintDialogTag = "over_write_hint";
                        String overWriteHintMessage = getString(R.string.msg_import_confirm,
                                mEtFileName.getText().toString());

                        AlertDialogFragment overwriteHintDialogFragment = AlertDialogFragment.newInstance(overWriteHintMessage,
                                R.drawable.writing_ic_error, true, overWriteHintDialogTag);

                        overwriteHintDialogFragment.registerAlertDialogButtonClickListener(
                                new AlertDialogButtonClickListener() {
                                    @Override
                                    public void onPositiveButtonClick(String fragmentTag) {
                                        dismiss();
                                        if (BackupSaveVia.DROPBOX != mSpinnerSaveFileVia.getSelectedItemPosition()) {
                                            new BackupAsyncTask(getActivity(), mBackupNoteBook.getUUID(), mDoSaveCurrent, mDeleteAfterBackup).execute(mBackupPath);
                                        } else {
                                            String fileName = mEtFileName.getText().toString() + ".note";
                                            new UploadDropboxSingleFileTask(getActivity(), mBackupNoteBook.getUUID(), mDeleteAfterBackup).execute(fileName);
                                        }
                                    }

                                    @Override
                                    public void onNegativeButtonClick(String fragmentTag) {

                                    }
                                },
                                BackupDialogFragment.class.getSimpleName());

                        overwriteHintDialogFragment.setupNegativeButton("No");
                        overwriteHintDialogFragment.setupPositiveButton("Yes");

                        ft.replace(R.id.alert_dialog_container, overwriteHintDialogFragment, overWriteHintDialogTag)
                                .commit();

                    } else {
                        dismiss();
                        if (BackupSaveVia.DROPBOX != mSpinnerSaveFileVia.getSelectedItemPosition()) {
                            new BackupAsyncTask(getActivity(), mBackupNoteBook.getUUID(), mDoSaveCurrent, mDeleteAfterBackup).execute(mBackupPath);
                        } else {
                            String fileName = mEtFileName.getText().toString() + ".note";
                            new UploadDropboxSingleFileTask(getActivity(), mBackupNoteBook.getUUID(), mDeleteAfterBackup).execute(fileName);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void updateExportPath(String nameStr) {
        String formatStr = ".note";
        switch (mSpinnerSaveFileVia.getSelectedItemPosition()) {
            case BackupSaveVia.INTERNAL:
                mBackupPath = Global.DIRECTORY_SDCARD_NOTE + "/" + nameStr + formatStr;
                break;
            case BackupSaveVia.EXTERNAL:
                mBackupPath = Global.DIRECTORY_EXTERNALSD_NOTE + "/" + nameStr + formatStr;
                break;
            case BackupSaveVia.EMAIL:
                mBackupPath = Global.PATH_SDCARD + Global.MAIL_FILE_TEMP_DIR + "/" + nameStr + formatStr;
                break;
            default:
                break;
        }
        updateStorageInfo();
    }

    Runnable invalidateViewToCleanSpinnerGhosting = new Runnable() {
        @Override
        public void run() {
            mView.invalidate(PenEventNTX.UPDATE_MODE_PARTIAL_GL16);
        }
    };

    private boolean isFileExists() {
        switch (mSpinnerSaveFileVia.getSelectedItemPosition()) {
            case BackupSaveVia.INTERNAL:
            case BackupSaveVia.EXTERNAL:
                File file = new File(mBackupPath);
                return file.exists();
            case BackupSaveVia.DROPBOX:
                String fileName = mEtFileName.getText().toString() + ".note";
                return mDropboxFileNameList.contains(fileName);
            case BackupSaveVia.EMAIL:
            default:
                return false;
        }
    }

    private void showInputMethod() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    private void hideInputMethod() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mEtFileName.getWindowToken(), 0);
        }
    }

    private boolean isWifiConnected() {
        ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = null;
        if (connManager != null) {
            wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        }
        return wifi != null && wifi.isConnected();
    }

    private void updateDropboxAccountInfo(boolean isSignIn) {
        if (isSignIn)
            initDropboxFileList();
        else {
            mDropboxAccountStr = "";
            updateStorageInfo();
        }
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

    private double getStorageAvailableSpace(int storage) {
        StatFs stat;
        if (BackupSaveVia.INTERNAL == storage)
            stat = new StatFs(Global.PATH_SDCARD);
        else
            stat = new StatFs(Global.PATH_EXTERNALSD);
        return ((double) stat.getBlockSize() * (double) stat.getAvailableBlocks());
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

    private void updateStorageInfo() {
        int filePathVisibility = View.GONE;
        String filePathStr = mBackupPath;
        if (mBackupPath.substring(0, 4).equalsIgnoreCase("/mnt"))
            filePathStr = mBackupPath.substring(4);
        mTvFilePath.setText(filePathStr);

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
                filePathVisibility = View.VISIBLE;
                dropboxAccountVisibility = View.GONE;
                dropboxSignInVisibility = View.GONE;
                dropboxSignOutVisibility = View.GONE;
                spaceInfoVisibility = View.VISIBLE;

                freeSpace = getStorageAvailableSpace(BackupSaveVia.INTERNAL);
                break;
            case BackupSaveVia.EXTERNAL:
                filePathVisibility = View.VISIBLE;
                dropboxAccountVisibility = View.GONE;
                dropboxSignInVisibility = View.GONE;
                dropboxSignOutVisibility = View.GONE;
                spaceInfoVisibility = View.VISIBLE;

                freeSpace = getStorageAvailableSpace(BackupSaveVia.EXTERNAL);
                break;
            case BackupSaveVia.DROPBOX:
                filePathVisibility = View.GONE;
                dropboxAccountVisibility = View.VISIBLE;
                dropboxSignInVisibility = !mIsDropboxSyncing ? (mIsGotAccountFreeSpace ? View.GONE : View.VISIBLE) : View.GONE;
                dropboxSignOutVisibility = !mIsDropboxSyncing ? (mIsGotAccountFreeSpace ? View.VISIBLE : View.GONE) : View.GONE;
                spaceInfoVisibility = mIsGotAccountFreeSpace ? View.VISIBLE : View.INVISIBLE;

                if (mIsGotAccountFreeSpace)
                    freeSpace = mAccountFreeSpace;
                break;
            case BackupSaveVia.EMAIL:
                filePathVisibility = View.GONE;
                dropboxAccountVisibility = View.GONE;
                dropboxSignInVisibility = View.GONE;
                dropboxSignOutVisibility = View.GONE;
                spaceInfoVisibility = View.GONE;

                freeSpace = getStorageAvailableSpace(BackupSaveVia.INTERNAL);
                break;
        }
        mTvFilePath.setVisibility(filePathVisibility);
        mTvDropboxAccount.setVisibility(dropboxAccountVisibility);
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
            mTvNoteSize.setText(String.format("(%s)", getSizeString(mNoteSize)));

            if (freeSpace > mNoteSize) {
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
                mTvFilePath.setText(getString(R.string.check_network));
                mLayoutEmailTempNotEnoughSpaceHint.setVisibility(View.GONE);
                btnBackupOkEnable = false;
            } else {
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
        hideInputMethod();
        dbx.unregisterOnMetaFileListLoadedListener();
        dbx.unRegisterTrySignInFinishListener();
        mGetSelectedBackupFileSizeTask.cancel(true);
        super.dismiss();
    }
}
