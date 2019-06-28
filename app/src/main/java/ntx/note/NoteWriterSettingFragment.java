package ntx.note;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;

import name.vbraun.lib.pen.PenEventNTX;
import name.vbraun.view.write.HandwriterView;
import name.vbraun.view.write.TouchHandlerPenABC;
import ntx.note.pencalibration.PenCalibrateActivity;
import ntx.note2.R;
import utility.CustomDialogFragment;

import static android.app.Activity.RESULT_OK;
import static ntx.note.NoteWriterActivity.REQUEST_PEN_OFFSET;

public class NoteWriterSettingFragment extends CustomDialogFragment {
    private Activity mActivity;
    private View rootView;

    private Spinner spinnerPenOffsetY;
    private Spinner spinnerPenOffsetX;
    private int tmp_intPenOffsetX;
    private int tmp_intPenOffsetY;

    private Switch mSwShowThemeMemo;

    private Handler mHandler = new Handler();

    private Runnable mRunnableInvalidate = new Runnable() {
        @Override
        public void run() {
            rootView.invalidate(PenEventNTX.UPDATE_MODE_PARTIAL_GL16);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mActivity = getActivity();
        rootView = inflater.inflate(R.layout.note_setting_dialog, container, false);
        initView(rootView);
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_PEN_OFFSET == requestCode) {
            if (resultCode != RESULT_OK)
                return;
            int auto_calibration_offset_x_value = data.getIntExtra(PenCalibrateActivity.RESULT_PEN_CALIBRATION_OFFSET_X,
                    0);
            int auto_calibration_offset_y_value = data.getIntExtra(PenCalibrateActivity.RESULT_PEN_CALIBRATION_OFFSET_Y,
                    0);
            for (int i = 0; i < getResources().getIntArray(R.array.dlg_offset_x_values).length; i++) {
                if (auto_calibration_offset_x_value == getResources().getIntArray(R.array.dlg_offset_x_values)[i]) {
                    spinnerPenOffsetX.setSelection(i);
                }
            }
            for (int i = 0; i < getResources().getIntArray(R.array.dlg_offset_y_values).length; i++) {
                if (auto_calibration_offset_y_value == getResources().getIntArray(R.array.dlg_offset_y_values)[i]) {
                    spinnerPenOffsetY.setSelection(i);
                }
            }
        }
    }

    private void initView(View v) {

        Button btnClose = (Button) v.findViewById(R.id.btn_setting_close);
        btnClose.setOnClickListener(onBtnClickListener);

        RadioGroup radioGroupToolboxLayout = (RadioGroup) v.findViewById(R.id.radio_group_toolbox_layout);
        radioGroupToolboxLayout.setOnCheckedChangeListener(onToolboxLayoutCheckedChangeListener);
        if (ToolboxConfiguration.getInstance().isToolbarAtLeft())
            radioGroupToolboxLayout.check(R.id.btn_toolbox_layout_left);
        else
            radioGroupToolboxLayout.check(R.id.btn_toolbox_layout_right);

        LinearLayout btnAutoCalibration = (LinearLayout) v.findViewById(R.id.btn_auto_calibration);
        btnAutoCalibration.setOnClickListener(onBtnClickListener);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity.getBaseContext());

        spinnerPenOffsetX = (Spinner) v.findViewById(R.id.spinner_offset_x);
        String[] pen_offset_x_table = getResources().getStringArray(R.array.toolbox_offset_x);
        ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(mActivity.getBaseContext(), R.layout.toolbox_spinner_item,
                pen_offset_x_table);
        mAdapter.setDropDownViewResource(R.layout.toolbox_spinner_item);
        spinnerPenOffsetX.setAdapter(mAdapter);
        spinnerPenOffsetX.setOnItemSelectedListener(new OnSpinnerItemClicked());
        tmp_intPenOffsetX = settings.getInt(HandwriterView.KEY_PEN_OFFSET_X, 0);
        for (int i = 0; i < getResources().getIntArray(R.array.dlg_offset_x_values).length; i++) {
            if (tmp_intPenOffsetX == getResources().getIntArray(R.array.dlg_offset_x_values)[i]) {
                spinnerPenOffsetX.setSelection(i);
            }
        }

        spinnerPenOffsetY = (Spinner) v.findViewById(R.id.spinner_offset_y);
        String[] pen_offset_y_table = getResources().getStringArray(R.array.toolbox_offset_y);
        mAdapter = new ArrayAdapter<String>(mActivity.getBaseContext(), R.layout.toolbox_spinner_item, pen_offset_y_table);
        mAdapter.setDropDownViewResource(R.layout.toolbox_spinner_item);
        spinnerPenOffsetY.setAdapter(mAdapter);
        spinnerPenOffsetY.setOnItemSelectedListener(new OnSpinnerItemClicked());
        tmp_intPenOffsetY = settings.getInt(HandwriterView.KEY_PEN_OFFSET_Y, 0);
        for (int i = 0; i < getResources().getIntArray(R.array.dlg_offset_y_values).length; i++) {
            if (tmp_intPenOffsetY == getResources().getIntArray(R.array.dlg_offset_y_values)[i]) {
                spinnerPenOffsetY.setSelection(i);
            }
        }

        mSwShowThemeMemo = (Switch) v.findViewById(R.id.sw_show_theme_memo);
        mSwShowThemeMemo.setChecked(ToolboxConfiguration.getInstance().showMemoTheme());
        mSwShowThemeMemo.setOnCheckedChangeListener(onSwShowMemoThemeCheckedChangeListener);
    }

    private Button.OnClickListener onBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (R.id.btn_auto_calibration == view.getId()) {
                Intent intent = new Intent(mActivity.getApplicationContext(), PenCalibrateActivity.class);
                startActivityForResult(intent, REQUEST_PEN_OFFSET);
                TouchHandlerPenABC.isPopupwindow = true;
            } else if (R.id.btn_setting_close == view.getId()) {
                ((NoteWriterActivity) mActivity).applyNoteWriterSettings(tmp_intPenOffsetX, tmp_intPenOffsetY);
                dismiss();
            }
        }
    };

    private class OnSpinnerItemClicked implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View item, int position, long id) {
            if (parent.getId() == R.id.spinner_offset_x) {
                tmp_intPenOffsetX = getResources().getIntArray(R.array.dlg_offset_x_values)[position];
                mHandler.postDelayed(mRunnableInvalidate, 500);
            } else if (parent.getId() == R.id.spinner_offset_y) {
                tmp_intPenOffsetY = getResources().getIntArray(R.array.dlg_offset_y_values)[position];
                mHandler.postDelayed(mRunnableInvalidate, 500);
            }
        }

        @Override
        public void onNothingSelected(AdapterView parent) {
        }
    }

    private RadioGroup.OnCheckedChangeListener onToolboxLayoutCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (R.id.btn_toolbox_layout_left == checkedId) {
                ToolboxConfiguration.getInstance().setToolbarAtLeft(true);
            } else {
                ToolboxConfiguration.getInstance().setToolbarAtLeft(false);
            }
        }
    };

    private CompoundButton.OnCheckedChangeListener onSwShowMemoThemeCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            ToolboxConfiguration.getInstance().setShowMemoTheme(isChecked);
        }
    };
}
