# 开发文档说明

## 目录划分

### `开发文档/backend`
放后端接口、数据库、联调和测试相关文档。

当前包含：
- `旅游规划模块接口测试文档.md`

### `开发文档/miniprogram`
放微信小程序端的需求、接口契约、开发说明和接入文档。

当前包含：
- `需求.md`
- `travel-api-contract.md`
- `小程序 API 调用指南.md`
- `小程序开发文档.md`
- `MINIPROGRAM_README.md`
- `6小时激进并行开发方案.md`

### `开发文档/archive`
放历史记录、过程性资料、临时文件和非当前主线资料，不参与日常开发入口。

当前包含：
- `对话记录-行程详情页地图可视化.md`
- `legacy-root/`：从根目录清理下来的历史残留目录

## 根目录整理规则

项目根目录只保留：
- 运行入口和配置：`pom.xml`、`README.md`、`start-dev-services.bat`
- 核心源码目录：`src`、`frontend`、`miniprogram`、`homepage`
- 通用说明：`docs/`、`开发文档/`

不要再把测试记录、对话记录、接口说明直接放到根目录。新文档请优先按 `backend`、`miniprogram`、`archive` 分类归档。
