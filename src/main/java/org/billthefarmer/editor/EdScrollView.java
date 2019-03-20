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
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

// EdScrollView
public class EdScrollView extends ScrollView
{
    // OnEdScrollChangeListener
    private OnEdScrollChangeListener onEdScrollChangeListener;

    // EdScrollView
    public EdScrollView (Context context, 
                         AttributeSet attrs)
    {
        super (context, attrs);
    }

    // setOnEdScrollChangeListener
    public void setOnEdScrollChangeListener (OnEdScrollChangeListener l)
    {
        onEdScrollChangeListener = l;
    }

    // onScrollChanged
    @Override
    protected void onScrollChanged (int l, 
                                    int t, 
                                    int oldl, 
                                    int oldt)
    {
        if (onEdScrollChangeListener != null)
            onEdScrollChangeListener.onScrollChange (this, l, t, oldl, oldt);
    }

    // OnEdScrollChangeListener
    public static interface OnEdScrollChangeListener
    {
        // onScrollChange
        public abstract void onScrollChange (View v, 
                                             int scrollX, 
                                             int scrollY, 
                                             int oldScrollX, 
                                             int oldScrollY);
    }
}
