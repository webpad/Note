package ntx.note.export;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import ntx.note2.R;
import utility.CustomDialogFragment;

public class InterruptibleProgressingDialogFragment extends CustomDialogFragment {
    private static String ARGUMENT_KEY_PROGRESS_MESSAGE = "progress_message";
    private static String ARGUMENT_KEY_PROGRESS_MAX_VALUE = "progress_max_value";
    private static String ARGUMENT_KEY_PROGRESS_INTERRUPTABLE = "progress_interruptable";

    private ProgressBar mBarProgress;
    private TextView mTvProgress;

    private String mProgressMessage;
    private int mProgressMaxValue;
    private boolean mInterruptible;

    private OnInterruptButtonClickListener callback;

    public interface OnInterruptButtonClickListener {
        void onClick();
    }

    public static InterruptibleProgressingDialogFragment newInstance(String progressMessage, int progressMaxValue,
                                                                     boolean interruptible) {
        InterruptibleProgressingDialogFragment fragment = new InterruptibleProgressingDialogFragment();
        Bundle args = new Bundle();

        args.putString(ARGUMENT_KEY_PROGRESS_MESSAGE, progressMessage);
        args.putInt(ARGUMENT_KEY_PROGRESS_MAX_VALUE, progressMaxValue);
        args.putBoolean(ARGUMENT_KEY_PROGRESS_INTERRUPTABLE, interruptible);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProgressMessage = getArguments().getString(ARGUMENT_KEY_PROGRESS_MESSAGE);
        mProgressMaxValue = getArguments().getInt(ARGUMENT_KEY_PROGRESS_MAX_VALUE);
        mInterruptible = getArguments().getBoolean(ARGUMENT_KEY_PROGRESS_INTERRUPTABLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_interruptible_progressing, container, false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        TextView tvProgressMessage = (TextView) v.findViewById(R.id.tv_progress_message);
        tvProgressMessage.setText(mProgressMessage);
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

        mBarProgress.setMax(mProgressMaxValue);
        mBarProgress.setProgress(0);
        mTvProgress.setText(getProgressTextStr(0, mProgressMaxValue));
    }

    private String getProgressTextStr(int progress, int maxValue) {
        return String.valueOf(progress) + "/" + String.valueOf(maxValue);
    }

    public void updateProgress(int progress) {
        mBarProgress.setProgress(progress);
        mTvProgress.setText(getProgressTextStr(progress, mProgressMaxValue));
    }

    public void setOnInterruptButtonClickListener(OnInterruptButtonClickListener listener) {
        this.callback = listener;
    }
}
