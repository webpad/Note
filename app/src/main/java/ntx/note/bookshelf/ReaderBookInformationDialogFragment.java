package ntx.note.bookshelf;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import ntx.note2.R;
import utility.CustomDialogFragment;
import utility.EllipsizingTextView;

import static ntx.note.Global.STRING_KB;
import static ntx.note.Global.STRING_MB;

public class ReaderBookInformationDialogFragment extends CustomDialogFragment {
    private final static String ARGUMENT_BOOK_INDEX = "arg_book_index";
    private final static String ARGUMENT_BOOK_TITLE = "arg_book_title";
    private final static String ARGUMENT_BOOK_LANGUAGE = "arg_book_language";
    private final static String ARGUMENT_BOOK_SIZE = "arg_book_size";
    private final static String ARGUMENT_BOOK_TYPE = "arg_book_type";
    private final static String ARGUMENT_BOOK_PATH = "arg_book_path";

    private Activity mActivity;

    private int mBookIndex;
    private String mBookTitle;
    private String mBookLanguage;
    private long mBookSize;
    private String mBookType;
    private String mBookPath;

    public interface OnButtonClickListener {
        void onDeleteBtnClick(int bookIndex);

        void onOpenBtnClick(int bookIndex);
    }

    private OnButtonClickListener mCallback;

    public void setOnButtonClickListener(OnButtonClickListener listener) {
        this.mCallback = listener;
    }

    public static ReaderBookInformationDialogFragment newInstance(RecentlyBookData bookData) {

        ReaderBookInformationDialogFragment frag = new ReaderBookInformationDialogFragment();
        Bundle args = new Bundle();

        args.putInt(ARGUMENT_BOOK_INDEX, bookData.getIndex());
        args.putString(ARGUMENT_BOOK_TITLE, bookData.getTitle());
        args.putString(ARGUMENT_BOOK_LANGUAGE, bookData.getLanguage());
        args.putLong(ARGUMENT_BOOK_SIZE, bookData.getSize());
        args.putString(ARGUMENT_BOOK_TYPE, bookData.getType());
        args.putString(ARGUMENT_BOOK_PATH, bookData.getPath());
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();

        mBookIndex = getArguments().getInt(ARGUMENT_BOOK_INDEX);
        mBookTitle = getArguments().getString(ARGUMENT_BOOK_TITLE, "");
        mBookLanguage = getArguments().getString(ARGUMENT_BOOK_LANGUAGE, "");
        mBookSize = getArguments().getLong(ARGUMENT_BOOK_SIZE, 0);
        mBookType = getArguments().getString(ARGUMENT_BOOK_TYPE, "");
        mBookPath = getArguments().getString(ARGUMENT_BOOK_PATH, "");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_home_book_item_info, container, false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        EllipsizingTextView tvBookTitle = (EllipsizingTextView) v.findViewById(R.id.tv_book_title);
        tvBookTitle.setText(mBookTitle);

        TextView tvBookLanguage = (TextView) v.findViewById(R.id.tv_book_language);
        tvBookLanguage.setText(mBookLanguage);

        TextView tvBookSize = (TextView) v.findViewById(R.id.tv_book_size);
        String sizeStr = "< 1 " + STRING_KB;
        if ((mBookSize / 1024f / 1024f) > 1) {
            sizeStr = " " + (int) (mBookSize / 1024f / 1024f) + " " + STRING_MB;
        } else if ((mBookSize / 1024f) > 1) {
            sizeStr = " " + (int) (mBookSize / 1024f) + " " + STRING_KB;
        }
        tvBookSize.setText(sizeStr);

        TextView tvBookFormat = (TextView) v.findViewById(R.id.tv_book_format);
        tvBookFormat.setText(mBookType);

        EllipsizingTextView tvBookPath = (EllipsizingTextView) v.findViewById(R.id.tv_book_path);
        tvBookPath.setText(mBookPath);


        Button btnClose = (Button) v.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        LinearLayout btnOpenBook = (LinearLayout) v.findViewById(R.id.btn_open_book);
        btnOpenBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null)
                    mCallback.onOpenBtnClick(mBookIndex);
                dismiss();
            }
        });

        LinearLayout btnDeleteBook = (LinearLayout) v.findViewById(R.id.btn_delete_book);
        btnDeleteBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null)
                    mCallback.onDeleteBtnClick(mBookIndex);
                dismiss();
            }
        });
    }

}
