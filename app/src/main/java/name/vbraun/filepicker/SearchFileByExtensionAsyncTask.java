package name.vbraun.filepicker;

import android.os.AsyncTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SearchFileByExtensionAsyncTask extends AsyncTask<String, Void, Void> {
    public AsyncTaskResult<List<File>> searchFinishCallback = null;

    private List<File> searchResult = new ArrayList<>();
    private String fileType;

    /*
     * (non-Javadoc)
     *
     * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
     *
     * arg0[0] : search directory file path arg0[1] : search file type
     */
    @Override
    protected Void doInBackground(String... arg0) {
        File file = new File(arg0[0]);
        fileType = arg0[1];

        if (!file.exists())
            return null;

        searchResult.clear();
        searchFiles(file);
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (searchFinishCallback != null)
            searchFinishCallback.taskFinish(searchResult);
    }

    private void searchFiles(File file) {
        File[] the_Files = file.listFiles();

        if (the_Files == null)
            return;

        for (File tempF : the_Files) {
            if (tempF.isDirectory()) {
                if (!tempF.isHidden())
                    searchFiles(tempF);
            } else {
                try {
                    if (tempF.getName().endsWith(fileType)) {
                        searchResult.add(tempF);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
