package ntx.note;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import junit.framework.Assert;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import name.vbraun.filepicker.RestoreDialogFragment;
import name.vbraun.lib.pen.Hardware;
import name.vbraun.lib.pen.HideBar;
import name.vbraun.lib.pen.PenEventNTX;
import name.vbraun.view.write.FastView;
import name.vbraun.view.write.Graphics;
import name.vbraun.view.write.Graphics.Tool;
import name.vbraun.view.write.GraphicsImage;
import name.vbraun.view.write.GraphicsLine;
import name.vbraun.view.write.GraphicsOval;
import name.vbraun.view.write.GraphicsRectangle;
import name.vbraun.view.write.GraphicsTriangle;
import name.vbraun.view.write.HandwriterView;
import name.vbraun.view.write.Page;
import name.vbraun.view.write.Paper;
import name.vbraun.view.write.Stroke;
import name.vbraun.view.write.TouchHandlerPenABC;
import ntx.draw.nDrawHelper;
import ntx.note.RelativePopupWindow.HorizontalPosition;
import ntx.note.RelativePopupWindow.VerticalPosition;
import ntx.note.bookshelf.CopyPageDialogFragment;
import ntx.note.bookshelf.InformationDialogFragment;
import ntx.note.bookshelf.NtxLauncherListAdapter;
import ntx.note.bookshelf.RenameNoteDialogFragment;
import ntx.note.data.Book;
import ntx.note.data.Bookshelf;
import ntx.note.data.StorageAndroid;
import ntx.note.data.TagManager;
import ntx.note.export.AlertDialogButtonClickListener;
import ntx.note.export.AlertDialogFragment;
import ntx.note.export.BackupDialogFragment;
import ntx.note.export.ConvertDialogFragment;
import ntx.note.export.DismissDelayPostAlertDialogFragment;
import ntx.note.image.ImageActivity;
import ntx.note.image.ImagePickerActivity;
import ntx.note.tag.TagDialogFragment;
import ntx.note.thumbnail.ThumbnailDialogFragment;
import ntx.note2.R;
import utility.HomeWatcher;
import utility.TextDialog;

import static name.vbraun.view.write.HandwriterView.KEY_PEN_OFFSET_X;
import static name.vbraun.view.write.HandwriterView.KEY_PEN_OFFSET_Y;
import static name.vbraun.view.write.HandwriterView.PAGE_REDRAW_TIME_THRESHOLD;

public class NoteWriterActivity extends ActivityBase
        implements name.vbraun.view.write.InputListener, AlertDialogButtonClickListener, View.OnTouchListener, View.OnClickListener {
    private static final String TAG = "NoteWriterActivity";

    public static final String DRAWING_ALERT_DIALOG_TAG = "drawing_alert_dialog";

    private static final String DELETE_PAGE_ALERT_DIALOG_TAG = "delete_page_alert_dialog";
    private static final String CLEAN_PAGE_ALERT_DIALOG_TAG = "clean_page_alert_dialog";
    private static final String EMPTY_PAGE_ALERT_DIALOG_TAG = "empty_page_alert_dialog";
    private static final String MAX_PAGE_ALERT_DIALOG_TAG = "max_page_alert_dialog";
    public static final String MAX_IMAGE_ALERT_DIALOG_TAG = "max_image_alert_dialog";
    public static final String SAVE_ALERT_DIALOG_TAG = "save_alert_dialog";
    private static final String SAVE_FAIL_ALERT_DIALOG_TAG = "save_fail_alert_dialog";
    private static final String CONVERT_SUCCESS_DIALOG_TAG = "convert_success_dialog";
    private static final String OOM_DIALOG_TAG = "oom_dialog";

    private static final int PAGE_MAX_SIZE = 200; // modify page max size from 100 to 200

    private final static int REQUEST_REPORT_BACK_KEY = 1;
    private final static int REQUEST_PICK_IMAGE = 2;
    private final static int REQUEST_EDIT_IMAGE = 3;
    public final static int REQUEST_PEN_OFFSET = 4;
    private final static int REQUEST_BG_PICK_IMAGE = 5;
    public final static int REQUEST_PICK_IMAGE_OOM = 6;

    private static boolean noteEdited = false; // if edited, note can be saved.

    private Context mContext;

    private HomeWatcher mHomeWatcher = new HomeWatcher(this);
    private Book book = null;

    private HandwriterView mHandwriterView;
    private FastView mFastView;

    private LinearLayout mDrawViewLayout;
    private LinearLayout mToolbox_vertical_left;
    private LinearLayout mToolbox_vertical_right;
    private LinearLayout mToolbox_horizontal;
    private LinearLayout mToolbox_normal_view_layout;

    private Button mBtnPageTitle;
    private Button mBtnPageNumber;

    private ImageButton mBtnUndo;
    private ImageButton mBtnRedo;

    private ImageButton mBtnPrevPage;
    private ImageButton mBtnOverview;

    private Toast mToast;
    private Handler mHandler = new Handler();

    private boolean mSharePrefSetting_isHideSystemBar;
    private boolean mSaveRunning = false;
    private boolean mCleanRunning = false;
    private boolean mDeleteRunning = false;
    private boolean mIsSettingPageBackgroundRequest = false;
    private String mStrPageBackgroundPath = "na";

    private EventBus mEventBus;

    private FileOperationsPopupWindow mFileOperationsPopupWindow;
    private PagePopupWindow mPagePopupWindow;
    private boolean mIsToolboxShown = true;

    private ArrayList mClose_nDraw_layout;
    private String mStrOpenFolderPath;
    private FrameLayout alert_dialog_container, dialog_container;

    private Runnable mRunnableInvalidateToolbox = new Runnable() {
        @Override
        public void run() {
            mToolbox_horizontal.invalidate(PenEventNTX.UPDATE_MODE_PARTIAL_GC16);
            if (ToolboxConfiguration.getInstance().isToolbarAtLeft())
                mToolbox_vertical_left.invalidate(PenEventNTX.UPDATE_MODE_PARTIAL_GC16);
            else
                mToolbox_vertical_right.invalidate(PenEventNTX.UPDATE_MODE_PARTIAL_GC16);
        }
    };

    private Runnable mRunnableInvalidateHandwriterView = new Runnable() {
        @Override
        public void run() {
            mHandwriterView.invalidate(PenEventNTX.UPDATE_MODE_PARTIAL_GL16);
        }
    };

    TextDialog SaveDialog = null;
    private Timer autoSaveTimer;
    private int autoSaveTime = 30000;
    private NtxLauncherListAdapter mLauncherListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getStringExtra("CreateNote") != null) {
            createNewNoteBookAndOpen(getIntent());
        }
        mEventBus = EventBus.getDefault();
        mContext = getApplicationContext();
        if (UpdateActivity.needUpdate(this))
            return;
        mClose_nDraw_layout = new ArrayList();
        mClose_nDraw_layout.add(R.id.btn_toolbox_back);
        mClose_nDraw_layout.add(R.id.btn_note_title);
        mClose_nDraw_layout.add(R.id.btn_toolbox_create_page);
        mClose_nDraw_layout.add(R.id.btn_toolbox_delete_page);
        mClose_nDraw_layout.add(R.id.btn_toolbox_undo);
        mClose_nDraw_layout.add(R.id.btn_toolbox_redo);
        mClose_nDraw_layout.add(R.id.btn_toolbox_full_refresh);
        mClose_nDraw_layout.add(R.id.btn_toolbox_prev_page);
        mClose_nDraw_layout.add(R.id.btn_page_number);
        mClose_nDraw_layout.add(R.id.btn_toolbox_next_page);
        mClose_nDraw_layout.add(R.id.btn_toolbox_overview);
        mClose_nDraw_layout.add(R.id.btn_toolbox_normal_view);
        mClose_nDraw_layout.add(R.id.ll_toolbox_horizontal);
        // reset edit status
        setNoteEdited(false);

        // Daniel 20151127 : Don't show the change log
        // if (!Global.releaseModeOEM) {
        // ChangeLog changeLog = new ChangeLog(this);
        // if (changeLog.firstRun())
        // changeLog.getLogDialog().show();
        // }
        String bookId = getIntent().getStringExtra("nNoteBookId");
        if (bookId != null && !bookId.isEmpty()) {
            if (bookId.equals("CREATE_NEW_NOTE"))
                Bookshelf.getInstance().newBook(Global.generateNoteName());
            else
                Bookshelf.getInstance().setCurrentBook(UUID.fromString(bookId));
        }
        book = Bookshelf.getInstance().getCurrentBook();

        // Dragon: if note is null, create a new one.
        if (book == null) {
            SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
            String date = sDateFormat.format(new java.util.Date());
            Bookshelf.getInstance().newBook("note_" + date);
            book = Bookshelf.getInstance().getCurrentBook();
        }

        book.setOnBookModifiedListener(UndoManager.getUndoManager());
        Assert.assertTrue("Book object not initialized.", book != null);

        Hardware.getInstance(getApplicationContext());

        mHandwriterView = new HandwriterView(this);
        setContentView(R.layout.ntx_note_write);

        mHandwriterView.setOnGraphicsModifiedListener(UndoManager.getUndoManager());
        mHandwriterView.setOnInputListener(this);
        mHandwriterView.getBottomHeight(getNavigationBarHeight());

        initViews();

        switchToPage(book.currentPage(), true);
        setKeepScreenOn();
        UndoManager.setApplication(this);

        mHomeWatcher.setOnHomePressedListener(new HomeWatcher.OnHomePressedListener() {
            @Override
            public void onHomePressed() {
                mHomeWatcher.stopWatch();
                if (getNoteEdited()) {
                    saveDialog(true);
                } else
                    finish();
            }

            @Override
            public void onHomeLongPressed() {
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getStringExtra("CreateNote") != null) {
            createNewNoteBookAndOpen(intent);
        }
    }

    @Override
    protected void onResume() {
        SaveDialog = new TextDialog(this, getResources().getString(R.string.saving));
        SaveDialog.setCanceledOnTouchOutside(false);
        autoSaveTimer = new Timer(false);
        autoSaveTimer.schedule(new AutoSaveTimerTask(), autoSaveTime, autoSaveTime);
        Global.closeWaitDialog(this);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        SharedPreferences _pref = getSharedPreferences(Global.GLOBAL_VALUE, MODE_PRIVATE);

        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int status_bar_height = 0;
        int resourceId = getApplicationContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            status_bar_height = getApplicationContext().getResources().getDimensionPixelSize(resourceId);
        }

        _pref.edit()
                .putInt(Global.MACHINE_PIXEL_HEIGHT, displayMetrics.heightPixels + status_bar_height)
                .putInt(Global.MACHINE_PIXEL_WIDTH, displayMetrics.widthPixels)
                .putFloat(Global.MACHINE_PIXEL_RATE, (float) displayMetrics.widthPixels / (float) (displayMetrics.heightPixels + status_bar_height))
                .apply();
        Global.MACHINE_PIXEL_RATE_VALUE = (float) displayMetrics.widthPixels / (float) (displayMetrics.heightPixels + status_bar_height);
        String recent_uuid = getSharedPreferences("recent_uuid", MODE_PRIVATE)
                .getString("recent_uuid", "");
        String current_uuid = Bookshelf.getInstance().getCurrentBook().getUUID().toString();

        if (!recent_uuid.equals(current_uuid)) {
            UndoManager.getUndoManager().clearHistory();
            CallbackEvent callbackEvent = new CallbackEvent();
            callbackEvent.setMessage(CallbackEvent.NOOSE_ALL_BTN_GONE);
            mEventBus.post(callbackEvent);
            closeAllFragmentDialog();
        }

        SharedPreferences pref = getSharedPreferences("recent_uuid", MODE_PRIVATE);
        pref.edit().putString("recent_uuid", current_uuid).commit();

        mHomeWatcher.startWatch();
        changeEinkControlPermission(true);
        mHandwriterView.stopInput();
        UndoManager.setApplication(this);
        super.onResume();

        if (false == mEventBus.isRegistered(this))
            mEventBus.register(this);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        ToolboxConfiguration toolboxConfiguration = ToolboxConfiguration.getInstance();
        toolboxConfiguration.loadSettings(settings);
        mHandwriterView.loadSettings(settings);
        if (mIsToolboxShown) {
            if (toolboxConfiguration.isToolbarAtLeft()) {
                mToolbox_vertical_left.setVisibility(View.VISIBLE);
                mToolbox_vertical_right.setVisibility(View.GONE);
            } else {
                mToolbox_vertical_left.setVisibility(View.GONE);
                mToolbox_vertical_right.setVisibility(View.VISIBLE);
            }

            // Karote 20180921 : wake up issue - Toolbox areas are blank.
            mHandler.postDelayed(mRunnableInvalidateToolbox, 500);
        }

        mSharePrefSetting_isHideSystemBar = settings.getBoolean(Preferences.KEY_HIDE_SYSTEM_BAR, false);
        if (mSharePrefSetting_isHideSystemBar)
            HideBar.hideSystembar(getApplicationContext());

        book = Bookshelf.getInstance().getCurrentBook();
        if (book != null) {
            Page p = book.currentPage();
            if (mHandwriterView.getPage() == p) {
                book.filterChanged();
                resetNDrawUpdateMode(p);
                // TagOverlay overlay = new TagOverlay(getApplicationContext(), p.getTags(),
                // book.currentPageNumber(),
                // mHandwriterView.isToolboxOnLeft());
                // mHandwriterView.setOverlay(overlay);
            } else {
                switchToPage(p, true);
            }
        } else {
            // Error: can not get current book.
            finish();
        }

        mHandwriterView.setOnInputListener(this);
        updateUndoRedoIcons();
        setKeepScreenOn();
        mHandwriterView.startInput();

        // Daniel 20181017 : enable 2-step-suspend
//        PowerEnhanceSet(1);
        nDrawHelper.NDrawInit(this);
        /**
         * 2019.03.13 Karote
         * post event for set the correct parameter to nDraw
         */
        mEventBus.post(toolboxConfiguration);
    }

    @Override
    protected void onPause() {
        autoSaveTimer.cancel();
        changeEinkControlPermission(false);
        Log.d(TAG, "NWA<---onPause");
        /**
         * 2019.02.21
         * Marked mHandwriterView.setAntiColor(false) by Karote
         * Reason: the anti-color feature does not enable, so it do not need to call setAntiColor(false).
         */
        // mHandwriterView.setAntiColor(false);
        mHandwriterView.stopInput();
        if (mSharePrefSetting_isHideSystemBar)
            HideBar.showSystembar(getApplicationContext());
        super.onPause();
        mHandwriterView.onPause();
        mHandwriterView.interrupt();

        saveToolBoxSetting(true);
        UndoManager.setApplication(null);
        // Daniel 20181017 : disable 2-step-suspend
//		PowerEnhanceSet(0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mEventBus.unregister(this);
        nDrawHelper.NDrawSwitch(false);
        nDrawHelper.NDrawUnInit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void finish() {
        mHandwriterView.onFinish();
        saveToolBoxSetting(false);
        autoSaveTimer.cancel();
        android.os.Process.killProcess(android.os.Process.myPid());
        super.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_REPORT_BACK_KEY:
                if (resultCode != RESULT_OK)
                    return;
                break;
            case REQUEST_PICK_IMAGE:
            case REQUEST_EDIT_IMAGE:

                if (resultCode == REQUEST_PICK_IMAGE_OOM) {
                    showOOMAlertDialogFragment();
                }

                if (resultCode != RESULT_OK) {
                    return;
                }
                setNoteEdited(true);
                String uuidStr = data.getStringExtra(ImageActivity.EXTRA_UUID);
                Assert.assertNotNull(uuidStr);
                UUID uuid = UUID.fromString(uuidStr);
                boolean constrain = data.getBooleanExtra(ImageActivity.EXTRA_CONSTRAIN_ASPECT, true);
                String uriStr = data.getStringExtra(ImageActivity.EXTRA_FILE_URI);
                if (uriStr == null)
                    mHandwriterView.setImage(uuid, null, constrain);
                else {
                    Uri uri = Uri.parse(uriStr);
                    String name = uri.getPath();
                    mHandwriterView.setImage(uuid, name, constrain);
                }
                break;
            case REQUEST_BG_PICK_IMAGE:
                if (resultCode != RESULT_OK)
                    break;

                // set flag
                mIsSettingPageBackgroundRequest = true;

                Uri uri = data.getData();

                if (uri == null) {
                    Log.e(TAG, "Selected image is NULL!");
                    return;
                } else {
                    Log.d(TAG, "Selected image: " + uri);
                }

                mStrPageBackgroundPath = uri.getPath();
                setUpPageBackground(Paper.CUSTOMIZED);
                break;
        }
    }

    @Override
    public void onBackPressed() {
//        if (getNoteEdited()) {
//            saveDialog(false);
        Intent homeIntent = new Intent("Ntx.InputKeyEvent");
        homeIntent.putExtra("KeyCode", KeyEvent.KEYCODE_HOME);
        sendBroadcast(homeIntent);
//        } else {
//            finish();
//        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (RelativePopupWindow.isPopupWindowShowing()) {
            if (ev.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER) {
                CallbackEvent callbackEvent = new CallbackEvent();
                callbackEvent.setMessage(CallbackEvent.DISMISS_POPUPWINDOW);
                mEventBus.post(callbackEvent);
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mClose_nDraw_layout.contains(v.getId()) && event.getAction() == MotionEvent.ACTION_DOWN) {
            nDrawHelper.NDrawSwitch(false);
        }
        if (RelativePopupWindow.isPopupWindowShowing()) {
            CallbackEvent callbackEvent = new CallbackEvent();
            callbackEvent.setMessage(CallbackEvent.DISMISS_POPUPWINDOW);
            mEventBus.post(callbackEvent);
            return true;
        }
        return false;
    }

    // The HandWriterView is not focusable and therefore does not receive KeyEvents
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        // Log.v(TAG, "KeyEvent "+action+" "+keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_UP) {
                    flip_page_next();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    flip_page_prev();
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isOverview())
                return false;
            else {
                return super.onKeyDown(keyCode, event);
            }
        }
        return true;
//        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        mDrawViewLayout.invalidate();
        nDrawHelper.NDrawSwitch(false);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }
        switch (v.getId()) {
            case R.id.btn_toolbox_full_refresh:
                mHandwriterView.invalidate();
                Intent ac = new Intent("ntx.eink_control.QUICK_REFRESH");
                ac.putExtra("updatemode", PenEventNTX.UPDATE_MODE_SCREEN);
                ac.putExtra("commandFromNtxApp", true);
                sendBroadcast(ac);
                break;
            case R.id.btn_toolbox_back:
//                if (getNoteEdited()) {
//                    saveDialog(false);
                Intent homeIntent = new Intent("Ntx.InputKeyEvent");
                homeIntent.putExtra("KeyCode", KeyEvent.KEYCODE_HOME);
                sendBroadcast(homeIntent);
//                } else {
//                    finish();
//                }
                break;
            case R.id.btn_toolbox_create_page:
                String msg;
                if (book.pagesSize() >= PAGE_MAX_SIZE) {
                    msg = getString(R.string.msg_warning_page_numbers, PAGE_MAX_SIZE);
                    //toast(msg);
                    showConfirmAlertDialogFragment(
                            msg,
                            R.drawable.writing_ic_error,
                            MAX_PAGE_ALERT_DIALOG_TAG,
                            false,
                            true);
                } else {
                    if (book.currentPage().isEmpty()) {
                        showConfirmAlertDialogFragment(
                                getString(R.string.quill_inserted_fail),
                                R.drawable.writing_ic_error,
                                EMPTY_PAGE_ALERT_DIALOG_TAG,
                                false,
                                true);
                    } else {
                        switchToPage(book.insertPage(), true);
                    }
                }
                saveCurrentNoteBook(false);
                break;
            case R.id.btn_toolbox_delete_page:
                if (!mDeleteRunning) {
                    mDeleteRunning = true;
                    showConfirmAlertDialogFragment(
                            getString(R.string.msg_delete_confirm),
                            R.drawable.writing_ic_error,
                            DELETE_PAGE_ALERT_DIALOG_TAG,
                            true,
                            true);
                }
                break;
            case R.id.btn_toolbox_undo:
                undo();
                break;
            case R.id.btn_toolbox_redo:
                redo();
                break;
            case R.id.btn_toolbox_next_page:
                flip_page_next();
                break;
            case R.id.btn_toolbox_prev_page:
                flip_page_prev();
                break;
            case R.id.btn_toolbox_overview:
                mToolbox_vertical_left.setVisibility(View.GONE);
                mToolbox_vertical_right.setVisibility(View.GONE);
                mToolbox_horizontal.setVisibility(View.GONE);
                mToolbox_normal_view_layout.setVisibility(View.VISIBLE);
                mIsToolboxShown = false;
                break;
            case R.id.btn_toolbox_normal_view:
                if (ToolboxConfiguration.getInstance().isToolbarAtLeft())
                    mToolbox_vertical_left.setVisibility(View.VISIBLE);
                else
                    mToolbox_vertical_right.setVisibility(View.VISIBLE);
                mToolbox_horizontal.setVisibility(View.VISIBLE);
                mToolbox_normal_view_layout.setVisibility(View.GONE);
                mIsToolboxShown = true;
                break;
            case R.id.btn_toolbox_info:
            case R.id.layout_toolbox_info:
                showInfoDialog();
                break;
            case R.id.btn_note_title:
                mDrawViewLayout.invalidate();
                v.setSelected(true);
                showFileOperationsPopupWindow();
                break;
            case R.id.btn_page_number:
                mDrawViewLayout.invalidate();
                v.setSelected(true);
                showPageSeekBarPopupWindow();
                break;
            default:
                break;
        }
    }

    @Override
    public void onStrokeFinishedListener() {
        @Tool int tool = ToolboxConfiguration.getInstance().getCurrentTool();
        mHandwriterView.setFullRefreshable(true);
        // if (tool != Tool.MOVE && tool != Tool.ERASER) return;
        // if (tool != Tool.MOVE && tool != Tool.IMAGE) return;
        if (tool != Tool.MOVE && tool != Tool.IMAGE)
            return;

        if (tool == Tool.IMAGE)
            return;
    }

    @Override
    public void onPickImageListener(GraphicsImage image) {
        Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
        intent.putExtra(ImageActivity.EXTRA_UUID, image.getUuid().toString());
        intent.putExtra(ImageActivity.BLANK, 1);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    public void onEditImageListener(GraphicsImage image) {
        Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
        intent.putExtra(ImageActivity.EXTRA_UUID, image.getUuid().toString());
        intent.putExtra(ImageActivity.EXTRA_CONSTRAIN_ASPECT, image.getConstrainAspect());
        intent.putExtra(ImageActivity.EXTRA_FILE_URI, image.getFileUri().toString());
        intent.putExtra(ImageActivity.BLANK, 2);
        startActivityForResult(intent, REQUEST_EDIT_IMAGE);
    }

    @Override
    public void onPositiveButtonClick(String fragmentTag) {
        Fragment fragment = getFragmentManager().findFragmentByTag(fragmentTag);
        if (fragment != null)
            getFragmentManager().beginTransaction().remove(fragment).commit();

        if (fragmentTag != null) {
            if (fragmentTag.equals(DELETE_PAGE_ALERT_DIALOG_TAG)) {
                deletePage();
            } else if (fragmentTag.equals(CLEAN_PAGE_ALERT_DIALOG_TAG)) {
                cleanPage();
            }
        }
    }

    @Override
    public void onNegativeButtonClick(String fragmentTag) {
        if (fragmentTag != null) {
            if (fragmentTag.equals(DELETE_PAGE_ALERT_DIALOG_TAG)) {
                mDeleteRunning = false;
            } else if (fragmentTag.equals(CLEAN_PAGE_ALERT_DIALOG_TAG)) {
                mCleanRunning = false;
            } else if (fragmentTag.equals(CONVERT_SUCCESS_DIALOG_TAG)) {
                openFolder(mStrOpenFolderPath);
            } else if (fragmentTag.equals(OOM_DIALOG_TAG)) {
                noteEdited = true;
                book.currentPage().setModified(true);
                mHandwriterView.removeImage();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(PageBackgroundChangeEvent event) {
        String type = event.getBackgroundType();
        if (type.equals(Paper.CUSTOMIZED)) {
            Intent intent = new Intent(NoteWriterActivity.this, ImagePickerActivity.class);
            intent.putExtra("pick_type", REQUEST_BG_PICK_IMAGE);
            startActivityForResult(intent, REQUEST_BG_PICK_IMAGE);
            return;
        } else {
            if (type.equals(mHandwriterView.getPagePaperType().toString()))
                return;
        }
        setUpPageBackground(type);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CallbackEvent event) {
        /**
         * File Operation
         */
        switch (event.getMessage()) {
            case CallbackEvent.RENAME_NOTE:
                Fragment renameFragment = RenameNoteDialogFragment.newInstance(book.getUUID(), false);
                showDialogFragment(renameFragment, RenameNoteDialogFragment.class.getSimpleName());
                break;
            case CallbackEvent.COPY_CURRENT_PAGE_TO:
                Fragment copyPageDialogFragment = new CopyPageDialogFragment();
                showDialogFragment(copyPageDialogFragment, CopyPageDialogFragment.class.getSimpleName());
                break;
            case CallbackEvent.BACKUP_NOTE:
                backupNote();
                break;
            case CallbackEvent.RESTORE_NOTE:
                restoreNote();
                break;
            case CallbackEvent.CONVERT_NOTE:
                convertNote();
                break;
            case CallbackEvent.INFO_NOTE:
                Fragment infoFragment = InformationDialogFragment.newInstance(book.getUUID());
                showDialogFragment(infoFragment, InformationDialogFragment.class.getSimpleName());
                break;
            default:
                break;
        }

        /**
         * Toolbox
         */
        switch (event.getMessage()) {
            case CallbackEvent.SHOW_CLEAN_DIALOG:
                showCleanDialog();
                break;
            case CallbackEvent.PAGE_ADD_QUICK_TAG:
                toggleQuickTag(true);
                break;
            case CallbackEvent.PAGE_REMOVE_QUICK_TAG:
                toggleQuickTag(false);
                break;
            case CallbackEvent.SEARCH_NOTE:
                notePageThumbnail();
                break;
            case CallbackEvent.SAVE_NOTE:
                saveNote_complete();
                break;
            case CallbackEvent.SWITCH_VERTICAL_TOOLBAR:
                if (ToolboxConfiguration.getInstance().isToolbarAtLeft()) {
                    mToolbox_vertical_left.setVisibility(View.VISIBLE);
                    mToolbox_vertical_right.setVisibility(View.GONE);
                } else {
                    mToolbox_vertical_right.setVisibility(View.VISIBLE);
                    mToolbox_vertical_left.setVisibility(View.GONE);
                }
                mHandwriterView.setDrawRegion();
                break;
            case CallbackEvent.SHOW_SETTING_CALIBRATION_DIALOG:
                Fragment noteWriterSettingFragment = new NoteWriterSettingFragment();
                showDialogFragment(noteWriterSettingFragment, NoteWriterSettingFragment.class.getSimpleName());
                break;
            case CallbackEvent.UPDATE_PAGE_TITLE:
                updatePageTitleString();
                break;
            case CallbackEvent.NOOSE_DELETE:
                deleteNoose();
                break;
            case CallbackEvent.NOOSE_COPY:
                copyNoose();
                break;
            case CallbackEvent.NOOSE_PASTE:
                pasteNoose();
                break;
            case CallbackEvent.NOOSE_CUT:
                cutNoose();
                break;
            default:
                break;
        }

        /**
         * Show message
         */
        switch (event.getMessage()) {
            case CallbackEvent.SAVE_COMPLETE:
                showConfirmAlertDialogFragment(
                        getString(R.string.activity_base_automatic_saved),
                        R.drawable.writing_ic_successful,
                        "",
                        false,
                        true);
                break;
            case CallbackEvent.SAVE_FAIL:
                showConfirmAlertDialogFragment(
                        getString(R.string.save_fail),
                        R.drawable.writing_ic_error,
                        SAVE_FAIL_ALERT_DIALOG_TAG,
                        false,
                        true);
                break;
            case CallbackEvent.PAGE_DRAW_TASK_HEAVY:
                showDismissDelayPostAlertDialogFragment("Drawing...", 0, DRAWING_ALERT_DIALOG_TAG);
                break;
            default:
                break;
        }

        /**
         * Others
         */
        switch (event.getMessage()) {
            case CallbackEvent.DO_DRAW_VIEW_INVALIDATE:
                mDrawViewLayout.invalidate();
                break;
            case CallbackEvent.DISMISS_POPUPWINDOW:
                dismissAllPopupWindow();
                break;
            case CallbackEvent.NEXT_PAGE:
                flip_page_next();
                break;
            case CallbackEvent.PREV_PAGE:
                flip_page_prev();
                break;
            case CallbackEvent.SEEKBAR_PAGE:
                book.setCurrentPage(book.getPage(mPagePopupWindow.getPageNumber()));
                switchToPage(book.currentPage(), true);
                break;
            case CallbackEvent.SEEKBAR_PROGRESS_INFO:
                mBtnPageNumber.setText((mPagePopupWindow.getPageNumber() + 1) + "/" + book.pagesSize());
                break;

            case CallbackEvent.RENAME_NOTE_DONE:
                book = Bookshelf.getInstance().getCurrentBook();
                updatePageTitleString();
                break;
            case CallbackEvent.RESTORE_NOTE_SUCCESS:
                book = Bookshelf.getInstance().getCurrentBook();
                if (book != null) {
                    Page p = book.currentPage();
                    if (mHandwriterView.getPage() == p) {
                        book.filterChanged();
                    } else {
                        switchToPage(p, true);
                    }
                } else {
                    Log.e(TAG, "can not get current book.");
                }
                break;
            case CallbackEvent.PAGE_DRAW_COMPLETED:
                if (book.currentPage().isCanvasDrawCompleted) {
                    mFastView.setVisibility(View.GONE);
                    mDrawViewLayout.invalidate();
                }
                break;
            case CallbackEvent.CONVERT_NOTE_EMAIL:
            case CallbackEvent.BACKUP_NOTE_EMAIL:
                sendEmail();
                break;
            default:
                break;
        }

        if (event.getMessage().equals(CallbackEvent.PAGE_TAG_SETTING)) {
            pageTagSetting();
        }
    }

    public static void setNoteEdited(boolean editedStatus) {
        noteEdited = editedStatus;
    }

    public static boolean getNoteEdited() {
        return noteEdited;
    }

    private void saveCurrentNoteBook(boolean isShowAlertMessage) {
        if (mSaveRunning)
            return;

        mSaveRunning = true;
        new SaveCurrentNoteBookAsyncTask(isShowAlertMessage).execute();
    }

    public void switchToPage(Page page, boolean doDraw) {

        mBtnPrevPage.setEnabled(!book.isFirstPage());
        if (mHandwriterView.getPage() != null) {
            if (mHandwriterView.getPage().isModified(false)) {
                if (!mHandwriterView.getPage().savePageToStorage()) {
                    CallbackEvent event = new CallbackEvent();
                    event.setMessage(CallbackEvent.SAVE_FAIL);
                    EventBus.getDefault().post(event);
                    return;
                }

                if (mHandwriterView.getPage().isCanvasDrawCompleted) {
                    if (mHandwriterView.getPage().objectsDrawTimePredict() > PAGE_REDRAW_TIME_THRESHOLD)
                        mHandwriterView.savePagePreview();
                    else
                        mHandwriterView.deletePagePreview();
                }
            }
        }
        CallbackEvent callbackEvent = new CallbackEvent();
        callbackEvent.setMessage(CallbackEvent.NOOSE_COPY_AND_DELETE_AND_CUT_BTN_GONE);
        mEventBus.post(callbackEvent);
        mFastView.setPageAndZoomOut(page);
        mHandwriterView.setPageAndZoomOut(page, doDraw);
        if (FastView.loadPagePreviewBitmapSuccess) {
            TouchHandlerPenABC.isFastView = true;
            mFastView.setVisibility(View.VISIBLE);
        } else {
            TouchHandlerPenABC.isFastView = false;
            mFastView.setVisibility(View.GONE);
        }

        // TagOverlay overlay = new TagOverlay(getApplicationContext(),
        // page.tags, book.currentPageNumber(), mHandwriterView.isToolboxOnLeft());
        // mHandwriterView.setOverlay(overlay);

        // mHandwriterView.setPageTitleString(book.getTitle());
        // mHandwriterView.setPageNumberString( (book.currentPageNumber() + 1) + "/" +
        // book.filteredPagesSize());

        updatePageTitleString();
        mBtnPageNumber.setText((book.currentPageNumber() + 1) + "/" + book.pagesSize());
        ToolboxConfiguration.getInstance().setPageBackground(page.getPaperType());
        resetNDrawUpdateMode(page);

        TagManager tagManager = book.getTagManager();
        TagManager.TagSet pageTagSet = page.tags;
        TagManager.Tag quickTag = tagManager.findTag(TagManager.QUICK_TAG_NAME);
        ToolboxConfiguration.getInstance().setPageCheckedQuickTag(pageTagSet.contains(quickTag));
    }

    public void updatePageNumber() {
        mBtnPageNumber.setText((book.currentPageNumber() + 1) + "/" + book.pagesSize());
    }

    public void setupPathForOpenFolder(String path) {
        this.mStrOpenFolderPath = path;
    }

    public void toast(String s) {
        if (mToast == null)
            mToast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
        else {
            mToast.setText(s);
        }
        mToast.show();
    }

    public void toast(int resId) {
        toast(getString(resId));
    }

    public void add(Page page, int position) {
        setNoteEdited(true);
        book.addPage(page, position, true);
        switchToPage(book.currentPage(), true);
        updateUndoRedoIcons();
    }

    public void remove(Page page, int position) {
        setNoteEdited(true);
        book.removePage(page, position, true);
        switchToPage(book.currentPage(), true);
        updateUndoRedoIcons();
    }

    public void add(Page page, Graphics graphics) {
        setNoteEdited(true);
        if (page != mHandwriterView.getPage()) {
            Assert.assertTrue("page not in book", book.getPages().contains(page));
            book.setCurrentPage(page);
            switchToPage(page, true);
        }
        mHandwriterView.add(graphics);
        updateUndoRedoIcons();
    }

    public void remove(Page page, Graphics graphics) {
        setNoteEdited(true);
        if (page != mHandwriterView.getPage()) {
            Assert.assertTrue("page not in book", book.getPages().contains(page));
            book.setCurrentPage(page);
            switchToPage(page, true);
        }
        mHandwriterView.remove(graphics);
        updateUndoRedoIcons();
    }

    public void remove_for_erase(Page page, LinkedList<Graphics> graphics) {
        setNoteEdited(true);
        if (page != mHandwriterView.getPage()) {
            Assert.assertTrue("page not in book", book.getPages().contains(page));
            book.setCurrentPage(page);
            switchToPage(page, true);
        }
        mHandwriterView.remove_for_erase(graphics);
        updateUndoRedoIcons();
    }

    public void add_for_erase_revert(Page page, LinkedList<Graphics> graphics) {
        if (page != mHandwriterView.getPage()) {
            Assert.assertTrue("page not in book", book.getPages().contains(page));
            book.setCurrentPage(page);
            switchToPage(page, true);
        }
        mHandwriterView.add_for_erase_revert(graphics);
        updateUndoRedoIcons();
    }

    public void remove_for_clear(Page page) {
        setNoteEdited(true);
        if (page != mHandwriterView.getPage()) {
            Assert.assertTrue("page not in book", book.getPages().contains(page));
            book.setCurrentPage(page);
            switchToPage(page, false);
        }
        mHandwriterView.remove_for_clear(page);
        updateUndoRedoIcons();
    }

    public void add_for_clear_revert(Page page,
                                     LinkedList<Stroke> strokes,
                                     LinkedList<GraphicsLine> lines,
                                     LinkedList<GraphicsRectangle> rectangles,
                                     LinkedList<GraphicsOval> ovals,
                                     LinkedList<GraphicsTriangle> triangles) {
        setNoteEdited(true);
        if (page != mHandwriterView.getPage()) {
            Assert.assertTrue("page not in book", book.getPages().contains(page));
            book.setCurrentPage(page);
            switchToPage(page, false);
        }
        mHandwriterView.add_for_clear_revert(strokes, lines, rectangles, ovals, triangles);
        updateUndoRedoIcons();
    }

    public void modify_graphics(Page page, Graphics oldGraphics, Graphics newGraphics) {
        setNoteEdited(true);
        if (page != mHandwriterView.getPage()) {
            Assert.assertTrue("page not in book", book.getPages().contains(page));
            book.setCurrentPage(page);
            switchToPage(page, false);
        }
        mHandwriterView.modify_graphics(oldGraphics, newGraphics);
        updateUndoRedoIcons();
    }

    public void modify_graphicsList(Page page, LinkedList<Graphics> oldGraphicsList, LinkedList<Graphics> newGraphicsList) {
        setNoteEdited(true);
        if (page != mHandwriterView.getPage()) {
            Assert.assertTrue("page not in book", book.getPages().contains(page));
            book.setCurrentPage(page);
            switchToPage(page, false);
        }
        mHandwriterView.modify_graphicsList(oldGraphicsList, newGraphicsList);
        updateUndoRedoIcons();
    }

    public void applyNoteWriterSettings(int offsetX, int offsetY) {
        mHandwriterView.setPenOffsetX(offsetX);
        mHandwriterView.setPenOffsetY(offsetY);

        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putInt(KEY_PEN_OFFSET_X, offsetX)
                .putInt(KEY_PEN_OFFSET_Y, offsetY)
                .apply();

        mHandwriterView.setDrawRegion();
    }

    private void initViews() {
        alert_dialog_container = findViewById(R.id.alert_dialog_container);
        dialog_container = findViewById(R.id.dialog_container);
        mDrawViewLayout = (LinearLayout) findViewById(R.id.ll_draw_view);
        mDrawViewLayout.addView(mHandwriterView);
        mFastView = (FastView) findViewById(R.id.layout_fast_view);
        mToolbox_vertical_left = (LinearLayout) findViewById(R.id.ll_toolbox_vertical_left);
        mToolbox_vertical_left.setOnTouchListener(this);
        ToolboxViewBuilder toolboxLeft = new ToolboxViewBuilder(this, R.layout.toolbox_left);
        mToolbox_vertical_left.addView(toolboxLeft);
        mToolbox_vertical_left.setVisibility(View.GONE);
        mToolbox_vertical_right = (LinearLayout) findViewById(R.id.ll_toolbox_vertical_right);
        mToolbox_vertical_right.setOnTouchListener(this);
        ToolboxViewBuilder toolboxRight = new ToolboxViewBuilder(this, R.layout.toolbox_right);
        mToolbox_vertical_right.addView(toolboxRight);
        mToolbox_vertical_right.setVisibility(View.GONE);
        mToolbox_horizontal = (LinearLayout) findViewById(R.id.ll_toolbox_horizontal);
        mToolbox_horizontal.setOnTouchListener(this);
        mToolbox_normal_view_layout = findViewById(R.id.toolbox_normal_view_layout);
        int left_toolbox_width = mToolbox_vertical_left.getLayoutParams().width;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        SharedPreferences _pref = getSharedPreferences(Global.GLOBAL_VALUE, MODE_PRIVATE);

        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int status_bar_height = 0;
        int resourceId = getApplicationContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            status_bar_height = getApplicationContext().getResources().getDimensionPixelSize(resourceId);
        }

        _pref.edit()
                .putInt(Global.MACHINE_PIXEL_HEIGHT, displayMetrics.heightPixels + status_bar_height)
                .putInt(Global.MACHINE_PIXEL_WIDTH, displayMetrics.widthPixels)
                .putFloat(Global.MACHINE_PIXEL_RATE, (float) displayMetrics.widthPixels / (float) (displayMetrics.heightPixels + status_bar_height))
                .apply();

        int screen_height = getSharedPreferences(Global.GLOBAL_VALUE, MODE_PRIVATE).getInt(Global.MACHINE_PIXEL_HEIGHT, 0);
        int screen_width = getSharedPreferences(Global.GLOBAL_VALUE, MODE_PRIVATE).getInt(Global.MACHINE_PIXEL_WIDTH, 0);
        float rate = getSharedPreferences(Global.GLOBAL_VALUE, MODE_PRIVATE).getFloat(Global.MACHINE_PIXEL_RATE, 0);
        int temp_height = (int) Math.rint((float) (screen_width - left_toolbox_width) / rate);
        temp_height = screen_height - temp_height;
        ViewGroup.LayoutParams params = mToolbox_horizontal.getLayoutParams();
        // Changes the height and width to the specified *pixels*
        params.height = temp_height;
        mToolbox_horizontal.setLayoutParams(params);
        ViewGroup.LayoutParams params2 = mToolbox_horizontal.getLayoutParams();
        params2.height = temp_height;
        mToolbox_normal_view_layout.setLayoutParams(params2);

        ImageButton btnBack = (ImageButton) findViewById(R.id.btn_toolbox_back);
        btnBack.setOnClickListener(this);
        btnBack.setOnTouchListener(this);

        mBtnPageTitle = (Button) findViewById(R.id.btn_note_title);
        mBtnPageTitle.setOnClickListener(this);
        mBtnPageTitle.setOnTouchListener(this);

        mBtnPageNumber = (Button) findViewById(R.id.btn_page_number);
        mBtnPageNumber.setOnClickListener(this);
        mBtnPageNumber.setOnTouchListener(this);

        ImageButton btnCreatePage = (ImageButton) findViewById(R.id.btn_toolbox_create_page);
        btnCreatePage.setOnClickListener(this);
        btnCreatePage.setOnTouchListener(this);

        ImageButton btnDeletePage = (ImageButton) findViewById(R.id.btn_toolbox_delete_page);
        btnDeletePage.setOnClickListener(this);
        btnDeletePage.setOnTouchListener(this);

        mBtnUndo = (ImageButton) findViewById(R.id.btn_toolbox_undo);
        mBtnUndo.setOnClickListener(this);
        mBtnUndo.setOnTouchListener(this);

        mBtnRedo = (ImageButton) findViewById(R.id.btn_toolbox_redo);
        mBtnRedo.setOnClickListener(this);
        mBtnRedo.setOnTouchListener(this);

        ImageButton btnFullRefresh = (ImageButton) findViewById(R.id.btn_toolbox_full_refresh);
        btnFullRefresh.setOnClickListener(this);
        btnFullRefresh.setOnTouchListener(this);

        ImageButton btnNextPage = (ImageButton) findViewById(R.id.btn_toolbox_next_page);
        btnNextPage.setOnClickListener(this);
        btnNextPage.setOnTouchListener(this);

        mBtnPrevPage = (ImageButton) findViewById(R.id.btn_toolbox_prev_page);
        mBtnPrevPage.setOnClickListener(this);
        mBtnPrevPage.setOnTouchListener(this);

        mBtnOverview = (ImageButton) findViewById(R.id.btn_toolbox_overview);
        mBtnOverview.setOnClickListener(this);
        mBtnOverview.setOnTouchListener(this);

        ImageButton btnNormalView = (ImageButton) findViewById(R.id.btn_toolbox_normal_view);
        btnNormalView.setOnClickListener(this);
        btnNormalView.setOnTouchListener(this);

    }

    private int getNavigationBarHeight() {
        Resources resources = mContext.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);

        return height;
    }

    private void setKeepScreenOn() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean screenOn = settings.getBoolean(Preferences.KEY_KEEP_SCREEN_ON, false);
        if (screenOn)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME)) {
            // if device has home key, set full screen to hide navigation bar.
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

    }

    private void changeEinkControlPermission(boolean isForNtxAppsOnly) {
        Intent changePermissionIntent = new Intent("ntx.eink_control.CHANGE_PERMISSION");
        changePermissionIntent.putExtra("isPermissionNtxApp", isForNtxAppsOnly);
        sendBroadcast(changePermissionIntent);
    }

    private void flip_page_prev() {
        if (!book.isFirstPage()) {
            switchToPage(book.previousPage(), true);
            Global.HAS_GREY_COLOR = false;
        }
    }

    private void flip_page_next() {
        if (book.isLastPage()) {
            if (book.currentPage().isEmpty()) {
                showConfirmAlertDialogFragment(
                        getString(R.string.quill_inserted_fail),
                        R.drawable.writing_ic_error,
                        EMPTY_PAGE_ALERT_DIALOG_TAG,
                        false,
                        true);
            } else {
                if (book.getPages().size() >= PAGE_MAX_SIZE) {
                    showConfirmAlertDialogFragment(
                            getString(R.string.msg_warning_page_numbers, PAGE_MAX_SIZE),
                            R.drawable.writing_ic_error,
                            MAX_PAGE_ALERT_DIALOG_TAG,
                            false,
                            true);
                } else {
                    Global.HAS_GREY_COLOR = false;
                    switchToPage(book.insertPage(), true);
                    saveCurrentNoteBook(false);
                }
            }
        } else {
            Global.HAS_GREY_COLOR = false;
            switchToPage(book.nextPage(), true);
        }
    }

    private void undo() {
        CallbackEvent callbackEvent;
        callbackEvent = new CallbackEvent();
        callbackEvent.setMessage(CallbackEvent.NOOSE_ALL_BTN_GONE);
        mEventBus.post(callbackEvent);
        mHandwriterView.getPage().nooseArt.clear();
        if (UndoManager.getUndoManager().undo())
            mHandwriterView.invalidate();
        updateUndoRedoIcons();
    }

    private void redo() {
        CallbackEvent callbackEvent;
        callbackEvent = new CallbackEvent();
        callbackEvent.setMessage(CallbackEvent.NOOSE_ALL_BTN_GONE);
        mEventBus.post(callbackEvent);
        mHandwriterView.getPage().nooseArt.clear();
        if (UndoManager.getUndoManager().redo())
            mHandwriterView.invalidate();
        updateUndoRedoIcons();
    }

    private void updateUndoRedoIcons() {
        UndoManager mgr = UndoManager.getUndoManager();
        if (mgr.haveUndo() != mBtnUndo.isEnabled()) {
            setUndoIconEnabled(mgr.haveUndo());
        }
        if (mgr.haveRedo() != mBtnRedo.isEnabled()) {
            setRedoIconEnabled(mgr.haveRedo());
        }
    }

    private void setUndoIconEnabled(boolean active) {
        mBtnUndo.setEnabled(active);
    }

    private void setRedoIconEnabled(boolean active) {
        mBtnRedo.setEnabled(active);
    }

    private void setUpPageBackground(String type) {
        book.currentPage().clearCustomizedBackground();

        book.currentPage().setModified(true);
        setNoteEdited(true);

        if (!mIsSettingPageBackgroundRequest) {
            // clear
            mStrPageBackgroundPath = "na";
        }
        mIsSettingPageBackgroundRequest = false;

        mHandwriterView.setPagePaperType(PaperTypeStringToEnumValue(type), mStrPageBackgroundPath);
    }

    private Paper.Type PaperTypeStringToEnumValue(String paper) {
        for (int i = 0; i < Paper.Table.length; i++) {
            Paper.Type paperType = Paper.Table[i].getType();
            if (paperType.toString().equals(paper)) {
                return paperType;
            }
        }
        return Paper.Table[0].getType();
    }

    private boolean isOverview() {
        if (!mIsToolboxShown) {
            mToolbox_horizontal.setVisibility(View.VISIBLE);
            if (ToolboxConfiguration.getInstance().isToolbarAtLeft()) {
                mToolbox_vertical_left.setVisibility(View.VISIBLE);
                mToolbox_vertical_right.setVisibility(View.GONE);
            } else {
                mToolbox_vertical_left.setVisibility(View.GONE);
                mToolbox_vertical_right.setVisibility(View.VISIBLE);
            }
            mToolbox_normal_view_layout.setVisibility(View.GONE);
            mIsToolboxShown = true;
        } else
            mIsToolboxShown = false;

        return mIsToolboxShown;
    }

    private void showInfoDialog() {
    }

    private void showCleanDialog() {
        if (!mCleanRunning) {
            mCleanRunning = true;
            showConfirmAlertDialogFragment(
                    getString(R.string.msg_clear_confirm),
                    R.drawable.writing_ic_error,
                    CLEAN_PAGE_ALERT_DIALOG_TAG,
                    true,
                    true);
        }
    }

    private void saveNote_complete() {
        SaveDialog.show();
        boolean normalSaveStatus = Bookshelf.getInstance().getCurrentBook().save();
        Log.d(TAG, "Normal Save Status : " + normalSaveStatus);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SaveDialog.dismiss();
            }
        }, 2000);
    }

    private void backupNote() {
        BackupDialogFragment backupDialogFragment = BackupDialogFragment.newInstance(book.getUUID(), noteEdited, false);
        showDialogFragment(backupDialogFragment, BackupDialogFragment.class.getSimpleName());
    }

    private void convertNote() {
        ConvertDialogFragment convertDialogFragment = ConvertDialogFragment.newInstance(book.getUUID());
        showDialogFragment(convertDialogFragment, ConvertDialogFragment.class.getSimpleName());
    }

    private void restoreNote() {
        RestoreDialogFragment restoreDialogFragment = RestoreDialogFragment.newInstance(book.getUUID());
        showDialogFragment(restoreDialogFragment, RestoreDialogFragment.class.getSimpleName());
    }

    private void pageTagSetting() {
        TagDialogFragment tagDialogFragment = TagDialogFragment.newInstance();
        showDialogFragment(tagDialogFragment, TagDialogFragment.class.getSimpleName());
    }

    private void notePageThumbnail() {
        ThumbnailDialogFragment thumbnailDialogFragment = ThumbnailDialogFragment.newInstance(getNoteEdited());
        showDialogFragment(thumbnailDialogFragment, ThumbnailDialogFragment.class.getSimpleName());
    }

    private void showFileOperationsPopupWindow() {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();

        int toolbox_vertical_width;
        if (mToolbox_vertical_left.getVisibility() == View.VISIBLE)
            toolbox_vertical_width = mToolbox_vertical_left.getWidth();
        else
            toolbox_vertical_width = mToolbox_vertical_right.getWidth();

        mFileOperationsPopupWindow = FileOperationsPopupWindow.getInstance(this,
                dm.widthPixels - toolbox_vertical_width);
        mFileOperationsPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mBtnPageTitle.setSelected(false);
            }
        });

        if (mToolbox_vertical_left.getVisibility() == View.VISIBLE) {
            mFileOperationsPopupWindow.showOnAnchor(mBtnPageTitle, HorizontalPosition.ALIGN_LEFT, VerticalPosition.BELOW);
        } else {
            mFileOperationsPopupWindow.showOnAnchor(mBtnPageTitle, HorizontalPosition.ALIGN_LEFT, VerticalPosition.BELOW,
                    -toolbox_vertical_width, 0);
        }
    }

    private void showPageSeekBarPopupWindow() {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();

        int toolbox_vertical_width;
        if (mToolbox_vertical_left.getVisibility() == View.VISIBLE)
            toolbox_vertical_width = mToolbox_vertical_left.getWidth();
        else
            toolbox_vertical_width = mToolbox_vertical_right.getWidth();

        mPagePopupWindow = new PagePopupWindow(this,
                (int) ((float) dm.widthPixels / 1.5f) - toolbox_vertical_width,
                book.getPageNumber(book.currentPage()), book.getPages().size());

        mPagePopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mBtnPageNumber.setSelected(false);
            }
        });

        if (mToolbox_vertical_left.getVisibility() == View.VISIBLE) {
            mPagePopupWindow.showOnAnchor(mBtnOverview, HorizontalPosition.ALIGN_RIGHT, VerticalPosition.BELOW, 15, 0, 0, 15);
        } else {
            mPagePopupWindow.showOnAnchor((LinearLayout) findViewById(R.id.ll_toolbox_horizontal_group2),
                    HorizontalPosition.ALIGN_RIGHT, VerticalPosition.BELOW, 15, 0, 0, 15);
        }
    }

    private void deletePage() {
        book.deletePage();
        Global.HAS_GREY_COLOR = false;
        switchToPage(book.currentPage(), true);
        mDeleteRunning = false;
    }

    private void cleanPage() {
        mHandwriterView.clear();
        Global.HAS_GREY_COLOR = false;
        mCleanRunning = false;
    }

    private void toggleQuickTag(boolean isCheck) {
        TagManager tagManager = book.getTagManager();
        TagManager.TagSet currentPageTagSet = book.currentPage().tags;
        TagManager.Tag quickTag = tagManager.findTag(TagManager.QUICK_TAG_NAME);
        if (isCheck)
            currentPageTagSet.add(quickTag);
        else
            currentPageTagSet.remove(quickTag);

        Bookshelf.getInstance().getCurrentBook().currentPage().touch();
    }

    public void showConfirmAlertDialogFragment(String msg, @Nullable Integer iconResId, String tag, boolean isNegativeButtonVisible, boolean enablePositiveButton) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        AlertDialogFragment confirmAlertDialogFragment;
        TouchHandlerPenABC.isPopupwindow = true;
        confirmAlertDialogFragment = AlertDialogFragment.newInstance(msg, iconResId, enablePositiveButton, tag);
        confirmAlertDialogFragment.registerAlertDialogButtonClickListener(this, tag);
        if (isNegativeButtonVisible) {
            confirmAlertDialogFragment.setupNegativeButton(getString(AlertDialogFragment.NEGATIVE_DEFAULT_STRING));
        }

        fragmentTransaction.replace(R.id.alert_dialog_container, confirmAlertDialogFragment, tag).commit();
    }

    private void showOOMAlertDialogFragment() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(getResources().getString(R.string.out_of_memory), R.drawable.writing_ic_error, false, OOM_DIALOG_TAG);
        alertDialogFragment.registerAlertDialogButtonClickListener(NoteWriterActivity.this, OOM_DIALOG_TAG);
        ft.replace(R.id.alert_dialog_container, alertDialogFragment, AlertDialogFragment.class.getSimpleName())
                .commit();
    }

    private void showDismissDelayPostAlertDialogFragment(String msg, int minDismissDelayTime, String tag) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DismissDelayPostAlertDialogFragment dismissDelayPostAlertDialogFragment = DismissDelayPostAlertDialogFragment.newInstance(msg, minDismissDelayTime, tag);
        TouchHandlerPenABC.isPopupwindow = true;
        ft.replace(R.id.alert_dialog_container, dismissDelayPostAlertDialogFragment, tag).commit();
    }

    private void showDialogFragment(Fragment fragment, String tag) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        TouchHandlerPenABC.isPopupwindow = true;
        fragmentTransaction.replace(R.id.dialog_container, fragment, tag).commit();
    }

    private void openFolder(String fullPath) {
        Uri pathUri;
        String path;
        if (fullPath.contains(Global.DIRECTORY_SDCARD_NOTE)) {
            pathUri = Uri.parse(Global.DIRECTORY_SDCARD_NOTE);
        } else if (fullPath.contains(Global.DIRECTORY_EXTERNALSD_NOTE)) {
            pathUri = Uri.parse(Global.DIRECTORY_EXTERNALSD_NOTE);
        } else if (fullPath.contains(Global.DIRECTORY_SDCARD_BOOK)) {
            pathUri = Uri.parse(Global.DIRECTORY_SDCARD_BOOK);
        } else if (fullPath.contains(Global.DIRECTORY_EXTERNALSD_BOOK)) {
            pathUri = Uri.parse(Global.DIRECTORY_EXTERNALSD_BOOK);
        } else if (fullPath.contains(Global.DIRECTORY_SDCARD_SLEEP)) {
            pathUri = Uri.parse(Global.DIRECTORY_SDCARD_SLEEP);
        } else if (fullPath.contains(Global.DIRECTORY_SDCARD_POWEROFF)) {
            pathUri = Uri.parse(Global.DIRECTORY_SDCARD_POWEROFF);
        } else {
            pathUri = Uri.parse(Global.PATH_SDCARD);
        }
        path = pathUri.toString();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pathUri, "resource/folder");
        intent.putExtra("path", path);
        try {
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetNDrawUpdateMode(Page page) {
        if (page.isNDrawDuModeRequiredForGrayscale()) {
            nDrawHelper.NDrawSetUpdateMode(PenEventNTX.UPDATE_MODE_PARTIAL_DU);
        } else {
            nDrawHelper.NDrawSetUpdateMode(PenEventNTX.UPDATE_MODE_PARTIAL_A2);
        }
    }

    private void dismissAllPopupWindow() {
        if (mFileOperationsPopupWindow != null && mFileOperationsPopupWindow.isShowing())
            mFileOperationsPopupWindow.dismiss();
        if (mPagePopupWindow != null && mPagePopupWindow.isShowing())
            mPagePopupWindow.dismiss();
    }

//    private void closeNoteAfterSaveCompleted() {
//        if (mHandwriterView.getPage().isModified()) {
//            if (mHandwriterView.getPage().isCanvasDrawCompleted) {
//                if (mHandwriterView.getPage().objectsDrawTimePredict() > PAGE_REDRAW_TIME_THRESHOLD)
//                    mHandwriterView.savePagePreview();
//                else
//                    mHandwriterView.deletePagePreview();
//            }
//        }
//
//        FragmentTransaction ft = getFragmentManager().beginTransaction();
//        SaveCompleteAutoDismissAlertDialogFragment saveCompleteAutoDismissAlertDialogFragment = SaveCompleteAutoDismissAlertDialogFragment.newInstance();
//        TouchHandlerPenABC.isPopupwindow = true;
//        ft.replace(R.id.alert_dialog_container, saveCompleteAutoDismissAlertDialogFragment, SAVE_ALERT_DIALOG_TAG).commit();
//    }

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

    private class SaveCurrentNoteBookAsyncTask extends AsyncTask<Void, Void, Void> {
        private boolean isShowAlertDialog;

        SaveCurrentNoteBookAsyncTask(boolean isShowAlertMessage) {
            this.isShowAlertDialog = isShowAlertMessage;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (mHandwriterView.getPage().objectsDrawTimePredict() > PAGE_REDRAW_TIME_THRESHOLD)
                mHandwriterView.savePagePreview();
            else
                mHandwriterView.deletePagePreview();
            Bookshelf.getInstance().getCurrentBook().save();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mSaveRunning = false;
            setNoteEdited(false);

            if (isShowAlertDialog) {
                try {
                    ((DismissDelayPostAlertDialogFragment) getFragmentManager().findFragmentByTag(SAVE_ALERT_DIALOG_TAG)).dismiss();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startRotate() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        } else if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        } else if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            // default
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        mHandwriterView.setCurrentRotation(getRequestedOrientation());
    }

    private void resetRotate() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            // default
            // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void invalidateToolboxView() {
        mHandler.removeCallbacks(mRunnableInvalidateToolbox);
        mHandler.postDelayed(mRunnableInvalidateToolbox, 0);
    }

    /**
     * Control the 2-Step-Suspend for Netronix eInk devices
     *
     * @param state 1 is enable.
     *              0 is disable.
     */
    private void PowerEnhanceSet(int state) {
        Log.d(TAG, "PowerEnhanceSet = " + state);
        try {
            Settings.System.putInt(mContext.getContentResolver(), "power_enhance_enable", state);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "set POWER_ENHANCE_ENABLE error!!");
        }
    }

    private void closeAllFragmentDialog() {
        hideInputMethod();
        alert_dialog_container.removeAllViews();
        dialog_container.removeAllViews();

        mSaveRunning = false;
        mCleanRunning = false;
        mDeleteRunning = false;
        mIsSettingPageBackgroundRequest = false;
    }

    private void hideInputMethod() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getWindow().getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveDialog(boolean allowSave) {
        SaveDialog.show();
        final boolean saveStatus = Bookshelf.getInstance().getCurrentBook().save();
        Bookshelf.getInstance().getCurrentBook().setAllowSave(allowSave);
        if (allowSave) {
            finish();
            return;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SaveDialog.dismiss();
                if (saveStatus) {
                    finish();
                }
            }
        }, 2000);
    }

    private void saveToolBoxSetting(boolean apply) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        mHandwriterView.saveSettings(editor);
        ToolboxConfiguration.getInstance().saveSettings(editor);
        if (apply) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    private void updatePageTitleString() {
        if (ToolboxConfiguration.getInstance().showMemoTheme()) {
            String mainTag = book.currentPage().getMainTag();
            if (mainTag.isEmpty())
                mBtnPageTitle.setText(book.getTitle());
            else
                mBtnPageTitle.setText(mainTag);
        } else
            mBtnPageTitle.setText(book.getTitle());
    }

    private class AutoSaveTimerTask extends TimerTask {
        public void run() {
            autoSaveTimer.cancel();
            boolean autoSaveStatus = Bookshelf.getInstance().getCurrentBook().save(true);
            Log.d(TAG, "Auto Save Status : " + autoSaveStatus);
            autoSaveTimer = new Timer(false);
            autoSaveTimer.schedule(new AutoSaveTimerTask(), autoSaveTime, autoSaveTime);
        }
    }

    private void deleteNoose() {
        setNoteEdited(true);
        mHandwriterView.getPage().setModified(true);
        mHandwriterView.deleteNooseSelect();
    }

    private void copyNoose() {
        setNoteEdited(true);
        mHandwriterView.getPage().setModified(true);
        mHandwriterView.copyNooseSelect();
    }

    private void pasteNoose() {
        setNoteEdited(true);
        mHandwriterView.pasteNooseSelect();
    }

    private void cutNoose() {
        copyNoose();
        deleteNoose();
    }

    private void createNewNoteBookAndOpen(Intent i) {

        StorageAndroid.initialize(getApplicationContext());

        if (!i.getStringExtra("CreateNote").equals("true")) {
            if (Bookshelf.getInstance().checkBookExist(UUID.fromString(i.getStringExtra("uuid")))) {
                Bookshelf.getInstance().setCurrentBook(UUID.fromString(i.getStringExtra("uuid")));
            } else {
                Bookshelf.getInstance().newBook(Global.generateNoteName());
            }
        } else {
            Bookshelf.getInstance().newBook(i.getStringExtra("titleName"));
            Bookshelf.getInstance().saveDateJsonFile();
        }
    }

}
