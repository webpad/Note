package name.vbraun.filepicker;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import ntx.note2.R;
import utility.ToggleImageButton;

/**
 * Created by karote on 2018/11/28.
 */

public class ImportItemGroupHeaderView extends ImportItemView {
    private ToggleImageButton mBtnCheckbox;

    private ImportItemClickListener mCallback;

    public ImportItemGroupHeaderView(Context context, int groupIndex, int groupSize) {
        super(context);
        View.inflate(context, R.layout.import_list_header_item, this);

        this.setTag(groupIndex);
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtnCheckbox.performClick();
            }
        });

        TextView tvHeaderContent = (TextView) findViewById(R.id.tv_content);
        String s = context.getString(R.string.import_list_group_header, groupSize);
        tvHeaderContent.setText(s);

        mBtnCheckbox = (ToggleImageButton) findViewById(R.id.btn_checkbox);
        mBtnCheckbox.setTag(groupIndex);
        mBtnCheckbox.setOnCheckedChangeListener(new ToggleImageButton.OnCheckedChangedListener() {
            @Override
            public void onCheckedChange(boolean value) {
                if (mCallback != null) {
                    mCallback.onCheckedChange(ImportItem.ImportItemType.GROUP_HEADER, getTag(), value);
                }
            }
        });
    }

    public void setOnItemClickListener(ImportItemClickListener listener) {
        this.mCallback = listener;
    }

    @Override
    public void updateCheckBox(boolean isChecked) {
        if (isChecked)
            mBtnCheckbox.setChecked();
        else
            mBtnCheckbox.setUnchecked();
    }
}
