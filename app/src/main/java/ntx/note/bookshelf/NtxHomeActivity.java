package ntx.note.bookshelf;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import name.vbraun.lib.pen.Hardware;
import name.vbraun.lib.pen.PenEventNTX;
import name.vbraun.view.write.Page;
import ntx.draw.nDrawHelper;
import ntx.note.CallbackEvent;
import ntx.note.Global;
import ntx.note.RelativePopupWindow;
import ntx.note.data.Book;
import ntx.note.data.Bookshelf;
import ntx.note.data.StorageAndroid;
import ntx.note.data.TagManager;
import ntx.note.export.AlertDialogButtonClickListener;
import ntx.note.export.AlertDialogFragment;
import ntx.note.export.BackupDialogFragment;
import ntx.note.export.InterruptibleProgressingDialogFragment;
import ntx.note2.R;

public class NtxHomeActivity extends Activity implements AlertDialogButtonClickListener {

    public @interface LauncherTab {
        int NOTE = 0;
        int READER = 1;
        int CALENDAR = 2;
        int MORE = 3;
    }

    public @interface BookBackground {
        int NONE = 0;
        int BOOK = 1;
    }

    private final static String GET_RECENT_BOOK_APP_PACKAGE_NAME = "ntx.reader3";
    private final static String DIALOG_TAG_STORAGE_NOTE_ENOUGH = "storage_not_enough";
    private final static long LIMIT_SPACE = 20; // if space >20MB return ture; <=20MB return false;
    private final static int RECENT_BOOK_LIST_SIZE = 4;
    private final static int WAIT_TIME_FOR_BOOK_SHELF_INITIALIZATION = 500;

    private Context mContext;
    private View layout;

    private List<RecentlyNoteData> mRecentlyNoteList;

    private RelativeLayout[] mTabButtons = new RelativeLayout[4];

    // ======= note ======
    private ImageView[] ivNoteCover = new ImageView[3];
    private TextView[] tvNoteTitle = new TextView[3];
    private LinearLayout[] llNote = new LinearLayout[3];
    // ======= reader ======
    private LinearLayout[] llBook = new LinearLayout[4];
    private ImageView[] ivBookCover = new ImageView[4];
    private TextView[] tvBookTitle = new TextView[4];

    private ApplicationInfo apInfo;

    private Handler mHandler = new Handler();
    private boolean mIsInitBookshelfFinished = false;
    private boolean mIsNewNoteCreating = false;

    private InterruptibleProgressingDialogFragment mProgressingDialogFragment;
    private EventBus mEventBus;

    private BroadcastReceiver mSaveRecentBookListServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            initBook();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Global.APP_DATA_PACKAGE_FILES_PATH = getFilesDir() + "/";

        mEventBus = EventBus.getDefault();

        mContext = getApplicationContext();
        layout = getLayoutInflater().inflate(R.layout.ntx_home_activity, null);
        setContentView(layout);

//        Global.APP_DATA_PACKAGE_FILES_PATH = getFilesDir() + "/";

        // Set external SD-Card storage path
//        Global.PACKAGE_DATA_DIR = getApplicationInfo().dataDir;
//		Global.getExternalMounts();

        PackageManager packageManager = getPackageManager();

        try {
            apInfo = packageManager.getApplicationInfo(GET_RECENT_BOOK_APP_PACKAGE_NAME, PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }


        initInstantKeys();
        initBookshelf();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void changeEinkControlPermission(boolean isForNtxAppsOnly) {
        Intent changePermissionIntent = new Intent("ntx.eink_control.CHANGE_PERMISSION");
        changePermissionIntent.putExtra("isPermissionNtxApp", isForNtxAppsOnly);
        sendBroadcast(changePermissionIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        changeEinkControlPermission(true);
        Global.closeWaitDialog(this);
        mEventBus.register(this);

        registerReceiver(mSaveRecentBookListServiceReceiver, new IntentFilter(Global.SAVE_RECENT_BOOK_FINISHED_NOTIFICATION));

        // disable tab under line.
        for (RelativeLayout btnTab : mTabButtons) {
            btnTab.setSelected(false);
        }

        Runnable runInit = new Runnable() {
            @Override
            public void run() {
                updateNote();
                clearAll();
                initNote();
                initBook();
            }
        };
        // fix framework invalidate issue, when hibernation back onresume.
        mHandler.removeCallbacks(runInit);
        mHandler.postDelayed(runInit, 500);

        // Daniel 20180920 : Always disable the nDraw , refresh mode and 2-Step-Suspend
        // in Home page
        nDrawHelper.NDrawSwitch(false);
        doFullRefresh(500);
        PowerEnhanceSet(0);
        resetDropFrames();
    }

    @Override
    protected void onPause() {
        super.onPause();
        changeEinkControlPermission(false);
        mEventBus.unregister(this);
        mIsNewNoteCreating = false;
        unregisterReceiver(mSaveRecentBookListServiceReceiver);
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public void onPositiveButtonClick(String fragmentTag) {

    }

    @Override
    public void onNegativeButtonClick(String fragmentTag) {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CallbackEvent event) {
        if (event.getMessage().equals(CallbackEvent.SAVE_RECENTLY_NOTE_JSON_DONE)) {
            updateNote();
        } else if (event.getMessage().equals(CallbackEvent.BACKUP_NOTE_EMAIL)) {
            sendEmail();
        }

    }

    private void initInstantKeys() {
        RelativeLayout btnTabNote = (RelativeLayout) findViewById(R.id.btn_tab_note);
        RelativeLayout btnTabReader = (RelativeLayout) findViewById(R.id.btn_tab_reader);
        RelativeLayout btnTabCalendar = (RelativeLayout) findViewById(R.id.btn_tab_calendar);
        RelativeLayout btnTabMore = (RelativeLayout) findViewById(R.id.btn_tab_more);
        mTabButtons[0] = btnTabNote;
        mTabButtons[1] = btnTabReader;
        mTabButtons[2] = btnTabCalendar;
        mTabButtons[3] = btnTabMore;
        for (RelativeLayout btnTab : mTabButtons) {
            btnTab.setOnClickListener(onTabButtonsClickListener);
        }

        // ========== note ==========
        Button btnAllNotes = (Button) findViewById(R.id.btn_all_notes);
        btnAllNotes.setOnClickListener(instantKeyListener);

        LinearLayout layoutCreateNote = (LinearLayout) findViewById(R.id.layout_create_note);
        layoutCreateNote.setOnClickListener(instantKeyListener);

        ivNoteCover[0] = (ImageView) findViewById(R.id.iv_note_book1);
        ivNoteCover[1] = (ImageView) findViewById(R.id.iv_note_book2);
        ivNoteCover[2] = (ImageView) findViewById(R.id.iv_note_book3);

        tvNoteTitle[0] = (TextView) findViewById(R.id.tv_note_book1);
        tvNoteTitle[1] = (TextView) findViewById(R.id.tv_note_book2);
        tvNoteTitle[2] = (TextView) findViewById(R.id.tv_note_book3);

        llNote[0] = (LinearLayout) findViewById(R.id.layout_note_book1);
        llNote[1] = (LinearLayout) findViewById(R.id.layout_note_book2);
        llNote[2] = (LinearLayout) findViewById(R.id.layout_note_book3);
        for (LinearLayout note : llNote) {
            note.setOnClickListener(onNoteClickListener);
            note.setOnLongClickListener(onNoteLongClickListener);
        }

        // ========== reader ==========
        Button btnAllBooks = (Button) findViewById(R.id.btn_all_books);
        btnAllBooks.setOnClickListener(onBookClickListener);

        ivBookCover[0] = (ImageView) findViewById(R.id.iv_reader_book1);
        ivBookCover[1] = (ImageView) findViewById(R.id.iv_reader_book2);
        ivBookCover[2] = (ImageView) findViewById(R.id.iv_reader_book3);
        ivBookCover[3] = (ImageView) findViewById(R.id.iv_reader_book4);

        tvBookTitle[0] = (TextView) findViewById(R.id.tv_reader_book1);
        tvBookTitle[1] = (TextView) findViewById(R.id.tv_reader_book2);
        tvBookTitle[2] = (TextView) findViewById(R.id.tv_reader_book3);
        tvBookTitle[3] = (TextView) findViewById(R.id.tv_reader_book4);

        llBook[0] = (LinearLayout) findViewById(R.id.layout_reader_book1);
        llBook[1] = (LinearLayout) findViewById(R.id.layout_reader_book2);
        llBook[2] = (LinearLayout) findViewById(R.id.layout_reader_book3);
        llBook[3] = (LinearLayout) findViewById(R.id.layout_reader_book4);
        for (LinearLayout book : llBook) {
            book.setOnClickListener(onBookClickListener);
            book.setOnLongClickListener(onBookLongClickListener);
        }
    }

    private void setTabSelected(int launcherTab) {
        for (RelativeLayout btnTab : mTabButtons) {
            btnTab.setSelected(false);
        }
        mTabButtons[launcherTab].setSelected(true);
    }

    private View.OnClickListener onTabButtonsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Global.openWaitDialog(NtxHomeActivity.this);
            switch (view.getId()) {
                case R.id.btn_tab_note:
                    setTabSelected(LauncherTab.NOTE);
                    switchToNote();
                    break;
                case R.id.btn_tab_reader:
                    setTabSelected(LauncherTab.READER);
                    switchToReader(Global.KEY_READER_MAIN_PAGE);
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

    private OnClickListener instantKeyListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            Global.openWaitDialog(NtxHomeActivity.this);

            switch (v.getId()) {
                case R.id.btn_all_notes:
                    switchToNote();
                    break;
                case R.id.layout_create_note:
                    createNewNote();
                    break;
            }
        }
    };

    private View.OnClickListener onNoteClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.layout_note_book1:
                    openNote(0);
                    break;
                case R.id.layout_note_book2:
                    openNote(1);
                    break;
                case R.id.layout_note_book3:
                    openNote(2);
                    break;
                default:
                    break;
            }
        }
    };

    private View.OnLongClickListener onNoteLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            int horizPos;
            if (view.getId() == R.id.layout_note_book3) {
                horizPos = RelativePopupWindow.HorizontalPosition.ALIGN_RIGHT;
            } else {
                horizPos = RelativePopupWindow.HorizontalPosition.CENTER;
            }

            HomeNoteLongPressedPopupWindow noteLongPressedPopupWindow = new HomeNoteLongPressedPopupWindow(NtxHomeActivity.this, view.getId());
            noteLongPressedPopupWindow.setOnBtnClickListener(onNotePopupWindowBtnClickListener);
            noteLongPressedPopupWindow.showOnAnchor(view, horizPos, RelativePopupWindow.VerticalPosition.BELOW, 0, -150);
            return true;
        }
    };

    private HomeNoteLongPressedPopupWindow.OnBtnClickListener onNotePopupWindowBtnClickListener = new HomeNoteLongPressedPopupWindow.OnBtnClickListener() {
        @Override
        public void onBtnClicked(int itemViewId, int actionViewId) {
            int index = 0;
            switch (itemViewId) {
                case R.id.layout_note_book1:
                    index = 0;
                    break;
                case R.id.layout_note_book2:
                    index = 1;
                    break;
                case R.id.layout_note_book3:
                    index = 2;
                    break;
            }

            switch (actionViewId) {
                case R.id.btn_rename:
                    showRenameDialogFragment(index);
                    break;
                case R.id.btn_copy:
                    copySelectedNote(index);
                    break;
                case R.id.btn_backup:
                    backupSelectedNote(index);
                    break;
                case R.id.btn_delete:
                    deleteSelectedNote(index);
                    break;
            }
        }
    };

    private void showRenameDialogFragment(final int index) {
        Runnable showRenameDialogRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsInitBookshelfFinished) {
                    Global.closeWaitDialog(NtxHomeActivity.this);
                    Book book = Bookshelf.getInstance().getBook(mRecentlyNoteList.get(index).getId());
                    Fragment renameFragment = RenameNoteDialogFragment.newInstance(book.getUUID(), true);
                    showDialogFragment(renameFragment, RenameNoteDialogFragment.class.getSimpleName());
                } else {
                    Global.openWaitDialog(NtxHomeActivity.this);
                    mHandler.postDelayed(this, WAIT_TIME_FOR_BOOK_SHELF_INITIALIZATION);
                }
            }
        };
        mHandler.post(showRenameDialogRunnable);
    }

    private void copySelectedNote(final int index) {
        Runnable copySelectedNoteRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsInitBookshelfFinished) {
                    new CopySelectedBookAsyncTask(mRecentlyNoteList.get(index).getId().toString()).execute();
                } else {
                    mHandler.postDelayed(this, WAIT_TIME_FOR_BOOK_SHELF_INITIALIZATION);
                }
            }
        };
        Global.openWaitDialog(this);
        mHandler.postDelayed(copySelectedNoteRunnable, 1000);
    }

    private void backupSelectedNote(final int index) {
        Runnable backupSelectedNoteRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsInitBookshelfFinished) {
                    Global.closeWaitDialog(NtxHomeActivity.this);
                    BackupDialogFragment backupSingleNoteDialogFragment = BackupDialogFragment.newInstance(
                            mRecentlyNoteList.get(index).getId(),
                            false,
                            false);
                    showDialogFragment(backupSingleNoteDialogFragment, BackupDialogFragment.class.getSimpleName());
                } else {
                    Global.openWaitDialog(NtxHomeActivity.this);
                    mHandler.postDelayed(this, WAIT_TIME_FOR_BOOK_SHELF_INITIALIZATION);
                }
            }
        };
        mHandler.post(backupSelectedNoteRunnable);
    }

    private void deleteSelectedNote(final int index) {
        Runnable deleteSelectedNoteRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsInitBookshelfFinished) {
                    Global.closeWaitDialog(NtxHomeActivity.this);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    String dialogTag = "delete_confirm";
                    String deleteConfirmMessage = getResources().getString(R.string.toolbox_message_delete_confirm, 1 + "");
                    AlertDialogFragment deleteConfirmDialogFragment = AlertDialogFragment.newInstance(deleteConfirmMessage, R.drawable.writing_ic_error, true, dialogTag);

                    deleteConfirmDialogFragment.setupPositiveButton(getString(android.R.string.yes));
                    deleteConfirmDialogFragment.setupNegativeButton(getString(android.R.string.no));
                    deleteConfirmDialogFragment.registerAlertDialogButtonClickListener(new AlertDialogButtonClickListener() {

                        @Override
                        public void onPositiveButtonClick(String fragmentTag) {
                            new DeleteNoteBookAsyncTask().execute(mRecentlyNoteList.get(index).getId().toString());
                        }

                        @Override
                        public void onNegativeButtonClick(String fragmentTag) {
                        }
                    }, dialogTag);

                    ft.replace(R.id.alert_dialog_container, deleteConfirmDialogFragment, dialogTag)
                            .commit();

                } else {
                    Global.openWaitDialog(NtxHomeActivity.this);
                    mHandler.postDelayed(this, WAIT_TIME_FOR_BOOK_SHELF_INITIALIZATION);
                }
            }
        };
        mHandler.post(deleteSelectedNoteRunnable);
    }

    private View.OnClickListener onBookClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Global.openWaitDialog(NtxHomeActivity.this);
            switch (view.getId()) {
                case R.id.btn_all_books:
                    switchToReader(Global.KEY_ALL_BOOKS);
                    break;
                case R.id.layout_reader_book1:
                    openBook((RecentlyBookData) llBook[0].getTag());
                    break;
                case R.id.layout_reader_book2:
                    openBook((RecentlyBookData) llBook[1].getTag());
                    break;
                case R.id.layout_reader_book3:
                    openBook((RecentlyBookData) llBook[2].getTag());
                    break;
                case R.id.layout_reader_book4:
                    openBook((RecentlyBookData) llBook[3].getTag());
                    break;
                default:
                    break;
            }
        }
    };

    private View.OnLongClickListener onBookLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            ReaderBookInformationDialogFragment fragment = ReaderBookInformationDialogFragment.newInstance((RecentlyBookData) view.getTag());
            fragment.setOnButtonClickListener(onBookInformationDialogButtonClickListener);
            showDialogFragment(fragment, ReaderBookInformationDialogFragment.class.getSimpleName());
            return true;
        }
    };

    private ReaderBookInformationDialogFragment.OnButtonClickListener onBookInformationDialogButtonClickListener = new ReaderBookInformationDialogFragment.OnButtonClickListener() {
        @Override
        public void onDeleteBtnClick(int bookIndex) {
            RecentlyBookData bookData = (RecentlyBookData) llBook[bookIndex].getTag();
//            deleteReaderBook(bookData.getPath());
            deleteSelectedBook(bookData.getPath());
        }

        @Override
        public void onOpenBtnClick(int bookIndex) {
            Global.openWaitDialog(NtxHomeActivity.this);
            openBook((RecentlyBookData) llBook[bookIndex].getTag());
        }
    };

    private void deleteSelectedBook(final String path) {
        Runnable deleteSelectedNoteRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsInitBookshelfFinished) {
                    Global.closeWaitDialog(NtxHomeActivity.this);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    String dialogTag = "delete_confirm";
                    String deleteConfirmMessage = getResources().getString(R.string.toolbox_message_delete_confirm_book, 1 + "");
                    AlertDialogFragment deleteConfirmDialogFragment = AlertDialogFragment.newInstance(deleteConfirmMessage, R.drawable.writing_ic_error, true, dialogTag);

                    deleteConfirmDialogFragment.setupPositiveButton(getString(android.R.string.yes));
                    deleteConfirmDialogFragment.setupNegativeButton(getString(android.R.string.no));
                    deleteConfirmDialogFragment.registerAlertDialogButtonClickListener(new AlertDialogButtonClickListener() {

                        @Override
                        public void onPositiveButtonClick(String fragmentTag) {
                            deleteReaderBook(path);
                        }

                        @Override
                        public void onNegativeButtonClick(String fragmentTag) {
                        }
                    }, dialogTag);

                    ft.replace(R.id.alert_dialog_container, deleteConfirmDialogFragment, dialogTag)
                            .commit();

                } else {
                    Global.openWaitDialog(NtxHomeActivity.this);
                    mHandler.postDelayed(this, WAIT_TIME_FOR_BOOK_SHELF_INITIALIZATION);
                }
            }
        };
        mHandler.post(deleteSelectedNoteRunnable);
    }

    private boolean isSpaceAvailable() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        // Dango 20181005 Bugfix - integer overflow
        double availableSizeInBytes = (double) stat.getBlockSize() * (double) stat.getAvailableBlocks() / 1024 / 1024;
        long totalSize = caculateFolderTotalSize(new File(Global.APP_DATA_PACKAGE_FILES_PATH));

        return availableSizeInBytes > LIMIT_SPACE && availableSizeInBytes > (totalSize * 2);
    }

    private long caculateFolderTotalSize(File file) {
        long totalSize = 0;
        File[] the_Files = file.listFiles();

        if (the_Files == null)
            return 0;

        for (File tempF : the_Files) {
            totalSize += tempF.length();
        }
        return (long) (totalSize / 1024f / 1024f);
    }

    private void switchToNote() {
        Runnable switchToNoteRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsInitBookshelfFinished)
                    startActivity(new Intent(NtxHomeActivity.this, NtxLauncherActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                else
                    mHandler.postDelayed(this, WAIT_TIME_FOR_BOOK_SHELF_INITIALIZATION);
            }
        };
        mHandler.post(switchToNoteRunnable);
    }

    private void SwitchToNoteEditor() {
        try {
            Global.HAS_GREY_COLOR = false;
            Intent mIntent = new Intent();
            ComponentName componentName = null;
            componentName = new ComponentName("ntx.note2", "ntx.note.NoteWriterActivity");
            mIntent.setComponent(componentName);
            startActivity(mIntent);
        } catch (Throwable e) {
            Toast.makeText(this, "APP NOT FIND !", Toast.LENGTH_LONG).show();
        }
    }

    private void switchToReader(String keyValue) {
        try {
            ComponentName componentName;
            Intent mIntent = new Intent();
            mIntent.putExtra(Global.KEY_ACTIVITY, keyValue);

            if (keyValue.equals(Global.KEY_READER_MAIN_PAGE)) {
                componentName = new ComponentName(Global.READER_PACKAGE, Global.READER_MAIN_PAGE_CLASS);

            } else if (keyValue.equals(Global.KEY_ALL_BOOKS)) {
                componentName = new ComponentName(Global.READER_PACKAGE, Global.READER_CLASS);

            } else {
                componentName = new ComponentName(Global.READER_PACKAGE, Global.READER_CLASS);

            }

            startActivity(mIntent.setComponent(componentName).setAction(Intent.ACTION_VIEW)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } catch (Throwable e) {
            Global.closeWaitDialog(this);
            Toast.makeText(this, "APP NOT FIND !", Toast.LENGTH_LONG).show();
        }
    }

    private void switchToCalendar() {
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

    private void switchToSettings() {
        try {
            ComponentName componentName = new ComponentName(Global.NTOOL_PACKAGE, Global.NTOOL_CLASS);
            Intent mIntent = new Intent();
            startActivity(mIntent.setComponent(componentName).setAction(Intent.ACTION_VIEW)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            startActivity(mIntent);
        } catch (Throwable e) {
            Global.closeWaitDialog(this);
            Toast.makeText(this, "APP NOT FIND !", Toast.LENGTH_LONG).show();
        }
    }

    private void setNoteCover(int index, Bitmap bmp) {
        ivNoteCover[index].setImageBitmap(bmp);

        if (bmp == null)
            ivNoteCover[index].getBackground().setLevel(BookBackground.NONE);
        else
            ivNoteCover[index].getBackground().setLevel(BookBackground.BOOK);

    }

    private void setBookCover(int index,String path, Bitmap bmp) {

        if (bmp == null) {
            if (path.equals("")){
                ivBookCover[index].setBackgroundColor(Color.parseColor("#DDDDDD"));
            }else{
                ivBookCover[index].setImageResource(getCoverTypeTagResourceId(path));
                ivBookCover[index].setBackgroundResource(R.drawable.book_cover_default);
            }
        } else {
            ivBookCover[index].setBackgroundDrawable(new BitmapDrawable(getResources(), bmp));
            ivBookCover[index].setImageBitmap(BitmapFactory.decodeResource(getResources(), getCoverTypeTagResourceId(path)));
        }
    }

    private void clearAll() {
        tvNoteTitle[0].setText("");
        tvNoteTitle[1].setText("");
        tvNoteTitle[2].setText("");

        tvBookTitle[0].setText("");
        tvBookTitle[1].setText("");
        tvBookTitle[2].setText("");
        tvBookTitle[3].setText("");

        for (int i = 0; i < 3; i++) {
            setNoteCover(i, null);
        }
        for (int i = 0; i < 4; i++) {
            setBookCover(i, "", null);
        }
    }

    private void initNote() {
        for (int i = 0; i < 3; i++) {
            ivNoteCover[i].getBackground().setLevel(BookBackground.NONE);
            llNote[i].setClickable(false);
            llNote[i].setLongClickable(false);
        }
        Gson gson = new Gson();
        BufferedReader br = null;

        try {
            String jsonFilePath = Global.APP_DATA_PACKAGE_FILES_PATH + "recentNote.json";
            br = new BufferedReader(new FileReader(jsonFilePath));
            mRecentlyNoteList = new ArrayList<RecentlyNoteData>(
                    Arrays.asList(gson.fromJson(br, RecentlyNoteData[].class)));

            for (int i = 0; i < mRecentlyNoteList.size(); i++) {
                ivNoteCover[i].getBackground().setLevel(BookBackground.BOOK);
                tvNoteTitle[i].setText(mRecentlyNoteList.get(i).getTitle());
                llNote[i].setClickable(true);
                llNote[i].setLongClickable(true);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void updateNote() {
        tvNoteTitle[0].setText("");
        tvNoteTitle[1].setText("");
        tvNoteTitle[2].setText("");
        for (int i = 0; i < 3; i++) {
            setNoteCover(i, null);
        }
        initNote();
    }

    private void initBook() {
        for (int i = 0; i < 4; i++) {
            setBookCover(i, "", null);
            tvBookTitle[i].setText("");
        }
        getRecentBookList();
    }

    private void getRecentBookList() {
        Gson gson = new Gson();
        BufferedReader br = null;

        for (int i = 0; i < 4; i++) {
            llBook[i].setClickable(false);
            llBook[i].setLongClickable(false);
        }

        try {
            FileInputStream fIn = new FileInputStream(new File(apInfo.dataDir + "/files/recentBooks.json"));
            br = new BufferedReader(new InputStreamReader(fIn));
            List<RecentlyBookData> bookListFromJson = new ArrayList<>(Arrays.asList(gson.fromJson(br, RecentlyBookData[].class)));
            setBookCoverFromRecentBookList(checkBookListFileExist(bookListFromJson));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<RecentlyBookData> checkBookListFileExist(List<RecentlyBookData> bookDataList) {
        List<RecentlyBookData> recentlyBookList = new ArrayList<>();

        SparseIntArray mRecentlyBookIndexMapping = new SparseIntArray();
        for (int i = 0; i < bookDataList.size(); i++) {
            mRecentlyBookIndexMapping.put(bookDataList.get(i).getIndex(), i);
        }

        for (int i = 0; i < mRecentlyBookIndexMapping.size(); i++) {
            int listIndex = mRecentlyBookIndexMapping.get(i);
            File bookFile = new File(bookDataList.get(listIndex).getPath());
            if (bookFile.exists())
                recentlyBookList.add(bookDataList.get(listIndex));
        }

        return recentlyBookList;
    }

    private void setBookCoverFromRecentBookList(List<RecentlyBookData> bookList) {
        for (int i = 0; i < RECENT_BOOK_LIST_SIZE; i++) {
            llBook[i].setTag((RecentlyBookData) bookList.get(i));
            llBook[i].setClickable(true);
            llBook[i].setLongClickable(true);

            Bitmap bookCover = null;
            String type = bookList.get(i).getType().toLowerCase();
            int resId = getDefaultBookCoverId(type);
            if (bookList.get(i).hasCover()) {
                String imageFileName = "Cover_" + i + ".png";
                FileInputStream input;
                try {
                    input = new FileInputStream(new File(apInfo.dataDir + "/files/" + imageFileName));
                    bookCover = BitmapFactory.decodeStream(input);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    // bookCover = BitmapFactory.decodeResource(getResources(), resId);

                }
            }
//            else {
//                bookCover = BitmapFactory.decodeResource(getResources(), resId);
//            }
            setBookCover(i, bookList.get(i).getPath(), bookCover);
            tvBookTitle[i].setText(bookList.get(i).getTitle());
        }
    }

    private int getDefaultBookCoverId(String type) {
        int resId = R.drawable.book_cover_default;
        if (type.equalsIgnoreCase("djvu")) {
            resId = R.drawable.book_cover_djvu;
        } else if (type.equalsIgnoreCase("epub")) {
            resId = R.drawable.book_cover_epub;
        } else if (type.equalsIgnoreCase("fb2")) {
            resId = R.drawable.book_cover_fb2;
        } else if (type.equalsIgnoreCase("mobi")) {
            resId = R.drawable.book_cover_mobi;
        } else if (type.equalsIgnoreCase("pdf")) {
            resId = R.drawable.book_cover_pdf;
        } else if (type.equalsIgnoreCase("txt")) {
            resId = R.drawable.book_cover_txt;
        }
        return resId;
    }

    private void alertDialogMessage(String msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.AlertDialog_custom).create();
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.filepicker_dialog_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void openNote(final int index) {
        Global.openWaitDialog(this);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mIsInitBookshelfFinished) {
                    Book book = Bookshelf.getInstance().getBook(mRecentlyNoteList.get(index).getId());
                    if (book == null) {
                        // There is no book with the UUID in book list.
                        Global.closeWaitDialog(NtxHomeActivity.this);
                        return;
                    }

                    Bookshelf.getInstance().setCurrentBook(book.getUUID());
                    // finish();
                    SwitchToNoteEditor();
                } else
                    mHandler.postDelayed(this, WAIT_TIME_FOR_BOOK_SHELF_INITIALIZATION);
            }
        };
        mHandler.removeCallbacks(runnable);
        mHandler.postDelayed(runnable, 500);
    }

    private void openBook(RecentlyBookData book) {
        if (book == null) {
            Global.closeWaitDialog(this);
            return;
        }
        try {
            ComponentName componentName = new ComponentName(Global.READER_PACKAGE, Global.READER_OPENBOOK_CLASS);
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putLong("bookId", book.getId());
            bundle.putLong(Global.KEY_READER_BOOK_ID, book.getId());
            bundle.putString("bookPath", book.getPath());
            bundle.putString("bookTitle", book.getTitle());
            bundle.putString("bookEncoding", book.getEncoding());
            bundle.putString("bookLanguage", book.getLanguage());
            intent.putExtras(bundle);
            startActivity(intent.setComponent(componentName).setAction(Intent.ACTION_VIEW)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } catch (Throwable e) {
            Global.closeWaitDialog(this);
            Toast.makeText(this, "APP NOT FIND !", Toast.LENGTH_LONG).show();
        }
    }

    private void deleteReaderBook(String path) {
        String bookName = path.substring(path.lastIndexOf("/") + 1, path.length());
        String bookPath = path.substring(0, path.lastIndexOf("/"));

        File file = new File(bookPath, bookName);
        if (!file.exists())
            return;

        String where = MediaStore.MediaColumns.DATA + "=?";
        String[] selectionArgs = new String[]{file.getAbsolutePath()};
        getContentResolver().delete(MediaStore.Files.getContentUri("external"), where, selectionArgs);

        if (file.exists()) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Files.FileColumns.DATA, file.getAbsolutePath());
            getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            getContentResolver().delete(MediaStore.Files.getContentUri("external"), where, selectionArgs);
        }

        initBook();
    }

    private void initBookshelf() {
        InitBookshelfAsyncTask initBookshelfAsyncTask = new InitBookshelfAsyncTask();
        initBookshelfAsyncTask.execute();
    }

    private void createNewNote() {
        Runnable createNewNoteBookRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsInitBookshelfFinished) {
                    if (isSpaceAvailable()) {
                        Bookshelf.getInstance().newBook(generateNoteName());
                        SwitchToNoteEditor();
                    } else {
                        Global.closeWaitDialog(NtxHomeActivity.this);
                        mIsNewNoteCreating = false;
                        showStorageNotEnoughAlertDialog();
                    }
                } else
                    mHandler.postDelayed(this, WAIT_TIME_FOR_BOOK_SHELF_INITIALIZATION);
            }
        };

        if (mIsNewNoteCreating)
            return;

        mIsNewNoteCreating = true;
        if (mIsInitBookshelfFinished) {
            mHandler.post(createNewNoteBookRunnable);
        } else {
            mHandler.postDelayed(createNewNoteBookRunnable, WAIT_TIME_FOR_BOOK_SHELF_INITIALIZATION);
        }
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
                if (Bookshelf.getInstance().getCount() != 0) {
                    Intent intent = new Intent();
                    intent.setClass(NtxHomeActivity.this, NtxLauncherActivity.class);
                    intent.putExtra("LauncherListType", NtxLauncherActivity.LauncherListType.FREE_STORAGE);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }

            @Override
            public void onNegativeButtonClick(String fragmentTag) {

            }
        }, "");
        ft.replace(R.id.alert_dialog_container, alertDialogFragment, DIALOG_TAG_STORAGE_NOTE_ENOUGH)
                .commit();
    }

    private String generateNoteName() {
        String noteName = "Note 1";

        List<String> bookTitleList = new ArrayList<>();
        for (Book book : Bookshelf.getInstance().getBookList()) {
            bookTitleList.add(book.getTitle());
        }

        int index = 1;
        while (bookTitleList.contains(noteName)) {
            index++;
            noteName = "Note " + index;
        }

        return noteName;
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

    private void doFullRefresh(int delay) {
        Runnable runInvalidate = new Runnable() {
            @Override
            public void run() {
                if (Hardware.isEinkHardwareType()) {
                    layout.invalidate(PenEventNTX.UPDATE_MODE_SCREEN_2 | PenEventNTX.UPDATE_MODE_GLOBAL_RESET);
                }
            }
        };

        if (Hardware.isEinkHardwareType()) {
            mHandler.removeCallbacks(runInvalidate);
            mHandler.postDelayed(runInvalidate, delay);
        }
    }

    private void resetDropFrames() {
        Intent dropIntent = new Intent("ntx.eink_control.DropFrames");
        dropIntent.putExtra("period", Global.DEFAULT_DROP_TIME);
        dropIntent.putExtra("commandFromNtxApp", true);
        sendBroadcast(dropIntent);
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

    private void showDialogFragment(Fragment fragment, String tag) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.dialog_container, fragment, tag).commit();
    }

    private void showAlertMessageDialog(String msg, boolean result) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        int iconId = result ? R.drawable.writing_ic_successful : R.drawable.writing_ic_error;
        String dialogTag = "alert_message_dialog";
        AlertDialogFragment alertMessageDialogFragment = AlertDialogFragment.newInstance(msg, iconId, true, dialogTag);
        ft.replace(R.id.alert_dialog_container, alertMessageDialogFragment, dialogTag)
                .commit();
    }

    private class InitBookshelfAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            StorageAndroid.initialize(NtxHomeActivity.this);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mIsInitBookshelfFinished = true;
        }
    }

    private class CopySelectedBookAsyncTask extends AsyncTask<Void, Integer, Boolean> {
        private String uuidString;
        private Book selectedBook;
        private LinkedList<Page> selectedBookPages;

        CopySelectedBookAsyncTask(String uuidString) {
            this.uuidString = uuidString;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            FragmentTransaction ft = getFragmentManager().beginTransaction();

            UUID uuid = UUID.fromString(uuidString);
            selectedBook = new Book(uuid, true);
            selectedBookPages = selectedBook.getPages();

            Global.closeWaitDialog(NtxHomeActivity.this);

            // Set interruptable= false. Let user can not interrupt.
            mProgressingDialogFragment = InterruptibleProgressingDialogFragment
                    .newInstance(getString(R.string.copying), selectedBookPages.size(), true);

            mProgressingDialogFragment.setOnInterruptButtonClickListener(
                    new InterruptibleProgressingDialogFragment.OnInterruptButtonClickListener() {
                        @Override
                        public void onClick() {
                            CopySelectedBookAsyncTask.this.cancel(true);
                        }
                    });

            ft.replace(R.id.alert_dialog_container, mProgressingDialogFragment,
                    InterruptibleProgressingDialogFragment.class.getSimpleName()).commit();
        }

        @Override
        protected Boolean doInBackground(Void... avoid) {
            List<String> nameList = new ArrayList<>();
            nameList.clear();
            for (Book book : Bookshelf.getInstance().getBookList()) {
                nameList.add(book.getTitle());
            }

            Book newBook = new Book(getNewNameNotInList(selectedBook.getTitle(), nameList));
            TagManager newBookTagManage = newBook.getTagManager();

            for (int i = 0; i < selectedBookPages.size(); i++) {
                if (isCancelled())
                    return false;

                publishProgress(i);
                List<TagManager.Tag> pageAllTags = selectedBookPages.get(i).getTags().allTags();
                for (TagManager.Tag tag : pageAllTags) {
                    newBookTagManage.newTag(tag.toString());
                }

                newBook.clonePageTo(selectedBookPages.get(i), newBook.pagesSize() - 1, true);
            }
            newBook.setCurrentPage(newBook.getPage(0));
            newBook.deletePage();

            // the fail reason may be storage not enough.
            return newBook.save();
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
                alertMsg = getResources().getString(R.string.toolbox_message_copy_success);
            else
                alertMsg = getResources().getString(R.string.fail);

            showAlertMessageDialog(alertMsg, aBoolean);
            updateNote();
        }
    }

    private class DeleteNoteBookAsyncTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            Bookshelf.getInstance().deleteBook(UUID.fromString(strings[0]));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            String alertMsg = getResources().getString(R.string.toolbox_message_delete_success);
            showAlertMessageDialog(alertMsg, true);
            updateNote();
        }
    }

    private void sendEmail() {
        File mailTempDir = new File(Global.PATH_SDCARD + Global.MAIL_FILE_TEMP_DIR);

        ArrayList<Uri> uris = new ArrayList<Uri>();
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

    // HomerReader Grid view icon
    public static final int BOOK_COVER_TAG_EPUB = R.drawable.book_cover_tag_epub;
    public static final int BOOK_COVER_TAG_MOBI = R.drawable.book_cover_tag_mobi;
    public static final int BOOK_COVER_TAG_PDF = R.drawable.book_cover_tag_pdf;
    public static final int BOOK_COVER_TAG_RTF = R.drawable.book_cover_tag_rtf;
    public static final int BOOK_COVER_TAG_TXT = R.drawable.book_cover_tag_txt;
    public static final int BOOK_COVER_TAG_DJVU = R.drawable.book_cover_tag_djvu;
    public static final int BOOK_COVER_TAG_AZW3 = R.drawable.book_cover_tag_azw3;
    public static final int BOOK_COVER_TAG_FB2 = R.drawable.book_cover_tag_fb2;
    public static final int BOOK_COVER_TAG_OTHER = R.drawable.book_cover_tag_other;
    public static final int BOOK_COVER_TAG_DEFAULT = R.drawable.book_cover_default;

    // HomerReader book cover format
    public static final String COVER_FORMAT_EPUB   = "epub";
    public static final String COVER_FORMAT_MOBI   = "mobi";
    public static final String COVER_FORMAT_PDF   = "pdf";
    public static final String COVER_FORMAT_RTF   = "rtf";
    public static final String COVER_FORMAT_TXT   = "txt";
    public static final String COVER_FORMAT_DJVU   = "djvu";
    public static final String COVER_FORMAT_AZW3   = "azw3";
    public static final String COVER_FORMAT_FB2   = "fb2";

    /**
     *
     * @param path : book path
     * @return int : drawable resource id
     */
    public static int getCoverTypeTagResourceId(String path) {

        final int index = path.lastIndexOf('.');
        final String myExtension = ((index > 0) ? path.substring(index).toLowerCase().intern() : "");

        if (myExtension.equals("."+COVER_FORMAT_EPUB)) 		  {	return BOOK_COVER_TAG_EPUB;
        } else if (myExtension.equals("."+COVER_FORMAT_MOBI)) {	return BOOK_COVER_TAG_MOBI;
        } else if (myExtension.equals("."+COVER_FORMAT_PDF))  {	return BOOK_COVER_TAG_PDF;
        } else if (myExtension.equals("."+COVER_FORMAT_RTF))  {	return BOOK_COVER_TAG_RTF;
        } else if (myExtension.equals("."+COVER_FORMAT_TXT))  {	return BOOK_COVER_TAG_TXT;
        } else if (myExtension.equals("."+COVER_FORMAT_DJVU)) {	return BOOK_COVER_TAG_DJVU;
        } else if (myExtension.equals("."+COVER_FORMAT_AZW3)) {	return BOOK_COVER_TAG_AZW3;
        } else if (myExtension.equals("."+COVER_FORMAT_FB2))  {	return BOOK_COVER_TAG_FB2;}

        return BOOK_COVER_TAG_OTHER;

    }
}
