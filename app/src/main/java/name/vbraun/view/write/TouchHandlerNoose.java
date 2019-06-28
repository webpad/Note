package name.vbraun.view.write;

import java.util.LinkedList;

import ntx.note.ToolboxConfiguration;

public class TouchHandlerNoose extends TouchHandlerControlPointABC {
    private static final String TAG = "TouchHandlerNoose";

    protected TouchHandlerNoose(HandwriterView view) {
        super(view, view.getOnlyPenInput());
    }

    @Override
    protected LinkedList<? extends GraphicsControlPoint> getGraphicsObjects() {
        return getPage().nooseArt;
    }

    @Override
    protected GraphicsControlPoint newGraphics(float x, float y, float pressure) {
        getPage().nooseArt.clear();
        GraphicsNoose noose = new GraphicsNoose(getPage().getTransform(), x, y,
                3, ToolboxConfiguration.getInstance().getPenColorInRGB());
        return noose;
    }

    @Override
    protected void destroy() {
    }

}
