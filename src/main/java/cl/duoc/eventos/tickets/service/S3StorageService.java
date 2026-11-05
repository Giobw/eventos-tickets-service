package cl.duoc.eventos.tickets.service;

import cl.duoc.eventos.tickets.exception.NotFoundException;
import cl.duoc.eventos.tickets.exception.StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** Acceso a AWS S3. Las claves siguen la estructura {eventId}/{userId}/{archivo}.pdf */
@Service
public class S3StorageService {

    private final S3Client s3;
    private final String bucket;

    public S3StorageService(S3Client s3, @Value("${app.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public String getBucket() {
        return bucket;
    }

    public void upload(String key, byte[] content) {
        requireBucket();
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType("application/pdf")
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (SdkException e) {
            throw new StorageException("No se pudo subir el archivo a S3 (" + key + "): " + e.getMessage(), e);
        }
    }

    public byte[] download(String key) {
        requireBucket();
        try {
            return s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new NotFoundException("El objeto no existe en S3: " + key);
        } catch (SdkException e) {
            throw new StorageException("No se pudo descargar el archivo desde S3 (" + key + "): " + e.getMessage(), e);
        }
    }

    public void delete(String key) {
        requireBucket();
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (SdkException e) {
            throw new StorageException("No se pudo eliminar el objeto de S3 (" + key + "): " + e.getMessage(), e);
        }
    }

    private void requireBucket() {
        if (bucket == null || bucket.isBlank()) {
            throw new StorageException("El bucket de S3 no está configurado (variable de entorno S3_BUCKET)");
        }
    }
}
