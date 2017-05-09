package com.sxq.webviewperformancemonitor;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;


import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by shixiaoqiangsx on 2017/4/11.
 */
public class MyWebViewClient extends WebViewClient {

    private WebView mWebView;
    private AndroidObject mAndroidObject;

    /**
     * WebView不支持修改Timeout , 这里自定义
     */
    private int mTimeOut = 3000;
    private int mJsTimeout = 500;

    private Timer mTimer = new Timer();

    /**
     * 避免重复执行mWebsiteLoadTimeoutTask
     */
    private boolean isWebTimeoutTaskScheduling = false;

    /**
     * 避免重复执行mJsInjectTimeoutTask
     */
    private boolean isJsTimeoutTaskScheduling = false;

    /**
     * 判断网页加载是否完成
     */
    private AtomicBoolean isWebLoadFinished = new AtomicBoolean(false);

    private TimerTask mWebsiteLoadTimeoutTask = new TimerTask() {
        @Override
        public void run() {
            if (mWebView != null && !isWebLoadFinished.get()) {
                sendWebsiteLoadTimeoutMsg();
            }
        }
    };

    private TimerTask mJsInjectTimeoutTask = new TimerTask() {
        @Override
        public void run() {
            if (mWebView != null && mAndroidObject != null) {
                if (!mAndroidObject.isDataReturn()) {
                    sendJsInjectTimeoutMsg();
                } else {
                    sendDestroyMsg();
                }
            }
        }
    };

    final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.HandlerMessage.MSG_DESTROY: {
                    destroyWebView();
                    break;
                }
                case Constants.HandlerMessage.MSG_WEBSITE_LOAD_TIMEOUT: {
                    if (mWebView != null) {
                        Logger.d("网页加载超时 , WebView进度:" + mWebView.getProgress() + " ,  url:" + mWebView.getUrl());
                        if (mWebView.getProgress() < 100) {
                            mAndroidObject.handleError("LoadUrlTimeout");
                            destroyWebView();
                        }
                    }
                    break;
                }
                case Constants.HandlerMessage.MSG_JS_INJECT_TIMEOUT: {
                    if (mWebView != null) {
                        if (mAndroidObject != null) {
                            if (!mAndroidObject.isDataReturn()) {
                                Logger.d("JS注入脚本执行超时");
                                String format = "ExecuteJsTimeout(%dms)";
                                mAndroidObject.handleError(String.format(format, mJsTimeout));
                                destroyWebView();
                            }
                        }
                    }
                    break;
                }

                default:
                    super.handleMessage(msg);
            }
        }
    };

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        Logger.d("网页开始加载:" + url);

        if (mWebView == null) {
            mWebView = view;
            if (mWebView instanceof MyWebView) {
                mAndroidObject = ((MyWebView) mWebView).getAndroidObject();
            }
        }
        setupWebLoadTimeout();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        //只会重定向时回调,然后回调onPageStarted
//        Logger.d("回调旧版shouldOverrideUrlLoading ， url :" + url);
        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        //只会重定向时回调,然后回调onPageStarted
//        Logger.d("回调新版shouldOverrideUrlLoading ， request method :" + request.getMethod() + "\t是否为重定向: " + request.isRedirect() + "\trequest url :" + request.getUrl());
        return super.shouldOverrideUrlLoading(view, request);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        //每次请求资源的时候都会在onLoadResource前回调，可用于拦截资源加载，修改request
//        Logger.d("回调旧版shouldInterceptRequest ， url :" + url);
        return super.shouldInterceptRequest(view, url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        //每次请求资源的时候都会在onLoadResource前回调，可用于拦截资源加载，修改request
//        Logger.d("回调新版shouldInterceptRequest " );
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        super.onLoadResource(view, url);
//        Logger.d("加载网页资源 , url:" + url + " , WebView进度:" + view.getProgress());
    }

    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        Logger.d("网页加载完成,WebView进度:" + view.getProgress());


        //可能会在进度<100或==100的情况下出现多次onPageFinished回调
        if (view.getProgress() == 100 && !isWebLoadFinished.get()) {
            Logger.d("注入js脚本");
            //可能会回调多次
            isWebLoadFinished.set(true);
            String format = "javascript:%s.sendResource(JSON.stringify(window.performance.timing));";
            String injectJs = String.format(format, MyWebView.ANDROID_OBJECT_NAME);
            view.loadUrl(injectJs);

            setupJsInjectTimeout();
        }
    }

    @Deprecated
    @Override
    public void onReceivedError(WebView view, int errorCode,
                                String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        Logger.d("回调旧版本onReceivedError():" + "错误描述:" + description + "\t错误代码:" + errorCode + "失败的Url:" + failingUrl);

        handleError(description);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        Logger.d("回调新版本onReceivedError:");
    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        super.onReceivedHttpError(view, request, errorResponse);
        Logger.d("回调onRecivedHttpError:");
        handleError("onReceivedHttpError");
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        super.onReceivedSslError(view, handler, error);
        Logger.d("回调onReceivedSslError()：" + "\terror:" + error.toString());
        handleError("onReceivedSslError");
    }

    private void handleError(String msg) {
        isWebLoadFinished.set(true);
        sendDestroyMsg();
        if (mAndroidObject != null) {
            mAndroidObject.handleError(msg);
        }

    }

    public int getTimeOut() {
        return mTimeOut;
    }

    public void setTimeOut(int timeOut) {
        mTimeOut = timeOut;
    }

    /**
     * 网页加载计时
     */
    private void setupWebLoadTimeout() {
        if (!isWebTimeoutTaskScheduling) {
            isWebTimeoutTaskScheduling = true;
            mTimer.schedule(mWebsiteLoadTimeoutTask, mTimeOut);
        }
    }

    /**
     * 注入js脚本执行计时
     * <p>
     * 注入js之后等待一段时间，如果这段时间内js不回调AndroidObject.handleResource()，则再销毁WebView
     * 过早销毁WebView，js不回调AndroidObject.handleResource()
     */
    private void setupJsInjectTimeout() {
        if (!isJsTimeoutTaskScheduling) {
            isJsTimeoutTaskScheduling = true;
            if (mAndroidObject != null) {
                mAndroidObject.setStartTime(System.currentTimeMillis());
            }
            mTimer.schedule(mJsInjectTimeoutTask, mJsTimeout);
        }
    }

    private void sendDestroyMsg() {
        handler.sendEmptyMessage(Constants.HandlerMessage.MSG_DESTROY);
    }

    private void sendWebsiteLoadTimeoutMsg() {
        handler.sendEmptyMessage(Constants.HandlerMessage.MSG_WEBSITE_LOAD_TIMEOUT);
    }

    private void sendJsInjectTimeoutMsg() {
        handler.sendEmptyMessage(Constants.HandlerMessage.MSG_JS_INJECT_TIMEOUT);
    }

    private void destroyWebView() {
        if (mWebView != null) {
            mWebView.clearCache(true);
            mWebView.clearHistory();
            mWebView.destroy();
            mWebView = null;
            Logger.d("成功销毁WebView");
        } else {
            Logger.d("销毁失败，WebView为空");
        }
    }

}
