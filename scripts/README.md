# 更新 frp 二进制文件

此脚本 `update_frp_binaries.sh` 会获取 [fatedier/frp](https://github.com/fatedier/frp) 的最新 release（或使用指定的 tag），并从预编译的压缩包中提取 `frpc` / `frps` 可执行文件，放置到 Android 项目的 `jniLibs` 目录中，支持以下架构映射：

- `android_arm64` -> `app/src/main/jniLibs/arm64-v8a/`
- `linux_amd64`   -> `app/src/main/jniLibs/x86_64/`
- `linux_arm`     -> `app/src/main/jniLibs/armeabi-v7a/`

提取的文件会被复制为 `libfrpc.so` 与 `libfrps.so`，并保留可执行权限（即 `chmod +x`）。

先决条件
- 系统需安装 `curl`、`jq`、`tar` 和 `bash`。

Linux 使用示例
```
# 下载最新 release 并更新 jniLibs
./scripts/update_frp_binaries.sh

# 指定 release tag
./scripts/update_frp_binaries.sh --tag v0.65.0

# 仅模拟运行（不会下载或写入文件）：
./scripts/update_frp_binaries.sh --dry-run

# 指定自定义目标基础目录
./scripts/update_frp_binaries.sh --dest app/src/main/jniLibs_custom

# 使用 GitHub Token 提升 API 请求配额
./scripts/update_frp_binaries.sh --token <GITHUB_TOKEN>
```

Windows PowerShell 使用示例:
```
# 下载最新 release 并更新 jniLibs
pwsh ./scripts/update_frp_binaries.ps1

# 指定 release tag
pwsh ./scripts/update_frp_binaries.ps1 -Tag v0.65.0

# 模拟运行（不会写入）
pwsh ./scripts/update_frp_binaries.ps1 -DryRun
```

