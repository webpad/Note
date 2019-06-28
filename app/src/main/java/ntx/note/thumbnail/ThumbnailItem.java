package ntx.note.thumbnail;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.UUID;

import name.vbraun.filepicker.AsyncTaskResult;
import name.vbraun.view.write.GetPagePreviewBitmapAsyncTask;
import name.vbraun.view.write.Page;
import ntx.note.data.Book;
import ntx.note.data.BookDirectory;
import ntx.note.data.Bookshelf;
import ntx.note.data.Storage;
import ntx.note.thumbnail.ThumbnailDialogFragment.ThumbnailListStyle;
import ntx.note2.R;
import utility.ToggleImageButton;

import static ntx.note.Global.PAGE_PREVIEW_BITMAP_FILE_NAME_PREFIX;
import static ntx.note.Global.PAGE_PREVIEW_BITMAP_FILE_TYPE;

/**
 * Created by karote on 2018/10/19.
 */

public class ThumbnailItem extends RelativeLayout {
    private Context mCtx;
    private RelativeLayout mRootLayout;
    private ImageView mIvThumbnail;
    private ToggleImageButton mBtnCheckbox;
    private TextView mTvIndex;

    private int[] imageSize = new int[2];

    private GetPagePreviewBitmapAsyncTask mGetPagePreviewBitmapAsyncTask;
    private UUID mBookUuid;
    private int mPageIndex = -1;

    public @interface BorderStyle {
        int FULL_AROUND = 0;
        int RIGHT_BOTTOM = 1;
        int BOTTOM = 2;
    }

    public interface ItemClickListener {
        void onClick(Object viewTag);

        void onCheckedChange(Object viewTag, boolean b);
    }

    private ItemClickListener mCallback;

    public ThumbnailItem(Context context) {
        super(context);
        init(context);
    }

    public ThumbnailItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ThumbnailItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context ctx) {
        this.mCtx = ctx;
        View.inflate(ctx, R.layout.book_thumbnail_page_preview_item, this);

        mRootLayout = (RelativeLayout) findViewById(R.id.layout_root);

        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
//                    ThumbnailItem.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                else
                ThumbnailItem.this.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                int imageWidth = getWidth();
                int imageHeight = Math.round(imageWidth * 1.3f);

                mIvThumbnail.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, imageHeight));
            }
        });

        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBtnCheckbox.getVisibility() == VISIBLE) {
                    mBtnCheckbox.performClick();
                    return;
                }

                if (mCallback != null)
                    mCallback.onClick(getTag());
            }
        });

        mIvThumbnail = (ImageView) findViewById(R.id.iv_page_preview);
        mIvThumbnail.setPadding(1, 1, 1, 1);
        mIvThumbnail.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
//                    mIvThumbnail.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                else
                mIvThumbnail.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                imageSize[0] = mIvThumbnail.getWidth();
                imageSize[1] = mIvThumbnail.getHeight();
                updatePreview(mIvThumbnail.getWidth(), mIvThumbnail.getHeight());
            }
        });

        mBtnCheckbox = (ToggleImageButton) findViewById(R.id.checkbox_page_preview);
        mBtnCheckbox.setOnCheckedChangeListener(new ToggleImageButton.OnCheckedChangedListener() {
            @Override
            public void onCheckedChange(boolean b) {
                if (mCallback != null) {
                    mCallback.onCheckedChange(getTag(), b);
                }
            }
        });

        mTvIndex = (TextView) findViewById(R.id.tv_page_preview_index);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isClickable())
            return super.onTouchEvent(event);
        else
            return true;
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        if (clickable)
            this.setAlpha(1.0f);
        else
            this.setAlpha(0.2f);
    }

    public void setBorderStyle(int style) {
        int resId;
        switch (style) {
            case BorderStyle.RIGHT_BOTTOM:
                resId = R.drawable.bg_thumbnail_right_bottom_bordar;
                break;
            case BorderStyle.BOTTOM:
                resId = R.drawable.bg_thumbnail_bottom_bordar;
                break;
            case BorderStyle.FULL_AROUND:
            default:
                resId = R.drawable.bg_thumbnail_full_around_bordar;
                break;
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        mRootLayout.setBackgroundDrawable(ContextCompat.getDrawable(mCtx, resId));
//        else
//            mRootLayout.setBackground(ContextCompat.getDrawable(mCtx, resId));
    }

    public void setupItemStyle(int style) {
        int[] thumbnailItemAttrs = {android.R.attr.layout_height};
        int itemStyleId = R.style.book_thumbnail_item_insert_style;
        TypedArray itemTypeArray = mCtx.obtainStyledAttributes(itemStyleId, thumbnailItemAttrs);
        @SuppressWarnings("ResourceType")
        int itemHeight = itemTypeArray.getDimensionPixelSize(0, ViewGroup.LayoutParams.MATCH_PARENT);

        if (ThumbnailListStyle.NORMAL_MODE == style)
            itemHeight = ViewGroup.LayoutParams.MATCH_PARENT;

        this.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeight, 1));
    }

    public void setOnItemClickListener(ItemClickListener listener) {
        this.mCallback = listener;
    }

    public void setCheckBoxVisible(boolean visible) {
        mBtnCheckbox.setVisibility(visible ? VISIBLE : GONE);
    }

    public void updateCheckBox(boolean isChecked) {
        if (isChecked)
            mBtnCheckbox.setChecked();
        else
            mBtnCheckbox.setUnchecked();
    }

    public void setBookUuid(UUID bookUuid) {
        this.mBookUuid = bookUuid;
    }

    public void updateIndex(int index) {
        this.mPageIndex = index;
        mTvIndex.setText(String.valueOf(index + 1));
    }

    public void updatePreview() {
        if (imageSize[0] != 0 && imageSize[1] != 0)
            updatePreview(imageSize[0], imageSize[1]);
    }

    private void updatePreview(final int width, final int height) {
        mIvThumbnail.setImageBitmap(null);

        if (mPageIndex < 0)
            return;

        if (mGetPagePreviewBitmapAsyncTask != null && mGetPagePreviewBitmapAsyncTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
            mGetPagePreviewBitmapAsyncTask.cancel(true);
        }

        if (!loadPagePreview()) {
            mGetPagePreviewBitmapAsyncTask = new GetPagePreviewBitmapAsyncTask(mBookUuid, mPageIndex);
            mGetPagePreviewBitmapAsyncTask.asyncTaskResult = new AsyncTaskResult<Bitmap>() {
                @Override
                public void taskFinish(Bitmap result) {
                    mIvThumbnail.setImageBitmap(cutBitmap(result));
                }
            };
            mGetPagePreviewBitmapAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, width, height);
        }
    }

    private boolean loadPagePreview() {
        Book book = new Book(mBookUuid, mPageIndex, 1);
        Page page = book.getPage(0);
        String previewBitmapFileName = PAGE_PREVIEW_BITMAP_FILE_NAME_PREFIX + page.getUUID().toString() + PAGE_PREVIEW_BITMAP_FILE_TYPE;
        BookDirectory dir = Storage.getInstance().getBookDirectory(Bookshelf.getInstance().getCurrentBook().getUUID());
        File previewBitmapFile = new File(dir, previewBitmapFileName);
        Bitmap previewBitmap;

        if (!previewBitmapFile.exists())
            return false;

        previewBitmap = BitmapFactory.decodeFile(previewBitmapFile.getPath());
        if (previewBitmap == null) {
            previewBitmapFile.delete();
            return false;
        }

        mIvThumbnail.setImageBitmap(cutBitmap(previewBitmap));

        return true;
    }

    private Bitmap cutBitmap(Bitmap original) {
        float origin_rate = (float) original.getWidth() / (float) original.getHeight();
        int new_width = (int) (original.getWidth() / 1.2f);
        int new_height = (int) (new_width / origin_rate);

        Bitmap cutBitmap = Bitmap.createBitmap(new_width, new_height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(cutBitmap);
        Rect desRect = new Rect(0, 0, new_width, new_height);
        Rect srcRect = new Rect(0, 0, new_width,
                new_height);
        canvas.drawBitmap(original, srcRect, desRect, null);
        original.recycle();
        original = null;
        return cutBitmap;
    }
}
