////////////////////////////////////////////////////////////////////////////////
//
//  Editor - Text editor for Android
//
//  Copyright © 2021  Bill Farmer
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

package org.mozilla.universalchardet;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.nio.charset.Charset;

public class CharsetDetector
{
    private CharsetDetector() {}

    /**
     * Array of supported charsets
     */
    public final static String[] CHARSETS =
    {
        Constants.CHARSET_ISO_2022_JP,
        Constants.CHARSET_ISO_2022_CN,
        Constants.CHARSET_ISO_2022_KR,
        Constants.CHARSET_ISO_8859_5,
        Constants.CHARSET_ISO_8859_7,
        Constants.CHARSET_ISO_8859_8,
        Constants.CHARSET_BIG5,
        Constants.CHARSET_GB18030,
        Constants.CHARSET_EUC_JP,
        Constants.CHARSET_EUC_KR,
        Constants.CHARSET_EUC_TW,
        Constants.CHARSET_SHIFT_JIS,
        Constants.CHARSET_IBM855,
        Constants.CHARSET_IBM866,
        Constants.CHARSET_KOI8_R,
        Constants.CHARSET_MACCYRILLIC,
        Constants.CHARSET_WINDOWS_1251,
        Constants.CHARSET_WINDOWS_1252,
        Constants.CHARSET_WINDOWS_1253,
        Constants.CHARSET_WINDOWS_1255,
        Constants.CHARSET_UTF_8,
        Constants.CHARSET_UTF_16BE,
        Constants.CHARSET_UTF_16LE,
        Constants.CHARSET_UTF_32BE,
        Constants.CHARSET_UTF_32LE,
        Constants.CHARSET_TIS620,
        Constants.CHARSET_US_ASCCI
    };

    /**
     * Gets the charset of content from InputStream.
     *
     * @param inputStream InputStream containing text file
     * @return The charset of the file, null if cannot be determined
     * @throws IOException if some IO error occurs
     */
    public static String detectCharset(InputStream inputStream)
        throws IOException
    {
        String encoding = UniversalDetector.detectCharset(inputStream);
        inputStream.reset();
        return encoding;
    }

    /**
     * Create a reader from a file with correct encoding
     * @param inputStream The stream to read from
     * @param defaultCharset defaultCharset to use if can't be determined
     * @return BufferedReader for the file with the correct encoding
     * @throws java.io.IOException if some I/O error ocurrs
     */
    public static BufferedReader createBufferedReader(InputStream inputStream,
                                                      Charset defaultCharset)
        throws IOException
    {
        Charset cs = (defaultCharset == null)?
            Charset.forName("UTF-8"): defaultCharset;

        String detectedEncoding = detectCharset(inputStream);

        if (detectedEncoding != null)
            cs = Charset.forName(detectedEncoding);

        if (!cs.name().contains("UTF"))
            return new BufferedReader
                (new InputStreamReader(inputStream, cs));

        return new BufferedReader
            (new InputStreamReader
             (new UnicodeBOMInputStream(inputStream), cs));
    }
}
