package ru.babay.vk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import com.perm.kate.api.Auth;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 12.01.13
 * Time: 21:12
 */
public class Vkontakte {

    public static final String CANCEL_URI = "xx";
    public static final String CALLBACK_URI = "xx";
    public static final String DENIED_URI = "xx";

    private int mIcon;
    private String mAppId;
    private Auth.ResponseType mResponceType;
    private int mPermissions;

    public Vkontakte(int mIcon, String mAppId, Auth.ResponseType mResponceType, int mPermissions) {
        this.mIcon = mIcon;
        this.mAppId = mAppId;
        this.mResponceType = mResponceType;
        this.mPermissions = mPermissions;
    }

    public void authorize(Context context, Fragment fragment, int requestCode) {
        Intent intent = new Intent(context, VkActivity.class);
        intent.putExtra(VkActivity.APP_ID_TAG, mAppId);
        intent.putExtra(VkActivity.ICON_TAG, mIcon);
        intent.putExtra(VkActivity.RESPONSE_TYPE_TAG, mResponceType);
        intent.putExtra(VkActivity.PERMISSIONS_TAG, mPermissions);
        fragment.startActivityForResult(intent, requestCode);
    }

    public static void receiveResult(int resultCode, Intent data, LoginListener callback) {
        if (resultCode == Activity.RESULT_OK) {
            callback.onComplete(data.getStringExtra(VkActivity.TOKEN_TAG),
                    data.getStringExtra(VkActivity.USER_ID_TAG),
                    data.getStringExtra(VkActivity.CODE_TAG),
                    data.getLongExtra(VkActivity.EXPIRE_DATE, 0));
        } else {
            if (data != null && data.hasExtra(VkActivity.ERROR_TAG))
                callback.onError((Throwable) data.getSerializableExtra(VkActivity.ERROR_TAG));
            else
                callback.onCancel();
        }
    }

    public static interface LoginListener {
        public void onComplete(String token, String userId, String code, long expireDate);

        public void onError(Throwable e);

        public void onCancel();
    }
}
