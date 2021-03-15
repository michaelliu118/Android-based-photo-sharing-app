package com.example.liyangass1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class GlobalRecyclerviewAdapter extends RecyclerView.Adapter<GlobalRecyclerviewAdapter.ViewHolder> {

    private final ArrayList<ArrayList> mData;
    private final LayoutInflater mInflater;
    private final FirebaseStorage mStorage;
    private GlobalCallback callback;

    public GlobalRecyclerviewAdapter(GlobalCallback callback, ArrayList<ArrayList> data, FirebaseStorage storage){
        this.mInflater = LayoutInflater.from((Context) callback);
        this.mData = data;
        this.mStorage = storage;
        this.callback = callback;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        ImageView myImageView;

        public ViewHolder(View itemView){
            super(itemView);
            myImageView = itemView.findViewById(R.id.global_recyclerview_item);
        }
    }
    @NonNull
    @Override
    public GlobalRecyclerviewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.global_recyclerview_item, parent, false);
        return new GlobalRecyclerviewAdapter.ViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull GlobalRecyclerviewAdapter.ViewHolder holder, int position) {
        String downloadURL = (String) this.mData.get(position).get(2);
        StorageReference photoRef = mStorage.getReferenceFromUrl(downloadURL);
        photoRef.getBytes(1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                String caption = (String) mData.get(position).get(0);
                String timestamp =  (String) mData.get(position).get(1);
                String email = (String) mData.get(position).get(3);
                holder.myImageView.setImageBitmap(bitmap);
                holder.myImageView.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v)
                    {
                        callback.onItemClick(bytes, caption, timestamp, downloadURL, email);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                holder.myImageView.setImageResource(R.drawable.photo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }
}
