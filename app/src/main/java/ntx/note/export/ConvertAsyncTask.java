package ntx.note.export;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.support.annotation.IntDef;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.UUID;

import name.vbraun.view.write.Page;
import ntx.note.CallbackEvent;
import ntx.note.Global;
import ntx.note.NoteWriterActivity;
import ntx.note.artist.ArtistPDF;
import ntx.note.artist.PaperType;
import ntx.note.data.Book;
import ntx.note.data.Bookshelf;
import ntx.note2.R;

public class ConvertAsyncTask extends AsyncTask<String, Integer, Integer> {

    @IntDef({ConvertType.PDF, ConvertType.PNG})
    public @interface ConvertType {
        int PDF = 0;
        int PNG = 1;
    }

    @IntDef({ConvertRange.CURRENT_PAGE, ConvertRange.ALL_PAGES})
    public @interface ConvertRange {
        int CURRENT_PAGE = 0;
        int ALL_PAGES = 1;
        // int TAGGED_PAGES = 2;
    }

    private final static int CONVERT_MSG_CODE_CONVERT_INTERRUPT = -2;
    private final static int CONVERT_MSG_CODE_FILE_CHECK_OK = -1;
    private final static int CONVERT_MSG_CODE_SUCCESS = 0;
    private final static int CONVERT_MSG_CODE_PATH_NOT_EXIST = 1;
    private final static int CONVERT_MSG_CODE_FILE_CAN_NOT_CREATED = 2;
    private final static int CONVERT_MSG_CODE_CAN_NOT_WRITE_FILE = 3;

    private int mConvertType;
    private int mOutputRange;
    private boolean mIncludeBackground;
    private boolean mEmailTask = false;

    private Context mContext;
    private UUID mBookUuid;
    private int mProgressMaxValue;

    private InterruptibleProgressingDialogFragment mConvertingDialogFragment;

    private EventBus mEventBus;
    private String mPath;
    private static final String CONVERT_SUCCESS_DIALOG_TAG = "convert_success_dialog";

    public ConvertAsyncTask(Context activityContext, UUID bookUuid, int convertType, int outputRang,
                            boolean includeBackground) {
        this.mContext = activityContext;
        this.mBookUuid = bookUuid;
        this.mConvertType = convertType;
        this.mOutputRange = outputRang;
        this.mIncludeBackground = includeBackground;

        Book book = new Book(mBookUuid, false);
        int pageSize = book.pagesSizeFromIndexFile();

        switch (mOutputRange) {
            case ConvertRange.ALL_PAGES:
                mProgressMaxValue = pageSize;
                break;
            case ConvertRange.CURRENT_PAGE:
            default:
                mProgressMaxValue = 1;
                break;
        }

        mEventBus = EventBus.getDefault();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();

        // Set interruptable= false. Let user can not interrupt.
        mConvertingDialogFragment = InterruptibleProgressingDialogFragment
                .newInstance(mContext.getString(R.string.converting), mProgressMaxValue, true);

        mConvertingDialogFragment.setOnInterruptButtonClickListener(
                new InterruptibleProgressingDialogFragment.OnInterruptButtonClickListener() {
                    @Override
                    public void onClick() {
                        ConvertAsyncTask.this.cancel(true);

                        CallbackEvent callbackEvent = new CallbackEvent();
                        callbackEvent.setMessage(CallbackEvent.CONVERT_NOTE_INTERRUPT);
                        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();
                        AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(
                                mContext.getResources().getString(R.string.interrupt),
                                R.drawable.writing_ic_error, true, null);

                        ft.replace(R.id.alert_dialog_container, alertDialogFragment, AlertDialogFragment.class.getSimpleName())
                                .commit();

                        mEventBus.post(callbackEvent);
                    }
                });

        ft.replace(R.id.alert_dialog_container, mConvertingDialogFragment,
                InterruptibleProgressingDialogFragment.class.getSimpleName()).commit();
    }

    @Override
    protected Integer doInBackground(String... arg0) {
        File file;
        String filePath = arg0[0];
        mPath = filePath;
        if (filePath.startsWith(Global.PATH_SDCARD + Global.MAIL_FILE_TEMP_DIR)) {
            mEmailTask = true;
            File mailTempDir = new File(Global.PATH_SDCARD + Global.MAIL_FILE_TEMP_DIR);
            if (!mailTempDir.exists())
                mailTempDir.mkdir();

            String[] children = mailTempDir.list();
            if (children != null) {
                for (String child : children) {
                    new File(mailTempDir, child).delete();
                }
            }
        }
        file = new File(filePath);
        int retCode = checkFile(file);
        if (CONVERT_MSG_CODE_FILE_CHECK_OK != retCode)
            return retCode;

        if (!file.exists())
            return CONVERT_MSG_CODE_PATH_NOT_EXIST;

        Book book = Bookshelf.getInstance().getCurrentBook();
        book.save();
        Page page = book.currentPage();

        if (ConvertType.PNG == mConvertType) {
            FileOutputStream outStream;
            int size_raster_width;
            int size_raster_height;
            int dim_big = 1440, dim_small = 1080;
            if (page.getAspectRatio() > 1) {
                size_raster_width = dim_big;
                size_raster_height = dim_small;
            } else {
                size_raster_width = dim_small;
                size_raster_height = dim_big;
            }
            try {
                outStream = new FileOutputStream(file);
            } catch (IOException e) {
                return CONVERT_MSG_CODE_CAN_NOT_WRITE_FILE;
            }

            Bitmap bitmap = page.renderBitmap(size_raster_width, size_raster_height, mIncludeBackground);
            bitmap.compress(CompressFormat.PNG, 0, outStream);
            try {
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return CONVERT_MSG_CODE_CAN_NOT_WRITE_FILE;
            }

            if (isCancelled()) {
                file.delete();
                return CONVERT_MSG_CODE_CONVERT_INTERRUPT;
            }
            return CONVERT_MSG_CODE_SUCCESS;

        } else { // ConvertType.PDF
            PaperType paper = new PaperType(PaperType.PageSize.A4);

            ArtistPDF artist = new ArtistPDF(file);
            artist.setPaper(paper);
            artist.setBackgroundVisible(mIncludeBackground);

            LinkedList<Page> pages = new LinkedList<>();

            switch (mOutputRange) {
                case ConvertRange.CURRENT_PAGE:
                    pages.add(page);
                    break;
                case ConvertRange.ALL_PAGES:
                    pages.addAll(book.getPages());
                    break;
                // case PageRange.TAGGED_PAGES:
                // pages.add(mBook.getFilteredPages());
                // break;
            }

            ListIterator<Page> iter = pages.listIterator();
            while (iter.hasNext()) {
                if (isCancelled()) {
                    file.delete();
                    return CONVERT_MSG_CODE_CONVERT_INTERRUPT;
                }
                publishProgress(iter.nextIndex());
                artist.addPage(iter.next());
            }

            artist.destroy();

            return CONVERT_MSG_CODE_SUCCESS;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        mConvertingDialogFragment.updateProgress(values[0]);
    }

    @Override
    protected void onPostExecute(final Integer result) {
        String msgStr = "";
        int alertIconResId = R.drawable.writing_ic_successful;
        CallbackEvent callbackEvent = new CallbackEvent();
        switch (result) {
            case CONVERT_MSG_CODE_SUCCESS:
                msgStr = mContext.getResources().getString(R.string.successful);
                alertIconResId = R.drawable.writing_ic_successful;
                callbackEvent.setMessage(CallbackEvent.CONVERT_NOTE_SUCCESS);
                break;
            case CONVERT_MSG_CODE_PATH_NOT_EXIST:
                msgStr = mContext.getResources().getString(R.string.export_err_path_does_not_exist);
                alertIconResId = R.drawable.writing_ic_error;
                callbackEvent.setMessage(CallbackEvent.CONVERT_NOTE_ERROR);
                break;
            case CONVERT_MSG_CODE_FILE_CAN_NOT_CREATED:
                msgStr = mContext.getResources().getString(R.string.export_err_cannot_create_file);
                alertIconResId = R.drawable.writing_ic_error;
                callbackEvent.setMessage(CallbackEvent.CONVERT_NOTE_ERROR);
                break;
            case CONVERT_MSG_CODE_CAN_NOT_WRITE_FILE:
                msgStr = mContext.getResources().getString(R.string.export_err_cannot_write_file);
                alertIconResId = R.drawable.writing_ic_error;
                callbackEvent.setMessage(CallbackEvent.CONVERT_NOTE_ERROR);
                break;
            case CONVERT_MSG_CODE_CONVERT_INTERRUPT:
            default:
                break;
        }

        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();
        AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(msgStr, alertIconResId, true, null);

        if (result == CONVERT_MSG_CODE_SUCCESS) {
            alertDialogFragment.setupNegativeButton(mContext.getResources().getString(R.string.dialog_open_folder));
            alertDialogFragment.registerAlertDialogButtonClickListener((AlertDialogButtonClickListener) mContext,
                    CONVERT_SUCCESS_DIALOG_TAG);
            if (mContext instanceof NoteWriterActivity) {
                ((NoteWriterActivity) mContext).setupPathForOpenFolder(mPath);
            }
        }

        if (mEmailTask && result == CONVERT_MSG_CODE_SUCCESS) {
            callbackEvent.setMessage(CallbackEvent.CONVERT_NOTE_EMAIL);
            mConvertingDialogFragment.dismiss();
        } else {
            ft.replace(R.id.alert_dialog_container, alertDialogFragment, AlertDialogFragment.class.getSimpleName())
                    .commit();
        }

        mEventBus.post(callbackEvent);
    }

    private int checkFile(File file) {
        if (file == null)
            return CONVERT_MSG_CODE_FILE_CAN_NOT_CREATED;

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                return CONVERT_MSG_CODE_PATH_NOT_EXIST;
            }
        }

        try {
            file.createNewFile();
        } catch (IOException e) {
            return CONVERT_MSG_CODE_FILE_CAN_NOT_CREATED;
        }

        return CONVERT_MSG_CODE_FILE_CHECK_OK;
    }
}
