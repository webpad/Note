package name.vbraun.view.write;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.IntDef;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import ntx.note.artist.Artist;

/**
 * Abstract base class for all graphics objects
 *
 * @author vbraun
 */
public abstract class Graphics {
    private static final String TAG = "Graphics";

    @IntDef({
            Tool.FOUNTAINPEN,
            Tool.PENCIL,
            Tool.MOVE,
            Tool.ERASER,
            Tool.TEXT,
            Tool.LINE,
            Tool.ARROW,
            Tool.IMAGE,
            Tool.BRUSH,
            Tool.INFO,
            Tool.RECTANGLE,
            Tool.OVAL,
            Tool.TRIANGLE,
            Tool.NOOSE,
            Tool.MAX_BOUNDS
    })
    @Retention(RetentionPolicy.SOURCE)
    /**
     * ATTENTION :
     * DO NOT change the value of these.
     *
     * It JUST ONLY can be added new one before MAX_BOUNDS.
     * (If this, please remember to increase the value of MAX_BOUNDS.)
     */
    public @interface Tool {
        int FOUNTAINPEN = 0;
        int PENCIL = 1;
        int MOVE = 2;
        int ERASER = 3;
        int TEXT = 4;
        int LINE = 5;
        int ARROW = 6;
        int IMAGE = 7;
        int BRUSH = 8;
        int INFO = 9;
        int RECTANGLE = 10;
        int OVAL = 11;
        int TRIANGLE = 12;
        int NOOSE = 13;
        int MAX_BOUNDS = 14;
    }

    protected @Tool
    int tool;

    /**
     * Copy constructor All derived classes must implement a copy constructor
     *
     * @param graphics
     */
    public Graphics(Graphics graphics) {
        tool = graphics.tool;
        setTransform(graphics.transform);
    }

    protected Graphics(@Tool int tool) {
        this.tool = tool;
    }

    public Graphics getBackupGraphics() {
        return this;
    }

    public Graphics getCloneGraphics(){
        return this;
    }

    public void replace(Graphics graphics){

    }

    public @Tool
    int getTool() {
        return tool;
    }

    protected Transformation transform = new Transformation();
    protected float offset_x = 0f;
    protected float offset_y = 0f;
    protected float scale = 1.0f;

    protected RectF bBoxFloat = new RectF();
    protected Rect bBoxInt = new Rect();
    protected boolean recompute_bounding_box = true;

    public RectF getBoundingBox() {
        if (recompute_bounding_box)
            computeBoundingBox();
        return bBoxFloat;
    }

    public Rect getBoundingBoxRoundOut() {
        if (recompute_bounding_box)
            computeBoundingBox();
        return bBoxInt;
    }

    protected void backup() {

    }

    protected void restore() {

    }

    /**
     * An implementation of computeBoundingBox must set bBoxFloat and bBoxInt
     */
    abstract protected void computeBoundingBox();

    protected void setTransform(Transformation transform) {
        if (this.transform.equals(transform))
            return;
        this.transform.set(transform);
        offset_x = transform.offset_x;
        offset_y = transform.offset_y;
        scale = transform.scale;
        recompute_bounding_box = true;
    }

    abstract public float distance(float x_screen, float y_screen);

    abstract public boolean intersects(RectF r_screen);

    abstract public void draw(Canvas c);

    abstract public void drawOutLine(Canvas c);

    abstract public void convert_draw(Canvas c);

    abstract public void render(Artist artist);

    abstract public void writeToStream(DataOutputStream out) throws IOException;

    abstract public void move(float offsetX, float offsetY);
}
