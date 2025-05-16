package cat.mood.streaming.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class S3Service {
    final AwsCredentials awsCredentials;
    final Region region;
    final URI endpoint;
    final String musicBucket;
    final S3AsyncClient s3AsyncClient;

    @Autowired
    S3Service(AwsCredentials awsCredentials, Region region, URI endpoint, String musicBucket) {
        this.awsCredentials = awsCredentials;
        this.region = region;
        this.endpoint = endpoint;
        this.musicBucket = musicBucket;

        this.s3AsyncClient = S3AsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.AWS_GLOBAL)
                .endpointOverride(endpoint)
                .build();
    }

    public CompletableFuture<PutObjectResponse> uploadSong(byte[] bytes, String key) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes cannot be null");
        }

        PutObjectRequest request = PutObjectRequest.builder()
                            .key(key)
                            .bucket(musicBucket)
                            .build();
        return s3AsyncClient.putObject(
                request,
                AsyncRequestBody.fromBytes(bytes)
        );
    }
}
