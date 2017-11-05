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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.SearchView;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
    public final static String DIRTY = "dirty";
    public final static String CONTENT = "content";

    public final static String PREF_WRAP = "pref_wrap";
    public final static String PREF_SUGGEST = "pref_suggest";
    public final static String PREF_THEME = "pref_theme";
    public final static String PREF_PATHS = "pref_paths";
    public final static String PREF_SIZE = "pref_size";
    public final static String PREF_TYPE = "pref_type";
    public final static String PREF_FILE = "pref_file";
    public final static String PREF_POSN = "pref_posn";

    public final static String DOCUMENTS = "Documents";
    public final static String FILE = "Editor.txt";

    private final static int BUFFER_SIZE = 1024;
    private final static int POSN_DELAY = 100;
    private final static int MAX_PATHS = 10;
    private final static int VERSION_M = 23;
    private final static int GET_TEXT = 0;
    private final static int TEXT = 1;

    private final static int LIGHT = 1;
    private final static int DARK  = 2;
    private final static int RETRO = 3;

    private final static int SMALL  = 12;
    private final static int MEDIUM = 18;
    private final static int LARGE  = 24;

    private final static int NORMAL = 1;
    private final static int MONO   = 2;

    private File file;
    private String path;
    private String toAppend;
    private EditText textView;
    private MenuItem searchItem;
    private ScrollView scrollView;
    private SearchView searchView;

    private Map<String, Integer> pathMap;
    private List<String> removeList;

    private boolean wrap = false;
    private boolean suggest = true;

    private boolean dirty = false;
    private boolean isapp = false;

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

        wrap = preferences.getBoolean(PREF_WRAP, false);
        suggest = preferences.getBoolean(PREF_SUGGEST, true);

        theme = preferences.getInt(PREF_THEME, LIGHT);
        size = preferences.getInt(PREF_SIZE, MEDIUM);
        type = preferences.getInt(PREF_TYPE, MONO);

        Set<String> pathSet = preferences.getStringSet(PREF_PATHS, null);
        pathMap = new HashMap<String, Integer>();

        if (pathSet != null)
            for (String path : pathSet)
                pathMap.put(path, preferences.getInt(path, 0));

        removeList = new ArrayList<String>();

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

        textView = (EditText) findViewById(R.id.text);
        scrollView = (ScrollView) findViewById(R.id.vscroll);

        if (!suggest)
            textView.setInputType(InputType.TYPE_CLASS_TEXT |
                                  InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                  InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        setSizeAndTypeface(size, type);

        Intent intent = getIntent();
        Uri uri = intent.getData();

        if (intent.getAction().equals(Intent.ACTION_EDIT) ||
                intent.getAction().equals(Intent.ACTION_VIEW))
        {
            if ((savedInstanceState == null) && (uri != null))
                readFile(uri);

            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        else if (intent.getAction().equals(Intent.ACTION_SEND))
        {
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

            isapp = true;
        }

        else if (intent.getAction().equals(Intent.ACTION_MAIN))
        {
            if (savedInstanceState == null)
                defaultFile(null);

            isapp = true;
        }

        setListeners();
    }

    // setListeners
    private void setListeners()
    {

        if (textView != null)
            textView.addTextChangedListener(new TextWatcher()
        {
            // afterTextChanged
            @Override
            public void afterTextChanged (Editable s)
            {
                dirty = true;
                invalidateOptionsMenu();
            }

            // beforeTextChanged
            @Override
            public void beforeTextChanged (CharSequence s,
                                           int start,
                                           int count,
                                           int after) {}
            // onTextChanged
            @Override
            public void onTextChanged (CharSequence s,
                                       int start,
                                       int before,
                                       int count) {}
        });
    }

    // onRestoreInstanceState
    @Override
    public void onRestoreInstanceState (Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        dirty = savedInstanceState.getBoolean(DIRTY);
        path = savedInstanceState.getString(PATH);
        invalidateOptionsMenu();

        file = new File(path);
        final Uri uri = Uri.fromFile(file);

        String title = uri.getLastPathSegment();
        setTitle(title);

        if (file.lastModified() > modified)
            alertDialog(R.string.appName, R.string.changedReload,
                        R.string.reload, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                switch (id)
                {
                case DialogInterface.BUTTON_POSITIVE:
                    readFile(uri);
                }
            }
        });
    }

    // onPause
    @Override
    public void onPause ()
    {
        super.onPause();

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

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

        if (dirty)
            saveFile();
    }

    // onSaveInstanceState
    @Override
    public void onSaveInstanceState (Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean(DIRTY, dirty);
        outState.putString(PATH, path);
    }

    // onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();

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
    public boolean onPrepareOptionsMenu (Menu menu)
    {
        menu.findItem(R.id.open).setVisible (isapp);
        menu.findItem(R.id.save).setVisible (dirty);

        menu.findItem(R.id.wrap).setChecked (wrap);
        menu.findItem(R.id.suggest).setChecked (suggest);

        switch (theme)
        {
        case LIGHT:
            menu.findItem(R.id.light).setChecked (true);
            break;

        case DARK:
            menu.findItem(R.id.dark).setChecked (true);
            break;

        case RETRO:
            menu.findItem(R.id.retro).setChecked (true);
            break;
        }

        switch (size)
        {
        case SMALL:
            menu.findItem(R.id.small).setChecked (true);
            break;

        case MEDIUM:
            menu.findItem(R.id.medium).setChecked (true);
            break;

        case LARGE:
            menu.findItem(R.id.large).setChecked (true);
            break;
        }

        switch (type)
        {
        case MONO:
            menu.findItem(R.id.mono).setChecked (true);
            break;

        case NORMAL:
            menu.findItem(R.id.normal).setChecked (true);
            break;
        }

        // Get a list of recent files
        List<Long> list = new ArrayList<Long>();
        Map<Long, String> map = new HashMap<Long, String>();

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
        case R.id.open:
            openFile();
            break;
        case R.id.save:
            saveFile();
            break;
        case R.id.saveAs:
            saveAs();
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

        // Save path
        savePath(path);

        return true;
    }

    // onBackPressed
    @Override
    public void onBackPressed()
    {
        if (dirty)
            alertDialog(R.string.appName, R.string.modified,
                        new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                switch (id)
                {
                case DialogInterface.BUTTON_POSITIVE:
                    finish();
                }
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

    // defaultFile
    private void defaultFile(String text)
    {
        File documents = new
        File(Environment.getExternalStorageDirectory(), DOCUMENTS);
        file = new File(documents, FILE);

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
                             DialogInterface.OnClickListener listener)
    {
        alertDialog(title, message, R.string.discard, listener);
    }

    // alertDialog
    private void alertDialog(int title, int message, int positiveButton,
                             DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add the buttons
        builder.setPositiveButton(positiveButton, listener);
        builder.setNegativeButton(R.string.cancel, listener);

        // Create the AlertDialog
        builder.show();
    }

    // savePath
    private void savePath(String path)
    {
        // Save the current position
        pathMap.put(path, scrollView.getScrollY());

        // Get a list of files
        List<Long> list = new ArrayList<Long>();
        Map<Long, String> map = new HashMap<Long, String>();
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
                alertDialog(R.string.openRecent, R.string.modified,
                            new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            switch (id)
                            {
                            case DialogInterface.BUTTON_POSITIVE:
                                readFile(uri);
                                break;
                            }
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
        saveAsDialog(R.string.saveAs, R.string.choose, name,
                     new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                switch (id)
                {
                case DialogInterface.BUTTON_POSITIVE:
                    EditText text =
                        (EditText) ((Dialog) dialog).findViewById(TEXT);
                    String name = text.getText().toString();
                    file = new File(name);

                    // Check absolute file
                    if (!file.isAbsolute())
                        file = new
                            File(Environment.getExternalStorageDirectory(),
                                 File.separator + name);

                    path = file.getPath();
                    saveFile();
                }
            }
        });
    }

    // saveAsDialog
    private void saveAsDialog(int title, int message, String path,
                              DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add the buttons
        builder.setPositiveButton(R.string.ok, listener);
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
            textView.setInputType(InputType.TYPE_CLASS_TEXT |
                                  InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        else
            textView.setInputType(InputType.TYPE_CLASS_TEXT |
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
        builder.show();
    }

    // openFile
    private void openFile()
    {
        if (dirty)
            alertDialog(R.string.open, R.string.modified,
                        new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                switch (id)
                {
                case DialogInterface.BUTTON_POSITIVE:
                    getContent();
                    break;
                }
            }
        });

        else
            getContent();
    }

    // getContent
    private void getContent()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        startActivityForResult(Intent.createChooser(intent, null), GET_TEXT);
    }

    // readFile
    private void readFile(Uri uri)
    {
        if (uri == null)
            return;

        if (uri.getScheme().equalsIgnoreCase(CONTENT))
            uri = resolveContent(uri);

        path = uri.getPath();
        file = new File(path);

        String title = uri.getLastPathSegment();
        setTitle(title);

        textView.setText(R.string.loading);

        ReadTask read = new ReadTask();
        read.execute(file);

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
                        R.string.overwrite, new
                        DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                switch (id)
                {
                case DialogInterface.BUTTON_POSITIVE:
                    saveFile(file);
                    break;
                }
            }
        });

        else
            saveFile(file);
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

        catch (Exception e) {}
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
        public boolean onQueryTextChange (String newText)
        {
            // Use regex search and spannable for highlighting
            height = scrollView.getHeight();
            editable = textView.getEditableText();
            text = textView.getText().toString();

            // Check text
            if (text.length() == 0)
                return false;

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

                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Scroll " + pos);

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
        public boolean onQueryTextSubmit (String query)
        {
            // Find next text
            if (matcher.find())
            {
                // Get index
                index = matcher.start();

                // Get text position
                int line = textView.getLayout().getLineForOffset(index);
                int pos = textView.getLayout().getLineBaseline(line);

                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Scroll " + pos);

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
    private class ReadTask
        extends AsyncTask<File, Integer, String>
    {
        // doInBackground
        @Override
        protected String doInBackground(File... params)
        {
            StringBuilder text = new StringBuilder();
            try
            {
                FileReader fileReader = new FileReader(params[0]);
                char buffer[] = new char[BUFFER_SIZE];
                int n;
                while ((n = fileReader.read(buffer)) != -1)
                    text.append(String.valueOf(buffer, 0, n));
                fileReader.close();
            }

            catch (Exception e) {}

            return text.toString();
        }

        // onProgressUpdate
        @Override
        protected void onProgressUpdate(Integer... progress)
        {
            // no-op
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
                textView.postDelayed(new Runnable()
                {
                    // run
                    @Override
                    public void run()
                    {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Scroll " + pathMap.get(path));

                        scrollView.smoothScrollTo(0, pathMap.get(path));
                    }
                }, POSN_DELAY);
            }

            invalidateOptionsMenu();
        }
    }
}
