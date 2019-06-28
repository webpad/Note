package ntx.note;

import junit.framework.Assert;

import java.util.LinkedList;

import name.vbraun.view.write.Graphics;
import name.vbraun.view.write.GraphicsModifiedListener;
import name.vbraun.view.write.Page;

/**
 * The command manager. Every change to the notebook has to go through
 * the command manager in the form of a Command class that knows how to
 * undo/redo itself.
 * <p>
 * The undo manager is a singlet and currently operates only within a
 * single notebook. If you change the notebook you must call
 * {@link #clearHistory()}.
 *
 * @author vbraun
 */
public class UndoManager implements GraphicsModifiedListener, BookModifiedListener {

    private UndoManager() {
    }

    private final static UndoManager instance = new UndoManager();

    public static UndoManager getUndoManager() {
        return instance;
    }

    protected LinkedList<Command> undoStack = new LinkedList<Command>();
    protected LinkedList<Command> redoStack = new LinkedList<Command>();

    private NoteWriterActivity main;

    public static void setApplication(NoteWriterActivity app) {
        getUndoManager().main = app;
    }

    protected static NoteWriterActivity getApplication() {
        NoteWriterActivity result = getUndoManager().main;
        Assert.assertNotNull(result);
        return result;
    }

    @Override
    public void onGraphicsCreateListener(Page page, Graphics toAdd) {
        Command cmd = new CommandCreateGraphics(page, toAdd);
        undoStack.addFirst(cmd);
        redoStack.clear();
        limitStackSize();
        cmd.execute();
    }

    @Override
    public void onGraphicsModifyListener(Page page, Graphics modifiedGraphics) {
        Command cmd = new CommandModifyGraphics(page, modifiedGraphics);
        undoStack.addFirst(cmd);
        redoStack.clear();
        limitStackSize();
        cmd.execute();
    }

    @Override
    public void onGraphicsListModifyListener(Page page, LinkedList<Graphics> modifiedGraphicsList) {
        Command cmd = new CommandModifyGraphicsList(page, modifiedGraphicsList);
        undoStack.addFirst(cmd);
        redoStack.clear();
        limitStackSize();
        cmd.execute();
    }

    @Override
    public void onGraphicsEraseListener(Page page, Graphics toErase) {
        Command cmd = new CommandEraseGraphics(page, toErase);
        undoStack.addFirst(cmd);
        redoStack.clear();
        limitStackSize();
        cmd.execute();
    }

    @Override
    public void onGraphicsListEraseListener(Page page, LinkedList<Graphics> toErase) {
        Command cmd = new CommandEraseGraphicsList(page, toErase);
        undoStack.addFirst(cmd);
        redoStack.clear();
        limitStackSize();
        cmd.execute();
    }

    @Override
    public void onGraphicsListCopyListener(Page page, LinkedList<Graphics> toCopy) {
        Command cmd = new CommandCopyGraphicsList(page, toCopy);
        undoStack.addFirst(cmd);
        redoStack.clear();
        limitStackSize();
        cmd.execute();
    }

    public void onPageClearListener(Page page) {
        Command cmd = new CommandClearPage(page);
        undoStack.addFirst(cmd);
        redoStack.clear();
        limitStackSize();
        cmd.execute();
    }

    public void onPageInsertListener(Page page, int position) {
        Command cmd = new CommandPage(page, position, true);
        undoStack.addFirst(cmd);
        redoStack.clear();
        limitStackSize();
        cmd.execute();
    }

    public void onPageDeleteListener(Page page, int position) {
        Command cmd = new CommandPage(page, position, false);
        undoStack.addFirst(cmd);
        redoStack.clear();
        limitStackSize();
        cmd.execute();
    }


    private static final int MAX_STACK_SIZE = 50;

    private void limitStackSize() {
        while (undoStack.size() > MAX_STACK_SIZE)
            undoStack.removeLast();
        while (redoStack.size() > MAX_STACK_SIZE)
            redoStack.removeLast();
    }


    public boolean undo() {
        Command cmd = undoStack.pollFirst();
        if (cmd == null)
            return false;
        cmd.getPage().nooseArt.clear();
        cmd.getPage().clearSelectedObjects();
        cmd.revert();
        redoStack.addFirst(cmd);
        return true;
    }

    public boolean redo() {
        Command cmd = redoStack.pollFirst();
        if (cmd == null)
            return false;
        cmd.getPage().nooseArt.clear();
        cmd.getPage().clearSelectedObjects();
        cmd.execute();
        undoStack.addFirst(cmd);
        return true;
    }

    public boolean haveUndo() {
        return !undoStack.isEmpty();
    }

    public boolean haveRedo() {
        return !redoStack.isEmpty();
    }

    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }
}
