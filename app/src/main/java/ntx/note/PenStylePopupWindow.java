package ntx.note;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import ntx.draw.nDrawHelper;
import ntx.note2.R;

public class PenStylePopupWindow extends RelativePopupWindow {
    private static PenStylePopupWindow mInstance;
    private ToolboxConfiguration mToolboxConfiguration;

    private ImageButton mBtnPencil;
    private ImageButton mBtnFountainPen;
    private ImageButton mBtnBrush;

    private ImageButton mBtnShapeLine;
    private ImageButton mBtnShapeRectangle;
    private ImageButton mBtnShapeCircle;
    private ImageButton mBtnShapeTriangle;

    private ImageButton mBtnColorBlack;
    private ImageButton mBtnColorDarkGray;
    private ImageButton mBtnColorGray;
    private ImageButton mBtnColorLightGray;
    private ImageButton mBtnColorWhite;

    private ImageButton mBtnThicknessLv1;
    private ImageButton mBtnThicknessLv2;
    private ImageButton mBtnThicknessLv3;
    private ImageButton mBtnThicknessLv4;
    private ImageButton mBtnThicknessLv5;
    private ImageView mIvThickness;
    private TextView mTvThickness;
    private SeekBar mSeekBarThickness;

    public static PenStylePopupWindow getInstance(Activity ctx) {
        synchronized (PenStylePopupWindow.class) {
            if (mInstance == null) {
                mInstance = new PenStylePopupWindow(ctx);
            }
            return mInstance;
        }
    }

    private PenStylePopupWindow(Activity ctx) {
        super(ctx);
        setContentView(LayoutInflater.from(ctx).inflate(R.layout.popupwindow_pen_style, null));
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setOutsideTouchable(false);
        setFocusable(false);
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View popupView = getContentView();

        mToolboxConfiguration = ToolboxConfiguration.getInstance();

        initView(popupView);
    }


    private void initView(View v) {
        mBtnPencil = (ImageButton) v.findViewById(R.id.btn_pencil);
        mBtnFountainPen = (ImageButton) v.findViewById(R.id.btn_fountainpen);
        mBtnBrush = (ImageButton) v.findViewById(R.id.btn_brush);

        mBtnShapeLine = (ImageButton) v.findViewById(R.id.btn_shape_line);
        mBtnShapeRectangle = (ImageButton) v.findViewById(R.id.btn_shape_rectangle);
        mBtnShapeCircle = (ImageButton) v.findViewById(R.id.btn_shape_circle);
        mBtnShapeTriangle = (ImageButton) v.findViewById(R.id.btn_shape_triangle);

        mBtnColorBlack = (ImageButton) v.findViewById(R.id.btn_pen_color_black);
        mBtnColorDarkGray = (ImageButton) v.findViewById(R.id.btn_pen_color_dark_gray);
        mBtnColorGray = (ImageButton) v.findViewById(R.id.btn_pen_color_gray);
        mBtnColorLightGray = (ImageButton) v.findViewById(R.id.btn_pen_color_light_gray);
        mBtnColorWhite = (ImageButton) v.findViewById(R.id.btn_pen_color_white);

        mBtnThicknessLv1 = (ImageButton) v.findViewById(R.id.btn_thickness_lv1);
        mBtnThicknessLv2 = (ImageButton) v.findViewById(R.id.btn_thickness_lv2);
        mBtnThicknessLv3 = (ImageButton) v.findViewById(R.id.btn_thickness_lv3);
        mBtnThicknessLv4 = (ImageButton) v.findViewById(R.id.btn_thickness_lv4);
        mBtnThicknessLv5 = (ImageButton) v.findViewById(R.id.btn_thickness_lv5);

        ImageButton btnThicknessDown = (ImageButton) v.findViewById(R.id.btn_thickness_down);
        ImageButton btnThicknessUp = (ImageButton) v.findViewById(R.id.btn_thickness_up);
        mSeekBarThickness = (SeekBar) v.findViewById(R.id.seekBar_thickness);
        Button btnClose = (Button) v.findViewById(R.id.btn_thickness_close);

        mIvThickness = (ImageView) v.findViewById(R.id.iv_thickness);
        mTvThickness = (TextView) v.findViewById(R.id.tv_thickness);

        mBtnPencil.setOnClickListener(onBtnClickListener);
        mBtnFountainPen.setOnClickListener(onBtnClickListener);
        mBtnBrush.setOnClickListener(onBtnClickListener);

        mBtnShapeLine.setOnClickListener(onBtnClickListener);
        mBtnShapeRectangle.setOnClickListener(onBtnClickListener);
        mBtnShapeCircle.setOnClickListener(onBtnClickListener);
        mBtnShapeTriangle.setOnClickListener(onBtnClickListener);

        mBtnColorBlack.setOnClickListener(onBtnClickListener);
        mBtnColorDarkGray.setOnClickListener(onBtnClickListener);
        mBtnColorGray.setOnClickListener(onBtnClickListener);
        mBtnColorLightGray.setOnClickListener(onBtnClickListener);
        mBtnColorWhite.setOnClickListener(onBtnClickListener);

        mBtnThicknessLv1.setOnClickListener(onBtnClickListener);
        mBtnThicknessLv2.setOnClickListener(onBtnClickListener);
        mBtnThicknessLv3.setOnClickListener(onBtnClickListener);
        mBtnThicknessLv4.setOnClickListener(onBtnClickListener);
        mBtnThicknessLv5.setOnClickListener(onBtnClickListener);
        btnThicknessDown.setOnClickListener(onBtnClickListener);
        btnThicknessUp.setOnClickListener(onBtnClickListener);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mSeekBarThickness.setMax(Global.THICKNESS_VALUE_MAX - Global.THICKNESS_VALUE_MIN);
        mSeekBarThickness.setOnSeekBarChangeListener(onThicknessSeekBarChangeListener);

        setPenStyleValue(mToolboxConfiguration.getPenStyle());
        setPenColorValue(mToolboxConfiguration.getPenColor());
        setThicknessValue(mToolboxConfiguration.getPenThickness());
        mSeekBarThickness.setProgress(mToolboxConfiguration.getPenThickness() - Global.THICKNESS_VALUE_MIN);
    }

    private View.OnClickListener onBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int thicknessValue = mToolboxConfiguration.getPenThickness();
            int penStyle = mToolboxConfiguration.getPenStyle();
            int penColor = mToolboxConfiguration.getPenColor();
            switch (v.getId()) {
                case R.id.btn_pencil:
                    penStyle = ToolboxConfiguration.PenStyle.PENCIL;
                    break;
                case R.id.btn_fountainpen:
                    penStyle = ToolboxConfiguration.PenStyle.FOUNTAINPEN;
                    break;
                case R.id.btn_brush:
                    penStyle = ToolboxConfiguration.PenStyle.BRUSH;
                    break;

                /**
                 * shape
                 */
                case R.id.btn_shape_line:
                    penStyle = ToolboxConfiguration.PenStyle.LINE;
                    break;
                case R.id.btn_shape_rectangle:
                    penStyle = ToolboxConfiguration.PenStyle.RECTANGLE;
                    break;
                case R.id.btn_shape_circle:
                    penStyle = ToolboxConfiguration.PenStyle.OVAL;
                    break;
                case R.id.btn_shape_triangle:
                    penStyle = ToolboxConfiguration.PenStyle.TRIANGLE;
                    break;

                /**
                 * color
                 */
                case R.id.btn_pen_color_black:
                    penColor = ToolboxConfiguration.PenColor.BLACK;
                    break;
                case R.id.btn_pen_color_dark_gray:
                    penColor = ToolboxConfiguration.PenColor.DARK_GRAY;
                    break;
                case R.id.btn_pen_color_gray:
                    penColor = ToolboxConfiguration.PenColor.GRAY;
                    break;
                case R.id.btn_pen_color_light_gray:
                    penColor = ToolboxConfiguration.PenColor.LIGHT_GRAY;
                    break;
                case R.id.btn_pen_color_white:
                    penColor = ToolboxConfiguration.PenColor.WHITE;
//                    nDrawHelper.NDrawSetPaintColor(Color.WHITE);
                    break;

                /**
                 * thickness
                 */
                case R.id.btn_thickness_lv1:
                    thicknessValue = Global.THICKNESS_VALUE_LV1;
                    break;
                case R.id.btn_thickness_lv2:
                    thicknessValue = Global.THICKNESS_VALUE_LV2;
                    break;
                case R.id.btn_thickness_lv3:
                    thicknessValue = Global.THICKNESS_VALUE_LV3;
                    break;
                case R.id.btn_thickness_lv4:
                    thicknessValue = Global.THICKNESS_VALUE_LV4;
                    break;
                case R.id.btn_thickness_lv5:
                    thicknessValue = Global.THICKNESS_VALUE_LV5;
                    break;
                case R.id.btn_thickness_down:
                    thicknessValue--;
                    thicknessValue = thicknessValue < Global.THICKNESS_VALUE_MIN ? Global.THICKNESS_VALUE_MIN
                            : thicknessValue;
                    break;
                case R.id.btn_thickness_up:
                    thicknessValue++;
                    thicknessValue = thicknessValue > Global.THICKNESS_VALUE_MAX ? Global.THICKNESS_VALUE_MAX
                            : thicknessValue;
                    break;
            }
            setPenStyleValue(penStyle);
            setPenColorValue(penColor);
            mSeekBarThickness.setProgress(thicknessValue - Global.THICKNESS_VALUE_MIN);
            setThicknessValue(thicknessValue);
            nDrawHelper.NDrawSetGreyPaint(penColor);
        }
    };

    private OnSeekBarChangeListener onThicknessSeekBarChangeListener = new OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser)
                setThicknessValue(progress + Global.THICKNESS_VALUE_MIN);
        }
    };

    private void setPenStyleValue(int style) {
        mBtnPencil.setSelected(false);
        mBtnFountainPen.setSelected(false);
        mBtnBrush.setSelected(false);
        mBtnShapeLine.setSelected(false);
        mBtnShapeRectangle.setSelected(false);
        mBtnShapeCircle.setSelected(false);
        mBtnShapeTriangle.setSelected(false);

        int imageLevelBase = 3;
        switch (style) {
            case ToolboxConfiguration.PenStyle.PENCIL:
                mBtnPencil.setSelected(true);
                imageLevelBase = 0;
                break;
            case ToolboxConfiguration.PenStyle.FOUNTAINPEN:
                mBtnFountainPen.setSelected(true);
                imageLevelBase = 1;
                break;
            case ToolboxConfiguration.PenStyle.BRUSH:
                mBtnBrush.setSelected(true);
                imageLevelBase = 2;
                break;
            case ToolboxConfiguration.PenStyle.LINE:
                mBtnShapeLine.setSelected(true);
                imageLevelBase = 3;
                break;
            case ToolboxConfiguration.PenStyle.RECTANGLE:
                mBtnShapeRectangle.setSelected(true);
                imageLevelBase = 3;
                break;
            case ToolboxConfiguration.PenStyle.OVAL:
                mBtnShapeCircle.setSelected(true);
                imageLevelBase = 3;
                break;
            case ToolboxConfiguration.PenStyle.TRIANGLE:
                mBtnShapeTriangle.setSelected(true);
                imageLevelBase = 3;
                break;
            default:
                break;
        }

        mToolboxConfiguration.setPenStyle(style);
        int imageLevel = mToolboxConfiguration.getPenThickness() + imageLevelBase * 15;
        mIvThickness.setImageLevel(imageLevel);
    }

    private void setPenColorValue(int color) {
        mBtnColorBlack.setSelected(false);
        mBtnColorDarkGray.setSelected(false);
        mBtnColorGray.setSelected(false);
        mBtnColorLightGray.setSelected(false);
        mBtnColorWhite.setSelected(false);

        switch (color) {
            case ToolboxConfiguration.PenColor.BLACK:
                mBtnColorBlack.setSelected(true);
                break;
            case ToolboxConfiguration.PenColor.DARK_GRAY:
                mBtnColorDarkGray.setSelected(true);
                break;
            case ToolboxConfiguration.PenColor.GRAY:
                mBtnColorGray.setSelected(true);
                break;
            case ToolboxConfiguration.PenColor.LIGHT_GRAY:
                mBtnColorLightGray.setSelected(true);
                break;
            case ToolboxConfiguration.PenColor.WHITE:
                mBtnColorWhite.setSelected(true);
                break;
        }

        mToolboxConfiguration.setPenColor(color);
    }

    private void setThicknessValue(int value) {
        mBtnThicknessLv1.setSelected(false);
        mBtnThicknessLv2.setSelected(false);
        mBtnThicknessLv3.setSelected(false);
        mBtnThicknessLv4.setSelected(false);
        mBtnThicknessLv5.setSelected(false);
        switch (value) {
            case Global.THICKNESS_VALUE_LV1:
                mBtnThicknessLv1.setSelected(true);
                break;
            case Global.THICKNESS_VALUE_LV2:
                mBtnThicknessLv2.setSelected(true);
                break;
            case Global.THICKNESS_VALUE_LV3:
                mBtnThicknessLv3.setSelected(true);
                break;
            case Global.THICKNESS_VALUE_LV4:
                mBtnThicknessLv4.setSelected(true);
                break;
            case Global.THICKNESS_VALUE_LV5:
                mBtnThicknessLv5.setSelected(true);
                break;
            default:
                break;
        }

        mTvThickness.setText(String.valueOf(value));
        int imageLevelBase;
        switch (mToolboxConfiguration.getPenStyle()) {
            case ToolboxConfiguration.PenStyle.PENCIL:
                imageLevelBase = 0;
                break;
            case ToolboxConfiguration.PenStyle.FOUNTAINPEN:
                imageLevelBase = 1;
                break;
            case ToolboxConfiguration.PenStyle.BRUSH:
                imageLevelBase = 2;
                break;
            case ToolboxConfiguration.PenStyle.LINE:
            case ToolboxConfiguration.PenStyle.RECTANGLE:
            case ToolboxConfiguration.PenStyle.OVAL:
            case ToolboxConfiguration.PenStyle.TRIANGLE:
            default:
                imageLevelBase = 3;
                break;
        }
        int imageLevel = value + imageLevelBase * 15;
        mIvThickness.setImageLevel(imageLevel);

        mToolboxConfiguration.setPenThickness(value);
    }
}
