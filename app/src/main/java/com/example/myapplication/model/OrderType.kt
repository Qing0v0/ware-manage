package com.example.myapplication.model

/*
ARTICLE_SOLD: 货被卖出
ARTICLE_BROKEN: 货出现破损
ARTICLE_RETURN: 货被退还给经销商
 */
enum class OrderType {
    ARTICLE_SOLD,
    ARTICLE_BROKEN,
    ARTICLE_RETURN,

    ARTICLE_PURCHASE,
}