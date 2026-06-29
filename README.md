# Floppy - AI 睡前音频陪伴应用

![Floppy Logo](app/src/main/res/drawable-nodpi/home_floppy_logo.png)

一个基于 AI 的睡前音频陪伴 Android 应用，帮助用户放松身心，改善睡眠质量。

## ✨ 功能特性

### 🎧 核心功能
- **个性化问卷** - 通过性别、年龄、职业、作息时间等维度了解用户
- **智能音频推荐** - 根据用户睡眠问题和偏好推荐最适合的音频内容
- **AI 音频生成** - 支持自定义生成专属的睡前陪伴音频
- **音频播放** - 基于 Media3 ExoPlayer 的高质量音频播放引擎
- **用户反馈** - 支持对音频内容进行评分和反馈
- **个人设置** - 自定义昵称、AI 伙伴名称、声音偏好等

### 🌙 睡眠问题覆盖
- 脑子停不下来 (Racing Thoughts)
- 压力大 (Stress)
- 睡得浅 (Light Sleep)
- 作息不稳定 (Irregular Schedule)
- 想被陪伴 (Loneliness)

### 🎤 陪伴风格
- 温柔治愈型 (Gentle)
- 耐心倾听型 (Patient)
- 安心哄睡型 (Reassuring)
- 轻松陪伴型 (Playful)
- 安静陪伴型 (Quiet)
- 引导放松型 (Coaching)
- 睡前故事型 (Storyteller)

### 🔊 声音偏好
- 温暖治愈音 (Warm Female)
- 沉稳低音 (Calm Male)
- 清澈中性音 (Neutral)
- 轻柔低语音 (Whisper)
- 睡前故事音 (Story)
- 柔和电台音 (Radio)
- 低沉海洋音 (Ocean)
- 明亮陪伴音 (Bright)

## 🛠️ 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 1.9+ |
| UI 框架 | Jetpack Compose | 1.8.2 |
| 状态管理 | ViewModel + Coroutines + Flow | - |
| 网络请求 | Retrofit + OkHttp | 2.9.0 |
| 音频播放 | Media3 ExoPlayer | 1.7.1 |
| 数据存储 | DataStore Preferences | 1.1.3 |
| 导航 | Navigation Compose | 2.9.0 |
| 构建工具 | Gradle | 8.x |

## 📱 最低要求

- **Android SDK**: 29 (Android 10.0)
- **Target SDK**: 36
- **Java Version**: 17

## 🚀 快速开始

### 环境要求

- JDK 17+
- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK 36

### 构建命令

```bash
# 使用本地 Gradle 缓存构建
GRADLE_USER_HOME=.gradle-home ./gradlew test assembleDebug

# 仅运行测试
GRADLE_USER_HOME=.gradle-home ./gradlew test

# 构建 Release 版本
GRADLE_USER_HOME=.gradle-home ./gradlew assembleRelease
```

### 配置说明

- **Debug 构建**: 默认使用 Mock API，无需后端服务即可运行
- **Release 构建**: 需配置 `API_BASE_URL` 指向实际后端服务

### API 配置

在 `app/build.gradle.kts` 中配置：

```kotlin
// 通过 Gradle 属性配置
gradle.properties:
floppy.useMockApi=true      # true 使用 Mock，false 使用真实 API
floppy.apiBaseUrl=http://your-api-base-url.com/
```

## 📁 项目结构

```
Floppy/
├── app/                              # Android 应用模块
│   ├── src/main/java/com/floppy/app/
│   │   ├── bluetooth/                # 蓝牙控制器
│   │   │   └── FloppyBluetoothController.kt
│   │   ├── data/                     # 数据层
│   │   │   ├── FloppyApi.kt          # Retrofit API 接口定义
│   │   │   ├── FloppyRepository.kt   # 数据仓库接口
│   │   │   ├── MockFloppyRepository.kt   # Mock 数据实现
│   │   │   ├── RemoteFloppyRepository.kt # 远程数据实现
│   │   │   ├── RepositoryFactory.kt      # 仓库工厂
│   │   │   ├── LocalProfileStorage.kt    # 本地 Profile 存储
│   │   │   ├── DemoTextIntentClient.kt   # Demo 意图客户端
│   │   │   ├── FallbackAudioLibrary.kt   # 后备音频库
│   │   │   └── StreamingSpeechClient.kt  # 流式语音客户端
│   │   ├── domain/                   # 领域模型
│   │   │   └── Models.kt             # 所有数据模型定义
│   │   ├── playback/                 # 播放控制层
│   │   │   ├── PlaybackController.kt     # 播放控制器接口
│   │   │   └── ExoPlaybackController.kt  # ExoPlayer 实现
│   │   ├── ui/                       # UI 层
│   │   │   ├── FloppyApp.kt          # 应用入口
│   │   │   ├── FloppyViewModel.kt    # 主 ViewModel
│   │   │   ├── theme/                # 主题配置
│   │   │   │   └── Theme.kt
│   │   │   └── video/                # 视频播放器
│   │   │       └── Mp4VideoPlayer.kt
│   │   ├── FloppyApplication.kt      # Application 类
│   │   └── MainActivity.kt           # 主 Activity
│   ├── src/main/res/                 # 资源文件
│   └── src/test/                     # 单元测试
├── design-assets/                    # 设计资源
├── gradle/wrapper/                   # Gradle Wrapper
├── .gitignore
├── build.gradle.kts                  # 根构建文件
├── gradle.properties                 # Gradle 属性
├── gradlew                           # Gradle 脚本
└── settings.gradle.kts               # 设置文件
```

## 🏗️ 架构设计

### MVP 架构模式

本项目采用简化的 MVP（Model-View-Presenter）架构模式：

```
View (Compose UI)
    ↕ (State/Events)
ViewModel (FloppyViewModel)
    ↕ (Repository API)
Repository (FloppyRepository)
    ↕ (Mock/Remote)
Data Sources
```

### 核心组件职责

| 组件 | 职责 |
|------|------|
| **ViewModel** | 持有 UI 状态，处理用户交互，协调业务逻辑 |
| **Repository** | 统一数据访问接口，管理数据源切换 |
| **API** | Retrofit 接口定义，与后端通信 |
| **PlaybackController** | 音频播放控制，状态管理 |
| **LocalProfileStorage** | 使用 DataStore 持久化用户配置 |

## 📡 API 接口

### 核心接口列表

| 接口 | 方法 | 描述 |
|------|------|------|
| `/users/{userId}/questionnaire` | PUT | 保存用户问卷 |
| `/users/{userId}/profile` | PUT | 保存用户配置 |
| `/v1/recommendations` | POST | 获取音频推荐 |
| `/v1/generation-tasks` | POST | 创建音频生成任务 |
| `/v1/generation-tasks/{taskId}` | GET | 查询生成任务状态 |
| `/v1/audio/{audioId}` | GET | 获取音频详情 |
| `/users/{userId}/audio-library` | GET | 获取音频库 |
| `/users/{userId}/uploads` | POST | 上传音频文件 |
| `/v1/feedback` | POST | 提交反馈 |
| `/voice/intent` | POST | 提交文本意图 |
| `/api/ai-companion/voices` | GET | 获取声音选项 |

### 数据模型

核心数据模型定义在 `domain/Models.kt` 中：

- **UserProfile** - 用户配置（性别、年龄、职业、作息、偏好等）
- **AudioItem** - 音频项（标题、时长、播放地址、封面等）
- **GenerationTask** - 生成任务（状态、进度、结果）
- **AppSettings** - 应用设置（用户昵称、AI 伙伴名称等）
- **Feedback** - 反馈（评分、原因）

## 🧪 测试

### 运行测试

```bash
# 运行所有测试
GRADLE_USER_HOME=.gradle-home ./gradlew test

# 运行特定测试类
GRADLE_USER_HOME=.gradle-home ./gradlew test --tests "com.floppy.app.MockFloppyRepositoryTest"
```

### 测试覆盖

- **MockFloppyRepositoryTest** - Mock 数据仓库测试
- 可扩展：Repository、ViewModel、API 接口测试

## 🤝 贡献指南

### 代码规范

- 遵循 Kotlin 官方代码风格指南
- 使用 Jetpack Compose 最佳实践
- 保持代码简洁、可读性强
- 添加必要的注释和文档

### 提交规范

```
<type>(<scope>): <description>

<optional body>

<optional footer>
```

**Types**:
- `feat` - 新功能
- `fix` - 修复 Bug
- `docs` - 文档更新
- `style` - 代码风格
- `refactor` - 重构
- `test` - 测试
- `chore` - 构建/工具

### 开发流程

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/xxx`)
3. 提交更改 (`git commit -m "feat: xxx"`)
4. 推送到分支 (`git push origin feature/xxx`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证。详情请参见 [LICENSE](LICENSE) 文件。

## 🙋‍♂️ 支持

如有问题或建议，请提交 [Issue](https://github.com/zxy20030810/Floppy/issues)。

---

**Made with ❤️ for better sleep**