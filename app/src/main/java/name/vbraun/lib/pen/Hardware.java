package name.vbraun.lib.pen;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;

import junit.framework.Assert;

import ntx.note.Global;
import ntx.note2.R;

import java.util.ArrayList;

public class Hardware {
	private final static String TAG = "PenHardware";

	public static final String KEY_OVERRIDE_PEN_TYPE = "override_pen_type";

	public static final String PEN_TYPE_AUTO 			= "PEN_TYPE_AUTO";
    public static final String PEN_TYPE_CAPACITIVE 		= "PEN_TYPE_CAPACITIVE";
    public static final String PEN_TYPE_THINKPAD_TABLET = "PEN_TYPE_THINKPAD_TABLET";
    public static final String PEN_TYPE_SAMSUNG_NOTE 	= "PEN_TYPE_SAMSUNG_NOTE";
    public static final String PEN_TYPE_LEFT_ALT 		= "PEN_TYPE_LEFT_ALT";
    public static final String PEN_TYPE_ICS 			= "PEN_TYPE_ICS";
    public static final String PEN_TYPE_HTC 			= "PEN_TYPE_HTC";

    public static final String TYPE_E60Q20   = "E60Q20";
    public static final String TYPE_E60Q30   = "E60Q30";
    public static final String TYPE_E60Q50   = "E60Q50";
    public static final String TYPE_E60Q60   = "E60Q60";
    public static final String TYPE_E60QR0   = "E60QR0";
    public static final String TYPE_E60QR2   = "E60QR2";
    public static final String TYPE_E60QD0   = "E60QD0";
    public static final String TYPE_E60QH0   = "E60QH0";
    public static final String TYPE_E70Q30   = "E70Q30";
    public static final String TYPE_EA0Q00   = "EA0Q00";
    public static final String TYPE_ED0Q00   = "ED0Q00";

	private final String model;
	private boolean mHasPenDigitizer;
	private boolean mHasPressureSensor;
	private PenEvent mPenEvent;

	private static Hardware instance = null;

	public static Hardware getInstance(Context context) {
		if (instance == null)
			instance = new Hardware(context);
		return instance;
	}

	/* set the HW button listener */
	HardwareButtonListener buttonListener = null;

	public void setOnHardwareButtonListener(HardwareButtonListener buttonListener) {
		this.buttonListener = buttonListener;
	}

	protected void callOnHardwareButtonListener(HardwareButtonListener.Type button) {
		if (buttonListener != null)
			buttonListener.onHardwareButtonListener(button);
	}

	private static ArrayList<String> tabletMODELwithoutPressure = new ArrayList<String>() {
		private static final long serialVersionUID = 1868225200818950866L; 	{
	    add("K1");   // Lenovo K1
	    add("A500");  // Acer Iconia A500
	    add("A501");  // Acer Iconia A501
	    add("AT100");   add("AT1S0");   // Toshiba thrive 10" and 7"
	    add("GT-P1000"); add("GT-P1000L"); add("GT-P1000N"); add("SGH-T849"); // Galaxy tab 10.1"
	    add("GT-P7510");  // Galaxy tab 10.1"
	    add("GT-P7501");  // Galaxy tab 10.1N"
	    add("GT-P6810");  // Samsung Galaxy Tab 7.7
	    add("GT-P6210");  // Samsung Galaxy Tab 7.0 Plus
	    add("Galaxy Nexus");  // Google Galaxy Nexus
	    add("VTAB1008");   // Vizio VTAB1008
	}};


	private Hardware(Context context) {
//		model = android.os.Build.MODEL;
        model = SystemProperties.get("ro.product.hardwareType", "");
//		Log.v(TAG, "HW<--- model=" + model);
		if (Global.releaseModeOEM){
			Log.i(TAG, "HW<---releaseModeOEM");
			autodetect(context);
		} else {
			Log.i(TAG, "HW<---forceFromPreferences");
			forceFromPreferences(context);
		}
		Log.v(TAG, "Model = >"+model+"<, pen digitizer: "+mHasPenDigitizer);
	}

	public void autodetect(Context context) {
		if (
				model.equalsIgnoreCase("ThinkPad Tablet") ||  // Lenovo ThinkPad Tablet
				model.matches("crane-iNote.*")) { // http://www.ibnote.com/
			forceThinkpadTablet();
		} else if (
				model.equalsIgnoreCase("OP080") ||
				model.matches("GT-N8...") || // Galaxy Note 10.1
				model.matches("GT-N5...") || // Galaxy Note 8(GT-N5100)
				model.matches("GT-N7...") || // Galaxy Note (5"), Note II, Note 3
				model.matches("SM-N9...") || // Galaxy Note 3(SM-N900)
				model.equalsIgnoreCase("GT-I9220") || // US Galaxy note 5"
				model.equalsIgnoreCase("SGH-i717") ) { // Galaxy note 5" AT&T
			forceSamsungNote();
		} else if (
				model.equalsIgnoreCase("HTC_Flyer_P512_NA") ||
				model.equalsIgnoreCase("HTC Flyer P510e") ||
				model.equalsIgnoreCase("HTC Flyer P512") ||
				model.equalsIgnoreCase("HTC P510e")) {
			forceHTC();
		} else if ( isEinkHandWritingHardwareType() ) { // Netronix EPD Note
			forceNTX();
		} else {
			// defaults; this works on HTC devices but might be more general
			mHasPenDigitizer = context.getPackageManager().hasSystemFeature("android.hardware.touchscreen.pen");
			mHasPressureSensor = !tabletMODELwithoutPressure.contains(model);
			if (mHasPenDigitizer)
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
					mPenEvent = new PenEventHoneycomb();
				else
					mPenEvent = new PenEventICS();
			else
				mPenEvent = new PenEvent();
		}
	}

	public void forceThinkpadTablet() {
		mHasPenDigitizer = true;
		mHasPressureSensor = true;
		mPenEvent = new PenEventThinkPadTablet();
	}

	public void forceSamsungNote() {
		mHasPenDigitizer = true;
		mHasPressureSensor = true;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			mPenEvent = new PenEventSamsungNoteHoneycomb();
		else
			mPenEvent = new PenEventSamsungNote();
	}

	public void forceCapacitivePen() {
		mHasPenDigitizer = false;
		mHasPressureSensor = true;
		mPenEvent = new PenEvent();
	}

	public void forceLeftAlt() {
		mHasPenDigitizer = true;
		mHasPressureSensor = true;
		mPenEvent = new PenEventLeftAlt();
	}

	public void forceICS() {
		mHasPenDigitizer = true;
		mHasPressureSensor = true;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			mPenEvent = new PenEventHoneycomb();
		else
			mPenEvent = new PenEventICS();
	}

	public void forceHTC() {
		mHasPenDigitizer = true;
		mHasPressureSensor = true;
		mPenEvent = new PenEventHTC();
	}

	public void forceNTX() {
		mHasPenDigitizer = true;
		mHasPressureSensor = true;
		mPenEvent = new PenEventNTX();
	}

	public void forceFromPreferences(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		String penType = settings.getString(KEY_OVERRIDE_PEN_TYPE,
				context.getString(R.string.preferences_override_pen_type_default));
		Log.i(TAG, "HW<---forceFromPreferences() pen type = "+ penType );
		if (penType.equals(PEN_TYPE_AUTO))
			autodetect(context);
		else if (penType.equals(PEN_TYPE_CAPACITIVE))
			forceCapacitivePen();
		else if (penType.equals(PEN_TYPE_THINKPAD_TABLET))
			forceThinkpadTablet();
		else if (penType.equals(PEN_TYPE_SAMSUNG_NOTE))
			forceSamsungNote();
		else if (penType.equals(PEN_TYPE_LEFT_ALT))
			forceLeftAlt();
		else if (penType.equals(PEN_TYPE_ICS))
			forceICS();
		else if (penType.equals(PEN_TYPE_HTC))
			forceHTC();
		else {
			SharedPreferences.Editor editor = settings.edit();
			editor.remove(KEY_OVERRIDE_PEN_TYPE);
			editor.commit();
			Assert.fail("The preference "+KEY_OVERRIDE_PEN_TYPE+" has invalid value: "+penType);
		}
	}	
	   
	public static boolean isEinkHardwareType() {
		return SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E60Q20)
				|| SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E60Q30)
				|| SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E60Q50)
				|| SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E60Q60)
				|| SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_ED0Q00)
				|| SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E60QD0)
				|| SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E70Q30)
				|| SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_EA0Q00)
				|| SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E60QR0)
				|| SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E60QR2);
	}

    public static boolean isEinkHandWritingHardwareType() {
        return SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E60Q60)
                || SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_ED0Q00)
                || SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E70Q30)
                || SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_EA0Q00)
                || SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E60QR0)
                || SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E60QR2);
    }
	
    public static boolean isEinkUsingLargerUI() {
        return SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E60Q60)
                || SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E60QR0)
                || SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E60QR2)
                || SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E70Q30);
    }

    public static boolean isPenUpdateModeDU() {
        return SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_ED0Q00)
                ;
    }

	public static boolean hasExternalSDCard() {
		return !SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_EA0Q00);
	}

    public static boolean isHasPhysicalKey() {
        return (!isHasPhysicalBackKey() && !isHasPhysicalHomeKey());
    }
    
    public static boolean isHasPhysicalBackKey() {
        return KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
    }
    
    public static boolean isHasPhysicalHomeKey() {
        return KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);
    }
	
	/**
	 * Test whether the device has an active pen
	 * @return boolean
	 */
	public static boolean hasPenDigitizer() {
		Assert.assertNotNull(instance);
		return instance.mHasPenDigitizer;
	}

	/**
	 * Test whether the device has a working pressure sensor
	 * @return
	 */
	public static boolean hasPressureSensor() {
		Assert.assertNotNull(instance);
		return instance.mHasPressureSensor;
	}

	/**
	 * Test whether the event may have been caused by the stylus
	 * @param event A MotionEvent
	 * @return
	 */
	public static boolean isPenEvent(MotionEvent event) {
		Assert.assertNotNull(instance);
		return instance.mPenEvent.isPenEvent(event);
	}

	/**
	 * Test whether the pen button is pressed
	 * @param event A MotionEvent
	 * @return
	 */
	public static boolean isPenButtonPressed(MotionEvent event) {
//		Log.e(TAG, "HW<--isPenButtonPressed");
		Assert.assertNotNull(instance);
		return instance.mPenEvent.isPenButtonPressed(event);
	}

	/**
	 * To be called from the controlling view's onKeyDown handler
	 * @param keyCode
	 * @param event
	 * @return true if the event was handled and processing should be stopped
	 */
	public static boolean onKeyDown(int keyCode, KeyEvent event) {
		Assert.assertNotNull(instance);
		return instance.mPenEvent.onKeyDown(keyCode, event);
	}

	/**
	 * Add an invisible view to capture the pen button on the Thinkpad Tablet
	 * @param viewGroup
	 */
	public void addViewHack(ViewGroup viewGroup) {
		mPenEvent.addViewHack(viewGroup);
	}

	/**
	 * To be called from the controlling view's onKeyUp handler
	 * @param keyCode
	 * @param event
	 * @return true if the event was handled and processing should be stopped
	 */
	public static boolean onKeyUp(int keyCode, KeyEvent event) {
		Assert.assertNotNull(instance);
		return instance.mPenEvent.onKeyUp(keyCode, event);
	}

	/**
	 * Because ntx devices can't get metrics.xdpi, so use the each device to set the length first.
	 * @param cm  centimeter
	 * @param metrics DisplayMetrics
	 * @return pixel
	 */
	public static int getCMtoPixel(float cm, DisplayMetrics metrics){
		if (      SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E60Q60)){ return (int) (cm * 104);
		}else if (SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_E70Q30)){ return (int) (cm * 118);
		}else if (SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_EA0Q00)){ return (int) (cm * 90);
		}else if (SystemProperties.get("ro.product.hardwareType", "").equals(TYPE_ED0Q00)){ return (int) (cm * 60);
		}else{
			if (metrics != null){
				return Math.round( (cm/2.54f) * metrics.xdpi); // cm/2.54f = Inch
			}else{
				return (int) (cm * 90);
			}
		}	
	}

}
