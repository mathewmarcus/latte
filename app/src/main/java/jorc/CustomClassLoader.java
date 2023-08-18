package jorc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

public class CustomClassLoader extends ClassLoader {
    private FileSystem jarFile = null;
    private Path classFileName = null;

    public CustomClassLoader(FileSystem jarFile) {
        this.jarFile = jarFile;
    }

    public CustomClassLoader(Path classFileName) {
        this.classFileName = classFileName;
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Path path;
        if (this.jarFile != null) {
            String archiveMemberName = String.format("%s.class", name.replace('.', File.separatorChar));
            path = this.jarFile.getPath(archiveMemberName);
        }
        else {
            path = this.classFileName;
        }

        byte[] classBytes;
        try {
            classBytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ClassCastException(name);
        }
        
        return defineClass(name, classBytes, 0, classBytes.length);
    }
}
