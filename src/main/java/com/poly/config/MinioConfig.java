package com.poly.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
 
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {
 
    @Bean
    public MinioClient minioClient(MinioProperties props) throws Exception {

        MinioClient client = MinioClient.builder()
            .endpoint(props.url())
            .credentials(props.accessKey(), props.secretKey())
            .build();
 
        // Auto-create the bucket on startup if it doesn't exist
        boolean exists = client.bucketExists(
            BucketExistsArgs.builder().bucket(props.bucket()).build()
        );
        if (!exists) {
            client.makeBucket(
                MakeBucketArgs.builder().bucket(props.bucket()).build()
            );
        }
        
        // Set some bucket to be public
        String policy = """
            {
            "Version":"2012-10-17",
            "Statement":[
                {
                "Effect":"Allow",
                "Principal":{"AWS":["*"]},
                "Action":["s3:GetObject"],
                "Resource":["arn:aws:s3:::%s/*"]
                }
            ]
            }
        """.formatted(props.bucket());

        client.setBucketPolicy(
            SetBucketPolicyArgs.builder()
                .bucket(props.bucket())
                .config(policy)
                .build()
        );

        return client;
    }
}
