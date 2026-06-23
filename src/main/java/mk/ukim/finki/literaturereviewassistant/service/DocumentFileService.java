package mk.ukim.finki.literaturereviewassistant.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface DocumentFileService {
    String uploadFile(MultipartFile file) throws IOException;
    Resource getFile(String fileName) throws IOException;
    void deleteFile(String fileName) throws IOException;
}
