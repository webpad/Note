package name.vbraun.view.write;

import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.Rect;
import android.graphics.RectF;

import junit.framework.Assert;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import ntx.note.artist.Artist;

public class GraphicsNoose extends GraphicsControlPoint {

    /**
     * cp_tl: control point top left
     * cp_tr: control point top right
     * cp_bl: control point bottom left
     * cp_br: control point bottom right
     * cp_c : control point center
     */
    private ControlPoint cp_tl, cp_tr, cp_bl, cp_br, cp_c;

    private Paint pen = new Paint();
    private final Rect rect = new Rect();
    private final RectF rectF = new RectF();
    private int pen_thickness;
    private int pen_color;

    protected GraphicsNoose(Transformation transform, float x, float y, int penThickness, int penColor) {
        super(Tool.NOOSE);
        setTransform(transform);
        cp_tl = new ControlPoint(transform, x, y);
        cp_bl = new ControlPoint(transform, x, y + 1);
        cp_tr = new ControlPoint(transform, x + 1, y);
        cp_br = new ControlPoint(transform, x + 1, y + 1);
        cp_c = new ControlPoint(transform, x, y);
        /**
         * Noose has only the center control point.
         */
        /*
        controlPoints.add(cp_tl);
        controlPoints.add(cp_tr);
        controlPoints.add(cp_br);
        controlPoints.add(cp_bl);
        */
        controlPoints.add(cp_c);
        setPen(penThickness, penColor);
    }

    @Override
    public boolean intersects(RectF r_screen) {
        boolean leftEdgeIntersect = r_screen.left < rect.left && r_screen.right > rect.left
                && r_screen.top > rect.top && r_screen.bottom < rect.bottom;

        boolean rightEdgeIntersect = r_screen.left < rect.right && r_screen.right > rect.right
                && r_screen.top > rect.top && r_screen.bottom < rect.bottom;

        boolean topEdgeIntersect = r_screen.top < rect.top && r_screen.bottom > rect.top
                && r_screen.left > rect.left && r_screen.right < rect.right;

        boolean bottomEdgeIntersect = r_screen.top < rect.bottom && r_screen.bottom > rect.bottom
                && r_screen.left > rect.left && r_screen.right < rect.right;

        if ((rect.right - rect.left) < (r_screen.right - r_screen.left)
                && (rect.bottom - rect.top) < (r_screen.bottom - r_screen.top))
            return r_screen.intersect(rect.left, rect.top, rect.right, rect.bottom);

        return leftEdgeIntersect || rightEdgeIntersect || topEdgeIntersect || bottomEdgeIntersect;
    }

    @Override
    public void draw(Canvas c) {

        if (HandwriterView.getAntiColor()) {
            pen.setARGB(Color.alpha(pen_color), Color.red(pen_color) ^ 0xff,
                    Color.green(pen_color) ^ 0xff, Color.blue(pen_color) ^ 0xff);
        } else {
            pen.setARGB(Color.alpha(pen_color), Color.red(pen_color),
                    Color.green(pen_color), Color.blue(pen_color));
        }

        BitmapShader bitmapShader = getShaderByPenColor(pen_color);
        pen.setShader(bitmapShader);
        pen.setStrokeWidth(pen_thickness);
        pen.setPathEffect(new DashPathEffect(new float[]{10, 20,}, 0));
        computeScreenRect();
        c.clipRect(0, 0, c.getWidth(), c.getHeight(), android.graphics.Region.Op.REPLACE);
        c.drawRect(rect, pen);
    }

    @Override
    public void drawOutLine(Canvas c) {

    }

    @Override
    public void convert_draw(Canvas c) {
    }

    @Override
    protected void drawControlPoints(Canvas canvas) {
        float x = cp_c.screenX();
        float y = cp_c.screenY();
        canvas.drawRect(x - controlPointRadius() / 2, y - controlPointRadius() / 2, x + controlPointRadius() / 2, y + controlPointRadius() / 2, fillPaint);
        canvas.drawRect(x - controlPointRadius() / 2, y - controlPointRadius() / 2, x + controlPointRadius() / 2, y + controlPointRadius() / 2, outlinePaint);
    }

    @Override
    protected void drawAssistLine(Canvas canvas) {
    }

    @Override
    public void writeToStream(DataOutputStream out) throws IOException {
    }

    @Override
    public void render(Artist artist) {
    }

    @Override
    protected ControlPoint initialControlPoint() {
        return cp_br;
    }

    @Override
    public RectF getBoundingBox() {
        return new RectF(rect);
    }

    @Override
    protected void computeBoundingBox() {
    }

    @Override
    void controlPointMoved(ControlPoint point, float newX, float newY) {
        point.x = newX;
        point.y = newY;
        super.controlPointMoved(point, newX, newY);
        if (point == cp_c) {
            float width2 = (cp_br.x - cp_bl.x) / 2;
            float height2 = (cp_tr.y - cp_br.y) / 2;
            cp_br.y = cp_bl.y = cp_c.y - height2;
            cp_tr.y = cp_tl.y = cp_c.y + height2;
            cp_br.x = cp_tr.x = cp_c.x + width2;
            cp_bl.x = cp_tl.x = cp_c.x - width2;
        } else {
            if (point == cp_tl) {
                cp_bl.x = point.x;
                cp_tr.y = point.y;
            } else if (point == cp_tr) {
                cp_br.x = point.x;
                cp_tl.y = point.y;
            } else if (point == cp_br) {
                cp_tr.x = point.x;
                cp_bl.y = point.y;
            } else {
                cp_tl.x = point.x;
                cp_br.y = point.y;
            }

            rectF.top = cp_tl.y;
            rectF.bottom = cp_br.y;
            rectF.left = cp_tl.x;
            rectF.right = cp_br.x;
            rectF.sort();

            cp_c.x = rectF.left + (rectF.right - rectF.left) / 2;
            cp_c.y = rectF.bottom + (rectF.top - rectF.bottom) / 2;
        }
        computeScreenRect();
    }

    @Override
    void restrictShape(ControlPoint point) {
    }

    @Override
    public void move(float offsetX, float offsetY) {

    }

    public void reSetRang(RectF newRange) {
        cp_tl.x = transform.inverseX(newRange.left);
        cp_bl.x = transform.inverseX(newRange.left);
        cp_tr.x = transform.inverseX(newRange.right);
        cp_br.x = transform.inverseX(newRange.right);

        cp_tl.y = transform.inverseY(newRange.top);
        cp_tr.y = transform.inverseY(newRange.top);
        cp_bl.y = transform.inverseY(newRange.bottom);
        cp_br.y = transform.inverseY(newRange.bottom);


        rectF.top = cp_tl.y;
        rectF.bottom = cp_br.y;
        rectF.left = cp_tl.x;
        rectF.right = cp_br.x;
        rectF.sort();

        cp_c.x = rectF.left + (rectF.right - rectF.left) / 2;
        cp_c.y = rectF.bottom + (rectF.top - rectF.bottom) / 2;

        computeScreenRect();
    }

    void setPen(int new_pen_thickness, int new_pen_color) {
        pen_thickness = new_pen_thickness;
        pen_color = new_pen_color;
        pen.setAntiAlias(true);
        pen.setColor(pen_color);
        pen.setStyle(Paint.Style.STROKE);
        pen.setStrokeCap(Paint.Cap.ROUND);
        recompute_bounding_box = true;
    }

    private void computeScreenRect() {
        rectF.bottom = cp_bl.screenY();
        rectF.top = cp_tl.screenY();
        rectF.left = cp_tl.screenX();
        rectF.right = cp_br.screenX();
        rectF.sort();
        rectF.round(rect);
    }

    private ControlPoint oppositeControlPoint(ControlPoint point) {
        if (point == cp_br)
            return cp_tl;
        if (point == cp_bl)
            return cp_tr;
        if (point == cp_tr)
            return cp_bl;
        if (point == cp_tl)
            return cp_br;
        if (point == cp_c)
            return cp_c;
        Assert.fail("Unreachable");
        return null;
    }

}
