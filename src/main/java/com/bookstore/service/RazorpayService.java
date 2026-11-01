package com.bookstore.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    public Order createOrder(double amount) throws RazorpayException {

        RazorpayClient razorpayClient =
                new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();

        // Razorpay amount is always in paise
        int amountInPaise = (int) Math.round(amount * 100);

        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "bookstore_" + System.currentTimeMillis());

        return razorpayClient.orders.create(orderRequest);
    }
}