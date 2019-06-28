package name.vbraun.view.write;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;

import junit.framework.Assert;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import ntx.note.artist.Artist;

public class GraphicsImage extends GraphicsControlPoint {
    private final static String TAG = "GraphicsImage";
    private final static float minDistancePixel = 30;

    private ControlPoint bottom_left, bottom_right, top_left, top_right,
            center;
    private final Paint paint = new Paint();
    private final Paint outline = new Paint();
    private final Rect rect = new Rect();
    private final RectF rectF = new RectF();

    private Bitmap bitmap = null;
    private File file = null;

    private int height, width;
    private float bitmapWidthHeightRatio;

    // persistent data
    protected UUID uuid = null;
    protected boolean constrainAspect = true;

    public enum FileType {
        NONE, PNG, JPG
    }

    /**
     * Construct a new image
     *
     * @param transform      The current transformation
     * @param x              Screen x coordinate
     * @param y              Screen y coordinate
     * @param //penThickness
     * @param //penColor
     */
    protected GraphicsImage(Transformation transform, float x, float y) {
        super(Tool.IMAGE);
        setTransform(transform);
        bottom_left = new ControlPoint(transform, x, y);
        bottom_right = new ControlPoint(transform, x, y);
        top_left = new ControlPoint(transform, x, y);
        top_right = new ControlPoint(transform, x, y);
        center = new ControlPoint(transform, x, y);
        controlPoints.add(bottom_left);
        controlPoints.add(bottom_right);
        controlPoints.add(top_left);
        controlPoints.add(top_right);
        controlPoints.add(center);
        init();
    }

    /**
     * The copy constructor
     *
     * @param image
     * @param dir   the directory to copy the image file to
     */
    protected GraphicsImage(final GraphicsImage image, File dir) {
        super(image);
        bottom_left = new ControlPoint(image.bottom_left);
        bottom_right = new ControlPoint(image.bottom_right);
        top_left = new ControlPoint(image.top_left);
        top_right = new ControlPoint(image.top_right);
        center = new ControlPoint(image.center);
        controlPoints.add(bottom_left);
        controlPoints.add(bottom_right);
        controlPoints.add(top_left);
        controlPoints.add(top_right);
        controlPoints.add(center);
        constrainAspect = image.constrainAspect;
        init();
        if (image.getFile() == null)
            return;
        final String fileName = getImageFileName(getUuid(), image.getFileType());
        if (!dir.exists())
            dir.mkdir();
        file = new File(dir, fileName);
        ntx.note.image.Util.copyfile(image.getFile(), file);
    }

    public GraphicsImage(DataInputStream in, File dir) throws IOException {
        super(Tool.IMAGE);
        int version = in.readInt();
        if (version > 1)
            throw new IOException("Unknown image version!");

        uuid = UUID.fromString(in.readUTF());
        float left = in.readFloat();
        float right = in.readFloat();
        float top = in.readFloat();
        float bottom = in.readFloat();
        constrainAspect = in.readBoolean();

        bottom_left = new ControlPoint(transform, left, bottom);
        bottom_right = new ControlPoint(transform, right, bottom);
        top_left = new ControlPoint(transform, left, top);
        top_right = new ControlPoint(transform, right, top);
        center = new ControlPoint(transform, (left + right) / 2, (top + bottom) / 2);
        controlPoints.add(bottom_left);
        controlPoints.add(bottom_right);
        controlPoints.add(top_left);
        controlPoints.add(top_right);
        controlPoints.add(center);
        init();
        file = new File(dir, getImageFileName(uuid, FileType.JPG));
    }

    @Override
    public boolean intersects(RectF screenRect) {
        return false;
    }

    @Override
    public void draw(Canvas c) {
        if (file != null && bitmap == null)
            try {
                loadBitmap();
            } catch (IOException e) {
                Log.e(TAG, "loading bitmap: " + e.getMessage());
            }

        computeScreenRect();
        c.clipRect(0, 0, c.getWidth(), c.getHeight(), android.graphics.Region.Op.REPLACE);

        if (bitmap == null) {
            c.drawRect(rect, paint);
            c.drawRect(rect, outline);
        } else {
            c.drawBitmap(bitmap, null, rect, null);
            bitmap.recycle();
            bitmap = null;
        }
    }

    @Override
    public void drawOutLine(Canvas c) {

    }

    @Override
    public void convert_draw(Canvas c) {

    }

    @Override
    public void render(Artist artist) {
        artist.imageJpeg(file, top_left.x, top_right.x, top_left.y, bottom_left.y);
    }

    @Override
    protected ControlPoint initialControlPoint() {
        return bottom_right;
    }

    @Override
    void controlPointMoved(ControlPoint point, float newX, float newY) {
        point.x = newX;
        point.y = newY;
        super.controlPointMoved(point, newX, newY);
        if (point == center) {
            float width2 = (bottom_right.x - bottom_left.x) / 2;
            float height2 = (top_right.y - bottom_right.y) / 2;
            bottom_right.y = bottom_left.y = center.y - height2;
            top_right.y = top_left.y = center.y + height2;
            bottom_right.x = top_right.x = center.x + width2;
            bottom_left.x = top_left.x = center.x - width2;
        } else {
            if (constrainAspect && bitmap != null) {
                ControlPoint opposite = oppositeControlPoint(point);
                float dx = Math.abs(opposite.x - point.x);
                float dy = Math.abs(opposite.y - point.y);
                float minDistance = minDistancePixel / scale;

                if (dx <= minDistance)
                    dx = minDistance;
                if (dy <= minDistance)
                    dy = minDistance;

                /**
                 * if width < height
                 *    use height to get width
                 * else
                 *    use width to get height
                 */
                if (bitmapWidthHeightRatio <= 1) {
                    dx = dy * bitmapWidthHeightRatio;
                } else {
                    dy = dx / bitmapWidthHeightRatio;
                }

                if (opposite.x < point.x)
                    point.x = opposite.x + dx;
                else
                    point.x = opposite.x - dx;

                if (opposite.y < point.y)
                    point.y = opposite.y + dy;
                else
                    point.y = opposite.y - dy;
            }

            if (point == top_left) {
                bottom_left.x = point.x;
                top_right.y = point.y;
            } else if (point == top_right) {
                bottom_right.x = point.x;
                top_left.y = point.y;
            } else if (point == bottom_right) {
                top_right.x = point.x;
                bottom_left.y = point.y;
            } else {
                top_left.x = point.x;
                bottom_right.y = point.y;
            }

            rectF.left = top_left.x;
            rectF.top = top_left.y;
            rectF.right = bottom_right.x;
            rectF.bottom = bottom_right.y;
            rectF.sort();

            center.x = rectF.left + (rectF.right - rectF.left) / 2;
            center.y = rectF.bottom + (rectF.top - rectF.bottom) / 2;
        }
    }

    public void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(1);  // protocol #1
        out.writeUTF(uuid.toString());
        out.writeFloat(top_left.x);
        out.writeFloat(top_right.x);
        out.writeFloat(top_left.y);
        out.writeFloat(bottom_left.y);
        out.writeBoolean(constrainAspect);
    }

    public boolean checkFileName(String fileName) {
        FileType fileType = getImageFileType(fileName);
        return fileName.endsWith(getImageFileName(uuid, fileType));
    }

    public static String getImageFileExt(FileType fileType) {
        if (fileType == FileType.JPG) {
            return ".jpg";
        } else if (fileType == FileType.PNG) {
            return ".png";
        } else {
            Assert.fail();
            return null;
        }
    }

    public static FileType getImageFileType(String fileName) {
        for (FileType t : FileType.values()) {
            if (t == FileType.NONE)
                continue;
            String ext = getImageFileExt(t);
            if (fileName.endsWith(ext))
                return t;
        }
        return FileType.NONE;
    }

    /**
     * Helper to construct a file name out of uuid and file type
     *
     * @param uuid
     * @param fileType
     * @return
     */
    public static String getImageFileName(UUID uuid, FileType fileType) {
        return uuid.toString() + getImageFileExt(fileType);
    }

    public String getFileName() {
        return file.getAbsolutePath();
    }

    public UUID getUuid() {
        if (uuid == null)
            uuid = UUID.randomUUID();
        return uuid;
    }

    public Uri getFileUri() {
        if (file == null)
            return null;   // no picture selected yet
        else
            return Uri.fromFile(file);
    }

    public File getFile() {
        return file;
    }

    public boolean getConstrainAspect() {
        return constrainAspect;
    }

    public FileType getFileType() {
        return getImageFileType(file.getName());
    }

    private void init() {
        paint.setARGB(0xff, 0x5f, 0xff, 0x5f);
//		paint.setStyle(Style.FILL);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(0);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
//		outline.setARGB(0xff, 0x0, 0xaa, 0x0);
        outline.setColor(Color.BLACK);
        outline.setStyle(Style.STROKE);
        outline.setStrokeWidth(4);
        outline.setAntiAlias(true);
        outline.setStrokeCap(Paint.Cap.ROUND);
    }

    private void computeScreenRect() {
        rectF.bottom = bottom_left.screenY();
        rectF.top = top_left.screenY();
        rectF.left = bottom_left.screenX();
        rectF.right = bottom_right.screenX();
        rectF.sort();
        rectF.round(rect);
    }

    public void setFile(String fileName, boolean constrainAspect) {
        // file = new File("/mnt/sdcard/d5efe912-4b03-4ed7-a124-bff4984691d6.jpg");
        if (!checkFileName(fileName)) {
            Log.e(TAG, "filename must be uuid.ext");
        }
        file = new File(fileName);
        try {
            loadBitmap();
        } catch (IOException e) {
            Log.e(TAG, "Unable to load file " + file.toString() + " (missing?");
        }
        this.constrainAspect = constrainAspect;
        if (constrainAspect) {
            float w = top_right.x - top_left.x;
            float h = bottom_right.y - top_right.y;
            float cRate = w / h;
            bitmapWidthHeightRatio = (float) width / (float) height;

            if (bitmapWidthHeightRatio < cRate) {
                w = h * bitmapWidthHeightRatio;
            } else {
                h = w / bitmapWidthHeightRatio;
            }
            bottom_left.x = top_left.x = center.x - w / 2;
            bottom_right.x = top_right.x = center.x + w / 2;
            bottom_left.y = bottom_right.y = center.y + h / 2;
            top_left.y = top_right.y = center.y - h / 2;
        }
    }

    private final int IMAGE_MAX_SIZE = 1024;

    private void loadBitmap() throws IOException {
        Assert.assertNotNull(file);
        InputStream fis;

        BitmapFactory.Options o1 = new BitmapFactory.Options();
        o1.inJustDecodeBounds = true;
        fis = new FileInputStream(file);
        BitmapFactory.decodeStream(fis, null, o1);
        fis.close();

        int h = o1.outHeight;
        int w = o1.outWidth;
        int scale = 1;
        if (h > IMAGE_MAX_SIZE || w > IMAGE_MAX_SIZE) {
            scale = (int) Math.pow(
                    2,
                    (int) Math.round(Math.log(IMAGE_MAX_SIZE
                            / (double) Math.max(h, w))
                            / Math.log(0.5)));
        }

        BitmapFactory.Options o2 = new BitmapFactory.Options();

        //o2.inSampleSize = scale;
        //enhance image quality -s
        o2.inSampleSize = 1;
        o2.inPreferQualityOverSpeed = true;
        o2.inScaled = false;
        o2.inDither = true;
        o2.inPreferredConfig = Bitmap.Config.ARGB_8888;
        //enhance image quality -e

        fis = new FileInputStream(file);
        try {
            bitmap = BitmapFactory.decodeStream(fis, null, o2);
        } catch (OutOfMemoryError e1) {
            o2.inSampleSize = 8 * scale;
            try {
                bitmap = BitmapFactory.decodeStream(fis, null, o2);
            } catch (OutOfMemoryError e2) {
                Log.e(TAG, "Not enough memory to load image");
                bitmap = null;
            }
        }
        fis.close();

        height = o2.outHeight;
        width = o2.outWidth;
        bitmapWidthHeightRatio = (float) width / (float) height;
    }

    private ControlPoint oppositeControlPoint(ControlPoint point) {
        if (point == bottom_right)
            return top_left;
        if (point == bottom_left)
            return top_right;
        if (point == top_right)
            return bottom_left;
        if (point == top_left)
            return bottom_right;
        if (point == center)
            return center;
        Assert.fail("Unreachable");
        return null;
    }

    @Override
    public void move(float offsetX, float offsetY) {

    }
}
