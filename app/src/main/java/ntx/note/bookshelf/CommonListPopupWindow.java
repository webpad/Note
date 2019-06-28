package ntx.note.bookshelf;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import ntx.note2.R;

/**
 * Created by karote on 2018/11/21.
 */

public class CommonListPopupWindow extends PopupWindow {
    private Context mCtx;
    private View.OnClickListener callback;
    private String[] mContentList;
    private boolean[] mContentCheckStatus;

    public CommonListPopupWindow(Context ctx, String title, String[] contentList, int checkIndex) {
        this.mCtx = ctx;
        this.mContentList = contentList;
        this.mContentCheckStatus = new boolean[mContentList.length];
        for (int i = 0; i < mContentCheckStatus.length; i++) {
            mContentCheckStatus[i] = false;
        }
        this.mContentCheckStatus[checkIndex] = true;

        setContentView(LayoutInflater.from(ctx).inflate(R.layout.popupwindow_common_list, null));
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setOutsideTouchable(true);
        setFocusable(true);
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View popupView = getContentView();

        TextView tvTitle = (TextView) popupView.findViewById(R.id.tv_title);
        ListView listContent = (ListView) popupView.findViewById(R.id.list_content);

        tvTitle.setText(title);

        CommonListAdapter adapter = new CommonListAdapter();
        listContent.setAdapter(adapter);
    }

    public void setOnItemClickListener(View.OnClickListener listener) {
        this.callback = listener;
    }

    private class CommonListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mContentList.length;
        }

        @Override
        public Object getItem(int position) {
            return mContentList[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View layout;
            if (convertView == null) {
                layout = LayoutInflater.from(mCtx).inflate(R.layout.check_icon_text_list_item, parent, false);
            } else {
                layout = convertView;
            }

            ImageView checkIcon = (ImageView) layout.findViewById(R.id.iv_check_icon);
            TextView textView = (TextView) layout.findViewById(R.id.tv_text);

            checkIcon.setVisibility(mContentCheckStatus[position] ? View.VISIBLE : View.INVISIBLE);
            textView.setText(mContentList[position]);

            layout.setTag(position);
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (int i = 0; i < mContentCheckStatus.length; i++) {
                        mContentCheckStatus[i] = false;
                    }
                    mContentCheckStatus[(int) view.getTag()] = true;
                    notifyDataSetChanged();

                    if (callback != null)
                        callback.onClick(view);
                }
            });

            return layout;
        }
    }

}
