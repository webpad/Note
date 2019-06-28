package name.vbraun.view.write;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Handler;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;

import ntx.note.CallbackEvent;
import ntx.note.Global;

import static java.lang.Thread.sleep;

public class Drawer {

    private static Drawer mInstance;

    private DrawThread mDrawThread;

    private Page mPage;

    private EventBus mEventBus;
    private Handler mIntervalInvalidateDrawHandler;

    private Runnable intervalInvalidateDrawRunnable = new Runnable() {
        @Override
        public void run() {
            CallbackEvent event = new CallbackEvent();
            event.setMessage(CallbackEvent.DO_DRAW_VIEW_INVALIDATE);
            mEventBus.post(event);
            mIntervalInvalidateDrawHandler.postDelayed(this, 500);
        }
    };

    public static Drawer getInstance() {
        if (mInstance == null)
            mInstance = new Drawer();
        return mInstance;
    }

    private Drawer() {
        mEventBus = EventBus.getDefault();
        mIntervalInvalidateDrawHandler = new Handler();
    }

    public void drawPage(Page page, Canvas canvas, RectF boundingBox, boolean doInvalidate) {
        if (mDrawThread != null) {
            mDrawThread.interrupt();
            mDrawThread = null;

            try {
                sleep(100);
            } catch (InterruptedException e) {

            }
        }

        mPage = page;

        mDrawThread = new DrawThread(canvas, boundingBox, doInvalidate);
        mDrawThread.start();
    }


    private class DrawThread extends Thread {
        Canvas canvas;
        RectF boundingBox;
        boolean doInvalidate;

        public DrawThread(Canvas _canvas, RectF _boundingBox, boolean _doInvalidate) {
            this.canvas = _canvas;
            this.boundingBox = _boundingBox;
            this.doInvalidate = _doInvalidate;
        }

        @Override
        public void run() {
            mPage.isCanvasDrawCompleted = false;

            canvas.save();
            canvas.clipRect(boundingBox);

            mPage.background.draw(canvas, boundingBox, mPage.transformation);
            mPage.backgroundText.draw(canvas);
            for (GraphicsImage graphics : Collections.unmodifiableList(new ArrayList<>(mPage.images))) {
                if (!currentThread().isInterrupted()) {
                    if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                        graphics.draw(canvas);
                } else {
                    canvas.restore();
                    return;
                }
            }

            if (doInvalidate)
                mIntervalInvalidateDrawHandler.postDelayed(intervalInvalidateDrawRunnable, 500);

            for (Stroke s : Collections.unmodifiableList(new ArrayList<>(mPage.strokes))) {
                Global.checkNeedRefresh(s.getStrokeColor());
                if (!currentThread().isInterrupted()) {
                    if (!canvas.quickReject(s.getBoundingBox(), Canvas.EdgeType.AA))
                        s.draw(canvas);
                } else {
                    canvas.restore();
                    return;
                }
            }

            for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(mPage.lineArt))) {
                Global.checkNeedRefresh(graphics.getGraphicsColor());
                if (!currentThread().isInterrupted()) {
                    if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                        graphics.draw(canvas);
                } else {
                    canvas.restore();
                    return;
                }
            }

            for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(mPage.rectangleArt))) {
                Global.checkNeedRefresh(graphics.getGraphicsColor());
                if (!currentThread().isInterrupted()) {
                    if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                        graphics.draw(canvas);
                } else {
                    canvas.restore();
                    return;
                }
            }

            for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(mPage.ovalArt))) {
                Global.checkNeedRefresh(graphics.getGraphicsColor());
                if (!currentThread().isInterrupted()) {
                    if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                        graphics.draw(canvas);
                } else {
                    canvas.restore();
                    return;
                }
            }

            for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(mPage.triangleArt))) {
                Global.checkNeedRefresh(graphics.getGraphicsColor());
                if (!currentThread().isInterrupted()) {
                    if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                        graphics.draw(canvas);
                } else {
                    canvas.restore();
                    return;
                }
            }

            for (GraphicsControlPoint graphics : Collections.unmodifiableList(new ArrayList<>(mPage.nooseArt))) {
                Global.checkNeedRefresh(graphics.getGraphicsColor());
                if (!currentThread().isInterrupted()) {
                    if (!canvas.quickReject(graphics.getBoundingBox(), Canvas.EdgeType.AA))
                        graphics.draw(canvas);
                } else {
                    canvas.restore();
                    return;
                }
            }

            mPage.isCanvasDrawCompleted = true;

            canvas.restore();

            if (doInvalidate) {
                mIntervalInvalidateDrawHandler.removeCallbacks(intervalInvalidateDrawRunnable);
            }

            CallbackEvent event = new CallbackEvent();
            event.setMessage(CallbackEvent.PAGE_DRAW_COMPLETED);
            mEventBus.post(event);
        }
    }
}
