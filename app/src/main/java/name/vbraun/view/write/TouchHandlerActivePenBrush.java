package name.vbraun.view.write;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;

import junit.framework.Assert;

import ntx.note.ALog;

/**
 * Touch handler for an active pen (stylus with digitizer) that can distinguish stylus from finger touches
 * @author Artis to implement calligraphy brush pen
 *
 */
public class TouchHandlerActivePenBrush extends TouchHandlerPenABC {
	private final static String TAG = "TouchHandlerActivePen";

	private int penID = -1;
	private int fingerId1 = -1;
	private int fingerId2 = -1;
	private float oldPressure, newPressure;
	private float oldX, oldY, newX, newY;  // main pointer (usually pen)
	private float oldX1, oldY1, newX1, newY1;  // for 1st finger
	private float oldX2, oldY2, newX2, newY2;  // for 2nd finger
	private long oldT, newT;

	protected TouchHandlerActivePenBrush(HandwriterView view) {
		super(view);
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

			if (getMoveGestureWhileWriting() && fingerId1 != -1 && fingerId2 == -1) {
				int idx1 = event.findPointerIndex(fingerId1);
				if (idx1 != -1) {
					oldX1 = newX1 = event.getX(idx1) + pen_offset_x;
					oldY1 = newY1 = event.getY(idx1) + pen_offset_y;
				}
			}
			if (getMoveGestureWhileWriting() && fingerId2 != -1) {
				Assert.assertTrue(fingerId1 != -1);
				int idx1 = event.findPointerIndex(fingerId1);
				int idx2 = event.findPointerIndex(fingerId2);
				if (idx1 == -1 || idx2 == -1) return true;
				newX1 = event.getX(idx1) + pen_offset_x;
				newY1 = event.getY(idx1) + pen_offset_y;
				newX2 = event.getX(idx2) + pen_offset_x;
				newY2 = event.getY(idx2) + pen_offset_y;
				view.invalidate();
				return true;
			}
			if (penID == -1 || N == 0) return true;
			int penIdx = event.findPointerIndex(penID);
			if (penIdx == -1) return true;

			// Log.v(TAG, "ACTION_MOVE index="+pen+" pointerID="+penID);
			oldX = newX;
			oldY = newY;
			oldPressure = newPressure;
			newX = event.getX(penIdx) + pen_offset_x;
			newY = event.getY(penIdx) + pen_offset_y;
			newPressure = event.getPressure(penIdx);


			//artis test
//			RectF mRectF = new RectF();
//			mRectF.set( position_x[0]-50f, position_y[0]-50f, newX+50f, newY+50f );
//
//			Page page = getPage();
//			Transformation t = pinchZoomTransform(page.getTransform(),
//					oldX1, newX1, oldX2, newX2, oldY1, newY1, oldY2, newY2);
//			page.setTransform(t, view.canvas);
//			page.draw(view.canvas );
//			page.draw(view.canvas, mRectF );




//			drawOutline(oldX, oldY, newX, newY, oldPressure, newPressure);

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
			return true;
		} else if (action == MotionEvent.ACTION_DOWN) {
			Assert.assertTrue(event.getPointerCount() == 1);
			ALog.i(TAG, "THAPB<-----ACTION_DOWN: Got "+N+" points.");
			newT = System.currentTimeMillis();
			if (useForTouch(event) && getDoubleTapWhileWriting() && Math.abs(newT-oldT) < 250) {
				// double-tap
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
			position_x[0] = newX = event.getX() + pen_offset_x;
			position_y[0] = newY = event.getY() + pen_offset_y;
			pressure[0] = newPressure = event.getPressure();
			N = 1;
			penID = event.getPointerId(0);
			initPenStyle();
			return true;
		} else if (action == MotionEvent.ACTION_UP) {
			Assert.assertTrue(event.getPointerCount() == 1);
			int id = event.getPointerId(0);
			if (id == penID) {
				ALog.i(TAG, "THAPB<-----ACTION_UP penID: Got "+N+" points." );
				saveStroke();
				N = 0;
				view.callOnStrokeFinishedListener();

			} else if (getMoveGestureWhileWriting() &&
						(id == fingerId1 || id == fingerId2) &&
						fingerId1 != -1 && fingerId2 != -1) {
				ALog.i(TAG, "THAPB<-----ACTION_UP gesture: Got "+N+" points.");
				Page page = getPage();
				Transformation t = pinchZoomTransform(page.getTransform(),
						oldX1, newX1, oldX2, newX2, oldY1, newY1, oldY2, newY2);
				page.setTransform(t, view.canvas);
				page.draw(view.canvas);
				view.invalidate();
			}
			penID = fingerId1 = fingerId2 = -1;

			// artis
//			if (Hardware.isEinkHardwareType()) {
//				view.invalidate( PenEventNTX.UPDATE_MODE_PARTIAL_GC16 );
//				ALog.e(TAG, "THAPB<-----ACTION_UP EPD GC16 to remove strokes" );
//			} else {
//				view.invalidate();
//			}
			return true;
		} else if (action == MotionEvent.ACTION_CANCEL) {
			// e.g. you start with finger and use pen
			// if (event.getPointerId(0) != penID) return true;
			Log.v(TAG, "ACTION_CANCEL");
			N = 0;
			penID = fingerId1 = fingerId2 = -1;
			getPage().draw(view.canvas);
			view.invalidate();
			return true;
		} else if (action == MotionEvent.ACTION_POINTER_DOWN) {  // start move gesture
			if (fingerId1 == -1) return true; // ignore after move finished
			if (fingerId2 != -1) return true; // ignore more than 2 fingers
			int idx2 = event.getActionIndex();
			oldX2 = newX2 = event.getX(idx2) + pen_offset_x;
			oldY2 = newY2 = event.getY(idx2) + pen_offset_y;
			float dx = newX2-newX1;
			float dy = newY2-newY1;
			float distance = (float) Math.sqrt(dx*dx+dy*dy);
			if (distance >= getMoveGestureMinDistance()) {
				fingerId2 = event.getPointerId(idx2);
			}
			// Log.v(TAG, "ACTION_POINTER_DOWN "+fingerId2+" + "+fingerId1+" "+oldX1+" "+oldY1+" "+oldX2+" "+oldY2);
		}
		return false;
	}

	@Override
	protected void draw(Canvas canvas, Bitmap bitmap) {
		if (fingerId2 != -1) {
			drawPinchZoomPreview(canvas, bitmap, oldX1, newX1, oldX2, newX2, oldY1, newY1, oldY2, newY2);
		} else
			canvas.drawBitmap(bitmap, 0, 0, null);
	}


}
