/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkins.plugins.leroy;

import hudson.model.Descriptor;
import hudson.model.PageDecorator;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author Yunus
 */
public class  SimpleThemeDecorator extends PageDecorator {
	private String cssUrl;
	private String jsUrl;

	public SimpleThemeDecorator() {
		super(SimpleThemeDecorator.class);
		cssUrl = "C:/Users/Yunus/Dropbox/leroy-jenkins-plugin/test-deployment/plugin/jprt-plugin-master/src/main/resources/hudson/plugins/jprt/sample.css";
                load();
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject formData)
			throws Descriptor.FormException {
            
                cssUrl = formData.getString("cssUrl");
                jsUrl = formData.getString("jsUrl");
		save();
		return super.configure(req, formData);
	}

	public String getCssUrl() {
		return cssUrl;
	}

	public String getJsUrl() {
		return jsUrl;
	}

}