package ntx.note.image;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import name.vbraun.lib.pen.Hardware;
import ntx.note.Global;
import ntx.note.asynctask.BitmapWorkerTask;
import ntx.note2.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("DefaultLocale")
public class ImagePickerActivity extends Activity implements TextWatcher {
	private final static String TAG = "ImagePickerActivity";
	private final static int NumPerPage_Portrait = 12;
	protected File sdcardDir, externalCardDir = null;
	private int totalPage, currentPage;
	public BitmapWorkerTask[] asyncTask = new BitmapWorkerTask[NumPerPage_Portrait];
	private ImageButton btn_exit, btn_search_input, btn_close_input;
	private LinearLayout input_layout, input_search_layout;
	private EditText search_editText;
	private ArrayList<ImageView> picker_image_list = new ArrayList<>();
	private ArrayList<TextView> picker_text_list = new ArrayList<>();
	private ArrayList<LinearLayout> image_layout_list = new ArrayList<>();
	private List<String> resultPath = new ArrayList<>();
	private List<String> searchResultPath = new ArrayList<>();
	private LinearLayout image_layout1, image_layout2, image_layout3, image_layout4, image_layout5, image_layout6, image_layout7, image_layout8, image_layout9, image_layout10, image_layout11, image_layout12;
	private TextView txtv_pageinfo, picker_text1, picker_text2, picker_text3, picker_text4, picker_text5, picker_text6, picker_text7, picker_text8, picker_text9, picker_text10, picker_text11, picker_text12;
	private ImageView imgv_pre_page, imgv_next_page, picker_image1, picker_image2, picker_image3, picker_image4, picker_image5, picker_image6, picker_image7, picker_image8, picker_image9, picker_image10, picker_image11, picker_image12;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_picker);
		initUI();
		autoSearch(true);
	}

	private OnClickListener instantKeyListener = new OnClickListener() {
		@Override
		public void onClick(final View v) {
			switch (v.getId()) {
				case R.id.imgb_pre_page:
					setPage(false);
					break;
				case R.id.imgb_next_page:
					setPage(true);
					break;
				case R.id.btn_back:
					pickFile(null);
					break;
				case R.id.btn_close_input:
					closeSearch();
					break;
				case R.id.btn_search_input:
					clickSearch();
					break;
				default:
					finish();
					break;
			}
		}
	};

	private void pickFile(@Nullable File file) {
		hideKeyboard(this);
		Intent intent = getIntent();
		if (intent.getIntExtra(ImageActivity.BLANK, 99) == 1 && file == null) {
			finish();
		} else {
			if (file != null) {
				intent.setData(Uri.fromFile(file));
			}
			setResult(RESULT_OK, intent);
			finish();
		}
	}

	@Override
	public void onBackPressed() {
		pickFile(null);
	}

	public void searchFiles(File file) {
		File[] the_Files = file.listFiles();

		if (the_Files == null)
			return;

		for (File tempF : the_Files) {
			if (tempF.isDirectory()) {
				if (!tempF.isHidden())
					searchFiles(tempF);
			} else {
				try {
					boolean isImage = false;
					//compare file. if the key is matched, return value > -1.
					for (String type : Global.searchImageType) {
						final int index = tempF.getPath().lastIndexOf('.');
						final String myExtension = ((index > 0) ? tempF.getPath().substring(index).toLowerCase().intern() : "");
						Log.d(TAG, "myExtension : " + myExtension);
						if (myExtension.equals(type) && tempF.getName().substring(0, 1).equals(".") == false) {
							isImage = true;
							break;
						}
					}
					if (isImage) {
						resultPath.add(tempF.getPath());
					}
				} catch (Exception e) {
					Toast.makeText(ImagePickerActivity.this, "error - searching image files.", Toast.LENGTH_LONG).show();
				}
			}
		}

	}

	@Override
	public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
		checkEditText(charSequence + "");
	}

	@Override
	public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
		checkEditText(charSequence + "");
	}

	@Override
	public void afterTextChanged(Editable editable) {
	}

	private void clickSearch() {

		searchResultPath.clear();

		if (input_layout.getVisibility() == View.VISIBLE && !search_editText.getText().toString().matches("")) {

			hideKeyboard(this);

			for (String path : resultPath) {
				String fullPath = path;
				int index = fullPath.lastIndexOf("/");
				String fileName = fullPath.substring(index + 1);
				if (fileName.toLowerCase().contains(search_editText.getText().toString().toLowerCase())) {
					searchResultPath.add(fullPath);
				}
			}

			if (searchResultPath.size() > 0) {
				totalPage = searchResultPath.size() >= NumPerPage_Portrait ? (searchResultPath.size() / NumPerPage_Portrait) + (searchResultPath.size() % NumPerPage_Portrait > 0 ? 1 : 0) : 1;
				currentPage = 1;
			} else {
				totalPage = 0;
				currentPage = 0;
			}

			setPageInfo();

			closeAllImage();

			int setSize = searchResultPath.size() > NumPerPage_Portrait ? NumPerPage_Portrait : searchResultPath.size();

			for (int i = 0; i < setSize; i++) {
				setPageImage(i, picker_image_list.get(i), picker_text_list.get(i), image_layout_list.get(i), searchResultPath.get(i));
			}

		} else {
			input_search_layout.setBackgroundDrawable(getResources().getDrawable(R.drawable.black_rectangle));

			setSearchEnable(false);

			input_layout.setVisibility(View.VISIBLE);

			search_editText.requestFocus();

			showKeyboard();
		}
	}

	private void closeSearch() {
		hideKeyboard(this);
		autoSearch(false);
		input_search_layout.setBackgroundColor(getResources().getColor(R.color.alpha_00));
		search_editText.setText("");
		setSearchEnable(true);
		input_layout.setVisibility(View.GONE);
	}

	private void checkEditText(String input) {
		if(input.matches("")){
			setSearchEnable(false);
		}else{
			setSearchEnable(true);
		}
	}

	private void setSearchEnable(boolean enable) {
		if (enable) {
			btn_search_input.setEnabled(true);
			btn_search_input.setAlpha(1f);
		} else {
			btn_search_input.setEnabled(false);
			btn_search_input.setAlpha(0.2f);
		}
	}

	public static void hideKeyboard(Activity activity) {
		InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		//Find the currently focused view, so we can grab the correct window token from it.
		View view = activity.getCurrentFocus();
		//If no view currently has focus, create a new one, just so we can grab a window token from it
		if (view == null) {
			view = new View(activity);
		}
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	private void showKeyboard() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
		}
	}

	private void setPageImage(int position, ImageView imageView, TextView textView, LinearLayout image_layout, final String path) {

		int mWidth = 205 * 2;
		int mHeight = 150 * 2;

		textView.setText(getFileName(path));

		if (asyncTask[position] != null && asyncTask[position].getStatus().equals(Status.RUNNING)) {
			asyncTask[position].cancel(true);
		}

		asyncTask[position] = new BitmapWorkerTask(imageView, mWidth, mHeight);
		asyncTask[position].execute(path);

		image_layout_list.get(position).setVisibility(View.VISIBLE);

		image_layout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				final File f = new File(path);
				if (f.isFile())
					pickFile(f);
			}
		});
	}

	private void initUI() {

		setTitle(R.string.image_pick_title);

		btn_exit = findViewById(R.id.btn_back);
		txtv_pageinfo = findViewById(R.id.page_info);
		imgv_pre_page = findViewById(R.id.imgb_pre_page);
		imgv_next_page = findViewById(R.id.imgb_next_page);
		btn_close_input = findViewById(R.id.btn_close_input);
		btn_search_input = findViewById(R.id.btn_search_input);
		search_editText = findViewById(R.id.search_editText);
		input_layout = findViewById(R.id.input_btn_layout);
		input_search_layout = findViewById(R.id.input_search_layout);

		picker_text1 = findViewById(R.id.picker_text1);
		picker_text2 = findViewById(R.id.picker_text2);
		picker_text3 = findViewById(R.id.picker_text3);
		picker_text4 = findViewById(R.id.picker_text4);
		picker_text5 = findViewById(R.id.picker_text5);
		picker_text6 = findViewById(R.id.picker_text6);
		picker_text7 = findViewById(R.id.picker_text7);
		picker_text8 = findViewById(R.id.picker_text8);
		picker_text9 = findViewById(R.id.picker_text9);
		picker_text10 = findViewById(R.id.picker_text10);
		picker_text11 = findViewById(R.id.picker_text11);
		picker_text12 = findViewById(R.id.picker_text12);

		picker_image1 = findViewById(R.id.picker_image1);
		picker_image2 = findViewById(R.id.picker_image2);
		picker_image3 = findViewById(R.id.picker_image3);
		picker_image4 = findViewById(R.id.picker_image4);
		picker_image5 = findViewById(R.id.picker_image5);
		picker_image6 = findViewById(R.id.picker_image6);
		picker_image7 = findViewById(R.id.picker_image7);
		picker_image8 = findViewById(R.id.picker_image8);
		picker_image9 = findViewById(R.id.picker_image9);
		picker_image10 = findViewById(R.id.picker_image10);
		picker_image11 = findViewById(R.id.picker_image11);
		picker_image12 = findViewById(R.id.picker_image12);

		image_layout1 = findViewById(R.id.image_layout1);
		image_layout2 = findViewById(R.id.image_layout2);
		image_layout3 = findViewById(R.id.image_layout3);
		image_layout4 = findViewById(R.id.image_layout4);
		image_layout5 = findViewById(R.id.image_layout5);
		image_layout6 = findViewById(R.id.image_layout6);
		image_layout7 = findViewById(R.id.image_layout7);
		image_layout8 = findViewById(R.id.image_layout8);
		image_layout9 = findViewById(R.id.image_layout9);
		image_layout10 = findViewById(R.id.image_layout10);
		image_layout11 = findViewById(R.id.image_layout11);
		image_layout12 = findViewById(R.id.image_layout12);

		picker_image_list.add(picker_image1);
		picker_image_list.add(picker_image2);
		picker_image_list.add(picker_image3);
		picker_image_list.add(picker_image4);
		picker_image_list.add(picker_image5);
		picker_image_list.add(picker_image6);
		picker_image_list.add(picker_image7);
		picker_image_list.add(picker_image8);
		picker_image_list.add(picker_image9);
		picker_image_list.add(picker_image10);
		picker_image_list.add(picker_image11);
		picker_image_list.add(picker_image12);

		picker_text_list.add(picker_text1);
		picker_text_list.add(picker_text2);
		picker_text_list.add(picker_text3);
		picker_text_list.add(picker_text4);
		picker_text_list.add(picker_text5);
		picker_text_list.add(picker_text6);
		picker_text_list.add(picker_text7);
		picker_text_list.add(picker_text8);
		picker_text_list.add(picker_text9);
		picker_text_list.add(picker_text10);
		picker_text_list.add(picker_text11);
		picker_text_list.add(picker_text12);

		image_layout_list.add(image_layout1);
		image_layout_list.add(image_layout2);
		image_layout_list.add(image_layout3);
		image_layout_list.add(image_layout4);
		image_layout_list.add(image_layout5);
		image_layout_list.add(image_layout6);
		image_layout_list.add(image_layout7);
		image_layout_list.add(image_layout8);
		image_layout_list.add(image_layout9);
		image_layout_list.add(image_layout10);
		image_layout_list.add(image_layout11);
		image_layout_list.add(image_layout12);

		imgv_pre_page.setOnClickListener(instantKeyListener);
		imgv_next_page.setOnClickListener(instantKeyListener);
		btn_exit.setOnClickListener(instantKeyListener);
		btn_close_input.setOnClickListener(instantKeyListener);
		btn_search_input.setOnClickListener(instantKeyListener);

		search_editText.addTextChangedListener(this);
		search_editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if ((actionId & EditorInfo.IME_MASK_ACTION) != 0) {
					clickSearch();
					return true;
				} else {
					return false;
				}
			}
		});
	}

	private void autoSearch(boolean init) {

		if (init) {
//			sdcardDir = new File(Global.PATH_SDCARD);
//
//			searchFiles(sdcardDir);
//
//			Intent intent = getIntent();
//			if (intent.getIntExtra("pick_type", 0) != 5) {
//				//scan external card files
//				externalCardDir = new File(Global.PATH_EXTERNALSD);
//				if (externalCardDir.exists()) {
//					searchFiles(externalCardDir);
//				}
//			}
			for (String s : Global.SEARCHPATH) {
				sdcardDir = new File(Global.PATH_SDCARD + "/" + s);
                if (!sdcardDir.exists()) {
                    sdcardDir.mkdirs();
                }
				searchFiles(sdcardDir);
			}

			Collections.sort(resultPath, new SortIgnoreCase());
			if (currentPage == 0)
				currentPage = 1;

			if (resultPath.isEmpty()) {
				Toast.makeText(ImagePickerActivity.this, R.string.image_pick_no_image, Toast.LENGTH_LONG).show();
			}
		}

		if (resultPath.size() > 0) {
			totalPage = resultPath.size() >= NumPerPage_Portrait ? (resultPath.size() / NumPerPage_Portrait) + (resultPath.size() % NumPerPage_Portrait > 0 ? 1 : 0) : 1;
			currentPage = 1;
		} else {
			totalPage = 0;
			currentPage = 0;
		}

		closeAllImage();

		setPageInfo();

		int setSize = resultPath.size() > NumPerPage_Portrait ? NumPerPage_Portrait : resultPath.size();

		for (int i = 0; i < setSize; i++) {
			setPageImage(i, picker_image_list.get(i), picker_text_list.get(i), image_layout_list.get(i), resultPath.get(i));
		}
	}

	private void setPageInfo() {
		txtv_pageinfo.setText(currentPage + " of " + totalPage);
	}

	private void closeAllImage() {
		for (int i = 0; i < image_layout_list.size(); i++) {
			image_layout_list.get(i).setVisibility(View.GONE);
			picker_image_list.get(i).setImageDrawable(null);
			picker_text_list.get(i).setText("");
		}
	}

	private String getFileName(String path) {
		String fullPath = path;
		int index = fullPath.lastIndexOf("/");
		String fileName = fullPath.substring(index + 1);

		return fileName;
	}

	private void setPage(boolean next) {

		if (totalPage <= 1)
			return;

		List<String> tempArray;

		tempArray = input_layout.getVisibility() == View.VISIBLE && searchResultPath.size() != 0  ? searchResultPath : resultPath;

		closeAllImage();

		currentPage = next ? (currentPage + 1 > totalPage ? 1 : currentPage + 1) : (currentPage - 1 <= 0 ? totalPage : currentPage - 1);

		setPageInfo();

		int start = (NumPerPage_Portrait * currentPage) - NumPerPage_Portrait;

		int endSize = tempArray.size() >= currentPage * NumPerPage_Portrait ? currentPage * NumPerPage_Portrait : tempArray.size();

		for (int i = start; i < endSize; i++) {

			int position = i - NumPerPage_Portrait * (currentPage - 1);

			setPageImage(position, picker_image_list.get(position), picker_text_list.get(position), image_layout_list.get(position), tempArray.get(i));
		}
	}

	public class SortIgnoreCase implements Comparator<Object> {
		@Override
		public int compare(Object o1, Object o2) {
			String s1 = (String) o1;
			String s2 = (String) o2;

			final int indexNameS1 = s1.lastIndexOf('/');
			final String filenameS1 = ((indexNameS1 > 1) ? s1.substring(indexNameS1).toLowerCase().intern() : "");

			final int indexNameS2 = s2.lastIndexOf('/');
			final String filenameS2 = ((indexNameS2 > 1) ? s2.substring(indexNameS2).toLowerCase().intern() : "");

			return filenameS1.toLowerCase().compareTo(filenameS2.toLowerCase());
		}
	}
}