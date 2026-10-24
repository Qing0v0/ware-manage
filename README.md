鞋仓管理安卓APP练习

# UI预览（待细节修改）
<img width="216" height="430" alt="a76bead5bf88863452ee7c702dd9886e" src="https://github.com/user-attachments/assets/c1533483-4fd9-4368-9eff-9c40e00f4a6e" />
<img width="216" height="430" alt="f09c43f25f6e1748035fd7337b5b0dc7" src="https://github.com/user-attachments/assets/f7940c59-80ef-40ec-84c4-7d2666886f85" />
<img width="216" height="430" alt="64ee61f197bd3fed489455bf142534b8" src="https://github.com/user-attachments/assets/b786ffbd-f182-4926-baf7-8f20305f922c" />

# 库存页面
- 显示库存总量、总款式数量信息。
- 提供入/出库功能，通过货号、经销商、34 - 44码鞋数量出入库。
- 按货号不同颜色显示库存， 库存旁+ / -图标方便该颜色出入库。

# 订单页面
- 按条件查询历史出/入库订单，按时间排序。

# 更多
## 数据恢复 / 备份
将数据从本地恢复 / 备份。

## 远程更新
1. 获取base64格式签名上传到github Repository secrets。
    ```
    1. 生成jks文件，Build -> Generate Signed App Bundle / APK
    2. 转换成base64格式，base64 -w 0 release.jks > keystore_b64.txt
    ```
2. 编写工作流`.github/workflows/android.yml`，逻辑推送tag后触发，编译apk后发布到github release中，格式为`Release ${versionName}`。
    ```
    推送tag
    git tag v1.1.2
    git push origin v1.1.2
    ```
3. app对比本地版本和github上.../release/latest中版本，下载安装。
