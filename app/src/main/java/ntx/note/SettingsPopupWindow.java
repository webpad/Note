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

public class SettingsPopupWindow extends RelativePopupWindow {
	private static SettingsPopupWindow mInstance;

	private ImageButton mBtnSettingCalibration;
	private ImageButton mBtnSettingSwitch;
	
	private EventBus mEventBus;

	public static SettingsPopupWindow getInstance(Activity ctx) {
		synchronized (SettingsPopupWindow.class) {
			if (mInstance == null) {
				mInstance = new SettingsPopupWindow(ctx);
			}
			return mInstance;
		}
	}

	private SettingsPopupWindow(Activity ctx) {
		super(ctx);
		
		mEventBus = EventBus.getDefault();
		
		setContentView(LayoutInflater.from(ctx).inflate(R.layout.popupwindow_settings, null));
		setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
		setOutsideTouchable(false);
		setFocusable(false);
		setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		View popupView = getContentView();

		initView(popupView);
	}

	private void initView(View v) {
		mBtnSettingCalibration = (ImageButton) v.findViewById(R.id.btn_setting_calibration);
		mBtnSettingSwitch = (ImageButton) v.findViewById(R.id.btn_setting_switch);

		mBtnSettingCalibration.setOnClickListener(onBtnClickListener);
		mBtnSettingSwitch.setOnClickListener(onBtnClickListener);
	}

	private View.OnClickListener onBtnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			CallbackEvent event = new CallbackEvent();
			switch (v.getId()) {
			case R.id.btn_setting_calibration:
				event.setMessage(CallbackEvent.SHOW_SETTING_CALIBRATION_DIALOG);
				break;
			case R.id.btn_setting_switch:
				event.setMessage(CallbackEvent.SWITCH_VERTICAL_TOOLBAR);
				break;
			}
			mEventBus.post(event);
			dismiss();
		}
	};
}
