package name.vbraun.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by karote on 2018/11/21.
 */

public class ReturnTouchEventLinearLayout extends LinearLayout {
    private View.OnTouchListener callback;

    public ReturnTouchEventLinearLayout(Context context) {
        super(context);
    }

    public ReturnTouchEventLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        MotionEvent newEvent = event.copy();
        if (event.getAction() == MotionEvent.ACTION_CANCEL)
            newEvent.setAction(MotionEvent.ACTION_MOVE);


        callback.onTouch(this, newEvent);
        return super.onTouchEvent(event);
    }

    public void registerTouchEventCallbackListener(View.OnTouchListener listener) {
        this.callback = listener;
    }
}
