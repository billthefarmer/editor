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
import android.widget.TextView;

public class LineNumbersTextView extends TextView {
    private boolean isLineNumbersEnabled;
    private LineNumbersDrawer lineNumbersDrawer;

    public LineNumbersTextView(Context context) {
        super(context);
    }

    public LineNumbersTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineNumbersTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isLineNumbersEnabled) {
            lineNumbersDrawer.draw(canvas);
        }
    }

    public void setEditText(final EditText editor) {
        lineNumbersDrawer = new LineNumbersDrawer(editor, this);
    }

    public void setLineNumbersEnabled(final boolean enabled) {
        if (enabled ^ isLineNumbersEnabled) {
            post(this::invalidate);
        }
        isLineNumbersEnabled = enabled;
        if (isLineNumbersEnabled) {
            lineNumbersDrawer.prepare();
        } else {
            lineNumbersDrawer.stop();
        }
    }

    static class LineNumbersDrawer {

        public final EditText _editor;
        public final LineNumbersTextView _textView;
        private final Paint _paint = new Paint();

        private static final int LINE_NUMBER_PADDING_LEFT = 1;
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
                _maxNumber -= countLines(s, start, start + count);
                _textView.setText(" ");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                _maxNumber += countLines(s, start, start + count);
                _textView.setText(" ");
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };

        public LineNumbersDrawer(final EditText editor, final LineNumbersTextView textView) {
            _editor = editor;
            _textView = textView;
            _paint.setColor(0xFF999999);
            _paint.setTextAlign(Paint.Align.RIGHT);
        }

        private int countLines(final CharSequence s, int start, int end) {
            int count = 0;
            for (int i = start; i < end; i++) {
                if (s.charAt(i) == '\n') {
                    count++;
                }
            }
            return count;
        }

        private boolean isTextSizeChanged() {
            final float textSize = _editor.getTextSize();
            if (textSize == _oldTextSize) {
                return false;
            } else {
                _paint.setTextSize(textSize);
                _oldTextSize = textSize;
                return true;
            }
        }

        private boolean isMaxNumberDigitsChanged() {
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

        private boolean isOutOfLineNumbersArea() {
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

        private void startLineTracking() {
            _editor.removeTextChangedListener(_lineTrackingWatcher);
            _maxNumber = 1;
            final CharSequence text = _editor.getText();
            if (text != null) {
                _maxNumber += countLines(text, 0, text.length());
            }
            _editor.addTextChangedListener(_lineTrackingWatcher);
        }

        private void stopLineTracking() {
            _editor.removeTextChangedListener(_lineTrackingWatcher);
        }

        public void prepare() {
            startLineTracking();
            _textView.setVisibility(VISIBLE);
        }

        public void updateState() {
            // If text size or the max line number of digits changed, update related variables
            if (isTextSizeChanged() || isMaxNumberDigitsChanged()) {
                _numberX = LINE_NUMBER_PADDING_LEFT + (int) _paint.measureText(String.valueOf(_maxNumber));
                _gutterX = _numberX + LINE_NUMBER_PADDING_RIGHT;
                _textView.setWidth(_gutterX + 1);
            }
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

            updateState();

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
         * Stop drawing line numbers and reset states.
         */
        public void stop() {
            stopLineTracking();
            _textView.setWidth(0);
            _textView.setVisibility(GONE);
            _maxNumberDigits = 0;
        }
    }
}
