# 洞箫练习 Android App

这是一个简易原生 Android 洞箫练习 App 原型，重点覆盖：

- 长音练习：实时拾音、实时波形、基频检测、目标音偏差、连续命中时长、稳定度。
- 多调门支持：内置 G/F/E 调洞箫，支持筒音作 5/2/1/6。
- 技巧练习：长音、音阶、吐音、气震音、滑音、打音/叠音。
- 练习评分：每次停止拾音后生成 0 到 100 分评分和简短建议。
- 曲目练习：内置短曲，支持合成伴奏播放、简谱式动态谱面、默认本地图片谱库、图片谱 OCR 转动态谱、原图动态高亮、App 内开源曲库访问，以及从 HTTPS ABC 曲谱链接导入第三方曲目并保存到本地。

## 工程结构

```text
app/src/main/java/com/dongxiao/practice/
├── MainActivity.java              # 主界面、权限、实时刷新
├── audio/
│   ├── AudioAnalyzer.java         # Android AudioRecord 拾音
│   ├── PitchDetector.java         # YIN 基频检测
│   └── PitchResult.java
├── music/
│   ├── FingeringMode.java         # 筒音作几
│   ├── MusicTheory.java           # 频率、MIDI、cent 转换
│   ├── TargetNote.java
│   └── XiaoTuning.java            # 洞箫调门和目标音生成
├── practice/
│   ├── PracticeAnalyzer.java      # 练习指标分析
│   ├── PracticeMode.java
│   ├── PracticeScore.java         # 单次练习评分结果
│   ├── PracticeSessionScorer.java # 单次练习评分器
│   └── PracticeStats.java
├── song/
│   ├── AbcSongImporter.java       # ABC 曲谱解析
│   ├── ImageScore.java            # 图片谱数据
│   ├── ImageScoreMarker.java      # 图片谱高亮坐标
│   ├── ImageScoreRepository.java  # 默认本地图片谱库
│   ├── JianpuTextCatalog.java     # 图片谱 OCR 转谱目录
│   ├── JianpuTextSongImporter.java # 简谱 OCR 文本解析
│   ├── JianpuTextSource.java
│   ├── LocalSongStore.java        # 本地导入曲目存储
│   ├── OnlineSongCatalog.java     # 在线开源曲库目录
│   ├── OnlineSongResource.java
│   ├── PracticeSong.java          # 曲目数据
│   ├── SongNote.java
│   ├── SongPlayer.java            # 合成伴奏播放器
│   └── SongRepository.java        # 内置曲库
└── ui/
    ├── DynamicScoreView.java      # 动态简谱
    ├── TunerView.java             # 音准表
    └── WaveformView.java          # 实时波形
```

## 构建

本机已按最快命令行方案配置项目内工具链：

- Android SDK：`.tools/android-sdk`
- JDK 17：`.tools/jdk-17.0.20+8`
- Gradle：`.tools/gradle-8.10.2`

重新打包 debug APK：

```bash
scripts/build-debug.sh
```

APK 输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

手机通过 USB 连接、开启开发者模式和 USB 调试，并在手机上确认授权后，可执行：

```bash
scripts/install-debug.sh
```

也可以用 Android Studio 打开本目录，安装：

- Android SDK 35
- Android Gradle Plugin 8.7.3
- JDK 17

如果使用系统级 Android SDK 和 Gradle，也可执行：

```bash
gradle assembleDebug
```

如果使用 Android Studio，它会自动下载缺失的 Gradle/AGP 依赖。

## 使用方式

1. 在首页选择练习项目，例如 `长音` 或 `气震音`，进入对应练习页。
2. 在练习页选择洞箫调门，例如 `F调`。
3. 选择筒音作法，例如 `筒音作5`。
4. 选择目标音，或保持 `自动匹配最近目标音`。
5. 点击 `开始拾音`，授权麦克风权限。
6. 手机麦克风距离洞箫约 20 到 50 厘米，观察音准表、实时波形和指标。
7. 点击 `停止拾音` 后查看本次练习评分。

曲目练习：

1. 在首页选择 `曲目练习`。
2. 选择内置曲目，例如 `小星星片段`。
3. 点击 `播放伴奏`，App 会播放合成伴奏并同步高亮动态简谱。
4. 可以在 `在线开源曲库` 中选择 ABC 资源，点击 `添加到本地` 后追加到曲目列表。
5. 也可以输入可直接下载 ABC 文本的 HTTPS 链接，点击 `导入` 后追加到曲目列表。
6. 导入曲目会保存在本地，下次打开 App 仍可选择。
7. `曲目选择` 中的 `图片转谱 · ...` 曲目来自本地图片谱 OCR 转换，播放时会在原图片谱上动态高亮当前音符。
8. 可以在 `本地图片谱` 中选择内置图片谱；选择图片谱会自动切换到对应的 `图片转谱 · ...` 动态曲目，多页曲谱可用 `上一页`、`下一页` 翻页查看。
9. 跟随当前高亮音符练习，点击 `停止伴奏` 可中断播放。

## 指标解释

- `目标偏差`：当前检测频率相对目标音的 cent 偏差；0 cent 最准。
- `连续命中`：长音偏差进入 ±25 cent 后的连续持续时间。
- `稳定度`：最近约 2 秒内音高偏差的标准差，越小越稳。
- `起音次数`：吐音练习中明显音量起点的计数。
- `气震频率/深度`：最近约 2.2 秒内音高周期性波动的估算。
- `最近滑动`：最近约 1.5 秒内音高向上或向下移动的趋势。
- `快速音高波动`：打音/叠音练习中短时间音高跳变的计数。
- `本次评分`：停止拾音后生成，综合整段练习的平均音准偏差、稳定度、有效发声时长、命中率和当前练习项目的专项指标。

## 当前边界

- 这是本地实时分析原型，没有账号、真实录音伴奏、历史统计。
- 曲目练习目前使用内置短曲、本地合成伴奏、在线 ABC 曲库和 ABC 文本导入。导入功能需要第三方链接直接返回标准 ABC 文本；复杂装饰音、复声部、多歌词等高级 ABC 语法只做基础兼容。
- 默认本地图片谱库使用 `app/src/main/assets/score_images/` 中的图片资源，同时使用 `app/src/main/assets/jianpu_text/` 中的 OCR 文本生成 `图片转谱 · ...` 动态曲目，并使用 `app/src/main/assets/score_layout/` 中的坐标数据在原图上高亮当前音符。OCR 转谱和坐标匹配可能存在错音、漏音、节奏误差或位置偏差，后续应以原图片谱为准逐首校对。
- 在线曲库当前仅 ABC 资源可直接添加到本地；MusicXML、MIDI、PDF、LilyPond 资源先保留访问入口，待后续解析器支持后再开放直接导入。
- 技巧练习目前是声学特征辅助判断，不是专业教师级动作判分。
- 练习评分用于自查趋势，不等同于专业考级或教师评价。
- 洞箫调门按常见“筒音作 5 时的 1”理解。例如 `F调` 在 `筒音作5` 时，低音 5 是 C4，低音 6 是 D4，中音 1 是 F4。如果你使用的指法表不同，只需要调整 `XiaoTuning`。
- 基频检测依赖手机麦克风质量和环境噪声；真机测试时建议在安静房间内验证。

## 图标来源

当前桌面图标基于 Twitter Emoji `Flute` SVG 改造成 Android launcher icon，并叠加了本项目原创竹叶背景装饰。

- Source: https://svgicons.com/icon/312002/flute
- License: CC BY 4.0
