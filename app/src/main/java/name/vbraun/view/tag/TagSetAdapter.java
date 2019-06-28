package name.vbraun.view.tag;



import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import ntx.note.data.TagManager.Tag;
import ntx.note.data.TagManager.TagSet;
import ntx.note2.R;

public class TagSetAdapter extends ArrayAdapter {

	private static final String TAG = "TagSetAdapter";

	private TagSet tags;
	private Context context;
	private int highlight = Color.BLACK;

	public TagSetAdapter(Context mContext, TagSet active_tags) {
		super(mContext, R.layout.tag_item,
					active_tags.allTags());
		tags = active_tags;
		context = mContext;

	}

	public void setHighlightColor(int color) {
		highlight = color;
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RelativeLayout tag_item_layout;
        if (convertView == null) {
            tag_item_layout = (RelativeLayout) LayoutInflater.from(context).inflate(
                    R.layout.tag_item, parent, false);
        } else {
            tag_item_layout = (RelativeLayout) convertView;
        }
        Tag t = tags.allTags().get(position);
        TextView tv = (TextView) tag_item_layout.findViewById(R.id.txtv_tag_item);
        ImageView iv = (ImageView) tag_item_layout.findViewById(R.id.img_tag_item);
        tv.setText(t.toString());
        if (tags.contains(t)) {
            iv.setImageResource(R.drawable.checkbox_pressed);
            tv.setShadowLayer(20, 0, 0, highlight);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            iv.setImageResource(R.drawable.checkbox_normal);
            tv.setShadowLayer(0, 0, 0, highlight);
            tv.setTypeface(Typeface.DEFAULT);
        }
        return tag_item_layout;
    }
}

