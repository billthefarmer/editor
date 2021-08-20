/*
 * Copyright (C) 2018 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.billthefarmer.editor;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileFilter;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * @author Peli
 * @author paulburke (ipaulpro)
 * @author Bill Farmer (billthefarmer)
 * @version 2017-06-22
 */
public class FileUtils
{
    /**
     * TAG for log messages.
     */
    private static final String TAG = "FileUtils";
    // pattern
    /**
     * File and folder comparator. TODO Expose sorting option method
     *
     * @author paulburke
     */
    public static Comparator<File> sComparator = (f1, f2) ->
    {
        // Sort alphabetically by lower case, which is much cleaner
        return f1.getName().toLowerCase(Locale.getDefault())
        .compareTo(f2.getName().toLowerCase(Locale.getDefault()));
    };

    public static final String MIME_TYPE_AUDIO = "audio/*";
    public static final String MIME_TYPE_TEXT = "text/*";
    public static final String MIME_TYPE_IMAGE = "image/*";
    public static final String MIME_TYPE_VIDEO = "video/*";
    public static final String MIME_TYPE_APP = "application/*";

    public static final String HIDDEN_PREFIX = ".";
    /**
     * File (not directories) filter.
     *
     * @author paulburke
     */
    public static FileFilter sFileFilter = file ->
    {
        final String fileName = file.getName();
        // Return files only (not directories) and skip hidden files
        return file.isFile() && !fileName.startsWith(HIDDEN_PREFIX);
    };
    /**
     * Folder (directories) filter.
     *
     * @author paulburke
     */
    public static FileFilter sDirFilter = file ->
    {
        final String fileName = file.getName();
        // Return directories only and skip hidden directories
        return file.isDirectory() && !fileName.startsWith(HIDDEN_PREFIX);
    };

    private FileUtils()
    {
    } // private constructor to enforce Singleton

    /**
     * Gets the extension of a file name, like ".png" or ".jpg".
     *
     * @param uri
     * @return Extension including the dot("."); "" if there is no
     * extension; null if uri was null.
     */
    public static String getExtension(String uri)
    {
        if (uri == null)
        {
            return null;
        }

        int dot = uri.lastIndexOf(".");
        if (dot >= 0)
        {
            return uri.substring(dot);
        }
        else
        {
            // No extension.
            return "";
        }
    }

    /**
     * @return Whether the URI is a local one.
     */
    public static boolean isLocal(String url)
    {
        return url != null && !url.startsWith("http://") &&
               !url.startsWith("https://");
    }

    /**
     * @return True if Uri is a MediaStore Uri.
     * @author paulburke
     */
    public static boolean isMediaUri(Uri uri)
    {
        return "media".equalsIgnoreCase(uri.getAuthority());
    }

    /**
     * Convert File into Uri.
     *
     * @param file
     * @return uri
     */
    public static Uri getUri(File file)
    {
        return (file != null) ? Uri.fromFile(file) : null;
    }

    /**
     * Returns the path only (without file name).
     *
     * @param file
     * @return
     */
    public static File getPathWithoutFilename(File file)
    {
        if (file != null)
        {
            if (file.isDirectory())
            {
                // no file to be split off. Return everything
                return file;
            }
            else
            {
                String filename = file.getName();
                String filepath = file.getAbsolutePath();

                // Construct path without file name.
                String pathwithoutname = filepath.substring(0,
                                         filepath.length() - filename.length());
                if (pathwithoutname.endsWith(File.separator))
                {
                    pathwithoutname = pathwithoutname
                        .substring(0, pathwithoutname.length() - 1);
                }
                return new File(pathwithoutname);
            }
        }
        return null;
    }

    /**
     * @return The MIME type for the given file.
     */
    public static String getMimeType(File file)
    {

        String extension =
            getExtension(file.getName()).toLowerCase(Locale.getDefault());

        if (extension.length() > 0)
            return MimeTypeMap.getSingleton()
                   .getMimeTypeFromExtension(extension.substring(1));

        return "application/octet-stream";
    }

    /**
     * @return The MIME type for the give Uri.
     */
    public static String getMimeType(Context context, Uri uri)
    {
        File file = new File(getPath(context, uri));
        return getMimeType(file);
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     * @author paulburke
     */
    public static boolean isExternalStorageDocument(Uri uri)
    {
        return "com.android.externalstorage.documents"
               .equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     * @author paulburke
     */
    public static boolean isDownloadsDocument(Uri uri)
    {
        return "com.android.providers.downloads.documents"
               .equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     * @author paulburke
     */
    public static boolean isMediaDocument(Uri uri)
    {
        return "com.android.providers.media.documents"
               .equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri)
    {
        return "com.google.android.apps.photos.content"
               .equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to match.
     * @return The file path from the FileProvider Uri.
     * @author billthefarmer
     */
    public static String fileProviderPath(Uri uri)
    {
        List<String> uriList = uri.getPathSegments();

        if (BuildConfig.DEBUG)
        {
            Log.d(TAG, "Uri: " + uri);
            Log.d(TAG, "Path: " + uriList);
        }

        List<String> segments =
            uriList.subList(1, uriList.size());

        // Try root path
        if (uriList.size() > 3)
        {
            StringBuilder path = new StringBuilder();
            for (String segment : segments)
            {
                path.append(File.separator);
                path.append(segment);
            }

            if (BuildConfig.DEBUG)
                Log.d(TAG, "Path: " + path);

            File file = new File(path.toString());
            if (file.isFile())
                return path.toString();
        }

        // Try external storage path
        if (uriList.size() > 1)
        {
            StringBuilder path = new StringBuilder();
            path.append(Environment.getExternalStorageDirectory());
            for (String segment : segments)
            {
                path.append(File.separator);
                path.append(segment);
            }

            if (BuildConfig.DEBUG)
                Log.d(TAG, "Path: " + path);

            File file = new File(path.toString());
            if (file.isFile())
                return path.toString();
        }

        return null;
    }

    /**
     * Get the display name for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the
     *                      query.
     * @return The display name of the file referred to by the Uri
     * @author Bill Farmer
     */
    public static String getDisplayName(Context context, Uri uri,
                                        String selection,
                                        String[] selectionArgs)
    {
        final String column = OpenableColumns.DISPLAY_NAME;
        final String[] projection =
            {
                column
            };

        try (Cursor cursor = context.getContentResolver()
             .query(uri, projection, selection, selectionArgs, null))
        {
            if (cursor != null && cursor.moveToFirst())
            {
                if (BuildConfig.DEBUG)
                    DatabaseUtils.dumpCursor(cursor);

                final int column_index = cursor.getColumnIndex(column);
                if (column_index >= 0)
                    return cursor.getString(column_index);
            }
        }

        catch (Exception e) {}

        return null;
    }

    /**
     * Get the size for this Uri. This is useful for MediaStore Uris,
     * and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the
     *                      query.
     * @return The size of the file referred to by the Uri
     * @author Bill Farmer
     */
    public static long getSize(Context context, Uri uri,
                              String selection,
                              String[] selectionArgs)
    {
        final String column = OpenableColumns.SIZE;
        final String[] projection =
            {
                column
            };

        try (Cursor cursor = context.getContentResolver()
             .query(uri, projection, selection, selectionArgs, null))
        {
            if (cursor != null && cursor.moveToFirst())
            {
                if (BuildConfig.DEBUG)
                    DatabaseUtils.dumpCursor(cursor);

                final int column_index = cursor.getColumnIndex(column);
                if (column_index >= 0)
                    return cursor.getLong(column_index);
            }
        }

        catch (Exception e) {}

        return 0;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the
     *                      query.
     * @return The value of the _data column, which is typically a
     * file path.
     * @author paulburke
     */
    public static String getDataColumn(Context context, Uri uri,
                                       String selection,
                                       String[] selectionArgs)
    {
        final String column = MediaStore.MediaColumns.DATA;
        final String[] projection =
        {
            column
        };

        try (Cursor cursor = context.getContentResolver()
             .query(uri, projection, selection, selectionArgs, null))
        {
            if (cursor != null && cursor.moveToFirst())
            {
                if (BuildConfig.DEBUG)
                    DatabaseUtils.dumpCursor(cursor);

                final int column_index = cursor.getColumnIndex(column);
                if (column_index >= 0)
                    return cursor.getString(column_index);
            }
        }

        catch (Exception e) {}

        return null;
    }

    /**
     * Get a file path from a Uri. This will get the the path for
     * Storage Access Framework Documents, as well as the _data field
     * for the MediaStore and other file-based ContentProviders.<br>
     * <br>
     * Callers should check whether the path is local before assuming
     * it represents a local file.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     * @see #isLocal(String)
     * @see #getFile(Context, Uri)
     */
    @TargetApi(19)
    public static String getPath(final Context context, final Uri uri)
    {

        if (BuildConfig.DEBUG)
            Log.d(TAG, "File: " +
                  "Authority: " + uri.getAuthority() +
                  ", Fragment: " + uri.getFragment() +
                  ", Port: " + uri.getPort() +
                  ", Query: " + uri.getQuery() +
                  ", Scheme: " + uri.getScheme() +
                  ", Host: " + uri.getHost() +
                  ", Segments: " + uri.getPathSegments().toString()
                 );

        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
            DocumentsContract.isDocumentUri(context, uri))
        {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri))
            {
                final List<String> segments = uri.getPathSegments();
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                final String id = split[1];

                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Segments: " + segments);

                if (BuildConfig.DEBUG)
                    Log.d(TAG, "DocId: " + docId);

                if ("primary".equalsIgnoreCase(type))
                {
                    return Environment.getExternalStorageDirectory() +
                        File.separator + id;
                }
                else if ("home".equalsIgnoreCase(type))
                {
                    return Environment .getExternalStorageDirectory() +
                        "/Documents/" + id;
                }
                else if (type != null && type.matches("[0-9A-Z]{4}-[0-9A-Z]{4}"))
                {
                    List<String> storage =
                        Uri.fromFile(Environment.getExternalStorageDirectory())
                        .getPathSegments();

                    return File.separator + storage.get(0) + File.separator +
                        type + File.separator + id;
                }
                else if ("document".equalsIgnoreCase(segments.get(0)))
                {
                    return Environment .getExternalStorageDirectory() +
                        File.separator + id;
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri))
            {
                // Check for non-numeric id
                try
                {
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri =
                        ContentUris
                        .withAppendedId(Uri.parse("content://downloads/public_downloads"),
                                        Long.valueOf(id));

                    return getDataColumn(context, contentUri, null, null);
                }


                // Id not a number
                catch (Exception e)
                {
                    return getDataColumn(context, uri, null, null);
                }
            }
            // MediaProvider
            else if (isMediaDocument(uri))
            {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                final String id = split[1];

                Uri contentUri = null;
                if ("image".equals(type))
                {
                    contentUri =
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }
                else if ("video".equals(type))
                {
                    contentUri =
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                }
                else if ("audio".equals(type))
                {
                    contentUri =
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]
                {
                    id
                };

                return getDataColumn(context, contentUri,
                                     selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme()))
        {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            // Return ContentProvider path
            String path = getDataColumn(context, uri, null, null);
            if (path != null)
                return path;

            // Return FileProvider path
            return fileProviderPath(uri);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme()))
        {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Convert Uri into File, if possible.
     *
     * @return file A local file that the Uri was pointing to, or null
     * if the Uri is unsupported or pointed to a remote
     * resource.
     * @author paulburke
     * @see #getPath(Context, Uri)
     */
    public static File getFile(Context context, Uri uri)
    {
        if (uri != null)
        {
            String path = getPath(context, uri);
            if (isLocal(path))
            {
                return new File(path);
            }
        }
        return null;
    }

    /**
     * Get the file size in a human-readable string.
     *
     * @param size
     * @return
     * @author paulburke
     */
    public static String getReadableFileSize(long size)
    {
        final int BYTES_IN_KILOBYTES = 1024;
        final DecimalFormat dec = new DecimalFormat("###.#");
        final String KILOBYTES = " KB";
        final String MEGABYTES = " MB";
        final String GIGABYTES = " GB";
        float fileSize = 0;
        String suffix = KILOBYTES;

        if (size > BYTES_IN_KILOBYTES)
        {
            fileSize = size / BYTES_IN_KILOBYTES;
            if (fileSize > BYTES_IN_KILOBYTES)
            {
                fileSize = fileSize / BYTES_IN_KILOBYTES;
                if (fileSize > BYTES_IN_KILOBYTES)
                {
                    fileSize = fileSize / BYTES_IN_KILOBYTES;
                    suffix = GIGABYTES;
                }
                else
                {
                    suffix = MEGABYTES;
                }
            }
        }
        return String.valueOf(dec.format(fileSize) + suffix);
    }

    /**
     * Attempt to retrieve the thumbnail of given File from the
     * MediaStore. This should not be called on the UI thread.
     *
     * @param context
     * @param file
     * @return
     * @author paulburke
     */
    public static Bitmap getThumbnail(Context context, File file)
    {
        return getThumbnail(context, getUri(file), getMimeType(file));
    }

    /**
     * Attempt to retrieve the thumbnail of given Uri from the
     * MediaStore. This should not be called on the UI thread.
     *
     * @param context
     * @param uri
     * @return
     * @author paulburke
     */
    public static Bitmap getThumbnail(Context context, Uri uri)
    {
        return getThumbnail(context, uri, getMimeType(context, uri));
    }

    /**
     * Attempt to retrieve the thumbnail of given Uri from the
     * MediaStore. This should not be called on the UI thread.
     *
     * @param context
     * @param uri
     * @param mimeType
     * @return
     * @author paulburke
     */
    public static Bitmap getThumbnail(Context context, Uri uri,
                                      String mimeType)
    {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Attempting to get thumbnail");

        if (!isMediaUri(uri))
        {
            Log.e(TAG,
                  "You can only retrieve thumbnails for images and videos.");
            return null;
        }

        Bitmap bm = null;
        final ContentResolver resolver = context.getContentResolver();
        try (Cursor cursor = resolver.query(uri, null, null, null, null))
        {
            if (cursor.moveToFirst())
            {
                final int id = cursor.getInt(0);
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Got thumb ID: " + id);

                if (mimeType.contains("video"))
                {
                    bm = MediaStore.Video.Thumbnails.getThumbnail(
                             resolver,
                             id,
                             MediaStore.Video.Thumbnails.MINI_KIND,
                             null);
                }
                else if (mimeType.contains(FileUtils.MIME_TYPE_IMAGE))
                {
                    bm = MediaStore.Images.Thumbnails.getThumbnail(
                             resolver,
                             id,
                             MediaStore.Images.Thumbnails.MINI_KIND,
                             null);
                }
            }
        }
        catch (Exception e)
        {
            if (BuildConfig.DEBUG)
                Log.e(TAG, "getThumbnail", e);
        }
        return bm;
    }

    /**
     * Get the Intent for selecting content to be used in an Intent Chooser.
     *
     * @return The intent for opening a file with Intent.createChooser()
     * @author paulburke
     */
    public static Intent createGetContentIntent()
    {
        // Implicitly allow the user to select a particular kind of data
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // The MIME data type filter
        intent.setType("*/*");
        // Only return URIs that can be opened with ContentResolver
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        return intent;
    }
}
