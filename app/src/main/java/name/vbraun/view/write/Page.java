package name.vbraun.view.write;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import junit.framework.Assert;

import org.greenrobot.eventbus.EventBus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.UUID;

import name.vbraun.lib.pen.Hardware;
import name.vbraun.view.write.Graphics.Tool;
import ntx.note.ALog;
import ntx.note.CallbackEvent;
import ntx.note.Global;
import ntx.note.artist.Artist;
import ntx.note.data.Book;
import ntx.note.data.Bookshelf;
import ntx.note.data.TagManager;
import ntx.note.data.TagManager.TagSet;


public class Page {
    private static final String TAG = "Page";
    private static final int PAGE_VERSION = 10;

    /**
     * Predict time factor
     */
    private static final float PREDICT_TIME_FACTOR_PENCIL = 0.026f;
    private static final float PREDICT_TIME_FACTOR_PRESSURE_PEN = 0.036f;
    private static final float PREDICT_TIME_FACTOR_LINE_RECTANGE = 0.9f;
    private static final float PREDICT_TIME_FACTOR_OVAL_TRIANGLE = 1.2f;

    protected final Background background = new Background();

    public static Page exceptionFixedPage;

    private TagManager tagManager;

    // persistent data
    private int version; // page version.
    protected UUID uuid;  // unique identifier
    public final LinkedList<GraphicsImage> images = new LinkedList<GraphicsImage>();
    public final LinkedList<Stroke> strokes = new LinkedList<Stroke>();
    // lineArt contains straight lines, arrows, etc.
    public final LinkedList<GraphicsLine> lineArt = new LinkedList<GraphicsLine>();
    public final LinkedList<GraphicsRectangle> rectangleArt = new LinkedList<GraphicsRectangle>();
    public final LinkedList<GraphicsOval> ovalArt = new LinkedList<GraphicsOval>();
    public final LinkedList<GraphicsTriangle> triangleArt = new LinkedList<GraphicsTriangle>();
    public final LinkedList<GraphicsNoose> nooseArt = new LinkedList<>();
    private SelectedObjects mSelectedObjects = new SelectedObjects();
    public final TagSet tags;
    private String mMainTag = "";
    protected float aspect_ratio = AspectRatio.Table[2].ratio;
    protected boolean is_readonly = false;

    protected Paper.Type paper_type = Paper.Type.EMPTY;
    protected String paper_path = "na";
    protected TextBox backgroundText = new TextBox(Tool.TEXT);

    // coordinate transformation Stroke -> screen
    protected Transformation transformation = new Transformation();

    protected boolean modified = false;
    protected boolean auto_modified = false;

    public volatile boolean isCanvasDrawCompleted = false;

    private boolean doInvalidate = true;

    private final RectF mRectF = new RectF();

    private EventBus mEventBus = EventBus.getDefault();
    private SimpleDateFormat s = new SimpleDateFormat("HH:mm:ss.SSS");

    private Runnable intervalInvalidateDrawRunnable = new Runnable() {
        @Override
        public void run() {
            CallbackEvent event = new CallbackEvent();
            event.setMessage(CallbackEvent.DO_DRAW_VIEW_INVALIDATE);
            mEventBus.post(event);
            mIntervalInvalidateDrawHandler.postDelayed(this, 500);
        }
    };

    private Handler mIntervalInvalidateDrawHandler = new Handler(Looper.getMainLooper());

    public TagSet getTags() {
        return tags;
    }

    public void setMainTag(String mainTag) {
        this.mMainTag = mainTag;
        this.modified = true;
        this.auto_modified = true;
        CallbackEvent event = new CallbackEvent();
        event.setMessage(CallbackEvent.UPDATE_PAGE_TITLE);
        mEventBus.post(event);
    }

    public String getMainTag() {
        return this.mMainTag;
    }

    public UUID getUUID() {
        return uuid;
    }

    public LinkedList<UUID> getBlobUUIDs() {
        LinkedList<UUID> blobs = new LinkedList<UUID>();
        for (GraphicsImage image : images)
            blobs.add(image.getUuid());
        return blobs;
    }

    public boolean isEmpty() {
        return strokes.isEmpty() && lineArt.isEmpty() && rectangleArt.isEmpty() && ovalArt.isEmpty() && triangleArt.isEmpty() && images.isEmpty();
    }

    public int objectsCount() {
        return strokes.size() + lineArt.size() + rectangleArt.size() + ovalArt.size() + triangleArt.size() + images.size();
    }

    public int objectsDrawTimePredict() {
        int predictTime = 0;
        for (Stroke stroke : strokes) {
            switch (stroke.tool) {
                case Tool.PENCIL:
                    predictTime = predictTime + pencilStrokeDrawPredictTime(stroke.N);
                    break;
                case Tool.FOUNTAINPEN:
                    predictTime = predictTime + fountainPenStrokeDrawPredictTime(stroke.N);
                    break;
                case Tool.BRUSH:
                    predictTime = predictTime + brushStrokeDrawPredictTime(stroke.N);
                    break;
            }
        }
        predictTime = predictTime
                + (int) Math.ceil(lineArt.size() * PREDICT_TIME_FACTOR_LINE_RECTANGE)
                + (int) Math.ceil(rectangleArt.size() * PREDICT_TIME_FACTOR_LINE_RECTANGE)
                + (int) Math.ceil(ovalArt.size() * PREDICT_TIME_FACTOR_OVAL_TRIANGLE)
                + (int) Math.ceil(triangleArt.size() * PREDICT_TIME_FACTOR_OVAL_TRIANGLE);

        return predictTime;
    }

    private int pencilStrokeDrawPredictTime(int pathLength) {
        return (int) Math.ceil(pathLength * PREDICT_TIME_FACTOR_PENCIL);
    }

    private int fountainPenStrokeDrawPredictTime(int pathLength) {
        return (int) Math.ceil(pathLength * PREDICT_TIME_FACTOR_PRESSURE_PEN);
    }

    private int brushStrokeDrawPredictTime(int pathLength) {
        return (int) Math.ceil(pathLength * PREDICT_TIME_FACTOR_PRESSURE_PEN);
    }

    /**
     * Get the smallest rectangle containing the most recent stroke in page coordinates.
     *
     * @return A RectF or null if the page is empty.
     */
    public RectF getLastStrokeRect() {
        if (strokes.isEmpty())
            return null;
        return strokes.getLast().getEnvelopingRect();
    }

    public void touch() {
        modified = true;
        auto_modified = true;
    }

    public void setModified(boolean b) {
        modified = b;
        auto_modified = b;
    }

    public boolean isModified(boolean autoSave) {
        if (autoSave)
            return auto_modified;
        else
            return modified;
    }

    void drawNotInvalidate() {
        synchronized (Page.class) {
            doInvalidate = false;
        }
    }

    public float getAspectRatio() {
        return aspect_ratio;
    }

    public Paper.Type getPaperType() {
        return paper_type;
    }

    public int getBackgroundColor() {
        return background.getPaperColour();
    }

    public boolean isReadonly() {
        return is_readonly;
    }

    public void setReadonly(boolean ro) {
        is_readonly = ro;
        modified = true;
        auto_modified = true;
    }

    public void setPaperType(Paper.Type type, String paperPath) {
        paper_type = type;
        paper_path = paperPath;
        modified = true;
        auto_modified = true;
        background.setPaperType(paper_type);
        background.setPaperPath(paper_path);
    }

    public void setBackgroundColor(int color) {
        background.setPaperColour(color);
    }

    public void setAspectRatio(float aspect) {
        aspect_ratio = aspect;
        modified = true;
        auto_modified = true;
        background.setAspectRatio(aspect_ratio);
    }

    protected void setTransform(float dx, float dy, float s) {
        transformation.offset_x = dx;
        transformation.offset_y = dy;
        transformation.scale = s;
        setTransformApply();
    }

    protected void setTransform(Transformation newTrans) {
        transformation.offset_x = newTrans.offset_x;
        transformation.offset_y = newTrans.offset_y;
        transformation.scale = newTrans.scale;
        setTransformApply();
    }

    /**
     * the common code of the setTransform(...) methods
     */
    private void setTransformApply() {
        for (Stroke stroke : strokes)
            stroke.setTransform(transformation);
        for (GraphicsControlPoint line : lineArt)
            line.setTransform(transformation);
        for (GraphicsControlPoint rectangle : rectangleArt)
            rectangle.setTransform(transformation);
        for (GraphicsControlPoint oval : ovalArt)
            oval.setTransform(transformation);
        for (GraphicsControlPoint triangle : triangleArt)
            triangle.setTransform(transformation);
        for (GraphicsImage image : images)
            image.setTransform(transformation);
        for (GraphicsNoose noose : nooseArt)
            noose.setTransform(transformation);
    }

    // set transform but clamp the offset such that the page stays visible
    protected void setTransform(float dx, float dy, float s, Canvas canvas) {
        float W = canvas.getWidth();
        float H = canvas.getHeight();
        dx = Math.min(dx, 2 * W / 3);
        dx = Math.max(dx, W / 3 - s * aspect_ratio);
        dy = Math.min(dy, 2 * H / 3);
        dy = Math.max(dy, H / 3 - s);
        setTransform(dx, dy, s);
    }

    protected void setTransform(Transformation newTrans, Canvas canvas) {
        setTransform(newTrans.offset_x, newTrans.offset_y, newTrans.scale, canvas);
    }


    protected Transformation getTransform() {
        return transformation;
    }

    public void addStroke(Stroke s) {
        strokes.add(s);
        s.setTransform(getTransform());
        modified = true;
        auto_modified = true;
    }

    public void removeStroke(Stroke s) {
        strokes.remove(s);
        modified = true;
        auto_modified = true;
    }

    public void addLine(GraphicsLine line) {
        lineArt.add(line);
        line.setTransform(getTransform());
        modified = true;
        auto_modified = true;
    }

    public void removeLine(GraphicsLine line) {
        lineArt.remove(line);
        modified = true;
        auto_modified = true;
    }

    public void addRectangle(GraphicsRectangle rectangle) {
        rectangleArt.add(rectangle);
        rectangle.setTransform(getTransform());
        modified = true;
        auto_modified = true;
    }

    public void removeRectangle(GraphicsRectangle rectangle) {
        rectangleArt.remove(rectangle);
        modified = true;
        auto_modified = true;
    }

    public void addOval(GraphicsOval oval) {
        ovalArt.add(oval);
        oval.setTransform(getTransform());
        modified = true;
        auto_modified = true;
    }

    public void removeOval(GraphicsOval oval) {
        ovalArt.remove(oval);
        modified = true;
        auto_modified = true;
    }

    public void addTriangle(GraphicsTriangle triangle) {
        triangleArt.add(triangle);
        triangle.setTransform(getTransform());
        modified = true;
        auto_modified = true;
    }

    public void removeTriangle(GraphicsTriangle triangle) {
        triangleArt.remove(triangle);
        modified = true;
        auto_modified = true;
    }

    public void addImage(GraphicsImage image) {
        images.add(image);
        image.setTransform(getTransform());
        modified = true;
        auto_modified = true;
    }

    public void removeImage(GraphicsImage image) {
        images.remove(image);
        modified = true;
        auto_modified = true;
    }

    public void modifyGraphics(Graphics oldGraphics, Graphics newGraphics) {
        if (oldGraphics instanceof Stroke) {
            strokes.get(strokes.indexOf(oldGraphics)).replace(newGraphics);
        }

        if (oldGraphics instanceof GraphicsLine) {
            lineArt.get(lineArt.indexOf(oldGraphics)).replace(newGraphics);
        }

        if (oldGraphics instanceof GraphicsRectangle) {
            rectangleArt.get(rectangleArt.indexOf(oldGraphics)).replace(newGraphics);
        }

        if (oldGraphics instanceof GraphicsOval) {
            ovalArt.get(ovalArt.indexOf(oldGraphics)).replace(newGraphics);
        }

        if (oldGraphics instanceof GraphicsTriangle) {
            triangleArt.get(triangleArt.indexOf(oldGraphics)).replace(newGraphics);
        }
    }

    public void draw(Canvas canvas) {
        RectF boundingBox = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());
        draw(canvas, boundingBox, true);
    }

    public void draw(Canvas canvas, RectF bounding_box) {
        draw(canvas, bounding_box, true);
    }

    private void draw(Canvas canvas, RectF bounding_box, boolean drawBackgroundLines) {
        isCanvasDrawCompleted = false;

        canvas.save();
        canvas.clipRect(bounding_box);
        ALog.d(TAG, "Page<---draw seek background");
        if (drawBackgroundLines)
            background.draw(canvas, bounding_box, transformation);
        else
            background.drawEmptyBackground(canvas, bounding_box, transformation);
        backgroundText.draw(canvas);
        for (GraphicsImage graphics : Collections.unmodifiableList(new ArrayList<>(images))) {
            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.draw(canvas);
        }

        //artis test to draw last strok
//      ALog.d(TAG, "Page<---draw strokes = " + strokes.size() );
//      strokes.getLast().draw(canvas, bounding_box);

        if (doInvalidate)
            mIntervalInvalidateDrawHandler.postDelayed(intervalInvalidateDrawRunnable, 500);
        Global.HAS_GREY_COLOR = false;
        for (Stroke s : Collections.unmodifiableList(new ArrayList<>(strokes))) {
            Global.checkNeedRefresh(s.getStrokeColor());
            if (!canvas.quickReject(s.getBoundingBox(), Canvas.EdgeType.AA))
                s.draw(canvas);
        }

        for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(lineArt))) {
            Global.checkNeedRefresh(graphics.getGraphicsColor());
            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.draw(canvas);
        }

        for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(rectangleArt))) {
            Global.checkNeedRefresh(graphics.getGraphicsColor());
            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.draw(canvas);
        }

        for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(ovalArt))) {
            Global.checkNeedRefresh(graphics.getGraphicsColor());
            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.draw(canvas);
        }

        for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(triangleArt))) {
            Global.checkNeedRefresh(graphics.getGraphicsColor());
            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.draw(canvas);
        }

        for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(nooseArt))) {
            graphics.draw(canvas);
            graphics.drawControlPoints(canvas);
        }
        canvas.restore();
        isCanvasDrawCompleted = true;
        if (doInvalidate) {
//            CallbackEvent event = new CallbackEvent();
//            event.setMessage(CallbackEvent.PAGE_DRAW_COMPLETED);
//            mEventBus.post(event);
            mIntervalInvalidateDrawHandler.removeCallbacks(intervalInvalidateDrawRunnable);
        }
    }

    public void drawSelectedObjects(Canvas canvas) {
        RectF boundingBox = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());
        drawSelectedObjects(canvas, boundingBox);
    }

    public void drawSelectedObjects(Canvas canvas, RectF bounding_box) {

        canvas.save();
        canvas.clipRect(bounding_box);

        // background.drawEmptyBackground(canvas, bounding_box, transformation);
        // background.draw(canvas, bounding_box, transformation);
        background.drawTransparentBackground(canvas, bounding_box, transformation);

        mIntervalInvalidateDrawHandler.postDelayed(intervalInvalidateDrawRunnable, 500);

        Global.HAS_GREY_COLOR = false;

        for (Stroke s : Collections.unmodifiableList(new ArrayList<>(mSelectedObjects.strokes))) {
            Global.checkNeedRefresh(s.getStrokeColor());
            if (!canvas.quickReject(s.getBoundingBox(), Canvas.EdgeType.AA))
                s.drawOutLine(canvas);
        }

        for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(mSelectedObjects.lineArt))) {
            Global.checkNeedRefresh(graphics.getGraphicsColor());
            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.drawOutLine(canvas);
        }

        for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(mSelectedObjects.rectangleArt))) {
            Global.checkNeedRefresh(graphics.getGraphicsColor());
            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.drawOutLine(canvas);
        }

        for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(mSelectedObjects.ovalArt))) {
            Global.checkNeedRefresh(graphics.getGraphicsColor());
            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.drawOutLine(canvas);
        }

        for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(mSelectedObjects.triangleArt))) {
            Global.checkNeedRefresh(graphics.getGraphicsColor());
            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.drawOutLine(canvas);
        }

        for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(nooseArt))) {
            graphics.draw(canvas);
            graphics.drawControlPoints(canvas);
        }
        canvas.restore();
        mIntervalInvalidateDrawHandler.removeCallbacks(intervalInvalidateDrawRunnable);
    }


    void drawInBackgroundThread(Canvas canvas) {
        RectF bounding_box = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());
        Drawer.getInstance().drawPage(this, canvas, bounding_box, doInvalidate);
    }

    public Stroke findStrokeAt(float x, float y, float radius) {
        ListIterator<Stroke> siter = strokes.listIterator();
        while (siter.hasNext()) {
            Stroke s = siter.next();
            if (!s.getBoundingBox().contains(x, y)) continue;
            if (s.distance(x, y) < radius)
                return s;
        }
        return null;
    }

    private void drawPNG(Canvas canvas, boolean isPrintBackground) {
        mRectF.set(0, 0, canvas.getWidth(), canvas.getHeight());

        canvas.save();
        canvas.clipRect(mRectF);

        if (isPrintBackground) {
            if (background.getPaperType() == Paper.Type.CUSTOMIZED) {
                background.drawPNG(canvas, mRectF, transformation);
            } else {
                background.draw(canvas, mRectF, transformation);
            }
        } else
            background.drawEmptyBackground(canvas, mRectF, transformation);

        backgroundText.draw(canvas);
        for (GraphicsImage graphics : images) {
            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.draw(canvas);
        }

        for (Stroke s : strokes) {
            if (!canvas.quickReject(s.getBoundingBox(), Canvas.EdgeType.AA))
                s.convert_draw(canvas);
        }

        for (GraphicsControlPoint graphics : lineArt) {
            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.convert_draw(canvas);
        }

        for (GraphicsControlPoint graphics : rectangleArt) {
            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.convert_draw(canvas);
        }

        for (GraphicsControlPoint graphics : ovalArt) {
            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.convert_draw(canvas);
        }

        for (GraphicsControlPoint graphics : triangleArt) {
            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.convert_draw(canvas);
        }
        canvas.restore();
    }

    public boolean savePageToStorage() {
        Log.d("Book", "Page Save Start : " + s.format(new Date()));
        Book currentBook = Bookshelf.getInstance().getCurrentBook();
        Log.d("Book", "Page Save End : " + s.format(new Date()));
        return currentBook.savePageToStorage(this);
    }

    public boolean tryRename(File originFile, File originFileRename, File tempFile, File tempFileRename) {

        File _originFile = originFile; //eg. index
        File _originFileRename = originFileRename; //eg. index.old
        File _tempFile = tempFile; //eg. index.temp
        File _tempFileRename = tempFileRename; //eg. index

        if (originFileRename.exists()) {
            for (int i = 0; i < 10; i++) {
                if (originFileRename.delete()) {
                    break;
                } else {
                    if (i == 9) {
                        return false;
                    }
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        for (int i = 0; i < 10; i++) {
            if (_originFile.renameTo(_originFileRename)) {
                break;
            } else {
                if (i == 9) {
                    return false;
                }
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < 10; i++) {
            if (_tempFile.renameTo(_tempFileRename)) {
                break;
            } else {
                if (i == 9) {
                    return false;
                }
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (originFileRename.exists()) {
            for (int i = 0; i < 10; i++) {
                if (originFileRename.delete()) {
                    break;
                } else {
                    if (i == 9) {
                        return false;
                    }
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    public void writeToStream(DataOutputStream out) throws IOException {
        /**
         * version 8 : 2019/1/25 - add rectangle
         */
        out.writeInt(PAGE_VERSION);  // protocol version number
        out.writeUTF(uuid.toString());
        tags.write_to_stream(out);
        out.writeInt(paper_type.ordinal());
        out.writeInt(images.size());
        for (GraphicsControlPoint img : Collections.unmodifiableList(new ArrayList<>(images)))
            img.writeToStream(out);

        out.writeInt(0); // reserved2
        out.writeBoolean(is_readonly);
        out.writeFloat(aspect_ratio);

        out.writeInt(strokes.size());
        for (Stroke stroke : Collections.unmodifiableList(new ArrayList<>(strokes)))
            stroke.writeToStream(out);

        out.writeInt(lineArt.size());
        for (GraphicsControlPoint line : Collections.unmodifiableList(new ArrayList<>(lineArt)))
            line.writeToStream(out);

        /**
         * version 8 : 2019/1/25 - add rectangle
         */
        out.writeInt(rectangleArt.size());
        for (GraphicsControlPoint rectangle : Collections.unmodifiableList(new ArrayList<>(rectangleArt)))
            rectangle.writeToStream(out);

        /**
         * version 9 : 2019/1/25 - add circle and triangle
         */
        out.writeInt(ovalArt.size());
        for (GraphicsControlPoint oval : Collections.unmodifiableList(new ArrayList<>(ovalArt)))
            oval.writeToStream(out);

        out.writeInt(triangleArt.size());
        for (GraphicsControlPoint triangle : Collections.unmodifiableList(new ArrayList<>(triangleArt)))
            triangle.writeToStream(out);

        out.writeInt(0); // reserved
        out.writeInt(0); // number of text boxes
        out.writeUTF(paper_path);

        /**
         * version 10 : 2019/05/13 - add MainTag
         */
        out.writeUTF(mMainTag);
    }

    /**
     * To be called after the page has been saved to the internal storage (but NOT: anywhere else like backups)
     */
    public void markAsSaved(boolean autoSave) {
        if (autoSave) {
            auto_modified = false;
        } else {
            modified = false;
        }
    }

    public Page(TagManager tagMgr) {
        uuid = UUID.randomUUID();
        version = PAGE_VERSION;
        tagManager = tagMgr;
        tags = tagManager.newTagSet();
        setPaperType(paper_type, paper_path);
        setAspectRatio(aspect_ratio);
        setTransform(transformation);
        modified = true;
        auto_modified = true;
    }


    /**
     * Construct a new page with the same paper type but without the content
     *
     * @param template
     * @return
     */
    public static Page emptyWithStyleOf(Page template) {
        return new Page(template, false);
    }

    /**
     * The copy constructor
     *
     * @param template
     */
    public Page(Page template, File dir) {
        tags = template.tags.copy();
        initPageStyle(template);
        for (Stroke stroke : template.strokes)
            strokes.add(new Stroke(stroke));
        for (GraphicsLine line : template.lineArt)
            lineArt.add(new GraphicsLine(line));
        for (GraphicsRectangle rectangle : template.rectangleArt)
            rectangleArt.add(new GraphicsRectangle(rectangle));
        for (GraphicsOval oval : template.ovalArt)
            ovalArt.add(new GraphicsOval(oval));
        for (GraphicsTriangle triangle : template.triangleArt)
            triangleArt.add(new GraphicsTriangle(triangle));
        for (GraphicsImage image : template.images)
            images.add(new GraphicsImage(image, dir));
    }

    /**
     * Implementation of emptyWithStyleOf
     */
    private Page(Page template, boolean dummy) {
        tags = template.tags.copy();
        initPageStyle(template);
    }

    private void initPageStyle(Page template) {
        version = template.version;
        uuid = UUID.randomUUID();
        tagManager = template.tagManager;
        setPaperType(template.paper_type, template.paper_path);
        setAspectRatio(template.aspect_ratio);
        setTransform(template.transformation);
        modified = true;
        auto_modified = true;
    }


    public Page(DataInputStream in, TagManager tagMgr, File dir) throws IOException {
        exceptionFixedPage = new Page(tagMgr);

        tagManager = tagMgr;
        version = in.readInt();
        if (version == 1) {
            uuid = UUID.randomUUID();
            exceptionFixedPage.uuid = uuid;
            tags = tagManager.newTagSet();
            paper_type = Paper.Type.EMPTY;
            exceptionFixedPage.paper_type = paper_type;
        } else if (version == 2) {
            uuid = UUID.randomUUID();
            exceptionFixedPage.uuid = uuid;
            tags = tagManager.newTagSet();
            paper_type = Paper.Type.values()[in.readInt()];
            exceptionFixedPage.paper_type = paper_type;
            in.readInt();
            in.readInt();
        } else if (version == 3) {
            uuid = UUID.randomUUID();
            exceptionFixedPage.uuid = uuid;
            tags = tagManager.loadTagSet(in);
            paper_type = Paper.Type.values()[in.readInt()];
            exceptionFixedPage.paper_type = paper_type;
            in.readInt();
            in.readInt();
        } else if (version == 4 || version == 5) {
            uuid = UUID.fromString(in.readUTF());
            exceptionFixedPage.uuid = uuid;
            tags = tagManager.loadTagSet(in);
            paper_type = Paper.Type.values()[in.readInt()];
            exceptionFixedPage.paper_type = paper_type;
            in.readInt();
            in.readInt();
        } else if (version >= 6) {
            uuid = UUID.fromString(in.readUTF());
            exceptionFixedPage.uuid = uuid;
            tags = tagManager.loadTagSet(in);
            paper_type = Paper.Type.values()[in.readInt()];
            exceptionFixedPage.paper_type = paper_type;
            int nImages = in.readInt();
            for (int i = 0; i < nImages; i++) {
                GraphicsImage gi = new GraphicsImage(in, dir);
                images.add(gi);
                exceptionFixedPage.images.add(gi);
            }
            int dummy = in.readInt();
            Assert.assertTrue(dummy == 0);
        } else
            throw new IOException("Unknown page version!");
        is_readonly = in.readBoolean();
        exceptionFixedPage.is_readonly = is_readonly;
        aspect_ratio = in.readFloat();
        exceptionFixedPage.aspect_ratio = aspect_ratio;

        int nStrokes = in.readInt();
        for (int i = 0; i < nStrokes; i++) {
            Stroke s = new Stroke(in);
            strokes.add(s);
            exceptionFixedPage.strokes.add(s);
        }

        if (version >= 5) {
            int nLines = in.readInt();
            for (int i = 0; i < nLines; i++) {
                GraphicsLine l = new GraphicsLine(in);
                lineArt.add(l);
                exceptionFixedPage.lineArt.add(l);
            }

            if (version >= 8) {
                int nRectangles = in.readInt();
                for (int i = 0; i < nRectangles; i++) {
                    GraphicsRectangle r = new GraphicsRectangle(in);
                    rectangleArt.add(r);
                    exceptionFixedPage.rectangleArt.add(r);
                }
                if (version >= 9) {
                    int nOvals = in.readInt();
                    for (int i = 0; i < nOvals; i++) {
                        GraphicsOval o = new GraphicsOval(in);
                        ovalArt.add(o);
                        exceptionFixedPage.ovalArt.add(o);
                    }
                    int nTriangle = in.readInt();
                    for (int i = 0; i < nTriangle; i++) {
                        GraphicsTriangle t = new GraphicsTriangle(in);
                        triangleArt.add(t);
                        exceptionFixedPage.triangleArt.add(t);
                    }
                }
            }

            in.readInt(); // dummy
            int nText = in.readInt();

            if (version >= 7) {
                paper_path = in.readUTF();
                exceptionFixedPage.paper_path = paper_path;

                if (version >= 10) {
                    mMainTag = in.readUTF();
                    exceptionFixedPage.mMainTag = mMainTag;
                }
            }
        }

        background.setAspectRatio(aspect_ratio);
        background.setPaperType(paper_type);
        background.setPaperPath(paper_path);

        exceptionFixedPage.background.setAspectRatio(aspect_ratio);
        exceptionFixedPage.background.setPaperType(paper_type);
        exceptionFixedPage.background.setPaperPath(paper_path);
    }

    /**
     * This method should be put in background thread.
     *
     * @param width
     * @param height
     * @return
     */
    public Bitmap renderBitmap(int width, int height, boolean isPrintBackground) {
        Transformation backup = new Transformation(getTransform());
        float scale = Math.min(height, width / aspect_ratio);
        setTransform(0, 0, scale);
        int actual_width = (int) Math.rint(scale * aspect_ratio);
        int actual_height = (int) Math.rint(scale);

        Bitmap bitmap = Bitmap.createBitmap
                (actual_width, actual_height, Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        drawPNG(c, isPrintBackground);
        setTransform(backup);
        return bitmap;
    }

    public void render(Artist artist) {
        background.render(artist);
        for (GraphicsImage image : images) {
            image.render(artist);
        }
        for (Stroke stroke : strokes) {
            ALog.d(TAG, "P<----rendering");
            stroke.render(artist);
        }
        for (GraphicsControlPoint line : lineArt)
            line.render(artist);
        for (GraphicsControlPoint rectangle : rectangleArt)
            rectangle.render(artist);
        for (GraphicsControlPoint oval : ovalArt)
            oval.render(artist);
        for (GraphicsControlPoint triangle : triangleArt)
            triangle.render(artist);
    }

    public void clearCustomizedBackground() {
        background.ClearBitmap();
    }

    public boolean isNDrawDuModeRequiredForGrayscale() {
        return paper_type == Paper.Type.CUSTOMIZED || !images.isEmpty() || Hardware.isPenUpdateModeDU();
    }

    public class SelectedObjects {
        public final LinkedList<Stroke> strokes = new LinkedList<>();
        public final LinkedList<GraphicsLine> lineArt = new LinkedList<>();
        public final LinkedList<GraphicsRectangle> rectangleArt = new LinkedList<>();
        public final LinkedList<GraphicsOval> ovalArt = new LinkedList<>();
        public final LinkedList<GraphicsTriangle> triangleArt = new LinkedList<>();
        public final LinkedList<Graphics> allGraphics = new LinkedList<>();
        public final RectF range = new RectF();

        private SelectedObjects() {
        }

        public void addSelectedStroke(Stroke selectedStroke) {
            strokes.add(selectedStroke);
            allGraphics.add(selectedStroke);
        }

        public void addSelectedLine(GraphicsLine selectedLine) {
            lineArt.add(selectedLine);
            allGraphics.add(selectedLine);
        }

        public void addSelectedRectangle(GraphicsRectangle selectedRectangle) {
            rectangleArt.add(selectedRectangle);
            allGraphics.add(selectedRectangle);
        }

        public void addSelectedOval(GraphicsOval selectedOval) {
            ovalArt.add(selectedOval);
            allGraphics.add(selectedOval);
        }

        public void addSelectedTriangle(GraphicsTriangle selectedTriangle) {
            triangleArt.add(selectedTriangle);
            allGraphics.add(selectedTriangle);
        }

        public boolean isEmpty() {
            return strokes.isEmpty() && lineArt.isEmpty() && rectangleArt.isEmpty() && ovalArt.isEmpty() && triangleArt.isEmpty();
        }

        public LinkedList<Graphics> getAllSelectedGraphics() {
            return allGraphics;
        }
    }

    private SelectedObjects getSelectedObjects(RectF nooseRange) {
        SelectedObjects selectedObjects = new SelectedObjects();

        for (Stroke stroke : strokes) {
            stroke.computeBoundingBox();
            RectF box = stroke.getBoundingBox();
            if (nooseRange.contains(box)) {
                stroke.backup();
                selectedObjects.addSelectedStroke(stroke);

                if (selectedObjects.range.isEmpty())
                    selectedObjects.range.set(box);
                else
                    selectedObjects.range.set(reSideRange(selectedObjects.range, box));
            }
        }

        for (GraphicsLine graphicsLine : lineArt) {
            graphicsLine.computeBoundingBox();
            RectF box = graphicsLine.getBoundingBox();
            if (nooseRange.contains(box)) {
                graphicsLine.backup();
                selectedObjects.addSelectedLine(graphicsLine);

                if (selectedObjects.range.isEmpty())
                    selectedObjects.range.set(box);
                else
                    selectedObjects.range.set(reSideRange(selectedObjects.range, box));
            }
        }

        for (GraphicsRectangle graphicsRectangle : rectangleArt) {
            graphicsRectangle.computeBoundingBox();
            RectF box = graphicsRectangle.getBoundingBox();
            if (nooseRange.contains(box)) {
                graphicsRectangle.backup();
                selectedObjects.addSelectedRectangle(graphicsRectangle);

                if (selectedObjects.range.isEmpty())
                    selectedObjects.range.set(box);
                else
                    selectedObjects.range.set(reSideRange(selectedObjects.range, box));
            }
        }

        for (GraphicsOval graphicsOval : ovalArt) {
            graphicsOval.computeBoundingBox();
            RectF box = graphicsOval.getBoundingBox();
            if (nooseRange.contains(box)) {
                graphicsOval.backup();
                selectedObjects.addSelectedOval(graphicsOval);

                if (selectedObjects.range.isEmpty())
                    selectedObjects.range.set(box);
                else
                    selectedObjects.range.set(reSideRange(selectedObjects.range, box));
            }
        }

        for (GraphicsTriangle graphicsTriangle : triangleArt) {
            graphicsTriangle.computeBoundingBox();
            RectF box = graphicsTriangle.getBoundingBox();
            if (nooseRange.contains(box)) {
                graphicsTriangle.backup();
                selectedObjects.addSelectedTriangle(graphicsTriangle);

                if (selectedObjects.range.isEmpty())
                    selectedObjects.range.set(box);
                else
                    selectedObjects.range.set(reSideRange(selectedObjects.range, box));
            }
        }

        selectedObjects.range.sort();

        return selectedObjects;
    }

    public RectF getSelectedCopyObjects(LinkedList<Graphics> toCopy, Canvas canvas) {
        SelectedObjects selectedObjects = new SelectedObjects();
        RectF nooseRange = new RectF();
        nooseRange.top = -canvas.getHeight();
        nooseRange.bottom = canvas.getHeight() * 2;
        nooseRange.left = -canvas.getWidth();
        nooseRange.right = canvas.getWidth() * 2;
        for (Graphics g : toCopy) {

            if (g instanceof Stroke) {
                g.computeBoundingBox();
                RectF box = g.getBoundingBox();
                if (nooseRange.contains(box)) {
                    selectedObjects.addSelectedStroke((Stroke) g);

                    if (selectedObjects.range.isEmpty())
                        selectedObjects.range.set(box);
                    else
                        selectedObjects.range.set(reSideRange(selectedObjects.range, box));
                }
            } else if (g instanceof GraphicsLine) {
                g.computeBoundingBox();
                RectF box = g.getBoundingBox();
                if (nooseRange.contains(box)) {
                    selectedObjects.addSelectedLine((GraphicsLine) g);

                    if (selectedObjects.range.isEmpty())
                        selectedObjects.range.set(box);
                    else
                        selectedObjects.range.set(reSideRange(selectedObjects.range, box));
                }
            } else if (g instanceof GraphicsRectangle) {
                g.computeBoundingBox();
                RectF box = g.getBoundingBox();
                if (nooseRange.contains(box)) {
                    selectedObjects.addSelectedRectangle((GraphicsRectangle) g);

                    if (selectedObjects.range.isEmpty())
                        selectedObjects.range.set(box);
                    else
                        selectedObjects.range.set(reSideRange(selectedObjects.range, box));
                }
            } else if (g instanceof GraphicsOval) {
                g.computeBoundingBox();
                RectF box = g.getBoundingBox();
                if (nooseRange.contains(box)) {
                    selectedObjects.addSelectedOval((GraphicsOval) g);

                    if (selectedObjects.range.isEmpty())
                        selectedObjects.range.set(box);
                    else
                        selectedObjects.range.set(reSideRange(selectedObjects.range, box));
                }
            } else if (g instanceof GraphicsTriangle) {
                g.computeBoundingBox();
                RectF box = g.getBoundingBox();
                if (nooseRange.contains(box)) {
                    selectedObjects.addSelectedTriangle((GraphicsTriangle) g);

                    if (selectedObjects.range.isEmpty())
                        selectedObjects.range.set(box);
                    else
                        selectedObjects.range.set(reSideRange(selectedObjects.range, box));
                }
            }
        }

        selectedObjects.range.sort();
        mSelectedObjects = selectedObjects;
        return selectedObjects.range;
    }

    private RectF reSideRange(RectF reSideRect, RectF referenceRect) {
        RectF result = new RectF();
        result.left = (referenceRect.left <= reSideRect.left) ? referenceRect.left : reSideRect.left;

        result.top = (referenceRect.top <= reSideRect.top) ? referenceRect.top : reSideRect.top;

        result.right = (referenceRect.right >= reSideRect.right) ? referenceRect.right : reSideRect.right;

        result.bottom = (referenceRect.bottom >= reSideRect.bottom) ? referenceRect.bottom : reSideRect.bottom;

        return result;
    }

    public RectF getSelectedRangeRect(RectF nooseRange) {
        nooseRange.sort();
        mSelectedObjects = getSelectedObjects(nooseRange);
        return mSelectedObjects.range;
    }

    public void clearSelectedObjects() {
        mSelectedObjects = null;
    }

    public SelectedObjects get_mSelectedObjects() {
        return mSelectedObjects;
    }

    public boolean isSelectedObjectsEmpty() {
        if (mSelectedObjects == null)
            return true;

        return mSelectedObjects.isEmpty();
    }

    public void moveSelectedObjects(float offsetX, float offsetY) {
        for (Stroke stroke : strokes) {
            for (Stroke stroke1 : mSelectedObjects.strokes) {
                if (stroke == stroke1) {
                    stroke.move(offsetX, offsetY);
                }
            }
        }

        for (GraphicsLine graphicsLine : lineArt) {
            for (GraphicsLine line : mSelectedObjects.lineArt) {
                if (graphicsLine == line) {
                    graphicsLine.move(offsetX, offsetY);
                }
            }
        }

        for (GraphicsRectangle graphicsRectangle : rectangleArt) {
            for (GraphicsRectangle rectangle : mSelectedObjects.rectangleArt) {
                if (graphicsRectangle == rectangle) {
                    graphicsRectangle.move(offsetX, offsetY);
                }
            }
        }

        for (GraphicsOval graphicsOval : ovalArt) {
            for (GraphicsOval oval : mSelectedObjects.ovalArt) {
                if (graphicsOval == oval) {
                    graphicsOval.move(offsetX, offsetY);
                }
            }
        }

        for (GraphicsTriangle graphicsTriangle : triangleArt) {
            for (GraphicsTriangle triangle : mSelectedObjects.triangleArt) {
                if (graphicsTriangle == triangle) {
                    graphicsTriangle.move(offsetX, offsetY);
                }
            }
        }
    }

}


