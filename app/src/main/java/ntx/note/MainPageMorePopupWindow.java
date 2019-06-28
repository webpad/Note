package ntx.note;

import org.greenrobot.eventbus.EventBus;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import ntx.note2.R;

public class MainPageMorePopupWindow extends RelativePopupWindow {
	private static MainPageMorePopupWindow mInstance;

	private ImageButton mIBtnSort;
	private ImageButton mIBtnPreview;
	private ImageButton mIBtnImport;
	private ImageButton mIBtnManage;

	private EventBus mEventBus;
	
	public static MainPageMorePopupWindow getInstance(Activity ctx) {
		synchronized (MainPageMorePopupWindow.class) {
			if (mInstance == null) {
				mInstance = new MainPageMorePopupWindow(ctx);
			}
			return mInstance;
		}
	}

	private MainPageMorePopupWindow(Activity ctx) {
		super(ctx);
		setContentView(LayoutInflater.from(ctx).inflate(R.layout.popupwindow_main_page_more, null));
		setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
		setOutsideTouchable(true);
		setFocusable(true);
		setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		View popupView = getContentView();

		initView(popupView);
		
		mEventBus = EventBus.getDefault();
	}

	private void initView(View v) {
		mIBtnSort = (ImageButton) v.findViewById(R.id.ibtn_more_sort);
		mIBtnPreview = (ImageButton) v.findViewById(R.id.ibtn_more_preview);
		mIBtnImport = (ImageButton) v.findViewById(R.id.ibtn_more_import);
		mIBtnManage = (ImageButton) v.findViewById(R.id.ibtn_more_manage);

		mIBtnSort.setOnClickListener(onBtnClickListener);
		mIBtnPreview.setOnClickListener(onBtnClickListener);
		mIBtnImport.setOnClickListener(onBtnClickListener);
		mIBtnManage.setOnClickListener(onBtnClickListener);

	}

	private View.OnClickListener onBtnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.ibtn_more_sort:
				callEvent(CallbackEvent.MORE_SORT);
				break;
			case R.id.ibtn_more_preview:
				callEvent(CallbackEvent.MORE_PREVIEW);
				break;
			case R.id.ibtn_more_import:
				callEvent(CallbackEvent.MORE_IMPORT);
				break;
			case R.id.ibtn_more_manage:
				callEvent(CallbackEvent.MORE_MANAGE);
				break;
			}
			dismiss();
		}
	};
	
	private void callEvent(String event){
		CallbackEvent callbackEvent = new CallbackEvent();
		callbackEvent.setMessage(event);
		mEventBus.post(callbackEvent);
	}
}
