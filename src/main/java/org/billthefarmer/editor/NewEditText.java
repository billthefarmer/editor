package org.billthefarmer.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

public class NewEditText extends EditText {
    private boolean isLineNumbersEnabled;
    private final LineNumbersDrawer lineNumbersDrawer = new LineNumbersDrawer(this);

    public NewEditText(Context context) {
        super(context);
    }

    public NewEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NewEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onPreDraw() {
        lineNumbersDrawer.setTextSize(getTextSize());
        return super.onPreDraw();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isLineNumbersEnabled) {
            lineNumbersDrawer.draw(canvas);
        }
    }

    public void setLineNumbersEnabled(final boolean enable) {
        if (enable ^ isLineNumbersEnabled) {
            post(this::invalidate);
        }
        isLineNumbersEnabled = enable;
        if (isLineNumbersEnabled) {
            lineNumbersDrawer.startLineTracking();
        } else {
            lineNumbersDrawer.reset();
            lineNumbersDrawer.stopLineTracking();
        }
    }

    /**
     * Count instances of a single char in a char sequence.
     */
    public static int countChar(final CharSequence s, int start, int end, final char c) {
        int count = 0;
        for (int i = start; i < end; i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    static class LineNumbersDrawer {

        private final EditText _editor;
        private final Paint _paint = new Paint();

        private final int _defaultPaddingLeft;
        private static final int LINE_NUMBER_PADDING_LEFT = 6;
        private static final int LINE_NUMBER_PADDING_RIGHT = 12;

        private final Rect _visibleArea = new Rect();
        private final Rect _lineNumbersArea = new Rect();

        private int _numberX;
        private int _gutterX;
        private int _maxNumber = 1; // to gauge gutter width
        private int _maxNumberDigits;
        private float _oldTextSize;
        private final int[] _startLine = {0, 1}; // {line index, actual line number}

        private final TextWatcher _lineTrackingWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                _maxNumber -= countChar(s, start, start + count, '\n');
                _maxNumber -= countChar(s, start, start + count, '\n');
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                _maxNumber += countChar(s, start, start + count, '\n');
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };

        public LineNumbersDrawer(final EditText editor) {
            _editor = editor;
            _paint.setColor(0xFF999999);
            _paint.setTextAlign(Paint.Align.RIGHT);
            _defaultPaddingLeft = editor.getPaddingLeft();
        }

        public void setTextSize(final float textSize) {
            _paint.setTextSize(textSize);
        }

        public boolean isTextSizeChanged() {
            if (_paint.getTextSize() == _oldTextSize) {
                return false;
            } else {
                _oldTextSize = _paint.getTextSize();
                return true;
            }
        }

        public boolean isMaxNumberDigitsChanged() {
            final int oldDigits = _maxNumberDigits;

            if (_maxNumber < 10) {
                _maxNumberDigits = 1;
            } else if (_maxNumber < 100) {
                _maxNumberDigits = 2;
            } else if (_maxNumber < 1000) {
                _maxNumberDigits = 3;
            } else if (_maxNumber < 10000) {
                _maxNumberDigits = 4;
            } else {
                _maxNumberDigits = 5;
            }
            return _maxNumberDigits != oldDigits;
        }

        public boolean isOutOfLineNumbersArea() {
            final int margin = (int) (_visibleArea.height() * 0.5f);
            final int top = _visibleArea.top - margin;
            final int bottom = _visibleArea.bottom + margin;

            if (top < _lineNumbersArea.top || bottom > _lineNumbersArea.bottom) {
                // Reset line numbers area
                // height of line numbers area = (1.5 + 1 + 1.5) * height of visible area
                _lineNumbersArea.top = top - _visibleArea.height();
                _lineNumbersArea.bottom = bottom + _visibleArea.height();
                return true;
            } else {
                return false;
            }
        }

        public void startLineTracking() {
            _editor.removeTextChangedListener(_lineTrackingWatcher);
            _maxNumber = 1;
            final CharSequence text = _editor.getText();
            if (text != null) {
                _maxNumber += countChar(text, 0, text.length(), '\n');
            }
            _editor.addTextChangedListener(_lineTrackingWatcher);
        }

        public void stopLineTracking() {
            _editor.removeTextChangedListener(_lineTrackingWatcher);
        }

        /**
         * Draw line numbers.
         *
         * @param canvas The canvas on which the line numbers will be drawn.
         */
        public void draw(final Canvas canvas) {
            if (!_editor.getLocalVisibleRect(_visibleArea)) {
                return;
            }

            final CharSequence text = _editor.getText();
            final Layout layout = _editor.getLayout();
            if (text == null || layout == null) {
                return;
            }

            // If text size or the max line number of digits changed,
            // update the variables and reset padding
            if (isTextSizeChanged() || isMaxNumberDigitsChanged()) {
                _numberX = LINE_NUMBER_PADDING_LEFT + (int) _paint.measureText(String.valueOf(_maxNumber));
                _gutterX = _numberX + LINE_NUMBER_PADDING_RIGHT;
                _editor.setPadding(_gutterX + 12, _editor.getPaddingTop(), _editor.getPaddingRight(), _editor.getPaddingBottom());
            }

            int i = _startLine[0], number = _startLine[1];
            // If current visible area is out of current line numbers area,
            // iterate from the first line to recalculate the start line
            if (isOutOfLineNumbersArea()) {
                i = 0;
                number = 1;
                _startLine[0] = -1;
            }

            // Draw border of the gutter
            canvas.drawLine(_gutterX, _lineNumbersArea.top, _gutterX, _lineNumbersArea.bottom, _paint);

            // Draw line numbers
            final int count = layout.getLineCount();
            final int offsetY = _editor.getPaddingTop();
            for (; i < count; i++) {
                final int start = layout.getLineStart(i);
                if (start == 0 || text.charAt(start - 1) == '\n') {
                    final int y = layout.getLineBaseline(i);
                    if (y > _lineNumbersArea.bottom) {
                        break;
                    }
                    if (y > _lineNumbersArea.top) {
                        if (_startLine[0] < 0) {
                            _startLine[0] = i;
                            _startLine[1] = number;
                        }
                        canvas.drawText(String.valueOf(number), _numberX, y + offsetY, _paint);
                    }
                    number++;
                }
            }
        }

        /**
         * Reset to the state without line numbers.
         */
        public void reset() {
            _editor.setPadding(_defaultPaddingLeft, _editor.getPaddingTop(), _editor.getPaddingRight(), _editor.getPaddingBottom());
            _maxNumberDigits = 0;
        }
    }
}
