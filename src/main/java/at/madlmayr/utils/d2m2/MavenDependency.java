package at.madlmayr.utils.d2m2;

import java.io.File;

/**
 * DTO for one maven dependency
 */
public class MavenDependency {

    private final String version;
    private final String artifactId;
    private final String groupId;
    private final File path;

    public MavenDependency(final String groupId, final String artifactId, final String version, final File path) {
        this.version = version;
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.path = path;
    }

    public String getVersion() {
        return version;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public File getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "MavenDependency{" +
                "version='" + version + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", path=" + path +
                '}';
    }
}
