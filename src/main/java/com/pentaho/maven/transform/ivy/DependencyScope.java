package com.pentaho.maven.transform.ivy;

import com.pentaho.maven.transform.Artifact;
import com.pentaho.maven.transform.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by Aliaksandr_Zhuk on 12/25/2016.
 */
public class DependencyScope implements Scope {

    private Path folder;
    private Map<Scopes, Map<Boolean, List<ComplexArtifact>>> map = new HashMap<>();
    private Map<Scopes, Map<Boolean, List<ComplexArtifact>>> excludesMap = new HashMap<>();

    public DependencyScope(Map<Scopes, Map<Boolean, List<ComplexArtifact>>> map, Map<Scopes, Map<Boolean, List<ComplexArtifact>>> excludesMap, Path folder) {

        this.map = map;
        this.excludesMap = excludesMap;
        this.folder = folder;

    }

    public void createTempFolder() throws IOException {

        Path temp = Paths.get(folder.toString(), IvyRunner.TEMP_MVN_SHIM_FOLDER);

        Path def = Paths.get(temp.toString(), SubFolder.DEFAULT.name().toLowerCase());
        Path client = Paths.get(temp.toString(), SubFolder.CLIENT.name().toLowerCase());
        Path pmr = Paths.get(temp.toString(), SubFolder.PMR.name().toLowerCase());
        Path test = Paths.get(temp.toString(), SubFolder.TEST.name().toLowerCase());
        Path provided = Paths.get(temp.toString(), SubFolder.PROVIDED.name().toLowerCase());

        FileUtils.removeFile(temp);
        FileUtils.createFolder(temp);

        FileUtils.createFolder(def);
        FileUtils.createFolder(client);
        FileUtils.createFolder(pmr);
        FileUtils.createFolder(test);
        FileUtils.createFolder(provided);

    }


    public List<ComplexArtifact> getTransitiveDepsByScope(boolean isTransitive, Scopes scope) {

        Map<Boolean, List<ComplexArtifact>> map;
        List<ComplexArtifact> list;

        map = getDependenciesByScope(scope);

        if (isTransitive) {
            list = map.get(true);
        } else {
            list = map.get(false);
        }

        return list;

    }


    public List<ComplexArtifact> getExcludesByScope(Scopes scope) {

        return excludesMap.get(scope).get(true);

    }

    @Override
    public Map<Boolean, List<ComplexArtifact>> getDependenciesByScope(Scopes scope) {

        return map.get(scope);

    }

    @Override
    public List<Artifact> getExcludes(ComplexArtifact artifact) {

        List<Artifact> list;
        Map<Artifact, List<Artifact>> map = new HashMap<>();
        list = artifact.getComplexArtifact().get(artifact.getArtifact());

        return list;

    }

}
