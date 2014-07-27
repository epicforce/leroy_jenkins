package org.jenkins.plugins.leroy;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.PasswordParameterDefinition;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Created by user on 27.07.2014.
 */
public class LeroyPasswordParameterDefinition extends PasswordParameterDefinition {

    @DataBoundConstructor
    public LeroyPasswordParameterDefinition(String name, String defaultValue, String description) {
        super(name, defaultValue, description);
    }

    @Override
    public ParameterDefinition copyWithDefaultValue(ParameterValue defaultValue) {
        if (defaultValue instanceof LeroyPasswordParameterValue) {
            LeroyPasswordParameterValue value = (LeroyPasswordParameterValue) defaultValue;
            return new LeroyPasswordParameterDefinition(getName(), Secret.toString(value.getValue()), getDescription());
        } else {
            return this;
        }
    }

    @Override
    public ParameterValue createValue(String value) {
        return new LeroyPasswordParameterValue(getName(), value, getDescription());
    }

    @Override
    public LeroyPasswordParameterValue createValue(StaplerRequest req, JSONObject jo) {
        LeroyPasswordParameterValue value = req.bindJSON(LeroyPasswordParameterValue.class, jo);
        value.setDescription(getDescription());
        return value;
    }

    @Override
    public ParameterValue getDefaultParameterValue() {
        return new LeroyPasswordParameterValue(getName(), getDefaultValue(), getDescription());
    }

    @Extension
    public final static class ParameterDescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return "Leroy Password Parameter";
        }

        @Override
        public String getHelpFile() {
            return "/help/parameter/string.html";
        }
    }


}
