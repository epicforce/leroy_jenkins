/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkins.plugins.leroy;

import hudson.EnvVars;
import hudson.Plugin;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 *
 * @author Yunus
 */
public class LeroyPlugin extends Plugin{
    public LeroyPlugin(){
        super();
    }
    
    public ListBoxModel doLeroyNodeCheck(){
        Jenkins jenkins = Jenkins.getInstance();
        Computer[] computers = jenkins.getComputers();
        ListBoxModel model = new ListBoxModel();
        for(int i = 0; i < computers.length; i++)
        {
            EnvVars envs = null; 
            try {
                try {
                    envs = computers[i].buildEnvironment(TaskListener.NULL);
                } catch (IOException ex) {
                    Logger.getLogger(LeroyPlugin.class.getName()).log(Level.SEVERE, null, ex);
                }
                String name = computers[i].getName();
                if(envs.containsKey("IS_LEROY_NODE"))
                {   
                    model.add("hasLeroyNode", "true");
                }            

            } catch (InterruptedException ex) {
                Logger.getLogger(NewProject.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if(model.isEmpty())
        {            
            model.add("hasLeroyNode", "false");
        }
        
        return model;
    }
}
