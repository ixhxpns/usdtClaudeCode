package com.usdttrading.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.entity.KycDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * KYC文檔數據訪問層
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Mapper
public interface KycDocumentMapper extends BaseMapper<KycDocument> {

    /**
     * 根據KYC ID查詢文檔列表
     *
     * @param kycId KYC ID
     * @return 文檔列表
     */
    @Select("SELECT * FROM kyc_documents WHERE kyc_id = #{kycId} AND deleted = false ORDER BY created_at ASC")
    List<KycDocument> selectByKycId(@Param("kycId") Long kycId);

    /**
     * 根據用戶ID查詢文檔列表
     *
     * @param userId 用戶ID
     * @return 文檔列表
     */
    @Select("SELECT * FROM kyc_documents WHERE user_id = #{userId} AND deleted = false ORDER BY created_at DESC")
    List<KycDocument> selectByUserId(@Param("userId") Long userId);

    /**
     * 根據文檔類型查詢
     *
     * @param kycId KYC ID
     * @param documentType 文檔類型
     * @return 文檔
     */
    @Select("SELECT * FROM kyc_documents WHERE kyc_id = #{kycId} AND document_type = #{documentType} AND deleted = false ORDER BY created_at DESC LIMIT 1")
    KycDocument selectByKycIdAndType(@Param("kycId") Long kycId, @Param("documentType") String documentType);

    /**
     * 查詢過期文檔
     *
     * @param expireTime 過期時間
     * @return 過期文檔列表
     */
    @Select("SELECT * FROM kyc_documents WHERE expires_at < #{expireTime} AND deleted = false")
    List<KycDocument> selectExpiredDocuments(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 統計用戶文檔數量
     *
     * @param userId 用戶ID
     * @return 文檔數量
     */
    @Select("SELECT COUNT(*) FROM kyc_documents WHERE user_id = #{userId} AND deleted = false")
    int countByUserId(@Param("userId") Long userId);

    /**
     * 分頁查詢文檔
     *
     * @param page 分頁對象
     * @param userId 用戶ID（可選）
     * @param documentType 文檔類型（可選）
     * @param status 狀態（可選）
     * @return 分頁結果
     */
    Page<KycDocument> selectDocumentsPage(Page<KycDocument> page, 
                                         @Param("userId") Long userId,
                                         @Param("documentType") String documentType,
                                         @Param("status") String status);
}