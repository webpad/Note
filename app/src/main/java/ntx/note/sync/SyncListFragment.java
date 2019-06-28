package ntx.note.sync;

import ntx.note2.R;
import android.app.ListFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public class SyncListFragment extends ListFragment {
	private final static String TAG = "SyncListFragment";

	private SyncData data = null;
	private SyncTask task = null;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
	}
	
	protected void setAccount(NoteAccount account) {
		if (data != null && data.getAccount().equals(account))
			return;
		SharedPreferences syncPrefs = getActivity().getSharedPreferences(SyncActivity.SYNC_PREFERENCES,
				Context.MODE_PRIVATE);
		SyncStatusFragment status = (SyncStatusFragment) 
				getFragmentManager().findFragmentById(R.id.sync_status_fragment);
		if (status != null) {
			status.setAccount(account);
		}
		data = new SyncData(syncPrefs, account);
		runBackgroundTask();
	}
	
	protected void setData(SyncData data) {
		this.data = data;
		setListAdapter(new SyncDataAdapter(getActivity(), R.layout.sync_item, data));		
	}
	
	protected void runBackgroundTask() {
		updateMenu(false);
		task = new SyncTask(getActivity());
		task.execute(data);
		task.setSyncListFragment(this);
		SyncStatusFragment status = (SyncStatusFragment) 
				getFragmentManager().findFragmentById(R.id.sync_status_fragment);
		if (status != null) {
			task.setSyncStatusFragment(status);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (task != null) {
			task.setSyncListFragment(this);
			SyncStatusFragment status = (SyncStatusFragment) 
					getFragmentManager().findFragmentById(R.id.sync_status_fragment);
			if (status != null) {
				task.setSyncStatusFragment(status);
			}
		}
	}
	
	@Override
	public void onPause() {
		if (task != null) {
			task.setSyncStatusFragment(null);
			task.setSyncListFragment(null);
		}
		super.onPause();
	}
	
	/**
	 * set menu items enabled / disabled depending on current state
	 */
	protected void updateMenu(boolean finished) {
		SyncActivity activity = (SyncActivity) getActivity();
		if (activity == null)
			return;
		MenuItem sync = activity.getSyncMenuItem();
		if (sync == null)
			return;
		sync.setEnabled(finished);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		SyncData.SyncItem item = data.get(position);
		item.cycleAction();
		Log.e(TAG, "click: "+item.getAction().toString());
		SyncDataAdapter adapter = (SyncDataAdapter) getListAdapter();
		adapter.notifyDataSetChanged();
		super.onListItemClick(l, v, position, id);
	}
}
