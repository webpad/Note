package ntx.note.tag;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import ntx.note2.R;
import utility.ToggleImageButton;

public class TagListItem extends LinearLayout {

    public @interface TagListMode {
        int SELECTION = 0;
        int SETTING = 1;
        int EDITING = 2;
        int LOCK = 3;
    }

    private int mIndex;

    private LinearLayout mLayoutTextViewMode;
    private LinearLayout mLayoutEditTextMode;
    private TextView mTvTagName;
    private EditText mEtTagName;
    private ToggleImageButton mBtnTagCheck;
    private ImageButton mBtnTagEdit;
    private ImageButton mBtnTagDelete;
    private ImageView mIvBlank;

    public interface ItemButtonClickListener {
        void onEditBtnClick(int index);

        void onDeleteBtnClick(Object tag);

        void onEditingCancelBtnClick();

        void onEditingOkBtnClick(Object tag, String newName);

        void onCheckedChange(Object tag, boolean isCheck);
    }

    private ItemButtonClickListener mCallback;

    public TagListItem(Context ctx) {
        super(ctx);
        init(ctx, null, 0);
    }

    public TagListItem(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init(ctx, attrs, 0);
    }

    public TagListItem(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        init(ctx, attrs, defStyle);
    }

    private void init(Context ctx, AttributeSet attrs, int defStyle) {
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.tag_list_item, this);

        mLayoutTextViewMode = (LinearLayout) findViewById(R.id.layout_textview_mode);
        mTvTagName = (TextView) findViewById(R.id.tv_tag_name);
        mBtnTagCheck = (ToggleImageButton) findViewById(R.id.btn_tag_check);
        mBtnTagEdit = (ImageButton) findViewById(R.id.btn_tag_edit);
        mBtnTagDelete = (ImageButton) findViewById(R.id.btn_tag_delete);
        mLayoutEditTextMode = (LinearLayout) findViewById(R.id.layout_edittext_mode);
        mEtTagName = (EditText) findViewById(R.id.et_tag_name);
        ImageButton btnTagEditCancel = (ImageButton) findViewById(R.id.btn_tag_edit_cancel);
        ImageButton btnTagEditOk = (ImageButton) findViewById(R.id.btn_tag_edit_ok);
        mIvBlank = (ImageView) findViewById(R.id.iv_blank);

        if (attrs != null) {
            final TypedArray typedArray = ctx.obtainStyledAttributes(attrs, R.styleable.TagListItem, defStyle, 0);

            mIndex = typedArray.getInteger(R.styleable.TagListItem_index, 0);
            boolean isUnderLineVisiable = typedArray.getBoolean(R.styleable.TagListItem_underLine, false);
            findViewById(R.id.view_underline).setVisibility(isUnderLineVisiable ? VISIBLE : GONE);

            typedArray.recycle();
        }


        mBtnTagEdit.setOnClickListener(onListItemButtonClickListener);
        mBtnTagDelete.setOnClickListener(onListItemButtonClickListener);
        btnTagEditCancel.setOnClickListener(onListItemButtonClickListener);
        btnTagEditOk.setOnClickListener(onListItemButtonClickListener);

        mBtnTagCheck.setOnCheckedChangeListener(new ToggleImageButton.OnCheckedChangedListener() {
            @Override
            public void onCheckedChange(boolean b) {
                mCallback.onCheckedChange(getTag(), b);
            }
        });
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBtnTagCheck.isShown())
                    mBtnTagCheck.performClick();
            }
        });
    }

    public void setOnListItemButtonClickListener(ItemButtonClickListener listener) {
        this.mCallback = listener;
    }

    public void updateTagName(String tagName) {
        mTvTagName.setText(tagName);
        mEtTagName.setText(tagName);
    }

    public void setTagCheck(boolean isCheck) {
        if (isCheck)
            mBtnTagCheck.setChecked();
        else
            mBtnTagCheck.setUnchecked();
    }


    public void updateListItemView(int mode) {
        if (TagListMode.SELECTION == mode) {
            mLayoutEditTextMode.setVisibility(View.GONE);
            mLayoutTextViewMode.setVisibility(View.VISIBLE);

            mBtnTagEdit.setVisibility(View.GONE);
            mBtnTagDelete.setVisibility(View.GONE);
            mBtnTagCheck.setVisibility(View.VISIBLE);
            mIvBlank.setVisibility(View.VISIBLE);
        } else if (TagListMode.SETTING == mode) {
            mLayoutEditTextMode.setVisibility(View.GONE);
            mLayoutTextViewMode.setVisibility(View.VISIBLE);

            mBtnTagCheck.setVisibility(View.GONE);
            mIvBlank.setVisibility(View.GONE);
            mBtnTagEdit.setVisibility(View.VISIBLE);
            mBtnTagDelete.setVisibility(View.VISIBLE);
        } else if (TagListMode.EDITING == mode) {
            mLayoutTextViewMode.setVisibility(View.GONE);
            mLayoutEditTextMode.setVisibility(View.VISIBLE);
        } else if (TagListMode.LOCK == mode) {
            mLayoutEditTextMode.setVisibility(View.GONE);
            mLayoutTextViewMode.setVisibility(View.VISIBLE);

            mBtnTagEdit.setVisibility(View.INVISIBLE);
            mBtnTagDelete.setVisibility(View.INVISIBLE);
            mBtnTagCheck.setVisibility(View.GONE);
            mIvBlank.setVisibility(View.GONE);
        }
    }

    private View.OnClickListener onListItemButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mCallback != null) {
                switch (view.getId()) {
                    case R.id.btn_tag_edit:
                        mCallback.onEditBtnClick(mIndex);
                        break;
                    case R.id.btn_tag_delete:
                        mCallback.onDeleteBtnClick(getTag());
                        break;
                    case R.id.btn_tag_edit_cancel:
                        mCallback.onEditingCancelBtnClick();
                        break;
                    case R.id.btn_tag_edit_ok:
                        mCallback.onEditingOkBtnClick(getTag(), mEtTagName.getText().toString());
                        break;
                    default:
                        break;
                }
            }
        }
    };
}
