package ntx.note.bookshelf;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ntx.note.CallbackEvent;
import ntx.note.data.Book;
import ntx.note.data.Bookshelf;
import ntx.note.export.AlertDialogFragment;
import ntx.note.export.DismissDelayPostAlertDialogFragment;
import ntx.note2.R;
import utility.CustomDialogFragment;

public class RenameNoteDialogFragment extends CustomDialogFragment implements OnClickListener {
    private final static String TAG_SAVE_ALERT_DIALOG = "tag_save_alert_dialog";

    private Activity mActivity;

    private Book mBook;
    private Button okButton;
    private EditText text;
    private boolean mIsLocatedCenter = false;

    public static List<String> titleList = new ArrayList<String>();
    private static EventBus mEventBus;
    private DismissDelayPostAlertDialogFragment mSavingAlertDialogFragment;

    public static RenameNoteDialogFragment newInstance(UUID uuid, boolean isLocatedCenter) {

        RenameNoteDialogFragment frag = new RenameNoteDialogFragment();
        Bundle args = new Bundle();

        args.putString("uuid", uuid.toString());
        args.putBoolean("isLocatedCenter", isLocatedCenter);
        frag.setArguments(args);

        mEventBus = EventBus.getDefault();

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();

        String uuidString = getArguments().getString("uuid", "");
        UUID uuid = UUID.fromString(uuidString);
        mIsLocatedCenter = getArguments().getBoolean("isLocatedCenter");
        mBook = new Book(uuid, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.file_rename_dialog, container, false);
        initView(v);
        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        hideInputMethod();
    }

    private void initView(View v) {

        View viewBlankTop = v.findViewById(R.id.view_blank_top);
        View viewBlankBottom = v.findViewById(R.id.view_blank_bottom);
        FrameLayout layoutMain = (FrameLayout) v.findViewById(R.id.layout_rename_dialog);

        LinearLayout.LayoutParams topBlankLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        LinearLayout.LayoutParams bottomBlankLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        LinearLayout.LayoutParams mainLayoutParams;
        if (mIsLocatedCenter) {
            mainLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mainLayoutParams.weight = 0;
        } else {
            mainLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
            mainLayoutParams.weight = 84;
        }

        topBlankLayoutParams.weight = mIsLocatedCenter ? 50 : 16;
        bottomBlankLayoutParams.weight = mIsLocatedCenter ? 50 : 0;
        viewBlankTop.setLayoutParams(topBlankLayoutParams);
        viewBlankBottom.setLayoutParams(bottomBlankLayoutParams);
        layoutMain.setLayoutParams(mainLayoutParams);

        // clear array
        titleList.clear();

        // add to array
        for (Book book : Bookshelf.getInstance().getBookList()) {
            titleList.add(book.getTitle());
        }

        text = (EditText) v.findViewById(R.id.edit_notebook_title);
        text.setText(mBook.getTitle());
        text.setSelectAllOnFocus(true);
        text.requestFocus();
        showInputMethod();
        text.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    utility.StringIllegal.checkFirstSpaceChar(text);
                    utility.StringIllegal.checkIllegalChar(text);

                    // fix crash, when input null string
                    if (text.getText().toString().equals("")) {
                        okButton.setTextColor(Color.GRAY);// gray
                        okButton.setEnabled(false);
                        return false;
                    }

                    String title = removeLastSpace(text.getText().toString());
                    saveTitle(title);
                    dismiss();
                }
                return false;
            }
        });

        // for cheking text input
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

                utility.StringIllegal.checkFirstSpaceChar(text);
                utility.StringIllegal.checkIllegalChar(text);

                if (s.toString().equals(" ")) {
                    text.setText(""); // fix crash, when input space char
                    okButton.setTextColor(Color.GRAY);
                    okButton.setEnabled(false);
                    return;
                }

                boolean isEqual = false;
                String value = s.toString();

                for (int i = 0; i < titleList.size(); i++) {
                    if (value.equals((String) titleList.get(i))) {
                        okButton.setTextColor(Color.GRAY);// gray
                        okButton.setEnabled(false);
                        isEqual = true;
                    }
                }

                if (!isEqual) {
                    okButton.setTextColor(Color.BLACK);// black
                    okButton.setEnabled(true);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
            }

        });

        okButton = (Button) v.findViewById(R.id.edit_notebook_button);
        Button cancelButton = (Button) v.findViewById(R.id.edit_notebook_cancel);

        okButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        okButton.setTextColor(Color.GRAY);// gray
        okButton.setEnabled(false);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.edit_notebook_button:

                utility.StringIllegal.checkFirstSpaceChar(text);
                utility.StringIllegal.checkIllegalChar(text);

                // fix crash, when input null string
                if (text.getText().toString().equals("")) {
                    okButton.setTextColor(Color.GRAY);// gray
                    okButton.setEnabled(false);
                    break;
                }

                String title = removeLastSpace(text.getText().toString());
                saveTitle(title);

                dismiss();
                break;
            case R.id.edit_notebook_cancel:
                dismiss();
                break;
        }
    }

    private String removeLastSpace(String title) {

        while (title.substring(title.length() - 1).equals(" ") || title.substring(title.length() - 1).equals("　")) {
            title = title.substring(0, title.length() - 1);
        }

        return title;
    }

    @Override
    public void dismiss() {
        hideInputMethod();
        super.dismiss();
    }

    private void saveTitle(String title) {
        mSavingAlertDialogFragment = DismissDelayPostAlertDialogFragment.newInstance(
                mActivity.getResources().getString(R.string.saving),
                2000,
                TAG_SAVE_ALERT_DIALOG);

        FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
        ft.replace(R.id.alert_dialog_container, mSavingAlertDialogFragment, AlertDialogFragment.class.getSimpleName()).commit();

        new SaveTitleAsyncTask().execute(title);
    }

    private void showInputMethod() {
        InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private void hideInputMethod() {
        try{
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mActivity.getWindow().getCurrentFocus().getWindowToken(), 0);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void callEvent(String event) {
        CallbackEvent callbackEvent = new CallbackEvent();
        callbackEvent.setMessage(event);
        mEventBus.post(callbackEvent);
    }

    private class SaveTitleAsyncTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            Book bookObject_forSave;
            if (mBook.getUUID().equals(Bookshelf.getInstance().getCurrentBook().getUUID())) {
                bookObject_forSave = Bookshelf.getInstance().getCurrentBook();
            } else {
                bookObject_forSave = new Book(mBook.getUUID(), true);
            }
            bookObject_forSave.setTitle(strings[0]);
            bookObject_forSave.save();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mSavingAlertDialogFragment.dismiss();
            callEvent(CallbackEvent.RENAME_NOTE_DONE);
        }
    }
}
