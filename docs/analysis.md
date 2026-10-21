# RunBeat APK 分析

分析对象：`runbeat.apk`（工作区根目录），包名 `com.voyago.runbeat`，版本 `2.2.6`（versionCode 37）。以下内容来自 APK 静态资源、Dart AOT 字符串、Java 外壳反编译，以及在 Android 17 模拟器上的实际运行观察。

## 1. 技术栈与包体

| 项目 | 结果 |
| --- | --- |
| 包名 / 版本 | `com.voyago.runbeat` / 2.2.6 (37) |
| 应用名 | RunBeat跑步节拍器 |
| 框架 | Flutter（`libflutter.so`、`libapp.so`、Flutter 资源清单） |
| minSdk / targetSdk / compileSdk | 24 / 36 / 36 |
| APK 大小 | 121,644,187 字节（约 121 MB） |
| 主要原生库 | Flutter、fluidSynth、sndfile、Oboe、Opus/Vorbis/FLAC、Umeng 统计/崩溃 |
| 最大资源 | `assets/flutter_assets/assets/sounds/RunBeat_Master.sf2`，75,015,170 字节 |
| Java 外壳 | `RunBeatApplication`、`MainActivity`、`AudioService`、微信/支付/统计 SDK 等 |

### 权限

- `INTERNET`、`ACCESS_NETWORK_STATE`、`ACCESS_WIFI_STATE`
- `WAKE_LOCK`
- `FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- `com.google.android.gms.permission.AD_ID`、`freemme.permission.msa`（广告/统计相关）

### 关键依赖/服务

- `com.ryanheise.audioservice.AudioService`：`mediaPlayback` 类型前台服务 + 媒体按键接收器。
- `just_audio` / `audio_service` 音频播放栈。
- `fluidsynth` + `RunBeat_Master.sf2`：本地 SoundFont 合成。
- TTS/语音播报能力（APK 字符串含 `TextToSpeech`、倒计时与训练完成播报逻辑）。
- 微信登录/支付、支付宝、Umeng、应用内购买与订阅页。

## 2. 实际界面与流程

在模拟器上首次启动依次出现：

1. Android 16 KB 兼容性提示（原 APK 的部分 `.so` 未按 16 KB 对齐）。
2. **User Agreement**（使用协议）弹窗：Disagree / Agree & Continue。
3. **Choose Mode**：Cadence Mix（New）、Improv Groove（Classic）、Pure Metronome，说明只有 Pure Metronome 可与其它音频应用同时播放。
4. 首页 **Daily Workout**：
   - 顶部 `RunBeat · PRO` 与设置抽屉。
   - `Daily Workout` / `+ New Plan` 两个入口。
   - `Segments · 35:00`、左滑删除提示。
   - 分段卡片：名称、时长、SPM；底部 `Add Segment`、`Change Plan`。
   - 耳机提示、圆形 `BEATS` 按钮、大号 `START`。
5. **Choose Training Type**：Blank Plan、Easy、Aerobic、Progressive、Tempo、Intervals、Pyramid、Fartlek；每种有图标、二行说明和配置页。
6. **训练中**：
   - 段落名称、剩余时间、分段进度线和节点、下一段。
   - 超大 SPM 数字、拍点指示。
   - Voice Prompts 开关、Double Volume 1x–4x、Sound Effects 选择。
   - 音量滑杆。
   - Pause、Next；Reset、Hold to stop、Cadence。
   - Cadence 弹窗：SPM + 滑杆 + 加减按钮 + Done。
7. **完成**：结束页/提示。
8. **设置抽屉**：Upgrade to PRO、Sign in、Invite Friends、Language、Encourage Us、Privacy Policy、Terms of Service、About。

### 训练类型

| 类型 | 副标题 | 含义 |
| --- | --- | --- |
| Blank Plan | Custom segments | 空白计划 |
| Easy | Daily recovery | 每日恢复 |
| Aerobic | Base endurance | 基础耐力 |
| Progressive | Stepped speed | 阶梯提速 |
| Tempo | Threshold pace | 阈值配速 |
| Intervals | Break limits | 间歇 |
| Pyramid | Rise & Fall | 金字塔升降 |
| Fartlek | Random speed | 随机变速 |

参数包括：主训练时长、热身/冷身开关、巡航 SPM、间歇重复组数、冲刺时长、恢复时长、最高 SPM 等。

## 3. 节拍声音

APK 字符串与运行界面显示 7 种音效：

| 音效 | 说明 |
| --- | --- |
| Electronic Beep · Soft | A3 + F3 |
| Electronic Beep · Mid | C4 + A3（默认） |
| Electronic Beep · Bright | E4 + C4 |
| Sine Beep · Soft | C5 + A4 |
| Sine Beep · Mid | C6 + A5 |
| Sine Beep · High | C7 + A6 |
| Wood Block | L: High / R: Low |

其它与音频相关的 APK 字符串：

- `Double Volume`、`metronomeVolumeMultiplier`
- `volume_metronome`、`volume_music`
- `Metronome Bell`、`Metronome Click`、`Agogo Bells`、`Claves`、`Cowbell`
- `USING METRONOME STRATEGY`
- `_playBeat() ... ensureSessionActive`、`harmonic_beat_trial_*`
- `/temp_metronome_`、`/bpm_transition_beep.mid`、`/plan_completion_beep.mid`

说明原版不仅有纯节拍，还会根据模式生成/拼接 MIDI、SoundFont 音乐与过渡提示音。4/4 拍下第一拍重音，是界面拍点与听感的共同基础。

## 4. 后台/通知/语音

- 清单声明 `AudioService` 为 `mediaPlayback` 前台服务，带 `MEDIA_BUTTON` 过滤器；这解释了锁屏/切后台仍持续播放。
- `WAKE_LOCK` + 忽略电池优化提示用于长距离跑步续航。
- 通知栏可显示播放状态并触发暂停/继续/停止。
- Dart 字符串含大量 TTS、倒计时、训练完成播报文案；原版还支持无网络时的内置语音。

## 5. 商业化与云能力

从字符串可以确认：

- PRO 订阅、终身会员、试用配额：`Metronome trial quota reached`、`Preset Plan trial quota reached`、`Cadence Mix trial quota reached`、`Clear metronome, unlimited`、`Preset & custom plans, unlimited`。
- 登录：短信、微信；服务端 `https://runbeat.voyago.cn`。
- 云端步频歌单（Cadence Mix）：`https://runbeat.voyago.cn/playlist`；字符串中出现 `songs/Broken Music Box.m4a`、`songs/December Warmth.m4a` 等曲目名（资源未随 APK 打包，可能由云端缓存/下载）。
- 支付：微信/支付宝/应用内购买；`market://details?id=com.voyago.runbeat`。
- 埋点/统计：Umeng、`AD_ID`、`paywall-event`、`execution-session` 等。

这些云能力、账号与付费逻辑不是节拍器核心，因此没有在 PaceBeat 中复刻。

## 6. 运行时音乐生成验证

在参考 App 的调试重打包版本中，可以直接读取其内部缓存：

- Pure Metronome 模式会生成 `cache/temp_metronome_*.mid`，其中只有 2 个音符：
  - 160 BPM、4/4、480 ticks/quarter；
  - `program 124 / bank 11`（MC-500 Beep）；
  - 第 1 拍 C4（MIDI 60），第 2–4 拍 A3（MIDI 57）；
  - 每个音符 120 ticks（约 93.75 ms），全程 30 分钟。
- Improv Groove 模式会生成 `cache/temp_beat_*.mid`：
  - 168 秒、2 个轨道；
  - 轨道 0 为鼓组（Standard/Jazz 等），轨道 1 为和弦/旋律（Jazz Guitar、Electric Piano 等），内部会随时间切换音色与风格；
  - `[MidiEngine] Effects ON (music)` 表明音乐模式启用混响/合唱，纯节拍模式则关闭。
- 这说明原版“音乐”不是打包的音频，而是由 SF2 + MIDI 生成器实时产生的。

当前 PaceBeat 只保留纯节拍器，不包含这段音乐；该分析用于说明原版音乐是运行时生成的，无法作为静态音频直接复用。

## 6. 对 PaceBeat 的设计结论

保留：

- 分段式训练计划 + 大号 SPM 主界面 + 下一段预告。
- 计划模板、空白计划、添加/编辑/删除分段。
- 3 种 CC0 Click + 11 种用户提供的 1/2 拍打击乐组合、重音、音量增强、语音提示、运行中调速；原版 7 种 SF2 节拍音色已移除。
- 可调拍组与用户提供的打击乐组合。
- 前台服务、通知控制、训练完成页。
- 橙色 + 奶白 + 大圆角卡片的视觉语言（独立重绘，不复制素材）。

不复刻：

- 登录、PRO 订阅、支付、邀请、埋点。
- 云端 Cadence Mix 与原版本地生成音乐（PaceBeat 不包含音乐伴奏）。
- 不在运行时打包/加载原版完整 SF2 资源；仅保留本地节拍采样（3 种 CC0 Click + 11 种用户音效）。

重新实现：

- Kotlin + Jetpack Compose 原生 UI。
- `AudioTrack MODE_STATIC` 循环预渲染 PCM，替代完整 fluidSynth + MIDI 运行时，减小包体并避免网络依赖。
- 3 种 Click 来自 Freesound CC0；11 种打击乐组合来自用户提供的 1/2 拍 8 秒 MP3，离线提取第 1/2 拍作为重音/普通拍。不再打包或调用原版 SF2 的节拍音色。
- 本地 `SharedPreferences` 保存计划与设置。
- 无 `INTERNET` 权限，完全离线。

## 7. 参考

- RunBeat 官方 App Store 介绍（用于交叉核对三模式、步频音乐、语音播报等产品定位）：<https://apps.apple.com/cn/app/runbeat-%E8%B7%91%E6%AD%A5%E8%8A%82%E6%8B%8D%E5%99%A8%E4%B8%8E%E6%AD%A5%E9%A2%91%E9%9F%B3%E4%B9%90/id6760161784>
- APK 内服务端/隐私/协议文案中的官方站点：<https://runbeat.voyago.cn>
