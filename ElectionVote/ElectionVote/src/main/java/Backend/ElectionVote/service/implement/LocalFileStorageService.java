package Backend.ElectionVote.service.implement;


import Backend.ElectionVote.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path root = Paths.get(System.getProperty("user.dir"), "uploads");

    @PostConstruct
    public void init() throws IOException { Files.createDirectories(root); }

    @Override
    public String store(String folder, String filename, InputStream data, long size, String contentType) throws IOException {
        Path dir = root.resolve(folder);
        Files.createDirectories(dir);
        Path dest = dir.resolve(filename);
        Files.copy(data, dest, StandardCopyOption.REPLACE_EXISTING);
        // In dev/local, expose via static handler or return "file://" path
        return dest.toUri().toString();
    }
}

