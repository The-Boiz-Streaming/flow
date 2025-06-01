package com.boyz.streaming.flow.service;

import com.boyz.streaming.flow.dto.SendRequestDTO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@Data
public class S3Service {
    final AwsCredentials awsCredentials;
    final Region region;
    final URI endpoint;
    final String musicBucket;
    final S3AsyncClient s3AsyncClient;
    final KafkaProducer kafkaProducer;

    @Autowired
    S3Service(AwsCredentials awsCredentials, Region region, URI endpoint, String musicBucket, KafkaProducer kafkaProducer) {
        this.awsCredentials = awsCredentials;
        this.region = region;
        this.endpoint = endpoint;
        this.musicBucket = musicBucket;
        this.kafkaProducer = kafkaProducer;

        this.s3AsyncClient = S3AsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.AWS_GLOBAL)
                .endpointOverride(endpoint)
                .forcePathStyle(true)
                .build();
    }

    public Mono<PutObjectResponse> uploadSong(byte[] bytes, String key) {
        if (bytes == null) {
            return Mono.error(new IllegalArgumentException("bytes cannot be null"));
        }

        PutObjectRequest request = PutObjectRequest.builder()
                            .key(key)
                            .bucket(musicBucket)
                            .build();
        return Mono.fromFuture(() -> s3AsyncClient.putObject(
                request,
                AsyncRequestBody.fromBytes(bytes)
        ))
        .doOnSuccess(response -> log.info("Successfully uploaded file: {}", key))
        .doOnError(error -> log.error("Error uploading file: {}", key, error));
    }

    public Mono<ResponseEntity<Flux<DataBuffer>>> streamSong(String key, String rangeHeader) {
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(musicBucket)
                .key(key)
                .build();

        kafkaProducer.send(new SendRequestDTO("flow", key));

        return Mono.fromFuture(s3AsyncClient.headObject(headRequest))
                .flatMap(head -> {
                    long fileSize = head.contentLength();

                    long rangeStart;
                    long rangeEnd;

                    boolean isPartial;

                    if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                        isPartial = true;
                        String[] ranges = rangeHeader.replace("bytes=", "").split("-");
                        rangeStart = Long.parseLong(ranges[0]);
                        if (ranges.length > 1 && !ranges[1].isEmpty()) {
                            rangeEnd = Long.parseLong(ranges[1]);
                        } else {
                            rangeEnd = fileSize - 1;
                        }
                    } else {
                        rangeEnd = fileSize - 1;
                        rangeStart = 0;
                        isPartial = false;
                    }

                    long contentLength = rangeEnd - rangeStart + 1;

                    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                            .bucket(musicBucket)
                            .key(key)
                            .range("bytes=" + rangeStart + "-" + rangeEnd)
                            .build();

                    return Mono.fromFuture(s3AsyncClient.getObject(getObjectRequest, AsyncResponseTransformer.toPublisher()))
                            .map(publisher -> {
                                Flux<DataBuffer> body = Flux.from(publisher)
                                        .map(byteBuffer -> {
                                            byte[] bytes = new byte[byteBuffer.remaining()];
                                            byteBuffer.get(bytes);
                                            return new DefaultDataBufferFactory().wrap(bytes);
                                        });

                                HttpHeaders headers = new HttpHeaders();
                                headers.setContentType(MediaType.valueOf("audio/mpeg"));
                                headers.set("Accept-Ranges", "bytes");
                                headers.setContentLength(contentLength);

                                if (isPartial) {
                                    headers.set(HttpHeaders.CONTENT_RANGE,
                                            String.format("bytes %d-%d/%d", rangeStart, rangeEnd, fileSize));
                                    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                                            .headers(headers)
                                            .body(body);
                                } else {
                                    return ResponseEntity.ok()
                                            .headers(headers)
                                            .body(body);
                                }
                            });
                });
    }

}
