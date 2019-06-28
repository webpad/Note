package name.vbraun.view.tag;



import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import ntx.note.data.TagManager.TagSet;
import ntx.note2.R;

public class TagListView extends RelativeLayout {
	private static final String TAG = "TagsListView";

	protected Context context;
	protected View layout;
	protected TagSetAdapter adapter = null;
	protected ListView list;
	protected EditText edittext;
	protected Button new_tag_button;
	protected TextView label, remind;
	protected TagSet tags = null;

	public void showNewTextEdit(boolean show) {
		if (show) {
			edittext.setVisibility(View.VISIBLE);
			label.setVisibility(View.VISIBLE);
			new_tag_button.setVisibility(View.VISIBLE);
		} else {
			edittext.setVisibility(View.GONE);
			label.setVisibility(View.GONE);
			new_tag_button.setVisibility(View.GONE);
			list.setPadding(
					list.getPaddingLeft(),
					0,
					list.getPaddingRight(),
					list.getPaddingBottom());
		}
	}

	public void notifyTagsChanged() {
        if (tags.allTags().size() == 0) {
            remind.setText(R.string.tag_list_remind_string_empty);
        } else {
            remind.setText(R.string.tag_list_remind_string);
        }
		adapter.notifyDataSetChanged();
	}

	public TagListView(Context mContext, AttributeSet attrs) {
		super(mContext, attrs);
		context = mContext;
		createLayout(context);
	}

	public TagListView(Context mContext) {
		super(mContext);
		context = mContext;
		createLayout(context);
	}

	private void createLayout(Context context) {
		Log.d(TAG, "TLV<---createLayout");
		LayoutInflater layoutInflater = (LayoutInflater)
			context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layout = layoutInflater.inflate(R.layout.tag_list, this);
        list = (ListView) findViewById(R.id.tag_list);
        edittext = (EditText) findViewById(R.id.tag_list_text);
        new_tag_button = (Button) findViewById(R.id.tag_list_new_tag_button);
        label = (TextView) findViewById(R.id.tag_list_new_label);
        remind = (TextView) findViewById(R.id.tag_list_remind_text);
    }

	public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
		list.setOnItemClickListener(listener);
	}

	public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener listener) {
		list.setOnItemLongClickListener(listener);
	}

	@Override
	public void setOnKeyListener(OnKeyListener listener) {
        edittext.setOnKeyListener(listener);
	}

    @Override
    public void setOnClickListener(OnClickListener listener) {
        new_tag_button.setOnClickListener(listener);
    }

	public ListView getAdapterView() {
		return list;
	}

	public void setTagSet(TagSet tags) {
	    this.tags = tags;
        if (tags.allTags().size() == 0) {
            remind.setText(R.string.tag_list_remind_string_empty);
        }
		adapter = new TagSetAdapter(context, tags);
		adapter.setHighlightColor(Color.BLACK);
        list.setAdapter(adapter);
	}

}
