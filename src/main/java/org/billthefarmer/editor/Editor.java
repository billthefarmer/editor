////////////////////////////////////////////////////////////////////////////////
//
//  Editor - Text editor for Android
//
//  Copyright Â© 2017  Bill Farmer
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;

import org.markdownj.MarkdownProcessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    public final static String DIRTY = "dirty";
    public final static String CONTENT = "content";
    public final static String MODIFIED = "modified";

    public final static String PREF_SAVE = "pref_save";
    public final static String PREF_WRAP = "pref_wrap";
    public final static String PREF_SUGGEST = "pref_suggest";
    public final static String PREF_THEME = "pref_theme";
    public final static String PREF_PATHS = "pref_paths";
    public final static String PREF_SIZE = "pref_size";
    public final static String PREF_TYPE = "pref_type";
    public final static String PREF_FILE = "pref_file";
    public final static String PREF_POSN = "pref_posn";

    public final static String DOCUMENTS = "Documents";
    public final static String EDIT_FILE = "Editor.txt";
    public final static String HTML_FILE = "Editor.html";

    public final static String TEXT_HTML = "text/html";
    public final static String TEXT_WILD = "text/*";

    private final static int BUFFER_SIZE = 1024;
    private final static int POSN_DELAY = 100;
    private final static int MAX_PATHS = 10;
    private final static int VERSION_M = 23;
    private final static int GET_TEXT = 0;
    private final static int TEXT = 1;

    private final static int LIGHT = 1;
    private final static int DARK = 2;
    private final static int RETRO = 3;

    private final static int SMALL = 12;
    private final static int MEDIUM = 18;
    private final static int LARGE = 24;

    private final static int NORMAL = 1;
    private final static int MONO = 2;

    private File file;
    private String path;
    private Uri content;
    private String toAppend;
    private EditText textView;
    private MenuItem searchItem;
    private ScrollView scrollView;

    private Map<String, Integer> pathMap;
    private List<String> removeList;

    private boolean save = false;
    private boolean edit = true;

    private boolean wrap = false;
    private boolean suggest = true;

    private boolean dirty = false;
    private boolean isApp = false;

    private long modified;

    private int theme = LIGHT;
    private int size = MEDIUM;
    private int type = MONO;

    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        save = preferences.getBoolean(PREF_SAVE, false);
        wrap = preferences.getBoolean(PREF_WRAP, false);
        suggest = preferences.getBoolean(PREF_SUGGEST, true);

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
        case DARK:
            setTheme(R.style.AppDarkTheme);
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

        if (savedInstanceState != null)
            edit = savedInstanceState.getBoolean(EDIT);

        if (!edit)
            textView.setRawInputType(InputType.TYPE_NULL);

        else if (!suggest)
            textView.setRawInputType(InputType.TYPE_CLASS_TEXT |
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
                    dirty = true;
                }

                // Get uri
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri != null)
                    readFile(uri);
            }

            isApp = true;
            break;
        case Intent.ACTION_MAIN:
            if (savedInstanceState == null)
                defaultFile(null);

            isApp = true;
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
                    dirty = true;
                    invalidateOptionsMenu();
                }

                // beforeTextChanged
                @Override
                public void beforeTextChanged(CharSequence s,
                                              int start,
                                              int count,
                                              int after)
                {
                }

                // onTextChanged
                @Override
                public void onTextChanged(CharSequence s,
                                          int start,
                                          int before,
                                          int count)
                {
                }
            });

            // onFocusChange
            textView.setOnFocusChangeListener((v, hasFocus) ->
            {
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager)
                getSystemService(INPUT_METHOD_SERVICE);
                if (!hasFocus)
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            });

            // onLongClick
            textView.setOnLongClickListener(v ->
            {
                // Do nothing if already editable
                if (edit)
                    return false;

                // Set editable with or without suggestions
                if (!suggest)
                    textView
                    .setRawInputType(InputType.TYPE_CLASS_TEXT |
                                     InputType.TYPE_TEXT_FLAG_MULTI_LINE);

                else
                    textView
                    .setRawInputType(InputType.TYPE_CLASS_TEXT |
                                     InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                     InputType
                                     .TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                // Change typeface temporarily as workaround for yet
                // another obscure feature of some versions of android
                if (Build.VERSION.SDK_INT == VERSION_M)
                {
                    textView.setTypeface((type == NORMAL)?
                                         Typeface.MONOSPACE:
                                         Typeface.DEFAULT, Typeface.NORMAL);
                    textView.setTypeface((type == NORMAL)?
                                         Typeface.DEFAULT:
                                         Typeface.MONOSPACE, Typeface.NORMAL);
                }

                // Update boolean
                edit = true;

                // Update menu
                invalidateOptionsMenu();

                return false;
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
        dirty = savedInstanceState.getBoolean(DIRTY);
        modified = savedInstanceState.getLong(MODIFIED);
        content = savedInstanceState.getParcelable(CONTENT);
        invalidateOptionsMenu();

        file = new File(path);
        final Uri uri = Uri.fromFile(file);

        String title = uri.getLastPathSegment();
        setTitle(title);

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

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putBoolean(PREF_SAVE, save);
        editor.putBoolean(PREF_WRAP, wrap);
        editor.putBoolean(PREF_SUGGEST, suggest);
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

        if (BuildConfig.DEBUG)
            for (String path : pathMap.keySet())
                Log.d(TAG, String.format("onPause %s %d", path,
                                         pathMap.get(path)));
        if (dirty && save)
            saveFile(file);
    }

    // onSaveInstanceState
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putParcelable(CONTENT, content);
        outState.putLong(MODIFIED, modified);
        outState.putBoolean(DIRTY, dirty);
        outState.putBoolean(EDIT, edit);
        outState.putString(PATH, path);
    }

    // onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        if (searchView != null)
        {
            searchView.setSubmitButtonEnabled(true);
            searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
            searchView.setOnQueryTextListener(new QueryTextListener());
        }

        return true;
    }

    // onPrepareOptionsMenu
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.findItem(R.id.edit).setVisible(!edit);
        menu.findItem(R.id.view).setVisible(edit);

        menu.findItem(R.id.save).setVisible(dirty);
        menu.findItem(R.id.open).setVisible(isApp);
        menu.findItem(R.id.openRecent).setVisible(isApp);

        menu.findItem(R.id.autoSave).setChecked(save);
        menu.findItem(R.id.wrap).setChecked(wrap);
        menu.findItem(R.id.suggest).setChecked(suggest);

        switch (theme)
        {
        case LIGHT:
            menu.findItem(R.id.light).setChecked(true);
            break;

        case DARK:
            menu.findItem(R.id.dark).setChecked(true);
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
            saveFile();
            break;
        case R.id.saveAs:
            saveAs();
            break;
        case R.id.viewMarkdown:
            viewMarkdown();
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
        case R.id.light:
            lightClicked(item);
            break;
        case R.id.dark:
            darkClicked(item);
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
        if (searchItem.isActionViewExpanded())
            searchItem.collapseActionView();

        return true;
    }

    // onBackPressed
    @Override
    public void onBackPressed()
    {
        if (dirty)
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
                dirty = false;
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
        // Do nothing if cancelled
        if (resultCode != RESULT_OK)
            return;

        switch (requestCode)
        {
        case GET_TEXT:
            Uri uri = data.getData();
            readFile(uri);
            break;
        }
    }

    // editClicked
    private void editClicked(MenuItem item)
    {
        // Set editable with or without suggestions
        if (!suggest)
            textView.setRawInputType(InputType.TYPE_CLASS_TEXT |
                                     InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        else
            textView.setRawInputType(InputType.TYPE_CLASS_TEXT |
                                     InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                     InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);


        // Change typeface temporarily as workaround for yet another
        // obscure feature of some versions of android
        if (Build.VERSION.SDK_INT == VERSION_M)
        {
            textView.setTypeface((type == NORMAL)?
                                 Typeface.MONOSPACE:
                                 Typeface.DEFAULT, Typeface.NORMAL);
            textView.setTypeface((type == NORMAL)?
                                 Typeface.DEFAULT:
                                 Typeface.MONOSPACE, Typeface.NORMAL);
        }

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

        Uri uri = Uri.fromFile(file);
        path = uri.getPath();

        if (file.exists())
        {
            readFile(uri);
            toAppend = text;
        }
        else
        {
            if (text != null)
                textView.append(text);

            String title = uri.getLastPathSegment();
            setTitle(title);
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

    // savePath
    private void savePath(String path)
    {
        // Save the current position
        pathMap.put(path, scrollView.getScrollY());

        if (BuildConfig.DEBUG)
            Log.d(TAG, String.format("savePath %s %d", path,
                                     pathMap.get(path)));
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
            final Uri uri = Uri.fromFile(file);

            if (dirty)
                alertDialog(R.string.openRecent, R.string.modified, R.string.save,
                            R.string.discard, (dialog, id) ->
            {
                switch (id)
                {
                case DialogInterface.BUTTON_POSITIVE:
                    saveFile();
                    readFile(uri);
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    dirty = false;
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
                EditText text =
                ((Dialog) dialog).findViewById(TEXT);
                String name1 = text.getText().toString();

                // Ignore empty string
                if (name1.isEmpty())
                    return;

                file = new File(name1);

                // Check absolute file
                if (!file.isAbsolute())
                    file = new
                    File(Environment.getExternalStorageDirectory(),
                         File.separator + name1);

                // Set interface title
                Uri uri = Uri.fromFile(file);
                String title = uri.getLastPathSegment();
                setTitle(title);

                path = file.getPath();
                saveFile();
            }
        });
    }

    // saveAsDialog
    private void saveAsDialog(String path, DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.save);
        builder.setMessage(R.string.choose);

        // Add the buttons
        builder.setPositiveButton(R.string.save, listener);
        builder.setNegativeButton(R.string.cancel, listener);

        // Create edit text
        Context context = builder.getContext();
        EditText text = new EditText(context);
        text.setId(TEXT);
        text.setText(path);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setView(text, 30, 0, 30, 0);
        dialog.show();
    }

    // viewMarkdown
    private void viewMarkdown()
    {
        MarkdownProcessor mark = new MarkdownProcessor();
        String text = textView.getText().toString();
        String html = mark.markdown(text);

        try
        {
            File file = new File(getExternalCacheDir(), HTML_FILE);
            file.deleteOnExit();

            FileWriter writer = new FileWriter(file);
            writer.write(html);
            writer.close();

            Uri uri = Uri.fromFile(file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, TEXT_HTML);
            startActivity(intent);
        }
        catch (Exception e)
        {
        }
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

        if (Build.VERSION.SDK_INT != VERSION_M)
            recreate();
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

        if (Build.VERSION.SDK_INT != VERSION_M)
            recreate();
    }

    // lightClicked
    private void lightClicked(MenuItem item)
    {
        theme = LIGHT;
        item.setChecked(true);

        if (Build.VERSION.SDK_INT != VERSION_M)
            recreate();
    }

    // darkClicked
    private void darkClicked(MenuItem item)
    {
        theme = DARK;
        item.setChecked(true);

        if (Build.VERSION.SDK_INT != VERSION_M)
            recreate();
    }

    // retroClicked
    private void retroClicked(MenuItem item)
    {
        theme = RETRO;
        item.setChecked(true);

        if (Build.VERSION.SDK_INT != VERSION_M)
            recreate();
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
    private void aboutClicked()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.about);

        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        String format = getString(R.string.version);

        String message =
            String.format(Locale.getDefault(),
                          format, BuildConfig.VERSION_NAME,
                          dateFormat.format(BuildConfig.BUILT));
        builder.setMessage(message);

        // Add the button
        builder.setPositiveButton(R.string.ok, null);

        // Create the AlertDialog
        Dialog dialog = builder.show();

        // Set movement method
        TextView text = dialog.findViewById(android.R.id.message);
        if (text != null)
            text.setMovementMethod(LinkMovementMethod.getInstance());
    }

    // openFile
    private void openFile()
    {
        if (dirty)
            alertDialog(R.string.open, R.string.modified,
                        R.string.save, R.string.discard, (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                saveFile();
                getContent();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                dirty = false;
                getContent();
                break;
            }
        });

        else
            getContent();
    }

    // getContent
    private void getContent()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(TEXT_WILD);
        startActivityForResult(Intent.createChooser(intent, null), GET_TEXT);
    }

    // readFile
    private void readFile(Uri uri)
    {
        if (uri == null)
            return;

        content = null;

        // Attempt to resolve content uri
        if (uri.getScheme().equalsIgnoreCase(CONTENT))
            uri = resolveContent(uri);

        // Read into default file if unresolved
        if (uri.getScheme().equalsIgnoreCase(CONTENT))
        {
            content = uri;
            file = getDefaultFile();
            Uri defaultUri = Uri.fromFile(file);
            path = defaultUri.getPath();

            String title = uri.getLastPathSegment();
            setTitle(title);
        }

        // Read file
        else
        {
            path = uri.getPath();
            file = new File(path);

            String title = uri.getLastPathSegment();
            setTitle(title);
        }

        textView.setText(R.string.loading);

        ReadTask read = new ReadTask();
        read.execute(uri);

        dirty = false;
        modified = file.lastModified();
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

    // saveFile
    private void saveFile()
    {
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
                saveFile(content);
        }
    }

    // saveFile
    private void saveFile(File file)
    {
        String text = textView.getText().toString();
        write(text, file);
        dirty = false;

        modified = file.lastModified();
        invalidateOptionsMenu();
    }

    // saveFile
    private void saveFile(Uri uri)
    {
        String text = textView.getText().toString();
        try
        {
            OutputStream outputStream =
                getContentResolver().openOutputStream(uri);
            write(text, outputStream);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // write
    private void write(String text, File file)
    {
        file.getParentFile().mkdirs();
        try
        {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(text);
            fileWriter.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // write
    private void write(String text, OutputStream os)
    {
        try
        {
            OutputStreamWriter writer = new OutputStreamWriter(os);
            writer.write(text, 0, text.length());
            writer.close();
            dirty = false;
            invalidateOptionsMenu();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
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
        private String text;
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
            text = textView.getText().toString();

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
                matcher = pattern.matcher(text);
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
                int line = textView.getLayout()
                           .getLineForOffset(index);
                int pos = textView.getLayout()
                          .getLineBaseline(line);

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

    // ReadTask
    @SuppressLint("StaticFieldLeak")
    private class ReadTask
        extends AsyncTask<Uri, Void, String>
    {
        // doInBackground
        @Override
        protected String doInBackground(Uri... params)
        {
            StringBuilder stringBuilder = new StringBuilder();

            try
            {
                InputStream inputStream =
                    getContentResolver().openInputStream(params[0]);
                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null)
                {
                    stringBuilder.append(line);
                    stringBuilder.append(System.getProperty("line.separator"));
                }

                reader.close();
            }
            catch (Exception e)
            {
            }

            return stringBuilder.toString();
        }

        // onPostExecute
        @Override
        protected void onPostExecute(String result)
        {
            if (textView != null)
                textView.setText(result);

            if (toAppend != null)
            {
                textView.append(toAppend);
                toAppend = null;
                dirty = true;
            }
            else
                dirty = false;

            // Check for saved position
            if (pathMap.containsKey(path))
            {
                // run
                textView.postDelayed(() -> scrollView.smoothScrollTo(0, pathMap.get(path)), POSN_DELAY);
            }

            // Set read only
            textView.setRawInputType(InputType.TYPE_NULL);
            textView.clearFocus();

            // Update boolean
            edit = false;

            // Update menu
            invalidateOptionsMenu();
        }
    }
}
