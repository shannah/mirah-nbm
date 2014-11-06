/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.maven;


import ca.weblite.netbeans.mirah.support.spi.MirahExtenderImplementation;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;



import org.netbeans.spi.project.ProjectServiceProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
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
    private static final String MIRAH_PLUGIN_VERSION = "1.0";
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
                            
                            if ( !retValue[0] ){
                                return;
                            }
                            
                            if (hasModelDependency(model, MIRAH_GROUP_ID , "mirah-tmp-classes")) {
                                retValue[0] = true;
                            } else {
                                retValue[0] = false;
                            }
                            
                            if ( !retValue[0] ){
                                return;
                            }
                            
                            if (hasModelDependency(model, MIRAH_GROUP_ID, "mirah-tmp-classes-test")) {
                                retValue[0] = true;
                            } else {
                                retValue[0] = false;
                            }
                            
                            if ( !retValue[0] ){
                                return;
                            }
                            
                            File libDir = new File(FileUtil.toFile(pom.getParent()), "lib");
                            if ( !libDir.exists() ){
                                retValue[0] = false;
                                return;
                            }
                            
                            File mirahClassesJar = new File(libDir, "mirah-tmp-classes.jar");
                            if ( !mirahClassesJar.exists()){
                                retValue[0] = false;
                                return;
                            }
                            
                            File mirahTestClassesJar = new File(libDir, "mirah-tmp-classes-test.jar");
                            if ( !mirahTestClassesJar.exists()){
                                retValue[0] = false;
                                return;
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
            
            
            
            if (!hasModelDependency(model, "ca.weblite", "mirah-tmp-classes" )) {
                // Now create an empty mirah-tmp-classes jar
                //System.out.println("About to create mirah-tmp-classes dir");
                File libDir = new File(FileUtil.toFile(pom.getParent()), "lib");
                File mirahTmpClassesDir = new File(libDir, "mirah-tmp-classes");
                if ( !mirahTmpClassesDir.exists()){
                    //System.out.println("Dir doesn't exist.. creating it");
                    mirahTmpClassesDir.mkdirs();
                } else {
                    //System.out.println("Dir already existed");
                }
                File mirahTmpClassesJar = new File(libDir, "mirah-tmp-classes.jar");
                if ( !mirahTmpClassesJar.exists() ){
                    try {
                        createJar(mirahTmpClassesDir, mirahTmpClassesDir.getPath(), mirahTmpClassesJar);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
                
                //Now add the dependency to the mirah classes jar (for code 
                //completion
                Dependency dependency = new Dependency();
                dependency.setArtifactId("mirah-tmp-classes");
                dependency.setGroupId("ca.weblite");
                dependency.setVersion("1.0-SNAPSHOT");
                dependency.setScope("system");
                dependency.setSystemPath("${basedir}/lib/mirah-tmp-classes.jar");
                dependency.setOptional(true);
                model.addDependency(dependency);
            }
            
            if (!hasModelDependency(model, "ca.weblite", "mirah-tmp-classes-test" )) {
                File libDir = new File(FileUtil.toFile(pom.getParent()), "lib");
                File mirahTmpClassesTestDir = new File(libDir, "mirah-tmp-classes-test");
                if ( !mirahTmpClassesTestDir.exists()){
                    mirahTmpClassesTestDir.mkdirs();
                } else {
                    //System.out.println("Dir already existed");
                }
                File mirahTmpClassesTestJar = new File(libDir, "mirah-tmp-classes-test.jar");
                if ( !mirahTmpClassesTestJar.exists() ){
                    try {
                        createJar(mirahTmpClassesTestDir, mirahTmpClassesTestDir.getPath(), mirahTmpClassesTestJar);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }



                Dependency dependency = new Dependency();
                dependency.setArtifactId("mirah-tmp-classes-test");
                dependency.setGroupId("ca.weblite");
                dependency.setVersion("1.0-SNAPSHOT");
                dependency.setScope("system");
                dependency.setSystemPath("${basedir}/lib/mirah-tmp-classes-test.jar");
                dependency.setOptional(true);
                
                model.addDependency(dependency);
                
            }
            
            
            
            
            
            
            
            
        }
        
        
        private void createJar(File source, String sourceRoot, File jarFile) throws IOException {
        FileOutputStream fos = null;
        JarOutputStream jos = null;
        try {
            fos = new FileOutputStream(jarFile);
            jos = new JarOutputStream(fos);
            jos.setLevel(0);
            
            addToJar(source, sourceRoot, jos);
        } finally {
            try {
                if ( jos != null ) jos.close();
            } catch ( Throwable t ){}
            try {
                if ( fos != null ) fos.close();
            } catch ( Throwable t){}
        }
              
        
        
    }
        private void addToJar(File source, String sourceRoot, JarOutputStream jos) throws IOException {
            if ( source.getName().endsWith(".class")){
                String fileName = formatEntry(source, sourceRoot, false);
                //System.out.println("Adding file "+fileName+" to jar ("+source+")");
                ZipEntry entry = new ZipEntry(fileName);
                jos.putNextEntry(entry);
                InputStream fis = null;
                try {
                    fis = new FileInputStream(source);
                    byte[] buf = new byte[4096];
                    int len;
                    while ( (len = fis.read(buf)) != -1 ){
                        //System.out.println("Writing "+len+" bytes");
                        jos.write(buf, 0, len);
                    }
                    jos.closeEntry();
                } finally {
                    try {
                        if ( fis != null ){
                            fis.close();
                        }
                    } catch ( Exception ex){}
                }
            } else if ( source.isDirectory() ){
                String dirName = formatEntry(source, sourceRoot, true);
                //System.out.println("Adding "+dirName+" to jar");
                //ZipEntry entry = new ZipEntry(dirName);
                //jos.putNextEntry(entry);
                for ( File child : source.listFiles()){
                    addToJar(child, sourceRoot, jos);
                }
                //jos.closeEntry();
            }
        }

        private String formatEntry(File f, String sourceRoot, boolean directory){
            if ( directory ){
                String name = f.getPath().substring(sourceRoot.length());
                name = name.replace("\\", "/");
                if ( !name.endsWith("/")){
                    name += "/";
                }
                if ( name.startsWith("/")){
                    name = name.substring(1);
                }
                return name;
            } else {
                String name = f.getPath().substring(sourceRoot.length());
                name = name.replace("\\", "/");

                if ( name.startsWith("/")){
                    name = name.substring(1);
                }
                return name;
            }
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
            execution.setId("Compile Mirah Sources");
            execution.setPhase("process-sources");
            execution.addGoal("compile");
            plugin.addExecution(execution);
            
            execution = new PluginExecution();
            execution.setId("Compile Mirah Tests");
            execution.setPhase("process-test-sources");
            execution.addGoal("testCompile");
            plugin.addExecution(execution);
            

            return plugin;
        }
        
    }
    
    

}
