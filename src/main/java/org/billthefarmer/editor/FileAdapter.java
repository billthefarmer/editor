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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.List;

// FileAdapter class
public class FileAdapter extends BaseAdapter
{
    private final static String TAG = "FileAdapter";

    private final static String IMAGE = "image";
    private final static String AUDIO = "audio";
    private final static String VIDEO = "video";

    private LayoutInflater inflater;
    private List<File> files;
    private int folderId;
    private int fileId;

    // Constructor
    public FileAdapter(Context context, List<File> files)
    {
        inflater = LayoutInflater.from(context);
        this.files = files;

        final TypedArray typedArray =
            context.obtainStyledAttributes(R.styleable.Editor);

        if (typedArray.hasValue(R.styleable.Editor_folder))
            folderId =
                typedArray.getResourceId(R.styleable.Editor_folder, 0);

        if (typedArray.hasValue(R.styleable.Editor_file))
            fileId =
                typedArray.getResourceId(R.styleable.Editor_file, 0);

        typedArray.recycle();
    }

    @Override
    public int getCount()
    {
        return files.size();
    }

    @Override
    public Object getItem(int position)
    {
        return null;
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    // Create a new View for each item referenced by the adapter
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // Create a new view
        if (convertView == null)
            convertView = inflater.inflate(R.layout.file, parent, false);

        // Find the views
        TextView name = convertView.findViewById(R.id.name);
        File file = files.get(position);

        if (name != null)
        {
            name.setText(file.getName());
            if (file.isDirectory())
                name.setCompoundDrawablesWithIntrinsicBounds(folderId, 0, 0, 0);

            else
                name.setCompoundDrawablesWithIntrinsicBounds(fileId, 0, 0, 0);

            name.setEnabled(true);
            name.setClickable(false);

            // Get the mime type
            String type = FileUtils.getMimeType(file);
            if (type != null)
            {
                if (type.startsWith(IMAGE) ||
                    type.startsWith(AUDIO) ||
                    type.startsWith(VIDEO))
                {
                    name.setEnabled(false);
                    name.setClickable(true);
                }
            }
        }

        return convertView;
    }
}
