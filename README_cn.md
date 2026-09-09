# 健身日志 app

[Readme English](./README.md)
[简体中文](./README_cn.md)

此项目是用于记录日常锻炼项目和身体情况测量数据的Android应用。

## 功能

支持功能：

* 创建和记录训练组、训练动作；
* 记录每次锻炼的详细信息；
* 训练中通过常驻通知卡片显示当前动作已完成组数，并可一键跳转添加一组；
* 记录体重、三围、体脂等身体测量信息；
* 统计、显示训练信息；
* 显示身体测量信息折现图。
* *(开发中)* AI 教练：基于 LLM（DeepSeek）推荐当天训练部位/计划或下一步动作建议。

### 训练动作

![训练组列表](./docs/images/screen_shot_train_page.webp)

![训练动作列表](./docs/images/screen_shot_train_list.webp)

可以创建、记录属于自己的训练动作，并按动作组分类。

并且根据记录的锻炼信息，统计显示各训练动作的使用次数、最大重量、最大单组次数等统计信息。

### 锻炼记录

![首页截图](./docs/images/screen_shot_homepage.webp)

![锻炼记录截图](./docs/images/screen_shot_workout_detail.webp)

![新增锻炼](./docs/images/screen_shot_add_workout.webp)

可以记录你每日的锻炼信息，精确记录你的运动轨迹。

### 身体记录

![身体数据](./docs/images/screen_shot_body_data.webp)

![新增身体数据](./docs/images/screen_shot_add_body_data.webp)

详细记录你的每日身体数据，用于分析身体变化，制定锻炼计划。

## 编译开发

此项目编译环境如下：

* Android Studio Giraffe Canary 8 或以上；
* JDK 17；
* gradle 8.0
* APG 8.1.0-alpha8

此项目依赖以下库：

* Jetpack Compose
* Android Room
* Android Hilt
* Kotlin coroutine
* [vico](https://github.com/patrykandpatrick/vico)

详细依赖列表可在[此处](gradle/libs.versions.toml)获得。

部分控件受到了一些博客文章的启发，进行了参考与定制

* [DropdownMenu](https://proandroiddev.com/improving-the-compose-dropdownmenu-88469b1ef34)
* [SegmentButton](https://medium.com/@manojbhadane/hello-everyone-558290eb632e)

## 免责声明

* 项目仅供个人学习与交流使用，不可用于商业用途。
* 项目仅将用户数据保存于本地，不会上传、与他人分享用户信息。

### AI 教练隐私说明 *(草案，功能完成后定稿)*

* AI 教练为可选功能：**除非你主动点击 AI 推荐入口，应用不会发起任何网络请求**。
* 仅当你触发 AI 教练时，应用才向第三方 LLM 服务（默认 DeepSeek）发送**训练摘要**（部位/动作名、组数、重量(Kg)、次数、时长(秒)；不包含备注与身体数据）。
* AI API Key 仅保存在本机。

## 开源许可

项目基于[GPLv3](./LICENSE)进行开源。
