package utility;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import ntx.note2.R;

@SuppressLint("AppCompatCustomView")
public class ToggleImageButton extends ImageButton implements View.OnClickListener {
    private static final String TAG = "ToggleImageButton";

    public static final boolean STATE_CHECKED = true;
    public static final boolean STATE_UNCHECKED = false;

    private boolean mState = STATE_UNCHECKED;
    private int mCheckedRes = 0;
    private int mUncheckedRes = 0;

    private OnCheckedChangedListener mCallbacks = null;

    public interface OnCheckedChangedListener {
        void onCheckedChange(boolean value);
    }


    public ToggleImageButton(Context context) {
        super(context);
        obtainStyledAttributes(context, null);
    }

    public ToggleImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        obtainStyledAttributes(context, attrs);
    }

    public ToggleImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        obtainStyledAttributes(context, attrs);
    }

    private void obtainStyledAttributes(final Context context, final AttributeSet attrs) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ToggleImageButton, 0, 0);

        try {
            mState = typedArray.getBoolean(R.styleable.ToggleImageButton_check_state, STATE_UNCHECKED);
            mCheckedRes = typedArray.getResourceId(R.styleable.ToggleImageButton_src_checked, 0);
            mUncheckedRes = typedArray.getResourceId(R.styleable.ToggleImageButton_src_unchecked, 0);

        } finally {
            typedArray.recycle();
        }

        if (mState)
            setImage(mCheckedRes);
        else
            setImage(mUncheckedRes);


        this.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {

        if (mState) {
            mState = STATE_UNCHECKED;
            setImage(mUncheckedRes);

        } else {
            mState = STATE_CHECKED;
            setImage(mCheckedRes);
        }

        if (mCallbacks != null)
            mCallbacks.onCheckedChange(mState);

    }

    private void setImage(int resID) {

        if (resID != 0) {
            this.setImageResource(resID);

        } else {
            Log.i(TAG, "setImage: No image resource provided");
        }
    }

    public void setOnCheckedChangeListener(OnCheckedChangedListener listener) {
        mCallbacks = listener;
    }

    public void setChecked() {
        setChecked(false);
    }

    public void setUnchecked() {
        setUnchecked(false);
    }

    public void setChecked(boolean callback) {
        mState = STATE_CHECKED;
        setImage(mCheckedRes);

        if (callback && mCallbacks != null)
            mCallbacks.onCheckedChange(mState);
    }

    public void setUnchecked(boolean callback) {
        mState = STATE_UNCHECKED;
        setImage(mUncheckedRes);

        if (callback && mCallbacks != null)
            mCallbacks.onCheckedChange(mState);
    }

    public boolean isChecked() {
        return mState;
    }
}
