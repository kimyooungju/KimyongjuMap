package com.example.kimyongjumap;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RestaurantInfoDialog extends Dialog {

    private Restaurant restaurant;
    private DatabaseReference mBookmarkDatabase;

    public RestaurantInfoDialog(@NonNull Context context, Restaurant restaurant) {
        super(context);
        this.restaurant = restaurant;
        mBookmarkDatabase = FirebaseDatabase.getInstance().getReference("bookmarks");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_restaurant_info);

        TextView textViewName = findViewById(R.id.textViewName);
        TextView textViewAddress = findViewById(R.id.textViewAddress);
        Button buttonBookmark = findViewById(R.id.buttonBookmark);
        Button buttonViewReviews = findViewById(R.id.buttonViewReviews);
        Button buttonWriteReview = findViewById(R.id.buttonWriteReview);

        textViewName.setText(restaurant.getTitle());
        textViewAddress.setText(restaurant.getAddress());

        buttonBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    // 로그인된 사용자만 북마크 저장 가능
                    String userId = user.getUid();
                    String key = mBookmarkDatabase.child(userId).push().getKey();
                    if (key != null) {
                        mBookmarkDatabase.child(userId).child(key).setValue(restaurant);
                        dismiss();
                    }
                } else {
                    Toast.makeText(getContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonViewReviews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), RestaurantReviewsActivity.class);
                intent.putExtra("restaurantId", restaurant.getId());
                getContext().startActivity(intent);
            }
        });

        buttonWriteReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), WriteReviewActivity.class);
                intent.putExtra("restaurantId", restaurant.getId());
                getContext().startActivity(intent);
            }
        });
    }
}
