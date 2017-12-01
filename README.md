# ![Logo](src/main/res/drawable-mdpi/ic_launcher.png) Editor [![Build Status](https://travis-ci.org/billthefarmer/editor.svg?branch=master)](https://travis-ci.org/billthefarmer/editor) [![Available on F-Droid](https://f-droid.org/wiki/images/c/ca/F-Droid-button_available-on_smaller.png)](https://f-droid.org/repository/browse/?fdid=org.billthefarmer.editor)

Android simple generic text editor. The app is available from
[F-Droid](https://f-droid.org/repository/browse/?fdid=org.billthefarmer.editor)
and [here](https://github.com/billthefarmer/editor/releases)

![Editor](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Editor.png) ![Editor](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Editor-styles.png)

![Editor](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Editor-landscape.png)

This is a very simple generic text editor which may be used standalone
or to show or edit any sort of text file from another app. If you
select a text file in a file manager or similar app you will be
offered the option of using this editor along with whatever other apps
you have installed that can show or edit a text file. Files will
initially be opened read only, long touch on the display to enable
editing. Changing theme, word wrap or suggestions or rotating the
device will also enable editing.

There are four toolbar items which may appear:
* **Search** - Interactive search of text using a regular expression
* **Edit** - Edit the current read only text
* **Open** - Open a text file using a chooser
* **Save** - Save the current file if modified

And on the menu:
* **Open recent** - Pop up a list of recent files
* **Save as** - Save the current file with a new name
* **View markdown** - View markdown in browser or html viewer
* **Auto save** - Save the current file on app pause
* **Word wrap** - Limit text width to screen width and word wrap
* **Suggestions** - Text input and spelling suggestions
* **Theme** - Choose theme
  * **Light**
  * **Dark**
  * **Retro**
* **Text size** - Choose text size
  * **Small**
  * **Medium**
  * **Large**
* **Typeface** - Choose typeface
  * **Monospace**
  * **Proportional**
* **About** - Show version, copyright and licence

### Search
Enter search text in the field that pops up in the toolbar. The first
matching item will be highlighted. Use the search button in the
keyboard for find next. The exact regular expression syntax used is in
the android documentation for
[Pattern](https://developer.android.com/reference/java/util/regex/Pattern.html#sum).

### Edit
Edit the current read only text.

### Open
Depending on what file managers or file pickers are installed, you
will get a choice of options to pick a file. The file will initially
be read only, touch the **Edit** toolbar item to enable editing.

### Open recent
Choose a file from the list that pops up. As above the file will
initially be read only.

### Save as
Enter a new file name in the dialog that pops up. Absolute names
starting with a slash '/' will be saved in that exact path. Names
without a starting slash will be saved relative to the main public
folder, `/sdcard/`, or `/storage/emulated/0/`.

### View markdown
You will be prompted to choose a viewer for an html file containing
the encoded markdown from the current open file. If the text contains
no markdown the result will be the same text.

### Unsaved file
If you touch the back or open button, and the current file has been
modified, you will be prompted whether you want to save it, else the
editor will just exit or open a file chooser. The current file may be
saved on app pause using the menu option. The scroll position and name
will be remembered for the last 10 files opened.

### Changed file
If a file has changed in storage while it was open in the editor, if
you attempt to save it, or the app is resumed, you will be prompted
whether to overwrite or reload the file.

### Default file
If there is no open file any text entered will by default be saved in
`Documents/Editor.txt`. This file will be loaded on start if it
exists. Use the `Save as` menu item to save it elsewhere.
