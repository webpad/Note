package ntx.note.export;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import ntx.note2.R;
import utility.CustomDialogFragment;

public class AlertDialogFragment extends CustomDialogFragment {
    public static int NEGATIVE_DEFAULT_STRING = android.R.string.cancel;

    private static String ARGUMENT_KEY_ALERT_MSG = "alert_msg";
    private static String ARGUMENT_KEY_ALERT_ICON_ID = "icon_id";
    private static String ARGUMENT_KEY_ALERT_BUTTON_ENABLE = "button_enable";
    private static String ARGUMENT_KEY_ALERT_TAG = "alert_tag";
    private static int ALERT_ICON_NO_SHOW = -1;
    private static String AlertTag = AlertDialogFragment.class.getSimpleName();

    private String mAlertMsgStr;
    private int mIconResId;

    private AlertDialogButtonClickListener callback = null;
    private String mBtnClickedCallbackFragmentTag;

    private String mSubMessageString = "";
    private String mNegativeButtonTextString = "";
    private boolean mSubMessageVisible = false;
    private boolean mNegativeButtonVisible = false;
    private boolean mPositiveButtonEnable = true;
    private String mPositiveButtonTextString = "";
    private TextView mTvAlertMsgMain;
    private Button mBtnPositive;
    private ImageView mIvAlertIcon;

    public static AlertDialogFragment newInstance(String alertMsg, @Nullable Integer iconResId, boolean enableButton, String tag) {
        AlertDialogFragment fragment = new AlertDialogFragment();
        Bundle args = new Bundle();

        args.putString(ARGUMENT_KEY_ALERT_MSG, alertMsg);
        if (iconResId == null)
            args.putInt(ARGUMENT_KEY_ALERT_ICON_ID, ALERT_ICON_NO_SHOW);
        else
            args.putInt(ARGUMENT_KEY_ALERT_ICON_ID, iconResId);
        args.putString(ARGUMENT_KEY_ALERT_TAG, tag);
        args.putBoolean(ARGUMENT_KEY_ALERT_BUTTON_ENABLE, enableButton);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlertMsgStr = getArguments().getString(ARGUMENT_KEY_ALERT_MSG);
        mIconResId = getArguments().getInt(ARGUMENT_KEY_ALERT_ICON_ID);
        mPositiveButtonEnable = getArguments().getBoolean(ARGUMENT_KEY_ALERT_BUTTON_ENABLE);
        if (getArguments().getString(ARGUMENT_KEY_ALERT_TAG) != null)
            AlertTag = getArguments().getString(ARGUMENT_KEY_ALERT_TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_alert_message, container, false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        mIvAlertIcon = (ImageView) v.findViewById(R.id.iv_alert_icon);
        if (ALERT_ICON_NO_SHOW == mIconResId)
            mIvAlertIcon.setVisibility(View.GONE);
        else
            mIvAlertIcon.setImageResource(mIconResId);
        mTvAlertMsgMain = (TextView) v.findViewById(R.id.tv_alert_msg_main);
        mTvAlertMsgMain.setText(mAlertMsgStr);
        TextView tvAlertMsgSub = (TextView) v.findViewById(R.id.tv_alert_msg_sub);
        tvAlertMsgSub.setText(mSubMessageString);
        if (mSubMessageVisible)
            tvAlertMsgSub.setVisibility(View.VISIBLE);
        Button btnNegative = (Button) v.findViewById(R.id.btn_negative);
        btnNegative.setText(mNegativeButtonTextString);
        if (mNegativeButtonVisible)
            btnNegative.setVisibility(View.VISIBLE);
        btnNegative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dismiss();

                if (callback != null) {
                    callback.onNegativeButtonClick(mBtnClickedCallbackFragmentTag);
                }
            }
        });

        mBtnPositive = (Button) v.findViewById(R.id.btn_positive);
        if (mPositiveButtonEnable)
            mBtnPositive.setAlpha(1.0f);
        else
            mBtnPositive.setAlpha(0.2f);
        mBtnPositive.setEnabled(mPositiveButtonEnable);
        if (!mPositiveButtonTextString.isEmpty())
            mBtnPositive.setText(mPositiveButtonTextString);
        mBtnPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dismiss();

                if (callback != null)
                    callback.onPositiveButtonClick(mBtnClickedCallbackFragmentTag);
            }
        });
    }

    public static String getFragmentTag() {
        return AlertTag;
    }

    public void setUpSubMessage(String subMessage) {
        if (!subMessage.isEmpty()) {
            mSubMessageString = subMessage;
            mSubMessageVisible = true;
        }
    }

    public void setupNegativeButton(String btnText) {
        if (!btnText.isEmpty())
            mNegativeButtonTextString = btnText;
        mNegativeButtonVisible = true;
    }

    public void setupPositiveButton(String btnText) {
        mPositiveButtonTextString = btnText;
    }

    public void enablePositiveButton(boolean enable) {
        if (enable)
            mBtnPositive.setAlpha(1.0f);
        else
            mBtnPositive.setAlpha(0.2f);

        mBtnPositive.setEnabled(enable);
    }

    public void updateAlertMessage(String message) {
        mTvAlertMsgMain.setText(message);
    }

    public void updateIcon(@Nullable Integer resId) {
        if (resId != null) {
            mIvAlertIcon.setImageResource(resId);
            mIvAlertIcon.setVisibility(View.VISIBLE);
        } else
            mIvAlertIcon.setVisibility(View.GONE);
    }

    public void registerAlertDialogButtonClickListener(AlertDialogButtonClickListener listener, String fragmentTag) {
        this.callback = listener;
        this.mBtnClickedCallbackFragmentTag = fragmentTag;
    }
}
