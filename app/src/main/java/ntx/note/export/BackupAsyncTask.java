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
import java.util.UUID;

import ntx.note.CallbackEvent;
import ntx.note.Global;
import ntx.note.NoteWriterActivity;
import ntx.note.data.BookDirectory;
import ntx.note.data.Bookshelf;
import ntx.note.data.Storage;
import ntx.note2.R;

public class BackupAsyncTask extends AsyncTask<String, Integer, Integer> {
    private final static String TAG = "BackupAsyncTask";

    private final static int BACKUP_MSG_CODE_BACKUP_INTERRUPT = -2;
    private final static int BACKUP_MSG_CODE_FILE_CHECK_OK = -1;
    private final static int BACKUP_MSG_CODE_SUCCESS = 0;
    private final static int BACKUP_MSG_CODE_PATH_NOT_EXIST = 1;
    private final static int BACKUP_MSG_CODE_FILE_CAN_NOT_CREATED = 2;
    private final static int BACKUP_MSG_CODE_CAN_NOT_WRITE_FILE = 3;

    private Context mContext;
    private UUID mBookUuid;
    private boolean mDoSaveCurrent;
    private boolean mDeleteAfterBackup;

    private InterruptibleTwoLevelProcessingDialogFragment mBackupingDialogFragment;
    private String mFilePath;
    private String mFileName;

    private EventBus mEventBus;
    private boolean mEmailTask;

    public BackupAsyncTask(Context activityContext, UUID bookUuid, boolean doSaveCurrent, boolean deleteAfterBackup) {
        this.mContext = activityContext;
        this.mBookUuid = bookUuid;
        this.mDoSaveCurrent = doSaveCurrent;
        this.mDeleteAfterBackup = deleteAfterBackup;
        mEventBus = EventBus.getDefault();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();

        // Set interruptible= false. Let user can not interrupt.
        mBackupingDialogFragment = InterruptibleTwoLevelProcessingDialogFragment
                .newInstance(mContext.getString(R.string.backuping),
                        R.drawable.writing_ic_convert_note,
                        1,
                        true);

        mBackupingDialogFragment.setOnInterruptButtonClickListener(
                new InterruptibleTwoLevelProcessingDialogFragment.OnInterruptButtonClickListener() {
                    @Override
                    public void onClick() {
                        BackupAsyncTask.this.cancel(true);

                        CallbackEvent callbackEvent = new CallbackEvent();
                        callbackEvent.setMessage(CallbackEvent.BACKUP_NOTE_INTERRUPT);
                        mEventBus.post(callbackEvent);

                        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();
                        AlertDialogFragment alertDialogFragment;
                        alertDialogFragment = AlertDialogFragment.newInstance(
                                mContext.getResources().getString(R.string.interrupt),
                                R.drawable.writing_ic_error,
                                true,
                                null);

                        ft.replace(R.id.alert_dialog_container, alertDialogFragment, AlertDialogFragment.class.getSimpleName()).commit();
                    }
                });

        ft.replace(R.id.alert_dialog_container, mBackupingDialogFragment,
                InterruptibleTwoLevelProcessingDialogFragment.class.getSimpleName()).commit();
    }

    @Override
    protected Integer doInBackground(String... arg0) {
        File file = new File(arg0[0]);
        mFilePath = arg0[0];
        if (mFilePath.startsWith(Global.PATH_SDCARD + Global.MAIL_FILE_TEMP_DIR)) {
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
        mFileName = file.getName();
        int retCode = checkFile(file);
        if (BACKUP_MSG_CODE_FILE_CHECK_OK != retCode)
            return retCode;

        if (!file.exists())
            return BACKUP_MSG_CODE_PATH_NOT_EXIST;

        if (mDoSaveCurrent) {
            Bookshelf.getInstance().getCurrentBook().save();
            if (mContext.getClass().getSimpleName().equals(NoteWriterActivity.class.getSimpleName()))
                NoteWriterActivity.setNoteEdited(false);
        }

        try {
            TarOutputStream out = new TarOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

            File dir = new BookDirectory(Storage.getInstance(), mBookUuid);
            File[] filesToTar = dir.listFiles();
            int count;
            byte data[] = new byte[2048];
            for (int i = 0; i < filesToTar.length; i++) {
                File f = filesToTar[i];
                if (isCancelled()) {
                    out.close();
                    file.delete();
                    return BACKUP_MSG_CODE_BACKUP_INTERRUPT;
                }

                double temp = (float) i / (float) filesToTar.length;
                publishProgress((int) (Math.floor(temp * 100)));

                String name = dir.getName() + File.separator + f.getName();
                out.putNextEntry(new TarEntry(f, name));
                BufferedInputStream origin = new BufferedInputStream(new FileInputStream(f));

                while ((count = origin.read(data)) != -1) {
                    if (isCancelled()) {
                        origin.close();
                        out.flush();
                        out.close();
                        file.delete();
                        return BACKUP_MSG_CODE_BACKUP_INTERRUPT;
                    }
                    out.write(data, 0, count);
                }
                out.flush();
                origin.close();

                if (isCancelled()) {
                    out.close();
                    file.delete();
                    return BACKUP_MSG_CODE_BACKUP_INTERRUPT;
                }
            }
            out.close();
        } catch (IOException e) {
            return BACKUP_MSG_CODE_CAN_NOT_WRITE_FILE;
        }

        return BACKUP_MSG_CODE_SUCCESS;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        mBackupingDialogFragment.setProgressItemName(mFileName);
        mBackupingDialogFragment.updateLv1Progress(values[0]);
        mBackupingDialogFragment.updateLv2Progress(1);
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
            FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();
            AlertDialogFragment alertDialogFragment;

            if (mDeleteAfterBackup && result == BACKUP_MSG_CODE_SUCCESS) {
                Bookshelf.getInstance().deleteBook(mBookUuid);
            }
            alertDialogFragment = AlertDialogFragment.newInstance(msgStr, alertIconResId, true, null);

            if (result == BACKUP_MSG_CODE_SUCCESS) {
                alertDialogFragment.registerAlertDialogButtonClickListener((AlertDialogButtonClickListener) mContext,
                        BackupDialogFragment.class.getSimpleName());
            }
            ft.replace(R.id.alert_dialog_container, alertDialogFragment, AlertDialogFragment.class.getSimpleName())
                    .commit();
        }
        mEventBus.post(callbackEvent);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        File file = new File(mFilePath);
        if (file.exists())
            file.delete();
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
}
