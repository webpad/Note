package com.dropbox.android;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.AsyncTask;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.util.IOUtil;
import com.dropbox.core.util.ProgressOutputStream;
import com.dropbox.core.v2.files.FileMetadata;

import org.greenrobot.eventbus.EventBus;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import ntx.note.CallbackEvent;
import ntx.note.Global;
import ntx.note.data.Book;
import ntx.note.data.BookDirectory;
import ntx.note.data.BookOldFormat;
import ntx.note.data.Bookshelf;
import ntx.note.data.Storage;
import ntx.note.export.AlertDialogFragment;
import ntx.note.export.InterruptibleTwoLevelProcessingDialogFragment;
import ntx.note2.R;

public class DownloadDropboxListFilesTask extends AsyncTask<Void, String, Integer> {
    private final static String NOTEBOOK_DIRECTORY_PREFIX = "notebook_";
    private final static int RETURN_CODE_SUCCESS = 0;
    private final static int RETURN_CODE_DOWNLOAD_FAIL = 1;
    private final static int RETURN_CODE_IMPORT_FAIL = 2;
    private final static int RETURN_CODE_INTERRUPTED = 3;

    private Context mContext;
    private DbxDownloader<FileMetadata> mDbxDownloader = null;
    private List<FileMetadata> mFileMetadataList;
    private int mRetCode = RETURN_CODE_SUCCESS;
    private Exception mException = null;

    private InterruptibleTwoLevelProcessingDialogFragment mDownloadingDialogFragment;
    private String[] mProgressArray = new String[4]; // {ProgressingMessage, ProgressingItemName, Percentage, Progress}

    private Storage mStorage;
    private EventBus mEventBus;

    public DownloadDropboxListFilesTask(Context activityContext, List<FileMetadata> fileMetadataList) {
        this.mContext = activityContext;
        this.mFileMetadataList = fileMetadataList;
        this.mEventBus = EventBus.getDefault();
    }

    @Override
    protected void onPreExecute() {
        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();

        mDownloadingDialogFragment = InterruptibleTwoLevelProcessingDialogFragment.newInstance(
                mContext.getString(R.string.downloading),
                R.drawable.ic_ota_download,
                mFileMetadataList.size(), true);

        mDownloadingDialogFragment.setOnInterruptButtonClickListener(new InterruptibleTwoLevelProcessingDialogFragment.OnInterruptButtonClickListener() {
            @Override
            public void onClick() {
                DownloadDropboxListFilesTask.this.cancel(true);
                showAlertDialog(mContext.getResources().getString(R.string.interrupt), R.drawable.writing_ic_error);

                if (mDbxDownloader != null) {
                    try {
                        mDbxDownloader.close();
                        mDbxDownloader = null;
                    } catch (Exception e) {
                        mDbxDownloader = null;
                        e.printStackTrace();
                    }
                }

                CallbackEvent callbackEvent = new CallbackEvent();
                callbackEvent.setMessage(CallbackEvent.IMPORT_INTERRUPT);
                mEventBus.post(callbackEvent);
            }
        });

        ft.replace(R.id.alert_dialog_container, mDownloadingDialogFragment,
                InterruptibleTwoLevelProcessingDialogFragment.class.getSimpleName()).commit();
    }

    @Override
    protected Integer doInBackground(Void... params) {
        if (this.isCancelled()) {
            mRetCode = RETURN_CODE_INTERRUPTED;
            return mRetCode;
        }

        String dir = Global.PACKAGE_DATA_DIR + Global.FILE_TEMP_DIR;
        File fileDir = new File(dir);
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        File path = new File(dir, Global.TEMP_FILE_NAME); // dataDir=/data/data/ntx.note2

        for (int i = 0; i < mFileMetadataList.size(); i++) {
            mRetCode = RETURN_CODE_SUCCESS;

            if (isCancelled()) {
                mRetCode = RETURN_CODE_INTERRUPTED;
                return mRetCode;
            }

            final FileMetadata metadata = mFileMetadataList.get(i);
            mProgressArray[3] = String.valueOf(i + 1);
            try {
                final File file = new File(path, metadata.getName());

                // Make sure the Downloads directory exists.
                if (!path.exists()) {
                    if (!path.mkdirs()) {
                        mRetCode = RETURN_CODE_DOWNLOAD_FAIL;
                        mException = new RuntimeException("Unable to create directory: " + path);
                    }
                } else if (!path.isDirectory()) {
                    mRetCode = RETURN_CODE_DOWNLOAD_FAIL;
                    mException = new IllegalStateException("Download path is not a directory: " + path);
                    return mRetCode;
                }
                // Download the file.
                OutputStream outputStream = new FileOutputStream(file);

                mDbxDownloader = DropboxClientFactory.getClient().files()
                        .download(metadata.getPathLower(), metadata.getRev());

                mDbxDownloader.download(new ProgressOutputStream(outputStream, new IOUtil.ProgressListener() {
                    @Override
                    public void onProgress(long bytesWritten) {
                        if (isCancelled()) {
                            mRetCode = RETURN_CODE_INTERRUPTED;
                            return;
                        }
                        mProgressArray[0] = mContext.getString(R.string.downloading);
                        mProgressArray[1] = metadata.getName();
                        double temp = (float) bytesWritten / (float) metadata.getSize();
                        mProgressArray[2] = String.valueOf((int) (Math.floor((temp / 2) * 100)));
                        publishProgress(mProgressArray[0], mProgressArray[1], mProgressArray[2], mProgressArray[3]);
                    }
                }) {
                    @Override
                    public void write(int c) throws IOException {
                        if (isCancelled()) {
                            mRetCode = RETURN_CODE_INTERRUPTED;
                            super.close();
                            return;
                        }
                        super.write(c);
                    }
                });

                if (file.exists()) {
                    if (isCancelled()) {
                        mRetCode = RETURN_CODE_INTERRUPTED;
                        file.delete();
                        return mRetCode;
                    }

                    Book book = null;
                    UUID uuid = null;
                    mStorage = Storage.getInstance();
                    try {
                        int pages = getPagesFromFile(file);
                        uuid = importArchive(file, pages);
                    } catch (Storage.StorageIOException e) {
                        try {
                            uuid = importOldArchive(file);
                        } catch (Storage.StorageIOException dummy) {
                            mRetCode = RETURN_CODE_IMPORT_FAIL;
                            return mRetCode;
                        }
                    }

                    if (RETURN_CODE_IMPORT_FAIL == mRetCode || RETURN_CODE_INTERRUPTED == mRetCode) {
                        file.delete();
                        return mRetCode;
                    } else {
                        book = new Book(uuid, false);
                        Bookshelf.getInstance().addBookToList(book);
                    }
                    file.delete();
                }
            } catch (DbxException | IOException e) {
                mRetCode = RETURN_CODE_DOWNLOAD_FAIL;
                mException = e;
                return mRetCode;
            }
        }
        return mRetCode;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        mDownloadingDialogFragment.setProgressMessage(values[0]);
        mDownloadingDialogFragment.setProgressItemName(values[1]);
        mDownloadingDialogFragment.updateLv1Progress(Integer.valueOf(values[2]));
        mDownloadingDialogFragment.updateLv2Progress(Integer.valueOf(values[3]));
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
                callbackEvent.setMessage(CallbackEvent.IMPORT_NOTE_SUCCESS);
                break;
            case RETURN_CODE_DOWNLOAD_FAIL:
                msgStr = mContext.getResources().getString(R.string.fail);
                alertIconResId = R.drawable.writing_ic_error;
                callbackEvent.setMessage(CallbackEvent.IMPORT_NOTE_ERROR);
                break;
            case RETURN_CODE_IMPORT_FAIL:
                msgStr = mContext.getResources().getString(R.string.fail);
                alertIconResId = R.drawable.writing_ic_error;
                callbackEvent.setMessage(CallbackEvent.IMPORT_NOTE_ERROR);
                break;
            case RETURN_CODE_INTERRUPTED:
            default:
                return;
        }

        showAlertDialog(msgStr, alertIconResId);
        mEventBus.post(callbackEvent);
    }

    private UUID importArchive(File file, int pages) throws Storage.StorageIOException {
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
                    try {
                        tis.close();
                    } catch (IOException e) {
                        throw new Storage.StorageIOException(e.getMessage());
                    }
                    mRetCode = RETURN_CODE_INTERRUPTED;
                    return null;
                }

                double temp = (float) entryCount / (float) pages;
                mProgressArray[0] = mContext.getString(R.string.importing);
                mProgressArray[2] = String.valueOf((int) (Math.floor((temp / 2) * 100)) + 50);
                publishProgress(mProgressArray[0], mProgressArray[1], mProgressArray[2], mProgressArray[3]);
                if (entry.getName() == null)
                    throw new Storage.StorageIOException("Incorrect book archive file");
                if (uuid == null) {
                    uuid = getBookUuidFromDirectoryName(entry.getName());
                    if (uuid == null)
                        throw new Storage.StorageIOException("Incorrect book archive file");
                    File notebookDir = getBookDirectory(uuid);
                    if (!notebookDir.exists())
                        notebookDir.mkdir();
                } else if (!uuid.equals(getBookUuidFromDirectoryName(entry.getName())))
                    throw new Storage.StorageIOException("Incorrect book archive file");
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
            throw new Storage.StorageIOException(e.getMessage());
        } finally {
            try {
                if (tis != null)
                    tis.close();
            } catch (IOException e) {
                throw new Storage.StorageIOException(e.getMessage());
            }
        }
        if (uuid == null)
            throw new Storage.StorageIOException("No ID in book archive file.");
        return uuid;
    }

    private UUID importOldArchive(File file) throws Storage.StorageIOException {
        try {
            Book book = new BookOldFormat(file);
            book.save();
            return book.getUUID();
        } catch (Book.BookLoadException e) {
            throw new Storage.StorageIOException(e.getMessage());
        }
    }

    private int getPagesFromFile(File file) throws IOException {
        TarInputStream tis = null;
        UUID uuid = null;
        int pages;

        // check file and get uuid
        try {
            tis = new TarInputStream(new BufferedInputStream(new FileInputStream(file)));
            TarEntry entry;
            pages = 0;
            while ((entry = tis.getNextEntry()) != null) {
                if (entry.getName() == null)
                    throw new IOException("Incorrect book archive file");
                if (uuid == null) {
                    uuid = getBookUuidFromDirectoryName(entry.getName());
                    if (uuid == null)
                        throw new IOException("Incorrect book archive file");
                } else if (!uuid.equals(getBookUuidFromDirectoryName(entry.getName())))
                    throw new IOException("Incorrect book archive file");

                pages++;
            }
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (tis != null)
                    tis.close();
            } catch (IOException e) {
                throw new IOException(e.getMessage());
            }
        }
        if (uuid == null)
            throw new IOException("No ID in book archive file.");

        return pages;
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
}