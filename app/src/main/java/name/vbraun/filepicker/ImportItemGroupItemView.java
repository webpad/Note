package name.vbraun.filepicker;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;

import ntx.note2.R;
import utility.ToggleImageButton;

import static ntx.note.Global.STRING_KB;
import static ntx.note.Global.STRING_MB;

/**
 * Created by karote on 2018/11/28.
 */

public class ImportItemGroupItemView extends ImportItemView {
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    private ToggleImageButton mBtnCheckbox;
    private int mGroupIndex;

    private ImportItemClickListener mCallback;

    public ImportItemGroupItemView(Context context, int groupIndex, ImportItem item) {
        super(context);
        this.mGroupIndex = groupIndex;
        View.inflate(context, R.layout.import_list_sub_item, this);

        this.setTag(item);
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtnCheckbox.performClick();
            }
        });

        TextView tvName = (TextView) findViewById(R.id.tv_name);
        tvName.setText(item.getFileName());

        long fileSize = item.getFileSize();
        String sizeStr = "< 1 " + STRING_KB;
        if ((fileSize / 1024f / 1024f) > 1) {
            sizeStr = " " + (int) (fileSize / 1024f / 1024f) + " " + STRING_MB;
        } else if ((fileSize / 1024f) > 1) {
            sizeStr = " " + (int) (fileSize / 1024f) + " " + STRING_KB;
        }
        TextView tvSize = (TextView) findViewById(R.id.tv_size);
        tvSize.setText(String.valueOf(sizeStr));

        TextView tvDate = (TextView) findViewById(R.id.tv_date);
        tvDate.setText(String.valueOf(simpleDateFormat.format(item.getFileDate())));

        mBtnCheckbox = (ToggleImageButton) findViewById(R.id.btn_checkbox);
        mBtnCheckbox.setTag(item);
        mBtnCheckbox.setOnCheckedChangeListener(new ToggleImageButton.OnCheckedChangedListener() {
            @Override
            public void onCheckedChange(boolean value) {
                if (mCallback != null) {
                    mCallback.onCheckedChange(ImportItem.ImportItemType.GROUP_ITEM, getTag(), value);
                }
            }
        });
    }

    public void setOnItemClickListener(ImportItemClickListener listener) {
        this.mCallback = listener;
    }

    public int getGroupIndex() {
        return mGroupIndex;
    }

    @Override
    public void updateCheckBox(boolean isChecked) {
        if (isChecked)
            mBtnCheckbox.setChecked();
        else
            mBtnCheckbox.setUnchecked();
    }
}
