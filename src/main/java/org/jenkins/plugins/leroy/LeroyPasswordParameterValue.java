package org.jenkins.plugins.leroy;

import hudson.model.PasswordParameterValue;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by user on 27.07.2014.
 */
public class LeroyPasswordParameterValue extends PasswordParameterValue {


    public LeroyPasswordParameterValue(String name, String value) {
        super(name, value);
    }

    @DataBoundConstructor
    public LeroyPasswordParameterValue(String name, String value, String description) {
        super(name, value, description);
    }
}
