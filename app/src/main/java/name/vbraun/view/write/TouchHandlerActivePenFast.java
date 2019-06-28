package name.vbraun.view.write;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;

import junit.framework.Assert;

import ntx.draw.nDrawHelper;
import ntx.note.Global;
import ntx.note.ToolboxConfiguration;

public class TouchHandlerActivePenFast extends TouchHandlerPenABC {
	private final static String TAG = "TouchHandlerActivePenFast";

	private int penID = -1;
	private int fingerId1 = -1;
	private int fingerId2 = -1;
	private float oldPressure, newPressure;
	private float oldX, oldY, newX, newY;  // main pointer (usually pen)
	private float oldX1, oldY1, newX1, newY1;  // for 1st finger
	private float oldX2, oldY2, newX2, newY2;  // for 2nd finger
	private long oldT, newT;

	protected TouchHandlerActivePenFast(HandwriterView view) {
		super(view);
		init();
	}

	@Override
    public void onResume() {
    }

	@Override
    public void onPause() {
    }

	@Override
	protected void destroy() {
	}

	@Override
	protected void interrupt() {
		super.interrupt();
		penID = fingerId1 = fingerId2 = -1;
	}

	@Override
	protected boolean onTouchEvent(MotionEvent event) {
		int pen_offset_x = view.getPenOffsetX();
		int pen_offset_y = view.getPenOffsetY();

		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_MOVE) {
			int penIdx = event.findPointerIndex(penID);
			if (penIdx == -1) return true;

			// Log.v(TAG, "ACTION_MOVE index="+pen+" pointerID="+penID);
			oldX = newX;
			oldY = newY;
			oldPressure = newPressure;
			newX = event.getX(penIdx) + pen_offset_x;
			newY = event.getY(penIdx) + pen_offset_y;
			newPressure = event.getPressure(penIdx);

			int n = event.getHistorySize();
			if (N+n+1 >= Nmax) saveStroke();
			for (int i = 0; i < n; i++) {
				position_x[N+i] = event.getHistoricalX(penIdx, i) + pen_offset_x;
				position_y[N+i] = event.getHistoricalY(penIdx, i) + pen_offset_y;
				pressure[N+i] = event.getHistoricalPressure(penIdx, i);
			}
			position_x[N+n] = newX;
			position_y[N+n] = newY;
			pressure[N+n] = newPressure;
			N = N+n+1;
		} else if (action == MotionEvent.ACTION_DOWN) {
			Assert.assertTrue(event.getPointerCount() == 1);
			// double-tap
			newT = System.currentTimeMillis();
			if (useForTouch(event) && getDoubleTapWhileWriting() && Math.abs(newT-oldT) < 250) {
//				view.centerAndFillScreen(event.getX(), event.getY());
				view.zoomOutAndFillScreen();
				penID = fingerId1 = fingerId2 = -1;
				return true;
			}
			oldT = newT;

			if (useForTouch(event) && getMoveGestureWhileWriting() && event.getPointerCount()==1) {
				fingerId1 = event.getPointerId(0);
				fingerId2 = -1;
				newX1 = oldX1 = event.getX() + pen_offset_x;
				newY1 = oldY1 = event.getY() + pen_offset_y;
			}
			if (penID != -1) {
				Log.e(TAG, "ACTION_DOWN without previous ACTION_UP");
				penID = -1;
				return true;
			}
			// Log.v(TAG, "ACTION_DOWN");
			if (!useForWriting(event))
				return true;   // eat non-pen events

            startUpdating();

			position_x[0] = newX = event.getX() + pen_offset_x;
			position_y[0] = newY = event.getY() + pen_offset_y;
			pressure[0] = newPressure = event.getPressure();
			N = 1;

			penID = event.getPointerId(0);
			initPenStyle();
		} else if (action == MotionEvent.ACTION_UP) {
			Assert.assertTrue(event.getPointerCount() == 1);

            mUpdateFlag = false;
            clear();

			int id = event.getPointerId(0);
			if (id == penID) {
				saveStroke();
				N = 0;
				view.callOnStrokeFinishedListener();
			} else if (getMoveGestureWhileWriting() &&
						(id == fingerId1 || id == fingerId2) &&
						fingerId1 != -1 && fingerId2 != -1) {
				Page page = getPage();
				Transformation t = pinchZoomTransform(page.getTransform(),
						oldX1, newX1, oldX2, newX2, oldY1, newY1, oldY2, newY2);
				page.setTransform(t, view.canvas);
				page.draw(view.canvas);
				view.invalidate();
			}
			penID = fingerId1 = fingerId2 = -1;
		}

        int historySize = event.getHistorySize();
        for (int i = 0; i < historySize; i++) {
            drawPoint(event.getHistoricalX(i) + pen_offset_x,
                    event.getHistoricalY(i) + pen_offset_y,
                    event.getHistoricalPressure(i),
                    event.getHistoricalSize(i));
        }

        drawPoint(event.getX() + pen_offset_x,
                event.getY() + pen_offset_y,
                event.getPressure(),
                event.getSize());

		return false;
	}

	@Override
	protected void draw(Canvas canvas, Bitmap bitmap) {
		if (fingerId2 != -1) {
			drawPinchZoomPreview(canvas, bitmap, oldX1, newX1, oldX2, newX2, oldY1, newY1, oldY2, newY2);
		} else
			canvas.drawBitmap(bitmap, 0, 0, null);
	}

    private static final int UPDATE_MSG = 1;
    boolean mUpdateFlag = false;
    boolean mStartFlag = false;
    boolean mNewRegionFlag = true;
    private int mEvent = 0;
    private static final int UPDATE_DELAY = 10;
    private int mUpdateType = 0;
    private Rect mRect;
    private Path mPath;

    Paint mPaint;
    private float floatLinewidth = Global.PENCIL_THICKNESS_BASE;//2.0f;

    public void init() {
        pen.setStyle(Paint.Style.STROKE);
        pen.setDither(true);
        pen.setAntiAlias(true);
        pen.setStrokeJoin(Paint.Join.ROUND);
        pen.setStrokeCap(Paint.Cap.ROUND);

        mPath = new Path();

        mRect = new Rect();
        mRect.left = 0;
        mRect.top = 0;
        mRect.right = 0;
        mRect.bottom = 0;

        mlastRect = new Rect();
        mlastRect.left = 0;
        mlastRect.top = 0;
        mlastRect.right = 0;
        mlastRect.bottom = 0;

        mFirstPointFlag = true;
    }


    void startUpdating() {
        mStartFlag = true;
        mUpdateFlag = true;
        invalidateUpdateHandler.removeMessages(UPDATE_MSG);
        invalidateUpdateHandler.sendEmptyMessage(UPDATE_MSG);
    }

    private Handler invalidateUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case UPDATE_MSG: {
                    invalidateUpdate();
                    if (mUpdateFlag == false) {
                        stopUpdating();
                    } else {
                        invalidateUpdateHandler.sendMessageDelayed(
                                invalidateUpdateHandler.obtainMessage(UPDATE_MSG), UPDATE_DELAY);
                    }
                    break;
                }
                default:
                    super.handleMessage(msg);
            }
        }
    };

    void stopUpdating() {
        invalidateUpdateHandler.removeMessages(UPDATE_MSG);
    }


    public synchronized void invalidateUpdate() {
        if (view.canvas != null) {

            int left = mRect.left;
            int top = mRect.top;
            int right = mRect.right;
            int bottom = mRect.bottom;

            if (left < 0)
                left = 0;
            if (top < 0)
                top = 0;
            if (right < 0)
                right = 0;
            if (bottom < 0)
                bottom = 0;

            if (mNewRegionFlag == true) {
                if (mEvent == 1) {
                    mUpdateFlag = false;
                }
			} else {
                nDrawHelper.NDrawSetMaxStrokeWidthWhenUsingPressure(-1);
                pen.setStrokeWidth(ToolboxConfiguration.getInstance().getPenThickness());
                nDrawHelper.NDrawSetStrokeWidth(ToolboxConfiguration.getInstance().getPenThickness());
                view.canvas.drawPath(mPath, pen);
                // Donn't draw the outline when nDraw enalbe
//                if (mUpdateType == 0) {
//                    if (Hardware.isEinkHardwareType()) {
//                        view.invalidate(left, top, right, bottom, PenEventNTX.UPDATE_MODE_PEN);
//                    } else {
//                        view.invalidate(left, top, right, bottom);
//                    }
//                } else {
//                	view.invalidate(left, top, right, bottom);
//                }
                mNewRegionFlag = true;
            }

        }
    }

	private int mCurX;
	private int mCurY;
	private float mCurSize;
	private int mCurWidth;
	boolean mFirstPointFlag = true;
	private Rect mlastRect;
    private synchronized void drawPoint(float x, float y, float pressure, float size) {
        int oldleft = mRect.left;
        int oldtop = mRect.top;
        int oldright = mRect.right;
        int oldbottom = mRect.bottom;

        int newleft = 0;
        int newtop = 0;
        int newright = 0;
        int newbottom = 0;

        int targetleft = 0;
        int targettop = 0;
        int targetright = 0;
        int targetbottom = 0;

        mCurX = (int) x;
        mCurY = (int) y;

        if (mCurY > view.getHeight())
            mCurY = view.getHeight();
        if (mCurY < 0)
            mCurY = 0;
        if (mCurX > view.getWidth())
            mCurX = view.getWidth();
        if (mCurX < 0)
            mCurX = 0;

        mCurSize = size;
        mCurWidth = (int) (mCurSize * (view.getWidth() / 3));
        if (mCurWidth < 1)
            mCurWidth = 1;
        if (view.canvas != null) {
            newleft = mCurX - mCurWidth;
            newtop = mCurY - mCurWidth;
            newright = mCurX + mCurWidth;
            newbottom = mCurY + mCurWidth;

            if (mFirstPointFlag == true) {
                mFirstPointFlag = false;
                oldleft = newleft;
                oldtop = newtop;
                oldright = newright;
                oldbottom = newbottom;
                mPath.reset();
                mPath.moveTo(mCurX, mCurY);
                mNewRegionFlag = false;
            } else {
                if (mNewRegionFlag == true) {
                    if (mStartFlag == true && mEvent == 0) {
                        mStartFlag = false;
                        oldleft = newleft;
                        oldtop = newtop;
                        oldright = newright;
                        oldbottom = newbottom;
                        mPath.reset();
                        mPath.moveTo(mCurX, mCurY);
                    } else {
                        oldleft = mlastRect.left;
                        oldtop = mlastRect.top;
                        oldright = mlastRect.right;
                        oldbottom = mlastRect.bottom;
                    }
                    mNewRegionFlag = false;
                }
            }

            mlastRect.left = newleft;
            mlastRect.top = newtop;
            mlastRect.right = newright;
            mlastRect.bottom = newbottom;

            {
                mPath.lineTo(mCurX, mCurY);
                if (oldleft < newleft)
                    targetleft = oldleft;
                else
                    targetleft = newleft;

                if (oldtop < newtop)
                    targettop = oldtop;
                else
                    targettop = newtop;

                if (oldright < newright)
                    targetright = newright;
                else
                    targetright = oldright;

                if (oldbottom < newbottom)
                    targetbottom = newbottom;
                else
                    targetbottom = oldbottom;
            }

            mRect.set(targetleft, targettop, targetright, targetbottom);
        }
    }

    public void clear() {
        if (view.canvas != null) {

            mPath.reset();

            mUpdateFlag = false;
            mRect.left = 0;
            mRect.top = 0;
            mRect.right = 0;
            mRect.bottom = 0;

            mlastRect.left = 0;
            mlastRect.top = 0;
            mlastRect.right = 0;
            mlastRect.bottom = 0;

            mEvent = 0;
            mFirstPointFlag = true;
            mNewRegionFlag = true;
        }
    }
}
