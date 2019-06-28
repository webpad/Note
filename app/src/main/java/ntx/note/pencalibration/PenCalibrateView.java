
package ntx.note.pencalibration;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import name.vbraun.lib.pen.Hardware;
import ntx.note2.R;

public class PenCalibrateView extends View {

    private final static String TAG = "PenCalibrateView";

    Context mContext;
    Bitmap mBitmap;
    Canvas mCanvas;
    Paint mPaint;
    private Path mPath;

    int intCanvasHeight = 0;
    int intCanvasWidth = 0;
    int calibrate_space = 300;
    int calibrate_point_x1, calibrate_point_y1;
    int calibrate_point_x2, calibrate_point_y2;
    int calibrate_point_x3, calibrate_point_y3;
    int intCalibratePointCount = 0;
    int delta_x1, delta_y1;
    int delta_x2, delta_y2;
    int delta_x3, delta_y3;
    int result_pen_offset_x, result_pen_offset_y;

    private int fLinewidth = 2;

    public PenCalibrateView(Context context) {
        this(context, null);
        mContext = context;
    }

    public PenCalibrateView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public PenCalibrateView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void init() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(fLinewidth);
        mPaint.setDither(true);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int curW = (mBitmap != null) ? mBitmap.getWidth() : 0;
        int curH = (mBitmap != null) ? mBitmap.getHeight() : 0;
        if (curW >= w && curH >= h) {
            return;
        }

        if (curW < w)
            curW = w;
        if (curH < h)
            curH = h;

        intCanvasHeight = curH;
        intCanvasWidth = curW;
        calibrate_point_x1 = calibrate_space;
        calibrate_point_y1 = calibrate_space;
        calibrate_point_x2 = intCanvasWidth - calibrate_space;
        calibrate_point_y2 = calibrate_space;
        calibrate_point_x3 = intCanvasWidth - calibrate_space;
        calibrate_point_y3 = intCanvasHeight - calibrate_space;

        Canvas newCanvas = new Canvas();
        Bitmap newBitmap = Bitmap.createBitmap(curW, curH, Bitmap.Config.ARGB_8888);
        newCanvas.setBitmap(newBitmap);
        newCanvas.drawColor(Color.WHITE);
        if (mBitmap != null) {
            newCanvas.drawBitmap(mBitmap, 0, 0, null);
        }
        mBitmap = newBitmap;
        mCanvas = newCanvas;

        startPenCalibrate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (Hardware.isEinkHandWritingHardwareType()) {
            if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                    return true;
            }
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (intCalibratePointCount == 1) {
                    delta_x1 = calibrate_point_x1 - Math.round(event.getX());
                    delta_y1 = calibrate_point_y1 - Math.round(event.getY());
                } else if (intCalibratePointCount == 2) {
                    delta_x2 = calibrate_point_x2 - Math.round(event.getX());
                    delta_y2 = calibrate_point_y2 - Math.round(event.getY());
                } else if (intCalibratePointCount == 0) {
                    delta_x3 = calibrate_point_x3 - Math.round(event.getX());
                    delta_y3 = calibrate_point_y3 - Math.round(event.getY());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                if (intCalibratePointCount == 1) {
                    intCalibratePointCount = 2;
                    drawCalibrationPoint(calibrate_point_x2, calibrate_point_y2);
                } else if (intCalibratePointCount == 2) {
                    intCalibratePointCount = 0;
                    drawCalibrationPoint(calibrate_point_x3, calibrate_point_y3);
                } else if (intCalibratePointCount == 0) {
                    result_pen_offset_x = Math.round((float)(delta_x1 + delta_x2 + delta_x3) / 3);
                    result_pen_offset_y = Math.round((float)(delta_y1 + delta_y2 + delta_y3) / 3);
                    if ((result_pen_offset_x % 4) != 0)
                        result_pen_offset_x = result_pen_offset_x - (result_pen_offset_x % 4);
                    if ((result_pen_offset_y % 4) != 0)
                        result_pen_offset_y = result_pen_offset_y - (result_pen_offset_y % 4);
                    if ((Math.abs(result_pen_offset_x) > 20) || (Math.abs(result_pen_offset_y) > 20)) {
                        startPenCalibrate();
                        return true;
                    }
                    ((PenCalibrateActivity) mContext).onPenCalibrationFinished(result_pen_offset_x, result_pen_offset_y);
                }
                break;
        }

        return true;
    }

    public void startPenCalibrate() {
        intCalibratePointCount = 1;
        drawCalibrationPoint(calibrate_point_x1, calibrate_point_y1);
        return;
    }

    private synchronized void drawCalibrationPoint(float x, float y) {

        mBitmap.eraseColor(getResources().getColor(R.drawable.white));
        mPath.reset();

        mPath.moveTo(x - 50, y);
        mPath.lineTo(x + 50, y);
        mPath.moveTo(x, y - 50);
        mPath.lineTo(x, y + 50);

        if (mCanvas == null) {
            return;
        }
        if (mPath == null) {
            return;
        }
        if (mPaint == null) {
            return;
        }
        mCanvas.drawPath(mPath, mPaint);
        invalidate();
    }
}
