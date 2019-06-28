package ntx.note;

import name.vbraun.view.write.Page;
import ntx.note2.R;

public class CommandPage extends Command {

	private int position;
	private boolean insertOrErase;
	
	public CommandPage(Page page, int pos, boolean insert) {
		super(page);
		position = pos;
		insertOrErase = insert;
	}
	
	@Override
	public void execute() {
		if (insertOrErase)
			UndoManager.getApplication().add(getPage(), position);
		else
			UndoManager.getApplication().remove(getPage(), position);
	}

	@Override
	public void revert() {
		if (insertOrErase)
			UndoManager.getApplication().remove(getPage(), position);
		else
			UndoManager.getApplication().add(getPage(), position);
	}

	@Override
	public String toString() {
		NoteWriterActivity app = UndoManager.getApplication();
		if (insertOrErase)
			return app.getString(R.string.command_page_insert, position);
		else
			return app.getString(R.string.command_page_erase, position);
	}
	
}
