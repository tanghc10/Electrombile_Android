package com.xunce.electrombile.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.LogUtil;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.xunce.electrombile.R;
import com.xunce.electrombile.bean.TracksBean;
import com.xunce.electrombile.fragment.MaptabFragment;
import com.xunce.electrombile.manager.SettingManager;
import com.xunce.electrombile.manager.TracksManager;
import com.xunce.electrombile.manager.TracksManager.TrackPoint;
import com.xunce.electrombile.utils.useful.NetworkUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

public class TestddActivity extends Activity {

    private final String TAG = "RecordActivity";
    Button btnCuston;
    Button btnBegin;
    Button btnEnd;
    Button btnOK;
    Button btnOneDay;
    Button btnTwoDay;
    DatePicker dpBegin;
    DatePicker dpEnd;
    ListView m_listview;
    TracksManager tracksManager;
    List<Item> ItemList = new ArrayList<>();

    //查询的开始和结束时间
    Date startT;
    Date endT;
    //生成动态数组，加入数据
    ArrayList<HashMap<String, Object>> listItem;
    //数据适配器
//    SimpleAdapter listItemAdapter;
    ExpandableAdapter adapter;
    //用来获取时间
    static Calendar can;
    //查询失败对话框
    Dialog dialog;
    //管理应用数据的类
    SettingManager sm;
    SimpleDateFormat sdfWithSecond;
    SimpleDateFormat sdf;
    //需要跳过的个数
    int totalSkip;
    List<AVObject> totalAVObjects;
    //等待对话框
    private ProgressDialog watiDialog;

    List<Message> messageList;

    Item item;



    static int GroupPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_testdd);

       // setContentView(R.layout.activity_record);
        init();


        tracksManager = new TracksManager(getApplicationContext());
        can = Calendar.getInstance();
        sm = new SettingManager(this);

        sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+08:00"));

        sdfWithSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdfWithSecond.setTimeZone(TimeZone.getTimeZone("GMT+08:00"));

        totalAVObjects = new ArrayList<AVObject>();
//        initView();
        //setCustonViewVisibility(false);
       // m_listview.setVisibility(View.INVISIBLE);

        if (TracksBean.getInstance().getTracksData().size() != 0) {
            // Log.i(TAG, "TracksBean.getInstance().getTracksData().size()" + TracksBean.getInstance().getTracksData().size());
            m_listview.setVisibility(View.VISIBLE);
            tracksManager.clearTracks();
            tracksManager.setTracksData(TracksBean.getInstance().getTracksData());
            //Log.i(TAG, "TrackManager size:" + tracksManager.getTracks().size());
            updateListView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

//    private void initView(){
//        watiDialog = new ProgressDialog(this);
//        btnCuston = (Button)findViewById(R.id.btn_custom);
//        btnCuston.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if(!btnBegin.isShown()) {
//                    setCustonViewVisibility(true);
//                    m_listview.setVisibility(View.INVISIBLE);
//                }
//                else {
//                }
//            }
//        });



        //绑定Layout里面的ListView
//        m_listview = (ListView) findViewById(R.id.listview);
//
//        //生成动态数组，加入数据
//        listItem = new ArrayList<HashMap<String, Object>>();
//        //生成适配器的Item和动态数组对应的元素
//        listItemAdapter = new SimpleAdapter(this,listItem,//数据源
//                R.layout.listview_item,//ListItem的XML实现
//                //动态数组与ImageItem对应的子项
//                new String[] {"ItemTotalTime", "ItemStartTime", "ItemEndTime", "ItemDistance"},
//                //,两个TextView ID
//                new int[] {R.id.ItemTotalTime,R.id.ItemStartTime, R.id.ItemEndTime, R.id.ItemDistance}
//        );
//
//        //添加并且显示
//        m_listview.setAdapter(listItemAdapter);
//
//        //添加点击
//        m_listview.setOnItemClickListener(new OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
//                                    long arg3) {
//                MaptabFragment.trackDataList = tracksManager.getTrack(arg2);
//                //Toast.makeText(getApplicationContext(), "点击第" + arg2 + "个项目", Toast.LENGTH_SHORT).show();
//                finish();
//            }
//        });
//
//
//        dialog = new AlertDialog.Builder(this)
//                .setPositiveButton("继续查询",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.dismiss();
//
//                            }
//                        }).setNegativeButton("返回地图", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        finish();
//
//                    }
//                }).create();
//
//
////        tracksManager.setTracksData(TracksBean.getInstance().getTracksData());
////        updateListView();
//    }



    private void findCloud(final Date st, final Date et, int skip) {
        totalSkip += skip;
        final int finalSkip = totalSkip;
        AVQuery<AVObject> query = new AVQuery<AVObject>("GPS");
        String IMEI = sm.getIMEI();
        // Log.i(TAG, "IMEI+++++" + IMEI);
        query.setLimit(1000);
        query.whereEqualTo("IMEI", IMEI);
        query.whereGreaterThanOrEqualTo("createdAt", startT);
        query.whereLessThan("createdAt", endT);
        query.setSkip(finalSkip);
        watiDialog.setMessage("正在查询数据，请稍后…");
        watiDialog.show();
        query.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> avObjects, AVException e) {
                //  Log.i(TAG, e + "");
                if (e == null) {
                    if (avObjects.size() > 0)
                        //     Log.e(TAG,"oooooooooooooook--------" + avObjects.size());
                        if (avObjects.size() == 0) {
                            clearListViewWhenFail();
                            dialog.setTitle("此时间段内没有数据");
                            dialog.show();
                            watiDialog.dismiss();
                            return;
                        }
                    for (AVObject thisObject : avObjects) {
                        totalAVObjects.add(thisObject);
                    }
                    if (avObjects.size() >= 1000) {
                        //     Log.d(TAG, "data more than 1000");
                        findCloud(st, et, 1000);
                    }
                    if ((totalAVObjects.size() > 1000) && (avObjects.size() < 1000) ||
                            (totalSkip == 0) && (avObjects.size() < 1000)) {
                        tracksManager.clearTracks();

//                        //清楚本地数据
                        TracksBean.getInstance().getTracksData().clear();

                        //将leancloud里得到的数据写到track里去
                        tracksManager.setTranks(totalAVObjects);

//                        //更新本地数据
                        TracksBean.getInstance().setTracksData(tracksManager.getTracks());

                        updateListView();
                        watiDialog.dismiss();
//                        listItemAdapter.notifyDataSetChanged();
                    }

                } else {
                    clearListViewWhenFail();

                    dialog.setTitle("查询失败");
                    dialog.show();
                    watiDialog.dismiss();
                }
            }
        } );
    }

    private void clearListViewWhenFail() {
        tracksManager.clearTracks();
        updateListView();
//        listItemAdapter.notifyDataSetChanged();
    }

    private void updateListView(){
        //   Log.i(TAG, "update list View");
//        listItem.clear();
        //如果没有数据，弹出对话框
        if(tracksManager.getTracks().size() == 0){
            dialog.setTitle("此时间段内没有数据");
            dialog.show();
            return;
        }


        double start_latitude;
        double start_longitude;
        double end_latitude;
        double end_longitude;
        Date startdate = new Date();
        Message message;
        messageList = new ArrayList<Message>();



        for(int i=0;i<tracksManager.getTracks().size();i++)
        {
            //如果当前路线段只有一个点 不显示
            if(tracksManager.getTracks().get(i).size() == 1) {
                //tracksManager.getTracks().remove(i);
                continue;
            }
            ArrayList<TrackPoint> trackList = tracksManager.getTracks().get(i);

            //获取当前路线段的开始和结束点
            TrackPoint startP = trackList.get(0);
            start_latitude = startP.point.latitude;
            start_longitude=startP.point.longitude;
            startdate = startP.time;

            TrackPoint endP = trackList.get(trackList.size() - 1);
            end_latitude = endP.point.latitude;
            end_longitude = endP.point.longitude;
            message = new Message(String.valueOf(startdate.getHours())+String.valueOf(startdate.getMinutes()),
                    String.valueOf(start_latitude)+","+String.valueOf(start_longitude),
                    String.valueOf(end_latitude)+","+String.valueOf(end_longitude));
            messageList.add(message);

            //计算开始点和结束点时间间隔
            long diff = (endP.time.getTime() - startP.time.getTime()) / 1000 +1;
            long days = diff / (60 * 60 * 24);
            long hours = (diff-days*(60 * 60 * 24))/(60 * 60);
            double minutes = (diff-days*( 60 * 60 * 24.0)-hours*(60 * 60))/(60.0);
            int secodes = (int)((minutes - Math.floor(minutes)) * 60);


            //计算路程
            double distance = 0;
            for(int j = 0; j < trackList.size() - 1; j++){
                LatLng m_start = trackList.get(j).point;
                LatLng m_end = trackList.get(j +1).point;
                distance += DistanceUtil.getDistance(m_start, m_end);

            }
            int distanceKM = (int)(distance / 1000);
            int diatanceM = (int)(distance - distanceKM * 1000);
            //更新列表信息
//            HashMap<String, Object> map = new HashMap<String, Object>();
//            map.put("ItemTotalTime", "历时:" + days + "天" + hours +"小时" + (int)Math.floor(minutes) + "分钟" + secodes + "秒");
//            map.put("ItemStartTime", "开始时间:" + sdfWithSecond.format(startP.time));
//            map.put("ItemEndTime", "结束时间:" + sdfWithSecond.format(endP.time));
//            map.put("ItemDistance", "距离:" + distanceKM + "千米" + diatanceM + "米");
//            listItem.add(map);
        }
        ItemList.get(GroupPosition).setMessagelist(messageList);
        adapter.notifyDataSetChanged();

    }



    @Override
    protected void onStart() {
        super.onStart();
        if(!NetworkUtils.isNetworkConnected(this)){
            NetworkUtils.networkDialogNoCancel(this);
        }
    }

    private void init(){
        watiDialog = new ProgressDialog(this);
        List<Message> test = new ArrayList<Message>();
//        test.add(new Message("12:00", "location1", "location2"));

        //创建了一种pattern
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        String[] result = new String[7];
        //parse作用: 把String型的字符串转换成特定格式的date类型
        Calendar c = Calendar.getInstance();
        result[0] = sdf.format(c.getTime());
        for(int i=1;i<result.length;i++){
            c.add(Calendar.DAY_OF_MONTH, -1);
            result[i] = sdf.format(c.getTime());
        }

        for(int i = 0;i<7;i++)
        {
            ItemList.add(new Item(result[i],test,true));
        }

        ExpandableListView expandableListView = new ExpandableListView(this);
        adapter = new ExpandableAdapter(this,ItemList);
        expandableListView.setAdapter(adapter);
        addContentView(expandableListView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                Toast.makeText(TestddActivity.this, "你点击的是第"+(groupPosition+1)+"的菜单下的第"+(childPosition+1)+"选项", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    void GetHistoryTrack(int groupPosition)
    {
        GroupPosition = groupPosition;
        //由groupPosition得到对应的日期
        GregorianCalendar gcStart = new GregorianCalendar(TimeZone.getTimeZone("GMT+08:00"));
        gcStart.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        startT= gcStart.getTime();

        GregorianCalendar gcEnd = new GregorianCalendar(TimeZone.getTimeZone("GMT+08:00"));
        gcEnd.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH) + 1, 0, 0, 0);
        endT = gcEnd.getTime();
        totalSkip = 0;
        if(totalAVObjects != null)
            totalAVObjects.clear();
       switch (groupPosition) {
           //如果减出了负数  会不会有问题?
           case 0:
               gcStart.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
               startT= gcStart.getTime();
               gcEnd.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH) + 1, 0, 0, 0);
               endT = gcEnd.getTime();
               break;
           case 1:
               gcStart.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH)-1, 0, 0, 0);
               startT= gcStart.getTime();
               gcEnd.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
               endT = gcEnd.getTime();
               break;
           case 2:
               gcStart.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH)-2, 0, 0, 0);
               startT= gcStart.getTime();
               gcEnd.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH)-1, 0, 0, 0);
               endT = gcEnd.getTime();
               break;
           case 3:
               gcStart.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH)-3, 0, 0, 0);
               startT= gcStart.getTime();
               gcEnd.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH)-2, 0, 0, 0);
               endT = gcEnd.getTime();
               break;
           case 4:
               gcStart.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH)-4, 0, 0, 0);
               startT= gcStart.getTime();
               gcEnd.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH)-3, 0, 0, 0);
               endT = gcEnd.getTime();
               break;
           case 5:
               gcStart.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH)-5, 0, 0, 0);
               startT= gcStart.getTime();
               gcEnd.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH)-4, 0, 0, 0);
               endT = gcEnd.getTime();
               break;
           case 6:
               gcStart.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH)-6, 0, 0, 0);
               startT= gcStart.getTime();
               gcEnd.set(can.get(Calendar.YEAR), can.get(Calendar.MONTH), can.get(Calendar.DAY_OF_MONTH)-5, 0, 0, 0);
               endT = gcEnd.getTime();
               break;

       }
        findCloud(startT, endT, 0);
    }
}
