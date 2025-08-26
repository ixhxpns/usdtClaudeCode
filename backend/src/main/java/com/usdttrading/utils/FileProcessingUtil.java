package com.usdttrading.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件處理工具類
 * 包含圖片處理、OCR識別、質量評估等功能
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Slf4j
@Component
public class FileProcessingUtil {

    private static final int MAX_IMAGE_WIDTH = 1920;
    private static final int MAX_IMAGE_HEIGHT = 1080;
    private static final int MIN_IMAGE_WIDTH = 300;
    private static final int MIN_IMAGE_HEIGHT = 200;

    // 身份證號碼正則表達式
    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
        "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$"
    );

    // 護照號碼正則表達式
    private static final Pattern PASSPORT_PATTERN = Pattern.compile(
        "^[A-Za-z]\\d{8}$|^[A-Za-z]{2}\\d{7}$"
    );

    /**
     * 為圖片添加水印
     *
     * @param originalImage 原始圖片
     * @param watermarkText 水印文字
     * @return 添加水印後的圖片
     */
    public BufferedImage addWatermark(BufferedImage originalImage, String watermarkText) {
        try {
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            // 創建新圖片
            BufferedImage watermarkedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = watermarkedImage.createGraphics();

            // 繪製原圖
            g2d.drawImage(originalImage, 0, 0, null);

            // 設置水印樣式
            g2d.setColor(new Color(255, 255, 255, 128)); // 半透明白色
            Font font = new Font("Arial", Font.BOLD, Math.max(width / 20, 12));
            g2d.setFont(font);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 計算水印位置
            FontMetrics fontMetrics = g2d.getFontMetrics();
            int textWidth = fontMetrics.stringWidth(watermarkText);
            int textHeight = fontMetrics.getHeight();

            // 在右下角添加水印
            int x = width - textWidth - 20;
            int y = height - 20;

            // 添加半透明背景
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRect(x - 10, y - textHeight, textWidth + 20, textHeight + 10);

            // 繪製水印文字
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.drawString(watermarkText, x, y);

            // 添加時間戳
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
            g2d.setFont(new Font("Arial", Font.PLAIN, Math.max(width / 30, 10)));
            FontMetrics timestampMetrics = g2d.getFontMetrics();
            int timestampWidth = timestampMetrics.stringWidth(timestamp);
            g2d.drawString(timestamp, width - timestampWidth - 20, y + 20);

            g2d.dispose();
            return watermarkedImage;

        } catch (Exception e) {
            log.error("添加水印失敗", e);
            return originalImage;
        }
    }

    /**
     * 壓縮圖片
     *
     * @param originalImage 原始圖片
     * @param maxWidth 最大寬度
     * @param maxHeight 最大高度
     * @param quality 圖片質量 (0.0-1.0)
     * @return 壓縮後的圖片
     */
    public BufferedImage compressImage(BufferedImage originalImage, int maxWidth, int maxHeight, float quality) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 如果圖片尺寸已經符合要求，直接返回
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return originalImage;
        }

        // 計算縮放比例
        double scaleX = (double) maxWidth / originalWidth;
        double scaleY = (double) maxHeight / originalHeight;
        double scale = Math.min(scaleX, scaleY);

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        // 創建縮放後的圖片
        BufferedImage compressedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = compressedImage.createGraphics();

        // 設置渲染質量
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return compressedImage;
    }

    /**
     * 評估圖片質量
     *
     * @param imageBytes 圖片字節數組
     * @return 質量評分 (1-10分)
     */
    public int assessImageQuality(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                return 1;
            }

            int score = 10;
            int width = image.getWidth();
            int height = image.getHeight();

            // 尺寸評估
            if (width < MIN_IMAGE_WIDTH || height < MIN_IMAGE_HEIGHT) {
                score -= 4; // 尺寸太小
            } else if (width > MAX_IMAGE_WIDTH || height > MAX_IMAGE_HEIGHT) {
                score -= 1; // 尺寸過大，但問題不大
            }

            // 文件大小評估
            double fileSizeKB = imageBytes.length / 1024.0;
            if (fileSizeKB < 50) {
                score -= 3; // 文件太小，可能壓縮過度
            } else if (fileSizeKB > 5120) { // 5MB
                score -= 1; // 文件過大
            }

            // 長寬比評估
            double aspectRatio = (double) width / height;
            if (aspectRatio < 0.5 || aspectRatio > 2.0) {
                score -= 2; // 長寬比不合理
            }

            // 亮度和對比度評估
            int brightnessScore = assessImageBrightness(image);
            score += (brightnessScore - 5); // -4 到 +4 的調整

            return Math.max(1, Math.min(10, score));

        } catch (Exception e) {
            log.error("圖片質量評估失敗", e);
            return 5; // 默認中等質量
        }
    }

    /**
     * 評估圖片亮度
     *
     * @param image 圖片
     * @return 亮度評分 (1-10)
     */
    private int assessImageBrightness(BufferedImage image) {
        long totalBrightness = 0;
        int pixelCount = 0;

        int width = image.getWidth();
        int height = image.getHeight();

        // 採樣評估，避免處理所有像素
        int stepX = Math.max(1, width / 100);
        int stepY = Math.max(1, height / 100);

        for (int x = 0; x < width; x += stepX) {
            for (int y = 0; y < height; y += stepY) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // 計算亮度 (使用標準權重)
                int brightness = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                totalBrightness += brightness;
                pixelCount++;
            }
        }

        int averageBrightness = (int) (totalBrightness / pixelCount);

        // 理想亮度範圍是 80-180
        if (averageBrightness >= 80 && averageBrightness <= 180) {
            return 8; // 良好亮度
        } else if (averageBrightness >= 60 && averageBrightness <= 200) {
            return 6; // 可接受亮度
        } else if (averageBrightness < 40 || averageBrightness > 220) {
            return 2; // 亮度很差
        } else {
            return 4; // 亮度一般
        }
    }

    /**
     * 模擬OCR識別（實際項目中應該集成真實的OCR服務）
     *
     * @param imageBytes 圖片字節數組
     * @param documentType 文檔類型
     * @return OCR識別結果
     */
    public Map<String, Object> performOCR(byte[] imageBytes, String documentType) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 這裡應該集成實際的OCR服務，如騰訊雲、阿里雲、百度等OCR API
            // 現在返回模擬數據

            result.put("success", true);
            result.put("accuracy", 0.95); // 模擬95%的識別準確度

            switch (documentType.toLowerCase()) {
                case "id_front":
                    result.put("name", "張三");
                    result.put("idNumber", "110101199001011234");
                    result.put("address", "北京市東城區某某街道");
                    break;
                case "id_back":
                    result.put("authority", "某某公安局");
                    result.put("validPeriod", "2020.01.01-2030.01.01");
                    break;
                case "passport":
                    result.put("passportNumber", "E12345678");
                    result.put("name", "ZHANG SAN");
                    result.put("nationality", "CHN");
                    break;
                default:
                    result.put("text", "OCR識別的文本內容");
            }

            log.info("OCR識別完成: documentType={}, accuracy={}", documentType, result.get("accuracy"));

        } catch (Exception e) {
            log.error("OCR識別失敗: documentType={}", documentType, e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("accuracy", 0.0);
        }

        return result;
    }

    /**
     * 模擬人臉識別比對
     *
     * @param faceImage1 第一張人臉圖片
     * @param faceImage2 第二張人臉圖片
     * @return 人臉匹配結果
     */
    public Map<String, Object> compareFaces(byte[] faceImage1, byte[] faceImage2) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 這裡應該集成實際的人臉識別服務
            // 現在返回模擬數據

            // 模擬匹配分數 (0-100)
            double matchScore = 85.5 + (Math.random() * 10); // 85.5-95.5之間的隨機分數

            result.put("success", true);
            result.put("matchScore", matchScore);
            result.put("isMatch", matchScore >= 80.0); // 80分以上認為匹配
            result.put("confidence", matchScore / 100.0);

            log.info("人臉比對完成: matchScore={}, isMatch={}", matchScore, matchScore >= 80.0);

        } catch (Exception e) {
            log.error("人臉比對失敗", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("matchScore", 0.0);
            result.put("isMatch", false);
        }

        return result;
    }

    /**
     * 驗證身份證號碼格式
     *
     * @param idNumber 身份證號碼
     * @return 是否為有效格式
     */
    public boolean validateIdNumber(String idNumber) {
        if (idNumber == null || idNumber.trim().isEmpty()) {
            return false;
        }

        return ID_CARD_PATTERN.matcher(idNumber.trim()).matches();
    }

    /**
     * 驗證護照號碼格式
     *
     * @param passportNumber 護照號碼
     * @return 是否為有效格式
     */
    public boolean validatePassportNumber(String passportNumber) {
        if (passportNumber == null || passportNumber.trim().isEmpty()) {
            return false;
        }

        return PASSPORT_PATTERN.matcher(passportNumber.trim().toUpperCase()).matches();
    }

    /**
     * 從身份證號碼提取出生日期
     *
     * @param idNumber 身份證號碼
     * @return 出生日期字符串 (yyyy-MM-dd)
     */
    public String extractBirthDateFromIdNumber(String idNumber) {
        if (!validateIdNumber(idNumber)) {
            return null;
        }

        try {
            String birthStr = idNumber.substring(6, 14);
            return birthStr.substring(0, 4) + "-" + birthStr.substring(4, 6) + "-" + birthStr.substring(6, 8);
        } catch (Exception e) {
            log.error("提取出生日期失敗: idNumber={}", idNumber, e);
            return null;
        }
    }

    /**
     * 從身份證號碼提取性別
     *
     * @param idNumber 身份證號碼
     * @return 性別 (MALE/FEMALE)
     */
    public String extractGenderFromIdNumber(String idNumber) {
        if (!validateIdNumber(idNumber)) {
            return null;
        }

        try {
            // 倒數第二位數字，奇數為男，偶數為女
            char genderChar = idNumber.charAt(idNumber.length() - 2);
            int genderDigit = Character.getNumericValue(genderChar);
            return (genderDigit % 2 == 1) ? "MALE" : "FEMALE";
        } catch (Exception e) {
            log.error("提取性別失敗: idNumber={}", idNumber, e);
            return null;
        }
    }

    /**
     * 計算年齡
     *
     * @param birthDate 出生日期字符串 (yyyy-MM-dd)
     * @return 年齡
     */
    public Integer calculateAge(String birthDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date birth = sdf.parse(birthDate);
            Date now = new Date();
            
            long ageInMillis = now.getTime() - birth.getTime();
            return (int) (ageInMillis / (365.25 * 24 * 60 * 60 * 1000));
        } catch (Exception e) {
            log.error("計算年齡失敗: birthDate={}", birthDate, e);
            return null;
        }
    }

    /**
     * 檢測圖片中的敏感內容
     *
     * @param imageBytes 圖片字節數組
     * @return 是否包含敏感內容
     */
    public boolean containsSensitiveContent(byte[] imageBytes) {
        try {
            // 這裡應該集成實際的內容審核服務
            // 現在返回模擬結果
            return false; // 假設沒有敏感內容
        } catch (Exception e) {
            log.error("敏感內容檢測失敗", e);
            return true; // 檢測失敗時保守處理
        }
    }

    /**
     * 將BufferedImage轉換為字節數組
     *
     * @param image BufferedImage對象
     * @param format 圖片格式 (jpg, png等)
     * @return 字節數組
     * @throws IOException IO異常
     */
    public byte[] imageToBytes(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }
}