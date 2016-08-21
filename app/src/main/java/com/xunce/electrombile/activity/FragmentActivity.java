package com.xunce.electrombile.activity;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.serializer.AfterFilter;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.LogUtil;
import com.baidu.mapapi.model.LatLng;
import com.xunce.electrombile.Callback;
import com.xunce.electrombile.Constants.ProtocolConstants;
import com.xunce.electrombile.Constants.ServiceConstants;
import com.xunce.electrombile.R;
import com.xunce.electrombile.applicatoin.App;
import com.xunce.electrombile.applicatoin.Historys;
import com.xunce.electrombile.eventbus.EventbusConstants;
import com.xunce.electrombile.eventbus.FirstEvent;
import com.xunce.electrombile.fragment.MaptabFragment;
import com.xunce.electrombile.fragment.SettingsFragment;
import com.xunce.electrombile.fragment.SwitchFragment;
import com.xunce.electrombile.log.MyLog;
import com.xunce.electrombile.manager.CmdCenter;
import com.xunce.electrombile.manager.SettingManager;
import com.xunce.electrombile.manager.TracksManager;
import com.xunce.electrombile.mqtt.Connection;
import com.xunce.electrombile.mqtt.Connections;
import com.xunce.electrombile.receiver.MyReceiver;
import com.xunce.electrombile.utils.system.ToastUtils;
import com.xunce.electrombile.utils.useful.JPushUtils;
import com.xunce.electrombile.utils.useful.NetworkUtils;
import com.xunce.electrombile.view.viewpager.CustomViewPager;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import cn.jpush.android.api.JPushInterface;


/**
 * Created by heyukun on 2015/3/24. 修改 by liyanbo
 */

public class FragmentActivity extends android.support.v4.app.FragmentActivity
        implements SwitchFragment.GPSDataChangeListener{
    public static final int NOTIFICATION_ALARMSTATUS = 0;
    public static final int NOTIFICATION_AUTOLOCKSTATUS = 1;
    private static final String TAG = "FragmentActivity:";
    private static final int REFRESHTIME = 1000*60*30;
    public MqttAndroidClient mac;
    public CmdCenter mCenter;
    public SwitchFragment switchFragment;
    public MaptabFragment maptabFragment;
    public SettingsFragment settingsFragment;
    public SettingManager setManager;
    //viewpager切换使用
    private CustomViewPager mViewPager;
    private RadioGroup main_radio;
    private int checkId = R.id.rbSwitch;
    //退出使用
    private boolean isExit = false;
    //接收广播
    public MyReceiver receiver;
    public MqttConnectManager mqttConnectManager;
    private DrawerLayout mDrawerLayout;
    //获取到include中的ui(左滑出来的)
    private TextView BindedCarIMEI;
    private ImageView img_car;
    private ListView OtherCarListview;
    public ArrayList<HashMap<String, Object>> list;
    private List<String> IMEIlist;
    private SimpleAdapter simpleAdapter;
    private View left_menu;
    public static Boolean firsttime_Flag = true;
    private Thread myThread;
    private JPushUtils jPushUtils;
    private MessageReceiver mMessageReceiver;

    public static final int SWITCHDEVICE = 1;
    public static final int ADDDEVICE = 2;
    public static final int DELETEMAINDEVICE = 3;
    public static final int DELETEMONMAINDEVICE = 4;

    public static final String MESSAGE_RECEIVED_ACTION = "com.example.jpushdemo.MESSAGE_RECEIVED_ACTION";
    public static final String KEY_TITLE = "title";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_EXTRAS = "extras";

    private ChangeListener changeListener = null;
    private Dialog waitDialog;
    public Handler timeHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
//            if (msg.what == 999) {
//                settingsFragment.temp = 0;
//                return;
//            }
            dismissWaitDialog();
            ToastUtils.showShort(FragmentActivity.this, "服务器没有回复");
        }
    };

    final Handler handler = new Handler( ) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MyLog.d("handler", "updateData");
                    //查询电量
                    getBatteryInfo();
                    updateTotalItinerary();
                    break;

                case 2:
                    mqttConnectManager.subscribe(setManager.getIMEI(), new MqttConnectManager.Callback() {
                        @Override
                        public void onSuccess() {
                            ToastUtils.showShort(FragmentActivity.this,"重新订阅topic成功");
                            afterSubscribe();
                        }

                        @Override
                        public void onFail(Exception e) {
                            ToastUtils.showShort(FragmentActivity.this,"重新订阅topic失败");
                            handler.sendEmptyMessageDelayed(2,60000);
                        }
                    });
            }
        }
    };

    private void getBatteryInfo(){
        sendMessage(this, mCenter.getBatteryInfo(), setManager.getIMEI());
    }

    //查询总的公里数
    public void updateTotalItinerary(){
        AVQuery<AVObject> query = new AVQuery<>("DID");
        query.whereEqualTo("IMEI", setManager.getIMEI());
        query.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> list, AVException e) {
                if (e == null) {
                    if (!list.isEmpty()) {
                        if (list.size() != 1) {
                            ToastUtils.showShort(FragmentActivity.this, "DID表中  该IMEI对应多条记录");
                            return;
                        }
                        AVObject avObject = list.get(0);
                        int itinerary = (int) avObject.get("itinerary");
                        switchFragment.refreshItineraryInfo(itinerary / 1000.0);
                    }
                } else {
                    ToastUtils.showShort(FragmentActivity.this, "在DID表中查询该IMEI 查询失败");
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.orhanobut.logger.Logger.i("FragmentActivity-onCreate", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        //初始化界面
        initView();
        initData();
        //判断是否绑定设备
        MyLog.d("FragmentActivity", "onCreate3");
        queryIMEIandMqttConnection();
        MyLog.d("FragmentActivity", "onCreate4");
        Historys.put(this);
        registerBroadCast();

        JPushInterface.init(getApplicationContext());
        registerMessageReceiver();
        jPushUtils.setJPushAlias("simcom_" + setManager.getIMEI());

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStart() {
        MyLog.d("FragmentActivity","onStart");
        com.orhanobut.logger.Logger.i("FragmentActivity-onStart", "start");
        super.onStart();
        if (!NetworkUtils.isNetworkConnected(this)) {
            NetworkUtils.networkDialog(this, true);
        }

        if(mqttConnectManager.getMac()==null){
            mqttConnectManager.setContext(FragmentActivity.this);
            mqttConnectManager.initMqtt();
            queryIMEIandMqttConnection();
            MyLog.d("FragmentActivity", "onStart-queryIMEIandMqttConnection");
        }else{
            mac = mqttConnectManager.getMac();
        }
    }

    @Override
    protected void onResume() {
        //这个函数的onResume会被反复执行吗?
        MyLog.d("FragmentActivity","onResume");
        com.orhanobut.logger.Logger.i("FragmentActivity-onResume", "onResume");
        super.onResume();
        //下面这句话只需要执行一次
        if (mac != null && mac.isConnected()) {
            //这句话干嘛的
            mac.registerResources(this);

            Connection c = Connections.getInstance(this).getConnection(ServiceConstants.handler);
            if(c.getStatus().equals("none")){
                LogUtil.log.i("mac.isConnected()&&status-none");
                Connections.getInstance(this).removeConnection(c);
                mqttConnectManager.initMqtt();
                queryIMEIandMqttConnection();
            }
        }else{
            if(mac!=null){
                Connection c = Connections.getInstance(this).getConnection(ServiceConstants.handler);
                if(c.getStatus().equals("none")){
                    LogUtil.log.i("mac.disConnected&&status-none");
                    Connections.getInstance(this).removeConnection(c);
                    mqttConnectManager.initMqtt();
                    queryIMEIandMqttConnection();
                }else if(c.getStatus().equals("DISCONNECTED")){
                    mqttConnectManager.reconnectMqtt(new MqttConnectManager.OnMqttConnectListener() {
                        @Override
                        public void MqttConnectSuccess() {
                            Log.d("FragmentAct-onResume","MqttConnectSuccess");
                        }

                        @Override
                        public void MqttConnectFail() {
                            Log.d("FragmentAct-onResume","MqttConnectFail");
                        }
                    });
                }
            }
        }
    }

    @Override
    protected void onPause() {
        com.orhanobut.logger.Logger.i("FragmentActivity-onPause", "onPause");
        super.onPause();
    }

    @Override
    public void onStop(){
        com.orhanobut.logger.Logger.i("FragmentActivity-onStop", "onStop");

        if (myThread != null) {
            myThread.interrupt();
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        com.orhanobut.logger.Logger.i("FragmentActivity-onDestroy", "start");
        cancelNotification();
        if (mac != null) {
            mac.unregisterResources();
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
            Connections.getInstance(this).getConnection(ServiceConstants.handler).removeChangeListener(changeListener);
        }
        if (TracksManager.getTracks() != null) TracksManager.clearTracks();

        super.onDestroy();
        EventBus.getDefault().unregister(this);//反注册EventBus
    }

    @Override
    public void gpsCallBack(LatLng desLat, TracksManager.TrackPoint trackPoint) {
        maptabFragment.locateMobile(trackPoint);
    }

    //取消显示常驻通知栏
    public void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
        notificationManager.cancel(R.string.app_name);
    }

    /**
     * 显示等待框
     */
    public void showWaitDialog() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_wait, null);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.alpha);
        view.findViewById(R.id.iv).startAnimation(animation);
        waitDialog = new Dialog(this, R.style.Translucent_NoTitle_trans);
        waitDialog.addContentView(view, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        waitDialog.setContentView(view);
        waitDialog.setCancelable(false);
        waitDialog.show();
        WindowManager.LayoutParams params = waitDialog.getWindow().getAttributes();
        params.y = -156;
        waitDialog.getWindow().setAttributes(params);
    }

    /**
     * 取消显示等待框
     */
    public void dismissWaitDialog() {
        if (waitDialog != null) {
            waitDialog.dismiss();
        }
    }

    /**
     * 取消等待框的显示
     */
    public void cancelWaitTimeOut() {
        if (waitDialog != null) {
            dismissWaitDialog();
            timeHandler.removeMessages(ProtocolConstants.TIME_OUT);
        }
        else{
            timeHandler.removeMessages(ProtocolConstants.TIME_OUT);
        }
    }

    /**
     * 建立MQTT连接
     */
    private void getMqttConnection() {
        mqttConnectManager.setOnMqttConnectListener(new MqttConnectManager.OnMqttConnectListener() {
            @Override
            public void MqttConnectSuccess() {
                Connection connection = Connections.getInstance(FragmentActivity.this).getConnection(ServiceConstants.handler);
                changeListener = new ChangeListener();
                connection.registerChangeListener(changeListener);

                mac = mqttConnectManager.getMac();
                //这些是在呈现了页面之后执行的
//                MqttConnectManager.status = MqttConnectManager.OK;
                com.orhanobut.logger.Logger.i("MqttConnectSuccess", "mqtt连接成功(是否反复重连 反复成功?)");
                if (firsttime_Flag) {
                    mac = mqttConnectManager.getMac();
//                    mqttConnectManager.subscribe(setManager.getIMEI());
                    mqttConnectManager.subscribe(setManager.getIMEI(), new MqttConnectManager.Callback() {
                        @Override
                        public void onSuccess() {
                            ToastUtils.showShort(FragmentActivity.this, "订阅topic成功，60s后重试");
                            afterSubscribe();
                        }

                        @Override
                        public void onFail(Exception e) {
                            ToastUtils.showShort(FragmentActivity.this, "订阅topic失败，60s后重试");
                            handler.sendEmptyMessageDelayed(2,60000);

                        }
                    });
                }
            }

            @Override
            public void MqttConnectFail() {
//                MqttConnectManager.status = MqttConnectManager.CONNECTING_FAIL;
                ToastUtils.showShort(FragmentActivity.this, "连接服务器失败");
            }
        });
        mqttConnectManager.getMqttConnection();
    }

    private void afterSubscribe(){
//        ToastUtils.showShort(FragmentActivity.this, "服务器连接成功");
        setManager.setMqttStatus(true);
        //开启报警服务
        startAlarmService();

        //获取小安宝的初始状态:电量;自动落锁状态;小安宝的开关状态
        sendMessage(FragmentActivity.this, mCenter.getInitialStatus(), setManager.getIMEI());

        //获取总公里数
        updateTotalItinerary();

        firsttime_Flag = false;

        RefreshThread refreshThread = new RefreshThread();
        myThread = new Thread(refreshThread);
        myThread.start();
    }

    /**
     * 开启报警服务
     */
    private void startAlarmService() {
        Intent intent = new Intent();
        intent.setAction("com.xunce.electrombile.alarmservice");
        intent.setPackage(getPackageName());
        FragmentActivity.this.startService(intent);
    }

    /**
     * 发送命令
     *
     * @param context 传递上下文，用于弹吐司
     * @param message 要发送的命令
     * @param IMEI    要发送的设备号
     */
    public void sendMessage(Context context, final byte[] message, final String IMEI) {
        if(NetworkUtils.isNetworkConnected(this)){
            if(mqttConnectManager!=null){
                mac = mqttConnectManager.getMac();
            }

            if (mac == null||!mac.isConnected()) {
                timeHandler.sendEmptyMessageDelayed(ProtocolConstants.TIME_OUT, ProtocolConstants.TIME_OUT_VALUE);
                mqttConnectManager.reconnectMqtt(new MqttConnectManager.OnMqttConnectListener() {
                    @Override
                    public void MqttConnectSuccess() {
                        Log.d("sendMessage-reconnect","MqttConnectSuccess");
                        try {
                            //向服务器发送命令
                            mac.publish("app2dev/" + IMEI + "/cmd", message, ServiceConstants.MQTT_QUALITY_OF_SERVICE, false);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void MqttConnectFail() {
                        Log.d("sendMessage-reconnect","MqttConnectFail");
                    }
                });
                return;
            }


            try {
                //向服务器发送命令
                mac.publish("app2dev/" + IMEI + "/cmd", message, ServiceConstants.MQTT_QUALITY_OF_SERVICE, false);
            } catch (MqttException e) {
                e.printStackTrace();
                return;
            }
            timeHandler.sendEmptyMessageDelayed(ProtocolConstants.TIME_OUT, ProtocolConstants.TIME_OUT_VALUE);
        }else{
            ToastUtils.showShort(context, "无网络,请检查网络连接");
        }
    }

    /**
     * 查询并判断是否该建立MQTT连接
     */
    public void queryIMEIandMqttConnection(){
        if (setManager.getIMEI().isEmpty()) {
            AVQuery<AVObject> query = new AVQuery<>("Bindings");
            final AVUser currentUser = AVUser.getCurrentUser();
            query.whereEqualTo("user", currentUser);
            query.findInBackground(new FindCallback<AVObject>() {
                @Override
                public void done(List<AVObject> avObjects, AVException e) {
                    if (e == null && avObjects.size() > 0) {
                        setManager.setIMEI((String) avObjects.get(0).get("IMEI"));
                        //建立连接
                        getMqttConnection();
                    } else {
                        Log.d("失败", "查询错误2: ");
                        ToastUtils.showShort(FragmentActivity.this, "请先绑定设备");
                    }
                }
            });

        }
        else {
            getMqttConnection();
            ToastUtils.showShort(FragmentActivity.this, "登陆成功");
        }
    }

    /**
     * 注册广播  监听话题为：cmd gps 433
     */
    private void registerBroadCast() {
        receiver = new MyReceiver(FragmentActivity.this);
        IntentFilter filter = new IntentFilter();
        filter.setPriority(800);
        filter.addAction("MqttService.callbackToActivity.v0");
        LocalBroadcastManager.getInstance(FragmentActivity.this).registerReceiver(receiver, filter);
    }

    /**
     * 界面初始化
     */
    private void initView() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        main_radio = (RadioGroup) findViewById(R.id.main_radio);
        mViewPager = (CustomViewPager) findViewById(R.id.viewpager);
        switchFragment = new SwitchFragment();
        maptabFragment = new MaptabFragment();
        settingsFragment = new SettingsFragment();

        //左滑菜单
        left_menu = findViewById(R.id.left_menu);
        BindedCarIMEI = (TextView)left_menu.findViewById(R.id.menutext1);
        img_car = (ImageView)left_menu.findViewById(R.id.img_car);
        OtherCarListview = (ListView)left_menu.findViewById(R.id.OtherCarListview);
    }


    /**
     * 数据初始化
     */
    private void initData() {
        mCenter = CmdCenter.getInstance();
        setManager = SettingManager.getInstance();
        mqttConnectManager = MqttConnectManager.getInstance();
        mqttConnectManager.setContext(FragmentActivity.this);
        mqttConnectManager.initMqtt();
        jPushUtils = JPushUtils.getInstance();

        List<Fragment> list = new ArrayList<>();
        list.add(switchFragment);
        list.add(maptabFragment);
        list.add(settingsFragment);
        HomePagerAdapter mAdapter = new HomePagerAdapter(getSupportFragmentManager(), list);
        mViewPager.setAdapter(mAdapter);
        main_radio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.rbSwitch:
//                        mqttConnectManager.unSubscribeGPS(setManager.getIMEI());
                        mViewPager.setCurrentItem(0, false);
                        checkId = 0;
                        break;
                    case R.id.rbMap:
//                        mqttConnectManager.subscribeGPS(setManager.getIMEI());
                        mViewPager.setCurrentItem(1, false);
                        checkId = 1;
                        break;
                    case R.id.rbSettings:
//                        mqttConnectManager.unSubscribeGPS(setManager.getIMEI());
                        mViewPager.setCurrentItem(2, false);
                        checkId = 2;
                        break;
                    default:
                        break;
                }
            }
        });
        main_radio.check(checkId);

        this.list = new ArrayList<>();
        String[] strings = {"img","whichcar"};
        int[] ids = {R.id.img,R.id.WhichCar};
        simpleAdapter = new SimpleAdapter(FragmentActivity.this, this.list, R.layout.item_othercarlistview_green, strings, ids);
        OtherCarListview.setAdapter(simpleAdapter);

        OtherCarListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DeviceChange(position);
            }
        });

        refreshBindList1();
        new Handler().postDelayed(new Runnable() {
            public void run() {
                refreshBindList1();
                //execute the task
            }
        }, 2000);

    }

    /**
     * 界面切换
     */
    class HomePagerAdapter extends FragmentPagerAdapter {
        private List<Fragment> list;

        public HomePagerAdapter(FragmentManager fm, List<Fragment> list) {
            super(fm);
            this.list = list;
        }

        @Override
        public Fragment getItem(int position) {
            return list.get(position);
        }

        @Override
        public int getCount() {
            return list.size();
        }
    }

    /**
     * 重复按下返回键退出app方法
//     */
//    public void exit() {


//        if (!isExit) {
//            isExit = true;
//            Toast.makeText(getApplicationContext(),
//                    "退出程序", Toast.LENGTH_SHORT).show();
//            exitHandler.sendEmptyMessageDelayed(0, 2000);
//        } else {
//            cancelNotification();
//            if (mac != null) {
//                mac.unregisterResources();
//                LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
//            }
//            //此方法会不在onDestory中调用，所以放在结束任务之前使用
//            if (TracksManager.getTracks() != null) TracksManager.clearTracks();
//            timeHandler.removeMessages(0);
//            timeHandler = null;
//            Historys.exit();
//        }
//    }



    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
//        exit();
    }

    public void refreshBindList1(int type,int position){
        IMEIlist = setManager.getIMEIlist();
        BindedCarIMEI.setText(setManager.getCarName(IMEIlist.get(0)));
        switch (type){
            case SWITCHDEVICE:
                list.get(position).put("whichcar", setManager.getCarName(IMEIlist.get(position + 1)));
                simpleAdapter.notifyDataSetChanged();
                break;

            case DELETEMONMAINDEVICE:
                list.remove(position);
                simpleAdapter.notifyDataSetChanged();
                break;

            case DELETEMAINDEVICE:
                list.remove(0);
                simpleAdapter.notifyDataSetChanged();
                break;


            default:
                list.clear();
                for (int i = 1; i < IMEIlist.size(); i++) {
                    HashMap<String, Object> map = null;
                    map = new HashMap<>();
                    map.put("whichcar",setManager.getCarName(IMEIlist.get(i)));
                    map.put("img", R.drawable.othercar);
                    list.add(map);
                }
                simpleAdapter.notifyDataSetChanged();
                break;
        }
    }

    public void refreshBindList1(){
        IMEIlist = setManager.getIMEIlist();
        BindedCarIMEI.setText(setManager.getCarName(IMEIlist.get(0)));

        HashMap<String, Object> map = null;
        list.clear();
        for (int i = 1; i < IMEIlist.size(); i++) {
            map = new HashMap<>();
            map.put("whichcar",setManager.getCarName(IMEIlist.get(i)));
            map.put("img", R.drawable.othercar);
            list.add(map);
        }
        simpleAdapter.notifyDataSetChanged();
    }

    //设备切换
    private void DeviceChange(final int position){
        if(NetworkUtils.checkNetwork(this)){
            ToastUtils.showShort(this, "请检查网络连接,切换无法完成");
            return;
        }
        //第一步 关闭抽屉
        closeDrawable();
        //第二步  逻辑上切换过来
        final String previous_IMEI = setManager.getIMEI();
        final String current_IMEI = IMEIlist.get(position+1);
           //在这里就解订阅原来的设备号,并且订阅新的设备号
        if(mqttConnectManager.returnMqttStatus()){
            //mqtt连接良好
            showWaitDialog();
            jPushUtils.setJPushAlias("simcom_" + current_IMEI, new Callback() {
                @Override
                public void onSuccess() {
                    LogUtil.log.i("jPushUtils-setJPushAlias-onSuccess");
                    mqttConnectManager.unSubscribe(previous_IMEI, new MqttConnectManager.Callback() {
                        @Override
                        public void onSuccess() {
                            LogUtil.log.i("unSubscribe-onSuccess");
                            mqttConnectManager.subscribe(current_IMEI, new MqttConnectManager.Callback() {
                                @Override
                                public void onSuccess() {
                                    LogUtil.log.i("Subscribe-onSuccess");
                                    setManager.setIMEI(current_IMEI);
//                                    //查询APP初始状态
                                    mqttConnectManager.sendMessage(mCenter.getInitialStatus(), current_IMEI);
                                    switchFragment.refreshBatteryToNULL();
                                    ToastUtils.showShort(FragmentActivity.this, "切换成功");

                                    IMEIlist.set(0, setManager.getIMEI());
                                    IMEIlist.set(position + 1, previous_IMEI);
                                    setManager.setIMEIlist(IMEIlist);

                                    //第三步:UI刷新
                                    Intent intent = new Intent("com.app.bc.test");
                                    intent.putExtra("KIND", "SWITCHDEVICE");
                                    intent.putExtra("POSITION", position);
                                    sendBroadcast(intent);//发送广播事件

                                    dismissWaitDialog();
                                }

                                @Override
                                public void onFail(Exception e) {
                                    dismissWaitDialog();
//                                    Subscribe失败
                                    ToastUtils.showShort(App.getInstance(),
                                            "切换设备失败,"+e.getMessage()+",请退出登录");
                                }
                            });
                        }

                        @Override
                        public void onFail(Exception e) {
                            dismissWaitDialog();
//                            unSubscribe失败
                            ToastUtils.showShort(App.getInstance(),
                                    "切换设备失败,"+e.getMessage()+",请退出登录");
                        }
                    });
                }

                @Override
                public void onFail() {
                    dismissWaitDialog();
//                    setJPushAlias失败
                    ToastUtils.showShort(App.getInstance(), "切换设备失败,请稍后再试");
                }
            });
        }
        else{
            ToastUtils.showShort(this, "mqtt连接失败");
        }
    }

    public void openDrawable(){
        mDrawerLayout.openDrawer(Gravity.LEFT);
    }

    public void closeDrawable(){
        mDrawerLayout.closeDrawer(left_menu);
    }

    public void setLeftMenuCarImage(Bitmap bm){
        img_car.setImageBitmap(bm);
    }

    class RefreshThread implements Runnable {
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    MyLog.d("refreshTime","refreshTime");
                    Thread.sleep(REFRESHTIME);
                } catch (InterruptedException e) {
                    myThread = null;
                    return;
                }

                Message message = new Message();
                message.what = 1;

                handler.sendMessage(message);
            }
        }
    }

    public void stopThread(){
        if(myThread!=null){
            myThread.interrupt();
        }
    }

    public void startThread(){
        if(myThread==null){
            RefreshThread refreshThread = new RefreshThread();
            myThread = new Thread(refreshThread);
            myThread.start();
        }
    }

    static public void cancelAllNotification(){
        NotificationManager notificationManager = (NotificationManager) App.getInstance().getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    public void registerMessageReceiver() {
        mMessageReceiver = new MessageReceiver();
        IntentFilter filter = new IntentFilter();
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        filter.addAction(MESSAGE_RECEIVED_ACTION);
        registerReceiver(mMessageReceiver, filter);
    }

    public class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (MESSAGE_RECEIVED_ACTION.equals(intent.getAction())) {
                String messge = intent.getStringExtra(KEY_MESSAGE);
                String extras = intent.getStringExtra(KEY_EXTRAS);
                StringBuilder showMsg = new StringBuilder();
                showMsg.append(KEY_MESSAGE + " : " + messge + "\n");
            }
        }
    }


    /**
     * <code>ChangeListener</code> updates the UI when the {@link Connection}
     * object it is associated with updates
     *
     */
    private class ChangeListener implements PropertyChangeListener {

        /**
         * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
         */
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            String status = Connections.getInstance(FragmentActivity.this).getConnection(ServiceConstants.handler).getStatus();
            switchFragment.refreshMqttstatus(status);
        }
    }

    @Subscribe
    public void onFirstEvent(FirstEvent event){
//        String msg = "onEventMainThread收到了消息：" + event.getMsg();
        if(event.getMsg().equals(EventbusConstants.FromgetHeadImageFromServer)){
            refreshBindList1();
        }
    }
}