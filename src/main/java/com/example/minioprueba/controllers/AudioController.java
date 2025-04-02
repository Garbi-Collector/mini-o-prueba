package com.example.minioprueba.controllers;

import com.example.minioprueba.dtos.AudioFileDto;
import com.example.minioprueba.entities.AudioFileEntity;
import com.example.minioprueba.exceptions.MinioException;
import com.example.minioprueba.services.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.net.URI;

@RestController
@RequestMapping("/audio")
public class AudioController {

    @Autowired
    MinioService minioService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            AudioFileEntity savedAudio = minioService.uploadAudioFile(file);
            return ResponseEntity.created(URI.create("/audio/" + savedAudio.getId()))
                    .body("Archivo subido con éxito");
        } catch (MinioException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inesperado al subir el archivo.");
        }
    }

    @GetMapping("/list")
    public Page<AudioFileDto> listAudioFiles(Pageable pageable) {
        return minioService.getPaginatedAudioFiles(pageable);
    }

    @GetMapping("/stream/{id}")
    public ResponseEntity<?> streamAudio(@PathVariable Long id) {
        return minioService.streamAudio(id).orElse(ResponseEntity.status(500).body("Error interno"));
    }

}
