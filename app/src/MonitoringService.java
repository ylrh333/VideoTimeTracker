package com.example.videotimetracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MonitoringService extends Service {
    private static final String TAG = "MonitoringService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "video_time_tracker_channel";
    
    private Handler handler;
    private Timer timer;
    private UsageStatsManager usageStatsManager;
    private Map<String, Long> lastTimeInForeground;
    private Map<String, Long> dailyUsage;
    
    // 监控的短视频应用包名
    private final String[] VIDEO_APPS = {
        "com.ss.android.ugc.aweme",    // 抖音
        "com.kuaishou.nebula",         // 快手
        "tv.danmaku.bili"              // 哔哩哔哩
    };
    
    // 每日使用限制（分钟）
    private int dailyLimit = 180; // 默认3小时
    
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        lastTimeInForeground = new HashMap<>();
        dailyUsage = new HashMap<>();
        
        // 初始化上次使用时间
        for (String app : VIDEO_APPS) {
            lastTimeInForeground.put(app, 0L);
            dailyUsage.put(app, 0L);
        }
        
        // 从偏好设置中读取每日限制
        dailyLimit = getSharedPreferences("app_settings", MODE_PRIVATE)
                .getInt("daily_limit", 180);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 创建通知渠道
        createNotificationChannel();
        
        // 创建前台服务通知
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("短视频时长监控")
                .setContentText("正在监控短视频使用情况")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build();
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, notification);
        
        // 启动定期监控
        startMonitoring();
        
        return START_STICKY;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "短视频监控服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
    
    private void startMonitoring() {
        if (timer != null) {
            timer.cancel();
        }
        
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkAppUsage();
            }
        }, 0, 60000); // 每分钟检查一次
    }
    
    private void checkAppUsage() {
        // 获取当天开始时间
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();
        
        // 查询使用统计
        List<UsageStats> stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        
        if (stats == null || stats.isEmpty()) {
            Log.e(TAG, "无法获取使用统计，请确保已授予权限");
            return;
        }
        
        int totalUsageMinutes = 0;
        
        // 检查每个短视频应用
        for (UsageStats usageStats : stats) {
            String packageName = usageStats.getPackageName();
            
            // 只关注我们监控的短视频应用
            for (String videoApp : VIDEO_APPS) {
                if (packageName.equals(videoApp)) {
                    long timeInForeground = usageStats.getTotalTimeInForeground();
                    long previousTime = lastTimeInForeground.get(videoApp);
                    
                    // 更新使用时间
                    if (timeInForeground > previousTime) {
                        long additionalTime = timeInForeground - previousTime;
                        long currentDailyUsage = dailyUsage.get(videoApp) + (additionalTime / 60000);
                        dailyUsage.put(videoApp, currentDailyUsage);
                        lastTimeInForeground.put(videoApp, timeInForeground);
                        
                        Log.d(TAG, videoApp + " 使用时间增加: " + (additionalTime / 60000) + "分钟");
                    }
                    
                    // 累计总使用时间
                    totalUsageMinutes += dailyUsage.get(videoApp);
                    break;
                }
            }
        }
        
        // 检查是否超过每日限制
        if (totalUsageMinutes >= dailyLimit) {
            sendLimitExceededNotification(totalUsageMinutes);
        }
    }
    
    private void sendLimitExceededNotification(int totalMinutes) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        String timeText = hours + "小时" + minutes + "分钟";
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("短视频使用时间提醒")
                .setContentText("您今日已使用短视频" + timeText + "，已超过设定的" + (dailyLimit / 60) + "小时限制")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
        
        notificationManager.notify(NOTIFICATION_ID + 1, notification);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 