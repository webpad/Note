
package ntx.note.pencalibration;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import ntx.note2.R;

public class PenCalibrateActivity extends Activity {

    private Context mContext;

    private PenCalibrateView myNewView;

    private LinearLayout mainLayout, viewLayout;
    protected TextView txtvThicknessNum;
    protected Spinner refreshModeSpinner;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();

        setContentView(R.layout.pen_calibration_activity);
        mainLayout = (LinearLayout) findViewById(R.id.pen_calibration_main_layout);

        txtvThicknessNum = (TextView) findViewById(R.id.pen_calibration_text);

        viewLayout = (LinearLayout) findViewById(R.id.pen_calibration_view);

        myNewView = new PenCalibrateView(this);
        viewLayout.addView(myNewView);
    }


    public static final String RESULT_PEN_CALIBRATION_OFFSET_X = "result_pen_calibration_offset_x";
    public static final String RESULT_PEN_CALIBRATION_OFFSET_Y = "result_pen_calibration_offset_y";
    protected void onPenCalibrationFinished(int x, int y) {
        Intent intent = new Intent();
        intent.putExtra(RESULT_PEN_CALIBRATION_OFFSET_X, x);
        intent.putExtra(RESULT_PEN_CALIBRATION_OFFSET_Y, y);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }
}


