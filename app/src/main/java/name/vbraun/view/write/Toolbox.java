package name.vbraun.view.write;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import junit.framework.Assert;

import java.util.HashMap;

import name.vbraun.lib.pen.Hardware;
import name.vbraun.view.write.Graphics.Tool;
import name.vbraun.view.write.GraphicsControlPoint.ControlPoint;
import ntx.draw.nDrawHelper;
import ntx.note.Global;
import ntx.note.NoteWriterActivity;
import ntx.note.image.ImagePickerActivity;
import ntx.note2.R;

/**
 * The toolbox is a view with a collapsed and expanded view. The expanded view
 * is a grid of icons.
 *
 * @author vbraun
 *
 */
public class Toolbox extends RelativeLayout implements View.OnClickListener, ToolHistory.OnToolHistoryChangedListener {
	private static final String TAG = "Toolbox";

	public interface OnToolboxListener {
		public void onToolboxListener(View view);

		public void onToolboxColorListener(int color);

		public void onToolboxLineThicknessListener(int thickness);

		public void onToolboxPaperListener(String paper);
	}

	private OnToolboxListener listener;

	public void setOnToolboxListener(OnToolboxListener listener) {
		this.listener = listener;
	}

	private boolean toolboxIsVisible = true;
	private boolean isFirstRowToolboxVisible = true;
	private boolean debugOptions;
	public boolean isPaperSpinnerInitRun;

	private RelativeLayout layoutPageNumber;

	private TextView tvPageTitle;
	private TextView tvPageNumber;

	private ImageButton btnMore;
	private ImageButton btnFullRefresh;
	private ImageButton btnPaper;
	private ImageButton btnPenThickness;

	private ImageButton btnPencil;
	private ImageButton btnFountainPen;
	private ImageButton btnBrushPen;
	private ImageButton btnLine;
	private ImageButton btnPhoto;

	private ImageButton btnThicknessLv1;
	private ImageButton btnThicknessLv2;
	private ImageButton btnThicknessLv3;
	private ImageButton btnThicknessLv4;
	private ImageButton btnThicknessLv5;
	private ImageButton btnThicknessDown;
	private ImageButton btnThicknessUp;

	private ImageButton btnEraser;
	private ImageButton btnClean;
	private ImageButton btnCreatePage;
	private ImageButton btnDeletePage;
	private ImageButton btnSave;
	private ImageButton btnExport;
	private ImageButton btnBackup;
	private ImageButton btnImport;
	private ImageButton btnUndo;
	private ImageButton btnRedo;
	private ImageButton btnMove;
	private ImageButton btnOverview;
	private ImageButton btnRotate;
	private ImageButton btnInverseColor;
	private ImageButton btnTag;
	private ImageButton btnBooksThumbnail;
	private ImageButton btnPreferences;
	private ImageButton btnInfomation;

	private ImageButton btnNextPage;
	private ImageButton btnPrevPage;

	private ImageButton btnAddText;
	private ImageButton btnPenHistory1;
	private ImageButton btnPenHistory2;
	private ImageButton btnPenHistory3;
	private ImageButton btnPenHistory4;
	private ImageButton btnColorLime;
	private ImageButton btnLastPage;
	private ImageButton btnFirstPage;
	private ImageButton btnBookShelf;
	private ImageButton btnMenu;
	private ImageButton btnControlPointGears;
	private ImageButton btnControlPointTrash;

	private Context mContext;
	
	private final static int REQUEST_BG_PICK_IMAGE = 5;

	private static final HashMap<String, Integer> PAPER_ICON_HASH_MAP;
	static {
		PAPER_ICON_HASH_MAP = new HashMap<String, Integer>();
		PAPER_ICON_HASH_MAP.put(Paper.Type.EMPTY.name(), R.drawable.ic_menu_paper_blank);
		PAPER_ICON_HASH_MAP.put(Paper.Type.NARROWRULED.name(), R.drawable.ic_menu_paper_narrow_ruled);
		PAPER_ICON_HASH_MAP.put(Paper.Type.COLLEGERULED.name(), R.drawable.ic_menu_paper_college_ruled);
		PAPER_ICON_HASH_MAP.put(Paper.Type.CORNELLNOTES.name(), R.drawable.ic_menu_paper_cornell_notes);
		PAPER_ICON_HASH_MAP.put(Paper.Type.TODOLIST.name(), R.drawable.ic_menu_paper_todolist);
		PAPER_ICON_HASH_MAP.put(Paper.Type.MINUTES.name(), R.drawable.ic_menu_paper_minutes);
		PAPER_ICON_HASH_MAP.put(Paper.Type.DIARY.name(), R.drawable.ic_menu_paper_diary);
		PAPER_ICON_HASH_MAP.put(Paper.Type.QUAD.name(), R.drawable.ic_menu_paper_quadrangle);
		PAPER_ICON_HASH_MAP.put(Paper.Type.CALLIGRAPHY_SMALL.name(), R.drawable.ic_menu_paper_calligraphy_small);
		PAPER_ICON_HASH_MAP.put(Paper.Type.CALLIGRAPHY_BIG.name(), R.drawable.ic_menu_paper_calligraphy_big);
		PAPER_ICON_HASH_MAP.put(Paper.Type.STAVE.name(), R.drawable.ic_menu_paper_stave);
		PAPER_ICON_HASH_MAP.put(Paper.Type.CUSTOMIZED.name(), R.drawable.ic_menu_paper_customized);
	}

	protected Toolbox(Context context, boolean left) {
		super(context);

		mContext = context;

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		debugOptions = settings.getBoolean(HandwriterView.KEY_DEBUG_OPTIONS, false);

		Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

		// if (left)
		View.inflate(context, R.layout.toolbox, this);
		// else
		// View.inflate(context, R.layout.toolbox_right, this);

		btnMore = (ImageButton) findViewById(R.id.btn_toolbox_more);
		btnUndo = (ImageButton) findViewById(R.id.btn_toolbox_undo);
		btnRedo = (ImageButton) findViewById(R.id.btn_toolbox_redo);
		btnFountainPen = (ImageButton) findViewById(R.id.btn_toolbox_fountainpen);
		btnBrushPen = (ImageButton) findViewById(R.id.btn_toolbox_brush); // artis:
		btnPencil = (ImageButton) findViewById(R.id.btn_toolbox_pencil);
		btnPenThickness = (ImageButton) findViewById(R.id.btn_toolbox_pen_thickness);
		btnLine = (ImageButton) findViewById(R.id.btn_toolbox_line);
		btnMove = (ImageButton) findViewById(R.id.btn_toolbox_move);
		btnOverview = (ImageButton) findViewById(R.id.btn_toolbox_overview);
		btnRotate = (ImageButton) findViewById(R.id.btn_toolbox_rotate);
		btnInverseColor = (ImageButton) findViewById(R.id.btn_toolbox_inverse_color);
		btnEraser = (ImageButton) findViewById(R.id.btn_toolbox_eraser);
		btnClean = (ImageButton) findViewById(R.id.btn_toolbox_clean);
		btnAddText = (ImageButton) findViewById(R.id.btn_toolbox_text);
		btnPhoto = (ImageButton) findViewById(R.id.btn_toolbox_photo);
		btnNextPage = (ImageButton) findViewById(R.id.btn_toolbox_next_page);
		btnPrevPage = (ImageButton) findViewById(R.id.btn_toolbox_prev_page);
		btnLastPage = (ImageButton) findViewById(R.id.btn_toolbox_last);
		btnFirstPage = (ImageButton) findViewById(R.id.btn_toolbox_first);
		btnCreatePage = (ImageButton) findViewById(R.id.btn_toolbox_create_page);
		btnDeletePage = (ImageButton) findViewById(R.id.btn_toolbox_delete_page);
		btnTag = (ImageButton) findViewById(R.id.btn_toolbox_tag);
		btnBooksThumbnail = (ImageButton) findViewById(R.id.btn_toolbox_booksthumbnail);
		btnBookShelf = (ImageButton) findViewById(R.id.btn_toolbox_bookshelf);
		btnSave = (ImageButton) findViewById(R.id.btn_toolbox_save);
		btnExport = (ImageButton) findViewById(R.id.btn_toolbox_export);
		btnBackup = (ImageButton) findViewById(R.id.btn_toolbox_backup);
		btnImport = (ImageButton) findViewById(R.id.btn_toolbox_import);
		btnPreferences = (ImageButton) findViewById(R.id.btn_toolbox_preferences);
		btnInfomation = (ImageButton) findViewById(R.id.btn_toolbox_info);
		tvPageTitle = (TextView) findViewById(R.id.tv_toolbox_page_title);
		tvPageNumber = (TextView) findViewById(R.id.tv_toolbox_page_number);
		btnFullRefresh = (ImageButton) findViewById(R.id.btn_toolbox_full_refresh);

		btnPenHistory1 = (ImageButton) findViewById(R.id.btn_toolbox_history_1);
		btnPenHistory2 = (ImageButton) findViewById(R.id.btn_toolbox_history_2);
		btnPenHistory3 = (ImageButton) findViewById(R.id.btn_toolbox_history_3);
		btnPenHistory4 = (ImageButton) findViewById(R.id.btn_toolbox_history_4);

		btnColorLime = (ImageButton) findViewById(R.id.btn_toolbox_color_lime);

		btnMenu = (ImageButton) findViewById(R.id.btn_toolbox_menu);

		btnPaper = (ImageButton) findViewById(R.id.btn_toolbox_paper);
		btnThicknessUp = (ImageButton) findViewById(R.id.btn_toolbox_thickness_up);
		btnThicknessDown = (ImageButton) findViewById(R.id.btn_toolbox_thickness_down);

		layoutPageNumber = (RelativeLayout) findViewById(R.id.layout_toolbox_page_number);

		btnThicknessLv1 = findImageButton(R.id.toolbox_thickness_lv1);
		btnThicknessLv2 = findImageButton(R.id.toolbox_thickness_lv2);
		btnThicknessLv3 = findImageButton(R.id.toolbox_thickness_lv3);
		btnThicknessLv4 = findImageButton(R.id.toolbox_thickness_lv4);
		btnThicknessLv5 = findImageButton(R.id.toolbox_thickness_lv5);

		isPaperSpinnerInitRun = true;

		btnControlPointGears = (ImageButton) findViewById(R.id.btn_toolbox_controlpoint_gears);
		btnControlPointTrash = (ImageButton) findViewById(R.id.btn_toolbox_controlpoint_trash);

		if (!Hardware.hasPressureSensor()) {
			btnFountainPen.setVisibility(View.INVISIBLE);
			btnBrushPen.setVisibility(View.INVISIBLE); // artis: for brush to display the variable thickness with
			// pressure
		}

		btnMore.setOnClickListener(this);
		btnUndo.setOnClickListener(this);
		btnRedo.setOnClickListener(this);
		btnFountainPen.setOnClickListener(this);
		btnBrushPen.setOnClickListener(this); // artis
		btnPencil.setOnClickListener(this);
		btnPenThickness.setOnClickListener(this);
		btnLine.setOnClickListener(this);
		btnMove.setOnClickListener(this);
		btnOverview.setOnClickListener(this);
		btnRotate.setOnClickListener(this);
		btnInverseColor.setOnClickListener(this);
		btnEraser.setOnClickListener(this);
		btnClean.setOnClickListener(this);
		btnAddText.setOnClickListener(this);
		btnPhoto.setOnClickListener(this);
		btnNextPage.setOnClickListener(this);
		btnPrevPage.setOnClickListener(this);
		btnThicknessUp.setOnClickListener(this);
		btnThicknessDown.setOnClickListener(this);
		btnLastPage.setOnClickListener(this);
		btnFirstPage.setOnClickListener(this);
		btnCreatePage.setOnClickListener(this);
		btnDeletePage.setOnClickListener(this);
		btnSave.setOnClickListener(this);
		btnExport.setOnClickListener(this);
		btnBackup.setOnClickListener(this);
		btnImport.setOnClickListener(this);
		btnTag.setOnClickListener(this);
		btnBooksThumbnail.setOnClickListener(this);
		btnBookShelf.setOnClickListener(this);
		btnPreferences.setOnClickListener(this);
		btnInfomation.setOnClickListener(this);
		btnPaper.setOnClickListener(this);
		btnFullRefresh.setOnClickListener(this);

		findLinearLayout(R.id.toolbox_paper_blank).setOnClickListener(this);
		findLinearLayout(R.id.toolbox_paper_narrow_ruled).setOnClickListener(this);
		findLinearLayout(R.id.toolbox_paper_college_ruled).setOnClickListener(this);
		findLinearLayout(R.id.toolbox_paper_cornell_notes).setOnClickListener(this);
		findLinearLayout(R.id.toolbox_paper_to_do_list).setOnClickListener(this);
		findLinearLayout(R.id.toolbox_paper_meeting_minutes).setOnClickListener(this);
		findLinearLayout(R.id.toolbox_paper_diary).setOnClickListener(this);
		findLinearLayout(R.id.toolbox_paper_quadrangle).setOnClickListener(this);
		findLinearLayout(R.id.toolbox_paper_calligraphy_small).setOnClickListener(this);
		findLinearLayout(R.id.toolbox_paper_calligraphy_big).setOnClickListener(this);
		findLinearLayout(R.id.toolbox_paper_stave).setOnClickListener(this);
		findLinearLayout(R.id.toolbox_paper_customized_paper).setOnClickListener(this);

		btnPenHistory1.setOnClickListener(this);
		btnPenHistory2.setOnClickListener(this);
		btnPenHistory3.setOnClickListener(this);
		btnPenHistory4.setOnClickListener(this);

		btnColorLime.setOnClickListener(this);

		btnMenu.setOnClickListener(this);

		btnThicknessLv1.setOnClickListener(this);
		btnThicknessLv2.setOnClickListener(this);
		btnThicknessLv3.setOnClickListener(this);
		btnThicknessLv4.setOnClickListener(this);
		btnThicknessLv5.setOnClickListener(this);

		if (!debugOptions) {
			btnAddText.setVisibility(View.GONE);
		}

		ToolHistory.getToolHistory().setOnToolHistoryChangedListener(this);
		onToolHistoryChanged(false);

		initInstantKeys();
	}

	private ImageButton getToolIcon(@Tool int tool) {
		switch (tool) {
			case Tool.BRUSH:
				return btnBrushPen;
			case Tool.FOUNTAINPEN:
				return btnFountainPen;
			case Tool.PENCIL:
				return btnPencil;
			case Tool.MOVE:
				return btnMove;
			case Tool.ERASER:
				return btnEraser;
			case Tool.LINE:
				return btnLine;
			case Tool.TEXT:
				return btnAddText;
			case Tool.IMAGE:
				return btnPhoto;
			default:
				Assert.fail();
				return null;
		}
	}

	private void setIconActive(@Tool int tool, boolean active) {
		getToolIcon(tool).setSelected(active);

		if (tool == Tool.PENCIL && true == active) {
			findImageButton(R.id.btn_toolbox_pen_style).setImageResource(R.drawable.ic_menu_pencil_active);
		} else if (tool == Tool.PENCIL && false == active) {
			findImageButton(R.id.btn_toolbox_pen_style).setImageResource(R.drawable.ic_menu_pencil);
		}

		if (tool == Tool.FOUNTAINPEN && true == active) {
			findImageButton(R.id.btn_toolbox_pen_style).setImageResource(R.drawable.ic_menu_quill_active);
		} else if (tool == Tool.FOUNTAINPEN && false == active) {
			findImageButton(R.id.btn_toolbox_pen_style).setImageResource(R.drawable.ic_menu_quill);
		}

		if (tool == Tool.BRUSH && true == active) {
			findImageButton(R.id.btn_toolbox_pen_style).setImageResource(R.drawable.ic_menu_brush_active);
		} else if (tool == Tool.BRUSH && false == active) {
			findImageButton(R.id.btn_toolbox_pen_style).setImageResource(R.drawable.ic_menu_brush);
		}

		if (tool == Tool.LINE && true == active) {
			findImageButton(R.id.btn_toolbox_pen_style).setImageResource(R.drawable.ic_menu_line_active);
		} else if (tool == Tool.LINE && false == active) {
			findImageButton(R.id.btn_toolbox_pen_style).setImageResource(R.drawable.ic_menu_line);
		}

		if (tool == Tool.IMAGE && true == active) {
			findImageButton(R.id.btn_toolbox_pen_style).setImageResource(R.drawable.ic_menu_photo_active);
		} else if (tool == Tool.IMAGE && false == active) {
			findImageButton(R.id.btn_toolbox_pen_style).setImageResource(R.drawable.ic_menu_photo);
		}

	}

	public void setUndoIconEnabled(boolean active) {
		btnUndo.setEnabled(active);
	}

	public void setRedoIconEnabled(boolean active) {
		btnRedo.setEnabled(active);
	}

	public void setPageTitleText(String mString) {
		tvPageTitle.setText(mString);
	}

	public void setPageTitleTextColor(int color) {
		tvPageTitle.setTextColor(color);
	}

	public void setPageNumberText(String mString) {
		tvPageNumber.setText(mString);
		String[] PageNumber;
		PageNumber = mString.split("/"); // current page number and total page number

		findSeekBar(R.id.seekbar_page).setMax(Integer.parseInt(PageNumber[1]));
		findSeekBar(R.id.seekbar_page).setProgress(Integer.parseInt(PageNumber[0]));
	}

	private @Tool int previousTool;

	public void setActiveTool(@Tool int tool) {
		setIconActive(previousTool, false);
		setIconActive(tool, true);

		if (tool == Tool.IMAGE) {// || tool == tool.ERASER) {
			showThckness(false);
		} else {
			showThckness(true);
		}

		previousTool = tool;
	}

	public void setToolboxVisible(boolean visible) {
		toolboxIsVisible = visible;
		int vis = visible ? View.VISIBLE : View.INVISIBLE;

		if (Hardware.hasPressureSensor()) {
			// fountainpenButton.setVisibility(vis);
			// brushpenButton.setVisibility(vis);
		}
		showSubPanel(R.id.scroll_toolbox_drawer, visible);

		if (!isFirstRowToolboxVisible) {
			btnPenThickness.setVisibility(vis);
			tvPageTitle.setVisibility(vis);
			tvPageNumber.setVisibility(vis);
			btnNextPage.setVisibility(vis);
			btnPrevPage.setVisibility(vis);
			layoutPageNumber.setVisibility(vis);
			btnFullRefresh.setVisibility(vis);

			btnPaper.setVisibility(vis);

			findImageButton(R.id.btn_toolbox_pen_style).setVisibility(vis);

			findLinearLayout(R.id.layout_seekbar_page).setVisibility(vis);
		}

		if (debugOptions) {
			btnAddText.setVisibility(vis);
		}
	}

	public void setFirstRowToolboxVisible(boolean visible) {
		isFirstRowToolboxVisible = visible;

		btnPenThickness.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
		tvPageTitle.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
		tvPageNumber.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
		btnFullRefresh.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);

		btnPaper.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);

		findImageButton(R.id.btn_toolbox_pen_style).setVisibility(visible ? View.VISIBLE : View.INVISIBLE);

		findLinearLayout(R.id.layout_seekbar_page).setVisibility(visible ? View.VISIBLE : View.INVISIBLE);

		btnNextPage.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
		btnPrevPage.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
		layoutPageNumber.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
	}

	public boolean getFirstRowToolboxVisible() {
		return isFirstRowToolboxVisible;
	}

	public boolean isToolboxVisible() {
		return toolboxIsVisible;
	}

	@Override
	public void onClick(View v) {

		nDrawHelper.NDrawSwitch(false);

		switch (v.getId()) {
			case R.id.btn_toolbox_more:
				if(findView(R.id.scroll_toolbox_drawer).isShown()) {
					setToolboxVisible(false);
				} else {
					setToolboxVisible(true);
				}
				if (listener != null)
					listener.onToolboxListener(v);
				break;
			case R.id.btn_toolbox_color_lime:
				if (listener != null)
					listener.onToolboxColorListener(Color.argb(0xff, 0x00, 0xff, 0x00));
				break;
			case R.id.btn_toolbox_paper:
				// Daniel 20180628 : fix - Stroke are missing when open the android spinner
				if (listener != null)
					listener.onToolboxListener(v);

				if (findView(R.id.scroll_toolbox_paper).isShown()) {
					showSubPanel(R.id.scroll_toolbox_paper, false);
				} else {
					showSubPanel(R.id.scroll_toolbox_paper, true);
				}

				isPaperSpinnerInitRun = false;
				break;
			case R.id.btn_toolbox_info:
			case R.id.layout_toolbox_info:
				// trigger info dialog
				showInfoDialog();
				break;
			case R.id.btn_toolbox_fountainpen:
			case R.id.btn_toolbox_brush:
			case R.id.btn_toolbox_pencil:
				if (listener != null)
					listener.onToolboxListener(v);

				showThckness(true);
				findButton(R.id.btn_style_close).performClick();
				break;
			case R.id.btn_toolbox_line:
				if (listener != null)
					listener.onToolboxListener(v);

				showThckness(true);
				break;
			case R.id.btn_toolbox_pen_style:
				if (findLinearLayout(R.id.layout_toolbox_pen_style).isShown()) {
					showSubPanel(R.id.layout_toolbox_pen_style, false);
				} else {
					showSubPanel(R.id.layout_toolbox_pen_style, true);
				}
				if (listener != null)
					listener.onToolboxListener(v);
				break;
			case R.id.btn_toolbox_pen_thickness:
				if (findLinearLayout(R.id.layout_toolbox_pen_thickness).isShown()) {
					showSubPanel(R.id.layout_toolbox_pen_thickness, false);
				} else {
					showSubPanel(R.id.layout_toolbox_pen_thickness, true);
				}

				findSeekBar(R.id.seekBar_thickness).setProgress(nowThickness - minThickness);
				findTextView(R.id.tv_thickness).setText(String.valueOf(nowThickness));
				findView(R.id.view_thickness).setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, nowThickness));
				if (listener != null)
					listener.onToolboxListener(v);
				break;
			case R.id.btn_style_close:
				showSubPanel(R.id.layout_toolbox_pen_style, false);
				break;
			case R.id.btn_thickness_close:
				showSubPanel(R.id.layout_toolbox_pen_thickness, false);
				break;
			case R.id.btn_toolbox_prev_page:
				int progress = findSeekBar(R.id.seekbar_page).getProgress();
				progress = progress - 1;
				if (progress <= 0)
					progress = 1;
				findSeekBar(R.id.seekbar_page).setProgress(progress);
				if (listener != null)
					listener.onToolboxListener(v);
				break;
			case R.id.btn_toolbox_next_page:
				progress = findSeekBar(R.id.seekbar_page).getProgress();
				progress = progress + 1;
				if (progress >= findSeekBar(R.id.seekbar_page).getMax())
					progress = findSeekBar(R.id.seekbar_page).getMax();
				findSeekBar(R.id.seekbar_page).setProgress(progress);
				if (listener != null)
					listener.onToolboxListener(v);
				break;
			case R.id.btn_toolbox_thickness_down:
				progress = findSeekBar(R.id.seekBar_thickness).getProgress();
				progress = progress - 1;
				if (progress <= 0)
					progress = 0;
				findSeekBar(R.id.seekBar_thickness).setProgress(progress);
				break;
			case R.id.btn_toolbox_thickness_up:
				progress = findSeekBar(R.id.seekBar_thickness).getProgress();
				progress = progress + 1;
				if (progress >= (maxThickness - minThickness))
					progress = maxThickness - minThickness;
				findSeekBar(R.id.seekBar_thickness).setProgress(progress);
				break;
			case R.id.toolbox_thickness_lv1:
				progress = Global.THICKNESS_VALUE_LV1;
				findSeekBar(R.id.seekBar_thickness).setProgress(progress - minThickness);
				setThicknessBtnSelected(progress);
				findButton(R.id.btn_thickness_close).performClick();
				break;
			case R.id.toolbox_thickness_lv2:
				progress = Global.THICKNESS_VALUE_LV2;
				findSeekBar(R.id.seekBar_thickness).setProgress(progress - minThickness);
				setThicknessBtnSelected(progress);
				findButton(R.id.btn_thickness_close).performClick();
				break;
			case R.id.toolbox_thickness_lv3:
				progress = Global.THICKNESS_VALUE_LV3;
				findSeekBar(R.id.seekBar_thickness).setProgress(progress - minThickness);
				setThicknessBtnSelected(progress);
				findButton(R.id.btn_thickness_close).performClick();
				break;
			case R.id.toolbox_thickness_lv4:
				progress = Global.THICKNESS_VALUE_LV4;
				findSeekBar(R.id.seekBar_thickness).setProgress(progress - minThickness);
				setThicknessBtnSelected(progress);
				findButton(R.id.btn_thickness_close).performClick();
				break;
			case R.id.toolbox_thickness_lv5:
				progress = Global.THICKNESS_VALUE_LV5;
				findSeekBar(R.id.seekBar_thickness).setProgress(progress - minThickness);
				setThicknessBtnSelected(progress);
				findButton(R.id.btn_thickness_close).performClick();
				break;

			case R.id.toolbox_paper_blank:
				if (listener != null)
					listener.onToolboxPaperListener(Paper.Type.EMPTY.name());
				break;
			case R.id.toolbox_paper_narrow_ruled:
				if (listener != null)
					listener.onToolboxPaperListener(Paper.Type.NARROWRULED.name());
				break;
			case R.id.toolbox_paper_college_ruled:
				if (listener != null)
					listener.onToolboxPaperListener(Paper.Type.COLLEGERULED.name());
				break;
			case R.id.toolbox_paper_cornell_notes:
				if (listener != null)
					listener.onToolboxPaperListener(Paper.Type.CORNELLNOTES.name());
				break;
			case R.id.toolbox_paper_to_do_list:
				if (listener != null)
					listener.onToolboxPaperListener(Paper.Type.TODOLIST.name());
				break;
			case R.id.toolbox_paper_meeting_minutes:
				if (listener != null)
					listener.onToolboxPaperListener(Paper.Type.MINUTES.name());
				break;
			case R.id.toolbox_paper_diary:
				if (listener != null)
					listener.onToolboxPaperListener(Paper.Type.DIARY.name());
				break;
			case R.id.toolbox_paper_quadrangle:
				if (listener != null)
					listener.onToolboxPaperListener(Paper.Type.QUAD.name());
				break;
			case R.id.toolbox_paper_calligraphy_small:
				if (listener != null)
					listener.onToolboxPaperListener(Paper.Type.CALLIGRAPHY_SMALL.name());
				break;
			case R.id.toolbox_paper_calligraphy_big:
				if (listener != null)
					listener.onToolboxPaperListener(Paper.Type.CALLIGRAPHY_BIG.name());
				break;
			case R.id.toolbox_paper_stave:
				if (listener != null)
					listener.onToolboxPaperListener(Paper.Type.STAVE.name());
				break;
			case R.id.toolbox_paper_customized_paper:
				Global.openWaitDialog(mContext);
				Intent intent = new Intent(((Activity) mContext), ImagePickerActivity.class);
				((Activity) mContext).startActivityForResult(intent, REQUEST_BG_PICK_IMAGE);
				break;

			case R.id.btn_toolbox_backup:
			case R.id.layout_toolbox_backup:
			case R.id.btn_toolbox_export:
			case R.id.layout_toolbox_export:
				isPaperSpinnerInitRun = true;
				// no break;
			default:
				if (listener != null)
					listener.onToolboxListener(v);
				break;
		}
	}

	private void setThicknessBtnSelected(int value) {
		btnThicknessLv1.setSelected(false);
		btnThicknessLv2.setSelected(false);
		btnThicknessLv3.setSelected(false);
		btnThicknessLv4.setSelected(false);
		btnThicknessLv5.setSelected(false);
		btnPenThickness.setImageLevel(value);
		switch (value) {
			case Global.THICKNESS_VALUE_LV1:
				btnThicknessLv1.setSelected(true);
				break;
			case Global.THICKNESS_VALUE_LV2:
				btnThicknessLv2.setSelected(true);
				break;
			case Global.THICKNESS_VALUE_LV3:
				btnThicknessLv3.setSelected(true);
				break;
			case Global.THICKNESS_VALUE_LV4:
				btnThicknessLv4.setSelected(true);
				break;
			case Global.THICKNESS_VALUE_LV5:
				btnThicknessLv5.setSelected(true);
				break;
			default:
				break;
		}
	}

	////////////////////////////////////
	private String[] paperChoices = getContext().getResources().getStringArray(R.array.dlg_paper_values);

	Dialog infoDialog;

	private void showInfoDialog() {
		infoDialog = new Dialog(mContext);
		infoDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		infoDialog.setContentView(R.layout.information_dialog);

		Button okButton = (Button) infoDialog.findViewById(R.id.information_ok);
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				infoDialog.dismiss();
			}
		});

		infoDialog.show();
	}

	int minThickness = 1, maxThickness = 15, nowThickness = 0;

	public void setThickness(int thickness) {
		nowThickness = thickness;
	}

	public void setPaperTypeValue(Paper.Type pagePaperType) {
		btnPaper.setImageResource(PAPER_ICON_HASH_MAP.get(pagePaperType.name()));
	}

	@Override
	public void onToolHistoryChanged(boolean onlyCurrent) {
		ToolHistory h = ToolHistory.getToolHistory();
		// Log.d(TAG, "onToolHistoryChanged "+h.size());
		btnPenHistory1.setImageDrawable(h.getIcon());
		if (onlyCurrent)
			return;
		if (h.size() > 0)
			btnPenHistory2.setImageDrawable(h.getIcon(0));
		if (h.size() > 1)
			btnPenHistory3.setImageDrawable(h.getIcon(1));
		if (h.size() > 2)
			btnPenHistory4.setImageDrawable(h.getIcon(2));
	}

	private boolean inControlpointMoveMode = false;
	private boolean toolboxVisibleBeforeMove;
	private Rect rectGears = new Rect();
	private Rect rectTrash = new Rect();

	public void startControlpointMove(boolean showGearsButton, boolean showTrashButton) {
		inControlpointMoveMode = true;
		toolboxVisibleBeforeMove = toolboxIsVisible;
		setToolboxVisible(false);
		if (showGearsButton)
			btnControlPointGears.setVisibility(VISIBLE);
		if (showTrashButton)
			btnControlPointTrash.setVisibility(VISIBLE);
		btnControlPointGears.setPressed(false);
		btnControlPointTrash.setPressed(false);
	}

	/**
	 * Stop the {@link ControlPoint} move mode.
	 *
	 * It is safe to call this method even if you are not in the move mode.
	 */
	public void stopControlpointMove() {
		NoteWriterActivity.setNoteEdited(true);
		if (inControlpointMoveMode)
			setToolboxVisible(toolboxVisibleBeforeMove);
		btnControlPointGears.setVisibility(GONE);
		btnControlPointTrash.setVisibility(GONE);
		inControlpointMoveMode = false;
	}

	/**
	 * While moving the control point, the event must be passed to this method.
	 *
	 * @param event
	 * @return true if the point hovers over the "gears" button that is visible only
	 *         while moving the control point.
	 */
	public boolean onControlpointMotion(MotionEvent event) {
		Assert.assertTrue(inControlpointMoveMode);
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_MOVE) {
			boolean pressGears = false;
			boolean pressTrash = false;
			float x = 0, y = 0;
			btnControlPointGears.getHitRect(rectGears);
			btnControlPointTrash.getHitRect(rectTrash);
			for (int idx = 0; idx < event.getPointerCount(); idx++) {
				x = event.getX(idx);
				y = event.getY(idx);
				pressGears = pressGears || rectGears.contains((int) x, (int) y);
				pressTrash = pressTrash || rectTrash.contains((int) x, (int) y);
			}
			btnControlPointGears.setPressed(pressGears);
			btnControlPointTrash.setPressed(pressTrash);
			return pressGears;
		}
		return false;
	}

	public boolean isGearsSelectedControlpointMove() {
		return btnControlPointGears.getVisibility() == VISIBLE && btnControlPointGears.isPressed();
	}

	public boolean isTrashSelectedControlpointMove() {
		return btnControlPointTrash.getVisibility() == VISIBLE && btnControlPointTrash.isPressed();
	}

	public void initInstantKeys() {
		findLinearLayout(R.id.layout_toolbox_delete_page).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_save).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_export).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_backup).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_import).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_undo).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_redo).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_move).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_overview).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_rotate).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_inverse_color).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_tag).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_booksthumbnail).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_preferences).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_info).setOnClickListener(this);

		findLinearLayout(R.id.layout_toolbox_pen_style).setVisibility(View.INVISIBLE);
		findLinearLayout(R.id.layout_toolbox_pen_thickness).setVisibility(View.INVISIBLE);

		findLinearLayout(R.id.layout_toolbox_eraser).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_clean).setOnClickListener(this);
		findLinearLayout(R.id.layout_toolbox_create_page).setOnClickListener(this);

		findImageButton(R.id.btn_toolbox_pen_style).setOnClickListener(this);
		findButton(R.id.btn_style_close).setOnClickListener(this);
		findButton(R.id.btn_thickness_close).setOnClickListener(this);

		findSeekBar(R.id.seekBar_thickness).setMax(maxThickness - minThickness);
		findSeekBar(R.id.seekBar_thickness).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				nowThickness = progress + minThickness;
				findTextView(R.id.tv_thickness).setText(String.valueOf(nowThickness));
				setThicknessBtnSelected(nowThickness);
				findView(R.id.view_thickness).setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, nowThickness));
				if (listener != null)
					listener.onToolboxLineThicknessListener(nowThickness);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				setThicknessBtnSelected(nowThickness);
				if (listener != null)
					listener.onToolboxLineThicknessListener(nowThickness);
			}
		});

		findSeekBar(R.id.seekbar_page).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				// ALog.debug("progress = "+progress+"fromUser = "+fromUser);
				tvPageNumber.setText(progress + "/" + seekBar.getMax());
				if (progress == 0) {
					findSeekBar(R.id.seekbar_page).setProgress(1);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// Log.d(TAG,"onStartTrackingTouch ");
				// ALog.debug("onStart TrackingTouch_"+seekBar.getProgress());
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// Log.d(TAG,"onStopTrackingTouch ");
				// ALog.debug("onStop TrackingTouch_"+seekBar.getProgress());
				if (seekBar.getProgress() >= seekBar.getMax()) {
					// 2019.03.04 Karote deprecated
					// NoteWriterActivity.updatePage(seekBar.getMax());
				} else {
					// 2019.03.04 Karote deprecated
					// NoteWriterActivity.updatePage(seekBar.getProgress());
				}
			}
		});

	}

	public void showThckness(boolean b) {

		if (b) {
			findSeekBar(R.id.seekBar_thickness).setProgress(nowThickness - minThickness);
			findTextView(R.id.tv_thickness).setText(String.valueOf(nowThickness));
			findView(R.id.view_thickness).setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, nowThickness));
		} else {
			findViewById(R.id.layout_toolbox_pen_thickness).setVisibility(View.INVISIBLE);
		}

	}

	public LinearLayout findLinearLayout(int id) {
		return (LinearLayout) findViewById(id);
	}

	public ImageButton findImageButton(int id) {
		return (ImageButton) findViewById(id);
	}

	private Button findButton(int id) {
		return (Button) findViewById(id);
	}

	private SeekBar findSeekBar(int id) {
		return (SeekBar) findViewById(id);
	}

	private TextView findTextView(int id) {
		return (TextView) findViewById(id);
	}

	private View findView(int id) {
		return (View) findViewById(id);
	}

	private void showSubPanel(int id, boolean isVisible) {
		int vis = isVisible ? View.VISIBLE : View.INVISIBLE;
		findView(R.id.layout_toolbox_pen_style).setVisibility(INVISIBLE);
		findView(R.id.layout_toolbox_pen_thickness).setVisibility(INVISIBLE);
		findView(R.id.scroll_toolbox_drawer).setVisibility(INVISIBLE);
		findView(R.id.scroll_toolbox_paper).setVisibility(INVISIBLE);

		findView(id).setVisibility(vis);

		toolboxIsVisible = isVisible;
	}

}
