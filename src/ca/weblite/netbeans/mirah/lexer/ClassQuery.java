/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.lexer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.java.classpath.ClassPath;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.openide.filesystems.FileObject;

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
        return findClass(fqn, fo, true );
    }
    
    public static Class findClass(String fqn, FileObject fo, boolean cache){
        ClassPath[] classPaths = getClassPaths(fo);
        
        for ( ClassPath cp : classPaths ){
            try {
                 Class cls = cp.getClassLoader(cache).loadClass(fqn);
                if ( cls != null ){
                    return cls;
                }
            } catch (Throwable ex) {
                //Exceptions.printStackTrace(ex);
            }
        }
        return null;
    }
    
    
    
    
    public ClassQuery(String className, FileObject fo, boolean cache){
        this.cls = findClass(className, fo, cache);
    }
    
    public Set<Method> getMethods(){
        if ( methods == null ){
            methods = new HashMap<String,Method>();
           
            Class c = cls;
            while ( c != null ){
                try {
                    for (Method m : c.getDeclaredMethods()){
                        String mid = getMethodId(m);
                        if ( !methods.containsKey(mid)){
                            methods.put(mid, m);
                        }
                    }
                } catch ( Throwable ex){}
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
    
    public Set<Class> getInterfaces(){
        
        //// BROKEN!!!!  Not finding any interfaces for any classes at all
        
        Set<Class> out = new HashSet<Class>();
        
        //out.addAll(Arrays.asList(cls.getInterfaces()));
        Class p = cls;
        while ( p != null ){
            out.addAll(Arrays.asList(p.getInterfaces()));
            p = p.getSuperclass();
        }
        return out; 
    }
    
    public Set<Method> getUnimplementedMethodsRequiredByInterfaces(){
        Map<String,Method> required = new HashMap<String,Method>();
        for ( Class i : getInterfaces()){
            ClassQuery iq = new ClassQuery(i);
            for ( Method m : iq.getMethods()){
                String mid = getMethodId(m);
                required.put(mid, m);
            }
        }
        for ( Method m : getMethods() ){
            String mid = getMethodId(m);
            required.remove(mid);
        }
        
        HashSet<Method> out = new HashSet<Method>();
        out.addAll(required.values());
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
    
    
    
    /**
    * Returns a list containing one parameter name for each argument accepted
    * by the given constructor. If the class was compiled with debugging
    * symbols, the parameter names will match those provided in the Java source
    * code. Otherwise, a generic "arg" parameter name is generated ("arg0" for
    * the first argument, "arg1" for the second...).
    * 
    * This method relies on the constructor's class loader to locate the
    * bytecode resource that defined its class.
    * 
    * @param constructor
    * @return 
    * @throws IOException
    */
   public  List<String> getParameterNames(Constructor<?> constructor) throws IOException {
       //Class<?> declaringClass = constructor.getDeclaringClass();
       Class declaringClass = cls;
       ClassLoader declaringClassLoader = declaringClass.getClassLoader();

       Type declaringType = Type.getType(declaringClass);
       String constructorDescriptor = Type.getConstructorDescriptor(constructor);
       String url = declaringType.getInternalName() + ".class";

       InputStream classFileInputStream = declaringClassLoader.getResourceAsStream(url);
       if (classFileInputStream == null) {
           throw new IllegalArgumentException("The constructor's class loader cannot find the bytecode that defined the constructor's class (URL: " + url + ")");
       }

       ClassNode classNode;
       try {
           classNode = new ClassNode();
           ClassReader classReader = new ClassReader(classFileInputStream);
           classReader.accept(classNode, 0);
       } finally {
           classFileInputStream.close();
       }

       @SuppressWarnings("unchecked")
       List<MethodNode> methods = classNode.methods;
       for (MethodNode method : methods) {
           if (method.name.equals("<init>") && method.desc.equals(constructorDescriptor)) {
               Type[] argumentTypes = Type.getArgumentTypes(method.desc);
               List<String> parameterNames = new ArrayList<String>(argumentTypes.length);

               @SuppressWarnings("unchecked")
               List<LocalVariableNode> localVariables = method.localVariables;
               for (int i = 0; i < argumentTypes.length; i++) {
                   // The first local variable actually represents the "this" object
                   parameterNames.add(localVariables.get(i + 1).name);
               }

               return parameterNames;
           }
       }

       return null;
   }
   
   public static List<String> getParameterNames(Class cls, Method constructor, FileObject fo) throws IOException {
       //Class<?> declaringClass = constructor.getDeclaringClass();
       Class declaringClass = cls;
       ClassLoader declaringClassLoader = declaringClass.getClassLoader();
       if ( declaringClassLoader == null ){
           cls = findClass(cls.getName(), fo);
           declaringClassLoader = cls.getClassLoader();
           
       }
       if ( declaringClassLoader == null ){
           return null;
       }
       Type declaringType = Type.getType(declaringClass);
       String constructorDescriptor = Type.getMethodDescriptor(constructor);
       String url = declaringType.getInternalName() + ".class";

       InputStream classFileInputStream = declaringClassLoader.getResourceAsStream(url);
       if (classFileInputStream == null) {
           throw new IllegalArgumentException("The constructor's class loader cannot find the bytecode that defined the constructor's class (URL: " + url + ")");
       }

       ClassNode classNode;
       try {
           classNode = new ClassNode();
           ClassReader classReader = new ClassReader(classFileInputStream);
           classReader.accept(classNode, 0);
       } finally {
           classFileInputStream.close();
       }

       @SuppressWarnings("unchecked")
       List<MethodNode> methods = classNode.methods;
       for (MethodNode method : methods) {
           if (method.name.equals(constructor.getName()) && method.desc.equals(constructorDescriptor)) {
               Type[] argumentTypes = Type.getArgumentTypes(method.desc);
               List<String> parameterNames = new ArrayList<String>(argumentTypes.length);
               @SuppressWarnings("unchecked")
               List<LocalVariableNode> localVariables = method.localVariables;
               boolean isStatic = true;
               if ( localVariables != null && !localVariables.isEmpty() && localVariables.get(0).name.equals("this")){
                   isStatic=false;
               }
               int offset = isStatic ? 0:1;
               for (int i = 0; i < argumentTypes.length; i++) {
                   // The first local variable actually represents the "this" object
                   try {
                    parameterNames.add(localVariables.get(i + offset).name);
                   } catch ( NullPointerException npe){
                       npe.printStackTrace();
                   }
               }

               return parameterNames;
           }
       }

       List<String> out = null;
       cls = cls.getSuperclass();
       while ( cls != null && out == null ){
           out = getParameterNames(cls, constructor, fo);
          cls = cls.getSuperclass();
       }
       if ( out != null ){
           return out;
       }
       return null;
   }
   
   /**
    * Returns a list containing one parameter name for each argument accepted
    * by the given constructor. If the class was compiled with debugging
    * symbols, the parameter names will match those provided in the Java source
    * code. Otherwise, a generic "arg" parameter name is generated ("arg0" for
    * the first argument, "arg1" for the second...).
    * 
    * This method relies on the constructor's class loader to locate the
    * bytecode resource that defined its class.
    * 
    * @param constructor
    * @return 
    * @throws IOException
    */
   public List<String> getParameterNames(Method constructor, FileObject fo) throws IOException {
       return getParameterNames(cls, constructor, fo);
   }
            
    
}
