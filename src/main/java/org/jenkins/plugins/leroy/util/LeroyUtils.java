package org.jenkins.plugins.leroy.util;

import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import org.jenkins.plugins.leroy.LeroyNodeProperty;

public class LeroyUtils {

    public static boolean isLeroyNode() {
        Jenkins jenkins = Jenkins.getInstance();
        DescribableList nodeProperties = jenkins.getNodeProperties();
        boolean result = false;
        for (Object property : nodeProperties) {
            if (property instanceof LeroyNodeProperty) {
                result =  true;
            }
        }
        return result;
    }

}
