package name.vbraun.view.write;

import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ListIterator;

import ntx.note.artist.Artist;
import ntx.note.artist.LineStyle;

import static name.vbraun.view.write.Stroke.LINE_THICKNESS_SCALE;

public class GraphicsTriangle extends GraphicsControlPoint {
    private final static String TAG = "GraphicsTriangle";

    /**
     * cp_tl: control point top left
     * cp_tr: control point top right
     * cp_bl: control point bottom left
     * cp_br: control point bottom right
     * cp_c : control point center
     * cp_tc : control point top center
     */
    private ControlPoint cp_tl, cp_tr, cp_bl, cp_br, cp_c;


    private Paint pen = new Paint();
    private final Rect rect = new Rect();
    private final RectF rectF = new RectF();
    private int pen_thickness;
    private int pen_color;

    /**
     * Construct a new triangle
     *
     * @param transform    The current transformation
     * @param x            Screen x coordinate
     * @param y            Screen y coordinate
     * @param penThickness
     * @param penColor
     */
    protected GraphicsTriangle(Transformation transform, float x, float y, int penThickness, int penColor) {
        super(Tool.TRIANGLE);
        setTransform(transform);
        cp_tl = new ControlPoint(transform, x, y);
        cp_bl = new ControlPoint(transform, x, y + 1);
        cp_tr = new ControlPoint(transform, x + 1, y);
        cp_br = new ControlPoint(transform, x + 1, y + 1);
        cp_c = new ControlPoint(transform, x, y);
        controlPoints.add(cp_tl);
        controlPoints.add(cp_tr);
        controlPoints.add(cp_br);
        controlPoints.add(cp_bl);
        controlPoints.add(cp_c);
        setPen(penThickness, penColor);
    }

    /**
     * Copy constructor
     */
    protected GraphicsTriangle(final GraphicsTriangle triangle) {
        super(triangle);
        cp_tl = new ControlPoint(triangle.cp_tl);
        cp_bl = new ControlPoint(triangle.cp_bl);
        cp_tr = new ControlPoint(triangle.cp_tr);
        cp_br = new ControlPoint(triangle.cp_br);
        cp_c = new ControlPoint(triangle.cp_c);
        controlPoints.add(cp_tl);
        controlPoints.add(cp_tr);
        controlPoints.add(cp_br);
        controlPoints.add(cp_bl);
        controlPoints.add(cp_c);
        setPen(triangle.pen_thickness, triangle.pen_color);
    }

    public GraphicsTriangle(DataInputStream in) throws IOException {
        super(Tool.TRIANGLE);
        int version = in.readInt();
        if (version > 1)
            throw new IOException("Unknown line version!");

        pen_color = in.readInt();
        pen_thickness = in.readInt();
        setPen(pen_thickness, pen_color);

        tool = in.readInt();
        if (tool != Tool.TRIANGLE)
            throw new IOException("Unknown tool type!");

        float left = in.readFloat();
        float right = in.readFloat();
        float top = in.readFloat();
        float bottom = in.readFloat();

        cp_bl = new ControlPoint(transform, left, bottom);
        cp_br = new ControlPoint(transform, right, bottom);
        cp_tl = new ControlPoint(transform, left, top);
        cp_tr = new ControlPoint(transform, right, top);
        cp_c = new ControlPoint(transform, (left + right) / 2, (top + bottom) / 2);
        controlPoints.add(cp_tl);
        controlPoints.add(cp_tr);
        controlPoints.add(cp_br);
        controlPoints.add(cp_bl);
        controlPoints.add(cp_c);
    }

    @Override
    public GraphicsTriangle getBackupGraphics() {
        GraphicsTriangle backupGraphics = new GraphicsTriangle(this);
        backupGraphics.restore();
        return backupGraphics;
    }

    @Override
    public GraphicsTriangle getCloneGraphics() {
        return new GraphicsTriangle(this);
    }

    @Override
    public void replace(Graphics graphics) {
        GraphicsTriangle t = (GraphicsTriangle) graphics;
        cp_tl = new ControlPoint(t.cp_tl);
        cp_bl = new ControlPoint(t.cp_bl);
        cp_tr = new ControlPoint(t.cp_tr);
        cp_br = new ControlPoint(t.cp_br);
        cp_c = new ControlPoint(t.cp_c);
        controlPoints.clear();
        controlPoints.add(cp_tl);
        controlPoints.add(cp_tr);
        controlPoints.add(cp_br);
        controlPoints.add(cp_bl);
        controlPoints.add(cp_c);
        setPen(t.pen_thickness, t.pen_color);
    }

    @Override
    public boolean intersects(RectF r_screen) {

        Point erase_tl, erase_tr, erase_bl, erase_br;

        erase_tl = new Point();
        erase_tr = new Point();
        erase_bl = new Point();
        erase_br = new Point();

        erase_tl.x = (int) r_screen.left;
        erase_tl.y = (int) r_screen.top;

        erase_tr.x = (int) r_screen.right;
        erase_tr.y = (int) r_screen.top;

        erase_bl.x = (int) r_screen.left;
        erase_bl.y = (int) r_screen.bottom;

        erase_br.x = (int) r_screen.right;
        erase_br.y = (int) r_screen.bottom;

        //triangle point
        Point point1_draw, point2_draw, point3_draw;

        point1_draw = new Point();
        point2_draw = new Point();
        point3_draw = new Point();

        point1_draw.x = (rect.right + rect.left) / 2;
        point1_draw.y = rect.top;

        point2_draw.x = rect.right;
        point2_draw.y = rect.bottom;

        point3_draw.x = rect.left;
        point3_draw.y = rect.bottom;

        boolean top = (IsIntersecting(erase_tl, erase_tr, point1_draw, point2_draw) ||
                IsIntersecting(erase_tl, erase_tr, point2_draw, point3_draw) ||
                IsIntersecting(erase_tl, erase_tr, point3_draw, point1_draw));
        boolean bottom = (IsIntersecting(erase_bl, erase_br, point1_draw, point2_draw) ||
                IsIntersecting(erase_bl, erase_br, point2_draw, point3_draw) ||
                IsIntersecting(erase_bl, erase_br, point3_draw, point1_draw));
        boolean left = (IsIntersecting(erase_tl, erase_bl, point1_draw, point2_draw) ||
                IsIntersecting(erase_tl, erase_bl, point2_draw, point3_draw) ||
                IsIntersecting(erase_tl, erase_bl, point3_draw, point1_draw));
        boolean right = (IsIntersecting(erase_tr, erase_br, point1_draw, point2_draw) ||
                IsIntersecting(erase_tr, erase_br, point2_draw, point3_draw) ||
                IsIntersecting(erase_tr, erase_br, point3_draw, point1_draw));
        if (Math.abs(rect.right - rect.left) < Math.abs(r_screen.right - r_screen.left)
                && Math.abs(rect.bottom - rect.top) < Math.abs(r_screen.bottom - r_screen.top))
            return r_screen.intersect(rect.left, rect.top, rect.right, rect.bottom);

        return top || bottom || left || right;
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
        computeScreenRect();

        Point point1_draw = new Point();
        Point point2_draw = new Point();
        Point point3_draw = new Point();

        point1_draw.x = (rect.right + rect.left) / 2;
        point1_draw.y = rect.top;

        point2_draw.x = rect.right;
        point2_draw.y = rect.bottom;

        point3_draw.x = rect.left;
        point3_draw.y = rect.bottom;

        Path path = new Path();

        path.moveTo(point1_draw.x, point1_draw.y);
        path.lineTo(point2_draw.x, point2_draw.y);
        path.lineTo(point3_draw.x, point3_draw.y);
        path.lineTo(point1_draw.x, point1_draw.y);
        path.close();

        c.drawPath(path, pen);

    }

    @Override
    public void drawOutLine(Canvas c) {
        if (HandwriterView.getAntiColor()) {
            pen.setARGB(Color.alpha(pen_color), Color.red(pen_color) ^ 0xff,
                    Color.green(pen_color) ^ 0xff, Color.blue(pen_color) ^ 0xff);
        } else {
            pen.setARGB(Color.alpha(pen_color), Color.red(pen_color),
                    Color.green(pen_color), Color.blue(pen_color));
        }

        BitmapShader bitmapShader = getShaderByPenColor(pen_color);
        pen.setShader(bitmapShader);
        pen.setStrokeWidth(pen_thickness + 2);
        computeScreenRect();

        Point point1_draw = new Point();
        Point point2_draw = new Point();
        Point point3_draw = new Point();

        point1_draw.x = (rect.right + rect.left) / 2;
        point1_draw.y = rect.top;

        point2_draw.x = rect.right;
        point2_draw.y = rect.bottom;

        point3_draw.x = rect.left;
        point3_draw.y = rect.bottom;

        Path path = new Path();

        path.moveTo(point1_draw.x, point1_draw.y);
        path.lineTo(point2_draw.x, point2_draw.y);
        path.lineTo(point3_draw.x, point3_draw.y);
        path.lineTo(point1_draw.x, point1_draw.y);
        path.close();

        c.drawPath(path, pen);

        pen.setColor(Color.WHITE);
        bitmapShader = getShaderByPenColor(Color.WHITE);
        pen.setShader(bitmapShader);
        pen.setStrokeWidth(pen_thickness);
        c.drawPath(path, pen);
    }

    @Override
    public void convert_draw(Canvas c) {
        pen = new Paint();
        pen.setStyle(Paint.Style.STROKE);
        pen.setColor(pen_color);
        pen.setStrokeJoin(Paint.Join.ROUND);
        pen.setStrokeCap(Paint.Cap.ROUND);
        pen.setStrokeWidth(pen_thickness);

        computeScreenRect();

        Point point1_draw = new Point();
        Point point2_draw = new Point();
        Point point3_draw = new Point();

        point1_draw.x = (rect.right + rect.left) / 2;
        point1_draw.y = rect.top;

        point2_draw.x = rect.right;
        point2_draw.y = rect.bottom;

        point3_draw.x = rect.left;
        point3_draw.y = rect.bottom;

        Path path = new Path();

        path.moveTo(point1_draw.x, point1_draw.y);
        path.lineTo(point2_draw.x, point2_draw.y);
        path.lineTo(point3_draw.x, point3_draw.y);
        path.lineTo(point1_draw.x, point1_draw.y);
        path.close();

        c.drawPath(path, pen);
    }

    @Override
    public void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(1);  // protocol #1
        out.writeInt(pen_color);
        out.writeInt(pen_thickness);
        out.writeInt(tool);
        out.writeFloat(cp_tl.x);
        out.writeFloat(cp_br.x);
        out.writeFloat(cp_tl.y);
        out.writeFloat(cp_br.y);
    }

    @Override
    public void render(Artist artist) {
        LineStyle line = new LineStyle();
        float scaled_pen_thickness = getScaledPenThickness(1f);
        line.setWidth(scaled_pen_thickness);
        line.setCap(LineStyle.Cap.ROUND_END);
        line.setJoin(LineStyle.Join.ROUND_JOIN);
        float red = Color.red(pen_color) / (float) 0xff;
        float green = Color.green(pen_color) / (float) 0xff;
        float blue = Color.blue(pen_color) / (float) 0xff;
        line.setColor(red, green, blue);
        artist.setLineStyle(line);
        artist.moveTo((cp_tl.x + cp_tr.x) / 2, cp_tl.y);
        artist.lineTo(cp_br.x, cp_br.y);
        artist.stroke();

        artist.moveTo(cp_br.x, cp_br.y);
        artist.lineTo(cp_bl.x, cp_bl.y);
        artist.stroke();

        artist.moveTo(cp_bl.x, cp_bl.y);
        artist.lineTo((cp_tl.x + cp_tr.x) / 2, cp_tl.y);
        artist.stroke();
    }

    @Override
    protected ControlPoint initialControlPoint() {
        return cp_br;
    }

    @Override
    protected void computeBoundingBox() {
        ListIterator<ControlPoint> iter = controlPoints.listIterator();
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
    protected float boundingBoxInset() {
        return -getScaledPenThickness() / 2 - 1;
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

    // this computes the argument to Paint.setStrokeWidth()
    private float getScaledPenThickness() {
        return scale * pen_thickness * LINE_THICKNESS_SCALE;
    }

    public float getScaledPenThickness(float scale) {
        return Stroke.getScaledPenThickness(scale, pen_thickness);
    }

    private void computeScreenRect() {
        rectF.bottom = cp_bl.screenY();
        rectF.top = cp_tl.screenY();
        rectF.left = cp_tl.screenX();
        rectF.right = cp_br.screenX();
//        rectF.sort();
        rectF.round(rect);
    }

    private boolean IsIntersecting(Point a, Point b, Point c, Point d) {
        float denominator = ((b.x - a.x) * (d.y - c.y)) - ((b.y - a.y) * (d.x - c.x));
        float numerator1 = ((a.y - c.y) * (d.x - c.x)) - ((a.x - c.x) * (d.y - c.y));
        float numerator2 = ((a.y - c.y) * (b.x - a.x)) - ((a.x - c.x) * (b.y - a.y));
        if (denominator == 0) return numerator1 == 0 && numerator2 == 0;
        float r = numerator1 / denominator;
        float s = numerator2 / denominator;
        return (r >= 0 && r <= 1) && (s >= 0 && s <= 1);
    }

    @Override
    public void move(float offsetX, float offsetY) {
        cp_c.x += transform.inverseX(offsetX);
        cp_c.y += transform.inverseY(offsetY);
        float width2 = (cp_br.x - cp_bl.x) / 2;
        float height2 = (cp_tr.y - cp_br.y) / 2;
        cp_br.y = cp_bl.y = cp_c.y - height2;
        cp_tr.y = cp_tl.y = cp_c.y + height2;
        cp_br.x = cp_tr.x = cp_c.x + width2;
        cp_bl.x = cp_tl.x = cp_c.x - width2;

        computeBoundingBox();
    }
}
