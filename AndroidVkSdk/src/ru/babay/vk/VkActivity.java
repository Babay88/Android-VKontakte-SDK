package ru.babay.vk;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.perm.kate.api.Auth;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 13.01.13
 * Time: 1:02
 */
public class VkActivity extends Activity {
    public static final String ICON_TAG = "ru.babay.vk.icon";
    public static final String APP_ID_TAG = "ru.babay.vk.appId";
    public static final String RESPONSE_TYPE_TAG = "ru.babay.vk.responseType";
    public static final String PERMISSIONS_TAG = "ru.babay.vk.permissions";
    public static final String TOKEN_TAG = "ru.babay.vk.token";
    public static final String CODE_TAG = "ru.babay.vk.code";
    public static final String EXPIRE_DATE = "ru.babay.vk.expire";
    public static final String USER_ID_TAG = "ru.babay.vk.userId";
    public static final String ERROR_TAG = "ru.babay.vk.error";

    static final int TW_BLUE = 0xFFC0DEED;
    static final int MARGIN = 4;
    static final int PADDING = 2;

    private int mIcon;
    String mAppId;
    Auth.ResponseType mResponseType;
    int mPermissions;

    private ProgressDialog mSpinner;
    private WebView mWebView;
    private ViewGroup mContent;
    private TextView mTitle;
    String mUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!loadParams())
            return;


        mSpinner = new ProgressDialog(this);
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage("Loading...");

        mContent = new LinearLayout(this);
        ((LinearLayout)mContent).setOrientation(LinearLayout.VERTICAL);
        mContent.setBackgroundColor(Color.WHITE);
        setUpTitle();
        setUpWebView();

        setContentView(mContent);

        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        retrieveRequestToken();
    }

    boolean loadParams(){
        Intent intent = getIntent();
        mIcon = intent.getIntExtra(ICON_TAG, 0);
        mPermissions = intent.getIntExtra(PERMISSIONS_TAG, 0);
        if (!intent.hasExtra(APP_ID_TAG)) {
            resultError(new Exception("No app id specified"));
            return false;
        }
        mAppId = intent.getStringExtra(APP_ID_TAG);
        if (!intent.hasExtra(RESPONSE_TYPE_TAG)) {
            resultError(new Exception("No responce type specified"));
            return false;
        }
        mResponseType = (Auth.ResponseType)intent.getSerializableExtra(RESPONSE_TYPE_TAG);
        return true;
    }

    void resultError(Throwable error) {
        Intent intent = new Intent();
        intent.putExtra(ERROR_TAG, error);
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    void resultCancel() {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    void resultOk(Auth.AuthData data) {
        Intent intent = new Intent();
        intent.putExtra(TOKEN_TAG, data.token);
        intent.putExtra(USER_ID_TAG, data.userId);
        intent.putExtra(CODE_TAG, data.code);
        intent.putExtra(EXPIRE_DATE, data.expiresIn == 0 ? 0 : System.currentTimeMillis() + data.expiresIn * 1000);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void retrieveRequestToken() {
        mSpinner.show();
        mContent.post(new Runnable() {
            @Override
            public void run() {
                mUrl = Auth.getUrl(mAppId, Integer.toString(mPermissions), mResponseType);
                mWebView.loadUrl(mUrl);
            }
        });
    }

    private void setUpTitle() {
        Drawable icon = getResources().getDrawable(mIcon);
        mTitle = new TextView(this);
        mTitle.setText("Вконтакте");
        mTitle.setTextColor(Color.WHITE);
        mTitle.setTypeface(Typeface.DEFAULT_BOLD);
        mTitle.setBackgroundColor(TW_BLUE);
        mTitle.setPadding(MARGIN + PADDING, MARGIN, MARGIN, MARGIN);
        mTitle.setCompoundDrawablePadding(MARGIN + PADDING);
        mTitle.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);

        ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mContent.addView(mTitle, lp);
    }

    private void setUpWebView() {
        mWebView = new WebView(this);
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setWebViewClient(new VkWebViewClient());
        mWebView.getSettings().setJavaScriptEnabled(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mContent.addView(mWebView, lp);
    }

    void retrieveAccessToken(String url) {
        try {
            resultOk(Auth.parseRedirectUrl(url));
        } catch (Exception e) {
            resultError(e);
        }
    }

    private class VkWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            if (uri.getQueryParameter("cancel") != null) {
                resultCancel();
                return true;
            }

            if (url.startsWith(Auth.REDIRECT_URL)) {
                retrieveAccessToken(url);
                return true;
            }

            mUrl = url;
            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            resultError(new DialogError(description, errorCode, failingUrl));
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (!mSpinner.isShowing())
                try{
                    mSpinner.show();
                }
                catch (Exception e){}
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            String title = mWebView.getTitle();
            if (title != null && title.length() > 0) {
                mTitle.setText(title);
            }
            try {
                if (mSpinner.isShowing())
                    mSpinner.dismiss();
            } catch (Exception ignored) {}
        }
    }
}
