package name.vbraun.view.write;

import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ntx.note.artist.Artist;
import ntx.note.artist.LineStyle;

import static name.vbraun.view.write.Stroke.LINE_THICKNESS_SCALE;

public class GraphicsLine extends GraphicsControlPoint {
    private static final String TAG = "GraphicsLine";

    private ControlPoint p0, p1;
    private Paint pen = new Paint();
    private int pen_thickness;
    private int pen_color;

    /**
     * Construct a new line
     *
     * @param transform    The current transformation
     * @param x            Screen x coordinate
     * @param y            Screen y coordinate
     * @param penThickness
     * @param penColor
     */
    protected GraphicsLine(Transformation transform, float x, float y, int penThickness, int penColor) {
        super(Tool.LINE);
        setTransform(transform);
        p0 = new ControlPoint(transform, x, y);
        p1 = new ControlPoint(transform, x, y);
        controlPoints.add(p0);
        controlPoints.add(p1);
        setPen(penThickness, penColor);
    }

    /**
     * Copy constructor
     *
     * @param line
     */
    protected GraphicsLine(final GraphicsLine line) {
        super(line);
        p0 = new ControlPoint(line.p0);
        p1 = new ControlPoint(line.p1);
        controlPoints.add(p0);
        controlPoints.add(p1);
        setPen(line.pen_thickness, line.pen_color);
    }

    public GraphicsLine(DataInputStream in) throws IOException {
        super(Tool.LINE);
        int version = in.readInt();
        if (version > 1)
            throw new IOException("Unknown line version!");
        pen_color = in.readInt();
        pen_thickness = in.readInt();
        tool = in.readInt();
        if (tool != Tool.LINE)
            throw new IOException("Unknown tool type!");

        p0 = new ControlPoint(in.readFloat(), in.readFloat());
        p1 = new ControlPoint(in.readFloat(), in.readFloat());
        controlPoints.add(p0);
        controlPoints.add(p1);
        setPen(pen_thickness, pen_color);
    }

    @Override
    public GraphicsLine getBackupGraphics() {
        GraphicsLine backupGraphics = new GraphicsLine(this);
        backupGraphics.restore();
        return backupGraphics;
    }

    @Override
    public GraphicsLine getCloneGraphics() {
        return new GraphicsLine(this);
    }

    @Override
    public void replace(Graphics graphics) {
        GraphicsLine l = (GraphicsLine) graphics;
        this.p0 = new ControlPoint(l.p0);
        this.p1 = new ControlPoint(l.p1);
        controlPoints.clear();
        controlPoints.add(p0);
        controlPoints.add(p1);
        setPen(l.pen_thickness, l.pen_color);
    }

    @Override
    void controlPointMoved(ControlPoint point, float newX, float newY) {
        point.x = newX;
        point.y = newY;
        super.controlPointMoved(point, newX, newY);
    }

    @Override
    public boolean intersects(RectF screenRect) {
        float x0 = p0.screenX();
        float x1 = p1.screenX();
        float y0 = p0.screenY();
        float y1 = p1.screenY();
        return lineIntersectsRectF(x0, y0, x1, y1, screenRect);
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
        final float scaled_pen_thickness = getScaledPenThickness();
        pen.setStrokeWidth(scaled_pen_thickness);
        float x0, x1, y0, y1;
        // note: we offset the first point by 1/10 pixel since android does not draw lines with start=end
        x0 = p0.screenX() + 0.1f;
        x1 = p1.screenX();
        y0 = p0.screenY();
        y1 = p1.screenY();
        // Log.v(TAG, "Line ("+x0+","+y0+") -> ("+x1+","+y1+"), thickness="+scaled_pen_thickness);
        c.drawLine(x0, y0, x1, y1, pen);
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
        pen.setStrokeWidth(getScaledPenThickness() + 2);
        float x0, x1, y0, y1;
        x0 = p0.screenX() + 0.1f;
        x1 = p1.screenX();
        y0 = p0.screenY();
        y1 = p1.screenY();
        c.drawLine(x0, y0, x1, y1, pen);

        pen.setColor(Color.WHITE);
        bitmapShader = getShaderByPenColor(Color.WHITE);
        pen.setShader(bitmapShader);
        pen.setStrokeWidth(getScaledPenThickness());
        c.drawLine(x0, y0, x1, y1, pen);
    }

    @Override
    public void convert_draw(Canvas c) {
        final float scaled_pen_thickness = getScaledPenThickness();
        pen = new Paint();
        pen.setStyle(Paint.Style.STROKE);
        pen.setColor(pen_color);
        pen.setStrokeJoin(Paint.Join.ROUND);
        pen.setStrokeCap(Paint.Cap.ROUND);
        pen.setStrokeWidth(scaled_pen_thickness);
        float x0, x1, y0, y1;
        // note: we offset the first point by 1/10 pixel since android does not draw lines with start=end
        x0 = p0.screenX() + 0.1f;
        x1 = p1.screenX();
        y0 = p0.screenY();
        y1 = p1.screenY();
        // Log.v(TAG, "Line ("+x0+","+y0+") -> ("+x1+","+y1+"), thickness="+scaled_pen_thickness);
        c.drawLine(x0, y0, x1, y1, pen);
    }

    @Override
    public void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(1);  // protocol #1
        out.writeInt(pen_color);
        out.writeInt(pen_thickness);
        out.writeInt(tool);
        out.writeFloat(p0.x);
        out.writeFloat(p0.y);
        out.writeFloat(p1.x);
        out.writeFloat(p1.y);
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
        artist.moveTo(p0.x, p0.y);
        artist.lineTo(p1.x, p1.y);
        artist.stroke();
    }

    @Override
    protected ControlPoint initialControlPoint() {
        return p1;
    }

    @Override
    protected float boundingBoxInset() {
        return -getScaledPenThickness() / 2 - 1;
    }

    void setPen(int new_pen_thickness, int new_pen_color) {
        pen_thickness = new_pen_thickness;
        pen_color = new_pen_color;
        pen.setAntiAlias(true);
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

    private static boolean lineIntersectsRectF(float x0, float y0, float x1, float y1, RectF rect) {
        // f(x,y) = (y1-y0)*x - (x1-x0)*y + x1*y0-x0*y1
        float dx = x1 - x0;
        float dy = y1 - y0;
        float constant = x1 * y0 - x0 * y1;
        float f1 = dy * rect.left - dx * rect.bottom + constant;
        float f2 = dy * rect.left - dx * rect.top + constant;
        float f3 = dy * rect.right - dx * rect.bottom + constant;
        float f4 = dy * rect.right - dx * rect.top + constant;
        boolean allNegative = (f1 < 0) && (f2 < 0) && (f3 < 0) && (f4 < 0);
        boolean allPositive = (f1 > 0) && (f2 > 0) && (f3 > 0) && (f4 > 0);
        if (allNegative || allPositive) return false;
        // rect intersects the infinite line, check segment endpoints
        float xMin = Math.min(x0, x1);
        if (xMin > rect.right) return false;
        float xMax = Math.max(x0, x1);
        if (xMax < rect.left) return false;
        float yMin = Math.min(y0, y1);
        if (yMin > rect.bottom) return false;
        float yMax = Math.max(y0, y1);
        if (yMax < rect.top) return false;
        return true;
    }

    @Override
    public void move(float offsetX, float offsetY) {
        p0.x += transform.inverseX(offsetX);
        p0.y += transform.inverseY(offsetY);

        p1.x += transform.inverseX(offsetX);
        p1.y += transform.inverseY(offsetY);

        computeBoundingBox();
    }
}
