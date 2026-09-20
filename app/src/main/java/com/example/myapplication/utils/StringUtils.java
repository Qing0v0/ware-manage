package com.example.myapplication.utils;

public interface StringUtils {
    String alertTitle = "提示";

    String sizeArrayEmpty = "入库鞋子数量为0，请至少填写一个尺寸的入库数量";
    String sizeTypeError = "输入入库数量不是数字，请重新填写";
    String articleIdEmpty = "请填写货号";
    String articleNameEmpty = "请填写货名";
    String dealerEmpty = "请填写经销商";
    String purchasePriceEmpty = "请填写进价";
    String sellingPriceEmpty = "请填写售价";
    String priceTypeError = "填写价格并非数字，请重新填写";
    String checkOk = "ok";

    // 存量相关（InventoryService 用），里面带 %s / %d 的由调用处按顺序填参数
    String inventoryNotFound = "存量里找不到 %s（%s），不能出库，请先入库";
    String inventoryNotEnough = "%s（%s）的 %d 码：存量 %d 双，本次需要 %d 双，存量不足";
}
