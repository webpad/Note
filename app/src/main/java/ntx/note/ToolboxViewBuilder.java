package ntx.note;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import name.vbraun.view.write.Graphics.Tool;
import ntx.draw.nDrawHelper;
import ntx.note.RelativePopupWindow.HorizontalPosition;
import ntx.note.RelativePopupWindow.VerticalPosition;
import ntx.note2.R;
import utility.ToggleImageButton;

public class ToolboxViewBuilder extends RelativeLayout implements View.OnClickListener, View.OnTouchListener {
    private Activity mContext;

    private boolean isRightSideLayout = false;

    private LinearLayout mLayoutRoot;

    private FrameLayout mBtnPenStyle;
    private ImageView mIvPenStyle;
    private TextView mTvPenThickness;

    private ImageButton mBtnNoose;
    private ImageButton mBtnPageBackground;
    private ImageButton mBtnEraserLine;
    private ImageButton mBtnEraserAll;

    private ImageButton mBtnZoom;
    private ImageButton mBtnPlugInImage;

    private ToggleImageButton mBtnMark;
    private ImageButton mBtnTag;
    private ImageButton mBtnSearch;
    private ImageButton mBtnSetting;
    private ImageButton mBtnSave;
    private ImageButton mBtnNooseDelete;
    private ImageButton mBtnNooseCopy;
    private ImageButton mBtnNoosePaste;
    private ImageButton mBtnNooseCut;
    private View mDivide1;
    private View mDivide2;
    private View mDivide3;
    private View mDivide4;

    private ImageButton mBtnExpand;

    private PenStylePopupWindow mPenStylePopupWindow;
    private PageBackgroundPopupWindow mPageBackgroundPopupWindow;
    private SettingsPopupWindow mSettingsPopupWindow;

    private ToolboxConfiguration mToolboxConfiguration;
    private EventBus mEventBus;
    private ArrayList close_nDraw_layout = new ArrayList();

    public ToolboxViewBuilder(Activity ctx, int layoutId) {
        super(ctx);

        mToolboxConfiguration = ToolboxConfiguration.getInstance();

        this.mContext = ctx;

        isRightSideLayout = layoutId != R.layout.toolbox_left;

        View.inflate(ctx, layoutId, this);
        initViews();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mEventBus = EventBus.getDefault();
        mEventBus.register(this);
        setIconSelect(mToolboxConfiguration.getCurrentToolViewId());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mEventBus.unregister(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        //Prevent nDraw open
        if (close_nDraw_layout.contains(v.getId()) && event.getAction() == MotionEvent.ACTION_DOWN) {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ToolboxConfiguration event) {
        mIvPenStyle.setImageLevel(event.getPenStyle());
        mTvPenThickness.setText(String.valueOf(event.getPenThickness()));
        if (event.isPageCheckedQuickTag())
            mBtnMark.setChecked();
        else
            mBtnMark.setUnchecked();
        setIconSelect(event.getCurrentToolViewId());
        updateButtonVisibility(event.isToolbarExpand());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CallbackEvent event) {
        if (event.getMessage().equals(CallbackEvent.DISMISS_POPUPWINDOW)) {
            if (mPenStylePopupWindow != null && mPenStylePopupWindow.isShowing())
                mPenStylePopupWindow.dismiss();
            if (mPageBackgroundPopupWindow != null && mPageBackgroundPopupWindow.isShowing())
                mPageBackgroundPopupWindow.dismiss();
            if (mSettingsPopupWindow != null && mSettingsPopupWindow.isShowing())
                mSettingsPopupWindow.dismiss();
        } else if (event.getMessage().equals(CallbackEvent.NOOSE_COPY_AND_DELETE_AND_CUT_BTN_VISIBLE)) {
            mBtnNooseDelete.setVisibility(VISIBLE);
            mBtnNooseCopy.setVisibility(VISIBLE);
            mBtnNooseCut.setVisibility(VISIBLE);
        } else if (event.getMessage().equals(CallbackEvent.NOOSE_COPY_AND_DELETE_AND_CUT_BTN_GONE)) {
            mBtnNooseDelete.setVisibility(GONE);
            mBtnNooseCopy.setVisibility(GONE);
            mBtnNooseCut.setVisibility(GONE);
        } else if (event.getMessage().equals(CallbackEvent.NOOSE_PASTE_BTN_VISIBLE)) {
            mBtnNoosePaste.setVisibility(VISIBLE);
        } else if (event.getMessage().equals(CallbackEvent.NOOSE_PASTE_BTN_GONE)) {
            mBtnNoosePaste.setVisibility(GONE);
            mBtnNooseDelete.setVisibility(VISIBLE);
            mBtnNooseCopy.setVisibility(VISIBLE);
            mBtnNooseCut.setVisibility(VISIBLE);
        } else if (event.getMessage().equals(CallbackEvent.NOOSE_ALL_BTN_GONE)) {
            mBtnNoosePaste.setVisibility(GONE);
            mBtnNooseDelete.setVisibility(GONE);
            mBtnNooseCopy.setVisibility(GONE);
            mBtnNooseCut.setVisibility(GONE);
        }
    }

    //Click don't use switch nDraw, because touch event has better speed for close nDraw
    @Override
    public void onClick(View view) {
        CallbackEvent callbackEvent;
        callbackEvent = new CallbackEvent();
        callbackEvent.setMessage(CallbackEvent.DO_DRAW_VIEW_INVALIDATE);
        mEventBus.post(callbackEvent);
        switch (view.getId()) {
            case R.id.btn_toolbox_pen_style:
                showPenStylePopupWindow();
                break;
            case R.id.btn_toolbox_noose:
                mToolboxConfiguration.setCurrentTool(Tool.NOOSE);
                break;
            case R.id.btn_toolbox_page_background:
                showPageBackgroundPopupWindow();
                break;
            case R.id.btn_toolbox_eraser_line:
                mToolboxConfiguration.setCurrentTool(Tool.ERASER);
                // TODO
                // mHandwriterView.doFullRefresh(200);
                break;
            case R.id.btn_toolbox_eraser_all:
                callbackEvent.setMessage(CallbackEvent.SHOW_CLEAN_DIALOG);
                mEventBus.post(callbackEvent);
                break;
            case R.id.btn_toolbox_zoom:
                break;
            case R.id.btn_toolbox_plugin_image:
                mToolboxConfiguration.setCurrentTool(Tool.IMAGE);
                break;
            case R.id.btn_toolbox_tag:
                callbackEvent.setMessage(CallbackEvent.PAGE_TAG_SETTING);
                mEventBus.post(callbackEvent);
                break;
            case R.id.btn_toolbox_search:
                callbackEvent.setMessage(CallbackEvent.SEARCH_NOTE);
                mEventBus.post(callbackEvent);
                break;
            case R.id.btn_toolbox_setting:
//                showSettingsPopupWindow();
                callbackEvent.setMessage(CallbackEvent.SHOW_SETTING_CALIBRATION_DIALOG);
                mEventBus.post(callbackEvent);
                break;
            case R.id.btn_toolbox_save:
                callbackEvent.setMessage(CallbackEvent.SAVE_NOTE);
                mEventBus.post(callbackEvent);
                break;
            case R.id.btn_noose_delete:
                callbackEvent.setMessage(CallbackEvent.NOOSE_DELETE);
                mEventBus.post(callbackEvent);
                break;
            case R.id.btn_noose_copy:
                callbackEvent.setMessage(CallbackEvent.NOOSE_COPY);
                mEventBus.post(callbackEvent);
                break;
            case R.id.btn_noose_paste:
                callbackEvent.setMessage(CallbackEvent.NOOSE_PASTE);
                mEventBus.post(callbackEvent);
                break;
            case R.id.btn_noose_cut:
                callbackEvent.setMessage(CallbackEvent.NOOSE_CUT);
                mEventBus.post(callbackEvent);
                break;
            default:
                break;
        }

        if (mToolboxConfiguration.getCurrentTool() != Tool.NOOSE) {
            callbackEvent.setMessage(CallbackEvent.NOOSE_ALL_BTN_GONE);
            mEventBus.post(callbackEvent);
        }

        setIconSelect(view.getId());
    }

    private void initViews() {
        mLayoutRoot = (LinearLayout) findViewById(R.id.ll_toolbox_vertical_root);
        mLayoutRoot.setOnTouchListener(this);
        mBtnPenStyle = (FrameLayout) findViewById(R.id.btn_toolbox_pen_style);
        mIvPenStyle = (ImageView) findViewById(R.id.iv_pen_style);
        mTvPenThickness = (TextView) findViewById(R.id.tv_pen_thickness);
        mBtnPlugInImage = (ImageButton) findViewById(R.id.btn_toolbox_plugin_image);

        mBtnNoose = (ImageButton) findViewById(R.id.btn_toolbox_noose);
        mBtnEraserLine = (ImageButton) findViewById(R.id.btn_toolbox_eraser_line);
        mBtnEraserAll = (ImageButton) findViewById(R.id.btn_toolbox_eraser_all);

        mBtnPageBackground = (ImageButton) findViewById(R.id.btn_toolbox_page_background);
        mBtnZoom = (ImageButton) findViewById(R.id.btn_toolbox_zoom);

        mBtnMark = (ToggleImageButton) findViewById(R.id.btn_toolbox_mark);
        mBtnTag = (ImageButton) findViewById(R.id.btn_toolbox_tag);
        mBtnSearch = (ImageButton) findViewById(R.id.btn_toolbox_search);
        mBtnSetting = (ImageButton) findViewById(R.id.btn_toolbox_setting);
        mBtnSave = (ImageButton) findViewById(R.id.btn_toolbox_save);

        mBtnExpand = (ImageButton) findViewById(R.id.btn_toolbox_expand);
        mBtnNooseDelete = (ImageButton) findViewById(R.id.btn_noose_delete);
        mBtnNooseCopy = (ImageButton) findViewById(R.id.btn_noose_copy);
        mBtnNoosePaste = (ImageButton) findViewById(R.id.btn_noose_paste);
        mBtnNooseCut = (ImageButton) findViewById(R.id.btn_noose_cut);
        mDivide1 = findViewById(R.id.view_divide1);
        mDivide2 = findViewById(R.id.view_divide2);
        mDivide3 = findViewById(R.id.view_divide3);
        mDivide4 = findViewById(R.id.view_divide4);

        mBtnPenStyle.setTag(new ButtonTag(true, true));
        mBtnPlugInImage.setTag(new ButtonTag(true, true));

        mBtnNoose.setTag(new ButtonTag(true, true));
        mBtnEraserLine.setTag(new ButtonTag(true, true));
        mBtnEraserAll.setTag(new ButtonTag(false, false));

        mBtnPageBackground.setTag(new ButtonTag(true, false));
        mBtnZoom.setTag(new ButtonTag(false, false));

        mBtnMark.setTag(new ButtonTag(false, false));
        mBtnTag.setTag(new ButtonTag(false, false));
        mBtnSearch.setTag(new ButtonTag(false, false));
        mBtnSetting.setTag(new ButtonTag(false, false));
        mBtnSave.setTag(new ButtonTag(false, false));
        mBtnNooseDelete.setTag(new ButtonTag(false, false));
        mBtnNooseCopy.setTag(new ButtonTag(false, false));
        mBtnNoosePaste.setTag(new ButtonTag(false, false));
        mBtnNooseCut.setTag(new ButtonTag(false, false));
        mBtnPenStyle.setOnClickListener(this);
        mBtnPenStyle.setOnTouchListener(this);

        mBtnNoose.setOnClickListener(this);
        mBtnNoose.setOnTouchListener(this);
        mBtnPageBackground.setOnClickListener(this);
        mBtnPageBackground.setOnTouchListener(this);
        mBtnEraserLine.setOnClickListener(this);
        mBtnEraserLine.setOnTouchListener(this);
        mBtnEraserAll.setOnClickListener(this);
        mBtnEraserAll.setOnTouchListener(this);

        mBtnZoom.setOnClickListener(this);
        mBtnZoom.setOnTouchListener(this);
        mBtnPlugInImage.setOnClickListener(this);
        mBtnPlugInImage.setOnTouchListener(this);

        mBtnMark.setOnCheckedChangeListener(markBtnCheckedChangeListener);
        mBtnMark.setOnTouchListener(this);
        mBtnTag.setOnClickListener(this);
        mBtnTag.setOnTouchListener(this);
        mBtnSearch.setOnClickListener(this);
        mBtnSearch.setOnTouchListener(this);
        mBtnSetting.setOnClickListener(this);
        mBtnSetting.setOnTouchListener(this);
        mBtnSave.setOnClickListener(this);
        mBtnSave.setOnTouchListener(this);
        mBtnNooseDelete.setOnClickListener(this);
        mBtnNooseDelete.setOnTouchListener(this);
        mBtnNooseCopy.setOnClickListener(this);
        mBtnNooseCopy.setOnTouchListener(this);
        mBtnNoosePaste.setOnClickListener(this);
        mBtnNoosePaste.setOnTouchListener(this);
        mBtnNooseCut.setOnClickListener(this);
        mBtnNooseCut.setOnTouchListener(this);
        mBtnExpand.setOnClickListener(onBtnExpandClickListener);
        mBtnExpand.setOnTouchListener(this);

        updateButtonVisibility(mToolboxConfiguration.isToolbarExpand());
        close_nDraw_layout.add(R.id.btn_toolbox_pen_style);
        close_nDraw_layout.add(R.id.btn_toolbox_noose);
        close_nDraw_layout.add(R.id.btn_toolbox_page_background);
        close_nDraw_layout.add(R.id.btn_toolbox_eraser_line);
        close_nDraw_layout.add(R.id.btn_toolbox_eraser_all);
        close_nDraw_layout.add(R.id.btn_toolbox_zoom);
        close_nDraw_layout.add(R.id.btn_toolbox_plugin_image);
        close_nDraw_layout.add(R.id.btn_toolbox_tag);
        close_nDraw_layout.add(R.id.btn_toolbox_search);
        close_nDraw_layout.add(R.id.btn_toolbox_setting);
        close_nDraw_layout.add(R.id.btn_toolbox_save);
        close_nDraw_layout.add(R.id.btn_toolbox_expand);
        close_nDraw_layout.add(R.id.btn_toolbox_mark);
        close_nDraw_layout.add(R.id.ll_toolbox_vertical_root);
    }

    private ToggleImageButton.OnCheckedChangedListener markBtnCheckedChangeListener = new ToggleImageButton.OnCheckedChangedListener() {
        @Override
        public void onCheckedChange(boolean value) {
            CallbackEvent callbackEvent = new CallbackEvent();
            if (value)
                callbackEvent.setMessage(CallbackEvent.PAGE_ADD_QUICK_TAG);
            else
                callbackEvent.setMessage(CallbackEvent.PAGE_REMOVE_QUICK_TAG);

            mEventBus.post(callbackEvent);
        }
    };

    private OnClickListener onBtnExpandClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean isExpand = mToolboxConfiguration.isToolbarExpand();
            mToolboxConfiguration.setToolbarExpand(!isExpand);
            CallbackEvent callbackEvent = new CallbackEvent();
            callbackEvent.setMessage(CallbackEvent.DO_DRAW_VIEW_INVALIDATE);
            mEventBus.post(callbackEvent);
        }
    };

    private void updateButtonVisibility(boolean isExpand) {
        if (isExpand) {
            mBtnNoose.setVisibility(View.VISIBLE);
            mBtnPageBackground.setVisibility(View.VISIBLE);
            // mBtnZoom.setVisibility(View.VISIBLE);
            mBtnPlugInImage.setVisibility(View.VISIBLE);
            mBtnMark.setVisibility(View.VISIBLE);
//            mBtnTag.setVisibility(View.VISIBLE);
            mBtnSearch.setVisibility(View.VISIBLE);
            mBtnSave.setVisibility(View.VISIBLE);
            mDivide1.setVisibility(View.VISIBLE);
            mDivide2.setVisibility(View.VISIBLE);
            mDivide3.setVisibility(View.VISIBLE);
            mDivide4.setVisibility(View.VISIBLE);

            mBtnExpand.setBackgroundResource(R.drawable.writing_toolbar_close);
        } else {
            mBtnNoose.setVisibility(View.GONE);
            mBtnPageBackground.setVisibility(View.GONE);
            // mBtnZoom.setVisibility(View.GONE);
            mBtnPlugInImage.setVisibility(View.GONE);
            mBtnMark.setVisibility(View.GONE);
//            mBtnTag.setVisibility(View.GONE);
            mBtnSearch.setVisibility(View.GONE);
            mBtnSave.setVisibility(View.GONE);
            mDivide1.setVisibility(View.GONE);
            mDivide2.setVisibility(View.GONE);
            mDivide3.setVisibility(View.GONE);
            mDivide4.setVisibility(View.GONE);

            mBtnExpand.setBackgroundResource(R.drawable.writing_toolbar_expand);
        }
    }

    private void setIconSelect(int viewId) {
        if (findViewById(viewId).isSelected())
            return;

        ButtonTag tag = (ButtonTag) findViewById(viewId).getTag();

        if (tag.isSelectable) {
            for (int i = 0; i < mLayoutRoot.getChildCount(); i++) {
                View view = mLayoutRoot.getChildAt(i);
                if (view instanceof ImageButton) {
                    view.setSelected(false);
                } else if (view instanceof FrameLayout) {
                    view.setSelected(false);
                    findViewById(R.id.iv_pen_style).setSelected(false);
                }
            }
            if (viewId == R.id.btn_toolbox_pen_style) {
                findViewById(R.id.iv_pen_style).setSelected(true);
            }

            findViewById(viewId).setSelected(true);
            mToolboxConfiguration.setCurrentToolViewId(viewId, tag.isKeepSelected);
        }
    }

    private void showPenStylePopupWindow() {
        if (mToolboxConfiguration.getCurrentTool() != Tool.PENCIL
                && mToolboxConfiguration.getCurrentTool() != Tool.FOUNTAINPEN
                && mToolboxConfiguration.getCurrentTool() != Tool.BRUSH
                && mToolboxConfiguration.getCurrentTool() != Tool.LINE
                && mToolboxConfiguration.getCurrentTool() != Tool.RECTANGLE
                && mToolboxConfiguration.getCurrentTool() != Tool.OVAL
                && mToolboxConfiguration.getCurrentTool() != Tool.TRIANGLE) {
            mToolboxConfiguration.setPenStyle(mToolboxConfiguration.getPenStyle());
            return;
        }

        if (mToolboxConfiguration.getKeepSelectedToolViewId() != R.id.btn_toolbox_pen_style) {
            mToolboxConfiguration.setKeepSelectedToolViewId(R.id.btn_toolbox_pen_style);
            mToolboxConfiguration.setPenStyle(mToolboxConfiguration.getPenStyle());
        }

        mPenStylePopupWindow = PenStylePopupWindow.getInstance(mContext);
        mPenStylePopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                setIconSelect(mToolboxConfiguration.getKeepSelectedToolViewId());
                nDrawHelper.NDrawSwitch(false);
            }
        });

        if (isRightSideLayout)
            mPenStylePopupWindow.showOnAnchor(mBtnPenStyle, HorizontalPosition.LEFT, VerticalPosition.ALIGN_TOP, 15, 0, 0, 15);
        else
            mPenStylePopupWindow.showOnAnchor(mBtnPenStyle, HorizontalPosition.RIGHT, VerticalPosition.ALIGN_TOP, 0, 0, 15, 15);
    }

    private void showPageBackgroundPopupWindow() {
        mPageBackgroundPopupWindow = PageBackgroundPopupWindow.getInstance(mContext);
        mPageBackgroundPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                setIconSelect(mToolboxConfiguration.getKeepSelectedToolViewId());
            }
        });

        if (isRightSideLayout)
            mPageBackgroundPopupWindow.showOnAnchor(mBtnPageBackground, HorizontalPosition.LEFT,
                    VerticalPosition.CENTER, 15, 15, 0, 15);
        else
            mPageBackgroundPopupWindow.showOnAnchor(mBtnPageBackground, HorizontalPosition.RIGHT,
                    VerticalPosition.CENTER, 0, 15, 15, 15);
    }

    private void showSettingsPopupWindow() {
        mSettingsPopupWindow = SettingsPopupWindow.getInstance(mContext);
        mSettingsPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                setIconSelect(mToolboxConfiguration.getKeepSelectedToolViewId());
            }
        });

        if (isRightSideLayout)
            mSettingsPopupWindow.showOnAnchor(mBtnSetting, HorizontalPosition.LEFT, VerticalPosition.ALIGN_TOP, 15, 15, 0, 15);
        else
            mSettingsPopupWindow.showOnAnchor(mBtnSetting, HorizontalPosition.RIGHT, VerticalPosition.ALIGN_TOP, 0, 15, 15, 15);
    }

    private class ButtonTag {
        public boolean isSelectable;
        public boolean isKeepSelected;

        public ButtonTag(boolean isSelectable, boolean isKeepSelected) {
            this.isSelectable = isSelectable;
            this.isKeepSelected = isKeepSelected;
        }
    }
}
