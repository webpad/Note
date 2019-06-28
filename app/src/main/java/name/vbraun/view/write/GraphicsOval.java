package name.vbraun.view.write;

import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import junit.framework.Assert;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ListIterator;

import ntx.note.artist.Artist;
import ntx.note.artist.LineStyle;

import static name.vbraun.view.write.Stroke.LINE_THICKNESS_SCALE;

public class GraphicsOval extends GraphicsControlPoint {
    private final static String TAG = "GraphicsOval";
    private final static int RESTRICT_RANGE = 5;

    /**
     * cp_tl: control point top left
     * cp_tr: control point top right
     * cp_bl: control point bottom left
     * cp_br: control point bottom right
     * cp_c : control point center
     */
    private ControlPoint cp_tl, cp_tr, cp_bl, cp_br, cp_c;


    private Paint pen = new Paint();
    private final Paint mAssistLinePen = new Paint();
    private final Rect rect = new Rect();
    private final RectF rectF = new RectF();
    private int pen_thickness;
    private int pen_color;

    /**
     * Construct a new oval
     *
     * @param transform    The current transformation
     * @param x            Screen x coordinate
     * @param y            Screen y coordinate
     * @param penThickness
     * @param penColor
     */
    protected GraphicsOval(Transformation transform, float x, float y, int penThickness, int penColor) {
        super(Tool.OVAL);
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
        setAssistLinePen();
    }

    /**
     * Copy constructor
     */
    protected GraphicsOval(final GraphicsOval oval) {
        super(oval);
        cp_tl = new ControlPoint(oval.cp_tl);
        cp_bl = new ControlPoint(oval.cp_bl);
        cp_tr = new ControlPoint(oval.cp_tr);
        cp_br = new ControlPoint(oval.cp_br);
        cp_c = new ControlPoint(oval.cp_c);
        controlPoints.add(cp_tl);
        controlPoints.add(cp_tr);
        controlPoints.add(cp_br);
        controlPoints.add(cp_bl);
        controlPoints.add(cp_c);
        setPen(oval.pen_thickness, oval.pen_color);
        setAssistLinePen();
    }

    public GraphicsOval(DataInputStream in) throws IOException {
        super(Tool.OVAL);
        int version = in.readInt();
        if (version > 1)
            throw new IOException("Unknown line version!");

        pen_color = in.readInt();
        pen_thickness = in.readInt();
        setPen(pen_thickness, pen_color);
        setAssistLinePen();

        tool = in.readInt();
        if (tool != Tool.OVAL)
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
    public GraphicsOval getBackupGraphics() {
        GraphicsOval backupGraphics = new GraphicsOval(this);
        backupGraphics.restore();
        return backupGraphics;
    }

    @Override
    public GraphicsOval getCloneGraphics() {
        return new GraphicsOval(this);
    }

    @Override
    public void replace(Graphics graphics) {
        GraphicsOval o = (GraphicsOval) graphics;
        cp_tl = new ControlPoint(o.cp_tl);
        cp_bl = new ControlPoint(o.cp_bl);
        cp_tr = new ControlPoint(o.cp_tr);
        cp_br = new ControlPoint(o.cp_br);
        cp_c = new ControlPoint(o.cp_c);
        controlPoints.clear();
        controlPoints.add(cp_tl);
        controlPoints.add(cp_tr);
        controlPoints.add(cp_br);
        controlPoints.add(cp_bl);
        controlPoints.add(cp_c);
        setPen(o.pen_thickness, o.pen_color);
    }

    @Override
    public boolean intersects(RectF r_screen) {
        double ovalCenter_x = rect.centerX();
        double ovalCenter_y = rect.centerY();
        double ovalRadiusA = rect.width() / 2;
        double ovalRadiusB = rect.height() / 2;
        for (int i = 0; i < 360; i++) {
            double tempX = ovalCenter_x + ovalRadiusA * Math.cos(i);
            double tempY = ovalCenter_y + ovalRadiusB * Math.sin(i);
            if (r_screen.left <= tempX && tempX <= r_screen.right
                    && r_screen.top <= tempY && tempY <= r_screen.bottom)
                return true;
        }
        return false;
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
        c.clipRect(0, 0, c.getWidth(), c.getHeight(), android.graphics.Region.Op.REPLACE);
        c.drawOval(rectF, pen);
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
        c.clipRect(0, 0, c.getWidth(), c.getHeight(), android.graphics.Region.Op.REPLACE);
        c.drawOval(rectF, pen);

        pen.setColor(Color.WHITE);
        bitmapShader = getShaderByPenColor(Color.WHITE);
        pen.setShader(bitmapShader);
        pen.setStrokeWidth(pen_thickness);
        c.drawOval(rectF, pen);
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
        c.clipRect(0, 0, c.getWidth(), c.getHeight(), android.graphics.Region.Op.REPLACE);
        c.drawOval(rectF, pen);
    }

    @Override
    protected void drawControlPoints(Canvas canvas) {
        super.drawControlPoints(canvas);
        drawAssistLine(canvas);
    }

    @Override
    protected void drawAssistLine(Canvas canvas) {
        boolean showAssistLine = Math.abs(Math.abs(cp_tl.screenX() - cp_tr.screenX()) - Math.abs(cp_tl.screenY() - cp_bl.screenY())) <= RESTRICT_RANGE;
        if (showAssistLine) {
            mAssistLinePen.setColor(Color.BLACK);
        } else {
            mAssistLinePen.setColor(Color.TRANSPARENT);
        }

        canvas.drawLine(rect.centerX(), rect.top, rect.centerX(), rect.bottom, mAssistLinePen);
        canvas.drawLine(rect.left, rect.centerY(), rect.right, rect.centerY(), mAssistLinePen);
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

        artist.ellipse(cp_c.x, cp_c.y, Math.abs(cp_br.x - cp_c.x), Math.abs(cp_tr.y - cp_c.y));
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
            cp_c.x = newX;
            cp_c.y = newY;
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

    /**
     * Restrict shape to circle.
     *
     * @param point
     */
    @Override
    void restrictShape(ControlPoint point) {
        /**
         * control point is center : just move, no need to restrict shape.
         */
        if (point == cp_c)
            return;

        /**
         * Restrict range : The offset between width and height is <= 2.
         */
        boolean isRestrict = Math.abs(rectF.width() - rectF.height()) <= RESTRICT_RANGE;
        if (!isRestrict)
            return;

        ControlPoint oppositePoint = oppositeControlPoint(point);

        if (rectF.width() > rectF.height()) {
            if (point.y > oppositePoint.y)
                point.y = oppositePoint.y + Math.abs(point.x - oppositePoint.x);
            else
                point.y = oppositePoint.y - Math.abs(point.x - oppositePoint.x);
        } else {
            if (point.x > oppositePoint.x)
                point.x = oppositePoint.x + Math.abs(point.y - oppositePoint.y);
            else
                point.x = oppositePoint.x - Math.abs(point.y - oppositePoint.y);
        }

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

        recompute_bounding_box = true;
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

    private void setAssistLinePen() {
        mAssistLinePen.setAntiAlias(true);
        mAssistLinePen.setColor(Color.BLACK);
        mAssistLinePen.setStyle(Paint.Style.STROKE);
        mAssistLinePen.setStrokeCap(Paint.Cap.ROUND);
        mAssistLinePen.setStrokeWidth(1);
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
