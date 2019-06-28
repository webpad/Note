package com.dropbox.android;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.util.IOUtil;
import com.dropbox.core.v2.files.UploadBuilder;
import com.dropbox.core.v2.files.UploadUploader;
import com.dropbox.core.v2.files.WriteMode;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.UUID;

import name.vbraun.view.write.Page;
import ntx.note.CallbackEvent;
import ntx.note.Global;
import ntx.note.artist.ArtistPDF;
import ntx.note.artist.PaperType;
import ntx.note.data.Book;
import ntx.note.data.Bookshelf;
import ntx.note.export.AlertDialogFragment;
import ntx.note.export.ConvertAsyncTask.ConvertRange;
import ntx.note.export.ConvertAsyncTask.ConvertType;
import ntx.note.export.InterruptibleProgressingDialogFragment;
import ntx.note.export.InterruptibleTwoLevelProcessingDialogFragment;
import ntx.note2.R;

public class UploadDropboxConvertFileTask extends AsyncTask<String, String, Integer> {
    private final static int RETURN_CODE_SUCCESS = 0;
    private final static int RETURN_CODE_EXPORT_TEMP_FILE_FAIL = 1;
    private final static int RETURN_CODE_UPLOAD_FAIL = 2;
    private final static int RETURN_CODE_INTERRUPTED = 3;

    private final static int CONVERT_MSG_CODE_CONVERT_INTERRUPT = -2;
    private final static int CONVERT_MSG_CODE_FILE_CHECK_OK = -1;
    private final static int CONVERT_MSG_CODE_SUCCESS = 0;
    private final static int CONVERT_MSG_CODE_PATH_NOT_EXIST = 1;
    private final static int CONVERT_MSG_CODE_FILE_CAN_NOT_CREATED = 2;
    private final static int CONVERT_MSG_CODE_CAN_NOT_WRITE_FILE = 3;

    private Context mContext;
    private UUID mBookUuid;
    private @ConvertType
    int mConvertType;
    private @ConvertRange
    int mOutputRange;
    private boolean mIncludeBackground;
    private Book mBook;
    private Page mPage;

    private InterruptibleTwoLevelProcessingDialogFragment mUploadingDialogFragment;
    private String[] mProgressArray = new String[3]; // {ProgressMessage, ProgressingItemName, Percentage, Progress}

    private EventBus mEventBus;
    private UploadUploader mDbxUploader;
    private File mTempFile;
    private int mRetCode;

    public UploadDropboxConvertFileTask(Context activityContext, UUID bookUuid, int convertType, int outputRang,
                                        boolean includeBackground) {
        this.mContext = activityContext;
        this.mBookUuid = bookUuid;
        this.mConvertType = convertType;
        this.mOutputRange = outputRang;
        this.mIncludeBackground = includeBackground;

        this.mEventBus = EventBus.getDefault();
    }

    @Override
    protected void onPreExecute() {

        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();

        // Set interruptable= false. Let user can not interrupt.
        mUploadingDialogFragment = InterruptibleTwoLevelProcessingDialogFragment.newInstance(
                mContext.getString(R.string.converting),
                R.drawable.ic_upload,
                1,
                true);

        mUploadingDialogFragment.setOnInterruptButtonClickListener(new InterruptibleTwoLevelProcessingDialogFragment.OnInterruptButtonClickListener() {
            @Override
            public void onClick() {
                UploadDropboxConvertFileTask.this.cancel(true);

                showAlertDialog(mContext.getResources().getString(R.string.interrupt), R.drawable.writing_ic_error);

                if (mDbxUploader != null) {
                    mDbxUploader.abort();
                    mDbxUploader = null;
                }

                CallbackEvent callbackEvent = new CallbackEvent();
                callbackEvent.setMessage(CallbackEvent.IMPORT_INTERRUPT);
                mEventBus.post(callbackEvent);
            }
        });

        ft.replace(R.id.alert_dialog_container, mUploadingDialogFragment,
                InterruptibleProgressingDialogFragment.class.getSimpleName()).commit();
    }

    @Override
    protected Integer doInBackground(String... params) {
        String tempFileName = params[0];
        mProgressArray[1] = tempFileName;
        makeSureTempDirectory();
        mTempFile = new File(Global.PACKAGE_DATA_DIR + Global.FILE_TEMP_DIR, tempFileName); // dataDir=/data/data/tempFileName
        mRetCode = checkFile(mTempFile);
        if (CONVERT_MSG_CODE_FILE_CHECK_OK != mRetCode)
            return mRetCode;

        if (!mTempFile.exists())
            return CONVERT_MSG_CODE_PATH_NOT_EXIST;


        mRetCode = convertBookPage();
        if (CONVERT_MSG_CODE_SUCCESS != mRetCode)
            return mRetCode;

        mRetCode = uploadFileToDropbox();

        if (mTempFile.exists())
            mTempFile.delete();

        return mRetCode;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        mUploadingDialogFragment.setProgressMessage(values[0]);
        mUploadingDialogFragment.setProgressItemName(values[1]);
        mUploadingDialogFragment.updateLv1Progress(Integer.valueOf(values[2]));
        mUploadingDialogFragment.updateLv2Progress(1);
    }

    @Override
    protected void onPostExecute(Integer result) {
        String msgStr;
        int alertIconResId;
        CallbackEvent callbackEvent = new CallbackEvent();

        switch (result) {
            case RETURN_CODE_SUCCESS:
                msgStr = mContext.getResources().getString(R.string.successful);
                alertIconResId = R.drawable.writing_ic_successful;
                callbackEvent.setMessage(CallbackEvent.UPLOAD_DROPBOX_FILES_SUCCESS);
                break;
            case RETURN_CODE_UPLOAD_FAIL:
                msgStr = mContext.getResources().getString(R.string.fail);
                alertIconResId = R.drawable.writing_ic_error;
                callbackEvent.setMessage(CallbackEvent.UPLOAD_DROPBOX_FILES_FAIL);
                break;
            case RETURN_CODE_INTERRUPTED:
            default:
                return;
        }

        showAlertDialog(msgStr, alertIconResId);
        mEventBus.post(callbackEvent);
    }

    @Override
    protected void onCancelled() {
        if (mTempFile.exists())
            mTempFile.delete();
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

    private int convertBookPage() {
        Book book = Bookshelf.getInstance().getCurrentBook();
        book.save();
        Page page = book.currentPage();

        mProgressArray[0] = mContext.getString(R.string.converting);

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
                outStream = new FileOutputStream(mTempFile);
            } catch (IOException e) {
                return CONVERT_MSG_CODE_CAN_NOT_WRITE_FILE;
            }

            Bitmap bitmap = page.renderBitmap(size_raster_width, size_raster_height, mIncludeBackground);
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, outStream);
            try {
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return CONVERT_MSG_CODE_CAN_NOT_WRITE_FILE;
            }

            if (isCancelled()) {
                mTempFile.delete();
                return CONVERT_MSG_CODE_CONVERT_INTERRUPT;
            }

            return CONVERT_MSG_CODE_SUCCESS;

        } else { // ConvertType.PDF
            PaperType paper = new PaperType(PaperType.PageSize.A4);

            ArtistPDF artist = new ArtistPDF(mTempFile);
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
                    mTempFile.delete();
                    return CONVERT_MSG_CODE_CONVERT_INTERRUPT;
                }

                double temp = (float) iter.nextIndex() / (float) pages.size();
                mProgressArray[2] = String.valueOf((int) Math.floor((temp / 2) * 100));
                publishProgress(mProgressArray[0], mProgressArray[1], mProgressArray[2]);

                artist.addPage(iter.next());
            }

            artist.destroy();

            return CONVERT_MSG_CODE_SUCCESS;
        }
    }

    private void makeSureTempDirectory() {
        if (!new File(Global.PACKAGE_DATA_DIR + Global.FILE_TEMP_DIR).exists()) {
            new File(Global.PACKAGE_DATA_DIR + Global.FILE_TEMP_DIR).mkdir();
        }
    }

    private int uploadFileToDropbox() {
        if (isCancelled())
            return RETURN_CODE_INTERRUPTED;

        if (!mTempFile.exists())
            return RETURN_CODE_UPLOAD_FAIL;

        String remoteFileName = mTempFile.getName();

        try (InputStream inputStream = new FileInputStream(mTempFile)) {
            UploadBuilder uploadBuilder = DropboxClientFactory.getClient().files().uploadBuilder("/" + remoteFileName);
            uploadBuilder.withMode(WriteMode.OVERWRITE);
            mDbxUploader = uploadBuilder.start();
            mDbxUploader.uploadAndFinish(inputStream, new IOUtil.ProgressListener() {
                @Override
                public void onProgress(long bytesWritten) {
                    if (isCancelled()) {
                        mRetCode = RETURN_CODE_INTERRUPTED;
                        mDbxUploader.abort();
                        return;
                    }
                    mProgressArray[0] = mContext.getString(R.string.uploading);
                    double temp = (float) bytesWritten / (float) mTempFile.length();
                    mProgressArray[2] = String.valueOf((int) (Math.floor((temp / 2) * 100)) + 50);
                    publishProgress(mProgressArray[0], mProgressArray[1], mProgressArray[2]);
                }
            });
        } catch (DbxException | IOException e) {
            mDbxUploader = null;
            return RETURN_CODE_UPLOAD_FAIL;
        } finally {
            mDbxUploader = null;
        }

        return mRetCode;
    }

    private void showAlertDialog(String msgStr, int iconId) {
        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();
        AlertDialogFragment alertDialogFragment;
        alertDialogFragment = AlertDialogFragment.newInstance(msgStr, iconId, true, null);
        ft.replace(R.id.alert_dialog_container, alertDialogFragment, AlertDialogFragment.class.getSimpleName()).commit();
    }
}