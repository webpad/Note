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
import java.util.List;
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
import ntx.note.export.InterruptibleTwoLevelProcessingDialogFragment;
import ntx.note2.R;

public class ImportAsyncTask extends AsyncTask<Void, String, Integer> {
    private static final String TAG = "ImportAsyncTask";
    private static final String NOTEBOOK_DIRECTORY_PREFIX = "notebook_";
    private final static int IMPORT_MSG_CODE_SUCCESS = 0;
    private final static int IMPORT_MSG_CODE_CAN_NOT_IMPORT = 1;
    private final static int IMPORT_MSG_CODE_INTERRUPT = 2;

    private Context mContext;
    private List<String> mFileList;
    private List<Integer> mSizeList;
    private InterruptibleTwoLevelProcessingDialogFragment mImportingDialogFragment;
    private String[] mProgressArray = new String[3];

    private Storage mStorage;

    private EventBus mEventBus;

    public ImportAsyncTask(Context activityContext, List<String> fileList, List<Integer> sizeList) {
        this.mContext = activityContext;
        this.mFileList = fileList;
        this.mSizeList = sizeList;
        mEventBus = EventBus.getDefault();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        mStorage = Storage.getInstance();

        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();

        mImportingDialogFragment = InterruptibleTwoLevelProcessingDialogFragment.newInstance(
                mContext.getString(R.string.importing),
                R.drawable.writing_ic_convert_note,
                mFileList.size(),
                true);
        mImportingDialogFragment.setOnInterruptButtonClickListener(
                new InterruptibleTwoLevelProcessingDialogFragment.OnInterruptButtonClickListener() {
                    @Override
                    public void onClick() {
                        ImportAsyncTask.this.cancel(true);

                        showAlertDialog(mContext.getResources().getString(R.string.interrupt), R.drawable.writing_ic_error);

                        CallbackEvent callbackEvent = new CallbackEvent();
                        callbackEvent.setMessage(CallbackEvent.IMPORT_NOTE_ERROR);
                        mEventBus.post(callbackEvent);
                    }
                });

        ft.replace(R.id.alert_dialog_container, mImportingDialogFragment,
                InterruptibleTwoLevelProcessingDialogFragment.class.getSimpleName()).commit();

    }

    @Override
    protected Integer doInBackground(Void... arg0) {
        int retCode;

        File mFile;
        for (int i = 0; i < mFileList.size(); i++) {
            if (isCancelled()) {
                retCode = IMPORT_MSG_CODE_INTERRUPT;
                return retCode;
            }

            mProgressArray[2] = String.valueOf(i + 1);
            mFile = new File(mFileList.get(i));
            mProgressArray[0] = mFile.getName();

            UUID uuid = null;
            try {
                uuid = importArchive(i, mFile);
            } catch (StorageIOException e) {
                Log.e(TAG, "importArchive failed (" + e.getMessage() + "), trying old format.");
                try {
                    uuid = importOldArchive(mFile);
                } catch (StorageIOException dummy) {
                    Log.e(TAG, "importOldArchive failed (" + dummy.getMessage() + ").");
                    retCode = IMPORT_MSG_CODE_CAN_NOT_IMPORT;
                    return retCode;
                }
            }

            if (isCancelled()) {
                retCode = IMPORT_MSG_CODE_INTERRUPT;
                return retCode;
            }

            Bookshelf.getInstance().updateBookInList(uuid);
        }
        Bookshelf.getInstance().sortBookList(true);
        Bookshelf.getInstance().saveDateJsonFile();
        retCode = IMPORT_MSG_CODE_SUCCESS;
        return retCode;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        mImportingDialogFragment.setProgressItemName(values[0]);
        mImportingDialogFragment.updateLv1Progress(Integer.valueOf(values[1]));
        mImportingDialogFragment.updateLv2Progress(Integer.valueOf(values[2]));
    }

    @Override
    protected void onPostExecute(Integer result) {
        String msgStr;
        int alertIconResId;
        CallbackEvent callbackEvent = new CallbackEvent();
        switch (result) {
            case IMPORT_MSG_CODE_SUCCESS:
                msgStr = mContext.getResources().getString(R.string.successful);
                alertIconResId = R.drawable.writing_ic_successful;
                callbackEvent.setMessage(CallbackEvent.IMPORT_NOTE_SUCCESS);
                break;
            case IMPORT_MSG_CODE_CAN_NOT_IMPORT:
                msgStr = mContext.getResources().getString(R.string.fail);
                alertIconResId = R.drawable.writing_ic_error;
                callbackEvent.setMessage(CallbackEvent.IMPORT_NOTE_ERROR);
                break;
            case IMPORT_MSG_CODE_INTERRUPT:
            default:
                return;
        }

        showAlertDialog(msgStr, alertIconResId);
        mEventBus.post(callbackEvent);
    }

    private UUID importArchive(int index, File file) throws StorageIOException {
        File filesFolder = mStorage.getFilesDir();
        File notebookFile = null;
        TarInputStream tis = null;
        UUID uuid = null;
        try {
            tis = new TarInputStream(new BufferedInputStream(new FileInputStream(file)));
            TarEntry entry;
            int entryCount = 0;
            while ((entry = tis.getNextEntry()) != null) {
                if (isCancelled()) {
                    tis.close();
                    if (uuid != null)
                        cleanImportUncompletedFile(getBookDirectory(uuid));
                    return null;
                }

                double temp = (float) entryCount / (float) mSizeList.get(index);
                mProgressArray[1] = String.valueOf((int) (Math.floor(temp * 100)));
                publishProgress(mProgressArray[0], mProgressArray[1], mProgressArray[2]);

                if (entry.getName() == null)
                    throw new StorageIOException("Incorrect book archive file");

                if (uuid == null) {
                    uuid = getBookUuidFromDirectoryName(entry.getName());

                    if(Bookshelf.getInstance().getCurrentBook().getUUID().equals(uuid)){
                        Bookshelf.getInstance().deleteBook(uuid);
                    }

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
                    if (isCancelled()) {
                        dest.close();
                        fos.close();
                        tis.close();
                        notebookFile.delete();
                        cleanImportUncompletedFile(getBookDirectory(uuid));
                        return null;
                    }

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
        return new BookDirectory(mStorage, uuid);
    }

    private void showAlertDialog(String msgStr, int iconId) {
        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();
        AlertDialogFragment alertDialogFragment;
        alertDialogFragment = AlertDialogFragment.newInstance(msgStr, iconId, true, null);
        ft.replace(R.id.alert_dialog_container, alertDialogFragment, AlertDialogFragment.class.getSimpleName()).commit();
    }

    private void cleanImportUncompletedFile(File bookDirectory) {
        for (String child : bookDirectory.list()) {
            new File(bookDirectory, child).delete();
        }
        bookDirectory.delete();
    }

}
