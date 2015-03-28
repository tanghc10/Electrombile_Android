package com.xunce.electrombile.UniversalTool;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import com.xunce.electrombile.R;

/**
 * Created by pc on 2015/3/25.
 */
public class ToastUtil {
    private static Toast mToast;

    private static Handler mHandler = new Handler();
    private static Runnable r = new Runnable() {
        @Override
        public void run() {
            mToast.cancel();
            mToast = null;
        }
    };
    public static void showToast(Context context,String message,int time){
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.toastutil_toast,null);
        TextView text = (TextView) view.findViewById(R.id.toast_message);
        text.setText(message);
        mHandler.removeCallbacks(r);
        if(mToast == null){
            mToast = new Toast(context);
            mToast.setDuration(Toast.LENGTH_SHORT);
            mToast.setGravity(Gravity.BOTTOM,0,150);
            mToast.setView(view);
        }
        mHandler.postDelayed(r,time);
        mToast.show();
    }
}
