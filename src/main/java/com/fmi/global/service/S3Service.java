package com.fmi.global.service;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class S3Service {
    private final S3Client s3Client;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    // S3에 저장된 이미지 객체의 public url을 반환
    public List<String> upload(List<MultipartFile> files) {
        // 각 파일을 업로드하고 url을 리스트로 반환
        return files.stream()
                .map(this::uploadImage)
                .toList();
    }

    // validateFile메서드를 호출하여 유효성 검증 후 uploadImageToS3메서드에 데이터를 반환하여 S3에 파일 업로드, public url을 받아 서비스 로직에 반환
    private String uploadImage(MultipartFile file) {
        validateFile(file.getOriginalFilename()); // 파일 유효성 검증
        return uploadImageToS3(file); // 이미지를 S3에 업로드하고, 저장된 파일의 public url을 서비스 로직에 반환
    }

    // 파일 유효성 검증
    private void validateFile(String filename) {
        // 파일 존재 유무 검증
        if (filename == null || filename.isEmpty()) {
            throw new GeneralException(ErrorStatus._NOT_EXIST_FILE);
        }

        // 확장자 존재 유무 검증
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new GeneralException(ErrorStatus._NOT_EXIST_FILE_EXTENSION);
        }

        // 허용되지 않는 확장자 검증
        String fileExtension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        List<String> allowedExtensions = Arrays.asList("jpg", "png", "jpeg");

        if (!allowedExtensions.contains(fileExtension)) {
            throw new GeneralException(ErrorStatus._INVALID_FILE_EXTENSION);
        }
    }

    // 직접적으로 S3에 업로드
    private String uploadImageToS3(MultipartFile file) {
        // 원본 파일 명
        String originalFilename = file.getOriginalFilename();
        // 확장자 명
        String extension = Objects.requireNonNull(originalFilename).substring(originalFilename.lastIndexOf(".") + 1);
        // 변경된 파일
        String s3FileName = UUID.randomUUID().toString().substring(0, 10) + "_" + originalFilename;

        // 이미지 파일 -> InputStream 변환
        try (InputStream inputStream = file.getInputStream()) {
            // PutObjectRequest 객체 생성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName) // 버킷 이름
                    .key(s3FileName) // 저장할 파일 이름
                    .contentType("image/" + extension) // 이미지 MIME 타입
                    .contentLength(file.getSize()) // 파일 크기
                    .build();
            // S3에 이미지 업로드
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            throw new GeneralException(ErrorStatus._IO_EXCEPTION_UPLOAD_FILE);
        }

        // public url 반환
        return s3Client.utilities().getUrl(url -> url.bucket(bucketName).key(s3FileName)).toString();
    }

    // 이미지의 public url을 이용하여 S3에서 해당 이미지를 제거, getKeyFromImageAddress 메서드를 호출하여 삭제에 필요한 key 획득
    public void delete(List<String> imageUrls) {
        List<String> keys = imageUrls.stream()
                .map(this::getKeyFromImageUrls)
                .toList();

        try {
            // S3에서 파일을 삭제하기 위한 요청 객체 생성
            DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName) // S3 버킷 이름 지정
                    .delete(delete -> delete.objects(
                            // S3 객체들을 삭제할 객체 목록을 생성
                            keys.stream()
                                    .map(key -> ObjectIdentifier.builder().key(key).build())
                                    .toList()
                    ))
                    .build();
            s3Client.deleteObjects(deleteObjectsRequest); // S3에서 객체 삭제
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            throw new GeneralException(ErrorStatus._IO_EXCEPTION_DELETE_FILE);
        }
    }

    // 삭제에 필요한 key 반환
    private String getKeyFromImageUrls(String imageUrl) {
        try {
            URL url = new URI(imageUrl).toURL(); // 인코딩된 주소를 URI 객체로 변환 후 URL 객체로 변환
            String decodedKey = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);// URI에서 경로 부분을 가져와 URL 디코딩을 통해 실제 키로 변환
            return decodedKey.substring(1); // 경로 앞에 '/'가 있으므로 이를 제거한 뒤 반환
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            throw new GeneralException(ErrorStatus._INVALID_URL_FORMAT);
        }
    }

}
