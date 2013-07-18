package com.rileybrewer.instantgraham;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.rileybrewer.instantgraham.Utils.TypeFaceSpan;
import com.rileybrewer.instantgraham.Utils.UIUtils;

import java.io.File;

/**
 * Created by krylez on 7/17/13.
 */
public class ViewActivity extends Activity implements View.OnClickListener {
    private static final String TAG = ViewActivity.class.getSimpleName();

    private Uri mImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImageUri = getIntent().getParcelableExtra(UIUtils.IMAGE_PATH);
        Log.d(TAG, "uri:" + mImageUri);

        setContentView(R.layout.activity_view);
        ImageView iv = (ImageView) findViewById(R.id.image);

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

        findViewById(R.id.share).setOnClickListener(this);
        findViewById(R.id.delete).setOnClickListener(this);

        ActionBar aBar = getActionBar();
        if (aBar != null) {
            aBar.setIcon(R.drawable.ic_action_home);
            aBar.setDisplayHomeAsUpEnabled(true);
            SpannableString s = new SpannableString(getString(R.string.app_name));
            s.setSpan(new TypeFaceSpan(this, "RobotoSlab-Regular"), 0, s.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            aBar.setTitle(s);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home: {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            }
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.share: {
                startActivity(createShareIntent(mImageUri));
                break;
            }
            case R.id.delete: {
                File file = new File(mImageUri.getPath());
                file.delete();
                finish();
                break;
            }
        }
    }
    private Intent createShareIntent(Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("image/*");

        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        return shareIntent;
    }
}
