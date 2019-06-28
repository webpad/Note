package ntx.note.bookshelf;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;

import ntx.note.data.Book;
import ntx.note2.R;
import utility.ToggleImageButton;

import static ntx.note.Global.STRING_KB;
import static ntx.note.Global.STRING_MB;

/**
 * Created by karote on 2019/3/19.
 */

public class FreeStorageListItemView extends FrameLayout {
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    private ToggleImageButton mBtnCheckbox;

    public interface FreeStorageListItemClickListener {
        void onCheckedChange(Object viewTag, boolean b);
    }

    private FreeStorageListItemClickListener mCallback;

    public FreeStorageListItemView(Context context, Book item) {
        super(context);
        View.inflate(context, R.layout.free_storage_list_item, this);

        this.setTag(item);
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtnCheckbox.performClick();
            }
        });

        TextView tvName = (TextView) findViewById(R.id.tv_name);
        tvName.setText(item.getTitle());

        long fileSize = item.getBookSizeInStorage();
        String sizeStr = "< 1 " + STRING_KB;
        if ((fileSize / 1024f / 1024f) > 1) {
            sizeStr = " " + (int) (fileSize / 1024f / 1024f) + " " + STRING_MB;
        } else if ((fileSize / 1024f) > 1) {
            sizeStr = " " + (int) (fileSize / 1024f) + " " + STRING_KB;
        }
        TextView tvSize = (TextView) findViewById(R.id.tv_size);
        tvSize.setText(String.valueOf(sizeStr));

        TextView tvDate = (TextView) findViewById(R.id.tv_date);
        tvDate.setText(String.valueOf(simpleDateFormat.format(item.getMtime().toMillis(false))));

        mBtnCheckbox = (ToggleImageButton) findViewById(R.id.btn_checkbox);
        mBtnCheckbox.setTag(item);
        mBtnCheckbox.setOnCheckedChangeListener(new ToggleImageButton.OnCheckedChangedListener() {
            @Override
            public void onCheckedChange(boolean value) {
                if (mCallback != null) {
                    mCallback.onCheckedChange(getTag(), value);
                }
            }
        });
    }

    public void setOnItemClickListener(FreeStorageListItemClickListener listener) {
        this.mCallback = listener;
    }

    public void updateCheckBox(boolean isChecked) {
        if (isChecked)
            mBtnCheckbox.setChecked();
        else
            mBtnCheckbox.setUnchecked();
    }
}
