package com.pentaho.maven.transform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Vasilina_Terehova on 12/9/2016.
 */
public class DirectoryComparator {
    public void compare(Path path1, Path path2) throws IOException {
        HashSet<String> fileNames1 = new HashSet<String>();
        HashSet<String> fileNames2 = new HashSet<String>();
        Files.list(path1).forEach(path -> fileNames1.add(path.getFileName().toString()));
        Files.list(path2).forEach(path -> fileNames2.add(path.getFileName().toString()));
        List<String> weHaveOnlyInPath1 = fileNames1.stream().filter(s -> !fileNames2.contains(s)).sorted().collect(Collectors.toList());
        List<String> weHaveOnlyInPath2 = fileNames2.stream().filter(s -> !fileNames1.contains(s)).sorted().collect(Collectors.toList());
        System.out.println(weHaveOnlyInPath1);
        System.out.println(weHaveOnlyInPath2);
//        List<Path> collect = list1.filter(path -> !list2.anyMatch(path3 -> path.getFileName().equals(path3.getFileName()))).collect(Collectors.toList());
//        collect.stream().forEach(path -> System.out.println(path.getFileName()));


    }
}
