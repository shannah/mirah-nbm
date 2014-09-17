/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shannah
 */
public class RecompileQueue {
    private static WeakHashMap<Project,RecompileQueue> defaultMap = new WeakHashMap<Project,RecompileQueue>();
    
    public static RecompileQueue getProjectQueue(Project proj){
        if ( !defaultMap.containsKey(proj)){
            RecompileQueue queue = new RecompileQueue();
            defaultMap.put(proj, queue);
        }
        return defaultMap.get(proj);
    }
    
    private Set<FileObject> changed = new HashSet<FileObject>();
    
    public synchronized String getAndClearChangedSourcePaths(){
        if ( changed.isEmpty() ){
            return null;
        }
        
        List<FileObject> l = new ArrayList<FileObject>(changed);
        changed.clear();
        
        Set<String> changedPaths = new HashSet<String>();
        for ( FileObject fo : l ){
            ClassPath srcClassPath = ClassPath.getClassPath(fo, ClassPath.SOURCE);
            changedPaths.add(srcClassPath.toString());
            
        }
        
        StringBuilder sb = new StringBuilder();
        for ( String path : changedPaths ){
            sb.append(path).append(File.pathSeparator);
        }
        return sb.substring(0, sb.length()-File.pathSeparator.length());
    }
    
    public synchronized void addChanged(FileObject fo){
        changed.add(fo);
        //System.out.println("Changed now "+changed);
    }
}
