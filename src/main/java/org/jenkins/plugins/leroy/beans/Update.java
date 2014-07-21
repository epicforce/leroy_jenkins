package org.jenkins.plugins.leroy.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Dzmitry Bahdanovich on 28.06.14.
 */
public class Update {

    /**
     * Controller version
     */
    private final int version;

    /**
     * Binary name to URL map
     */
    private final Map<String, String> binaries;

    public Update(int version, Map<String, String> binaries) {
        this.version = version;
        this.binaries = binaries;
    }

    public int getVersion() {
        return version;
    }

    public Map<String, String> getBinaries() {
        return binaries;
    }

    /**
     * @return the names of Leroy Deploy binaries, ignoring jenkins binaries
     */
    public List<String> getLeroyDeployOnlyBinariesNames() {
        List<String> names = new ArrayList<String>();
        for (Map.Entry<String, String> entry : binaries.entrySet()) {
            if (!"jenkins".equalsIgnoreCase(entry.getKey())) {
                names.add(entry.getKey());
            }
        }
        return names;
    }
}
