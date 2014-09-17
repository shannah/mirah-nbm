/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.netbeans.api.java.classpath.ClassPath;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shannah
 */
public class ClassIndex {

    public abstract static class Query implements Comparable<Query> {
        private int priority;
        private String simpleName;
        private String prefix = "";
        private static int nextId = 0;
        private int id;
        
        protected Query(int priority, String simpleName, String prefix){
            this.priority = priority;
            this.simpleName = simpleName;
            this.prefix = prefix;
            id = nextId++;
        }
        

        @Override
        public int compareTo(Query o) {
            if ( priority < o.priority ){
                return -1;
            } else if ( priority > o.priority ){
                return 1;
            } else if ( id < o.id ){
                return -1;
            } else if ( id > o.id ){
                return 1;
            } else {
                return 0;
            }
        }
        
        public abstract FileObject[] getRoots();
    }
    
    public static class ClassPathQuery extends Query {
        
        ClassPath classPath;
        public ClassPathQuery(int priority, String simpleName, String prefix, ClassPath classPath){
            super(priority, simpleName, prefix);
            this.classPath = classPath;
        }

        @Override
        public FileObject[] getRoots() {
            return classPath.getRoots();
        }
        
    }
    
    public static class FileObjectQuery extends Query {
        FileObject root;
        public FileObjectQuery(int priority, String simpleName, String prefix, FileObject root){
            super(priority, simpleName, prefix);
            this.root = root;
        }

        @Override
        public FileObject[] getRoots() {
            return new FileObject[]{root};
        }   
    }
    
    public static class CompoundQuery extends Query {
        SortedSet<Query> queries = new TreeSet<Query>();
        
        public CompoundQuery(int priority){
            super(priority, "*", "");
        }
        
        public void addQuery(Query query){
            queries.add(query);
        }

        @Override
        public FileObject[] getRoots() {
            List<FileObject> roots = new ArrayList<FileObject>();
            for ( Query q : queries ){
                for ( FileObject root : q.getRoots() ){
                    roots.add(root);
                }
            }
            return roots.toArray(new FileObject[0]);
        }
        
        
    }
    
    public abstract static class Future {
        List<String> matches = new ArrayList<String>();
        Set<String> matchSet = new HashSet<String>();
        
        protected abstract void resultsAdded();
        public List<String> getMatches(){
            return matches;
        }
        public void addMatch(String match){
            if ( matchSet.contains(match)){
                //System.out.println("Matchset already contains "+match);
                return;
            }
            //System.out.println("Adding match "+match);
            matchSet.add(match);
            //System.out.println(matchSet);
            while ( match.indexOf(".")==0 ){
                match = match.substring(1);
            }
            matches.add(match);
            resultsAdded();
        }
        
    }
    
    
    
    
    public void findClass(CompoundQuery query, Future results){
        for ( Query q : query.queries ){
            findClass(q, results);
        }
    }
    
    public void findClass(Query query, Future results){
        for ( FileObject root : query.getRoots() ){
            int numBefore = results.getMatches().size();
            findClass(root, results.getMatches(), query.simpleName, query.prefix);
            int numAfter = results.getMatches().size();
            if ( numBefore != numAfter ){
                results.resultsAdded();
            }
        }
    }
    
    private static  void findClass(FileObject root, List<String> matches, final String simpleName){
        findClass(root, matches, simpleName, "");
    }
    
    private static void findClass(FileObject root, List<String> matches, final String simpleName, final String prefix) {
        FileObject start = root;
        String[] prefixSegments = prefix.split("/");
        for ( String seg : prefixSegments ){
            if ( start.getFileObject(seg) != null ){
                start = start.getFileObject(seg);
            }
        }
        Object cmp = new Object(){

            @Override
            public boolean equals(Object obj) {
                if ( obj instanceof FileObject ){
                    FileObject fo = (FileObject)obj;
                    String name = fo.getName();
                    //if ( name.contains("JMenu")){
                    //    System.err.println("Name "+name+" simple name "+simpleName+" fo "+fo+" ext "+fo.getExt());
                    //}
                    return ("class".equals(fo.getExt()) && (simpleName.equals(name) || name.endsWith("$"+simpleName)));
                }
                return false;
            }
                
        };
        List<FileObject> foMatches = new ArrayList<FileObject>();
        find(start, foMatches, cmp);
        for ( FileObject fo : foMatches ){
            String fqn = getFQN(root, fo);
            matches.add(fqn);
            
        }
    }
    
    private static void findClass(ClassPath cp, List<String> matches, final String simpleName){
        for ( FileObject root : cp.getRoots()){
            findClass(root, matches, simpleName);
        }
    }
    
    private static void findClass(ClassPath[] classPaths, List<String> matches, String simpleName){
        for ( ClassPath cp : classPaths ){
            findClass(cp, matches, simpleName);
        }
    }
    
    
    private static String getFQN(FileObject root, FileObject fo){
        
        if ( !fo.getPath().startsWith(root.getPath())){
            throw new IllegalArgumentException("The root must be an ancestor of the file object in question.");
        } else {
            String path = fo.getPath();
            path = path.substring(root.getPath().length(), path.lastIndexOf("."));
            if ( path.indexOf(".")==0){
                path = path.substring(1);
            }
            return path.replace("/", ".").replace("$", ".");
        }
    }
    
    
    
    private static void find(FileObject root, List<FileObject> results, Object cmp){
        
        //System.err.println("File Root "+root);
        if ( cmp.equals(root)){
            results.add(root);
        }
        
        
        for ( FileObject child : root.getChildren()){
            //System.err.println("CHILD "+child+" of PARENT "+root);
            find(child, results, cmp);
        } 
    }
    
    
    

}
