package com.boyz.streaming.flow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadResponseDTO {
    private UUID id;
    private Integer duration;
}
