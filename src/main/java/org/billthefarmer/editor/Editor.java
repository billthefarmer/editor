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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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

    public final static String DOCUMENTS = "Documents";
    public final static String FILE = "Editor.txt";

    private final static int BUFFER_SIZE = 1024;
    private final static int GET_TEXT = 0;

    private File file;
    private String path;
    private EditText textView;

    private boolean dirty = false;
    private boolean isapp = false;

    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editor);

        textView = (EditText) findViewById(R.id.text);

        Intent intent = getIntent();
        Uri uri = intent.getData();

        if (savedInstanceState == null)
        {
            if (uri != null)
                readFile(uri);
        }

        if (uri == null)
        {
            isapp = true;

            File documents = new
                File(Environment.getExternalStorageDirectory(), DOCUMENTS);
            file = new File(documents, FILE);
            uri = Uri.fromFile(file);
            path = uri.getPath();
        }

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

    // openFile
    private void openFile()
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

        String title = uri.getLastPathSegment();
        setTitle(title);

        textView.setText(R.string.loading);

        path = uri.getPath();
        file = new File(path);
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
            textView.setText(result);
            dirty = false;
            invalidateOptionsMenu();
        }
    }
}
