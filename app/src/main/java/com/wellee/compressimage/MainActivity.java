package com.wellee.compressimage;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.os.EnvironmentCompat;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    //用于保存拍照图片的uri
    private Uri mCameraUri;
    private File photoFile = null;

    // 用于保存图片的文件路径，Android 10以下使用图片路径访问图片
    private String mCameraImagePath;

    // 是否是Android 10以上手机
    private boolean isAndroidQ = Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;

    private ImageView ivPhoto;

    // 拍照的requestCode
    private static final int CAMERA_REQUEST_CODE = 0x00000010;

    private String compressPath = Environment.getExternalStorageDirectory() + File.separator + "compress";

    private String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int i = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int j = ContextCompat.checkSelfPermission(this, Manifest.permission_group.STORAGE);
        if (i != PackageManager.PERMISSION_GRANTED && j != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 1000);
        }
        ivPhoto = findViewById(R.id.ivPhoto);
    }

    public void takePhoto(View view) {
        openCamera();
    }

    public void compress(View view) {
//        if (isAndroidQ) {
//            Bitmap bitmap = get(mCameraUri);
//            if (bitmap == null) {
//                Toast.makeText(this, "未获取到图片", Toast.LENGTH_SHORT).show();
//                return;
//            }
////            showBitmapInfo(bitmap);
//            File file = new File(compressPath);
//            if (!file.exists()) {
//                file.mkdirs();
//            }
//            String fileName = compressPath + File.separator + System.currentTimeMillis() + ".jpg";
//            ImageUtil.compressImage(bitmap, 75, fileName);
//            File compressBitmapFile = new File(fileName);
//            Log.e("compress", "compressBitmapFile = " + compressBitmapFile.length());
//        } else {
            Bitmap bitmap = ImageUtil.decodeFile(mCameraImagePath);
            File bitmapFile = new File(mCameraImagePath);
            Log.e("compress", "bitmapFileSize = " + bitmapFile.length());
            File file = new File(compressPath);
            if (!file.exists()) {
                file.mkdirs();
            }
            String fileName = compressPath + File.separator + new File(mCameraImagePath).getName();
            ImageUtil.compressImage(bitmap, 75, fileName);
            File compressBitmapFile = new File(fileName);
            Log.e("compress", "compressBitmapFileSize = " + compressBitmapFile.length());
//        }
    }
    /**
     * 调起相机拍照
     */
    private void openCamera() {
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 判断是否有相机
        if (captureIntent.resolveActivity(getPackageManager()) != null) {
            Uri photoUri = null;

//            if (isAndroidQ) {
//                // 适配android 10
//                photoUri = createImageUri();
//            } else {
                try {
                    photoFile = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (photoFile != null) {
                    mCameraImagePath = photoFile.getAbsolutePath();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        //适配Android 7.0文件权限，通过FileProvider创建一个content类型的Uri
                        photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                    } else {
                        photoUri = Uri.fromFile(photoFile);
                    }
                }
//            }

            mCameraUri = photoUri;
            if (photoUri != null) {
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(captureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    /**
     * 创建图片地址uri,用于保存拍照后的照片 Android 10以后使用这种方法
     */
    private Uri createImageUri() {
        String status = Environment.getExternalStorageState();
        // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        } else {
            return getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, new ContentValues());
        }
    }

    /**
     * 创建保存图片的文件
     */
    private File createImageFile() throws IOException {
        String imageName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
        File tempFile = new File(storageDir, imageName + ".jpg");
        if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(tempFile))) {
            return null;
        }
        return tempFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (isAndroidQ) {
                    // Android 10 使用图片uri加载
                    ivPhoto.setImageURI(mCameraUri);
                } else {
                    // 使用图片路径加载
                    ivPhoto.setImageBitmap(BitmapFactory.decodeFile(mCameraImagePath));
                }
            } else {
                Toast.makeText(this, "取消", Toast.LENGTH_LONG).show();
            }
        }
    }

    private Bitmap get(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        FileDescriptor fileDescriptor = null;
        Bitmap tagBitmap = null;
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null && parcelFileDescriptor.getFileDescriptor() != null) {
                fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                //转换uri为bitmap类型
                tagBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                return tagBitmap;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (parcelFileDescriptor != null) {
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
