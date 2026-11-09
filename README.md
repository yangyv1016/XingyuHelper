# 星语互联手机助手

AstraTaleStudio 作品。适用于 Android 11 及以上。首次由 Windows 客户端通过已授权的 USB 安装和初始化，日常可在手机助手里直接开关无线调试。 [星语互联 Windows 客户端与连接说明](https://github.com/yangyv1016/XingyuConnect#用手机助手无线连接)

## 构建与产物

GitHub Actions 在 main 更新、拉取请求和手动触发时执行构建。每次成功运行的 Artifacts 区域提供 XingyuHelper-Android，包含以下文件。 [构建工作流](.github/workflows/build.yml)

- XingyuHelper.apk 用于安装到 Android 手机。
- XingyuWifiBootstrap.jar 供 Windows 客户端在首次初始化时通过已授权 USB 临时运行。
- SHA256SUMS 用于核对上述文件。

本地构建需要 JDK 17、Android SDK Platform 34 与 Build Tools 34.0.0。设置 Android SDK 路径后，在 Windows 执行 ./gradlew.bat :app:assembleRelease :app:assembleWifiBootstrap。Linux 和 macOS 使用 ./gradlew。输出位于 app/build/outputs/apk/release/ 和 app/build/outputs/bootstrap/。

工作流 APK 使用运行环境的测试签名。测试签名可能与 Windows 发布包内的 APK 不同，Android 会拒绝不同签名的覆盖安装。通过 Windows 客户端初始化时，请使用它内置的版本。

## 源码与许可

开关及快捷设置图块适配自 DevSwitch，固定上游修订 c346948440e01157a4047bcc5606cb5401e21f59。修改包含无线专用控制、中文界面及电脑端一次初始化流程。项目代码采用 GPL-3.0-or-later。工作室品牌素材保留原权利。 [DevSwitch 上游](https://github.com/retransmit/DevSwitch) · [许可证](LICENSE)
