package name.vbraun.view.write;

import java.util.LinkedList;

import ntx.note.ToolboxConfiguration;

public class TouchHandlerTriangle extends TouchHandlerControlPointABC {
    private static final String TAG = "TouchHandlerTriangle";

    protected TouchHandlerTriangle(HandwriterView view) {
        super(view, view.getOnlyPenInput());
    }

    @Override
    protected LinkedList<? extends GraphicsControlPoint> getGraphicsObjects() {
        return getPage().triangleArt;
    }

    @Override
    protected GraphicsControlPoint newGraphics(float x, float y, float pressure) {
        GraphicsTriangle triangle = new GraphicsTriangle(getPage().getTransform(), x, y,
                ToolboxConfiguration.getInstance().getPenThickness(), ToolboxConfiguration.getInstance().getPenColorInRGB());
        return triangle;
    }

    @Override
    protected void destroy() {
    }

}
