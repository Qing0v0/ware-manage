鞋仓管理安卓APP练习

# MainActivity
主界面包括导航(menu\navigation_bottom.xml)，以及切换页面功能(layout\fragment_xxx.xml)
/manifests/AndroidManifest.xml 中 `android:name=".activity.MainActivity"`定义程序主入口位置

```Kotlin
// 程序入口
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main) // 主界面布局 res\layout\activity_main.xml
    ...
}
```

# 库存页面 WareFragment
提供入/出库功能，显示当前商品存量

## 侧边栏
### 远程更新
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



# 订单页面 BillsFragment
按条件查询历史出/入库订单


