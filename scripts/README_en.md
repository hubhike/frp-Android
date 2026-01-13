# Update frp Binaries

This script `update_frp_binaries.sh` fetches the latest release of [fatedier/frp](https://github.com/fatedier/frp) and extracts prebuilt `frpc`/`frps` executables for the following architectures:

- `android_arm64` -> `app/src/main/jniLibs/arm64-v8a/`
- `linux_amd64`   -> `app/src/main/jniLibs/x86_64/`
- `linux_arm`     -> `app/src/main/jniLibs/armeabi-v7a/`

Files will be copied as `libfrpc.so` and `libfrps.so` in the target directories (executable bit preserved).

Prerequisites
- `curl`, `jq`, `tar` and `bash` must be available on your system.

Linux Usage Examples:
```
# Download latest release and place in jniLibs
./scripts/update_frp_binaries.sh

# Specify release tag
./scripts/update_frp_binaries.sh --tag v0.65.0

# Dry-run (safe):
./scripts/update_frp_binaries.sh --dry-run

# Use a custom destination base directory
./scripts/update_frp_binaries.sh --dest app/src/main/jniLibs_custom

# Use a GitHub token to increase API quota
./scripts/update_frp_binaries.sh --token <GITHUB_TOKEN>
```

Windows PowerShell Usage Examples:
```
# Download latest release
pwsh ./scripts/update_frp_binaries.ps1

# Use a specific release tag
pwsh ./scripts/update_frp_binaries.ps1 -Tag v0.65.0

# Dry-run:
pwsh ./scripts/update_frp_binaries.ps1 -DryRun
```
