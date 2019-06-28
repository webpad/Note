package name.vbraun.view.write;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.AsyncTask;

import java.util.UUID;

import name.vbraun.filepicker.AsyncTaskResult;
import ntx.note.data.Book;

public class GetPagePreviewBitmapAsyncTask extends AsyncTask<Integer, Void, Bitmap> {
    private Book mBook;

    public AsyncTaskResult<Bitmap> asyncTaskResult;

    public GetPagePreviewBitmapAsyncTask(UUID bookUuid, int pageIndex) {
        this.mBook = new Book(bookUuid, pageIndex, 1);
    }

    @Override
    protected Bitmap doInBackground(Integer... integers) {
        int width = integers[0];
        int height = integers[1];
        Page page = mBook.getPage(0);
        return renderPageBitmap(page, width, height, page.getAspectRatio());
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (asyncTaskResult != null)
            asyncTaskResult.taskFinish(bitmap);

        this.mBook = null;
    }

    @Override
    protected void onCancelled() {
        this.mBook = null;
    }

    private Bitmap renderPageBitmap(Page page, int width, int height, float aspect_ratio) {
        float scale = Math.min(height, width / aspect_ratio);
        setTransform(page, 0, 0, scale);
        int actual_width = (int) Math.rint(scale * aspect_ratio);
        int actual_height = (int) Math.rint(scale);

        Bitmap bitmap = Bitmap.createBitmap(actual_width, actual_height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        RectF rectF = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());

        canvas.save();
        canvas.clipRect(rectF);

        page.background.draw(canvas, rectF, page.transformation);

        TextBox backgroundText = new TextBox(Graphics.Tool.TEXT);
        backgroundText.draw(canvas);
        for (GraphicsImage graphics : page.images) {
            if (isCancelled())
                return null;

            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.draw(canvas);
        }

        for (Stroke s : page.strokes) {
            if (isCancelled())
                return null;

            if (!canvas.quickReject(s.getBoundingBox(), Canvas.EdgeType.AA))
                s.drawThumbnail(canvas);
        }

        for (GraphicsControlPoint graphics : page.lineArt) {
            if (isCancelled())
                return null;

            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.draw(canvas);
        }

        for (GraphicsControlPoint graphics : page.rectangleArt) {
            if (isCancelled())
                return null;

            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.draw(canvas);
        }

        for (GraphicsControlPoint graphics : page.ovalArt) {
            if (isCancelled())
                return null;

            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.draw(canvas);
        }

        for (GraphicsControlPoint graphics : page.triangleArt) {
            if (isCancelled())
                return null;

            if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                graphics.draw(canvas);
        }
        canvas.restore();
        return bitmap;
    }

    protected void setTransform(Page page, float dx, float dy, float s) {
        page.transformation.offset_x = dx;
        page.transformation.offset_y = dy;
        page.transformation.scale = s;
        setTransformApply(page);
    }

    private void setTransformApply(Page page) {
        for (Stroke stroke : page.strokes) {
            if (isCancelled())
                return;
            stroke.setTransform(page.getTransform());
        }
        for (GraphicsControlPoint line : page.lineArt) {
            if (isCancelled())
                return;
            line.setTransform(page.getTransform());
        }
        for (GraphicsControlPoint rectangle : page.rectangleArt) {
            if (isCancelled())
                return;
            rectangle.setTransform(page.getTransform());
        }
        for (GraphicsControlPoint oval : page.ovalArt) {
            if (isCancelled())
                return;
            oval.setTransform(page.getTransform());
        }
        for (GraphicsControlPoint triangle : page.triangleArt) {
            if (isCancelled())
                return;
            triangle.setTransform(page.getTransform());
        }
        for (GraphicsImage image : page.images) {
            if (isCancelled())
                return;
            image.setTransform(page.transformation);
        }
    }
}
