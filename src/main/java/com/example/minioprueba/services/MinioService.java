package com.example.minioprueba.services;

import com.example.minioprueba.dtos.AudioFileDto;
import com.example.minioprueba.entities.AudioFileEntity;
import com.example.minioprueba.exceptions.MinioException;
import com.example.minioprueba.repositories.AudioFileRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@Service
public class MinioService {

    private final MinioClient minioClient;
    private final AudioFileRepository repository;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public MinioService(MinioClient minioClient, AudioFileRepository repository) {
        this.minioClient = minioClient;
        this.repository = repository;
    }

    /**
     * Subir un archivo MP3 a MinIO y guardar metadatos en la base de datos
     */
    @Transactional
    public AudioFileEntity uploadAudioFile(MultipartFile file) {
        if (!"audio/mpeg".equals(file.getContentType())) {
            throw new MinioException("Solo se permiten archivos MP3.");
        }

        String uniqueFileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        saveMinioMp3(file, uniqueFileName);

        long durationInSeconds = getMp3Duration(file);

        return saveAudioFile(file, uniqueFileName, durationInSeconds);
    }

    /**
     * Obtener un listado paginado de archivos MP3
     */
    public Page<AudioFileDto> getPaginatedAudioFiles(Pageable pageable) {
        return repository.findAll(pageable)
                .map(audio -> new AudioFileDto(audio.getId(), audio.getFileName(), audio.getDuration()));
    }

    /**
     * Reproducir un archivo MP3 desde MinIO (Streaming)
     */
    public Optional<ResponseEntity<?>> streamAudio(Long id) {
        Optional<AudioFileEntity> audioFile = repository.findById(id);
        if (audioFile.isEmpty()) {
            return Optional.of(ResponseEntity.notFound().build());
        }

        try {
            InputStream stream = downloadFile(audioFile.get().getFileUrl());
            return Optional.of(ResponseEntity.ok()
                    .contentType(MediaType.valueOf(audioFile.get().getFileType()))
                    .body(new InputStreamResource(stream))); // Streaming real
        } catch (Exception e) {
            return Optional.of(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al reproducir archivo"));
        }
    }


    //---------------------------------------- Métodos auxiliares ---------------------------------------------------

    private InputStream downloadFile(String fileName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build()
        );
    }

    private AudioFileEntity saveAudioFile(MultipartFile file, String uniqueFileName, long durationInSeconds) {
        AudioFileEntity audioFile = new AudioFileEntity();
        audioFile.setFileName(file.getOriginalFilename());
        audioFile.setFileType(file.getContentType());
        audioFile.setFileSize(file.getSize());
        audioFile.setFileUrl(uniqueFileName);
        audioFile.setDuration(durationInSeconds);
        return repository.save(audioFile);
    }

    private void saveMinioMp3(MultipartFile file, String uniqueFileName) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(uniqueFileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType("audio/mpeg")
                            .build()
            );
        } catch (Exception e) {
            throw new MinioException("Error al subir el archivo a MinIO.");
        }
    }

    /**
     * Obtener la duración de un archivo MP3 en segundos
     */
    private long getMp3Duration(MultipartFile file) {
        try {
            Path tempFilePath = Files.createTempFile("temp-audio", ".mp3");
            file.transferTo(tempFilePath.toFile());

            AudioFile audioFile = AudioFileIO.read(tempFilePath.toFile());
            MP3AudioHeader audioHeader = (MP3AudioHeader) audioFile.getAudioHeader();

            Files.delete(tempFilePath); // Eliminar archivo temporal
            return audioHeader.getTrackLength();
        } catch (Exception e) {
            throw new MinioException("No se pudo obtener la duración del archivo.");
        }
    }
}
