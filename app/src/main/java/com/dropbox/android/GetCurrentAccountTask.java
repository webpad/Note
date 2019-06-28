package com.dropbox.android;

import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;
import com.dropbox.core.v2.users.SpaceUsage;

/**
 * Async task for getting user account info
 */
class GetCurrentAccountTask extends AsyncTask<Void, Void, Void> {

    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;

    private FullAccount mFullAccount;
    private SpaceUsage mSpaceUsage;

    public interface Callback {
        void onComplete(FullAccount result, SpaceUsage spaceUsage);
        void onError(Exception e);
    }

    GetCurrentAccountTask(DbxClientV2 dbxClient, Callback callback) {
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onComplete(mFullAccount, mSpaceUsage);
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            mFullAccount = mDbxClient.users().getCurrentAccount();
            mSpaceUsage = mDbxClient.users().getSpaceUsage();
        } catch (DbxException e) {
            mException = e;
        }
        return null;
    }
}
