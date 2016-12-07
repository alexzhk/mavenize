package com.pentaho.maven.transform;

/**
 * Created by Vasilina_Terehova on 12/7/2016.
 */
public class ShimProperties {
    private String hadoopVersion;
    private String hbaseVersion;
    private String hbaseGenerationVersion;
    private String pigVersion;

    public ShimProperties(String hadoopVersion, String hbaseVersion, String hbaseGenerationVersion, String pigVersion) {
        this.hadoopVersion = hadoopVersion;
        this.hbaseVersion = hbaseVersion;
        this.hbaseGenerationVersion = hbaseGenerationVersion;
        this.pigVersion = pigVersion;
    }

    public String getHbaseVersion() {
        return hbaseVersion;
    }

    public void setHbaseVersion(String hbaseVersion) {
        this.hbaseVersion = hbaseVersion;
    }

    public String getPigVersion() {
        return pigVersion;
    }

    public void setPigVersion(String pigVersion) {
        this.pigVersion = pigVersion;
    }

    public String getHbaseGenerationVersion() {
        return hbaseGenerationVersion;
    }

    public void setHbaseGenerationVersion(String hbaseGenerationVersion) {
        this.hbaseGenerationVersion = hbaseGenerationVersion;
    }

    public String getHadoopVersion() {
        return hadoopVersion;
    }

    public void setHadoopVersion(String hadoopVersion) {
        this.hadoopVersion = hadoopVersion;
    }
}
