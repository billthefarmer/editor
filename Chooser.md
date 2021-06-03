# File Chooser

![Editor](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Editor-chooser.png)

The chooser shows a scrollable row of folder buttons and a list of
files with icons. Files which appear to be media files, or are too
large will be disabled and not selectable. Touch a folder button or
folder to change folder, or a file to open a file. Hidden files
beginning with a '.'  will not appear.

The parent folder, if it exists, will appear first in the list. Touch
that folder to move up the directory tree. If a folder is not
accessible, the chooser will show the parent folder, if it exists, and
the external storage folder (`/storage/emulated/0`).

Use the **Storage** button to open files using the android file
manager. This should ensure that files on removeable SD cards can be
saved using the **Save** button. The file manager may refuse to open
some types of text files not recognised by android.
