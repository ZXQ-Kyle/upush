package com.wsl.upush;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.umeng.message.UmengNotifyClickActivity;

import org.android.agoo.common.AgooConstants;

import java.util.List;

public class UPushOffLineNotificationActivity extends UmengNotifyClickActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_mipush);
        ImageView iv = findViewById(R.id.iv);
        setAppIcon(iv);
    }

    public void setAppIcon(ImageView iv){
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo info = packageManager.getApplicationInfo(getPackageName(), 0);
            Drawable drawable = info.loadIcon(packageManager);
            iv.setImageDrawable(drawable);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void a(Intent intent) {
        super.a(intent);
    }

    @Override
    public void onMessage(Intent intent) {
        super.onMessage(intent);

        final String body = intent.getStringExtra(AgooConstants.MESSAGE_BODY);

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(this.getPackageName());
        List<ResolveInfo> mApps = getPackageManager().queryIntentActivities(mainIntent, 0);


//        for (int i = 0; i < mApps.size(); i++) {
//        }
        //启动第一个启动页
        if (mApps.size() >= 1) {
            ResolveInfo info = mApps.get(0);
            String packageName = info.activityInfo.packageName;
            String appName = info.activityInfo.name;

            ComponentName mComponentName = new ComponentName(packageName, appName);
            Intent intent1 = new Intent();
            intent1.setComponent(mComponentName);
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Bundle mExtras = new Bundle();
            mExtras.putString("pushJson", body);
            intent1.putExtras(mExtras);
            startActivity(intent1);
            Log.d("pushJson", "MipushTestActivity:" + body);
            UPushApplication.mUPushApplication.onOffLineMsgClickHandler(body);
            finish();
        }
    }
}
