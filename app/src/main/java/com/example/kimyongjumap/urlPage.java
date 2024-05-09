package com.example.kimyongjumap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.annotation.Nullable;

public class urlPage extends Activity {
    class myWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(android.webkit.WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    Button wgo, wback, layBack;
    WebView markUrlView;
    View.OnClickListener cl;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view);
        Intent i;
        i = getIntent();
        String restaurantWeburl = i.getStringExtra("urlText");

        myWebViewClient webView = new myWebViewClient();

        wgo = (Button) findViewById(R.id.webGo);
        wback = (Button) findViewById(R.id.webBack);
        layBack = (Button) findViewById(R.id.layoutBack);
        markUrlView = (WebView) findViewById(R.id.markerUrl);

        markUrlView.setWebViewClient(webView);

        markUrlView.getSettings().setJavaScriptEnabled(true);
        markUrlView.loadUrl(restaurantWeburl);

        cl = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id =v.getId();
                if (id == R.id.webGo){
                    markUrlView.goForward();
                }else if (id == R.id.webBack){
                    markUrlView.goBack();
                }else if ( id == R.id.layoutBack){
                    finish();
                }
            }
        };
        wgo.setOnClickListener(cl);
        wback.setOnClickListener(cl);
        layBack.setOnClickListener(cl);
    }
}
