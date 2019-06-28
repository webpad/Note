package name.vbraun.lib.pen;

import android.view.MotionEvent;

public class PenEventNTX extends PenEvent {
    private static final String TAG = "PenEventNTX";

    //    public static final int KITKAT = 19; // Android 4.4: KitKat
//    public static final int EINK_DITHER_MODE_DITHER_BY_API = ((Build.VERSION.SDK_INT>=KITKAT) ? android.view.View.EINK_DITHER_MODE_DITHER : (EINK_DITHER_MODE_DITHER | EINK_DITHER_COLOR_Y1));
    public static final int EINK_STATIC_MODE_SET = 0x10000000;
    public static final int EINK_STATIC_MODE_CLEAR = 0x20000000;

    public static final int UPDATE_MODE_APPNDRAWSTROKESYNC = 0x01000000;

    /* update mode for handwriting in eink */
    public static final int UPDATE_MODE_PARTIAL_DU =
            android.view.View.EINK_WAVEFORM_MODE_DU
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL;

    public static final int UPDATE_MODE_PARTIAL_DU_WAIT =
            android.view.View.EINK_WAVEFORM_MODE_DU
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL
                    | android.view.View.EINK_WAIT_MODE_WAIT;

    public static final int UPDATE_MODE_PARTIAL_A2 =
            android.view.View.EINK_WAVEFORM_MODE_A2
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL;

    public static final int UPDATE_MODE_PARTIAL_A2_WAIT =
            android.view.View.EINK_WAVEFORM_MODE_A2
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL
                    | android.view.View.EINK_WAIT_MODE_WAIT;

    public static final int UPDATE_MODE_PARTIAL_GC4 =
            android.view.View.EINK_WAVEFORM_MODE_GC4
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL;

    public static final int UPDATE_MODE_PARTIAL_DU_WITH_DITHER =
            android.view.View.EINK_WAVEFORM_MODE_DU
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL
                    | android.view.View.EINK_DITHER_MODE_DITHER;

    public static final int UPDATE_MODE_PARTIAL_A2_WITH_DITHER =
            android.view.View.EINK_WAVEFORM_MODE_A2
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL
                    | android.view.View.EINK_DITHER_MODE_DITHER;

    public static final int UPDATE_MODE_PARTIAL_GC4_WITH_DITHER =
            android.view.View.EINK_WAVEFORM_MODE_GC4
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL
                    | android.view.View.EINK_DITHER_MODE_DITHER;

    public static final int UPDATE_MODE_PARTIAL_DU_WITH_MONO =
            android.view.View.EINK_WAVEFORM_MODE_DU
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL
                    | android.view.View.EINK_DITHER_MODE_DITHER
                    | android.view.View.EINK_MONOCHROME_MODE_MONOCHROME;

    public static final int UPDATE_MODE_PARTIAL_A2_WITH_MONO =
            android.view.View.EINK_WAVEFORM_MODE_A2
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL
                    | android.view.View.EINK_DITHER_MODE_DITHER
                    | android.view.View.EINK_MONOCHROME_MODE_MONOCHROME;

    public static final int UPDATE_MODE_PARTIAL_GC4_WITH_MONO =
            android.view.View.EINK_WAVEFORM_MODE_GC4
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL
                    | android.view.View.EINK_DITHER_MODE_DITHER
                    | android.view.View.EINK_MONOCHROME_MODE_MONOCHROME;

    public static final int UPDATE_MODE_PARTIAL_GC16 =
            android.view.View.EINK_WAVEFORM_MODE_GC16
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL;

    public static final int UPDATE_MODE_PARTIAL_AUTO =
            android.view.View.EINK_WAVEFORM_MODE_AUTO
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL;

    public static final int UPDATE_MODE_FULL_GC16 =
            android.view.View.EINK_WAVEFORM_MODE_GC16
                    | android.view.View.EINK_UPDATE_MODE_FULL;

    public static final int UPDATE_MODE_PARTIAL_GL16 =
            android.view.View.EINK_WAVEFORM_MODE_GL16
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL;

    public static final int UPDATE_MODE_FULL_A2 =
            android.view.View.EINK_WAVEFORM_MODE_A2
                    | android.view.View.EINK_UPDATE_MODE_FULL
                    | android.view.View.EINK_MONOCHROME_MODE_MONOCHROME;

    public static final int UPDATE_MODE_FULL_DU =
            android.view.View.EINK_WAVEFORM_MODE_DU
                    | android.view.View.EINK_UPDATE_MODE_FULL
                    | android.view.View.EINK_MONOCHROME_MODE_MONOCHROME;

    public static final int UPDATE_MODE_FULL_DU_WITH_DITHER =
            android.view.View.EINK_WAVEFORM_MODE_DU
                    | android.view.View.EINK_UPDATE_MODE_FULL
                    | android.view.View.EINK_DITHER_MODE_DITHER;

    public static final int UPDATE_MODE_GLOBAL_PARTIAL_A2_WITH_DITHER =
            android.view.View.EINK_AUTO_MODE_REGIONAL
                    | android.view.View.EINK_WAIT_MODE_NOWAIT
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL
                    | android.view.View.EINK_WAVEFORM_MODE_A2
                    | android.view.View.EINK_DITHER_MODE_DITHER
                    | EINK_STATIC_MODE_SET;

    public static final int UPDATE_MODE_GLOBAL_PARTIAL_A2_WITH_DITHER_WITH_WAIT =
            android.view.View.EINK_AUTO_MODE_REGIONAL
                    | android.view.View.EINK_WAIT_MODE_WAIT
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL
                    | android.view.View.EINK_WAVEFORM_MODE_A2
                    | android.view.View.EINK_DITHER_MODE_DITHER
                    | EINK_STATIC_MODE_SET;

    public static final int UPDATE_MODE_GLOBAL_PARTIAL_DU_WITH_DITHER_WITH_WAIT =
            android.view.View.EINK_AUTO_MODE_REGIONAL
                    | android.view.View.EINK_WAIT_MODE_WAIT
                    | android.view.View.EINK_UPDATE_MODE_PARTIAL
                    | android.view.View.EINK_WAVEFORM_MODE_DU
                    | android.view.View.EINK_DITHER_MODE_DITHER
                    | EINK_STATIC_MODE_SET;

    public static final int UPDATE_MODE_GLOBAL_FULL_AUTO =
            android.view.View.EINK_WAIT_MODE_NOWAIT
                    | android.view.View.EINK_UPDATE_MODE_FULL
                    | android.view.View.EINK_WAVEFORM_MODE_AUTO
                    | EINK_STATIC_MODE_SET;


    public static final int UPDATE_MODE_GLOBAL_RESET = EINK_STATIC_MODE_CLEAR;

    public static final int UPDATE_MODE_PEN = Hardware.isPenUpdateModeDU() ? UPDATE_MODE_PARTIAL_DU : UPDATE_MODE_PARTIAL_A2;
    public static final int UPDATE_MODE_PEN_UP = UPDATE_MODE_APPNDRAWSTROKESYNC | android.view.View.EINK_WAVEFORM_MODE_GC16;
    public static final int UPDATE_MODE_SCREEN = UPDATE_MODE_FULL_GC16;
    public static final int UPDATE_MODE_SCREEN_2 = UPDATE_MODE_PARTIAL_GC16;

    @Override
    public boolean isPenEvent(MotionEvent event) {
        return event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
                || event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER;
    }

    @Override
    public boolean isPenButtonPressed(MotionEvent event) {
        // jimmychung 20160601 fix issue #1078 no erase function while press pen button.
        // return event.getButtonState() == MotionEvent.BUTTON_SECONDARY;
        return (event.getButtonState() & MotionEvent.BUTTON_SECONDARY) > 0 ? true : false;
    }
}
