# ![Logo](src/main/res/drawable-mdpi/ic_launcher.png) Editor [![Build Status](https://travis-ci.org/billthefarmer/editor.svg?branch=master)](https://travis-ci.org/billthefarmer/editor) [![Available on F-Droid](https://f-droid.org/wiki/images/c/ca/F-Droid-button_available-on_smaller.png)](https://f-droid.org/repository/browse/?fdid=org.billthefarmer.editor)

Android simple generic text editor. The app is available from
[F-Droid](https://f-droid.org/repository/browse/?fdid=org.billthefarmer.editor)
and [here](https://github.com/billthefarmer/editor/releases)

![Editor](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Editor.png) ![Editor](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Editor-styles.png)

This is a very simple generic text editor which may be used standalone
or to show or edit any sort of text file from another app. If you
select a text file in a file manager or similar app you will be
offered the option of using this editor along with whatever other apps
you have installed that can show or edit a text file.

There are two toolbar items which may appear:
* **Open** - Open a text file using a chooser
* **Save** - Save the current file if modified

And on the menu:
* **Word wrap** - Limit text width to screen width and word wrap
* **Dark theme** - Dark background with light text

If you touch the back or open button, and the current file has been
modified, you will be prompted whether you want to discard it, else
the editor will just exit or open a file chooser.

If there is no open file any text entered will be saved in
```Documents/Editor.txt```. Use a file manager to move and rename it
if required. If you have the OI File Manager installed the file picker
will allow a file name to be entered. The latest version (2.1.1) from
F-Droid has issues on Android 7, use the earlier version from the Play
Store (2.0.5).
