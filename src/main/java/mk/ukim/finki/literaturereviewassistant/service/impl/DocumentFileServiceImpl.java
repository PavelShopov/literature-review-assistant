package mk.ukim.finki.literaturereviewassistant.service.impl;

import mk.ukim.finki.literaturereviewassistant.service.DocumentFileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class DocumentFileServiceImpl implements DocumentFileService {

    @Value("${file.upload.dir:/app/uploads}")
    private String uploadDir;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        validateFile(file);
        
        String uploadsPath = ensureUploadDirExists();
        String fileName = generateUniqueFileName(file.getOriginalFilename());
        Path filePath = Paths.get(uploadsPath, fileName);

        file.transferTo(filePath.toFile());
        
        return fileName;
    }

    @Override
    public Resource getFile(String fileName) throws IOException {
        validateFileName(fileName);
        
        Path filePath = Paths.get(uploadDir, fileName);
        
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + fileName);
        }
        
        if (!isPathSafe(filePath)) {
            throw new IOException("Invalid file path");
        }
        
        return new FileSystemResource(filePath.toFile());
    }

    @Override
    public void deleteFile(String fileName) throws IOException {
        validateFileName(fileName);
        
        Path filePath = Paths.get(uploadDir, fileName);
        
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + fileName);
        }
        
        if (!isPathSafe(filePath)) {
            throw new IOException("Invalid file path");
        }
        
        Files.delete(filePath);
    }

    private String ensureUploadDirExists() throws IOException {
        Path dirPath = Paths.get(uploadDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        return uploadDir;
    }

    private String generateUniqueFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        return UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("File is empty");
        }
        
        if (file.getSize() > 104857600) { // 100MB limit
            throw new IOException("File size exceeds 100MB limit");
        }
    }

    private void validateFileName(String fileName) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IOException("File name cannot be empty");
        }
        
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IOException("Invalid file name");
        }
    }

    private boolean isPathSafe(Path filePath) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toRealPath();
        Path resolvedPath = filePath.toRealPath();
        return resolvedPath.startsWith(uploadPath);
    }
}
