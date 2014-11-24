/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */

package ca.weblite.netbeans.mirah.antproject.base;

import ca.weblite.asm.WLMirahCompiler;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.project.classpath.ProjectClassPathModifier;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.project.ant.AntBuildExtender;
import org.netbeans.api.project.libraries.Library;
import org.netbeans.api.project.libraries.LibraryManager;
import ca.weblite.netbeans.mirah.support.spi.MirahExtenderImplementation;
import java.io.PrintWriter;
import java.net.URI;
import org.netbeans.spi.project.support.ant.GeneratedFilesHelper;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.EditableProperties;

import org.openide.util.Exceptions;
import org.openide.util.Mutex;
import org.openide.util.MutexException;
import org.openide.util.Parameters;

/**
 * Base class for each Ant based mirah extender. This class encapsulates most of
 * the code common for those types of project. The only thing that implementor
 * needs to do is return xls file used for mirah-build.xml generation.
 *
 * @author Martin Janicek
 */
public abstract class AbstractMirahExtender implements MirahExtenderImplementation {
    
    private static final String EXTENSIBLE_TARGET_NAME = "-pre-pre-compile"; // NOI18N
    private static final String MIRAH_EXTENSION_ID = "mirah"; // NOI18N
    private static final String PROJECT_PROPERTIES_PATH = "nbproject/project.properties"; // NOI18N
    private static final String CN1_LIBRARY_PROPERTIES_PATH = "codenameone_library.properties";  
    private static final String EXCLUDE_PROPERTY = "build.classes.excludes"; // NOI18N
    private static final String DISABLE_COMPILE_ON_SAVE = "compile.on.save.unsupported.mirah"; // NOI18N
    private static final String EXCLUSION_PATTERN = "**/*.mirah"; // NOI18N
    private static final String MIRAH_BUILD_PATH_PROPERTY = "mirah.build.dir";
    private static final String MIRAH_MACROS_JARDIR_PROPERTY = "mirah.macros.jardir";
    private static final String VERSION_PROPERTY = "mirah.plugin.version";
    private static final int PLUGIN_VERSION=25;
    

    private final Project project;


    protected AbstractMirahExtender(Project project) {
        this.project = project;
    }

    protected abstract URL getMirahBuildXls();


    /**
     * Checks if the project has mirah activated. Please be aware that this method
     * checks only build script extension, not ClassPath nor excludes
     *
     * @return true if the project has modified build-impl.xml with mirah extension
     */
    @Override
    public boolean isActive() {
        URL loc = WLMirahCompiler.class.getProtectionDomain().getCodeSource().getLocation();
       
        AntBuildExtender extender = project.getLookup().lookup(AntBuildExtender.class);
        boolean out = extender != null && extender.getExtension(MIRAH_EXTENSION_ID) != null;
        
        return out;
    }

    @Override
    public boolean activate() {
        
        boolean out = addClasspath() & addExcludes() & addBuildScript() & addMacrosClasspath()/*& addDisableCompileOnSaveProperty()*/;
        
        return out;
    }

    @Override
    public boolean deactivate() {
        return removeClasspath() & removeExcludes() & removeBuildScript() /*& removeDisableCompileOnSaveProperty()*/;
    }

    /**
     * Add mirah-all.jar to the project ClassPath.
     */
    protected final boolean addClasspath() {
        
        try {
            
            Sources sources = ProjectUtils.getSources(project);
            
            FileObject rootDir = project.getProjectDirectory();
            FileObject buildDir = rootDir.getFileObject("build");
            if ( buildDir == null ){
                buildDir = rootDir.createFolder("build");
            }
            FileObject mirahDir = buildDir.getFileObject("mirah");
            if ( mirahDir == null ){
                mirahDir = buildDir.createFolder("mirah");
            }
            
            
            
            SourceGroup[] sourceGroups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
            for (SourceGroup sourceGroup : sourceGroups) {
                if (!sourceGroup.getRootFolder().getName().equals("test")) {
                    //ProjectClassPathModifier.addLibraries(new Library[]{mirahAllLib}, sourceGroup.getRootFolder(), ClassPath.COMPILE);
                    ProjectClassPathModifier.addRoots(new URI[]{mirahDir.toURI()}, sourceGroup.getRootFolder(), ClassPath.COMPILE);
                }
            }
            return true;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (UnsupportedOperationException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    /**
     * Inverse operation to {@link #addClasspath()}.
     * Removes mirah-all.jar from project ClassPath.
     */
    protected final boolean removeClasspath() {
        if  ( true ) return true;
        Library mirahAllLib = getMirahAllLibrary(); // NOI18N
        if (mirahAllLib != null) {
            try {
                Sources sources = ProjectUtils.getSources(project);
                SourceGroup[] sourceGroups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
                for (SourceGroup sourceGroup : sourceGroups) {
                    ProjectClassPathModifier.removeLibraries(new Library[]{mirahAllLib}, sourceGroup.getRootFolder(), ClassPath.COMPILE);
                }
                return true;
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } catch (UnsupportedOperationException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return false;
    }

    private Library getMirahAllLibrary() {
        for (Library library : LibraryManager.getDefault().getLibraries()) {

            List<URL> uriContent = library.getContent("classpath"); // NOI18N
            
            try {
                if (containsClass(uriContent, "ca.weblite.mirah.ant.WLMirahCompiler")) { // NOI18N
                    return library;
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

        }
        return null;
    }

    private boolean containsClass(List<URL> classPath, String className) throws IOException {
        Parameters.notNull("classpath", classPath); // NOI18N
        Parameters.notNull("className", className); // NOI18N

        List<File> diskFiles = new ArrayList<File>();
        for (URL url : classPath) {
            URL archiveURL = FileUtil.getArchiveFile(url);

            if (archiveURL != null) {
                url = archiveURL;
            }

            if ("nbinst".equals(url.getProtocol())) { // NOI18N
                // try to get a file: URL for the nbinst: URL
                FileObject fo = URLMapper.findFileObject(url);
                if (fo != null) {
                    URL localURL = URLMapper.findURL(fo, URLMapper.EXTERNAL);
                    if (localURL != null) {
                        url = localURL;
                    }
                }
            }

            FileObject fo = URLMapper.findFileObject(url);
            if (fo != null) {
                File diskFile = FileUtil.toFile(fo);
                if (diskFile != null) {
                    diskFiles.add(diskFile);
                }
            }
        }

        return containsClass(diskFiles, className);
    }

    private boolean containsClass(Collection<File> classpath, String className) throws IOException {
        Parameters.notNull("classpath", classpath); // NOI18N
        Parameters.notNull("driverClassName", className); // NOI18N
        String classFilePath = className.replace('.', '/') + ".class"; // NOI18N
        for (File file : classpath) {
            if (file.isFile()) {
                JarFile jf = new JarFile(file);
                try {
                    Enumeration entries = jf.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = (JarEntry) entries.nextElement();
                        if (classFilePath.equals(entry.getName())) {
                            return true;
                        }
                    }
                } finally {
                    jf.close();
                }
            } else {
                if (new File(file, classFilePath).exists()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add **\/*.mirah to build.classes.excludes.
     */
    protected final boolean addExcludes() {
        try {
            EditableProperties props = getEditableProperties(project, PROJECT_PROPERTIES_PATH);
            String exclude = props.getProperty(EXCLUDE_PROPERTY);
            //props.setProperty("mirahc.ant.classpath", "foobarfoo");
            if (!exclude.contains(EXCLUSION_PATTERN)) {
                //System.out.println(EXCLUSION_PATTERN+" is not found");
                props.setProperty(EXCLUDE_PROPERTY, exclude + "," + EXCLUSION_PATTERN); // NOI18N
                storeEditableProperties(project, PROJECT_PROPERTIES_PATH, props);
            } else {
                //System.out.println("Exclusion pattern "+EXCLUSION_PATTERN+" was found");
            }
            return true;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    /**
     * Inverse operation to {@link #addExcludes()}.
     * Remove **\/*.mirah from build.classes.excludes.
     */
    protected final boolean removeExcludes() {
        try {
            EditableProperties props = getEditableProperties(project, PROJECT_PROPERTIES_PATH);
            String exclude = props.getProperty(EXCLUDE_PROPERTY);
            if (exclude.contains("," + EXCLUSION_PATTERN)) {
                exclude = exclude.replace("," + EXCLUSION_PATTERN, "");
                props.setProperty(EXCLUDE_PROPERTY, exclude);
                storeEditableProperties(project, PROJECT_PROPERTIES_PATH, props);
            }
            return true;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    protected final boolean addMacrosClasspath(){
        try {
            EditableProperties props = getEditableProperties(project, PROJECT_PROPERTIES_PATH);
            props.setProperty(MIRAH_MACROS_JARDIR_PROPERTY, "lib/mirah/macros"); // NOI18N
            storeEditableProperties(project, PROJECT_PROPERTIES_PATH, props);
            FileObject projectFO = project.getProjectDirectory();
            
            FileUtil.createFolder(new File(projectFO.getPath(), "lib/mirah/macros"));
            return true;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }
    
    /**
     * Wraps javac into the mirahc using imported mirah-build.xml. Adds mirah-build.xml
     * to the project, modifies build-impl.xml with respect to mirah compiler. This method
     * has to be call on every project that needs to compile mirah scripts/classes.
     *
     * @return true if the mirah extension were successfully applied, false otherwise
     */
    protected final boolean addBuildScript() {
        
        try {
            
        
            AntBuildExtender extender = project.getLookup().lookup(AntBuildExtender.class);
            System.out.println("Step 0");
            if (extender != null && extender.getExtensibleTargets().contains(EXTENSIBLE_TARGET_NAME)) {
                System.out.println("Step 1");
                AntBuildExtender.Extension extension = extender.getExtension(MIRAH_EXTENSION_ID);
                
                if (extension == null) {
                    System.out.println("Step 2");
                    FileObject destDirFO = project.getProjectDirectory().getFileObject("nbproject"); // NOI18N
                    FileObject projectFO = project.getProjectDirectory();
                    try {
                        GeneratedFilesHelper helper = new GeneratedFilesHelper(project.getProjectDirectory());
                        // Check if this is a codename one project
                        FileObject cn1PropertiesFO = projectFO.getFileObject("codenameone_settings", "properties");
                        FileObject cn1LibraryPropertiesFO = projectFO.getFileObject("codenameone_library", "properties");
                        boolean isCodename1Lib = cn1LibraryPropertiesFO != null;
                        boolean isCodename1Proj = cn1PropertiesFO != null;
                        if ( isCodename1Lib ){
                            addCN1LibProperty();
                        }
                        if ( isCodename1Lib || isCodename1Proj){
                            // This is a codename one project
                            // With Codename One projects we don't want to override Javac.
                            // Instead we will do a precompile and add the .class files to the 
                            // lib/cls/impl directory so that they will be treated like 
                            // a .cn1lib file.
                            FileObject cn1buildFO = destDirFO.getFileObject("mirah-build-cn1", "xml");
                            OutputStream os = null;
                            if ( cn1buildFO == null ){
                                os = destDirFO.createAndOpen("mirah-build-cn1.xml");
                            } else {
                                os = cn1buildFO.getOutputStream();
                            }
                            InputStream input = AbstractMirahExtender.class.getResourceAsStream("/ca/weblite/netbeans/mirah/antproject/resources/mirah-build-cn1.xml");
                            try {
                                FileUtil.copy(input, os);
                                cn1buildFO = destDirFO.getFileObject("mirah-build-cn", "xml");
                            } finally {
                                try {
                                    os.close();
                                } catch ( Exception ex){}
                                try {
                                    input.close();

                                } catch ( Exception ex){}
                            }
                            
                            if ( cn1buildFO != null ){
                                AntBuildExtender.Extension cn1extension = extender.getExtension("mirah-cn1-build");
                                if ( cn1extension == null ){
                                    cn1extension = extender.addExtension("mirah-cn1-build", cn1buildFO);
                                    cn1extension.addDependency(EXTENSIBLE_TARGET_NAME, "mirah-precompile");
                                    cn1extension.addDependency(EXTENSIBLE_TARGET_NAME, "mirah-precompile-cn1lib");
                                    cn1extension.addDependency("-pre-pre-jar", "mirah-postcompile-cn1lib");
                                    
                                    
                                }
                                
                            }
                            
                        }
                        
                        helper.generateBuildScriptFromStylesheet("nbproject/mirah-build.xml", getMirahBuildXls());
                        FileObject destFileFO = destDirFO.getFileObject("mirah-build", "xml"); // NOI18N
                        extension = extender.addExtension(MIRAH_EXTENSION_ID, destFileFO);
                        extension.addDependency(EXTENSIBLE_TARGET_NAME, "-mirah-init-macrodef-javac"); // NOI18N
                        //extension.addDependency("-init-project", "-mirah-pre-init");
                        ProjectManager.getDefault().saveProject(project);
                        FileObject buildImplFO = destDirFO.getFileObject("build-impl", "xml");
                        String contents = buildImplFO.asText();
                        if ( !contents.contains("mirah-build.xml")){
                            // Codename One projects don't seem to be regenerating the build-impl.xml file automatically
                            // so we have to use this ugly regex workaround to updating it ourselves. 
                            contents = contents.replaceAll("<project [^>]*>", "$0\n <import file=\"mirah-build.xml\"/>");
                            if ( isCodename1Lib || isCodename1Proj){
                                contents = contents.replaceAll("<project [^>]*>", "$0\n <import file=\"mirah-build-cn1.xml\"/>");
                            }
                            contents = contents.replaceAll("(<target depends=\".*?)(\"[^>]* name=\"-pre-pre-compile\">)", "$1,-mirah-init-macrodef-javac$2");
                            if ( isCodename1Lib || isCodename1Proj){
                                contents = contents.replaceAll("(<target depends=\".*?)(\"[^>]* name=\"-pre-pre-compile\">)", "$1,mirah-precompile,mirah-precompile-cn1lib$2");
                            } 
                            contents = contents.replaceAll("(<target depends=\".*?)(\"[^>]* name=\"init\"[^>]*>)", "$1,-mirah-pre-init$2");
                            PrintWriter os = null;
                            try {
                                os = new PrintWriter(buildImplFO.getOutputStream());
                                os.write(contents);
                            } finally {
                                try {
                                    os.close();
                                } catch ( Exception ex){}
                            }
                        } else if ( !contents.contains("-mirah-pre-init")){
                            System.out.println("Installing mirah pre init");
                            contents = contents.replaceAll("(<target depends=\".*?)(\"[^>]* name=\"init\"[^>]*>)", "$1,-mirah-pre-init$2");
                            PrintWriter os = null;
                            try {
                                os = new PrintWriter(buildImplFO.getOutputStream());
                                os.write(contents);
                            } finally {
                                try {
                                    os.close();
                                } catch ( Exception ex){}
                            }
                        }
                        return true;
                    } catch (IOException ioe) {
                        Exceptions.printStackTrace(ioe);
                    }
                } else {
                    // extension is already registered
                    return true;
                }
            }
        } finally {
            
        }
        return false;
    }

    /**
     * Inverse operation to {@link #addBuildScript()}. Removes mirah-build.xml
     * from the project and reverts changes in build-impl.xml related to mirah
     * activation.
     *
     * @return true if the mirah extension in build scripts were successfully deactivated
     */
    protected final boolean removeBuildScript() {
        AntBuildExtender extender = project.getLookup().lookup(AntBuildExtender.class);
        if (extender != null && extender.getExtensibleTargets().contains(EXTENSIBLE_TARGET_NAME)) {
            AntBuildExtender.Extension extension = extender.getExtension(MIRAH_EXTENSION_ID);
            if (extension != null) {
                FileObject destDirFO = project.getProjectDirectory().getFileObject("nbproject"); // NOI18N
                try {
                    extension.removeDependency(EXTENSIBLE_TARGET_NAME, "-mirah-init-macrodef-javac"); // NOI18N
                    //extension.removeDependency("-init-project", "-mirah-pre-init");
                    extender.removeExtension(MIRAH_EXTENSION_ID);
                    if (destDirFO != null) {
                        FileObject fileToRemove = destDirFO.getFileObject("mirah-build.xml"); // NOI18N
                        if (fileToRemove != null) {
                            fileToRemove.delete();
                        }
                    }
                    ProjectManager.getDefault().saveProject(project);
                    
                    FileObject buildImplFO = destDirFO.getFileObject("build-impl", "xml");
                    String contents = buildImplFO.asText();
                    if ( contents.contains("mirah-build.xml")){
                        //contents = contents.replaceAll("<project [^>]*>", "$0\n <import file=\"mirah-build-cn1.xml\"/><import file=\"mirah-build.xml\"/>");
                        contents = contents.replaceAll("<import file=\"mirah-build-cn1.xml\"/>", "");
                        contents = contents.replaceAll("<import file=\"mirah-build.xml\"/>", "");
                        //contents = contents.replaceAll("(<target depends=\".*?)(\"[^>]* name=\"-pre-pre-compile\">)", "$1,mirah-precompile,mirah-precompile-cn1lib,-mirah-init-macrodef-javac$2");
                        contents = contents.replaceAll(",mirah-precompile,mirah-precompile-cn1lib", "");
                        contents = contents.replaceAll(",-mirah-init-macrodef-javac", "");
                        //contents = contents.replaceAll("(<target depends=\".*?)(\"[^>]* name=\"init\"[^>]*>)", "$1,-mirah-pre-init$2");
                        contents = contents.replaceAll(",-mirah-pre-init", "");
                        PrintWriter os = null;
                        try {
                            os = new PrintWriter(buildImplFO.getOutputStream());
                            os.write(contents);
                        } finally {
                            try {
                                os.close();
                            } catch ( Exception ex){}
                        }
                    } else if ( contents.contains(",-mirah-pre-init")){
                        contents = contents.replaceAll(",-mirah-pre-init", "");
                        PrintWriter os = null;
                        try {
                            os = new PrintWriter(buildImplFO.getOutputStream());
                            os.write(contents);
                        } finally {
                            try {
                                os.close();
                            } catch ( Exception ex){}
                        }
                    }
                    
                    return true;
                } catch (IOException ioe) {
                    Exceptions.printStackTrace(ioe);
                }
            } else {
                // extension is not registered
                return true;
            }
        }
        return false;
    }

    /**
     * Disables compile on save for the project.
     *
     * @return true if CoS were disabled, false otherwise
     */
    protected final boolean addDisableCompileOnSaveProperty() {
        try {
            EditableProperties props = getEditableProperties(project, PROJECT_PROPERTIES_PATH);
            props.put(DISABLE_COMPILE_ON_SAVE, "true");
            storeEditableProperties(project, PROJECT_PROPERTIES_PATH, props);
            return true;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }
    
    private final boolean addCN1LibProperty(){
        try {
            EditableProperties props = getEditableProperties(project, CN1_LIBRARY_PROPERTIES_PATH);
            props.put("codename1.is_library", "true");
            storeEditableProperties(project, CN1_LIBRARY_PROPERTIES_PATH, props);
            return true;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    /**
     * Inverse operation to {@link #addDisableCompileOnSaveProperty()}.
     * Enabled compile on save for the project.
     *
     * @return true if CoS were enabled, false otherwise
     */
    protected final boolean removeDisableCompileOnSaveProperty() {
        try {
            EditableProperties props = getEditableProperties(project, PROJECT_PROPERTIES_PATH);
            props.remove(DISABLE_COMPILE_ON_SAVE);
            storeEditableProperties(project, PROJECT_PROPERTIES_PATH, props);
            return true;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    private static EditableProperties getEditableProperties(final Project prj,final  String propertiesPath)
        throws IOException {
        try {
            return
            ProjectManager.mutex().readAccess(new Mutex.ExceptionAction<EditableProperties>() {
                @Override
                public EditableProperties run() throws IOException {
                    FileObject propertiesFo = prj.getProjectDirectory().getFileObject(propertiesPath);
                    EditableProperties ep = null;
                    if (propertiesFo!=null) {
                        InputStream is = null;
                        ep = new EditableProperties(false);
                        try {
                            is = propertiesFo.getInputStream();
                            ep.load(is);
                        } finally {
                            if (is != null) {
                                is.close();
                            }
                        }
                    }
                    return ep;
                }
            });
        } catch (MutexException ex) {
            return null;
        }
    }

    private static void storeEditableProperties(final Project prj, final  String propertiesPath, final EditableProperties ep)
        throws IOException {
        try {
            ProjectManager.mutex().writeAccess(new Mutex.ExceptionAction<Void>() {
                @Override
                public Void run() throws IOException {
                    FileObject propertiesFo = prj.getProjectDirectory().getFileObject(propertiesPath);
                    if (propertiesFo!=null) {
                        OutputStream os = null;
                        try {
                            os = propertiesFo.getOutputStream();
                            ep.store(os);
                        } finally {
                            if (os != null) {
                                os.close();
                            }
                        }
                    }
                    return null;
                }
            });
        } catch (MutexException ex) {
        }
    }

    @Override
    public boolean isCurrent(){
        try {
            EditableProperties props = getEditableProperties(project, PROJECT_PROPERTIES_PATH);
            int version = 0;
            if ( props.containsKey(VERSION_PROPERTY) ){
                try {
                    version = Integer.parseInt(props.getProperty(VERSION_PROPERTY));
                } catch ( Throwable t){}
            }
            System.out.println("The current version is "+version);
            return isActive() &&  version >= PLUGIN_VERSION;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        
    }

    @Override
    public boolean update(){
        try {
            EditableProperties props = getEditableProperties(project, PROJECT_PROPERTIES_PATH);
            int version = 0;
            if ( props.containsKey(VERSION_PROPERTY) ){
                try {
                    version = Integer.parseInt(props.getProperty(VERSION_PROPERTY));
                } catch ( Throwable t){}
            }
            
            if ( !removeExcludes() || !removeBuildScript() || !addExcludes() || !addBuildScript()){
                System.out.println("Failed to update");
                return false;
            }
            
            if ( version < PLUGIN_VERSION){
                props = getEditableProperties(project, PROJECT_PROPERTIES_PATH);
                props.setProperty(VERSION_PROPERTY, String.valueOf(PLUGIN_VERSION));
                storeEditableProperties(project, PROJECT_PROPERTIES_PATH, props);
            }
            
            
            return true;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return false;
        }
    }
    
    
}
