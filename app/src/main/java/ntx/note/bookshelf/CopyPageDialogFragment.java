package ntx.note.bookshelf;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import name.vbraun.view.write.Page;
import ntx.note.CallbackEvent;
import ntx.note.NoteWriterActivity;
import ntx.note.bookshelf.BookshelfListAdapter.CheckableBook;
import ntx.note.data.Book;
import ntx.note.data.Bookshelf;
import ntx.note.export.AlertDialogButtonClickListener;
import ntx.note.export.AlertDialogFragment;
import ntx.note2.R;
import utility.CustomDialogFragment;

import static name.vbraun.view.write.HandwriterView.PAGE_REDRAW_TIME_THRESHOLD;

public class CopyPageDialogFragment extends CustomDialogFragment {
    private Activity mActivity;

    private RadioGroup mRadioGroupCopyDestination;
    private LinearLayout mLayoutCopyToOtherNote;
    private TextView mTvOtherNoteHint;
    private BookshelfListAdapter mBookshelfListAdapter;
    private TextView mTvBookshelfListCurrentPage;
    private TextView mTvBookshelfListTotalPage;
    private List<LinearLayout> mBookshelfList = new ArrayList<>();

    private Book mCurrentBook;
    private boolean mIsAllBookListInitialized = false;
    private InitAllBookListAsyncTask mInitAllBookListAsyncTask;
    private AlertDialogFragment mAlertDialogFragment;
    private Button mBtnOk;

    private EventBus mEventBus = EventBus.getDefault();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mActivity = getActivity();
        mCurrentBook = Bookshelf.getInstance().getCurrentBook();
        View v = inflater.inflate(R.layout.file_copy_to_xxx_dialog, container, false);
        initView(v);
        return v;
    }

    private void initView(View v) {
        mLayoutCopyToOtherNote = (LinearLayout) v.findViewById(R.id.layout_copy_to_other_note);
        mTvOtherNoteHint = (TextView) v.findViewById(R.id.tv_other_note_list_hint);

        Button btnCancel = (Button) v.findViewById(R.id.btn_cancel);
        mBtnOk = (Button) v.findViewById(R.id.btn_ok);
        btnCancel.setOnClickListener(onBtnClickListener);
        mBtnOk.setOnClickListener(onBtnClickListener);

        mRadioGroupCopyDestination = (RadioGroup) v.findViewById(R.id.radio_group_copy_destination);
        mRadioGroupCopyDestination.setOnCheckedChangeListener(onCopyDestinationCheckedChangeListener);

        ImageButton btnPageUP = (ImageButton) v.findViewById(R.id.btn_bookshelf_list_page_up);
        ImageButton btnPageDown = (ImageButton) v.findViewById(R.id.btn_bookshelf_list_page_down);
        btnPageUP.setOnClickListener(onPageBtnClickListener);
        btnPageDown.setOnClickListener(onPageBtnClickListener);

        LinearLayout item1 = (LinearLayout) v.findViewById(R.id.bookshelf_list_item1);
        LinearLayout item2 = (LinearLayout) v.findViewById(R.id.bookshelf_list_item2);
        LinearLayout item3 = (LinearLayout) v.findViewById(R.id.bookshelf_list_item3);
        LinearLayout item4 = (LinearLayout) v.findViewById(R.id.bookshelf_list_item4);
        LinearLayout item5 = (LinearLayout) v.findViewById(R.id.bookshelf_list_item5);
        LinearLayout item6 = (LinearLayout) v.findViewById(R.id.bookshelf_list_item6);
        mBookshelfList.add(item1);
        mBookshelfList.add(item2);
        mBookshelfList.add(item3);
        mBookshelfList.add(item4);
        mBookshelfList.add(item5);
        mBookshelfList.add(item6);
        for (int i = 0; i < mBookshelfList.size(); i++) {
            mBookshelfList.get(i).setTag(i);
            mBookshelfList.get(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBookshelfListAdapter.setListItemCheck((int) v.getTag());
                    updateBookshelfListView();
                }
            });
        }

        mTvBookshelfListCurrentPage = (TextView) v.findViewById(R.id.tv_bookshelf_list_current_page);
        mTvBookshelfListTotalPage = (TextView) v.findViewById(R.id.tv_bookshelf_list_total_page);
    }

    private Button.OnClickListener onBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            dismiss();
            if (R.id.btn_cancel == view.getId())
                return;

            if (R.id.btn_copy_to_current_note == mRadioGroupCopyDestination.getCheckedRadioButtonId())
                copyCurrentPageToCurrentNote();
            else
                copyCurrentPageToOtherNote();
        }
    };

    private ImageButton.OnClickListener onPageBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int currentPage = mBookshelfListAdapter.getCurrentPage();
            if (R.id.btn_bookshelf_list_page_down == view.getId())
                currentPage++;
            else
                currentPage--;

            mBookshelfListAdapter.setCurrentPage(currentPage);
            mBookshelfListAdapter.notifyDataSetChanged();
            updateBookshelfListView();
        }
    };

    private RadioGroup.OnCheckedChangeListener onCopyDestinationCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (R.id.btn_copy_to_other_note == checkedId) {
                mLayoutCopyToOtherNote.setVisibility(View.VISIBLE);

                if (!mIsAllBookListInitialized) {
                    mBtnOk.setAlpha(0.2f);
                    mBtnOk.setEnabled(false);
                    initAllBookList();
                }
            } else {
                mBtnOk.setAlpha(1.0f);
                mBtnOk.setEnabled(true);
                mLayoutCopyToOtherNote.setVisibility(View.GONE);
            }
        }
    };

    private void initAllBookList() {
        if (mInitAllBookListAsyncTask == null) {
            mInitAllBookListAsyncTask = new InitAllBookListAsyncTask();
            mInitAllBookListAsyncTask.execute();
        }

    }

    private void updateBookshelfListView() {
        for (LinearLayout layout : mBookshelfList) {
            layout.setVisibility(View.INVISIBLE);
        }

        if (!mIsAllBookListInitialized) {
            mBtnOk.setAlpha(0.2f);
            mBtnOk.setEnabled(false);
            return;
        }

        if (mBookshelfListAdapter.getCount() == 0) {
            mTvOtherNoteHint.setText(getString(R.string.other_note_empty_hint));
            return;
        } else
            mTvOtherNoteHint.setVisibility(View.GONE);

        mTvBookshelfListCurrentPage.setText(String.valueOf(mBookshelfListAdapter.getCurrentPage()));
        mTvBookshelfListTotalPage.setText(String.valueOf(mBookshelfListAdapter.getTotalPage()));

        List<CheckableBook> currentPageList = mBookshelfListAdapter.getCurrentPageList();
        for (int i = 0; i < currentPageList.size(); i++) {
            TextView bookTitle = (TextView) mBookshelfList.get(i).findViewById(R.id.tv_book_title);
            bookTitle.setText(currentPageList.get(i).getTitle());
            mBookshelfList.get(i).setSelected(currentPageList.get(i).isChecked());
            mBookshelfList.get(i).setVisibility(View.VISIBLE);
        }
    }

    private void copyCurrentPageToCurrentNote() {
        NoteWriterActivity.setNoteEdited(true);
        mCurrentBook.cloneCurrentPageToNextPage();
        if (mActivity.getClass().getSimpleName().equals(NoteWriterActivity.class.getSimpleName())) {
            ((NoteWriterActivity) mActivity).updatePageNumber();

            CallbackEvent event = new CallbackEvent();
            if (mCurrentBook.currentPage().objectsDrawTimePredict() > PAGE_REDRAW_TIME_THRESHOLD) {
                event.setMessage(CallbackEvent.PAGE_DRAW_TASK_HEAVY);
            } else {
                event.setMessage(CallbackEvent.PAGE_DRAW_TASK_LIGHT);
                showAlertMessageDialog(mActivity.getResources().getString(R.string.successful), R.drawable.writing_ic_successful, true);
            }
            mEventBus.post(event);

            ((NoteWriterActivity) mActivity).switchToPage(mCurrentBook.currentPage(), true);
        }
    }

    private void copyCurrentPageToOtherNote() {
        showAlertMessageDialog(mActivity.getResources().getString(R.string.copying), null, false);

        new CopyPageToNoteAsyncTask().execute();
    }

    private class InitAllBookListAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            List<Book> allBookList = new ArrayList<>(Bookshelf.getInstance().getBookList());
            for (Book book : allBookList) {
                if (book.getUUID().equals(mCurrentBook.getUUID())) {
                    allBookList.remove(book);
                    break;
                }
            }
            mBookshelfListAdapter = new BookshelfListAdapter(allBookList);
            mBookshelfListAdapter.setCurrentPage(1);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mIsAllBookListInitialized = true;
            mBtnOk.setAlpha(1.0f);
            mBtnOk.setEnabled(true);
            updateBookshelfListView();
        }
    }

    private class CopyPageToNoteAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Page currentPage = mCurrentBook.currentPage();
            CheckableBook selectedItem = (CheckableBook) mBookshelfListAdapter.getSelectedItem();
            if (selectedItem == null)
                return null;
            Book otherNote = new Book(selectedItem.getUUID(), true);
            otherNote.clonePageTo(currentPage, otherNote.pagesSize() - 1, false);
            otherNote.save();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            showAlertMessageDialog(mActivity.getResources().getString(R.string.successful), R.drawable.writing_ic_successful, true);
        }
    }

    private void showAlertMessageDialog(String message, @Nullable Integer resId, boolean enableButton) {
        if (mAlertDialogFragment == null) {
            mAlertDialogFragment = AlertDialogFragment.newInstance(message, resId, enableButton, null);

            FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
            mAlertDialogFragment.registerAlertDialogButtonClickListener((AlertDialogButtonClickListener) mActivity, CopyPageDialogFragment.class.getSimpleName());
            ft.replace(R.id.alert_dialog_container, mAlertDialogFragment, AlertDialogFragment.class.getSimpleName()).commit();
        } else {
            mAlertDialogFragment.updateIcon(resId);
            mAlertDialogFragment.updateAlertMessage(message);
            mAlertDialogFragment.enablePositiveButton(enableButton);
        }
    }
}
