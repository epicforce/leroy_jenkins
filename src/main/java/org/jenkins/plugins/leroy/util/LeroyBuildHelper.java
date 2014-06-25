package org.jenkins.plugins.leroy.util;

import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.jenkins.plugins.leroy.LeroyNodeProperty;

import java.util.*;

/**
 * Created by dzmitry_bahdanovich on 23.06.14.
 */
public class LeroyBuildHelper {

    //test
    static String FORM = "{\"name\":\"j3\",\"description\":\"\",\"logrotate\":false,\"\":\"0\",\"buildDiscarder\":{\"stapler-class\":\"hudson.tasks.LogRotator\",\"daysToKeepStr\":\"\",\"numToKeepStr\":\"\",\"artifactDaysToKeepStr\":\"\",\"artifactNumToKeepStr\":\"\"},\"properties\":{\"stapler-class-bag\":\"true\",\"hudson-model-ParametersDefinitionProperty\":{\"parameterized\":{\"parameter\":[{\"name\":\"Workflow\",\"choices\":\"echotest\\nerror_state_convey\\ninternal_test\\nisfailure\\nissuccess\\nis_not_failure\\nis_not_success\\nprecondition\\npythontest\\npyurl\\nsub\\ntest\\ntransfertest\\nunix_exec\\nwindows-test\",\"description\":\"\",\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"},{\"name\":\"Environment\",\"choices\":\"aix\\ntest\",\"description\":\"\",\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"}]}},\"hudson-plugins-copyartifact-CopyArtifactPermissionProperty\":{}},\"scm\":{\"value\":\"3\",\"locations\":{\"remote\":\"svn://planeti.biz/proj/leroy-jenkins/LEROY_CONFIGURATION_ROOT\",\"local\":\".\",\"depthOption\":\"infinity\",\"ignoreExternalsOption\":false},\"\":[\"0\",\"auto\"],\"workspaceUpdater\":{\"stapler-class\":\"hudson.scm.subversion.UpdateUpdater\"},\"ignoreDirPropChanges\":false,\"excludedRegions\":\"\",\"includedRegions\":\"\",\"excludedUsers\":\"\",\"excludedCommitMessages\":\"\",\"excludedRevprop\":\"\",\"filterChangelog\":false},\"builder\":[{\"projectName\":\"Leroy Configuration Job\",\"\":\"4\",\"selector\":{\"stapler-class\":\"hudson.plugins.copyartifact.SpecificBuildSelector\",\"buildNumber\":\"7\"},\"filter\":\"**\",\"target\":\"c:\\\\leroy\\\\artifacts/\",\"parameters\":\"\",\"flatten\":false,\"optional\":false,\"fingerprintArtifacts\":true,\"stapler-class\":\"hudson.plugins.copyartifact.CopyArtifact\",\"kind\":\"hudson.plugins.copyartifact.CopyArtifact\"},{\"workflow\":\"echotest\",\"projectname\":\"j3\",\"envtablediv\":{\"envtable\":{\"enabled_envs\":[true,true],\"envlist\":\"aix\\ntest\\n\",\"autodeploy_envs\":[true,false],\"aix\":\"last\",\"default_env\":\"test\",\"test\":\"scm\"}},\"stapler-class\":\"org.jenkins.plugins.leroy.LeroyBuilder\",\"kind\":\"org.jenkins.plugins.leroy.LeroyBuilder\"}],\"publisher\":{\"artifacts\":\"**\",\"excludes\":\"\",\"latestOnly\":false,\"allowEmptyArchive\":false,\"onlyIfSuccessful\":false,\"stapler-class\":\"hudson.tasks.ArtifactArchiver\",\"kind\":\"hudson.tasks.ArtifactArchiver\"},\"core:apply\":\"\"}";
    static String ENVTABLEDIV = "[{\"envtablediv\":{\"envtable\":{\"aix\":\"last\",\"default_env\":\"test\",\"autodeploy_envs\":[true,false],\"test\":\"scm\",\"enabled_envs\":[true,true]}},\"stapler-class\":\"org.jenkins.plugins.leroy.LeroyBuilder\",\"projectname\":\"j3\",\"workflow\":\"echotest\",\"kind\":\"org.jenkins.plugins.leroy.LeroyBuilder\"}]";
    static String PARAMETERS = "[{\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"description\":\"\",\"choices\":\"echotest\\nerror_state_convey\\ninternal_test\\nisfailure\\nissuccess\\nis_not_failure\\nis_not_success\\nprecondition\\npythontest\\npyurl\\nsub\\ntest\\ntransfertest\\nunix_exec\\nwindows-test\",\"name\":\"Workflow\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"},{\"stapler-class\":\"hudson.model.ChoiceParameterDefinition\",\"description\":\"\",\"choices\":\"aix\\ntest\",\"name\":\"Environment\",\"kind\":\"hudson.model.ChoiceParameterDefinition\"}]";


    private static final String LEROY_BUILDER_JPATH = "$.builder[*][?(@.kind==org.jenkins.plugins.leroy.LeroyBuilder)]";
    private static final String LEROY_BUILDER_PARAMETERS_JPATH = "$.properties.hudson-model-ParametersDefinitionProperty.parameterized.parameter";


    public static String getLeroyBuilderJSON(String json)  {
        JSONArray props = JsonPath.read(json, LEROY_BUILDER_JPATH);
        if (props != null) {
            return props.toJSONString();
        }
        return "";
    }

    public static String getParametersJSON(String json) {
        JSONArray props = JsonPath.read(json, LEROY_BUILDER_PARAMETERS_JPATH);
        if (props != null) {
            return props.toJSONString();
        }
        return "";
    }

    public static Map<String, String[]> getAllParameters(String json) {
        Map<String, String[]> results = new HashMap<String, String[]>();
        JSONArray props = JsonPath.read(json, LEROY_BUILDER_PARAMETERS_JPATH);
        if (props != null) {
            for (int i = 0; i < props.size(); i++) {
                JSONObject property = (JSONObject)props.get(i);
                String choices = (String)property.get("choices");
                if (choices != null) {
                    String[] choicesArr = choices.split("\n");
                    String name = (String)property.get("name");
                    if (name != null && !name.isEmpty()) {
                        results.put(name, choicesArr);
                    }
                }
            }
        }
        return results;
    }

    public static List<String> getAllEnvironments(String json) {
//        Map<String, String[]> parameters = getAllParameters(json);
        List<String> result = new ArrayList<String>();
//        if (parameters != null) {
//            String[] allEnvs = parameters.get(Constants.ENVIRONMENT_PARAM);
//            result = new ArrayList<String>(Arrays.asList(allEnvs));
//        }
        String builderJson = getLeroyBuilderJSON(json);
        String values = JsonPath.read(builderJson,"$.[0].envtablediv.envtable.envlist");
        result = new ArrayList<String>(Arrays.asList(values.split("\\n")));
        return result;
    }

    public static Map<String, String> getEnvUsedConfigs(String json) {
        List<String> allEnvs = getAllEnvironments(json);
        String builderJson = getLeroyBuilderJSON(json);
        Map<String, String> res = new HashMap<String, String>();
        for (String envName : allEnvs) {
            String query = "$.[0].envtablediv.envtable." + envName;
            String value = JsonPath.read(builderJson,query);
            res.put(envName, (value == null ? "scm" : value));
        }
        return res;
    }

    public static List<String> getEnabledEnvironments(String json) {
        List<String> allEnvs = getAllEnvironments(json);
        List<String> result = new ArrayList<String>();
        if (allEnvs != null && allEnvs.size() > 0) {
            String builderJson = getLeroyBuilderJSON(json);
            JSONArray values = JsonPath.read(builderJson,"$.[0].envtablediv.envtable.enabled_envs");
            if (values != null && values.size() > 0) {
                result = leaveSelected(allEnvs, values.toArray());
            }
        }
        return result;
    }

    public static List<String> getAutodeployEnvironments(String json) {
        List<String> allEnvs = getAllEnvironments(json);
        List<String> result = new ArrayList<String>();
            if (allEnvs != null && allEnvs.size() > 0) {
                String builderJson = getLeroyBuilderJSON(json);
                JSONArray values = JsonPath.read(builderJson,"$.[0].envtablediv.envtable.autodeploy_envs");
                if (values != null && values.size() > 0) {
                    result = leaveSelected(allEnvs, values.toArray());
                }
            }

        return result;
    }


    public static String getDefaultEnvironment(String json) {
        String builderJson = getLeroyBuilderJSON(json);
        String value = JsonPath.read(builderJson,"$.[0].envtablediv.envtable.default_env");
        return (value == null ? "None" : value);
    }

    public static List<String> getAllWorkflows(String json) {
        Map<String, String[]> parameters = getAllParameters(json);
        List<String> result = new ArrayList<String>();
        if (parameters != null) {
            String[] allParams = parameters.get(Constants.WORKFLOW_PARAM);
            result = new ArrayList<String>(Arrays.asList(allParams));
        }
        return result;
    }

    public static String getDefaultWorkflow(String json) {
        String builderJson = getLeroyBuilderJSON(json);
        String value = JsonPath.read(builderJson,"$.[0].workflow");
        return (value == null ? "None" : value);
    }


    private static List<String> leaveSelected(List<String> list, Object[] isChosen) {
        List<String> result = new ArrayList<String>();
        if (list == null || isChosen == null) {
            System.out.println("Arrays are null"); //TODO Logger
            return result;
        } else if (list.size() != isChosen.length){
            System.out.println("Length doesn't match"); //TODO logger
            return result;
        }

        for (int i = 0; i < list.size(); i++) {
            if ((Boolean)isChosen[i]) {
                result.add(list.get(i));
            }
        }
        return result;
    }


    // temporary, for development
    public static void main(String[] args) {
        LeroyBuildHelper.getAllParameters(FORM);
        String builderJSon = getLeroyBuilderJSON(FORM);
        List<String> allEnvs = getAllEnvironments(FORM);
        List<String> enabledEnvs = getEnabledEnvironments(FORM);
        List<String> autodeployEnvs = getAutodeployEnvironments(FORM);
        String defaultEnv = getDefaultEnvironment(FORM);
        List<String> allWorkflows = getAllWorkflows(FORM);
        String defaultWorkflow = getDefaultWorkflow(FORM);
        System.out.println();
    }

}
