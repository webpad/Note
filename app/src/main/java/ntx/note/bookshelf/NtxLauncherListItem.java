package ntx.note.bookshelf;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import name.vbraun.filepicker.AsyncTaskResult;
import name.vbraun.view.ReturnTouchEventLinearLayout;
import name.vbraun.view.write.GetPagePreviewBitmapAsyncTask;
import ntx.note.data.Book;
import ntx.note2.R;
import utility.ToggleImageButton;

/**
 * Created by karote on 2018/11/14.
 */

public class NtxLauncherListItem extends RelativeLayout {

    public @interface NoteType {
        int CREATE_NOTE = 1;
        int NOTE_BOOK = 2;
        int NOTE_BOOK_CHECKABLE = 3;
    }

    private Context mCtx;
    private int mNoteType;

    private ReturnTouchEventLinearLayout mLayoutLauncherItem;
    private ImageView mIvIcon;
    private TextView mTvTitle;
    private ToggleImageButton mBtnCheck;
    private GetPagePreviewBitmapAsyncTask mGetPagePreviewBitmapAsyncTask;

    public interface ItemClickListener {
        void onClick(Object viewTag);

        void onLongClick(Object viewTag);

        void onCheckedChange(Object viewTag, boolean b);
    }

    private ItemClickListener mCallback;

    public NtxLauncherListItem(Context context) {
        super(context);
        this.mCtx = context;
        initView();
    }

    public NtxLauncherListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCtx = context;
        initView();
    }

    public NtxLauncherListItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mCtx = context;
        initView();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            this.setAlpha(1.0f);
            mLayoutLauncherItem.setEnabled(true);
        } else {
            this.setAlpha(0.2f);
            mLayoutLauncherItem.setEnabled(false);
        }
    }

    private void initView() {
        View.inflate(mCtx, R.layout.ntx_launcher_item, this);

        mLayoutLauncherItem = (ReturnTouchEventLinearLayout) findViewById(R.id.layout_launcher_item);
        mLayoutLauncherItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBtnCheck.getVisibility() == VISIBLE) {
                    mBtnCheck.performClick();
                    return;
                }

                if (mCallback != null)
                    mCallback.onClick(getTag());

            }
        });
        mLayoutLauncherItem.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (NoteType.CREATE_NOTE == mNoteType)
                    return performClick();

                if (mCallback != null)
                    mCallback.onLongClick(getTag());
                return true;
            }
        });

        mIvIcon = (ImageView) findViewById(R.id.iv_icon);
        mTvTitle = (TextView) findViewById(R.id.tv_title);
        mBtnCheck = (ToggleImageButton) findViewById(R.id.btn_check);
        mBtnCheck.setVisibility(INVISIBLE);
        mBtnCheck.setOnCheckedChangeListener(new ToggleImageButton.OnCheckedChangedListener() {
            @Override
            public void onCheckedChange(boolean b) {
                if (mCallback != null) {
                    mCallback.onCheckedChange(getTag(), b);
                }
            }
        });
    }

    public void registerItemTouchEventListener(View.OnTouchListener listener) {
        mLayoutLauncherItem.registerTouchEventCallbackListener(listener);
    }

    public void setType(int noteType, @NonNull String title) {
        this.mNoteType = noteType;
        mIvIcon.getBackground().setLevel(noteType);
        switch (noteType) {
            case NoteType.CREATE_NOTE:
                mIvIcon.setImageBitmap(null);
                mTvTitle.setText(mCtx.getResources().getString(R.string.edit_notebook_title_new));
                mBtnCheck.setVisibility(INVISIBLE);
                break;
            case NoteType.NOTE_BOOK:
                mTvTitle.setText(title);
                mBtnCheck.setVisibility(INVISIBLE);
                break;
            case NoteType.NOTE_BOOK_CHECKABLE:
                mTvTitle.setText(title);
                mBtnCheck.setVisibility(VISIBLE);
                break;
        }
    }

    public void setTitle(String txt) {
        mTvTitle.setText(txt);
    }

    public void setOnItemClickListener(ItemClickListener listener) {
        this.mCallback = listener;
    }

    public void setCheckBoxVisible(boolean visible) {
        mBtnCheck.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

    public void updateCheckBox(boolean isChecked) {
        if (isChecked)
            mBtnCheck.setChecked();
        else
            mBtnCheck.setUnchecked();
    }

    public void updateIconPreview(Book book) {
        mIvIcon.setImageBitmap(null);
        if (book == null) {
            return;
        }

        if (mGetPagePreviewBitmapAsyncTask != null && mGetPagePreviewBitmapAsyncTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
            mGetPagePreviewBitmapAsyncTask.cancel(true);
        }

        mGetPagePreviewBitmapAsyncTask = new GetPagePreviewBitmapAsyncTask(book.getUUID(), 0);
        mGetPagePreviewBitmapAsyncTask.asyncTaskResult = new AsyncTaskResult<Bitmap>() {
            @Override
            public void taskFinish(Bitmap result) {
                mIvIcon.setImageBitmap(result);
            }
        };
        mGetPagePreviewBitmapAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 300, 300);
    }

}
