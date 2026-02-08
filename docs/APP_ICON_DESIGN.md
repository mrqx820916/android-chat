# 轻聊应用图标设计说明

## 设计规范

### 品牌标识
- **应用名称**: 轻聊
- **品牌色**: #22c55e (微信绿)
- **设计风格**: Material Design 3

### 图标设计元素

#### 主图标
1. **背景圆形**: #22c55e 微信绿色圆形背景
2. **聊天气泡**: 白色聊天气泡图形，象征即时通讯
3. **装饰元素**: 三个小圆点代表消息内容

#### 尺寸规格
- **自适应图标**: 108x108dp (遵循Android自适应图标规范)
  - 前景层: ic_launcher_foreground.xml
  - 背景层: ic_launcher_background.xml

- **传统图标**:
  - mdpi: 48x48dp
  - hdpi: 72x72dp
  - xhdpi: 96x96dp
  - xxhdpi: 144x144dp
  - xxxhdpi: 192x192dp

#### 启动图标
- **尺寸**: 192x192dp
- **元素**: 放大版的主图标元素
- **用途**: 启动画面中心显示

## 图标生成方案

### 方案一: Android Studio生成
1. 在Android Studio中: 右键res文件夹 → New → Image Asset
2. 选择Foreground Layer，使用ic_launcher_foreground.xml
3. 选择Background Layer，使用ic_launcher_background.xml
4. 勾选"Adaptive and Legacy Icons"
5. 完成后自动生成所有尺寸的PNG图标

### 方案二: 在线工具生成
推荐使用以下在线工具:
- **Android Asset Studio**: https://romannurik.github.io/AndroidAssetStudio/
- **AppIconGenerator**: https://appicon.co/

操作步骤:
1. 上传SVG或PNG源文件
2. 选择自适应图标模板
3. 设置背景色为 #22c55e
4. 生成并下载所有尺寸

### 方案三: 命令行工具生成
使用ImageMagick或其他图像处理工具批量生成:

```bash
# 安装ImageMagick
# Windows: choco install imagemagick
# macOS: brew install imagemagick
# Linux: sudo apt-get install imagemagick

# 从SVG生成不同尺寸的PNG
convert -background none -resize 48x48 icon.svg mipmap-mdpi/ic_launcher.png
convert -background none -resize 72x72 icon.svg mipmap-hdpi/ic_launcher.png
convert -background none -resize 96x96 icon.svg mipmap-xhdpi/ic_launcher.png
convert -background none -resize 144x144 icon.svg mipmap-xxhdpi/ic_launcher.png
convert -background none -resize 192x192 icon.svg mipmap-xxxhdpi/ic_launcher.png
```

## SVG源文件

### 主图标SVG
```svg
<svg width="512" height="512" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">
  <!-- 背景圆形 -->
  <circle cx="256" cy="256" r="240" fill="#22c55e"/>

  <!-- 聊天气泡 -->
  <path d="M128,160h256c24,0 48,24 48,48v96c0,24 -24,48 -48,48h-32v32l-48,-32h-176c-24,0 -48,-24 -48,-48v-96c0,-24 24,-48 48,-48z" fill="#ffffff"/>

  <!-- 装饰圆点 -->
  <circle cx="200" cy="256" r="16" fill="#dcfce7"/>
  <circle cx="256" cy="256" r="16" fill="#dcfce7"/>
  <circle cx="312" cy="256" r="16" fill="#dcfce7"/>
</svg>
```

## 验收标准

### 视觉质量
- [ ] 图标清晰，无锯齿
- [ ] 颜色准确，符合品牌色 #22c55e
- [ ] 在不同背景下可见性好
- [ ] 在不同尺寸下可识别

### 技术规范
- [ ] 包含所有必需尺寸 (mdpi到xxxhdpi)
- [ ] 自适应图标配置正确
- [ ] 图标文件命名规范
- [ ] 在不同Android版本上显示正常

### 测试清单
- [ ] 在Android 8.0+设备上测试自适应图标
- [ ] 在Android 8.0以下设备上测试传统图标
- [ ] 在不同启动器上测试显示效果
- [ ] 在不同屏幕密度上测试清晰度

## 注意事项

1. **自适应图标**: Android 8.0+使用自适应图标，需要同时提供前景和背景层
2. **圆角处理**: 不同设备的启动器可能应用不同的遮罩，图标内容应避开边缘
3. **安全区域**: 图标重要内容应保持在中心66x66dp区域内
4. **视觉平衡**: 确保图标在不同背景下都有良好的视觉效果

## 资源文件位置

```
android-chat/app/src/main/res/
├── mipmap-*/ic_launcher.png
├── mipmap-*/ic_launcher_round.png
└── drawable/
    ├── ic_launcher_foreground.xml
    ├── ic_launcher_background.xml
    └── ic_splash.xml
```

## 更新历史

- 2025-02-08: 创建初始设计文档和SVG资源
