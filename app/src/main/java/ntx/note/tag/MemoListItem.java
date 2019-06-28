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

import ntx.note.data.TagManager;
import ntx.note2.R;

public class MemoListItem extends LinearLayout {

    public @interface MemoListMode {
        int VIEW = 0;
        int EDITING = 1;
        int LOCK = 2;
    }

    private int mIndex;

    private LinearLayout mLayoutMemoViewMode;
    private LinearLayout mLayoutMemoEditMode;
    private ImageButton mBtnMemoEdit;
    private ImageButton mBtnMemoDelete;
    private ImageView mIvFlag;
    private TextView mTvMemoName;
    private EditText mEtMemoName;

    public interface ItemClickListener {
        void onItemClick(Object tag);

        void onItemEditClick(int index);

        void onItemEditCancelClick();

        void onItemEditOkClick(Object tag, String newTagString);

        void onItemDeleteClick(Object tag);
    }

    private ItemClickListener mCallback;

    public MemoListItem(Context ctx) {
        super(ctx);
        init(ctx, null, 0);
    }

    public MemoListItem(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init(ctx, attrs, 0);
    }

    public MemoListItem(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        init(ctx, attrs, defStyle);
    }

    private void init(Context ctx, AttributeSet attrs, int defStyle) {
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.memo_list_item, this);

        mLayoutMemoViewMode = (LinearLayout) findViewById(R.id.layout_memo_view_mode);
        mLayoutMemoEditMode = (LinearLayout) findViewById(R.id.layout_memo_edit_mode);
        mIvFlag = (ImageView) findViewById(R.id.iv_flag);
        mTvMemoName = (TextView) findViewById(R.id.tv_memo_name);
        mBtnMemoEdit = (ImageButton) findViewById(R.id.btn_memo_edit);
        mBtnMemoDelete = (ImageButton) findViewById(R.id.btn_memo_delete);
        mEtMemoName = (EditText) findViewById(R.id.et_memo_name);
        mEtMemoName.setSelectAllOnFocus(true);
        ImageButton btnMemoEditCancel = (ImageButton) findViewById(R.id.btn_memo_edit_cancel);
        ImageButton btnMemoEditOk = (ImageButton) findViewById(R.id.btn_memo_edit_ok);

        if (attrs != null) {
            final TypedArray typedArray = ctx.obtainStyledAttributes(attrs, R.styleable.MemoListItem, defStyle, 0);

            mIndex = typedArray.getInteger(R.styleable.MemoListItem_memo_list_index, 0);

            typedArray.recycle();
        }

        mLayoutMemoViewMode.setOnClickListener(onListItemButtonClickListener);
        mBtnMemoEdit.setOnClickListener(onListItemButtonClickListener);
        mBtnMemoDelete.setOnClickListener(onListItemButtonClickListener);
        btnMemoEditCancel.setOnClickListener(onListItemButtonClickListener);
        btnMemoEditOk.setOnClickListener(onListItemButtonClickListener);
    }

    public void registerItemClickListener(ItemClickListener listener) {
        this.mCallback = listener;
    }

    public void updateTagName(String tagName) {
        mTvMemoName.setText(tagName);
        mEtMemoName.setText(tagName);
    }

    public void showFlag(boolean isShow) {
        if (isShow)
            mIvFlag.setVisibility(VISIBLE);
        else
            mIvFlag.setVisibility(INVISIBLE);
    }

    public void updateListItemView(int mode) {
        if (MemoListMode.VIEW == mode) {
            mLayoutMemoEditMode.setVisibility(View.GONE);
            mLayoutMemoViewMode.setVisibility(View.VISIBLE);

            mBtnMemoEdit.setVisibility(View.VISIBLE);
            mBtnMemoDelete.setVisibility(View.VISIBLE);
        } else if (MemoListMode.EDITING == mode) {
            mLayoutMemoViewMode.setVisibility(View.GONE);
            mLayoutMemoEditMode.setVisibility(View.VISIBLE);

            mEtMemoName.requestFocus();
        } else if (MemoListMode.LOCK == mode) {
            mLayoutMemoEditMode.setVisibility(View.GONE);
            mLayoutMemoViewMode.setVisibility(View.VISIBLE);

            mBtnMemoEdit.setVisibility(View.INVISIBLE);
            mBtnMemoDelete.setVisibility(View.INVISIBLE);
        }
    }

    private OnClickListener onListItemButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mCallback != null) {
                switch (view.getId()) {
                    case R.id.layout_memo_view_mode:
                        mCallback.onItemClick(getTag());
                        break;
                    case R.id.btn_memo_edit:
                        mCallback.onItemEditClick(mIndex);
                        break;
                    case R.id.btn_memo_delete:
                        mCallback.onItemDeleteClick(getTag());
                        break;
                    case R.id.btn_memo_edit_cancel:
                        mEtMemoName.setText(mTvMemoName.getText().toString());
                        mCallback.onItemEditCancelClick();
                        break;
                    case R.id.btn_memo_edit_ok:
                        String newTagString = mEtMemoName.getText().toString();
                        TagManager.Tag oldTag = (TagManager.Tag) getTag();
                        if (!oldTag.toString().equals(newTagString))
                            mCallback.onItemEditOkClick(oldTag, newTagString);
                        else
                            mCallback.onItemEditCancelClick();
                        break;
                }
            }
        }
    };
}
