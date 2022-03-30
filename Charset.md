# Character set

![Editor](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/Editor-charset.png)

## Default
The default character set is set to `UTF-8` on selecting **New** from
the toolbar. It may be changed in the menu. Android defaults to
`UTF-8`, don't use anything else unless you are sure what you are
doing.

## Detection
The current character set is optionally detected on reading a file by
the detection code from [International Components for
Unicode](https://unicode-org.github.io/icu/userguide/icu4j). If there
are no accented characters or symbols in the text to give the
detection algorithm something to work on it may not get it right.

## Saving
Files will be saved using the current character set. To change it, use
the **Charset** item in the menu, which shows the current set.

**Caution** &ndash; If you add accented characters or symbols to the
text, make sure to check the current character set before you save it.

## Mode line
The character set may be set to `UTF-8` by using a mode line in the text.

```
# ed: cs:u
```

See [Mode line](Mode.md) for details.
