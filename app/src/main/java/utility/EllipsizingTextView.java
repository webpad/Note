package utility;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

public class EllipsizingTextView extends android.support.v7.widget.AppCompatTextView {
    private static final String ELLIPSIS = "...";

    public interface EllipsizeListener {
        void ellipsizeStateChanged(boolean ellipsized);
    }

    private final List<EllipsizeListener> ellipsizeListeners = new ArrayList<EllipsizeListener>();
    private boolean isEllipsized;
    private boolean isStale;
    private boolean programmaticChange;
    private String fullText;
    private int maxLines = -1;
    private float lineSpacingMultiplier = 1.0f;
    private float lineAdditionalVerticalPadding = 0.0f;
    private EllipsizeStrategy mEllipsizeStrategy;

    private abstract class EllipsizeStrategy {
        public String processText(String text) {
            if (!isInLayout(text)) {
                return createEllipsizedText(text);
            }
            return text;
        }

        public boolean isInLayout(String text) {
            Layout layout = createWorkingLayout(text);
            return layout.getLineCount() <= maxLines;
        }

        protected Layout createWorkingLayout(String workingText) {
            return new StaticLayout(workingText, getPaint(), getWidth() - getPaddingLeft() - getPaddingRight(),
                    Alignment.ALIGN_NORMAL, lineSpacingMultiplier, lineAdditionalVerticalPadding, false);
        }

        protected abstract String createEllipsizedText(String fullText);
    }

    private class EllipsizeNoneStrategy extends EllipsizeStrategy {
        @Override
        protected String createEllipsizedText(String fullText) {
            return fullText;
        }
    }


    private class EllipsizeEndStrategy extends EllipsizeStrategy {
        @Override
        protected String createEllipsizedText(String fullText) {
            Layout layout = createWorkingLayout(fullText);
            int cutOffIndex = layout.getLineEnd(maxLines - 1);
            String workingText = fullText.substring(0, cutOffIndex).trim();
            while (createWorkingLayout(workingText + ELLIPSIS).getLineCount() > maxLines) {
                int lastSpace = workingText.lastIndexOf(' ');
                if (lastSpace == -1) {
                    break;
                }
                workingText = workingText.substring(0, lastSpace);
            }
            workingText = workingText + ELLIPSIS;
            return workingText;
        }
    }

    private class EllipsizeStartStrategy extends EllipsizeStrategy {
        @Override
        protected String createEllipsizedText(String fullText) {
            Layout layout = createWorkingLayout(fullText);
            int cutOffIndex = layout.getLineEnd(maxLines - 1);
            int textLength = fullText.length();
            int cutOffLength = textLength - cutOffIndex;
            if (cutOffLength < ELLIPSIS.length()) cutOffLength = ELLIPSIS.length();
            String workingText = fullText.substring(cutOffLength, textLength).trim();
            while (createWorkingLayout(workingText + ELLIPSIS).getLineCount() > maxLines) {
                int firstSpace = workingText.indexOf(' ');
                if (firstSpace == -1) {
                    break;
                }
                workingText = workingText.substring(firstSpace, workingText.length());
            }
            workingText = ELLIPSIS + workingText;
            return workingText;
        }
    }

    private class EllipsizeMiddleStrategy extends EllipsizeStrategy {
        @Override
        protected String createEllipsizedText(String fullText) {
            Layout layout = createWorkingLayout(fullText);
            int cutOffIndex = layout.getLineEnd(maxLines - 1);
            int textLength = fullText.length();
            int cutOffLength = textLength - cutOffIndex;
            if (cutOffLength < ELLIPSIS.length()) cutOffLength = ELLIPSIS.length();
            cutOffLength += cutOffIndex % 2; // make it even
            String firstPart = fullText.substring(0, textLength / 2 - cutOffLength / 2).trim();
            String secondPart = fullText.substring(textLength / 2 + cutOffLength / 2, textLength).trim();
            while (createWorkingLayout(firstPart + ELLIPSIS + secondPart).getLineCount() > maxLines) {
                int lastSpaceFirstPart = firstPart.lastIndexOf(' ');
                int firstSpaceSecondPart = secondPart.indexOf(' ');
                if (lastSpaceFirstPart == -1 || firstSpaceSecondPart == -1) {
                    break;
                }
                firstPart = firstPart.substring(0, lastSpaceFirstPart).trim();
                secondPart = secondPart.substring(firstSpaceSecondPart, secondPart.length()).trim();
            }
            return firstPart + ELLIPSIS + secondPart;
        }
    }

    public EllipsizingTextView(Context context) {
        super(context);
    }

    public EllipsizingTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getValuesFromAttributes(context, attrs, 0);
    }

    public EllipsizingTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getValuesFromAttributes(context, attrs, defStyle);
    }

    private void getValuesFromAttributes(Context context, AttributeSet attrs, int defStyle) {
        TypedArray a = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.maxLines, android.R.attr.ellipsize}, defStyle, 0);
        setMaxLines(a.getInt(0, 2));
        a.recycle();
    }

    @Override
    public void setMaxLines(int maxLines) {
        super.setMaxLines(maxLines);
        this.maxLines = maxLines;
        isStale = true;
    }

    public void addEllipsizeListener(EllipsizeListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        ellipsizeListeners.add(listener);
    }

    public void removeEllipsizeListener(EllipsizeListener listener) {
        ellipsizeListeners.remove(listener);
    }

    public boolean isEllipsized() {
        return isEllipsized;
    }

    @Override
    public void setLineSpacing(float add, float mult) {
        this.lineAdditionalVerticalPadding = add;
        this.lineSpacingMultiplier = mult;
        super.setLineSpacing(add, mult);
    }

    @Override
    public void setEllipsize(TruncateAt where) {
        switch (where) {
            case END:
                mEllipsizeStrategy = new EllipsizeEndStrategy();
                break;
            case START:
                mEllipsizeStrategy = new EllipsizeStartStrategy();
                break;
            case MIDDLE:
                mEllipsizeStrategy = new EllipsizeMiddleStrategy();
                break;
            case MARQUEE:
            default:
                mEllipsizeStrategy = new EllipsizeNoneStrategy();
                break;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isStale) {
            super.setEllipsize(null);
            resetText();
        }
        super.onDraw(canvas);
    }

    private void resetText() {
        int maxLines = getMaxLines();
        String workingText = fullText;
        boolean ellipsized = false;
        if (maxLines != -1) {
            workingText = mEllipsizeStrategy.processText(fullText);
            ellipsized = !mEllipsizeStrategy.isInLayout(fullText);
        }
        if (!workingText.equals(getText())) {
            programmaticChange = true;
            try {
                setText(workingText);
            } finally {
                programmaticChange = false;
            }
        }
        isStale = false;
        if (ellipsized != isEllipsized) {
            isEllipsized = ellipsized;
            for (EllipsizeListener listener : ellipsizeListeners) {
                listener.ellipsizeStateChanged(ellipsized);
            }
        }
    }

    public int getMaxLines() {
        return maxLines;
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        super.onTextChanged(text, start, before, after);
        if (!programmaticChange) {
            fullText = text.toString();
            isStale = true;
        }
    }
}