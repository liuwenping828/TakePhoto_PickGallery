package com.svchina.android7photo;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.svchina.android7photo.util.BitmapUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final int RC_CAMERA_AND_SDCARD = 0x110;
    private static final int REQUEST_TAKE_PHOTO = 0x120;
    private static final int REQUEST_CROP_PHOTO = 0x130;
    private static final String TAG = "python";
    private static final int RC_EXTERNAL_STORAGE = 0x140;
    private static final int REQUEST_GALLERY = 0x150;
    ImageView mImageView;
    private String mCurrentPhotoPath;
    private String mCropPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = findViewById(R.id.img);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    public void takePhoto(View view) {
        String[] pers = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, pers)) {
            dispatchTakePictureIntent();
        } else {
            EasyPermissions.requestPermissions(this, "this app is need permission", RC_CAMERA_AND_SDCARD, pers);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.svchina.android7photo.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
//            galleryAddPic();
//            setPic();
            cropImage();
        } else if (requestCode == REQUEST_CROP_PHOTO && resultCode == RESULT_OK) {
            setPic(mCropPhotoPath);
        } else if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String path = BitmapUtils.getPath(this, uri);
            Log.d(TAG, "gallery path:" + path);
            this.mCurrentPhotoPath = path;
            cropImage();
        }
    }

    private void cropImage() {
        File cropFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), System.currentTimeMillis() + ".jpg");
        mCropPhotoPath = cropFile.getPath();
        Log.d("python", "crop path:" + mCropPhotoPath);
        // todo 输出Uri 只能使用  Uri.fromFile
        Uri outputUri = Uri.fromFile(cropFile);

        File file = new File(mCurrentPhotoPath);
        Uri imageUri = FileProvider.getUriForFile(this, "com.svchina.android7photo.fileprovider", file);
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(imageUri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        // 裁剪后图片大小
        intent.putExtra("outputX", 350);
        intent.putExtra("outputY", 350);
        // 是将结果保存在data中返回
        intent.putExtra("return-data", false); //  http://blog.csdn.net/augusaccount/article/details/54890169
        intent.putExtra("noFaceDetection", true); // no face detection
        startActivityForResult(intent, REQUEST_CROP_PHOTO);
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic(String imagePath) {
        // Get the dimensions of the View
        int targetW = 500;
        int targetH = 500;

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        Log.d(TAG, "src Image Height:" + photoH);
        Log.d(TAG, "src Image Width:" + photoW);

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, bmOptions);
        mImageView.setImageBitmap(bitmap);
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == RC_CAMERA_AND_SDCARD) {
            dispatchTakePictureIntent();
        } else if (requestCode == RC_EXTERNAL_STORAGE) {
            pickGallery();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    public void pick(View view) {
        String pers = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (EasyPermissions.hasPermissions(this, pers)) {
            pickGallery();
        } else {
            EasyPermissions.requestPermissions(this, "sdcard permission", RC_EXTERNAL_STORAGE, pers);
        }
    }

    private void pickGallery() {
//        Intent intent = new Intent(Intent.ACTION_PICK);
        Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
        startActivityForResult(pickIntent, REQUEST_GALLERY);
    }
}
