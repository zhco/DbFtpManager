# DB & FTP Manager

一款安卓端的数据库和 FTP 文件管理工具。

## 功能特性

### 数据库管理
- 支持 MySQL、PostgreSQL、SQL Server、Oracle、MariaDB、SQLite
- SQL 编辑器，支持增删改查等所有 SQL 操作
- 浏览数据库、表、表结构
- 查询结果展示，显示行数和执行耗时
- SQL 历史记录
- 表数据查看、插入新行

### FTP 管理
- 支持 FTP / SFTP 协议
- 浏览远程文件目录
- 上传文件、下载文件
- 删除文件/文件夹
- 新建文件夹、重命名
- 显示文件大小、修改时间、权限

## 技术栈
- Kotlin + AndroidX
- Room Database（本地存储）
- Apache Commons Net（FTP）
- JDBC（数据库连接）
- Material Design
- MVVM 架构

## 编译
用 Android Studio 打开项目，Gradle 同步后编译运行。
最低 SDK: 26 (Android 8.0) | 目标 SDK: 34
