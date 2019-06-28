package ntx.note.asynctask;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;

import name.vbraun.view.write.Page;
import ntx.note.data.Book;
import ntx.note.data.BookDirectory;
import ntx.note.data.Bookshelf;
import ntx.note.data.TagManager;

public class NoteLoadTask extends AsyncTask<String, Void, Void> {
    private LinkedList<UUID> loadPageUUIDs = null;
    BookDirectory loadDir = null;
    LinkedList<Page> loadPages = null;

    public NoteLoadTask(LinkedList<UUID> pageUUIDs, BookDirectory dir, LinkedList<Page> pages) {
        this.loadPageUUIDs = pageUUIDs;
        this.loadDir = dir;
        this.loadPages = pages;
    }

    public NoteLoadTask() {
    }

    @Override
    protected Void doInBackground(String... params) {

        for (int i = 0; i < loadPageUUIDs.size(); i++) {
            if (i == Bookshelf.getInstance().getCurrentBook().currentPageNumber()) {
                continue;
            }
            try {
                loadPage(loadPageUUIDs.get(i), loadDir);

                //replace and delete
                Page nowPage = loadPages.getLast();
                loadPages.removeLast();
                loadPages.set(i, nowPage);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        Bookshelf.getInstance().getCurrentBook().filterChanged();

        return null;
    }

    private void loadPage(UUID uuid, File dir) throws IOException {
        File file = getPageFile(dir, uuid);
        FileInputStream fis = null;
        BufferedInputStream buffer = null;
        DataInputStream dataIn = null;
        try {
            TagManager tagManager = new TagManager();
            fis = new FileInputStream(file);
            buffer = new BufferedInputStream(fis);
            dataIn = new DataInputStream(buffer);
            Page page = new Page(dataIn, tagManager, dir);
            if (!page.getUUID().equals(uuid)) {
                //Storage.getInstance().LogError(TAG, "Page UUID mismatch.");
                page.touch();
            }
            loadPages.add(page);
        } finally {
            if (dataIn != null) dataIn.close();
            else if (buffer != null) buffer.close();
            else if (fis != null) fis.close();
        }
    }

    private File getPageFile(File dir, UUID uuid) {
        return new File(dir, Book.PAGE_FILE_PREFIX + uuid.toString() + Book.QUILL_DATA_FILE_SUFFIX);
    }

    protected void onProgressUpdate() {

    }

    protected void onPostExecute() {

    }

}
