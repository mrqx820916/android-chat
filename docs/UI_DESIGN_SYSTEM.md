# 轻聊应用 - UI设计系统文档

## 概述

本文档定义了轻聊应用(Lightweight Chat)的完整UI设计系统,遵循Material Design 3规范,以微信绿(#22c55e)为主色调。

## 颜色系统

### 品牌色
```
主色: #22c55e (微信绿)
主色深: #16a34a
主色浅: #86efac
主色容器: #dcfce7
```

### 中性色
```
背景色: #ffffff
次背景: #f9fafb
文本主: #111827
文本次: #6b7280
文本提示: #9ca3af
边框: #e5e7eb
分割线: #f3f4f6
```

### 聊天专用色
```
发送气泡: #95EC69
接收气泡: #FFFFFF
聊天背景: #EDEDED
链接文本: #576B95
```

### 语义色
```
成功: #22c55e
错误: #ef4444
警告: #f59e0b
信息: #3b82f6
```

## 字体系统

### 字体大小
```
Display: 32sp  - 大标题
Headline: 24sp - 标题
Title: 20sp    - 小标题
Body Large: 16sp - 正文大
Body: 14sp     - 正文
Caption: 12sp  - 说明文字
```

### 字重
```
Regular: 400
Medium: 500
Bold: 700
```

## 间距系统

基于8dp基准网格:
```
spacing_1: 2dp
spacing_2: 4dp
spacing_3: 8dp
spacing_4: 12dp
spacing_5: 16dp
spacing_6: 20dp
spacing_7: 24dp
spacing_8: 32dp
spacing_9: 40dp
spacing_10: 48dp
```

## 圆角系统

```
小: 8dp   - 卡片、输入框
中: 12dp  - 按钮、对话框
大: 16dp  - 底部抽屉
超大: 24dp - 全屏模态
```

## 组件规范

### 按钮

**主按钮**
- 高度: 48dp
- 圆角: 12dp
- 背景色: #22c55e
- 文字颜色: #ffffff
- 文字大小: 16sp

**次要按钮(描边)**
- 高度: 48dp
- 圆角: 12dp
- 边框色: #22c55e
- 文字颜色: #22c55e

**文本按钮**
- 高度: 48dp
- 文字颜色: #22c55e
- 无背景、无边框

### 输入框

**高度**: 48dp
**圆角**: 12dp
**边框**: 1dp
**边框色(聚焦)**: #22c55e
**提示文字色**: #9ca3af

### 卡片

**圆角**: 12dp
**阴影**: 2dp
**内边距**: 16dp

### 列表项

**高度**: 72dp
**内边距**: 横向16dp,纵向12dp
**头像大小**: 40dp

### 消息气泡

**最大宽度**: 260dp
**圆角**: 8dp
**内边距**: 12dp
**发送背景**: #95EC69
**接收背景**: #FFFFFF

### 对话框

**圆角**: 12dp
**按钮**: 右对齐
**标题**: 左对齐,20sp粗体

## 图标系统

### 尺寸
```
小: 16dp
中: 24dp
大: 32dp
超大: 48dp
```

### 头像尺寸
```
小: 32dp
中: 40dp
大: 56dp
```

## Elevation系统

```
无: 0dp    - 基础元素
小: 2dp    - 卡片
中: 4dp    - 菜单、下拉框
大: 8dp    - 对话框、底部抽屉
```

## 应用图标

### 设计规范
- **风格**: 扁平化,圆形
- **背景**: 微信绿渐变
- **图标**: 白色聊天气泡
- **尺寸**: 512x512px (源文件)

### 适配分辨率
```
mdpi: 48x48dp
hdpi: 72x72dp
xhdpi: 96x96dp
xxhdpi: 144x144dp
xxxhdpi: 192x192dp
```

## 主题配置

### 浅色主题
```xml
<item name="colorPrimary">#22c55e</item>
<item name="colorOnPrimary">#ffffff</item>
<item name="android:colorBackground">#ffffff</item>
<item name="colorOnBackground">#111827</item>
<item name="colorSurface">#ffffff</item>
<item name="colorOnSurface">#111827</item>
```

### 暗色主题(可选)
```xml
<item name="colorPrimary">#86efac</item>
<item name="colorOnPrimary">#0f4f26</item>
<item name="android:colorBackground">#111827</item>
<item name="colorOnBackground">#f9fafb</item>
<item name="colorSurface">#1f2937</item>
<item name="colorOnSurface">#f9fafb</item>
```

## 响应式布局

### 断点
```
手机: <600dp
平板: 600dp-840dp
大屏: >840dp
```

### 边距
```
手机: 16dp
平板: 24dp
大屏: 32dp
```

## 动画

### 时长
```
快速: 150ms - 状态变化
标准: 250ms - 页面转场
慢速: 350ms - 复杂动画
```

### 缓动函数
```
标准: cubic-bezier(0.4, 0.0, 0.2, 1)
减速: cubic-bezier(0.0, 0.0, 0.2, 1)
加速: cubic-bezier(0.4, 0.0, 1, 1)
```

## 无障碍设计

### 触摸目标
- 最小尺寸: 48x48dp
- 推荐尺寸: 56x56dp

### 对比度
- 正常文字: 4.5:1
- 大文字(18sp+): 3:1

### 语义化
- 所有交互元素有contentDescription
- 重要信息不只依赖颜色传达
- 支持屏幕阅读器

## 验收清单

- [x] 主题色符合品牌规范(#22c55e)
- [x] 颜色系统完整定义
- [x] 字体样式和间距系统创建
- [x] 通用UI组件定义
- [x] 应用图标设计完成
- [x] 启动页布局创建
- [x] 符合Material Design 3规范
- [x] 对话/消息列表布局完成
- [ ] 深色模式适配(可选)

## 资源文件位置

```
app/src/main/res/
├── values/
│   ├── colors.xml          # 颜色定义
│   ├── dimens.xml          # 尺寸定义
│   ├── strings.xml         # 字符串资源
│   ├── themes.xml          # 主题配置
│   └── styles_*.xml        # 样式定义
├── layout/
│   ├── activity_splash.xml # 启动页
│   ├── item_conversation.xml    # 对话列表项
│   ├── item_message_sent.xml    # 发送消息
│   └── item_message_received.xml # 接收消息
└── drawable/
    └── ic_launcher_*.xml   # 应用图标
```

## 设计资源

- Material Design 3: https://m3.material.io/
- 颜色工具: https://m3.material.io/theme-builder
- 图标库: https://fonts.google.com/icons
