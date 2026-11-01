package com.bookstore.controller;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.json.JSONObject;
import com.razorpay.Utils;

import com.bookstore.entity.Book;
import com.bookstore.entity.Cart;
import com.bookstore.entity.Order;
import com.bookstore.entity.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.OrderRepository;
import com.razorpay.RazorpayException;
import com.bookstore.service.RazorpayService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CheckoutController {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private RazorpayService razorpayService;
    
    @Value("${razorpay.key.id}")
    private String razorpayKeyId;
    
    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;


    // Show Checkout Page
    @GetMapping("/checkout")
    public String checkout(
            @RequestParam(defaultValue = "false") boolean buyNow,
            HttpSession session,
            Model model) {

        User user =
                (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

     // Check if this is a Buy Now checkout
        Object buyNowBookId =
                session.getAttribute("buyNowBookId");

        Object buyNowQuantity =
                session.getAttribute("buyNowQuantity");

        if (buyNow && buyNowBookId != null && buyNowQuantity != null) {

            Book book =
                    bookRepository
                            .findById((Long) buyNowBookId)
                            .orElse(null);

            if (book == null) {
                session.removeAttribute("buyNowBookId");
                session.removeAttribute("buyNowQuantity");
                return "redirect:/";
            }

            int quantity =
                    (Integer) buyNowQuantity;

            if (quantity <= 0 ||
                    quantity > book.getQuantity()) {

                session.removeAttribute("buyNowBookId");
                session.removeAttribute("buyNowQuantity");
                return "redirect:/";
            }

            // Create temporary cart item for checkout display
            Cart buyNowCart = new Cart();

            buyNowCart.setBookId(book.getId());
            buyNowCart.setBookTitle(book.getTitle());
            buyNowCart.setPrice(book.getPrice());
            buyNowCart.setQuantity(quantity);
            buyNowCart.setUserId(user.getId());

            model.addAttribute(
                    "cartItems",
                    List.of(buyNowCart)
            );

        } else {

            // Normal Cart checkout
            // Clear any old Buy Now session
            session.removeAttribute("buyNowBookId");
            session.removeAttribute("buyNowQuantity");

            List<Cart> cartItems =
                    cartRepository.findByUserId(user.getId());

            if (cartItems.isEmpty()) {
                return "redirect:/cart";
            }

            model.addAttribute(
                    "cartItems",
                    cartItems
            );
        }

        model.addAttribute("user", user);
        model.addAttribute("razorpayKeyId", razorpayKeyId);

        return "checkout";
    }

    @Transactional
    // Place Order
    @PostMapping("/place-order")
    public String placeOrder(
            @RequestParam String phone,
            @RequestParam String address,
            @RequestParam String paymentMethod,
            HttpSession session,
            Model model) {

        User user =
                (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        // Check if this is a Buy Now order
        Object buyNowBookId =
                session.getAttribute("buyNowBookId");

        Object buyNowQuantity =
                session.getAttribute("buyNowQuantity");

        // =====================================================
        // BUY NOW + CASH ON DELIVERY
        // =====================================================

        if (buyNowBookId != null &&
                buyNowQuantity != null) {

            Book book =
                    bookRepository
                            .findById((Long) buyNowBookId)
                            .orElse(null);

            if (book == null) {
                session.removeAttribute("buyNowBookId");
                session.removeAttribute("buyNowQuantity");
                return "redirect:/";
            }

            int quantity =
                    (Integer) buyNowQuantity;

            // Check stock
            if (quantity <= 0 ||
                    quantity > book.getQuantity()) {

                return "redirect:/";
            }

            double totalPrice =
                    book.getPrice() * quantity;

            // Create order
            Order order = new Order();

            order.setUserId(user.getId());
            order.setBookId(book.getId());
            order.setCustomerName(user.getName());
            order.setCustomerEmail(user.getEmail());
            order.setPhone(phone);
            order.setAddress(address);
            order.setPaymentMethod(
                    "Cash on Delivery"
            );
            order.setStatus("PLACED");

            order.setBookTitle(
                    book.getTitle()
            );

            order.setQuantity(quantity);

            order.setTotalPrice(
                    totalPrice
            );

            orderRepository.save(order);

            // Reduce stock
            book.setQuantity(
                    book.getQuantity() - quantity
            );

            bookRepository.save(book);

            // Clear Buy Now session
            session.removeAttribute(
                    "buyNowBookId"
            );

            session.removeAttribute(
                    "buyNowQuantity"
            );

            // Confirmation page
            model.addAttribute(
                    "user",
                    user
            );

            model.addAttribute(
                    "phone",
                    phone
            );

            model.addAttribute(
                    "address",
                    address
            );

            model.addAttribute(
                    "paymentMethod",
                    "Cash on Delivery"
            );

            model.addAttribute(
                    "totalPrice",
                    totalPrice
            );

            return "orderConfirmation";
        }

        // =====================================================
        // NORMAL CART + CASH ON DELIVERY
        // =====================================================

        List<Cart> cartItems =
                cartRepository.findByUserId(
                        user.getId()
                );

        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        double grandTotal = 0;

        // Check stock first
        for (Cart cart : cartItems) {

            Book book =
                    bookRepository
                            .findById(cart.getBookId())
                            .orElse(null);

            if (book == null) {
                continue;
            }

            if (cart.getQuantity() >
                    book.getQuantity()) {

                model.addAttribute(
                        "error",
                        "Not enough stock available for: "
                                + book.getTitle()
                );

                model.addAttribute(
                        "cartItems",
                        cartItems
                );

                model.addAttribute(
                        "user",
                        user
                );

                model.addAttribute(
                        "razorpayKeyId",
                        razorpayKeyId
                );

                return "checkout";
            }
        }

        // Create orders and reduce stock
        for (Cart cart : cartItems) {

            Book book =
                    bookRepository
                            .findById(cart.getBookId())
                            .orElse(null);

            if (book == null) {
                continue;
            }

            double itemTotal =
                    cart.getPrice()
                            * cart.getQuantity();

            grandTotal += itemTotal;

            Order order = new Order();

            order.setUserId(user.getId());
            order.setBookId(book.getId());
            order.setCustomerName(user.getName());
            order.setCustomerEmail(user.getEmail());
            order.setPhone(phone);
            order.setAddress(address);
            order.setPaymentMethod(
                    paymentMethod
            );
            order.setStatus("PLACED");

            order.setBookTitle(
                    cart.getBookTitle()
            );

            order.setQuantity(
                    cart.getQuantity()
            );

            order.setTotalPrice(
                    itemTotal
            );

            orderRepository.save(order);

            // Reduce stock
            book.setQuantity(
                    book.getQuantity()
                            - cart.getQuantity()
            );

            bookRepository.save(book);
        }

        // Clear cart
        cartRepository.deleteAll(cartItems);

        // Confirmation page
        model.addAttribute(
                "user",
                user
        );

        model.addAttribute(
                "phone",
                phone
        );

        model.addAttribute(
                "address",
                address
        );

        model.addAttribute(
                "paymentMethod",
                paymentMethod
        );

        model.addAttribute(
                "totalPrice",
                grandTotal
        );

        return "orderConfirmation";
    }
    
    @PostMapping("/create-payment-order")
    @ResponseBody
    public Map<String, Object> createPaymentOrder(HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {

            User user =
                    (User) session.getAttribute("loggedInUser");

            if (user == null) {
                response.put("success", false);
                response.put("message", "Please login first.");
                return response;
            }

            // Check if this is a Buy Now checkout
            Object buyNowBookId =
                    session.getAttribute("buyNowBookId");

            Object buyNowQuantity =
                    session.getAttribute("buyNowQuantity");

            double grandTotal = 0;

            if (buyNowBookId != null &&
                    buyNowQuantity != null) {

                // BUY NOW

                Book book =
                        bookRepository
                                .findById((Long) buyNowBookId)
                                .orElse(null);

                if (book == null) {
                    response.put("success", false);
                    response.put("message", "Book not found.");
                    return response;
                }

                int quantity =
                        (Integer) buyNowQuantity;

                if (quantity <= 0 ||
                        quantity > book.getQuantity()) {

                    response.put("success", false);
                    response.put(
                            "message",
                            "Not enough stock available."
                    );
                    return response;
                }

                grandTotal =
                        book.getPrice() * quantity;

            } else {

                // NORMAL CART CHECKOUT

                List<Cart> cartItems =
                        cartRepository.findByUserId(user.getId());

                if (cartItems.isEmpty()) {
                    response.put("success", false);
                    response.put(
                            "message",
                            "Your cart is empty."
                    );
                    return response;
                }

                for (Cart cart : cartItems) {

                    Book book =
                            bookRepository
                                    .findById(cart.getBookId())
                                    .orElse(null);

                    if (book == null) {
                        continue;
                    }

                    if (cart.getQuantity() >
                            book.getQuantity()) {

                        response.put("success", false);
                        response.put(
                                "message",
                                "Not enough stock available for: "
                                        + book.getTitle()
                        );

                        return response;
                    }

                    grandTotal +=
                            cart.getPrice()
                                    * cart.getQuantity();
                }
            }

            // Create Razorpay order
            com.razorpay.Order razorpayOrder =
                    razorpayService.createOrder(grandTotal);

            session.setAttribute(
                    "pendingRazorpayOrderId",
                    razorpayOrder.get("id")
            );

            session.setAttribute(
                    "pendingRazorpayAmount",
                    razorpayOrder.get("amount")
            );

            response.put(
                    "success",
                    true
            );

            response.put(
                    "orderId",
                    razorpayOrder.get("id")
            );

            response.put(
                    "amount",
                    razorpayOrder.get("amount")
            );

            response.put(
                    "currency",
                    razorpayOrder.get("currency")
            );

            return response;

        } catch (RazorpayException e) {

            e.printStackTrace();

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "Unable to create Razorpay order."
            );

            return response;
        }
    }
    
    @Transactional
    @PostMapping("/verify-payment")
    @ResponseBody
    public Map<String, Object> verifyPayment(
            @RequestParam String razorpay_payment_id,
            @RequestParam String razorpay_order_id,
            @RequestParam String razorpay_signature,
            @RequestParam String phone,
            @RequestParam String address,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {

            User user =
                    (User) session.getAttribute("loggedInUser");

            if (user == null) {
                response.put("success", false);
                response.put("message", "Please login first.");
                return response;
            }

            // Check Razorpay order
            Object pendingOrder =
                    session.getAttribute("pendingRazorpayOrderId");

            if (pendingOrder == null ||
                    !pendingOrder.toString()
                            .equals(razorpay_order_id)) {

                response.put("success", false);
                response.put(
                        "message",
                        "Invalid Razorpay order."
                );
                return response;
            }

            // Verify Razorpay signature
            JSONObject paymentData =
                    new JSONObject();

            paymentData.put(
                    "razorpay_payment_id",
                    razorpay_payment_id
            );

            paymentData.put(
                    "razorpay_order_id",
                    razorpay_order_id
            );

            paymentData.put(
                    "razorpay_signature",
                    razorpay_signature
            );

            boolean verified =
                    Utils.verifyPaymentSignature(
                            paymentData,
                            razorpayKeySecret
                    );

            if (!verified) {

                response.put("success", false);
                response.put(
                        "message",
                        "Payment verification failed."
                );

                return response;
            }
            
            // Verify payment amount
            Object pendingAmount =
                    session.getAttribute("pendingRazorpayAmount");

            if (pendingAmount == null) {

                response.put("success", false);
                response.put(
                        "message",
                        "Payment amount verification failed."
                );

                return response;
            }

            double paidAmount =
                    Double.parseDouble(pendingAmount.toString());
            
             // Amount that the server expects to be paid
            double expectedAmount = 0.0;

            // Check if this is a Buy Now purchase
            Object buyNowBookId =
                    session.getAttribute("buyNowBookId");

            Object buyNowQuantity =
                    session.getAttribute("buyNowQuantity");

            // =====================================================
            // BUY NOW PAYMENT
            // =====================================================

            if (buyNowBookId != null &&
                    buyNowQuantity != null) {

                Book book =
                        bookRepository
                                .findById((Long) buyNowBookId)
                                .orElse(null);

                if (book == null) {

                    response.put("success", false);
                    response.put(
                            "message",
                            "Book not found."
                    );

                    return response;
                }

                int quantity =
                        (Integer) buyNowQuantity;

                // Check stock again
                if (quantity <= 0 ||
                        quantity > book.getQuantity()) {

                    response.put("success", false);
                    response.put(
                            "message",
                            "Not enough stock available."
                    );

                    return response;
                }

                double totalPrice =
                        book.getPrice() * quantity;

                expectedAmount = totalPrice;

                // Verify payment amount
                if (Math.abs(expectedAmount - paidAmount) > 0.01) {

                    response.put("success", false);
                    response.put(
                            "message",
                            "Payment amount does not match the order amount."
                    );

                    return response;
                }

                // Create order
                Order order = new Order();

                order.setUserId(user.getId());
                order.setBookId(book.getId());
                order.setCustomerName(user.getName());
                order.setCustomerEmail(user.getEmail());
                order.setPhone(phone);
                order.setAddress(address);
                order.setPaymentMethod("Online Payment");
                order.setStatus("PLACED");

                order.setBookTitle(
                        book.getTitle()
                );

                order.setQuantity(quantity);

                order.setTotalPrice(
                        totalPrice
                );

                orderRepository.save(order);

                // Reduce stock
                book.setQuantity(
                        book.getQuantity() - quantity
                );

                bookRepository.save(book);

                // Clear Buy Now session
                session.removeAttribute(
                        "buyNowBookId"
                );

                session.removeAttribute(
                        "buyNowQuantity"
                );

            }

            // =====================================================
            // NORMAL CART PAYMENT
            // =====================================================

            else {

                List<Cart> cartItems =
                        cartRepository.findByUserId(
                                user.getId()
                        );

                if (cartItems.isEmpty()) {

                    response.put("success", false);
                    response.put(
                            "message",
                            "Your cart is empty."
                    );

                    return response;
                }

                // Check stock first
                for (Cart cart : cartItems) {

                    Book book =
                            bookRepository
                                    .findById(cart.getBookId())
                                    .orElse(null);

                    if (book == null) {
                        continue;
                    }

                    if (cart.getQuantity() >
                            book.getQuantity()) {

                        response.put("success", false);
                        response.put(
                                "message",
                                "Not enough stock available for: "
                                        + book.getTitle()
                        );

                        return response;
                    }
                    
                    expectedAmount +=
                            cart.getPrice() * cart.getQuantity();
                }

	             // Verify payment amount
	             if (Math.abs(expectedAmount - paidAmount) > 0.01) {
	
	                 response.put("success", false);
	                 response.put(
	                         "message",
	                         "Payment amount does not match the order amount."
	                 );
	
	                 return response;
	             }
	
	             // Create orders and reduce stock
	             for (Cart cart : cartItems) {

                    Book book =
                            bookRepository
                                    .findById(cart.getBookId())
                                    .orElse(null);

                    if (book == null) {
                        continue;
                    }

                    double itemTotal =
                            cart.getPrice()
                                    * cart.getQuantity();

                    Order order = new Order();

                    order.setUserId(user.getId());
                    order.setBookId(book.getId());
                    order.setCustomerName(user.getName());
                    order.setCustomerEmail(user.getEmail());
                    order.setPhone(phone);
                    order.setAddress(address);
                    order.setPaymentMethod(
                            "Online Payment"
                    );
                    order.setStatus("PLACED");

                    order.setBookTitle(
                            cart.getBookTitle()
                    );

                    order.setQuantity(
                            cart.getQuantity()
                    );

                    order.setTotalPrice(
                            itemTotal
                    );

                    orderRepository.save(order);

                    // Reduce stock
                    book.setQuantity(
                            book.getQuantity()
                                    - cart.getQuantity()
                    );

                    bookRepository.save(book);
                }

                // Clear cart
                cartRepository.deleteAll(cartItems);
            }

            // Remove Razorpay session data
            session.removeAttribute(
                    "pendingRazorpayOrderId"
            );

            session.removeAttribute(
                    "pendingRazorpayAmount"
            );

            response.put(
                    "success",
                    true
            );

            response.put(
                    "message",
                    "Payment successful and order placed."
            );

            return response;

        } catch (Exception e) {

            e.printStackTrace();

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "Payment verification failed."
            );

            return response;
        }
    }
}