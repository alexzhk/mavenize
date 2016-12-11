package com.pentaho.maven.transform;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by Vasilina_Terehova on 12/7/2016.
 */
public class PropertyReader {

    private final Path path;
    private Properties prop;
    private Path buildPropertyPath;

    public PropertyReader(Path folder) {
        this.path = folder;
        this.buildPropertyPath = Paths.get(path.toString(), "build.properties");
        prop = new Properties();
        InputStream input = null;

        try {

            input = new FileInputStream(buildPropertyPath.toString());

            // load a properties file
            prop.load(input);
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
    }

    public ShimProperties readShimProperties() {
        return new ShimProperties(prop.getProperty("common.hadoop.shim.version"), prop.getProperty("common.hbase.shim.version"),
                prop.getProperty("hbase.generation"), prop.getProperty("common.pig.shim.version"));
    }

    //now some error running assemble goal with oss license not understandable
    public void setOssLicenseFalse() throws IOException {
        prop.remove("oss-license.filename");
        FileOutputStream fileOutputStream = new FileOutputStream(buildPropertyPath.toString());
        prop.store(fileOutputStream, null);
    }
}
