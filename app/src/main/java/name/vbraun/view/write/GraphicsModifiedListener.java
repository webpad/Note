package name.vbraun.view.write;

import java.util.LinkedList;

public interface GraphicsModifiedListener {
	public void onGraphicsCreateListener(Page page, Graphics toAdd);
	public void onGraphicsModifyListener(Page page, Graphics modifiedGraphics);
	public void onGraphicsListModifyListener(Page page, LinkedList<Graphics> modifiedGraphicsList);
	public void onGraphicsEraseListener(Page page, Graphics toErase);
	public void onGraphicsListEraseListener(Page page, LinkedList<Graphics> toErase);
	public void onGraphicsListCopyListener(Page page, LinkedList<Graphics> toCopy);
	public void onPageClearListener(Page page);
}
