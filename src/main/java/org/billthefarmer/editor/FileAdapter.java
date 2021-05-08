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
import android.os.Environment;
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

    private final static String ROOT = "/";

    private final static String IMAGE = "image";
    private final static String AUDIO = "audio";
    private final static String VIDEO = "video";

    private final static String APPLICATION = "application";

    private final static long LARGE = 262144;

    private LayoutInflater inflater;
    private List<File> files;

    private int fileId;
    private int audioId;
    private int imageId;
    private int videoId;
    private int folderId;
    private int parentId;
    private int externalId;
    private int applicationId;

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

        if (typedArray.hasValue(R.styleable.Editor_parent))
            parentId =
                typedArray.getResourceId(R.styleable.Editor_parent, 0);

        if (typedArray.hasValue(R.styleable.Editor_file))
            fileId =
                typedArray.getResourceId(R.styleable.Editor_file, 0);

        if (typedArray.hasValue(R.styleable.Editor_audio))
            audioId =
                typedArray.getResourceId(R.styleable.Editor_audio, 0);

        if (typedArray.hasValue(R.styleable.Editor_image))
            imageId =
                typedArray.getResourceId(R.styleable.Editor_image, 0);

        if (typedArray.hasValue(R.styleable.Editor_video))
            videoId =
                typedArray.getResourceId(R.styleable.Editor_video, 0);

        if (typedArray.hasValue(R.styleable.Editor_external))
            externalId =
                typedArray.getResourceId(R.styleable.Editor_external, 0);

        if (typedArray.hasValue(R.styleable.Editor_application))
            applicationId =
                typedArray.getResourceId(R.styleable.Editor_application, 0);

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
            if (file.getParentFile() == null)
                name.setText(ROOT);

            else
                name.setText(file.getName());

            name.setEnabled(true);
            name.setClickable(false);

            if (file.isDirectory())
            {
                if (position == 0)
                    name.setCompoundDrawablesWithIntrinsicBounds(parentId,
                                                                 0, 0, 0);
                else
                    name.setCompoundDrawablesWithIntrinsicBounds(folderId,
                                                                 0, 0, 0);
            }

            else
            {
                // Get the mime type
                String type = FileUtils.getMimeType(file);
                if (type != null && type.startsWith(IMAGE))
                {
                    name.setEnabled(false);
                    name.setClickable(true);
                    name.setCompoundDrawablesWithIntrinsicBounds(imageId,
                                                                 0, 0, 0);
                }

                else if (type != null && type.startsWith(AUDIO))
                {
                    name.setEnabled(false);
                    name.setClickable(true);
                    name.setCompoundDrawablesWithIntrinsicBounds(audioId,
                                                                 0, 0, 0);
                }

                else if (type != null && type.startsWith(VIDEO))
                {
                    name.setEnabled(false);
                    name.setClickable(true);
                    name.setCompoundDrawablesWithIntrinsicBounds(videoId,
                                                                 0, 0, 0);
                }

                else if (type != null && type.startsWith(APPLICATION))
                    name.setCompoundDrawablesWithIntrinsicBounds(applicationId,
                                                                 0, 0, 0);

                else
                    name.setCompoundDrawablesWithIntrinsicBounds(fileId,
                                                                 0, 0, 0);
            }

            // Too large
            if (file.length() > LARGE)
            {
                name.setEnabled(false);
                name.setClickable(true);
            }
        }

        return convertView;
    }
}
