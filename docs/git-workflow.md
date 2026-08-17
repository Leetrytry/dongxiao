# Git workflow

本项目使用 Git 做版本管理。后续修改遵循以下规则：

- 每个独立功能、修复、资源调整或文档更新使用独立 commit。
- commit message 使用明确动词和范围，例如 `feat: add pitch analyzer`、`fix: correct tuning offset`、`design: update launcher icon`。
- 不提交本地构建环境和产物，包括 `.tools/`、`.gradle/`、`build/`、`app/build/`、`local.properties`、`*.apk`、`*.aab`。
- 修改 Android 源码或资源后，提交前优先运行 `scripts/build-debug.sh` 验证。
- 生成的 APK 作为本地交付文件保留，不作为源码版本历史的一部分。
