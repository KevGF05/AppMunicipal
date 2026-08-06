package com.municipal.backend.service;

import com.municipal.backend.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;

@Service
public class StorageService {
    private final Path root;

    public StorageService(@Value("${app.storage.location}") String location) {
        this.root = Path.of(location).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo crear la carpeta de fotografías.", ex);
        }
    }

    public String storeImage(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("La fotografía es obligatoria.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("El archivo adjunto debe ser una imagen.");
        }
        String extension = extension(file.getOriginalFilename());
        String filename = prefix + "_" + UUID.randomUUID() + extension;
        try {
            Path destination = root.resolve(filename).normalize();
            if (!destination.startsWith(root)) throw new BusinessException("Nombre de archivo inválido.");
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + filename;
        } catch (IOException ex) {
            throw new BusinessException("No fue posible guardar la fotografía.");
        }
    }

    private String extension(String original) {
        if (original == null) return ".jpg";
        int index = original.lastIndexOf('.');
        if (index < 0 || index == original.length() - 1) return ".jpg";
        String value = original.substring(index).toLowerCase(Locale.ROOT);
        return value.matches("\\.(jpg|jpeg|png|webp)") ? value : ".jpg";
    }
}
