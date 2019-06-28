package name.vbraun.view.write;

import java.util.LinkedList;

import ntx.note.ToolboxConfiguration;

public class TouchHandlerLine extends TouchHandlerControlPointABC {
    private static final String TAG = "TouchHandlerLine";

    protected TouchHandlerLine(HandwriterView view) {
        super(view, view.getOnlyPenInput());
    }

    @Override
    protected LinkedList<? extends GraphicsControlPoint> getGraphicsObjects() {
        return getPage().lineArt;
    }

    @Override
    protected GraphicsControlPoint newGraphics(float x, float y, float pressure) {
        GraphicsLine line = new GraphicsLine(getPage().getTransform(), x, y,
                ToolboxConfiguration.getInstance().getPenThickness(), ToolboxConfiguration.getInstance().getPenColorInRGB());
        return line;
    }

    @Override
    protected void destroy() {
    }

}
