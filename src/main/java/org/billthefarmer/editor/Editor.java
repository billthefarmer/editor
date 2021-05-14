////////////////////////////////////////////////////////////////////////////////
//
//  Editor - Text editor for Android
//
//  Copyright © 2017  Bill Farmer
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
////////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.editor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;

import android.support.v4.content.FileProvider;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.lang.ref.WeakReference;

import java.text.DateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Editor extends Activity
{
    public final static String TAG = "Editor";

    public final static String PATH = "path";
    public final static String EDIT = "edit";
    public final static String CHANGED = "changed";
    public final static String CONTENT = "content";
    public final static String MODIFIED = "modified";

    public final static String PREF_FILE = "pref_file";
    public final static String PREF_HIGHLIGHT = "pref_highlight";
    public final static String PREF_PATHS = "pref_paths";
    public final static String PREF_SAVE = "pref_save";
    public final static String PREF_VIEW = "pref_view";
    public final static String PREF_SIZE = "pref_size";
    public final static String PREF_SUGGEST = "pref_suggest";
    public final static String PREF_THEME = "pref_theme";
    public final static String PREF_TYPE = "pref_type";
    public final static String PREF_WRAP = "pref_wrap";

    public final static String DOCUMENTS = "Documents";
    public final static String FOLDER = "Folder:  ";

    public final static String NEW_FILE = "Untitled.txt";
    public final static String EDIT_FILE = "Editor.txt";
    public final static String HTML_FILE = "Editor.html";

    public final static String TEXT_HTML = "text/html";
    public final static String TEXT_PLAIN = "text/plain";
    public final static String TEXT_WILD = "text/*";

    public final static Pattern PATTERN_CHARS =
        Pattern.compile("[\\(\\)\\[\\]\\{\\}\\<\\>\"'`]");
    public final static String BRACKET_CHARS = "([{<";

    public final static String HTML_HEAD =
        "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n" +
        "<meta name=\"viewport\" content=\"width=device-width, " +
        "initial-scale=1.0\">\n</head>\n<body>\n";
    public final static String HTML_TAIL = "\n</body>\n</html>\n";
    public final static String FILE_PROVIDER =
        "org.billthefarmer.editor.fileprovider";

    public final static String CC_EXT =
        "\\.(c(c|pp|xx|\\+\\+)?|go|h|java|js|m|py|swift)";

    public final static String HTML_EXT =
        "\\.html?";

    public final static String CSS_EXT =
        "\\.css?";

    public final static String MD_EXT =
        "\\.md";

    public final static String SH_EXT =
        "\\.sh";

    // Syntax patterns
    public final static Pattern KEYWORDS = Pattern.compile
        ("\\b(abstract|and|arguments|as(m|sert|sociativity)?|auto|break|" +
         "case|catch|chan|char|class|con(st|tinue|venience)|continue|" +
         "de(bugger|f|fault|fer|in|init|l|lete)|didset|do(ne)?|dynamic" +
         "(type)?|el(if|se)|enum|esac|eval|ex(cept|ec|plicit|port|" +
         "tends|tension|tern)|fal(lthrough|se)|fi(nal|nally)?|for|" +
         "friend|from|func(tion)?|get|global|go(to)?|if|" +
         "im(plements|port)|in(fix|it|line|out|stanceof|terface|" +
         "ternal)?|is|lambda|lazy|left|let|local|map|mut(able|ating)|" +
         "namespace|native|new|nil|none|nonmutating|not|null|" +
         "operator|optional|or|override|package|pass|postfix|" +
         "pre(cedence|fix)|print|private|prot(ected|ocol)|public|" +
         "raise|range|register|required|return|right|select|self|" +
         "set|signed|sizeof|static|strictfp|struct|subscript|super|" +
         "switch|synchronized|template|th(en|is|rows?)|transient|" +
         "true|try|type(alias|def|id|name|of)?|un(ion|owned|signed)|" +
         "using|var|virtual|void|volatile|weak|wh(ere|ile)|willset|" +
         "with|yield)\\b", Pattern.MULTILINE);

    public final static Pattern TYPES = Pattern.compile
        ("\\b(j?bool(ean)?|(u|j)?(byte|char|double|float|int(eger)?|" +
         "long|short))\\b", Pattern.MULTILINE);

    public final static Pattern ANNOTATION =
        Pattern.compile("@\\b[A-Za-z]+\\b", Pattern.MULTILINE);

    public final static Pattern CC_COMMENT = Pattern.compile
        ("//.*$|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/",
         Pattern.MULTILINE);

    public final static Pattern CLASS = Pattern.compile
        ("\\b[A-Z][A-Za-z0-9_]+\\b", Pattern.MULTILINE);

    public final static Pattern CONSTANT = Pattern.compile
        ("\\b(([A-Z][A-Z0-9_]+)|(k[A-Z][A-Za-z0-9]+))\\b",
         Pattern.MULTILINE);

    public final static Pattern NUMBER = Pattern.compile
        ("\\b\\d+(\\.\\d*)?(e(\\+|\\-)?\\d+)?\\b",
         Pattern.MULTILINE);

    public final static Pattern QUOTED = Pattern.compile
        // "'([^\\\\']+|\\\\([btnfr\"'\\\\]|" +
        // "[0-3]?[0-7]{1,2}|u[0-9a-fA-F]{4}))*'|" +
        ("\"([^\\\\\"]+|\\\\([btnfr\"'\\\\]|" +
         "[0-3]?[0-7]{1,2}|u[0-9a-fA-F]{4}))*\"",
         Pattern.MULTILINE);

    public final static Pattern HTML_TAGS = Pattern.compile
        ("\\b(html|base|head|link|meta|style|title|body|address|article|" +
         "aside|footer|header|h\\d|hgroup|main|nav|section|blockquote|dd|" +
         "dir|div|dl|dt|figcaption|figure|hr|li|main|ol|p|pre|ul|a|abbr|" +
         "b|bdi|bdo|br|cite|code|data|dfn|em|i|kbd|mark|q|rb|rp|rt|rtc|" +
         "ruby|s|samp|small|span|strong|sub|sup|time|tt|u|var|wbr|area|" +
         "audio|img|map|track|video|applet|embed|iframe|noembed|object|" +
         "param|picture|source|canvas|noscript|script|del|ins|caption|" +
         "col|colgroup|table|tbody|td|tfoot|th|thead|tr|button|datalist|" +
         "fieldset|form|input|label|legend|meter|optgroup|option|output|" +
         "progress|select|textarea|details|dialog|menu|menuitem|summary|" +
         "content|element|shadow|slot|template|acronym|applet|basefont|" +
         "bgsound|big|blink|center|command|content|dir|element|font|" +
         "frame|frameset|image|isindex|keygen|listing|marquee|menuitem|" +
         "multicol|nextid|nobr|noembed|noframes|plaintext|shadow|spacer|" +
         "strike|tt|xmp|doctype)\\b",
         Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public final static Pattern HTML_ATTRS = Pattern.compile
        ("\\b(accept|accesskey|action|align|allow|alt|async|" +
         "auto(capitalize|complete|focus|play)|background|" +
         "bgcolor|border|buffered|challenge|charset|checked|cite|" +
         "class|code(base)?|color|cols|colspan|content(" +
         "editable)?|contextmenu|controls|coords|crossorigin|" +
         "csp|data|datetime|decoding|def(ault|er)|dir|dirname|" +
         "disabled|download|draggable|dropzone|enctype|enterkeyhint|" +
         "equiv|for|form(action|novalidate)?|headers|height|" +
         "hidden|high|href(lang)?|http|icon|id|importance|" +
         "inputmode|integrity|intrinsicsize|ismap|itemprop|keytype|" +
         "kind|label|lang|language|list|loading|loop|low|manifest|" +
         "max|maxlength|media|method|min|minlength|multiple|muted|" +
         "name|novalidate|open|optimum|pattern|ping|placeholder|" +
         "poster|preload|property|radiogroup|readonly|referrerpolicy|" +
         "rel|required|reversed|rows|rowspan|sandbox|scope|scoped|" +
         "selected|shape|size|sizes|slot|span|spellcheck|src|srcdoc|" +
         "srclang|srcset|start|step|style|summary|tabindex|target|" +
         "title|translate|type|usemap|value|width|wrap)\\b",
         Pattern.MULTILINE);

    public final static Pattern HTML_COMMENT =
        Pattern.compile("<!--.*?-->", Pattern.MULTILINE);

    public final static Pattern CSS_STYLES = Pattern.compile
        ("\\b(action|active|additive|adjust|after|align|all|alternates|" +
         "animation|annotation|area|areas|as|asian|attachment|attr|" +
         "auto|backdrop|backface|background|basis|before|behavior|" +
         "bezier|bidi|blend|block|blur|border|both|bottom|box|break|" +
         "brightness|calc|caps|caption|caret|cells|center|ch|change|" +
         "character|charset|checked|child|circle|clamp|clear|clip|" +
         "cm|collapse|color|column|columns|combine|composite|conic|" +
         "content|contrast|count|counter|counters|cross|cubic|cue|" +
         "cursor|decoration|default|deg|delay|dir|direction|" +
         "disabled|display|dpcm|dpi|dppx|drop|duration|east|element|" +
         "ellipse|em|emphasis|empty|enabled|end|env|events|ex|face|" +
         "fade|fallback|family|feature|fill|filter|first|fit|flex|" +
         "float|flow|focus|font|format|forms|fr|frames|fullscreen|" +
         "function|gap|grad|gradient|grayscale|grid|grow|hanging|" +
         "height|historical|hover|hsl|hsla|hue|hyphens|hz|image|import|" +
         "in|increment|indent|indeterminate|index|inherit|initial|" +
         "inline|inset|inside|invalid|invert|isolation|items|" +
         "iteration|justify|khz|kerning|keyframes|lang|language|" +
         "last|layout|leader|left|letter|ligatures|line|linear|link|" +
         "list|local|margin|mask|matrix|matrix3d|max|media|min|" +
         "minmax|mix|mm|mode|ms|name|namespace|negative|none|not|nth|" +
         "numeric|object|of|offset|only|opacity|optical|optional|" +
         "order|orientation|origin|ornaments|orphans|out|outline|" +
         "outset|outside|overflow|override|pad|padding|page|path|pc|" +
         "perspective|place|placeholder|play|pointer|polygon|" +
         "position|prefix|property|pt|punctuation|px|q|quotes|rad|" +
         "radial|radius|range|read|rect|relative|rem|rendering|repeat|" +
         "repeating|required|reset|resize|revert|rgb|rgba|right|" +
         "root|rotate|rotate3d|rotatex|rotatey|rotatez|row|rows|" +
         "rule|s|saturate|scale|scale3d|scalex|scaley|scalez|scope|" +
         "scroll|scrollbar|selection|self|sepia|set|settings|shadow|" +
         "shape|shrink|side|size|sizing|skew|skewx|skewy|slice|" +
         "slotted|snap|source|space|spacing|span|speak|src|start|" +
         "state|static|steps|stop|stretch|style|styleset|stylistic|suffix|" +
         "supports|swash|symbols|synthesis|system|tab|table|target|" +
         "template|text|threshold|timing|top|touch|transform|" +
         "transition|translate|translate3d|translatex|translatey|" +
         "translatez|turn|type|underline|unicode|unset|upright|url|" +
         "user|valid|values|var|variant|variation|vertical|vh|" +
         "viewport|visibility|visited|vmax|vmin|vw|weight|white|" +
         "widows|width|will|word|wrap|write|writing|x|y|z|zoom)\\b",
         Pattern.MULTILINE);

    public final static Pattern CSS_HEX = Pattern.compile
        ("#\\b[A-Fa-f0-9]+\\b", Pattern.MULTILINE);

    public final static Pattern MD_HEADER = Pattern.compile
        ("(^.+\\s+-+$)|(^.+\\s+=+$)|(^#+ +.+$)", Pattern.MULTILINE);

    public final static Pattern MD_LINK = Pattern.compile
        ("(\\!?\\[.+\\] *\\(.+\\))|(!?\\[.+\\] *\\[.+\\])|" +
         "( *\\[.+\\]: +.+$)", Pattern.MULTILINE);

    public final static Pattern MD_EMPH = Pattern.compile
        ("(([*~]{1,2})\\b(\\w| )+?\\b\\2)|(\\b(_{1,2})(\\w| )+?\\5\\b)",
         Pattern.MULTILINE);

    public final static Pattern MD_CODE = Pattern.compile
        ("(^ {4,}.+$)|(`.+?`)", Pattern.MULTILINE);

    public final static Pattern SH_COMMENT = Pattern.compile
        ("#.*$", Pattern.MULTILINE);

    public final static Pattern MODE_PATTERN = Pattern.compile
        ("^\\S+\\s+ed:(.+)$", Pattern.MULTILINE);
    public final static Pattern OPTION_PATTERN = Pattern.compile
        ("(\\s+(no)?(vw|ww|sg|hs|th|ts|tf)(:\\w)?)", Pattern.MULTILINE);
    public final static Pattern WORD_PATTERN = Pattern.compile
        ("\\w+", Pattern.MULTILINE);

    private final static double KEYBOARD_RATIO = 0.25;

    private final static int LAST_SIZE = 256;
    private final static int FIRST_SIZE = 256;
    private final static int POSITION_DELAY = 128;
    private final static int UPDATE_DELAY = 128;
    private final static int FIND_DELAY = 128;
    private final static int MAX_PATHS = 10;

    private final static int GET_TEXT = 0;

    private final static int REQUEST_READ = 1;
    private final static int REQUEST_SAVE = 2;
    private final static int REQUEST_OPEN = 3;

    private final static int OPEN_DOCUMENT   = 1;
    private final static int CREATE_DOCUMENT = 2;

    private final static int LIGHT = 1;
    private final static int DARK  = 2;
    private final static int BLACK = 3;
    private final static int RETRO = 4;

    private final static int SMALL  = 12;
    private final static int MEDIUM = 18;
    private final static int LARGE  = 24;

    private final static int NORMAL = 1;
    private final static int MONO   = 2;

    private final static int NO_SYNTAX   = 0;
    private final static int CC_SYNTAX   = 1;
    private final static int HTML_SYNTAX = 2;
    private final static int CSS_SYNTAX  = 3;
    private final static int MD_SYNTAX   = 4;
    private final static int SH_SYNTAX   = 5;
    private final static int DEF_SYNTAX  = 6;

    private Uri uri;
    private File file;
    private String path;
    private Uri content;
    private String append;
    private EditText textView;
    private TextView customView;
    private MenuItem searchItem;
    private SearchView searchView;
    private ScrollView scrollView;
    private Runnable updateHighlight;
    private Runnable updateWordCount;

    private Map<String, Integer> pathMap;
    private List<String> removeList;

    private boolean highlight = false;

    private boolean save = false;
    private boolean edit = false;
    private boolean view = false;

    private boolean wrap = false;
    private boolean suggest = true;

    private boolean changed = false;

    private long modified;

    private int theme = LIGHT;
    private int size = MEDIUM;
    private int type = MONO;

    private int syntax;

    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        save = preferences.getBoolean(PREF_SAVE, false);
        view = preferences.getBoolean(PREF_VIEW, true);
        wrap = preferences.getBoolean(PREF_WRAP, false);
        suggest = preferences.getBoolean(PREF_SUGGEST, true);
        highlight = preferences.getBoolean(PREF_HIGHLIGHT, false);

        theme = preferences.getInt(PREF_THEME, LIGHT);
        size = preferences.getInt(PREF_SIZE, MEDIUM);
        type = preferences.getInt(PREF_TYPE, MONO);

        Set<String> pathSet = preferences.getStringSet(PREF_PATHS, null);
        pathMap = new HashMap<>();

        if (pathSet != null)
            for (String path : pathSet)
                pathMap.put(path, preferences.getInt(path, 0));

        removeList = new ArrayList<>();

        switch (theme)
        {
        case LIGHT:
            setTheme(R.style.AppTheme);
            break;

        case DARK:
            setTheme(R.style.AppDarkTheme);
            break;

        case BLACK:
            setTheme(R.style.AppBlackTheme);
            break;

        case RETRO:
            setTheme(R.style.AppRetroTheme);
            break;
        }

        if (wrap)
            setContentView(R.layout.wrap);

        else
            setContentView(R.layout.edit);

        textView = findViewById(R.id.text);
        scrollView = findViewById(R.id.vscroll);

        getActionBar().setCustomView(R.layout.custom);
        getActionBar().setDisplayShowCustomEnabled(true);
        customView = (TextView) getActionBar().getCustomView();

        updateWordCount = () -> wordcountText();

        if (savedInstanceState != null)
            edit = savedInstanceState.getBoolean(EDIT);

        if (!edit)
            textView.setRawInputType(InputType.TYPE_NULL);

        else if (!suggest)
            textView.setInputType(InputType.TYPE_CLASS_TEXT |
                                  InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                  InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        setSizeAndTypeface(size, type);

        final TypedArray typedArray =
            obtainStyledAttributes(R.styleable.Editor);

        if (typedArray.hasValue(R.styleable.Editor_BackgroundColour))
            textView
            .setBackgroundColor(typedArray
                                .getColor(R.styleable
                                          .Editor_BackgroundColour, 0));
        typedArray.recycle();

        Intent intent = getIntent();
        Uri uri = intent.getData();

        switch (intent.getAction())
        {
        case Intent.ACTION_EDIT:
        case Intent.ACTION_VIEW:
            if ((savedInstanceState == null) && (uri != null))
                readFile(uri);

            getActionBar().setDisplayHomeAsUpEnabled(true);
            break;

        case Intent.ACTION_SEND:
            if (savedInstanceState == null)
            {
                // Get text
                String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (text != null)
                {
                    defaultFile(text);
                    changed = true;
                }

                // Get uri
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri != null)
                    readFile(uri);
            }
            break;

        case Intent.ACTION_MAIN:
            if (savedInstanceState == null)
                defaultFile(null);
            break;
        }

        setListeners();
    }

    // setListeners
    private void setListeners()
    {
        if (textView != null)
        {
            textView.addTextChangedListener(new TextWatcher()
            {
                // afterTextChanged
                @Override
                public void afterTextChanged(Editable s)
                {
                    if (!changed)
                    {
                        changed = true;
                        invalidateOptionsMenu();
                    }

                    if (updateHighlight != null)
                    {
                        textView.removeCallbacks(updateHighlight);
                        textView.postDelayed(updateHighlight, UPDATE_DELAY);
                    }

                    if (updateWordCount != null)
                    {
                        textView.removeCallbacks(updateWordCount);
                        textView.postDelayed(updateWordCount, UPDATE_DELAY);
                    }
                }

                // beforeTextChanged
                @Override
                public void beforeTextChanged(CharSequence s,
                                              int start,
                                              int count,
                                              int after)
                {
                    if (searchItem != null &&
                        searchItem.isActionViewExpanded())
                    {
                        final CharSequence query = searchView.getQuery();

                        textView.postDelayed(() ->
                        {
                            if (searchItem != null &&
                                searchItem.isActionViewExpanded())
                            {
                                if (query != null)
                                    searchView.setQuery(query, false);
                            }
                        }, UPDATE_DELAY);
                    }
                }

                // onTextChanged
                @Override
                public void onTextChanged(CharSequence s,
                                          int start,
                                          int before,
                                          int count) {}
            });

            // onFocusChange
            textView.setOnFocusChangeListener((v, hasFocus) ->
            {
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
                if (!hasFocus)
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                if (updateHighlight != null)
                {
                    textView.removeCallbacks(updateHighlight);
                    textView.postDelayed(updateHighlight, UPDATE_DELAY);
                }
            });

            // onLongClick
            textView.setOnLongClickListener(v ->
            {
                // Do nothing if already editable
                if (edit)
                    return false;

                // Get scroll position
                int y = scrollView.getScrollY();
                // Get height
                int height = scrollView.getHeight();
                // Get width
                int width = scrollView.getWidth();

                // Get offset
                int line = textView.getLayout()
                    .getLineForVertical(y + height / 2);
                int offset = textView.getLayout()
                    .getOffsetForHorizontal(line, width / 2);
                // Set cursor
                textView.setSelection(offset);

                // Set editable with or without suggestions
                if (suggest)
                    textView
                    .setInputType(InputType.TYPE_CLASS_TEXT |
                                  InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                else
                    textView
                    .setInputType(InputType.TYPE_CLASS_TEXT |
                                  InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                  InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                // Change typeface temporarily as workaround for yet
                // another obscure feature of some versions of android
                textView.setTypeface((type == NORMAL)?
                                     Typeface.MONOSPACE:
                                     Typeface.DEFAULT, Typeface.NORMAL);
                textView.setTypeface((type == NORMAL)?
                                     Typeface.DEFAULT:
                                     Typeface.MONOSPACE, Typeface.NORMAL);
                // Update boolean
                edit = true;

                // Update menu
                invalidateOptionsMenu();

                return false;
            });

            textView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener()
            {
                private boolean keyboard;

                // onGlobalLayout
                @Override
                public void onGlobalLayout()
                {
                    if (updateHighlight != null)
                    {
                        int rootHeight = scrollView.getRootView().getHeight();
                        int height = scrollView.getHeight();

                        boolean shown = (((rootHeight - height) /
                                         (double) rootHeight) >
                                         KEYBOARD_RATIO);

                        if (shown != keyboard)
                        {
                            if (!shown)
                            {
                                textView.removeCallbacks(updateHighlight);
                                textView.postDelayed(updateHighlight,
                                                     UPDATE_DELAY);
                            }

                            keyboard = shown;
                        }
                    }
                }
            });
        }

        if (scrollView != null)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                // onScrollChange
                scrollView.setOnScrollChangeListener((v, x, y, oldX, oldY) ->
                {
                    if (updateHighlight != null)
                    {
                        textView.removeCallbacks(updateHighlight);
                        textView.postDelayed(updateHighlight, UPDATE_DELAY);
                    }
                });

            else
                // onScrollChange
                scrollView.getViewTreeObserver()
                    .addOnScrollChangedListener(() ->
                {
                    if (updateHighlight != null)
                    {
                        textView.removeCallbacks(updateHighlight);
                        textView.postDelayed(updateHighlight, UPDATE_DELAY);
                    }
                });
        }
    }

    // onRestoreInstanceState
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        path = savedInstanceState.getString(PATH);
        edit = savedInstanceState.getBoolean(EDIT);
        changed = savedInstanceState.getBoolean(CHANGED);
        modified = savedInstanceState.getLong(MODIFIED);
        content = savedInstanceState.getParcelable(CONTENT);
        invalidateOptionsMenu();

        file = new File(path);
        uri = Uri.fromFile(file);

        if (content != null)
            setTitle(FileUtils.getDisplayName(this, content, null, null));

        else
            setTitle(uri.getLastPathSegment());

        checkHighlight();

        if (file.lastModified() > modified)
            alertDialog(R.string.appName, R.string.changedReload,
                        R.string.reload, R.string.cancel, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                readFile(uri);
            }
        });
    }

    // onPause
    @Override
    public void onPause()
    {
        super.onPause();

        // Save current path
        savePath(path);

        // Stop highlighting
        textView.removeCallbacks(updateHighlight);
        textView.removeCallbacks(updateWordCount);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putBoolean(PREF_SAVE, save);
        editor.putBoolean(PREF_VIEW, view);
        editor.putBoolean(PREF_WRAP, wrap);
        editor.putBoolean(PREF_SUGGEST, suggest);
        editor.putBoolean(PREF_HIGHLIGHT, highlight);
        editor.putInt(PREF_THEME, theme);
        editor.putInt(PREF_SIZE, size);
        editor.putInt(PREF_TYPE, type);

        // Add the set of recent files
        editor.putStringSet(PREF_PATHS, pathMap.keySet());

        // Add a position for each file
        for (String path : pathMap.keySet())
            editor.putInt(path, pathMap.get(path));

        // Remove the old ones
        for (String path : removeList)
            editor.remove(path);

        editor.apply();

        // Save file
        if (changed && save)
            saveFile();
    }

    // onSaveInstanceState
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putParcelable(CONTENT, content);
        outState.putLong(MODIFIED, modified);
        outState.putBoolean(CHANGED, changed);
        outState.putBoolean(EDIT, edit);
        outState.putString(PATH, path);
    }

    // onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        return true;
    }

    // onPrepareOptionsMenu
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        // Set up search view
        searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();

        // Set up search view options and listener
        if (searchView != null)
        {
            searchView.setSubmitButtonEnabled(true);
            searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
            searchView.setOnQueryTextListener(new QueryTextListener());
        }

        // Show find all item
        if (menu.findItem(R.id.search).isActionViewExpanded())
            menu.findItem(R.id.findAll).setVisible(true);
        else
            menu.findItem(R.id.findAll).setVisible(false);

        menu.findItem(R.id.edit).setVisible(!edit);
        menu.findItem(R.id.view).setVisible(edit);

        menu.findItem(R.id.save).setVisible(changed);

        menu.findItem(R.id.viewFile).setChecked(view);
        menu.findItem(R.id.autoSave).setChecked(save);
        menu.findItem(R.id.wrap).setChecked(wrap);
        menu.findItem(R.id.suggest).setChecked(suggest);
        menu.findItem(R.id.highlight).setChecked(highlight);

        switch (theme)
        {
        case LIGHT:
            menu.findItem(R.id.light).setChecked(true);
            break;

        case DARK:
            menu.findItem(R.id.dark).setChecked(true);
            break;

        case BLACK:
            menu.findItem(R.id.black).setChecked(true);
            break;

        case RETRO:
            menu.findItem(R.id.retro).setChecked(true);
            break;
        }

        switch (size)
        {
        case SMALL:
            menu.findItem(R.id.small).setChecked(true);
            break;

        case MEDIUM:
            menu.findItem(R.id.medium).setChecked(true);
            break;

        case LARGE:
            menu.findItem(R.id.large).setChecked(true);
            break;
        }

        switch (type)
        {
        case MONO:
            menu.findItem(R.id.mono).setChecked(true);
            break;

        case NORMAL:
            menu.findItem(R.id.normal).setChecked(true);
            break;
        }

        // Get a list of recent files
        List<Long> list = new ArrayList<>();
        Map<Long, String> map = new HashMap<>();

        // Get the last modified dates
        for (String path : pathMap.keySet())
        {
            File file = new File(path);
            long last = file.lastModified();
            list.add(last);
            map.put(last, path);
        }

        // Sort in reverse order
        Collections.sort(list);
        Collections.reverse(list);

        // Get the submenu
        MenuItem item = menu.findItem(R.id.openRecent);
        SubMenu sub = item.getSubMenu();
        sub.clear();

        // Add the recent files
        for (long date : list)
        {
            String path = map.get(date);

            // Remove path prefix
            String name =
                path.replaceFirst(Environment
                                  .getExternalStorageDirectory()
                                  .getPath() + File.separator, "");
            sub.add(name);
        }

        // Add clear list item
        sub.add(Menu.NONE, R.id.clearList, Menu.NONE, R.string.clearList);

        return true;
    }

    // onOptionsItemSelected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case android.R.id.home:
            onBackPressed();
            break;
        case R.id.neu:
            freshFile();
            break;
        case R.id.edit:
            editClicked(item);
            break;
        case R.id.view:
            viewClicked(item);
            break;
        case R.id.open:
            openFile();
            break;
        case R.id.save:
            saveCheck();
            break;
        case R.id.saveAs:
            saveAs();
            break;
        case R.id.clearList:
            clearList();
            break;
        case R.id.findAll:
            findAll();
            break;
        case R.id.viewMarkdown:
            viewMarkdown();
            break;
        case R.id.viewFile:
            viewFileClicked(item);
            break;
        case R.id.autoSave:
            autoSaveClicked(item);
            break;
        case R.id.wrap:
            wrapClicked(item);
            break;
        case R.id.suggest:
            suggestClicked(item);
            break;
        case R.id.highlight:
            highlightClicked(item);
            break;
        case R.id.light:
            lightClicked(item);
            break;
        case R.id.dark:
            darkClicked(item);
            break;
        case R.id.black:
            blackClicked(item);
            break;
        case R.id.retro:
            retroClicked(item);
            break;
        case R.id.small:
            smallClicked(item);
            break;
        case R.id.medium:
            mediumClicked(item);
            break;
        case R.id.large:
            largeClicked(item);
            break;
        case R.id.mono:
            monoClicked(item);
            break;
        case R.id.normal:
            normalClicked(item);
            break;
        case R.id.about:
            aboutClicked();
            break;
        default:
            openRecent(item);
            break;
        }

        // Close text search
        if (searchItem != null && searchItem.isActionViewExpanded() &&
                item.getItemId() != R.id.findAll)
            searchItem.collapseActionView();

        return true;
    }

    // onBackPressed
    @Override
    public void onBackPressed()
    {
        if (changed)
            alertDialog(R.string.appName, R.string.modified,
                        R.string.save, R.string.discard, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                saveFile();
                finish();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                changed = false;
                finish();
                break;
            }
        });

        else
            finish();
    }

    // onActivityResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data)
    {
        if (resultCode == RESULT_CANCELED)
            return;

        switch (requestCode)
        {
        case OPEN_DOCUMENT:
            content = data.getData();
            readFile(content);
            break;

        case CREATE_DOCUMENT:
            content = data.getData();
            setTitle(FileUtils.getDisplayName(this, content, null, null));
            saveFile();
            break;
        }
    }

    // editClicked
    private void editClicked(MenuItem item)
    {
        // Get scroll position
        int y = scrollView.getScrollY();
        // Get height
        int height = scrollView.getHeight();
        // Get width
        int width = scrollView.getWidth();

        // Get offset
        int line = textView.getLayout()
            .getLineForVertical(y + height / 2);
        int offset = textView.getLayout()
            .getOffsetForHorizontal(line, width / 2);
        // Set cursor
        textView.setSelection(offset);

        // Set editable with or without suggestions
        if (suggest)
            textView.setInputType(InputType.TYPE_CLASS_TEXT |
                                  InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        else
            textView.setInputType(InputType.TYPE_CLASS_TEXT |
                                  InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                  InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // Change typeface temporarily as workaround for yet another
        // obscure feature of some versions of android
        textView.setTypeface((type == NORMAL)?
                             Typeface.MONOSPACE:
                             Typeface.DEFAULT, Typeface.NORMAL);
        textView.setTypeface((type == NORMAL)?
                             Typeface.DEFAULT:
                             Typeface.MONOSPACE, Typeface.NORMAL);
        // Update boolean
        edit = true;

        // Update menu
        invalidateOptionsMenu();
    }

    // viewClicked
    private void viewClicked(MenuItem item)
    {
        // Set read only
        textView.setRawInputType(InputType.TYPE_NULL);
        textView.clearFocus();

        // Update boolean
        edit = false;

        // Update menu
        invalidateOptionsMenu();
    }

    // freshFile
    private void freshFile()
    {
        // Check if file changed
        if (changed)
            alertDialog(R.string.neu, R.string.modified,
                        R.string.save, R.string.discard, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                saveFile();
                newFile();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                newFile();
                break;
            }

            invalidateOptionsMenu();
        });

        else
            newFile();

        invalidateOptionsMenu();
    }

    // newFile
    private void newFile()
    {
        textView.setText("");
        changed = false;

        file = getNewFile();
        uri = Uri.fromFile(file);
        path = uri.getPath();

        setTitle(uri.getLastPathSegment());
    }

    // getNewFile
    private File getNewFile()
    {
        File documents = new
            File(Environment.getExternalStorageDirectory(), DOCUMENTS);
        return new File(documents, NEW_FILE);
    }

    // getDefaultFile
    private File getDefaultFile()
    {
        File documents = new
            File(Environment.getExternalStorageDirectory(), DOCUMENTS);
        return new File(documents, EDIT_FILE);
    }

    // defaultFile
    private void defaultFile(String text)
    {
        file = getDefaultFile();

        uri = Uri.fromFile(file);
        path = uri.getPath();

        if (file.exists())
        {
            readFile(uri);
            append = text;
        }

        else
        {
            if (text != null)
                textView.append(text);

            setTitle(uri.getLastPathSegment());
        }
    }

    // alertDialog
    private void alertDialog(int title, int message,
                             int positiveButton, int negativeButton,
                             DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add the buttons
        builder.setPositiveButton(positiveButton, listener);
        builder.setNegativeButton(negativeButton, listener);

        // Create the AlertDialog
        builder.show();
    }

    // alertDialog
    private void alertDialog(int title, String message, int neutralButton)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add the buttons
        builder.setNeutralButton(neutralButton, null);

        // Create the AlertDialog
        builder.show();
    }

    // savePath
    private void savePath(String path)
    {
        // Save the current position
        pathMap.put(path, scrollView.getScrollY());

        // Get a list of files
        List<Long> list = new ArrayList<>();
        Map<Long, String> map = new HashMap<>();
        for (String name : pathMap.keySet())
        {
            File file = new File(name);
            list.add(file.lastModified());
            map.put(file.lastModified(), name);
        }

        // Sort in reverse order
        Collections.sort(list);
        Collections.reverse(list);

        int count = 0;
        for (long date : list)
        {
            String name = map.get(date);

            // Remove old files
            if (count >= MAX_PATHS)
            {
                pathMap.remove(name);
                removeList.add(name);
            }

            count++;
        }
    }

    // openRecent
    private void openRecent(MenuItem item)
    {
        String name = item.getTitle().toString();
        File file = new File(name);

        // Check absolute file
        if (!file.isAbsolute())
            file = new File(Environment.getExternalStorageDirectory(),
                            File.separator + name);
        // Check it exists
        if (file.exists())
        {
            Uri uri = Uri.fromFile(file);

            if (changed)
                alertDialog(R.string.openRecent, R.string.modified,
                            R.string.save, R.string.discard, (dialog, id) ->
            {
                switch (id)
                {
                case DialogInterface.BUTTON_POSITIVE:
                    saveFile();
                    readFile(uri);
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    changed = false;
                    readFile(uri);
                    break;
                }
            });
            else
                readFile(uri);
        }
    }

    // saveAs
    private void saveAs()
    {
        // Remove path prefix
        String name =
            path.replaceFirst(Environment
                              .getExternalStorageDirectory()
                              .getPath() + File.separator, "");

        // Open dialog
        saveAsDialog(name, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                EditText text = ((Dialog) dialog).findViewById(R.id.path_text);
                String string = text.getText().toString();

                // Ignore empty string
                if (string.isEmpty())
                    return;

                file = new File(string);

                // Check absolute file
                if (!file.isAbsolute())
                    file = new
                        File(Environment.getExternalStorageDirectory(), string);

                // Set interface title
                uri = Uri.fromFile(file);
                String title = uri.getLastPathSegment();
                setTitle(title);

                path = file.getPath();
                saveFile();
                break;

            case DialogInterface.BUTTON_NEUTRAL:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.setType(TEXT_WILD);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, CREATE_DOCUMENT);
                }
                break;
            }
        });
    }

    // saveAsDialog
    private void saveAsDialog(String path,
                              DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.save);
        builder.setMessage(R.string.choose);

        // Add the buttons
        builder.setPositiveButton(R.string.save, listener);
        builder.setNegativeButton(R.string.cancel, listener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            builder.setNeutralButton(R.string.storage, listener);

        // Create edit text
        Context context = builder.getContext();
        EditText text = new EditText(context);
        text.setId(R.id.path_text);
        text.setText(path);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setView(text, 40, 0, 40, 0);
        dialog.show();
    }

    // clearList
    private void clearList()
    {
        for (String path : pathMap.keySet())
            removeList.add(path);

        pathMap.clear();
    }

    // findAll
    public void findAll()
    {
        // Get search string
        String search = searchView.getQuery().toString();

        FindTask findTask = new FindTask(this);
        findTask.execute(search);
    }

    // viewMarkdown
    private void viewMarkdown()
    {
        String text = textView.getText().toString();

        // Use commonmark
        Parser parser = Parser.builder().build();
        Node document = parser.parse(text);
        HtmlRenderer renderer = HtmlRenderer.builder().build();

        String html = renderer.render(document);

        File file = new File(getCacheDir(), HTML_FILE);
        file.deleteOnExit();

        try (FileWriter writer = new FileWriter(file))
        {
            // Add HTML header and footer to make a valid page.
            writer.write(HTML_HEAD);
            writer.write(html);
            writer.write(HTML_TAIL);
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            // Get file provider uri
            Uri uri = FileProvider.getUriForFile
                (this, FILE_PROVIDER, file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, TEXT_HTML);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // viewFileClicked
    private void viewFileClicked(MenuItem item)
    {
        view = !view;
        item.setChecked(view);
    }

    // autoSaveClicked
    private void autoSaveClicked(MenuItem item)
    {
        save = !save;
        item.setChecked(save);
    }

    // wrapClicked
    private void wrapClicked(MenuItem item)
    {
        wrap = !wrap;
        item.setChecked(wrap);
	recreate(this);
    }

    // suggestClicked
    private void suggestClicked(MenuItem item)
    {
        suggest = !suggest;
        item.setChecked(suggest);

        if (suggest)
            textView.setRawInputType(InputType.TYPE_CLASS_TEXT |
                                     InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        else
            textView.setRawInputType(InputType.TYPE_CLASS_TEXT |
                                     InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                     InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
	recreate(this);
    }

    // highlightClicked
    private void highlightClicked(MenuItem item)
    {
        highlight = !highlight;
        item.setChecked(highlight);

        checkHighlight();
    }

    // lightClicked
    private void lightClicked(MenuItem item)
    {
        theme = LIGHT;
        item.setChecked(true);
	recreate(this);
    }

    // darkClicked
    private void darkClicked(MenuItem item)
    {
        theme = DARK;
        item.setChecked(true);
	recreate(this);
    }

    // blackClicked
    private void blackClicked(MenuItem item)
    {
        theme = BLACK;
        item.setChecked(true);
	recreate(this);
    }

    // retroClicked
    private void retroClicked(MenuItem item)
    {
        theme = RETRO;
        item.setChecked(true);
	recreate(this);
    }

    // smallClicked
    private void smallClicked(MenuItem item)
    {
        size = SMALL;
        item.setChecked(true);

        textView.setTextSize(size);
    }

    // mediumClicked
    private void mediumClicked(MenuItem item)
    {
        size = MEDIUM;
        item.setChecked(true);

        textView.setTextSize(size);
    }

    // largeClicked
    private void largeClicked(MenuItem item)
    {
        size = LARGE;
        item.setChecked(true);

        textView.setTextSize(size);
    }

    // monoClicked
    private void monoClicked(MenuItem item)
    {
        type = MONO;
        item.setChecked(true);

        textView.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
    }

    // normalClicked
    private void normalClicked(MenuItem item)
    {
        type = NORMAL;
        item.setChecked(true);

        textView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
    }

    // setSizeAndTypeface
    private void setSizeAndTypeface(int size, int type)
    {
        // Update size
        switch (size)
        {
        case SMALL:
        case MEDIUM:
        case LARGE:
            break;

        default:
            size = MEDIUM;
            invalidateOptionsMenu();
            break;
        }

        // Set size
        textView.setTextSize(size);

        // Set type
        switch (type)
        {
        case MONO:
            textView.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            break;

        case NORMAL:
            textView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            break;
        }
    }

    // aboutClicked
    @SuppressWarnings("deprecation")
    private void aboutClicked()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.appName);

        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        SpannableStringBuilder spannable =
            new SpannableStringBuilder(getText(R.string.version));
        Pattern pattern = Pattern.compile("%s");
        Matcher matcher = pattern.matcher(spannable);
        if (matcher.find())
            spannable.replace(matcher.start(), matcher.end(),
                              BuildConfig.VERSION_NAME);
        matcher.reset(spannable);
        if (matcher.find())
            spannable.replace(matcher.start(), matcher.end(),
                              dateFormat.format(BuildConfig.BUILT));
        builder.setMessage(spannable);

        // Add the button
        builder.setPositiveButton(R.string.ok, null);

        // Create the AlertDialog
        Dialog dialog = builder.show();

        // Set movement method
        TextView text = dialog.findViewById(android.R.id.message);
        if (text != null)
        {
            text.setTextAppearance(builder.getContext(),
                                   android.R.style.TextAppearance_Small);
            text.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    // recreate
    private void recreate(Context context)
    {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.M)
            recreate();
    }

    // openFile
    private void openFile()
    {
        // Check if file changed
        if (changed)
            alertDialog(R.string.open, R.string.modified,
                        R.string.save, R.string.discard, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                saveFile();
                getFile();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                changed = false;
                getFile();
                break;
            }
        });

        else
            getFile();

    }

    // getFile
    private void getFile()
    {
        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_OPEN);
                return;
            }
        }

        // Open parent folder
        File dir = file.getParentFile();
        getFile(dir);
    }

    // getFile
    private void getFile(File dir)
    {
        // Get list of files
        List<File> list = getList(dir);
        if (list == null)
            return;

        // Pop up dialog
        String title = FOLDER + dir.getPath();
        openDialog(title, list, (dialog, which) ->
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                DialogInterface.BUTTON_NEUTRAL == which)
            {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType(TEXT_WILD);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, OPEN_DOCUMENT);
                return;
            }

            File selection = list.get(which);
            if (selection.isDirectory())
                getFile(selection);

            else
                readFile(Uri.fromFile(selection));
        });
    }

    // getList
    private List<File> getList(File dir)
    {
        List<File> list = null;
        File[] files = dir.listFiles();
        // Check files
        if (files == null)
        {
            // Create a list with just the parent folder and the
            // external storage folder
            list = new ArrayList<File>();
            if (dir.getParentFile() == null)
                list.add(dir);

            else
                list.add(dir.getParentFile());

            list.add(Environment.getExternalStorageDirectory());

            return list;
        }

        // Sort the files
        Arrays.sort(files);
        // Create a list
        list = new ArrayList<File>(Arrays.asList(files));
        // Remove hidden files
        Iterator<File> iterator = list.iterator();
        while (iterator.hasNext())
        {
            File item = iterator.next();
            if (item.getName().startsWith("."))
                iterator.remove();
        }

        // Add parent folder
        if (dir.getParentFile() == null)
            list.add(0, dir);

        else
            list.add(0, dir.getParentFile());

        return list;
    }

    // openDialog
    private void openDialog(String title, List<File> list,
                            DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        // Add the adapter
        FileAdapter adapter = new FileAdapter(builder.getContext(), list);
        builder.setAdapter(adapter, listener);

        // Add storage button
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            builder.setNeutralButton(R.string.storage, listener);
        // Add cancel button
        builder.setNegativeButton(R.string.cancel, null);

        // Create the Dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // onRequestPermissionsResult
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults)
    {
        switch (requestCode)
        {
        case REQUEST_SAVE:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .WRITE_EXTERNAL_STORAGE) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Granted, save file
                    saveFile();
            break;

        case REQUEST_READ:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .READ_EXTERNAL_STORAGE) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Granted, read file
                    readFile(uri);
            break;

        case REQUEST_OPEN:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .READ_EXTERNAL_STORAGE) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    // Granted, open file
                    getFile();
            break;
        }
    }

    // readFile
    private void readFile(Uri uri)
    {
        if (uri == null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ);
                this.uri = uri;
                return;
            }
        }

        // Attempt to resolve content uri
        if (CONTENT.equalsIgnoreCase(uri.getScheme()))
        {
            content = uri;
            uri = resolveContent(uri);
        }

        else
            content = null;

        // Read into default file if unresolved
        if (CONTENT.equalsIgnoreCase(uri.getScheme()))
        {
            file = getDefaultFile();
            Uri defaultUri = Uri.fromFile(file);
            path = defaultUri.getPath();

            setTitle(FileUtils.getDisplayName(this, content, null, null));
        }

        // Read file
        else
        {
            this.uri = uri;
            path = uri.getPath();
            file = new File(path);

            setTitle(uri.getLastPathSegment());
        }

        textView.setText(R.string.loading);

        ReadTask read = new ReadTask(this);
        read.execute(uri);

        changed = false;
        modified = file.lastModified();
        savePath(path);
        invalidateOptionsMenu();
    }

    // resolveContent
    private Uri resolveContent(Uri uri)
    {
        String path = FileUtils.getPath(this, uri);

        if (path != null)
        {
            File file = new File(path);
            if (file.canRead())
                uri = Uri.fromFile(file);
        }

        return uri;
    }

    // saveCheck
    private void saveCheck()
    {
        Uri uri = Uri.fromFile(file);
        Uri newUri = Uri.fromFile(getNewFile());
        if (newUri.getPath().equals(uri.getPath()))
            saveAs();

        else
            saveFile();
    }

    // saveFile
    private void saveFile()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                     Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_SAVE);
                return;
            }
        }

        if (file.lastModified() > modified)
            alertDialog(R.string.appName, R.string.changedOverwrite,
                        R.string.overwrite, R.string.cancel, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                saveFile(file);
                break;
            }
        });

        else
        {
            if (content == null)
                saveFile(file);

            else
            {
                saveFile(content);
                content = null;
            }
        }
    }

    // saveFile
    private void saveFile(File file)
    {
        CharSequence text = textView.getText();
        write(text, file);
    }

    // saveFile
    private void saveFile(Uri uri)
    {
        CharSequence text = textView.getText();
        try (OutputStream outputStream =
             getContentResolver().openOutputStream(uri))
        {
            write(text, outputStream);
        }

        catch (Exception e)
        {
            alertDialog(R.string.appName, e.getMessage(), R.string.ok);
            e.printStackTrace();
            return;
        }
    }

    // write
    private void write(CharSequence text, File file)
    {
        file.getParentFile().mkdirs();
        try (FileWriter fileWriter = new FileWriter(file))
        {
            fileWriter.append(text);
        }

        catch (Exception e)
        {
            alertDialog(R.string.appName, e.getMessage(), R.string.ok);
            e.printStackTrace();
            return;
        }

        changed = false;
        invalidateOptionsMenu();
        modified = file.lastModified();
        savePath(file.getPath());
    }

    // write
    private void write(CharSequence text, OutputStream os)
    {
        try (OutputStreamWriter writer = new OutputStreamWriter(os))
        {
            writer.append(text);
        }

        catch (Exception e)
        {
            alertDialog(R.string.appName, e.getMessage(), R.string.ok);
            e.printStackTrace();
            return;
        }

        changed = false;
        invalidateOptionsMenu();
    }

    // checkHighlight
    private void checkHighlight()
    {
        // No syntax
        syntax = NO_SYNTAX;

        // Check extension
        if (highlight && file != null)
        {
            String ext = FileUtils.getExtension(file.getName());
            if (ext != null)
            {
                String type = FileUtils.getMimeType(file);

                if (ext.matches(CC_EXT))
                    syntax = CC_SYNTAX;

                else if (ext.matches(HTML_EXT))
                    syntax = HTML_SYNTAX;

                else if (ext.matches(CSS_EXT))
                    syntax = CSS_SYNTAX;

                else if (ext.matches(MD_EXT))
                    syntax = MD_SYNTAX;

                else if (ext.matches(SH_EXT))
                    syntax = SH_SYNTAX;

                else if (!TEXT_PLAIN.equals(type))
                    syntax = DEF_SYNTAX;

                else
                    syntax = NO_SYNTAX;

                // Add callback
                if (textView != null && syntax != NO_SYNTAX)
                {
                    if (updateHighlight == null)
                        updateHighlight = () -> highlightText();

                    textView.removeCallbacks(updateHighlight);
                    textView.postDelayed(updateHighlight, UPDATE_DELAY);

                    return;
                }
            }
        }

        // Remove highlighting
        if (updateHighlight != null)
        {
            textView.removeCallbacks(updateHighlight);
            textView.postDelayed(updateHighlight, UPDATE_DELAY);

            updateHighlight = null;
        }
    }

    // highlightText
    private void highlightText()
    {
        // Get visible extent
        int top = scrollView.getScrollY();
        int height = scrollView.getHeight();

        int line = textView.getLayout().getLineForVertical(top);
        int start = textView.getLayout().getLineStart(line);
        int first = textView.getLayout().getLineStart(line + 1);

        line = textView.getLayout().getLineForVertical(top + height);
        int end = textView.getLayout().getLineEnd(line);
        int last = (line == 0)? end:
            textView.getLayout().getLineStart(line - 1);

        // Move selection if outside range
        if (textView.getSelectionStart() < start)
            textView.setSelection(first);

        if (textView.getSelectionStart() > end)
            textView.setSelection(last);

        // Get editable
        Editable editable = textView.getEditableText();

        // Get current spans
        ForegroundColorSpan spans[] =
            editable.getSpans(start, end, ForegroundColorSpan.class);
        // Remove spans
        for (ForegroundColorSpan span: spans)
            editable.removeSpan(span);

        Matcher matcher;

        switch (syntax)
        {
        case NO_SYNTAX:
            // Get current spans
            spans = editable.getSpans(0, editable.length(),
                                      ForegroundColorSpan.class);
            // Remove spans
            for (ForegroundColorSpan span: spans)
                editable.removeSpan(span);
            break;

        case CC_SYNTAX:
            matcher = KEYWORDS.matcher(editable);
            matcher.region(start, end);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(TYPES);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.MAGENTA);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(CLASS);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.BLUE);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(NUMBER);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.YELLOW);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(ANNOTATION);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(CONSTANT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.LTGRAY);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(CC_COMMENT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.RED);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            break;

        case HTML_SYNTAX:
            matcher = HTML_TAGS.matcher(editable);
            matcher.region(start, end);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(HTML_ATTRS);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.MAGENTA);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(QUOTED);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.RED);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(HTML_COMMENT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.RED);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            break;

        case CSS_SYNTAX:
            matcher = CSS_STYLES.matcher(editable);
            matcher.region(start, end);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(CSS_HEX);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.MAGENTA);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(CC_COMMENT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.RED);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            break;

        case MD_SYNTAX:
            matcher = MD_HEADER.matcher(editable);
            matcher.region(start, end);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.BLUE);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(MD_LINK);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(MD_EMPH);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.MAGENTA);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(MD_CODE);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            break;

        case SH_SYNTAX:
            matcher = KEYWORDS.matcher(editable);
            matcher.region(start, end);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(NUMBER);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.YELLOW);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(CONSTANT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.LTGRAY);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(SH_COMMENT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.RED);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            break;

        case DEF_SYNTAX:
            matcher = KEYWORDS.matcher(editable);
            matcher.region(start, end);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.CYAN);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(TYPES);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.MAGENTA);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(CLASS);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.BLUE);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(NUMBER);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.YELLOW);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(CONSTANT);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.LTGRAY);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            matcher.region(start, end).usePattern(QUOTED);
            while (matcher.find())
            {
                ForegroundColorSpan span = new
                    ForegroundColorSpan(Color.RED);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            break;
        }
    }

    // wordcountText
    private void wordcountText()
    {
        int words = 0;
        Matcher matcher = WORD_PATTERN.matcher(textView.getText());
        while (matcher.find())
        {
            words++;
        }

        if (customView != null)
        {
            String string = String.format(Locale.getDefault(), "%d\n%d",
                                          words, textView.length());
            customView.setText(string);
        }
    }

    // onActionModeStarted
    @Override
    public void onActionModeStarted(ActionMode mode)
    {
        super.onActionModeStarted(mode);

        // If there's a file
        if (file != null)
        {
            // Get the mime type
            String type = FileUtils.getMimeType(file);
            // If the type is not text/plain
            if (!TEXT_PLAIN.equals(type))
            {
                // Get the start and end of the selection
                int start = textView.getSelectionStart();
                int end = textView.getSelectionEnd();
                // And the text
                CharSequence text = textView.getText();

                // Get a pattern and a matcher for delimiter
                // characters
                Matcher matcher = PATTERN_CHARS.matcher(text);

                // Find the first match after the end of the selection
                if (matcher.find(end))
                {
                    // Update the selection end
                    end = matcher.start();

                    // Get the matched char
                    char c = text.charAt(end);

                    // Check for opening brackets
                    if (BRACKET_CHARS.indexOf(c) == -1)
                    {
                        switch (c)
                        {
                            // Check for close brackets and look for
                            // the open brackets
                        case ')':
                            c = '(';
                            break;

                        case ']':
                            c = '[';
                            break;

                        case '}':
                            c = '{';
                            break;

                        case '>':
                            c = '<';
                            break;
                        }

                        String string = text.toString();
                        // Do reverse search
                        start = string.lastIndexOf(c, start) + 1;

                        // Check for included newline
                        if (start > string.lastIndexOf('\n', end))
                            // Update selection
                            textView.setSelection(start, end);
                    }
                }
            }
        }
    }

    // checkMode
    private void checkMode(CharSequence text)
    {
        boolean change = false;

        CharSequence first = text.subSequence
            (0, Math.min(text.length(), FIRST_SIZE));
        CharSequence last = text.subSequence
            (Math.max(0, text.length() - LAST_SIZE), text.length());
        for (CharSequence sequence: new CharSequence[]{first, last})
        {
            Matcher matcher = MODE_PATTERN.matcher(sequence);
            if (matcher.find())
            {
                matcher.region(matcher.start(1), matcher.end(1));
                matcher.usePattern(OPTION_PATTERN);
                while (matcher.find())
                {
                    boolean no = "no".equals(matcher.group(2));

                    if ("vw".equals(matcher.group(3)))
                    {
                        if (view == no)
                        {
                            view = !no;
                            change = true;
                        }
                    }

                    else if ("ww".equals(matcher.group(3)))
                    {
                        if (wrap == no)
                        {
                            wrap = !no;
                            change = true;
                        }
                    }

                    else if ("sg".equals(matcher.group(3)))
                    {
                        if (suggest == no)
                        {
                            suggest = !no;
                            change = true;
                        }
                    }

                    else if ("hs".equals(matcher.group(3)))
                    {
                        if (highlight == no)
                        {
                            highlight = !no;
                            checkHighlight();
                        }
                    }

                    else if ("th".equals(matcher.group(3)))
                    {
                        if (":l".equals(matcher.group(4)))
                        {
                            if (theme != LIGHT)
                            {
                                theme = LIGHT;
                                change = true;
                            }
                        }

                        else if (":d".equals(matcher.group(4)))
                        {
                            if (theme != DARK)
                            {
                                theme = DARK;
                                change = true;
                            }
                        }

                        else if (":b".equals(matcher.group(4)))
                        {
                            if (theme != BLACK)
                            {
                                theme = BLACK;
                                change = true;
                            }
                        }

                        else if (":r".equals(matcher.group(4)))
                        {
                            if (theme != RETRO)
                            {
                                theme = RETRO;
                                change = true;
                            }
                        }
                    }

                    else if ("ts".equals(matcher.group(3)))
                    {
                        if (":l".equals(matcher.group(4)))
                        {
                            if (size != LARGE)
                            {
                                size = LARGE;
                                textView.setTextSize(size);
                            }
                        }

                        else if (":m".equals(matcher.group(4)))
                        {
                            if (size != MEDIUM)
                            {
                                size = MEDIUM;
                                textView.setTextSize(size);
                            }
                        }

                        else if (":s".equals(matcher.group(4)))
                        {
                            if (size != SMALL)
                            {
                                size = SMALL;
                                textView.setTextSize(size);
                            }
                        }
                    }

                    else if ("tf".equals(matcher.group(3)))
                    {
                        if (":m".equals(matcher.group(4)))
                        {
                            if (type != MONO)
                            {
                                type = MONO;
                                textView.setTypeface
                                    (Typeface.MONOSPACE, Typeface.NORMAL);
                            }
                        }

                        else if (":p".equals(matcher.group(4)))
                        {
                            if (type != NORMAL)
                            {
                                type = NORMAL;
                                textView.setTypeface
                                    (Typeface.DEFAULT, Typeface.NORMAL);
                            }
                        }
                    }
                }
            }
        }

        if (change)
            recreate(this);
    }

    // loadText
    private void loadText(CharSequence text)
    {
        if (textView != null)
            textView.setText(text);

        if (append != null)
        {
            textView.append(append);
            append = null;
            changed = true;
        }

        else
            changed = false;

        // Check for saved position
        if (pathMap.containsKey(path))
            textView.postDelayed(() ->
                                 scrollView.smoothScrollTo
                                 (0, pathMap.get(path)),
                                 POSITION_DELAY);
        else
            textView.postDelayed(() ->
                                 scrollView.smoothScrollTo(0, 0),
                                 POSITION_DELAY);
        // Check mode
        checkMode(text);

        // Check highlighting
        checkHighlight();

        // Set read only
        if (view)
        {
            textView.setRawInputType(InputType.TYPE_NULL);

            // Update boolean
            edit = false;
        }

        else
        {
            // Set editable with or without suggestions
            if (suggest)
                textView.setInputType(InputType.TYPE_CLASS_TEXT |
                                      InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            else
                textView.setInputType(InputType.TYPE_CLASS_TEXT |
                                      InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                      InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

            // Change typeface temporarily as workaround for yet another
            // obscure feature of some versions of android
            textView.setTypeface((type == NORMAL)?
                                 Typeface.MONOSPACE:
                                 Typeface.DEFAULT, Typeface.NORMAL);
            textView.setTypeface((type == NORMAL)?
                                 Typeface.DEFAULT:
                                 Typeface.MONOSPACE, Typeface.NORMAL);
            // Update boolean
            edit = true;
        }

        // Dismiss keyboard
        textView.clearFocus();

        // Update menu
        invalidateOptionsMenu();
    }

    // QueryTextListener
    private class QueryTextListener
        implements SearchView.OnQueryTextListener
    {
        private BackgroundColorSpan span = new
            BackgroundColorSpan(Color.YELLOW);
        private Editable editable;
        private Matcher matcher;
        private Pattern pattern;
        private int index;
        private int height;

        // onQueryTextChange
        @Override
        @SuppressWarnings("deprecation")
        public boolean onQueryTextChange(String newText)
        {
            // Use regex search and spannable for highlighting
            height = scrollView.getHeight();
            editable = textView.getEditableText();

            // Reset the index and clear highlighting
            if (newText.length() == 0)
            {
                index = 0;
                editable.removeSpan(span);
                return false;
            }

            // Check pattern
            try
            {
                pattern = Pattern.compile(newText, Pattern.MULTILINE);
                matcher = pattern.matcher(editable);
            }

            catch (Exception e)
            {
                return false;
            }

            // Find text
            if (matcher.find(index))
            {
                // Get index
                index = matcher.start();

                // Check layout
                if (textView.getLayout() == null)
                    return false;

                // Get text position
                int line = textView.getLayout().getLineForOffset(index);
                int pos = textView.getLayout().getLineBaseline(line);

                // Scroll to it
                scrollView.smoothScrollTo(0, pos - height / 2);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            else
                index = 0;

            return true;
        }

        // onQueryTextSubmit
        @Override
        public boolean onQueryTextSubmit(String query)
        {
            // Find next text
            if (matcher.find())
            {
                // Get index
                index = matcher.start();

                // Get text position
                int line = textView.getLayout().getLineForOffset(index);
                int pos = textView.getLayout().getLineBaseline(line);

                // Scroll to it
                scrollView.smoothScrollTo(0, pos - height / 2);

                // Highlight it
                editable.setSpan(span, matcher.start(), matcher.end(),
                                 Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            else
            {
                matcher.reset();
                index = 0;
            }

            return true;
        }
    }

    // readFile
    private static CharSequence readFile(File file)
    {
        StringBuilder text = new StringBuilder();
        // Open file
        try (BufferedReader reader = new
             BufferedReader(new FileReader(file)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                text.append(line);
                text.append(System.getProperty("line.separator"));
            }

            return text;
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }

        return text;
    }

    // FindTask
    private static class FindTask
            extends AsyncTask<String, Void, List<File>>
    {
        private WeakReference<Editor> editorWeakReference;
        private Pattern pattern;
        private String search;

        // FindTask
        public FindTask(Editor editor)
        {
            editorWeakReference = new WeakReference<>(editor);
        }

        // doInBackground
        @Override
        protected List<File> doInBackground(String... params)
        {
            // Create a list of matches
            List<File> matchList = new ArrayList<>();
            final Editor editor = editorWeakReference.get();
            if (editor == null)
                return matchList;

            search = params[0];
            // Check pattern
            try
            {
                pattern = Pattern.compile(search, Pattern.MULTILINE);
            }

            catch (Exception e)
            {
                return matchList;
            }

            // Get entry list
            List<File> entries = new ArrayList<>();
            for (String path : editor.pathMap.keySet())
            {
                File entry = new File(path);
                entries.add(entry);
            }
 
            // Check the entries
            for (File file : entries)
            {
                CharSequence content = readFile(file);
                Matcher matcher = pattern.matcher(content);
                if (matcher.find())
                    matchList.add(file);
            }

            return matchList;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(List<File> matchList)
        {
            final Editor editor = editorWeakReference.get();
            if (editor == null)
                return;

            // Build dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(editor);
            builder.setTitle(R.string.findAll);

            // If found populate dialog
            if (!matchList.isEmpty())
            {
                List<String> choiceList = new ArrayList<>();
                for (File file : matchList)
                {
                    // Remove path prefix
                    String path = file.getPath();
                    String name =
                        path.replaceFirst(Environment
                                          .getExternalStorageDirectory()
                                          .getPath() + File.separator, "");

                    choiceList.add(name);
                }

                String[] choices = choiceList.toArray(new String[0]);
                builder.setItems(choices, (dialog, which) ->
                {
                    File file = matchList.get(which);
                    Uri uri = Uri.fromFile(file);
                    // Open the entry chosen
                    editor.readFile(uri);

                    // Put the search text back - why it
                    // disappears I have no idea or why I have to
                    // do it after a delay
                    editor.searchView.postDelayed(() ->
                      editor.searchView.setQuery(search, false), FIND_DELAY);
                });
            }

            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
    }

    // ReadTask
    private static class ReadTask
        extends AsyncTask<Uri, Void, CharSequence>
    {
        private WeakReference<Editor> editorWeakReference;

        public ReadTask(Editor editor)
        {
            editorWeakReference = new WeakReference<>(editor);
        }

        // doInBackground
        @Override
        protected CharSequence doInBackground(Uri... uris)
        {
            StringBuilder stringBuilder = new StringBuilder();
            final Editor editor = editorWeakReference.get();
            if (editor == null)
                return stringBuilder;

            try (InputStream inputStream = editor.getContentResolver()
                 .openInputStream(uris[0]);
                 BufferedReader reader = new BufferedReader
                 (new InputStreamReader(inputStream)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    stringBuilder.append(line);
                    stringBuilder.append(System.getProperty("line.separator"));
                }
            }

            catch (Exception e)
            {
                editor.runOnUiThread(() ->
                                     editor.alertDialog(R.string.appName,
                                                        e.getMessage(),
                                                        R.string.ok));
                e.printStackTrace();
            }

            return stringBuilder;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(CharSequence result)
        {
            final Editor editor = editorWeakReference.get();
            if (editor == null)
                return;

            editor.loadText(result);
        }
    }
}
