package ntx.note.export;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import ntx.note.CallbackEvent;
import ntx.note.data.Bookshelf;
import ntx.note2.R;

public class SaveCompleteAutoDismissAlertDialogFragment extends Fragment {
    private static String AlertTag = SaveCompleteAutoDismissAlertDialogFragment.class.getSimpleName();

    private long time_saveStart;

    private EventBus mEventBus = EventBus.getDefault();

    public static SaveCompleteAutoDismissAlertDialogFragment newInstance() {
        return new SaveCompleteAutoDismissAlertDialogFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEventBus.register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_alert_message_auto_dismiss, container, false);
        initView(v);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        time_saveStart = System.currentTimeMillis();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Bookshelf.getInstance().getCurrentBook().save();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mEventBus.unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CallbackEvent event) {
        if (event.getMessage().equals(CallbackEvent.SAVE_RECENTLY_NOTE_JSON_DONE)) {
            long time_saveStop = System.currentTimeMillis();
            long time_elapse = time_saveStop - time_saveStart;
            if (time_elapse < 2000) {
                try {
                    Thread.sleep(2000 - time_elapse);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                getActivity().finish();
            } else {
                getActivity().finish();
            }
        }
    }

    private void initView(View v) {
        TextView tvAlertMsg = (TextView) v.findViewById(R.id.tv_alert_msg);
        tvAlertMsg.setText(getString(R.string.saving));
    }
}
