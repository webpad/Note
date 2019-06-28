package ntx.note.export;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.AsyncTask;

import org.greenrobot.eventbus.EventBus;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import ntx.note.CallbackEvent;
import ntx.note.Global;
import ntx.note.data.Book;
import ntx.note.data.BookDirectory;
import ntx.note.data.Bookshelf;
import ntx.note.data.Storage;
import ntx.note2.R;

public class BackupMultipleAsyncTask extends AsyncTask<String, String, Integer> {

    private final static int BACKUP_MSG_CODE_BACKUP_INTERRUPT = -2;
    private final static int BACKUP_MSG_CODE_FILE_CHECK_OK = -1;
    private final static int BACKUP_MSG_CODE_SUCCESS = 0;
    private final static int BACKUP_MSG_CODE_PATH_NOT_EXIST = 1;
    private final static int BACKUP_MSG_CODE_FILE_CAN_NOT_CREATED = 2;
    private final static int BACKUP_MSG_CODE_CAN_NOT_WRITE_FILE = 3;

    private Context mContext;

    private InterruptibleTwoLevelProcessingDialogFragment mBackupingDialogFragment;

    private EventBus mEventBus;
    private ArrayList<UUID> mUUIDs;
    private String[] mProgressArray = new String[3];
    private boolean mEmailTask = false;
    private boolean mDeleteAfterBackup = false;

    public BackupMultipleAsyncTask(Context activityContext, ArrayList<UUID> uuids, boolean deleteAfterBackup) {
        this.mContext = activityContext;
        this.mUUIDs = new ArrayList<>();
        this.mUUIDs.addAll(uuids);
        this.mDeleteAfterBackup = deleteAfterBackup;
        mEventBus = EventBus.getDefault();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();
        mBackupingDialogFragment = InterruptibleTwoLevelProcessingDialogFragment.newInstance(
                mContext.getString(R.string.backup) + "...",
                R.drawable.writing_ic_convert_note,
                mUUIDs.size(),
                true);
        mBackupingDialogFragment.setOnInterruptButtonClickListener(
                new InterruptibleTwoLevelProcessingDialogFragment.OnInterruptButtonClickListener() {
                    @Override
                    public void onClick() {
                        BackupMultipleAsyncTask.this.cancel(true);

                        CallbackEvent callbackEvent = new CallbackEvent();
                        callbackEvent.setMessage(CallbackEvent.BACKUP_NOTE_INTERRUPT);
                        mEventBus.post(callbackEvent);

                        showAlertMessageDialogFragment(
                                mContext.getResources().getString(R.string.interrupt),
                                R.drawable.writing_ic_error);
                    }
                });

        ft.replace(R.id.alert_dialog_container, mBackupingDialogFragment,
                InterruptibleTwoLevelProcessingDialogFragment.class.getSimpleName()).commit();
    }

    @Override
    protected Integer doInBackground(String... arg0) {
        int retCode;
        String filePath = arg0[0];
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

        for (int i = 0; i < mUUIDs.size(); i++) {
            if (isCancelled()) {
                return BACKUP_MSG_CODE_BACKUP_INTERRUPT;
            }

            mProgressArray[2] = String.valueOf(i + 1);
            Book book = new Book(mUUIDs.get(i), true);
            String title = book.getTitle();
            mProgressArray[0] = title;
            File file = new File(filePath + "/" + title + ".note");
            retCode = exportBook(file, book);
            if (isCancelled()) {
                file.delete();
                return BACKUP_MSG_CODE_BACKUP_INTERRUPT;
            }
            if (BACKUP_MSG_CODE_SUCCESS != retCode)
                return retCode;

            if(mDeleteAfterBackup){
                Bookshelf.getInstance().deleteBook(mUUIDs.get(i));
            }
        }
        return BACKUP_MSG_CODE_SUCCESS;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        mBackupingDialogFragment.setProgressItemName(values[0]);
        mBackupingDialogFragment.updateLv1Progress(Integer.valueOf(values[1]));
        mBackupingDialogFragment.updateLv2Progress(Integer.valueOf(values[2]));
    }

    @Override
    protected void onPostExecute(final Integer result) {
        String msgStr;
        int alertIconResId;
        CallbackEvent callbackEvent = new CallbackEvent();
        switch (result) {
            case BACKUP_MSG_CODE_SUCCESS:
                msgStr = mContext.getResources().getString(R.string.successful);
                alertIconResId = R.drawable.writing_ic_successful;
                callbackEvent.setMessage(CallbackEvent.BACKUP_NOTE_SUCCESS);
                break;
            case BACKUP_MSG_CODE_PATH_NOT_EXIST:
                msgStr = mContext.getResources().getString(R.string.export_err_path_does_not_exist);
                alertIconResId = R.drawable.writing_ic_error;
                callbackEvent.setMessage(CallbackEvent.BACKUP_NOTE_ERROR);
                break;
            case BACKUP_MSG_CODE_FILE_CAN_NOT_CREATED:
                msgStr = mContext.getResources().getString(R.string.export_err_cannot_create_file);
                alertIconResId = R.drawable.writing_ic_error;
                callbackEvent.setMessage(CallbackEvent.BACKUP_NOTE_ERROR);
                break;
            case BACKUP_MSG_CODE_CAN_NOT_WRITE_FILE:
                msgStr = mContext.getResources().getString(R.string.export_err_cannot_write_file);
                alertIconResId = R.drawable.writing_ic_error;
                callbackEvent.setMessage(CallbackEvent.BACKUP_NOTE_ERROR);
                break;
            case BACKUP_MSG_CODE_BACKUP_INTERRUPT:
            default:
                return;
        }

        if (mEmailTask && result == BACKUP_MSG_CODE_SUCCESS) {
            callbackEvent.setMessage(CallbackEvent.BACKUP_NOTE_EMAIL);
            mBackupingDialogFragment.dismiss();
        } else {
            showAlertMessageDialogFragment(msgStr, alertIconResId);
        }
        mEventBus.post(callbackEvent);
    }

    private int exportBook(File file, Book book) {
        int retCode = checkFile(file);
        if (BACKUP_MSG_CODE_FILE_CHECK_OK != retCode)
            return retCode;

        if (file == null || !file.exists())
            return BACKUP_MSG_CODE_PATH_NOT_EXIST;

        int pageSize = book.pagesSize();

        try {
            TarOutputStream out = new TarOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

            File dir = new BookDirectory(Storage.getInstance(), book.getUUID());
            File[] filesToTar = dir.listFiles();
            int count;
            byte data[] = new byte[2048];
            for (int i = 0; i < filesToTar.length; i++) {
                File f = filesToTar[i];
                if (isCancelled()) {
                    out.flush();
                    out.close();
                    file.delete();
                    return BACKUP_MSG_CODE_BACKUP_INTERRUPT;
                }

                double temp = (float) i / (float) pageSize;
                mProgressArray[1] = String.valueOf((int) (Math.floor(temp * 100)));
                publishProgress(mProgressArray[0], mProgressArray[1], mProgressArray[2]);

                String name = dir.getName() + File.separator + f.getName();
                out.putNextEntry(new TarEntry(f, name));
                BufferedInputStream origin = new BufferedInputStream(new FileInputStream(f));

                while ((count = origin.read(data)) != -1) {
                    if (isCancelled()) {
                        out.flush();
                        out.close();
                        origin.close();
                        file.delete();
                        return BACKUP_MSG_CODE_BACKUP_INTERRUPT;
                    }
                    out.write(data, 0, count);
                }

                out.flush();
                origin.close();
            }
            out.close();
        } catch (IOException e) {
            return BACKUP_MSG_CODE_CAN_NOT_WRITE_FILE;
        }

        return BACKUP_MSG_CODE_SUCCESS;
    }

    private int checkFile(File file) {
        if (file == null)
            return BACKUP_MSG_CODE_FILE_CAN_NOT_CREATED;

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                return BACKUP_MSG_CODE_PATH_NOT_EXIST;
            }
        }

        try {
            file.createNewFile();
        } catch (IOException e) {
            return BACKUP_MSG_CODE_FILE_CAN_NOT_CREATED;
        }

        return BACKUP_MSG_CODE_FILE_CHECK_OK;
    }

    private void showAlertMessageDialogFragment(String msgStr, int alertIconResId) {
        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();
        AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(msgStr, alertIconResId, true, null);
        ft.replace(R.id.alert_dialog_container, alertDialogFragment, AlertDialogFragment.class.getSimpleName())
                .commit();
    }
}
