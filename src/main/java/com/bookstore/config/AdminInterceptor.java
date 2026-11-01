package com.bookstore.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.bookstore.entity.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        User user =
                (User) request.getSession().getAttribute("loggedInUser");

        // User is not logged in
        if (user == null) {
            response.sendRedirect("/login");
            return false;
        }

        // User is not an ADMIN
        if (!"ADMIN".equals(user.getRole())) {
            response.sendRedirect("/");
            return false;
        }

        // User is ADMIN
        return true;
    }
}