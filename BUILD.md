# 短视频使用时长监控应用构建指南

本文档提供了将短视频使用时长监控MVP原型转化为实际可运行的Android应用的步骤。

## 开发环境准备

1. 安装 [Android Studio](https://developer.android.com/studio)
2. 设置 Android SDK (通过Android Studio安装)
3. 配置JDK (推荐JDK 11或更高版本)

## 项目结构转换

我们需要将当前项目结构转换为标准的Android项目结构。

### 1. 创建新的Android项目

在Android Studio中:

1. 选择 `File > New > New Project`
2. 选择 `Empty Activity` 模板
3. 设置项目名称为 `VideoTimeTracker`
4. 设置包名为 `com.example.videotimetracker`
5. 语言选择 `Java`
6. 最低SDK版本设置为 `API 21: Android 5.0 (Lollipop)` 或更高
7. 点击 `Finish` 完成创建

### 2. 导入Web原型文件

1. 在Android Studio中，展开 `app` 目录
2. 右键点击 `src/main` 目录，选择 `New > Directory`，创建 `assets` 目录
3. 在 `assets` 目录下创建 `src` 文件夹
4. 将原型中的 `index.html` 和 `app.js` 复制到 `app/src/main/assets/src/` 目录下

### 3. 替换Java文件

1. 复制本项目中的 `MainActivity.java` 文件内容，替换Android Studio项目中的同名文件
2. 将本项目的 `MonitoringService.java` 文件复制到Android Studio项目的 `app/src/main/java/com/example/videotimetracker/` 目录下

### 4. 更新布局文件

1. 打开 `app/src/main/res/layout/activity_main.xml`
2. 用本项目中的 `activity_main.xml` 文件内容替换

### 5. 更新AndroidManifest.xml

1. 打开 `app/src/main/AndroidManifest.xml`
2. 用本项目中的 `AndroidManifest.xml` 文件内容替换

## 添加项目依赖

在 `app/build.gradle` 文件中添加以下依赖:

```gradle
implementation 'androidx.appcompat:appcompat:1.4.1'
implementation 'androidx.constraintlayout:constraintlayout:2.1.3'
implementation 'androidx.core:core:1.7.0'
```

## 构建和运行应用

1. 在Android Studio中，点击 `Build > Make Project` 来构建应用
2. 点击 `Run > Run 'app'` 在模拟器或已连接的设备上运行应用

> **注意**: 首次运行时，应用会请求用户开启 `使用情况访问权限`。此权限需要在系统设置中手动授予。

## 权限说明

应用需要以下权限:

1. `PACKAGE_USAGE_STATS` - 访问应用使用情况统计信息
2. `INTERNET` - 访问网络资源 
3. `FOREGROUND_SERVICE` - 运行前台服务

其中 `PACKAGE_USAGE_STATS` 是一种特殊权限，需要用户在系统设置中手动授予。应用会在首次运行时引导用户到相应的设置页面。

## 生成签名APK

准备发布应用时，需要创建签名版本:

1. 在Android Studio中选择 `Build > Generate Signed Bundle / APK`
2. 选择 `APK` 选项
3. 创建或选择一个已有的密钥库文件(.jks)
4. 填写密钥库凭据
5. 选择 `release` 构建类型
6. 点击 `Finish` 生成签名APK文件

生成的APK文件可以在 `app/release/` 目录下找到。

## 自定义应用

可以根据需要自定义以下部分:

1. 应用图标 - 替换 `res/mipmap` 目录下的文件
2. 应用名称 - 修改 `res/values/strings.xml` 中的 `app_name` 值
3. 应用主题颜色 - 修改 `res/values/colors.xml` 中的颜色值
4. 监控的应用列表 - 修改 `MainActivity.java` 和 `MonitoringService.java` 中的 `VIDEO_APPS` 数组

## 高级自定义

若要进一步改进应用:

1. 添加应用使用情况统计图表
2. 实现智能提醒功能
3. 增加用户账户系统
4. 添加家长控制模式
5. 实现应用锁定功能
