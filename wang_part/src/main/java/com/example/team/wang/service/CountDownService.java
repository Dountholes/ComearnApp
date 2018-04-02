package com.example.team.wang.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.team.wang.activity.OnClassActivity;
import com.example.team.wang.engine.fragment.class_main.ClassMainModel;
import com.example.team.wang.utils.PackageNameMonitor;
import com.example.team.comearnlib.receiver.BaseReceiver;
import com.example.team.comearnlib.utils.ConvertTools;
import com.example.team.comearnlib.utils.ToastTools;
import com.example.team.monitorlib.components.AppMonitor;

import java.util.Calendar;

public class CountDownService extends Service {

    private PackageNameMonitor mMonitor;

    public class CountDownBinder extends Binder{
        public CountDownService getService(){
            return CountDownService.this;
        }
    }

    public static final String TAG_GET_CALENDAR = "get_calendar";

    private CountDownBinder mBinder = new CountDownBinder();

    private Calendar mCalendar;

    private AlarmManager mManager;

    private StartCountDownForServiceReceiver mReceiver;

    private ClassMainModel mModel = new ClassMainModel(this);


    public CountDownService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initStartClassReceiver();
        mMonitor = new PackageNameMonitor();
        mMonitor.getMonitor().setDetectListener(new AppMonitor.DetectListener() {
            @Override
            public void afterDetect(Context context) {
                context.startActivity(new Intent(context, OnClassActivity.class));
            }
        });
        mMonitor.attach(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground(101, new Notification());

        initAlarmService(intent);

        if (mModel.getClassState()){
            mMonitor.startMonitor();
        }

        return START_STICKY;
    }

    private void initAlarmService(Intent intent) {

        mCalendar = (Calendar) intent.getSerializableExtra(TAG_GET_CALENDAR);

        if (mCalendar != null) {
            Intent i = new Intent(mReceiver.getAction());
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
            Log.d("CDS", pi.toString());

            mManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (mManager != null) {
                mManager.cancel(pi);
                mManager.setExact(AlarmManager.RTC_WAKEUP, mCalendar.getTimeInMillis(), pi);
                ToastTools.showToast(this, "本课堂将在   "
                        + mCalendar.get(Calendar.HOUR_OF_DAY) + ":" + mCalendar.get(Calendar.MINUTE)
                        + "开始");

            }
        }
    }

    private void initStartClassReceiver() {
        mReceiver = new StartCountDownForServiceReceiver();
        mReceiver.setmAction("start_count_down");
        registerReceiver(mReceiver, new IntentFilter(mReceiver.getAction()));
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        mMonitor.detach();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public Calendar getStopTimeCalendar(){
        return mCalendar;
    }

    public class StartCountDownForServiceReceiver extends BaseReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("SCDFSR", "On Start Count Receive Successfully");

            if (intent.getAction() != null && intent.getAction().equals(mAction)) {

                Intent activityIntent = new Intent(context, OnClassActivity.class);
                activityIntent.setAction("refresh_on_class_activity");

                if (mModel.getClassState()){
                    mModel.saveClassState(false);

                    context.startActivity(activityIntent);

                    ToastTools.showToast(CountDownService.this, "From Service");

                    mMonitor.getMonitor().setDetectListener(new AppMonitor.DetectListener() {
                        @Override
                        public void afterDetect(Context context) {
                            context.startActivity(new Intent(context, OnClassActivity.class));
                        }
                    });
                    mMonitor.stopMonitor();

                    stopSelf();
                }else {
                    mModel.saveClassState(true);

                    Intent serviceIntent = new Intent(context, CountDownService.class);
                    serviceIntent.putExtra(TAG_GET_CALENDAR, ConvertTools.constructFromTimeInMilis(mModel.getClassStopTime()));
                    startService(serviceIntent);

                    mMonitor.startMonitor();

                    context.startActivity(activityIntent);
                }
            }
        }
    }
}
