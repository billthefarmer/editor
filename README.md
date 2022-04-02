# ![Logo](src/main/res/drawable-hdpi/ic_launcher.png) Editor ![.github/workflows/main.yml](https://github.com/billthefarmer/editor/workflows/.github/workflows/main.yml/badge.svg) [![Release](https://img.shields.io/github/release/billthefarmer/editor.svg?logo=github)](https://github.com/billthefarmer/editor/releases)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.svg" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/org.billthefarmer.editor)

Android simple generic text editor. The app is available from
[F-Droid](https://f-droid.org/packages/org.billthefarmer.editor)
and [here](https://github.com/billthefarmer/editor/releases)

### Issues
There have been a number of issues raised on this app where users have
obviously not read this README, looked at the
[documentation](https://billthefarmer.github.io/editor/), or looked at
old closed issues. Please read the README, read the
[docs](https://billthefarmer.github.io/editor/), and look at the old
issues before raising another one, you can search the issues from the
box at the top.

![Editor](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Editor.png) ![Editor](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Editor-chooser.png)

![Editor](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Editor-landscape.png)

![Editor](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Editor-syntax.png)

This is a fairly simple generic text editor which may be used
standalone or to show or edit any sort of text file from another
app. If you select a text file in a file manager or similar app you
will be offered the option of using this editor along with whatever
other apps you have installed that can show or edit a text file. Files
will initially be opened read only, long touch on the display or touch
the edit item in the toolbar to enable editing.

There are five toolbar items which may appear:
* **Edit** &ndash; Edit the current read only file
* **View** &ndash; View the current file read only
* **Save** &ndash; Save the current file if modified
* **New** &ndash; Start a new empty file
* **Open** &ndash; Open a text file using a chooser

And on the menu:
* **Open recent** &ndash; Pop up a list of recent files
  * **Clear list** &ndash; Clear list of recent files
* **Search** &ndash; Interactive search of text using a regular
    expression
* **Find all** &ndash; Find all recent files containing search text
* **Save as** &ndash; Save the current file with a new name
* **Charset** &ndash; Change the current character set, shows current set
* **View markdown** &ndash; View markdown in browser or html viewer
* **View files** &ndash; Open files read only for viewing
* **Auto save** &ndash; Save the current file on app pause
* **Word wrap** &ndash; Limit text width to screen width and word wrap
* **Suggestions** &ndash; Text input and spelling suggestions
* **Highlight syntax** &ndash; Highlight programming language syntax
* **Theme** &ndash; Choose theme
  * **Light**
  * **Dark**
  * **Black**
  * **Retro**
* **Text size** &ndash; Choose text size
  * **Small**
  * **Medium**
  * **Large**
* **Typeface** &ndash; Choose typeface
  * **Monospace**
  * **Proportional**
* **About** &ndash; Show version, copyright and licence

### Edit
Edit the current read only text.

### View
View the current file read only.

### Save
Save the current file if modified.

### New
Start a new empty file. Use the **Save as** item to save the new file.

### Open
Choose a file to open from the chooser dialog that pops up. The parent
folder will be the first in the list. See [File
Chooser](Chooser.md). The file will initially be read-only. Touch the
**Edit** toolbar item to enable editing.

### Open recent
Choose a file from the list that pops up. As above the file will
initially be read only. The last entry, **Clear list**, will clear the
list.

### Save as
Use the dialog and the **Save** button or the **Storage** button to
use the android file manager to save the file.

### Search
Enter search text in the field that pops up in the toolbar. The first
matching item will be highlighted. Use the search button in the
keyboard for find next. The exact regular expression syntax used is in
the android documentation for
[Pattern](https://developer.android.com/reference/java/util/regex/Pattern#sum).

### Find all
You may find all recent files that contain the current search
text. This menu item will only appear while the search widget is
active. A dialog will pop up with a list of matching files. Touch an
entry to open that file. You may repeat this or refine the search text
to find the desired file.

### View markdown
You will be prompted to choose a viewer for an html file containing
the encoded markdown from the current open file. If the text contains
no markdown the result will be the same text.

### Shortcut
You may create a **New file** shortcut in the launcher which will open
a new file in Editor.

### Regular expressions
Explaining [regular
expressions](https://en.wikipedia.org/wiki/Regular_expression) used in
the text search is beyond the scope of this README. There is at least
one
[book](https://www.oreilly.com/library/view/mastering-regular-expressions/0596528124/)
devoted to the subject. Use `(?i)` for case insensitive search.

### Character set
The current character set is displayed under the current file
name. The character set is optionally detected when a new file is
read. It may be changed by selecting the **Charset** item in the menu,
which shows the current character set. See [Character
set](Charset.md).

### Highlight syntax
If the current open file is a C, C++, Objective C, Go, Java,
Javascript, Python, Shell script, Swift, CSS, HTML, Org or Markdown
file, the keywords, classes comments, strings, etc will be
highlighted. See [Syntax Highlighting](Syntax.md).

### Mode line
If a line of text is found within the first or last two or three lines
of the file which matches the mode line pattern, the mode of the
editor will be changed after the file is loaded. See [Mode
line](Mode.md).
```
# ed: [[no]vw] [[no]ww] [[no]sg] [[no]hs] [cs:u] [th:l|d|b|r] [ts:l|m|s] [tf:m|p]
```

### Word count
The file word count and character count are shown in the toolbar. Due
to the algorithm used, the result may differ from that produced by
other utilities.

### Extended selection
If the file being edited is not a plain text file, selections created
by double tapping or long touching on the text will be extended to
enclosing delimiters (brackets, quotes) on the same text line.

### Text size
Text size may be changed from the menu or by pinch or expand gestures
on the text or by doubletap and swipe. The response to gestures on
large files may be slow or delayed.

### Unsaved file
If you touch the new, back or open button, and the current file has been
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

### Shared file
Text files opened or shared by another app may be viewed and
edited. Some apps may share files or text using a `content` URI that
is not resolvable to a path to a file in storage. In that case the
editor will read the file into a new file. The **Save** item will save
the file in the original location if possible.

**Note** &ndash; Apps that share files will usually only give another app read
permission. Use the **Save as** menu item to save the file elsewhere.

### Keyboard shortcuts
When using an external keyboard, some keyboard shortcuts are
implemented:
 * Ctrl+E &ndash; Edit mode
 * Ctrl+Shift+E &ndash; View mode
 * Ctrl+N &ndash; New file
 * Ctrl+O &ndash; Open file
 * Ctrl+S &ndash; Save file
 * Ctrl+Shift+S &ndash; Save as

### SD cards
Android allows removable SD cards to be used like a USB stick or as
part of the device storage. Files opened using the file chooser on a
removable SD card may not save successfully using the save button. Use
the **Save as** menu item and the **Storage** button to save it using
the android file manager. Alternatively use the **Storage** button on
the file chooser dialog to open the file using the android file
manager. See [File Chooser](Chooser.md).
