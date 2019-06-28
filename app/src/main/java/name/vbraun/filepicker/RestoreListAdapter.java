package name.vbraun.filepicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class RestoreListAdapter extends BaseAdapter {
	private static final int NUM_PER_PAGE = 5; // for showing items per page

	private List<RestoreItem> mList = new ArrayList<RestoreItem>();
	private int mTotalPage = 1;
	private int mCurrentPage = 1;

	public RestoreListAdapter(List<RestoreItem> list) {
		this.mList = list;
		setTotalPage();
	}

	@Override
	public int getCount() {
		return this.mList.size();
	}

	@Override
	public Object getItem(int position) {
		return this.mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return null;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		setTotalPage();
	}

	public int getTotalPage() {
		return this.mTotalPage;
	}

	public int getCurrentPage() {
		return mCurrentPage;
	}

	public void setCurrentPage(int currentPage) {
		if (currentPage > mTotalPage) { // only one page
			currentPage = 1;
		} else if (currentPage <= 0) {
			currentPage = mTotalPage;
		}
		this.mCurrentPage = currentPage;
	}

	public List<RestoreItem> getCurrentPageList() {
		List<RestoreItem> currentList = new ArrayList<RestoreItem>();
		currentList.clear();
		for (int i = 0; i < NUM_PER_PAGE; i++) {
			if (getRealPosition(i) >= mList.size())
				break;
			currentList.add(mList.get(getRealPosition(i)));
		}

		return currentList;
	}

	public void sortByNameAscending() {
		Collections.sort(mList, new NameIgnoreCaseComparator());
	}

	public void sortByNameDescending() {
		Collections.sort(mList, new NameIgnoreCaseComparator());
		Collections.reverse(mList);
	}

	public void sortByDateAscending() {
		Collections.sort(mList, new DateComparator());
	}

	public void sortByDateDescending() {
		Collections.sort(mList, new DateComparator());
		Collections.reverse(mList);
	}

	public void sortBySizeAscending() {
		Collections.sort(mList, new SizeComparator());
	}

	public void sortBySizeDescending() {
		Collections.sort(mList, new SizeComparator());
		Collections.reverse(mList);
	}

	private int getRealPosition(int position) {
		return (((mCurrentPage - 1) * NUM_PER_PAGE) + position);
	}

	private void setTotalPage() {
		if (mList.size() == 0) {
			this.mTotalPage = 1;
		} else if (mList.size() % NUM_PER_PAGE != 0) {
			this.mTotalPage = (mList.size() / NUM_PER_PAGE) + 1;
		} else {
			this.mTotalPage = mList.size() / NUM_PER_PAGE;
		}
	}

	private class NameIgnoreCaseComparator implements Comparator<RestoreItem> {
		public int compare(RestoreItem o1, RestoreItem o2) {
			return o1.getFileName().toLowerCase().compareTo(o2.getFileName().toLowerCase());
		}
	}

	private class DateComparator implements Comparator<RestoreItem> {
		public int compare(RestoreItem o1, RestoreItem o2) {
			return Long.valueOf(o1.getFileDate()).compareTo(Long.valueOf(o2.getFileDate()));
		}
	}

	private class SizeComparator implements Comparator<RestoreItem> {
		public int compare(RestoreItem o1, RestoreItem o2) {
			return Long.valueOf(o1.getFileSize()).compareTo(Long.valueOf(o2.getFileSize()));
		}
	}

}
