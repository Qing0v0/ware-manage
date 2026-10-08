package com.example.myapplication.utils;

import android.content.Context;
import com.example.myapplication.R;
import com.example.myapplication.model.Color;
import com.example.myapplication.model.OrderBatch;
import com.example.myapplication.model.OrderType;

import java.math.BigDecimal;
import java.util.Date;

public class OrderBatchUtils {
    public String[] sizeArray;
    public String articleId;
    public String price;
    public Color color;
    public String dealer;
    public OrderType orderType;

    public OrderBatchUtils(
            String[] sizeArray, String articleId, String dealer,
            String color, String price, OrderType orderType
    ) {
        this.sizeArray = sizeArray;
        this.articleId = articleId;
        this.dealer = dealer;
        this.color = Color.matchColor(color);
        this.price = price;
        this.orderType = orderType;
    }

    public String checkInputs(Context context) {
        // check articleId
        if ("".equals(articleId)) {
            return context.getString(R.string.article_id_hint);
        }

        //check dealer
        if ("".equals(dealer) && orderType == OrderType.ARTICLE_PURCHASE) {
            return context.getString(R.string.dealer_hint);
        }

        // check price
        if ("".equals(price)) {
            return orderType == OrderType.ARTICLE_PURCHASE
                    ? context.getString(R.string.purchase_price_hint)
                    : context.getString(R.string.selling_price_hint);
        }

        try {
            new BigDecimal(price);
        } catch (NumberFormatException e) {
            return context.getString(R.string.priceTypeError);
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
                return context.getString(R.string.sizeTypeError);
            }
        }
        if (totalNum == 0) {
            return context.getString(R.string.sizeArrayEmpty);
        }

        return context.getString(R.string.checkOk);
    }

    public OrderBatch buildOrderBatch() {
        return new OrderBatch(
                0,
                articleId,
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
