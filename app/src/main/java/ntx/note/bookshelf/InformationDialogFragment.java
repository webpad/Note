package ntx.note.bookshelf;

import android.app.Fragment;
import android.os.Bundle;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.SimpleTimeZone;
import java.util.UUID;

import ntx.note.CallbackEvent;
import ntx.note.data.Book;
import ntx.note2.R;

import static ntx.note.CallbackEvent.RENAME_NOTE;
import static ntx.note.Global.STRING_KB;
import static ntx.note.Global.STRING_MB;

public class InformationDialogFragment extends Fragment {

    private Book mBook;

    public static InformationDialogFragment newInstance(UUID uuid) {

        InformationDialogFragment frag = new InformationDialogFragment();
        Bundle args = new Bundle();

        args.putString("uuid", uuid.toString());
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String uuidString = getArguments().getString("uuid", "");
        UUID uuid = UUID.fromString(uuidString);
        mBook = new Book(uuid, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.file_information_dialog, container, false);
        initView(v);
        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void initView(View v) {
        LinearLayout btnRename = (LinearLayout) v.findViewById(R.id.btn_rename);
        Button btnClose = (Button) v.findViewById(R.id.btn_close);
        btnRename.setOnClickListener(onButtonClickListener);
        btnClose.setOnClickListener(onButtonClickListener);

        TextView tvNoteName = (TextView) v.findViewById(R.id.tv_note_name);
        TextView tvNoteSize = (TextView) v.findViewById(R.id.tv_note_size);
        TextView tvCreatedTime = (TextView) v.findViewById(R.id.tv_created_time);
        TextView tvLastModifiedTime = (TextView) v.findViewById(R.id.tv_last_modified_time);

        tvNoteName.setText(mBook.getTitle());

        long fileSize = mBook.getBookSizeInStorage();
        String sizeStr = "< 1 " + STRING_KB;
        if ((fileSize / 1024f / 1024f) > 1) {
            sizeStr = " " + (int) (fileSize / 1024f / 1024f) + " " + STRING_MB;
        } else if ((fileSize / 1024f) > 1) {
            sizeStr = " " + (int) (fileSize / 1024f) + " " + STRING_KB;
        }
        tvNoteSize.setText(String.valueOf(sizeStr));

        Time cTime = mBook.getCtime();
        Time mTime = mBook.getMtime();
        tvCreatedTime.setText(cTime.format("%Y/%m/%d %H:%M:%S"));
        tvLastModifiedTime.setText(mTime.format("%Y/%m/%d %H:%M:%S"));
    }

    private View.OnClickListener onButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_rename:
                    CallbackEvent event = new CallbackEvent();
                    event.setMessage(RENAME_NOTE);
                    EventBus.getDefault().post(event);
                    break;
                case R.id.btn_close:
                default:
                    break;
            }
            dismiss();
        }
    };

    public void dismiss() {
        getActivity().getFragmentManager().beginTransaction().remove(this).commit();
    }
}
