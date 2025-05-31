package com.boyz.streaming.flow.configuration;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

@Configuration
@ConfigurationProperties("cat.mood.s3")
@Data
public class S3Configuration {
    String accessKey;
    String secretKey;
    String region;
    String endpoint;
    String musicBucket;

    @Bean
    AwsCredentials getAwsCredentials() {
        return AwsBasicCredentials.create(accessKey, secretKey);
    }

    @Bean
    Region getRegion() {
        return Region.of(region);
    }

    @Bean
    URI getEndpoint() {
        return URI.create(endpoint);
    }

    @Bean
    String getMusicBucket() {
        return musicBucket;
    }
}
