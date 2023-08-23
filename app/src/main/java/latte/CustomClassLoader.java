package latte;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

public class CustomClassLoader extends URLClassLoader {
    private Path classFileName = null;

    public CustomClassLoader(URL jarFile) {
        super(new URL[] {jarFile});
    }

    public CustomClassLoader(Path classFileName) {
        super(new URL[0]);
        this.classFileName = classFileName;
    }

    public void setAdditionalClassPath(String classPath) throws IOException {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*.{jar,JAR}");
        for (String pathName : classPath.split(File.pathSeparator)) {
            Path path = Paths.get(pathName);
            if (path.getFileName().toString().equals("*")) {
                for (Path jar : Files.find(path.getParent(), 1, (p, attr) -> attr.isRegularFile() && pathMatcher.matches(p.getFileName())).toArray(Path[]::new)) {
                    this.addURL(jar.toUri().toURL());
                }
            }
            else {
                this.addURL(path.toUri().toURL());
            }
        }
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (this.classFileName != null && this.classFileName.endsWith(classFileName)) {
            byte[] classBytes;
            try {
                classBytes = Files.readAllBytes(this.classFileName);
            } catch (IOException e) {
                throw new ClassNotFoundException(name);
            }
            return defineClass(name, classBytes, 0, classBytes.length);
        }
        return super.findClass(name);
    }
}
