package org.jenkins.plugins.leroy;

import hudson.model.StringParameterValue;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by user on 27.07.2014.
 */
public class LeroyStringParameterValue extends StringParameterValue {

    @DataBoundConstructor
    public LeroyStringParameterValue(String name, String value) {
        super(name, value);
    }

    public LeroyStringParameterValue(String name, String value, String description) {
        super(name, value, description);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        LeroyStringParameterValue other = (LeroyStringParameterValue) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "(LeroyStringParameterValue) " + getName() + "='" + value + "'";
    }

}
