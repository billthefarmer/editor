# Mode line

If a line of text is found within the first two or three lines of the
file which matches the mode line pattern, the mode of the editor will
be changed after the file is loaded. The pattern is one or more non
white space characters, one or more white space characters, the text
`ed:` followed by one or more option patterns separated by white space.

```
# ed: [[no]ww] [[no]sg] [[no]hs] [th:l|d|r] [ts:l|m|s] [tf:m|p]
```

The initial non white space characters are intended to be used to hide
the mode line from compilers and interpreters by commenting it out.

The option patterns are:

 * **[no]ww** &ndash; Word wrap
 * **[no]sg** &ndash; Suggestions
 * **[no]hs** &ndash; Syntax highlighting
 * **th:l|d|r** &ndash; Theme &ndash; light, dark or retro
 * **ts:l|m|s** &ndash; TextSize &ndash; large, medium or small
 * **tf:m|p** &ndash; Typeface &ndash; monospace or proportional

The mode line is read after the file is loaded and will change
immediately, except in Android 6, Marshmallow due to an obscure
bug. Rotate the device to change mode.
