# 短视频使用时长监控 MVP

这是一个用于监控短视频应用使用时长的安卓应用 MVP 原型。

## 项目概述

该应用旨在帮助用户追踪和管理他们在短视频应用（如抖音、快手、哔哩哔哩等）上花费的时间，并可以设置每日使用限制。

## 功能特点

- 📊 实时统计短视频应用的使用时长
- ⏱️ 设置每日使用时间限制
- 📱 分应用展示使用情况
- 🔔 超出限制时发出提醒
- 📆 每日自动重置统计数据

## MVP 原型说明

当前项目是一个基于 HTML/CSS/JavaScript 的原型演示，展示了应用的基本 UI 和交互逻辑。要将其转化为真正的安卓应用，可以使用以下几种方法：

### 方法 1: 使用 WebView 包装

1. 创建一个新的安卓项目
2. 添加一个 WebView 组件来加载原型文件
3. 实现原生与 JavaScript 的桥接（JavascriptInterface）来获取真实的应用使用数据

```java
// 简单的 WebView 实现示例
WebView webView = findViewById(R.id.webView);
webView.getSettings().setJavaScriptEnabled(true);
webView.addJavascriptInterface(new AppUsageInterface(), "AppUsage");
webView.loadUrl("file:///android_asset/index.html");

// 桥接类
public class AppUsageInterface {
    @JavascriptInterface
    public String getAppUsageStats() {
        // 在这里实现读取真实应用使用情况的代码
        // 使用 UsageStatsManager API
        return jsonString; // 返回 JSON 格式的使用数据
    }
}
```

### 方法 2: 使用 React Native 重构

1. 创建一个新的 React Native 项目
2. 基于原型的设计重构 UI 组件
3. 使用 React Native 模块访问安卓的 UsageStatsManager API

```javascript
// 示例 React Native 组件
import { UsageStats } from 'react-native-usage-stats';

function App() {
  const [stats, setStats] = useState(null);
  
  useEffect(() => {
    async function fetchStats() {
      const granted = await UsageStats.requestUsagePermission();
      if (granted) {
        const usageStats = await UsageStats.getUsageStats();
        setStats(usageStats);
      }
    }
    
    fetchStats();
  }, []);
  
  // UI 渲染
}
```

### 方法 3: 原生安卓开发

1. 创建一个新的安卓项目
2. 使用 Kotlin/Java 和 XML 实现 UI
3. 直接使用 UsageStatsManager API 获取应用使用数据

```kotlin
// Kotlin 示例代码
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        if (checkUsagePermission()) {
            val stats = getAppUsageStats()
            updateUI(stats)
        } else {
            requestPermission()
        }
    }
    
    private fun getAppUsageStats(): Map<String, Long> {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        // 获取指定时间段的使用统计
        // ...
    }
}
```

## 核心技术实现要点

### 获取应用使用统计

在真实的安卓应用中，需要使用 `UsageStatsManager` API 来获取应用使用时长。这需要特殊权限：

1. 在 AndroidManifest.xml 中添加权限:
   ```xml
   <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" 
        tools:ignore="ProtectedPermissions" />
   ```

2. 引导用户启用权限:
   ```kotlin
   private fun requestPermission() {
       startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
   }
   ```

### 后台监控服务

为了持续监控应用使用情况，需要实现一个前台服务：

```kotlin
class MonitoringService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 创建前台通知
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // 开始周期性检查
        startMonitoring()
        
        return START_STICKY
    }
    
    private fun startMonitoring() {
        // 实现周期性检查逻辑
    }
}
```

## 后续开发建议

1. 实现用户配置文件，支持多用户
2. 添加详细的使用统计图表
3. 实现智能提醒和建议功能
4. 集成屏幕时间管理功能
5. 添加家长控制模式

## 技术要求

- Android 5.0+ (API 21+)，因为 UsageStatsManager 在 API 21 中引入
- 需要 PACKAGE_USAGE_STATS 权限，这是一个特殊权限，需要用户在设置中手动授予

## 许可证

MIT 