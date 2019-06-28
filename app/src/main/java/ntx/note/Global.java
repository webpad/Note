package ntx.note;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import name.vbraun.lib.pen.PenEventNTX;
import ntx.note.data.Book;
import ntx.note.data.Bookshelf;
import ntx.note2.BuildConfig;

/**
 * Global variables
 *
 * @author vbraun
 */
public class Global {

    /**
     * Static Final
     */
    // releaseModeOEM mode hides various goodies like root support
    // public static final boolean releaseModeOEM = true;
    public static final boolean releaseModeOEM = false;
    //ARTIS' GLOBAL CONSTANTS
    public static final boolean isDebug = false;        // enable this will dump debug logcats
    //	public static final boolean isDebug 	= false;
    public static final int BRUSH_THICKNESS_WEIGHT = 3;
    public static final float BRUSH_SCALE_WEIGHT = 1.0f;
    public static final float BRUSH_NORM_WEIGHT = 2.0f;
    public static final float PENCIL_THICKNESS_BASE = 2.0f;
    public static final int PENCIL_MIN_THICKNESS = 2;
    public static final int THICKNESS_VALUE_MIN = 1;
    public static final int THICKNESS_VALUE_MAX = 15;
    public static final int THICKNESS_VALUE_LV1 = 2;
    public static final int THICKNESS_VALUE_LV2 = 4;
    public static final int THICKNESS_VALUE_LV3 = 6;
    public static final int THICKNESS_VALUE_LV4 = 8;
    public static final int THICKNESS_VALUE_LV5 = 10;
    public static final int NDRAW_PEN_TYPE_PENCIL = 0;
    public static final int NDRAW_PEN_TYPE_FOUNTAINPEN = 1;
    public static final int NDRAW_PEN_TYPE_BRUSH = 2;
    public static final String MACHINE_PIXEL_WIDTH = "MACHINE_PIXEL_WIDTH";
    public static final String MACHINE_PIXEL_HEIGHT = "MACHINE_PIXEL_HEIGHT";
    public static final String MACHINE_PIXEL_RATE = "MACHINE_PIXEL_RATE";
    public static final String GLOBAL_VALUE = "GLOBAL_VALUE";
    public static final String STRING_KB = "KB";
    public static final String STRING_MB = "MB";
    public static final String STRING_GB = "GB";
    //swipe detector in note writer
    public static final boolean swipeNoteWriter = true; //enable swipe detector
    public static final float SWIPE_DISTANCE_THRESHOLD_BY_CM = 4.0f; // centimeter
    public static final String noteType = ".note"; // auto list ".note" files
    public static final String[] searchImageType = new String[]{".png", ".jpg", ".jpeg", ".bmp"}; //auto list files
    //    public static final String[] tempFileType = {".tmp", ".quill", ".json"}; //auto list ".note" files
    //	public static final String TEMP_DIR="/data/data/ntx.note2/files/temp"; // default path
    public static final String FILE_TEMP_DIR = "/files/temp"; // default path
    private static final String[] DIRECTORY_EXTERNALSD_TRY = {"/mnt/external_sd", "/mnt/sdcard2", "/mnt/sdcard/external_sd", "/mnt/extSdCard", "/mnt/extsd", "/mnt/media_rw/extsd"};
    private static final String[] DIRECTORY_USBDRIVE_TRY = {"/mnt/usbdrive", "/mnt/usb", "/mnt/usb0", "/mnt/sdcard/usbStorage", "/mnt/UsbDriveA", "/mnt/UsbDriveB"};
    public static final String DIRECTORY_USBDRIVE = tryDirectories(DIRECTORY_USBDRIVE_TRY);
    // ===== email backup =====
    public static final String MAIL_FILE_TEMP_DIR = "/mail_temp"; // for temp file directory to send email
    public static final int tempFileKeepDays = 7; // default 7 days to clear temp file
    // ==== Grey Colors definition ========
    public static final int black = 0x000000;
    public static final int white = 0xFFFFFF;
    public static final int grey_0 = 0xFF000000;
    public static final int grey_1 = 0xFF111111;
    public static final int grey_2 = 0xFF222222;
    public static final int grey_3 = 0xFF333333;
    public static final int grey_4 = 0xFF444444;
    public static final int grey_5 = 0xFF555555;
    public static final int grey_6 = 0xFF666666;
    public static final int grey_7 = 0xFF777777;
    public static final int grey_8 = 0xFF888888;
    public static final int grey_9 = 0xFF999999;
    public static final int grey_A = 0xFFAAAAAA;
    public static final int grey_B = 0xFFBBBBBB;
    public static final int grey_C = 0xFFCCCCCC;
    public static final int grey_D = 0xFFDDDDDD;
    public static final int grey_E = 0xFFEEEEEE;
    public static final int grey_F = 0xFFFFFFFF;
    // ===== Dropbox =====
    public static final String TEMP_FILE_NAME = "temp.note"; // dropbox download temp file name.
    public static final String NOTE_DIR = "nNote";
    public static final String BOOK_DIR = "Books";
    public static final String APP_KEY = "f1irdmrgxiwdrts";
    //	public static final String APP_SECRET = "67ebronbt4qwz0x";
    public static final String ACCESS_KEY = "dropbox-sample";
    public static final String ACCESS_TOKEN = "access-token";
    public static final String ACCESS_USER_ID = "user-id";
    public static final String ACCESS_USER_Email = "user-email";
    public static final String ACCESS_USER_USED_FREE_SPACE = "user-used_free_space";
    // You don't need to change these, leave them alone.
//	public static final String ACCOUNT_PREFS_NAME = "prefs";
//	public static final String ACCESS_KEY_NAME = "ACCESS_KEY";
//	public static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    public static final String NTOOL_PACKAGE = "ntx.tools";
    public static final String NTOOL_CLASS = "ntx.tools.MainActivity";
    public static final String READER_PACKAGE = "ntx.reader3";
    public static final String READER_CLASS = "org.geometerplus.android.fbreader.library.LibraryActivity";
    public static final String READER_OPENBOOK_CLASS = "org.geometerplus.android.fbreader.library.OpenBookActivity";
    public static final String READER_MAIN_PAGE_CLASS = "org.geometerplus.android.fbreader.library.NtxMainPageActivity";
    public static final String CALENDAR_PACKAGE = "com.simplemobiletools.calendar";
    public static final String CALENDAR_CLASS = "com.simplemobiletools.calendar.activities.MainActivity";
    public static final String SAVE_RECENT_BOOK_FINISHED_NOTIFICATION = "org.geometerplus.android.fbreader.library.SaveRecentBookListService";
    public static final String KEY_ACTIVITY = "activity";
    public static final String KEY_READER_MAIN_PAGE = "reader_main_page";
    public static final String KEY_ALL_BOOKS = "allbooks";
    public static final String KEY_READER_BOOK_ID = "reader_book_id";
    public static final String PAGE_PREVIEW_BITMAP_FILE_NAME_PREFIX = "fastShow_";
    public static final String PAGE_PREVIEW_BITMAP_FILE_TYPE = ".png";
    public static final long DEFAULT_DROP_TIME = 0L;
    public static final int COMPRESS_WIDTH_HEIGHT = 500;
    public static final int COMPRESS_QUALITY = 50;

    /**
     * Static
     */
    // ===== Dropbox =====
    private static String tryDirectories(String[] dirs) {
        for (String name : dirs) {
            File dir = new File(name);
            if (dir.exists())
                return name;
        }
        return dirs[0];
    }

    public static float MACHINE_PIXEL_RATE_VALUE = 0;
    public static int SWIPE_DISTANCE_THRESHOLD = 450; // default swipe detector threshold
    public static int TELEPORT_DISTANCE_THRESHOLD = 200;

    public boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    public static String PACKAGE_DATA_DIR = "/data/data/ntx.note2"; // default path= /data/data/ntx.note2 Set in NtxLauncherActivity.java_onCreate()
    public static String APP_DATA_PACKAGE_FILES_PATH = "/data/data/ntx.note2/files/";
    public static String DATA_START = "notebook_";
    public static String PATH_SDCARD = Environment.getExternalStorageDirectory().getPath();
    public static String[] SEARCHPATH = {"Download", "nNote", "Pictures"};
    // public static String PATH_EXTERNALSD = Environment.getExternalExtSDStorageDirectory().getPath();
    public static String PATH_EXTERNALSD = "/mnt/media_rw/extsd";
    //    public static String PATH_EXTERNALSD = tryDirectories(DIRECTORY_EXTERNALSD_TRY);
    public static String DIRECTORY_SDCARD_NOTE = PATH_SDCARD + File.separator + Global.NOTE_DIR;
    public static String DIRECTORY_EXTERNALSD_NOTE = PATH_EXTERNALSD + File.separator + Global.NOTE_DIR;
    public static String DIRECTORY_SDCARD_BOOK = PATH_SDCARD + File.separator + Global.BOOK_DIR;
    public static String DIRECTORY_EXTERNALSD_BOOK = PATH_EXTERNALSD + File.separator + Global.BOOK_DIR;
    public static String DIRECTORY_SDCARD_SLEEP = PATH_SDCARD + File.separator + "sleep";
    public static String DIRECTORY_SDCARD_POWEROFF = PATH_SDCARD + File.separator + "poweroff";

    public static void closeWaitDialog(final Context c) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent();
                i.setComponent(new ComponentName("ntx.tools", "ntx.tools.OverlayService"));
                c.stopService(i);
            }
        }, 1000);
    }

    public static void closeWaitDialog(final Context c, int delay) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent();
                i.setComponent(new ComponentName("ntx.tools", "ntx.tools.OverlayService"));
                c.stopService(i);
            }
        }, delay);
    }

    public static void openWaitDialog(final Context c) {
        Intent i = new Intent();
        i.setComponent(new ComponentName("ntx.tools", "ntx.tools.OverlayService"));
        c.startService(i);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static boolean HAS_GREY_COLOR = false;

    public static void checkNeedRefresh(int color) {
        if (Global.HAS_GREY_COLOR)
            return;
        if (color != Color.BLACK && color != Color.WHITE) {
            HAS_GREY_COLOR = true;
        } else {
            HAS_GREY_COLOR = false;
        }
    }

    public static void refresh(final Activity activity) {
        if (HAS_GREY_COLOR && activity.getComponentName().toString().contains(NoteWriterActivity.class.getSimpleName())) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent ac = new Intent("ntx.eink_control.QUICK_REFRESH");
                    ac.putExtra("updatemode", PenEventNTX.UPDATE_MODE_SCREEN);
                    ac.putExtra("commandFromNtxApp", true);
                    activity.sendBroadcast(ac);
                }
            }, 700);
        }
    }

    public static String generateNoteName() {
        String noteName = "Note 1";

        List<String> bookTitleList = new ArrayList<>();
        for (Book book : Bookshelf.getInstance().getBookList()) {
            bookTitleList.add(book.getTitle());
        }

        int index = 1;
        while (bookTitleList.contains(noteName)) {
            index++;
            noteName = "Note " + index;
        }

        return noteName;
    }

    /**
     * set storage path
     */
//	public static void getExternalMounts() {
//		final HashSet<String> out = new HashSet<String>();
//		ArrayList<String> path = new ArrayList<String>();
//
//		String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
//		String s = "";
//		try {
//			final Process process = new ProcessBuilder().command("mount")
//					.redirectErrorStream(true).start();
//			process.waitFor();
//			final InputStream is = process.getInputStream();
//			final byte[] buffer = new byte[1024];
//			while (is.read(buffer) != -1) {
//				s = s + new String(buffer);
//			}
//			is.close();
//		} catch (final Exception e) {
//			e.printStackTrace();
//		}
//
//		// parse output
//		final String[] lines = s.split("\n");
//		for (String line : lines) {
//			if (!line.toLowerCase(Locale.US).contains("asec")) {
//				if (line.matches(reg)) {
//					String[] parts = line.split(" ");
//					for (String part : parts) {
//						if (part.startsWith("/"))
//							if (!part.toLowerCase(Locale.US).contains("vold"))
//								out.add(part);
//
//					}
//				}
//			}
//		}
//
//		for (String p : out){
//			path.add(p);
//		}
//
//		for (int i = 0; i<path.size(); i++){
//			if (i==0) 		Global.PATH_SDCARD = path.get(0);
//			else if (i==1) 	Global.PATH_EXTERNALSD = path.get(1);
//		}
//
//		DIRECTORY_SDCARD_NOTE = PATH_SDCARD + File.separator + Global.NOTE_DIR;
//		DIRECTORY_EXTERNALSD_NOTE = PATH_EXTERNALSD + File.separator + Global.NOTE_DIR;
//
//		DIRECTORY_SDCARD_SLEEP = PATH_SDCARD + File.separator + "sleep";
//		DIRECTORY_SDCARD_POWEROFF = PATH_SDCARD + File.separator + "poweroff";
//	}
}
