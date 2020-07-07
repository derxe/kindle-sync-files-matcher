package sync;

import java.io.IOException;
import java.io.InputStream;

interface IFile {
    String getPath();

    InputStream open() throws IOException;
}
