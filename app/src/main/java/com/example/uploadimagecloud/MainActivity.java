package com.example.uploadimagecloud;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView; // Import CardView

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private ImageView imageView;
    private Button buttonSelect, buttonUpload, buttonLoadImageUrl;
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

        buttonSelect.setOnClickListener(v -> openFileChooser());
        buttonUpload.setOnClickListener(v -> uploadFile());
        buttonLoadImageUrl.setOnClickListener(v -> loadFromUrl());
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

    private void loadFromUrl() {
        String url = editTextImageUrl.getText().toString().trim();
        if (!url.isEmpty() && isValidImageUrl(url)) {
            new Thread(() -> {
                try {
                    InputStream input = new java.net.URL(url).openStream();
                    bitmap = BitmapFactory.decodeStream(input);
                    runOnUiThread(() -> {
                        imageView.setImageBitmap(bitmap);
                        imageCardView.setVisibility(View.VISIBLE); // Show CardView when image is loaded from URL
                        buttonUpload.setVisibility(View.VISIBLE);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            Toast.makeText(this, "Please enter a valid image URL", Toast.LENGTH_SHORT).show();
        }
    }


    private void uploadFile() {
        if (imageUri != null) {
            uploadImageFromUri(imageUri);
        } else if (bitmap != null) {
            uploadImageFromBitmap(bitmap);
        } else {
            Toast.makeText(this, "Không có file nào được chọn", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageFromUri(Uri uri) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference().child("uploads/" + System.currentTimeMillis() + ".jpg");

        storageReference.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(MainActivity.this, "Upload thành công", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Upload thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(MainActivity.this, "Upload thành công", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(MainActivity.this, "Upload thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private boolean isValidImageUrl(String url) {
        if (!android.webkit.URLUtil.isValidUrl(url)) {
            return false;
        }
        String lowerUrl = url.toLowerCase();
        return lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".png") || lowerUrl.endsWith(".bmp") || lowerUrl.endsWith(".gif");
    }
}
