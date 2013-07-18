package com.rileybrewer.instantgraham.Utils;

import android.graphics.Bitmap;

/**
 * Created by krylez on 7/17/13.
 */
public class UIUtils {

    public static final String IMAGE_PATH = "image-path";

    public static final int IMAGE_SIZE = 640;

    private static Bitmap sBitmap;

    public static Bitmap getBitmap() {
        return sBitmap;
    }

    public static void setBitmap(Bitmap bitmap) {
        sBitmap = bitmap;
    }
}
