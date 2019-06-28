package name.vbraun.view.write;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import name.vbraun.view.write.Graphics.Tool;
import ntx.draw.nDrawHelper;
import ntx.note.ALog;
import ntx.note.Global;
import ntx.note.ToolboxConfiguration;

public abstract class TouchHandlerPenABC extends TouchHandlerABC {
    @SuppressWarnings("unused")
    private static final String TAG = "TouchHandlerABC";

    public static boolean isPopupwindow = true;
    public static boolean isFastView = false;

    private final Rect mRect = new Rect();
    protected int N = 0;
    protected static final int Nmax = 1024;
    protected float[] position_x = new float[Nmax];
    protected float[] position_y = new float[Nmax];
    protected float[] pressure = new float[Nmax];

    protected Paint pen;

    private EventBus mEventBus;

    protected TouchHandlerPenABC(HandwriterView view) {
        super(view);
        pen = new Paint();
        pen.setAntiAlias(true);
        pen.setARGB(0xff, 0, 0, 0);
        pen.setStrokeCap(Paint.Cap.ROUND);

        mEventBus = EventBus.getDefault();
        mEventBus.register(this);
    }

    @Override
    protected void destroy() {
        mEventBus.unregister(this);
    }

    @Override
    protected void interrupt() {
        super.interrupt();
        N = 0;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ToolboxConfiguration event) {
        @Tool int currentTool = event.getCurrentTool();
        boolean nDrawSwitchValue;
        if (currentTool == Tool.FOUNTAINPEN) {
            nDrawHelper.NDrawSetPenType(Global.NDRAW_PEN_TYPE_FOUNTAINPEN);
            if (event.getPenThickness() == 1) {//minimum thickness -> set the same value as pencil's
                nDrawHelper.NDrawSetMaxStrokeWidthWhenUsingPressure(-1);
                nDrawHelper.NDrawSetStrokeWidth(1);
            } else {
                nDrawHelper.NDrawSetMaxStrokeWidthWhenUsingPressure((int) getScaledPenThickness());
            }
            nDrawSwitchValue = !isPopupwindow && !isFastView;
            nDrawHelper.NDrawSwitch(nDrawSwitchValue);

        } else if (currentTool == Tool.BRUSH) {
            nDrawHelper.NDrawSetPenType(Global.NDRAW_PEN_TYPE_BRUSH);
            if ((int) (getScaledPenThickness()) == 0) {
                nDrawHelper.NDrawSetMaxStrokeWidthWhenUsingPressure(HandwriterView.Brush_thickness * Global.BRUSH_THICKNESS_WEIGHT);
            } else {
                nDrawHelper.NDrawSetMaxStrokeWidthWhenUsingPressure(
                        (int) (getScaledPenThickness() * Global.BRUSH_THICKNESS_WEIGHT));
            }
            nDrawSwitchValue = !isPopupwindow && !isFastView;
            nDrawHelper.NDrawSwitch(nDrawSwitchValue);

        } else if (currentTool == Tool.PENCIL) {
            nDrawHelper.NDrawSetPenType(Global.NDRAW_PEN_TYPE_PENCIL);
            nDrawHelper.NDrawSetMaxStrokeWidthWhenUsingPressure(-1);
            nDrawHelper.NDrawSetStrokeWidth(ToolboxConfiguration.getInstance().getPenThickness());
            nDrawSwitchValue = !isPopupwindow && !isFastView;
            nDrawHelper.NDrawSwitch(nDrawSwitchValue);
        } else {
            nDrawHelper.NDrawSwitch(false);
        }
    }

    /**
     * Set the pen style. Subsequent calls to drawOutline() will use this pen.
     */
    protected void initPenStyle() {
        int penColor = ToolboxConfiguration.getInstance().getPenColorInRGB();
        if (view.getAntiColor()) {
            pen.setARGB(Color.alpha(penColor), Color.red(penColor) ^ 0xff, Color.green(penColor) ^ 0xff,
                    Color.blue(penColor) ^ 0xff);
        } else {
            pen.setARGB(Color.alpha(penColor), Color.red(penColor), Color.green(penColor), Color.blue(penColor));
        }

        float scaledPenThickness = getScaledPenThickness();
        pen.setStrokeWidth(scaledPenThickness);
    }

    protected void drawOutline(float oldX, float oldY, float newX, float newY, float oldPressure, float newPressure) {
        int extra = 0;
        @Tool int currentTool = ToolboxConfiguration.getInstance().getCurrentTool();
        if (currentTool == Tool.FOUNTAINPEN) {
            nDrawHelper.NDrawSetPenType(Global.NDRAW_PEN_TYPE_FOUNTAINPEN);
            ALog.e(TAG, "THPABC<----- pressure " + oldPressure + " --> " + newPressure);
            nDrawHelper.NDrawSetMaxStrokeWidthWhenUsingPressure((int) getScaledPenThickness());
            float scaledPenThickness = getScaledPenThickness() * (oldPressure + newPressure) / 2f;
            pen.setStrokeWidth(scaledPenThickness);
            // nDrawHelper.NDrawSetStrokeWidth((int)scaledPenThickness);
        } else if (currentTool == Tool.BRUSH) {
            nDrawHelper.NDrawSetPenType(Global.NDRAW_PEN_TYPE_BRUSH);
            float wop = 1.0f; // weight of oldPressure
            float wnp = 50.0f; // weight of newPressure
            nDrawHelper.NDrawSetMaxStrokeWidthWhenUsingPressure(
                    (int) (getScaledPenThickness() * Global.BRUSH_THICKNESS_WEIGHT));
            float scaledPenThickness = getScaledPenThickness() * Global.BRUSH_THICKNESS_WEIGHT
                    * ((wop * oldPressure) + (wnp * newPressure)) / (wop + wnp);
            ALog.e(TAG, "THPABC<----- brush pressure=( " + oldPressure + " --> " + newPressure + "), thickness="
                    + scaledPenThickness);
            pen.setStrokeWidth(scaledPenThickness);
            // nDrawHelper.NDrawSetStrokeWidth((int)scaledPenThickness);
        } else if (currentTool == Tool.PENCIL) {
            nDrawHelper.NDrawSetPenType(Global.NDRAW_PEN_TYPE_PENCIL);
            nDrawHelper.NDrawSetMaxStrokeWidthWhenUsingPressure(-1);
            nDrawHelper.NDrawSetStrokeWidth(ToolboxConfiguration.getInstance().getPenThickness());
        }

        extra = -(int) (pen.getStrokeWidth() / 2) - 1;

        view.canvas.drawLine(oldX, oldY, newX, newY, pen);
        mRect.set((int) oldX, (int) oldY, (int) newX, (int) newY);
        mRect.sort();
        mRect.inset(extra, extra);
        // Donn't draw the outline when nDraw enalbe
        // if (Hardware.isEinkHardwareType()) {
        // view.invalidate(mRect, PenEventNTX.UPDATE_MODE_PEN );
        // } else {
        // view.invalidate(mRect);
        // }
    }

    protected void saveStroke() {
        if (N == 0)
            return;
        if (N == 1) { // need two points to draw a connecting line
            N = 2;
            position_x[1] = position_x[0];
            position_y[1] = position_y[0];
            pressure[1] = pressure[0];
        }

        Stroke newStroke = null;
        @Tool int currentTool = ToolboxConfiguration.getInstance().getCurrentTool();
        int currentThickness = ToolboxConfiguration.getInstance().getPenThickness();
        int penColor = ToolboxConfiguration.getInstance().getPenColorInRGB();
        if (currentTool == Tool.BRUSH) {
            newStroke = Stroke.fromInput(currentTool, (currentThickness + 7) * Global.BRUSH_THICKNESS_WEIGHT, penColor,
                    getPage().getTransform(), position_x, position_y, pressure, N, view.getPenSmoothFilter());
        } else if (currentTool == Tool.PENCIL) {
            newStroke = Stroke.fromInput(currentTool, currentThickness, penColor, getPage().getTransform(), position_x,
                    position_y, pressure, N, view.getPenSmoothFilter());
        } else {
            newStroke = Stroke.fromInput(currentTool, (currentThickness + 1), penColor, getPage().getTransform(),
                    position_x, position_y, pressure, N, view.getPenSmoothFilter());
        }
        view.saveStroke(newStroke);
        N = 0;
        Global.checkNeedRefresh(newStroke.getStrokeColor());
    }

}