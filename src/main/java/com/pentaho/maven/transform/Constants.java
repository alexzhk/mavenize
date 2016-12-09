package com.pentaho.maven.transform;

/**
 * Created by Vasilina_Terehova on 12/10/2016.
 */
public class Constants {
    public static String PARENT_TAG = "<parent>\n" +
            "    <groupId>pentaho</groupId>\n" +
            "    <artifactId>pentaho-hadoop-shims-parent</artifactId>\n" +
            "    <version>7.1-SNAPSHOT</version>\n" +
            "  </parent>";

    public static String ASSEMBLY_PLUGIN = "<plugin>\n" +
            "<artifactId>maven-assembly-plugin</artifactId>\n" +
            "<version>2.6</version>\n" +
            "<executions>\n" +
            "<execution>\n" +
            "<id>pkg</id>\n" +
            "<phase>package</phase>\n" +
            "<goals>\n" +
            "<goal>single</goal>\n" +
            "</goals>\n" +
            "</execution>\n" +
            "</executions>\n" +
            "<configuration>\n" +
            "<descriptor>${basedir}/src/main/descriptor/assembly.xml</descriptor>\n" +
            "<appendAssemblyId>false</appendAssemblyId>\n" +
            "</configuration>\n" +
            "</plugin>";

    public static String MAKEPOM_TARGET = "<target name=\"transfer\" depends=\"subfloor.resolve-default\">\n" +
            "    <ivy:makepom ivyfile=\"${basedir}/ivy.xml\" pomfile=\"${basedir}/pom.xml\">\n" +
            "      <mapping conf=\"default\" scope=\"compile\"/>\n" +
            "      <mapping conf=\"pmr\" scope=\"compile\"/>\n" +
            "      <mapping conf=\"client\" scope=\"compile\"/>\n" +
            "      <mapping conf=\"provided\" scope=\"provided\"/>\n" +
            "      <mapping conf=\"test\" scope=\"test\"/>\n" +
            "    </ivy:makepom>\n" +
            "  </target>";


}
