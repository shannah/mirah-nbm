/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.maven;


import ca.weblite.netbeans.mirah.support.spi.MirahExtenderImplementation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.project.Project;



import org.netbeans.spi.project.ProjectServiceProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.util.Exceptions;

/**
 *
 * @author Martin Janicek
 */
@ProjectServiceProvider(
    service = {
        MirahExtenderImplementation.class
    },
    projectType = {
        "org-netbeans-modules-maven"
    }
)
public class MavenMirahExtender implements MirahExtenderImplementation{

    private static final String MIRAH_GROUP_ID = "ca.weblite"; // NOI18N
    private static final String MIRAH_ARTIFACT_ID = "maven-mirah-plugin";       // NOI18N
    private static final String MIRAH_PLUGIN_VERSION = "1.0-SNAPSHOT";
    private final FileObject pom;

    
    public MavenMirahExtender(Project project) {
        this.pom = project.getProjectDirectory().getFileObject("pom.xml"); //NOI18N
        
    }

    @Override
    public boolean isActive() {
        final Boolean[] retValue = new Boolean[1];
        retValue[0] = false;
        try {
            pom.getFileSystem().runAtomicAction(new FileSystem.AtomicAction() {
                @Override
                public void run() throws IOException {
                    
                    ModelOperation op = new ModelOperation() {

                        @Override
                        public void performOperation(Model model) {
                            if (hasModelDependency(model, MIRAH_GROUP_ID, MIRAH_ARTIFACT_ID)) {
                                retValue[0] = true;
                            } else {
                                retValue[0] = false;
                            }
                        }
                    };
                    try {
                        Utilities.performPOMModelOperations(pom, Collections.singletonList(op));
                    } catch (XmlPullParserException ex) {
                       throw new IOException(ex);
                    }
                        
                    
                }
            });
        } catch (IOException ex) {
            return retValue[0];
        }
        return retValue[0];
    }

    @Override
    public boolean activate() {
        try {
            pom.getFileSystem().runAtomicAction(new FileSystem.AtomicAction() {
                @Override
                public void run() throws IOException {
                    List<ModelOperation> operations = new ArrayList<ModelOperation>();
                    operations.add(new AddMirahCompilerPlugin());
                    try {
                        Utilities.performPOMModelOperations(pom, operations);
                    } catch (XmlPullParserException ex) {
                        throw new IOException(ex);
                    }
                }
            });
        } catch (IOException ex) {
            return false;
        }
        return true;
    }

    @Override
    public boolean deactivate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isCurrent() {
        return isActive();
    }

    @Override
    public boolean update() {
        return activate();
    }

    
    
    public boolean hasModelDependency(Model model, String groupId, String artifactId){
        for ( Dependency dep : model.getDependencies()){
            if ( groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())){
                return true;
            }
        }
        return false;
    }
    
    
    
    class AddMirahCompilerPlugin implements ModelOperation {

        
        private Model model;
        @Override
        public void performOperation(final Model model) {
            
            
            this.model = model;
            
            Build build = model.getBuild();
            if ( build == null ){
                build = new Build();
                model.setBuild(build);
            }
            Plugin compilerPlugin = mirahCompilerPluginExists(build);
            if ( compilerPlugin == null ){
                compilerPlugin = createMirahCompilerPlugin();
                build.addPlugin(compilerPlugin);
                
            } else {
                Plugin newPlugin = createMirahCompilerPlugin();
                build.removePlugin(compilerPlugin);
                build.addPlugin(compilerPlugin);
            }
            
            
            
            if (hasModelDependency(model, "ca.weblite", "mirah-classes" )) {
                return;
            }
            
            
            
            //Now add the dependency to the mirah classes jar (for code 
            //completion
            Dependency dependency = new Dependency();
            dependency.setArtifactId("mirah-classes");
            dependency.setGroupId("ca.weblite");
            dependency.setVersion("1.0-SNAPSHOT");
            dependency.setScope("system");
            dependency.setSystemPath("${basedir}/target/mirah_tmp/classes.jar");
            dependency.setOptional(true);
            model.addDependency(dependency);
            
            
            
        }
        
        
        private Plugin mirahCompilerPluginExists(final Build build) {
            List<Plugin> plugins = build.getPlugins();
            
            if (plugins != null) {
                for (Plugin plugin : plugins) {
                    if (MIRAH_GROUP_ID.equals(plugin.getGroupId()) &&
                        MIRAH_ARTIFACT_ID.equals(plugin.getArtifactId())) {
                        return plugin;
                    }
                }
            }
            return null;
        }
        
        private Plugin createMirahCompilerPlugin() {
            Plugin plugin = new Plugin();
            plugin.setArtifactId(MIRAH_ARTIFACT_ID);
            plugin.setVersion(MIRAH_PLUGIN_VERSION);
            plugin.setGroupId(MIRAH_GROUP_ID);
            PluginExecution execution = new PluginExecution();
            execution.setPhase("process-sources");
            execution.addGoal("compile");
            plugin.addExecution(execution);
            

            return plugin;
        }
        
    }
    
    

}
