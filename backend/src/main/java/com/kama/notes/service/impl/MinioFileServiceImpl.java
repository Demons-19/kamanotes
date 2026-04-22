package com.kama.notes.service.impl;

import com.kama.notes.config.MinioProperties;
import com.kama.notes.service.FileService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
public class MinioFileServiceImpl implements FileService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS =
            Arrays.asList(".jpg", ".jpeg", ".png", ".webp");

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        return uploadToMinio(file, "file");
    }

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传的图片文件为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("图片大小不能超过 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("图片文件名无效");
        }

        String lowerCaseExtension = originalFilename
                .substring(originalFilename.lastIndexOf("."))
                .toLowerCase();

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(lowerCaseExtension)) {
            throw new IllegalArgumentException(
                    "只支持 " + ALLOWED_IMAGE_EXTENSIONS + " 等格式图片");
        }

        return uploadToMinio(file, "avatar");
    }

    private String uploadToMinio(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("文件名不合法");
        }

        String fileExtension = originalFilename
                .substring(originalFilename.lastIndexOf("."))
                .toLowerCase();

        LocalDate today = LocalDate.now();
        String objectName = String.format(
                "%s/%d/%02d/%s%s",
                folder,
                today.getYear(),
                today.getMonthValue(),
                UUID.randomUUID(),
                fileExtension
        );

        try (InputStream inputStream = file.getInputStream()) {
            ensureBucketExists();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return buildPublicUrl(objectName);
        } catch (Exception e) {
            throw new IllegalStateException("文件上传 MinIO 失败: " + e.getMessage(), e);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .build()
        );
        if (!bucketExists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .build()
            );
        }
        // 设置 bucket 公开读取，否则上传后的 URL 会 403
        String policyJson = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + minioProperties.getBucket() + "/*\"]}]}";
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .config(policyJson)
                        .build()
        );
    }

    private String buildPublicUrl(String objectName) {
        String publicUrl = minioProperties.getPublicUrl();
        if (publicUrl.endsWith("/")) {
            return publicUrl + objectName;
        }
        return publicUrl + "/" + objectName;
    }
}
