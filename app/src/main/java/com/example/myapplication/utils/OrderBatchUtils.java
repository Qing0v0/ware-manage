package com.example.myapplication.utils;

import com.example.myapplication.model.Color;
import com.example.myapplication.model.OrderBatch;
import com.example.myapplication.model.OrderType;

import java.math.BigDecimal;
import java.util.Date;

public class OrderBatchUtils implements StringUtils{
    public String[] sizeArray;
    public String articleId;
    public String articleName;
    public String price;
    public Color color;
    public String dealer;
    public OrderType orderType;

    public OrderBatchUtils(
            String[] sizeArray, String articleId, String articleName, String dealer,
            String color, String price, OrderType orderType
    ) {
        this.sizeArray = sizeArray;
        this.articleId = articleId;
        this.articleName = articleName;
        this.dealer = dealer;
        this.color = Color.matchColor(color);
        this.price = price;
        this.orderType = orderType;
    }

    public String checkInputs() {
        // check articleId
        if ("".equals(articleId)) {
            return articleIdEmpty;
        }

        // check articleName
        if ("".equals(articleName)) {
            return articleNameEmpty;
        }

        //check dealer
        if ("".equals(dealer) && orderType == OrderType.ARTICLE_PURCHASE) {
            return dealerEmpty;
        }

        // check price
        if ("".equals(price)) {
            return orderType == OrderType.ARTICLE_PURCHASE ? purchasePriceEmpty : sellingPriceEmpty;
        }

        try {
            new BigDecimal(price);
        } catch (NumberFormatException e) {
            return priceTypeError;
        }

        // check sizeArray
        int totalNum = 0;
        for (int i = 0; i < sizeArray.length; i++) {
            if ("".equals(sizeArray[i])) {
                sizeArray[i] = "0";
            }
            // check type
            try {
                totalNum += Integer.parseInt(sizeArray[i]);
            } catch (NumberFormatException e) {
                return sizeTypeError;
            }
        }
        if (totalNum == 0) {
            return sizeArrayEmpty;
        }

        return checkOk;
    }

    public OrderBatch buildOrderBatch() {
        return new OrderBatch(
                0,
                articleId,
                articleName,
                color,
                Integer.parseInt(sizeArray[0]), Integer.parseInt(sizeArray[1]), Integer.parseInt(sizeArray[2]),
                Integer.parseInt(sizeArray[3]), Integer.parseInt(sizeArray[4]), Integer.parseInt(sizeArray[5]),
                Integer.parseInt(sizeArray[6]), Integer.parseInt(sizeArray[7]), Integer.parseInt(sizeArray[8]),
                Integer.parseInt(sizeArray[9]), Integer.parseInt(sizeArray[10]),
                orderType,
                dealer,
                new BigDecimal(price),
                new Date()
        );
    }
}
