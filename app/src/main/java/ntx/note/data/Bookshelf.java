package ntx.note.data;

import android.text.format.Time;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import junit.framework.Assert;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import ntx.note.CallbackEvent;
import ntx.note.Global;
import ntx.note.UndoManager;
import ntx.note.bookshelf.DateNoteData;
import ntx.note.bookshelf.RecentlyNoteData;
import ntx.note.data.Book.BookIOException;
import ntx.note.data.Book.BookLoadException;
import ntx.note.data.Book.BookSaveException;
import ntx.note.data.Storage.StorageIOException;

import static ntx.note.CallbackEvent.SAVE_FAIL;
import static ntx.note.CallbackEvent.SAVE_RECENTLY_NOTE_JSON_DONE;

/**
 * The Bookshelf is a singleton holding the current Book
 * (fully loaded data) and light-weight Book for all books.
 */
public class Bookshelf {
    private static final String TAG = "Bookshelf";
    private static final String QUILL_EXTENSION = ".quill";

    //types for preview sorting
    public @interface PreviewOrder {
        int LAST_MODIFIED = 0;
        int NAME = 1;
        int CREATED_TIME = 2;
    }

    private static Bookshelf instance;

    private LinkedList<Book> data = new LinkedList<>();
    private LinkedList<Book> filterList = new LinkedList<>();
    private Book mCurrentBook;
    private Storage mStorage;
    private int mPreviewOrder = PreviewOrder.LAST_MODIFIED;
    private boolean mDisableBackup = false; // don't backup when download file from dropbox.

    public final static Book NullBook = new Book(Book.NULL_BOOK);

    /**
     * Constructor
     */
    private Bookshelf() {
        this.mStorage = Storage.getInstance();
        LinkedList<UUID> bookUUIDs = mStorage.listBookUUIDs();
        for (UUID uuid : bookUUIDs) {
            data.add(new Book(uuid, false));
        }

        if (!data.isEmpty()) {
            UUID currentBookUuid = mStorage.loadCurrentBookUUID();
            if (currentBookUuid == null)
                currentBookUuid = data.getFirst().getUUID();
            mCurrentBook = new Book(currentBookUuid, true);
        } else {
            mCurrentBook = NullBook;
        }

    }

    /**
     * This is called automatically from the Storage initializer
     */
    protected static void initialize() {
        if (instance == null) {
            Log.v(TAG, "Reading notebook list from storage.");
            instance = new Bookshelf();
        }
    }

    /**
     * Getter
     */
    public static Bookshelf getInstance() {
        Assert.assertNotNull(instance);
        return instance;
    }

    public LinkedList<Book> getBookList() {
        LinkedList<Book> dataClone = new LinkedList<>();
        dataClone.addAll(data);
        return dataClone;
    }

    public LinkedList<Book> getFilterBookList(String keyword) {
        filterList.clear();
        for (Book book : data) {
            String bookTitleLowerCase = book.getTitle().toLowerCase();
            String keywordLowerCase = keyword.toLowerCase();
            if (bookTitleLowerCase.contains(keywordLowerCase))
                filterList.add(book);
        }
        return filterList;
    }

    public int getCount() {
        return data.size();
    }

    public Book getCurrentBook() {
        return mCurrentBook;
    }

    public int getPreviewOrder() {
        return mPreviewOrder;
    }

    /**
     * Setter
     */
    public void setCurrentBook(UUID uuid) {
        if (mCurrentBook != NullBook) {
            if (uuid.equals(mCurrentBook.getUUID()))
                return;
        }

        boolean isUuidInList = false;
        for (Book datum : data) {
            if (datum.uuid.equals(uuid)) {
                isUuidInList = true;
                break;
            }
        }
        if (!isUuidInList)
            return;

        mCurrentBook = new Book(uuid, true);
        UndoManager.getUndoManager().clearHistory();
        mCurrentBook.setOnBookModifiedListener(UndoManager.getUndoManager());
        mStorage.saveCurrentBookUUID(uuid);
    }

    public boolean checkBookExist(UUID uuid) {
        boolean isUuidInList = false;
        for (Book datum : data) {
            if (datum.uuid.equals(uuid)) {
                isUuidInList = true;
                break;
            }
        }
        return isUuidInList;
    }

    public void clearCurrentBook() {
        mCurrentBook = NullBook;
    }

    public void setPreviewOrder(int previewOrder) {
        mPreviewOrder = previewOrder;
    }

    /**
     * Return the preview associated with the given UUID
     *
     * @param uuid
     * @return The Book with matching UUID or null.
     */
    public Book getBook(UUID uuid) {
        for (Book nb : data) {
            if (nb.getUUID().equals(uuid))
                return nb;
        }
        return null;
    }

    public void deleteBook(UUID uuid) {
        if (mCurrentBook != NullBook && uuid.equals(mCurrentBook.uuid)) {
            mCurrentBook = NullBook;
        }
        Book nb = getBook(uuid);
        if (nb == null)
            return;

        mStorage.getBookDirectory(uuid).deleteAll();
        data.remove(nb);

        sortBookList(true);
    }

    public void deleteStorageBook(UUID uuid) {
        if (mCurrentBook != NullBook && uuid.equals(mCurrentBook.uuid)) {
            mCurrentBook = NullBook;
        }
        Book nb = getBook(uuid);
        if (nb == null)
            return;
        data.remove(nb);

        sortBookList(true);
    }

    public void newBook(String title) {
        mCurrentBook = new Book(title);
        mCurrentBook.save();
        sortBookList(true);
    }

    public void addBookToList(Book book) {
        if (data.contains(book))
            return;

        if (book != NullBook)
            data.add(book);
    }

    public boolean removeBookFromList(Book book) {
        return data.remove(book);
    }

    public void sortBookList(boolean saveJsonFile) {
        Assert.assertNotNull(data);

        Collections.sort(data, new BookPreviewComparator());

        if ((mPreviewOrder == PreviewOrder.LAST_MODIFIED) || (mPreviewOrder == PreviewOrder.CREATED_TIME)) {
            Collections.reverse(data);
        }

        if (saveJsonFile)
            saveJson();
    }

    public void updateBookInList(UUID bookUuid) {
        data.remove(getBook(bookUuid));
        data.add(new Book(bookUuid, false));
    }

    protected void assertNoCurrentBook() {
        Assert.assertNull(mCurrentBook);
    }

    /**
     * Private methods
     */
    private void saveJson() {

        // ===== get note list 1~3 =====
        List<RecentlyNoteData> mRecentlyNoteList = new ArrayList<RecentlyNoteData>();
        RecentlyNoteData temp;

        for (int i = 0; i < (data.size() >= 3 ? 3 : data.size()); i++) {
            temp = new RecentlyNoteData(i);
            temp.setId(data.get(i).getUUID());
            temp.setTitle(data.get(i).getTitle());
            mRecentlyNoteList.add(temp);
        }

        // ===== get recentNote.json list 1~3 compare with note list 1~3 =====
        Gson gson = new Gson();
        BufferedReader br = null;

        try {
            String jsonFilePath = Global.APP_DATA_PACKAGE_FILES_PATH + "recentNote.json";
            File folder = new File(Global.APP_DATA_PACKAGE_FILES_PATH);
            if (folder.exists()) {
                br = new BufferedReader(new FileReader(jsonFilePath));
                List<RecentlyNoteData> mRecentlyNoteListInJson = new ArrayList<RecentlyNoteData>(Arrays.asList(gson.fromJson(br, RecentlyNoteData[].class)));

                if (mRecentlyNoteList.size() == mRecentlyNoteListInJson.size()) {
                    boolean isSame = true;
                    for (int i = 0; i < mRecentlyNoteList.size(); i++) {
                        // if change to save json file, else not save json file
                        if (false == mRecentlyNoteList.get(i).getTitle().toString().equals(mRecentlyNoteListInJson.get(i).getTitle().toString())) {
                            isSame = false;
                        }
                    }
                    if (isSame) {
                        CallbackEvent callbackEvent = new CallbackEvent();
                        callbackEvent.setMessage(SAVE_RECENTLY_NOTE_JSON_DONE);
                        EventBus.getDefault().post(callbackEvent);
                        return; // if no Change , do not save json file.
                    }
                }
            } else {
                folder.mkdirs();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // ======= Save =======
        Type listType = new TypeToken<List<RecentlyNoteData>>() {
        }.getType();
        String jsonStr = gson.toJson(mRecentlyNoteList, listType);

        FileWriter jsonFileWriter = null;
        try {
            String jsonFilePath = Global.APP_DATA_PACKAGE_FILES_PATH + "recentNote.json";
            File file = new File(jsonFilePath);
            file.getParentFile().mkdirs();
            jsonFileWriter = new FileWriter(file);
            jsonFileWriter.write(jsonStr);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jsonFileWriter != null) {
                try {
                    jsonFileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            CallbackEvent callbackEvent = new CallbackEvent();
            callbackEvent.setMessage(SAVE_RECENTLY_NOTE_JSON_DONE);
            EventBus.getDefault().post(callbackEvent);
        }
    }

    private class BookPreviewComparator implements Comparator<Book> {
        @Override
        public int compare(Book lhs, Book rhs) {
            if (mPreviewOrder == PreviewOrder.NAME) {
                return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
            } else if (mPreviewOrder == PreviewOrder.CREATED_TIME) {
                return Time.compare(lhs.getCtime(), rhs.getCtime());
            } else {// PreviewOrder.LAST_MODIFIED
                return Time.compare(lhs.getMtime(), rhs.getMtime());
            }
        }
    }


    /**
     * Never be called
     */
    // disable backup(), when dropbox download file
    public void disableBackup() {
        mDisableBackup = true;
    }

    public void enableBackup() {
        mDisableBackup = false;
    }

    /**
     * Import a notebook archive and make it the current book
     *
     * @param file A backup.quill notebook archive
     * @throws BookIOException
     */
    public void importBook(File file) throws BookIOException {
        Book currentBook = null;
        if (mCurrentBook != NullBook) { // Jacky 20160722 fix no Book fail issue
            currentBook = getBook(mCurrentBook.uuid);
            mCurrentBook.save();
        }

        mCurrentBook = NullBook;
        UUID uuid;
        try {
            uuid = mStorage.importArchive(file);
        } catch (StorageIOException e) {
            Log.e(TAG, "importArchive failed (" + e.getMessage() + "), trying old format.");
            try {
                uuid = mStorage.importOldArchive(file);
            } catch (StorageIOException dummy) {
                setCurrentBook(currentBook.getUUID());
                throw new BookLoadException(e.getMessage());
            }
        }

        currentBook = new Book(uuid, true);
        data.add(currentBook);
        setCurrentBook(currentBook.getUUID());
        mCurrentBook.save();
    }

    /**
     * Import a notebook directory.
     * The directory is deleted after importing it.
     *
     * @param dir  A quill notebook directory
     * @param uuid The uuid of the notebook
     * @throws BookIOException
     */
    public void importBookDirectory(File dir, UUID uuid) {
        Book nb;
        final boolean isCurrentBook = mCurrentBook.getUUID().equals(uuid);
        if (isCurrentBook)
            mCurrentBook = NullBook;

        File bookDir = mStorage.getBookDirectory(uuid);
        bookDir.mkdir();
        for (File src : dir.listFiles()) {
            File dst = new File(bookDir, src.getName());
            dst.delete();
            src.renameTo(dst);
        }
        dir.delete();

        nb = new Book(uuid, false);
        data.add(nb);
        if (isCurrentBook)
            setCurrentBook(uuid);
        Assert.assertTrue(data.contains(nb));
    }

    /**
     * Backup all notebooks
     */
    public void backup() {
        if (mDisableBackup) return; // disable backup(), when dropbox download file

        File dir = mStorage.getBackupDir();
        if (dir == null) return;  // backups are disabled by user request
        backup(dir);
    }

    /**
     * Backup all notebooks. Does not overwrite backup files that have the same or newer modification time.
     *
     * @param dir The directory to save the backups in
     */
    private void backup(File dir) {
        for (Book nb : data) {
            UUID uuid = nb.getUUID();
            File file = new File(dir, uuid.toString() + QUILL_EXTENSION);
            File index = new File(mStorage.getBookDirectory(uuid), Book.INDEX_FILE);
            if (file.exists() && file.lastModified() >= index.lastModified()) continue;
            try {
                exportBook(uuid, file);
            } catch (BookSaveException e) {
                mStorage.LogError(TAG, e.getLocalizedMessage());
            }
            backupDescription(dir);
        }
    }

    private void exportBook(UUID uuid, File file) throws BookSaveException {
        if (mCurrentBook.getUUID().equals(uuid))
            mCurrentBook.save();

        try {
            mStorage.exportArchive(uuid, file);
        } catch (StorageIOException e) {
            throw new BookSaveException(e.getMessage());
        }
    }

    /**
     * Save the backup file names and corresponding notebook titles in a JSON file
     */
    private void backupDescription(File dir) {
        JSONObject json = new JSONObject();
        for (Book nb : getBookList())
            try {
                json.put(nb.getUUID().toString(), nb.getTitle());
            } catch (JSONException e) {
                mStorage.LogError(TAG, "cannot create json file");
            }
        File indexFile = new File(dir, "description.json");
        Writer output = null;
        try {
            output = new BufferedWriter(new FileWriter(indexFile));
            output.write(json.toString());
            output.close();
        } catch (IOException e) {
            mStorage.LogError(TAG, "cannot save json file");
        }
    }

    public void saveDateJsonFile() {
        Gson gson = new Gson();

        File folder = new File("/data/data/com.simplemobiletools.calendar/files/");

        String jsonFilePath = "/data/data/com.simplemobiletools.calendar/files/dateNote.json";

        if (!folder.exists()) {
            folder.mkdirs();
        }

        List<DateNoteData> DateNoteDataList = new ArrayList<>();

        for (Book book : Bookshelf.getInstance().getBookList()) {
            DateNoteData tempData = new DateNoteData();
            tempData.setTitle(book.getTitle());
            tempData.setId(book.getUUID());
            Time cTime = book.getCtime();
            tempData.setCreateTime(cTime.year + "/" + (cTime.month + 1) + "/" + cTime.monthDay + " " + cTime.hour + ":" + ((cTime.minute + "").equals("0") ? "00" : (cTime.minute + "")));
            Time mTime = book.getMtime();
            tempData.setModifyTime(mTime.year + "/" + (mTime.month + 1) + "/" + mTime.monthDay + " " + mTime.hour + ":" + ((mTime.minute + "").equals("0") ? "00" : (mTime.minute + "")));
            DateNoteDataList.add(tempData);
        }

        Type listType = new TypeToken<List<DateNoteData>>() {
        }.getType();

        String jsonStr = gson.toJson(DateNoteDataList, listType);

        FileWriter jsonFileWriter = null;
        try {
            File file = new File(jsonFilePath);
            file.getParentFile().mkdirs();
            jsonFileWriter = new FileWriter(file);
            jsonFileWriter.write(jsonStr);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jsonFileWriter != null) {
                try {
                    jsonFileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<DateNoteData> loadJsonFile() {
        String jsonPath = "/data/data/com.simplemobiletools.calendar/files/dateNote.json";
        List<DateNoteData> dateNoteDataList;
        Gson gson = new Gson();
        BufferedReader br;
        if (new File(jsonPath).exists()) {
            try {
                br = new BufferedReader(new FileReader(jsonPath));
                dateNoteDataList = new ArrayList<>(Arrays.asList(gson.fromJson(br, DateNoteData[].class)));
                return dateNoteDataList;
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }
}
