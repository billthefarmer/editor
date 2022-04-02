# Syntax Highlighting

## Selection
The type of highlighting is selected by file extension. Files with
`.c`, `.cc`, `.c++`, `.cxx`, `.m`, `.h`, `.go`, `.js`, `.java`, `.py`,
`.sh`, `.swift` extensions will be highlighted with 'C' style
highlighting. Files with `.htm`, `.html` extensions will be
highlighted as HTML. Files with `.cs`, `.css` extensions will be
highlighted as CSS. Files with `.org` extension will be highlighted as
Emacs Org. Files with `.md` extensions will be highlighted as
markdown.

## Parsing
The algorithm makes no attempt at parsing. The text is scanned for
relevent keywords, classes, constants, strings and comments and
highlighted accordingly. Therefore it will not be exactly correct, but
good enough for a simple text editor. See [Source Code Syntax
Highlighting][1].

### C type
Keywords and types are matched from lists of C/C++/Objective
C/Go/Java/Javascript/Python/Shell/Swift keywords and types. Classes
are capitalised words. Constants are all caps words. Strings are in
double quotes. Single quotes are ignored because apostrophes break the
algorithm. Both `/* */` and `// ` C style comments and `# ` Shell
script style comments are recognised.

### HTML
HTML keywords are matched from a list. Double quoted arguments are
highlighted. As above, single quotes are ignored. HTML comments `<!--
-->` are recognised.

### CSS
CSS style names are matched from a list. Double quoted arguments are
highlighted. As above, single quotes are ignored. C style comments are
recognised.

### Emacs Org
Metadata, headers, links, emphasis and inline code will be highlighted.

### Markdown
Markdown headers, links, emphasis and code will be highlighted.

### Default
Files with unrecognised extensions which are not plain text files
will be highlighted with default highlighting, similar to C type
highlighting. Comments will not be highlighted as there is little
consistency with comment delimiters outside C type languages.

## Limitations
Because scanning and highlighting a large file can be quite slow,
making the app unresponsive, only the text currently in view is
scanned and highlighted. Therefore as the text is edited or scrolled,
the new region in view will be scanned and highlighted after a short
delay to allow for user typing without the highlighting running
constantly.

### Scrolling
After the text is highlighted, the android view system will re-layout
the views whether they need it or not. That causes the current cursor
position to be scrolled back into view, which can be extremely
annoying. So the cursor is moved if necessary to keep it within the
visible region.

### Horizontal scrolling
On devices running android versions less than Marshmallow M (6),
horizontal scrolling will scroll back again. Make the text size
smaller or rotate the device to avoid this. Or turn the highlighting
off.

 [1]: https://billthefarmer.github.io/blog/post/source-code-highlighting (https://billthefarmer.github.io/blog/post/source-code-highlighting)
