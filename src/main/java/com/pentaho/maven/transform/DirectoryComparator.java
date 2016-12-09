package com.pentaho.maven.transform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Created by Vasilina_Terehova on 12/9/2016.
 */
public class DirectoryComparator {
    public void compare(Path path1, Path path2) throws IOException {
        Stream<Path> list1 = Files.list(path1);
        Stream<Path> list2 = Files.list(path2);
        list1.filter(path -> !list2.anyMatch(path3 -> path.getFileName().equals(path3.getFileName())));


    }
}
