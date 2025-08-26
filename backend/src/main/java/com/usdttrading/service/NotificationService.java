package com.usdttrading.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.usdttrading.entity.Notification;
import com.usdttrading.entity.User;
import com.usdttrading.entity.UserKyc;
import com.usdttrading.enums.NotificationCategory;
import com.usdttrading.enums.NotificationPriority;
import com.usdttrading.enums.NotificationStatus;
import com.usdttrading.enums.NotificationType;
import com.usdttrading.repository.NotificationMapper;
import com.usdttrading.repository.UserKycMapper;
import com.usdttrading.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * 通知服務
 * 統一處理各種類型的用戶通知，包括站內通知和郵件通知
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final UserKycMapper userKycMapper;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;

    /**
     * 發送KYC提交通知
     *
     * @param userId 用戶ID
     * @param kycId KYC ID
     */
    public void sendKycSubmissionNotification(Long userId, Long kycId) {
        try {
            // 創建站內通知
            createNotification(
                userId,
                NotificationType.KYC_SUBMITTED,
                NotificationCategory.KYC,
                NotificationPriority.NORMAL,
                "KYC身份驗證已提交",
                "您的KYC身份驗證申請已成功提交，我們將在1-3個工作日內完成審核。",
                String.format("{\"kycId\": %d}", kycId)
            );

            // 發送郵件通知
            User user = userMapper.selectById(userId);
            if (user != null && user.getEmail() != null) {
                sendKycSubmissionEmail(user.getEmail(), userId, kycId);
            }

            log.info("KYC提交通知已發送: userId={}, kycId={}", userId, kycId);

        } catch (Exception e) {
            log.error("發送KYC提交通知失敗: userId={}, kycId={}, error={}", userId, kycId, e.getMessage(), e);
        }
    }

    /**
     * 發送KYC審核通過通知
     *
     * @param userId 用戶ID
     * @param kycId KYC ID
     */
    public void sendKycApprovalNotification(Long userId, Long kycId) {
        try {
            UserKyc kyc = userKycMapper.selectById(kycId);
            
            // 創建站內通知
            createNotification(
                userId,
                NotificationType.KYC_APPROVED,
                NotificationCategory.KYC,
                NotificationPriority.HIGH,
                "KYC身份驗證已通過",
                String.format("恭喜！您的KYC身份驗證已通過審核，現在可以享受完整的交易功能。KYC等級：L%d", 
                             kyc != null ? kyc.getKycLevel() : 1),
                String.format("{\"kycId\": %d, \"kycLevel\": %d}", kycId, kyc != null ? kyc.getKycLevel() : 1)
            );

            // 發送郵件通知
            User user = userMapper.selectById(userId);
            if (user != null && user.getEmail() != null) {
                sendKycApprovalEmail(user.getEmail(), userId, kycId, kyc);
            }

            log.info("KYC審核通過通知已發送: userId={}, kycId={}", userId, kycId);

        } catch (Exception e) {
            log.error("發送KYC審核通過通知失敗: userId={}, kycId={}, error={}", userId, kycId, e.getMessage(), e);
        }
    }

    /**
     * 發送KYC審核拒絕通知
     *
     * @param userId 用戶ID
     * @param kycId KYC ID
     * @param reason 拒絕原因
     */
    public void sendKycRejectionNotification(Long userId, Long kycId, String reason) {
        try {
            // 創建站內通知
            createNotification(
                userId,
                NotificationType.KYC_REJECTED,
                NotificationCategory.KYC,
                NotificationPriority.HIGH,
                "KYC身份驗證未通過",
                String.format("很抱歉，您的KYC身份驗證未能通過審核。原因：%s。您可以修正後重新提交申請。", reason),
                String.format("{\"kycId\": %d, \"reason\": \"%s\"}", kycId, reason)
            );

            // 發送郵件通知
            User user = userMapper.selectById(userId);
            if (user != null && user.getEmail() != null) {
                sendKycRejectionEmail(user.getEmail(), userId, kycId, reason);
            }

            log.info("KYC審核拒絕通知已發送: userId={}, kycId={}, reason={}", userId, kycId, reason);

        } catch (Exception e) {
            log.error("發送KYC審核拒絕通知失敗: userId={}, kycId={}, error={}", userId, kycId, e.getMessage(), e);
        }
    }

    /**
     * 發送KYC需要補充材料通知
     *
     * @param userId 用戶ID
     * @param kycId KYC ID
     * @param requirement 補充要求
     */
    public void sendKycSupplementRequiredNotification(Long userId, Long kycId, String requirement) {
        try {
            // 創建站內通知
            createNotification(
                userId,
                NotificationType.KYC_SUPPLEMENT_REQUIRED,
                NotificationCategory.KYC,
                NotificationPriority.HIGH,
                "KYC需要補充材料",
                String.format("您的KYC申請需要補充以下材料：%s。請儘快補充以完成身份驗證。", requirement),
                String.format("{\"kycId\": %d, \"requirement\": \"%s\"}", kycId, requirement)
            );

            // 發送郵件通知
            User user = userMapper.selectById(userId);
            if (user != null && user.getEmail() != null) {
                sendKycSupplementEmail(user.getEmail(), userId, kycId, requirement);
            }

            log.info("KYC補充材料通知已發送: userId={}, kycId={}, requirement={}", userId, kycId, requirement);

        } catch (Exception e) {
            log.error("發送KYC補充材料通知失敗: userId={}, kycId={}, error={}", userId, kycId, e.getMessage(), e);
        }
    }

    /**
     * 發送KYC重新提交通知
     *
     * @param userId 用戶ID
     * @param kycId KYC ID
     */
    public void sendKycResubmissionNotification(Long userId, Long kycId) {
        try {
            // 創建站內通知
            createNotification(
                userId,
                NotificationType.KYC_RESUBMITTED,
                NotificationCategory.KYC,
                NotificationPriority.NORMAL,
                "KYC已重新提交",
                "您的KYC身份驗證申請已重新提交，我們將重新進行審核。",
                String.format("{\"kycId\": %d}", kycId)
            );

            log.info("KYC重新提交通知已發送: userId={}, kycId={}", userId, kycId);

        } catch (Exception e) {
            log.error("發送KYC重新提交通知失敗: userId={}, kycId={}, error={}", userId, kycId, e.getMessage(), e);
        }
    }

    /**
     * 發送KYC人工審核通知（給管理員）
     *
     * @param kycId KYC ID
     */
    public void sendKycManualReviewNotification(Long kycId) {
        try {
            // 這裡可以給管理員發送通知
            // 或者推送到管理員的工作台
            log.info("KYC需要人工審核: kycId={}", kycId);

            // 可以發送到管理員群組郵箱或通過其他方式通知
            
        } catch (Exception e) {
            log.error("發送KYC人工審核通知失敗: kycId={}, error={}", kycId, e.getMessage(), e);
        }
    }

    /**
     * 創建通知記錄
     */
    @Transactional
    protected void createNotification(Long userId, NotificationType type, NotificationCategory category,
                                     NotificationPriority priority, String title, String content, String metadata) {
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setCategory(category);
            notification.setPriority(priority);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setMetadata(metadata);
            notification.setStatus(NotificationStatus.UNREAD);
            notification.setCreatedAt(LocalDateTime.now());

            notificationMapper.insert(notification);

        } catch (Exception e) {
            log.error("創建通知記錄失敗: userId={}, type={}, error={}", userId, type, e.getMessage(), e);
        }
    }

    /**
     * 發送KYC提交郵件
     */
    private void sendKycSubmissionEmail(String email, Long userId, Long kycId) {
        CompletableFuture.runAsync(() -> {
            try {
                Context context = new Context();
                context.setVariable("kycId", kycId);
                context.setVariable("userId", userId);
                context.setVariable("submissionTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                context.setVariable("expectedProcessingDays", "1-3個工作日");

                String htmlContent = templateEngine.process("kyc-submission-notification", context);
                emailService.sendTextEmail(email, "KYC身份驗證提交成功", htmlContent, userId, "KYC_SUBMISSION");

            } catch (Exception e) {
                log.error("發送KYC提交郵件失敗: email={}, userId={}, kycId={}", email, userId, kycId, e);
            }
        });
    }

    /**
     * 發送KYC審核通過郵件
     */
    private void sendKycApprovalEmail(String email, Long userId, Long kycId, UserKyc kyc) {
        CompletableFuture.runAsync(() -> {
            try {
                Context context = new Context();
                context.setVariable("kycId", kycId);
                context.setVariable("userId", userId);
                context.setVariable("kycLevel", kyc != null ? kyc.getKycLevel() : 1);
                context.setVariable("approvalTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                context.setVariable("expiryTime", kyc != null && kyc.getExpiresAt() != null ? 
                    kyc.getExpiresAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "一年後");

                String htmlContent = templateEngine.process("kyc-approval-notification", context);
                emailService.sendTextEmail(email, "KYC身份驗證通過", htmlContent, userId, "KYC_APPROVAL");

            } catch (Exception e) {
                log.error("發送KYC審核通過郵件失敗: email={}, userId={}, kycId={}", email, userId, kycId, e);
            }
        });
    }

    /**
     * 發送KYC審核拒絕郵件
     */
    private void sendKycRejectionEmail(String email, Long userId, Long kycId, String reason) {
        CompletableFuture.runAsync(() -> {
            try {
                Context context = new Context();
                context.setVariable("kycId", kycId);
                context.setVariable("userId", userId);
                context.setVariable("rejectionReason", reason);
                context.setVariable("rejectionTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                String htmlContent = templateEngine.process("kyc-rejection-notification", context);
                emailService.sendTextEmail(email, "KYC身份驗證未通過", htmlContent, userId, "KYC_REJECTION");

            } catch (Exception e) {
                log.error("發送KYC審核拒絕郵件失敗: email={}, userId={}, kycId={}", email, userId, kycId, e);
            }
        });
    }

    /**
     * 發送KYC補充材料郵件
     */
    private void sendKycSupplementEmail(String email, Long userId, Long kycId, String requirement) {
        CompletableFuture.runAsync(() -> {
            try {
                Context context = new Context();
                context.setVariable("kycId", kycId);
                context.setVariable("userId", userId);
                context.setVariable("supplementRequirement", requirement);
                context.setVariable("requestTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                String htmlContent = templateEngine.process("kyc-supplement-notification", context);
                emailService.sendTextEmail(email, "KYC需要補充材料", htmlContent, userId, "KYC_SUPPLEMENT");

            } catch (Exception e) {
                log.error("發送KYC補充材料郵件失敗: email={}, userId={}, kycId={}", email, userId, kycId, e);
            }
        });
    }

    /**
     * 標記通知為已讀
     *
     * @param notificationId 通知ID
     * @param userId 用戶ID
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        try {
            QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", notificationId);
            queryWrapper.eq("user_id", userId);
            
            Notification notification = notificationMapper.selectOne(queryWrapper);
            if (notification != null && NotificationStatus.UNREAD.equals(notification.getStatus())) {
                notification.setStatus(NotificationStatus.READ);
                notification.setReadAt(LocalDateTime.now());
                notificationMapper.updateById(notification);
            }

        } catch (Exception e) {
            log.error("標記通知為已讀失敗: notificationId={}, userId={}, error={}", 
                     notificationId, userId, e.getMessage(), e);
        }
    }

    /**
     * 批量標記通知為已讀
     *
     * @param userId 用戶ID
     * @param category 通知類別（可選）
     */
    @Transactional
    public void markAllAsRead(Long userId, NotificationCategory category) {
        try {
            QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId);
            queryWrapper.eq("status", NotificationStatus.UNREAD);
            
            if (category != null) {
                queryWrapper.eq("category", category);
            }

            Notification updateNotification = new Notification();
            updateNotification.setStatus(NotificationStatus.READ);
            updateNotification.setReadAt(LocalDateTime.now());
            
            notificationMapper.update(updateNotification, queryWrapper);

            log.info("批量標記通知為已讀: userId={}, category={}", userId, category);

        } catch (Exception e) {
            log.error("批量標記通知為已讀失敗: userId={}, category={}, error={}", 
                     userId, category, e.getMessage(), e);
        }
    }

    /**
     * 發送訂單通知
     *
     * @param userId 用戶ID
     * @param orderId 訂單ID
     * @param title 通知標題
     * @param content 通知內容
     */
    public void sendOrderNotification(Long userId, Long orderId, String title, String content) {
        try {
            // 創建站內通知
            createNotification(
                userId,
                NotificationType.ORDER_STATUS_CHANGED,
                NotificationCategory.TRANSACTION,
                NotificationPriority.NORMAL,
                title,
                content,
                String.format("{\"orderId\": %d}", orderId)
            );

            // 發送郵件通知
            User user = userMapper.selectById(userId);
            if (user != null && user.getEmail() != null) {
                sendOrderEmailNotification(user.getEmail(), userId, orderId, title, content);
            }

            log.info("訂單通知已發送: userId={}, orderId={}, title={}", userId, orderId, title);

        } catch (Exception e) {
            log.error("發送訂單通知失敗: userId={}, orderId={}, error={}", userId, orderId, e.getMessage(), e);
        }
    }

    /**
     * 發送訂單郵件通知
     */
    private void sendOrderEmailNotification(String email, Long userId, Long orderId, String title, String content) {
        CompletableFuture.runAsync(() -> {
            try {
                Context context = new Context();
                context.setVariable("orderId", orderId);
                context.setVariable("userId", userId);
                context.setVariable("title", title);
                context.setVariable("content", content);
                context.setVariable("notificationTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                // 使用通用的通知模板
                String htmlContent = templateEngine.process("order-notification", context);
                emailService.sendTextEmail(email, title, htmlContent, userId, "ORDER_NOTIFICATION");

            } catch (Exception e) {
                log.error("發送訂單郵件通知失敗: email={}, userId={}, orderId={}", email, userId, orderId, e);
            }
        });
    }

    /**
     * 獲取未讀通知數量
     *
     * @param userId 用戶ID
     * @return 未讀通知數量
     */
    public long getUnreadCount(Long userId) {
        try {
            QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId);
            queryWrapper.eq("status", NotificationStatus.UNREAD);
            
            return notificationMapper.selectCount(queryWrapper);

        } catch (Exception e) {
            log.error("獲取未讀通知數量失敗: userId={}, error={}", userId, e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 發送交易通知
     *
     * @param fromUserId 發送方用戶ID
     * @param toUserId 接收方用戶ID
     * @param type 通知類型
     * @param message 通知消息
     */
    public void sendTransactionNotification(Long fromUserId, Long toUserId, String type, String message) {
        try {
            createNotification(
                toUserId,
                NotificationType.TRANSACTION_UPDATE,
                NotificationCategory.TRANSACTION,
                NotificationPriority.NORMAL,
                "交易通知",
                message,
                String.format("{\"fromUserId\": %d, \"type\": \"%s\"}", fromUserId, type)
            );

            log.info("交易通知已發送: fromUserId={}, toUserId={}, type={}", fromUserId, toUserId, type);

        } catch (Exception e) {
            log.error("發送交易通知失敗: fromUserId={}, toUserId={}, type={}, error={}", 
                     fromUserId, toUserId, type, e.getMessage(), e);
        }
    }
}