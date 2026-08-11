package com.municipal.backend.service;

import com.municipal.backend.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Service
public class StorageService implements AutoCloseable {
    private final String mode;
    private final Path root;
    private final String bucket;
    private final int signedUrlMinutes;
    private final S3Client s3;
    private final S3Presigner presigner;

    public StorageService(
        @Value("${app.storage.mode:local}") String mode,
        @Value("${app.storage.location:./uploads}") String location,
        @Value("${app.storage.s3.endpoint:}") String endpoint,
        @Value("${app.storage.s3.region:}") String region,
        @Value("${app.storage.s3.access-key:}") String accessKey,
        @Value("${app.storage.s3.secret-key:}") String secretKey,
        @Value("${app.storage.s3.bucket:evidencias}") String bucket,
        @Value("${app.storage.s3.signed-url-minutes:15}") int signedUrlMinutes
    ) {
        this.mode = mode == null ? "local" : mode.trim().toLowerCase(Locale.ROOT);
        this.root = Path.of(location).toAbsolutePath().normalize();
        this.bucket = bucket;
        this.signedUrlMinutes = Math.max(1, Math.min(signedUrlMinutes, 60));

        if (isS3()) {
            requireS3Value(endpoint, "SUPABASE_S3_ENDPOINT");
            requireS3Value(region, "SUPABASE_S3_REGION");
            requireS3Value(accessKey, "SUPABASE_S3_ACCESS_KEY");
            requireS3Value(secretKey, "SUPABASE_S3_SECRET_KEY");
            requireS3Value(bucket, "SUPABASE_STORAGE_BUCKET");

            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            StaticCredentialsProvider provider = StaticCredentialsProvider.create(credentials);
            S3Configuration configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .checksumValidationEnabled(false)
                .build();
            URI endpointUri = URI.create(endpoint);
            Region awsRegion = Region.of(region);

            this.s3 = S3Client.builder()
                .endpointOverride(endpointUri)
                .region(awsRegion)
                .credentialsProvider(provider)
                .serviceConfiguration(configuration)
                .build();
            this.presigner = S3Presigner.builder()
                .endpointOverride(endpointUri)
                .region(awsRegion)
                .credentialsProvider(provider)
                .serviceConfiguration(configuration)
                .build();
        } else {
            this.s3 = null;
            this.presigner = null;
            try {
                Files.createDirectories(root);
            } catch (IOException ex) {
                throw new IllegalStateException("No se pudo crear la carpeta de fotografías.", ex);
            }
        }
    }

    public String storeImage(MultipartFile file, String prefix) {
        validateImage(file);
        String extension = extension(file.getOriginalFilename());
        String safePrefix = sanitizePrefix(prefix);
        String filename = UUID.randomUUID() + extension;

        if (isS3()) {
            String key = safePrefix + "/" + filename;
            try (InputStream input = file.getInputStream()) {
                PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
                s3.putObject(request, RequestBody.fromInputStream(input, file.getSize()));
                return key;
            } catch (Exception ex) {
                throw new BusinessException("No fue posible guardar la fotografía en el almacenamiento seguro.");
            }
        }

        String localFilename = safePrefix + "_" + filename;
        try {
            Path destination = root.resolve(localFilename).normalize();
            if (!destination.startsWith(root)) {
                throw new BusinessException("Nombre de archivo inválido.");
            }
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + localFilename;
        } catch (IOException ex) {
            throw new BusinessException("No fue posible guardar la fotografía.");
        }
    }

    /**
     * En local conserva /uploads/...; en Supabase devuelve una URL firmada temporal.
     * Así el bucket puede permanecer privado y Android no recibe las credenciales S3.
     */
    public String resolveUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return null;
        if (!isS3() || storedPath.startsWith("http://") || storedPath.startsWith("https://") || storedPath.startsWith("/uploads/")) {
            return storedPath;
        }

        try {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storedPath)
                .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(signedUrlMinutes))
                .getObjectRequest(objectRequest)
                .build();
            return presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception ex) {
            throw new BusinessException("No fue posible preparar el acceso seguro a la fotografía.");
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("La fotografía es obligatoria.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equalsIgnoreCase("image/jpeg")
            || contentType.equalsIgnoreCase("image/png")
            || contentType.equalsIgnoreCase("image/webp"))) {
            throw new BusinessException("La fotografía debe ser JPG, PNG o WebP.");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new BusinessException("La fotografía no puede superar 10 MB.");
        }
    }

    private String sanitizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) return "evidencias";
        String value = prefix.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return value.isBlank() ? "evidencias" : value;
    }

    private String extension(String original) {
        if (original == null) return ".jpg";
        int index = original.lastIndexOf('.');
        if (index < 0 || index == original.length() - 1) return ".jpg";
        String value = original.substring(index).toLowerCase(Locale.ROOT);
        return value.matches("\\.(jpg|jpeg|png|webp)") ? value : ".jpg";
    }

    private boolean isS3() {
        return "s3".equals(mode);
    }

    private void requireS3Value(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta configurar la variable " + variableName + ".");
        }
    }

    @Override
    public void close() {
        if (s3 != null) s3.close();
        if (presigner != null) presigner.close();
    }
}
