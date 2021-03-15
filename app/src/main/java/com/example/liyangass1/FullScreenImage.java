package com.example.liyangass1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class FullScreenImage extends AppCompatActivity {

    private String userName;
    private boolean ifIsOwner;
    private TextView myComment;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.full_screen_image);
        ifIsOwner = getIntent().getBooleanExtra("ifIsOwner", false);
        //find every element by its id
        ImageView fullScreenImage = findViewById(R.id.FullScreenImage);
        TextView caption = findViewById(R.id.viewCaption);
        myComment = findViewById(R.id.myComment);
        //show the photo using the bitmap passed from logged_in_activity
        byte[] bytes = getIntent().getByteArrayExtra("photo");
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0, bytes.length);
        fullScreenImage.setImageBitmap(bitmap);
        //show the caption using the caption string from logged_in_activity
        String captionString = getIntent().getStringExtra("caption");
        caption.setText(captionString);
        //get the userName passed from logged in activity
        userName = getIntent().getStringExtra("userName");
        //get the comment using the unique code user email+photo timestamp
        String commentID = getIntent().getStringExtra("commentID");
        getComments(commentID);
    }

    private void getComments(String commentID){
        RecyclerView comments = findViewById(R.id.comments);
        int numberOfColumns = 1;
        comments.setLayoutManager(new GridLayoutManager(this, numberOfColumns));
        db.collection("comments").
                whereEqualTo("commentID", commentID).
                get().
                addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            //using treemap to sort the query results
                            Map orderMap = new TreeMap();
                            for (QueryDocumentSnapshot result: task.getResult()){
                                String userName = (String) result.getData().get("userName");
                                String commentContent = (String) result.getData().get("commentContent");
                                Long timestamp = (Long) result.getData().get("timestamp");
                                //the key is timestamp
                                orderMap.put(timestamp, new String[]{userName, commentContent});
                            }
                            ArrayList<String[]> data = new ArrayList<String[]>(orderMap.values());
                            CommentsAdapter adapter = new CommentsAdapter(data, FullScreenImage.this);
                            comments.setAdapter(adapter);

                        } else {
                            Toast.makeText(FullScreenImage.this,
                                    "Can't retrieve comments.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //invoked when the user comments on a status
    public void sendComment(View view) {
        Map<String, Object> uploadMap = new HashMap<String, Object>();
        uploadMap.put("userName", userName);
        Long timeLong = System.currentTimeMillis();
        uploadMap.put("timestamp", timeLong);
        String commentID = getIntent().getStringExtra("commentID");
        uploadMap.put("commentID", commentID);
        String content = myComment.getText().toString();
        uploadMap.put("commentContent", content);
        db.collection("comments").
                document().
                set(uploadMap).
                addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        getComments(commentID);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(FullScreenImage.this,
                                "Failed to upload your comment.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
        myComment.setText("");
    }

    public void deletePhoto(View view) {
        if (ifIsOwner){
            String storageRef = getIntent().getStringExtra("downloadURL");
            storage.getReferenceFromUrl(storageRef).delete();
            CollectionReference photorRef = db.collection("photos");
            photorRef.whereEqualTo("storageRef", storageRef).
            get().
            addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()){
                        String documentID = task.getResult().getDocuments().get(0).getId();
                        photorRef.document(documentID).delete();
                        finish();
                    }
                }
            });
        } else {
            Log.d("tag", "you are wrong");
            Toast.makeText(this,
                    "You are not the owner of the photo.",
                    Toast.LENGTH_SHORT).show();
        }
    }

}
