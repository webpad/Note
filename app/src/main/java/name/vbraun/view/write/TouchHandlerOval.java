package name.vbraun.view.write;

import java.util.LinkedList;

import ntx.note.ToolboxConfiguration;

public class TouchHandlerOval extends TouchHandlerControlPointABC {
    private static final String TAG = "TouchHandlerOval";

    protected TouchHandlerOval(HandwriterView view) {
        super(view, view.getOnlyPenInput());
    }

    @Override
    protected LinkedList<? extends GraphicsControlPoint> getGraphicsObjects() {
        return getPage().ovalArt;
    }

    @Override
    protected GraphicsControlPoint newGraphics(float x, float y, float pressure) {
        GraphicsOval oval = new GraphicsOval(getPage().getTransform(), x, y,
                ToolboxConfiguration.getInstance().getPenThickness(), ToolboxConfiguration.getInstance().getPenColorInRGB());
        return oval;
    }

    @Override
    protected void destroy() {
    }

}
