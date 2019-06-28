package ntx.note.bookshelf;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import ntx.note.RelativePopupWindow;
import ntx.note2.R;

public class HomeNoteLongPressedPopupWindow extends RelativePopupWindow {
    private int mViewId;

    public interface OnBtnClickListener {
        void onBtnClicked(int itemViewId, int actionViewId);
    }

    private OnBtnClickListener mCallback;

    public void setOnBtnClickListener(OnBtnClickListener listener) {
        this.mCallback = listener;
    }

    HomeNoteLongPressedPopupWindow(Activity ctx, int viewId) {
        super(ctx);
        this.mViewId = viewId;
        setContentView(LayoutInflater.from(ctx).inflate(R.layout.popupwindow_home_note_long_pressed, null));
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setOutsideTouchable(true);
        setFocusable(true);
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View popupView = getContentView();

        initView(popupView);
    }

    private void initView(View v) {
        LinearLayout btnRename = (LinearLayout) v.findViewById(R.id.btn_rename);
        LinearLayout btnCopy = (LinearLayout) v.findViewById(R.id.btn_copy);
        LinearLayout btnBackup = (LinearLayout) v.findViewById(R.id.btn_backup);
        LinearLayout btnDelete = (LinearLayout) v.findViewById(R.id.btn_delete);

        btnRename.setOnClickListener(onBtnClickListener);
        btnCopy.setOnClickListener(onBtnClickListener);
        btnBackup.setOnClickListener(onBtnClickListener);
        btnDelete.setOnClickListener(onBtnClickListener);

    }

    private View.OnClickListener onBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCallback != null)
                mCallback.onBtnClicked(mViewId, v.getId());

            dismiss();
        }
    };
}
