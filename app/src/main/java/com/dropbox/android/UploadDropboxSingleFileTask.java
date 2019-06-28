package com.dropbox.android;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.util.IOUtil;
import com.dropbox.core.v2.files.UploadBuilder;
import com.dropbox.core.v2.files.UploadUploader;
import com.dropbox.core.v2.files.WriteMode;

import org.greenrobot.eventbus.EventBus;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import ntx.note.CallbackEvent;
import ntx.note.Global;
import ntx.note.data.Book;
import ntx.note.data.BookDirectory;
import ntx.note.data.Bookshelf;
import ntx.note.data.Storage;
import ntx.note.export.AlertDialogFragment;
import ntx.note.export.InterruptibleTwoLevelProcessingDialogFragment;
import ntx.note2.R;

public class UploadDropboxSingleFileTask extends AsyncTask<String, String, Integer> {
    private final static int RETURN_CODE_SUCCESS = 0;
    private final static int RETURN_CODE_EXPORT_TEMP_FILE_FAIL = 1;
    private final static int RETURN_CODE_UPLOAD_FAIL = 2;
    private final static int RETURN_CODE_INTERRUPTED = 3;

    private Context mContext;
    private UUID mBookUuid;
    private boolean mDeleteAfterBackup = false;
    private int mRetCode = RETURN_CODE_SUCCESS;

    private InterruptibleTwoLevelProcessingDialogFragment mUploadingDialogFragment;
    private String[] mProgressArray = new String[3]; // {ProgressMessage, ProgressingItemName, Percentage, Progress}

    private EventBus mEventBus;
    private UploadUploader mDbxUploader;
    private File mTempFile;

    public UploadDropboxSingleFileTask(Context activityContext, UUID bookUuid, boolean deleteAfterBackup) {
        this.mContext = activityContext;
        this.mBookUuid = bookUuid;
        this.mDeleteAfterBackup = deleteAfterBackup;
        this.mEventBus = EventBus.getDefault();
    }

    @Override
    protected void onPreExecute() {
        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();

        mUploadingDialogFragment = InterruptibleTwoLevelProcessingDialogFragment.newInstance(
                mContext.getString(R.string.uploading),
                R.drawable.ic_upload,
                1,
                true);

        mUploadingDialogFragment.setOnInterruptButtonClickListener(new InterruptibleTwoLevelProcessingDialogFragment.OnInterruptButtonClickListener() {
            @Override
            public void onClick() {
                UploadDropboxSingleFileTask.this.cancel(true);

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
                InterruptibleTwoLevelProcessingDialogFragment.class.getSimpleName()).commit();
    }

    @Override
    protected Integer doInBackground(String... params) {
        if (this.isCancelled()) {
            mRetCode = RETURN_CODE_INTERRUPTED;
            return mRetCode;
        }

        mRetCode = RETURN_CODE_SUCCESS;

        if (isCancelled()) {
            mRetCode = RETURN_CODE_INTERRUPTED;
            return mRetCode;
        }

        makeSureTempDirectory();
        String tempFileName = params[0];
        mProgressArray[1] = tempFileName;
        mTempFile = new File(Global.PACKAGE_DATA_DIR + Global.FILE_TEMP_DIR, tempFileName); // dataDir=/data/data/ntx.note2

        exportBookToFile(mBookUuid, mTempFile);

        uploadFileToDropbox(mTempFile);

        if (mTempFile.exists())
            mTempFile.delete();

        if(mDeleteAfterBackup && RETURN_CODE_SUCCESS == mRetCode)
            Bookshelf.getInstance().deleteBook(mBookUuid);

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

    private void makeSureTempDirectory() {
        if (!new File(Global.PACKAGE_DATA_DIR + Global.FILE_TEMP_DIR).exists()) {
            new File(Global.PACKAGE_DATA_DIR + Global.FILE_TEMP_DIR).mkdir();
        }
    }

    private void exportBookToFile(UUID bookUuid, File file) {
        Book book = new Book(bookUuid, true);
        int pageSize = book.pagesSize();

        try {
            TarOutputStream out = new TarOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

            File dir = new BookDirectory(Storage.getInstance(), bookUuid);
            File[] filesToTar = dir.listFiles();
            int count;
            byte data[] = new byte[2048];
            for (int i = 0; i < filesToTar.length; i++) {
                File f = filesToTar[i];
                if (isCancelled()) {
                    out.close();
                    file.delete();
                    mRetCode = RETURN_CODE_INTERRUPTED;
                    return;
                }

                mProgressArray[0] = mContext.getResources().getString(R.string.backuping);
                double temp = (float) i / (float) pageSize;
                int uploadPercentage = (int) (Math.floor((temp / 2) * 100));
                // Let the upload percentage keep 99% until the task finished.
                if (uploadPercentage == 100)
                    uploadPercentage = 99;
                mProgressArray[2] = String.valueOf(uploadPercentage);
                publishProgress(mProgressArray[0], mProgressArray[1], mProgressArray[2]);

                String name = dir.getName() + File.separator + f.getName();
                out.putNextEntry(new TarEntry(f, name));
                BufferedInputStream origin = new BufferedInputStream(new FileInputStream(f));

                while ((count = origin.read(data)) != -1) {
                    if (isCancelled()) {
                        origin.close();
                        out.close();
                        file.delete();
                        mRetCode = RETURN_CODE_EXPORT_TEMP_FILE_FAIL;
                        return;
                    }
                    out.write(data, 0, count);
                }

                out.flush();
                origin.close();
            }
            out.close();
        } catch (IOException e) {
            mRetCode = RETURN_CODE_EXPORT_TEMP_FILE_FAIL;
        }
    }

    private void uploadFileToDropbox(final File file) {
        if (isCancelled()) {
            mRetCode = RETURN_CODE_INTERRUPTED;
            return;
        }

        if (!file.exists()) {
            mRetCode = RETURN_CODE_UPLOAD_FAIL;
            return;
        }

        String remoteFileName = file.getName();

        try (InputStream inputStream = new FileInputStream(file)) {
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
                    double temp = (float) bytesWritten / (float) file.length();
                    mProgressArray[2] = String.valueOf((int) (Math.floor((temp / 2) * 100)) + 50);
                    publishProgress(mProgressArray[0], mProgressArray[1], mProgressArray[2]);
                }
            });
        } catch (DbxException | IOException e) {
            mRetCode = RETURN_CODE_UPLOAD_FAIL;
            mDbxUploader = null;
        } finally {
            mDbxUploader = null;
        }
    }

    private void showAlertDialog(String msgStr, int iconId) {
        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();
        AlertDialogFragment alertDialogFragment;
        alertDialogFragment = AlertDialogFragment.newInstance(msgStr, iconId, true, null);
        ft.replace(R.id.alert_dialog_container, alertDialogFragment, AlertDialogFragment.class.getSimpleName()).commit();
    }
}