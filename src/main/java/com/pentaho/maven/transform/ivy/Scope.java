package com.pentaho.maven.transform.ivy;

import com.pentaho.maven.transform.Artifact;

import java.util.List;
import java.util.Map;

/**
 * Created by Aliaksandr_Zhuk on 12/25/2016.
 */
public interface Scope {

    Map<Boolean, List<ComplexArtifact>> getDependenciesByScope(Scopes scope);

    List<Artifact> getExcludes(ComplexArtifact artifact);

}
