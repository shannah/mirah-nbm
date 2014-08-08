/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.lexer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.java.classpath.ClassPath;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author shannah
 */
public class ClassQuery {
    
    private Class cls;
    Map<String,Method> methods;  
    
    
    public ClassQuery(Class cls){
        this.cls = cls;
    }
    
    
    public static ClassPath[] getClassPaths(FileObject fo){
        return new ClassPath[]{
            ClassPath.getClassPath(fo, ClassPath.SOURCE),
            ClassPath.getClassPath(fo, ClassPath.EXECUTE),
            ClassPath.getClassPath(fo, ClassPath.COMPILE),
            ClassPath.getClassPath(fo, ClassPath.BOOT)
        };
    }
    
    public static Class findClass(String fqn, FileObject fo){
        ClassPath[] classPaths = getClassPaths(fo);
        
        for ( ClassPath cp : classPaths ){
            try {
                 Class cls = cp.getClassLoader(true).loadClass(fqn);
                if ( cls != null ){
                    return cls;
                }
            } catch (Throwable ex) {
                //Exceptions.printStackTrace(ex);
            }
        }
        return null;
    }
    
    
    
    
    public ClassQuery(String className, FileObject fo){
        this.cls = findClass(className, fo);
    }
    
    public Set<Method> getMethods(){
        if ( methods == null ){
            methods = new HashMap<String,Method>();
            
            Class c = cls;
            while ( c != null ){
                for (Method m : c.getDeclaredMethods()){
                    String mid = getMethodId(m);
                    if ( !methods.containsKey(mid)){
                        methods.put(mid, m);
                    }
                }
                if ( c.getSuperclass() == c ){
                    break;
                }
                c = c.getSuperclass();
            }
        }
        return new HashSet<Method>(methods.values());
        
    }
    
    public Set<Method> getAbstractMethods(){
        Set<Method> out = new HashSet<Method>();
        for ( Method m : getMethods()){
            if ( Modifier.isAbstract(m.getModifiers())){
                out.add(m);
            }
        }
        return out;
    }
    
    public Set<Method> getProtectedMethods(){
        Set<Method> out = new HashSet<Method>();
        for ( Method m : getMethods()){
            if ( Modifier.isProtected(m.getModifiers())){
                out.add(m);
            }
        }
        return out;
    }
    
    boolean isAncestorOf(Class parent){
        Class c = parent;
        while ( c != null ){
            if ( c == cls){
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }
    
    
    public Set<Method> getAccessibleMethods(Class context){
        Set<Method> out = new HashSet<Method>();
        boolean packagePrivateAllowed = context.getPackage().equals(cls.getPackage());
        boolean protectedAllowed = this.isAncestorOf(context);
        boolean privateAllowed = context == cls;
        for ( Method m : getMethods()){
            int mods = m.getModifiers();
            if ( Modifier.isPrivate(mods) && !privateAllowed){
                continue;
            } 
            if ( Modifier.isProtected(mods) && !protectedAllowed ){
                continue;
            } 
            if ( !Modifier.isPublic(mods) &&
                    !Modifier.isProtected(mods) &&
                    !Modifier.isPrivate(mods) &&
                    !packagePrivateAllowed ){
                continue;
            }
            out.add(m);
            
            
        }
        return out;
    }
    
    static String getMethodId(Method m){
        StringBuilder sb = new StringBuilder();
        sb.append(m.getName());
        for ( Class paramType : m.getParameterTypes()){
            sb.append("_").append(paramType.getName());
        }
        return sb.toString();
    }
    
    
    
            
    
}
