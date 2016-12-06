package com.pentaho.maven.transform;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by Vasilina_Terehova on 12/7/2016.
 */
public class PropertyReader {

    private final Path path;

    public PropertyReader(Path folder) {
        this.path = folder;
    }

    public ShimProperties readShimProperties() {
        Properties prop = new Properties();
        InputStream input = null;

        try {

            input = new FileInputStream(Paths.get(path.toString(), "build.properties").toString());

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            System.out.println();
            System.out.println(prop.getProperty("common.hbase.shim.version"));
            System.out.println(prop.getProperty("common.pig.shim.version"));
            System.out.println(prop.getProperty("hbase.generation"));
            return new ShimProperties(prop.getProperty("common.hadoop.shim.version"), prop.getProperty("common.hbase.shim.version"),
                    prop.getProperty("hbase.generation"), prop.getProperty("common.pig.shim.version"));
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
