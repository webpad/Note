package ntx.note.bookshelf;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.android.DeleteDropboxListFilesTask;
import com.dropbox.android.DownloadDropboxListFilesTask;
import com.dropbox.android.Dropbox;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.Metadata;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import name.vbraun.filepicker.AsyncTaskResult;
import name.vbraun.filepicker.GetImportListAsyncTask;
import name.vbraun.filepicker.ImportAsyncTask;
import name.vbraun.filepicker.ImportItem;
import name.vbraun.filepicker.ImportItemClickListener;
import name.vbraun.filepicker.ImportItemGroupHeaderView;
import name.vbraun.filepicker.ImportItemGroupItemView;
import name.vbraun.filepicker.ImportItemSingleView;
import name.vbraun.filepicker.ImportItemView;
import name.vbraun.filepicker.ImportListAdapter;
import name.vbraun.filepicker.RestoreItem;
import name.vbraun.filepicker.SearchFileByExtensionAsyncTask;
import name.vbraun.lib.pen.Hardware;
import name.vbraun.lib.pen.PenEventNTX;
import name.vbraun.view.write.Page;
import ntx.draw.nDrawHelper;
import ntx.note.ActivityBase;
import ntx.note.CallbackEvent;
import ntx.note.Global;
import ntx.note.MainPageMorePopupWindow;
import ntx.note.RelativePopupWindow.HorizontalPosition;
import ntx.note.RelativePopupWindow.VerticalPosition;
import ntx.note.bookshelf.NtxHomeActivity.LauncherTab;
import ntx.note.bookshelf.NtxLauncherListItem.NoteType;
import ntx.note.data.Book;
import ntx.note.data.Bookshelf;
import ntx.note.data.TagManager;
import ntx.note.export.AlertDialogButtonClickListener;
import ntx.note.export.AlertDialogFragment;
import ntx.note.export.BackupDialogFragment;
import ntx.note.export.BackupMultipleDialogFragment;
import ntx.note.export.InterruptibleProgressingDialogFragment;
import ntx.note2.R;
import utility.ToggleImageButton;

public class NtxLauncherActivity extends ActivityBase implements AlertDialogButtonClickListener {
    private final static String INTERNAL_PATH = Global.DIRECTORY_SDCARD_NOTE;
    private final static String EXTERNAL_PATH = Global.DIRECTORY_EXTERNALSD_NOTE;
    private final static String SEARCH_FILE_TYPE = ".note";
    private final static String KEY_SORT_TYPE = "sort_type";
    private final static String KEY_PREVIEW_MODE = "preview_mode";
    private final static String DIALOG_TAG_STORAGE_NOTE_ENOUGH = "storage_not_enough";
    private final static int SORT_ASCENDING_TAG = 0;
    private final static int SORT_DESCENDING_TAG = 1;
    private final static long LIMIT_SPACE = 20; // if space >20MB return ture;   <=20MB return false;

    private SimpleDateFormat DropboxSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private float FLYING_GESTURE_HORIZONTAL_MIN_DISTANCE;

    private Context mContext;
    private View mRootLayout;
    private boolean backPressedImmediately = true;

    private RelativeLayout mLayoutRoot;

    // Tab views
    private RelativeLayout[] mBtnTabs = new RelativeLayout[4];

    private LinearLayout mLayoutNormalMode;

    // Manage views
    private RelativeLayout mLayoutManageMode;
    private ImageButton mBtnMore;
    private ToggleImageButton mBtnSelectAll;
    private LinearLayout mBtnRename;
    private LinearLayout mBtnCopy;
    private LinearLayout mBtnBackup;
    private LinearLayout mBtnDelete;
    private TextView mTvSelectedCount;

    // Search views
    private LinearLayout mLayoutSearchMode;
    private ImageButton mBtnSearch;
    private EditText mEtSearchNote;
    private ImageButton mBtnSearchEnter;
    private TextView mTvSearchNotFoundHint;

    // Import backup views
    private LinearLayout mLayoutImportMode;
    private TextView mTvImportListSelectedCount;
    private ImageButton mBtnSearchImport;
    private LinearLayout mLayoutSearchImport;
    private EditText mEtSearchImportFileInput;
    private ImageButton mBtnSearchImportFileEnter;
    private LinearLayout mLayoutDropboxAccount;
    private TextView mTvDropboxAccount;
    private LinearLayout mBtnDeleteSelectedBackupFile;
    private LinearLayout mBtnImportSelectedBackupFile;
    private LinearLayout mLayoutLauncherImportBackup;
    private LinearLayout mLayoutDropboxSyncMessage;
    private LinearLayout mLayoutNoInternetConnectionMessage;
    private LinearLayout mLayoutNotLoggedInDropboxMessage;
    private TextView mTvImportListEmptyHint;
    private LinearLayout mLayoutImportFileList;
    private Spinner mSpinnerImportFileVia;
    private ToggleImageButton mBtnSelectAllImport;
    private ImageButton mBtnImportListSortByName;
    private ImageButton mBtnImportListSortByDate;
    private ImageButton mBtnImportListSortBySize;
    private ImageButton mBtnImportListPageUp;
    private ImageButton mBtnImportListPageDown;
    private TextView mTvImportListCurrentPage;
    private TextView mTvImportListTotalPage;

    // Free Storage views
    private LinearLayout mLayoutFreeStorageMode;
    private LinearLayout mLayoutLauncherFreeStorage;
    private LinearLayout mLayoutFreeStorageList;
    private TextView mTvFreeStorageListSelectedCount;
    private LinearLayout mBtnBackupAndDeleteSelectedFileToFree;
    private LinearLayout mBtnDeleteSelectedFileToFree;
    private ImageButton mBtnFreeStorageListSortByName;
    private ImageButton mBtnFreeStorageListSortByDate;
    private ImageButton mBtnFreeStorageListSortBySize;
    private TextView mTvFreeStorageListCurrentPage;
    private TextView mTvFreeStorageListTotalPage;


    // Page info views
    private RelativeLayout mLayoutPageInfo;
    private TextView mTvPageInfo;

    private LinearLayout mLayoutNoteList;

    private List<NtxLauncherListItem> mLauncherListItems = new ArrayList<>();
    private NtxLauncherListAdapter mLauncherListAdapter;

    private @interface ListSort {
        int DATE_ASCENDING = 1;
        int DATE_DESCENDING = 2;
        int NAME_ASCENDING = 3;
        int NAME_DESCENDING = 4;
        int SIZE_ASCENDING = 5;
        int SIZE_DESCENDING = 6;
    }

    private int mSortImportListProperty = ListSort.DATE_DESCENDING;
    private List<ImportItem> mCurrentPageImportList = new ArrayList<>();
    private List<ImportItem> mInternalImportList = new ArrayList<>();
    private List<ImportItem> mExternalImportList = new ArrayList<>();
    private List<ImportItem> mDropboxImportList = new ArrayList<>();
    private List<Metadata> mDropboxMetadataList = new ArrayList<>();
    private List<ImportItem> mConflictImportList = new ArrayList<>();
    private HashMap<String, String> mUuidTitleHashMap = new HashMap<>();
    private ImportListAdapter mInternalImportListAdapter = new ImportListAdapter(mInternalImportList);
    private ImportListAdapter mExternalImportListAdapter = new ImportListAdapter(mExternalImportList);
    private ImportListAdapter mDropboxImportListAdapter = new ImportListAdapter(mDropboxImportList);
    private int mImportListCurrentPage;
    private int mImportListTotalPage;
    private boolean mIsLoadingExternalBackupFileList = true;
    private boolean mIsLoadingInternalBackupFileList = true;

    private int mSortFreeStorageListProperty = ListSort.DATE_ASCENDING;
    private FreeStorageListAdapter mFreeStorageListAdapter;
    private ToggleImageButton mBtnSelectAllFileToFree;
    private int mFreeStorageListCurrentPage;

    private Handler mHandler = new Handler();

    private InterruptibleProgressingDialogFragment mProgressingDialogFragment;
    private static GestureDetector mGestureDetector;

    public @interface LauncherListType {
        int NORMAL = 0;
        int MANAGE = 1;
        int SEARCH = 2;
        int MANAGE_SEARCH = 3;
        int IMPORT = 4;
        int FREE_STORAGE = 5;
    }

    private int mLauncherListType = LauncherListType.NORMAL;

    private @interface PreviewMode {
        int TITLE = 0;
        int THUMBNAIL = 1;
    }

    private @interface ImportFileVia {
        int INTERNAL = 0;
        int EXTERNAL = 1;
        int DROPBOX = 2;
    }

    private int mPreviewMode = PreviewMode.TITLE;
    private int mPreviewOrder = Bookshelf.PreviewOrder.LAST_MODIFIED;

    private Dropbox dbx;

    private boolean mIsNewNoteCreating = false;
    private EventBus mEventBus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();

        mRootLayout = getLayoutInflater().inflate(R.layout.ntx_launcher_activity, null);
        setContentView(mRootLayout);


//        Global.PACKAGE_DATA_DIR = getApplicationInfo().dataDir;
//		Global.getExternalMounts(); // set storage path

        Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        FLYING_GESTURE_HORIZONTAL_MIN_DISTANCE = Hardware.getCMtoPixel(Global.SWIPE_DISTANCE_THRESHOLD_BY_CM, metrics);

        setPath();

        initView();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        setTabSelected(LauncherTab.NOTE);
    }

    public void listFilesForFolder(final File folder) {
        ArrayList<UUID> realPathUUID = new ArrayList<>();
        ArrayList<UUID> bookPathUUID = new ArrayList<>();
        if (folder.exists()) {
            for (final File fileEntry : folder.listFiles()) {
                if (fileEntry.isDirectory() && fileEntry.getName().startsWith(Global.DATA_START)) {
                    realPathUUID.add(UUID.fromString(fileEntry.getName().replace(Global.DATA_START, "")));
                }
            }
            for (Book b : Bookshelf.getInstance().getBookList()) {
                bookPathUUID.add(b.getUUID());
            }

            bookPathUUID.removeAll(realPathUUID);

            for (UUID uuid : bookPathUUID) {
                Bookshelf.getInstance().deleteStorageBook(uuid);
            }
            Bookshelf.getInstance().saveDateJsonFile();
        }
    }

    @Override
    protected void onResume() {
        listFilesForFolder(new File(Global.APP_DATA_PACKAGE_FILES_PATH));

        changeEinkControlPermission(true);
        ActivityBase.quillIncRefcount();
        super.onResume();
        Global.closeWaitDialog(this, 1500);

        mEventBus = EventBus.getDefault();
        dbx = new Dropbox(this);

        if (!mEventBus.isRegistered(this))
            mEventBus.register(this);

        //get sort setting
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        mPreviewOrder = settings.getInt(KEY_SORT_TYPE, mPreviewOrder);

        Bookshelf.getInstance().setPreviewOrder(mPreviewOrder);
        Bookshelf.getInstance().sortBookList(true);

        //get preview mode
        mPreviewMode = settings.getInt(KEY_PREVIEW_MODE, mPreviewMode);

        resetRotate();

        setTabSelected(LauncherTab.NOTE);
        changeLauncherListType(this.getIntent().getIntExtra("LauncherListType", LauncherListType.NORMAL));
        Book currentBook = Bookshelf.getInstance().getCurrentBook();
        if (currentBook != null) {
            int page = mLauncherListAdapter.getPageOfItemInCurrentList(currentBook);
            if (mLauncherListAdapter.getCurrentPage() != page)
                mLauncherListAdapter.setCurrentPage(page);
        }
        updateLauncherListView();
        updatePageInfo();

        clearTempFileDir();

        // Always disable the nDraw and 2-Step-Suspend in Home page
        PowerEnhanceSet(0);
        nDrawHelper.NDrawSwitch(false);

        int selectedItemIndex = this.getIntent().getIntExtra("LauncherListItemLongClick", 0);
        if (selectedItemIndex != 0) {
            Book nb = mLauncherListAdapter.getCurrentPageList().get(selectedItemIndex - 1);
            mLauncherListAdapter.setBookSelected(nb.getUUID(), true);
            changeLauncherListType(LauncherListType.MANAGE);
            int page = mLauncherListAdapter.getPageOfItemInCurrentList(nb);
            if (mLauncherListAdapter.getCurrentPage() != page) {
                switchToPage(page);
            }
        }

        doFullRefresh(500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        changeEinkControlPermission(false);
        mIsNewNoteCreating = false;
        mEventBus.unregister(this);
        dbx.unregisterOnMetaFileListLoadedListener();
        dbx.unRegisterTrySignInFinishListener();
        dbx = null;

        ActivityBase.quillDecRefcount();

        //save sort setting/ preview mode
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(KEY_SORT_TYPE, mPreviewOrder);
        editor.putInt(KEY_PREVIEW_MODE, mPreviewMode);
        editor.apply();

    }

    @Override
    public void onBackPressed() {
        // Do nothing when back key pressed in the Launcher
    }

    @Override
    public void onUserInteraction() {
        if (backPressedImmediately) {
            Handler handler = new Handler();
            Runnable afterUserInteraction = new Runnable() {
                @Override
                public void run() {
                    backPressedImmediately = false;
                }
            };
            handler.postDelayed(afterUserInteraction, 250);
        }
        super.onUserInteraction();
    }

    @Override
    public void onPositiveButtonClick(String fragmentTag) {
        Fragment fragment = getFragmentManager().findFragmentByTag(fragmentTag);
        if (fragment != null)
            getFragmentManager().beginTransaction().remove(fragment).commit();
    }

    @Override
    public void onNegativeButtonClick(String fragmentTag) {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CallbackEvent event) {
        switch (event.getMessage()) {
            case CallbackEvent.MORE_SORT:
                showSortBookPopupWindow();
                break;
            case CallbackEvent.MORE_PREVIEW:
                showPreviewModePopupWindow();
                break;
            case CallbackEvent.MORE_IMPORT:
                changeLauncherListType(LauncherListType.IMPORT);
                getInternalBackupFileList();
                getExternalBackupFileList();
                break;
            case CallbackEvent.MORE_MANAGE:
                changeLauncherListType(LauncherListType.MANAGE);
                break;
            case CallbackEvent.IMPORT_NOTE_SUCCESS:
            case CallbackEvent.IMPORT_NOTE_ERROR:
            case CallbackEvent.RENAME_NOTE_DONE:
                changeLauncherListType(LauncherListType.NORMAL);
                break;
            case CallbackEvent.DELETE_DROPBOX_FILES_SUCCESS:
            case CallbackEvent.DELETE_DROPBOX_FILES_FAIL:
            case CallbackEvent.DELETE_DROPBOX_FILES_INTERRUPTED:
                getDropboxBackupFileList();
                break;
            case CallbackEvent.BACKUP_NOTE_SUCCESS:
            case CallbackEvent.UPLOAD_DROPBOX_FILES_SUCCESS:
                changeLauncherListType(LauncherListType.NORMAL);
                break;
            case CallbackEvent.BACKUP_NOTE_EMAIL:
                changeLauncherListType(LauncherListType.NORMAL);
                sendEmail();
                break;
            case CallbackEvent.SAVE_RECENTLY_NOTE_JSON_DONE:
                mLauncherListAdapter.updateList(Bookshelf.getInstance().getBookList());
                mLauncherListAdapter.notifyDataSetChanged();
                updateLauncherListView();
                break;
        }
    }

    private void changeEinkControlPermission(boolean isForNtxAppsOnly) {
        Intent changePermissionIntent = new Intent("ntx.eink_control.CHANGE_PERMISSION");
        changePermissionIntent.putExtra("isPermissionNtxApp", isForNtxAppsOnly);
        sendBroadcast(changePermissionIntent);
    }

    private void initView() {
        mLayoutRoot = (RelativeLayout) findViewById(R.id.layout_root);
        mLayoutNormalMode = (LinearLayout) findViewById(R.id.layout_normal_mode);

        initTabViews();
        initManageViews();
        initSearchViews();
        initPageInfoViews();
        initLauncherListView();
        initImportListView();
        initFreeStorageViews();
    }

    private void initTabViews() {
        RelativeLayout btnTabNote = (RelativeLayout) findViewById(R.id.btn_tab_note);
        RelativeLayout btnTabReader = (RelativeLayout) findViewById(R.id.btn_tab_reader);
        RelativeLayout btnTabCalendar = (RelativeLayout) findViewById(R.id.btn_tab_calendar);
        RelativeLayout btnTabMore = (RelativeLayout) findViewById(R.id.btn_tab_more);
        mBtnTabs[0] = btnTabNote;
        mBtnTabs[1] = btnTabReader;
        mBtnTabs[2] = btnTabCalendar;
        mBtnTabs[3] = btnTabMore;

        for (RelativeLayout btnTab : mBtnTabs) {
            btnTab.setOnClickListener(onTabBtnClickListener);
        }

        setTabSelected(LauncherTab.NOTE);
    }

    private View.OnClickListener onTabBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            switch (v.getId()) {
                case R.id.btn_tab_note:
                    setTabSelected(LauncherTab.NOTE);
                    break;
                case R.id.btn_tab_reader:
                    setTabSelected(LauncherTab.READER);
                    switchToHomerReaderLibrary();
                    break;
                case R.id.btn_tab_calendar:
                    setTabSelected(LauncherTab.CALENDAR);
                    switchToCalendar();
                    break;
                case R.id.btn_tab_more:
                    setTabSelected(LauncherTab.MORE);
                    switchToSettings();
                    break;
            }
        }
    };

    private void setTabSelected(int launcherTab) {
        for (RelativeLayout btnTab : mBtnTabs) {
            btnTab.setSelected(false);
        }
        mBtnTabs[launcherTab].setSelected(true);
    }

    private void initManageViews() {
        mLayoutManageMode = (RelativeLayout) findViewById(R.id.layout_manage_mode);
        mLayoutManageMode.setVisibility(View.INVISIBLE);

        mBtnMore = (ImageButton) findViewById(R.id.btn_more);
        mBtnRename = (LinearLayout) findViewById(R.id.btn_file_rename);
        mBtnCopy = (LinearLayout) findViewById(R.id.btn_file_copy_to);
        mBtnBackup = (LinearLayout) findViewById(R.id.btn_file_backup);
        mBtnDelete = (LinearLayout) findViewById(R.id.btn_file_delete);
        ImageButton btnExitManageMode = (ImageButton) findViewById(R.id.btn_exit_manage_mode);

        mBtnMore.setOnClickListener(onManageBtnClickListener);
        mBtnRename.setOnClickListener(onManageBtnClickListener);
        mBtnCopy.setOnClickListener(onManageBtnClickListener);
        mBtnBackup.setOnClickListener(onManageBtnClickListener);
        mBtnDelete.setOnClickListener(onManageBtnClickListener);
        btnExitManageMode.setOnClickListener(onManageBtnClickListener);

        mTvSelectedCount = (TextView) findViewById(R.id.tv_selected_count);

        LinearLayout layoutSelectAll = (LinearLayout) findViewById(R.id.layout_select_all);
        layoutSelectAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mBtnSelectAll.performClick();
            }
        });

        mBtnSelectAll = (ToggleImageButton) findViewById(R.id.btn_select_all);
        mBtnSelectAll.setOnCheckedChangeListener(new ToggleImageButton.OnCheckedChangedListener() {
            @Override
            public void onCheckedChange(boolean isChecked) {
                mLauncherListAdapter.setListSelected(isChecked);
                updateLauncherListSelectedStatus();
                updateManageViews();
            }
        });
    }

    private void updateManageViews() {
        List<Book> selectedList = mLauncherListAdapter.getSelectedBookList();
        int selectedCount = selectedList.size();
        mTvSelectedCount.setText(String.valueOf(selectedCount));

        mBtnRename.setVisibility(selectedCount > 0 && selectedCount < 2 ? View.VISIBLE : View.INVISIBLE);
        mBtnCopy.setVisibility(selectedCount > 0 ? View.VISIBLE : View.INVISIBLE);
        mBtnBackup.setVisibility(selectedCount > 0 ? View.VISIBLE : View.INVISIBLE);
        mBtnDelete.setVisibility(selectedCount > 0 ? View.VISIBLE : View.INVISIBLE);

        if ((mLauncherListAdapter.getCount() != 0)
                && (selectedCount == mLauncherListAdapter.getCount()))
            mBtnSelectAll.setChecked();
        else
            mBtnSelectAll.setUnchecked();
    }

    private View.OnClickListener onManageBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            switch (v.getId()) {
                case R.id.btn_more:
                    showMainPageMorePopupWindow();
                    break;
                case R.id.btn_file_rename:
                    List<Book> selectedList = mLauncherListAdapter.getSelectedBookList();
                    Book nb = selectedList.get(0);
                    renameSelectedNoteBook(nb);
                    break;
                case R.id.btn_file_copy_to:
                    CopySelectedBookAsyncTask copyNoteBookAsyncTask = new CopySelectedBookAsyncTask();
                    copyNoteBookAsyncTask.execute();
                    break;
                case R.id.btn_file_backup:
                    backupMultiple();
                    break;
                case R.id.btn_file_delete:
                    deleteSelectedNoteBook(mLauncherListAdapter.getSelectedBookList());
                    break;
                case R.id.btn_exit_manage_mode:
                    if (LauncherListType.MANAGE == mLauncherListType)
                        changeLauncherListType(LauncherListType.NORMAL);
                    else if (LauncherListType.MANAGE_SEARCH == mLauncherListType)
                        changeLauncherListType(LauncherListType.SEARCH);
                    break;
            }
        }
    };

    private void initSearchViews() {
        mBtnSearch = (ImageButton) findViewById(R.id.btn_search);
        mBtnSearch.setEnabled(false);
        mBtnSearch.setAlpha(0.2f);
        mBtnSearch.setOnClickListener(onSearchBtnClickListener);

        mLayoutSearchMode = (LinearLayout) findViewById(R.id.layout_search_mode);
        mLayoutSearchMode.setVisibility(View.GONE);

        mEtSearchNote = (EditText) findViewById(R.id.et_search_input);
        mEtSearchNote.setOnKeyListener(onSearchEditTextKeyListener);
        mEtSearchNote.addTextChangedListener(searchEditTextWatcher);
        mEtSearchNote.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b)
                    showInputMethod();
                else
                    hideInputMethod(mEtSearchNote);
            }
        });
        ImageButton btnExitSearchMode = (ImageButton) findViewById(R.id.btn_exit_search_mode);
        ImageButton btnSearchResultClear = (ImageButton) findViewById(R.id.btn_search_result_clear);
        mBtnSearchEnter = (ImageButton) findViewById(R.id.btn_search_enter);
        mBtnSearchEnter.setAlpha(0.2f);
        mBtnSearchEnter.setEnabled(false);
        btnExitSearchMode.setOnClickListener(onSearchBtnClickListener);
        btnSearchResultClear.setOnClickListener(onSearchBtnClickListener);
        mBtnSearchEnter.setOnClickListener(onSearchBtnClickListener);

        mTvSearchNotFoundHint = (TextView) findViewById(R.id.tv_search_result_not_find_hint);
        mTvSearchNotFoundHint.setVisibility(View.GONE);
    }

    private View.OnClickListener onSearchBtnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_search:
                    changeLauncherListType(LauncherListType.SEARCH);
                    break;
                case R.id.btn_exit_search_mode:
                    changeLauncherListType(LauncherListType.NORMAL);
                    break;
                case R.id.btn_search_result_clear:
                    mEtSearchNote.setText("");
                    mTvSearchNotFoundHint.setVisibility(View.GONE);
                    mLauncherListAdapter.updateList(Bookshelf.getInstance().getBookList());
                    mLauncherListAdapter.notifyDataSetChanged();
                    switchToPage(1);
                    break;
                case R.id.btn_search_enter:
                    hideInputMethod(mEtSearchNote);
                    List<Book> newList = Bookshelf.getInstance().getFilterBookList(mEtSearchNote.getText().toString());
                    if (newList.size() > 0)
                        mTvSearchNotFoundHint.setVisibility(View.GONE);
                    else
                        mTvSearchNotFoundHint.setVisibility(View.VISIBLE);

                    mLauncherListAdapter.updateList(newList);
                    mLauncherListAdapter.notifyDataSetChanged();
                    switchToPage(1);
                    break;
            }
        }
    };

    private void initLauncherListView() {
        mGestureDetector = new GestureDetector(this, new ListLayoutFlingListener());
        mLayoutNoteList = (LinearLayout) findViewById(R.id.layout_note_list);
        mLayoutNoteList.setOnTouchListener(NoteListLayoutOnTouchListener);

        NtxLauncherListItem item0 = (NtxLauncherListItem) findViewById(R.id.launcher_list_item0);
        NtxLauncherListItem item1 = (NtxLauncherListItem) findViewById(R.id.launcher_list_item1);
        NtxLauncherListItem item2 = (NtxLauncherListItem) findViewById(R.id.launcher_list_item2);
        NtxLauncherListItem item3 = (NtxLauncherListItem) findViewById(R.id.launcher_list_item3);
        NtxLauncherListItem item4 = (NtxLauncherListItem) findViewById(R.id.launcher_list_item4);
        NtxLauncherListItem item5 = (NtxLauncherListItem) findViewById(R.id.launcher_list_item5);
        NtxLauncherListItem item6 = (NtxLauncherListItem) findViewById(R.id.launcher_list_item6);
        NtxLauncherListItem item7 = (NtxLauncherListItem) findViewById(R.id.launcher_list_item7);
        mLauncherListItems.add(item0);
        mLauncherListItems.add(item1);
        mLauncherListItems.add(item2);
        mLauncherListItems.add(item3);
        mLauncherListItems.add(item4);
        mLauncherListItems.add(item5);
        mLauncherListItems.add(item6);
        mLauncherListItems.add(item7);
        for (int i = 0; i < mLauncherListItems.size(); i++) {
            mLauncherListItems.get(i).setTag(i);
            mLauncherListItems.get(i).setOnItemClickListener(launcherListItemClickListener);
            mLauncherListItems.get(i).registerItemTouchEventListener(NoteListLayoutOnTouchListener);
        }

        mLauncherListAdapter = new NtxLauncherListAdapter(Bookshelf.getInstance().getBookList(), 7);
        switchToPage(1);
    }

    private View.OnTouchListener NoteListLayoutOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            return mGestureDetector.onTouchEvent(motionEvent);
        }
    };

    private NtxLauncherListItem.ItemClickListener launcherListItemClickListener = new NtxLauncherListItem.ItemClickListener() {

        @Override
        public void onClick(Object viewTag) {
            final int index = (int) viewTag;
            if (LauncherListType.SEARCH == mLauncherListType) {
                hideInputMethod(mEtSearchNote);
            }
            Global.openWaitDialog(NtxLauncherActivity.this);
            if (index == 0) {
                if (mIsNewNoteCreating)
                    return;
                createNewNoteBookAndOpen();
            } else {
                if (index > mLauncherListAdapter.getCurrentPageList().size())
                    return;
                Book nb = mLauncherListAdapter.getCurrentPageList().get(index - 1);
                openNoteBook(nb);
            }
        }

        @Override
        public void onLongClick(Object viewTag) {
            if (LauncherListType.NORMAL == mLauncherListType || LauncherListType.SEARCH == mLauncherListType) {
                int index = (int) viewTag - 1;

                Book nb = mLauncherListAdapter.getCurrentPageList().get(index);
                mLauncherListAdapter.setBookSelected(nb.getUUID(), true);

                if (LauncherListType.NORMAL == mLauncherListType)
                    changeLauncherListType(LauncherListType.MANAGE);
                else
                    changeLauncherListType(LauncherListType.MANAGE_SEARCH);

                int page = mLauncherListAdapter.getPageOfItemInCurrentList(nb);
                if (mLauncherListAdapter.getCurrentPage() != page) {
                    switchToPage(page);
                }
            }
        }

        @Override
        public void onCheckedChange(Object viewTag, boolean b) {
            int index = (int) viewTag - 1;
            Book nb = mLauncherListAdapter.getCurrentPageList().get(index);
            mLauncherListAdapter.setBookSelected(nb.getUUID(), b);
            updateManageViews();
        }
    };

    private void initImportListView() {
        mLayoutImportMode = (LinearLayout) findViewById(R.id.layout_import_mode);
        mTvImportListSelectedCount = (TextView) findViewById(R.id.tv_import_list_selected_count);
        mBtnSearchImport = (ImageButton) findViewById(R.id.btn_search_import);
        mBtnSearchImport.setOnClickListener(onSearchImportBtnClickListener);
        mLayoutSearchImport = (LinearLayout) findViewById(R.id.layout_search_import);
        mEtSearchImportFileInput = (EditText) findViewById(R.id.et_search_import_file_input);
        mEtSearchImportFileInput.setOnKeyListener(onSearchImportEditTextKeyListener);
        mEtSearchImportFileInput.addTextChangedListener(searchImportEditTextWatcher);
        mEtSearchImportFileInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b)
                    showInputMethod();
                else
                    hideInputMethod(mEtSearchImportFileInput);
            }
        });
        ImageButton btnExitSearchImport = (ImageButton) findViewById(R.id.btn_exit_search_import);
        mBtnSearchImportFileEnter = (ImageButton) findViewById(R.id.btn_search_import_file_enter);
        btnExitSearchImport.setOnClickListener(onSearchImportBtnClickListener);
        mBtnSearchImportFileEnter.setOnClickListener(onSearchImportBtnClickListener);
        ImageButton btnExitImportMode = (ImageButton) findViewById(R.id.btn_exit_import_mode);
        mLayoutDropboxAccount = (LinearLayout) findViewById(R.id.layout_dropbox_account);
        mTvDropboxAccount = (TextView) findViewById(R.id.tv_dropbox_account);
        TextView tvDropboxSignOut = (TextView) findViewById(R.id.tv_dropbox_sign_out);
        tvDropboxSignOut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dbx.logOut();
                mTvDropboxAccount.setText("");
                mLayoutDropboxAccount.setVisibility(View.GONE);
            }
        });
        mBtnDeleteSelectedBackupFile = (LinearLayout) findViewById(R.id.btn_delete_selected_backup);
        mBtnImportSelectedBackupFile = (LinearLayout) findViewById(R.id.btn_import_selected_backup);
        btnExitImportMode.setOnClickListener(onImportBtnClickListener);
        mBtnDeleteSelectedBackupFile.setOnClickListener(onImportBtnClickListener);
        mBtnImportSelectedBackupFile.setOnClickListener(onImportBtnClickListener);

        mLayoutLauncherImportBackup = (LinearLayout) findViewById(R.id.layout_launcher_import_backup);
        mLayoutDropboxSyncMessage = (LinearLayout) findViewById(R.id.layout_dropbox_sync_message);
        mLayoutNoInternetConnectionMessage = (LinearLayout) findViewById(R.id.layout_no_internet_connect_message);
        TextView tvSetNetwork = (TextView) findViewById(R.id.tv_set_network);
        tvSetNetwork.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToWifiSettings();
            }
        });
        mLayoutNotLoggedInDropboxMessage = (LinearLayout) findViewById(R.id.layout_not_login_in_message);
        TextView tvDropboxSignIn = (TextView) findViewById(R.id.tv_dropbox_sign_in);
        tvDropboxSignIn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dbx.logIn();
            }
        });
        mTvImportListEmptyHint = (TextView) findViewById(R.id.tv_import_list_empty_hint);
        mLayoutImportFileList = (LinearLayout) mLayoutLauncherImportBackup.findViewById(R.id.layout_import_file_list);

        mSpinnerImportFileVia = (Spinner) mLayoutLauncherImportBackup.findViewById(R.id.sp_import_file_via);
        String[] importFileStringArray = Hardware.hasExternalSDCard()
                ? getResources().getStringArray(R.array.import_via_entries)
                : getResources().getStringArray(R.array.import_via_entries_no_extsd);
        LinkedList<String> via_values = new LinkedList<String>();
        ArrayAdapter<CharSequence> importFileViaValuesAdapter = new ArrayAdapter(this, R.layout.spinner_import_file_via, via_values);
        importFileViaValuesAdapter.addAll(importFileStringArray);
        importFileViaValuesAdapter.setDropDownViewResource(R.layout.cinny_ui_spinner_item);
        mSpinnerImportFileVia.setAdapter(importFileViaValuesAdapter);
        mSpinnerImportFileVia.setOnItemSelectedListener(onImportFileViaSpinnerItemSelectedListener);

        LinearLayout layoutSelectAllImportFile = (LinearLayout) mLayoutLauncherImportBackup.findViewById(R.id.layout_select_all_import_file);
        layoutSelectAllImportFile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mBtnSelectAllImport.performClick();
            }
        });
        mBtnSelectAllImport = (ToggleImageButton) mLayoutLauncherImportBackup.findViewById(R.id.btn_select_all_import_file);
        mBtnSelectAllImport.setOnCheckedChangeListener(new ToggleImageButton.OnCheckedChangedListener() {
            @Override
            public void onCheckedChange(boolean value) {
                selectImportListAll(value);
                updateImportListViewSelectStatus();
            }
        });

        mBtnImportListSortByName = (ImageButton) mLayoutLauncherImportBackup.findViewById(R.id.btn_sort_by_name);
        mBtnImportListSortByDate = (ImageButton) mLayoutLauncherImportBackup.findViewById(R.id.btn_sort_by_date);
        mBtnImportListSortBySize = (ImageButton) mLayoutLauncherImportBackup.findViewById(R.id.btn_sort_by_size);
        mBtnImportListSortByName.setTag(new Integer(SORT_ASCENDING_TAG));
        mBtnImportListSortByDate.setTag(new Integer(SORT_DESCENDING_TAG));
        mBtnImportListSortBySize.setTag(new Integer(SORT_ASCENDING_TAG));
        mBtnImportListSortByName.setImageLevel(SORT_ASCENDING_TAG);
        mBtnImportListSortByDate.setImageLevel(SORT_DESCENDING_TAG);
        mBtnImportListSortBySize.setImageLevel(SORT_ASCENDING_TAG);
        mBtnImportListSortByName.setOnClickListener(onImportListSortButtonClickListener);
        mBtnImportListSortByDate.setOnClickListener(onImportListSortButtonClickListener);
        mBtnImportListSortBySize.setOnClickListener(onImportListSortButtonClickListener);
        mBtnImportListSortByDate.setSelected(true);

        mTvImportListCurrentPage = (TextView) mLayoutLauncherImportBackup.findViewById(R.id.tv_import_list_page_index);
        mTvImportListTotalPage = (TextView) mLayoutLauncherImportBackup.findViewById(R.id.tv_import_list_page_total);
        mBtnImportListPageUp = (ImageButton) mLayoutLauncherImportBackup.findViewById(R.id.btn_import_list_page_up);
        mBtnImportListPageDown = (ImageButton) mLayoutLauncherImportBackup.findViewById(R.id.btn_import_list_page_down);
        mBtnImportListPageUp.setOnClickListener(onImportListPageButtonClickListener);
        mBtnImportListPageDown.setOnClickListener(onImportListPageButtonClickListener);
    }

    private EditText.OnKeyListener onSearchImportEditTextKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                utility.StringIllegal.checkFirstSpaceChar(mEtSearchImportFileInput);
                utility.StringIllegal.checkIllegalChar(mEtSearchImportFileInput);

                if (mEtSearchImportFileInput.getText().toString().equals("")) {
                    mBtnSearchImportFileEnter.setAlpha(0.2f);
                    mBtnSearchImportFileEnter.setEnabled(false);
                    return false;
                }

                String keyword = removeLastSpace(mEtSearchImportFileInput.getText().toString());
                mEtSearchImportFileInput.setText(keyword);
                mBtnSearchImportFileEnter.performClick();
            }
            return false;
        }
    };

    private TextWatcher searchImportEditTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.toString().equals("") || s.toString().isEmpty()) {
                mBtnSearchImportFileEnter.setAlpha(0.2f);
                mBtnSearchImportFileEnter.setEnabled(false);
                return;
            }

            utility.StringIllegal.checkFirstSpaceChar(mEtSearchImportFileInput);
            utility.StringIllegal.checkIllegalChar(mEtSearchImportFileInput);

            if (s.toString().equals(" ")) {
                mEtSearchImportFileInput.setText("");
                mBtnSearchImportFileEnter.setAlpha(0.2f);
                mBtnSearchImportFileEnter.setEnabled(false);
                return;
            }

            mBtnSearchImportFileEnter.setAlpha(1.0f);
            mBtnSearchImportFileEnter.setEnabled(true);
        }
    };

    private View.OnClickListener onSearchImportBtnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_search_import:
                    mBtnSearchImport.setVisibility(View.GONE);
                    mLayoutSearchImport.setVisibility(View.VISIBLE);
                    mEtSearchImportFileInput.requestFocus();
                    showInputMethod();
                    break;
                case R.id.btn_exit_search_import:
                    mEtSearchImportFileInput.setText("");

                    mInternalImportListAdapter.updateList(mInternalImportList);
                    mInternalImportListAdapter.clearAllItemSelect();
                    mInternalImportListAdapter.notifyDataSetChanged();
                    mInternalImportListAdapter.setCurrentPage(1);

                    mExternalImportListAdapter.updateList(mExternalImportList);
                    mExternalImportListAdapter.clearAllItemSelect();
                    mExternalImportListAdapter.notifyDataSetChanged();
                    mExternalImportListAdapter.setCurrentPage(1);

                    mDropboxImportListAdapter.updateList(mDropboxImportList);
                    mDropboxImportListAdapter.clearAllItemSelect();
                    mDropboxImportListAdapter.notifyDataSetChanged();
                    mDropboxImportListAdapter.setCurrentPage(1);

                    mLayoutSearchImport.setVisibility(View.GONE);
                    mBtnSearchImport.setVisibility(View.VISIBLE);
                    break;
                case R.id.btn_search_import_file_enter:
                    hideInputMethod(mEtSearchImportFileInput);

                    List<ImportItem> internalImportFilterList = mInternalImportListAdapter.getFilterList(mEtSearchImportFileInput.getText().toString());
                    mInternalImportListAdapter.updateList(internalImportFilterList);
                    mInternalImportListAdapter.clearAllItemSelect();
                    mInternalImportListAdapter.notifyDataSetChanged();
                    mInternalImportListAdapter.setCurrentPage(1);

                    List<ImportItem> externalImportFilterList = mExternalImportListAdapter.getFilterList(mEtSearchImportFileInput.getText().toString());
                    mExternalImportListAdapter.updateList(externalImportFilterList);
                    mExternalImportListAdapter.clearAllItemSelect();
                    mExternalImportListAdapter.notifyDataSetChanged();
                    mExternalImportListAdapter.setCurrentPage(1);

                    List<ImportItem> dropboxImportFilterList = mDropboxImportListAdapter.getFilterList(mEtSearchImportFileInput.getText().toString());
                    mDropboxImportListAdapter.updateList(dropboxImportFilterList);
                    mDropboxImportListAdapter.clearAllItemSelect();
                    mDropboxImportListAdapter.notifyDataSetChanged();
                    mDropboxImportListAdapter.setCurrentPage(1);

                    break;
            }
            updateImportListView();
            updateImportListViewSelectStatus();
        }
    };

    private View.OnClickListener onImportBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            switch (v.getId()) {
                case R.id.btn_exit_import_mode:
                    changeLauncherListType(LauncherListType.NORMAL);
                    break;
                case R.id.btn_delete_selected_backup:
                    if (ImportFileVia.DROPBOX == mSpinnerImportFileVia.getSelectedItemPosition()) {
                        operateBackupFileOnDropboxServer(FileAction.DELETE, mDropboxImportListAdapter.getSelectedList());
                    } else {
                        deleteSelectedBackup();
                    }
                    break;
                case R.id.btn_import_selected_backup:
                    if (ImportFileVia.DROPBOX == mSpinnerImportFileVia.getSelectedItemPosition()) {
                        operateBackupFileOnDropboxServer(FileAction.DOWNLOAD, mDropboxImportListAdapter.getSelectedList());
                    } else {
                        checkSelectedListIsExistInLauncherList();
                        if (mConflictImportList.isEmpty()) {
                            if (ImportFileVia.INTERNAL == mSpinnerImportFileVia.getSelectedItemPosition())
                                importSelectedList(mInternalImportListAdapter.getSelectedList());
                            else
                                importSelectedList(mExternalImportListAdapter.getSelectedList());
                        } else {
                            showConflictDialogFragment();
                        }
                    }
                    break;
            }
        }
    };

    private View.OnClickListener onImportListSortButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mBtnImportListSortByDate.setSelected(false);
            mBtnImportListSortByName.setSelected(false);
            mBtnImportListSortBySize.setSelected(false);

            Integer viewTag = (Integer) view.getTag();

            switch (view.getId()) {
                case R.id.btn_sort_by_name:
                    if (SORT_ASCENDING_TAG == viewTag) {
                        if (ListSort.NAME_ASCENDING == mSortImportListProperty) {
                            mSortImportListProperty = ListSort.NAME_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mInternalImportListAdapter.sortByNameDescending();
                            mExternalImportListAdapter.sortByNameDescending();
                            mDropboxImportListAdapter.sortByNameDescending();
                        } else {
                            mSortImportListProperty = ListSort.NAME_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mInternalImportListAdapter.sortByNameAscending();
                            mExternalImportListAdapter.sortByNameAscending();
                            mDropboxImportListAdapter.sortByNameAscending();
                        }
                    } else if (SORT_DESCENDING_TAG == viewTag) {
                        if (ListSort.NAME_DESCENDING == mSortImportListProperty) {
                            mSortImportListProperty = ListSort.NAME_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mInternalImportListAdapter.sortByNameAscending();
                            mExternalImportListAdapter.sortByNameAscending();
                            mDropboxImportListAdapter.sortByNameAscending();
                        } else {
                            mSortImportListProperty = ListSort.NAME_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mInternalImportListAdapter.sortByNameDescending();
                            mExternalImportListAdapter.sortByNameDescending();
                            mDropboxImportListAdapter.sortByNameDescending();
                        }
                    }
                    break;
                case R.id.btn_sort_by_date:
                    if (SORT_ASCENDING_TAG == viewTag) {
                        if (ListSort.DATE_ASCENDING == mSortImportListProperty) {
                            mSortImportListProperty = ListSort.DATE_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mInternalImportListAdapter.sortByDateDescending();
                            mExternalImportListAdapter.sortByDateDescending();
                            mDropboxImportListAdapter.sortByDateDescending();
                        } else {
                            mSortImportListProperty = ListSort.DATE_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mInternalImportListAdapter.sortByDateAscending();
                            mExternalImportListAdapter.sortByDateAscending();
                            mDropboxImportListAdapter.sortByDateAscending();
                        }
                    } else if (SORT_DESCENDING_TAG == viewTag) {
                        if (ListSort.DATE_DESCENDING == mSortImportListProperty) {
                            mSortImportListProperty = ListSort.DATE_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mInternalImportListAdapter.sortByDateAscending();
                            mExternalImportListAdapter.sortByDateAscending();
                            mDropboxImportListAdapter.sortByDateAscending();
                        } else {
                            mSortImportListProperty = ListSort.DATE_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mInternalImportListAdapter.sortByDateDescending();
                            mExternalImportListAdapter.sortByDateDescending();
                            mDropboxImportListAdapter.sortByDateDescending();
                        }
                    }
                    break;
                case R.id.btn_sort_by_size:
                    if (SORT_ASCENDING_TAG == viewTag) {
                        if (ListSort.SIZE_ASCENDING == mSortImportListProperty) {
                            mSortImportListProperty = ListSort.SIZE_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mInternalImportListAdapter.sortBySizeDescending();
                            mExternalImportListAdapter.sortBySizeDescending();
                            mDropboxImportListAdapter.sortBySizeDescending();
                        } else {
                            mSortImportListProperty = ListSort.SIZE_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mInternalImportListAdapter.sortBySizeAscending();
                            mExternalImportListAdapter.sortBySizeAscending();
                            mDropboxImportListAdapter.sortBySizeAscending();
                        }
                    } else if (SORT_DESCENDING_TAG == viewTag) {
                        if (ListSort.SIZE_DESCENDING == mSortImportListProperty) {
                            mSortImportListProperty = ListSort.SIZE_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mInternalImportListAdapter.sortBySizeAscending();
                            mExternalImportListAdapter.sortBySizeAscending();
                            mDropboxImportListAdapter.sortBySizeAscending();
                        } else {
                            mSortImportListProperty = ListSort.SIZE_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mInternalImportListAdapter.sortBySizeDescending();
                            mExternalImportListAdapter.sortBySizeDescending();
                            mDropboxImportListAdapter.sortBySizeDescending();
                        }
                    }
                    break;
                default:
                    break;
            }
            view.setTag(viewTag);
            view.setSelected(true);
            ((ImageButton) view).setImageLevel(viewTag);
            mInternalImportListAdapter.notifyDataSetChanged();
            mExternalImportListAdapter.notifyDataSetChanged();
            mDropboxImportListAdapter.notifyDataSetChanged();
            updateImportListView();
        }
    };

    private View.OnClickListener onImportListPageButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_import_list_page_up:
                    mImportListCurrentPage--;
                    break;
                case R.id.btn_import_list_page_down:
                    mImportListCurrentPage++;
                    break;
                default:
                    break;
            }

            switch (mSpinnerImportFileVia.getSelectedItemPosition()) {
                case ImportFileVia.INTERNAL:
                    mInternalImportListAdapter.setCurrentPage(mImportListCurrentPage);
                    break;
                case ImportFileVia.EXTERNAL:
                    mExternalImportListAdapter.setCurrentPage(mImportListCurrentPage);
                    break;
                case ImportFileVia.DROPBOX:
                    mDropboxImportListAdapter.setCurrentPage(mImportListCurrentPage);
                    break;
                default:
                    break;
            }

            updateImportListView();
        }
    };

    private AdapterView.OnItemSelectedListener onImportFileViaSpinnerItemSelectedListener = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case ImportFileVia.INTERNAL:
                case ImportFileVia.EXTERNAL:
                    clearImportListView();
                    mInternalImportListAdapter.setCurrentPage(1);
                    mExternalImportListAdapter.setCurrentPage(1);
                    updateImportListView();
                    break;
                case ImportFileVia.DROPBOX:
                    if (isWifiConnected()) {
                        showDropboxSyncMessage();
                        dbx.trySignIn(new Dropbox.TrySignInFinishListener() {
                            @Override
                            public void onTryFinished(boolean isSignIn) {
                                if (!isSignIn)
                                    showNotLoggedInToDropboxMessage();
                                else {
                                    SharedPreferences prefs = getSharedPreferences(Global.ACCESS_KEY, MODE_PRIVATE);
                                    String accountEmailStr = prefs.getString(Global.ACCESS_USER_Email, "");
                                    mTvDropboxAccount.setText(accountEmailStr);
                                    getDropboxBackupFileList();
                                }
                            }
                        });
                    } else
                        showNoInternetConnectionMessage();
                    break;
            }
        }
    };

    private void showDropboxSyncMessage() {
        mLayoutImportFileList.removeAllViews();
        mTvImportListCurrentPage.setText(String.valueOf(1));
        mTvImportListTotalPage.setText(String.valueOf(1));
        mTvImportListEmptyHint.setVisibility(View.GONE);
        mLayoutNotLoggedInDropboxMessage.setVisibility(View.GONE);
        mLayoutNoInternetConnectionMessage.setVisibility(View.GONE);
        mLayoutDropboxSyncMessage.setVisibility(View.VISIBLE);
        mBtnImportListPageUp.setEnabled(false);
        mBtnImportListPageDown.setEnabled(false);

        mLayoutDropboxAccount.setVisibility(View.GONE);
        mBtnDeleteSelectedBackupFile.setVisibility(View.GONE);
        mBtnImportSelectedBackupFile.setVisibility(View.GONE);
    }

    private void showNoInternetConnectionMessage() {
        mLayoutImportFileList.removeAllViews();
        mTvImportListCurrentPage.setText(String.valueOf(1));
        mTvImportListTotalPage.setText(String.valueOf(1));
        mTvImportListEmptyHint.setVisibility(View.GONE);
        mLayoutDropboxSyncMessage.setVisibility(View.GONE);
        mLayoutNotLoggedInDropboxMessage.setVisibility(View.GONE);
        mLayoutNoInternetConnectionMessage.setVisibility(View.VISIBLE);
        mBtnImportListPageUp.setEnabled(false);
        mBtnImportListPageDown.setEnabled(false);

        mLayoutDropboxAccount.setVisibility(View.GONE);
        mBtnDeleteSelectedBackupFile.setVisibility(View.GONE);
        mBtnImportSelectedBackupFile.setVisibility(View.GONE);
    }

    private void showNotLoggedInToDropboxMessage() {
        mLayoutImportFileList.removeAllViews();
        mTvImportListCurrentPage.setText(String.valueOf(1));
        mTvImportListTotalPage.setText(String.valueOf(1));
        mTvImportListEmptyHint.setVisibility(View.GONE);
        mLayoutDropboxSyncMessage.setVisibility(View.GONE);
        mLayoutNoInternetConnectionMessage.setVisibility(View.GONE);
        mLayoutNotLoggedInDropboxMessage.setVisibility(View.VISIBLE);
        mBtnImportListPageUp.setEnabled(false);
        mBtnImportListPageDown.setEnabled(false);

        mLayoutDropboxAccount.setVisibility(View.GONE);
        mBtnDeleteSelectedBackupFile.setVisibility(View.GONE);
        mBtnImportSelectedBackupFile.setVisibility(View.GONE);
    }

    private void resetImportView() {
        mSpinnerImportFileVia.setSelection(ImportFileVia.INTERNAL);
        mSortImportListProperty = ListSort.DATE_DESCENDING;
        mBtnImportListSortByName.setTag(new Integer(SORT_ASCENDING_TAG));
        mBtnImportListSortByDate.setTag(new Integer(SORT_DESCENDING_TAG));
        mBtnImportListSortBySize.setTag(new Integer(SORT_ASCENDING_TAG));
        mBtnImportListSortByName.setImageLevel(SORT_ASCENDING_TAG);
        mBtnImportListSortByDate.setImageLevel(SORT_DESCENDING_TAG);
        mBtnImportListSortBySize.setImageLevel(SORT_ASCENDING_TAG);
        mBtnImportListSortByDate.setSelected(true);
        mBtnImportListSortByName.setSelected(false);
        mBtnImportListSortBySize.setSelected(false);
        mEtSearchImportFileInput.setText("");

        mLayoutDropboxAccount.setVisibility(View.GONE);
        mIsLoadingInternalBackupFileList = true;
        mIsLoadingExternalBackupFileList = true;
        mInternalImportList.clear();
        mExternalImportList.clear();
        mDropboxImportList.clear();
        mInternalImportListAdapter.updateList(mInternalImportList);
        mExternalImportListAdapter.updateList(mExternalImportList);
        mDropboxImportListAdapter.updateList(mDropboxImportList);
        mInternalImportListAdapter.notifyDataSetChanged();
        mExternalImportListAdapter.notifyDataSetChanged();
        mDropboxImportListAdapter.notifyDataSetChanged();
        updateImportListView();
    }

    private void clearImportListView() {
        mLayoutImportFileList.removeAllViews();
        mTvImportListCurrentPage.setText(String.valueOf(1));
        mTvImportListTotalPage.setText(String.valueOf(1));
        mLayoutDropboxSyncMessage.setVisibility(View.GONE);
        mTvImportListEmptyHint.setVisibility(View.VISIBLE);
        mBtnImportListPageUp.setEnabled(false);
        mBtnImportListPageDown.setEnabled(false);
    }

    private void updateImportListView() {
        mLayoutImportFileList.removeAllViews();
        mCurrentPageImportList.clear();
        switch (mSpinnerImportFileVia.getSelectedItemPosition()) {
            case ImportFileVia.INTERNAL:
                if (mIsLoadingInternalBackupFileList)
                    mTvImportListEmptyHint.setText(getString(R.string.loading_notebook));
                else
                    mTvImportListEmptyHint.setText(getString(R.string.restore_list_empty_hint));

                mCurrentPageImportList = mInternalImportListAdapter.getCurrentPageList();
                mImportListCurrentPage = mInternalImportListAdapter.getCurrentPage();
                mImportListTotalPage = mInternalImportListAdapter.getTotalPage();
                break;
            case ImportFileVia.EXTERNAL:
                if (mIsLoadingExternalBackupFileList)
                    mTvImportListEmptyHint.setText(getString(R.string.loading_notebook));
                else
                    mTvImportListEmptyHint.setText(getString(R.string.restore_list_empty_hint));

                mCurrentPageImportList = mExternalImportListAdapter.getCurrentPageList();
                mImportListCurrentPage = mExternalImportListAdapter.getCurrentPage();
                mImportListTotalPage = mExternalImportListAdapter.getTotalPage();
                break;
            case ImportFileVia.DROPBOX:
                mCurrentPageImportList = mDropboxImportListAdapter.getCurrentPageList();
                mImportListCurrentPage = mDropboxImportListAdapter.getCurrentPage();
                mImportListTotalPage = mDropboxImportListAdapter.getTotalPage();

                mLayoutDropboxAccount.setVisibility(View.VISIBLE);
                break;

            default:
                break;
        }

        mTvImportListCurrentPage.setText(String.valueOf(this.mImportListCurrentPage));
        mTvImportListTotalPage.setText(String.valueOf(this.mImportListTotalPage));

        if (mCurrentPageImportList.isEmpty() && this.mImportListCurrentPage == 1 && this.mImportListTotalPage == 1) {
            mLayoutDropboxSyncMessage.setVisibility(View.GONE);
            mLayoutNoInternetConnectionMessage.setVisibility(View.GONE);
            mLayoutNotLoggedInDropboxMessage.setVisibility(View.GONE);
            mTvImportListEmptyHint.setVisibility(View.VISIBLE);
            mBtnImportListPageUp.setEnabled(false);
            mBtnImportListPageDown.setEnabled(false);

            mBtnSelectAllImport.setUnchecked();
            mBtnDeleteSelectedBackupFile.setVisibility(View.GONE);
            mBtnImportSelectedBackupFile.setVisibility(View.GONE);
            mTvImportListSelectedCount.setText(String.valueOf(0));
            return;
        } else {
            mLayoutDropboxSyncMessage.setVisibility(View.GONE);
            mLayoutNoInternetConnectionMessage.setVisibility(View.GONE);
            mLayoutNotLoggedInDropboxMessage.setVisibility(View.GONE);
            mTvImportListEmptyHint.setVisibility(View.GONE);
            mBtnImportListPageUp.setEnabled(true);
            mBtnImportListPageDown.setEnabled(true);
        }

        for (ImportItem importItem : mCurrentPageImportList) {
            switch (importItem.getItemType()) {
                case ImportItem.ImportItemType.GROUP_HEADER:
                    ImportItemGroupHeaderView headerView = new ImportItemGroupHeaderView(this, importItem.getGroupIndex(), importItem.getGroupSize());
                    headerView.setOnItemClickListener(onImportItemClickListener);
                    mLayoutImportFileList.addView(headerView);
                    break;
                case ImportItem.ImportItemType.GROUP_ITEM:
                    ImportItemGroupItemView groupItemView = new ImportItemGroupItemView(this, importItem.getGroupIndex(), importItem);
                    groupItemView.setOnItemClickListener(onImportItemClickListener);
                    mLayoutImportFileList.addView(groupItemView);
                    break;
                case ImportItem.ImportItemType.SINGLE_ITEM:
                    ImportItemSingleView singleView = new ImportItemSingleView(this, importItem);
                    singleView.setOnItemClickListener(onImportItemClickListener);
                    mLayoutImportFileList.addView(singleView);
                    break;
            }
        }

        updateImportListViewSelectStatus();
    }

    private ImportItemClickListener onImportItemClickListener = new ImportItemClickListener() {
        @Override
        public void onCheckedChange(int importItemType, Object viewTag, boolean b) {
            if (!b)
                mBtnSelectAllImport.setUnchecked();

            switch (importItemType) {
                case ImportItem.ImportItemType.GROUP_HEADER:
                    selectImportListGroupHeader((int) viewTag, b);
                    break;
                case ImportItem.ImportItemType.GROUP_ITEM:
                    selectImportListGroupItem((ImportItem) viewTag, b);
                    break;
                case ImportItem.ImportItemType.SINGLE_ITEM:
                    selectSingleItem((ImportItem) viewTag, b);
                    break;
            }
            updateImportListViewSelectStatus();
        }
    };

    private void updateImportListViewSelectStatus() {

        for (int i = 0; i < mCurrentPageImportList.size(); i++) {
            ((ImportItemView) mLayoutImportFileList.getChildAt(i)).updateCheckBox(mCurrentPageImportList.get(i).isItemSelected());
        }

        boolean isAllSelect;
        int selectedCount;
        switch (mSpinnerImportFileVia.getSelectedItemPosition()) {
            case ImportFileVia.INTERNAL:
                isAllSelect = mInternalImportListAdapter.isEveryItemSelected();
                selectedCount = mInternalImportListAdapter.getSelectedCount();
                break;
            case ImportFileVia.EXTERNAL:
                isAllSelect = mExternalImportListAdapter.isEveryItemSelected();
                selectedCount = mExternalImportListAdapter.getSelectedCount();
                break;
            case ImportFileVia.DROPBOX:
                isAllSelect = mDropboxImportListAdapter.isEveryItemSelected();
                selectedCount = mDropboxImportListAdapter.getSelectedCount();
                break;
            default:
                isAllSelect = false;
                selectedCount = 0;
                break;
        }

        if (isAllSelect)
            mBtnSelectAllImport.setChecked();
        else
            mBtnSelectAllImport.setUnchecked();

        if (selectedCount > 0) {
            mLayoutDropboxAccount.setVisibility(View.GONE);
            mBtnDeleteSelectedBackupFile.setVisibility(View.VISIBLE);
            mBtnImportSelectedBackupFile.setVisibility(View.VISIBLE);
        } else {
            mBtnDeleteSelectedBackupFile.setVisibility(View.GONE);
            mBtnImportSelectedBackupFile.setVisibility(View.GONE);
            if (ImportFileVia.DROPBOX == mSpinnerImportFileVia.getSelectedItemPosition())
                mLayoutDropboxAccount.setVisibility(View.VISIBLE);
        }

        mTvImportListSelectedCount.setText(String.valueOf(selectedCount));
    }

    private void selectImportListAll(boolean isSelect) {
        switch (mSpinnerImportFileVia.getSelectedItemPosition()) {
            case ImportFileVia.INTERNAL:
                mInternalImportListAdapter.clearAllItemSelect();

                if (isSelect)
                    mInternalImportListAdapter.setEveryItemSelected();

                mCurrentPageImportList = mInternalImportListAdapter.getCurrentPageList();
                break;
            case ImportFileVia.EXTERNAL:
                mExternalImportListAdapter.clearAllItemSelect();

                if (isSelect)
                    mExternalImportListAdapter.setEveryItemSelected();

                mCurrentPageImportList = mExternalImportListAdapter.getCurrentPageList();
                break;
            case ImportFileVia.DROPBOX:
                mDropboxImportListAdapter.clearAllItemSelect();

                if (isSelect)
                    mDropboxImportListAdapter.setEveryItemSelected();

                mCurrentPageImportList = mDropboxImportListAdapter.getCurrentPageList();
                break;
            default:
                break;
        }
    }

    private void selectImportListGroupHeader(int groupIndex, boolean isSelect) {
        switch (mSpinnerImportFileVia.getSelectedItemPosition()) {
            case ImportFileVia.INTERNAL:
                if (isSelect)
                    mInternalImportListAdapter.setGroupItemNewestSelect(groupIndex);
                else
                    mInternalImportListAdapter.clearGroupItemSelect(groupIndex);

                mCurrentPageImportList = mInternalImportListAdapter.getCurrentPageList();
                break;
            case ImportFileVia.EXTERNAL:
                if (isSelect)
                    mExternalImportListAdapter.setGroupItemNewestSelect(groupIndex);
                else
                    mExternalImportListAdapter.clearGroupItemSelect(groupIndex);

                mCurrentPageImportList = mExternalImportListAdapter.getCurrentPageList();
                break;
            case ImportFileVia.DROPBOX:
                // Dropbox list has no grouped.
                break;
            default:
                break;
        }
    }

    private void selectImportListGroupItem(ImportItem item, boolean isSelect) {
        int[] indexArray;
        int groupIndex;
        int itemIndex;
        switch (mSpinnerImportFileVia.getSelectedItemPosition()) {
            case ImportFileVia.INTERNAL:
                indexArray = mInternalImportListAdapter.getGroupItemGroupIndex(item);
                groupIndex = indexArray[0];
                itemIndex = indexArray[1];
                if (groupIndex >= 0 && itemIndex >= 0) {
                    mInternalImportListAdapter.clearGroupItemSelect(groupIndex);
                    mInternalImportListAdapter.setGroupItemSelect(groupIndex, itemIndex, isSelect);
                }
                mCurrentPageImportList = mInternalImportListAdapter.getCurrentPageList();
                break;
            case ImportFileVia.EXTERNAL:
                indexArray = mExternalImportListAdapter.getGroupItemGroupIndex(item);
                groupIndex = indexArray[0];
                itemIndex = indexArray[1];
                if (groupIndex >= 0 && itemIndex >= 0) {
                    mExternalImportListAdapter.clearGroupItemSelect(groupIndex);
                    mExternalImportListAdapter.setGroupItemSelect(groupIndex, itemIndex, isSelect);
                }
                mCurrentPageImportList = mExternalImportListAdapter.getCurrentPageList();
                break;
            case ImportFileVia.DROPBOX:
                // Dropbox list has no grouped.
                break;
            default:
                break;
        }
    }

    private void selectSingleItem(ImportItem item, boolean isSelect) {
        switch (mSpinnerImportFileVia.getSelectedItemPosition()) {
            case ImportFileVia.INTERNAL:
                mInternalImportListAdapter.setSingleItemSelect(item, isSelect);
                mCurrentPageImportList = mInternalImportListAdapter.getCurrentPageList();
                break;
            case ImportFileVia.EXTERNAL:
                mExternalImportListAdapter.setSingleItemSelect(item, isSelect);
                mCurrentPageImportList = mExternalImportListAdapter.getCurrentPageList();
                break;
            case ImportFileVia.DROPBOX:
                mDropboxImportListAdapter.setSingleItemSelect(item, isSelect);
                mCurrentPageImportList = mDropboxImportListAdapter.getCurrentPageList();
                break;
            default:
                break;
        }
    }

    private void importSelectedList(List<ImportItem> selectedList) {
        List<String> filePathList = new ArrayList<>();
        List<Integer> sizeList = new ArrayList<>();
        for (ImportItem item : selectedList) {
            filePathList.add(item.getFilePath());
            sizeList.add(item.getPages());
        }
        ImportAsyncTask importAsyncTask = new ImportAsyncTask(this, filePathList, sizeList);
        importAsyncTask.execute();
    }

    private void initFreeStorageViews() {
        mLayoutFreeStorageMode = (LinearLayout) findViewById(R.id.layout_free_storage_mode);
        mLayoutLauncherFreeStorage = (LinearLayout) findViewById(R.id.layout_launcher_free_storage);

        mTvFreeStorageListSelectedCount = (TextView) findViewById(R.id.tv_free_list_selected_count);

        mBtnBackupAndDeleteSelectedFileToFree = (LinearLayout) findViewById(R.id.btn_backup_and_delete_selected_to_free);
        mBtnDeleteSelectedFileToFree = (LinearLayout) findViewById(R.id.btn_delete_selected_to_free);
        ImageButton btnExitFreeStorageMode = (ImageButton) findViewById(R.id.btn_exit_free_storage_mode);

        mBtnBackupAndDeleteSelectedFileToFree.setOnClickListener(onFreeModeBtnClickListener);
        mBtnDeleteSelectedFileToFree.setOnClickListener(onFreeModeBtnClickListener);
        btnExitFreeStorageMode.setOnClickListener(onFreeModeBtnClickListener);

        LinearLayout layoutSelectAllFileToFree = (LinearLayout) mLayoutLauncherFreeStorage.findViewById(R.id.layout_select_all_files_to_free);
        layoutSelectAllFileToFree.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mBtnSelectAllFileToFree.performClick();
            }
        });
        mBtnSelectAllFileToFree = (ToggleImageButton) mLayoutLauncherFreeStorage.findViewById(R.id.btn_select_all_files_to_free);
        mBtnSelectAllFileToFree.setOnCheckedChangeListener(new ToggleImageButton.OnCheckedChangedListener() {
            @Override
            public void onCheckedChange(boolean value) {
                mFreeStorageListAdapter.setListSelected(value);
                updateFreeStorageListViewSelectStatus();
            }
        });

        mLayoutFreeStorageList = (LinearLayout) mLayoutLauncherFreeStorage.findViewById(R.id.layout_free_file_list);

        mBtnFreeStorageListSortByName = (ImageButton) mLayoutLauncherFreeStorage.findViewById(R.id.btn_sort_by_name);
        mBtnFreeStorageListSortByDate = (ImageButton) mLayoutLauncherFreeStorage.findViewById(R.id.btn_sort_by_date);
        mBtnFreeStorageListSortBySize = (ImageButton) mLayoutLauncherFreeStorage.findViewById(R.id.btn_sort_by_size);
        mBtnFreeStorageListSortByName.setTag(new Integer(SORT_ASCENDING_TAG));
        mBtnFreeStorageListSortByDate.setTag(new Integer(SORT_ASCENDING_TAG));
        mBtnFreeStorageListSortBySize.setTag(new Integer(SORT_ASCENDING_TAG));
        mBtnFreeStorageListSortByName.setImageLevel(SORT_ASCENDING_TAG);
        mBtnFreeStorageListSortByDate.setImageLevel(SORT_ASCENDING_TAG);
        mBtnFreeStorageListSortBySize.setImageLevel(SORT_ASCENDING_TAG);
        mBtnFreeStorageListSortByName.setOnClickListener(onFreeStorageListSortButtonClickListener);
        mBtnFreeStorageListSortByDate.setOnClickListener(onFreeStorageListSortButtonClickListener);
        mBtnFreeStorageListSortBySize.setOnClickListener(onFreeStorageListSortButtonClickListener);
        mBtnFreeStorageListSortByDate.setSelected(true);

        mTvFreeStorageListCurrentPage = (TextView) mLayoutLauncherFreeStorage.findViewById(R.id.tv_free_storage_list_page_index);
        mTvFreeStorageListTotalPage = (TextView) mLayoutLauncherFreeStorage.findViewById(R.id.tv_free_storage_list_page_total);
        ImageButton btnFreeStorageListPageUp = (ImageButton) mLayoutLauncherFreeStorage.findViewById(R.id.btn_free_storage_list_page_up);
        ImageButton btnFreeStorageListPageDown = (ImageButton) mLayoutLauncherFreeStorage.findViewById(R.id.btn_free_storage_list_page_down);
        btnFreeStorageListPageUp.setOnClickListener(onFreeStorageListPageButtonClickListener);
        btnFreeStorageListPageDown.setOnClickListener(onFreeStorageListPageButtonClickListener);

        mFreeStorageListAdapter = new FreeStorageListAdapter(Bookshelf.getInstance().getBookList(), 8);
    }

    private View.OnClickListener onFreeModeBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_backup_and_delete_selected_to_free:
                    ArrayList<UUID> selectedUuidList = new ArrayList<>();
                    for (Book book : mFreeStorageListAdapter.getSelectedBookList()) {
                        selectedUuidList.add(book.getUUID());
                    }

                    if (selectedUuidList.size() == 1) {
                        BackupDialogFragment backupSingleNoteDialogFragment = BackupDialogFragment.newInstance(selectedUuidList.get(0), false, true);
                        showDialogFragment(backupSingleNoteDialogFragment, BackupDialogFragment.class.getSimpleName());
                    } else {
                        BackupMultipleDialogFragment backupMultipleDialogFragment = BackupMultipleDialogFragment.newInstance(selectedUuidList, true);
                        showDialogFragment(backupMultipleDialogFragment, BackupMultipleDialogFragment.class.getSimpleName());
                    }
                    break;
                case R.id.btn_delete_selected_to_free:
                    deleteSelectedNoteBook(mFreeStorageListAdapter.getSelectedBookList());
                    break;
                case R.id.btn_exit_free_storage_mode:
                    changeLauncherListType(LauncherListType.NORMAL);
                    break;
            }
        }
    };

    private View.OnClickListener onFreeStorageListSortButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mBtnFreeStorageListSortByDate.setSelected(false);
            mBtnFreeStorageListSortByName.setSelected(false);
            mBtnFreeStorageListSortBySize.setSelected(false);

            Integer viewTag = (Integer) view.getTag();

            switch (view.getId()) {
                case R.id.btn_sort_by_name:
                    if (SORT_ASCENDING_TAG == viewTag) {
                        if (ListSort.NAME_ASCENDING == mSortFreeStorageListProperty) {
                            mSortFreeStorageListProperty = ListSort.NAME_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mFreeStorageListAdapter.sortByNameDescending();
                        } else {
                            mSortFreeStorageListProperty = ListSort.NAME_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mFreeStorageListAdapter.sortByNameAscending();
                        }
                    } else if (SORT_DESCENDING_TAG == viewTag) {
                        if (ListSort.NAME_DESCENDING == mSortFreeStorageListProperty) {
                            mSortFreeStorageListProperty = ListSort.NAME_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mFreeStorageListAdapter.sortByNameAscending();
                        } else {
                            mSortFreeStorageListProperty = ListSort.NAME_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mFreeStorageListAdapter.sortByNameDescending();
                        }
                    }
                    break;
                case R.id.btn_sort_by_date:
                    if (SORT_ASCENDING_TAG == viewTag) {
                        if (ListSort.DATE_ASCENDING == mSortFreeStorageListProperty) {
                            mSortFreeStorageListProperty = ListSort.DATE_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mFreeStorageListAdapter.sortByDateDescending();
                        } else {
                            mSortFreeStorageListProperty = ListSort.DATE_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mFreeStorageListAdapter.sortByDateAscending();
                        }
                    } else if (SORT_DESCENDING_TAG == viewTag) {
                        if (ListSort.DATE_DESCENDING == mSortFreeStorageListProperty) {
                            mSortFreeStorageListProperty = ListSort.DATE_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mFreeStorageListAdapter.sortByDateAscending();
                        } else {
                            mSortFreeStorageListProperty = ListSort.DATE_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mFreeStorageListAdapter.sortByDateDescending();
                        }
                    }
                    break;
                case R.id.btn_sort_by_size:
                    if (SORT_ASCENDING_TAG == viewTag) {
                        if (ListSort.SIZE_ASCENDING == mSortFreeStorageListProperty) {
                            mSortFreeStorageListProperty = ListSort.SIZE_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mFreeStorageListAdapter.sortBySizeDescending();
                        } else {
                            mSortFreeStorageListProperty = ListSort.SIZE_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mFreeStorageListAdapter.sortBySizeAscending();
                        }
                    } else if (SORT_DESCENDING_TAG == viewTag) {
                        if (ListSort.SIZE_DESCENDING == mSortFreeStorageListProperty) {
                            mSortFreeStorageListProperty = ListSort.SIZE_ASCENDING;
                            viewTag = SORT_ASCENDING_TAG;
                            mFreeStorageListAdapter.sortBySizeAscending();
                        } else {
                            mSortFreeStorageListProperty = ListSort.SIZE_DESCENDING;
                            viewTag = SORT_DESCENDING_TAG;
                            mFreeStorageListAdapter.sortBySizeDescending();
                        }
                    }
                    break;
                default:
                    break;
            }
            view.setTag(viewTag);
            view.setSelected(true);
            ((ImageButton) view).setImageLevel(viewTag);
            mFreeStorageListAdapter.notifyDataSetChanged();
            updateFreeStorageListViews();
        }
    };

    private View.OnClickListener onFreeStorageListPageButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_free_storage_list_page_up:
                    mFreeStorageListCurrentPage--;
                    break;
                case R.id.btn_free_storage_list_page_down:
                    mFreeStorageListCurrentPage++;
                    break;
                default:
                    break;
            }

            mFreeStorageListAdapter.setCurrentPage(mFreeStorageListCurrentPage);
            updateFreeStorageListViews();
        }
    };

    private void updateFreeStorageListViews() {
        mLayoutFreeStorageList.removeAllViews();

        mFreeStorageListCurrentPage = mFreeStorageListAdapter.getCurrentPage();
        mTvFreeStorageListCurrentPage.setText(String.valueOf(mFreeStorageListCurrentPage));
        mTvFreeStorageListTotalPage.setText(String.valueOf(mFreeStorageListAdapter.getTotalPage()));

        for (Book book : mFreeStorageListAdapter.getCurrentPageList()) {
            FreeStorageListItemView itemView = new FreeStorageListItemView(this, book);
            itemView.updateCheckBox(mFreeStorageListAdapter.isBookSelected(book.getUUID()));
            itemView.setOnItemClickListener(new FreeStorageListItemView.FreeStorageListItemClickListener() {
                @Override
                public void onCheckedChange(Object viewTag, boolean b) {
                    mFreeStorageListAdapter.setBookSelected(((Book) viewTag).getUUID(), b);
                    updateFreeStorageListViewSelectStatus();
                }
            });
            mLayoutFreeStorageList.addView(itemView);
        }
    }

    private void updateFreeStorageListViewSelectStatus() {
        List<Book> selectedFreeStorageList = mFreeStorageListAdapter.getSelectedBookList();
        if (selectedFreeStorageList.size() == mFreeStorageListAdapter.getCount())
            mBtnSelectAllFileToFree.setChecked();
        else
            mBtnSelectAllFileToFree.setUnchecked();

        mTvFreeStorageListSelectedCount.setText(String.valueOf(selectedFreeStorageList.size()));

        if (selectedFreeStorageList.size() == 0) {
            mBtnBackupAndDeleteSelectedFileToFree.setVisibility(View.GONE);
            mBtnDeleteSelectedFileToFree.setVisibility(View.GONE);
        } else {
            mBtnBackupAndDeleteSelectedFileToFree.setVisibility(View.VISIBLE);
            mBtnDeleteSelectedFileToFree.setVisibility(View.VISIBLE);
        }

        updateFreeStorageListViews();
    }

    private void initPageInfoViews() {
        mLayoutPageInfo = (RelativeLayout) findViewById(R.id.layout_page_info);
        mLayoutPageInfo.setVisibility(View.GONE);

        mTvPageInfo = (TextView) findViewById(R.id.tv_page_info);
        ImageButton btnPrevPage = (ImageButton) findViewById(R.id.btn_prev_page);
        ImageButton btnNextPage = (ImageButton) findViewById(R.id.btn_next_page);
        ImageButton btnFirstPage = (ImageButton) findViewById(R.id.btn_first_page);
        ImageButton btnLastPage = (ImageButton) findViewById(R.id.btn_last_page);
        btnPrevPage.setOnClickListener(onPageBtnClickListener);
        btnNextPage.setOnClickListener(onPageBtnClickListener);
        btnFirstPage.setOnClickListener(onPageBtnClickListener);
        btnLastPage.setOnClickListener(onPageBtnClickListener);
    }

    private void updatePageInfo() {

        if (Bookshelf.getInstance().getBookList().size() > 0) {
            mBtnSearch.setEnabled(true);
            mBtnSearch.setAlpha(1.0f);
        } else {
            mBtnSearch.setEnabled(false);
            mBtnSearch.setAlpha(0.2f);
        }

        String pageInfoStr = String.valueOf(mLauncherListAdapter.getCurrentPage())
                + " of " + String.valueOf(mLauncherListAdapter.getTotalPage());

        mTvPageInfo.setText(pageInfoStr);

        if (mLauncherListAdapter.getTotalPage() == 1) { //only one page
            mLayoutPageInfo.setVisibility(View.INVISIBLE);
        } else {
            mLayoutPageInfo.setVisibility(View.VISIBLE);
        }
    }

    private View.OnClickListener onPageBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.btn_prev_page) {
                prePage();
            } else if (view.getId() == R.id.btn_next_page) {
                nextPage();
            } else if (view.getId() == R.id.btn_first_page) {
                switchToPage(1);
            } else if (view.getId() == R.id.btn_last_page) {
                switchToPage(mLauncherListAdapter.getTotalPage());
            }
        }
    };

    private void prePage() {
        int currentPage = mLauncherListAdapter.getCurrentPage();
        currentPage--;
        switchToPage(currentPage);
    }

    private void nextPage() {
        int currentPage = mLauncherListAdapter.getCurrentPage();
        currentPage++;
        switchToPage(currentPage);
    }

    private void switchToPage(int page) {
        mLauncherListAdapter.setCurrentPage(page);
        updatePageInfo();
        updateLauncherListView();
    }

    private void renameSelectedNoteBook(Book nb) {
        Fragment renameFragment = RenameNoteDialogFragment.newInstance(nb.getUUID(), true);
        showDialogFragment(renameFragment, RenameNoteDialogFragment.class.getSimpleName());
    }

    private void deleteSelectedNoteBook(final List<Book> deleteList) {
        int count = deleteList.size();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        String dialogTag = "delete_confirm";
        String deleteConfirmMessage = getResources().getString(R.string.toolbox_message_delete_confirm, count + "");

        AlertDialogFragment deleteConfirmDialogFragment = AlertDialogFragment.newInstance(deleteConfirmMessage, R.drawable.writing_ic_error, true, dialogTag);

        deleteConfirmDialogFragment.setupPositiveButton(getString(android.R.string.yes));
        deleteConfirmDialogFragment.setupNegativeButton(getString(android.R.string.no));
        deleteConfirmDialogFragment.registerAlertDialogButtonClickListener(new AlertDialogButtonClickListener() {

            @Override
            public void onPositiveButtonClick(String fragmentTag) {
                DeleteNoteBookAsyncTask deleteNoteBookAsyncTask = new DeleteNoteBookAsyncTask(deleteList);
                deleteNoteBookAsyncTask.execute();
            }

            @Override
            public void onNegativeButtonClick(String fragmentTag) {
            }
        }, dialogTag);

        ft.replace(R.id.alert_dialog_container, deleteConfirmDialogFragment, dialogTag)
                .commit();
    }

    private class DeleteNoteBookAsyncTask extends AsyncTask<Void, Integer, Void> {
        private boolean interruptDelete = false;
        private int deleteCounter;
        private List<Book> selectedList = new ArrayList<>();

        DeleteNoteBookAsyncTask(List<Book> bookList) {
            selectedList.addAll(bookList);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            FragmentTransaction ft = getFragmentManager().beginTransaction();

            // Set interruptable= false. Let user can not interrupt.
            mProgressingDialogFragment = InterruptibleProgressingDialogFragment
                    .newInstance(getString(R.string.deleting), mLauncherListAdapter.getSelectedBookList().size(), true);

            mProgressingDialogFragment.setOnInterruptButtonClickListener(
                    new InterruptibleProgressingDialogFragment.OnInterruptButtonClickListener() {
                        @Override
                        public void onClick() {
                            interruptDelete = true;
                        }
                    });

            ft.replace(R.id.alert_dialog_container, mProgressingDialogFragment,
                    InterruptibleProgressingDialogFragment.class.getSimpleName()).commit();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (int i = 0; i < selectedList.size(); i++) {
                Bookshelf.getInstance().deleteBook(selectedList.get(i).getUUID());
                deleteCounter++;
                publishProgress(i + 1);

                if (interruptDelete)
                    break;
            }
            Bookshelf.getInstance().saveDateJsonFile();
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            mProgressingDialogFragment.updateProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            String alertMsg = deleteCounter + " " + getResources().getString(R.string.toolbox_message_delete_success);
            showAlertMessageDialog(alertMsg, true);
            changeLauncherListType(LauncherListType.NORMAL);
            switchToPage(1);
            updatePageInfo();
        }
    }

    private class CopySelectedBookAsyncTask extends AsyncTask<Void, Integer, Boolean> {
        private boolean interruptCopy = false;
        private int copyCounter;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            FragmentTransaction ft = getFragmentManager().beginTransaction();

            // Set interruptable= false. Let user can not interrupt.
            mProgressingDialogFragment = InterruptibleProgressingDialogFragment
                    .newInstance(getString(R.string.copying), mLauncherListAdapter.getSelectedBookList().size(), true);

            mProgressingDialogFragment.setOnInterruptButtonClickListener(
                    new InterruptibleProgressingDialogFragment.OnInterruptButtonClickListener() {
                        @Override
                        public void onClick() {
                            interruptCopy = true;
                        }
                    });

            ft.replace(R.id.alert_dialog_container, mProgressingDialogFragment,
                    InterruptibleProgressingDialogFragment.class.getSimpleName()).commit();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            List<Book> selectedList = mLauncherListAdapter.getSelectedBookList();
            List<String> nameList = new ArrayList<>();
            nameList.clear();
            for (Book book : Bookshelf.getInstance().getBookList()) {
                nameList.add(book.getTitle());
            }

            for (int i = 0; i < selectedList.size(); i++) {
                if (interruptCopy)
                    break;

                Book selectedBook = new Book(selectedList.get(i).getUUID(), true);
                List<Page> selectedBookPages = selectedBook.getPages();

                Book newBook = new Book(getNewNameNotInList(selectedList.get(i).getTitle(), nameList));
                TagManager newBookTagManage = newBook.getTagManager();

                for (Page page : selectedBookPages) {
                    List<TagManager.Tag> pageAllTags = page.getTags().allTags();
                    for (TagManager.Tag tag : pageAllTags) {
                        newBookTagManage.newTag(tag.toString());
                    }

                    newBook.clonePageTo(page, newBook.pagesSize() - 1, true);
                }
                newBook.setCurrentPage(newBook.getPage(0));
                newBook.deletePage();

                // the fail reason may be storage not enough.
                if (!newBook.save())
                    return false;

                copyCounter++;
                publishProgress(i);
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            mProgressingDialogFragment.updateProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            String alertMsg;
            if (aBoolean)
                alertMsg = copyCounter + " " + getResources().getString(R.string.toolbox_message_copy_success);
            else
                alertMsg = copyCounter + " " + getResources().getString(R.string.fail);

            showAlertMessageDialog(alertMsg, aBoolean);
            changeLauncherListType(LauncherListType.NORMAL);
            switchToPage(1);
            updatePageInfo();
        }
    }

    private String getNewNameNotInList(String inputName, List<String> nameList) {
        String copyName = inputName + "-COPY";
        String newName = inputName + "-COPY";
        int i = 1;

        while (nameList.contains(newName)) {
            i++;
            newName = copyName + String.valueOf(i);
        }

        return newName;
    }

    private void doFullRefresh(int delay) {
        Runnable invalidateRunnable = new Runnable() {
            @Override
            public void run() {
                if (Hardware.isEinkHardwareType()) {
                    mLayoutRoot.invalidate(PenEventNTX.UPDATE_MODE_SCREEN_2 | PenEventNTX.UPDATE_MODE_GLOBAL_RESET);
                }
            }
        };

        if (Hardware.isEinkHardwareType()) {
            mHandler.removeCallbacks(invalidateRunnable);
            mHandler.postDelayed(invalidateRunnable, delay);
        }
    }

    private void switchToNoteEditor() {
        Global.openWaitDialog(this);
        try {
            Global.HAS_GREY_COLOR = false;
            Intent mIntent = new Intent();
            ComponentName componentName = new ComponentName("ntx.note2", "ntx.note.NoteWriterActivity");
            mIntent.setComponent(componentName);
            startActivity(mIntent);
        } catch (Throwable e) {
            Global.closeWaitDialog(this);
            Toast.makeText(this, "APP NOT FIND !", Toast.LENGTH_LONG).show();
        }
    }

    private void switchToHomerReaderLibrary() {
        Global.openWaitDialog(this);
        try {
            ComponentName componentName = new ComponentName(Global.READER_PACKAGE, Global.READER_MAIN_PAGE_CLASS);
            Intent mIntent = new Intent();
            startActivity(mIntent.setComponent(componentName)
                    .setAction(Intent.ACTION_VIEW)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } catch (Throwable e) {
            Global.closeWaitDialog(this);
            Toast.makeText(this, "APP NOT FIND !", Toast.LENGTH_LONG).show();
        }
    }

    private void switchToCalendar() {
        Global.openWaitDialog(this);
        try {
            ComponentName componentName = new ComponentName(Global.CALENDAR_PACKAGE, Global.CALENDAR_CLASS);
            Intent mIntent = new Intent();
            startActivity(mIntent.setComponent(componentName).setAction(Intent.ACTION_VIEW)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            startActivity(mIntent);
        } catch (Throwable e) {
            Global.closeWaitDialog(this);
            Toast.makeText(this, "APP NOT FIND !", Toast.LENGTH_LONG).show();
        }

    }

    private void switchToPainter() {
        try {
            Intent mIntent = new Intent();
            ComponentName componentName = new ComponentName("ntx.painter", "ntx.painter.MainActivity");
            mIntent.setComponent(componentName);
            startActivity(mIntent);
        } catch (Throwable e) {
            Toast.makeText(this, "APP NOT FIND !", Toast.LENGTH_LONG).show();
        }
    }

    private void switchToCalculator() {
        try {
            Intent mIntent = new Intent();
            ComponentName componentName = new ComponentName("com.visionobjects.calculator", "com.visionobjects.calculator.activity.MainActivity");
            mIntent.setComponent(componentName);
            startActivity(mIntent);
        } catch (Throwable e) {
            Toast.makeText(this, "APP NOT FIND !", Toast.LENGTH_LONG).show();
        }
    }

    private void switchToPrinterDownloader() {
        try {
            Intent mIntent = new Intent();
            ComponentName componentName = new ComponentName("com.printerdownloader", "com.printerdownloader.MainActivity");
            mIntent.setComponent(componentName);
            startActivity(mIntent);
        } catch (Throwable e) {
            Toast.makeText(this, "APP NOT FIND !", Toast.LENGTH_LONG).show();
        }
    }

    private void switchToSettings() {
        Global.openWaitDialog(this);
        try {
            ComponentName componentName = new ComponentName("ntx.tools",
                    "ntx.tools.MainActivity");
            Intent mIntent = new Intent();
            startActivity(mIntent.setComponent(componentName)
                    .setAction(Intent.ACTION_VIEW)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            startActivity(mIntent);
        } catch (Throwable e) {
            Global.closeWaitDialog(this);
            Toast.makeText(this, "APP NOT FIND !", Toast.LENGTH_LONG).show();
        }
    }

    private void switchToWifiSettings() {
        Intent WifiSetting = new Intent("/");
        ComponentName comp = new ComponentName("com.android.settings", "com.android.settings.wifi.WifiSettings");
        WifiSetting.setComponent(comp);
        startActivity(WifiSetting);
    }

    private void resetRotate() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        // else {
        //////default
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // }
    }

    /**
     * @param msg
     * @param result : true = successful icon; false = fail icon
     */
    private void showAlertMessageDialog(String msg, boolean result) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        int iconId = result ? R.drawable.writing_ic_successful : R.drawable.writing_ic_error;
        String dialogTag = "alert_message_dialog";
        AlertDialogFragment alertMessageDialogFragment = AlertDialogFragment.newInstance(msg, iconId, true, dialogTag);
        ft.replace(R.id.alert_dialog_container, alertMessageDialogFragment, dialogTag)
                .commit();
    }

    private void showStorageNotEnoughAlertDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(
                getString(R.string.message_space_not_enough),
                R.drawable.writing_ic_error,
                true,
                DIALOG_TAG_STORAGE_NOTE_ENOUGH);

        alertDialogFragment.setUpSubMessage(getString(R.string.message_delete_notebook_try_again));

        if (Bookshelf.getInstance().getCount() != 0) {
            alertDialogFragment.setupNegativeButton(getString(android.R.string.ok));
            alertDialogFragment.setupPositiveButton(getString(R.string.manage_notebooks));
        } else {
            alertDialogFragment.setupPositiveButton(getString(android.R.string.ok));
        }

        alertDialogFragment.registerAlertDialogButtonClickListener(new AlertDialogButtonClickListener() {
            @Override
            public void onPositiveButtonClick(String fragmentTag) {
                if (Bookshelf.getInstance().getCount() != 0)
                    changeLauncherListType(LauncherListType.FREE_STORAGE);
            }

            @Override
            public void onNegativeButtonClick(String fragmentTag) {

            }
        }, "");
        ft.replace(R.id.alert_dialog_container, alertDialogFragment, DIALOG_TAG_STORAGE_NOTE_ENOUGH)
                .commit();
    }

    private void setPath() {
        if (Global.DIRECTORY_SDCARD_NOTE != null) {
            File nNoteDir = new File(Global.DIRECTORY_SDCARD_NOTE);
            if (!nNoteDir.exists()) {
                nNoteDir.mkdirs();
            }

        }
        if (Global.DIRECTORY_EXTERNALSD_NOTE != null) {
            File nNoteDir = new File(Global.DIRECTORY_EXTERNALSD_NOTE);
            if (!nNoteDir.exists()) {
                nNoteDir.mkdirs();
            }
        }
        if (Global.DIRECTORY_USBDRIVE != null) {
            File nNoteDir = new File(Global.DIRECTORY_USBDRIVE);
            if (!nNoteDir.exists()) {
                nNoteDir.mkdirs();
            }
        }
    }

    private void clearTempFileDir() {
        searchTempFiles(new File(Global.PACKAGE_DATA_DIR + Global.FILE_TEMP_DIR)); // dataDir=/data/data/ntx.note2
    }

    private void searchTempFiles(File file) {
        File[] the_Files = file.listFiles();

        if (the_Files == null)
            return;

        for (File tempF : the_Files) {
            if (tempF.isDirectory()) {
                if (!tempF.isHidden())
                    searchTempFiles(tempF);
            } else {
                tempF.delete();
            }
        }
    }

    private void searchTempFilesEmailCopy(File file) { // over 7 days to clear email temp file
        File[] the_Files = file.listFiles();

        if (the_Files == null)
            return;

        for (File tempF : the_Files) {
            if (tempF.isDirectory()) {
                if (!tempF.isHidden())
                    searchTempFilesEmailCopy(tempF);
            } else {
                Calendar nowCalendar = Calendar.getInstance();

                Calendar lastModCalendar = Calendar.getInstance();
                lastModCalendar.setTime(new Date(tempF.lastModified()));

                if (daysOfTwo(lastModCalendar, nowCalendar) >= Global.tempFileKeepDays) { // if over 7(Global.tempFileKeepDays) days, delete file.
                    tempF.delete();
                }
            }
        }
    }

    private int daysOfTwo(Calendar befor, Calendar after) {
        long m = after.getTimeInMillis() - befor.getTimeInMillis();

        m = m / (24 * 60 * 60 * 1000);
        // check the same date
        if (m == 0 && after.get(Calendar.DAY_OF_YEAR) != befor.get(Calendar.DAY_OF_YEAR)) {
            m += 1;
        }
        return (int) m;
    }

    private boolean isSpaceAvailable() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        //Dango 20181005 Bugfix - integer overflow
        double availableSizeInMB = (double) stat.getBlockSize() * (double) stat.getAvailableBlocks() / 1024 / 1024;

//        if (Global.APP_DATA_PACKAGE_FILES_PATH == null) {
//            Global.APP_DATA_PACKAGE_FILES_PATH = getFilesDir() + "/";
//        }

        long totalSize = calculateFolderTotalSize(new File(Global.APP_DATA_PACKAGE_FILES_PATH));

        return availableSizeInMB > LIMIT_SPACE && availableSizeInMB > (totalSize * 2);
    }

    private long calculateFolderTotalSize(File file) {
        long totalSize = 0;

        File[] the_Files = file.listFiles();

        if (the_Files == null)
            return 0;

        for (File tempF : the_Files) {
            totalSize += tempF.length();
        }
        return (long) (totalSize / 1024f / 1024f);
    }

    /**
     * Control the 2-Step-Suspend for Netronix eInk devices
     *
     * @param state 1 is enable. 0 is disable.
     */
    private void PowerEnhanceSet(int state) {
        try {
            Settings.System.putInt(mContext.getContentResolver(), "power_enhance_enable", state);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showMainPageMorePopupWindow() {

        mBtnMore.setSelected(true);

        MainPageMorePopupWindow mainPageMorePopupWindow = MainPageMorePopupWindow.getInstance(this);
        mainPageMorePopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mBtnMore.setSelected(false);
            }
        });
        mainPageMorePopupWindow.showOnAnchor(mBtnMore, HorizontalPosition.ALIGN_RIGHT, VerticalPosition.BELOW);
    }

    private void showDialogFragment(Fragment fragment, String tag) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.dialog_container, fragment, tag).commit();
    }

    private void backupMultiple() {
        ArrayList<UUID> selectedUuidList = new ArrayList<>();
        List<Book> selectedList = mLauncherListAdapter.getSelectedBookList();

        for (Book book : selectedList) {
            selectedUuidList.add(book.getUUID());
        }

        if (selectedUuidList.size() == 1) {
            BackupDialogFragment backupSingleNoteDialogFragment = BackupDialogFragment.newInstance(selectedUuidList.get(0), false, false);
            showDialogFragment(backupSingleNoteDialogFragment, BackupDialogFragment.class.getSimpleName());
        } else {
            BackupMultipleDialogFragment backupMultipleDialogFragment = BackupMultipleDialogFragment.newInstance(selectedUuidList, false);
            showDialogFragment(backupMultipleDialogFragment, BackupMultipleDialogFragment.class.getSimpleName());
        }

    }

    private void showInputMethod() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    private void hideInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private EditText.OnKeyListener onSearchEditTextKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                utility.StringIllegal.checkFirstSpaceChar(mEtSearchNote);
                utility.StringIllegal.checkIllegalChar(mEtSearchNote);

                if (mEtSearchNote.getText().toString().equals("")) {
                    mBtnSearchEnter.setAlpha(0.2f);
                    mBtnSearchEnter.setEnabled(false);
                    return false;
                }

                String keyword = removeLastSpace(mEtSearchNote.getText().toString());
                mEtSearchNote.setText(keyword);
                mBtnSearchEnter.performClick();
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
                mBtnSearchEnter.setAlpha(0.2f);
                mBtnSearchEnter.setEnabled(false);
                return;
            }

            utility.StringIllegal.checkFirstSpaceChar(mEtSearchNote);
            utility.StringIllegal.checkIllegalChar(mEtSearchNote);

            if (s.toString().equals(" ")) {
                mEtSearchNote.setText("");
                mBtnSearchEnter.setAlpha(0.2f);
                mBtnSearchEnter.setEnabled(false);
                return;
            }

            mBtnSearchEnter.setAlpha(1.0f);
            mBtnSearchEnter.setEnabled(true);
        }
    };

    private String removeLastSpace(String inputString) {
        while (inputString.substring(inputString.length() - 1).equals(" ") || inputString.substring(inputString.length() - 1).equals("　")) {
            inputString = inputString.substring(0, inputString.length() - 1);
        }
        return inputString;
    }

    private void changeLauncherListType(int type) {
        mLauncherListType = type;
        mLayoutNormalMode.setVisibility(View.INVISIBLE);
        mLayoutManageMode.setVisibility(View.INVISIBLE);
        mLayoutSearchMode.setVisibility(View.INVISIBLE);
        mLayoutImportMode.setVisibility(View.INVISIBLE);
        mLayoutFreeStorageMode.setVisibility(View.INVISIBLE);
        mTvSearchNotFoundHint.setVisibility(View.GONE);

        switch (mLauncherListType) {
            case LauncherListType.NORMAL:
                mLayoutNormalMode.setVisibility(View.VISIBLE);
                mLauncherListAdapter.updateList(Bookshelf.getInstance().getBookList());
                mLauncherListAdapter.setListSelected(false);
                mLauncherListAdapter.notifyDataSetChanged();
                mEtSearchImportFileInput.setText("");
                mLayoutSearchImport.setVisibility(View.GONE);
                mBtnSearchImport.setVisibility(View.VISIBLE);
                mLayoutLauncherImportBackup.setVisibility(View.GONE);
                mLayoutLauncherFreeStorage.setVisibility(View.GONE);
                mLayoutNoteList.setVisibility(View.VISIBLE);
                mEtSearchNote.setText("");
                hideInputMethod(mEtSearchNote);
                updateLauncherListView();
                updatePageInfo();
                resetImportView();
                break;
            case LauncherListType.MANAGE:
                mLayoutManageMode.setVisibility(View.VISIBLE);
                mLayoutNoteList.setVisibility(View.VISIBLE);
                hideInputMethod(mEtSearchNote);
                updateManageViews();
                updateLauncherListView();
                updatePageInfo();
                break;
            case LauncherListType.SEARCH:
                mLayoutSearchMode.setVisibility(View.VISIBLE);
                mLayoutNoteList.setVisibility(View.VISIBLE);
                mLauncherListAdapter.setListSelected(false);
                mEtSearchNote.requestFocus();
                showInputMethod();
                updateLauncherListView();
                updatePageInfo();
                break;
            case LauncherListType.MANAGE_SEARCH:
                mLayoutManageMode.setVisibility(View.VISIBLE);
                hideInputMethod(mEtSearchNote);
                updateManageViews();
                updateLauncherListView();
                updatePageInfo();
                break;
            case LauncherListType.IMPORT:
                mLayoutImportMode.setVisibility(View.VISIBLE);
                mLayoutNoteList.setVisibility(View.GONE);
                mLayoutPageInfo.setVisibility(View.GONE);
                mLayoutLauncherImportBackup.setVisibility(View.VISIBLE);
                break;
            case LauncherListType.FREE_STORAGE:
                mLayoutFreeStorageMode.setVisibility(View.VISIBLE);
                mLayoutNoteList.setVisibility(View.GONE);
                mLayoutPageInfo.setVisibility(View.GONE);
                mFreeStorageListAdapter.updateList(Bookshelf.getInstance().getBookList());
                mSortFreeStorageListProperty = ListSort.DATE_ASCENDING;
                mFreeStorageListAdapter.sortByDateAscending();
                mFreeStorageListAdapter.setListSelected(false);
                mFreeStorageListAdapter.notifyDataSetChanged();
                mFreeStorageListCurrentPage = 1;
                mFreeStorageListAdapter.setCurrentPage(mFreeStorageListCurrentPage);
                mLayoutLauncherFreeStorage.setVisibility(View.VISIBLE);
                updateFreeStorageListViewSelectStatus();
                break;
        }
    }

    private void createNewNoteBookAndOpen() {

        if (isSpaceAvailable()) {
            mIsNewNoteCreating = true;
            Bookshelf.getInstance().newBook(Global.generateNoteName());
            Book currentBook = Bookshelf.getInstance().getCurrentBook();
            int page = mLauncherListAdapter.getPageOfItemInCurrentList(currentBook);
            if (mLauncherListAdapter.getCurrentPage() != page)
                switchToPage(page);

            switchToNoteEditor();
        } else {
            //Dango 20181005 Bugfix - waitDialog did not disappear when space not enough
            Global.closeWaitDialog(this);
            showStorageNotEnoughAlertDialog();
        }
    }

    private void openNoteBook(final Book nb) {
        Runnable openNoteBookRunnable = new Runnable() {
            @Override
            public void run() {
                Bookshelf.getInstance().setCurrentBook(nb.getUUID());
                switchToNoteEditor();
                Global.closeWaitDialog(NtxLauncherActivity.this);
            }
        };
        mHandler.removeCallbacks(openNoteBookRunnable);
        mHandler.postDelayed(openNoteBookRunnable, 500);
    }

    private void updateLauncherListView() {
        List<Book> currentPageList = mLauncherListAdapter.getCurrentPageList();
        int noteType = 0;
        switch (mLauncherListType) {
            case LauncherListType.NORMAL:
            case LauncherListType.SEARCH:
                mLauncherListItems.get(0).setEnabled(true);
                noteType = NoteType.NOTE_BOOK;
                break;
            case LauncherListType.MANAGE:
            case LauncherListType.MANAGE_SEARCH:
                mLauncherListItems.get(0).setEnabled(false);
                noteType = NoteType.NOTE_BOOK_CHECKABLE;
                break;
        }

        mLauncherListItems.get(0).setType(NoteType.CREATE_NOTE, "");
        mLauncherListItems.get(0).setVisibility(View.VISIBLE);

        for (int i = 1; i < mLauncherListItems.size(); i++) {
            mLauncherListItems.get(i).setVisibility(View.INVISIBLE);
        }

        for (int i = 0; i < currentPageList.size(); i++) {
            mLauncherListItems.get(i + 1).setType(noteType, currentPageList.get(i).getTitle());

            if (PreviewMode.TITLE == mPreviewMode) {
                mLauncherListItems.get(i + 1).updateIconPreview(null);
            } else if (PreviewMode.THUMBNAIL == mPreviewMode) {
                mLauncherListItems.get(i + 1).updateIconPreview(currentPageList.get(i));
            }

            mLauncherListItems.get(i + 1).updateCheckBox(mLauncherListAdapter.isBookSelected(currentPageList.get(i).getUUID()));
            mLauncherListItems.get(i + 1).setVisibility(View.VISIBLE);
            mLauncherListItems.get(i + 1).setEnabled(true);
        }
    }

    private void updateLauncherListSelectedStatus() {
        List<Book> currentPageList = mLauncherListAdapter.getCurrentPageList();

        for (int i = 0; i < currentPageList.size(); i++) {
            boolean isSelect = mLauncherListAdapter.isBookSelected(currentPageList.get(i).getUUID());
            mLauncherListItems.get(i + 1).updateCheckBox(isSelect);
        }
    }

    private void showSortBookPopupWindow() {
        String[] stringsSortCategory = new String[3];
        stringsSortCategory[0] = getString(R.string.ntxbookshelf_sort_last_modified);
        stringsSortCategory[1] = getString(R.string.ntxbookshelf_sort_name);
        stringsSortCategory[2] = getString(R.string.ntxbookshelf_sort_created);

        final CommonListPopupWindow sortBookPopupWindow = new CommonListPopupWindow(
                this,
                getString(R.string.sort),
                stringsSortCategory,
                Bookshelf.getInstance().getPreviewOrder());

        sortBookPopupWindow.showAtLocation(mRootLayout, Gravity.CENTER, 0, 0);

        sortBookPopupWindow.setOnItemClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mPreviewOrder = (int) view.getTag();
                Bookshelf.getInstance().setPreviewOrder(mPreviewOrder);
                Bookshelf.getInstance().sortBookList(true);
                mLauncherListAdapter.updateList(Bookshelf.getInstance().getBookList());
                switchToPage(1);
                sortBookPopupWindow.dismiss();
            }
        });
    }

    private void showPreviewModePopupWindow() {
        String[] stringsPreviewCategory = new String[2];

        stringsPreviewCategory[0] = getString(R.string.ntxbookshelf_preview_title);
        stringsPreviewCategory[1] = getString(R.string.ntxbookshelf_preview_thumbnail);
        final CommonListPopupWindow previewModePopupWindow = new CommonListPopupWindow(this, getString(R.string.preview), stringsPreviewCategory, mPreviewMode);
        previewModePopupWindow.showAtLocation(mRootLayout, Gravity.CENTER, 0, 0);

        previewModePopupWindow.setOnItemClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mPreviewMode = (int) view.getTag();
                updateLauncherListView();
                previewModePopupWindow.dismiss();
            }
        });
    }

    private void getInternalBackupFileList() {
        mIsLoadingInternalBackupFileList = true;
        SearchFileByExtensionAsyncTask searchInternalTask = new SearchFileByExtensionAsyncTask();
        searchInternalTask.searchFinishCallback = new AsyncTaskResult<List<File>>() {
            @Override
            public void taskFinish(List<File> result) {
                List<RestoreItem> tempList = new ArrayList<>();
                for (File file : result) {
                    tempList.add(new RestoreItem(file.getPath(), file.getName(), file.lastModified(),
                            file.length()));
                }
                getImportList(tempList, mInternalImportList, mInternalImportListAdapter, ImportFileVia.INTERNAL);
            }
        };
        searchInternalTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, INTERNAL_PATH, SEARCH_FILE_TYPE);
    }

    private void getExternalBackupFileList() {
        mIsLoadingExternalBackupFileList = true;
        SearchFileByExtensionAsyncTask searchExternalTask = new SearchFileByExtensionAsyncTask();
        searchExternalTask.searchFinishCallback = new AsyncTaskResult<List<File>>() {
            @Override
            public void taskFinish(List<File> result) {
                List<RestoreItem> tempList = new ArrayList<>();
                for (File file : result) {
                    tempList.add(new RestoreItem(file.getPath(), file.getName(), file.lastModified(),
                            file.length()));
                }
                getImportList(tempList, mExternalImportList, mExternalImportListAdapter, ImportFileVia.EXTERNAL);
            }
        };
        searchExternalTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, EXTERNAL_PATH, SEARCH_FILE_TYPE);
    }

    private void getDropboxBackupFileList() {
        DropboxSimpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        dbx.registerOnMetaFileListLoadedListener(new Dropbox.OnMetadataFileListLoadedListener() {
            @Override
            public void onMetaFileListLoaded(List<Metadata> metadataFileList) {
                mDropboxMetadataList.clear();
                mDropboxMetadataList.addAll(metadataFileList);
                mDropboxImportList.clear();
                mDropboxImportList.addAll(transformMetadataListToImportItemList(mDropboxMetadataList));
                mDropboxImportListAdapter.updateList(mDropboxImportList);
                switch (mSortImportListProperty) {
                    case ListSort.DATE_ASCENDING:
                        mDropboxImportListAdapter.sortByDateAscending();
                        break;
                    case ListSort.DATE_DESCENDING:
                        mDropboxImportListAdapter.sortByDateDescending();
                        break;
                    case ListSort.NAME_ASCENDING:
                        mDropboxImportListAdapter.sortByNameAscending();
                        break;
                    case ListSort.NAME_DESCENDING:
                        mDropboxImportListAdapter.sortByNameDescending();
                        break;
                    case ListSort.SIZE_ASCENDING:
                        mDropboxImportListAdapter.sortBySizeAscending();
                        break;
                    case ListSort.SIZE_DESCENDING:
                        mDropboxImportListAdapter.sortBySizeDescending();
                        break;
                }
                mDropboxImportListAdapter.setCurrentPage(1);
                mDropboxImportListAdapter.notifyDataSetChanged();
                updateImportListView();
            }
        });
        dbx.getDropboxFileName();
    }

    private List<ImportItem> transformMetadataListToImportItemList(List<Metadata> metadataList) {
        List<ImportItem> importItemList = new ArrayList<>();
        for (int i = 0; i < metadataList.size(); i++) {
            Gson gson = new Gson();
            DropboxNoteData data = gson.fromJson(metadataList.get(i).toStringMultiline(), DropboxNoteData.class);

            long itemDate = 0;
            try {
                itemDate = DropboxSimpleDateFormat.parse(data.getServerModified()).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (data.getName().endsWith(SEARCH_FILE_TYPE)) {
                RestoreItem restoreItem = new RestoreItem(data.getPathDisplay(), data.getName(), itemDate, data.getSize());
                importItemList.add(new ImportItem(data.getId(), restoreItem));
            }
        }
        return importItemList;
    }

    private void getImportList(List<RestoreItem> inputList, final List<ImportItem> outputList, final ImportListAdapter adapter, final int importFileVia) {
        GetImportListAsyncTask getImportListAsyncTask = new GetImportListAsyncTask(inputList);
        getImportListAsyncTask.groupSameUuidListFinishCallback = new AsyncTaskResult<List<ImportItem>>() {
            @Override
            public void taskFinish(List<ImportItem> result) {
                outputList.clear();
                outputList.addAll(result);
                adapter.updateList(outputList);
                switch (mSortImportListProperty) {
                    case ListSort.DATE_ASCENDING:
                        adapter.sortByDateAscending();
                        break;
                    case ListSort.DATE_DESCENDING:
                        adapter.sortByDateDescending();
                        break;
                    case ListSort.NAME_ASCENDING:
                        adapter.sortByNameAscending();
                        break;
                    case ListSort.NAME_DESCENDING:
                        adapter.sortByNameDescending();
                        break;
                    case ListSort.SIZE_ASCENDING:
                        adapter.sortBySizeAscending();
                        break;
                    case ListSort.SIZE_DESCENDING:
                        adapter.sortBySizeDescending();
                        break;
                }
                adapter.notifyDataSetChanged();
                int importFileSpinnerSelection = mSpinnerImportFileVia.getSelectedItemPosition();
                if (ImportFileVia.INTERNAL == importFileVia) {
                    mIsLoadingInternalBackupFileList = false;
                } else if (ImportFileVia.EXTERNAL == importFileVia) {
                    mIsLoadingExternalBackupFileList = false;
                }
                if (importFileVia == importFileSpinnerSelection) {
                    updateImportListView();
                }
            }
        };
        getImportListAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void operateBackupFileOnDropboxServer(FileAction action, List<ImportItem> list) {
        List<FileMetadata> selectedList = new ArrayList<>();
        for (ImportItem item : list) {
            for (Metadata metadata : mDropboxMetadataList) {
                if (metadata.getName().equals(item.getFileName())) {
                    selectedList.add((FileMetadata) metadata);
                }
            }
        }
        performWithPermissions(action, selectedList);
    }

    private void performWithPermissions(final FileAction action, List<FileMetadata> fileList) {
        if (hasPermissionsForAction(action)) {
            switch (action) {
                case DOWNLOAD:
                    new DownloadDropboxListFilesTask(this, fileList).execute();
                    break;
                case DELETE:
                    new DeleteDropboxListFilesTask(this, fileList).execute();
                    break;
                default:
                    break;
            }
            return;
        }

        if (shouldDisplayRationaleForAction(action)) {
            new android.support.v7.app.AlertDialog.Builder(this)
                    .setMessage("This app requires storage access to download and upload files.")
                    .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissionsForAction(action);
                        }
                    })
                    .setNegativeButton(getString(android.R.string.cancel), null)
                    .create()
                    .show();
        } else {
            requestPermissionsForAction(action);
        }
    }

    private boolean hasPermissionsForAction(FileAction action) {
        for (String permission : action.getPermissions()) {
            int result = ContextCompat.checkSelfPermission(this, permission);
            if (result == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldDisplayRationaleForAction(FileAction action) {
        for (String permission : action.getPermissions()) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    private void requestPermissionsForAction(FileAction action) {
        ActivityCompat.requestPermissions(
                this,
                action.getPermissions(),
                action.getCode()
        );
    }

    private enum FileAction {
        DOWNLOAD(Manifest.permission.WRITE_EXTERNAL_STORAGE),
        UPLOAD(Manifest.permission.READ_EXTERNAL_STORAGE),
        DELETE;

        private static final FileAction[] values = values();

        private final String[] permissions;

        FileAction(String... permissions) {
            this.permissions = permissions;
        }

        public int getCode() {
            return ordinal();
        }

        public String[] getPermissions() {
            return permissions;
        }

        public static FileAction fromCode(int code) {
            if (code < 0 || code >= values.length) {
                throw new IllegalArgumentException("Invalid FileAction code: " + code);
            }
            return values[code];
        }
    }

    private void checkSelectedListIsExistInLauncherList() {
        List<ImportItem> selectedList;
        if (ImportFileVia.INTERNAL == mSpinnerImportFileVia.getSelectedItemPosition())
            selectedList = mInternalImportListAdapter.getSelectedList();
        else
            selectedList = mExternalImportListAdapter.getSelectedList();

        List<String> selectedUuidList = new ArrayList<>();
        for (int i = 0; i < selectedList.size(); i++) {
            selectedUuidList.add(selectedList.get(i).getItemUuid());
        }

        HashMap<String, String> currentLauncherUuidTitleHashMap = new HashMap<>();
        for (Book book : Bookshelf.getInstance().getBookList()) {
            currentLauncherUuidTitleHashMap.put(book.getUUID().toString(), book.getTitle());
        }

        mConflictImportList.clear();
        mUuidTitleHashMap.clear();
        for (int i = 0; i < selectedUuidList.size(); i++) {
            String noteTitle = currentLauncherUuidTitleHashMap.get(selectedUuidList.get(i));
            if (noteTitle != null) {
                ImportItem newItem = new ImportItem(selectedList.get(i).getItemUuid(), selectedList.get(i));
                newItem.setItemSelected(true);
                mConflictImportList.add(newItem);
                mUuidTitleHashMap.put(selectedUuidList.get(i), noteTitle);
            }
        }
    }

    private void showConflictDialogFragment() {
        ImportConflictList importConflictList = new ImportConflictList(mConflictImportList, mUuidTitleHashMap);
        ImportConflictDialogFragment fragment = ImportConflictDialogFragment.newInstance(importConflictList);
        fragment.setOnFinishedConflictItemSelectionListener(new ImportConflictDialogFragment.OnFinishedConflictItemSelection() {
            @Override
            public void onFinishedSelection(List<ImportItem> resultList) {
                List<ImportItem> selectedList;
                if (ImportFileVia.INTERNAL == mSpinnerImportFileVia.getSelectedItemPosition())
                    selectedList = mInternalImportListAdapter.getSelectedList();
                else
                    selectedList = mExternalImportListAdapter.getSelectedList();

                for (ImportItem item : resultList) {
                    innerLoop:
                    if (!item.isItemSelected()) {
                        for (ImportItem selectedListItem : selectedList) {
                            if (selectedListItem.getItemUuid().equals(item.getItemUuid())) {
                                selectedList.remove(selectedListItem);
                                break innerLoop;
                            }
                        }
                    }
                }

                importSelectedList(selectedList);
            }
        });
        showDialogFragment(fragment, ImportConflictDialogFragment.class.getSimpleName());
    }

    private void deleteSelectedBackup() {
        final List<ImportItem> selectedList;
        if (ImportFileVia.INTERNAL == mSpinnerImportFileVia.getSelectedItemPosition())
            selectedList = mInternalImportListAdapter.getSelectedList();
        else
            selectedList = mExternalImportListAdapter.getSelectedList();

        int count = selectedList.size();

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        String dialogTag = "delete_backup_confirm";
        String deleteConfirmMessage = count + " " + getResources().getString(R.string.delete_backup_confirm);
        AlertDialogFragment deleteBackupConfirmDialogFragment = AlertDialogFragment.newInstance(deleteConfirmMessage, R.drawable.writing_ic_error, true, dialogTag);
        deleteBackupConfirmDialogFragment.setupNegativeButton(getString(android.R.string.no));
        deleteBackupConfirmDialogFragment.registerAlertDialogButtonClickListener(new AlertDialogButtonClickListener() {
            @Override
            public void onPositiveButtonClick(String fragmentTag) {
                new DeleteBackupFileAsyncTask().execute(selectedList);
            }

            @Override
            public void onNegativeButtonClick(String fragmentTag) {
            }
        }, dialogTag);

        ft.replace(R.id.alert_dialog_container, deleteBackupConfirmDialogFragment, dialogTag)
                .commit();
    }

    private boolean isWifiConnected() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    private class DeleteBackupFileAsyncTask extends AsyncTask<List<ImportItem>, Integer, Void> {
        private boolean interruptDelete = false;
        private int deleteCounter;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            FragmentTransaction ft = getFragmentManager().beginTransaction();

            // Set interruptable= false. Let user can not interrupt.
            mProgressingDialogFragment = InterruptibleProgressingDialogFragment
                    .newInstance(getString(R.string.deleting), mLauncherListAdapter.getSelectedBookList().size(), true);

            mProgressingDialogFragment.setOnInterruptButtonClickListener(
                    new InterruptibleProgressingDialogFragment.OnInterruptButtonClickListener() {
                        @Override
                        public void onClick() {
                            interruptDelete = true;
                        }
                    });

            ft.replace(R.id.alert_dialog_container, mProgressingDialogFragment,
                    InterruptibleProgressingDialogFragment.class.getSimpleName()).commit();
        }

        @Override
        protected Void doInBackground(List<ImportItem>[] lists) {
            List<ImportItem> selectedList = lists[0];
            for (int i = 0; i < selectedList.size(); i++) {
                String filePath = selectedList.get(i).getFilePath();
                File file = new File(filePath);
                file.delete();
                deleteCounter++;
                publishProgress(i);

                if (interruptDelete)
                    break;
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            mProgressingDialogFragment.updateProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mTvImportListEmptyHint.setText(getString(R.string.loading_notebook));
            clearImportListView();
            mBtnSelectAllImport.setUnchecked();
            mBtnDeleteSelectedBackupFile.setVisibility(View.GONE);
            mBtnImportSelectedBackupFile.setVisibility(View.GONE);
            mTvImportListSelectedCount.setText(String.valueOf(0));
            getInternalBackupFileList();
            getExternalBackupFileList();

            String alertMsg = deleteCounter + " " + getResources().getString(R.string.toolbox_message_delete_success);
            showAlertMessageDialog(alertMsg, true);
        }
    }

    private class ListLayoutFlingListener extends GestureDetector.SimpleOnGestureListener {
        int FLYING_GESTURE_MIN_VELOCITY = 0;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() - e2.getX() > FLYING_GESTURE_HORIZONTAL_MIN_DISTANCE && Math.abs(velocityX) > FLYING_GESTURE_MIN_VELOCITY) {
                nextPage();
            } else if (e2.getX() - e1.getX() > FLYING_GESTURE_HORIZONTAL_MIN_DISTANCE && Math.abs(velocityX) > FLYING_GESTURE_MIN_VELOCITY) {
                prePage();
            }
            return false;
        }
    }

    private void sendEmail() {
        File mailTempDir = new File(Global.PATH_SDCARD + Global.MAIL_FILE_TEMP_DIR);

        ArrayList<Uri> uris = new ArrayList<>();
        String[] children = mailTempDir.list();
        if (children != null) {
            for (String child : children) {
                uris.add(Uri.fromFile(new File(mailTempDir, child)));
            }
        }

        String subject = "File from Notes";
        String[] emailTo = {""};

        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, emailTo);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Please find the attachment.");
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        try {
            startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.exportBackup_email_login)));
        } catch (android.content.ActivityNotFoundException ex) {
            Log.e("", "There are no email clients installed");
        }
    }
}
