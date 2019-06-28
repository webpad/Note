package ntx.note;

import org.greenrobot.eventbus.EventBus;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import name.vbraun.view.write.Paper;
import ntx.note.ToolboxConfiguration.PageBackground;
import ntx.note2.R;

public class PageBackgroundPopupWindow extends RelativePopupWindow {
	private static PageBackgroundPopupWindow mInstance;
	private ToolboxConfiguration mToolboxConfiguration;

	private ImageButton mBtnPageBgBlank;
	private ImageButton mBtnPageBgNarrow;
	private ImageButton mBtnPageBgCollege;
	private ImageButton mBtnPageBgCornell;
	private ImageButton mBtnPageBgTodo;
	private ImageButton mBtnPageBgMeeting;
	private ImageButton mBtnPageBgDiary;
	private ImageButton mBtnPageBgQuadrangle;
	private ImageButton mBtnPageBgStave;
	private ImageButton mBtnPageBgCalligraphySmall;
	private ImageButton mBtnPageBgCalligraphyBig;
	private ImageButton mBtnPageBgCustomized;

	private EventBus mEventBus;

	public static PageBackgroundPopupWindow getInstance(Activity ctx) {
		synchronized (PageBackgroundPopupWindow.class) {
			if (mInstance == null) {
				mInstance = new PageBackgroundPopupWindow(ctx);
			}
			return mInstance;
		}
	}

	private PageBackgroundPopupWindow(Activity ctx) {
		super(ctx);

		mEventBus = EventBus.getDefault();

		setContentView(LayoutInflater.from(ctx).inflate(R.layout.popupwindow_page_background, null));
		setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
		setOutsideTouchable(false);
		setFocusable(false);
		setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		View popupView = getContentView();

		mToolboxConfiguration = ToolboxConfiguration.getInstance();

		initView(popupView);
	}
	
	@Override
	public void showOnAnchor(View anchor, int horizPos, int vertPos, int padL, int padT, int padR, int padB) {
		// Karote 20180921 : Renew selection icon before show up.
		setPageBackgroundValue(mToolboxConfiguration.getPageBackground());
		super.showOnAnchor(anchor, horizPos, vertPos, padL, padT, padR, padB);
	}

	private void initView(View v) {

		mBtnPageBgBlank = (ImageButton) v.findViewById(R.id.btn_page_bg_blank);
		mBtnPageBgNarrow = (ImageButton) v.findViewById(R.id.btn_page_bg_narrow);
		mBtnPageBgCollege = (ImageButton) v.findViewById(R.id.btn_page_bg_college);
		mBtnPageBgCornell = (ImageButton) v.findViewById(R.id.btn_page_bg_cornell);
		mBtnPageBgTodo = (ImageButton) v.findViewById(R.id.btn_page_bg_todo);
		mBtnPageBgMeeting = (ImageButton) v.findViewById(R.id.btn_page_bg_meeting);
		mBtnPageBgDiary = (ImageButton) v.findViewById(R.id.btn_page_bg_diary);
		mBtnPageBgQuadrangle = (ImageButton) v.findViewById(R.id.btn_page_bg_quadrangle);
		mBtnPageBgStave = (ImageButton) v.findViewById(R.id.btn_page_bg_stave);
		mBtnPageBgCalligraphySmall = (ImageButton) v.findViewById(R.id.btn_page_bg_calligraphy_small);
		mBtnPageBgCalligraphyBig = (ImageButton) v.findViewById(R.id.btn_page_bg_calligraphy_big);
		mBtnPageBgCustomized = (ImageButton) v.findViewById(R.id.btn_page_bg_customized);

		mBtnPageBgBlank.setOnClickListener(onBtnClickListener);
		mBtnPageBgNarrow.setOnClickListener(onBtnClickListener);
		mBtnPageBgCollege.setOnClickListener(onBtnClickListener);
		mBtnPageBgCornell.setOnClickListener(onBtnClickListener);
		mBtnPageBgTodo.setOnClickListener(onBtnClickListener);
		mBtnPageBgMeeting.setOnClickListener(onBtnClickListener);
		mBtnPageBgDiary.setOnClickListener(onBtnClickListener);
		mBtnPageBgQuadrangle.setOnClickListener(onBtnClickListener);
		mBtnPageBgStave.setOnClickListener(onBtnClickListener);
		mBtnPageBgCalligraphySmall.setOnClickListener(onBtnClickListener);
		mBtnPageBgCalligraphyBig.setOnClickListener(onBtnClickListener);
		mBtnPageBgCustomized.setOnClickListener(onBtnClickListener);

		setPageBackgroundValue(mToolboxConfiguration.getPageBackground());
	}

	private View.OnClickListener onBtnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			int pageBackground = PageBackground.BLANK;
			PageBackgroundChangeEvent event = new PageBackgroundChangeEvent();
			switch (v.getId()) {
			case R.id.btn_page_bg_blank:
				pageBackground = PageBackground.BLANK;
				event.setBackgroundType(Paper.EMPTY);
				break;
			case R.id.btn_page_bg_narrow:
				pageBackground = PageBackground.NARROW;
				event.setBackgroundType(Paper.NARROWRULED);
				break;
			case R.id.btn_page_bg_college:
				pageBackground = PageBackground.COLLEGE;
				event.setBackgroundType(Paper.COLLEGERULED);
				break;
			case R.id.btn_page_bg_cornell:
				pageBackground = PageBackground.CORNELL;
				event.setBackgroundType(Paper.CORNELLNOTES);
				break;
			case R.id.btn_page_bg_todo:
				pageBackground = PageBackground.TODO;
				event.setBackgroundType(Paper.TODOLIST);
				break;
			case R.id.btn_page_bg_meeting:
				pageBackground = PageBackground.MEETING;
				event.setBackgroundType(Paper.MINUTES);
				break;
			case R.id.btn_page_bg_diary:
				pageBackground = PageBackground.DIARY;
				event.setBackgroundType(Paper.DIARY);
				break;
			case R.id.btn_page_bg_quadrangle:
				pageBackground = PageBackground.QUADRANGLE;
				event.setBackgroundType(Paper.QUADPAPER);
				break;
			case R.id.btn_page_bg_stave:
				pageBackground = PageBackground.STAVE;
				event.setBackgroundType(Paper.STAVE);
				break;
			case R.id.btn_page_bg_calligraphy_small:
				pageBackground = PageBackground.CALLIGRAPHY_SMALL;
				event.setBackgroundType(Paper.CALLIGRAPHY_SMALL);
				break;
			case R.id.btn_page_bg_calligraphy_big:
				pageBackground = PageBackground.CALLIGRAPHY_BIG;
				event.setBackgroundType(Paper.CALLIGRAPHY_BIG);
				break;
			case R.id.btn_page_bg_customized:
				pageBackground = PageBackground.CUSTOMIZED;
				event.setBackgroundType(Paper.CUSTOMIZED);
				break;
			}
			mEventBus.post(event);
			setPageBackgroundValue(pageBackground);
			dismiss();
		}
	};

	private void setPageBackgroundValue(int pageBackground) {
		mBtnPageBgBlank.setSelected(false);
		mBtnPageBgNarrow.setSelected(false);
		mBtnPageBgCollege.setSelected(false);
		mBtnPageBgCornell.setSelected(false);
		mBtnPageBgTodo.setSelected(false);
		mBtnPageBgMeeting.setSelected(false);
		mBtnPageBgDiary.setSelected(false);
		mBtnPageBgQuadrangle.setSelected(false);
		mBtnPageBgStave.setSelected(false);
		mBtnPageBgCalligraphySmall.setSelected(false);
		mBtnPageBgCalligraphyBig.setSelected(false);
		mBtnPageBgCustomized.setSelected(false);

		switch (pageBackground) {
		case PageBackground.BLANK:
			mBtnPageBgBlank.setSelected(true);
			break;
		case PageBackground.NARROW:
			mBtnPageBgNarrow.setSelected(true);
			break;
		case PageBackground.COLLEGE:
			mBtnPageBgCollege.setSelected(true);
			break;
		case PageBackground.CORNELL:
			mBtnPageBgCornell.setSelected(true);
			break;
		case PageBackground.TODO:
			mBtnPageBgTodo.setSelected(true);
			break;
		case PageBackground.MEETING:
			mBtnPageBgMeeting.setSelected(true);
			break;
		case PageBackground.DIARY:
			mBtnPageBgDiary.setSelected(true);
			break;
		case PageBackground.QUADRANGLE:
			mBtnPageBgQuadrangle.setSelected(true);
			break;
		case PageBackground.STAVE:
			mBtnPageBgStave.setSelected(true);
			break;
		case PageBackground.CALLIGRAPHY_SMALL:
			mBtnPageBgCalligraphySmall.setSelected(true);
			break;
		case PageBackground.CALLIGRAPHY_BIG:
			mBtnPageBgCalligraphyBig.setSelected(true);
			break;
		case PageBackground.CUSTOMIZED:
			mBtnPageBgCustomized.setSelected(true);
			break;
		}
	}
}
