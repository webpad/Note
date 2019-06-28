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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.dropbox.android.Dropbox;
import com.dropbox.android.UploadDropboxConvertFileTask;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import name.vbraun.lib.pen.Hardware;
import name.vbraun.lib.pen.PenEventNTX;
import ntx.note.Global;
import ntx.note.data.Book;
import ntx.note.export.ConvertAsyncTask.ConvertRange;
import ntx.note.export.ConvertAsyncTask.ConvertType;
import ntx.note2.R;
import utility.CustomDialogFragment;

import static android.content.Context.MODE_PRIVATE;
import static ntx.note.Global.STRING_GB;
import static ntx.note.Global.STRING_KB;
import static ntx.note.Global.STRING_MB;

public class ConvertDialogFragment extends CustomDialogFragment {

    private @interface ConvertSaveVia {
        int INTERNAL = 0;
        int EXTERNAL = 1;
        int DROPBOX = 2;
        int E_MAIL = 3;
        int SLEEP_SCREEN = 4;
        int POWER_OFF_SCREEN = 5;
    }

    private @interface ConvertSaveViaNoExtSD {
        int INTERNAL = 0;
        int DROPBOX = 1;
        int E_MAIL = 2;
        int SLEEP_SCREEN = 3;
        int POWER_OFF_SCREEN = 4;
    }

    private static String ARGUMENT_KEY_UUID = "uuid";

    private static String[] STRING_ARRAY_SAVE_FILE_VIA;

    private EditText mEtFileName;
    private RadioGroup mRadioGroupOutputFormat;
    private RadioGroup mRadioGroupRange;
    private RadioButton mBtnRangeWholeNote;
    private CheckBox mCkbIncludePageBackground;
    private MySpinner mSpinnerSaveFileVia;
    private TextView mTvFilePath;
    private TextView mTvDropboxAccount;
    private TextView mTvDropboxSignIn;
    private TextView mTvDropboxSignOut;
    private TextView mTvFreeSpace;
    private Button mBtnConvertOk;

    private Book mBook;
    private ArrayAdapter<CharSequence> mSaveFileViaValuesAdapter;

    private String mFormatStr;
    private String mExportPath;
    private int mConvertType = ConvertType.PDF;
    private int mConvertRange = ConvertRange.CURRENT_PAGE;
    private int mSaveStateConvertType = mConvertType;
    private int mSaveStateConvertRange = mConvertRange;
    private boolean mSaveStateIncludePageBackground = true;
    private int mSaveStateConvertSaveFileVia = ConvertSaveVia.INTERNAL;

    private List<String> mDropboxFileNameList = new ArrayList<>();
    private String mDropboxAccountStr;
    private long mAccountFreeSpace = 0;
    private boolean mIsDropboxSyncing = true;
    private boolean mIsGotAccountInfo = false;
    private Dropbox dbx;
    private View mView;
    private Handler mHandler = new Handler();

    public static ConvertDialogFragment newInstance(UUID uuid) {
        ConvertDialogFragment fragment = new ConvertDialogFragment();
        Bundle args = new Bundle();

        args.putString(ARGUMENT_KEY_UUID, uuid.toString());

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String uuidString = getArguments().getString(ARGUMENT_KEY_UUID, "");

        mBook = new Book(UUID.fromString(uuidString), false);
        dbx = new Dropbox(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.file_convert_to_xxx_dialog, container, false);
        initView(v);
        mView = v;
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mEtFileName.setText(mBook.getTitle()); // always set content value as title
        mEtFileName.setSelectAllOnFocus(true);
        // Restore state
        mConvertType = mSaveStateConvertType;
        mConvertRange = mSaveStateConvertRange;
        setViewValue(mSaveStateConvertType, mSaveStateConvertRange, mSaveStateIncludePageBackground,
                mSaveStateConvertSaveFileVia);
        if (Hardware.hasExternalSDCard())
            updateExportPath(mEtFileName.getText().toString());
        else
            updateExportPathNoExtSD(mEtFileName.getText().toString());
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void dismiss() {
        // Clear focus for next time on focus could be selected all.
        dbx.unregisterOnMetaFileListLoadedListener();
        dbx.unRegisterTrySignInFinishListener();
        mEtFileName.setSelectAllOnFocus(false);
        mEtFileName.clearFocus();
        super.dismiss();
    }

    private void initView(View v) {
        mBtnConvertOk = (Button) v.findViewById(R.id.btn_convert_ok);
        Button btnConvertCancel = (Button) v.findViewById(R.id.btn_convert_cancel);

        mEtFileName = (EditText) v.findViewById(R.id.et_convert_name);
        mRadioGroupOutputFormat = (RadioGroup) v.findViewById(R.id.radio_group_output_format);
        mRadioGroupRange = (RadioGroup) v.findViewById(R.id.radio_group_range);
        mBtnRangeWholeNote = (RadioButton) v.findViewById(R.id.btn_range_whole_note);
        mSpinnerSaveFileVia = (MySpinner) v.findViewById(R.id.sp_save_file_via);
        mCkbIncludePageBackground = (CheckBox) v.findViewById(R.id.ckb_include_page_background);
        mTvFilePath = (TextView) v.findViewById(R.id.tv_file_path);
        mTvDropboxAccount = (TextView) v.findViewById(R.id.tv_dropbox_account);
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
        mTvFreeSpace = (TextView) v.findViewById(R.id.tv_free_space);

        mEtFileName.setText(mBook.getTitle());
        mEtFileName.setSelectAllOnFocus(true);
        mEtFileName.addTextChangedListener(edtFileNameTextWatcher);
        mRadioGroupOutputFormat.setOnCheckedChangeListener(onOutputFormatCheckedChangeListener);

        mRadioGroupRange.setOnCheckedChangeListener(onRangeCheckedChangeListener);

        STRING_ARRAY_SAVE_FILE_VIA = Hardware.hasExternalSDCard()
                ? getResources().getStringArray(R.array.convert_via_entries)
                : getResources().getStringArray(R.array.convert_via_entries_no_extsd);
        LinkedList<String> via_values = new LinkedList<String>();
        mSaveFileViaValuesAdapter = new ArrayAdapter(getActivity(), R.layout.cinny_ui_spinner, via_values);
        mSaveFileViaValuesAdapter.addAll(STRING_ARRAY_SAVE_FILE_VIA);
        mSaveFileViaValuesAdapter.setDropDownViewResource(R.layout.cinny_ui_spinner_item);
        mSpinnerSaveFileVia.setAdapter(mSaveFileViaValuesAdapter);
        mSpinnerSaveFileVia.setOnItemSelectedEvenIfUnchangedListener(onSaveFileViaSpinnerItemSelectedListener);

        btnConvertCancel.setOnClickListener(onBtnClickListener);
        mBtnConvertOk.setOnClickListener(onBtnClickListener);

        setViewValue(ConvertType.PDF, ConvertRange.CURRENT_PAGE, true, ConvertSaveVia.INTERNAL);
    }

    private void setViewValue(int convertType, int convertRange, boolean includeBackground, int convertSaveVia) {

        if (ConvertType.PDF == convertType)
            mRadioGroupOutputFormat.check(R.id.btn_format_pdf);
        else
            mRadioGroupOutputFormat.check(R.id.btn_format_png);

        if (ConvertRange.CURRENT_PAGE == convertRange)
            mRadioGroupRange.check(R.id.btn_range_current_page);
        else
            mRadioGroupRange.check(R.id.btn_range_whole_note);

        mCkbIncludePageBackground.setChecked(includeBackground);

        mSpinnerSaveFileVia.setSelection(convertSaveVia);
    }

    private TextWatcher edtFileNameTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            if (Hardware.hasExternalSDCard())
                updateExportPath(s.toString());
            else
                updateExportPathNoExtSD(s.toString());
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(final CharSequence s, int start, int before, int count) {
        }

    };

    private RadioGroup.OnCheckedChangeListener onOutputFormatCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {

            if (Hardware.hasExternalSDCard()) {
                if (mSpinnerSaveFileVia.getSelectedItemPosition() > ConvertSaveVia.EXTERNAL)
                    mSpinnerSaveFileVia.setSelection(ConvertSaveVia.INTERNAL);

                mSaveFileViaValuesAdapter.remove(STRING_ARRAY_SAVE_FILE_VIA[ConvertSaveVia.SLEEP_SCREEN]);
                mSaveFileViaValuesAdapter.remove(STRING_ARRAY_SAVE_FILE_VIA[ConvertSaveVia.POWER_OFF_SCREEN]);
            } else {
                if (mSpinnerSaveFileVia.getSelectedItemPosition() > ConvertSaveViaNoExtSD.INTERNAL)
                    mSpinnerSaveFileVia.setSelection(ConvertSaveViaNoExtSD.INTERNAL);

                mSaveFileViaValuesAdapter.remove(STRING_ARRAY_SAVE_FILE_VIA[ConvertSaveViaNoExtSD.SLEEP_SCREEN]);
                mSaveFileViaValuesAdapter.remove(STRING_ARRAY_SAVE_FILE_VIA[ConvertSaveViaNoExtSD.POWER_OFF_SCREEN]);
            }

            if (R.id.btn_format_pdf == checkedId) {
                mBtnRangeWholeNote.setVisibility(View.VISIBLE);
                mConvertType = ConvertType.PDF;
            } else if (R.id.btn_format_png == checkedId) {
                mBtnRangeWholeNote.setVisibility(View.GONE);
                mRadioGroupRange.check(R.id.btn_range_current_page);

                if (Hardware.hasExternalSDCard()) {
                    mSaveFileViaValuesAdapter.add(STRING_ARRAY_SAVE_FILE_VIA[ConvertSaveVia.SLEEP_SCREEN]);
                    mSaveFileViaValuesAdapter.add(STRING_ARRAY_SAVE_FILE_VIA[ConvertSaveVia.POWER_OFF_SCREEN]);
                } else {
                    mSaveFileViaValuesAdapter.add(STRING_ARRAY_SAVE_FILE_VIA[ConvertSaveViaNoExtSD.SLEEP_SCREEN]);
                    mSaveFileViaValuesAdapter.add(STRING_ARRAY_SAVE_FILE_VIA[ConvertSaveViaNoExtSD.POWER_OFF_SCREEN]);
                }

                mConvertType = ConvertType.PNG;
            }
            if (Hardware.hasExternalSDCard())
                updateExportPath(mEtFileName.getText().toString());
            else
                updateExportPathNoExtSD(mEtFileName.getText().toString());
            mSaveFileViaValuesAdapter.notifyDataSetChanged();
        }
    };

    private RadioGroup.OnCheckedChangeListener onRangeCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (R.id.btn_range_current_page == checkedId)
                mConvertRange = ConvertRange.CURRENT_PAGE;
            else if (R.id.btn_range_whole_note == checkedId)
                mConvertRange = ConvertRange.ALL_PAGES;
        }
    };

    private OnItemSelectedListener onSaveFileViaSpinnerItemSelectedListener = new OnItemSelectedListener() {

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (Hardware.hasExternalSDCard())
                updateExportPath(mEtFileName.getText().toString());
            else
                updateExportPathNoExtSD(mEtFileName.getText().toString());
            mHandler.postDelayed(invalidateViewToCleanSpinnerGhosting, 500);
        }
    };

    private Button.OnClickListener onBtnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_convert_cancel:
                    dismiss();
                    break;
                case R.id.btn_convert_ok:
                    if (isFileExists()) {
                        showOverwriteConfirmDialog();
                    } else {
                        if (Hardware.hasExternalSDCard()) {
                            if (ConvertSaveVia.DROPBOX != mSpinnerSaveFileVia.getSelectedItemPosition()) {
                                ConvertAsyncTask convertAsyncTask = new ConvertAsyncTask(getActivity(), mBook.getUUID(), mConvertType, mConvertRange, mCkbIncludePageBackground.isChecked());
                                convertAsyncTask.execute(mExportPath);
                            } else {
                                UploadDropboxConvertFileTask uploadDropboxConvertFileTask = new UploadDropboxConvertFileTask(getActivity(), mBook.getUUID(), mConvertType, mConvertRange, mCkbIncludePageBackground.isChecked());
                                String fileName = mEtFileName.getText() + mFormatStr;
                                uploadDropboxConvertFileTask.execute(fileName);
                            }
                        } else {
                            if (ConvertSaveViaNoExtSD.DROPBOX != mSpinnerSaveFileVia.getSelectedItemPosition()) {
                                ConvertAsyncTask convertAsyncTask = new ConvertAsyncTask(getActivity(), mBook.getUUID(), mConvertType, mConvertRange, mCkbIncludePageBackground.isChecked());
                                convertAsyncTask.execute(mExportPath);
                            } else {
                                UploadDropboxConvertFileTask uploadDropboxConvertFileTask = new UploadDropboxConvertFileTask(getActivity(), mBook.getUUID(), mConvertType, mConvertRange, mCkbIncludePageBackground.isChecked());
                                String fileName = mEtFileName.getText() + mFormatStr;
                                uploadDropboxConvertFileTask.execute(fileName);
                            }
                        }
                        dismiss();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void updateExportPath(String nameStr) {
        mFormatStr = "";
        double freeSpace = 0;
        String freeSpaceStr = "";
        if (R.id.btn_format_pdf == mRadioGroupOutputFormat.getCheckedRadioButtonId())
            mFormatStr = ".pdf";
        else if (R.id.btn_format_png == mRadioGroupOutputFormat.getCheckedRadioButtonId())
            mFormatStr = ".png";

        switch (mSpinnerSaveFileVia.getSelectedItemPosition()) {
            case ConvertSaveVia.INTERNAL:
                mTvFilePath.setVisibility(View.VISIBLE);
                mTvDropboxAccount.setVisibility(View.GONE);
                mTvDropboxSignIn.setVisibility(View.GONE);
                mTvDropboxSignOut.setVisibility(View.GONE);
                if (mFormatStr.equals(".pdf")) {
                    mExportPath = Global.DIRECTORY_SDCARD_BOOK + "/" + nameStr + mFormatStr;
                } else {
                    mExportPath = Global.DIRECTORY_SDCARD_NOTE + "/" + nameStr + mFormatStr;
                }
                mBtnConvertOk.setEnabled(true);
                mBtnConvertOk.setAlpha(1.0f);

                mTvFreeSpace.setVisibility(View.VISIBLE);
                freeSpace = getStorageAvailableSpace(ConvertSaveVia.INTERNAL);
                break;
            case ConvertSaveVia.EXTERNAL:
                mTvFilePath.setVisibility(View.VISIBLE);
                mTvDropboxAccount.setVisibility(View.GONE);
                mTvDropboxSignIn.setVisibility(View.GONE);
                mTvDropboxSignOut.setVisibility(View.GONE);
                if (mFormatStr.equals(".pdf")) {
                    mExportPath = Global.DIRECTORY_EXTERNALSD_BOOK + "/" + nameStr + mFormatStr;
                } else {
                    mExportPath = Global.DIRECTORY_EXTERNALSD_NOTE + "/" + nameStr + mFormatStr;
                }
                mBtnConvertOk.setEnabled(true);
                mBtnConvertOk.setAlpha(1.0f);

                mTvFreeSpace.setVisibility(View.VISIBLE);
                freeSpace = getStorageAvailableSpace(ConvertSaveVia.EXTERNAL);
                break;
            case ConvertSaveVia.DROPBOX:
                mTvFilePath.setVisibility(View.GONE);
                mTvDropboxAccount.setVisibility(View.VISIBLE);
                mTvDropboxSignIn.setVisibility(View.GONE);
                mTvDropboxSignOut.setVisibility(View.GONE);

                if (isWifiConnected()) {
                    if (!mIsGotAccountInfo) {
                        mDropboxAccountStr = getString(R.string.sync_with_dropbox);
                        mIsDropboxSyncing = true;
                        dbx.trySignIn(new Dropbox.TrySignInFinishListener() {
                            @Override
                            public void onTryFinished(boolean isSignIn) {
                                mIsDropboxSyncing = false;
                                updateDropboxAccountInfo(isSignIn);
                            }
                        });
                        mBtnConvertOk.setEnabled(false);
                        mBtnConvertOk.setAlpha(0.2f);
                    } else {
                        boolean isSignIn = !mDropboxAccountStr.isEmpty();
                        mTvDropboxSignIn.setVisibility(isSignIn ? View.GONE : View.VISIBLE);
                        mTvDropboxSignOut.setVisibility(isSignIn ? View.VISIBLE : View.GONE);
                        if (isSignIn) {
                            mBtnConvertOk.setEnabled(true);
                            mBtnConvertOk.setAlpha(1.0f);
                        } else {
                            mBtnConvertOk.setEnabled(false);
                            mBtnConvertOk.setAlpha(0.2f);
                        }
                    }
                } else {
                    mDropboxAccountStr = getString(R.string.check_network);
                    mBtnConvertOk.setEnabled(false);
                    mBtnConvertOk.setAlpha(0.2f);
                }
                mTvDropboxAccount.setText(mDropboxAccountStr);

                if (mIsGotAccountInfo) {
                    freeSpace = mAccountFreeSpace;
                    mTvFreeSpace.setVisibility(View.VISIBLE);
                } else
                    mTvFreeSpace.setVisibility(View.INVISIBLE);
                break;
            case ConvertSaveVia.E_MAIL:
                mTvFilePath.setVisibility(View.VISIBLE);
                mTvDropboxAccount.setVisibility(View.GONE);
                mTvDropboxSignIn.setVisibility(View.GONE);
                mTvDropboxSignOut.setVisibility(View.GONE);
                boolean wifi = isWifiConnected();
                mExportPath = wifi ? Global.PATH_SDCARD + Global.MAIL_FILE_TEMP_DIR + "/" + nameStr + mFormatStr : getString(R.string.check_network);
                if (wifi) {
                    mBtnConvertOk.setEnabled(true);
                    mBtnConvertOk.setAlpha(1.0f);
                } else {
                    mBtnConvertOk.setEnabled(false);
                    mBtnConvertOk.setAlpha(0.2f);
                }

                mTvFreeSpace.setVisibility(View.VISIBLE);
                freeSpace = getStorageAvailableSpace(ConvertSaveVia.INTERNAL);
                break;
            case ConvertSaveVia.SLEEP_SCREEN:
                mTvFilePath.setVisibility(View.VISIBLE);
                mTvDropboxAccount.setVisibility(View.GONE);
                mTvDropboxSignIn.setVisibility(View.GONE);
                mTvDropboxSignOut.setVisibility(View.GONE);
                mExportPath = Global.DIRECTORY_SDCARD_SLEEP + "/" + nameStr + mFormatStr;
                mBtnConvertOk.setEnabled(true);
                mBtnConvertOk.setAlpha(1.0f);

                freeSpace = getStorageAvailableSpace(ConvertSaveVia.INTERNAL);
                break;
            case ConvertSaveVia.POWER_OFF_SCREEN:
                mTvFilePath.setVisibility(View.VISIBLE);
                mTvDropboxAccount.setVisibility(View.GONE);
                mTvDropboxSignIn.setVisibility(View.GONE);
                mTvDropboxSignOut.setVisibility(View.GONE);
                mExportPath = Global.DIRECTORY_SDCARD_POWEROFF + "/" + nameStr + mFormatStr;
                mBtnConvertOk.setEnabled(true);
                mBtnConvertOk.setAlpha(1.0f);

                mTvFreeSpace.setVisibility(View.VISIBLE);
                freeSpace = getStorageAvailableSpace(ConvertSaveVia.INTERNAL);
                break;
            default:
                break;
        }

        // hide "/mnt" string
        String filePathStr = mExportPath;
        if (mExportPath.substring(0, 4).equalsIgnoreCase("/mnt"))
            filePathStr = mExportPath.substring(4);

        mTvFilePath.setText(filePathStr);

        freeSpaceStr = String.format("%s %s",
                getSizeString(freeSpace),
                getString(R.string.dropbox_free_space));
        mTvFreeSpace.setText(freeSpaceStr);
    }

    private void updateExportPathNoExtSD(String nameStr) {
        mFormatStr = "";
        double freeSpace = 0;
        String freeSpaceStr = "";
        if (R.id.btn_format_pdf == mRadioGroupOutputFormat.getCheckedRadioButtonId())
            mFormatStr = ".pdf";
        else if (R.id.btn_format_png == mRadioGroupOutputFormat.getCheckedRadioButtonId())
            mFormatStr = ".png";

        switch (mSpinnerSaveFileVia.getSelectedItemPosition()) {
            case ConvertSaveViaNoExtSD.INTERNAL:
                mTvFilePath.setVisibility(View.VISIBLE);
                mTvDropboxAccount.setVisibility(View.GONE);
                mTvDropboxSignIn.setVisibility(View.GONE);
                mTvDropboxSignOut.setVisibility(View.GONE);
                if (mFormatStr.equals(".pdf")) {
                    mExportPath = Global.DIRECTORY_SDCARD_BOOK + "/" + nameStr + mFormatStr;
                } else {
                    mExportPath = Global.DIRECTORY_SDCARD_NOTE + "/" + nameStr + mFormatStr;
                }
                mBtnConvertOk.setEnabled(true);
                mBtnConvertOk.setAlpha(1.0f);

                mTvFreeSpace.setVisibility(View.VISIBLE);
                freeSpace = getStorageAvailableSpace(ConvertSaveViaNoExtSD.INTERNAL);
                break;
            case ConvertSaveViaNoExtSD.DROPBOX:
                mTvFilePath.setVisibility(View.GONE);
                mTvDropboxAccount.setVisibility(View.VISIBLE);
                mTvDropboxSignIn.setVisibility(View.GONE);
                mTvDropboxSignOut.setVisibility(View.GONE);

                if (isWifiConnected()) {
                    if (!mIsGotAccountInfo) {
                        mDropboxAccountStr = getString(R.string.sync_with_dropbox);
                        mIsDropboxSyncing = true;
                        dbx.trySignIn(new Dropbox.TrySignInFinishListener() {
                            @Override
                            public void onTryFinished(boolean isSignIn) {
                                mIsDropboxSyncing = false;
                                updateDropboxAccountInfo(isSignIn);
                            }
                        });
                        mBtnConvertOk.setEnabled(false);
                        mBtnConvertOk.setAlpha(0.2f);
                    } else {
                        boolean isSignIn = !mDropboxAccountStr.isEmpty();
                        mTvDropboxSignIn.setVisibility(isSignIn ? View.GONE : View.VISIBLE);
                        mTvDropboxSignOut.setVisibility(isSignIn ? View.VISIBLE : View.GONE);
                        if (isSignIn) {
                            mBtnConvertOk.setEnabled(true);
                            mBtnConvertOk.setAlpha(1.0f);
                        } else {
                            mBtnConvertOk.setEnabled(false);
                            mBtnConvertOk.setAlpha(0.2f);
                        }
                    }
                } else {
                    mDropboxAccountStr = getString(R.string.check_network);
                    mBtnConvertOk.setEnabled(false);
                    mBtnConvertOk.setAlpha(0.2f);
                }
                mTvDropboxAccount.setText(mDropboxAccountStr);

                if (mIsGotAccountInfo) {
                    freeSpace = mAccountFreeSpace;
                    mTvFreeSpace.setVisibility(View.VISIBLE);
                } else
                    mTvFreeSpace.setVisibility(View.INVISIBLE);
                break;
            case ConvertSaveViaNoExtSD.E_MAIL:
                mTvFilePath.setVisibility(View.VISIBLE);
                mTvDropboxAccount.setVisibility(View.GONE);
                mTvDropboxSignIn.setVisibility(View.GONE);
                mTvDropboxSignOut.setVisibility(View.GONE);
                boolean wifi = isWifiConnected();
                mExportPath = wifi ? Global.PATH_SDCARD + Global.MAIL_FILE_TEMP_DIR + "/" + nameStr + mFormatStr : getString(R.string.check_network);
                if (wifi) {
                    mBtnConvertOk.setEnabled(true);
                    mBtnConvertOk.setAlpha(1.0f);
                } else {
                    mBtnConvertOk.setEnabled(false);
                    mBtnConvertOk.setAlpha(0.2f);
                }

                mTvFreeSpace.setVisibility(View.VISIBLE);
                freeSpace = getStorageAvailableSpace(ConvertSaveViaNoExtSD.INTERNAL);
                break;
            case ConvertSaveViaNoExtSD.SLEEP_SCREEN:
                mTvFilePath.setVisibility(View.VISIBLE);
                mTvDropboxAccount.setVisibility(View.GONE);
                mTvDropboxSignIn.setVisibility(View.GONE);
                mTvDropboxSignOut.setVisibility(View.GONE);
                mExportPath = Global.DIRECTORY_SDCARD_SLEEP + "/" + nameStr + mFormatStr;
                mBtnConvertOk.setEnabled(true);
                mBtnConvertOk.setAlpha(1.0f);

                freeSpace = getStorageAvailableSpace(ConvertSaveViaNoExtSD.INTERNAL);
                break;
            case ConvertSaveViaNoExtSD.POWER_OFF_SCREEN:
                mTvFilePath.setVisibility(View.VISIBLE);
                mTvDropboxAccount.setVisibility(View.GONE);
                mTvDropboxSignIn.setVisibility(View.GONE);
                mTvDropboxSignOut.setVisibility(View.GONE);
                mExportPath = Global.DIRECTORY_SDCARD_POWEROFF + "/" + nameStr + mFormatStr;
                mBtnConvertOk.setEnabled(true);
                mBtnConvertOk.setAlpha(1.0f);

                mTvFreeSpace.setVisibility(View.VISIBLE);
                freeSpace = getStorageAvailableSpace(ConvertSaveViaNoExtSD.INTERNAL);
                break;
            default:
                break;
        }

        // hide "/mnt" string
        String filePathStr = mExportPath;
        if (mExportPath.substring(0, 4).equalsIgnoreCase("/mnt"))
            filePathStr = mExportPath.substring(4);

        mTvFilePath.setText(filePathStr);

        freeSpaceStr = String.format("%s %s",
                getSizeString(freeSpace),
                getString(R.string.dropbox_free_space));
        mTvFreeSpace.setText(freeSpaceStr);
    }

    Runnable invalidateViewToCleanSpinnerGhosting = new Runnable() {
        @Override
        public void run() {
            mView.invalidate(PenEventNTX.UPDATE_MODE_PARTIAL_GL16);
        }
    };

    private boolean isWifiConnected() {
        ConnectivityManager connManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = null;
        if (connManager != null) {
            wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        }
        return wifi != null && wifi.isConnected();
    }

    private void updateDropboxAccountInfo(boolean isSignIn) {
        if (isSignIn) {
            initDropboxFileList();

            SharedPreferences prefs = getActivity().getSharedPreferences(Global.ACCESS_KEY, MODE_PRIVATE);
            mDropboxAccountStr = prefs.getString(Global.ACCESS_USER_Email, "");
            mAccountFreeSpace = prefs.getLong(Global.ACCESS_USER_USED_FREE_SPACE, 0);
            String freeSpaceStr = String.format("%s %s",
                    getSizeString(mAccountFreeSpace),
                    getString(R.string.dropbox_free_space));
            mTvFreeSpace.setText(freeSpaceStr);
            mTvFreeSpace.setVisibility(View.VISIBLE);
            mTvDropboxSignOut.setVisibility(View.VISIBLE);
            mBtnConvertOk.setEnabled(true);
            mBtnConvertOk.setAlpha(1.0f);
        } else {
            mIsGotAccountInfo = true;
            mDropboxAccountStr = "";
            mTvDropboxSignIn.setVisibility(View.VISIBLE);
            mBtnConvertOk.setEnabled(false);
            mBtnConvertOk.setAlpha(0.2f);
        }
        mTvDropboxAccount.setText(mDropboxAccountStr);
    }

    private void initDropboxFileList() {
        mDropboxFileNameList.clear();
        dbx.registerOnMetaFileListLoadedListener(new Dropbox.OnMetadataFileListLoadedListener() {
            @Override
            public void onMetaFileListLoaded(List<Metadata> metadataFileList) {
                for (Metadata metadata : metadataFileList) {
                    mDropboxFileNameList.add(metadata.getName());
                }
                mIsGotAccountInfo = true;
            }
        });
        dbx.getDropboxFileName();
    }

    private boolean isFileExists() {
        if (Hardware.hasExternalSDCard())
            switch (mSpinnerSaveFileVia.getSelectedItemPosition()) {
                case ConvertSaveVia.INTERNAL:
                case ConvertSaveVia.EXTERNAL:
                case ConvertSaveVia.SLEEP_SCREEN:
                case ConvertSaveVia.POWER_OFF_SCREEN:
                    File file = new File(mExportPath);
                    return file.exists();
                case ConvertSaveVia.DROPBOX:
                    String fileName = mEtFileName.getText().toString() + mFormatStr;
                    return mDropboxFileNameList.contains(fileName);
                case ConvertSaveVia.E_MAIL:
                default:
                    return false;
            }
        else
            switch (mSpinnerSaveFileVia.getSelectedItemPosition()) {
                case ConvertSaveViaNoExtSD.INTERNAL:
                case ConvertSaveViaNoExtSD.SLEEP_SCREEN:
                case ConvertSaveViaNoExtSD.POWER_OFF_SCREEN:
                    File file = new File(mExportPath);
                    return file.exists();
                case ConvertSaveViaNoExtSD.DROPBOX:
                    String fileName = mEtFileName.getText().toString() + mFormatStr;
                    return mDropboxFileNameList.contains(fileName);
                case ConvertSaveViaNoExtSD.E_MAIL:
                default:
                    return false;
            }
    }

    private void showOverwriteConfirmDialog() {
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
                        if (Hardware.hasExternalSDCard()) {
                            if (ConvertSaveVia.DROPBOX != mSpinnerSaveFileVia.getSelectedItemPosition()) {
                                ConvertAsyncTask convertAsyncTask = new ConvertAsyncTask(getActivity(), mBook.getUUID(), mConvertType, mConvertRange, mCkbIncludePageBackground.isChecked());
                                convertAsyncTask.execute(mExportPath);
                            } else {
                                UploadDropboxConvertFileTask uploadDropboxConvertFileTask = new UploadDropboxConvertFileTask(getActivity(), mBook.getUUID(), mConvertType, mConvertRange, mCkbIncludePageBackground.isChecked());
                                String fileName = mEtFileName.getText() + mFormatStr;
                                uploadDropboxConvertFileTask.execute(fileName);
                            }
                        } else {
                            if (ConvertSaveViaNoExtSD.DROPBOX != mSpinnerSaveFileVia.getSelectedItemPosition()) {
                                ConvertAsyncTask convertAsyncTask = new ConvertAsyncTask(getActivity(), mBook.getUUID(), mConvertType, mConvertRange, mCkbIncludePageBackground.isChecked());
                                convertAsyncTask.execute(mExportPath);
                            } else {
                                UploadDropboxConvertFileTask uploadDropboxConvertFileTask = new UploadDropboxConvertFileTask(getActivity(), mBook.getUUID(), mConvertType, mConvertRange, mCkbIncludePageBackground.isChecked());
                                String fileName = mEtFileName.getText() + mFormatStr;
                                uploadDropboxConvertFileTask.execute(fileName);
                            }
                        }
                        mBtnConvertOk.setEnabled(false);
                        mBtnConvertOk.setAlpha(0.2f);
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
    }

    private double getStorageAvailableSpace(int storage) {
        StatFs stat;
        if (BackupDialogFragment.BackupSaveVia.INTERNAL == storage)
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
}
