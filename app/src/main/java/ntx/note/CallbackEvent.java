package ntx.note;

public class CallbackEvent {
    public final static String SHOW_SETTING_CALIBRATION_DIALOG = "show_setting_calibration_dialog";
    public final static String SWITCH_VERTICAL_TOOLBAR = "switch_vertical_toolbar";
    public final static String SHOW_CLEAN_DIALOG = "show_clean_dialog";
    public final static String SEARCH_NOTE = "search_note";
    public final static String SAVE_NOTE = "save_note";
    public final static String SAVE_FAIL = "save_fail";
    public final static String SAVE_COMPLETE = "saved_complete";
    public final static String PREV_PAGE = "prev_page";
    public final static String NEXT_PAGE = "next_page";
    public final static String SEEKBAR_PAGE = "seerbar_page";
    public final static String RENAME_NOTE = "rename_note";
    public final static String RENAME_NOTE_DONE = "rename_note_done";
    public final static String UPDATE_PAGE_TITLE = "update_page_title";
    public final static String COPY_CURRENT_PAGE_TO = "copy_current_page_to";
    public final static String BACKUP_NOTE = "backup_note";
    public final static String BACKUP_NOTE_SUCCESS = "backup_note_success";
    public final static String BACKUP_NOTE_ERROR = "backup_note_error";
    public final static String BACKUP_NOTE_INTERRUPT = "backup_note_interrupt";
    public final static String BACKUP_NOTE_EMAIL = "backup_note_email";
    public final static String RESTORE_NOTE = "restore_note";
    public final static String INFO_NOTE = "info_note";
    public final static String RESTORE_NOTE_SUCCESS = "restore_note_success";
    public final static String RESTORE_NOTE_ERROR = "restore_note_error";
    public final static String CONVERT_NOTE = "convert_page";
    public final static String CONVERT_NOTE_SUCCESS = "convert_page_success";
    public final static String CONVERT_NOTE_ERROR = "convert_page_error";
    public final static String CONVERT_NOTE_INTERRUPT = "convert_page_interrupt";
    public final static String CONVERT_NOTE_EMAIL = "convert_page_email";
    public final static String PAGE_TAG_SETTING = "page_tag_setting";
    public final static String SEEKBAR_PROGRESS_INFO = "seekbar_progress_info";
    public final static String DO_DRAW_VIEW_INVALIDATE = "do_draw_view_invalidate";
    public final static String MORE_SORT = "more_sort";
    public final static String MORE_PREVIEW = "more_preview";
    public final static String MORE_IMPORT = "more_import";
    public final static String IMPORT_NOTE_SUCCESS = "import_note_success";
    public final static String IMPORT_NOTE_ERROR = "import_note_error";
    public final static String IMPORT_INTERRUPT = "import_interrupt";
    public final static String DELETE_DROPBOX_FILES_SUCCESS = "delete_dropbox_files_success";
    public final static String DELETE_DROPBOX_FILES_FAIL = "delete_dropbox_files_fail";
    public final static String DELETE_DROPBOX_FILES_INTERRUPTED = "delete_dropbox_files_interrupted";
    public final static String UPLOAD_DROPBOX_FILES_SUCCESS = "upload_dropbox_files_success";
    public final static String UPLOAD_DROPBOX_FILES_FAIL = "upload_dropbox_files_fail";
    public final static String UPLOAD_DROPBOX_FILES_INTERRUPTED = "upload_dropbox_files_interrupted";
    public final static String MORE_MANAGE = "more_manage";
    public final static String PAGE_ADD_QUICK_TAG = "page_add_quick_tag";
    public final static String PAGE_REMOVE_QUICK_TAG = "page_remove_quick_tag";
    public final static String SAVE_RECENTLY_NOTE_JSON_DONE = "save_recently_note_json_done";
    public final static String DISMISS_POPUPWINDOW = "dismiss_popupwindow";
    public final static String PAGE_DRAW_TASK_HEAVY = "page_draw_task_heavy";
    public final static String PAGE_DRAW_TASK_LIGHT = "page_draw_task_light";
    public final static String PAGE_DRAW_COMPLETED = "page_draw_completed";
    public final static String NOOSE_DELETE = "noose_delete";
    public final static String NOOSE_COPY_AND_DELETE_AND_CUT_BTN_VISIBLE = "noose_copy_and_delete_btn_visible";
    public final static String NOOSE_COPY_AND_DELETE_AND_CUT_BTN_GONE = "noose_copy_and_delete_btn_gone";
    public final static String NOOSE_PASTE_BTN_VISIBLE = "noose_paste_btn_visible";
    public final static String NOOSE_PASTE_BTN_GONE = "noose_paste_btn_gone";
//    public final static String NOOSE_ALL_BTN_VISIBLE = "noose_all_btn_visible";
    public final static String NOOSE_ALL_BTN_GONE = "noose_all_btn_gone";
    public final static String NOOSE_COPY = "noose_copy";
    public final static String NOOSE_PASTE = "noose_paste";
    public final static String NOOSE_CUT = "noose_cut";
    private String message;

    public void setMessage(String msg) {
        this.message = msg;
    }

    public String getMessage() {
        return this.message;
    }
}
