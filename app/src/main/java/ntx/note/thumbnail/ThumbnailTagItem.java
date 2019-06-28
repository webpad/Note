package ntx.note.thumbnail;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import ntx.note.data.TagManager;
import ntx.note2.R;
import utility.ToggleImageButton;

/**
 * Created by karote on 2018/10/22.
 */

public class ThumbnailTagItem extends LinearLayout {
    private TextView mTvTagName;
    private ImageView mIvQuickTag;
    private ToggleImageButton mBtnTagCheck;

    public interface TagCheckChangedListener {
        void onCheckedChange(Object tag, boolean b);
    }

    private TagCheckChangedListener mCallback;

    public ThumbnailTagItem(Context context) {
        super(context);
        init(context);
    }

    public ThumbnailTagItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ThumbnailTagItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context ctx) {
        View.inflate(ctx, R.layout.book_thumbnail_tag_list_item, this);

        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtnTagCheck.performClick();
            }
        });

        mTvTagName = (TextView) findViewById(R.id.tv_tag_name);
        mIvQuickTag = (ImageView) findViewById(R.id.iv_quick_tag);

        mBtnTagCheck = (ToggleImageButton) findViewById(R.id.btn_tag_check);
        mBtnTagCheck.setOnCheckedChangeListener(new ToggleImageButton.OnCheckedChangedListener() {
            @Override
            public void onCheckedChange(boolean value) {
                if (mCallback != null) {
                    mCallback.onCheckedChange(getTag(), value);
                }
            }
        });
    }

    public void setOnTagCheckedChangeListener(TagCheckChangedListener listener) {
        this.mCallback = listener;
    }

    public void setTagName(String tagName) {
        mTvTagName.setText(tagName);
        if (tagName.equals(TagManager.QUICK_TAG_NAME)) {
            mTvTagName.setVisibility(GONE);
            mIvQuickTag.setVisibility(VISIBLE);
        } else {
            mTvTagName.setVisibility(VISIBLE);
            mIvQuickTag.setVisibility(GONE);
        }
    }

    public String getTagName() {
        return mTvTagName.getText().toString();
    }

    public void setTagCheck(boolean isCheck) {
        if (isCheck)
            mBtnTagCheck.setChecked();
        else
            mBtnTagCheck.setUnchecked();
    }

    public boolean isTagCheck() {
        return mBtnTagCheck.isChecked();
    }
}
