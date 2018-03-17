package com.coordinate_transformation.transformation;

import android.Manifest;
import android.content.Context;;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import net.sf.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public final String URL = "http://api.cellocation.com:81/cell/?";
    public String mcc = null;
    public String mnc = null;
    public Integer lac = null;
    public Integer cid = null;
    public String longitude = null;
    public String latitude = null;
    public String address = null;
    public static String result = null;
    public boolean clickonce = true;        //只点击一次
    //百度地图控件
    private MapView mMapView = null;
    // 百度地图对象
    private BaiduMap bdMap;
    MyLocationData.Builder locationBuilder = new MyLocationData.Builder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        avaiableLocation();

        //设置进度条
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.process_bar);
        progressBar.setVisibility(View.GONE);

        //设置GPS按钮不可见
        Button openGPS = (Button)findViewById(R.id.bt_openGPS);
        openGPS.setVisibility(View.GONE);

        initGPS();  //打开GPS

        //判断网络连接情况
        AlertDialog.Builder dialog1 = new AlertDialog.Builder(this);
        dialog1.setTitle("提醒： ");
        dialog1.setMessage("请打开网络连接");
        if(!NetworkUtil.isNetworkAvailable(this)){
            dialog1.show();
        }

        //点击按钮
        Button bt_showMess = (Button)findViewById(R.id.bt_showMess);
        bt_showMess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocationManager locationManager = (LocationManager) MainActivity.this
                        .getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)){
                    Toast.makeText(MainActivity.this,"暂未开启GPS,功能受限",Toast.LENGTH_SHORT).show();
                    //显示按钮设置为灰色，不可点击
                    Button bt = (Button)findViewById(R.id.bt_showMess);
                    bt.setEnabled(false);
                    bt.setBackgroundColor(Color.GRAY);

                    //显示openGPS按钮
                    Button openGPS = (Button)findViewById(R.id.bt_openGPS);
                    openGPS.setVisibility(View.VISIBLE);
                }
                else {
                    click_bt_showMess();
                }

            }
        });


        openGPS.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickopenGPS();
                LocationManager locationManager = (LocationManager) MainActivity.this.
                        getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)){
                    recreate();
                }

            }
        });
    }

    //判断位置权限是否打开
    public void avaiableLocation(){

        //-1是没有 0是已经开启
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if(PackageManager.PERMISSION_GRANTED == permissionCheck) {
            Log.i(TAG, "avaiableLocation: 已经开启了权限" );

        }
        else {
            Log.i(TAG, "avaiableLocation: 没有开启权限");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1
                    );
        }

        //Log.i(TAG, "avaiableLocation: 是否开启" + PackageManager.PERMISSION_GRANTED);
        //Log.i(TAG, "avaiableLocation: 是否开启" + PackageManager.PERMISSION_DENIED);

    }

    //打开GPS
    private void initGPS() {

        LocationManager locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);

        // 判断GPS模块是否开启，如果没有则跳转至设置开启界面，设置完毕后返回到首页
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder dialog3 = new AlertDialog.Builder(this);
            dialog3.setTitle("提醒：");
            dialog3.setMessage("为了更好的为您服务，请您打开您的GPS!");
            dialog3.setCancelable(false);

            //界面上左边按钮，及其监听
            dialog3.setNeutralButton("确定",
                    new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {

                            // 转到手机设置界面，用户设置GPS
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, 1); // 设置完成后返回到原来的界面
                            //重新加载活动
                            onResume();
                        }
                    });

            //界面上右边按钮，及其监听
            dialog3.setPositiveButton("取消", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    arg0.dismiss();
                    //显示提示
                    Toast.makeText(MainActivity.this,"定位未开启，部分功能受限",Toast.LENGTH_SHORT).show();

                    //显示按钮设置为灰色，不可点击
                    Button bt = (Button)findViewById(R.id.bt_showMess);
                    bt.setEnabled(false);
                    bt.setBackgroundColor(Color.GRAY);

                    //显示openGPS按钮
                    Button openGPS = (Button)findViewById(R.id.bt_openGPS);
                    openGPS.setVisibility(View.VISIBLE);
                }
            });

            dialog3.show();

        }

        return ;
    }

    //点击显示按钮之后的响应
    public void click_bt_showMess(){

        ProgressBar progressBar = (ProgressBar)findViewById(R.id.process_bar);
        progressBar.setVisibility(View.VISIBLE);

        if(clickonce){
            clickonce = false;
            getBaseStation();
            if(lac==null || cid == null){
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle("提醒：");
                dialog.setMessage("当前位置获取失败");
            }
            sendGet(URL, mcc, mnc, lac.toString(), cid.toString());
            SystemClock.sleep(500);
            if(result == null){
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("提醒：");
                dialog.setMessage("查询出错,即将退出程序");
                dialog.setCancelable(false);
                dialog.setNeutralButton("确定",
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                System.exit(0);
                            }
                        });
                dialog.show();
            }

            processResult();
            progressBar.setVisibility(View.GONE);
            show();
        }

    }

    //openGPS点击事件
    public void ClickopenGPS(){
        AlertDialog.Builder dialog3 = new AlertDialog.Builder(MainActivity.this);
        dialog3.setTitle("提醒：");
        dialog3.setMessage("打开您的GPS?");
        dialog3.setCancelable(false);

        //界面上左边按钮，及其监听
        dialog3.setNeutralButton("确定",
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                        // 转到手机设置界面，用户设置GPS
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, 1); // 设置完成后返回到原来的界面
                        //重新加载活动
                        //onResume();
                    }
                });
        dialog3.show();
    }

    //获得基站信息
    public void getBaseStation(){
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService
                (Context.TELEPHONY_SERVICE);
        //获得mcc、mnc
        String operator = telephonyManager.getNetworkOperator();
        mcc = operator.substring(0, 3);
        mnc = operator.substring(3);

        //中国移动和中国联通获取LAC、CID的方式
        GsmCellLocation location = (GsmCellLocation) telephonyManager.getCellLocation();
        lac = location.getLac();
        cid = location.getCid();
    }

    //连接定位网站API
    public void sendGet(final String url, final String mcc, final String mnc,
                        final String lac, final String cid){

        new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader in = null;
                try{
                    String urlNameString = url + "mcc=" + mcc + "&mnc=" + mnc +
                            "&lac=" + lac + "&ci=" + cid + "&output=json";
                    URL realUrl = new URL(urlNameString);
                    HttpURLConnection connection = (HttpURLConnection)realUrl.openConnection();
                    connection.connect();
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    result = in.readLine();

                }catch (Exception e){
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("提醒：");
                    dialog.setMessage("查询出错");
                }finally {
                    try{
                        if(in != null) in.close();
                    }catch (Exception e){
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                        dialog.setTitle("提醒：");
                        dialog.setMessage("程序出错");
                    }
                }
            }
        }).start();


    }

    //字符处理，获得经纬度以及地址
    public void processResult(){

        JSONObject jsonObject = JSONObject.fromObject(result);
        Map<String, String> map = JSONObject.fromObject(jsonObject);
        longitude = map.get("lon");
        latitude = map.get("lat");
        address = map.get("address");
    }

    //显示地图
    private void initMap(){

        locationBuilder.longitude(Double.parseDouble(longitude));    //经度
        locationBuilder.latitude(Double.parseDouble(latitude));     //纬度

        MyLocationData locationData = locationBuilder.build();

        mMapView = (MapView)findViewById(R.id.bmapview);
        bdMap = mMapView.getMap();
        bdMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);

        LatLng cenpt = new LatLng(Double.parseDouble(latitude),Double.parseDouble(longitude));
        MapStatus mapStatus = new MapStatus.Builder()
                .target(cenpt)
                .zoom(19)
                .build();
        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatus);

        bdMap.setMapStatus(mapStatusUpdate);        //移动到当前位置
        bdMap.setMyLocationEnabled(true);
        bdMap.setMyLocationData(locationData);

    }

    //显示
    public void show(){

        //显示基站信息
        TextView txt_mcc = (TextView) findViewById(R.id.txt_mcc);
        txt_mcc.append(mcc);
        TextView txt_mnc = (TextView) findViewById(R.id.txt_mnc);
        txt_mnc.append(mnc);

        TextView txt_lac = (TextView)findViewById(R.id.txt_lac);
        txt_lac.append(lac.toString());
        TextView txt_cid = (TextView)findViewById(R.id.txt_cid);
        txt_cid.append(cid.toString());

        TextView txt_lon = (TextView)findViewById(R.id.txt_longitude);
        txt_lon.append(longitude);
        TextView txt_lat = (TextView)findViewById(R.id.txt_latitude);
        txt_lat.append(latitude);
        TextView txt_address = (TextView)findViewById(R.id.txt_add);
        txt_address.append(address);

        initMap();

    }

/*
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }
    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        mMapView = null;
        super.onDestroy();
    }
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base); MultiDex.install(this);
    }
*/
}

class NetworkUtil {
    /**
     * 检查网络是否可用
     *
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {

        ConnectivityManager manager = (ConnectivityManager) context
                .getApplicationContext().getSystemService(
                        Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            return false;
        }

        NetworkInfo networkinfo = manager.getActiveNetworkInfo();

        if (networkinfo == null || !networkinfo.isAvailable()) {
            return false;
        }

        return true;
    }

}
