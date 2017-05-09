package com.sxq.webviewperformancemonitor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button mBtBegin ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtBegin = (Button)findViewById(R.id.btBegin);
        mBtBegin.setOnClickListener(this);

    }


    @Override
    public void onClick(View v){
        final MyWebView mWebView = new MyWebView(this); //此时并不算是UI,不能通过view.post或postDelayed更新UI，但可以通过Handler更新或销毁
        mWebView.setVisibility(View.GONE);

        WebSettings setting = mWebView.getSettings();
        setting.setJavaScriptEnabled(true);
        setting.setCacheMode(WebSettings.LOAD_NO_CACHE);
        setting.setLoadsImagesAutomatically(false);

        MyWebViewClient myWebViewClient = new MyWebViewClient();
        myWebViewClient.setTimeOut(3000);
        mWebView.setWebViewClient(myWebViewClient);

        mWebView.setAndroidObject(new AndroidObject() {
            @Override
            public void handleError(String msg) {
                Logger.d("AndroidObject,错误信息:" + msg);
            }

            @Override
            public void handleResource(String jsonStr) {
                Logger.d("AndroidObject,Timing信息:" + jsonStr);
            }
        });
        mWebView.loadUrl("http://www.baidu.com");
    }
}
