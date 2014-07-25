/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkins.plugins.leroy;

import hudson.Extension;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yunus
 * @author Dzmitry Bahdanovich
 */
public class LeroyConfigurationProject extends ConfigurationProject<LeroyConfigurationProject, LeroyConfigurationBuild> implements TopLevelItem {

    /**
     * @deprecated as of 1.390
     */
    public LeroyConfigurationProject(Jenkins parent, String name) throws IOException {
        super(parent, name);
    }

    public LeroyConfigurationProject(ItemGroup parent, String name) throws IOException {
        super(parent, name);
    }

    @Override
    protected Class<LeroyConfigurationBuild> getBuildClass() {
        return LeroyConfigurationBuild.class;
    }

    @Override
    public LeroyConfigurationProject.DescriptorImpl getDescriptor() {
        return (LeroyConfigurationProject.DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    /**
     * Descriptor is instantiated as a field purely for backward compatibility.
     * Do not do this in your code. Put @Extension on your DescriptorImpl class instead.
     */
    @Restricted(NoExternalUse.class)
    @Extension(ordinal = 1000)
    public final static LeroyConfigurationProject.DescriptorImpl DESCRIPTOR = new LeroyConfigurationProject.DescriptorImpl();


    public static class DescriptorImpl extends AbstractProject.AbstractProjectDescriptor {

        @Override
        public String getDisplayName() {
            return "Leroy Configuration Job"; //TODO externalize
        }

        @Override
        public LeroyConfigurationProject newInstance(ItemGroup parent, String name) {
            try {
                return new LeroyConfigurationProject(parent, name);
            } catch (IOException ex) {
                Logger.getLogger(LeroyConfigurationProject.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

    }

}
