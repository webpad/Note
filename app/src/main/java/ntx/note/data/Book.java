package ntx.note.data;

import android.text.format.Time;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import junit.framework.Assert;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import name.vbraun.view.write.Page;
import ntx.note.ALog;
import ntx.note.BookModifiedListener;
import ntx.note.CallbackEvent;
import ntx.note.Global;
import ntx.note.bookshelf.DateNoteData;
import ntx.note.bookshelf.RecentlyNoteData;
import ntx.note.data.TagManager.TagSet;

/**
 * Data model for "Book"
 * A book is a collection of Pages and the tag manager together with some
 * metadata like its title. The data is stored in a fixed
 * {@link BookDirectory}.
 *
 * @author vbraun
 */
public class Book {
    public static final String BEFORE_QUILL_DATA_FILE_SUFFIX = ".quill_data";
    public static final String QUILL_DATA_FILE_SUFFIX = ".page";
    public static final String QUILL_DATA_FILE_SUFFIX_TEMP = ".temp";
    public static final String QUILL_DATA_FILE_SUFFIX_OLD = ".old";
    public static final String PAGE_FILE_PREFIX = "page_";
    public static final String AUTO_PAGE_FILE_PREFIX = "auto_page_";
    public static final String NULL_BOOK = "null_book";

    private static final String TAG = "Book";
    protected static final String BEFORE_INDEX_FILE = "index" + BEFORE_QUILL_DATA_FILE_SUFFIX;
    protected static final String INDEX_FILE = "index";
    protected static final String INDEX_FILE_TEMP = "index" + QUILL_DATA_FILE_SUFFIX_TEMP;
    protected static final String INDEX_FILE_OLD = "index" + QUILL_DATA_FILE_SUFFIX_OLD;
    protected static final String AUTO_INDEX_FILE = "auto_index";
    protected static final String AUTO_INDEX_FILE_TEMP = "auto_index" + QUILL_DATA_FILE_SUFFIX_TEMP;
    protected static final String AUTO_INDEX_FILE_OLD = "auto_index" + QUILL_DATA_FILE_SUFFIX_OLD;
    protected static final String AUTO = "auto_";

    protected UUID uuid;
    protected String title = "Default nNote notebook";
    protected Time ctime = new Time(); // creation time
    protected Time mtime = new Time(); // last modification time
    protected boolean allowSave = false; // unset this to ensure that the book is never saved (truncated previews, for example)
    protected int numPages;
    protected int currentPage = 0;
    protected final TagManager tagManager = new TagManager();
    protected final LinkedList<Page> pages = new LinkedList<Page>();
    protected final LinkedList<Page> filteredPages = new LinkedList<Page>();
    protected final LinkedList<Page> unionFilteredPages = new LinkedList<Page>();

    private TagSet filter = tagManager.newTagSet();
    private boolean modified = false; // You must set this to true if you change metadata (e.g. title)
    private boolean auto_modified = false; // You must set this to true if you change metadata (e.g. title)
    private boolean mIsFailSaveIndexData = false; // if saveIndex dataOut null, savePage also not save.
    private BookModifiedListener bookModifiedListener; // Report page changes to the undo manager
    private SimpleDateFormat s = new SimpleDateFormat("HH:mm:ss.SSS");
    //	public NoteLoadTask asyncTask=new NoteLoadTask(); // Define null first to avoid crash.

    protected Book() {
        allowSave = true;
    }

    final private static Object lock = "SaveLock";

    /**
     * Construct Book and load all pages or none.
     *
     * @param uuid
     * @param isLoadedAllPages true:load all pages, false:load none
     */
    public Book(UUID uuid, boolean isLoadedAllPages) {
        allowSave = true;
        this.uuid = uuid;
        Storage storage = Storage.getInstance();
        BookDirectory dir = storage.getBookDirectory(uuid);
        int totalPages = isLoadedAllPages ? -1 : 0;
        try {
            doLoadBookFromDirectory(dir, 0, totalPages);
        } catch (BookLoadException e) {
            storage.LogError(TAG, e.getLocalizedMessage());
        } catch (EOFException e) {
            storage.LogError(TAG, "Truncated data file");
        } catch (IOException e) {
            storage.LogError(TAG, e.getLocalizedMessage());
        }
        loadingFinishedHook();
    }

    public void setAllowSave(boolean allowSave) {
        this.allowSave = allowSave;
    }

    /**
     * Construct Book and load numPages from startPageIndex.
     *
     * @param uuid
     * @param startPageIndex
     * @param numPages
     */
    public Book(UUID uuid, int startPageIndex, int numPages) {
        allowSave = true;
        this.uuid = uuid;
        Storage storage = Storage.getInstance();
        BookDirectory dir = storage.getBookDirectory(uuid);
        try {
            doLoadBookFromDirectory(dir, startPageIndex, numPages);
        } catch (BookLoadException e) {
            storage.LogError(TAG, e.getLocalizedMessage());
        } catch (EOFException e) {
            storage.LogError(TAG, "Truncated data file");
        } catch (IOException e) {
            storage.LogError(TAG, e.getLocalizedMessage());
        }
        loadingFinishedHook();
    }

    public Book(String description) {
        if (description.equals(NULL_BOOK)) {
            allowSave = false;
            numPages = 0;
            pages.clear();
            ctime.setToNow();
            mtime.setToNow();
            uuid = new UUID(0, 0);
            title = description;
            modified = false;
            auto_modified = false;
            currentPage = 0;
        } else {
            allowSave = true;
            pages.add(new Page(tagManager));
            ctime.setToNow();
            mtime.setToNow();
            uuid = UUID.randomUUID();
            title = description;
            modified = true;
            auto_modified = true;
            loadingFinishedHook();
        }
    }

    /**
     * Public Getter
     */
    public UUID getUUID() {
        return uuid;
    }

    public String getTitle() {
        return title;
    }

    public Time getCtime() {
        return ctime;
    }

    public Time getMtime() {
        return mtime;
    }

    public TagManager getTagManager() {
        return tagManager;
    }

    public TagSet getFilter() {
        return filter;
    }

    public LinkedList<Page> getPages() {
        LinkedList<Page> pagesClone = new LinkedList<>();
        pagesClone.addAll(pages);
        return pagesClone;
    }

    public LinkedList<Page> getFilteredPages() {
        LinkedList<Page> filteredPagesClone = new LinkedList<>();
        filteredPagesClone.addAll(filteredPages);
        return filteredPagesClone;
    }

    public Page getPage(int n) {
        return pages.get(n);
    }

    public int getPageNumber(Page page) {
        return pages.indexOf(page);
    }

    /**
     * Public Setter
     */
    public void setOnBookModifiedListener(BookModifiedListener newListener) {
        bookModifiedListener = newListener;
    }

    public void setFilter(TagSet newFilter) {
        filter = newFilter;
    }

    public void tagSetRemoveTag(TagManager.Tag tag) {
        filter.remove(tag);
    }

    public void tagSetAddTag(TagManager.Tag tag) {
        filter.add(tag);
    }

    public void clearTagSetCheckedTag() {
        filter.clearCheckedTag();
    }

    /**
     * Call this whenever the filter changed
     * will ensure that there is at least one page matching the filter
     * but will not change the current page (which need not match).
     */
    public void filterChanged() {
        Page curr = currentPage();
        updateFilteredPages();
        Assert.assertTrue("current page must not change", curr == currentPage());
    }

    private void updateFilteredPages() {
        filteredPages.clear();
        ListIterator<Page> iter = pages.listIterator();
        while (iter.hasNext()) {
            Page p = iter.next();
            if (pageMatchesFilter(p))
                filteredPages.add(p);
        }
    }

    private void updateUnionFilteredPages() {
        unionFilteredPages.clear();
        for (Page p : pages) {
            if (pageContainsFilterTag(p))
                unionFilteredPages.add(p);
        }
    }

    public LinkedList<Page> getUnionFilteredPages(boolean includeQuickTag) {
        updateUnionFilteredPages();
        LinkedList<Page> unionFilteredPagesClone = new LinkedList<>();
        if (!includeQuickTag)
            unionFilteredPagesClone.addAll(unionFilteredPages);
        else {
            TagManager.Tag quickTag = tagManager.findTag(TagManager.QUICK_TAG_NAME);
            for (Page p : unionFilteredPages) {
                if (p.tags.contains(quickTag))
                    unionFilteredPagesClone.add(p);
            }
        }
        return unionFilteredPagesClone;
    }

    /**
     * remove empty pages as far as possible
     */
    private void removeEmptyPages() {
        Page curr = currentPage();
        LinkedList<Page> empty = new LinkedList<Page>();
        ListIterator<Page> iter = pages.listIterator();
        while (iter.hasNext()) {
            Page p = iter.next();
            if (p == curr)
                continue;
            if (filteredPages.size() <= 1 && filteredPages.contains(p))
                continue;
            if (p.isEmpty()) {
                empty.add(p);
                filteredPages.remove(p);
            }
        }
        iter = empty.listIterator();
        while (iter.hasNext()) {
            Page p = iter.next();
            requestRemovePage(p);
        }
        currentPage = pages.indexOf(curr);
        Assert.assertTrue("Current page removed?", currentPage >= 0);
    }

    public void setTitle(String title) {
        this.title = title;
        modified = true;
        auto_modified = true;
    }

    public BookDirectory getDirectory() {
        Storage storage = Storage.getInstance();
        return storage.getBookDirectory(uuid);
    }

    public long getBookSizeInStorage() {
        Storage storage = Storage.getInstance();
        return storage.getBookDirectory(uuid).getTotalSize();
    }

    // to be called from the undo manager
    public void addPage(Page page, int position, boolean changeCurrentToNew) {
        Assert.assertFalse("page already in book", pages.contains(page));
        touchAllSubsequentPages(position);
        pages.add(position, page);
        updateFilteredPages();
        if (changeCurrentToNew)
            currentPage = position;
        modified = true;
        auto_modified = true;
    }

    // to be called from the undo manager
    public void removePage(Page page, int position, boolean changeCurrentToNew) {
        Assert.assertTrue("page not in book", getPage(position) == page);
        int pos = filteredPages.indexOf(page);
        if (pos >= 0) {
            if (pos + 1 < filteredPages.size()) {
                pos = pages.indexOf(filteredPages.get(pos + 1)) - 1;
            } else if (pos - 1 >= 0)
                pos = pages.indexOf(filteredPages.get(pos - 1));
            else
                pos = -1;
        }
        if (pos == -1) {
            if (position + 1 < pages.size())
                pos = position + 1 - 1;
            else if (position - 1 >= 0)
                pos = position - 1;
            else
                Assert.fail("Cannot create empty book");
        }
        pages.remove(position);
        updateFilteredPages();
        touchAllSubsequentPages(position);
        if (changeCurrentToNew)
            currentPage = pos;
        else {
            if (currentPage >= pages.size())
                currentPage--;
            else if (position == currentPage)
                currentPage = pos;
        }
        modified = true;
        auto_modified = true;
        if (Global.isDebug)
            Log.d(TAG, "Removed page " + position + ", current = " + currentPage);
    }

    // mark all subsequent pages as changed so that they are saved again
    private void touchAllSubsequentPages(int fromPage) {
        for (int i = fromPage; i < pages.size(); i++)
            getPage(i).touch();
    }

    private void requestAddPage(Page page, int position) {

        /**
         * MobiScribe request: the customized background is still kept.
         */
//        if (page.getPaperType() == Paper.Type.CUSTOMIZED) {
//            page.setPaperType(Paper.Type.EMPTY, "na");
//        }

        if (bookModifiedListener == null)
            addPage(page, position, true);
        else
            bookModifiedListener.onPageInsertListener(page, position);
    }

    private void requestRemovePage(Page page) {
        int position = pages.indexOf(page);
        if (bookModifiedListener == null)
            removePage(page, position, true);
        else
            bookModifiedListener.onPageDeleteListener(page, position);
    }

    private boolean pageMatchesFilter(Page page) {
        ListIterator<TagManager.Tag> iter = getFilter().tagIterator();
        while (iter.hasNext()) {
            TagManager.Tag t = iter.next();
            if (!page.tags.contains(t)) {
                // Log.d(TAG, "does not match: "+t.name+" "+page.tags.size());
                return false;
            }
        }
        return true;
    }

    private boolean pageContainsFilterTag(Page page) {
        for (TagManager.Tag tag : filter.tags) {
            if (tag.toString().equals(TagManager.QUICK_TAG_NAME))
                continue;
            if (page.tags.contains(tag))
                return true;
        }
        return false;
    }

    public int currentPageNumber() {
        return currentPage;
    }

    public Page currentPage() {
        // Log.v(TAG, "current_page() "+currentPage+"/"+pages.size());
        Assert.assertTrue(currentPage >= 0 && currentPage < pages.size());
        return pages.get(currentPage);
    }

    public void setCurrentPage(Page page) {
        currentPage = pages.indexOf(page);
        Assert.assertTrue(currentPage >= 0);
    }

    public int pagesSize() {
        return pages.size();
    }

    public int pagesSizeFromIndexFile() {
        return numPages;
    }

    public int filteredPagesSize() {
        return filteredPages.size();
    }

    public Page getFilteredPage(int position) {
        return filteredPages.get(position);
    }

    // deletes page but makes sure that there is at least one page
    // the book always has at least one page.
    // deleting the last page is only clearing it etc.
    public void deletePage() {
        if (Global.isDebug)
            Log.d(TAG, "delete_page() " + currentPage + "/" + pages.size());
        Page page = currentPage();
        if (pages.size() == 1) {
            requestAddPage(Page.emptyWithStyleOf(page), 1);
        }
        requestRemovePage(page);
    }

    public void deletePagesSet(LinkedList<Page> pagesSet) {
        Page firstPage = pages.getFirst();
        for (int i = pagesSet.size() - 1; i >= 0; i--) {
            Page p = pagesSet.get(i);
            if (i == 0 && pages.size() == 1) {
                addPage(Page.emptyWithStyleOf(firstPage), 1, true);
            }
            removePage(p, pages.indexOf(p), false);
        }
    }

    public Page nextPage() {
        int pos = filteredPages.indexOf(currentPage());
        Page next = null;
        if (pos >= 0) {
            ListIterator<Page> iter = filteredPages.listIterator(pos);
            iter.next(); // == currentPage()
            if (iter.hasNext())
                next = iter.next();
        } else {
            ListIterator<Page> iter = pages.listIterator(currentPage);
            iter.next(); // == currentPage()
            while (iter.hasNext()) {
                Page p = iter.next();
                if (pageMatchesFilter(p)) {
                    next = p;
                    break;
                }
            }
        }
        if (next == null)
            return currentPage();
        currentPage = pages.indexOf(next);
        Assert.assertTrue(currentPage >= 0);
        return next;
    }

    public Page previousPage() {
        int pos = filteredPages.indexOf(currentPage());
        Page prev = null;
        if (pos >= 0) {
            ListIterator<Page> iter = filteredPages.listIterator(pos);
            if (iter.hasPrevious())
                prev = iter.previous();
            if ((prev != null) && (Global.isDebug == true))
                Log.d(TAG, "Prev " + pos + " " + pageMatchesFilter(prev));
        } else {
            ListIterator<Page> iter = pages.listIterator(currentPage);
            while (iter.hasPrevious()) {
                Page p = iter.previous();
                if (pageMatchesFilter(p)) {
                    prev = p;
                    break;
                }
            }
        }
        if (prev == null)
            return currentPage();
        currentPage = pages.indexOf(prev);
        Assert.assertTrue(currentPage >= 0);
        return prev;
    }

    public Page nextPageUnfiltered() {
        if (currentPage + 1 < pages.size())
            currentPage += 1;
        return pages.get(currentPage);
    }

    public Page previousPageUnfiltered() {
        if (currentPage > 0)
            currentPage -= 1;
        return pages.get(currentPage);
    }

    // inserts a page at position and makes it the current page
    // empty pages are removed
    public Page insertPage(Page template, int position) {
        Page new_page;
        if (template != null)
            new_page = Page.emptyWithStyleOf(template);
        else
            new_page = new Page(tagManager);
        new_page.tags.add(getFilter());
        requestAddPage(new_page, position); // pages.add(position, new_page);
//		removeEmptyPages();
        Assert.assertTrue("Missing tags?", pageMatchesFilter(new_page));
        Assert.assertTrue("wrong page", new_page == currentPage());
        return new_page;
    }

    public Page cloneCurrentPageToNextPage() {
        Page new_page;
        Storage storage = Storage.getInstance();
        BookDirectory dir = storage.getBookDirectory(uuid);
        new_page = new Page(currentPage(), dir);
        new_page.tags.add(currentPage().getTags());
        addPage(new_page, currentPage + 1, true);
        return new_page;
    }

    public void clonePageTo(Page page, int position, boolean includeTags) {
        Page new_page;
        Storage storage = Storage.getInstance();
        BookDirectory dir = storage.getBookDirectory(uuid);
        new_page = new Page(page, dir);
        if (includeTags)
            new_page.tags.add(page.getTags());
        addPage(new_page, position + 1, false);
    }

    public Page insertPage() {
        return insertPage(currentPage(), currentPage + 1);
    }

    public Page insertPageAtEnd() {
        return insertPage(currentPage(), pages.size());
    }

    public boolean isFirstPage() {
        if (filteredPages.isEmpty()) return false;
        return currentPage() == filteredPages.getFirst();
    }

    public boolean isLastPage() {
        if (filteredPages.isEmpty()) return false;
        return currentPage() == filteredPages.getLast();
    }

    public boolean isFirstPageUnfiltered() {
        return currentPage == 0;
    }

    public boolean isLastPageUnfiltered() {
        return currentPage + 1 == pages.size();
    }

    // ///////////////////////////////////////////////////
    // Input/Output

    // Base exception
    public static class BookIOException extends Exception {
        public BookIOException(String string) {
            super(string);
        }

        private static final long serialVersionUID = 4923229504804959444L;
    }

    public static class BookLoadException extends BookIOException {
        public BookLoadException(String string) {
            super(string);
        }

        private static final long serialVersionUID = -4727997764997002754L;
    }

    public static class BookSaveException extends BookIOException {
        public BookSaveException(String string) {
            super(string);
        }

        private static final long serialVersionUID = -7622965955861362254L;
    }

    // Pick a current page if it is out of bounds
    private void makeCurrentPageConsistent() {
        if (currentPage < 0)
            currentPage = 0;
        if (currentPage >= pages.size())
            currentPage = pages.size() - 1;
        if (pages.isEmpty()) {
            Page p = new Page(tagManager);
            p.tags.add(getFilter());
            pages.add(p);
            currentPage = 0;
        }
    }

    /**
     * Called at the end of every constructor
     */
    protected void loadingFinishedHook() {
        makeCurrentPageConsistent();
        filterChanged();
    }

    public boolean save() {
        return save(false);
    }

    // save data internally. To load, use the constructor.
    public boolean save(boolean autoSave) {
        synchronized (lock) {
            if (!allowSave && !autoSave)
                return false;
            if (autoSave) {
                Log.d(TAG, "Auto Save Start : " + s.format(new Date()));
            } else {
                Log.d(TAG, "Normal Save Start : " + s.format(new Date()));
            }
            Storage storage = Storage.getInstance();
            try {
                BookDirectory dir = storage.getBookDirectory(getUUID());
                if (!doSaveBookInDirectory(dir, autoSave)) {
                    if (!autoSave) {
                        CallbackEvent event = new CallbackEvent();
                        event.setMessage(CallbackEvent.SAVE_FAIL);
                        EventBus.getDefault().post(event);
                    }
                    return false;
                }

                if (autoSave) {
                    markAsSaved(autoSave);
                } else {
                    markAsSaved(autoSave);
                    markAsSaved(!autoSave);
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                if (!autoSave) {
                    CallbackEvent event = new CallbackEvent();
                    event.setMessage(CallbackEvent.SAVE_FAIL);
                    EventBus.getDefault().post(event);
                }
                return false;
            }
            Bookshelf.getInstance().updateBookInList(uuid);
            Bookshelf.getInstance().sortBookList(true);
            if (autoSave) {
                Log.d(TAG, "Auto Save End : " + s.format(new Date()));
            } else {
                Log.d(TAG, "Normal Save End : " + s.format(new Date()));
            }
            Bookshelf.getInstance().saveDateJsonFile();
            return true;
        }
    }

    public boolean savePageToStorage(Page page) {

        synchronized (lock) {
            boolean useTempSave;
            Storage storage = Storage.getInstance();
            BookDirectory dir = storage.getBookDirectory(getUUID());
            File file;

            useTempSave = new File(dir, PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX).exists() ? true : false;

            if (useTempSave) {
                file = new File(dir, PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX_TEMP);
            } else {
                file = new File(dir, PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX);
            }

            FileOutputStream fos = null;
            BufferedOutputStream buffer = null;
            DataOutputStream dataOut = null;
            try {
                fos = new FileOutputStream(file);
                buffer = new BufferedOutputStream(fos);
                dataOut = new DataOutputStream(buffer);
                page.writeToStream(dataOut);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (dataOut != null)
                        dataOut.close();
                    if (buffer != null)
                        buffer.close();
                    if (fos != null)
                        fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                fos.close();
                dataOut.close();
                buffer.close();
            } catch (Exception e) {

            }

            if (useTempSave) {
                File originFile = new File(dir, PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX);
                File originFileRename = new File(dir, PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX_OLD);
                File tempFile = new File(dir, PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX_TEMP);
                File tempFileRename = new File(dir, PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX);
                if (!tryRename(originFile, originFileRename, tempFile, tempFileRename)) {
                    return false;
                }
            }

            if (!deleteAutoSave(new File(dir, AUTO_PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX))) {
                return false;
            }

            markAsSaved(false);
            markAsSaved(true);
            return true;
        }
    }

    private boolean deleteAutoSave(File auto) {
        if (auto.exists()) {
            for (int i = 0; i < 10; i++) {
                if (auto.delete()) {
                    return true;
                } else {
                    if (i == 9) {
                        return false;
                    }
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private boolean isModified(boolean autoSave) {
        if (autoSave) {
            if (auto_modified)
                return true;

            for (Page page : pages)
                if (page.isModified(autoSave))
                    return true;

            return false;
        } else {
            if (modified)
                return true;

            for (Page page : pages)
                if (page.isModified(autoSave))
                    return true;

            return false;
        }
    }

    /**
     * To be called after the book has been saved to the internal storage (but NOT: anywhere else like backups)
     */
    private void markAsSaved(boolean autoSave) {
        if (autoSave) {
            auto_modified = false;
        } else {
            modified = false;
        }
        for (Page page : pages)
            page.markAsSaved(autoSave);
    }

    /**
     * Karote 20181226
     * startPageIndex : the page index to start loading. first index = 0.
     * totalPages: the page size to load, min=1, max=book.pageSize(), -1 will load all page.
     */
    private void doLoadBookFromDirectory(BookDirectory dir, int startPageIndex, int totalPages) throws BookLoadException, IOException {

        if (!dir.isDirectory())
            throw new BookLoadException("No such directory: " + dir.toString());

        LinkedList<UUID> pageUUIDs;

        FileInputStream fis = null;
        BufferedInputStream buffer = null;
        DataInputStream dataIn = null;
        try {
            File before_index_file = new File(dir, BEFORE_INDEX_FILE);
            File after_index_file = new File(dir, INDEX_FILE);
            if (before_index_file.exists() && after_index_file.exists()) {
                before_index_file.delete();
            }
            if (before_index_file.exists() && !after_index_file.exists()) {
                boolean index_rename_success;
                index_rename_success = before_index_file.renameTo(after_index_file);
                Log.d(TAG, "Index Rename : " + index_rename_success);
            }

            File auto = new File(dir, AUTO_INDEX_FILE);
            File origin = new File(dir, INDEX_FILE);
            File temp = new File(dir, INDEX_FILE_TEMP);
            File old = new File(dir, INDEX_FILE_OLD);

            if (auto.exists() && origin.exists()) {
                Date auto_date = new Date(auto.lastModified());
                Date origin_date = new Date(origin.lastModified());
                int comparison = auto_date.compareTo(origin_date);
                if (comparison < 0) {
                    fis = new FileInputStream(origin);
                    Log.d(TAG, "Index : origin");
                } else {
                    fis = new FileInputStream(auto);
                    Log.d(TAG, "Index : auto");
                }
            } else if (auto.exists() && !origin.exists()) {
                fis = new FileInputStream(auto);
                Log.d(TAG, "Index : auto1");
            } else if (!auto.exists() && origin.exists()) {
                fis = new FileInputStream(origin);
                Log.d(TAG, "Index : origin1");
            } else if (before_index_file.exists()) {
                fis = new FileInputStream(before_index_file);
                Log.d(TAG, "Index : before_index_file");
            } else if (temp.exists()) {
                fis = new FileInputStream(temp);
                Log.d(TAG, "Index : temp");
            } else if (old.exists()) {
                fis = new FileInputStream(old);
                Log.d(TAG, "Index : old");
            } else {
                Log.d(TAG, "Index : null");
                fis = null;
            }

            buffer = new BufferedInputStream(fis);
            dataIn = new DataInputStream(buffer);
            pageUUIDs = loadIndex(dataIn);

            /**
             * 2019.04.26 Karote marked
             * To find fail page will test page files and test function will load page.
             * To load page files will make I/O open and close to many times.
             * It may make I/O run out EBADF exception.
             */
            /*
            LinkedList<UUID> pageUUIDsFail = new LinkedList<UUID>();

            // add  remaining page uuids from files in dir
            LinkedList<UUID> pageUUIDsInDir = dir.listPages();
            pageUUIDsInDir.removeAll(pageUUIDs);

            // Jacky 20170822 find fail page
            for (int i = 0; i < pageUUIDsInDir.size(); i++) {
                if (isPageOK(pageUUIDsInDir.get(i), dir) == false) {
                    pageUUIDsFail.add(pageUUIDsInDir.get(i));
                }
            }

            // Jacky 20170822 remove fail page
            pageUUIDsInDir.removeAll(pageUUIDsFail);

            if (!pageUUIDsInDir.isEmpty()) {
                pageUUIDs.addAll(pageUUIDsInDir);
                Storage.getInstance().LogError(TAG, "I recovered pages missing in notebook index");
            }
            */

        } catch (Exception e) {

            /**
             * 2019.04.26 Karote marked
             * To test page files will load page files.
             * To load page files will make I/O open and close to many times.
             * It may make I/O run out EBADF exception.
             * So...DO NOT to test the page files.
             */
            /*
            // Jacky 20170822 If the index.quill_data file is damaged, ignore "pageUUIDs = loadIndex(dir)"
            pageUUIDs = new LinkedList<UUID>();
            LinkedList<UUID> pageUUIDsFromDir = dir.listPages();
            for (int i = 0; i < pageUUIDsFromDir.size(); i++) {
                if (isPageOK(pageUUIDsFromDir.get(i), dir)) {
                    pageUUIDs.add(pageUUIDsFromDir.get(i));
                }
            }
            */
            pageUUIDs = dir.listPages();
        }

        pages.clear();

        if (totalPages == 0)
            return;
        if (totalPages > 0 && startPageIndex + totalPages > pageUUIDs.size())
            return;

        for (int i = startPageIndex; i < pageUUIDs.size(); i++) {
            if (totalPages > 0 && pages.size() >= totalPages)
                return;
            try {
                File before_page_file = new File(dir, PAGE_FILE_PREFIX + pageUUIDs.get(i).toString() + BEFORE_QUILL_DATA_FILE_SUFFIX);
                File after_page_file = new File(dir, PAGE_FILE_PREFIX + pageUUIDs.get(i).toString() + QUILL_DATA_FILE_SUFFIX);
                if (before_page_file.exists() && after_page_file.exists()) {
                    before_page_file.delete();
                }
                if (before_page_file.exists() && !after_page_file.exists()) {
                    boolean page_rename_success;
                    page_rename_success = before_page_file.renameTo(after_page_file);
                    Log.d(TAG, "Page Rename : " + page_rename_success);
                }

                File auto = new File(dir, AUTO_PAGE_FILE_PREFIX + pageUUIDs.get(i).toString() + QUILL_DATA_FILE_SUFFIX);
                File origin = new File(dir, PAGE_FILE_PREFIX + pageUUIDs.get(i).toString() + QUILL_DATA_FILE_SUFFIX);
                File temp = new File(dir, PAGE_FILE_PREFIX + pageUUIDs.get(i).toString() + QUILL_DATA_FILE_SUFFIX_TEMP);
                File old = new File(dir, PAGE_FILE_PREFIX + pageUUIDs.get(i).toString() + QUILL_DATA_FILE_SUFFIX_OLD);

                if (auto.exists() && origin.exists()) {
                    Date auto_date = new Date(auto.lastModified());
                    Date origin_date = new Date(origin.lastModified());
                    int comparison = auto_date.compareTo(origin_date);
                    if (comparison < 0) {
                        fis = new FileInputStream(origin);
                        Log.d(TAG, "Page : origin");
                    } else {
                        fis = new FileInputStream(auto);
                        Log.d(TAG, "Page : auto");
                    }
                } else if (auto.exists() && !origin.exists()) {
                    fis = new FileInputStream(auto);
                    Log.d(TAG, "Page : auto1");
                } else if (!auto.exists() && origin.exists()) {
                    fis = new FileInputStream(origin);
                    Log.d(TAG, "Page : origin1");
                } else if (before_page_file.exists()) {
                    fis = new FileInputStream(before_page_file);
                    Log.d(TAG, "Page : before_index_file");
                } else if (temp.exists()) {
                    fis = new FileInputStream(temp);
                    Log.d(TAG, "Page : temp");
                } else if (old.exists()) {
                    fis = new FileInputStream(old);
                    Log.d(TAG, "Page : old");
                } else {
                    origin.createNewFile();
                    fis = new FileInputStream(origin);
                    Log.d(TAG, "Page : new Page");
                }

                buffer = new BufferedInputStream(fis);
                dataIn = new DataInputStream(buffer);
                Page page = new Page(dataIn, tagManager, dir);
                if (!page.getUUID().equals(pageUUIDs.get(i))) {
                    Storage.getInstance().LogError(TAG, "Page UUID mismatch.");
                    page.touch();
                }
                pages.add(page);
            } catch (EOFException e) {
                pages.add(Page.exceptionFixedPage);
                this.modified = true;
                this.auto_modified = true;
                continue;
            }
        }
        if (fis != null)
            fis.close();
        if (buffer != null)
            buffer.close();
        if (dataIn != null)
            dataIn.close();
//		}

    }

    private boolean doSaveBookInDirectory(BookDirectory dir, boolean autoSave) throws BookSaveException, IOException {

        if (autoSave) {
            return doAutoSaveBookInDirectory(dir);
        }

        if (!dir.isDirectory() && !dir.mkdir())
            throw new BookSaveException("Error creating directory " + dir.toString());
        FileOutputStream fos = null;
        BufferedOutputStream buffer = null;
        DataOutputStream dataOut = null;
        boolean useTempSave;
        File file;

        /**
         * Save Index ---- Start
         * */
        useTempSave = new File(dir, INDEX_FILE).exists() ? true : false;

        if (useTempSave) {
            file = new File(dir, INDEX_FILE_TEMP);
        } else {
            file = new File(dir, INDEX_FILE);
        }
        try {
            fos = new FileOutputStream(file);
            buffer = new BufferedOutputStream(fos);
            dataOut = new DataOutputStream(buffer);
            if (dataOut == null) {
                Log.e(TAG, "Normal save index fail");
                return false;
            }
            indexWriteToStream(dataOut, false);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        } finally {
            if (dataOut != null)
                dataOut.close();
            if (buffer != null)
                buffer.close();
            if (fos != null)
                fos.close();
        }
        if (useTempSave) {
            File originFile = new File(dir, INDEX_FILE);
            File originFileRename = new File(dir, INDEX_FILE_OLD);
            File tempFile = new File(dir, INDEX_FILE_TEMP);
            File tempFileRename = new File(dir, INDEX_FILE);

            if (!tryRename(originFile, originFileRename, tempFile, tempFileRename)) {
                Log.e(TAG, "Normal save index tryRename() fail");
                return false;
            }
        }
        /**
         * Save Index ---- End
         * */


        /**
         * Save Page ---- Start
         * */
        LinkedList<UUID> pageUUIDsInDir = dir.listPages();
        LinkedList<UUID> blobUUIDsInDir = dir.listBlobs();
        LinkedList<File> autoFilesInDir = dir.listAutos();
        for (Page page : pages) {
            pageUUIDsInDir.remove(page.getUUID());
            blobUUIDsInDir.removeAll(page.getBlobUUIDs());
            if (!page.isModified(false))
                continue;

            useTempSave = new File(dir, PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX).exists() ? true : false;
            if (useTempSave) {
                file = new File(dir, PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX_TEMP);
            } else {
                file = new File(dir, PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX);
            }

            try {
                fos = new FileOutputStream(file);
                buffer = new BufferedOutputStream(fos);
                dataOut = new DataOutputStream(buffer);
                if (dataOut == null) {
                    Log.e(TAG, "Normal save page fail [" + page.getUUID() + "]");
                    return false;
                }
                page.writeToStream(dataOut);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                return false;
            } finally {
                if (dataOut != null)
                    dataOut.close();
                if (buffer != null)
                    buffer.close();
                if (fos != null)
                    fos.close();
            }
            if (useTempSave) {
                File originFile = new File(dir, PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX);
                File originFileRename = new File(dir, PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX_OLD);
                File tempFile = new File(dir, PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX_TEMP);
                File tempFileRename = new File(dir, PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX);

                if (!tryRename(originFile, originFileRename, tempFile, tempFileRename)) {
                    Log.e(TAG, "Normal save page tryRename() fail");
                    return false;
                }
            }
        }
        /**
         * Save Page ---- End
         * */
        try {
            if (dataOut != null)
                dataOut.close();
            if (buffer != null)
                buffer.close();
            if (fos != null)
                fos.close();
        } catch (Exception e) {

        }

        for (UUID unused : pageUUIDsInDir) {
            file = dir.getFile(unused);
            if (Global.isDebug)
                Log.d(TAG, "Deleting unused page file: " + file.toString());
            file.delete();
        }
        for (UUID unused : blobUUIDsInDir) {
            file = dir.getFile(unused);
            if (Global.isDebug)
                Log.d(TAG, "Deleting unused blob file: " + file.toString());
            file.delete();
        }
        for (File auto : autoFilesInDir) {
            if (auto.exists()) {
                for (int i = 0; i < 10; i++) {
                    if (auto.delete()) {
                        break;
                    } else {
                        if (i == 9) {
                            return false;
                        }
                    }
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return true;
    }

    private boolean doAutoSaveBookInDirectory(BookDirectory dir) throws BookSaveException, IOException {
        if (!dir.isDirectory() && !dir.mkdir())
            throw new BookSaveException("Error creating directory " + dir.toString());
        FileOutputStream fos = null;
        BufferedOutputStream buffer = null;
        DataOutputStream dataOut = null;
        boolean useTempSave;
        File file;

        /**
         * Save Index ---- Start
         * */
        useTempSave = new File(dir, AUTO_INDEX_FILE).exists() ? true : false;

        if (useTempSave) {
            file = new File(dir, AUTO_INDEX_FILE_TEMP);
        } else {
            file = new File(dir, AUTO_INDEX_FILE);
        }
        try {
            fos = new FileOutputStream(file);
            buffer = new BufferedOutputStream(fos);
            dataOut = new DataOutputStream(buffer);
            if (dataOut == null) {
                Log.e(TAG, "Auto save index fail");
                return false;
            }
            indexWriteToStream(dataOut, true);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        } finally {
            if (dataOut != null)
                dataOut.close();
            if (buffer != null)
                buffer.close();
            if (fos != null)
                fos.close();
        }

        if (useTempSave) {

            File originFile = new File(dir, AUTO_INDEX_FILE);
            File originFileRename = new File(dir, AUTO_INDEX_FILE_OLD);
            File tempFile = new File(dir, AUTO_INDEX_FILE_TEMP);
            File tempFileRename = new File(dir, AUTO_INDEX_FILE);

            if (!tryRename(originFile, originFileRename, tempFile, tempFileRename)) {
                Log.e(TAG, "Auto save index tryRename() fail");
                return false;
            }
        }
        /**
         * Save Index ---- End
         * */

        for (Page page : pages) {
            if (!page.isModified(true))
                continue;
            {
                Log.d(TAG, "Real page save : " + page.getUUID());
                useTempSave = new File(dir, AUTO_PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX).exists() ? true : false;
                if (useTempSave) {
                    file = new File(dir, AUTO_PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX_TEMP);
                } else {
                    file = new File(dir, AUTO_PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX);
                }

                try {
                    fos = new FileOutputStream(file);
                    buffer = new BufferedOutputStream(fos);
                    dataOut = new DataOutputStream(buffer);
                    if (dataOut == null) {
                        Log.e(TAG, "Auto save page fail [" + page.getUUID() + "]");
                        return false;
                    }
                    page.writeToStream(dataOut);

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    return false;
                } finally {
                    if (dataOut != null)
                        dataOut.close();
                    if (buffer != null)
                        buffer.close();
                    if (fos != null)
                        fos.close();
                }
                if (useTempSave) {

                    File originFile = new File(dir, AUTO_PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX);
                    File originFileRename = new File(dir, AUTO_PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX_OLD);
                    File tempFile = new File(dir, AUTO_PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX_TEMP);
                    File tempFileRename = new File(dir, AUTO_PAGE_FILE_PREFIX + page.getUUID().toString() + QUILL_DATA_FILE_SUFFIX);

                    if (!tryRename(originFile, originFileRename, tempFile, tempFileRename)) {
                        Log.e(TAG, "Auto save page tryRename() fail");
                        return false;
                    }
                }
            }
        }
        try {
            if (dataOut != null)
                dataOut.close();
            if (buffer != null)
                buffer.close();
            if (fos != null)
                fos.close();
        } catch (Exception e) {

        }

        return true;
    }

    private LinkedList<UUID> loadIndex(DataInputStream dataIn) throws IOException, BookLoadException {
        LinkedList<UUID> pageUuidList = null;
        int version = dataIn.readInt();
        if (version == 4) {
            numPages = dataIn.readInt();
            pageUuidList = new LinkedList<UUID>();
            for (int i = 0; i < numPages; i++)
                pageUuidList.add(UUID.fromString(dataIn.readUTF()));
            currentPage = dataIn.readInt();
            title = dataIn.readUTF();
            ctime.set(dataIn.readLong());
            mtime.set(dataIn.readLong());
            uuid = UUID.fromString(dataIn.readUTF());
            setFilter(tagManager.loadTagSet(dataIn));
        } else
            throw new BookLoadException("Unknown version in load_index()");
        return pageUuidList;
    }

    protected void indexWriteToStream(DataOutputStream dataOut, boolean autoSave) throws IOException {
        if (Global.isDebug)
            Log.d(TAG, "Saving book index");
        dataOut.writeInt(4);
        dataOut.writeInt(pages.size());
        for (int i = 0; i < pages.size(); i++)
            dataOut.writeUTF(getPage(i).getUUID().toString());
        dataOut.writeInt(currentPage);
        dataOut.writeUTF(title);
        dataOut.writeLong(ctime.toMillis(false));
        if (isModified(autoSave))
            mtime.setToNow();
        dataOut.writeLong(mtime.toMillis(false));
        dataOut.writeUTF(uuid.toString());
        getFilter().write_to_stream(dataOut);
    }

    private File getPageFile(File dir, UUID uuid) {
        return new File(dir, PAGE_FILE_PREFIX + uuid.toString() + QUILL_DATA_FILE_SUFFIX);
    }

    public boolean tryRename(File originFile, File originFileRename, File tempFile, File tempFileRename) {

        File _originFile = originFile; //eg. index
        File _originFileRename = originFileRename; //eg. index.old
        File _tempFile = tempFile; //eg. index.temp
        File _tempFileRename = tempFileRename; //eg. index

        if (originFileRename.exists()) {
            for (int i = 0; i < 10; i++) {
                if (originFileRename.delete()) {
                    break;
                } else {
                    if (i == 9) {
                        return false;
                    }
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        for (int i = 0; i < 10; i++) {
            if (_originFile.renameTo(_originFileRename)) {
                break;
            } else {
                if (i == 9) {
                    return false;
                }
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < 10; i++) {
            if (_tempFile.renameTo(_tempFileRename)) {
                break;
            } else {
                if (i == 9) {
                    return false;
                }
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (originFileRename.exists()) {
            for (int i = 0; i < 10; i++) {
                if (originFileRename.delete()) {
                    break;
                } else {
                    if (i == 9) {
                        return false;
                    }
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    private boolean isPageOK(UUID uuid, File dir) throws IOException {

        File file = getPageFile(dir, uuid);
        FileInputStream fis = null;
        BufferedInputStream buffer = null;
        DataInputStream dataIn = null;
        try {
            fis = new FileInputStream(file);
            buffer = new BufferedInputStream(fis);
            dataIn = new DataInputStream(buffer);
            new Page(dataIn, tagManager, dir);

        } catch (Exception e) {
            ALog.debug(e.toString());
            return false;
        } finally {
            if (dataIn != null) dataIn.close();
            else if (buffer != null) buffer.close();
            else if (fis != null) fis.close();
        }

        return true;
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
}
