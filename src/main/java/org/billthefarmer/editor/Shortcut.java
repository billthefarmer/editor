////////////////////////////////////////////////////////////////////////////////
//
//  Editor - Text editor for Android
//
//  Copyright © 2019  Bill Farmer
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
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
////////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.editor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

public class Shortcut extends Activity
{
    public final static String TAG = "Shortcut";

    // onCreate
    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Create the shortcut intent
        Intent shortcut = new
            Intent(this, Editor.class);
        shortcut.setAction(Editor.OPEN_NEW);
        shortcut.addCategory(Intent.CATEGORY_DEFAULT);

        BitmapDrawable drawable = (BitmapDrawable) getResources()
            .getDrawable(R.drawable.ic_launcher);
        Bitmap bitmap = drawable.getBitmap();

        // Create the shortcut
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.newFile));
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);

        setResult(RESULT_OK, intent);
        finish();
    }
}
