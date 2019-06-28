package com.dropbox.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.dropbox.core.v2.users.SpaceUsage;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ntx.note.Global;
import ntx.note.bookshelf.DropboxNoteData;
import ntx.note2.R;

import static android.content.Context.MODE_PRIVATE;

public class Dropbox {

    private Activity mActivity;

    private final static String EXTRA_PATH = "FilesActivity_Path";
    private List<Metadata> mFiles;
    private List<DropboxNoteData> mDropboxNoteList = new ArrayList<>();
    private GetCurrentAccountTask mGetCurrentAccountTask;

    public Dropbox(Activity activity) {
        mActivity = activity;
    }

    public interface OnMetadataFileListLoadedListener {
        void onMetaFileListLoaded(List<Metadata> metadataFileList);
    }

    private OnMetadataFileListLoadedListener mMetadataFileListLoadedCallback = null;

    public void registerOnMetaFileListLoadedListener(OnMetadataFileListLoadedListener listener) {
        this.mMetadataFileListLoadedCallback = listener;
    }

    public void unregisterOnMetaFileListLoadedListener() {
        this.mMetadataFileListLoadedCallback = null;
    }

    public interface TrySignInFinishListener {
        void onTryFinished(boolean isSignIn);
    }

    private TrySignInFinishListener mTrySignInFinishCallback = null;

    public void logIn() {
        Auth.startOAuth2Authentication(mActivity, Global.APP_KEY);
    }

    public void logOut() {
        cleanSharedPreferences();

        // Android 4.0 之后不能在主线程中请求HTTP请求
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DropboxClientFactory.getClient().auth().tokenRevoke();
                } catch (DbxException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.dropbox.com/logout"));
        mActivity.startActivity(browserIntent);
    }

    public void logOutWithAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setMessage(mActivity.getResources().getString(R.string.dropbox_logout) + " ?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        cleanSharedPreferences(); // Clear our stored keys

                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.dropbox.com/logout"));
                        mActivity.startActivity(browserIntent);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        builder.show();
    }

    public void getDropboxFileName() {

        String mPath = mActivity.getIntent().getStringExtra(EXTRA_PATH);
        mPath = mPath == null ? "" : mPath;
        mDropboxNoteList.clear();

        new ListFolderTask(DropboxClientFactory.getClient(), new ListFolderTask.Callback() {
            @Override
            public void onDataLoaded(ListFolderResult result) {
                mFiles = Collections.unmodifiableList(new ArrayList<>(result.getEntries()));
                if (mMetadataFileListLoadedCallback != null)
                    mMetadataFileListLoadedCallback.onMetaFileListLoaded(mFiles);

                for (int i = 0; i < mFiles.size(); i++) {
                    mDropboxNoteList.add(parseJSON(mFiles.get(i).toStringMultiline()));
                }
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        }).execute(mPath);
    }

    public void unRegisterTrySignInFinishListener() {
        mTrySignInFinishCallback = null;
    }

    public void trySignIn(TrySignInFinishListener listener) {
        mTrySignInFinishCallback = listener;

        SharedPreferences prefs = mActivity.getSharedPreferences(Global.ACCESS_KEY, MODE_PRIVATE);
        String accessToken = prefs.getString(Global.ACCESS_TOKEN, "");
        if (accessToken.isEmpty()) {
            accessToken = Auth.getOAuth2Token();
            if (accessToken == null) {
                if (mTrySignInFinishCallback != null)
                    mTrySignInFinishCallback.onTryFinished(false);
            } else {
                prefs.edit().putString(Global.ACCESS_TOKEN, accessToken).apply();
                initAndLoadData(accessToken);
            }
        } else {
            initAndLoadData(accessToken);
        }
    }

    private void cleanSharedPreferences() {
        // Clear our stored keys
        SharedPreferences prefs = mActivity.getSharedPreferences(Global.ACCESS_KEY, MODE_PRIVATE);
        prefs.edit().putString(Global.ACCESS_TOKEN, "")
                .putString(Global.ACCESS_USER_ID, "")
                .putString(Global.ACCESS_USER_Email, "")
                .apply();
    }

    private void sharedPreferencesStoreUserId() {
        String uid = Auth.getUid();
        SharedPreferences prefs = mActivity.getSharedPreferences(Global.ACCESS_KEY, MODE_PRIVATE);
        String storedUid = prefs.getString(Global.ACCESS_USER_ID, null);
        if (uid != null && !uid.equals(storedUid)) {
            prefs.edit().putString(Global.ACCESS_USER_ID, uid).apply();
        }
    }

    private void sharedPreferencesStoreUserEmail(String email) {
        SharedPreferences prefs = mActivity.getSharedPreferences(Global.ACCESS_KEY, MODE_PRIVATE);
        prefs.edit().putString(Global.ACCESS_USER_Email, email).apply();
    }

    private void sharedPreferencesStoreUserFreeSpace(long freeSpace) {
        SharedPreferences prefs = mActivity.getSharedPreferences(Global.ACCESS_KEY, MODE_PRIVATE);
        prefs.edit().putLong(Global.ACCESS_USER_USED_FREE_SPACE, freeSpace).apply();
    }

    private void initAndLoadData(String accessToken) {
        if (accessToken.isEmpty())
            return;
        DropboxClientFactory.init(accessToken);
        loadData();
        sharedPreferencesStoreUserId();
    }

    private void loadData() {
        if (mGetCurrentAccountTask == null) {
            runNewGetCurrentAccountTask();
        } else {
            if (AsyncTask.Status.FINISHED == mGetCurrentAccountTask.getStatus()) {
                mGetCurrentAccountTask = null;
                runNewGetCurrentAccountTask();
            }
        }
    }

    private void runNewGetCurrentAccountTask() {
        mGetCurrentAccountTask = new GetCurrentAccountTask(DropboxClientFactory.getClient(), new GetCurrentAccountTask.Callback() {
            @Override
            public void onComplete(FullAccount result, SpaceUsage spaceUsage) {
                sharedPreferencesStoreUserEmail(result.getEmail());
                long usedSize = spaceUsage.getUsed();
                long allocatedSize = spaceUsage.getAllocation().getIndividualValue().getAllocated();
                long freeSize = allocatedSize - usedSize;
                sharedPreferencesStoreUserFreeSpace(freeSize);
                if (mTrySignInFinishCallback != null)
                    mTrySignInFinishCallback.onTryFinished(true);
            }

            @Override
            public void onError(Exception e) {
                Log.e(getClass().getName(), "Failed to get account details.", e);
                if (mTrySignInFinishCallback != null)
                    mTrySignInFinishCallback.onTryFinished(false);
            }
        });
        mGetCurrentAccountTask.execute();
    }

    private DropboxNoteData parseJSON(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, DropboxNoteData.class);
    }

    public List<DropboxNoteData> getDropboxNoteList() {
        return mDropboxNoteList;
    }

    public List<Metadata> getFiles() {
        return mFiles;
    }
}
