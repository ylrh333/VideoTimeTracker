// 应用数据模型
const appState = {
    dailyLimit: 3, // 小时
    todayUsage: 0, // 分钟
    apps: [
        { name: '抖音', usage: 95, icon: '抖' },
        { name: '快手', usage: 40, icon: '快' },
        { name: '哔哩哔哩', usage: 0, icon: 'B' }
    ],
    // 模拟后台运行，收集使用时间数据
    isMonitoring: false
};

// 检查是否在Android环境中运行
const isAndroid = () => {
    return typeof AndroidInterface !== 'undefined';
};

// 从本地存储加载数据
function loadData() {
    if (isAndroid()) {
        // 从Android原生代码获取数据
        try {
            const statsJson = AndroidInterface.getAppUsageStats();
            const stats = JSON.parse(statsJson);
            
            if (stats.error) {
                console.error("获取应用使用统计失败:", stats.error);
            } else {
                appState.todayUsage = stats.todayUsage || 0;
                if (stats.apps && stats.apps.length > 0) {
                    stats.apps.forEach((appStat, index) => {
                        if (index < appState.apps.length) {
                            appState.apps[index].usage = appStat.usage || 0;
                        }
                    });
                }
            }
        } catch (e) {
            console.error("获取Android应用使用统计出错:", e);
        }
    } else {
        // 在Web环境中，使用本地存储
        const savedData = localStorage.getItem('videoTimeTracker');
        if (savedData) {
            const parsed = JSON.parse(savedData);
            appState.dailyLimit = parsed.dailyLimit || 3;
            appState.todayUsage = parsed.todayUsage || 0;
            appState.apps = parsed.apps || appState.apps;
            
            // 检查是否是新的一天，如果是则重置数据
            const lastDate = localStorage.getItem('lastTrackedDate');
            const today = new Date().toLocaleDateString();
            if (lastDate !== today) {
                appState.todayUsage = 0;
                appState.apps.forEach(app => app.usage = 0);
                localStorage.setItem('lastTrackedDate', today);
            }
        } else {
            // 首次使用，初始化
            localStorage.setItem('lastTrackedDate', new Date().toLocaleDateString());
        }
    }
    
    updateUI();
}

// 将数据保存到本地存储
function saveData() {
    if (!isAndroid()) {
        localStorage.setItem('videoTimeTracker', JSON.stringify(appState));
    }
}

// 开始监控
function startMonitoring() {
    if (appState.isMonitoring) return;
    
    appState.isMonitoring = true;
    
    if (isAndroid()) {
        // Android环境中，更新频率由原生代码控制
        // 我们设置定时器每30秒刷新一次UI
        setInterval(() => {
            loadData();
            checkLimits();
        }, 30000);
    } else {
        // Web环境中的模拟监控代码
        // 模拟每分钟更新一次数据
        setInterval(() => {
            // 假设用户正在使用抖音
            if (Math.random() > 0.3) {
                appState.todayUsage += 1;
                appState.apps[0].usage += 1;
                
                saveData();
                updateUI();
                
                // 检查是否超出限制
                checkLimits();
            }
        }, 60000); // 实际运行每分钟检查一次
    }
}

// 重置统计数据
function resetStats() {
    if (confirm('确定要重置所有统计数据吗？')) {
        appState.todayUsage = 0;
        appState.apps.forEach(app => app.usage = 0);
        saveData();
        updateUI();
    }
}

// 保存限制设置
function saveLimit() {
    const limitInput = document.getElementById('limit');
    const newLimit = parseInt(limitInput.value);
    
    if (newLimit >= 0) {
        appState.dailyLimit = newLimit;
        saveData();
        updateUI();
        
        if (isAndroid()) {
            // 通知Android更新设置
            try {
                AndroidInterface.saveDailyLimit(newLimit);
            } catch (e) {
                console.error("保存Android设置出错:", e);
            }
        }
        
        alert('设置已保存');
    } else {
        alert('请输入有效的时间限制');
    }
}

// 检查是否超过限制
function checkLimits() {
    const limitInMinutes = appState.dailyLimit * 60;
    if (appState.todayUsage >= limitInMinutes) {
        // 在Web环境中显示警告
        if (!isAndroid()) {
            alert('您今日的短视频使用时间已达到限制！');
        }
    }
}

// 更新UI显示
function updateUI() {
    // 更新今日使用时长
    const hoursUsed = Math.floor(appState.todayUsage / 60);
    const minutesUsed = appState.todayUsage % 60;
    document.getElementById('today-usage').innerText = `${hoursUsed}小时${minutesUsed}分钟`;
    
    // 更新进度条
    const limitInMinutes = appState.dailyLimit * 60;
    const percentage = Math.min(100, (appState.todayUsage / limitInMinutes) * 100);
    document.querySelector('.progress').style.width = `${percentage}%`;
    
    // 更新限制显示
    document.querySelector('.stat-card div[style*="text-align: right"]').innerText = `每日限制: ${appState.dailyLimit}小时`;
    document.getElementById('limit').value = appState.dailyLimit;
    
    // 更新应用使用情况
    const appUsageElements = document.querySelectorAll('.app-usage');
    appState.apps.forEach((app, index) => {
        if (appUsageElements[index]) {
            const hoursApp = Math.floor(app.usage / 60);
            const minutesApp = app.usage % 60;
            appUsageElements[index].querySelector('.app-time').innerText = `${hoursApp}小时${minutesApp}分钟`;
        }
    });
}

// 事件监听
document.addEventListener('DOMContentLoaded', () => {
    // 加载保存的数据
    loadData();
    
    // 开始监控
    startMonitoring();
    
    // 重置按钮
    document.getElementById('reset-stats').addEventListener('click', resetStats);
    
    // 保存限制按钮
    document.getElementById('save-limit').addEventListener('click', saveLimit);
    
    // 选项卡切换（在实际应用中会有多个页面）
    const tabs = document.querySelectorAll('.tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            // 在实际应用中，这里会切换不同的页面视图
        });
    });
}); 