package com.memories.platform.media.service;

import com.memories.platform.config.MediaStorageProperties;
import com.memories.platform.media.dto.MediaUploadTarget;
import com.memories.platform.media.entity.MediaAsset;
import com.memories.platform.media.exception.MediaStorageUnavailableException;
import com.memories.platform.media.exception.MediaUploadVerificationException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class MediaObjectStorageService {

    private static final int MAGIC_BYTE_RANGE_END = 63;

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final MediaStorageProperties properties;

    public MediaObjectStorageService(
            S3Client s3Client,
            S3Presigner presigner,
            MediaStorageProperties properties
    ) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.properties = properties;
    }

    public MediaUploadTarget presignUpload(MediaAsset asset) {
        try {
            PutObjectRequest.Builder objectRequest = PutObjectRequest.builder()
                    .bucket(asset.getBucketName())
                    .key(asset.getObjectKey())
                    .contentType(asset.getMimeType())
                    .contentLength(asset.getFileSize());
            Map<String, String> requiredHeaders = new LinkedHashMap<>();
            requiredHeaders.put("Content-Type", asset.getMimeType());
            if (asset.getChecksum() != null) {
                objectRequest.checksumSHA256(asset.getChecksum());
                requiredHeaders.put("X-Amz-Checksum-Sha256", asset.getChecksum());
            }
            PresignedPutObjectRequest request = presigner.presignPutObject(
                    PutObjectPresignRequest.builder()
                            .signatureDuration(properties.presignedPutTtl())
                            .putObjectRequest(objectRequest.build())
                            .build()
            );
            return new MediaUploadTarget(
                    request.url().toExternalForm(),
                    Map.copyOf(requiredHeaders)
            );
        } catch (RuntimeException exception) {
            throw new MediaStorageUnavailableException(exception);
        }
    }

    public void verifyUpload(MediaAsset asset) {
        try {
            HeadObjectResponse head = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(asset.getBucketName())
                            .key(asset.getObjectKey())
                            .checksumMode(ChecksumMode.ENABLED)
                            .build()
            );
            if (head.contentLength() == null || head.contentLength() != asset.getFileSize()) {
                throw new MediaUploadVerificationException();
            }
            if (!normalizeMimeType(head.contentType()).equals(asset.getMimeType())) {
                throw new MediaUploadVerificationException();
            }
            if (asset.getChecksum() != null
                    && head.checksumSHA256() != null
                    && !asset.getChecksum().equals(head.checksumSHA256())) {
                throw new MediaUploadVerificationException();
            }

            ResponseBytes<GetObjectResponse> objectPrefix = s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(asset.getBucketName())
                            .key(asset.getObjectKey())
                            .range("bytes=0-" + MAGIC_BYTE_RANGE_END)
                            .build(),
                    ResponseTransformer.toBytes()
            );
            if (!asset.getMimeType().equals(detectImageMimeType(objectPrefix.asByteArray()))) {
                throw new MediaUploadVerificationException();
            }
        } catch (MediaUploadVerificationException exception) {
            throw exception;
        } catch (NoSuchKeyException exception) {
            throw new MediaUploadVerificationException();
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new MediaUploadVerificationException();
            }
            throw new MediaStorageUnavailableException(exception);
        } catch (RuntimeException exception) {
            throw new MediaStorageUnavailableException(exception);
        }
    }

    public String presignDelivery(MediaAsset asset) {
        try {
            return presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .signatureDuration(properties.presignedGetTtl())
                            .getObjectRequest(request -> request
                                    .bucket(asset.getBucketName())
                                    .key(asset.getObjectKey()))
                            .build()
            ).url().toExternalForm();
        } catch (RuntimeException exception) {
            throw new MediaStorageUnavailableException(exception);
        }
    }

    private String normalizeMimeType(String mimeType) {
        if (mimeType == null) {
            return "";
        }
        return mimeType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private String detectImageMimeType(byte[] bytes) {
        if (bytes.length >= 3
                && unsigned(bytes[0]) == 0xff
                && unsigned(bytes[1]) == 0xd8
                && unsigned(bytes[2]) == 0xff) {
            return "image/jpeg";
        }
        if (bytes.length >= 8
                && unsigned(bytes[0]) == 0x89
                && bytes[1] == 'P'
                && bytes[2] == 'N'
                && bytes[3] == 'G'
                && unsigned(bytes[4]) == 0x0d
                && unsigned(bytes[5]) == 0x0a
                && unsigned(bytes[6]) == 0x1a
                && unsigned(bytes[7]) == 0x0a) {
            return "image/png";
        }
        if (bytes.length >= 12
                && ascii(bytes, 0, 4).equals("RIFF")
                && ascii(bytes, 8, 4).equals("WEBP")) {
            return "image/webp";
        }
        if (bytes.length >= 16 && ascii(bytes, 4, 4).equals("ftyp")) {
            for (int offset = 8; offset + 4 <= bytes.length; offset += 4) {
                String brand = ascii(bytes, offset, 4);
                if (brand.equals("avif") || brand.equals("avis")) {
                    return "image/avif";
                }
            }
        }
        throw new MediaUploadVerificationException();
    }

    private int unsigned(byte value) {
        return value & 0xff;
    }

    private String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }
}
