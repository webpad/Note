package name.vbraun.filepicker;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import ntx.note.CallbackEvent;
import ntx.note.data.Book;
import ntx.note.data.Book.BookLoadException;
import ntx.note.data.BookDirectory;
import ntx.note.data.BookOldFormat;
import ntx.note.data.Bookshelf;
import ntx.note.data.Storage;
import ntx.note.data.Storage.StorageIOException;
import ntx.note.export.AlertDialogFragment;
import ntx.note.export.InterruptibleProgressingDialogFragment;
import ntx.note2.R;

public class RestoreAsyncTask extends AsyncTask<Void, Integer, Integer> {
    private static final String TAG = "RestoreAsyncTask";
    private static final String NOTEBOOK_DIRECTORY_PREFIX = "notebook_";
    private final static int RESTORE_MSG_CODE_SUCCESS = 0;
    private final static int RESTORE_MSG_CODE_CAN_NOT_RESTORE = 1;

    private Context mContext;
    private File mFile;
    private int mEntrySize;
    private InterruptibleProgressingDialogFragment mRestoringDialogFragment;

    private EventBus mEventBus;

    public RestoreAsyncTask(Context activityContext, String filePath, int entrySize) {
        this.mContext = activityContext;
        this.mFile = new File(filePath);
        this.mEntrySize = entrySize;
        mEventBus = EventBus.getDefault();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();

        mRestoringDialogFragment = InterruptibleProgressingDialogFragment
                .newInstance(mContext.getString(R.string.restoring), mEntrySize, false);

        ft.replace(R.id.alert_dialog_container, mRestoringDialogFragment,
                InterruptibleProgressingDialogFragment.class.getSimpleName()).commit();

    }

    @Override
    protected Integer doInBackground(Void... arg0) {
        int retCode;
        Book currentBook = Bookshelf.getInstance().getCurrentBook();
        Bookshelf.getInstance().deleteBook(currentBook.getUUID());
        Bookshelf.getInstance().removeBookFromList(currentBook);
        Bookshelf.getInstance().clearCurrentBook();

        UUID uuid = null;
        try {
            uuid = importArchive(mFile);
        } catch (StorageIOException e) {
            Log.e(TAG, "importArchive failed (" + e.getMessage() + "), trying old format.");
            try {
                uuid = importOldArchive(mFile);
            } catch (StorageIOException dummy) {
                Log.e(TAG, "importOldArchive failed (" + dummy.getMessage() + ").");
                retCode = RESTORE_MSG_CODE_CAN_NOT_RESTORE;
                return retCode;
            }
        }

        if (uuid == null)
            retCode = RESTORE_MSG_CODE_CAN_NOT_RESTORE;
        else {
            currentBook = new Book(uuid, false);
            Bookshelf.getInstance().addBookToList(currentBook);
            Bookshelf.getInstance().setCurrentBook(uuid);
            retCode = RESTORE_MSG_CODE_SUCCESS;
        }

        return retCode;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        mRestoringDialogFragment.updateProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Integer result) {
        String msgStr = "";
        int alertIconResId = R.drawable.writing_ic_successful;
        CallbackEvent callbackEvent = new CallbackEvent();
        switch (result) {
            case RESTORE_MSG_CODE_SUCCESS:
                msgStr = mContext.getResources().getString(R.string.successful);
                alertIconResId = R.drawable.writing_ic_successful;
                callbackEvent.setMessage(CallbackEvent.RESTORE_NOTE_SUCCESS);
                break;
            case RESTORE_MSG_CODE_CAN_NOT_RESTORE:
                msgStr = mContext.getResources().getString(R.string.fail);
                alertIconResId = R.drawable.writing_ic_error;
                callbackEvent.setMessage(CallbackEvent.RESTORE_NOTE_ERROR);
                break;
            default:
                break;
        }

        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();
        AlertDialogFragment alertDialogFragment;
        alertDialogFragment = AlertDialogFragment.newInstance(msgStr, alertIconResId, true, null);
        ft.replace(R.id.alert_dialog_container, alertDialogFragment, AlertDialogFragment.class.getSimpleName())
                .commit();

        mEventBus.post(callbackEvent);
    }

    private UUID importArchive(File file) throws StorageIOException {
        File filesFolder = Storage.getInstance().getFilesDir();
        File notebookFile;
        TarInputStream tis = null;
        UUID uuid = null;
        try {
            tis = new TarInputStream(new BufferedInputStream(new FileInputStream(file)));
            TarEntry entry;
            int entryCount = 0;
            while ((entry = tis.getNextEntry()) != null) {
                publishProgress(entryCount);
                if (entry.getName() == null)
                    throw new StorageIOException("Incorrect book archive file");
                if (uuid == null) {
                    uuid = getBookUuidFromDirectoryName(entry.getName());
                    if (uuid == null)
                        throw new StorageIOException("Incorrect book archive file");
                    File notebookDir = getBookDirectory(uuid);
                    if (!notebookDir.exists())
                        notebookDir.mkdir();
                } else if (!uuid.equals(getBookUuidFromDirectoryName(entry.getName())))
                    throw new StorageIOException("Incorrect book archive file");
                notebookFile = new File(filesFolder, entry.getName());
                int count;
                byte data[] = new byte[2048];
                FileOutputStream fos = new FileOutputStream(notebookFile);
                BufferedOutputStream dest = new BufferedOutputStream(fos);
                while ((count = tis.read(data)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
                entryCount++;
            }
        } catch (IOException e) {
            throw new StorageIOException(e.getMessage());
        } finally {
            try {
                if (tis != null)
                    tis.close();
            } catch (IOException e) {
                throw new StorageIOException(e.getMessage());
            }
        }
        if (uuid == null)
            throw new StorageIOException("No ID in book archive file.");
        return uuid;
    }

    private UUID importOldArchive(File file) throws StorageIOException {
        try {
            Book book = new BookOldFormat(file);
            book.save();
            return book.getUUID();
        } catch (BookLoadException e) {
            throw new StorageIOException(e.getMessage());
        }
    }

    private UUID getBookUuidFromDirectoryName(String name) {
        if (!name.startsWith(NOTEBOOK_DIRECTORY_PREFIX))
            return null;
        int n = NOTEBOOK_DIRECTORY_PREFIX.length();
        String uuid = name.substring(n, n + 36);
        return UUID.fromString(uuid);
    }

    private BookDirectory getBookDirectory(UUID uuid) {
        return new BookDirectory(Storage.getInstance(), uuid);

    }

}
