package com.example.myapplication.model

/*
ARTICLE_SOLD: 货被卖出
ARTICLE_BROKEN: 货出现破损
ARTICLE_RETURN: 货被退还给经销商
ARTICLE_PURCHASE: 入库

increaseStock = true 表示这一单是往存量里加（入库），false 表示从存量里减
 */
enum class OrderType(val increaseStock: Boolean) {
    ARTICLE_SOLD(false),
    ARTICLE_BROKEN(false),
    ARTICLE_RETURN(false),

    ARTICLE_PURCHASE(true),
}