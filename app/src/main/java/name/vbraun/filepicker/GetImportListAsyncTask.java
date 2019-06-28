package name.vbraun.filepicker;

import android.os.AsyncTask;

import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class GetImportListAsyncTask extends AsyncTask<Void, Void, Void> {
    private static final String NOTEBOOK_DIRECTORY_PREFIX = "notebook_";

    public AsyncTaskResult<List<ImportItem>> groupSameUuidListFinishCallback;

    private List<RestoreItem> mList = new ArrayList<>();
    private List<ImportItem> mResult = new ArrayList<>();
    private int mEntryCount;

    public GetImportListAsyncTask(List<RestoreItem> list) {
        this.mList.clear();
        this.mList.addAll(list);
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        if (mList.isEmpty())
            return null;

        List<List<ImportItem>> groupItemList = new ArrayList<>();
        List<ImportItem> singleItemList = new ArrayList<>();

        HashMap<String, List<RestoreItem>> hashMap = new HashMap<>();
        List<String> notOnlyOneList = new ArrayList<>();

        for (RestoreItem item : mList) { // make a HashMap<UUID, List<>> and a notOnlyOneList<UUID>
            File file = new File(item.getFilePath());
            try {
                String fUuid = getUuidFromFile(file);
                if (!hashMap.containsKey(fUuid)) {
                    List<RestoreItem> newEmptyList = new ArrayList<>();
                    item.setPages(mEntryCount - 1);
                    newEmptyList.add(item);
                    hashMap.put(fUuid, newEmptyList);
                } else {
                    item.setPages(mEntryCount - 1);
                    hashMap.get(fUuid).add(item);

                    if (!notOnlyOneList.contains(fUuid))
                        notOnlyOneList.add(fUuid);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < notOnlyOneList.size(); i++) { // pick up the item which uuid is not only one from HashMap to make group list
            String key = notOnlyOneList.get(i);

            List<ImportItem> newItems = new ArrayList<>();
            int size = hashMap.get(key).size();
            for (RestoreItem item : hashMap.get(key)) {
                ImportItem newItem = new ImportItem(i, size, key, item);
                newItems.add(newItem);
            }
            groupItemList.add(newItems);
            hashMap.remove(key);
        }

        for (String key : hashMap.keySet()) { // make single list by rest HashMap
            singleItemList.add(new ImportItem(key, hashMap.get(key).get(0)));
        }

        // make import list for return back
        mResult.clear();
        for (int i = 0; i < groupItemList.size(); i++) {
            int size = groupItemList.get(i).size();
            String uuid = groupItemList.get(i).get(0).getItemUuid();

            mResult.add(new ImportItem(i, size, uuid)); // add group header first
            mResult.addAll(groupItemList.get(i));
        }
        mResult.addAll(singleItemList);

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (groupSameUuidListFinishCallback != null)
            groupSameUuidListFinishCallback.taskFinish(mResult);
    }

    private String getUuidFromFile(File file) throws IOException {
        TarInputStream tis = null;
        UUID uuid = null;

        // check file and get uuid
        try {
            tis = new TarInputStream(new BufferedInputStream(new FileInputStream(file)));
            TarEntry entry;
            mEntryCount = 0;
            while ((entry = tis.getNextEntry()) != null) {
                if (entry.getName() == null)
                    throw new IOException("Incorrect book archive file");
                if (uuid == null) {
                    uuid = getBookUuidFromDirectoryName(entry.getName());
                    if (uuid == null)
                        throw new IOException("Incorrect book archive file");
                } else if (!uuid.equals(getBookUuidFromDirectoryName(entry.getName())))
                    throw new IOException("Incorrect book archive file");

                mEntryCount++;
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

        return uuid.toString();
    }

    private UUID getBookUuidFromDirectoryName(String name) {
        if (!name.startsWith(NOTEBOOK_DIRECTORY_PREFIX))
            return null;
        int n = NOTEBOOK_DIRECTORY_PREFIX.length();
        String uuid = name.substring(n, n + 36);
        return UUID.fromString(uuid);
    }
}
