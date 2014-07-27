package org.jenkins.plugins.leroy;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Created by user on 27.07.2014.
 */
public class LeroyStringParameterDefinition extends StringParameterDefinition {

    @DataBoundConstructor
    public LeroyStringParameterDefinition(String name, String defaultValue, String description) {
        super(name, defaultValue, description);
    }

    public LeroyStringParameterDefinition(String name, String defaultValue) {
        super(name, defaultValue);
    }

    @Override
    public ParameterDefinition copyWithDefaultValue(ParameterValue defaultValue) {
        if (defaultValue instanceof LeroyStringParameterValue) {
            LeroyStringParameterValue value = (LeroyStringParameterValue) defaultValue;
            return new StringParameterDefinition(getName(), value.value, getDescription());
        } else {
            return this;
        }
    }

    @Override
    public LeroyStringParameterValue getDefaultParameterValue() {
        LeroyStringParameterValue v = new LeroyStringParameterValue(getName(), getDefaultValue(), getDescription());
        return v;
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return "Leroy String Parameter";
        }

        @Override
        public String getHelpFile() {
            return "/help/parameter/string.html";
        }
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        LeroyStringParameterValue value = req.bindJSON(LeroyStringParameterValue.class, jo);
        value.setDescription(getDescription());
        return value;
    }

    public ParameterValue createValue(String value) {
        return new LeroyStringParameterValue(getName(), value, getDescription());
    }

}
