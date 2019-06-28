package ntx.note.export;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import name.vbraun.lib.pen.PenEventNTX;
import ntx.note.CallbackEvent;
import ntx.note.Global;
import ntx.note2.R;

import static ntx.note.NoteWriterActivity.DRAWING_ALERT_DIALOG_TAG;

public class DismissDelayPostAlertDialogFragment extends Fragment {
    private static String ARGUMENT_KEY_ALERT_MSG = "alert_msg";
    private static String ARGUMENT_KEY_MIN_DISMISS_DELAY_TIME = "min_dismiss_delay_time";
    private static String ARGUMENT_KEY_ALERT_TAG = "alert_tag";
    private static String AlertTag = DismissDelayPostAlertDialogFragment.class.getSimpleName();

    private String mAlertMsgStr;
    private int mMinDismissDelayTime;

    private FrameLayout mLayout;
    private TextView mTvAlertMsg;

    private EventBus mEventBus = EventBus.getDefault();

    public static DismissDelayPostAlertDialogFragment newInstance(String alertMsg, int minDismissDelayTime, String tag) {
        DismissDelayPostAlertDialogFragment fragment = new DismissDelayPostAlertDialogFragment();
        Bundle args = new Bundle();

        args.putString(ARGUMENT_KEY_ALERT_MSG, alertMsg);
        args.putInt(ARGUMENT_KEY_MIN_DISMISS_DELAY_TIME, minDismissDelayTime);
        args.putString(ARGUMENT_KEY_ALERT_TAG, tag);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEventBus.register(this);
        mAlertMsgStr = getArguments().getString(ARGUMENT_KEY_ALERT_MSG);
        mMinDismissDelayTime = getArguments().getInt(ARGUMENT_KEY_MIN_DISMISS_DELAY_TIME);
        if (getArguments().getString(ARGUMENT_KEY_ALERT_TAG) != null)
            AlertTag = getArguments().getString(ARGUMENT_KEY_ALERT_TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_alert_message_auto_dismiss, container, false);
        initView(v);
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mEventBus.unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CallbackEvent event) {
        if (event.getMessage().equals(CallbackEvent.PAGE_DRAW_COMPLETED)
                && AlertTag.equals(DRAWING_ALERT_DIALOG_TAG)) {
            dismiss();
        }
    }

    private void initView(View v) {
        mLayout = (FrameLayout) v.findViewById(R.id.layout_dialog);
        if (mAlertMsgStr != null && mAlertMsgStr.isEmpty()) {
            mLayout.setVisibility(View.GONE);
        }
        mTvAlertMsg = (TextView) v.findViewById(R.id.tv_alert_msg);
        mTvAlertMsg.setText(mAlertMsgStr);
    }

    public static String getFragmentTag() {
        return AlertTag;
    }

    public void updateAlertMessage(String message) {
        mTvAlertMsg.setText(message);
    }

    public void dismiss() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try{
                    getActivity().getFragmentManager().beginTransaction().remove(DismissDelayPostAlertDialogFragment.this).commit();
                    Global.refresh(getActivity());
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        };
        handler.postDelayed(runnable, mMinDismissDelayTime);
    }
}
