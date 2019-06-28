package utility;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import ntx.note2.R;

public class TextDialog extends Dialog {
    Context context;

    TextView content_text;

    String contentString;

    public TextDialog(Context context,String contentString) {
        super(context);
        this.context = context;
        this.contentString = contentString;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_text);
        content_text = findViewById(R.id.tv_alert_msg);
        content_text.setText(contentString);
    }
}
