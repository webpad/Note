package name.vbraun.view.write;

import java.util.LinkedList;

import ntx.note.ToolboxConfiguration;

public class TouchHandlerRectangle extends TouchHandlerControlPointABC {
    private static final String TAG = "TouchHandlerRectangle";

    protected TouchHandlerRectangle(HandwriterView view) {
        super(view, view.getOnlyPenInput());
    }

    @Override
    protected LinkedList<? extends GraphicsControlPoint> getGraphicsObjects() {
        return getPage().rectangleArt;
    }

    @Override
    protected GraphicsControlPoint newGraphics(float x, float y, float pressure) {
        GraphicsRectangle rectangle = new GraphicsRectangle(getPage().getTransform(), x, y,
                ToolboxConfiguration.getInstance().getPenThickness(), ToolboxConfiguration.getInstance().getPenColorInRGB());
        return rectangle;
    }

    @Override
    protected void destroy() {
    }

}
