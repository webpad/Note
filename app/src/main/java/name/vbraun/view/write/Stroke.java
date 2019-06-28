package name.vbraun.view.write;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import junit.framework.Assert;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;

import ntx.note.ALog;
import ntx.note.Global;
import ntx.note.artist.Artist;
import ntx.note.artist.FillStyle;
import ntx.note.artist.LineStyle;

public class Stroke extends Graphics {
    private static final String TAG = "Stroke";

    // line thickness in fraction of the larger dimension of the page
    public static final float LINE_THICKNESS_SCALE = 1 / 1600f;
    private static final int STROKE_VERSION = 3;
    private int version;

    // the actual data
    protected int N;
    protected float[] position_x;
    protected float[] position_y;
    protected float[] backup_position_x;
    protected float[] backup_position_y;
    protected float[] pressure;

    private final Paint mPen = new Paint();
    private Path path = new Path();
    protected int pen_thickness = 0;
    protected int pen_color = Color.BLACK;
    public static final int LIGHT_GRAY = Global.grey_A;
    public static final int DARK_GRAY = Global.grey_5;
    public static final int GRAY = Global.grey_8;

    // subsampling tolerance
    private static final float EPSILON = 2e-4f;

    //simplify threshold
    private static final int SIMPLIFY_THRESHOLD = 60;

    /**
     * Constructor for pen stroke objects
     *
     * @param pen_type
     * @param pen_thickness
     * @param pen_color
     * @param transform
     * @param x             array of floats, the x coordinates (normalized 0..1)
     * @param y             array of floats, the y coordinates (normalized 0..1)
     * @param p             array of floats, the pressure (normalized 0..1)
     * @param from          integer, the start of the range of values to use from the
     *                      arrays
     * @param to            integer, the last value to use from the arrays
     */
    public Stroke(@Tool int pen_type, int pen_thickness, int pen_color, Transformation transform, float[] x, float[] y,
                  float[] p, int from, int to) {
        super(pen_type);
        version = STROKE_VERSION;
//		Assert.assertTrue("Pen type is not actual pen.", pen_type == Tool.FOUNTAINPEN || pen_type == Tool.PENCIL);
        Assert.assertTrue("Pen type is not actual pen.", (pen_type == Tool.BRUSH) || (pen_type == Tool.FOUNTAINPEN) || (pen_type == Tool.PENCIL));    //artis
        N = to - from;
        Assert.assertTrue("Stroke must consist of at least two points", N >= 2);
        position_x = Arrays.copyOfRange(x, from, to);
        position_y = Arrays.copyOfRange(y, from, to);
        pressure = Arrays.copyOfRange(p, from, to);
        setPen(pen_thickness, pen_color);
        setTransform(transform);
    }

    public int getStrokeColor() {
        return pen_color;
    }

    /**
     * Copy constructor
     */
    protected Stroke(final Stroke stroke) {
        super(stroke);
        N = stroke.N;
        version = stroke.version;
        position_x = stroke.position_x.clone();
        position_y = stroke.position_y.clone();
        pressure = stroke.pressure.clone();
        setPen(stroke.pen_thickness, stroke.pen_color);
    }

    @Override
    public Graphics getBackupGraphics() {
        Stroke backupGraphics = new Stroke(this);
        backupGraphics.position_x = this.backup_position_x.clone();
        backupGraphics.position_y = this.backup_position_y.clone();
        backupGraphics.computeBoundingBox();
        return backupGraphics;
    }

    @Override
    public Stroke getCloneGraphics() {
        return new Stroke(this);
    }

    @Override
    protected void backup() {
        backup_position_x = new float[N];
        backup_position_y = new float[N];

        backup_position_x = position_x.clone();
        backup_position_y = position_y.clone();
    }

    @Override
    protected void restore() {
        position_x = backup_position_x.clone();
        position_y = backup_position_y.clone();
    }

    @Override
    public void replace(Graphics graphics) {
        Stroke s = (Stroke) graphics;
        this.position_x = s.position_x.clone();
        this.position_y = s.position_y.clone();
        computeBoundingBox();
    }

    /**
     * Create a new Stroke object from raw input data
     *
     * @param pen_type
     * @param pen_thickness
     * @param pen_color
     * @param transform
     * @param x             array of floats, the x coordinates (screen coordinates)
     * @param y             array of floats, the y coordinates (screen coordinates)
     * @param p             array of floats, the pressure (normalized 0..1)
     * @param N             integer, the common length of the arrays.
     */
    public static Stroke fromInput(@Tool int pen_type, int pen_thickness, int pen_color, Transformation transform,
                                   float[] x, float[] y, float[] p, int N, LinearFilter.Filter filter) {
        Stroke s = new Stroke(pen_type, pen_thickness, pen_color, transform, x, y, p, 0, N);
        s.applyInverseTransform();
//		s.computeBoundingBox();

//		s.smooth(filter);
//		s.smoothGaussianFilter(5);	//artis: test

        if (N > SIMPLIFY_THRESHOLD) {
//		s.simplify();
        }

        return s;
    }

    private void setPen(int new_pen_thickness, int new_pen_color) {
        pen_thickness = new_pen_thickness;
        pen_color = new_pen_color;
        mPen.setStrokeCap(Cap.ROUND);
        recompute_bounding_box = true;
    }

    // static method that exports the pen scaling algorithm
    public static float getScaledPenThickness(float scale, float pen_thickness) {
        return pen_thickness * scale * LINE_THICKNESS_SCALE * 4 / 3;
    }

    // static method that exports the pen scaling algorithm
    public static float getScaledPenThickness(Transformation transform, float pen_thickness) {
        return pen_thickness * transform.scale * LINE_THICKNESS_SCALE * 4 / 3;
    }

    // this computes the argument to Paint.setStrokeWidth()
    public float getScaledPenThickness() {
        return getScaledPenThickness(scale, pen_thickness);
    }

    // Get the scaled thickness for a different scale factor (i.e. printing)
    public float getScaledPenThickness(float scale) {
        return getScaledPenThickness(scale, pen_thickness);
    }

    public float getPencilThickness() {
        return pen_thickness;
    }

    /**
     * Return the smallest rectangle (in raw page coordinates) containing the
     * stroke
     */
    public RectF getEnvelopingRect() {
        float x, y, xmin, ymin, xmax, ymax;
        xmin = xmax = position_x[0];
        ymin = ymax = position_y[0];
        for (int i = 1; i < N; i++) {
            x = position_x[i];
            y = position_y[i];
            xmin = Math.min(xmin, x);
            xmax = Math.max(xmax, x);
            ymin = Math.min(ymin, y);
            ymax = Math.max(ymax, y);
        }
        return new RectF(xmin, ymin, xmax, ymax);
    }


    @Override
    protected void computeBoundingBox() {
        float x0, x1, y0, y1, x, y;
        float x_p0, x_p1, y_p0, y_p1, b_s;
        x0 = x1 = position_x[0] * scale + offset_x;
        y0 = y1 = position_y[0] * scale + offset_y;

        float scaled_pen_thickness = getScaledPenThickness(scale, pen_thickness);
        if (tool == Tool.BRUSH) {
            if (version >= 3)
                b_s = (scaled_pen_thickness * ((float) Math.pow(pressure[0], 3))) / 2;
            else
                b_s = (scaled_pen_thickness * pressure[0]) / 2;

            x_p0 = x0 - b_s;
            x_p1 = x0 + b_s;
            y_p0 = y0 - b_s;
            y_p1 = y0 + b_s;

            x0 = Math.min(x_p0, x_p1);
            x1 = Math.max(x_p0, x_p1);
            y0 = Math.min(y_p0, y_p1);
            y1 = Math.max(y_p0, y_p1);
        } else if (tool == Tool.FOUNTAINPEN) {
            if (pen_thickness == 2) {
                scaled_pen_thickness = 1;
            }
            b_s = (scaled_pen_thickness * pressure[0]) / 2;

            x_p0 = x0 - b_s;
            x_p1 = x0 + b_s;
            y_p0 = y0 - b_s;
            y_p1 = y0 + b_s;

            x0 = Math.min(x_p0, x_p1);
            x1 = Math.max(x_p0, x_p1);
            y0 = Math.min(y_p0, y_p1);
            y1 = Math.max(y_p0, y_p1);
        }

        for (int i = 1; i < N; i++) {
            x = position_x[i] * scale + offset_x;
            y = position_y[i] * scale + offset_y;
            if (tool == Tool.BRUSH) {
                if (version >= 3)
                    b_s = (scaled_pen_thickness * ((float) Math.pow(pressure[i], 3))) / 2;
                else
                    b_s = (scaled_pen_thickness * pressure[i]) / 2;

                x_p0 = x - b_s;
                x_p1 = x + b_s;
                y_p0 = y - b_s;
                y_p1 = y + b_s;

                x0 = Math.min(x_p0, x0);
                x1 = Math.max(x_p1, x1);
                y0 = Math.min(y_p0, y0);
                y1 = Math.max(y_p1, y1);
            } else if (tool == Tool.FOUNTAINPEN) {
                if (pen_thickness == 2) {
                    scaled_pen_thickness = 1;
                }
                b_s = (scaled_pen_thickness * pressure[0]) / 2;

                x_p0 = x - b_s;
                x_p1 = x + b_s;
                y_p0 = y - b_s;
                y_p1 = y + b_s;

                x0 = Math.min(x_p0, x0);
                x1 = Math.max(x_p1, x1);
                y0 = Math.min(y_p0, y0);
                y1 = Math.max(y_p1, y1);
            } else {
                x0 = Math.min(x0, x);
                x1 = Math.max(x1, x);
                y0 = Math.min(y0, y);
                y1 = Math.max(y1, y);
            }
        }

        if (tool == Tool.BRUSH || tool == Tool.FOUNTAINPEN)
            bBoxFloat.set(x0, y0, x1, y1);
        else
            bBoxFloat.set((x0 - scaled_pen_thickness / 2), (y0 - scaled_pen_thickness / 2), (x1 + scaled_pen_thickness / 2), (y1 + scaled_pen_thickness / 2));

        recompute_bounding_box = false;
    }


    /**
     * Apply the inverse transform screen -> page coordinates. This is only
     * useful when creating the stroke from raw pen data.
     */
    private void applyInverseTransform() {
        float x, y;
//		if(tool==tool.PENCIL) {
//			scale = 1.0f;
//		}

        for (int i = 0; i < N; i++) {
            x = position_x[i];
            y = position_y[i];
            position_x[i] = (x - offset_x) / scale;
            position_y[i] = (y - offset_y) / scale;
        }
        recompute_bounding_box = true;
    }

    @Override
    public float distance(float x_screen, float y_screen) {
        float x = (x_screen - offset_x) / scale;
        float y = (y_screen - offset_y) / scale;
        float d = Math.abs(x - position_x[0]) + Math.abs(y - position_y[0]);
        for (int i = 1; i < N; i++) {
            float d_new = Math.abs(x - position_x[i]) + Math.abs(y - position_y[i]);
            d = Math.min(d, d_new);
        }
        return d * scale;
    }

    @Override
    public boolean intersects(RectF r_screen) {
        // Log.v(TAG,
        // ""+r_screen.left+" "+r_screen.bottom+" "+r_screen.right+" "+r_screen.top);
        RectF r = new RectF((r_screen.left - offset_x) / scale, (r_screen.top - offset_y) / scale,
                (r_screen.right - offset_x) / scale, (r_screen.bottom - offset_y) / scale);
        // Log.v(TAG, ""+r.left+" "+r.bottom+" "+r.right+" "+r.top);
        for (int i = 0; i < N; i++)
            if (r.contains(position_x[i], position_y[i]))
                return true;
        return false;
    }

    @Override
    public void draw(Canvas c) {

        if (HandwriterView.getAntiColor()) {
            mPen.setARGB(Color.alpha(pen_color), Color.red(pen_color) ^ 0xff,
                    Color.green(pen_color) ^ 0xff, Color.blue(pen_color) ^ 0xff);
        } else {
            mPen.setARGB(Color.alpha(pen_color), Color.red(pen_color),
                    Color.green(pen_color), Color.blue(pen_color));
        }
        mPen.setStyle(Paint.Style.STROKE);
        mPen.setColor(pen_color);

        mPen.setStrokeJoin(Paint.Join.ROUND);
        mPen.setStrokeCap(Paint.Cap.ROUND);

        BitmapShader mBitmapShader = getShaderByPenColor(pen_color);
        mPen.setShader(mBitmapShader);

        if (recompute_bounding_box)
            computeBoundingBox();

        // if we are zoomed in use higher-quality graphics
        final boolean zoom = (scale > 1500f);
        if (N <= 2 || (tool == Tool.PENCIL && !zoom))
            drawWithStraightLine(c, false);
        else if (tool == Tool.PENCIL)
            //drawPencilWithQuadraticBezier(c);
            drawWithStraightLine(c, false);
        else if (tool == Tool.BRUSH) {    //artis
//			drawBrushWithCubicBezier(c);
            drawWithStraightLineUSingBrushFountain(c, false);
        } else if (tool == Tool.FOUNTAINPEN)
//			drawFountainpenWithCubicBezier(c);
            drawWithStraightLineUSingBrushFountain(c, false);
    }

    @Override
    public void drawOutLine(Canvas c) {
        if (HandwriterView.getAntiColor()) {
            mPen.setARGB(Color.alpha(pen_color), Color.red(pen_color) ^ 0xff,
                    Color.green(pen_color) ^ 0xff, Color.blue(pen_color) ^ 0xff);
        } else {
            mPen.setARGB(Color.alpha(pen_color), Color.red(pen_color),
                    Color.green(pen_color), Color.blue(pen_color));
        }

        mPen.setStyle(Paint.Style.FILL_AND_STROKE);

        mPen.setStrokeJoin(Paint.Join.ROUND);
        mPen.setStrokeCap(Paint.Cap.ROUND);

        BitmapShader bitmapShader;

        if (recompute_bounding_box)
            computeBoundingBox();

        final boolean zoom = (scale > 1500f);
        if (N <= 2 || (tool == Tool.PENCIL && !zoom)) {
            /**
             * Draw original
             */
            mPen.setColor(pen_color);
            bitmapShader = getShaderByPenColor(pen_color);
            mPen.setShader(bitmapShader);
            drawWithStraightLine(c, true);

            /**
             * Draw White
             */
            mPen.setColor(Color.WHITE);
            bitmapShader = getShaderByPenColor(Color.WHITE);
            mPen.setShader(bitmapShader);
            drawWithStraightLine(c, false);
        } else if (tool == Tool.PENCIL) {
            /**
             * Draw original
             */
            mPen.setColor(pen_color);
            bitmapShader = getShaderByPenColor(pen_color);
            mPen.setShader(bitmapShader);
            drawWithStraightLine(c, true);

            /**
             * Draw White
             */
            mPen.setColor(Color.WHITE);
            bitmapShader = getShaderByPenColor(Color.WHITE);
            mPen.setShader(bitmapShader);
            drawWithStraightLine(c, false);
        } else if (tool == Tool.BRUSH) {
            /**
             * Draw Black
             */
            mPen.setColor(pen_color);
            bitmapShader = getShaderByPenColor(pen_color);
            mPen.setShader(bitmapShader);
            drawWithStraightLineUSingBrushFountain(c, true);

            /**
             * Draw White
             */
            mPen.setColor(Color.WHITE);
            bitmapShader = getShaderByPenColor(Color.WHITE);
            mPen.setShader(bitmapShader);
            drawWithStraightLineUSingBrushFountain(c, false);
        } else if (tool == Tool.FOUNTAINPEN) {
            /**
             * Draw Black
             */
            mPen.setColor(pen_color);
            bitmapShader = getShaderByPenColor(pen_color);
            mPen.setShader(bitmapShader);
            drawWithStraightLineUSingBrushFountain(c, true);

            /**
             * Draw White
             */
            mPen.setColor(Color.WHITE);
            bitmapShader = getShaderByPenColor(Color.WHITE);
            mPen.setShader(bitmapShader);
            drawWithStraightLineUSingBrushFountain(c, false);
        }
    }

    public void convert_draw(Canvas c) {
        // Page.draw already checked the bounding box, we definitely need to
        // draw

        if (HandwriterView.getAntiColor()) {
            mPen.setARGB(Color.alpha(pen_color), Color.red(pen_color) ^ 0xff,
                    Color.green(pen_color) ^ 0xff, Color.blue(pen_color) ^ 0xff);
        } else {
            mPen.setARGB(Color.alpha(pen_color), Color.red(pen_color),
                    Color.green(pen_color), Color.blue(pen_color));
        }
        mPen.setStyle(Paint.Style.STROKE);
        mPen.setColor(pen_color);
        mPen.setShader(null);//clear shader when convert
        mPen.setStrokeJoin(Paint.Join.ROUND);
        mPen.setStrokeCap(Paint.Cap.ROUND);

        if (recompute_bounding_box)
            computeBoundingBox();

        // if we are zoomed in use higher-quality graphics
        final boolean zoom = (scale > 1500f);
        if (N <= 2 || (tool == Tool.PENCIL && !zoom))
            drawWithStraightLine(c, false);
        else if (tool == Tool.PENCIL)
            drawWithStraightLine(c, false);
        else if (tool == Tool.BRUSH) {
            drawWithStraightLineUSingBrushFountain(c, false);
        } else if (tool == Tool.FOUNTAINPEN)
            drawWithStraightLineUSingBrushFountain(c, false);
    }

    public void drawThumbnail(Canvas c) {
        if (HandwriterView.getAntiColor()) {
            mPen.setARGB(Color.alpha(pen_color), Color.red(pen_color) ^ 0xff,
                    Color.green(pen_color) ^ 0xff, Color.blue(pen_color) ^ 0xff);
        } else {
            mPen.setARGB(Color.alpha(pen_color), Color.red(pen_color),
                    Color.green(pen_color), Color.blue(pen_color));
        }

        if (recompute_bounding_box)
            computeBoundingBox();

        final boolean zoom = (scale > 1500f);
        if (N <= 2 || (tool == Tool.PENCIL && !zoom))
            drawThumbWithStraightLine(c);
        else if (tool == Tool.PENCIL)
            drawThumbWithStraightLine(c);
        else if (tool == Tool.BRUSH)
            drawWithStraightLineUSingBrushFountain(c, false);
        else if (tool == Tool.FOUNTAINPEN)
            drawWithStraightLineUSingBrushFountain(c, false);
    }

    // 2019 1212 Alan: using the easiest way to draw brush
    private void drawWithStraightLineUSingBrushFountain(Canvas c, boolean plusBorderThickness) {

        float x0, x1, y0, y1, p0, p1;
        float scaledPenThickness = getScaledPenThickness(scale, pen_thickness);
        int strokeWidth;
        // c.drawRect(left, top, right, bottom, paint)
        // note: we offset the first point by 1/10 pixel since android does not
        // draw lines with start=end
        x0 = position_x[0] * scale + offset_x;
        y0 = position_y[0] * scale + offset_y;
        p0 = pressure[0];
//		Log.d("test","stroke"+ getScaledPenThickness() * Global.BRUSH_THICKNESS_WEIGHT);
        for (int i = 1; i < N; i++) {
            x1 = position_x[i] * scale + offset_x;
            y1 = position_y[i] * scale + offset_y;
            if (tool == Tool.BRUSH) {
                if (version >= 3)
                    p1 = (float) Math.pow(pressure[i], 3);
                else
                    p1 = pressure[i];
                strokeWidth = (int) (scaledPenThickness * p1);
                if (plusBorderThickness) {
                    strokeWidth += 2;
                }
                mPen.setStrokeWidth(strokeWidth);
                p0 = p1;
            }
            if (tool == Tool.FOUNTAINPEN) {
                p1 = pressure[i];
                if (pen_thickness == 2) {//minimum thickness -> set the same value as pencil's
                    if (plusBorderThickness)
                        mPen.setStrokeWidth(3);
                    else
                        mPen.setStrokeWidth(1);
                } else {
                    strokeWidth = (int) (scaledPenThickness * p1);
                    if (plusBorderThickness) {
                        strokeWidth += 2;
                    }
                    mPen.setStrokeWidth(strokeWidth);
                }
                p0 = p1;
            }
            c.drawLine(x0, y0, x1, y1, mPen);
            x0 = x1;
            y0 = y1;
        }


    }

    /**
     * The simplest way to render: use straight lines (ugly but fast)
     */
    private void drawWithStraightLine(Canvas c, boolean plusBorderThickness) {
        int currentThickness = (int) (getPencilThickness());
        if (plusBorderThickness) {
            currentThickness += 2;
        }
        mPen.setStrokeWidth(currentThickness);

        float x0, x1, y0, y1, p0, p1;
        // c.drawRect(left, top, right, bottom, paint)
        // note: we offset the first point by 1/10 pixel since android does not
        // draw lines with start=end
        x0 = position_x[0] * scale + offset_x + 0.1f;
//		x0 = position_x[0] * scale + offset_x;
        y0 = position_y[0] * scale + offset_y;
        p0 = pressure[0];
        for (int i = 1; i < N; i++) {
            x1 = position_x[i] * scale + offset_x;
            y1 = position_y[i] * scale + offset_y;

            c.drawLine(x0, y0, x1, y1, mPen);
            x0 = x1;
            y0 = y1;
        }
    }

    private BitmapShader getShaderByPenColor(int pen_color) {
        Bitmap mPatternBitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.RGB_565);
        mPatternBitmap.setDensity(185);

        switch (pen_color) {
            case Color.BLACK:
                mPatternBitmap.eraseColor(Color.BLACK);
                break;
            case DARK_GRAY:
                mPatternBitmap.setPixel(0, 0, Color.WHITE);
                mPatternBitmap.setPixel(1, 0, Color.BLACK);
                mPatternBitmap.setPixel(0, 1, Color.BLACK);
                mPatternBitmap.setPixel(1, 1, Color.BLACK);
                break;
            case GRAY:
                mPatternBitmap.setPixel(0, 0, Color.WHITE);
                mPatternBitmap.setPixel(1, 0, Color.BLACK);
                mPatternBitmap.setPixel(0, 1, Color.BLACK);
                mPatternBitmap.setPixel(1, 1, Color.WHITE);
                break;
            case LIGHT_GRAY:
                mPatternBitmap.setPixel(0, 0, Color.WHITE);
                mPatternBitmap.setPixel(1, 0, Color.BLACK);
                mPatternBitmap.setPixel(0, 1, Color.WHITE);
                mPatternBitmap.setPixel(1, 1, Color.WHITE);
                break;
            case Color.WHITE:
                mPatternBitmap.eraseColor(Color.WHITE);
                break;
            default:
                mPatternBitmap.eraseColor(Color.BLACK);
        }

        BitmapShader mBitmapShader = new BitmapShader(mPatternBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

        return mBitmapShader;
    }

    private void drawThumbWithStraightLine(Canvas c) {
        final float scaled_pen_thickness = getScaledPenThickness();
        // mPen.setColor(Color.BLACK);

        mPen.setStyle(Paint.Style.STROKE);
        mPen.setStrokeCap(Cap.ROUND);
        mPen.setStrokeJoin(Join.ROUND);
        mPen.setStrokeWidth(scaled_pen_thickness);
        float x0, x1, y0, y1, p0, p1;
        x0 = position_x[0] * scale + offset_x + 0.1f;
//		x0 = position_x[0] * scale + offset_x;
        y0 = position_y[0] * scale + offset_y;
        p0 = pressure[0];
        for (int i = 1; i < N; i++) {
            x1 = position_x[i] * scale + offset_x;
            y1 = position_y[i] * scale + offset_y;

            c.drawLine(x0, y0, x1, y1, mPen);
            x0 = x1;
            y0 = y1;
        }
    }

    /**
     * Quadratic Bezier curve for constant width
     * <p>
     * The trick is to use midpoints as start/stop point of the Bezier, and
     * actual data points as the control point.
     */
    private void drawPencilWithQuadraticBezier(Canvas c) {
        Assert.assertTrue(tool == Tool.PENCIL && N >= 3);
        path.rewind();
        mPen.setStyle(Paint.Style.STROKE);
        mPen.setStrokeCap(Cap.ROUND);
        mPen.setStrokeJoin(Join.ROUND);
        if (getScaledPenThickness() < 1.0f) {
            mPen.setStrokeWidth(getScaledPenThickness() * Global.PENCIL_THICKNESS_BASE);
        } else {
            mPen.setStrokeWidth(getScaledPenThickness());
        }
        float x0, x1, x2, x3, y0, y1, y2, y3;

        // the first actual point is treated as a midpoint
        x0 = position_x[0] * scale + offset_x + 0.1f;
        y0 = position_y[0] * scale + offset_y;
        path.moveTo(x0, y0);

        x1 = position_x[1] * scale + offset_x + 0.1f;
        y1 = position_y[1] * scale + offset_y;
        for (int i = 2; i < N - 1; i++) {
            // (x0,y0) and (x2,y2) are midpoints, (x1,y1) and (x3,y3) are actual points
            x3 = position_x[i] * scale + offset_x;
            y3 = position_y[i] * scale + offset_y;
            x2 = (x1 + x3) / 2f;
            y2 = (y1 + y3) / 2f;
            path.quadTo(x1, y1, x2, y2);
            x0 = x2;
            y0 = y2;
            x1 = x3;
            y1 = y3;
        }

        // the last actual point is treated as a midpoint
        x2 = position_x[N - 1] * scale + offset_x;
        y2 = position_y[N - 1] * scale + offset_y;
        path.quadTo(x1, y1, x2, y2);

        c.drawPath(path, mPen);
    }

    /**
     * Cubic Bezier for variable-width curves
     * <p>
     * This works similar to drawPencilWithQuadraticBezier, midpoints are
     * start/stop point and the actual data point is used as control. Only now
     * we draw as a filled shape instead of a stroke along the Bezier path. The
     * start/end point are displaced in the normal direction. The data point is
     * translated in the two distinct normal directions, yielding two control
     * points for the cubic Bezier.
     */
    private void drawFountainpenWithCubicBezier(Canvas c) {
        Assert.assertTrue(tool == Tool.FOUNTAINPEN && N >= 3);
        path.rewind();
        mPen.setStyle(Paint.Style.FILL);

        //Paint paint = new Paint();
        //paint.setARGB(0xff, 0xff, 0x0, 0x0);
        //paint.setStrokeWidth(0);
//		mPen.setStyle(Paint.Style.STROKE);

        final float scaled_pen_thickness = getScaledPenThickness() - 1;
        float x0, x1, x2, x3, y0, y1, y2, y3, p0, p1, p2, p3;
        float vx01, vy01, vx21, vy21;  // unit tangent vectors 0->1 and 1<-2
        float norm;
        float n_x0, n_y0, n_x2, n_y2; // the normals

        // the first actual point is treated as a midpoint
        x0 = position_x[0] * scale + offset_x + 0.1f;
        y0 = position_y[0] * scale + offset_y;
        p0 = pressure[0];

        x1 = position_x[1] * scale + offset_x + 0.1f;
        y1 = position_y[1] * scale + offset_y;
        p1 = pressure[1];
        vx01 = x1 - x0;
        vy01 = y1 - y0;
        // instead of dividing tangent/norm by two, we multiply norm by 2
        norm = (float) Math.sqrt(vx01 * vx01 + vy01 * vy01 + 0.0001f) * 2f;
        vx01 = vx01 / norm * scaled_pen_thickness * p0;
        vy01 = vy01 / norm * scaled_pen_thickness * p0;
        n_x0 = vy01;
        n_y0 = -vx01;
        for (int i = 2; i < N - 1; i++) {
            // (x0,y0) and (x2,y2) are midpoints, (x1,y1) and (x3,y3) are actual points
            x3 = position_x[i] * scale + offset_x;
            y3 = position_y[i] * scale + offset_y;
            p3 = pressure[i];

            // jimmychung 20170105 fix break line while fast draw.
            if (x3 == x1 && y3 == y1) continue;

            x2 = (x1 + x3) / 2f;
            y2 = (y1 + y3) / 2f;
            p2 = (p1 + p3) / 2f;
            vx21 = x1 - x2;
            vy21 = y1 - y2;
            norm = (float) Math.sqrt(vx21 * vx21 + vy21 * vy21 + 0.0001f) * 2f;
            vx21 = vx21 / norm * scaled_pen_thickness * p2;
            vy21 = vy21 / norm * scaled_pen_thickness * p2;
            n_x2 = -vy21;
            n_y2 = vx21;

            path.rewind();
            path.moveTo(x0 + n_x0, y0 + n_y0);
            // The + boundary of the stroke
            path.cubicTo(x1 + n_x0, y1 + n_y0, x1 + n_x2, y1 + n_y2, x2 + n_x2, y2 + n_y2);
            // round out the cap
            path.cubicTo(x2 + n_x2 - vx21, y2 + n_y2 - vy21, x2 - n_x2 - vx21, y2 - n_y2 - vy21, x2 - n_x2, y2 - n_y2);
            // THe - boundary of the stroke
            path.cubicTo(x1 - n_x2, y1 - n_y2, x1 - n_x0, y1 - n_y0, x0 - n_x0, y0 - n_y0);
            // round out the other cap
            path.cubicTo(x0 - n_x0 - vx01, y0 - n_y0 - vy01, x0 + n_x0 - vx01, y0 + n_y0 - vy01, x0 + n_x0, y0 + n_y0);
            c.drawPath(path, mPen);

            x0 = x2;
            y0 = y2;
            p0 = p2;
            x1 = x3;
            y1 = y3;
            p1 = p3;
            vx01 = -vx21;
            vy01 = -vy21;
            n_x0 = n_x2;
            n_y0 = n_y2;
        }

        // the last actual point is treated as a midpoint
        x2 = position_x[N - 1] * scale + offset_x;
        y2 = position_y[N - 1] * scale + offset_y;
        p2 = pressure[N - 1];
        vx21 = x1 - x2;
        vy21 = y1 - y2;
        norm = (float) Math.sqrt(vx21 * vx21 + vy21 * vy21 + 0.0001f) * 2f;
        vx21 = vx21 / norm * scaled_pen_thickness * p2;
        vy21 = vy21 / norm * scaled_pen_thickness * p2;
        n_x2 = -vy21;
        n_y2 = vx21;

        path.rewind();
        path.moveTo(x0 + n_x0, y0 + n_y0);
        path.cubicTo(x1 + n_x0, y1 + n_y0, x1 + n_x2, y1 + n_y2, x2 + n_x2, y2 + n_y2);
        path.cubicTo(x2 + n_x2 - vx21, y2 + n_y2 - vy21, x2 - n_x2 - vx21, y2 - n_y2 - vy21, x2 - n_x2, y2 - n_y2);
        path.cubicTo(x1 - n_x2, y1 - n_y2, x1 - n_x0, y1 - n_y0, x0 - n_x0, y0 - n_y0);
        path.cubicTo(x0 - n_x0 - vx01, y0 - n_y0 - vy01, x0 + n_x0 - vx01, y0 + n_y0 - vy01, x0 + n_x0, y0 + n_y0);
        c.drawPath(path, mPen);
    }


    /**
     * Cubic Bezier for variable-width curves  Artis' patch for brush
     * <p>
     * This works similar to drawPencilWithQuadraticBezier, midpoints are
     * start/stop point and the actual data point is used as control. Only now
     * we draw as a filled shape instead of a stroke along the Bezier path. The
     * start/end point are displaced in the normal direction. The data point is
     * translated in the two distinct normal directions, yielding two control
     * points for the cubic Bezier.
     */
    private void drawBrushWithCubicBezier(Canvas c) {
        Assert.assertTrue(tool == Tool.BRUSH && N >= 3);
        path.rewind();
        mPen.setStyle(Paint.Style.FILL);

//		Paint paint = new Paint();
//		paint.setARGB(0xff, 0xff, 0x0, 0x0);
//		paint.setStrokeWidth(10);
//		mPen.setStyle(Paint.Style.FILL_AND_STROKE);

        float thickness_weight = 1;    //artis, value should be set in HandwriteView::setPenThickness()
        float b_scale = scale;        //artis, brush scale
        float b_offset_x = offset_x;    //artis, brush offset_x
        float b_offset_y = offset_y;    //artis, brush offset_y
        float pressure_weight = 1.0f;    //artis,

//		final float scaled_pen_thickness = getScaledPenThickness();	//artis:
        final float scaled_pen_thickness = getScaledPenThickness(b_scale, pen_thickness * thickness_weight);    //artis: NG
//		ALog.d(TAG, "S<-- drawBrushWithCubicBezier, b_scale=" + b_scale + ", scaled_pen_thickness=" + scaled_pen_thickness + ", N=" +N );


        float x0, x1, x2, x3, y0, y1, y2, y3, p0, p1, p2, p3;
        float vx01, vy01, vx21, vy21;  // unit tangent vectors 0->1 and 1<-2
        float norm;
        float n_x0, n_y0, n_x2, n_y2; // the normals

        // the first actual point is treated as a midpoint
        x0 = (position_x[0] * b_scale + b_offset_x + 0.1f);
        y0 = (position_y[0] * b_scale + b_offset_y);
        p0 = (pressure[0] * pressure_weight);

        x1 = (position_x[1] * b_scale + b_offset_x + 0.1f);
        y1 = (position_y[1] * b_scale + b_offset_y);
        p1 = (pressure[1] * pressure_weight);
        vx01 = (x1 - x0);
        vy01 = (y1 - y0);


//		float norm_weight = 2.0f;	//artis, original value
        float norm_weight = Global.BRUSH_NORM_WEIGHT;    //artis, test value
        // instead of dividing tangent/norm by two, we multiply norm by 2
        norm = ((float) Math.sqrt(vx01 * vx01 + vy01 * vy01 + 0.0001f) * norm_weight);
        vx01 = (vx01 / norm * scaled_pen_thickness * p0);
        vy01 = (vy01 / norm * scaled_pen_thickness * p0);
        n_x0 = vy01;
        n_y0 = -vx01;
        for (int i = 2; i < N - 1; i++) {
            // (x0,y0) and (x2,y2) are midpoints, (x1,y1) and (x3,y3) are actual points
            x3 = position_x[i] * b_scale + b_offset_x;
            y3 = position_y[i] * b_scale + b_offset_y;
//			p3 = pressure[i];						//artis, original value
            p3 = (pressure_weight * pressure[i]);    //artis, test value

            // jimmychung 20170105 fix break line while fast draw.
            if (x3 == x1 && y3 == y1) continue;

            x2 = (x1 + x3) / 2f;
            y2 = (y1 + y3) / 2f;
            p2 = (p1 + p3) / 2f;
            vx21 = x1 - x2;
            vy21 = y1 - y2;
            norm = (float) Math.sqrt(vx21 * vx21 + vy21 * vy21 + 0.0001f) * norm_weight;
            vx21 = vx21 / norm * scaled_pen_thickness * p2;
            vy21 = vy21 / norm * scaled_pen_thickness * p2;
            n_x2 = -vy21;
            n_y2 = vx21;

            path.rewind();
            path.moveTo(x0 + n_x0, y0 + n_y0);
            // The + boundary of the stroke
            path.cubicTo(x1 + n_x0, y1 + n_y0, x1 + n_x2, y1 + n_y2, x2 + n_x2, y2 + n_y2);

            // round out the cap
            path.cubicTo(x2 + n_x2 - vx21, y2 + n_y2 - vy21, x2 - n_x2 - vx21, y2 - n_y2 - vy21, x2 - n_x2, y2 - n_y2);
            // THe - boundary of the stroke
            path.cubicTo(x1 - n_x2, y1 - n_y2, x1 - n_x0, y1 - n_y0, x0 - n_x0, y0 - n_y0);
            // round out the other cap
            path.cubicTo(x0 - n_x0 - vx01, y0 - n_y0 - vy01, x0 + n_x0 - vx01, y0 + n_y0 - vy01, x0 + n_x0, y0 + n_y0);
            c.drawPath(path, mPen);

            x0 = x2;
            y0 = y2;
            p0 = p2;
            x1 = x3;
            y1 = y3;
            p1 = p3;
            vx01 = -vx21;
            vy01 = -vy21;
            n_x0 = n_x2;
            n_y0 = n_y2;
        }

        // the last actual point is treated as a midpoint
        x2 = position_x[N - 1] * b_scale + b_offset_x;
        y2 = position_y[N - 1] * b_scale + b_offset_y;
        p2 = (pressure[N - 1] * pressure_weight);
        vx21 = x1 - x2;
        vy21 = y1 - y2;
        norm = (float) Math.sqrt(vx21 * vx21 + vy21 * vy21 + 0.0001f) * norm_weight;
        vx21 = vx21 / norm * scaled_pen_thickness * p2;
        vy21 = vy21 / norm * scaled_pen_thickness * p2;
        n_x2 = -vy21;
        n_y2 = vx21;

        path.rewind();
        path.moveTo(x0 + n_x0, y0 + n_y0);
        path.cubicTo(x1 + n_x0, y1 + n_y0, x1 + n_x2, y1 + n_y2, x2 + n_x2, y2 + n_y2);
        path.cubicTo(x2 + n_x2 - vx21, y2 + n_y2 - vy21, x2 - n_x2 - vx21, y2 - n_y2 - vy21, x2 - n_x2, y2 - n_y2);
        path.cubicTo(x1 - n_x2, y1 - n_y2, x1 - n_x0, y1 - n_y0, x0 - n_x0, y0 - n_y0);
        path.cubicTo(x0 - n_x0 - vx01, y0 - n_y0 - vy01, x0 + n_x0 - vx01, y0 + n_y0 - vy01, x0 + n_x0, y0 + n_y0);
        c.drawPath(path, mPen);
    }


    @Override
    public void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(version); // protocol #1
        out.writeInt(pen_color);
        out.writeInt(pen_thickness);
        out.writeInt(tool);
        out.writeInt(N);
        for (int i = 0; i < N; i++) {
            out.writeFloat(position_x[i]);
            out.writeFloat(position_y[i]);
            out.writeFloat(pressure[i]);
        }
    }

    public Stroke(DataInputStream in) throws IOException {
        super(Tool.FOUNTAINPEN);
//		ALog.e(TAG, "S<-----Stroke super:");
        version = in.readInt();
        if (version < 1 || version > 3)
            throw new IOException("Unknown stroke version!");
        pen_color = in.readInt();
        pen_thickness = in.readInt();
        int toolInt = in.readInt();
        if (toolInt < 0 || toolInt >= Tool.MAX_BOUNDS)
            throw new IOException("Tool ID out of bounds.");
        tool = toolInt;
        setPen(pen_thickness, pen_color);
        N = in.readInt();
        position_x = new float[N];
        position_y = new float[N];
        pressure = new float[N];
        for (int i = 0; i < N; i++) {
            position_x[i] = in.readFloat();
            position_y[i] = in.readFloat();
            pressure[i] = in.readFloat();
        }
        if (version == 1) {
            // I changed the thickness quantization for v2
            pen_thickness *= 2;
            simplify();
        }
    }

    /**
     * Apply a filter to smoothen the sample points
     */
    private void smooth(LinearFilter.Filter filterId) {
//		ALog.e(TAG, "S<-----smooth:");
        LinearFilter filter = LinearFilter.get(filterId);
        filter.apply(position_x);
        filter.apply(position_y);
        filter.apply(pressure);
    }

    /**
     * Use a Gaussian filter to smoothen See
     * http://en.wikipedia.org/wiki/Gaussian_filter for details
     *
     * @param n_l integer, useful values are 0 (none), 1 (least), .., 5 (most)
     */
    private void smoothGaussianFilter(int n_l) {
        ALog.e(TAG, "S<-----smoothGaussianFilter:");
        if (n_l <= 0)
            return;

        int nw = 2 * n_l + 1;
        float[] new_position_x = new float[N];
        float[] new_position_y = new float[N];
        float[] new_pressure = new float[N];
        float[] window = new float[nw];

        // generate a gaussian filter window
        float sumw = 0;
        for (int i = 0; i < nw; i++) {
            float sig = 4 * (float) (i - n_l) / (float) (nw);
            window[i] = (float) Math.exp(-sig * sig);
            sumw += window[i];
        }
        for (int i = 0; i < nw; i++) {
            window[i] /= sumw;
        }

        float xpos, ypos, pres;
        int nl;
        boolean correctd;
        new_position_x[0] = position_x[0];
        new_position_y[0] = position_y[0];
        new_pressure[0] = pressure[0];
        new_position_x[N - 1] = position_x[N - 1];
        new_position_y[N - 1] = position_y[N - 1];
        new_pressure[N - 1] = pressure[N - 1];
        for (int l = 1; l < N - 1; l++) {
            if (l < n_l) {
                nl = l;
                correctd = true;
            } else if (l >= N - n_l) {
                nl = N - l - 1;
                correctd = true;
            } else {
                nl = n_l;
                correctd = false;
            }
            xpos = 0f;
            ypos = 0f;
            pres = 0f;
            for (int i = l - nl, j = n_l - nl; i < l + nl + 1; i++, j++) {
                xpos += position_x[i] * window[j];
                ypos += position_y[i] * window[j];
                pres += pressure[i] * window[j];
            }
            if (correctd) {
                sumw = 0;
                for (int j = n_l - nl; j < n_l + nl + 1; j++) {
                    sumw += window[j];
                }
                xpos /= sumw;
                ypos /= sumw;
                pres /= sumw;
            }
            new_position_x[l] = xpos;
            new_position_y[l] = ypos;
            new_pressure[l] = pres;
        }
        position_x = new_position_x;
        position_y = new_position_y;
        pressure = new_pressure;
    }

    // Reduce the number of points
    // http://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm
    // Assumes that x,y coordinates and pressure are scaled to be within [0,1]
    // for example, using apply_inverse_transform
    // non-standard metric for "perpendicular distance" for numerical stability
    private void simplify() {
        // points.add(0);
        // points.add(N-1);
        // ListIterator<Integer> point_iter = points.listIterator(1);
        // simplifyRecursion(0, N-1, point_iter);
        LinkedList<Integer> points = simplifyWithoutRecursion();

        int new_N = points.size();
        float[] new_position_x = new float[new_N];
        float[] new_position_y = new float[new_N];
        float[] new_pressure = new float[new_N];
        int n = 0;
        ListIterator<Integer> point_iter = points.listIterator();
        while (point_iter.hasNext()) {
            int p = point_iter.next();
            new_position_x[n] = position_x[p];
            new_position_y[n] = position_y[p];
            new_pressure[n] = pressure[p];
            n++;
        }
        Assert.assertEquals(n, new_N);
        N = new_N;
        position_x = new_position_x;
        position_y = new_position_y;
        pressure = new_pressure;
    }

    // find the mid point with the largest deviation from a straight line
    // return -1 if there is none up to the desired precision EPSILON
    private Integer simplifyFindMidPoint(Integer point0, Integer point1) {
        float x0 = position_x[point0];
        float y0 = position_y[point0];
        float p0 = pressure[point0];

        float x1 = position_x[point1];
        float y1 = position_y[point1];
        float p1 = pressure[point1];

        // the line has the equation ax + by + c = 0
        float a = y1 - y0;
        float b = x0 - x1;
        float c = x1 * y0 - x0 * y1;
        float normal_abs = (float) Math.sqrt(a * a + b * b);

        // distance between p0 and p1
        float dx = x1 - x0;
        float dy = y1 - y0;
        float distance_01 = (float) Math.sqrt(dx * dx + dy * dy);

        // average pressure is the 3rd dimension (line thickness is determined
        // by it)
        float p_avg = (p0 + p1) / 2;

        int mid = -1;
        float distance_max = 0;
        for (int i = point0 + 1; i < point1; i++) {
            float x = position_x[i];
            float y = position_y[i];
            float p = pressure[i];
            float distance = 0;

            // distance in pressure
            if ((tool == Tool.FOUNTAINPEN) || (tool == Tool.BRUSH)) {
                float p_0_avg = (p0 + p) / 2;
                float p_1_avg = (p1 + p) / 2;
                float pressure_difference = Math.max(Math.abs(p_0_avg - p_avg), Math.abs(p_1_avg - p_avg))
                        * LINE_THICKNESS_SCALE * 3;
                distance = Math.max(distance, pressure_difference);
            }

            // distance for degenerate triangles where midpoint is far away from
            // p0, p1
            float dx0 = x - x0;
            float dy0 = y - y0;
            float distance_p0 = (float) Math.sqrt(dx0 * dx0 + dy0 * dy0);
            distance = Math.max(distance, distance_p0 - distance_01);
            float dx1 = x - x1;
            float dy1 = y - y1;
            float distance_p1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
            distance = Math.max(distance, distance_p1 - distance_01);

            // perpendicular distance
            if (distance_01 > EPSILON) {
                float d = Math.abs(a * x + b * y + c) / normal_abs;
                distance = Math.max(distance, d);
            }

            if (distance > distance_max) {
                distance_max = distance;
                mid = i;
            }
        }
        if (distance_max < EPSILON || mid == -1)
            return null;
        return mid;
    }

    // Implement Ramer-Douglas-Peucker without recursion since stack space is
    // very limited
    private LinkedList<Integer> simplifyWithoutRecursion() {
        LinkedList<Integer> simplified_points = new LinkedList<Integer>();
        simplified_points.add(0);
        LinkedList<Integer> endpoint = new LinkedList<Integer>();
        endpoint.addLast(N - 1);
        Integer point0 = 0;
        while (!endpoint.isEmpty()) {
            Integer point1 = endpoint.getLast();
            Integer mid = simplifyFindMidPoint(point0, point1);
            // Log.d(TAG, "Simplify "+point0+" - "+point1+" contains "+mid);
            if (mid == null) {
                simplified_points.add(point1);
                point0 = point1;
                endpoint.removeLast();
            } else
                endpoint.addLast(mid);
        }
        return simplified_points;
    }

    @Override
    public void render(Artist artist) {
        float red = Color.red(pen_color) / (float) 0xff;
        float green = Color.green(pen_color) / (float) 0xff;
        float blue = Color.blue(pen_color) / (float) 0xff;
        LineStyle line = new LineStyle();
        line.setColor(red, green, blue);
        FillStyle fill = new FillStyle();
        fill.setColor(red, green, blue);
        switch (tool) {
            case Tool.FOUNTAINPEN:
                renderFountainpen(artist, line, fill);
                break;
            case Tool.BRUSH:
                renderBrush(artist, line, fill);
                break;
            case Tool.PENCIL:
                renderPencil(artist, line);
                break;
            default:
                Assert.fail();
        }
    }

    private void renderFountainpen(Artist artist, LineStyle line, FillStyle fill) {
//		if (N <= 2)
//			renderPencilWithStraightLine(artist, line);
//		else
//			renderFountainpenWithCubicBezier(artist, fill);
        renderFountainWithCubicBezier(artist, fill);
    }

    //artis
    private void renderBrush(Artist artist, LineStyle line, FillStyle fill) {
//		if (N <= 2)
//			renderPencilWithStraightLine(artist, line);
//		else
        renderBrushWithCubicBezier(artist, fill);
    }

    private void renderPencil(Artist artist, LineStyle line) {
//		if (N <= 2)
        renderPencilWithStraightLine(artist, line);
//		else
//			renderPencilWithQuadraticBezier(artist, line);
    }

    private void renderPencilWithStraightLine(Artist artist, LineStyle line) {
        float scaled_pen_thickness = getScaledPenThickness(1f);
        line.setWidth(scaled_pen_thickness);
        line.setCap(LineStyle.Cap.ROUND_END);
        line.setJoin(LineStyle.Join.ROUND_JOIN);
        artist.setLineStyle(line);
        float x = position_x[0];
        float y = position_y[0];
        artist.moveTo(x, y);
        for (int i = 1; i < N; i++) {
            x = position_x[i];
            y = position_y[i];
            artist.lineTo(x, y);
        }
        artist.stroke();
    }

    private void renderPencilWithQuadraticBezier(Artist artist, LineStyle line) {
        float scaled_pen_thickness = getScaledPenThickness(0.5f) * Global.PENCIL_THICKNESS_BASE;
        line.setWidth(scaled_pen_thickness);
        line.setCap(LineStyle.Cap.ROUND_END);
        line.setJoin(LineStyle.Join.ROUND_JOIN);
        artist.setLineStyle(line);

        float x0, x1, x2, x3, y0, y1, y2, y3;

        // the first actual point is treated as a midpoint
        x0 = position_x[0];
        y0 = position_y[0];
        artist.moveTo(x0, y0);

        x1 = position_x[1];
        y1 = position_y[1];
        for (int i = 2; i < N - 1; i++) {
            // (x0,y0) and (x2,y2) are midpoints, (x1,y1) and (x3,y3) are actual points
            x3 = position_x[i];
            y3 = position_y[i];
            x2 = (x1 + x3) / 2f;
            y2 = (y1 + y3) / 2f;
            artist.quadTo(x1, y1, x2, y2);
            x0 = x2;
            y0 = y2;
            x1 = x3;
            y1 = y3;
        }

        // the last actual point is treated as a midpoint
        x2 = position_x[N - 1];
        y2 = position_y[N - 1];
        artist.quadTo(x1, y1, x2, y2);

        artist.stroke();
    }

    private void renderFountainpenWithCubicBezier(Artist artist, FillStyle fill) {
        float scaled_pen_thickness = getScaledPenThickness(1f);
        artist.setFillStyle(fill);

        float x0, x1, x2, x3, y0, y1, y2, y3, p0, p1, p2, p3;
        float vx01, vy01, vx21, vy21;  // unit tangent vectors 0->1 and 1<-2
        float norm;
        float n_x0, n_y0, n_x2, n_y2; // the normals

        // the first actual point is treated as a midpoint
        x0 = position_x[0];
        y0 = position_y[0];
        p0 = pressure[0];

        x1 = position_x[1];
        y1 = position_y[1];
        p1 = pressure[1];
        vx01 = x1 - x0;
        vy01 = y1 - y0;

        ALog.e(TAG, "S<-----renderFountainpenWithCubicBezier, [x0,y0,p0]-->[x1,y1,p1] = [" + x0 + ", " + y0 + ", " + p0 + "]-->[" + x1 + ", " + y1 + ", " + p1 + "]");


        // instead of dividing tangent/norm by two, we multiply norm by 2
        norm = (float) Math.sqrt(vx01 * vx01 + vy01 * vy01 + 0.0000001f) * 2f;
        vx01 = vx01 / norm * scaled_pen_thickness * p0;
        vy01 = vy01 / norm * scaled_pen_thickness * p0;
        n_x0 = vy01;
        n_y0 = -vx01;
        for (int i = 2; i < N - 1; i++) {
            // (x0,y0) and (x2,y2) are midpoints, (x1,y1) and (x3,y3) are actual points
            x3 = position_x[i];
            y3 = position_y[i];
            p3 = pressure[i];
            x2 = (x1 + x3) / 2f;
            y2 = (y1 + y3) / 2f;
            p2 = (p1 + p3) / 2f;
            vx21 = x1 - x2;
            vy21 = y1 - y2;
            norm = (float) Math.sqrt(vx21 * vx21 + vy21 * vy21 + 0.0000001f) * 2f;
            vx21 = vx21 / norm * scaled_pen_thickness * p2;
            vy21 = vy21 / norm * scaled_pen_thickness * p2;
            n_x2 = -vy21;
            n_y2 = vx21;

            artist.moveTo(x0 + n_x0, y0 + n_y0);
            // The + boundary of the stroke
            artist.cubicTo(x1 + n_x0, y1 + n_y0, x1 + n_x2, y1 + n_y2, x2 + n_x2, y2 + n_y2);
            // round out the cap
            artist.cubicTo(x2 + n_x2 - vx21, y2 + n_y2 - vy21, x2 - n_x2 - vx21, y2 - n_y2 - vy21, x2 - n_x2, y2 - n_y2);
            // THe - boundary of the stroke
            artist.cubicTo(x1 - n_x2, y1 - n_y2, x1 - n_x0, y1 - n_y0, x0 - n_x0, y0 - n_y0);
            // round out the other cap
            artist.cubicTo(x0 - n_x0 - vx01, y0 - n_y0 - vy01, x0 + n_x0 - vx01, y0 + n_y0 - vy01, x0 + n_x0, y0 + n_y0);
            artist.fill();

            x0 = x2;
            y0 = y2;
            p0 = p2;
            x1 = x3;
            y1 = y3;
            p1 = p3;
            vx01 = -vx21;
            vy01 = -vy21;
            n_x0 = n_x2;
            n_y0 = n_y2;
        }

        // the last actual point is treated as a midpoint
        x2 = position_x[N - 1];
        y2 = position_y[N - 1];
        p2 = pressure[N - 1];
        vx21 = x1 - x2;
        vy21 = y1 - y2;
        norm = (float) Math.sqrt(vx21 * vx21 + vy21 * vy21 + 0.0000001f) * 2f;
        vx21 = vx21 / norm * scaled_pen_thickness * p2;
        vy21 = vy21 / norm * scaled_pen_thickness * p2;
        n_x2 = -vy21;
        n_y2 = vx21;

        artist.moveTo(x0 + n_x0, y0 + n_y0);
        artist.cubicTo(x1 + n_x0, y1 + n_y0, x1 + n_x2, y1 + n_y2, x2 + n_x2, y2 + n_y2);
        artist.cubicTo(x2 + n_x2 - vx21, y2 + n_y2 - vy21, x2 - n_x2 - vx21, y2 - n_y2 - vy21, x2 - n_x2, y2 - n_y2);
        artist.cubicTo(x1 - n_x2, y1 - n_y2, x1 - n_x0, y1 - n_y0, x0 - n_x0, y0 - n_y0);
        artist.cubicTo(x0 - n_x0 - vx01, y0 - n_y0 - vy01, x0 + n_x0 - vx01, y0 + n_y0 - vy01, x0 + n_x0, y0 + n_y0);
        artist.fill();
    }


    //artis:
    private void renderFountainWithCubicBezier(Artist artist, FillStyle fill) {
        float thickness_weight = 1;    //artis, value should be set in HandwriteView::setPenThickness()
        float b_scale = scale;        //artis, brush scale
        float b_offset_x = offset_x;    //artis, brush offset_x
        float b_offset_y = offset_y;    //artis, brush offset_y
        float pressure_weight = 1.0f;    //artis,

        //float scaled_pen_thickness = getScaledPenThickness( b_scale, pen_thickness * thickness_weight );	//artis: NG
        float scaled_pen_thickness = getScaledPenThickness(1f);

        artist.setFillStyle(fill);

        float x0, x1, x2, x3, y0, y1, y2, y3, p0, p1, p2, p3;
        float vx01, vy01, vx21, vy21;  // unit tangent vectors 0->1 and 1<-2
        float norm;
        float n_x0, n_y0, n_x2, n_y2; // the normals


        // the first actual point is treated as a midpoint
        x0 = position_x[0];
        y0 = position_y[0];
        p0 = pressure[0] * pressure_weight;

        x1 = position_x[1];
        y1 = position_y[1];
        p1 = pressure[1] * pressure_weight;
        vx01 = x1 - x0;
        vy01 = y1 - y0;


//		float norm_weight = 2.0f;	//artis, original value
        float norm_weight = Global.BRUSH_NORM_WEIGHT;    //artis, test value
        // instead of dividing tangent/norm by two, we multiply norm by 2
        norm = (float) Math.sqrt(vx01 * vx01 + vy01 * vy01 + 0.0000001f) * norm_weight;
        vx01 = vx01 / norm * scaled_pen_thickness * p0;
        vy01 = vy01 / norm * scaled_pen_thickness * p0;
        n_x0 = vy01;
        n_y0 = -vx01;
        for (int i = 2; i < N - 1; i++) {
            // (x0,y0) and (x2,y2) are midpoints, (x1,y1) and (x3,y3) are actual points
            x3 = position_x[i];
            y3 = position_y[i];
            p3 = pressure[i] * pressure_weight;    //artis, test value
            x2 = (x1 + x3) / 2f;
            y2 = (y1 + y3) / 2f;
            p2 = (p1 + p3) / 2f;
            vx21 = x1 - x2;
            vy21 = y1 - y2;
            norm = (float) Math.sqrt(vx21 * vx21 + vy21 * vy21 + 0.0000001f) * norm_weight;
            vx21 = vx21 / norm * scaled_pen_thickness * p2;
            vy21 = vy21 / norm * scaled_pen_thickness * p2;
            n_x2 = -vy21;
            n_y2 = vx21;

            artist.moveTo(x0 + n_x0, y0 + n_y0);
            // The + boundary of the stroke
            artist.cubicTo(x1 + n_x0, y1 + n_y0, x1 + n_x2, y1 + n_y2, x2 + n_x2, y2 + n_y2);
            // round out the cap
            artist.cubicTo(x2 + n_x2 - vx21, y2 + n_y2 - vy21, x2 - n_x2 - vx21, y2 - n_y2 - vy21, x2 - n_x2, y2 - n_y2);
            // THe - boundary of the stroke
            artist.cubicTo(x1 - n_x2, y1 - n_y2, x1 - n_x0, y1 - n_y0, x0 - n_x0, y0 - n_y0);
            // round out the other cap
            artist.cubicTo(x0 - n_x0 - vx01, y0 - n_y0 - vy01, x0 + n_x0 - vx01, y0 + n_y0 - vy01, x0 + n_x0, y0 + n_y0);
            artist.fill();

            x0 = x2;
            y0 = y2;
            p0 = p2;
            x1 = x3;
            y1 = y3;
            p1 = p3;
            vx01 = -vx21;
            vy01 = -vy21;
            n_x0 = n_x2;
            n_y0 = n_y2;
        }

        // the last actual point is treated as a midpoint
        x2 = position_x[N - 1];
        y2 = position_y[N - 1];
        p2 = pressure[N - 1] * pressure_weight;
        vx21 = x1 - x2;
        vy21 = y1 - y2;
        norm = (float) Math.sqrt(vx21 * vx21 + vy21 * vy21 + 0.0000001f) * norm_weight;
        vx21 = vx21 / norm * scaled_pen_thickness * p2;
        vy21 = vy21 / norm * scaled_pen_thickness * p2;
        n_x2 = -vy21;
        n_y2 = vx21;

        artist.moveTo(x0 + n_x0, y0 + n_y0);
        artist.cubicTo(x1 + n_x0, y1 + n_y0, x1 + n_x2, y1 + n_y2, x2 + n_x2, y2 + n_y2);
        artist.cubicTo(x2 + n_x2 - vx21, y2 + n_y2 - vy21, x2 - n_x2 - vx21, y2 - n_y2 - vy21, x2 - n_x2, y2 - n_y2);
        artist.cubicTo(x1 - n_x2, y1 - n_y2, x1 - n_x0, y1 - n_y0, x0 - n_x0, y0 - n_y0);
        artist.cubicTo(x0 - n_x0 - vx01, y0 - n_y0 - vy01, x0 + n_x0 - vx01, y0 + n_y0 - vy01, x0 + n_x0, y0 + n_y0);
        artist.fill();
    }

    private void renderBrushWithCubicBezier(Artist artist, FillStyle fill) {
        float thickness_weight = 1;    //artis, value should be set in HandwriteView::setPenThickness()
        float b_scale = scale;        //artis, brush scale
        float b_offset_x = offset_x;    //artis, brush offset_x
        float b_offset_y = offset_y;    //artis, brush offset_y
        float pressure_weight = 1.0f;    //artis,

        //float scaled_pen_thickness = getScaledPenThickness( b_scale, pen_thickness * thickness_weight );	//artis: NG
        float scaled_pen_thickness = getScaledPenThickness(1f);

        artist.setFillStyle(fill);

        float x0, x1, x2, x3, y0, y1, y2, y3, p0, p1, p2, p3;
        float vx01, vy01, vx21, vy21;  // unit tangent vectors 0->1 and 1<-2
        float norm;
        float n_x0, n_y0, n_x2, n_y2; // the normals


        // the first actual point is treated as a midpoint
        x0 = position_x[0];
        y0 = position_y[0];
        if (version >= 3)
            p0 = (float) Math.pow(pressure[0], 3) * pressure_weight;
        else
            p0 = pressure[0] * pressure_weight;

        x1 = position_x[1];
        y1 = position_y[1];
        if (version >= 3)
            p1 = (float) Math.pow(pressure[1], 3) * pressure_weight;
        else
            p1 = pressure[1] * pressure_weight;
        vx01 = x1 - x0;
        vy01 = y1 - y0;

//		float norm_weight = 2.0f;	//artis, original value
        float norm_weight = Global.BRUSH_NORM_WEIGHT;    //artis, test value
        // instead of dividing tangent/norm by two, we multiply norm by 2
        norm = (float) Math.sqrt(vx01 * vx01 + vy01 * vy01 + 0.0000001f) * norm_weight;
        vx01 = vx01 / norm * scaled_pen_thickness * p0;
        vy01 = vy01 / norm * scaled_pen_thickness * p0;
        n_x0 = vy01;
        n_y0 = -vx01;
        for (int i = 2; i < N - 1; i++) {
            // (x0,y0) and (x2,y2) are midpoints, (x1,y1) and (x3,y3) are actual points
            x3 = position_x[i];
            y3 = position_y[i];
            if (version >= 3)
                p3 = (float) Math.pow(pressure[i], 3) * pressure_weight;    //artis, test value
            else
                p3 = pressure[i] * pressure_weight;    //artis, test value

            x2 = (x1 + x3) / 2f;
            y2 = (y1 + y3) / 2f;
            p2 = (p1 + p3) / 2f;
            vx21 = x1 - x2;
            vy21 = y1 - y2;
            norm = (float) Math.sqrt(vx21 * vx21 + vy21 * vy21 + 0.0000001f) * norm_weight;
            vx21 = vx21 / norm * scaled_pen_thickness * p2;
            vy21 = vy21 / norm * scaled_pen_thickness * p2;
            n_x2 = -vy21;
            n_y2 = vx21;

            artist.moveTo(x0 + n_x0, y0 + n_y0);
            // The + boundary of the stroke
            artist.cubicTo(x1 + n_x0, y1 + n_y0, x1 + n_x2, y1 + n_y2, x2 + n_x2, y2 + n_y2);
            // round out the cap
            artist.cubicTo(x2 + n_x2 - vx21, y2 + n_y2 - vy21, x2 - n_x2 - vx21, y2 - n_y2 - vy21, x2 - n_x2, y2 - n_y2);
            // THe - boundary of the stroke
            artist.cubicTo(x1 - n_x2, y1 - n_y2, x1 - n_x0, y1 - n_y0, x0 - n_x0, y0 - n_y0);
            // round out the other cap
            artist.cubicTo(x0 - n_x0 - vx01, y0 - n_y0 - vy01, x0 + n_x0 - vx01, y0 + n_y0 - vy01, x0 + n_x0, y0 + n_y0);
            artist.fill();

            x0 = x2;
            y0 = y2;
            p0 = p2;
            x1 = x3;
            y1 = y3;
            p1 = p3;
            vx01 = -vx21;
            vy01 = -vy21;
            n_x0 = n_x2;
            n_y0 = n_y2;
        }

        // the last actual point is treated as a midpoint
        x2 = position_x[N - 1];
        y2 = position_y[N - 1];
        if (version >= 3)
            p2 = (float) Math.pow(pressure[N - 1], 3) * pressure_weight;
        else
            p2 = pressure[N - 1] * pressure_weight;

        vx21 = x1 - x2;
        vy21 = y1 - y2;
        norm = (float) Math.sqrt(vx21 * vx21 + vy21 * vy21 + 0.0000001f) * norm_weight;
        vx21 = vx21 / norm * scaled_pen_thickness * p2;
        vy21 = vy21 / norm * scaled_pen_thickness * p2;
        n_x2 = -vy21;
        n_y2 = vx21;

        artist.moveTo(x0 + n_x0, y0 + n_y0);
        artist.cubicTo(x1 + n_x0, y1 + n_y0, x1 + n_x2, y1 + n_y2, x2 + n_x2, y2 + n_y2);
        artist.cubicTo(x2 + n_x2 - vx21, y2 + n_y2 - vy21, x2 - n_x2 - vx21, y2 - n_y2 - vy21, x2 - n_x2, y2 - n_y2);
        artist.cubicTo(x1 - n_x2, y1 - n_y2, x1 - n_x0, y1 - n_y0, x0 - n_x0, y0 - n_y0);
        artist.cubicTo(x0 - n_x0 - vx01, y0 - n_y0 - vy01, x0 + n_x0 - vx01, y0 + n_y0 - vy01, x0 + n_x0, y0 + n_y0);
        artist.fill();
    }

    @Override
    public void move(float offsetX, float offsetY) {
        for (int i = 0; i < position_x.length; i++) {
            position_x[i] += transform.inverseX(offsetX);
            position_y[i] += transform.inverseY(offsetY);
        }
        computeBoundingBox();
    }
}


/**
 * REF:
 * 0. http://www.google.com.tw/url?sa=t&rct=j&q=&esrc=s&source=web&cd=19&ved=0CG0QFjAIOAo&url=http%3A%2F%2Fwww.researchgate.net%2Fpublication%2F220585350_Droplet_A_Virtual_Brush_Model_to_Simulate_Chinese_Calligraphy_and_Painting%2Flinks%2F09e41513826791c767000000&ei=_RUpVMK5Ccrk8AXo2IKABw&usg=AFQjCNElt5dqUB8mMZNRwDCUvJ4ABtGdiA&sig2=9A4G-zf8X0plgkUEN6vs1Q
 * Droplet_A_Virtual_Brush_Model_to_Simulate_Chinese_Calligraphy_and_Painting
 * <p>
 * <p>
 * 1. http://www.cse.chalmers.se/~uffe/xjobb/BrushPaintingAlgorithms.pdf
 * <p>
 * 2. http://stackoverflow.com/questions/85993/i-need-an-algorithm-for-rendering-soft-paint-brush-strokes
 * <p>
 * 3. http://www.heathershrewsbury.com/dreu2010/wp-content/uploads/2010/07/PainterlyRenderingWithCurvedBrushStrokesOfMultipleSizes.pdf
 * <p>
 * 4. http://cns.bu.edu/~brumberg/CS580/npr/MAIN/p4.html
 */
