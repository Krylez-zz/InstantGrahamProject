package com.rileybrewer.instantgraham;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.rileybrewer.instantgraham.Utils.UIUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HomeActivity extends Activity implements View.OnClickListener {
    private static final String TAG = HomeActivity.class.getSimpleName();

    private static final int IMAGE_SIZE = 640;

    private static final int TAKE_PHOTO = 1000;
    private static final int SELECT_PHOTO = 1001;

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    private static final SimpleDateFormat SDF =  new SimpleDateFormat("yyyyMMdd_HHmmss");

    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        findViewById(R.id.import_photo_btn).setOnClickListener(this);
        findViewById(R.id.take_photo_btn).setOnClickListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.import_photo_btn: {
                dispatchChoosePictureIntent();
                break;
            }
            case R.id.take_photo_btn: {
                dispatchTakePictureIntent();
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            switch(requestCode) {
                case TAKE_PHOTO: {
                    handleTakePhoto();
                    break;
                }
                case SELECT_PHOTO: {
                    handleSelectPhoto(data.getData());
                    break;
                }
            }
        }
    }

    private void dispatchChoosePictureIntent() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File f;

        try {
            f = setUpPhotoFile();
            mCurrentPhotoPath = f.getAbsolutePath();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        } catch (IOException e) {
            e.printStackTrace();
            mCurrentPhotoPath = null;
        }
        startActivityForResult(takePictureIntent, TAKE_PHOTO);
    }

    private void handleSelectPhoto(Uri uri) {
        setPic(uri);
    }

    private void handleTakePhoto() {
        if (mCurrentPhotoPath != null) {
            setPic(null);
            galleryAddPic();
            mCurrentPhotoPath = null;
        }
    }

    private void setPic(Uri uri) {

		/* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        if (uri == null)
            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        else {
            InputStream in;
            try {
                in = getContentResolver().openInputStream(uri);
            } catch (Exception e) {
                Log.e(TAG, "error decoding file", e);
                return;
            }
            BitmapFactory.decodeStream(in, null, bmOptions);
        }

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        scaleFactor = Math.min(photoW/IMAGE_SIZE, photoH/IMAGE_SIZE);
        Log.d(TAG, "scale factor=" + scaleFactor);

		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
        Bitmap bitmap;
        if (uri == null)
            bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        else {
            InputStream in;
            try {
                in = getContentResolver().openInputStream(uri);
            } catch (Exception e) {
                Log.e(TAG, "error decoding file", e);
                return;
            }
            bitmap = BitmapFactory.decodeStream(in, null, bmOptions);
        }

        int width, height;
        if (bitmap.getHeight() > bitmap.getWidth()) {
            width = IMAGE_SIZE;
            height = (int) (IMAGE_SIZE * bitmap.getHeight() / ((float) bitmap.getWidth()));
        } else {
            width = (int) (IMAGE_SIZE * bitmap.getWidth() / ((float) bitmap.getHeight()));
            height = IMAGE_SIZE;
        }
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        int x, y;
        if (bitmap.getHeight() > bitmap.getWidth()) {
            x = 0;
            y = (int) ((bitmap.getHeight() - bitmap.getWidth()) / 2f);
        } else {
            x = (int) ((bitmap.getWidth() - bitmap.getHeight()) / 2f);
            y = 0;
        }
        bitmap = Bitmap.createBitmap(bitmap, x, y, IMAGE_SIZE, IMAGE_SIZE);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bitmap);
        Bitmap mask = BitmapFactory.decodeResource(getResources(),
                R.drawable.mask);
        canvas.drawBitmap(mask, 0, 0, null);
        Log.d(TAG, "w=" + bitmap.getWidth() + " h=" + bitmap.getHeight());
        UIUtils.setBitmap(bitmap);
        Intent intent = new Intent(this, ViewActivity.class);
        startActivity(intent);
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private File getAlbumStorageDir(String albumName) {
        return new File(
            Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), albumName);
    }

    private String getAlbumName() {
        return getString(R.string.album_name);
    }

    private File getAlbumDir() {
        File storageDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            storageDir = getAlbumStorageDir(getAlbumName());
            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        Log.d(TAG, "failed to create directory: " + storageDir.toString());
                        return null;
                    }
                }
            }
        } else {
            Log.v(TAG, "External storage is not mounted READ/WRITE.");
        }
        return storageDir;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = SDF.format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File image = File.createTempFile(
            imageFileName,
            JPEG_FILE_SUFFIX,
            getAlbumDir());
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private File setUpPhotoFile() throws IOException {
        File f = createImageFile();
        mCurrentPhotoPath = f.getAbsolutePath();
        return f;
    }
}
