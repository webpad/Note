package ntx.note;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import name.vbraun.view.write.TouchHandlerPenABC;
import ntx.draw.nDrawHelper;
import ntx.note2.R;

public class RelativePopupWindow extends PopupWindow {
    private static boolean isShowing = false;

    public @interface VerticalPosition {
        int CENTER = 0;
        int ABOVE = 1;
        int BELOW = 2;
        int ALIGN_TOP = 3;
        int ALIGN_BOTTOM = 4;
    }

    public @interface HorizontalPosition {
        int CENTER = 0;
        int LEFT = 1;
        int RIGHT = 2;
        int ALIGN_LEFT = 3;
        int ALIGN_RIGHT = 4;
    }

    private View contentView;
    private float pxToDpScale;
    private int paddingDpAsPixels_Top, paddingDpAsPixels_Right, paddingDpAsPixels_Bottom, paddingDpAsPixels_Left;
    private Handler mHandler = new Handler();
    private Activity mCtx;

    private Runnable nDrawSwitchOff = new Runnable() {
        @Override
        public void run() {
            nDrawHelper.NDrawSwitch(false);
        }
    };

    public RelativePopupWindow(Activity context) {
        super(context);
        this.mCtx = context;
        pxToDpScale = context.getResources().getDisplayMetrics().density;
    }

    public static boolean isPopupWindowShowing() {
        return isShowing;
    }

    private static int makeDropDownMeasureSpec(int measureSpec) {
        int mode;
        if (measureSpec == ViewGroup.LayoutParams.WRAP_CONTENT) {
            mode = View.MeasureSpec.UNSPECIFIED;
        } else {
            mode = View.MeasureSpec.EXACTLY;
        }
        return View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(measureSpec), mode);
    }

    public void showOnAnchor(View anchor, int horizPos, int vertPos) {
        contentView = getContentView();
        int x = 0, y = 0;
        showWindow(anchor, horizPos, vertPos, x, y);
    }

    public void showOnAnchor(View anchor, int horizPos, int vertPos, int offsetX, int offsetY) {
        contentView = getContentView();
        int x = 0, y = 0;
        x += offsetX;
        y += offsetY;
        showWindow(anchor, horizPos, vertPos, x, y);
    }

    public void showOnAnchor(View anchor, int horizPos, int vertPos, int padL, int padT, int padR, int padB) {
        contentView = getContentView();
        setBackgroundPadding(contentView, padL, padT, padR, padB);
        int x = 0, y = 0;
        showWindow(anchor, horizPos, vertPos, x, y);
    }

    private void showWindow(View anchor, int horizPos, int vertPos, int x, int y) {
        isShowing = true;
        nDrawHelper.NDrawSwitch(false);
        TouchHandlerPenABC.isPopupwindow = true;
        mHandler.postDelayed(nDrawSwitchOff, 100);
        contentView.measure(makeDropDownMeasureSpec(getWidth()), makeDropDownMeasureSpec(getHeight()));
        final int measuredW = contentView.getMeasuredWidth();
        final int measuredH = contentView.getMeasuredHeight();
        switch (vertPos) {
            case VerticalPosition.ABOVE:
                y -= measuredH + anchor.getHeight();
                break;
            case VerticalPosition.ALIGN_BOTTOM:
                y -= measuredH + paddingDpAsPixels_Top;
                break;
            case VerticalPosition.CENTER:
                y -= anchor.getHeight() / 2 + measuredH / 2 + paddingDpAsPixels_Top;
                break;
            case VerticalPosition.ALIGN_TOP:
                y -= anchor.getHeight() + paddingDpAsPixels_Top;
                break;
            case VerticalPosition.BELOW:
                // Default position.
                break;
        }
        switch (horizPos) {
            case HorizontalPosition.LEFT:
                x -= measuredW;
                break;
            case HorizontalPosition.ALIGN_RIGHT:
                x -= measuredW - anchor.getWidth();
                break;
            case HorizontalPosition.CENTER:
                x += anchor.getWidth() / 2 - measuredW / 2;
                break;
            case HorizontalPosition.ALIGN_LEFT:
                // Default position.
                break;
            case HorizontalPosition.RIGHT:
                x += anchor.getWidth();
                break;
        }
        showAsDropDown(anchor, x, y, Gravity.START);
    }

    @Override
    public void dismiss() {
        Global.refresh(mCtx);
        isShowing = false;
        super.dismiss();
    }

    private void setBackgroundPadding(View v, int padL, int padT, int padR, int padB) {

        FrameLayout backgroundLayout = (FrameLayout) v.findViewById(R.id.layout_background);

        if (backgroundLayout == null) {
            ALog.e("NULL", "PopupWindow not set FrameLayout, id:layout_background");
            return;
        }

        paddingDpAsPixels_Left = (int) (padL * pxToDpScale + 0.5f);
        paddingDpAsPixels_Top = (int) (padT * pxToDpScale + 0.5f);
        paddingDpAsPixels_Right = (int) (padR * pxToDpScale + 0.5f);
        paddingDpAsPixels_Bottom = (int) (padB * pxToDpScale + 0.5f);

        backgroundLayout.setPadding(paddingDpAsPixels_Left, paddingDpAsPixels_Top, paddingDpAsPixels_Right,
                paddingDpAsPixels_Bottom);
    }

}
