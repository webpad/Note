package name.vbraun.view.write;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

import name.vbraun.lib.pen.Hardware;
import ntx.draw.nDrawHelper;
import ntx.note.CallbackEvent;
import ntx.note.Global;
import ntx.note.data.BookDirectory;
import ntx.note.data.Bookshelf;
import ntx.note.data.Storage;
import ntx.note2.R;

import static ntx.note.Global.PAGE_PREVIEW_BITMAP_FILE_NAME_PREFIX;
import static ntx.note.Global.PAGE_PREVIEW_BITMAP_FILE_TYPE;

public class FastView extends RelativeLayout {
    public static boolean loadPagePreviewBitmapSuccess = false;

    private ImageView mIvFastView;

    private Page page;
    private Bitmap bitmap;

    private float x1;

    private EventBus mEventBus;

    public FastView(Context context) {
        super(context);
        init(context);
    }

    public FastView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FastView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if (View.VISIBLE == visibility)
            nDrawHelper.NDrawSwitch(false);

        super.onVisibilityChanged(changedView, visibility);
    }

    @Override
    protected void onAttachedToWindow() {
        nDrawHelper.NDrawSwitch(false);
        super.onAttachedToWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (bitmap != null)
            mIvFastView.setImageBitmap(bitmap);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (Hardware.isEinkHandWritingHardwareType()) {
            if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                if (Global.swipeNoteWriter) {
                    int distanceThreshold = 0;
                    if (Hardware.isEinkUsingLargerUI())
                        distanceThreshold = Global.SWIPE_DISTANCE_THRESHOLD;// *4/3;
                    else
                        distanceThreshold = Global.SWIPE_DISTANCE_THRESHOLD;

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            x1 = event.getX();
                            break;
                        case MotionEvent.ACTION_UP:
                            float x2 = event.getX();
                            float deltaX = x2 - x1;
                            if ((Math.abs(deltaX) > distanceThreshold) && (deltaX > 0)) {
                                CallbackEvent prevPageCallbackEvent = new CallbackEvent();
                                prevPageCallbackEvent.setMessage(CallbackEvent.PREV_PAGE);
                                mEventBus.post(prevPageCallbackEvent);
                            } else if ((Math.abs(deltaX) > distanceThreshold) && (deltaX < 0)) {
                                CallbackEvent nextPageCallbackEvent = new CallbackEvent();
                                nextPageCallbackEvent.setMessage(CallbackEvent.NEXT_PAGE);
                                mEventBus.post(nextPageCallbackEvent);
                            }
                            break;
                    }
                }
                return true;
            }
        }

        return true;
    }

    private void init(Context context) {
        mEventBus = EventBus.getDefault();

        View.inflate(context, R.layout.fast_view, this);

        mIvFastView = (ImageView) findViewById(R.id.iv_fast_view);
    }

    public void setPageAndZoomOut(Page new_page) {
        if (new_page == null)
            return;
        page = new_page;

        loadPagePreviewBitmapSuccess = loadPagePreview();

        invalidate();
    }

    private boolean loadPagePreview() {
        String previewBitmapFileName = PAGE_PREVIEW_BITMAP_FILE_NAME_PREFIX + page.getUUID().toString() + PAGE_PREVIEW_BITMAP_FILE_TYPE;
        BookDirectory dir = Storage.getInstance().getBookDirectory(Bookshelf.getInstance().getCurrentBook().getUUID());
        File previewBitmapFile = new File(dir, previewBitmapFileName);

        if (!previewBitmapFile.exists())
            return false;

        bitmap = BitmapFactory.decodeFile(previewBitmapFile.getPath());
        if (bitmap == null) {
            previewBitmapFile.delete();
            return false;
        }

        mIvFastView.setImageBitmap(bitmap);

        return true;
    }
}
