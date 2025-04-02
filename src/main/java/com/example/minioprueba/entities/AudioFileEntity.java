package com.example.minioprueba.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class AudioFileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileUrl;
    private Long Duration;
}
