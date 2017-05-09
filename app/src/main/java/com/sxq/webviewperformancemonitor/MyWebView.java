package com.sxq.webviewperformancemonitor;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Created by shixiaoqiangsx on 2017/4/11.
 */
public class MyWebView extends WebView {

    public final static String ANDROID_OBJECT_NAME = "android";

    private AndroidObject mAndroidObject = null;

    public MyWebView(Context context) {
        this(context, null, 0);
    }

    public MyWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setAndroidObject(AndroidObject object) {
        if (object == null) {
            Logger.d("AndroidObject can not be null !");
            this.mAndroidObject = new AndroidObject() {
                @Override
                public void handleError(String msg) {
                }

                @Override
                public void handleResource(String jsonStr) {
                }
            };
        } else {
            this.mAndroidObject = object;
        }

        super.addJavascriptInterface(mAndroidObject, ANDROID_OBJECT_NAME);
    }

    protected AndroidObject getAndroidObject() {
        return this.mAndroidObject;
    }
}
