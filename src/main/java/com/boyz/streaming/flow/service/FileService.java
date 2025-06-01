package com.boyz.streaming.flow.service;

import com.mpatric.mp3agic.Mp3File;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Data
@AllArgsConstructor
@Slf4j
public class FileService {
    public int calculateMp3DurationInSeconds(byte[] bytes) {
        try {
            // Создаем временный файл
            Path tempFile = Files.createTempFile("upload-", ".mp3");
            Files.write(tempFile, bytes);

            // Передаем путь в Mp3File
            Mp3File mp3File = new Mp3File(tempFile.toFile());

            // Удаляем временный файл
            Files.delete(tempFile);

            return (int) mp3File.getLengthInSeconds();
        } catch (Exception e) {
            log.error("Failed to parse mp3 duration", e);
            return 0;
        }
    }
}
