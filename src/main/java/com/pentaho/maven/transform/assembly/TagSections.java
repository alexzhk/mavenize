package com.pentaho.maven.transform.assembly;

import java.util.List;

/**
 * Created by Aliaksandr_Zhuk on 12/14/2016.
 */
public class TagSections {


    private String shim;
    private String dependencySet;

    private String archFolder;

    private String excludeSection;

    public TagSections(String shim, String dependencySet) {

        this.shim = shim;
        this.dependencySet = dependencySet;

    }

    public TagSections(String archFolder, List<String> excludeTags) {

        this.archFolder = archFolder;

        StringBuilder s = new StringBuilder();
        for (String exclude : excludeTags) {
            s.append(exclude);
        }

        excludeSection = s.toString();
    }

    public String getAssemblyPlugin() {

        String assemblyPlugin = "<assembly>\n" +
                "  <id>plugin</id>\n" +
                "  <baseDirectory>" + shim + "</baseDirectory>\n" +
                "  <formats>\n" +
                "    <format>zip</format>\n" +
                "  </formats>\n" +
                "  <fileSets>\n" +
                "    <fileSet>\n" +
                "      <directory>${basedir}/src/main/resources</directory>\n" +
                "      <outputDirectory>/</outputDirectory>\n" +
                "    </fileSet>\n" +
                "    <fileSet>\n" +
                "      <directory>${oss.license.directory}</directory>\n" +
                "      <outputDirectory>/</outputDirectory>\n" +
                "    </fileSet>\n"+
                "  </fileSets>\n" +
                "  <dependencySets>\n" +
                "    <dependencySet>\n" +
                "      <outputDirectory>/</outputDirectory>\n" +
                "      <useTransitiveDependencies>false</useTransitiveDependencies>\n" +
                "      <useTransitiveFiltering>false</useTransitiveFiltering>\n" +
                "      <includes>\n" +
                "        <include>org.pentaho:pentaho-hadoop-shims-"+shim+"</include>\n" +
                "      </includes>\n" +
                "    </dependencySet>\n"+
                "" + dependencySet +
                "  </dependencySets>\n" +
                "</assembly>";

        return assemblyPlugin;
    }

    public String getTransitiveFalse() {

        String transitiveFalse = "    <dependencySet>\n" +
                "      <outputDirectory>" + archFolder + "</outputDirectory>\n" +
                "      <useTransitiveDependencies>true</useTransitiveDependencies>\n" +
                "      <useTransitiveFiltering>false</useTransitiveFiltering>\n" +
                "      <includes>\n" +
                "" + excludeSection +
                "      </includes>\n" +
                "      <excludes>\n" +
                "        <exclude>*:tests:*</exclude>\n" +
                "      </excludes>\n" +
                "    </dependencySet>\n";

        return transitiveFalse;

    }

    public String getFilteringFalse() {

        // Only explicitly listed artifacts are added
        String filteringFalse = "    <dependencySet>\n" +
                "      <outputDirectory>" + archFolder + "</outputDirectory>\n" +
                "      <includes>\n" +
                "" + excludeSection +
                "      </includes>\n" +
                "      <excludes>\n" +
                "        <exclude>*:tests:*</exclude>\n" +
                "      </excludes>\n" +
                "    </dependencySet>\n";

        return filteringFalse;

    }
}
