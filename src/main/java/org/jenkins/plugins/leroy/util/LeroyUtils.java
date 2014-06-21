package org.jenkins.plugins.leroy.util;

import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Run;
import hudson.triggers.SCMTrigger;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import org.jenkins.plugins.leroy.LeroyNodeProperty;

public class LeroyUtils {

    public static final String USER_ID_CAUSE_CLASS_NAME = "hudson.model.Cause$UserIdCause";
    public static final String SCM_TRIGGER = "SCMTrigger";

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

    public static String getUserRunTheBuild(Run build) {

        // If build has been triggered form an upstream build, get UserCause from there to set user build variables
        Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause) build.getCause(Cause.UpstreamCause.class);
        if (upstreamCause != null) {
            Job job = Jenkins.getInstance().getItemByFullName(upstreamCause.getUpstreamProject(), Job.class);
            if (job != null) {
                Run upstream = job.getBuildByNumber(upstreamCause.getUpstreamBuild());
                if (upstream != null) {
                    getUserRunTheBuild(upstream);
                }
            }
        }

        // set BUILD_USER_NAME to fixed value if the build was triggered by a change in the scm
        SCMTrigger.SCMTriggerCause scmTriggerCause = (SCMTrigger.SCMTriggerCause) build.getCause(SCMTrigger.SCMTriggerCause.class);
        if (scmTriggerCause != null) {
            return SCM_TRIGGER;
        }

        // Use UserIdCause.class if it exists in the system (should be starting from b1.427 of jenkins).
        if(isClassExists(USER_ID_CAUSE_CLASS_NAME)){
			/* Try to use UserIdCause to get & set jenkins user build variables */
            Cause.UserIdCause userIdCause = (Cause.UserIdCause) build.getCause(Cause.UserIdCause.class);
            if (userIdCause != null) {
                return userIdCause.getUserId();
            }
        }

        // Try to use deprecated UserCause to get & set jenkins user build variables
        Cause.UserCause userCause = (Cause.UserCause) build.getCause(Cause.UserCause.class);
        if (userCause != null) {
            return userCause.getUserName();
        }

        return "unknown";
    }

    public static boolean isClassExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch(ClassNotFoundException e) {
            return false;
        }
    }

}
