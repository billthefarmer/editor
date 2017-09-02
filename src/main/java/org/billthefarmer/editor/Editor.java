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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class Editor extends Activity
{
    public final static String TAG = "Editor";
    public final static String PATH = "path";
    public final static String DIRTY = "dirty";
    public final static String CONTENT = "content";

    public final static String PREF_WRAP = "pref_wrap";
    public final static String PREF_DARK = "pref_dark";

    public final static String DOCUMENTS = "Documents";
    public final static String FILE = "Editor.txt";

    private final static int MAX_LENGTH = 1048576;
    private final static int BUFFER_SIZE = 1024;
    private final static int VERSION_M = 23;
    private final static int GET_TEXT = 0;
    private final static int TEXT = 1;

    private File file;
    private String path;
    private EditText textView;

    private boolean wrap = false;
    private boolean dark = false;

    private boolean dirty = false;
    private boolean isapp = false;

    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        wrap = preferences.getBoolean(PREF_WRAP, false);
        dark = preferences.getBoolean(PREF_DARK, false);

        if (dark)
            setTheme(R.style.AppDarkTheme);

        if (wrap)
            setContentView(R.layout.wrap);

        else
            setContentView(R.layout.edit);

        textView = (EditText) findViewById(R.id.text);

        Intent intent = getIntent();
        Uri uri = intent.getData();

        if (savedInstanceState == null)
        {
            if (uri != null)
                readFile(uri);

            else
                defaultFile();
        }

        if (uri == null)
            isapp = true;

        else
            getActionBar().setDisplayHomeAsUpEnabled(true);

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
        Uri uri = Uri.fromFile(file);

        String title = uri.getLastPathSegment();
        setTitle(title);
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
        editor.putBoolean(PREF_DARK, dark);
        editor.apply();
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
        return true;
    }

    // onPrepareOptionsMenu
    @Override
    public boolean onPrepareOptionsMenu (Menu menu)
    {
        menu.findItem(R.id.open).setVisible (isapp);
        menu.findItem(R.id.save).setVisible (dirty);

        menu.findItem(R.id.wrap).setChecked (wrap);
        menu.findItem(R.id.dark).setChecked (dark);

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
        case R.id.dark:
            darkClicked(item);
            break;
        default:
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    // onBackPressed
    @Override
    public void onBackPressed()
    {
        if (dirty)
            alertDialog(R.string.appName, R.string.modified, new
                        DialogInterface.OnClickListener()
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
    private void defaultFile()
    {
        File documents = new
            File(Environment.getExternalStorageDirectory(), DOCUMENTS);
        file = new File(documents, FILE);

        Uri uri = Uri.fromFile(file);
        path = uri.getPath();

        String title = uri.getLastPathSegment();
        setTitle(title);
    }

    // alertDialog
    private void alertDialog(int title, int message,
                             DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add the buttons
        builder.setPositiveButton(R.string.ok, listener);
        builder.setNegativeButton(R.string.cancel, listener);

        // Create the AlertDialog
        builder.show();
    }

    // saveAs
    private void saveAs()
    {
        saveAsDialog(R.string.saveAs, path,
                     new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    switch (id)
                    {
                    case DialogInterface.BUTTON_POSITIVE:
                        EditText text =
                            (EditText) ((Dialog) dialog).findViewById(TEXT);
                        path = text.getText().toString();
                        file = new File(path);
                        saveFile();
                    }
                }
            });
    }

    // saveAsDialog
    private void saveAsDialog(int title, String path,
                              DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

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

    // darkClicked
    private void darkClicked(MenuItem item)
    {
        dark = !dark;
        item.setChecked(dark);

        if (Build.VERSION.SDK_INT != VERSION_M)
            recreate();
    }

    // openFile
    private void openFile()
    {
        if (dirty)
            alertDialog(R.string.open, R.string.modified, new
                        DialogInterface.OnClickListener()
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
        String text = textView.getText().toString();
        write(text, file);
        dirty = false;
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

            dirty = false;
            invalidateOptionsMenu();
        }
    }
}
