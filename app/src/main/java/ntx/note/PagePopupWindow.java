package ntx.note;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import org.greenrobot.eventbus.EventBus;

import ntx.note2.R;

class PagePopupWindow extends RelativePopupWindow {

    private SeekBar mSeekBar;

    private EventBus mEventBus;

    private int mCurrentPage = 1;
    private int mTotalPage = 1;
    private ImageView mBtnPrevPage;
    private ImageView mBtnNextPage;
    private ImageView mBtnPrevPageFirst;
    private ImageView mBtnNextPageEnd;

    PagePopupWindow(Activity ctx, int width, int currentPage, int totalPage) {
        super(ctx);
        mEventBus = EventBus.getDefault();

        setContentView(LayoutInflater.from(ctx).inflate(R.layout.popupwindow_page, null));
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setWidth(width);
        setOutsideTouchable(false);
        setFocusable(false);
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View popupView = getContentView();

        initView(popupView, currentPage, totalPage);
    }

    private void initView(View v, int currentPage, int totalPage) {
        mBtnPrevPage = (ImageView) v.findViewById(R.id.btn_toolbox_prev_page);
        mBtnNextPage = (ImageView) v.findViewById(R.id.btn_toolbox_next_page);
        mBtnPrevPageFirst = (ImageView) v.findViewById(R.id.btn_toolbox_prev_first_page);
        mBtnNextPageEnd = (ImageView) v.findViewById(R.id.btn_toolbox_next_end_page);

        Button btnOK = (Button) v.findViewById(R.id.btn_page_close);

        mSeekBar = (SeekBar) v.findViewById(R.id.seekBar_thickness);
        initSeekBar(currentPage, totalPage);

        mBtnPrevPage.setOnClickListener(onBtnClickListener);
        mBtnNextPage.setOnClickListener(onBtnClickListener);
        mBtnPrevPageFirst.setOnClickListener(onBtnClickListener);
        mBtnNextPageEnd.setOnClickListener(onBtnClickListener);
        btnOK.setOnClickListener(onBtnClickListener);

    }

    private View.OnClickListener onBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int seekBarMaxValue = mSeekBar.getMax();
            int seekBarProgress = mSeekBar.getProgress();
            switch (v.getId()) {
                case R.id.btn_toolbox_prev_page:
                    if (seekBarProgress > 0)
                        seekBarProgress--;
                    else
                        seekBarProgress = 0;

                    if (mTotalPage == 1)
                        mSeekBar.setProgress(1);
                    else
                        mSeekBar.setProgress(seekBarProgress);
                    break;

                case R.id.btn_toolbox_next_page:
                    if (seekBarProgress < seekBarMaxValue)
                        seekBarProgress++;
                    else
                        seekBarProgress = seekBarMaxValue;

                    if (mTotalPage == 1)
                        mSeekBar.setProgress(1);
                    else
                        mSeekBar.setProgress(seekBarProgress);
                    break;

                case R.id.btn_toolbox_prev_first_page:
                    if (mTotalPage == 1)
                        mSeekBar.setProgress(1);
                    else
                        mSeekBar.setProgress(0);
                    dismiss();
                    break;

                case R.id.btn_toolbox_next_end_page:
                    mSeekBar.setProgress(seekBarMaxValue);
                    dismiss();
                    break;
                case R.id.btn_page_close:
                    dismiss();
                    break;
            }
        }
    };

    private void callEvent(String event) {
        CallbackEvent callbackEvent = new CallbackEvent();
        callbackEvent.setMessage(event);
        mEventBus.post(callbackEvent);
    }

    private void initSeekBar(int pNum, int total) {
        mCurrentPage = pNum;
        mTotalPage = total;
        invalidatePageBtn();

        if (total == 1) { // only one page
            mSeekBar.setMax(1);
            mSeekBar.setProgress(1);
        } else {
            mSeekBar.setMax(mTotalPage - 1);
            mSeekBar.setProgress(mCurrentPage);
        }

        mSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
    }

    public int getPageNumber() {
        return mCurrentPage;
    }

    private OnSeekBarChangeListener onSeekBarChangeListener = new OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            callEvent(CallbackEvent.SEEKBAR_PAGE);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (mTotalPage == 1)
                mCurrentPage = 0;
            else
                mCurrentPage = progress;

            if (fromUser)
                callEvent(CallbackEvent.SEEKBAR_PROGRESS_INFO);
            else
                callEvent(CallbackEvent.SEEKBAR_PAGE);

            invalidatePageBtn();
        }
    };

    private void invalidatePageBtn() {
        if (mTotalPage == 1) {
            mBtnPrevPage.setEnabled(false);
            mBtnPrevPageFirst.setEnabled(false);
            mBtnNextPage.setEnabled(false);
            mBtnNextPageEnd.setEnabled(false);
        } else {
            mBtnPrevPage.setEnabled((mCurrentPage != 0));
            mBtnPrevPageFirst.setEnabled((mCurrentPage != 0));
            mBtnNextPage.setEnabled((mCurrentPage != mTotalPage - 1));
            mBtnNextPageEnd.setEnabled((mCurrentPage != mTotalPage - 1));
        }
    }
}
