package com.coociks.temfex.config;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MinioBucketConfig {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Bean
    public ApplicationRunner initializeMinioBucket() {
        return args -> {
            String bucketName = minioConfig.getBucketName();
            
            // Проверяем, существует ли бакет, и создаём если нет
            boolean bucketExists = minioClient.bucketExists(
                io.minio.BucketExistsArgs.builder().bucket(bucketName).build()
            );
            
            if (!bucketExists) {
                System.out.println(" Создание бакета MinIO: " + bucketName);
                minioClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
                );
                
                // Устанавливаем политику, чтобы файлы можно было скачивать по прямой ссылке (опционально)
                String policy = String.format("""
                {
                    "Version": "2012-10-17",
                    "Statement": [{
                        "Effect": "Allow",
                        "Principal": {"AWS": ["*"]},
                        "Action": ["s3:GetObject"],
                        "Resource": ["arn:aws:s3:::%s/*"]
                    }]
                }
                """, bucketName);
                
                minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policy)
                        .build()
                );
                
                System.out.println("✅ Бакет " + bucketName + " успешно создан!");
            } else {
                System.out.println("✅ Бакет " + bucketName + " уже существует");
            }
        };
    }
}