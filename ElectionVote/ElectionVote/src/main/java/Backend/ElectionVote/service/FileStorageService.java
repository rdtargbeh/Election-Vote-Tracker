package Backend.ElectionVote.service;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {

    /** Store and return a public (or signed) URL */
    String store(String folder, String filename, InputStream data, long size, String contentType) throws IOException;
}
