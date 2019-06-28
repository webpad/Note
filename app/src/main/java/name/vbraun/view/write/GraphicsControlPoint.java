package name.vbraun.view.write;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Shader;
import android.util.Log;

import junit.framework.Assert;

import java.util.LinkedList;
import java.util.ListIterator;

import ntx.note.Global;

/**
 * Base class for graphics objects that have control points
 * (everything except pen strokes, really).
 *
 * @author vbraun
 */

/**
 * @author vbraun
 */
public abstract class GraphicsControlPoint extends Graphics {
    private static final String TAG = "GraphicsControlPoint";
    private static final int LIGHT_GRAY = Global.grey_A;
    private static final int DARK_GRAY = Global.grey_5;
    public static final int GRAY = Global.grey_8;
    protected int pen_color;
    boolean isNewOne = false;

    void setNewOne(boolean newOne) {
        isNewOne = newOne;
    }

    public class ControlPoint {
        protected float x, y;   // page coordinates

        public ControlPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public ControlPoint(Transformation transform, float x, float y) {
            this.x = transform.inverseX(x);
            this.y = transform.inverseY(y);
        }

        public ControlPoint(final ControlPoint controlPoint) {
            this.x = controlPoint.x;
            this.y = controlPoint.y;
        }

        public void move(float x, float y) {
            GraphicsControlPoint.this.controlPointMoved(this, transform.inverseX(x), transform.inverseY(y));
        }

        public void restrictShape() {
            GraphicsControlPoint.this.restrictShape(this);
        }

        public GraphicsControlPoint getGraphics() {
            return GraphicsControlPoint.this;
        }

        public String toString() {
            return "(" + x + "," + y + ")";
        }

        public float screenX() {
            return transform.applyX(x);
        }

        public float screenY() {
            return transform.applyY(y);
        }

        public ControlPoint copy() {
            return new ControlPoint(x, y);
        }

        public void set(final ControlPoint p) {
            x = p.x;
            y = p.y;
        }
    }

    /**
     * Copy constructor
     *
     * @param graphics
     */
    protected GraphicsControlPoint(final GraphicsControlPoint graphics) {
        super(graphics);
        fillPaint = new Paint(graphics.fillPaint);
        outlinePaint = new Paint(graphics.outlinePaint);
        backupControlPoints = new LinkedList<>();
        for (ControlPoint p : graphics.backupControlPoints) {
            backupControlPoints.add(p.copy());
        }
    }

    /**
     * Derived classes must add their control points to this list
     */
    protected LinkedList<ControlPoint> controlPoints = new LinkedList<ControlPoint>();

    protected LinkedList<ControlPoint> backupControlPoints = null;

    /**
     * Backup the controlPoints so that you can restore them later (e.g. user aborted move)
     */
    @Override
    protected void backup() {
        if (backupControlPoints == null) {
            backupControlPoints = new LinkedList<ControlPoint>();
            for (ControlPoint p : controlPoints)
                backupControlPoints.add(p.copy());
        } else {
            ListIterator<ControlPoint> point_iter = controlPoints.listIterator();
            ListIterator<ControlPoint> backup_iter = backupControlPoints.listIterator();
            while (point_iter.hasNext())
                backup_iter.next().set(point_iter.next());
        }
    }


    /**
     * Restore the control points having calling backup() earlier
     */
    @Override
    protected void restore() {
        if (backupControlPoints == null) {
            Log.e(TAG, "restore() called without backup()");
            return;
        }
        ListIterator<ControlPoint> point_iter = controlPoints.listIterator();
        ListIterator<ControlPoint> backup_iter = backupControlPoints.listIterator();
        while (point_iter.hasNext())
            point_iter.next().set(backup_iter.next());
    }

    @Override
    public void replace(Graphics graphics) {
        super.replace(graphics);
    }

    /**
     * The control point that is active after object creation.
     *
     * @return A ControlPoint or null (indicating that there is none active)
     */
    protected ControlPoint initialControlPoint() {
        return null;
    }

    void controlPointMoved(ControlPoint point, float newX, float newY) {
        recompute_bounding_box = true;
    }

    void restrictShape(ControlPoint point) {

    }

    protected GraphicsControlPoint(@Tool int mTool) {
        super(mTool);
        fillPaint = new Paint();
//		fillPaint.setARGB(0x20, 0xff, 0x0, 0x0);
//		fillPaint.setStyle(Style.FILL);
        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Style.STROKE);
        fillPaint.setAntiAlias(true);
        outlinePaint = new Paint();
//		outlinePaint.setARGB(0x80, 0x0, 0x0, 0x0);
        outlinePaint.setColor(Color.BLACK);
        outlinePaint.setStyle(Style.STROKE);
        outlinePaint.setStrokeWidth(2.5f);
        outlinePaint.setAntiAlias(true);
    }

    /**
     * By default, the bounding box is the box containing the control points
     * inset by this much (which you can override in a derived class).
     *
     * @return
     */
    protected float boundingBoxInset() {
        return -1;
    }

    protected final Paint fillPaint, outlinePaint;

    /**
     * The (maximal) size of a control point
     *
     * @return The size in pixels
     */
    protected float controlPointRadius() {
        return 30;
    }

    protected void drawControlPoints(Canvas canvas) {
        for (ControlPoint p : controlPoints) {
            float x = p.screenX();
            float y = p.screenY();
//			canvas.drawCircle(x, y, controlPointRadius(), fillPaint);
//			canvas.drawCircle(x, y, controlPointRadius(), outlinePaint);
            canvas.drawRect(x - controlPointRadius() / 2, y - controlPointRadius() / 2, x + controlPointRadius() / 2, y + controlPointRadius() / 2, fillPaint);
            canvas.drawRect(x - controlPointRadius() / 2, y - controlPointRadius() / 2, x + controlPointRadius() / 2, y + controlPointRadius() / 2, outlinePaint);
        }
    }

    protected void drawAssistLine(Canvas canvas) {

    }

    @Override
    protected void computeBoundingBox() {
        ListIterator<ControlPoint> iter = controlPoints.listIterator();
        Assert.assertTrue(iter.hasNext()); // must have at least one control point
        ControlPoint p = iter.next();
        float xmin, xmax, ymin, ymax;
        xmin = xmax = transform.applyX(p.x);
        ymin = ymax = transform.applyY(p.y);
        while (iter.hasNext()) {
            p = iter.next();
            float x = p.screenX();
            xmin = Math.min(xmin, x);
            xmax = Math.max(xmax, x);
            float y = p.screenY();
            ymin = Math.min(ymin, y);
            ymax = Math.max(ymax, y);
        }
        bBoxFloat.set(xmin, ymin, xmax, ymax);
        float extra = boundingBoxInset();
        bBoxFloat.inset(extra, extra);
        bBoxFloat.roundOut(bBoxInt);
        recompute_bounding_box = false;
    }

    @Override
    public float distance(float x_screen, float y_screen) {
        return 0;
    }

    protected BitmapShader getShaderByPenColor(int pen_color) {
        this.pen_color = pen_color;
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

    public int getGraphicsColor() {
        return this.pen_color;
    }

}
