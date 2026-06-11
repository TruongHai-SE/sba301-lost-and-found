package com.sba301.lostandfound.service;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otpCode, String purpose);
}
