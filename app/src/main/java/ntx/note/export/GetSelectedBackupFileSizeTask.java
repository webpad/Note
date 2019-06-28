package ntx.note.export;

import android.os.AsyncTask;

import java.util.List;
import java.util.UUID;

import name.vbraun.filepicker.AsyncTaskResult;
import ntx.note.data.Book;

/**
 * Created by karote on 2018/12/20.
 */

public class GetSelectedBackupFileSizeTask extends AsyncTask<Void, Void, Long> {
    private List<UUID> mList;

    public AsyncTaskResult<Long> asyncTaskResult;

    public GetSelectedBackupFileSizeTask(List<UUID> list) {
        this.mList = list;
    }

    @Override
    protected Long doInBackground(Void... voids) {
        long totalSize = 0;
        for (UUID uuid : mList) {
            if (isCancelled()) {
                totalSize = 0;
                return totalSize;
            }
            Book tempBook = new Book(uuid, false);
            totalSize += tempBook.getBookSizeInStorage();
        }
        return totalSize;
    }

    @Override
    protected void onPostExecute(Long result) {
        if (asyncTaskResult != null)
            asyncTaskResult.taskFinish(result);
    }
}
