package com.xunce.electrombile.fragment;

import android.app.Activity;
import android.app.AlertDialog;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.content.BroadcastReceiver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.LogUtil;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;
import com.xunce.electrombile.Constants.ProtocolConstants;
import com.xunce.electrombile.R;
import com.xunce.electrombile.activity.CropActivity;
import com.xunce.electrombile.activity.FragmentActivity;
import com.xunce.electrombile.activity.MqttConnectManager;
import com.xunce.electrombile.activity.account.HeadImageActivity;
import com.xunce.electrombile.bean.WeatherBean;
import com.xunce.electrombile.utils.device.VibratorUtil;
import com.xunce.electrombile.utils.system.BitmapUtils;
import com.xunce.electrombile.utils.system.ToastUtils;
import com.xunce.electrombile.utils.system.WIFIUtil;
import com.xunce.electrombile.utils.useful.JSONUtils;
import com.xunce.electrombile.utils.useful.NetworkUtils;
import com.xunce.electrombile.utils.useful.StringUtils;
import com.xunce.electrombile.view.MyHorizontalScrollView;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;

public class SwitchFragment extends BaseFragment implements OnGetGeoCoderResultListener,OnClickListener {
    private static final int DELAYTIME = 1000;
    private static String TAG = "SwitchFragment";
    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new MyLocationListener();
    private GeoCoder mSearch = null; // 搜索模块，也可去掉地图模块独立使用
    private boolean alarmState = false;
    //缓存view
    private View rootView;
    private MyHorizontalScrollView myHorizontalScrollView;
    private TextView BindedCarIMEI;
    private List<String> OtherCar;
    private Dialog waitDialog;
    private String WeatherData;
    private Button btnAlarmState1;
    private static int Count = 0;
    static MKOfflineMap mkOfflineMap;
    private static String localcity;
    private List<String> IMEIlist;
    private Bitmap bitmap;
    private ImageView headImage;
    private TextView tv_temperature;
    private TextView tv_weatherCondition;
    private TextView tv_location;
    private ImageView img_weather;
    private ImageView leftmenu_CarImage;
    private BroadcastReceiver MyBroadcastReceiver;
    private PopupWindow mpopupWindow;
    private Uri imageUri;
    public static final int TAKE_PHOTE=1;
    public static final int CROP_PHOTO=2;
    public static final int CHOOSE_PHOTO=3;



//    private Logger log;

    public Handler timeHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            dismissWaitDialog();
            ToastUtils.showShort(m_context, "指令下发失败，请检查网络和设备工作是否正常。");
        }
    };

    private Handler mhandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case 1://处理侧滑的message
                    myHorizontalScrollView.UpdateListview();
                    break;
                case 2:
                    cancelWaitTimeOut();
                    break;
                case 3:
                    msgSuccessArrived();
                    break;
                case 4:
                    if (setManager.getAlarmFlag()) {
                        openStateAlarmBtn();
                        showNotification("安全宝防盗系统已启动");
                    } else {
                        closeStateAlarmBtn();
                    }
                    break;
            }
            return;
        }
    };


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_pop_FromGallery:
                File outputImage = new File(Environment.getExternalStorageDirectory(),"output_image.jpg");
                try{
                    if(outputImage.exists()){
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                }catch(IOException e){
                    e.printStackTrace();
                }
                imageUri = Uri.fromFile(outputImage);
                Intent intent = new Intent("android.intent.action.PICK");
                intent.setType("image/*");
                startActivityForResult(intent,CHOOSE_PHOTO);
                mpopupWindow.dismiss();
                break;

            case R.id.tv_pop_camera:
                File outputImage1 = new File(Environment.getExternalStorageDirectory(),"output_image.jpg");
                try{
                    if(outputImage1.exists()){
                        outputImage1.delete();
                    }
                    outputImage1.createNewFile();
                }catch(IOException e){
                    e.printStackTrace();
                }
                imageUri = Uri.fromFile(outputImage1);
                Intent intent1 = new Intent("android.media.action.IMAGE_CAPTURE");
                intent1.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent1,TAKE_PHOTE);
                mpopupWindow.dismiss();
                break;

            case R.id.tv_pop_cancel:
                mpopupWindow.dismiss();
                break;

            default:
                break;
        }
    }

    @Override
    public void onAttach(Activity activity) {
//        log = Logger.getLogger(SwitchFragment.class);
//        log.info("onAttach-start");
        super.onAttach(activity);
//        log.info("onAttach-finish");
    }

    @Override
    public void onCreate(Bundle saveInstanceState) {
//        log.info("onCreate-start");
        super.onCreate(saveInstanceState);
        // 初始化搜索模块，注册事件监听
        mSearch = GeoCoder.newInstance();
        mSearch.setOnGetGeoCodeResultListener(this);

        mLocationClient = new LocationClient(m_context);     //声明LocationClient类
        mLocationClient.registerLocationListener(myListener);    //注册监听函数
        initLocation();
        mLocationClient.start();

        mkOfflineMap = new MKOfflineMap(); //初始化离线地图
        mkOfflineMap.init(new MKOfflineMapListener() {
            @Override
            public void onGetOfflineMapState(int i, int i1) {
            }
        });
        new Handler().postDelayed(new Runnable() {
            public void run() {
                offlineMapAutoDownload(); //检查网络是否可以离线下载
                //execute the task
            }
        }, DELAYTIME);
//        log.info("onCreate-finish");

        MyBroadcastReceiver = new MyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.app.bc.test");
        m_context.registerReceiver(MyBroadcastReceiver,filter);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
//        log.info("onCreateView-start");
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.switch_fragment, container, false);
            initView(rootView);
        }
//        log.info("onCreateView-finish");
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
//        log.info("onViewCreated-start");
        super.onViewCreated(view, savedInstanceState);
        initEvent();
//        log.info("onViewCreated-finish");
      }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
//        log.info("onActivityCreated-start");
        super.onActivityCreated(savedInstanceState);
//        log.info("onActivityCreated-finish");
    }

    @Override
    public void onStart(){
//        log.info("onStart-start");
        super.onStart();
//        log.info("onStart-finish");
    }

    @Override
    public void onResume() {
//        log.info("onResume-start");
        super.onResume();
        if (setManager.getAlarmFlag()) {
            openStateAlarmBtn();
            showNotification("安全宝防盗系统已启动");
        } else {
            closeStateAlarmBtn();
        }
//        log.info("onResume-finish");
    }

    @Override
    public void onPause() {
//        log.info("onPause-start");
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        super.onPause();
//        log.info("onPause-finish");
    }

    @Override
    public void onStop(){
//        log.info("onStop-start");
        super.onStop();
//        log.info("onStop-finish");
    }

    @Override
    public void onDestroyView() {
//        log.info("onDestroyView-start");
        super.onDestroyView();
        ((ViewGroup) rootView.getParent()).removeView(rootView);
//        log.info("onDestroyView-finish");
    }

    @Override
    public void onDestroy() {
//        log.info("onDestroy-start");
        m_context.unregisterReceiver(MyBroadcastReceiver);
        m_context = null;
        mSearch = null;

        super.onDestroy();
//        log.info("onDestroy-finish");
    }

    @Override
    public void onDetach(){
//        log.info("onDetach-start");
        super.onDetach();
//        log.info("onDetach-finish");
    }

    private void initView(View v){
        View leftmenu = v.findViewById(R.id.ll_leftmenu) ;
        leftmenu_CarImage = (ImageView)leftmenu.findViewById(R.id.img_car);

        btnAlarmState1 = (Button) v.findViewById(R.id.btn_AlarmState1);
        BindedCarIMEI = (TextView)v.findViewById(R.id.menutext1);
        OtherCar = new ArrayList();
        myHorizontalScrollView = (MyHorizontalScrollView) v.findViewById(R.id.myHorizontalScrollView);
        Button ChangeAutobike = (Button) v.findViewById(R.id.ChangeAutobike);
        headImage = (ImageView) v.findViewById(R.id.img_headImage);
        headImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopMenu();
            }
        });

        bitmap = BitmapUtils.compressImageFromFile(setManager.getIMEI());
        if(bitmap!=null){
            headImage.setImageBitmap(bitmap);
            leftmenu_CarImage.setImageBitmap(bitmap);
        }

        img_weather = (ImageView)v.findViewById(R.id.img_weather);

        ChangeAutobike.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //侧滑的代码
                myHorizontalScrollView.toggle();
                if (myHorizontalScrollView.getIsOpen()) {
                    myHorizontalScrollView.UpdateListview();
                }
            }
        });

        btnAlarmState1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                alarmStatusChange();
            }
        });

        myHorizontalScrollView.InitView();
        myHorizontalScrollView.setHandler(mhandler);
        myHorizontalScrollView.otherCarlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //UI界面改变
                String IMEI_previous = BindedCarIMEI.getText().toString();
                String IMEI_now = OtherCar.get(position);

                BindedCarIMEI.setText(IMEI_now);

                myHorizontalScrollView.list.remove(position);
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("whichcar", IMEI_previous);
                map.put("img", R.drawable.othercar);
                myHorizontalScrollView.list.add(map);
                myHorizontalScrollView.UpdateListview();
                OtherCar.remove(position);
                OtherCar.add(IMEI_previous);
                //实际逻辑改变
                DeviceChange(IMEI_previous, IMEI_now, position);
            }
        });

        tv_temperature = (TextView)v.findViewById(R.id.tv_temperature);
        tv_weatherCondition = (TextView)v.findViewById(R.id.tv_weatherCondition);
        tv_location = (TextView)v.findViewById(R.id.tv_location);
    }

    private void initEvent() {
        IMEIlist = setManager.getIMEIlist();
        refreshBindList1();
        (m_context).receiver.setAlarmHandler(mhandler);
        //从设置切换回主页的时候  会执行这个函数  如果主页中的侧滑菜单是打开的  那么就关闭侧滑菜单
        if(myHorizontalScrollView.getIsOpen())
        {
            myHorizontalScrollView.toggle();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case TAKE_PHOTE:
                if(resultCode == Activity.RESULT_OK) {
                    Intent intent = new Intent(m_context,CropActivity.class);
                    intent.setData(imageUri);
                    startActivityForResult(intent, CROP_PHOTO);
                }
                break;

            case CHOOSE_PHOTO:
                if(resultCode == Activity.RESULT_OK) {
                    imageUri = data.getData();
                    Intent intent = new Intent(m_context,CropActivity.class);
                    intent.setData(imageUri);
                    startActivityForResult(intent, CROP_PHOTO);
                }
                break;

            case CROP_PHOTO:
                if(resultCode == Activity.RESULT_OK) {
                    //上传到服务器
                    bitmap = BitmapUtils.compressImageFromFile(setManager.getIMEI());
                    if (bitmap != null) {
                        headImage.setImageBitmap(bitmap);
                        leftmenu_CarImage.setImageBitmap(bitmap);

                    }
                }
                break;
            default:
                break;
        }
    }

    //设备切换
    private void DeviceChange(String previous_IMEI,String current_IMEI,int position){
        //在这里就解订阅原来的设备号,并且订阅新的设备号,然后查询小安宝的开关状态
        MqttConnectManager mqttConnectManager = MqttConnectManager.getInstance();
        if(mqttConnectManager.returnMqttStatus()){
            //mqtt连接良好
            mqttConnectManager.unSubscribe(previous_IMEI);
            setManager.setIMEI(current_IMEI);
            mqttConnectManager.subscribe(current_IMEI);
            mqttConnectManager.sendMessage((m_context).mCenter.cmdFenceGet(), current_IMEI);
            ToastUtils.showShort(m_context,"切换成功");
            myHorizontalScrollView.toggle();
        }
        else{
            ToastUtils.showShort(m_context,"mqtt连接失败");
        }

        IMEIlist.set(0, setManager.getIMEI());
        IMEIlist.set(position+1,previous_IMEI);
        setManager.setIMEIlist(IMEIlist);

        //切换头像
        DeviceChangeHeadImage();

    }

    private void DeviceChangeHeadImage(){
        String fileName = Environment.getExternalStorageDirectory() + "/"+setManager.getIMEI()+"crop_result.png";
        File f = new File(fileName);
        if(f.exists()){
            bitmap = BitmapUtils.compressImageFromFile(setManager.getIMEI());
            if(bitmap!=null){
                headImage.setImageBitmap(bitmap);
                leftmenu_CarImage.setImageBitmap(bitmap);
                return;
            }
            else{
                //默认图像
                Bitmap bitmap = BitmapFactory.decodeResource(m_context.getResources(), R.drawable.person);
                headImage.setImageBitmap(bitmap);
                leftmenu_CarImage.setImageBitmap(bitmap);
            }
        }
        else{
            Bitmap bitmap = BitmapFactory.decodeResource(m_context.getResources(), R.drawable.person);
            headImage.setImageBitmap(bitmap);
            leftmenu_CarImage.setImageBitmap(bitmap);
        }
    }

    public void reverserGeoCedec(LatLng pCenter) {
        mSearch.reverseGeoCode(new ReverseGeoCodeOption()
                .location(pCenter));
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {

    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
        LogUtil.log.i("进入位置设置:" + result.getAddress());
        if (result.error != SearchResult.ERRORNO.NO_ERROR) {
            return;
        }
    }

    private void httpGetWeather(String city) {
        String ss = city;
        city = city.replace("市", "");
        Log.e(TAG, "city：" + city);
        city = StringUtils.encode(city);
        Log.e(TAG, "city" + city);
        HttpUtils http = new HttpUtils();
        http.send(HttpRequest.HttpMethod.GET,
                "http://apistore.baidu.com/microservice/weather?cityname=" + city,
                new RequestCallBack<String>() {
                    //更新任务进度
                    @Override
                    public void onLoading(long total, long current, boolean isUploading) {
                        Log.e(TAG, "onLoading");
                    }

                    @Override
                    public void onSuccess(ResponseInfo<String> responseInfo) {
                        String test = responseInfo.result;
                        Log.i(TAG, StringUtils.decodeUnicode(responseInfo.result));
                        String originData = StringUtils.decodeUnicode(responseInfo.result);
                        WeatherBean data = new WeatherBean();
                        parseWeatherErr(data, originData);
                    }

                    private void parseWeatherErr(WeatherBean data, String originData) {
                        try {
                            data.errNum = JSONUtils.ParseJSON(originData, "errNum");
                            data.errMsg = JSONUtils.ParseJSON(originData, "errMsg");
                            if ("0".equals(data.errNum) && "success".equals(data.errMsg)) {
                                data.retData = JSONUtils.ParseJSON(originData, "retData");
                                parseRetData(data.retData, data);
                            } else {
                                Log.e(TAG, "fail to get Weather info");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    private void parseRetData(String originData, WeatherBean data) {
                        try {
                            data.city = JSONUtils.ParseJSON(originData, "city");
                            data.time = JSONUtils.ParseJSON(originData, "time");
                            data.weather = JSONUtils.ParseJSON(originData, "weather");
                            data.temp = JSONUtils.ParseJSON(originData, "temp");
                            data.l_tmp = JSONUtils.ParseJSON(originData, "l_tmp");
                            data.h_tmp = JSONUtils.ParseJSON(originData, "h_tmp");
                            data.WD = JSONUtils.ParseJSON(originData, "WD");
                            data.WS = JSONUtils.ParseJSON(originData, "WS");
                            setWeather(data);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            ToastUtils.showShort(m_context, "天气查询失败");
                        }
                    }

                    private void setWeather(WeatherBean data) {
                        tv_temperature.setText(data.temp+"摄氏度");
                        tv_weatherCondition.setText(data.weather);
                        tv_location.setText(data.city);

                        if(data.weather.contains("雨")){
                            img_weather.setImageDrawable(m_context.getResources().getDrawable((R.drawable.rain)));
                        }
                        else if(data.weather.equals("多云")){
                            img_weather.setImageDrawable(m_context.getResources().getDrawable((R.drawable.sunny)));
                        }
                        else{
                            img_weather.setImageDrawable(m_context.getResources().getDrawable((R.drawable.snow)));
                        }
                    }

                    @Override
                    public void onStart() {
                        Log.i(TAG, "start");
                    }

                    @Override
                    public void onFailure(HttpException error, String msg) {
                        Log.i(TAG, "失败");
                    }
                });
    }

    /**
     * 判断是不是在WIFI环境下，如果是就自动下载离线地图
     */
    private void offlineMapAutoDownload() {
        if (WIFIUtil.isWIFI(getActivity())) {
            com.orhanobut.logger.Logger.d("没错，就是在WIFI环境下，我要开始下载离线地图了");
            downloadOfflinemap();
        } else {
            com.orhanobut.logger.Logger.d("没连WIFI，不敢下……");
        }
    }

    private static void downloadOfflinemap() {
        if (localcity != null) {
            com.orhanobut.logger.Logger.d("我获取到了城市的名字%s", localcity);
            MKOLUpdateElement element = LocalCityelement();
            if (element != null) {
                com.orhanobut.logger.Logger.d("以前好像下过了，我看看下完没");
                if (element.status != MKOLUpdateElement.FINISHED) {
                    com.orhanobut.logger.Logger.d("没下完，我接着下");
                    mkOfflineMap.start(element.cityID);
                }
            } else {
                com.orhanobut.logger.Logger.d("以前没下过");
                ArrayList<MKOLSearchRecord> records = mkOfflineMap.searchCity(localcity);
                for (MKOLSearchRecord record : records) {
                    com.orhanobut.logger.Logger.d("下载:%d", record.cityID);
                    mkOfflineMap.start(record.cityID);
                }
            }
        }
    }

    //内部类
    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
                String city = location.getCity();
                httpGetWeather(city);
                localcity = city;
            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
                String city = location.getCity();
                httpGetWeather(city);
                localcity = city;
            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                Log.i(TAG, "离线定位成功，离线定位结果也是有效的");
                String city = location.getCity();
                httpGetWeather(city);
                localcity = city;
            } else {
                Log.e(TAG, "服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
            }
        }
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
        );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        int span = 1000;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIgnoreKillProcess(false);//可选，默认false，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        mLocationClient.setLocOption(option);
    }

    public void alarmStatusChange() {
        Count++;
        if(1 == Count)
        {
            (m_context).receiver.setAlarmHandler(mhandler);
        }

        if (alarmState) {
            if (!setManager.getIMEI().isEmpty()) {
                if (NetworkUtils.isNetworkConnected(m_context)) {
                    //关闭报警
                    //等状态设置成功之后再改变按钮的显示状态，并且再更改标志位等的保存。
                    cancelNotification();
                    (m_context).sendMessage(m_context, mCenter.cmdFenceOff(), setManager.getIMEI());
                    showWaitDialog();
                    timeHandler.sendEmptyMessageDelayed(ProtocolConstants.TIME_OUT, ProtocolConstants.TIME_OUT_VALUE);
                } else {
                    ToastUtils.showShort(m_context, "网络连接失败");
                }
            } else {
                ToastUtils.showShort(m_context, "请等待设备绑定");
            }
        } else {
            if (NetworkUtils.isNetworkConnected(m_context)) {
                //打开报警
                if (!setManager.getIMEI().isEmpty()) {
                    //等状态设置成功之后再改变按钮的显示状态，并且再更改标志位等的保存。
                    cancelNotification();
                    VibratorUtil.Vibrate(m_context, 700);
                    //发送命令之前判断一下   mqtt连接是否正常
                    if ((m_context).mac != null && (m_context).mac.isConnected())
                    {
                        (m_context).sendMessage(m_context, mCenter.cmdFenceOn(), setManager.getIMEI());
                        showWaitDialog();
                        timeHandler.sendEmptyMessageDelayed(ProtocolConstants.TIME_OUT, ProtocolConstants.TIME_OUT_VALUE);
                    }
                    else{
                        ToastUtils.showShort(m_context,"mqtt连接失败");
                    }
                } else {
                    ToastUtils.showShort(m_context, "请先绑定设备");
                }
            } else {
                ToastUtils.showShort(m_context, "网络连接失败");
            }
        }
    }

    public void showWaitDialog() {
        LayoutInflater inflater = (LayoutInflater) (m_context).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_wait, null);
        Animation animation = AnimationUtils.loadAnimation(m_context, R.anim.alpha);
        view.findViewById(R.id.iv).startAnimation(animation);
        waitDialog = new Dialog(m_context, R.style.Translucent_NoTitle_trans);
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
    }

    //取消显示常驻通知栏
    public void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) (m_context).getSystemService(m_context
                .NOTIFICATION_SERVICE);
        notificationManager.cancel(R.string.app_name);
    }

    //点击打开报警按钮时按钮样式的响应操作
    public void openStateAlarmBtn() {
        alarmState = true;
//        btnAlarmState1.setText("防盗关闭");
        btnAlarmState1.setBackgroundResource(R.drawable.img_open);
    }

    //点击关闭报警按钮时按钮样式的响应操作
    public void closeStateAlarmBtn() {
        alarmState = false;
//        btnAlarmState1.setText("防盗开启");
        btnAlarmState1.setBackgroundResource(R.drawable.img_close);
    }

    public void msgSuccessArrived() {
        if (setManager.getAlarmFlag()) {
            showNotification("安全宝防盗系统已启动");
            openStateAlarmBtn();
        } else {
            showNotification("安全宝防盗系统已关闭");
            VibratorUtil.Vibrate(m_context, 500);
            closeStateAlarmBtn();
        }
    }

    //显示常驻通知栏
    public void showNotification(String text) {
        NotificationManager notificationManager = (NotificationManager) (m_context).getSystemService(
                m_context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(m_context, FragmentActivity.class);
        PendingIntent contextIntent = PendingIntent.getActivity(m_context, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(m_context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("安全宝")
                .setWhen(System.currentTimeMillis())
                .setTicker("安全宝正在设置~")
                .setOngoing(true)
                .setContentText(text)
                .setContentIntent(contextIntent)
                .build();
        notificationManager.notify(R.string.app_name, notification);
    }

    private static MKOLUpdateElement LocalCityelement() {
        ArrayList<MKOLUpdateElement> localCitylist = mkOfflineMap.getAllUpdateInfo();
        if (localCitylist != null) {
            for (MKOLUpdateElement element : localCitylist) {
                element.cityName.contains(localcity);
                return element;
            }
        }
        return null;
    }

    public static class NetWorkListen extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifiInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (wifiInfo != null && wifiInfo.isConnected()) {
                com.orhanobut.logger.Logger.d("没错，就是在WIFI环境下，我要看看获取到城市名字了没");
                downloadOfflinemap();
            } else {
                com.orhanobut.logger.Logger.d("没连WIFI");
            }
        }
    }

    public void refreshBindList1(){
        myHorizontalScrollView.list.clear();
        OtherCar.clear();
        BindedCarIMEI.setText(IMEIlist.get(0));
        HashMap<String, Object> map = null;
        for (int i = 1; i < IMEIlist.size(); i++) {
            map = new HashMap<>();
            map.put("whichcar",IMEIlist.get(i));
            map.put("img", R.drawable.othercar);
            myHorizontalScrollView.list.add(map);
            OtherCar.add(IMEIlist.get(i));
        }
    }
    //调整好位置  把正在绑定的IMEI号放在IMEIlist的第一个

    class MyBroadcastReceiver extends BroadcastReceiver {
        //接收到广播会被自动调用
        @Override
        public void onReceive (Context context, Intent intent) {
            //从Intent中获取action
            DeviceChangeHeadImage();
        }
    }

    private void showPopMenu() {
        View view = View.inflate(m_context, R.layout.popwindow_changecarpic, null);
        TextView tv_pop_FromGallery = (TextView) view.findViewById(R.id.tv_pop_FromGallery);
        TextView tv_pop_camera = (TextView) view.findViewById(R.id.tv_pop_camera);
        TextView tv_pop_cancel = (TextView) view.findViewById(R.id.tv_pop_cancel);
        tv_pop_FromGallery.setOnClickListener(this);
        tv_pop_camera.setOnClickListener(this);
        tv_pop_cancel.setOnClickListener(this);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mpopupWindow.dismiss();
            }
        });

        view.startAnimation(AnimationUtils.loadAnimation(m_context, R.anim.fade_in));
        LinearLayout ll_popup_carpic = (LinearLayout) view.findViewById(R.id.ll_popup_carpic);
        ll_popup_carpic.startAnimation(AnimationUtils.loadAnimation(m_context, R.anim.push_bottom_in));

        if(mpopupWindow==null){
            mpopupWindow = new PopupWindow(m_context);
            mpopupWindow.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
            mpopupWindow.setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
            mpopupWindow.setBackgroundDrawable(new BitmapDrawable());

            mpopupWindow.setFocusable(true);
            mpopupWindow.setOutsideTouchable(true);
        }

        mpopupWindow.setContentView(view);
        mpopupWindow.showAtLocation(headImage, Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
        mpopupWindow.update();
    }
}
