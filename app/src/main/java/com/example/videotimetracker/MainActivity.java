package com.example.videotimetracker;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private static final int MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 100;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化WebView
        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        
        // 注入JS接口
        webView.addJavascriptInterface(new AppUsageInterface(this), "AndroidInterface");
        
        // 检查并请求权限
        if (!checkUsageStatsPermission()) {
            requestUsageStatsPermission();
        }
        
        // 启动监控服务
        startService(new Intent(this, MonitoringService.class));
        
        // 加载HTML原型
        webView.loadUrl("file:///android_asset/src/index.html");
    }
    
    // 检查是否有使用统计权限
    private boolean checkUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
    
    // 请求使用统计权限
    private void requestUsageStatsPermission() {
        Toast.makeText(this, "请开启使用情况访问权限", Toast.LENGTH_LONG).show();
        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS) {
            if (checkUsageStatsPermission()) {
                // 权限已获取，刷新WebView
                webView.reload();
                
                // 启动监控服务
                startService(new Intent(this, MonitoringService.class));
            } else {
                Toast.makeText(this, "没有使用情况访问权限，应用功能将受限", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    // JavaScript接口，用于桥接WebView和原生Android代码
    public class AppUsageInterface {
        private Context context;
        
        public AppUsageInterface(Context context) {
            this.context = context;
        }
        
        // 获取应用使用统计
        @JavascriptInterface
        public String getAppUsageStats() {
            if (!checkUsageStatsPermission()) {
                return "{\"error\": \"没有权限获取使用统计\"}";
            }
            
            try {
                // 获取使用统计数据
                Map<String, UsageInfo> statsMap = getUsageStatistics();
                
                // 转换为JSON
                JSONArray appsArray = new JSONArray();
                
                // 关注的短视频应用列表及其包名
                String[][] videoApps = {
                        {"抖音", "抖", "com.ss.android.ugc.aweme"},
                        {"快手", "快", "com.smile.gifmaker"},
                        {"哔哩哔哩", "B", "tv.danmaku.bili"},
                        {"西瓜视频", "西", "com.ss.android.article.video"}
                };
                
                int totalUsageMinutes = 0;
                
                for (String[] appInfo : videoApps) {
                    String appName = appInfo[0];
                    String appIcon = appInfo[1];
                    String packageName = appInfo[2];
                    
                    JSONObject appObj = new JSONObject();
                    appObj.put("name", appName);
                    appObj.put("icon", appIcon);
                    
                    // 获取使用时间（分钟）
                    int usageMinutes = 0;
                    if (statsMap.containsKey(packageName)) {
                        usageMinutes = (int) (statsMap.get(packageName).timeInForeground / 60000L);
                    }
                    
                    appObj.put("usage", usageMinutes);
                    totalUsageMinutes += usageMinutes;
                    
                    appsArray.put(appObj);
                }
                
                // 创建最终的JSON对象
                JSONObject result = new JSONObject();
                result.put("apps", appsArray);
                result.put("todayUsage", totalUsageMinutes);
                
                // 获取当前设置的每日限制
                SharedPreferences prefs = context.getSharedPreferences("app_settings", MODE_PRIVATE);
                int dailyLimit = prefs.getInt("daily_limit", 180); // 默认3小时
                result.put("dailyLimit", dailyLimit / 60); // 转换为小时
                
                return result.toString();
                
            } catch (JSONException e) {
                e.printStackTrace();
                return "{\"error\": \"数据处理错误\"}";
            }
        }
        
        // 保存每日使用时间限制
        @JavascriptInterface
        public void saveDailyLimit(int limitHours) {
            // 将小时转换为分钟
            int limitMinutes = limitHours * 60;
            
            // 保存到SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences("app_settings", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("daily_limit", limitMinutes);
            editor.apply();
            
            // 重启服务以应用新限制
            context.stopService(new Intent(context, MonitoringService.class));
            context.startService(new Intent(context, MonitoringService.class));
            
            Toast.makeText(context, "已更新每日使用限制为" + limitHours + "小时", Toast.LENGTH_SHORT).show();
        }
        
        // 获取当天使用统计
        private Map<String, UsageInfo> getUsageStatistics() {
            UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            
            // 获取今天的开始时间
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startTime = calendar.getTimeInMillis();
            long endTime = System.currentTimeMillis();
            
            // 查询今天的使用统计
            List<UsageStats> stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
            
            // 处理使用数据
            Map<String, UsageInfo> usageMap = new HashMap<>();
            
            for (UsageStats usageStats : stats) {
                String packageName = usageStats.getPackageName();
                long timeInForeground = usageStats.getTotalTimeInForeground();
                
                // 只关心使用时间大于0的应用
                if (timeInForeground > 0) {
                    UsageInfo info = new UsageInfo();
                    info.packageName = packageName;
                    info.timeInForeground = timeInForeground;
                    usageMap.put(packageName, info);
                }
            }
            
            return usageMap;
        }
        
        // 使用统计信息类
        private class UsageInfo {
            String packageName;
            long timeInForeground;
        }
    }
} 