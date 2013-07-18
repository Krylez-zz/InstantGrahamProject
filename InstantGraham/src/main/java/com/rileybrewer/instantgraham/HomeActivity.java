package com.rileybrewer.instantgraham;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.rileybrewer.iab.IabHelper;
import com.rileybrewer.iab.IabResult;
import com.rileybrewer.iab.Inventory;
import com.rileybrewer.iab.Purchase;
import com.rileybrewer.instantgraham.Utils.TypeFaceSpan;
import com.rileybrewer.instantgraham.Utils.UIUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HomeActivity extends Activity implements View.OnClickListener {
    private static final String TAG = HomeActivity.class.getSimpleName();

    private static final int IMAGE_SIZE = 960;

    private static final int CORNER_RADIUS = 50;
    private static final int MARGIN = 25;

    private static final int TAKE_PHOTO = 1000;
    private static final int SELECT_PHOTO = 1001;
    private static final int RC_REQUEST = 1002;

    private static final String RAW_IMAGE_PREFIX = "RAW_";
    private static final String FINAL_IMAGE_PREFIX = "FIN_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    private static final SimpleDateFormat SDF =  new SimpleDateFormat("yyyyMMdd_HHmmss");

    private boolean mIsPremium = false;

    private static final String SKU_PREMIUM = "premium";

    private String mCurrentPhotoPath;

    IabHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        findViewById(R.id.import_photo_btn).setOnClickListener(this);
        findViewById(R.id.take_photo_btn).setOnClickListener(this);

        ActionBar aBar = getActionBar();
        if (aBar != null) {
            aBar.setIcon(R.drawable.ic_action_home);
            SpannableString s = new SpannableString(getString(R.string.app_name));
            s.setSpan(new TypeFaceSpan(this, "RobotoSlab-Regular"), 0, s.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            aBar.setTitle(s);

        }

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                savePic(imageUri);
            }
        }

        String base64EncodedPublicKey = "CONSTRUCT_YOUR_KEY_AND_PLACE_IT_HERE";

        mHelper = new IabHelper(this, base64EncodedPublicKey);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    return;
                }
                Log.d(TAG, "Setup successful. Querying inventory.");
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mHelper != null) mHelper.dispose();
        mHelper = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_purchase: {

                mHelper.launchPurchaseFlow(this, SKU_PREMIUM, RC_REQUEST,
                        mPurchaseFinishedListener, "");
                return true;
            }
        }
        return false;
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
        savePic(uri);
    }

    private void handleTakePhoto() {
        if (mCurrentPhotoPath != null) {
            savePic(null);
//            galleryAddPic(mCurrentPhotoPath);
        }
    }

    private void savePic(Uri uri) {
        Log.d (TAG, "savePic: " + uri);
        new AsyncTask<Uri, Void, Bitmap>() {

            private ProgressDialog pd;
            private Uri imageUri;

            @Override
            protected Bitmap doInBackground(Uri... uris) {
                Uri uri = null;
                if (uris != null) {
                    uri = uris[0];
                }
                Bitmap bitmap = setPic(uri);
                imageUri = saveImage(bitmap);
                Log.d(TAG, "imageUri" + imageUri);
                return bitmap;
            }

            @Override
            protected void onPreExecute() {
                pd = new ProgressDialog(HomeActivity.this);
                pd.setTitle("Processing...");
                pd.setMessage("Please wait.");
                pd.setCancelable(false);
                pd.setIndeterminate(true);
                pd.show();
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                pd.dismiss();
                UIUtils.setBitmap(bitmap);
//                File file = new File(mCurrentPhotoPath);
//                file.delete();
                mCurrentPhotoPath = null;
                Intent intent = new Intent(HomeActivity.this, ViewActivity.class);
                intent.putExtra(UIUtils.IMAGE_PATH, imageUri);
                startActivity(intent);
            }
        }.execute(uri);
    }
    private Bitmap setPic(Uri uri) {

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
                return null;
            }
            BitmapFactory.decodeStream(in, null, bmOptions);
        }

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
        int scaleFactor = Math.min(photoW/IMAGE_SIZE, photoH/IMAGE_SIZE);

		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
        Bitmap bitmap;
        if (uri == null) {
            bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

            Log.d(TAG, "mCurrentPhotoPath=" + mCurrentPhotoPath);
            File file  = new File(mCurrentPhotoPath);
            file.delete();
        } else {
            InputStream in;
            try {
                in = getContentResolver().openInputStream(uri);
            } catch (Exception e) {
                Log.e(TAG, "error decoding file", e);
                return null;
            }
            bitmap = BitmapFactory.decodeStream(in, null, bmOptions);
        }

        bitmap = drawBorder(bitmap);
        Log.d(TAG, "w=" + bitmap.getWidth() + " h=" + bitmap.getHeight());
        return bitmap;
    }
    private Uri saveImage(Bitmap bitmap) {
        File file;
        try {
            file = createImageFile(FINAL_IMAGE_PREFIX);
            OutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            galleryAddPic(file.getPath());
            return Uri.fromFile(file);
        } catch (Exception e) {
            Log.e(TAG, "error creating file", e);
            return null;
        }
    }
    private Bitmap drawBorder(Bitmap bitmap) {
        Bitmap toReturn = Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(toReturn);

        canvas.drawColor(Color.WHITE);

        Matrix matrix = new Matrix();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        if (bitmap.getWidth() > bitmap.getHeight()) {
            int diff = bitmap.getWidth() - bitmap.getHeight();
            float scale = 1f * IMAGE_SIZE / bitmap.getHeight();
            matrix.postTranslate(-diff / 2, 0);
            matrix.postScale(scale, scale);
        } else {
            int diff = bitmap.getHeight() - bitmap.getWidth();
            float scale = 1f * IMAGE_SIZE / bitmap.getWidth();
            matrix.postTranslate(0, -diff / 2);
            matrix.postScale(scale, scale);
        }

        BitmapShader shader = new BitmapShader(bitmap,
                Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        shader.setLocalMatrix(matrix);

        Bitmap mask = BitmapFactory.decodeResource(getResources(),
                R.drawable.mask);
        Shader shader2 = new BitmapShader(mask,
                Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        RectF mRect = new RectF();
        mRect.set(MARGIN, MARGIN, IMAGE_SIZE - MARGIN, IMAGE_SIZE - MARGIN);
        paint.setShader(shader);
        RadialGradient shader4 = new RadialGradient(
                mRect.centerX(), mRect.centerY() * 1.0f / 0.7f, mRect.centerX() * 1.3f,
                new int[] { 0, 0, 0x7f000000 }, new float[] { 0.0f, 0.7f, 1.0f },
                Shader.TileMode.CLAMP);

        Matrix oval = new Matrix();
        oval.setScale(1.0f, 0.7f);
        shader4.setLocalMatrix(oval);

        paint.setShader(new ComposeShader(
                new ComposeShader(shader, shader2, PorterDuff.Mode.SRC_OVER),
                shader4,
                PorterDuff.Mode.SRC_OVER));

        canvas.drawRoundRect(mRect, CORNER_RADIUS, CORNER_RADIUS, paint);
        return toReturn;
    }

    private void galleryAddPic(String path) {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(path);
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

    private File createImageFile(String prefix) throws IOException {
        // Create an image file name
        String timeStamp = SDF.format(new Date());
        String imageFileName = prefix + timeStamp + "_";
        File image = File.createTempFile(
            imageFileName,
            JPEG_FILE_SUFFIX,
            getAlbumDir());
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private File setUpPhotoFile() throws IOException {
        File f = createImageFile(RAW_IMAGE_PREFIX);
        mCurrentPhotoPath = f.getAbsolutePath();
        Log.d(TAG, "mCurrentPhotoPath" + mCurrentPhotoPath);
        return f;
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");
            if (result.isFailure()) {
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
            mIsPremium = premiumPurchase != null;
            Log.d(TAG, "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));

            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            if (result.isFailure()) {
                return;
            }

            Log.d(TAG, "Purchase successful.");
            if (purchase.getSku().equals(SKU_PREMIUM)) {
                mIsPremium = true;
                // TODO thank the user
            }
        }
    };
}
