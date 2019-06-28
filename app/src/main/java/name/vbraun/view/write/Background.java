package name.vbraun.view.write;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.Log;

import name.vbraun.lib.pen.Hardware;
import ntx.note.ALog;
import ntx.note.Global;
import ntx.note.artist.Artist;
import ntx.note.artist.LineStyle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Locale;


public class Background {
    public static final String TAG = "Background";
    private static final float INCH_in_CM = 2.54f;
    private static final float INCH_in_MM = INCH_in_CM * 10;

    private static final float marginMm = 5;

    private static final float LEGALRULED_SPACING = 10.0f;
    private static final float COLLEGERULED_SPACING = 10.0f;
    private static final float NARROWRULED_SPACING = 6.4f;
    private static final float TODOLIST_SPACING = 10.0f;
    private static final float STAVE_SPACING = 11.0f; //8.0f;
    private static final float STAVE_MARGIN = 8.0f; //20.0f;
//    public static boolean flag_zoom = false;

    private Paper.Type paperType = Paper.Type.EMPTY;
    private String paperPath = "na";
    private AspectRatio aspectRatio = AspectRatio.Table[0];
    private float heightMm, widthMm;
    private int centerWidth, centerHeight = 0;
    private Bitmap bitmap = null;

    private final RectF paper = new RectF();
    private final Paint paint = new Paint();

    private int CALLIGRAPHY_TYPE_SMALL = 0;
    private int CALLIGRAPHY_TYPE_BIG = 1;

    private int shade = Color.BLACK;
    private float threshold = 1500;

    public void setPaperType(Paper.Type paper) {
        paperType = paper;
        paint.setStrokeCap(Cap.BUTT);
    }

    public Paper.Type getPaperType() {
        return paperType;
    }

    public void setPaperPath(String paper_path) {
        paperPath = paper_path;
    }

    public void setAspectRatio(float aspect) {
        aspectRatio = AspectRatio.closestMatch(aspect);
        heightMm = aspectRatio.guessHeightMm();
        widthMm = aspectRatio.guessWidthMm();
    }

    private int paperColour = Color.WHITE;

    public int getPaperColour() {
        return paperColour;
    }

    public void setPaperColour(int paperColour) {
        this.paperColour = paperColour;
    }

    private void drawGreyFrame(Canvas canvas, RectF bBox, Transformation t) {
        paper.set(t.offset_x, t.offset_y,
                t.offset_x + aspectRatio.ratio * t.scale, t.offset_y + t.scale);
        if (!paper.contains(bBox))
            canvas.drawARGB(0xff, 0xaa, 0xaa, 0xaa);
    }

    private void drawEmptyFrame(Canvas canvas, RectF bBox, Transformation t) {
        paper.set(t.offset_x, t.offset_y,
                t.offset_x + aspectRatio.ratio * t.scale, t.offset_y + t.scale);
        if (!paper.contains(bBox))
            canvas.drawARGB(Color.alpha(paperColour),
                    Color.red(paperColour) ^ 0xff, Color.green(paperColour) ^ 0xff, Color.blue(paperColour) ^ 0xff);
    }

    /**
     * Artis: For EPD, set the black frame to cover and hide the out boundary strokes
     *
     * @param canvas
     * @param bBox
     * @param t
     */
    private void drawBlackFrame(Canvas canvas, RectF bBox, Transformation t) {
        paper.set(t.offset_x, t.offset_y,
                t.offset_x + aspectRatio.ratio * t.scale, t.offset_y + t.scale);
        if (!paper.contains(bBox))
            canvas.drawARGB(0xff, 0x00, 0x00, 0x00);
    }


    /**
     * This is where we clear the (possibly uninitialized) backing bitmap in the canvas.
     * The background is filled with white, which is most suitable for printing.
     *
     * @param canvas The canvas to draw on
     * @param bBox   The damage area
     * @param t      The linear transformation from paper to screen
     */
    public void drawWhiteBackground(Canvas canvas, RectF bBox, Transformation t) {
//		drawGreyFrame(canvas, bBox, t);		// original
        drawBlackFrame(canvas, bBox, t);    // Artis: for EPD
        paint.setARGB(0xff, 0xff, 0xff, 0xff);
        paint.setColor(Color.BLACK);
        canvas.drawRect(paper, paint);
    }

    /**
     * This is where we clear the (possibly uninitialized) backing bitmap in the canvas.
     *
     * @param canvas The canvas to draw on
     * @param bBox   The damage area
     * @param t      The linear transformation from paper to screen
     */
    public void drawEmptyBackground(Canvas canvas, RectF bBox, Transformation t) {
//		drawGreyFrame(canvas, bBox, t);		// original
//		drawBlackFrame(canvas, bBox, t);	// Artis: for EPD
        drawEmptyFrame(canvas, bBox, t);
        paint.setColor(paperColour);
        canvas.drawRect(paper, paint);
    }

    public void drawTransparentBackground(Canvas canvas, RectF bBox, Transformation t) {
        drawEmptyFrame(canvas, bBox, t);
        paint.setColor(Color.TRANSPARENT);
        canvas.drawRect(paper, paint);
    }

    public void draw(Canvas canvas, RectF bBox, Transformation t) {
        //Log.v(TAG, "draw_paper at scale "+scale);

        if (HandwriterView.getAntiColor()) {
            setPaperColour(Color.BLACK);
        } else {
            setPaperColour(Color.WHITE);
        }

        // the paper is 1 high and aspect_ratio wide
        drawEmptyBackground(canvas, bBox, t);
        switch (paperType) {
            case EMPTY:
                return;
            case RULED:
                draw_ruled(canvas, t, LEGALRULED_SPACING, 31.75f);
                return;
            case COLLEGERULED:
                draw_ruled(canvas, t, COLLEGERULED_SPACING, 25.0f);
                return;
            case NARROWRULED:
                draw_ruled(canvas, t, NARROWRULED_SPACING, 25.0f);
                return;
            case QUAD:
                draw_quad(canvas, t);
                return;
            case CORNELLNOTES:
                draw_cornellnotes(canvas, t);
                return;
            case DAYPLANNER:
                draw_dayplanner(canvas, t, Calendar.getInstance());
                return;
            case MUSIC:
                draw_music_manuscript(canvas, t);
                return;
            case CALLIGRAPHY_SMALL:
                draw_calligraphy(canvas, t, CALLIGRAPHY_TYPE_SMALL);
                return;
            case CALLIGRAPHY_BIG:
                draw_calligraphy(canvas, t, CALLIGRAPHY_TYPE_BIG);
                return;
            case TODOLIST:
                draw_todolist(canvas, t, TODOLIST_SPACING, 20.0f);
                return;
            case MINUTES:
                draw_minutes(canvas, t, TODOLIST_SPACING, 20.0f);
                return;
            case STAVE:
                draw_stave(canvas, t, STAVE_SPACING, STAVE_MARGIN);
                return;
            case DIARY:
                draw_diary(canvas, t, TODOLIST_SPACING, 20.0f);
                return;
            case HEX:
                // TODO
                return;
            case CUSTOMIZED:
                draw_customized(canvas, bBox);
                return;
        }
    }

    public void drawPNG(Canvas canvas, RectF bBox, Transformation t) {

        if (HandwriterView.getAntiColor()) {
            setPaperColour(Color.BLACK);
        } else {
            setPaperColour(Color.WHITE);
        }

        // the paper is 1 high and aspect_ratio wide
        drawEmptyBackground(canvas, bBox, t);

        drawPNG_customized(canvas, bBox);

    }

    private void draw_dayplanner(Canvas c, Transformation t, Calendar calendar) {
        float x0, x1, y, y0, y1;
        float textHeight;
        if (t.scale < threshold)
            shade += (int) ((threshold - t.scale) / threshold * (0xff - shade));
        //paint.setARGB(0xff, shade, shade, shade);
        paint.setStrokeWidth(0);
        paint.setColor(Color.BLACK);

        Typeface font = Typeface.create(Typeface.SERIF, Typeface.BOLD);
        paint.setTypeface(font);
        paint.setAntiAlias(true);

        // Header
        float headerHeightMm = 30f;
        x0 = t.applyX(marginMm / heightMm);
        x1 = t.applyX((widthMm - marginMm) / heightMm);
        y = t.applyY(headerHeightMm / heightMm);
        c.drawLine(x0, y, x1, y, paint);

        textHeight = t.scaleText(24f);
        paint.setTextSize(textHeight);
        y = t.applyY(marginMm / heightMm) + textHeight;
        c.drawText(calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()), x0, y, paint);

// I'm leaving this out for now; Should there be a gui to pick the day of the year? Or just let the user write the date?
//		y0 = t.applyY((widthMm-marginMm)/widthMm);
//		c.drawText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), x0, y0, paint);
//
//		paint.setTextSize(t.scaleText(12f));
//
//		c.drawText(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()), x0 + t.applyX(2*marginMm/heightMm), y0 + t.applyY(marginMm/heightMm), paint);
//
//		paint.setTextSize(t.scaleText(10f));
//		font = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
//		paint.setTextAlign(Align.RIGHT);
//		c.drawText("Week " + calendar.get(Calendar.WEEK_OF_YEAR),t.applyX((widthMm-marginMm)/heightMm), t.applyY((float) (marginMm*1.75/widthMm)), paint);

        // Details
        paint.setTextAlign(Align.LEFT);
        paint.setARGB(0xff, shade, shade, shade);
        paint.setColor(Color.BLACK);
        float spacingMm = COLLEGERULED_SPACING;
        int n = (int) Math.floor((heightMm - headerHeightMm - marginMm) / spacingMm);

        x0 = t.applyX(marginMm / heightMm);
        x1 = t.applyX((widthMm - marginMm) / heightMm);

        int hourMarker = 7;
        textHeight = t.scaleText(10f);
        paint.setTextSize(textHeight);

        for (int i = 1; i <= n; i++) {
            y = t.applyY((headerHeightMm + i * spacingMm) / heightMm);
            c.drawLine(x0, y, x1, y, paint);

            if (i % 2 == 1) {
                y = t.applyY((headerHeightMm + (i - 0.5f) * spacingMm) / heightMm) + textHeight / 2;
                c.drawText(hourMarker + ":", x0, y, paint);

                hourMarker++;
                if (hourMarker == 13)
                    hourMarker = 1;
            }

        }
    }

    private void draw_cornellnotes(Canvas c, Transformation t) {

        float x0, x1, y0, y1;
        final float MARGIN = 1.25f;

        if (t.scale < threshold)
            shade += (int) ((threshold - t.scale) / threshold * (0xff - shade));
        paint.setARGB(0xff, shade, shade, shade);

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);


        // Details
        float spacingMm = COLLEGERULED_SPACING;
        if (Hardware.isEinkUsingLargerUI()) {
            spacingMm = (float) (spacingMm * 1.7);
        }
        int n = (int) Math.floor((heightMm - (MARGIN * INCH_in_MM) - 2 * marginMm) / spacingMm);

        x0 = t.applyX((MARGIN * INCH_in_MM) / widthMm + marginMm / heightMm);
        x1 = t.applyX((widthMm - marginMm) / heightMm);

        for (int i = 1; i <= n - 3; i++) {
            float y = t.applyY(((heightMm - n * spacingMm - MARGIN * INCH_in_MM) + i * spacingMm) / heightMm);
            c.drawLine(x0, y, x1, y, paint);
        }

        // Cue Column
        x0 = t.applyX((MARGIN * INCH_in_MM) / widthMm);
        x1 = x0;
        y0 = t.applyY(0);
        y1 = t.applyY((heightMm - spacingMm * 2 - (MARGIN * INCH_in_MM)) / heightMm);

        c.drawLine(x0, y0, x1, y1, paint);

        // Summary area at base of page
        x0 = t.applyX(0);
        x1 = t.applyX(widthMm / heightMm);
        y0 = t.applyY((heightMm - spacingMm * 2 - (MARGIN * INCH_in_MM)) / heightMm);
        y1 = y0;

        c.drawLine(x0, y0, x1, y1, paint);

    }


    private void draw_ruled(Canvas c, Transformation t, float lineSpacing, float margin) {

        float spacingMm = lineSpacing;
        if (Hardware.isEinkUsingLargerUI()) {
            spacingMm = (float) (spacingMm * 1.7);
        }
        float vertLineMm = margin;

        if (t.scale < threshold)
            shade += (int) ((threshold - t.scale) / threshold * (0xff - shade));
        paint.setARGB(0xff, shade, shade, shade);

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);
        int n = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm) - 2;
        float x0 = t.applyX(marginMm / heightMm);
        float x1 = t.applyX((widthMm - marginMm) / heightMm);


        if (lineSpacing == COLLEGERULED_SPACING) {

            for (int i = 0; i <= n; i++) {
                float y = t.applyY(((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm);
                c.drawLine(x0, y, x1, y, paint);
            }

        } else if (lineSpacing == NARROWRULED_SPACING) {

            for (int i = 1; i <= n - 1; i++) {
                float y = t.applyY(((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm);
                c.drawLine(x0, y, x1, y, paint);
            }

        } else {

            for (int i = 1; i <= n - 2; i++) {
                float y = t.applyY(((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm);
                c.drawLine(x0, y, x1, y, paint);
            }

        }

        // Paint margin
        if (margin > 0.0f) {
            paint.setARGB(0xff, shade, shade, shade);
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(2);
            float y0 = t.applyY(marginMm / heightMm);
            float y1 = 0;

            if (lineSpacing == COLLEGERULED_SPACING) {

                y1 = t.applyY((heightMm - marginMm - spacingMm) / heightMm);

            } else if (lineSpacing == NARROWRULED_SPACING) {

                y1 = t.applyY((heightMm - marginMm - spacingMm * 1.5f) / heightMm);

            } else {

                y1 = t.applyY((heightMm - marginMm - spacingMm * 2) / heightMm);

            }

            float x = t.applyX(vertLineMm / widthMm);
            c.drawLine(x, y0, x, y1, paint);
        }
    }

    private void draw_quad(Canvas c, Transformation t) {
        float spacingMm = 10f;
        if (Hardware.isEinkUsingLargerUI()) {
            spacingMm = (float) (spacingMm * 1.7);
        }
        int nx, ny;
        float x, x0, x1, y, y0, y1;

        if (t.scale < threshold)
            shade += (int) ((threshold - t.scale) / threshold * (0xff - shade));
        paint.setARGB(0xff, shade, shade, shade);

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);
        ny = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm);
        nx = (int) Math.floor((widthMm - 2 * marginMm) / spacingMm);
        float marginXMm = (widthMm - nx * spacingMm) / 2;
        float marginYMm = (heightMm - ny * spacingMm) / 2;
        x0 = t.applyX(marginXMm / heightMm);
        x1 = t.applyX((widthMm - marginXMm) / heightMm);
        y0 = t.applyY(marginYMm / heightMm);
        y1 = t.applyY((heightMm - marginYMm - spacingMm) / heightMm);
        for (int i = 0; i < ny; i++) {
            y = t.applyY((marginYMm + i * spacingMm) / heightMm);
            c.drawLine(x0, y, x1, y, paint);
        }
        for (int i = 0; i <= nx; i++) {
            x = t.applyX((marginXMm + i * spacingMm) / heightMm);
            c.drawLine(x, y0, x, y1, paint);
        }
    }

    private void draw_calligraphy(Canvas c, Transformation t, int type) {
        int positionBufferNum = 0;// base on the max value of ny and nx
        float spacingMmBase = 10f; // base on quad paper
        float spacingMm = 0;

        //calculate spacing according to type
        if (type == CALLIGRAPHY_TYPE_SMALL)
            spacingMm = (float) 6 * spacingMmBase;
        else
            // CALLIGRAPHY_TYPE_BIG
            spacingMm = (float) 8 * spacingMmBase;

		/*
		if (Hardware.isEink6InchHardwareType()) {
			spacingMm = (float) (spacingMm * 1.7);
		}
		*/

        int nx, ny;
        float x, x0, x1, y, y0, y1;
        if (t.scale < threshold)
            shade += (int) ((threshold - t.scale) / threshold * (0xff - shade));
        paint.setARGB(0xff, shade, shade, shade);

        paint.setColor(Color.BLACK);
        //paint.setStrokeWidth(6);
        paint.setStrokeWidth(2);
        ny = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm);
        nx = (int) Math.floor((widthMm - 2 * marginMm) / spacingMm);
        float marginXMm = (widthMm - nx * spacingMm) / 2;
        float marginYMm = (heightMm - ny * spacingMm) / 2;
        x0 = t.applyX(marginXMm / heightMm);
        x1 = t.applyX((widthMm - marginXMm) / heightMm);
        y0 = t.applyY((marginYMm * 2 / 3) / heightMm);
        y1 = t.applyY((heightMm - marginYMm * 4 / 3) / heightMm);

        if (nx >= ny)
            positionBufferNum = nx + 1;
        else
            positionBufferNum = ny + 1;

        //for recording x, y positions
        float[] position_x = new float[positionBufferNum];
        float[] position_y = new float[positionBufferNum];

        //draw frame line
        //draw "-" line
        for (int i = 0; i <= ny; i++) {
            y = t.applyY((marginYMm * 2 / 3 + i * spacingMm) / heightMm);
            c.drawLine(x0, y, x1, y, paint);
            position_y[i] = y;
        }
        //draw "|" line
        for (int i = 0; i <= nx; i++) {
            x = t.applyX((marginXMm + i * spacingMm) / heightMm);
            c.drawLine(x, y0, x, y1, paint);
            position_x[i] = x;
        }

        //debug position info
		/*
		for (int i=0; i<=nx; i++) {
			ALog.debug("position_x["+i+"]:" +position_x[i]);
		}
		for (int i=0; i<=ny; i++) {
			ALog.debug("position_y["+i+"]:" +position_y[i]);
		}
		*/

        //draw "x" and '+' line
        //paint.setStrokeWidth(2);
        for (int i = 0; i < ny; i++) {
            for (int j = 0; j < nx; j++) {
                // draw "\" line
                c.drawLine(position_x[j], position_y[i], position_x[j + 1], position_y[i + 1], paint);
                // draw "|" line
                c.drawLine((position_x[j] + position_x[j + 1]) / 2, position_y[i],
                        (position_x[j] + position_x[j + 1]) / 2, position_y[ny], paint);
                // draw "/" line
                c.drawLine(position_x[j + 1], position_y[i], position_x[j], position_y[i + 1], paint);
                // draw "-" line
                c.drawLine(position_x[j], (position_y[i] + position_y[i + 1]) / 2, position_x[nx],
                        (position_y[i] + position_y[i + 1]) / 2, paint);
            }
        }
    }

    private void draw_music_manuscript(Canvas c, Transformation t) {
        float lineSpacingMm = 2.5f;
        float staveHeight = 4 * lineSpacingMm;
        float staffTopMarginMm = 25.0f;
        float staffBottomMarginMm = 15.0f;
        float staffSideMarginMm = 15.0f;
        int staveCount;
        if (aspectRatio.isPortrait())
            staveCount = 12;
        else
            staveCount = 8;
        float staffTotal = staffTopMarginMm + staffBottomMarginMm + staveCount * staveHeight;
        float staffSpacing = staveHeight + (heightMm - staffTotal) / (staveCount - 1);

        float x0, x1, y;

        if (t.scale < threshold)
            shade += (int) ((threshold - t.scale) / threshold * (0xff - shade));
        paint.setARGB(0xff, shade, shade, shade);

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(0);

        x0 = t.applyX(staffSideMarginMm / heightMm);
        x1 = t.applyX((widthMm - staffSideMarginMm) / heightMm);

        for (int i = 0; i < staveCount; i++) {
            for (int j = 0; j < 5; j++) {
                y = t.applyY((staffTopMarginMm + i * staffSpacing + j * lineSpacingMm) / heightMm);
                c.drawLine(x0, y, x1, y, paint);
            }
        }
    }

    private void draw_todolist(Canvas c, Transformation t, float lineSpacing, float margin) {

        float spacingMm = lineSpacing;
        if (Hardware.isEinkUsingLargerUI()) {
            spacingMm = (float) (spacingMm * 1.7);
        }
        float vertLineMm = margin;

        if (t.scale < threshold)
            shade += (int) ((threshold - t.scale) / threshold * (0xff - shade));
        paint.setARGB(0xff, shade, shade, shade);

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);
        int n = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm) - 2;
        float x0 = t.applyX(marginMm / heightMm);
        float x1 = t.applyX((widthMm - marginMm) / heightMm);
        float y0 = t.applyY(((heightMm - n * spacingMm) / 2) / heightMm);
        float y1 = t.applyY(((heightMm - n * spacingMm) / 2 + spacingMm) / heightMm);
        float x2, x3, y2, y3;

        float checkboxmargin = (y1 - y0) / 8;
        float checkboxspaceing = checkboxmargin * 6;
        for (int i = 1; i <= n - 1; i++) {
            y0 = t.applyY(((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm);
            x2 = x0 + checkboxmargin;
            x3 = x2 + checkboxspaceing;
            y2 = y0 - checkboxspaceing - checkboxmargin;
            y3 = y2 + checkboxspaceing;
            c.drawLine(x0, y0, x1, y0, paint);
            paint.setStyle(Paint.Style.STROKE);
            c.drawRect(x2, y2, x3, y3, paint);
        }
        paint.setStyle(Paint.Style.FILL);

    }

    private void draw_minutes(Canvas c, Transformation t, float lineSpacing, float margin) {
        Log.v(TAG, "draw_minutes=====>>>");
        float spacingMm = lineSpacing;
        int nIndex = 1;
        boolean bEink6Inch = false;
        if (Hardware.isEinkUsingLargerUI()) {
            spacingMm = (float) (spacingMm * 1.7);
            bEink6Inch = true;
            nIndex = 0;
        }
        float vertLineMm = margin;

        if (t.scale < threshold)
            shade += (int) ((threshold - t.scale) / threshold * (0xff - shade));
        paint.setARGB(0xff, shade, shade, shade);

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);
        int n = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm) - 2;
        float x0 = t.applyX(marginMm / heightMm);
        float x1 = t.applyX((widthMm - marginMm) / heightMm);
        float y0 = t.applyY(((heightMm - n * spacingMm) / 2) / heightMm);
        float y1 = t.applyY(((heightMm - n * spacingMm) / 2 + spacingMm) / heightMm);
        float x2, x3, y2, y3;

        float checkboxmargin = (y1 - y0) / 8;
        float checkboxspaceing = checkboxmargin * 6;

        Log.v(TAG, "margin:" + margin + " lineSpacing:" + lineSpacing + " spacingMm:" + spacingMm);
        Log.v(TAG, "x0:" + x0 + " x1:" + x1);
        Log.v(TAG, " widthMm:" + widthMm + ", heightMm:" + heightMm);

        for (int i = nIndex; i <= n; i++) {
            y0 = t.applyY(((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm);
            c.drawLine(x0, y0, x1, y0, paint);
            Log.v(TAG, "x0:" + x0 + ", x1:" + x1 + ", y0:" + y0);
        }


        float textxoffset = lineSpacing;
        float lineyoffset = margin / lineSpacing;
        float textsize = margin * 2;

        paint.setTextSize(textsize);

        if (bEink6Inch) {
            for (int i = 0; i <= 8; i++) {
                y0 = t.applyY(((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm);
                y1 = y0 - spacingMm;
                y2 = y0 - lineyoffset;
                if (i == 0) {
                    y2 = y0 - lineyoffset * 10;
                    c.drawText("Page", x0, y2, paint);
                    y2 = y0 - lineyoffset * 2;
                    c.drawLine(x0, y2, x1, y2, paint);
                }
                if (i == 1) {
                    y2 = y0 - checkboxspaceing - checkboxmargin;
                    y3 = y2 + checkboxspaceing;
                    x2 = x0 + textsize * 3;
                    x3 = (x1 - x0) / 2 - textxoffset;
                    c.drawText("Date", x0, y1, paint);
                    c.drawText("Time", x3 + textxoffset, y1, paint);
                    c.drawLine(x0, y0 - lineyoffset, x1, y0 - lineyoffset, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    c.drawRect(x2, y2, x3, y3, paint);
                    x2 = x3 + textsize * 3;
                    x3 = x1;
                    c.drawRect(x2, y2, x3, y3, paint);
                }
                if (i == 2) {
                    paint.setStyle(Paint.Style.FILL);
                    c.drawText("Agenda", x0, y1, paint);
                    c.drawLine(x0, y2, x1, y2, paint);
                }
                if (i == 4) {
                    c.drawText("Attendees", x0, y1, paint);
                    c.drawLine(x0, y2, x1, y2, paint);
                }
                if (i == 7) {
                    c.drawText("Note", x0, y1, paint);
                    c.drawLine(x0, y2, x1, y2, paint);
                }
            }
        } else {
            for (int i = 1; i <= 9; i++) {
                y0 = t.applyY(((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm);
                y1 = y0 - spacingMm;
                y2 = y0 - lineyoffset;
                if (i == 1) {
                    y2 = y0 - lineyoffset * 10;
                    c.drawText("Page", x0, y2, paint);
                    y2 = y0 - lineyoffset * 2;
                    c.drawLine(x0, y2, x1, y2, paint);
                }
                if (i == 2) {
                    y2 = y0 - checkboxspaceing - checkboxmargin;
                    y3 = y2 + checkboxspaceing;
                    x2 = x0 + textsize * 3;
                    x3 = (x1 - x0) / 2 - textxoffset;
                    c.drawText("Date", x0, y1, paint);
                    c.drawText("Time", x3 + textxoffset, y1, paint);
                    c.drawLine(x0, y0 - lineyoffset, x1, y0 - lineyoffset, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    c.drawRect(x2, y2, x3, y3, paint);
                    x2 = x3 + textsize * 3;
                    x3 = x1;
                    c.drawRect(x2, y2, x3, y3, paint);
                }
                if (i == 3) {
                    paint.setStyle(Paint.Style.FILL);
                    c.drawText("Agenda", x0, y1, paint);
                    c.drawLine(x0, y2, x1, y2, paint);
                }
                if (i == 5) {
                    c.drawText("Attendees", x0, y1, paint);
                    c.drawLine(x0, y2, x1, y2, paint);
                }
                if (i == 8) {
                    c.drawText("Note", x0, y1, paint);
                    c.drawLine(x0, y2, x1, y2, paint);
                }
            }
        }
        paint.setStyle(Paint.Style.FILL);

    }

    private void draw_stave(Canvas c, Transformation t, float lineSpacing, float margin) {

        float spacingMm = lineSpacing;
        if (Hardware.isEinkUsingLargerUI()) {
            spacingMm = (float) (spacingMm * 1.7);
        }
        if (t.scale < threshold)
            shade += (int) ((threshold - t.scale) / threshold * (0xff - shade));
        paint.setARGB(0xff, shade, shade, shade);

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);
        int n = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm) - 2;
        float x0 = t.applyX(marginMm / heightMm);
        float x1 = t.applyX((widthMm - marginMm) / heightMm);
        float y0 = t.applyY(((heightMm - n * spacingMm) / 2) / heightMm);
        float y1 = t.applyY(((heightMm - n * spacingMm) / 2 + spacingMm) / heightMm);

        float linemargin = (y1 - y0) / 4;

        for (int i = 0; i <= n - 1; i += 3) {
            y0 = t.applyY(((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm);
            for (int j = 0; j < 5; j++) {
                y1 = y0 + linemargin * j;
                c.drawLine(x0, y1, x1, y1, paint);
            }
        }

    }

    private void draw_diary(Canvas c, Transformation t, float lineSpacing, float margin) {

        float spacingMm = lineSpacing;
        if (Hardware.isEinkUsingLargerUI()) {
            spacingMm = (float) (spacingMm * 1.7);
        }

        if (t.scale < threshold)
            shade += (int) ((threshold - t.scale) / threshold * (0xff - shade));
        paint.setARGB(0xff, shade, shade, shade);

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);
        int n = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm) - 2;
        float x0 = t.applyX(marginMm / heightMm);
        float x1 = t.applyX((widthMm - marginMm) / heightMm);
        float y0 = t.applyY(((heightMm - n * spacingMm) / 2) / heightMm);
        float y1 = t.applyY(((heightMm - n * spacingMm) / 2 + spacingMm) / heightMm);
        float offsety = y0 + (y1 - y0) / 2 - t.applyY(spacingMm / heightMm);

        float textsize = margin * 2;
        paint.setTextSize(textsize);

        c.drawText("Date                /", x0, offsety, paint);
        for (int i = 0; i <= n; i++) {
            y0 = t.applyY(((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm);
            c.drawLine(x0, y0, x1, y0, paint);
        }

    }


    public void render(Artist artist) {
        if (!artist.getBackgroundVisible()) return;

        switch (paperType) {
            case EMPTY:
                return;
            case RULED:
                render_ruled(artist, LEGALRULED_SPACING, 31.75f);
                return;
            case COLLEGERULED:
                render_ruled(artist, COLLEGERULED_SPACING, 25.0f);
                return;
            case NARROWRULED:
                render_ruled(artist, NARROWRULED_SPACING, 25.0f);
                return;
            case QUAD:
                render_quad(artist);
                return;
            case CORNELLNOTES:
                render_cornellnotes(artist);
                return;
            case DAYPLANNER:
                return;
            case MUSIC:
                render_music_manuscript(artist);
            case CALLIGRAPHY_SMALL:
                render_calligraphy(artist, CALLIGRAPHY_TYPE_SMALL);
                return;
            case CALLIGRAPHY_BIG:
                render_calligraphy(artist, CALLIGRAPHY_TYPE_BIG);
                return;
            case TODOLIST:
                render_todolist(artist, TODOLIST_SPACING, 25.0f);
                return;
            case MINUTES:
                render_minutes(artist, TODOLIST_SPACING, 25.0f);
                return;
            case STAVE:
                render_stave(artist, STAVE_SPACING, 25.0f);
                return;
            case DIARY:
                render_diary(artist, TODOLIST_SPACING, 25.0f);
                return;
            case HEX:
                return;
            case CUSTOMIZED:
                render_customized(artist);
        }
    }


    private void render_ruled(Artist artist, float lineSpacing, float margin) {
        float spacingMm = lineSpacing;
        if (Hardware.isEinkUsingLargerUI()) {
            spacingMm = (float) (spacingMm * 1.7);
        }
        float vertLineMm = margin;
        LineStyle line = new LineStyle();
        line.setColor(0f, 0f, 0f);
        line.setWidth(0);
        int n = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm) - 2;
        float x0 = marginMm / heightMm;
        float x1 = (widthMm - marginMm) / heightMm;
        if (lineSpacing == COLLEGERULED_SPACING) {
            for (int i = 0; i <= n; i++) {
                float y = ((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm;
                artist.drawLine(x0, y, x1, y, line);
            }
        } else if (lineSpacing == NARROWRULED_SPACING) {

            for (int i = 1; i <= n - 1; i++) {
                float y = ((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm;
                artist.drawLine(x0, y, x1, y, line);
            }

        } else {

            for (int i = 1; i <= n - 2; i++) {
                float y = ((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm;
                artist.drawLine(x0, y, x1, y, line);
            }

        }


        // Paint margin
        if (margin > 0.0f) {
            line.setColor(1f, 0f, 0f);
            line.setWidth(0);
            float y0 = marginMm / heightMm;
            float y1;

            if (lineSpacing == COLLEGERULED_SPACING) {

                y1 = (heightMm - marginMm - spacingMm) / heightMm;

            } else if (lineSpacing == NARROWRULED_SPACING) {

                y1 = (heightMm - marginMm - spacingMm * 1.5f) / heightMm;

            } else {

                y1 = (heightMm - marginMm - spacingMm * 2) / heightMm;

            }

            float x = vertLineMm / widthMm;
            artist.drawLine(x, y0, x, y1, line);
        }
    }

    private void render_quad(Artist artist) {
        float spacingMm = 10f;
        if (Hardware.isEinkUsingLargerUI()) {
            spacingMm = (float) (spacingMm * 1.7);
        }
        int nx, ny;
        float x, x0, x1, y, y0, y1;
        LineStyle line = new LineStyle();
        line.setColor(0f, 0f, 0f);
        line.setWidth(0);
        ny = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm);
        nx = (int) Math.floor((widthMm - 2 * marginMm) / spacingMm);
        float marginXMm = (widthMm - nx * spacingMm) / 2;
        float marginYMm = (heightMm - ny * spacingMm) / 2;
        x0 = marginXMm / heightMm;
        x1 = (widthMm - marginXMm) / heightMm;
        y0 = marginYMm / heightMm;
        y1 = (heightMm - marginYMm - spacingMm) / heightMm;
        for (int i = 0; i < ny; i++) {
            y = (marginYMm + i * spacingMm) / heightMm;
            artist.drawLine(x0, y, x1, y, line);
        }
        for (int i = 0; i <= nx; i++) {
            x = (marginXMm + i * spacingMm) / heightMm;
            artist.drawLine(x, y0, x, y1, line);
        }
    }

    private void render_calligraphy(Artist artist, int type) {
        int positionBufferNum = 0; // base on the max value of ny and nx
        float spacingMmBase = 10f; // base on quad paper
        float spacingMm = 0;

        // calculate spacing according to type
        if (type == CALLIGRAPHY_TYPE_SMALL)
            spacingMm = (float) 6 * spacingMmBase;
        else
            // CALLIGRAPHY_TYPE_BIG
            spacingMm = (float) 8 * spacingMmBase;

		/*
		if (Hardware.isEink6InchHardwareType()) {
			spacingMm = (float) (spacingMm * 1.7);
		}
		*/
        int nx, ny;
        float x, x0, x1, y, y0, y1;

        LineStyle line = new LineStyle();
        line.setColor(0f, 0f, 0f);
        line.setWidth(0);
        ny = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm);
        nx = (int) Math.floor((widthMm - 2 * marginMm) / spacingMm);
        float marginXMm = (widthMm - nx * spacingMm) / 2;
        float marginYMm = (heightMm - ny * spacingMm) / 2;
        x0 = marginXMm / heightMm;
        x1 = (widthMm - marginXMm) / heightMm;
        y0 = (marginYMm * 2 / 3) / heightMm;
        y1 = (heightMm - marginYMm * 4 / 3) / heightMm;

        if (nx >= ny)
            positionBufferNum = nx + 1;
        else
            positionBufferNum = ny + 1;

        // for recording x, y positions
        float[] position_x = new float[positionBufferNum];
        float[] position_y = new float[positionBufferNum];

        // draw frame line
        // draw "-" line
        for (int i = 0; i <= ny; i++) {
            y = (marginYMm * 2 / 3 + i * spacingMm) / heightMm;
            artist.drawLine(x0, y, x1, y, line);
            position_y[i] = y;
        }
        // draw "|" line
        for (int i = 0; i <= nx; i++) {
            x = (marginXMm + i * spacingMm) / heightMm;
            artist.drawLine(x, y0, x, y1, line);
            position_x[i] = x;
        }

        // debug position info
		/*
		for (int i=0; i<=nx; i++) {
			ALog.debug("position_x["+i+"]:" +position_x[i]);
		}
		for (int i=0; i<=ny; i++) {
			ALog.debug("position_y["+i+"]:" +position_y[i]);
		}
		 */

        // draw "x" and '+' line
        line.setWidth(0);
        for (int i = 0; i < ny; i++) {
            for (int j = 0; j < nx; j++) {
                // draw "\" line
                artist.drawLine(position_x[j], position_y[i], position_x[j + 1], position_y[i + 1], line);
                // draw "|" line
                artist.drawLine((position_x[j] + position_x[j + 1]) / 2, position_y[i],
                        (position_x[j] + position_x[j + 1]) / 2, position_y[ny], line);
                // draw "/" line
                artist.drawLine(position_x[j + 1], position_y[i], position_x[j], position_y[i + 1], line);
                // draw "-" line
                artist.drawLine(position_x[j], (position_y[i] + position_y[i + 1]) / 2, position_x[nx],
                        (position_y[i] + position_y[i + 1]) / 2, line);
            }
        }
    }

    private void render_cornellnotes(Artist artist) {
        float x0, x1, y0, y1;
        final float MARGIN = 1.25f;
        LineStyle line = new LineStyle();
        line.setColor(0f, 0f, 0f);
        line.setWidth(0);

        // Details
        float spacingMm = COLLEGERULED_SPACING;
        if (Hardware.isEinkUsingLargerUI()) {
            spacingMm = (float) (spacingMm * 1.7);
        }
        int n = (int) Math.floor((heightMm - (MARGIN * INCH_in_MM) - 2 * marginMm) / spacingMm);

        x0 = (MARGIN * INCH_in_MM) / widthMm + marginMm / heightMm;
        x1 = (widthMm - marginMm) / heightMm;

        for (int i = 1; i <= n - 3; i++) {
            float y = (heightMm - n * spacingMm - MARGIN * INCH_in_MM + i * spacingMm) / heightMm;
            artist.drawLine(x0, y, x1, y, line);
        }

        // Cue Column
        x0 = (MARGIN * INCH_in_MM) / widthMm;
        x1 = x0;
        y0 = 0f;
        y1 = (heightMm - spacingMm * 2 - (MARGIN * INCH_in_MM)) / heightMm;
        artist.drawLine(x0, y0, x1, y1, line);

        // Summary area at base of page
        x0 = 0f;
        x1 = widthMm / heightMm;
        y0 = (heightMm - spacingMm * 2 - (MARGIN * INCH_in_MM)) / heightMm;
        y1 = y0;
        artist.drawLine(x0, y0, x1, y1, line);
    }

    private void render_music_manuscript(Artist artist) {
        float lineSpacingMm = 2.0f;
        float staveHeight = 4 * lineSpacingMm;
        float staffTopMarginMm = 25.0f;
        float staffBottomMarginMm = 15.0f;
        float staffSideMarginMm = 15.0f;

        int staveCount = 12;
        if (aspectRatio.isPortrait())
            staveCount = 12;
        else
            staveCount = 8;
        float staffTotal = staffTopMarginMm + staffBottomMarginMm + staveCount * staveHeight;
        float staffSpacing = staveHeight + (heightMm - staffTotal) / (staveCount - 1);

        float x0, x1, y;
        LineStyle line = new LineStyle();
        line.setColor(0f, 0f, 0f);

        line.setWidth(0);
        staffSpacing = staveHeight + (heightMm - staffTotal) / (staveCount - 1);

        x0 = staffSideMarginMm / heightMm;
        x1 = (widthMm - staffSideMarginMm) / heightMm;

        for (int i = 0; i < staveCount; i++) {
            for (int j = 0; j < 5; j++) {
                y = (staffTopMarginMm + i * staffSpacing + j * lineSpacingMm) / heightMm;
                artist.drawLine(x0, y, x1, y, line);
            }
        }
    }

    private void render_todolist(Artist artist, float lineSpacing, float margin) {

        Log.v(TAG, "render_todolist ===>>");
        float spacingMm = lineSpacing;
        if (Hardware.isEinkUsingLargerUI()) {
            spacingMm = (float) (spacingMm * 1.7);
        }
        float vertLineMm = margin;
        LineStyle line = new LineStyle();
        line.setColor(0f, 0f, 0f);
        line.setWidth(0);
        int n = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm) - 2;
        float x0 = marginMm / heightMm;
        float x1 = (widthMm - marginMm) / heightMm;
        float y0 = ((heightMm - n * spacingMm) / 2) / heightMm;
        float y1 = ((heightMm - n * spacingMm) / 2 + spacingMm) / heightMm;
        float x2, x3, y2, y3;
        float checkboxmargin = (y1 - y0) / 8;
        float checkboxspaceing = checkboxmargin * 6;
        Log.v(TAG, "heightMm:" + heightMm + ", widthMm:" + widthMm + ", marginMm:" + marginMm + ", lineN:" + n + ", spacingMm:" + spacingMm);
        Log.v(TAG, "x0:" + x0 + ", x1:" + x1);
        Log.v(TAG, "y0:" + y0 + ",y1:" + y1 + ",checkboxmargin:" + checkboxmargin + ", checkboxspaceing:" + checkboxspaceing);

        for (int i = 1; i <= n - 1; i++) {
            y0 = ((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm;
            Log.v(TAG, "Y:" + y0);
            artist.drawLine(x0, y0, x1, y0, line);
            x2 = x0 + checkboxmargin;
            x3 = x2 + checkboxspaceing;
            y2 = y0 - checkboxspaceing - checkboxmargin;
            y3 = y2 + checkboxspaceing;
            artist.drawLine(x2, y2, x3, y2, line);
            artist.drawLine(x3, y2, x3, y3, line);
            artist.drawLine(x3, y3, x2, y3, line);
            artist.drawLine(x2, y3, x2, y2, line);
        }

    }

    private void render_minutes(Artist artist, float lineSpacing, float margin) {
        Log.v(TAG, "render_minutes ====>>>");
        float spacingMm = lineSpacing;
        float textsize = margin / widthMm / spacingMm / 2;
        int nIndex = 1;
        boolean bEink6Inch = false;
        if (Hardware.isEinkUsingLargerUI()) {
            spacingMm = (float) (spacingMm * 1.7);
            textsize = margin / widthMm / spacingMm;
            bEink6Inch = true;
            nIndex = 0;
        }
        float vertLineMm = margin;
        LineStyle line = new LineStyle();
        line.setColor(0f, 0f, 0f);
        line.setWidth(0);
        int n = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm) - 2;
        float x0 = marginMm / heightMm;
        float x1 = (widthMm - marginMm) / heightMm;
        float y0 = ((heightMm - n * spacingMm) / 2) / heightMm;
        float y1 = ((heightMm - n * spacingMm) / 2 + spacingMm) / heightMm;
        float x2, x3, y2, y3;
        float checkboxmargin = (y1 - y0) / 8;
        float checkboxspaceing = checkboxmargin * 6;
//		float textxoffset = lineSpacing/widthMm/10;
        float textxoffset = lineSpacing / widthMm / spacingMm;
        float lineyoffset = checkboxmargin / 6;

        for (int i = nIndex; i <= n; i++) {
            y0 = ((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm;
            artist.drawLine(x0, y0, x1, y0, line);
        }

        Log.v(TAG, "margin:" + margin + " lineSpacing:" + lineSpacing + " spacingMm:" + spacingMm + " widthMm:" + widthMm + " heightMm:" + heightMm);
        Log.v(TAG, "x0:" + x0 + " x1:" + x1);

        if (bEink6Inch) {
            for (int i = 0; i <= 8; i++) {
                y0 = ((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm;
                y1 = y0 - spacingMm;
                y2 = y0 - lineyoffset;
                Log.v(TAG, "y0:" + y0 + " y1:" + y1 + " y2:" + y2);
                if (i == 0) {
                    y2 = y0 - lineyoffset * 2;
                    artist.drawLine(x0, y2, x1, y2, line);
                }
                if (i == 1) {
                    y2 = y0 - checkboxspaceing - checkboxmargin;
                    y3 = y2 + checkboxspaceing;
                    x2 = x0 + textsize * 12;
                    x3 = (x1 - x0) / 2 - textxoffset;
                    artist.drawLine(x0, y0 - lineyoffset, x1, y0 - lineyoffset, line);
                    artist.drawLine(x2, y2, x3, y2, line);
                    artist.drawLine(x3, y2, x3, y3, line);
                    artist.drawLine(x3, y3, x2, y3, line);
                    artist.drawLine(x2, y3, x2, y2, line);
                    x2 = x3 + textsize * 12;
                    x3 = x1;
                    artist.drawLine(x2, y2, x3, y2, line);
                    artist.drawLine(x3, y2, x3, y3, line);
                    artist.drawLine(x3, y3, x2, y3, line);
                    artist.drawLine(x2, y3, x2, y2, line);
                }
                if (i == 2 || i == 4 || i == 7) {
                    //draw text ?
                    artist.drawLine(x0, y2, x1, y2, line);
                }
            }
        } else {
            for (int i = 1; i <= 9; i++) {
                y0 = ((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm;
                y1 = y0 - spacingMm;
                y2 = y0 - lineyoffset;
                Log.v(TAG, "y0:" + y0 + " y1:" + y1 + " y2:" + y2);
                if (i == 1) {
                    y2 = y0 - lineyoffset * 2;
                    artist.drawLine(x0, y2, x1, y2, line);
                }
                if (i == 2) {
                    y2 = y0 - checkboxspaceing - checkboxmargin;
                    y3 = y2 + checkboxspaceing;
                    x2 = x0 + textsize * 12;
                    x3 = (x1 - x0) / 2 - textxoffset;
                    artist.drawLine(x0, y0 - lineyoffset, x1, y0 - lineyoffset, line);
                    artist.drawLine(x2, y2, x3, y2, line);
                    artist.drawLine(x3, y2, x3, y3, line);
                    artist.drawLine(x3, y3, x2, y3, line);
                    artist.drawLine(x2, y3, x2, y2, line);
                    x2 = x3 + textsize * 12;
                    x3 = x1;
                    artist.drawLine(x2, y2, x3, y2, line);
                    artist.drawLine(x3, y2, x3, y3, line);
                    artist.drawLine(x3, y3, x2, y3, line);
                    artist.drawLine(x2, y3, x2, y2, line);
                }
                if (i == 3 || i == 5 || i == 8) {
                    //draw text ?
                    artist.drawLine(x0, y2, x1, y2, line);
                }
            }
        }

    }

    private void render_stave(Artist artist, float lineSpacing, float margin) {
        if (Hardware.isEinkUsingLargerUI()) {
            offset_bottom = 0.056f;
        } else {
            offset_bottom = 0.033f;
        }

        float spacingMm = lineSpacing;
        if (Hardware.isEinkUsingLargerUI()) {
            spacingMm = (float) (spacingMm * 1.7);
        }
        LineStyle line = new LineStyle();
        line.setColor(0f, 0f, 0f);
        line.setWidth(0);
        int n = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm) - 2;
        float x0 = marginMm / heightMm;
        float x1 = (widthMm - marginMm) / heightMm;
        float y0 = ((heightMm - n * spacingMm) / 2) / heightMm;
        float y1 = ((heightMm - n * spacingMm) / 2 + spacingMm) / heightMm;

        float linemargin = (y1 - y0) / 4;

        for (int i = 0; i <= n - 1; i += 3) {
            y0 = ((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm;
            for (int j = 0; j < 5; j++) {
                y1 = y0 + linemargin * j;
                artist.drawLine(x0, y1, x1, y1, line);
            }
        }
    }

    private void render_diary(Artist artist, float lineSpacing, float margin) {

        Log.v(TAG, "render_diary ===>>");
        float spacingMm = lineSpacing;
        if (Hardware.isEinkUsingLargerUI()) {
            spacingMm = (float) (spacingMm * 1.7);
        }
        LineStyle line = new LineStyle();
        line.setColor(0f, 0f, 0f);
        line.setWidth(0);
        int n = (int) Math.floor((heightMm - 2 * marginMm) / spacingMm) - 2;
        float x0 = marginMm / heightMm;
        float x1 = (widthMm - marginMm) / heightMm;
        float y0;

        for (int i = 0; i <= n; i++) {
            y0 = ((heightMm - n * spacingMm) / 2 + i * spacingMm) / heightMm;
            artist.drawLine(x0, y0, x1, y0, line);
        }

    }

    private Bitmap loadBitmap(String file_path, RectF bBox) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;

        bitmap = BitmapFactory.decodeFile(file_path, options);
        int oldwidth = bitmap.getWidth();
        int oldheight = bitmap.getHeight();

        float scale;

        Matrix matrix = new Matrix();

        if (oldwidth >= oldheight) {
            scale = bBox.width() / (float) oldwidth;
        } else {
            scale = bBox.height() / (float) oldheight;
        }

        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(bitmap, 0, 0, oldwidth, oldheight, matrix, true);
    }

    public void ClearBitmap() {
        bitmap = null;
    }

    private void draw_customized(Canvas canvas, RectF bBox) {

        File file = new File(paperPath);
        if (!file.exists()) return;
        if (!paperPath.equals("na")) {
            //  2018 0921  Alan   no matter if bitmap exists or not, load bitmap again to fit to screen when entering overview mode
//        	else if (bitmap!=null ){

            if (bitmap == null || (bitmap.getHeight() != canvas.getHeight() && bitmap.getWidth() != canvas.getWidth())) {
                bBox.top = 0;
                bBox.bottom = canvas.getHeight();
                bBox.left = 0;
                bBox.right = canvas.getWidth();
                bitmap = loadBitmap(paperPath, bBox);
                centerWidth = (int) (bBox.width() - bitmap.getWidth()) / 2;
                centerHeight = (int) (bBox.height() - bitmap.getHeight()) / 2;
            }
        }

        canvas.drawBitmap(bitmap,
                centerWidth,
                centerHeight,
                null);
    }

    private void drawPNG_customized(Canvas canvas, RectF bBox) {
        File file = new File(paperPath);
        if (!file.exists()) return;

        if (!paperPath.equals("na")) {
//            if (bitmap == null) {
            bitmap = loadBitmap(paperPath, bBox);
            centerWidth = (int) (bBox.width() - bitmap.getWidth()) / 2;
            centerHeight = (int) (bBox.height() - bitmap.getHeight()) / 2;
//            }
            canvas.drawBitmap(bitmap,
                    centerWidth,
                    centerHeight,
                    null);
            bitmap = null;
        }
    }

    private void render_customized(Artist artist) {
        Bitmap bmp = loadPdfBitmap(paperPath, artist.getPdf().getWidth() * 2, artist.getPdf().getHeight() * 2);

        File file = savebitmap(bmp);

        if (!file.exists()) return;

        artist.imageBackground(file, 0, 0, artist.getPdf().getWidth(), artist.getPdf().getHeight());

    }

    float offset_left, offset_right, offset_top, offset_bottom;

    private Bitmap loadPdfBitmap(String file_path, float width, float height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;

        Bitmap bmp = BitmapFactory.decodeFile(file_path, options);
        int oldwidth = bmp.getWidth();
        int oldheight = bmp.getHeight();

        float rate = Global.MACHINE_PIXEL_RATE_VALUE;
        int calHeight = (int) Math.rint(width / rate);

        Matrix matrix = new Matrix();
        float scale;

        if (oldwidth >= oldheight) {
            scale = ((width) / (float) oldwidth);
        } else {
            scale = (calHeight / (float) oldheight);
        }
        matrix.postScale(scale, scale);

        Bitmap newBmp = Bitmap.createBitmap(bmp, 0, 0, oldwidth, oldheight, matrix, true);

        // 設定畫布大小
        Bitmap fittingBitmap = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(fittingBitmap);
        canvas.drawColor(Color.WHITE);    //white background

        int centerWidth = (int) (width - newBmp.getWidth()) / 2;
        int centerHeight = (calHeight - newBmp.getHeight()) / 2;

        canvas.drawBitmap(newBmp,
                centerWidth,
                centerHeight,
                null);
        return fittingBitmap;
    }

    private File savebitmap(Bitmap bmp) {
        String tempFile_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/nNote";
        String tempFile_name = "temp.jpg";

        File dir = new File(tempFile_path);
        if (!dir.exists())
            dir.mkdirs();

        File file = new File(dir, tempFile_name);

        OutputStream outStream = null;

        try {
            outStream = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e("file", "" + file);
        return file;

    }
}
