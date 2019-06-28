package name.vbraun.view.write;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

import junit.framework.Assert;

import org.greenrobot.eventbus.EventBus;

import java.util.LinkedList;

import name.vbraun.view.write.Graphics.Tool;
import name.vbraun.view.write.GraphicsControlPoint.ControlPoint;
import ntx.note.CallbackEvent;
import ntx.note.ToolboxConfiguration;
import ntx.note.ToolboxViewBuilder;

/**
 * Base class for touch handles than manipulate control points
 *
 * @author vbraun
 */
public abstract class TouchHandlerControlPointABC extends TouchHandlerABC {
    private final static String TAG = "TouchHandlerControlPointABC";

    private final boolean activePen;

    private int penID = -1;
    private int fingerId1 = -1;
    private int fingerId2 = -1;
    private float oldPressure, newPressure;
    private float oldX, oldY, newX, newY; // main pointer (usually pen)
    private float oldX1, oldY1, newX1, newY1; // for 1st finger
    private float oldX2, oldY2, newX2, newY2; // for 2nd finger
    private long oldT, newT;

    private GraphicsControlPoint.ControlPoint activeControlPoint = null;
    private GraphicsControlPoint nowEditedGraphics = null;

    private boolean isMove = false;
    private int moveThreshold = 1;
    private int counter = 0;
    public int down_sample_counter = 0;
    private float penDownX, penDownY, penUpX, penUpY;
    private ToolboxViewBuilder toolboxViewBuilder;
    private float lastCenterScreenX, lastCenterScreenY;
    private EventBus mEventBus;

    protected TouchHandlerControlPointABC(HandwriterView view, boolean activePen) {
        super(view);
        this.activePen = activePen;
        view.invalidate(); // make control points appear
        mEventBus = EventBus.getDefault();
    }

    @Override
    protected void interrupt() {
        abortMotion();
        super.interrupt();
    }

    @Override
    protected boolean onTouchEvent(MotionEvent event) {
        if (activePen)
            return onTouchEventActivePen(event);
        else
            return onTouchEventPassivePen(event);
    }

    protected boolean onTouchEventPassivePen(MotionEvent event) {
        // TODO
        return onTouchEventActivePen(event);
    }

    protected GraphicsControlPoint newGraphicsObject = null;

    protected boolean onTouchEventActivePen(MotionEvent event) {

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_MOVE) {

            //if (ToolboxConfiguration.getInstance().getCurrentTool() == Tool.LINE) {
            // Dango 20180921 : down sample for Tool.LINE
            down_sample_counter++;
            if (down_sample_counter % 15 != 0) {
                return false;
            } else {
                down_sample_counter = 0;
            }
            //}

            counter++;

            if (counter >= moveThreshold)
                isMove = true;


            if (getMoveGestureWhileWriting() && fingerId1 != -1 && fingerId2 == -1) {
                int idx1 = event.findPointerIndex(fingerId1);
                if (idx1 != -1) {
                    oldX1 = newX1 = event.getX(idx1);
                    oldY1 = newY1 = event.getY(idx1);
                }
            }
            if (getMoveGestureWhileWriting() && fingerId2 != -1) {
                Assert.assertTrue(fingerId1 != -1);
                int idx1 = event.findPointerIndex(fingerId1);
                int idx2 = event.findPointerIndex(fingerId2);
                if (idx1 == -1 || idx2 == -1)
                    return true;
                newX1 = event.getX(idx1);
                newY1 = event.getY(idx1);
                newX2 = event.getX(idx2);
                newY2 = event.getY(idx2);
                view.invalidate();
                return true;
            }
            if (penID == -1)
                return true;
            int penIdx = event.findPointerIndex(penID);
            if (penIdx == -1)
                return true;

            oldT = newT;
            newT = System.currentTimeMillis();
            // Log.v(TAG, "ACTION_MOVE index="+pen+" pointerID="+penID);
            oldX = newX;
            oldY = newY;
            oldPressure = newPressure;
            newX = event.getX(penIdx);
            newY = event.getY(penIdx);
            newPressure = event.getPressure(penIdx);
            drawOutline(newX, newY);
            return true;
        } else if (action == MotionEvent.ACTION_DOWN) {
            penDownX = event.getX();
            penDownY = event.getY();
            Assert.assertTrue(event.getPointerCount() == 1);
            newT = System.currentTimeMillis();
            if (useForTouch(event) && getDoubleTapWhileWriting() && Math.abs(newT - oldT) < 250) {
                // double-tap
                // view.centerAndFillScreen(event.getX(), event.getY());
                view.zoomOutAndFillScreen();
                abortMotion();
                return true;
            }
            oldT = newT;
            if (useForTouch(event) && getMoveGestureWhileWriting() && event.getPointerCount() == 1) {
                fingerId1 = event.getPointerId(0);
                fingerId2 = -1;
                newX1 = oldX1 = event.getX();
                newY1 = oldY1 = event.getY();
            }
            if (penID != -1) {
                abortMotion();
                return true;
            }
            // Log.v(TAG, "ACTION_DOWN");
            if (!useForWriting(event))
                return true; // eat non-pen events
            penID = event.getPointerId(0);
            activeControlPoint = findControlPoint(event.getX(), event.getY());

            if (activeControlPoint == null) {
                // none within range, create new graphics
                getPage().clearSelectedObjects();
                if (!getPage().nooseArt.isEmpty()) {
                    RectF box = getPage().nooseArt.get(0).getBoundingBox();
                    box.inset(-10, -10);
                    Rect boxRoundOut = new Rect();
                    box.roundOut(boxRoundOut);
                    getPage().nooseArt.clear();
                    getPage().draw(view.canvas, box);
                    CallbackEvent callbackEvent = new CallbackEvent();
                    callbackEvent.setMessage(CallbackEvent.NOOSE_ALL_BTN_GONE);
                    mEventBus.post(callbackEvent);
                    view.invalidate(boxRoundOut);
                }
                newGraphicsObject = newGraphics(event.getX(), event.getY(), event.getPressure());
                newGraphicsObject.setNewOne(true);
                activeControlPoint = newGraphicsObject.initialControlPoint();
                bBox.setEmpty();
            } else {
                if (activeControlPoint.getGraphics() instanceof GraphicsNoose) {
                    lastCenterScreenX = activeControlPoint.screenX();
                    lastCenterScreenY = activeControlPoint.screenY();
                }
                activeControlPoint.getGraphics().backup();
                bBox.set(activeControlPoint.getGraphics().getBoundingBox());
            }
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            penUpX = event.getX();
            penUpY = event.getY();
            Assert.assertTrue(event.getPointerCount() == 1);
            int id = event.getPointerId(0);

            /////////////////////////////
            if (!isMove && (ToolboxConfiguration.getInstance().getCurrentTool() == Tool.IMAGE)) {
                // show control menu
                boolean inBound = isPointInBound(event.getX(), event.getY());
                if (inBound) {
                    view.photoControlDialog.show();

                    if (newGraphicsObject != null)
                        removeGraphics(newGraphicsObject);

                    abortMotion();
                    // reset
                    isMove = false;
                    counter = 0;
                    return true;
                }
            }
            // reset
            isMove = false;
            counter = 0;
            /////////////////////////////

            onPenUp();
            abortMotion();
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            // e.g. you start with finger and use pen
            // if (event.getPointerId(0) != penID) return true;

            abortMotion();
            getPage().draw(view.canvas);
            view.invalidate();
            return true;
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) { // start move gesture
            if (penID != -1)
                return true; // ignore, we are currently moving a control point
            if (fingerId1 == -1)
                return true; // ignore after move finished
            if (fingerId2 != -1)
                return true; // ignore more than 2 fingers
            int idx2 = event.getActionIndex();
            oldX2 = newX2 = event.getX(idx2);
            oldY2 = newY2 = event.getY(idx2);
            float dx = newX2 - newX1;
            float dy = newY2 - newY1;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance >= getMoveGestureMinDistance()) {
                fingerId2 = event.getPointerId(idx2);
            }
            // Log.v(TAG, "ACTION_POINTER_DOWN "+fingerId2+" + "+fingerId1+" "+oldX1+"
            // "+oldY1+" "+oldX2+" "+oldY2);
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            int idx = event.getActionIndex();
            int id = event.getPointerId(idx);
            if (getMoveGestureWhileWriting() && (id == fingerId1 || id == fingerId2) && fingerId1 != -1
                    && fingerId2 != -1) {
                Page page = getPage();

                Transformation t = pinchZoomTransform(page.getTransform(), oldX1, newX1, oldX2, newX2, oldY1, newY1,
                        oldY2, newY2);
                page.setTransform(t, view.canvas);

                page.draw(view.canvas);
                view.invalidate();
                abortMotion();
            }
        }
        return false;
    }

    protected void onPenUp() {

        boolean isNew = (newGraphicsObject != null);

        if (isNew) {
            if (ToolboxConfiguration.getInstance().getCurrentTool() == Tool.IMAGE) {
                view.setNowEditedGraphics(newGraphicsObject);
                float deltaX = Math.abs(penUpX - penDownX);
                float deltaY = Math.abs(penUpY - penDownY);
                if (deltaX > 50 && deltaY > 50) {
                    view.addPhotoControlDialog.show();
                } else {
                    view.remove(newGraphicsObject);
                }
            } else if (ToolboxConfiguration.getInstance().getCurrentTool() == Tool.NOOSE) {
                CallbackEvent callbackEvent = new CallbackEvent();
                RectF newRange = getPage().getSelectedRangeRect(newGraphicsObject.getBoundingBox());
                if (!newRange.isEmpty()) {
                    view.setNowEditedGraphics(newGraphicsObject);
                    ((GraphicsNoose) newGraphicsObject).reSetRang(newRange);
                    getPage().nooseArt.add((GraphicsNoose) newGraphicsObject);
                    callbackEvent.setMessage(CallbackEvent.NOOSE_COPY_AND_DELETE_AND_CUT_BTN_VISIBLE);
                    mEventBus.post(callbackEvent);
                } else {
                    callbackEvent.setMessage(CallbackEvent.NOOSE_ALL_BTN_GONE);
                    mEventBus.post(callbackEvent);
                }

                getPage().draw(view.canvas);

                if (!newRange.isEmpty())
                    getPage().drawSelectedObjects(view.canvas, newRange);

                view.invalidate();
            } else {
                saveGraphics(newGraphicsObject);
            }
            newGraphicsObject.setNewOne(false);
        }

        // if shape is rectangle or oval, redraw shape to square or circle when it is in the restrict range.
        GraphicsControlPoint graphics = activeControlPoint.getGraphics();
        if (ToolboxConfiguration.getInstance().getCurrentTool() == Tool.RECTANGLE
                || ToolboxConfiguration.getInstance().getCurrentTool() == Tool.OVAL) {
            final float dr = graphics.controlPointRadius();
            activeControlPoint.restrictShape();
            RectF newBoundingBox = graphics.getBoundingBox();
            newBoundingBox.inset(-dr, -dr);
            bBox.set(newBoundingBox);
            getPage().draw(view.canvas, bBox);
            bBox.roundOut(rect);
            view.invalidate(rect);
        }

        if (!isNew) {
            if (ToolboxConfiguration.getInstance().getCurrentTool() == Tool.NOOSE) {
                ControlPoint center = graphics.controlPoints.getLast();
                float offsetX = center.screenX() - lastCenterScreenX;
                float offsetY = center.screenY() - lastCenterScreenY;
                getPage().moveSelectedObjects(offsetX, offsetY);
                getPage().draw(view.canvas);
                view.invalidate();

                view.modifyGraphicsList(getPage().get_mSelectedObjects().getAllSelectedGraphics());
            } else {
                view.modifyGraphics(graphics);
            }
        }

        newGraphicsObject = null;
        view.callOnStrokeFinishedListener();
    }

    private void abortMotion() {
        penID = fingerId1 = fingerId2 = -1;
        newGraphicsObject = null;
        activeControlPoint = null;
        getPage().touch();
    }

    @Override
    protected void draw(Canvas canvas, Bitmap bitmap) {
        if (fingerId2 != -1) {
            drawPinchZoomPreview(canvas, bitmap, oldX1, newX1, oldX2, newX2, oldY1, newY1, oldY2, newY2);
        } else {
            canvas.drawBitmap(bitmap, 0, 0, null);
            drawControlPoints(canvas);
        }
    }

    protected void drawControlPoints(Canvas canvas) {
        for (GraphicsControlPoint graphics : getGraphicsObjects()) {
            graphics.drawControlPoints(canvas);
        }
        // If to do invalidate here, the full refresh will not work.
        // view.invalidate();
    }

    /**
     * @return all graphics objects of the given type (e.g. all images) on the page
     */
    protected abstract LinkedList<? extends GraphicsControlPoint> getGraphicsObjects();

    /**
     * Create a new graphics object
     *
     * @param x        initial x position
     * @param y        initial y position
     * @param pressure initial pressure
     * @return a new object derived from GraphicsControlPoint
     */
    protected abstract GraphicsControlPoint newGraphics(float x, float y, float pressure);

    /**
     * Save the graphics object to the current page
     */
    protected void saveGraphics(GraphicsControlPoint graphics) {
        view.saveGraphics(graphics);
    }

    /**
     * Remove the graphics from the current page
     */
    protected void removeGraphics(GraphicsControlPoint graphics) {
        view.removeGraphics(graphics);
    }

    /**
     * Edit an existing graphics object
     */
    protected void editGraphics(GraphicsControlPoint graphics) {
    }

    private final RectF bBox = new RectF();
    private final Rect rect = new Rect();

    private void drawOutline(float newX, float newY) {
        GraphicsControlPoint graphics = activeControlPoint.getGraphics();
        activeControlPoint.move(newX, newY);
        RectF newBoundingBox = graphics.getBoundingBox();
        final float dr = graphics.controlPointRadius();
        if (activeControlPoint.getGraphics() instanceof GraphicsNoose) {
            newBoundingBox.inset(-10, -10);
            if (!getPage().isSelectedObjectsEmpty()) {
                ControlPoint center = graphics.controlPoints.getLast();
                float offsetX = center.screenX() - lastCenterScreenX;
                float offsetY = center.screenY() - lastCenterScreenY;
                lastCenterScreenX = center.screenX();
                lastCenterScreenY = center.screenY();
                getPage().moveSelectedObjects(offsetX, offsetY);
            }
        } else {
            newBoundingBox.inset(-dr, -dr);
        }
        bBox.union(newBoundingBox);
        getPage().draw(view.canvas, bBox);
        graphics.drawAssistLine(view.canvas);
        if (newGraphicsObject != null) {
            newGraphicsObject.getBoundingBox();
            newGraphicsObject.draw(view.canvas);
        }
        bBox.roundOut(rect);
        view.invalidate(rect);
        bBox.set(newBoundingBox);
    }

    /**
     * Maximal distance to select control point (measured in dp)
     */
    protected float maxDistanceControlPointScreen() {
        return 15f;
    }

    /**
     * Maximal distance to select control point (in page coordinate units)
     *
     * @return
     */
    protected float maxDistanceControlPoint() {
        final Transformation transform = getPage().getTransform();
        return maxDistanceControlPointScreen() * view.screenDensity / transform.scale;
    }

    /**
     * Find the closest control point to a given screen position
     *
     * @param xScreen X screen coordinate
     * @param yScreen Y screen coordinate
     * @return The closest ControlPoint or null if there is none within MAX_DISTANCE
     */
    protected ControlPoint findControlPoint(float xScreen, float yScreen) {
        final Transformation transform = getPage().getTransform();
        final float x = transform.inverseX(xScreen);
        final float y = transform.inverseY(yScreen);
        final float rMax = maxDistanceControlPoint();

        float rMin2 = rMax * rMax;
        ControlPoint closest = null;
        for (GraphicsControlPoint graphics : getGraphicsObjects())
            for (GraphicsControlPoint.ControlPoint p : graphics.controlPoints) {
                final float dx = x - p.x;
                final float dy = y - p.y;
                final float r2 = dx * dx + dy * dy;
                if (r2 < rMin2) {
                    rMin2 = r2;
                    closest = p;
                }
            }
        return closest;
    }

    /**
     * Is a given screen position in bound
     *
     * @param xScreen X screen coordinate
     * @param yScreen Y screen coordinate
     * @return is a given screen position in image bound
     */
    private boolean isPointInBound(float xScreen, float yScreen) {

        GraphicsControlPoint graphics = null;
        for (int i = getGraphicsObjects().size() - 1; i >= 0; i--) {
            graphics = getGraphicsObjects().get(i);

            RectF graphicsBoundingBox = graphics.getBoundingBox();

            if ((graphicsBoundingBox.left < xScreen) && (xScreen < graphicsBoundingBox.right)
                    && (graphicsBoundingBox.top < yScreen) && (yScreen < graphicsBoundingBox.bottom)) {
                nowEditedGraphics = graphics;
                view.setNowEditedGraphics(nowEditedGraphics);
                return true;
            }
        }
        return false;
    }

}
