package com.example.uploadimagecloud;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView; // Import CardView

import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import es.dmoral.toasty.Toasty;

import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private ImageView imageView;
    private Button buttonSelect, buttonUpload, buttonLoadImageUrl, buttonCloseImage;
    private EditText editTextImageUrl;
    private Bitmap bitmap;
    private CardView imageCardView; // Declare CardView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.image_view);
        buttonSelect = findViewById(R.id.button_select_image);
        buttonUpload = findViewById(R.id.button_upload_image);
        buttonLoadImageUrl = findViewById(R.id.button_load_image_url);
        editTextImageUrl = findViewById(R.id.edit_text_image_url);
        imageCardView = findViewById(R.id.image_card_view); // Initialize CardView
        buttonCloseImage = findViewById(R.id.button_close_image);

        buttonSelect.setOnClickListener(v -> openFileChooser());
        buttonUpload.setOnClickListener(v -> {
            // Tránh gọi uploadFile nhiều lần
            buttonUpload.setEnabled(false); // Vô hiệu hóa nút sau khi nhấn để tránh gọi lại
            uploadFile();
        });
        buttonLoadImageUrl.setOnClickListener(v -> loadFromUrl());
        buttonCloseImage.setOnClickListener(v -> resetInputAndHideCardView());
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
            imageCardView.setVisibility(View.VISIBLE); // Show CardView when image is selected
            buttonUpload.setVisibility(View.VISIBLE);
        }
    }

    //    private void loadFromUrl() {
//        String url = editTextImageUrl.getText().toString().trim();
//        if (!url.isEmpty() && isValidImageUrl(url)) {
//            runOnUiThread(() -> {
//                imageCardView.setVisibility(View.VISIBLE); // Show CardView before loading image
//                buttonUpload.setVisibility(View.VISIBLE);
//                Glide.with(MainActivity.this)
//                        .load(url)
//                        .into(imageView);
//            });
//        } else {
//            Toast.makeText(this, "Please enter a valid image URL", Toast.LENGTH_SHORT).show();
//        }
//    }
    private void loadFromUrl() {
        String url = editTextImageUrl.getText().toString().trim();
        if (!url.isEmpty()) {
            Glide.with(this)
                    .asBitmap() // Sử dụng asBitmap để tải ảnh dưới dạng Bitmap
                    .load(url)
                    .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            imageView.setImageBitmap(resource);
                            imageCardView.setVisibility(View.VISIBLE); // Show CardView when image is loaded from URL
                            buttonUpload.setVisibility(View.VISIBLE);
                            bitmap = resource; // Gán giá trị cho biến bitmap
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Xử lý khi ảnh bị loại bỏ
                        }
                    });
        } else {
            Toasty.warning(this, "Please enter a valid image URL", Toast.LENGTH_SHORT, true).show();
        }
    }


    private void uploadFile() {
        if (imageUri != null) {
            Log.d("MainActivity", "Uploading image from URI: " + imageUri.toString());
            uploadImageFromUri(imageUri);
        } else if (bitmap != null) {
            Log.d("MainActivity", "Uploading image from Bitmap");
            uploadImageFromBitmap(bitmap);
        } else {
            Toasty.error(this, "Không có ảnh nào được chọn", Toast.LENGTH_SHORT, true).show();
            buttonUpload.setEnabled(true); // Cho phép nhấn lại nếu không có ảnh
        }
    }


    private void uploadImageFromUri(Uri uri) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference().child("uploads/" + System.currentTimeMillis() + ".jpg");

        storageReference.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    Toasty.success(MainActivity.this, "Upload thành công", Toast.LENGTH_SHORT, true).show();
                    resetInputAndHideCardView();
                    buttonUpload.setEnabled(true); // Kích hoạt lại nút sau khi upload
                })
                .addOnFailureListener(e -> {
                    Toasty.error(MainActivity.this, "Upload thất bại: " + e.getMessage(), Toast.LENGTH_SHORT, true).show();
                    buttonUpload.setEnabled(true); // Kích hoạt lại nút nếu upload thất bại
                });
    }

    private void uploadImageFromBitmap(Bitmap bitmap) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference().child("uploads/" + System.currentTimeMillis() + ".jpg");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = storageReference.putBytes(data);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            Toasty.success(MainActivity.this, "Upload thành công", Toast.LENGTH_SHORT, true).show();
            resetInputAndHideCardView();
            buttonUpload.setEnabled(true); // Kích hoạt lại nút sau khi upload
        }).addOnFailureListener(e -> {
            Toasty.error(MainActivity.this, "Upload thất bại: " + e.getMessage(), Toast.LENGTH_SHORT, true).show();
            buttonUpload.setEnabled(true); // Kích hoạt lại nút nếu upload thất bại
        });
    }

    //    private boolean isValidImageUrl(String url) {
//        if (!android.webkit.URLUtil.isValidUrl(url)) {
//            return false;
//        }
//        String lowerUrl = url.toLowerCase();
//        return lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".png") || lowerUrl.endsWith(".bmp") || lowerUrl.endsWith(".gif");
//    }

    private void resetInputAndHideCardView() {
        editTextImageUrl.setText(""); // Xóa input
        imageView.setImageDrawable(null); // Xóa hình ảnh trong ImageView
        imageCardView.setVisibility(View.GONE); // Ẩn CardView
        imageUri = null; // Reset imageUri
        bitmap = null; // Reset bitmap
    }
}
