package com.pentaho.maven.transform.assembly;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by Aliaksandr_Zhuk on 12/14/2016.
 */
public interface TreeGenerator {

    void createTree(Path shim) throws IOException;

}
