package name.vbraun.view.write;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.Toast;

import junit.framework.Assert;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import name.vbraun.lib.pen.Hardware;
import name.vbraun.lib.pen.HardwareButtonListener;
import name.vbraun.lib.pen.PenEventNTX;
import name.vbraun.view.write.Graphics.Tool;
import name.vbraun.view.write.LinearFilter.Filter;
import ntx.draw.nDrawHelper;
import ntx.note.ALog;
import ntx.note.CallbackEvent;
import ntx.note.Global;
import ntx.note.NoteWriterActivity;
import ntx.note.RelativePopupWindow;
import ntx.note.ToolboxConfiguration;
import ntx.note.data.BookDirectory;
import ntx.note.data.Bookshelf;
import ntx.note.data.Storage;
import ntx.note2.R;

import static ntx.note.Global.PAGE_PREVIEW_BITMAP_FILE_NAME_PREFIX;
import static ntx.note.Global.PAGE_PREVIEW_BITMAP_FILE_TYPE;

public class HandwriterView extends ViewGroup implements HardwareButtonListener {

    private static final String TAG = "Handwrite";

    public final static String KEY_LIST_PEN_INPUT_MODE = "pen_input_mode";
    public final static String KEY_DOUBLE_TAP_WHILE_WRITE = "double_tap_while_write";
    public final static String KEY_MOVE_GESTURE_WHILE_WRITING = "move_gesture_while_writing";
    public final static String KEY_MOVE_GESTURE_FIX_ZOOM = "move_gesture_fix_zoom";
    public final static String KEY_PALM_SHIELD = "palm_shield";
    public final static String KEY_DEBUG_OPTIONS = "debug_options_enable";
    public final static String KEY_PEN_SMOOTH_FILTER = "pen_smooth_filter";

    // values for the preferences key KEY_LIST_PEN_INPUT_MODE
    public final static String STYLUS_ONLY = "STYLUS_ONLY";
    public final static String STYLUS_WITH_GESTURES = "STYLUS_WITH_GESTURES";
    public final static String STYLUS_AND_TOUCH = "STYLUS_AND_TOUCH";

    private final static String KEY_TOOLBOX_IS_VISIBLE = "toolbox_is_visible";
    private final static String KEY_FIRST_ROW_TOOLBOX_IS_VISIBLE = "first_row_toolbox_is_visible";
    private final static String KEY_FULL_REFRESH_TIME = "fullrefresh_time";

    public final static String KEY_PEN_OFFSET_X = "pen_offset_x";
    public final static String KEY_PEN_OFFSET_Y = "pen_offset_y";

    private final static String BRUSH_THICKNESS = "brush_thickness";
    private final static String FOUNTAIN_THICKNESS = "fountain_thickness";

    private final static long DEFAULT_FULL_REFRESH_TIME = 60 * 60 * 1000;
    private final static int HOVER_EXIT_ACTION_DELAY = 500;
    public static final int PAGE_REDRAW_TIME_THRESHOLD = 2000;

    //// Alan add /////
    private static int mCurrentRotation = 1;
    private static int mNavigationBarHeight;
    private static int Fountain_thickness;
    public static int Brush_thickness;
    public static int Relative_Left;
    public static int width, height;

    protected final float screenDensity;
    protected final NoteWriterActivity mActivity;
    private TouchHandlerABC touchHandler;

    protected Context mContext;

    private Bitmap bitmap;
    protected Canvas canvas;
    private Toast toast;

    private boolean palmShield = false;
    private RectF palmShieldRect;
    private Paint palmShieldPaint;
    private Handler mHandler = new Handler();
    private boolean isFullRefreshable = true;
    private boolean isPenUp = true;

    private boolean doNDrawSwitchOff = true;
    private boolean doInvalidate = false;
    private int[] packet = {0, 0, 0, 0};
    private float x1, x2 = 0;

    private EventBus mEventBus;

    private Overlay overlay = null;

    private GraphicsModifiedListener graphicsListener = null;

    private InputListener inputListener = null;

    // actual data
    private Page page;

    // preferences
    private long full_refresh_time = -1;

    private int pen_offset_x = 0;
    private int pen_offset_y = 0;
    public static boolean is_anti_color = false;

    private Timer mTimer;

    // artis for test
    // private Filter penSmoothFilter = Filter.KERNEL_SAVITZKY_GOLAY_11;
    // private Filter penSmoothFilter = Filter.KERNEL_SAVITZKY_GOLAY_5;
    // private Filter penSmoothFilter = Filter.KERNEL_GAUSSIAN_11;
    // private Filter penSmoothFilter = Filter.KERNEL_GAUSSIAN_5;
    private Filter penSmoothFilter = Filter.KERNEL_NONE;

    protected boolean onlyPenInput = true;
    protected boolean moveGestureWhileWriting = true;
    protected boolean moveGestureFixZoom = true;
    protected int moveGestureMinDistance = 400; // pixels
    protected boolean doubleTapWhileWriting = true;

    private boolean acceptInput = false;

    public AlertDialog photoControlDialog = null;
    public AlertDialog addPhotoControlDialog = null;
    private String[] stringsPhotoControl = new String[3];
    private String[] stringsAddPhotoControl = new String[2];

    public GraphicsControlPoint NowEditedGraphics = null;

    private boolean allowFlipPage = true;
    private float previousPositionX;
    private LinkedList<Graphics> toCopy = new LinkedList<Graphics>();
    private Runnable runHoverExitAction = new Runnable() {
        @Override
        public void run() {
            if (isPenUp) {
                if (Hardware.isEinkHardwareType()) {
                    if (doNDrawSwitchOff) {
                        nDrawHelper.NDrawSwitch(false);
                    }
                    if (doInvalidate) {
                        invalidate();
                    }
                }
                // TODO - invalidate toolbox will make the nDraw strokes flash
                // mActivity.invalidateToolboxView();
            }
        }
    };

    public HandwriterView(Context context) {
        super(context);

        mContext = context;

        mEventBus = EventBus.getDefault();
        mEventBus.register(this);

        mActivity = (NoteWriterActivity) context;
        //////  Alan 20190223  move nDrawInit to NoteWriterActivity///
        /* nDrawHelper.NDrawInit(activity);    */

        setFocusable(true);
        setAlwaysDrawnWithCacheEnabled(false);
        setDrawingCacheEnabled(false);
        setWillNotDraw(false);
        setBackgroundDrawable(null);

        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        screenDensity = metrics.density;

        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;


        Global.SWIPE_DISTANCE_THRESHOLD = Hardware.getCMtoPixel(Global.SWIPE_DISTANCE_THRESHOLD_BY_CM, metrics);

        Hardware hw = Hardware.getInstance(context);
        hw.addViewHack(this);
        hw.setOnHardwareButtonListener(this);
        // setLayerType(LAYER_TYPE_SOFTWARE, null);
        photoControlDialog = getPhotoControlDialog();
        addPhotoControlDialog = getAddPhotoControlDialog();

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++)
            getChildAt(i).layout(l, t, r, b);
        // toolbox.layout(l, t, r, b);
        // if (editText != null) {
        // editText.layout(100, 70, 400, 200);
        // }
        if (palmShield)
            initPalmShield();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int curW = bitmap != null ? bitmap.getWidth() : 0;
        int curH = bitmap != null ? bitmap.getHeight() : 0;
        // Dango 20181002 modify draw region when toolbar position changed in portrait
        // mode.
        Boolean isLeft = ToolboxConfiguration.getInstance().isToolbarAtLeft();
        Relative_Left = getRelativeLeft(HandwriterView.this);
        // if (curW >= w && curH >= h) {
        // return;
        // }
        if (curW >= w)
            curW = w;
        if (curH >= h)
            curH = h;
        if (curW < w)
            curW = w;
        if (curH < h)
            curH = h;

        Bitmap newBitmap = Bitmap.createBitmap(curW, curH, Bitmap.Config.ARGB_8888);
        Canvas newCanvas = new Canvas();
        newCanvas.setBitmap(newBitmap);
        if (bitmap != null) {
            newCanvas.drawBitmap(bitmap, 0, 0, null);
        }
        bitmap = newBitmap;
        canvas = newCanvas;
        setPageAndZoomOut(page, true);
        switch (mCurrentRotation) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                // nDrawHelper.NDrawSkia2FBUnInit();
                int landscape_left;
                int landscape_top;
                int landscape_right;
                int landscape_bottom;

                if (isLeft) {

                    landscape_left = width - curW;
                    landscape_top = getRelativeTop(HandwriterView.this);
                    landscape_right = landscape_left + curW;
                    landscape_bottom = landscape_top + curH;

                } else {

                    landscape_left = 0;
                    landscape_top = getRelativeTop(HandwriterView.this);
                    landscape_right = landscape_left + curW;
                    landscape_bottom = landscape_top + curH;
                }
                // l t r b landscape
                packet[0] = landscape_left;
                packet[1] = landscape_top;
                packet[2] = landscape_right;
                packet[3] = landscape_bottom;
//                nDrawHelper.NDrawInit(activity);
                nDrawHelper.NDrawSetDrawRegion(packet);
                initPenStatus();
                break;

            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                // nDrawHelper.NDrawSkia2FBUnInit();
                int landscape_left_b = 0;
                int landscape_top_b = getRelativeTop(HandwriterView.this);
                int landscape_right_b = landscape_left_b + curW;
                int landscape_bottom_b = landscape_top_b + curH;
                int[] packet_b = {landscape_left_b, landscape_top_b, landscape_right_b, landscape_bottom_b};
                // nDrawHelper.NDrawInit(packet_b);
                nDrawHelper.NDrawSetDrawRegion(packet);
                break;

            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                // nDrawHelper.NDrawSkia2FBUnInit();
                int landscape_left_rl = mNavigationBarHeight;
                int landscape_top_rl = 0;
                int landscape_right_rl = landscape_left_rl + curH;
                int landscape_bottom_rl = landscape_top_rl + curW;
                int[] packet_rl = {landscape_left_rl, landscape_top_rl, landscape_right_rl, landscape_bottom_rl};// l t r
                // b
                // landscape
                // nDrawHelper.NDrawInit(packet_rl);
                nDrawHelper.NDrawSetDrawRegion(packet);
                // Log.d("testtHere",""+curH+" "+curW);
                Log.d("Alan_test", "NavigationBarHeight" + mNavigationBarHeight);
                break;

            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                // nDrawHelper.NDrawSkia2FBUnInit();
                int landscape_left_a = 0;
                int landscape_top_a = getRelativeTop(HandwriterView.this);
                int landscape_right_a = landscape_left_a + curW;
                int landscape_bottom_a = landscape_top_a + curH;
                int[] packet_a = {landscape_left_a, landscape_top_a, landscape_right_a, landscape_bottom_a};
                // nDrawHelper.NDrawInit(packet_a);
                nDrawHelper.NDrawSetDrawRegion(packet);
                break;

        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap == null)
            return;

        if (touchHandler != null) {
            touchHandler.draw(canvas, bitmap);
        } else {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
        if (overlay != null)
            overlay.draw(canvas);
        if (palmShield) {
            canvas.drawRect(palmShieldRect, palmShieldPaint);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (RelativePopupWindow.isPopupWindowShowing()) {
            if (ev.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER) {
                CallbackEvent callbackEvent = new CallbackEvent();
                callbackEvent.setMessage(CallbackEvent.DISMISS_POPUPWINDOW);
                mEventBus.post(callbackEvent);
            }
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!acceptInput)
            return false;
        if (touchHandler == null)
            return false;

        ToolboxConfiguration mToolboxConfiguration = ToolboxConfiguration.getInstance();
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (Hardware.isPenButtonPressed(event) || (event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER)) {
                mToolboxConfiguration.setCurrentToolViewId(mToolboxConfiguration.getPrevSelectedToolViewId(), true);
                mToolboxConfiguration.setCurrentTool(mToolboxConfiguration.getPrevTool());
                return false;
            }
            // Alan: Reset to default update mode when acton_up
            if (mToolboxConfiguration.getCurrentTool() == Tool.LINE
                    || mToolboxConfiguration.getCurrentTool() == Tool.RECTANGLE
                    || mToolboxConfiguration.getCurrentTool() == Tool.OVAL
                    || mToolboxConfiguration.getCurrentTool() == Tool.TRIANGLE
                    || mToolboxConfiguration.getCurrentTool() == Tool.IMAGE
                    || mToolboxConfiguration.getCurrentTool() == Tool.NOOSE) {
                Intent ar = new Intent("ntx.eink_control.GLOBAL_REFRESH");
                ar.putExtra("updatemode", PenEventNTX.UPDATE_MODE_GLOBAL_RESET);
                ar.putExtra("commandFromNtxApp", true);
                getContext().sendBroadcast(ar);
                // Dango 20180921 use full auto double white refresh after action up to decrease
                // ghosting
                // Intent whiteRefreshIntent = new Intent("ntx.eink_control.QUICK_REFRESH");
                // whiteRefreshIntent.putExtra("updatemode",PenEventNTX.UPDATE_MODE_GLOBAL_FULL_AUTO);
                // getContext().sendBroadcast(whiteRefreshIntent);

                // Dango 20180928 only pen-up triggers GC16 Full Refresh
                if (Hardware.isPenEvent(event)) {

                    Intent quickIntent = new Intent("ntx.eink_control.QUICK_REFRESH");
                    quickIntent.putExtra("updatemode", PenEventNTX.UPDATE_MODE_FULL_GC16);
                    quickIntent.putExtra("commandFromNtxApp", true);
                    getContext().sendBroadcast(quickIntent);
                    invalidate();//Dango 20190116 Clean abnormal ghosting after moving image

                }

            }

        } else if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (Hardware.isPenButtonPressed(event) || (event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER)) {
                if (mToolboxConfiguration.getCurrentTool() == Tool.PENCIL
                        || mToolboxConfiguration.getCurrentTool() == Tool.FOUNTAINPEN
                        || mToolboxConfiguration.getCurrentTool() == Tool.BRUSH) {
                    if (page.isNDrawDuModeRequiredForGrayscale()) {
                        invalidate(PenEventNTX.UPDATE_MODE_PARTIAL_GL16);
                    } else {
                        invalidate(PenEventNTX.UPDATE_MODE_PARTIAL_DU_WAIT);
                    }
                }
                mToolboxConfiguration.setCurrentToolViewId(R.id.btn_toolbox_eraser_line, true);
                mToolboxConfiguration.setCurrentTool(Tool.ERASER);
            }
            Boolean isEraser = ((event.getButtonState() & MotionEvent.BUTTON_SECONDARY) > 0 ? true : false)
                    || (event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER);
            if ((mToolboxConfiguration.getCurrentTool() == Tool.LINE
                    || mToolboxConfiguration.getCurrentTool() == Tool.RECTANGLE
                    || mToolboxConfiguration.getCurrentTool() == Tool.OVAL
                    || mToolboxConfiguration.getCurrentTool() == Tool.TRIANGLE
                    || mToolboxConfiguration.getCurrentTool() == Tool.IMAGE
                    || mToolboxConfiguration.getCurrentTool() == Tool.NOOSE) && (!isEraser)
                    && Hardware.isPenEvent(event)) {
                // Alan : Using A2+D+Wait mode
                Intent aw = new Intent("ntx.eink_control.GLOBAL_REFRESH");
                aw.putExtra("updatemode", PenEventNTX.UPDATE_MODE_GLOBAL_PARTIAL_A2_WITH_DITHER_WITH_WAIT);
                aw.putExtra("commandFromNtxApp", true);
                getContext().sendBroadcast(aw);

                // Dango 20180921 use quick_refresh du before moving to decrease ghosting
                Intent quickIntent = new Intent("ntx.eink_control.QUICK_REFRESH");
                quickIntent.putExtra("updatemode", PenEventNTX.UPDATE_MODE_FULL_DU_WITH_DITHER);
                quickIntent.putExtra("commandFromNtxApp", true);
                getContext().sendBroadcast(quickIntent);
            }
        }

        if (mToolboxConfiguration.getCurrentTool() == Tool.PENCIL
                || mToolboxConfiguration.getCurrentTool() == Tool.FOUNTAINPEN
                || mToolboxConfiguration.getCurrentTool() == Tool.BRUSH) {

            TouchHandlerPenABC.isPopupwindow = false;
            nDrawHelper.NDrawSwitch(true);

        } else if (mToolboxConfiguration.getCurrentTool() == Tool.IMAGE
                || mToolboxConfiguration.getCurrentTool() == Tool.INFO
                || mToolboxConfiguration.getCurrentTool() == Tool.ERASER
                || mToolboxConfiguration.getCurrentTool() == Tool.LINE
                || mToolboxConfiguration.getCurrentTool() == Tool.RECTANGLE
                || mToolboxConfiguration.getCurrentTool() == Tool.OVAL
                || mToolboxConfiguration.getCurrentTool() == Tool.TRIANGLE
                || mToolboxConfiguration.getCurrentTool() == Tool.NOOSE
        ) {

            nDrawHelper.NDrawSwitch(false);

        } else {
            TouchHandlerPenABC.isPopupwindow = false;
            nDrawHelper.NDrawSwitch(true);
        }

        // if (Hardware.isPenButtonPressed(event)) {
        // bPenButtonPressedInTouch = true;
        // } else {
        // bPenButtonPressedInTouch = false;
        // }

        if (Hardware.isEinkHandWritingHardwareType()) {
            if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                nDrawHelper.NDrawSwitch(false);
                // ALog.debug("FINGER! getActionMasked():", event.getActionMasked());
                ////////////////// simplest swipe detector //Dragon
                if (Global.swipeNoteWriter) {
                    int distanceThreshold = 0;
                    // public boolean onTouchEvent(MotionEvent event)
                    {
                        if (Hardware.isEinkUsingLargerUI())
                            distanceThreshold = Global.SWIPE_DISTANCE_THRESHOLD;// *4/3;
                        else
                            distanceThreshold = Global.SWIPE_DISTANCE_THRESHOLD;

                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                x1 = event.getX();
                                allowFlipPage = true;
                                previousPositionX = x1;
                                break;
                            case MotionEvent.ACTION_POINTER_DOWN:
                                allowFlipPage = false;
                                break;
                            case MotionEvent.ACTION_POINTER_UP:
                                allowFlipPage = false;
                                break;
                            case MotionEvent.ACTION_MOVE:
                                float currentPositionX = event.getX();
                                boolean isTeleport = Math.abs(currentPositionX - previousPositionX) > Global.TELEPORT_DISTANCE_THRESHOLD;
                                if (isTeleport) {
                                    allowFlipPage = false;
                                }
                                previousPositionX = currentPositionX;
                                break;
                            case MotionEvent.ACTION_UP:
                                x2 = event.getX();
                                float deltaX = x2 - x1;
                                // ALog.debug("x1: "+ x1+" x2: "+ x2+" deltaX: "+ deltaX);
                                if ((Math.abs(deltaX) > distanceThreshold) && (deltaX > 0) && allowFlipPage) {
                                    // ALog.debug("SWIPE! BACK!");
                                    // trigger previous button
                                    // ((ImageButton) findViewById(R.id.btn_toolbox_prev_page)).performClick();
                                    CallbackEvent prevPageCallbackEvent = new CallbackEvent();
                                    prevPageCallbackEvent.setMessage(CallbackEvent.PREV_PAGE);
                                    mEventBus.post(prevPageCallbackEvent);
                                } else if ((Math.abs(deltaX) > distanceThreshold) && (deltaX < 0) && allowFlipPage) {
                                    // ALog.debug("SWIPE! FORWARD!");
                                    // trigger next button
                                    // ((ImageButton) findViewById(R.id.btn_toolbox_next_page)).performClick();
                                    CallbackEvent nextPageCallbackEvent = new CallbackEvent();
                                    nextPageCallbackEvent.setMessage(CallbackEvent.NEXT_PAGE);
                                    mEventBus.post(nextPageCallbackEvent);
                                } else {
                                    // nothing
                                }
                                break;
                        }
                        // return super.onTouchEvent(event);
                    }
                }
                //////////////////
                return true;
            }
        }

        // if ( Hardware.isPenButtonPressed(event) ) {
        // PenBtnPressedCounter++;
        //
        // Log.d(TAG, "HWV<--isPenButtonPressed(" + PenBtnPressedCounter +")");
        // boolean toolbox_is_visible = getToolBox().isToolboxVisible();
        // toolbox_is_visible = !toolbox_is_visible;
        // getToolBox().setToolboxVisible(toolbox_is_visible);
        //// getToolBox().setToolboxVisible(false);
        // }

        // return touchHandler.onTouchEvent(event);
        setFullRefreshable(false);
        touchHandler.onTouchEvent(event);

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            isPenUp = false;
            mHandler.removeCallbacks(runHoverExitAction);

        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            // Invalidate with MODE_APPNDRAWSTROKESYNC when PEN_UP
            // if (getToolType() == Tool.ERASER || Hardware.isPenButtonPressed(event)){
            // invalidate();
            // }else{
            invalidate(PenEventNTX.UPDATE_MODE_PEN_UP);
            // }
            isPenUp = true;
            // toolbox.toolboxClose();
        }

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Hardware.onKeyDown(keyCode, event))
            return true;
        else
            return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (Hardware.onKeyUp(keyCode, event))
            return true;
        else
            return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        ALog.e(TAG, "onDragEv");
        return super.onDragEvent(event);
    }

    // private boolean bPenButtonPressedInMotion = false;
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {

        // if pen is near, reset swipeNoteWriter detector.
        if (event.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
            x1 = 0;
            x2 = 0;
        }

        // Daniel : Switch the toolbox visibleness when pen button is pressed in user
        // motion
        // if (Hardware.isPenButtonPressed(event)) {
        // bPenButtonPressedInMotion = true;
        // } else {
        // if (bPenButtonPressedInMotion) {
        // if (!bPenButtonPressedInTouch) {
        // boolean bToolboxVisible = getToolBox().isToolboxVisible();
        // setToolboxVisible(!bToolboxVisible);
        // }
        // bPenButtonPressedInMotion = false;
        // bPenButtonPressedInTouch = false;
        // }
        // }
        //
        // if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
        // bPenButtonPressedInMotion = false;
        // bPenButtonPressedInTouch = false;
        // }

        if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
            if (!isPenUp) {
                if (ToolboxConfiguration.getInstance().getCurrentTool() == Tool.ERASER) {
                    // [E60QR2] Eraser : it will not call pen ACTION_UP when pen up
                    callOnStrokeFinishedListener();
                }
            }
            if (isPenUp)
                doHoverExitAction(true, false);
        }

        return true;
    }

    @Override
    public void onHardwareButtonListener(Type button) {
        // interrupt();
        // setToolType(Tool.ERASER);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mEventBus.unregister(this);

    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent(ToolboxConfiguration event) {
        @Tool int tool = event.getCurrentTool();
        setUpTouchHandler(tool);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CallbackEvent event) {
        if (event.getMessage().equals(CallbackEvent.PAGE_DRAW_COMPLETED)) {
            String previewBitmapFileName = PAGE_PREVIEW_BITMAP_FILE_NAME_PREFIX + page.getUUID().toString() + PAGE_PREVIEW_BITMAP_FILE_TYPE;
            BookDirectory dir = Storage.getInstance().getBookDirectory(Bookshelf.getInstance().getCurrentBook().getUUID());
            File previewBitmapFile = new File(dir, previewBitmapFileName);
            if (!previewBitmapFile.exists()) {
                if (page.objectsDrawTimePredict() > PAGE_REDRAW_TIME_THRESHOLD)
                    savePagePreview();
            } else {
                if (page.objectsDrawTimePredict() <= PAGE_REDRAW_TIME_THRESHOLD)
                    deletePagePreview();
            }
        }
    }

    protected void callOnStrokeFinishedListener() {
        if (inputListener != null) {
            // ALog.i(TAG, "HWV<-----callOnStrokeFinishedListener....");
            inputListener.onStrokeFinishedListener();
        }
    }

    protected void callOnEditImageListener(GraphicsImage image) {
        File file = image.getFile();
        if (inputListener == null)
            return;
        if (file == null)
            inputListener.onPickImageListener(image);
        else
            inputListener.onEditImageListener(image);
    }

    /**
     * Whether the point (x,y) is on the palm shield and hence should be ignored.
     *
     * @param event
     * @return whether the touch point is to be ignored.
     */
    protected boolean isOnPalmShield(MotionEvent event) {
        if (!palmShield)
            return false;
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return palmShieldRect.contains(event.getX(), event.getY());
            case MotionEvent.ACTION_POINTER_DOWN:
                int idx = event.getActionIndex();
                return palmShieldRect.contains(event.getX(idx), event.getY(idx));
        }
        return false;
    }

    protected void toastIsReadonly() {
        String s = "Page is readonly";
        if (toast == null)
            toast = Toast.makeText(getContext(), s, Toast.LENGTH_SHORT);
        else {
            toast.setText(s);
        }
        toast.show();
    }

    protected void saveStroke(Stroke s) {
        if (page.is_readonly) {
            toastIsReadonly();
            return;
        }
        if (page != null && graphicsListener != null) {
            graphicsListener.onGraphicsCreateListener(page, s);
        }
    }

    protected void saveGraphics(GraphicsControlPoint graphics) {
        if (page.is_readonly) {
            toastIsReadonly();
            return;
        }
        if (page != null && graphicsListener != null) {
            graphicsListener.onGraphicsCreateListener(page, graphics);
        }
    }

    protected void removeGraphics(GraphicsControlPoint graphics) {
        if (page.is_readonly) {
            toastIsReadonly();
            return;
        }
        if (page != null && graphicsListener != null) {
            graphicsListener.onGraphicsEraseListener(page, graphics);
        }
    }

    protected void modifyGraphics(Graphics modifiedGraphics) {
        if (page.is_readonly) {
            toastIsReadonly();
            return;
        }
        if (page != null && graphicsListener != null) {
            graphicsListener.onGraphicsModifyListener(page, modifiedGraphics);
        }
    }

    protected void modifyGraphicsList(LinkedList<Graphics> modifiedGraphicsList) {
        if (page.is_readonly) {
            toastIsReadonly();
            return;
        }
        if (page != null && graphicsListener != null) {
            graphicsListener.onGraphicsListModifyListener(page, modifiedGraphicsList);
        }
    }

    protected void setNowEditedGraphics(GraphicsControlPoint nowEdited) {
        NowEditedGraphics = nowEdited;
    }

    protected Filter getPenSmoothFilter() {
        return penSmoothFilter;
    }

    protected static boolean getAntiColor() {
        return is_anti_color;
    }

    protected boolean getOnlyPenInput() {
        return onlyPenInput;
    }

    protected boolean getDoubleTapWhileWriting() {
        return doubleTapWhileWriting;
    }

    protected boolean getMoveGestureWhileWriting() {
        return moveGestureWhileWriting;
    }

    protected boolean getMoveGestureFixZoom() {
        return moveGestureFixZoom;
    }

    protected int getMoveGestureMinDistance() {
        return moveGestureMinDistance;
    }

    protected boolean eraseGraphicsIn(RectF r) {
        LinkedList<Graphics> toRemove = new LinkedList<>();
        for (Stroke s : page.strokes) {
            if (!RectF.intersects(r, s.getBoundingBox()))
                continue;
            if (s.intersects(r)) {
                toRemove.add(s);
            }
        }
        for (GraphicsControlPoint graphics : page.lineArt) {
            if (!RectF.intersects(r, graphics.getBoundingBox()))
                continue;
            if (graphics.intersects(r)) {
                toRemove.add(graphics);
            }
        }
        for (GraphicsControlPoint graphics : page.rectangleArt) {
            if (!RectF.intersects(r, graphics.getBoundingBox()))
                continue;
            if (graphics.intersects(r)) {
                toRemove.add(graphics);
            }
        }
        for (GraphicsControlPoint graphics : page.ovalArt) {
            if (!RectF.intersects(r, graphics.getBoundingBox()))
                continue;
            if (graphics.intersects(r)) {
                toRemove.add(graphics);
            }
        }
        for (GraphicsControlPoint graphics : page.triangleArt) {
            if (!RectF.intersects(r, graphics.getBoundingBox()))
                continue;
            if (graphics.intersects(r)) {
                toRemove.add(graphics);
            }
        }

        if (toRemove.isEmpty())
            return false;
        else {
            graphicsListener.onGraphicsListEraseListener(page, toRemove);
            invalidate();
            return true;
        }
    }

    protected void zoomOutAndFillScreen() {
        // float W = canvas.getWidth();
        // float H = canvas.getHeight();
        // float scaleToSeeAll = Math.min(H, W / page.aspect_ratio);
        // float dx, dy;
        // dx = (W - scaleToSeeAll * page.aspect_ratio) / 2;
        // dy = (H - scaleToSeeAll) / 2;
        // page.setTransform(dx, dy, scaleToSeeAll, canvas);
        callOnStrokeFinishedListener();
        zoomFitWidth();
        page.draw(canvas);
        invalidate();
        doFullRefresh(1000);
    }

    protected void doFullRefresh(int delay) {
        Runnable runInvalidate = new Runnable() {
            @Override
            public void run() {
                if (Hardware.isEinkHardwareType()) {
                    invalidate(PenEventNTX.UPDATE_MODE_SCREEN);
                }
            }
        };
        if (Hardware.isEinkHardwareType()) {
            mHandler.removeCallbacks(runInvalidate);
            mHandler.postDelayed(runInvalidate, delay);
        }
    }

    protected void centerAndFillScreen(float xCenter, float yCenter) {
        float page_offset_x = page.transformation.offset_x;
        float page_offset_y = page.transformation.offset_y;
        float page_scale = page.transformation.scale;
        float W = canvas.getWidth();
        float H = canvas.getHeight();
        float scaleToFill = Math.max(H, W / page.aspect_ratio);
        float scaleToSeeAll = Math.min(H, W / page.aspect_ratio);
        float scale;
        boolean seeAll = (page_scale == scaleToFill); // toggle
        if (seeAll)
            scale = scaleToSeeAll;
        else
            scale = scaleToFill;
        float x = (xCenter - page_offset_x) / page_scale * scale;
        float y = (yCenter - page_offset_y) / page_scale * scale;
        float dx, dy;
        if (seeAll) {
            dx = (W - scale * page.aspect_ratio) / 2;
            dy = (H - scale) / 2;
        } else if (scale == H) {
            dx = W / 2 - x;// + (-scale*page.aspect_ratio)/2;
            dy = 0;
        } else {
            dx = 0;
            dy = H / 2 - y;// + (-scale)/2;
        }
        page.setTransform(dx, dy, scale, canvas);
        page.draw(canvas);
        invalidate();
        // doFullRefresh(1000);
    }

    public void onFinish() {
        if (getPage().objectsDrawTimePredict() > PAGE_REDRAW_TIME_THRESHOLD)
            savePagePreview();
        else
            deletePagePreview();

        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
    }

    public void setCurrentRotation(int mCurrent_Rotation) {
        this.mCurrentRotation = mCurrent_Rotation;
        nDrawHelper.NDrawSetInputRotation(mCurrentRotation);
    }

    public void getBottomHeight(int mNavigationBar_Height) {
        this.mNavigationBarHeight = mNavigationBar_Height;

    }

    public void setOverlay(Overlay overlay) {
        this.overlay = overlay;
        invalidate();
    }

    public void setOnInputListener(InputListener listener) {
        inputListener = listener;
    }

    /**
     * Stop input event processing (to be called from onPause/onResume if you want
     * to make sure that no events are processed
     */
    public void stopInput() {
        acceptInput = false;
    }

    /**
     * Start input event processing. Needs to be called from onResume() when the
     * activity is ready to receive input.
     */
    public void startInput() {
        acceptInput = true;
    }

    public void setOnGraphicsModifiedListener(GraphicsModifiedListener newListener) {
        graphicsListener = newListener;
    }

    public void add(Graphics graphics) {
        if (graphics instanceof Stroke) { // most likely first
            Stroke s = (Stroke) graphics;
            page.addStroke(s);
            s.draw(canvas);
            return;
        } else if (graphics instanceof GraphicsLine) {
            GraphicsLine l = (GraphicsLine) graphics;
            page.addLine(l);
        } else if (graphics instanceof GraphicsRectangle) {
            GraphicsRectangle r = (GraphicsRectangle) graphics;
            page.addRectangle(r);
        } else if (graphics instanceof GraphicsOval) {
            GraphicsOval o = (GraphicsOval) graphics;
            page.addOval(o);
        } else if (graphics instanceof GraphicsTriangle) {
            GraphicsTriangle t = (GraphicsTriangle) graphics;
            page.addTriangle(t);
        } else if (graphics instanceof GraphicsImage) {
            GraphicsImage img = (GraphicsImage) graphics;
            page.addImage(img);
        } else
            Assert.fail("Unknown graphics object");

        page.draw(canvas, graphics.getBoundingBox());
        // if (Hardware.isEinkHardwareType()) {
        // invalidate(graphics.getBoundingBoxRoundOut(),PenEventNTX.UPDATE_MODE_PEN);
        // } else {
        // invalidate(graphics.getBoundingBoxRoundOut());
        // }
    }

    public void remove(Graphics graphics) {
        if (graphics instanceof Stroke) {
            Stroke s = (Stroke) graphics;
            page.removeStroke(s);
        } else if (graphics instanceof GraphicsLine) {
            GraphicsLine l = (GraphicsLine) graphics;
            page.removeLine(l);
        } else if (graphics instanceof GraphicsRectangle) {
            GraphicsRectangle r = (GraphicsRectangle) graphics;
            page.removeRectangle(r);
        } else if (graphics instanceof GraphicsOval) {
            GraphicsOval o = (GraphicsOval) graphics;
            page.removeOval(o);
        } else if (graphics instanceof GraphicsTriangle) {
            GraphicsTriangle t = (GraphicsTriangle) graphics;
            page.removeTriangle(t);
        } else if (graphics instanceof GraphicsImage) {
            GraphicsImage img = (GraphicsImage) graphics;
            page.removeImage(img);
        } else
            Assert.fail("Unknown graphics object");
        page.draw(canvas, graphics.getBoundingBox());
        // Invalidate with MODE_APPNDRAWSTROKESYNC when erase a stroke
        invalidate(graphics.getBoundingBoxRoundOut());
        // invalidate(PenEventNTX.UPDATE_MODE_PEN_UP);
    }

    public void remove_for_erase(LinkedList<Graphics> graphics) {
        RectF boundingBox = new RectF();
        Rect boundingBoxRoundOut = new Rect();
        RectF firstGraphicBoundingBox = graphics.getFirst().getBoundingBox();

        boundingBox.left = firstGraphicBoundingBox.left;
        boundingBox.top = firstGraphicBoundingBox.top;
        boundingBox.right = firstGraphicBoundingBox.right;
        boundingBox.bottom = firstGraphicBoundingBox.bottom;

        for (Graphics graphic : graphics) {
            if (graphic instanceof Stroke) {
                page.removeStroke((Stroke) graphic);
            } else if (graphic instanceof GraphicsLine) {
                page.removeLine((GraphicsLine) graphic);
            } else if (graphic instanceof GraphicsRectangle) {
                page.removeRectangle((GraphicsRectangle) graphic);
            } else if (graphic instanceof GraphicsOval) {
                page.removeOval((GraphicsOval) graphic);
            } else if (graphic instanceof GraphicsTriangle) {
                page.removeTriangle((GraphicsTriangle) graphic);
            } else
                Assert.fail("Unknown graphics object");

            RectF r = graphic.getBoundingBox();
            boundingBox.left = r.left < boundingBox.left ? r.left : boundingBox.left;
            boundingBox.top = r.top < boundingBox.top ? r.top : boundingBox.top;
            boundingBox.right = r.right > boundingBox.right ? r.right : boundingBox.right;
            boundingBox.bottom = r.bottom > boundingBox.bottom ? r.bottom : boundingBox.bottom;
        }

        boundingBox.roundOut(boundingBoxRoundOut);
        boundingBox.top -= 5;
        boundingBox.left -= 5;
        boundingBox.bottom += 5;
        boundingBox.right += 5;
        page.draw(canvas, boundingBox);
        invalidate(boundingBoxRoundOut);
    }

    public void add_for_erase_revert(LinkedList<Graphics> graphics) {
        for (Graphics graphic : graphics) {
            if (graphic instanceof Stroke) {
                page.addStroke((Stroke) graphic);
            } else if (graphic instanceof GraphicsLine) {
                page.addLine((GraphicsLine) graphic);
            } else if (graphic instanceof GraphicsRectangle) {
                page.addRectangle((GraphicsRectangle) graphic);
            } else if (graphic instanceof GraphicsOval) {
                page.addOval((GraphicsOval) graphic);
            } else if (graphic instanceof GraphicsTriangle) {
                page.addTriangle((GraphicsTriangle) graphic);
            } else
                Assert.fail("Unknown graphics object");

            graphic.draw(canvas);
        }
        invalidate();
    }

    public void remove_for_clear(Page newPage) {
        page = newPage;
        page.strokes.clear();
        page.lineArt.clear();
        page.rectangleArt.clear();
        page.ovalArt.clear();
        page.triangleArt.clear();
        page.draw(canvas);
        invalidate();
    }

    public void add_for_clear_revert(LinkedList<Stroke> strokes,
                                     LinkedList<GraphicsLine> lines,
                                     LinkedList<GraphicsRectangle> rectangles,
                                     LinkedList<GraphicsOval> ovals,
                                     LinkedList<GraphicsTriangle> triangles) {
        page.strokes.addAll(strokes);
        page.lineArt.addAll(lines);
        page.rectangleArt.addAll(rectangles);
        page.ovalArt.addAll(ovals);
        page.triangleArt.addAll(triangles);
        CallbackEvent event = new CallbackEvent();
        if (page.objectsDrawTimePredict() > PAGE_REDRAW_TIME_THRESHOLD) {
            event.setMessage(CallbackEvent.PAGE_DRAW_TASK_HEAVY);
        } else {
            event.setMessage(CallbackEvent.PAGE_DRAW_TASK_LIGHT);
        }
        mEventBus.post(event);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                page.draw(canvas);
            }
        });
        invalidate();
    }

    public void add(LinkedList<Stroke> penStrokes) {
        page.strokes.addAll(penStrokes);
        page.draw(canvas);
        invalidate();
    }

    public void remove(LinkedList<Stroke> penStrokes) {
        page.strokes.removeAll(penStrokes);
        page.setModified(true);
        page.draw(canvas);
        invalidate();
        // doFullRefresh(1000);
    }

    public void modify_graphics(Graphics oldGraphics, Graphics newGraphics) {
        Rect boundingBoxRoundOut = new Rect();
        RectF oldBoundingBox = new RectF();
        RectF newBoundingBox = new RectF();

        oldBoundingBox.left = oldGraphics.getBoundingBox().left;
        oldBoundingBox.right = oldGraphics.getBoundingBox().right;
        oldBoundingBox.top = oldGraphics.getBoundingBox().top;
        oldBoundingBox.bottom = oldGraphics.getBoundingBox().bottom;
        newBoundingBox.left = newGraphics.getBoundingBox().left;
        newBoundingBox.right = newGraphics.getBoundingBox().right;
        newBoundingBox.top = newGraphics.getBoundingBox().top;
        newBoundingBox.bottom = newGraphics.getBoundingBox().bottom;

        page.modifyGraphics(oldGraphics, newGraphics);

        page.draw(canvas, oldBoundingBox);
        oldBoundingBox.roundOut(boundingBoxRoundOut);
        invalidate(boundingBoxRoundOut);

        page.draw(canvas, newBoundingBox);
        newBoundingBox.roundOut(boundingBoxRoundOut);
        invalidate(boundingBoxRoundOut);
    }

    public void modify_graphicsList(LinkedList<Graphics> oldGraphicsList, LinkedList<Graphics> newGraphicsList) {
        Rect boundingBoxRoundOut = new Rect();
        oldGraphicsList.get(0).computeBoundingBox();
        newGraphicsList.get(0).computeBoundingBox();
        RectF oldBoundingBox = new RectF();
        RectF newBoundingBox = new RectF();
        oldBoundingBox.left = oldGraphicsList.get(0).getBoundingBox().left;
        oldBoundingBox.right = oldGraphicsList.get(0).getBoundingBox().right;
        oldBoundingBox.top = oldGraphicsList.get(0).getBoundingBox().top;
        oldBoundingBox.bottom = oldGraphicsList.get(0).getBoundingBox().bottom;
        newBoundingBox.left = newGraphicsList.get(0).getBoundingBox().left;
        newBoundingBox.right = newGraphicsList.get(0).getBoundingBox().right;
        newBoundingBox.top = newGraphicsList.get(0).getBoundingBox().top;
        newBoundingBox.bottom = newGraphicsList.get(0).getBoundingBox().bottom;

        page.modifyGraphics(oldGraphicsList.get(0), newGraphicsList.get(0));

        for (int i = 1; i < oldGraphicsList.size(); i++) {
            oldGraphicsList.get(i).computeBoundingBox();
            newGraphicsList.get(i).computeBoundingBox();
            oldBoundingBox.union(oldGraphicsList.get(i).getBoundingBox());
            newBoundingBox.union(newGraphicsList.get(i).getBoundingBox());
            page.modifyGraphics(oldGraphicsList.get(i), newGraphicsList.get(i));
        }

        oldBoundingBox.top -= 5;
        oldBoundingBox.left -= 5;
        oldBoundingBox.bottom += 5;
        oldBoundingBox.right += 5;

        page.draw(canvas, oldBoundingBox);
        oldBoundingBox.roundOut(boundingBoxRoundOut);
        invalidate(boundingBoxRoundOut);

        page.draw(canvas, newBoundingBox);
        newBoundingBox.roundOut(boundingBoxRoundOut);
        invalidate(boundingBoxRoundOut);
    }

    /**
     * Set the image
     *
     * @param uuid The UUID
     * @param name The image file name (path+uuid+extension)
     */
    public void setImage(UUID uuid, String name, boolean constrainAspect) {
        for (GraphicsImage image : page.images)
            if (image.getUuid().equals(uuid)) {
                if (name == null)
                    page.images.remove(image);
                else {
                    if (image.checkFileName(name)) {
                        image.setFile(name, constrainAspect);
                    } else {
                        ALog.e(TAG, "incorrect image file name");
                        page.images.remove(image);
                    }
                }
                page.draw(canvas);
                invalidate();
                return;
            }
        ALog.e(TAG, "setImage(): Image does not exist");
    }

    public void removeImage() {
        remove(NowEditedGraphics);
        doFullRefresh(500);
        resetNDrawUpdateMode(page);
    }

    public GraphicsImage getImage(UUID uuid) {
        for (GraphicsImage image : page.images)
            if (image.getUuid().equals(uuid))
                return image;
        ALog.e(TAG, "getImage(): Image does not exists");
        return null;
    }

    public void interrupt() {
        if (page == null || canvas == null)
            return;
        ALog.d(TAG, "Interrupting current interaction");
        if (touchHandler != null)
            touchHandler.interrupt();
        /**
         * 2019.02.21
         * Marked page.draw(canvas) by Karote
         * Reason: it should not to call page to draw because this function(interrupt) is called by Activity.onPause().
         */
        // page.draw(canvas);
        invalidate();
    }

    public void setFullRefreshTime(long time) {
        ALog.e(TAG, "HWV<-----setFullRefreshTime time= " + time);
        full_refresh_time = time;
        // setToolboxVisible(false);
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (full_refresh_time > 0) {
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (isFullRefreshable) {
                        doFullRefresh();
                    }
                }
            }, 0, full_refresh_time);
        }
    }

    public void setPenOffsetX(int offset) {
        ALog.e(TAG, "HWV<-----setPenOffsetX  offset= " + offset);
        pen_offset_x = offset;
        nDrawHelper.NDrawSetInputOffset(pen_offset_x, pen_offset_y);
        // setToolboxVisible(false);
    }

    public void setPenOffsetY(int offset) {
        ALog.e(TAG, "HWV<-----setPenOffsetY  offset= " + offset);
        pen_offset_y = offset;
        nDrawHelper.NDrawSetInputOffset(pen_offset_x, pen_offset_y);
        // setToolboxVisible(false);
    }

    public long getFullRefreshTime() {
        return full_refresh_time;
    }

    public int getPenOffsetX() {
        return pen_offset_x;
    }

    public int getPenOffsetY() {
        return pen_offset_y;
    }

    // public void setFirstRowToolboxVisible(boolean visible) {
    // getToolBox().setFirstRowToolboxVisible(visible);
    // }

    // public boolean getFirstRowToolboxVisible() {
    // return getToolBox().getFirstRowToolboxVisible();
    // }

    public Page getPage() {
        return page;
    }

    public Paper.Type getPagePaperType() {
        return page.paper_type;
    }

    public void setPagePaperType(Paper.Type paper_type, String paper_path) {
        ToolboxConfiguration.getInstance().setPageBackground(paper_type);
        page.setPaperType(paper_type, paper_path);
        page.draw(canvas);
        invalidate();
        // doFullRefresh(1000);
        resetNDrawUpdateMode(page);
    }

    public void setAntiColor(boolean isAntiColor) {

        is_anti_color = isAntiColor;
        if (isAntiColor) {
            page.setBackgroundColor(Color.BLACK);
            // toolbox.setPageTitleTextColor(Color.WHITE);
            nDrawHelper.NDrawSetPaintColor(Color.WHITE);
        } else {
            page.setBackgroundColor(Color.WHITE);
            // toolbox.setPageTitleTextColor(Color.BLACK);
            nDrawHelper.NDrawSetPaintColor(Color.BLACK);
        }
        if (canvas == null) {
            return;
        }
        page.draw(canvas);
        // setToolboxVisible(false);
        invalidate();
        doFullRefresh(1500);
    }

    public float getPageAspectRatio() {
        return page.aspect_ratio;
    }

    public void setPageAspectRatio(float aspect_ratio) {
        page.setAspectRatio(aspect_ratio);
        setPageAndZoomOut(page, true);
        invalidate();
        // doFullRefresh(1000);
    }

    public void setFullRefreshable(boolean isFullRefreshable) {
        this.isFullRefreshable = isFullRefreshable;
    }

    /**
     * To be called from the onResume method of the activity. Update appearance
     * according to preferences etc.
     */
    public void loadSettings(SharedPreferences settings) {

        boolean toolbox_is_visible = settings.getBoolean(KEY_TOOLBOX_IS_VISIBLE, false);

        setMoveGestureMinDistance(settings.getInt("move_gesture_min_distance", 400));

        boolean first_row_toolbox_is_visible = settings.getBoolean(KEY_FIRST_ROW_TOOLBOX_IS_VISIBLE, true);
        // getToolBox().setFirstRowToolboxVisible(first_row_toolbox_is_visible);

        long fullRefreshTime = settings.getLong(KEY_FULL_REFRESH_TIME, DEFAULT_FULL_REFRESH_TIME);
        int penOffsetX = settings.getInt(KEY_PEN_OFFSET_X, 0);
        int penOffsetY = settings.getInt(KEY_PEN_OFFSET_Y, 0);
        nDrawHelper.NDrawSetInputOffset(penOffsetX, penOffsetY);
        nDrawHelper.NDrawSetStrokeWidth(ToolboxConfiguration.getInstance().getPenThickness());  // set ndraw initial width
        nDrawHelper.NDrawSetGreyPaint(ToolboxConfiguration.getInstance().getPenColor());

        if ((int) (getScaledPenThickness()) == 0) {
            Brush_thickness = settings.getInt(BRUSH_THICKNESS, 30);
            Fountain_thickness = settings.getInt(FOUNTAIN_THICKNESS, 30);
        }

        // reset ThicknessSpinner
        //	setFullRefreshTime(fullRefreshTime);
        setPenOffsetX(penOffsetX);
        setPenOffsetY(penOffsetY);

        final boolean hwPen = Hardware.hasPenDigitizer();
        String pen_input_mode;
        if (hwPen) {
            pen_input_mode = settings.getString(KEY_LIST_PEN_INPUT_MODE, STYLUS_WITH_GESTURES);
            ALog.i(TAG, "HWV<----loadSettings = STYLUS_WITH_GESTURES");
        } else {
            pen_input_mode = STYLUS_AND_TOUCH;
            ALog.i(TAG, "HWV<----loadSettings = STYLUS_AND_TOUCH");
        }
        ALog.d(TAG, "pen input mode " + pen_input_mode);
        if (pen_input_mode.equals(STYLUS_ONLY)) {
            setOnlyPenInput(true);
            setDoubleTapWhileWriting(false);
            setMoveGestureWhileWriting(false);
            setMoveGestureFixZoom(false);
            setPalmShieldEnabled(false);
        } else if (pen_input_mode.equals(STYLUS_WITH_GESTURES)) {
            setOnlyPenInput(true);
            setDoubleTapWhileWriting(settings.getBoolean(KEY_DOUBLE_TAP_WHILE_WRITE, hwPen));
            setMoveGestureWhileWriting(settings.getBoolean(KEY_MOVE_GESTURE_WHILE_WRITING, hwPen));
            setMoveGestureFixZoom(settings.getBoolean(KEY_MOVE_GESTURE_FIX_ZOOM, false));
            setPalmShieldEnabled(false);
        } else if (pen_input_mode.equals(STYLUS_AND_TOUCH)) {
            setOnlyPenInput(false);
            setDoubleTapWhileWriting(false);
            setMoveGestureWhileWriting(false);
            setMoveGestureFixZoom(false);
            setPalmShieldEnabled(settings.getBoolean(KEY_PALM_SHIELD, false));
        } else
            Assert.fail();

        // artis: for EPD, reduce the refresh times of strokes
        String pen_smooth_filter = "";
        // final String pen_smooth_filter = settings.getString
        // (KEY_PEN_SMOOTH_FILTER, Filter.KERNEL_SAVITZKY_GOLAY_11.toString());
        // final String pen_smooth_filter = settings.getString(KEY_PEN_SMOOTH_FILTER,
        // getContext().getString(R.string.preferences_pen_smooth_default));
        pen_smooth_filter = settings.getString(KEY_PEN_SMOOTH_FILTER, Filter.KERNEL_NONE.toString());
        ALog.d(TAG, "HV<-- pen_smooth_filter = " + pen_smooth_filter);
        setPenSmoothFilter(Filter.valueOf(pen_smooth_filter));
    }

    /**
     * To be called from the onPause method of the activity. Save preferences etc.
     * Note: Settings that can only be changed in preferences need not be saved,
     * they are saved by the preferences.
     */
    public void saveSettings(SharedPreferences.Editor editor) {

        // editor.putBoolean(KEY_TOOLBOX_IS_VISIBLE, toolbox.isToolboxVisible());
        // editor.putBoolean(KEY_FIRST_ROW_TOOLBOX_IS_VISIBLE,
        // getToolBox().getFirstRowToolboxVisible());
        editor.putLong(KEY_FULL_REFRESH_TIME, getFullRefreshTime());
        editor.putInt(KEY_PEN_OFFSET_X, getPenOffsetX());
        editor.putInt(KEY_PEN_OFFSET_Y, getPenOffsetY());
        editor.putInt(BRUSH_THICKNESS, (int) (getScaledPenThickness()));
        editor.putInt(FOUNTAIN_THICKNESS, (int) (getScaledPenThickness()));
    }

    public void onPause() {
        nDrawHelper.NDrawSwitch(false);
    }

    public void setDrawRegion() {

        int curW = getWidth();
        int curH = getHeight();

        Boolean isLeft = ToolboxConfiguration.getInstance().isToolbarAtLeft();

        switch (mCurrentRotation) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:

                int landscape_left;
                int landscape_top;
                int landscape_right;
                int landscape_bottom;

                if (isLeft) {

                    landscape_left = width - curW;
                    landscape_top = getRelativeTop(HandwriterView.this);
                    landscape_right = landscape_left + curW;
                    landscape_bottom = landscape_top + curH;

                } else {

                    landscape_left = 0;
                    landscape_top = getRelativeTop(HandwriterView.this);
                    landscape_right = landscape_left + curW;
                    landscape_bottom = landscape_top + curH;

                }

                packet[0] = landscape_left;
                packet[1] = landscape_top;
                packet[2] = landscape_right;
                packet[3] = landscape_bottom;
                nDrawHelper.NDrawSetDrawRegion(packet);
                break;

        }

    }

    public void setPageAndZoomOut(Page new_page, boolean doDraw) {
        if (new_page == null)
            return;
        page = new_page;
        if (canvas == null)
            return;
        // if (getResources().getConfiguration().orientation ==
        // Configuration.ORIENTATION_LANDSCAPE) {
        zoomFitWidth();
        // } else {
        // zoomOutOverview();
        // }
//        Background.flag_zoom = true;
        if (!doDraw)
            return;

        if (FastView.loadPagePreviewBitmapSuccess) {
            page.drawNotInvalidate();
        } else {
            CallbackEvent event = new CallbackEvent();
            if (page.objectsDrawTimePredict() > PAGE_REDRAW_TIME_THRESHOLD) {
                event.setMessage(CallbackEvent.PAGE_DRAW_TASK_HEAVY);
            } else {
                event.setMessage(CallbackEvent.PAGE_DRAW_TASK_LIGHT);
            }
            mEventBus.post(event);
        }
        page.drawInBackgroundThread(canvas);

        if (!FastView.loadPagePreviewBitmapSuccess)
            invalidate();
        // doFullRefresh(1000);
    }

    public void clear() {
        ALog.e(TAG, "HWV<----clear()");
        CallbackEvent callbackEvent = new CallbackEvent();
        callbackEvent.setMessage(CallbackEvent.NOOSE_COPY_AND_DELETE_AND_CUT_BTN_GONE);
        mEventBus.post(callbackEvent);
        page.nooseArt.clear();
        graphicsListener.onPageClearListener(page);
        // setToolboxVisible(false);
        page.draw(canvas);
        invalidate();
        // doFullRefresh(1500);
    }

    public void savePagePreview() {
        if (page == null)
            return;

        String previewBitmapFileName = PAGE_PREVIEW_BITMAP_FILE_NAME_PREFIX + page.getUUID().toString() + PAGE_PREVIEW_BITMAP_FILE_TYPE;
        BookDirectory dir = Storage.getInstance().getBookDirectory(Bookshelf.getInstance().getCurrentBook().getUUID());
        File previewBitmapFile = new File(dir, previewBitmapFileName);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(previewBitmapFile));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deletePagePreview() {
        String previewBitmapFileName = PAGE_PREVIEW_BITMAP_FILE_NAME_PREFIX + page.getUUID().toString() + PAGE_PREVIEW_BITMAP_FILE_TYPE;
        BookDirectory dir = Storage.getInstance().getBookDirectory(Bookshelf.getInstance().getCurrentBook().getUUID());
        File previewBitmapFile = new File(dir, previewBitmapFileName);
        previewBitmapFile.delete();
    }

    private void setMoveGestureWhileWriting(boolean moveGestureWhileWriting) {
        this.moveGestureWhileWriting = moveGestureWhileWriting;
    }

    private void setPenSmoothFilter(Filter filter) {
        this.penSmoothFilter = filter;
        // ALog.e(TAG, "Pen smoothen filter = "+filter);
    }

    private void setOnlyPenInput(boolean onlyPenInput) {
        this.onlyPenInput = onlyPenInput;
    }

    private void setDoubleTapWhileWriting(boolean doubleTapWhileWriting) {
        this.doubleTapWhileWriting = doubleTapWhileWriting;
    }

    private void setMoveGestureFixZoom(boolean moveGestureFixZoom) {
        this.moveGestureFixZoom = moveGestureFixZoom;
    }

    private void setMoveGestureMinDistance(int moveGestureMinDistance) {
        this.moveGestureMinDistance = moveGestureMinDistance;
    }

    private void setPalmShieldEnabled(boolean enabled) {
        palmShield = enabled;
        initPalmShield();
        // invalidate();
    }

    /**
     * Control the action when pen HOVER EXIT.
     *
     * @param doNDrawSwitchOff Set to true to switch nDraw off, false to keep. The default value
     *                         is true.
     * @param doInvalidate     Set to true to do invalidate. The default value is false.
     */
    private void doHoverExitAction(boolean doNDrawSwitchOff, boolean doInvalidate) {
        this.doNDrawSwitchOff = doNDrawSwitchOff;
        this.doInvalidate = doInvalidate;
        if (Hardware.isEinkHardwareType()) {
            mHandler.removeCallbacks(runHoverExitAction);
            mHandler.postDelayed(runHoverExitAction, HOVER_EXIT_ACTION_DELAY);
        }
    }

    private float getScaledPenThickness() {
        if (ToolboxConfiguration.getInstance().getCurrentTool() == Tool.FOUNTAINPEN)
            return Stroke.getScaledPenThickness(this.page.transformation,
                    ToolboxConfiguration.getInstance().getPenThickness() + 1);
        else
            return Stroke.getScaledPenThickness(this.page.transformation,
                    ToolboxConfiguration.getInstance().getPenThickness() + 7);
    }

    private void setUpTouchHandler(@Tool int tool) {
        if (touchHandler != null) {
            /**
             * Clear noose, selected objects
             */
            if (touchHandler instanceof TouchHandlerNoose) {
                if (!page.nooseArt.isEmpty()) {
                    RectF refreshBox = page.nooseArt.get(0).getBoundingBox();
                    page.nooseArt.clear();
                    page.clearSelectedObjects();
                    refreshBox.inset(-10, -10);
                    page.draw(canvas, refreshBox);
                    CallbackEvent callbackEvent = new CallbackEvent();
                    callbackEvent.setMessage(CallbackEvent.NOOSE_COPY_AND_DELETE_AND_CUT_BTN_GONE);
                    mEventBus.post(callbackEvent);
                }
            }
            touchHandler.destroy();
            touchHandler = null;
        }
        switch (tool) {
            case Tool.FOUNTAINPEN:
                if (onlyPenInput)
                    touchHandler = new TouchHandlerActivePen(this);
                else
                    touchHandler = new TouchHandlerPassivePen(this);
                break;
            case Tool.PENCIL:
                // Daniel 20181121 : Use orig pencil touchhandler to fix the draw line storage will slows down problem on the nDraw version
                // touchHandler = new TouchHandlerActivePenFast(this);
                touchHandler = new TouchHandlerActivePen(this);
                break;
            case Tool.ARROW:
                break;
            case Tool.LINE:
                touchHandler = new TouchHandlerLine(this);
                break;
            case Tool.RECTANGLE:
                touchHandler = new TouchHandlerRectangle(this);
                break;
            case Tool.OVAL:
                touchHandler = new TouchHandlerOval(this);
                break;
            case Tool.TRIANGLE:
                touchHandler = new TouchHandlerTriangle(this);
                break;
            case Tool.MOVE:
                touchHandler = new TouchHandlerMoveZoom(this);
                break;
            case Tool.ERASER:
                touchHandler = new TouchHandlerEraser(this);
                break;
            case Tool.TEXT:
                touchHandler = new TouchHandlerText(this);
                break;
            case Tool.IMAGE:
                touchHandler = new TouchHandlerImage(this);
                break;
            case Tool.BRUSH:
                // Artis: for brush rendering touch
                touchHandler = new TouchHandlerActivePenBrush(this);
                break;
            case Tool.NOOSE:
                touchHandler = new TouchHandlerNoose(this);
                break;
            default:
                touchHandler = null;
        }
    }

    private void doFullRefresh() {
        doFullRefresh(0);
    }

    private void initPalmShield() {
        if (!palmShield)
            return;
        if (ToolboxConfiguration.getInstance().isToolbarAtLeft()) // for right-handed user
            palmShieldRect = new RectF(0, getHeight() / 2, getWidth(), getHeight());
        else // for left-handed user
            palmShieldRect = new RectF(0, 0, getWidth(), getHeight() / 2);
        palmShieldPaint = new Paint();
        palmShieldPaint.setARGB(0x22, 0, 0, 0);
    }

    private void zoomOutOverview() {
        float H = canvas.getHeight();
        float W = canvas.getWidth();
        float dimension = Math.min(H, W / page.aspect_ratio);
        float h = dimension;
        float w = dimension * page.aspect_ratio;
        if (h < H)
            page.setTransform(0, (H - h) / 2, dimension);
        else if (w < W)
            page.setTransform((W - w) / 2, 0, dimension);
        else
            page.setTransform(0, 0, dimension);
    }

    private void zoomFitWidth() {
        float H = canvas.getHeight();
        float W = canvas.getWidth();
        float dimension = W / page.aspect_ratio;
        float w = dimension * page.aspect_ratio;
        float offset_y;
        RectF r = page.getLastStrokeRect();
        if (r == null)
            offset_y = 0;
        else {
            float y_center = r.centerY() * dimension;
            float screen_h = w / W * H;
            // offset_y = screen_h/2 - y_center; // put y_center at screen center
            offset_y = 0;
            if (offset_y > 0)
                offset_y = 0;
            if (offset_y - screen_h < -dimension)
                offset_y = -dimension + screen_h;
        }
        page.setTransform(0, offset_y, dimension);
    }

    private void initPenStatus() {
        @Tool int currentTool = ToolboxConfiguration.getInstance().getCurrentTool();
        if (currentTool == Tool.FOUNTAINPEN) {
            nDrawHelper.NDrawSetPenType(Global.NDRAW_PEN_TYPE_FOUNTAINPEN);
            nDrawHelper.NDrawSetMaxStrokeWidthWhenUsingPressure((int) getScaledPenThickness());
        } else if (currentTool == Tool.BRUSH) {
            nDrawHelper.NDrawSetPenType(Global.NDRAW_PEN_TYPE_BRUSH);
            nDrawHelper.NDrawSetMaxStrokeWidthWhenUsingPressure(
                    (int) (getScaledPenThickness() * Global.BRUSH_THICKNESS_WEIGHT));
        } else if (currentTool == Tool.PENCIL) {
            nDrawHelper.NDrawSetPenType(Global.NDRAW_PEN_TYPE_PENCIL);
            nDrawHelper.NDrawSetMaxStrokeWidthWhenUsingPressure(-1);
            nDrawHelper.NDrawSetStrokeWidth(ToolboxConfiguration.getInstance().getPenThickness());
        }
    }

    private int getRelativeTop(View myView) {
        if (myView.getParent() == myView.getRootView())
            return myView.getTop();
        else
            return myView.getTop() + getRelativeTop((View) myView.getParent());
    }

    private int getRelativeLeft(View myView) {
        if (myView.getParent() == myView.getRootView())
            return myView.getLeft();
        else
            return myView.getLeft() + getRelativeLeft((View) myView.getParent());
    }

    private AlertDialog getPhotoControlDialog() {
        Builder builder = new Builder(mContext, R.style.AlertDialog_custom);

        stringsPhotoControl[0] = getResources().getString(R.string.toolbox_photo_control_remove);
        stringsPhotoControl[1] = getResources().getString(R.string.toolbox_photo_control_edit);
        stringsPhotoControl[2] = getResources().getString(R.string.toolbox_photo_control_cancel);

        builder.setItems(stringsPhotoControl, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        remove(NowEditedGraphics);
                        doFullRefresh(500);
                        resetNDrawUpdateMode(page);
                        break;
                    case 1:
                        GraphicsImage image = (GraphicsImage) NowEditedGraphics;
                        callOnEditImageListener(image);
                        break;
                    case 2:
                        break;
                }
            }
        });

        AlertDialog alertDialogObject = builder.create();
        ListView listView = alertDialogObject.getListView();
        listView.setDivider(new ColorDrawable(Color.LTGRAY)); // set color
        listView.setDividerHeight(1); // set height

        return alertDialogObject;
    }

    private AlertDialog getAddPhotoControlDialog() {
        Builder builder = new Builder(mContext, R.style.AlertDialog_custom);

        stringsAddPhotoControl[0] = getResources().getString(R.string.toolbox_photo_control_add);
        stringsAddPhotoControl[1] = getResources().getString(R.string.toolbox_photo_control_cancel);

        builder.setItems(stringsAddPhotoControl, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        if (Bookshelf.getInstance().getCurrentBook().currentPage().images.size() >= 3) {
                            mActivity.showConfirmAlertDialogFragment(
                                    mActivity.getString(R.string.msg_warning_image, 3),
                                    R.drawable.writing_ic_error,
                                    NoteWriterActivity.MAX_IMAGE_ALERT_DIALOG_TAG,
                                    false,
                                    true);
                            remove(NowEditedGraphics);
                            doFullRefresh(500);
                            break;
                        }
                        saveGraphics(NowEditedGraphics);
                        GraphicsImage image = (GraphicsImage) NowEditedGraphics;
                        callOnEditImageListener(image);
                        break;
                    case 1:
                        remove(NowEditedGraphics);
                        doFullRefresh(500);
                        break;
                }
            }
        });
        builder.setCancelable(false);

        AlertDialog alertDialogObject = builder.create();
        ListView listView = alertDialogObject.getListView();
        listView.setDivider(new ColorDrawable(Color.LTGRAY)); // set color
        listView.setDividerHeight(1); // set height

        return alertDialogObject;
    }

    private void resetNDrawUpdateMode(Page page) {
        if (page.isNDrawDuModeRequiredForGrayscale()) {
            nDrawHelper.NDrawSetUpdateMode(PenEventNTX.UPDATE_MODE_PARTIAL_DU);
        } else {
            nDrawHelper.NDrawSetUpdateMode(PenEventNTX.UPDATE_MODE_PARTIAL_A2);
        }
    }

    public void deleteNooseSelect() {
        CallbackEvent callbackEvent = new CallbackEvent();
        callbackEvent.setMessage(CallbackEvent.NOOSE_COPY_AND_DELETE_AND_CUT_BTN_GONE);
        mEventBus.post(callbackEvent);
        page.nooseArt.clear();
        LinkedList<Graphics> toRemove = new LinkedList<Graphics>();
        for (Graphics g : page.get_mSelectedObjects().strokes) {
            toRemove.add(g);
        }
        for (Graphics g : page.get_mSelectedObjects().lineArt) {
            toRemove.add(g);
        }
        for (Graphics g : page.get_mSelectedObjects().rectangleArt) {
            toRemove.add(g);
        }
        for (Graphics g : page.get_mSelectedObjects().ovalArt) {
            toRemove.add(g);
        }
        for (Graphics g : page.get_mSelectedObjects().triangleArt) {
            toRemove.add(g);
        }
        if (toRemove.size() > 0)
            graphicsListener.onGraphicsListEraseListener(page, toRemove);
        doFullRefresh(500);
    }

    public void copyNooseSelect() {
        CallbackEvent callbackEvent = new CallbackEvent();
        callbackEvent.setMessage(CallbackEvent.NOOSE_PASTE_BTN_VISIBLE);
        mEventBus.post(callbackEvent);

        toCopy = new LinkedList<Graphics>();

        for (Graphics g : page.get_mSelectedObjects().strokes) {
            Stroke stroke = new Stroke((Stroke) g);
            toCopy.add(stroke);
            toCopy.getLast().backup();
        }
        for (Graphics g : page.get_mSelectedObjects().lineArt) {
            GraphicsLine graphicsLine = new GraphicsLine((GraphicsLine) g);
            toCopy.add(graphicsLine);
            toCopy.getLast().backup();
        }
        for (Graphics g : page.get_mSelectedObjects().rectangleArt) {
            GraphicsRectangle graphicsRectangle = new GraphicsRectangle((GraphicsRectangle) g);
            toCopy.add(graphicsRectangle);
            toCopy.getLast().backup();
        }
        for (Graphics g : page.get_mSelectedObjects().ovalArt) {
            GraphicsOval graphicsOval = new GraphicsOval((GraphicsOval) g);
            toCopy.add(graphicsOval);
            toCopy.getLast().backup();
        }
        for (Graphics g : page.get_mSelectedObjects().triangleArt) {
            GraphicsTriangle graphicsTriangle = new GraphicsTriangle((GraphicsTriangle) g);
            toCopy.add(graphicsTriangle);
            toCopy.getLast().backup();
        }
    }

    public void pasteNooseSelect() {
        page.setModified(true);
        CallbackEvent callbackEvent = new CallbackEvent();
        callbackEvent.setMessage(CallbackEvent.NOOSE_PASTE_BTN_GONE);
        mEventBus.post(callbackEvent);
        graphicsListener.onGraphicsListCopyListener(page, toCopy);
        RectF newRange = page.getSelectedCopyObjects(toCopy, canvas);
        ((GraphicsNoose) NowEditedGraphics).reSetRang(newRange);
        page.nooseArt.clear();
        page.nooseArt.addFirst(((GraphicsNoose) NowEditedGraphics));
        page.draw(canvas, NowEditedGraphics.getBoundingBox());
        doFullRefresh(500);
    }
}
