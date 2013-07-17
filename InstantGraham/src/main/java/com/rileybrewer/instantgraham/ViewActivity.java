package com.rileybrewer.instantgraham;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;

import com.rileybrewer.instantgraham.Utils.UIUtils;

/**
 * Created by krylez on 7/17/13.
 */
public class ViewActivity extends Activity implements View.OnClickListener {
    private static final String TAG = ViewActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        ImageView iv = (ImageView) findViewById(R.id.image);
        ImageView mask = (ImageView) findViewById(R.id.mask);

        Bitmap bitmap = UIUtils.getBitmap();

        iv.setImageBitmap(bitmap);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        if (width > bitmap.getWidth() || height > bitmap.getHeight()) {
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Log.d(TAG, "FIT_CENTER");
        } else {
            iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            Log.d(TAG, "CENTER_INSIDE");
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.share: {
                // TODO start the share intent
                break;
            }
            case R.id.delete: {
                // TODO delete the file!
                break;
            }
        }
    }
}
