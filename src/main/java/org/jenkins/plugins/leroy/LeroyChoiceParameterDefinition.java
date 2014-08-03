package org.jenkins.plugins.leroy;

import hudson.Extension;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Created by user on 27.07.2014.
 */
public class LeroyChoiceParameterDefinition extends ChoiceParameterDefinition {

    public static final String CHOICES_DELIMETER = "\\r?\\n";

    public static boolean areValidChoices(String choices) {
        String strippedChoices = choices.trim();
        return !StringUtils.isEmpty(strippedChoices) && strippedChoices.split(CHOICES_DELIMETER).length > 0;
    }


    @DataBoundConstructor
    public LeroyChoiceParameterDefinition(String name, String choices, String description) {
        super(name, choices, description);
    }

    public LeroyChoiceParameterDefinition(String name, String[] choices, String description) {
        super(name, choices, description);
    }

    @Override
    // default value is not passed
    public ParameterDefinition copyWithDefaultValue(ParameterValue defaultValue) {
        if (defaultValue instanceof LeroyStringParameterValue) {
            StringParameterValue value = (LeroyStringParameterValue) defaultValue;
            return new LeroyChoiceParameterDefinition(getName(), getChoices().toArray(new String[]{}), getDescription());
        } else {
            return this;
        }
    }

    @Override
    public StringParameterValue getDefaultParameterValue() {
//        return new StringParameterValue(getName(), defaultValue == null ? choices.get(0) : defaultValue, getDescription());
        return new LeroyStringParameterValue(getName(), getChoices().get(0), getDescription());
    }

    private StringParameterValue checkValue(StringParameterValue value) {
        if (!getChoices().contains(value.value))
            throw new IllegalArgumentException("Illegal choice: " + value.value);
        return value;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        LeroyStringParameterValue value = req.bindJSON(LeroyStringParameterValue.class, jo);
        value.setDescription(getDescription());
        return checkValue(value);
    }

    public StringParameterValue createValue(String value) {
        return checkValue(new LeroyStringParameterValue(getName(), value, getDescription()));
    }


    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return "Leroy Choice Parameter";
        }

        @Override
        public String getHelpFile() {
            return "/help/parameter/choice.html";
        }

        /**
         * Checks if parameterised build choices are valid.
         */
        public FormValidation doCheckChoices(@QueryParameter String value) {
            if (areValidChoices(value)) {
                return FormValidation.ok();
            } else {
                return FormValidation.error("Choices are missing");
            }
        }
    }
}
