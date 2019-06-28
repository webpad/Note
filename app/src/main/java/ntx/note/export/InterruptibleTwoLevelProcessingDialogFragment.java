package ntx.note.export;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import ntx.note2.R;
import utility.CustomDialogFragment;

public class InterruptibleTwoLevelProcessingDialogFragment extends CustomDialogFragment {
    private static String ARGUMENT_KEY_PROGRESS_MESSAGE = "progress_message";
    private static String ARGUMENT_KEY_RESOURCE_ID = "resource_id";
    private static String ARGUMENT_KEY_PROGRESS_MAX_VALUE = "progress_max_value";
    private static String ARGUMENT_KEY_PROGRESS_INTERRUPTABLE = "progress_interruptable";

    private ProgressBar mBarProgress;
    private TextView mTvProgressMessage;
    private TextView mTvProgressItemName;

    private TextView mTvProgressbarProgressPercent;
    private TextView mTvProgress;

    private String mProgressMessage;
    private int mResId;
    private int mProgressMaxValue;
    private boolean mInterruptible;

    private OnInterruptButtonClickListener callback;

    public interface OnInterruptButtonClickListener {
        void onClick();
    }

    public static InterruptibleTwoLevelProcessingDialogFragment newInstance(String progressMessage,
                                                                            int resId,
                                                                            int progressMaxValue,
                                                                            boolean interruptible) {
        InterruptibleTwoLevelProcessingDialogFragment fragment = new InterruptibleTwoLevelProcessingDialogFragment();
        Bundle args = new Bundle();

        args.putString(ARGUMENT_KEY_PROGRESS_MESSAGE, progressMessage);
        args.putInt(ARGUMENT_KEY_RESOURCE_ID, resId);
        args.putInt(ARGUMENT_KEY_PROGRESS_MAX_VALUE, progressMaxValue);
        args.putBoolean(ARGUMENT_KEY_PROGRESS_INTERRUPTABLE, interruptible);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProgressMessage = getArguments().getString(ARGUMENT_KEY_PROGRESS_MESSAGE);
        mResId = getArguments().getInt(ARGUMENT_KEY_RESOURCE_ID);
        mProgressMaxValue = getArguments().getInt(ARGUMENT_KEY_PROGRESS_MAX_VALUE);
        mInterruptible = getArguments().getBoolean(ARGUMENT_KEY_PROGRESS_INTERRUPTABLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_interruptible_2_level_processing, container, false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        mTvProgressMessage = (TextView) v.findViewById(R.id.tv_progress_message);
        mTvProgressMessage.setText(mProgressMessage);
        ImageView ivIcon = (ImageView) v.findViewById(R.id.iv_icon);
        ivIcon.setImageResource(mResId);
        mTvProgressItemName = (TextView) v.findViewById(R.id.tv_progress_item_name);
        mBarProgress = (ProgressBar) v.findViewById(R.id.progressbar_progress);
        mTvProgress = (TextView) v.findViewById(R.id.tv_progress);
        Button btnInterrupt = (Button) v.findViewById(R.id.btn_interrupt);
        btnInterrupt.setVisibility(mInterruptible ? View.VISIBLE : View.GONE);
        btnInterrupt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (callback != null)
                    callback.onClick();
                dismiss();
            }
        });

        mBarProgress.setMax(100);
        mBarProgress.setProgress(0);
        mTvProgressbarProgressPercent = (TextView) v.findViewById(R.id.tv_progressbar_progress_percent);
        mTvProgress.setText(getProgressPercentTextStr(0));
        mTvProgress.setText(getProgressTextStr(0, mProgressMaxValue));
    }

    private String getProgressTextStr(int progress, int maxValue) {
        return String.valueOf(progress) + "/" + String.valueOf(maxValue);
    }

    private String getProgressPercentTextStr(int progress) {
        return String.valueOf(progress) + "%";
    }

    public void setProgressMessage(String message) {
        this.mTvProgressMessage.setText(message);
    }

    public void setProgressItemName(String itemName) {
        this.mTvProgressItemName.setText(itemName);
    }

    public void updateLv1Progress(int progress) {
        mBarProgress.setProgress(progress);
        mTvProgressbarProgressPercent.setText(getProgressPercentTextStr(progress));
    }

    public void updateLv2Progress(int progress) {
        mTvProgress.setText(getProgressTextStr(progress, mProgressMaxValue));
    }

    public void setOnInterruptButtonClickListener(OnInterruptButtonClickListener listener) {
        this.callback = listener;
    }
}
