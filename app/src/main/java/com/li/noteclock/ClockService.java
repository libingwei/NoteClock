package com.li.noteclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

public class ClockService extends Service {

    public static Context context;
    public static AppWidgetManager appWidgetManager;
    public static RemoteViews remoteViews;
    private SimpleDateFormat df = new SimpleDateFormat("HHmmss");
    public static final String M_ALARM_ACTION="com.li.noteclock.Alarm";
    public static final long HOUR_MILLIS=3600*1000;

    public static AlarmManager alarmManager;
    public static PendingIntent pi;

    private MediaPlayer mp;
    private boolean isWakeUp=true;

    private  PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock=null;
    private Handler mTimeHandle;

    // 数字图片的ID
    private int[] numberIcon = new int[] { R.mipmap.red_num_0,
            R.mipmap.red_num_01, R.mipmap.red_num_02,
            R.mipmap.red_num_03, R.mipmap.red_num_04,
            R.mipmap.red_num_05, R.mipmap.red_num_06,
            R.mipmap.red_num_07, R.mipmap.red_num_08,
            R.mipmap.red_num_09,};
    // 用于显示数字的ImageView的ID
    private int[] numberView = new int[] { R.id.hour01, R.id.hour02,
            R.id.minute01, R.id.minute02 };

    public ClockService() {

    }

    // 覆盖基类的抽象方法
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 在本服务创建时将监听系统时间的BroadcastReceiver注册
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("service", "--service created--");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK); // 时间的流逝
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED); // 时间被改变，人为设置时间
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(M_ALARM_ACTION);
        registerReceiver(boroadcastReceiver, intentFilter);

        mp = MediaPlayer.create(this, R.raw.music);
        mTimeHandle= new Handler();

        //获取AlarmManager对象:
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent();
        intent.setAction(M_ALARM_ACTION);
        pi = PendingIntent.getBroadcast(ClockService.this, 0, intent, 0);//方式不同用不同方法
        setAlarm();

        Log.d("onCreate","setAlarm");
    }

    static void setAlarm(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar offsetTime = Calendar.getInstance();
        Log.d("curtime",sdf.format(offsetTime.getTime()));

        offsetTime.clear(Calendar.MINUTE);
        offsetTime.clear(Calendar.SECOND);
        offsetTime.clear(Calendar.MILLISECOND);
        offsetTime.add(Calendar.HOUR_OF_DAY,1);
        Log.d("setAlrm",sdf.format(offsetTime.getTime()));

        alarmManager.cancel(pi);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //参数2是开始时间、参数3是允许系统延迟的时间
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, offsetTime.getTimeInMillis(), 1000, pi);
        } else {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, offsetTime.getTimeInMillis(), HOUR_MILLIS, pi);
        }


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("service", "--service started--");
        updateUI(); // 开始服务前先刷新一次UI
        return START_STICKY;
    }

    // 在服务停止时解注册BroadcastReceiver
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(boroadcastReceiver);
        mp.stop();
        mp.release();
        alarmManager.cancel(pi);
    }

    // 用于监听系统时间变化Intent.ACTION_TIME_TICK的BroadcastReceiver，此BroadcastReceiver须为动态注册
    private BroadcastReceiver boroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//			Log.e("time received", "--receive--");
            String action = intent.getAction();

            if (action.equals(Intent.ACTION_TIME_TICK)) {
                updateUI();
            }
            else if ( action.equals(Intent.ACTION_TIME_CHANGED)) {
                updateUI();
                setAlarm();
            }
            else if (action.equals(Intent.ACTION_SCREEN_ON)){
                // TODO: 2016/12/4 开屏
                Log.d("Receiver","屏幕开启");
            }
            else if (action.equals(Intent.ACTION_SCREEN_OFF)){
                // TODO: 2016/12/4 锁屏后处理
                Log.d("Receiver","屏幕关闭");
            }
            else if (action.equals(M_ALARM_ACTION)) {
                Log.d("Receiver","整点报时");
                //播放声音
                mp.start();
                if(isWakeUp) {
                    // 点亮亮屏
                    mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
                    mWakeLock = mPowerManager.newWakeLock(
                                PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.SCREEN_DIM_WAKE_LOCK, "Tag");

                    mWakeLock.acquire();
                    mTimeHandle.postDelayed(new Runnable(){
                        public void run(){
                            mWakeLock.release();
                            mWakeLock=null;
                            setAlarm();
                        }
                    }, 6*1000);
                }


            }
        }
    };

    // 根据当前时间设置小部件相应的数字图片
    private void updateUI() {

        String timeString = df.format(new Date());
        int num;
        for (int i = 0; i < numberView.length; i++) {
            num = timeString.charAt(i) - 48;
            remoteViews.setImageViewResource(numberView[i], numberIcon[num]);
        }

        // 将AppWidgetProvider的子类包装成ComponentName对象
        ComponentName componentName = new ComponentName(context,
                ClockWidget.class);
        // 调用AppWidgetManager将remoteViews添加到ComponentName中
        appWidgetManager.updateAppWidget(componentName, remoteViews);

    }
}
