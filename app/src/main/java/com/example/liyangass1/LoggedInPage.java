package com.example.liyangass1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import com.example.liyangass1.ui.login.LoginActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class LoggedInPage extends AppCompatActivity implements Callback{

    private TextView TheUserName;
    private TextView ShortBio;
    private FirebaseFirestore db;
    private ImageView Selfie;
    private FirebaseAuth mAuth;
    private FirebaseStorage mStorage;
    private RecyclerView recyclerView;
    private int CAMERA_REQUEST_CODE = 1888;
    private String email;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged_in_page);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        TheUserName = findViewById(R.id.TheUserName);
        ShortBio = findViewById(R.id.TheShortBio);
        Selfie = findViewById(R.id.Selfie);
        mStorage = FirebaseStorage.getInstance();
        recyclerView = findViewById(R.id.RecyclerView);
        int numberOfColumns = 3;
        recyclerView.setLayoutManager(new GridLayoutManager(LoggedInPage.this, numberOfColumns));
        email = getIntent().getStringExtra("email");
        getUserBoioFromFirestore(email);
        getUserPhotosFromFirestore(email);
    }

    @Override
    public void onResume(){
        super.onResume();
        getUserPhotosFromFirestore(email);
    }



    private void getUserPhotosFromFirestore(String email){
        db.collection("photos")
                .whereEqualTo("uid", email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            //Using Treemap to sort the downloading url by timestamp
                            Map orderMap = new TreeMap();
                            for (QueryDocumentSnapshot document: task.getResult()){
                                Long timestamp = (Long) document.getData().get("timestamp");
                                String url = (String) document.getData().get("storageRef");
                                String caption = (String) document.getData().get("caption");
                                ArrayList<String> datas = new ArrayList<String>();
                                datas.add(url);
                                datas.add(caption);
                                datas.add(timestamp.toString());
                                orderMap.put(timestamp, datas);
                            }
                            //Transforming the treemap to an array
                            ArrayList<ArrayList> data = new ArrayList<ArrayList>(orderMap.values());
                            MyRecyclerViewAdapter adapter = new MyRecyclerViewAdapter(
                                    LoggedInPage.this,
                                    data,
                                    mStorage);
                            recyclerView.setAdapter(adapter);
                        } else{
                            Toast.makeText(LoggedInPage.this,
                                    "Can not retrieve photos!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //The callback method that is executed in the RecyclerView's adapter when image is clicked
    public void onItemClick(byte[] byteArray, String caption, String timestamp, String url){
        Intent intent = new Intent(this, FullScreenImage.class);
        intent.putExtra("photo", byteArray);
        intent.putExtra("caption", caption);
        intent.putExtra("commentID", email+timestamp);
        intent.putExtra("userName", userName);
        boolean ifIsOwner = true;
        intent.putExtra("ifIsOwner", ifIsOwner);
        intent.putExtra("downloadURL", url);
        intent.putExtra("email",email);
        startActivity(intent);
    }

    private void getUserBoioFromFirestore(String email){
        db.collection("users")
                .document(email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            Map<String, Object> dummy = document.getData();
                            userName = (String) dummy.get("UserName");
                            TheUserName.setText(userName);
                            ShortBio.setText((String) dummy.get("ShortBio"));
                            String downloadURL = (String) dummy.get("selfieURL");
                            if (!downloadURL.equals("None")) {
                                StorageReference selfieRef = mStorage.getReferenceFromUrl(downloadURL);
                                selfieRef.getBytes(1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                    @Override
                                    public void onSuccess(byte[] bytes) {
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                        Selfie.setImageBitmap(bitmap);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        Toast.makeText(LoggedInPage.this,
                                                "Failed to load profile picture.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            TheUserName.setText("Error happens pulling your profile.");
                            Log.d("TAG", "get failed with ", task.getException());
                        }
                    }
                });
    }

    public void TakePhotos(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        }
        else {
            Toast.makeText(LoggedInPage.this,
                    "The version of your OS does not support this action.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 1888:
                try {
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    photo.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    Intent intent = new Intent(LoggedInPage.this, PhotoConfirmation.class);
                    intent.putExtra("photo", byteArray);
                    intent.putExtra("email", email);
                    startActivityForResult(intent, 1);
                    break;
                } catch (Exception e){break;}

            //The user has confirmed to upload the image
            case 1:
                if (resultCode==1) {
                    Log.d("wobeiyingyongle", Integer.toString(requestCode));
                    Toast.makeText(LoggedInPage.this,
                            "Image Update Successful!",
                            Toast.LENGTH_LONG).show();
                    getUserPhotosFromFirestore(email);
                }
        }

    }


    public void ToSignOuT(View view) {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void ToGlobal(View view) {
        Intent intent = new Intent(this, GlobalView.class);
        intent.putExtra("userName", userName);
        intent.putExtra("email", email);
        startActivity(intent);
    }
}