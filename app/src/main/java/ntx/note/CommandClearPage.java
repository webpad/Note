package ntx.note;

import java.util.LinkedList;

import name.vbraun.view.write.GraphicsLine;
import name.vbraun.view.write.GraphicsOval;
import name.vbraun.view.write.GraphicsRectangle;
import name.vbraun.view.write.GraphicsTriangle;
import name.vbraun.view.write.Page;
import name.vbraun.view.write.Stroke;
import ntx.note.data.Bookshelf;
import ntx.note2.R;

public class CommandClearPage extends Command {

    protected final LinkedList<Stroke> strokes = new LinkedList<Stroke>();
    protected final LinkedList<GraphicsLine> lineArt = new LinkedList<GraphicsLine>();
    protected final LinkedList<GraphicsRectangle> rectangleArt = new LinkedList<GraphicsRectangle>();
    protected final LinkedList<GraphicsOval> ovalArt = new LinkedList<GraphicsOval>();
    protected final LinkedList<GraphicsTriangle> triangleArt = new LinkedList<GraphicsTriangle>();

    public CommandClearPage(Page page) {
        super(page);
        strokes.addAll(page.strokes);
        lineArt.addAll(page.lineArt);
        rectangleArt.addAll(page.rectangleArt);
        ovalArt.addAll(page.ovalArt);
        triangleArt.addAll(page.triangleArt);
    }

    @Override
    public void execute() {
        UndoManager.getApplication().remove_for_clear(getPage());
    }

    @Override
    public void revert() {
        UndoManager.getApplication().add_for_clear_revert(getPage(), strokes, lineArt, rectangleArt, ovalArt, triangleArt);
    }

    @Override
    public String toString() {
        int n = Bookshelf.getInstance().getCurrentBook().getPageNumber(getPage());
        NoteWriterActivity app = UndoManager.getApplication();
        return app.getString(R.string.command_clear_page, n);
    }

}
