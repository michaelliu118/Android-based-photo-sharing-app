package com.example.liyangass1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telecom.Call;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.liyangass1.ui.login.LoginActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class GlobalView extends AppCompatActivity implements GlobalCallback {

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String userName;
    private String email;
    private RecyclerView recyclerView;
    private final int CAMERA_REQUEST_CODE=1888;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gloabl_view);
        userName = getIntent().getStringExtra("userName");
        email = getIntent().getStringExtra("email");
        //Construct the recyclerview for the global photos
        recyclerView = findViewById(R.id.global_recyclerview);
        int numberOfColumns = 1;
        recyclerView.setLayoutManager(new GridLayoutManager(this, numberOfColumns));
        String userName = getIntent().getStringExtra("userName");
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        getPhotos();
    }

    @Override
    public void onResume(){
        super.onResume();
        getPhotos();
    }


    private void getPhotos(){
        TreeMap<Long, ArrayList> orderMap = new TreeMap<Long, ArrayList>();
        db.collection("photos").get().
                addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (QueryDocumentSnapshot document: task.getResult()){
                            ArrayList list = new ArrayList<>();
                            Map resultMap = document.getData();
                            String caption = (String) resultMap.get("caption");
                            String url = (String) resultMap.get("storageRef");
                            Long timestamp = (Long) resultMap.get("timestamp");
                            String email = (String) resultMap.get("uid");
                            list.add(caption);
                            list.add(timestamp.toString());
                            list.add(url);
                            list.add(email);
                            orderMap.put(timestamp, list);
                        }
                        ArrayList<ArrayList> data = new ArrayList<ArrayList>(orderMap.values());
                        GlobalRecyclerviewAdapter adapter = new GlobalRecyclerviewAdapter(GlobalView.this, data, storage);
                        recyclerView.setAdapter(adapter);
                    }
                });
    }

    @Override
    public void onItemClick(byte[] byteArray, String caption, String timestamp, String url, String email) {
        Intent intent = new Intent(this, FullScreenImage.class);
        intent.putExtra("photo", byteArray);
        intent.putExtra("caption", caption);
        intent.putExtra("commentID", email+timestamp);
        intent.putExtra("userName", userName);
        boolean ifIsOwner = this.email.equals(email);
        intent.putExtra("ifIsOwner", ifIsOwner);
        intent.putExtra("downloadURL", url);
        startActivity(intent);
    }

    public void TakePhotosGlobal(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        }
        else {
            Toast.makeText(this,
                    "The version of your OS does not support this action.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1888:
                try {
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    photo.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    Intent intent = new Intent(this, PhotoConfirmation.class);
                    intent.putExtra("photo", byteArray);
                    intent.putExtra("email", email);
                    startActivityForResult(intent, 1);
                    break;
                } catch (Exception e) {
                    break;
                }

                //The user has confirmed to upload the image
            case 1:
                if (resultCode == 1) {
                    Log.d("wobeiyingyongle", Integer.toString(requestCode));
                    Toast.makeText(this,
                            "Image Update Successful!",
                            Toast.LENGTH_LONG).show();
                    getPhotos();
                }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    public void GlobalToSignOuT(View view) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
}
