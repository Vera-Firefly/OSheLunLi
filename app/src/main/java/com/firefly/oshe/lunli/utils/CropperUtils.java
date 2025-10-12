package com.firefly.oshe.lunli.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CropperUtils {
    public interface CropperResultListener {
        void onCropSuccess(Bitmap croppedBitmap, String base64String);
        void onCropFailure(String errorMessage);
    }

    public static void startImagePicker(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        activity.startActivityForResult(intent, requestCode);
    }

    public static Uri startImageCrop(Activity activity, Uri sourceUri, int requestCode) throws IOException {
        try {
            File tempFile = createTempFile(activity);
            Uri outputUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", tempFile);

            int sizeInPx = dpToPx(36, activity);

            Intent cropIntent = createCropIntent(sourceUri, outputUri, sizeInPx);

            if (cropIntent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivityForResult(cropIntent, requestCode);
                return outputUri;
            } else {

            }
            return null;
        } catch (Exception e) {
            Toast.makeText(activity, "Creat file Failed", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public static void handleCropResult(Activity activity, Uri croppedImageUri, CropperResultListener listener) {
        if (croppedImageUri == null) {
            if (listener != null) {
                listener.onCropFailure("Null Image Uri");
            }
            return;
        }

        try {
            Bitmap croppedBitmap = BitmapFactory.decodeStream(activity.getContentResolver().openInputStream(croppedImageUri));

            if (croppedBitmap != null) {
                String base64String = bitmapToBase64(croppedBitmap);

                if (listener != null)
                    listener.onCropSuccess(croppedBitmap, base64String);

                deleteTempfile(croppedImageUri);
            } else {
                if (listener != null) {
                    listener.onCropFailure("Can not Pick Image To Base64");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onCropFailure("Fix Image Fail: " + e.getMessage());
            }
        }
    }

    public static void performCustomCrop(Activity activity, Uri sourceUri, Uri outputUri, int sizeInPx, CropperResultListener listener) {
        new Thread(() -> {
            try {
                Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), sourceUri);

                Bitmap croppedBitmap = cropAndScaleBitmap(originalBitmap, sizeInPx);

                saveBitmapToUri(activity, croppedBitmap, outputUri);

                activity.runOnUiThread(() -> {
                    if (listener != null) {
                        String base64String = bitmapToBase64(croppedBitmap);
                        listener.onCropSuccess(croppedBitmap, base64String);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> {
                    if (listener != null)
                        listener.onCropFailure("Custom Pick Failed: " + e.getMessage());
                });
            }
        });
    }

    private static Bitmap cropAndScaleBitmap(Bitmap originalBitmap, int sizeInPx) {
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();
        int cropSize = Math.min(originalWidth, originalHeight);

        int x = (originalWidth - cropSize) / 2;
        int y = (originalHeight - cropSize) / 2;

        Bitmap croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, cropSize, cropSize);
        return Bitmap.createScaledBitmap(croppedBitmap, sizeInPx, sizeInPx, true);
    }

    private static Intent createCropIntent(Uri sourceUri, Uri outputUri, int sizeInPx) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(sourceUri, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", sizeInPx);
        intent.putExtra("outputY", sizeInPx);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

        return intent;
    }

    private static int dpToPx(int i, Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(i * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private static File createTempFile(Context context) throws IOException {
        return File.createTempFile("cropped_image_" + System.currentTimeMillis(), ".jpg", context.getCacheDir());
    }

    private static void deleteTempfile(Uri fileUri) {
        if (fileUri != null && "file".equals(fileUri.getScheme())) {
            new File(fileUri.getPath()).delete();
        }
    }

    public static void saveBitmapToUri(Context context, Bitmap bitmap, Uri outputUri) throws IOException {
        try (FileOutputStream fos = (FileOutputStream) context.getContentResolver().openOutputStream(outputUri)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        }
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public static Bitmap base64ToBitmap(String base64String) {
        byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    public static boolean isSystemCropAvailable(Context context) {
        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        cropIntent.setType("image/*");

        return cropIntent.resolveActivity(context.getPackageManager()) != null;
    }
}
