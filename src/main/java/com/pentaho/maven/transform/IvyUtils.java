package com.pentaho.maven.transform;

import com.pentaho.maven.transform.xml.XmlUtilsJDom1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by Vasilina_Terehova on 1/9/2017.
 */
public class IvyUtils {
    public static void fixIvy(String[] shimsToProcess) {
        Path folder = Paths.get("D:\\1\\p-h-s1\\pentaho-hadoop-shims\\");
        List<String> shimList = Arrays.asList(shimsToProcess);
        try (Stream<Path> paths = Files.list(folder)) {
            paths.forEach(filePath -> {
                if (Files.isDirectory(filePath) && shimList.contains(filePath.getFileName().toString())) {

                    try {
                        XmlUtilsJDom1.fixIvyWithClassifier(filePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ShimCannotBeProcessed shimCannotBeProcessed) {
                        shimCannotBeProcessed.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
