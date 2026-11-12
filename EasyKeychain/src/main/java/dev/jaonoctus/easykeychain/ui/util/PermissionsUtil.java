/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.jaonoctus.easykeychain.ui.util;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import android.os.Build.VERSION_CODES;
import android.widget.Toast;

import dev.jaonoctus.easykeychain.R;

public class PermissionsUtil {

    private static final int PERMISSION_READ_EXTERNAL_STORAGE = 1;
    public static final String[] READ_PERMISSIONS = { permission.READ_EXTERNAL_STORAGE };

    /**
     * Request READ_EXTERNAL_STORAGE permission on Android >= 6.0 to read content from "file" Uris.
     * <p/>
     * This method returns true on Android < 6, or if permission is already granted. It
     * requests the permission and returns false otherwise.
     * <p/>
     * see https://commonsware.com/blog/2015/10/07/runtime-permissions-files-action-send.html
     */
    @SuppressLint("NewApi") // Api level is checked in checkReadPermission
    public static boolean checkAndRequestReadPermission(Activity activity, Uri uri) {
        boolean result = checkReadPermission(activity, uri);
        if (!result) {
            requestReadPermission(activity, null);
        }
        return result;
    }

    public static boolean checkAndRequestReadPermission(Fragment fragment, Uri uri) {
        boolean result = checkReadPermission(fragment.getContext(), uri);
        if (!result) {
            requestReadPermission(null, fragment);
        }
        return result;
    }

    /**
     * From Android 13 on, READ_EXTERNAL_STORAGE is gone and nothing replaces it for our purpose:
     * the READ_MEDIA_* permissions only cover photos, videos and audio, not key or message files.
     * A "file" Uri can not be read there, so say so instead of asking for a permission that
     * would not help. Files picked through the document picker are not affected.
     */
    private static void requestReadPermission(Activity activity, Fragment fragment) {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            Context context = activity != null ? activity : fragment.getContext();
            Toast.makeText(context, R.string.error_denied_storage_permission, Toast.LENGTH_LONG).show();
        } else if (activity != null) {
            activity.requestPermissions(READ_PERMISSIONS, PERMISSION_READ_EXTERNAL_STORAGE);
        } else {
            fragment.requestPermissions(READ_PERMISSIONS, PERMISSION_READ_EXTERNAL_STORAGE);
        }
    }

    public static boolean checkReadPermission(Context context, Uri uri) {
        if (!ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return true;
        }

        // Additional check due to:
        // https://commonsware.com/blog/2015/11/09/you-cannot-hold-nonexistent-permissions.html
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        return false;
    }

    public static boolean checkReadPermissionResult(Context context,
                                                    int requestCode,
                                                    int[] grantResults) {

        if (requestCode != PERMISSION_READ_EXTERNAL_STORAGE) {
            return false;
        }

        boolean permissionWasGranted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (permissionWasGranted) {
            return true;
        } else {
            Toast.makeText(context, R.string.error_denied_storage_permission, Toast.LENGTH_LONG)
                    .show();

            return false;
        }
    }

}
