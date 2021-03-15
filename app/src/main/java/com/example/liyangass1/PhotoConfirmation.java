package com.example.liyangass1;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhotoConfirmation extends AppCompatActivity {

    private Bitmap bitmap;
    private byte[] bytes;
    private String email;
    private EditText caption;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_confirmation);
        ImageView need_confirmation_image = findViewById(R.id.need_confirmation_image);
        bytes = getIntent().getByteArrayExtra("photo");
        Bitmap beforeRotationBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Matrix matrix = new Matrix();
        matrix.postRotate(180);
        bitmap = Bitmap.createBitmap(beforeRotationBitmap, 0, 0, beforeRotationBitmap.getWidth(), beforeRotationBitmap.getHeight(), matrix, true);
        need_confirmation_image.setImageBitmap(bitmap);
        email = getIntent().getStringExtra("email");
        caption = findViewById(R.id.caption);

    }


    //User cancel the photo taken will invoke thsi method
    public void CancelPhoto(View view) {
        finish();
    }

    //use the resultcode to tell starting activity that user has confirmed photo upload
    private void updateUI(int resultCode){
        setResult(resultCode);
        finish();
    }

    public void ConfirmThePhoto(View view) {
        Long timeLong = System.currentTimeMillis();
        String timeString = timeLong.toString();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseStorage mStorage = FirebaseStorage.getInstance();
        StorageReference storgaeRef = mStorage.getReference();
        StorageReference userRef = storgaeRef.child(email);
        StorageReference selfieRef = userRef.child(timeString);
        String selfieURL = selfieRef.toString();
        int haha = selfieURL.indexOf("%40");
        int length = selfieURL.length();
        selfieURL = selfieURL.substring(0, haha) + "@" + selfieURL.substring(haha+3, length);
        String captionText = caption.getText().toString();
        Map<String, Object> photoURL = new HashMap<String, Object>();
        photoURL.put("storageRef", selfieURL);
        photoURL.put("timestamp", timeLong);
        photoURL.put("uid",email);
        photoURL.put("caption", captionText);
        UploadTask uploadTask = selfieRef.putBytes(bytes);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                db.collection("photos")
                        .document()
                        .set(photoURL);
                updateUI(1);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(PhotoConfirmation.this,
                        "Failed to load profile picture.",
                        Toast.LENGTH_SHORT).show();
                updateUI(1);
            }
        });
    }

    public void autoHashtags(View view) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        ImageLabelerOptions options =
        new ImageLabelerOptions.Builder().setConfidenceThreshold(0.7f).build();
        ImageLabeler labeler = ImageLabeling.getClient(options);
        labeler.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                    @Override
                    public void onSuccess(List<ImageLabel> labels) {
                        String builder = " ";
                        for (ImageLabel label : labels) {
                            String text = label.getText();
                            builder = builder + "#" + text;
                        }
                        caption.append(builder);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(PhotoConfirmation.this,
                                "Can not get hashtags",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
