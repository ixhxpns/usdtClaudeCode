package com.usdttrading.service;

import cn.hutool.core.util.RandomUtil;
import com.usdttrading.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * 郵件服務類
 * 負責發送各種類型的郵件，包括驗證碼、密碼重設等
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AuditLogService auditLogService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:USDT交易平台}")
    private String appName;

    @Value("${app.domain:localhost:8080}")
    private String appDomain;

    @Value("${app.support-email:support@usdttrading.com}")
    private String supportEmail;

    /**
     * 生成6位數驗證碼
     */
    public String generateVerificationCode() {
        return RandomUtil.randomNumbers(6);
    }

    /**
     * 發送預註冊驗證郵件
     */
    public CompletableFuture<Void> sendPreRegistrationVerificationEmail(String to, String verificationCode) {
        return CompletableFuture.runAsync(() -> {
            try {
                Context context = new Context();
                context.setVariable("appName", appName);
                context.setVariable("verificationCode", verificationCode);
                context.setVariable("email", to);
                context.setVariable("supportEmail", supportEmail);
                context.setVariable("currentYear", LocalDateTime.now().getYear());
                context.setVariable("expiryTime", "10分鐘");

                String htmlContent = templateEngine.process("pre-registration-verification", context);
                
                sendHtmlEmail(to, appName + " - 註冊驗證碼", htmlContent, null, "PRE_REGISTER_VERIFICATION");
                
                log.info("預註冊驗證郵件已發送: {}", to);
                
            } catch (Exception e) {
                log.error("發送預註冊驗證郵件失敗: {} - 錯誤: {}", to, e.getMessage());
                auditLogService.logEmailEvent(null, "PRE_REGISTER_VERIFICATION_FAILED", to, 
                        "預註冊驗證郵件發送失敗", false, e.getMessage());
            }
        });
    }

    /**
     * 發送註冊驗證郵件
     */
    public CompletableFuture<Void> sendVerificationEmail(String to, Long userId, String verificationCode) {
        return CompletableFuture.runAsync(() -> {
            try {
                Context context = new Context();
                context.setVariable("appName", appName);
                context.setVariable("verificationCode", verificationCode);
                context.setVariable("userId", userId);
                context.setVariable("supportEmail", supportEmail);
                context.setVariable("currentYear", LocalDateTime.now().getYear());
                context.setVariable("expiryTime", "5分鐘");

                String htmlContent = templateEngine.process("email-verification", context);
                
                sendHtmlEmail(to, "歡迎註冊" + appName + " - 郵箱驗證", htmlContent, userId, "EMAIL_VERIFICATION");
                
                log.info("註冊驗證郵件已發送: {} - 用戶ID: {}", to, userId);
                
            } catch (Exception e) {
                log.error("發送註冊驗證郵件失敗: {} - 用戶ID: {} - 錯誤: {}", to, userId, e.getMessage());
                auditLogService.logEmailEvent(userId, "EMAIL_VERIFICATION_FAILED", to, 
                        "註冊驗證郵件發送失敗", false, e.getMessage());
            }
        });
    }

    /**
     * 發送密碼重設郵件
     */
    public CompletableFuture<Void> sendPasswordResetEmail(String to, Long userId, String resetToken) {
        return CompletableFuture.runAsync(() -> {
            try {
                String resetUrl = String.format("https://%s/reset-password?token=%s", appDomain, resetToken);
                
                Context context = new Context();
                context.setVariable("appName", appName);
                context.setVariable("resetUrl", resetUrl);
                context.setVariable("userId", userId);
                context.setVariable("supportEmail", supportEmail);
                context.setVariable("currentYear", LocalDateTime.now().getYear());
                context.setVariable("expiryTime", "30分鐘");
                context.setVariable("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                String htmlContent = templateEngine.process("password-reset", context);
                
                sendHtmlEmail(to, appName + " - 密碼重設", htmlContent, userId, "PASSWORD_RESET");
                
                log.info("密碼重設郵件已發送: {} - 用戶ID: {}", to, userId);
                
            } catch (Exception e) {
                log.error("發送密碼重設郵件失敗: {} - 用戶ID: {} - 錯誤: {}", to, userId, e.getMessage());
                auditLogService.logEmailEvent(userId, "PASSWORD_RESET_EMAIL_FAILED", to, 
                        "密碼重設郵件發送失敗", false, e.getMessage());
            }
        });
    }

    /**
     * 發送登錄安全提醒郵件
     */
    public CompletableFuture<Void> sendLoginSecurityAlert(String to, Long userId, String loginIp, String loginLocation, String userAgent) {
        return CompletableFuture.runAsync(() -> {
            try {
                Context context = new Context();
                context.setVariable("appName", appName);
                context.setVariable("loginIp", loginIp);
                context.setVariable("loginLocation", loginLocation != null ? loginLocation : "未知位置");
                context.setVariable("userAgent", userAgent);
                context.setVariable("loginTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                context.setVariable("supportEmail", supportEmail);
                context.setVariable("currentYear", LocalDateTime.now().getYear());

                String htmlContent = templateEngine.process("login-security-alert", context);
                
                sendHtmlEmail(to, appName + " - 登錄安全提醒", htmlContent, userId, "LOGIN_SECURITY_ALERT");
                
                log.info("登錄安全提醒郵件已發送: {} - 用戶ID: {}", to, userId);
                
            } catch (Exception e) {
                log.error("發送登錄安全提醒郵件失敗: {} - 用戶ID: {} - 錯誤: {}", to, userId, e.getMessage());
                auditLogService.logEmailEvent(userId, "LOGIN_ALERT_EMAIL_FAILED", to, 
                        "登錄安全提醒郵件發送失敗", false, e.getMessage());
            }
        });
    }

    /**
     * 發送密碼修改成功通知郵件
     */
    public CompletableFuture<Void> sendPasswordChangeNotification(String to, Long userId, String changeIp) {
        return CompletableFuture.runAsync(() -> {
            try {
                Context context = new Context();
                context.setVariable("appName", appName);
                context.setVariable("changeIp", changeIp);
                context.setVariable("changeTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                context.setVariable("supportEmail", supportEmail);
                context.setVariable("currentYear", LocalDateTime.now().getYear());

                String htmlContent = templateEngine.process("password-change-notification", context);
                
                sendHtmlEmail(to, appName + " - 密碼修改通知", htmlContent, userId, "PASSWORD_CHANGE_NOTIFICATION");
                
                log.info("密碼修改通知郵件已發送: {} - 用戶ID: {}", to, userId);
                
            } catch (Exception e) {
                log.error("發送密碼修改通知郵件失敗: {} - 用戶ID: {} - 錯誤: {}", to, userId, e.getMessage());
                auditLogService.logEmailEvent(userId, "PASSWORD_CHANGE_NOTIFICATION_FAILED", to, 
                        "密碼修改通知郵件發送失敗", false, e.getMessage());
            }
        });
    }

    /**
     * 發送賬戶鎖定通知郵件
     */
    public CompletableFuture<Void> sendAccountLockedNotification(String to, Long userId, String lockReason, LocalDateTime unlockTime) {
        return CompletableFuture.runAsync(() -> {
            try {
                Context context = new Context();
                context.setVariable("appName", appName);
                context.setVariable("lockReason", lockReason);
                context.setVariable("lockTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                context.setVariable("unlockTime", unlockTime != null ? unlockTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "請聯繫客服");
                context.setVariable("supportEmail", supportEmail);
                context.setVariable("currentYear", LocalDateTime.now().getYear());

                String htmlContent = templateEngine.process("account-locked-notification", context);
                
                sendHtmlEmail(to, appName + " - 賬戶安全通知", htmlContent, userId, "ACCOUNT_LOCKED_NOTIFICATION");
                
                log.info("賬戶鎖定通知郵件已發送: {} - 用戶ID: {}", to, userId);
                
            } catch (Exception e) {
                log.error("發送賬戶鎖定通知郵件失敗: {} - 用戶ID: {} - 錯誤: {}", to, userId, e.getMessage());
                auditLogService.logEmailEvent(userId, "ACCOUNT_LOCKED_NOTIFICATION_FAILED", to, 
                        "賬戶鎖定通知郵件發送失敗", false, e.getMessage());
            }
        });
    }

    /**
     * 發送歡迎郵件
     */
    public CompletableFuture<Void> sendWelcomeEmail(String to, Long userId, String userName) {
        return CompletableFuture.runAsync(() -> {
            try {
                Context context = new Context();
                context.setVariable("appName", appName);
                context.setVariable("userName", userName);
                context.setVariable("supportEmail", supportEmail);
                context.setVariable("currentYear", LocalDateTime.now().getYear());

                String htmlContent = templateEngine.process("welcome", context);
                
                sendHtmlEmail(to, "歡迎加入" + appName + "！", htmlContent, userId, "WELCOME");
                
                log.info("歡迎郵件已發送: {} - 用戶ID: {}", to, userId);
                
            } catch (Exception e) {
                log.error("發送歡迎郵件失敗: {} - 用戶ID: {} - 錯誤: {}", to, userId, e.getMessage());
                auditLogService.logEmailEvent(userId, "WELCOME_EMAIL_FAILED", to, 
                        "歡迎郵件發送失敗", false, e.getMessage());
            }
        });
    }

    /**
     * 發送HTML郵件
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent, Long userId, String emailType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            try {
                helper.setFrom(fromEmail, appName);
            } catch (Exception e) {
                helper.setFrom(fromEmail);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            // 設置郵件頭信息
            message.setHeader("X-Mailer", appName);
            message.setHeader("X-Priority", "3");
            
            mailSender.send(message);
            
            // 記錄郵件發送成功
            auditLogService.logEmailEvent(userId, emailType, to, subject, true, null);
            
        } catch (MessagingException e) {
            log.error("發送HTML郵件失敗: {} - 主題: {} - 錯誤: {}", to, subject, e.getMessage());
            auditLogService.logEmailEvent(userId, emailType + "_FAILED", to, subject, false, e.getMessage());
            throw new BusinessException("郵件發送失敗: " + e.getMessage());
        }
    }

    /**
     * 發送純文本郵件
     */
    public void sendTextEmail(String to, String subject, String content, Long userId, String emailType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            
            try {
                helper.setFrom(fromEmail, appName);
            } catch (Exception e) {
                helper.setFrom(fromEmail);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);
            
            mailSender.send(message);
            
            // 記錄郵件發送成功
            auditLogService.logEmailEvent(userId, emailType, to, subject, true, null);
            
            log.info("文本郵件已發送: {} - 主題: {}", to, subject);
            
        } catch (MessagingException e) {
            log.error("發送文本郵件失敗: {} - 主題: {} - 錯誤: {}", to, subject, e.getMessage());
            auditLogService.logEmailEvent(userId, emailType + "_FAILED", to, subject, false, e.getMessage());
            throw new BusinessException("郵件發送失敗: " + e.getMessage());
        }
    }

    /**
     * 批量發送郵件
     */
    public CompletableFuture<Void> sendBulkEmail(String[] recipients, String subject, String htmlContent, String emailType) {
        return CompletableFuture.runAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setFrom(fromEmail, appName);
                helper.setBcc(recipients); // 使用密送避免洩露收件人信息
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                
                mailSender.send(message);
                
                log.info("批量郵件已發送 - 收件人數量: {} - 主題: {}", recipients.length, subject);
                
                // 記錄批量郵件發送
                auditLogService.logEmailEvent(null, emailType, String.join(",", recipients), 
                        subject, true, "批量發送成功");
                
            } catch (Exception e) {
                log.error("批量郵件發送失敗 - 主題: {} - 錯誤: {}", subject, e.getMessage());
                auditLogService.logEmailEvent(null, emailType + "_FAILED", String.join(",", recipients), 
                        subject, false, e.getMessage());
                throw new BusinessException("批量郵件發送失敗: " + e.getMessage());
            }
        });
    }

    /**
     * 驗證郵箱格式
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }
}