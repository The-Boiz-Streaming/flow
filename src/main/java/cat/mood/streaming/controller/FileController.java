package cat.mood.streaming.controller;

import cat.mood.streaming.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {
    final S3Service s3Service;

    @GetMapping
    public Mono<?> hello() {
        log.info("hello");
        return Mono.just("Hello World");
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequestMapping("/upload")
    public Mono<ResponseEntity<String>> upload(@RequestPart("file") FilePart filePart) {
        UUID uuid = UUID.randomUUID();
        var response = DataBufferUtils.join(filePart.content()).map(
                dataBuffer -> {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableByteCount());
                    dataBuffer.toByteBuffer(byteBuffer);
                    return s3Service.uploadSong(byteBuffer.array(), uuid.toString());

//                    response.whenComplete((res, ex) -> {
//                        if (ex != null) {
//                            log.error("Upload failed for key {}: {}", uuid, ex.getMessage(), ex);
//                        } else {
//                            log.debug("Upload succeeded for key {}. ETag: {}", uuid, res.eTag());
//                        }
//                    });
//
//                    return ResponseEntity.status(HttpStatus.OK).body("Upload successful. UUID: " + uuid);
                }
        );

        
    }
}
