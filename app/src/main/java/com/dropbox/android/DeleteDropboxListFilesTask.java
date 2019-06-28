package com.dropbox.android;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import ntx.note.CallbackEvent;
import ntx.note.export.AlertDialogFragment;
import ntx.note.export.InterruptibleTwoLevelProcessingDialogFragment;
import ntx.note2.R;

public class DeleteDropboxListFilesTask extends AsyncTask<Void, String, Integer> {
    private final static int RETURN_CODE_SUCCESS = 0;
    private final static int RETURN_CODE_FAIL = 1;
    private final static int RETURN_CODE_INTERRUPTED = 2;

    private Context mContext;
    private List<FileMetadata> mFileMetadataList;
    private int mRetCode = RETURN_CODE_SUCCESS;

    private InterruptibleTwoLevelProcessingDialogFragment mDownloadingDialogFragment;
    private String[] mProgressArray = new String[3]; // {ProgressingItemName, Percentage, Progress}

    private EventBus mEventBus;

    public DeleteDropboxListFilesTask(Context activityContext, List<FileMetadata> fileMetadataList) {
        this.mContext = activityContext;
        this.mFileMetadataList = fileMetadataList;
        this.mEventBus = EventBus.getDefault();
    }

    @Override
    protected void onPreExecute() {
        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();

        mDownloadingDialogFragment = InterruptibleTwoLevelProcessingDialogFragment.newInstance(
                mContext.getString(R.string.deleting),
                R.drawable.ic_delete,
                mFileMetadataList.size(), true);

        mDownloadingDialogFragment.setOnInterruptButtonClickListener(new InterruptibleTwoLevelProcessingDialogFragment.OnInterruptButtonClickListener() {
            @Override
            public void onClick() {
                DeleteDropboxListFilesTask.this.cancel(true);

                showAlertDialog(mContext.getResources().getString(R.string.interrupt), R.drawable.writing_ic_error);

                CallbackEvent callbackEvent = new CallbackEvent();
                callbackEvent.setMessage(CallbackEvent.DELETE_DROPBOX_FILES_INTERRUPTED);
                mEventBus.post(callbackEvent);
            }
        });

        ft.replace(R.id.alert_dialog_container, mDownloadingDialogFragment,
                InterruptibleTwoLevelProcessingDialogFragment.class.getSimpleName()).commit();
    }

    @Override
    protected Integer doInBackground(Void... params) {
        for (int i = 0; i < mFileMetadataList.size(); i++) {
            if (isCancelled()) {
                mRetCode = RETURN_CODE_INTERRUPTED;
                return mRetCode;
            }

            FileMetadata metadata = mFileMetadataList.get(i);
            mProgressArray[0] = metadata.getName();
            double temp = (float) (i + 1) / (float) mFileMetadataList.size();
            mProgressArray[1] = String.valueOf((int) (Math.floor(temp * 100)));
            mProgressArray[2] = String.valueOf(i + 1);
            publishProgress(mProgressArray[0], mProgressArray[1], mProgressArray[2]);

            try {
                DropboxClientFactory.getClient().files().deleteV2(metadata.getPathLower());
                mRetCode = RETURN_CODE_SUCCESS;
            } catch (DbxException e) {
                mRetCode = RETURN_CODE_FAIL;
            }
        }
        return mRetCode;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        mDownloadingDialogFragment.setProgressItemName(values[0]);
        mDownloadingDialogFragment.updateLv1Progress(Integer.valueOf(values[1]));
        mDownloadingDialogFragment.updateLv2Progress(Integer.valueOf(values[2]));
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
                callbackEvent.setMessage(CallbackEvent.DELETE_DROPBOX_FILES_SUCCESS);
                break;
            case RETURN_CODE_FAIL:
                msgStr = mContext.getResources().getString(R.string.fail);
                alertIconResId = R.drawable.writing_ic_error;
                callbackEvent.setMessage(CallbackEvent.DELETE_DROPBOX_FILES_FAIL);
                break;
            case RETURN_CODE_INTERRUPTED:
            default:
                return;
        }

        showAlertDialog(msgStr, alertIconResId);
        mEventBus.post(callbackEvent);
    }

    private void showAlertDialog(String msgStr, int iconId) {
        FragmentTransaction ft = ((Activity) mContext).getFragmentManager().beginTransaction();
        AlertDialogFragment alertDialogFragment;
        alertDialogFragment = AlertDialogFragment.newInstance(msgStr, iconId, true, null);
        ft.replace(R.id.alert_dialog_container, alertDialogFragment, AlertDialogFragment.class.getSimpleName()).commit();
    }
}