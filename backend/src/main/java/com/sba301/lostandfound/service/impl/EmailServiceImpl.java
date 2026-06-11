package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:}")
    private String mailUsername;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otpCode, String purpose) {
        if (!org.springframework.util.StringUtils.hasText(mailUsername)) {
            log.warn("SMTP username is empty (not configured). Skipping sending OTP email. " +
                    "For local testing, retrieve OTP code directly from the DB log. Code: {}", otpCode);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Lost & Found — Mã xác thực OTP");

            String htmlContent = buildOtpEmailHtml(otpCode);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("OTP email successfully sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", toEmail, e);
            throw new IllegalStateException("Failed to send OTP email", e);
        }
    }

    private String buildOtpEmailHtml(String otpCode) {
        return "<!DOCTYPE html>\n" +
            "<html lang=\"vi\">\n" +
            "<head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"></head>\n" +
            "<body style=\"margin:0;padding:0;background-color:#F1F3F4;font-family:'Be Vietnam Pro','Segoe UI',Arial,sans-serif;\">\n" +
            "\n" +
            "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#F1F3F4;padding:32px 16px;\">\n" +
            "<tr><td align=\"center\">\n" +
            "<table role=\"presentation\" width=\"520\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.08);\">\n" +
            "\n" +
            "  <!-- Header -->\n" +
            "  <tr><td style=\"background-color:#1A73E8;padding:22px 32px;text-align:center;\">\n" +
            "    <span style=\"font-size:20px;font-weight:700;color:#ffffff;font-family:'Be Vietnam Pro',Arial,sans-serif;letter-spacing:0.3px;\">Lost & Found</span>\n" +
            "  </td></tr>\n" +
            "\n" +
            "  <!-- Body -->\n" +
            "  <tr><td style=\"padding:32px 32px 24px;\">\n" +
            "    <p style=\"margin:0 0 6px;font-size:18px;font-weight:600;color:#202124;font-family:'Be Vietnam Pro',Arial,sans-serif;\">Xác thực tài khoản</p>\n" +
            "    <p style=\"margin:0 0 24px;font-size:14px;color:#5F6368;line-height:1.6;font-family:'Be Vietnam Pro',Arial,sans-serif;\">\n" +
            "      Chúng tôi nhận được yêu cầu đặt lại mật khẩu. Sử dụng mã bên dưới để tiếp tục:\n" +
            "    </p>\n" +
            "\n" +
            "    <!-- OTP Code - single block, copyable -->\n" +
            "    <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:24px;\">\n" +
            "      <tr><td align=\"center\">\n" +
            "        <div style=\"display:inline-block;font-size:32px;font-weight:700;font-family:'Be Vietnam Pro',monospace,Arial,sans-serif;" +
            "color:#1A73E8;letter-spacing:12px;padding:14px 24px 14px 36px;" +
            "background-color:#EBF3FE;border:1px solid #C5DCFA;border-radius:8px;\">" +
            otpCode +
            "</div>\n" +
            "      </td></tr>\n" +
            "    </table>\n" +
            "\n" +
            "    <!-- Expiry notice -->\n" +
            "    <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#FEF7E0;border-left:3px solid #FBBC04;border-radius:6px;margin-bottom:24px;\">\n" +
            "      <tr><td style=\"padding:12px 16px;font-size:13px;color:#5F6368;font-family:'Be Vietnam Pro',Arial,sans-serif;line-height:1.5;\">\n" +
            "        Mã có hiệu lực trong <strong style=\"color:#C55500;\">5 phút</strong>. Không chia sẻ mã này với bất kỳ ai.\n" +
            "      </td></tr>\n" +
            "    </table>\n" +
            "\n" +
            "    <p style=\"margin:0;font-size:13px;color:#5F6368;line-height:1.5;font-family:'Be Vietnam Pro',Arial,sans-serif;\">\n" +
            "      Nếu bạn không yêu cầu, hãy bỏ qua email này.\n" +
            "    </p>\n" +
            "  </td></tr>\n" +
            "\n" +
            "  <!-- Footer -->\n" +
            "  <tr><td style=\"padding:16px 32px;border-top:1px solid #E8EAED;text-align:center;\">\n" +
            "    <p style=\"margin:0;font-size:12px;color:#9AA0A6;font-family:'Be Vietnam Pro',Arial,sans-serif;\">&copy; 2026 Lost &amp; Found. All rights reserved.</p>\n" +
            "  </td></tr>\n" +
            "\n" +
            "</table>\n" +
            "</td></tr>\n" +
            "</table>\n" +
            "</body>\n" +
            "</html>";
    }
}
