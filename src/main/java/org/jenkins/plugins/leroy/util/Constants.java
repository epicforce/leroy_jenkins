package org.jenkins.plugins.leroy.util;

public class Constants {

    public static final String LEROY_HOME = "LEROY_HOME";
    public static final String IS_LEROY_NODE = "IS_LEROY_NODE";

    public static final String ENVIRONMENT_PARAM = "Environment";
    public static final String WORKFLOW_PARAM = "Workflow";
    public static final String CONFIG_SOURCE_PARAM = "Configuration Source";
    public static final String TARGET_CONFIGURATION = "Target Configuration";

    public static final String MASTER_NODE = "<master_node>";

    public static final String LEROY_PROPERTY_PREFIX = "LEROY_PROPERTY_";

    public static final String UPDATE_XML = "https://dl.dropboxusercontent.com/u/250424534/update.xml"; // TODO move to properties file

    public static enum Architecture {
        AIX("AIX-00F604884C00"),
        DARWIN("Darwin"),
        WIN64("Win64"),
        WIN32("Win32"),
        LINUX_X86_64("Linux-x86_64"),
        LINUX_I686("Linux-i686");

        private Architecture(String value) {
            this.value = value;
        }

        private String value;

        public String getValue() {
            return value;
        }
    }

    public static enum ConfigSource {
        SCM("SCM"),
        LAST_BUILD("Last Build");

        private ConfigSource(String value) {
            this.value = value;
        }

        private String value;

        public String getValue() {
            return value;
        }
    }

}
