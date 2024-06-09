package com.example.kimyongjumap;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class CategoryActivity extends AppCompatActivity {

    private LinearLayout categoryLayout;
    private EditText editTextCategory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        categoryLayout = findViewById(R.id.categoryLayout);
        editTextCategory = findViewById(R.id.editTextCategory);
        Button buttonAddCategory = findViewById(R.id.buttonAddCategory);

        buttonAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCategory();
            }
        });
    }

    private void addCategory() {
        String category = editTextCategory.getText().toString().trim();
        if (!category.isEmpty()) {
            // 카테고리를 추가하고 화면에 표시
            Button button = new Button(this);
            button.setText(category);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 카테고리별 검색 로직 구현
                    searchCategory(category);
                }
            });
            categoryLayout.addView(button);
        }
    }

    private void searchCategory(String category) {
        // 카테고리별 검색 로직 구현
    }
}
