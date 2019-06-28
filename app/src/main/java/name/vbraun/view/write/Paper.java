package name.vbraun.view.write;

import junit.framework.Assert;

import ntx.note2.R;

import android.content.Context;
import android.content.res.Resources;

public class Paper {
	private static final String TAG = "PaperType";

	//add new paper type in the end
    public enum Type {
        EMPTY, RULED, QUAD, HEX, COLLEGERULED, NARROWRULED,
        CORNELLNOTES, DAYPLANNER, MUSIC, CALLIGRAPHY_SMALL, CALLIGRAPHY_BIG,
        TODOLIST, MINUTES, STAVE, DIARY,CUSTOMIZED,
    }
    
    public final static String EMPTY 				= "EMPTY";
    public final static String RULED 				= "RULED";
    public final static String COLLEGERULED 		= "COLLEGERULED";
    public final static String NARROWRULED 		= "NARROWRULED";
    public final static String QUADPAPER 			= "QUAD";
    public final static String CORNELLNOTES 		= "CORNELLNOTES";
	public final static String DAYPLANNER 			= "DAYPLANNER";
	public final static String MUSIC 				= "MUSIC";
	public final static String CALLIGRAPHY_SMALL	= "CALLIGRAPHY_SMALL";
	public final static String CALLIGRAPHY_BIG 	= "CALLIGRAPHY_BIG";
	public final static String TODOLIST 				= "TODOLIST";
	public final static String MINUTES 				= "MINUTES";
	public final static String STAVE 				= "STAVE";
	public final static String DIARY 				= "DIARY";
	public final static String CUSTOMIZED               = "CUSTOMIZED";
	

	private CharSequence resourceName;
	private Type type;

	public static final Paper[] Table = {
		new Paper(EMPTY, Type.EMPTY),
		new Paper(RULED, Type.RULED),
		new Paper(COLLEGERULED, Type.COLLEGERULED),
		new Paper(NARROWRULED, Type.NARROWRULED),
		new Paper(TODOLIST, Type.TODOLIST),
		new Paper(MINUTES, Type.MINUTES),
		new Paper(DIARY, Type.DIARY),
		new Paper(QUADPAPER, Type.QUAD),
		new Paper(CORNELLNOTES, Type.CORNELLNOTES),
		new Paper(DAYPLANNER, Type.DAYPLANNER),
		new Paper(MUSIC, Type.MUSIC),
		new Paper(CALLIGRAPHY_SMALL, Type.CALLIGRAPHY_SMALL),
		new Paper(CALLIGRAPHY_BIG, Type.CALLIGRAPHY_BIG),
		new Paper(STAVE, Type.STAVE),
		new Paper(CUSTOMIZED, Type.CUSTOMIZED),
	};


	public Paper(String resourceName, Type t) {
		this.resourceName = resourceName;
		type = t;
	}

	public CharSequence getName(Context context) {
		Resources res = context.getResources();
		String[] values  = res.getStringArray(R.array.dlg_background_values);
		String[] entries = res.getStringArray(R.array.dlg_background_entries);
		Assert.assertTrue(values.length == entries.length);
		for (int i=0; i<entries.length; i++)
			if (resourceName.equals(values[i]))
				return entries[i];
		return null;
	}

	public Type getType() {
		return type;
	}


}
