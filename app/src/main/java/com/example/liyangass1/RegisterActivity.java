package com.example.liyangass1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage mStorage = FirebaseStorage.getInstance();;
    StorageReference storageRef = mStorage.getReference();
    private final int CAMERA_REQUEST_CODE = 1888;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private EditText usernameEditText;
    private EditText shortBioEditText;
    private ImageView selfie;
    private Bitmap photo;
    //the SelfieStatus is used to show whether or not the user has taken a selfie(0 means no,, 1 yes)
    private int selfieStatus = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        emailEditText = findViewById(R.id.Email);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.confirm_password);
        usernameEditText = findViewById(R.id.UserName);
        shortBioEditText = findViewById(R.id.ShortBio);
        selfie = findViewById(R.id.TakePhote);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void Registering(View view) {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        String userName = usernameEditText.getText().toString();
        String shortBio = shortBioEditText.getText().toString();
        if (!password.equals(confirmPassword)){
            Toast.makeText(RegisterActivity.this, "Confirm passworld not consistent with password.",
                    Toast.LENGTH_SHORT).show();
        }
        else if (email.equals("") || password.equals("") ||confirmPassword.equals("") ||
                userName.equals("") || shortBio.equals("")){
            Toast.makeText(RegisterActivity.this, "Please fill in all fields.",
                    Toast.LENGTH_SHORT).show();
        }
        else if(selfieStatus==0){
            Toast.makeText(this, "Please take a selfie of yourself!", Toast.LENGTH_SHORT).show();
        }
        else {
            signTheUserUp(email, password);
        }

    }

    private void signTheUserUp(String email, String password){
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            UpdateUser();
                            Toast.makeText(RegisterActivity.this, "Sign Up Successful!.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(RegisterActivity.this, "Registration failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void UpdateUser(){
        String userName = usernameEditText.getText().toString();
        String shortBio = shortBioEditText.getText().toString();
        String email = emailEditText.getText().toString();
        Map<String, Object> user = new HashMap<>();
        StorageReference userRef = storageRef.child(email);
        StorageReference selfieRef = userRef.child("selfie");
        String selfieURL = selfieRef.toString();
        int haha = selfieURL.indexOf("%40");
        int length = selfieURL.length();
        selfieURL = selfieURL.substring(0, haha) + "@" + selfieURL.substring(haha+3, length);
        user.put("UserName", userName);
        user.put("ShortBio", shortBio);
        user.put("selfieURL", selfieURL);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] byteArray = baos.toByteArray();
        UploadTask uploadTask = selfieRef.putBytes(byteArray);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                updateUI();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                user.put("selfieURL", "None");
                Toast.makeText(RegisterActivity.this,
                        "Failed to load profile picture.",
                        Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });
        db.collection("users")
                .document(email)
                .set(user);
    }

    private void updateUI(){
        Intent intent = new Intent(RegisterActivity.this, LoggedInPage.class);
        intent.putExtra("email", emailEditText.getText().toString());
        startActivity(intent);
    }

    public void TakePhoto(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        }
        else {
            Toast.makeText(RegisterActivity.this,
                    "The version of your OS does not support this action.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_REQUEST_CODE && data != null && resultCode == RESULT_OK) {
            try {
                photo = (Bitmap) data.getExtras().get("data");
                selfie.setImageBitmap(photo);
                selfieStatus = 1;
            } catch (Exception e){}
        }
        else {
            photo = BitmapFactory.decodeResource(getResources(), R.drawable.photo);
        }
    }
}