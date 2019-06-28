package ntx.note;

import name.vbraun.view.write.Graphics;
import name.vbraun.view.write.Page;

public class CommandModifyGraphics extends Command {

    protected final Graphics graphics, backupDataGraphics, newDataGraphics;

    public CommandModifyGraphics(Page page, Graphics modifiedGraphics) {
        super(page);
        graphics = modifiedGraphics;
        backupDataGraphics = modifiedGraphics.getBackupGraphics();
        newDataGraphics = modifiedGraphics.getCloneGraphics();
    }

    @Override
    public void execute() {
        UndoManager.getApplication().modify_graphics(getPage(), graphics, newDataGraphics);
    }

    @Override
    public void revert() {
        UndoManager.getApplication().modify_graphics(getPage(), graphics, backupDataGraphics);

    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return null;
    }

}
