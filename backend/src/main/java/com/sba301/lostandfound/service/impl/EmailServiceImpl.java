package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @org.springframework.beans.factory.annotation.Value("${brevo.api-key:}")
    private String brevoApiKey;

    @org.springframework.beans.factory.annotation.Value("${brevo.sender-name:Lost & Found}")
    private String brevoSenderName;

    @org.springframework.beans.factory.annotation.Value("${brevo.sender-email:zapter1111@gmail.com}")
    private String brevoSenderEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendOtpEmail(String toEmail, String otpCode, String purpose) {
        if (!org.springframework.util.StringUtils.hasText(brevoApiKey)) {
            log.warn("Brevo API key is empty (not configured). Skipping sending OTP email. " +
                    "For local testing, retrieve OTP code directly from the DB log. Code: {}", otpCode);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.set("api-key", brevoApiKey);

            String htmlContent = buildOtpEmailHtml(otpCode);

            Map<String, Object> payload = Map.of(
                "sender", Map.of("name", brevoSenderName, "email", brevoSenderEmail),
                "to", java.util.List.of(Map.of("email", toEmail)),
                "subject", "Lost & Found — Mã xác thực OTP",
                "htmlContent", htmlContent
            );

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.brevo.com/v3/smtp/email",
                requestEntity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("OTP email successfully sent to {}", toEmail);
            } else {
                log.error("Failed to send OTP email. Brevo response: {}", response.getBody());
                throw new IllegalStateException("Failed to send OTP email: Brevo API error " + response.getStatusCode());
            }
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
