package org.jenkins.plugins.leroy;

import hudson.tasks.ArtifactArchiver;

/**
 * Created by dzmitry_bahdanovich on 21.06.14.
 */
public class LeroyArtifactArchiver extends ArtifactArchiver implements Hidden {
    public LeroyArtifactArchiver(String artifacts, String excludes, boolean latestOnly) {
        super(artifacts, excludes, latestOnly);
    }

    public LeroyArtifactArchiver(String artifacts, String excludes, boolean latestOnly, boolean allowEmptyArchive) {
        super(artifacts, excludes, latestOnly, allowEmptyArchive);
    }
}
