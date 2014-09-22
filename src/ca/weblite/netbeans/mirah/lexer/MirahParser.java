/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.lexer;


import ca.weblite.asm.WLMirahCompiler;
import ca.weblite.netbeans.mirah.RecompileQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.event.ChangeListener;
import javax.swing.text.Document;
import javax.tools.Diagnostic;
import mirah.lang.ast.Node;
import mirah.lang.ast.Super;
import org.mirah.jvm.mirrors.debug.DebuggerInterface;
import org.mirah.tool.Mirahc;
import org.mirah.typer.ResolvedType;
import org.mirah.typer.TypeFuture;
import org.mirah.typer.TypeListener;
import org.mirah.util.Context;
import org.mirah.util.SimpleDiagnostics;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author shannah
 */
public class MirahParser extends Parser {
    private static WeakHashMap<Document,DocumentDebugger> documentDebuggers = 
            new WeakHashMap<Document,DocumentDebugger>();
    private static WeakHashMap<Document,String> lastContent = 
            new WeakHashMap<Document,String>();
    private Snapshot snapshot;
    private MirahParseDiagnostics diag;
    private static final Logger LOG = 
            Logger.getLogger(MirahParser.class.getCanonicalName());
    private static int count = 0;
    
    public static DocumentDebugger getDocumentDebugger(Document doc){
        return documentDebuggers.get(doc);
    }
    
    private static WeakHashMap<Document, Queue<Runnable>> parserCallbacks = 
            new WeakHashMap<Document,Queue<Runnable>>();
    
    
    
    
    public static void addCallback(Document d, Runnable r){
        synchronized (parserCallbacks ){
            if ( !parserCallbacks.containsKey(d) ){
                parserCallbacks.put(d, new LinkedList<Runnable>());
            }
            parserCallbacks.get(d).add(r);
        }
    }
    
    private static void fireOnParse(Document d){
        List<Runnable> tasks = new ArrayList<Runnable>();
        synchronized (parserCallbacks ){
            Queue<Runnable> queue = parserCallbacks.get(d);
            if ( queue != null ){
                tasks.addAll(queue);
            }
            parserCallbacks.remove(d);
        }
        
        for ( Runnable r : tasks ){
            r.run();
        }
    }
    
    String lastSource = "";
    
     @Override
    public void parse(Snapshot snapshot, Task task, SourceModificationEvent sme) 
            throws ParseException {
        
        String oldContent = lastContent.get(
                snapshot.getSource().getDocument(false)
        );
        
        String newContent = snapshot.getText().toString();
        
        boolean changed = oldContent == null || !oldContent.equals(newContent);
        
        if ( sme.sourceChanged() && changed){
            lastContent.put(
                    snapshot.getSource().getDocument(false), 
                    newContent
            );
            reparse(snapshot);
        }
        
    }
    
    
    
    public void reparse(Snapshot snapshot ) throws ParseException {
        reparse(snapshot, snapshot.getText().toString());
    }
    
    
    
    
    private void copyIfChanged(File sourceRoot, File destRoot, File sourceFile) throws IOException {
        if ( sourceFile.getName().endsWith(".class")){
            String relativePath = sourceFile.getPath().substring(sourceRoot.getPath().length());
            if ( relativePath.indexOf(File.separator) == 0 ){
                relativePath = relativePath.substring(File.separator.length());
            }
            File destFile = new File(destRoot, relativePath);
            if ( destFile.exists() && destFile.lastModified() < sourceFile.lastModified()){
                FileInputStream fis = null;
                FileOutputStream fos = null;
                try {
                    fis = new FileInputStream(sourceFile);
                    fos = new FileOutputStream(destFile);
                    FileUtil.copy(fis, fos);
                } finally {
                    if ( fis != null ){
                        try {
                            fis.close();
                        } catch ( Exception ex){}
                    }
                    if ( fos != null ){
                        try {
                            fos.close();
                            
                        } catch ( Exception ex){}
                    }
                }
            }
        } else if ( sourceFile.isDirectory()){
            for ( File child : sourceFile.listFiles()){
                copyIfChanged(sourceRoot, destRoot, child);
            }
        }
    }
    
    public void reparse(Snapshot snapshot, String content) 
            throws ParseException {
        
        
        //(new RuntimeException()).printStackTrace();
         this.snapshot = snapshot;
        diag = new MirahParseDiagnostics();
        WLMirahCompiler compiler = new WLMirahCompiler();
        
        FileObject src = snapshot.getSource().getFileObject();
        
        Project project = FileOwnerQuery.getOwner(src);
        
        FileObject projectDirectory = project.getProjectDirectory();
        FileObject buildDir = projectDirectory.getFileObject("build");
        
        ClassPath compileClassPath = 
                ClassPath.getClassPath(src, ClassPath.COMPILE);
        String compileClassPathStr = "";
        if ( compileClassPath != null ){
            compileClassPathStr = compileClassPath.toString();
        }
        ClassPath buildClassPath = 
                ClassPath.getClassPath(src, ClassPath.EXECUTE);
        String buildClassPathStr = "";
        if (buildClassPath != null ){
            buildClassPathStr = buildClassPath.toString();
        }
        
        ClassPath srcClassPath = ClassPath.getClassPath(src, ClassPath.SOURCE);
        String srcClassPathStr = "";
        
        if ( srcClassPath != null ) srcClassPathStr = srcClassPath.toString();
        String changedSourcePaths = RecompileQueue.getProjectQueue(project).getAndClearChangedSourcePaths();
        if ( changedSourcePaths != null ){
            Set<String> set = new HashSet<String>();
            set.addAll(Arrays.asList(changedSourcePaths.split(Pattern.quote(File.pathSeparator))));
            set.addAll(Arrays.asList(srcClassPathStr.split(Pattern.quote(File.pathSeparator))));
            StringBuilder sb = new StringBuilder();
            for ( String p : set ){
                sb.append(p).append(File.pathSeparator);
            }
            srcClassPathStr = sb.substring(0, sb.length()-File.pathSeparator.length());
        }
        compiler.setSourcePath(srcClassPathStr);
        
        String dest = buildClassPathStr;
        FileObject mirahDir = null;
        try {
            if ( buildDir == null ){
                buildDir = projectDirectory.createFolder("build");
            }
            mirahDir = buildDir.getFileObject("mirah");
            if (mirahDir == null ){
                mirahDir = buildDir.createFolder("mirah");
            }
            File javaStubDir = new File(buildDir.getPath(), "mirah_tmp"+File.separator+"java_stub_dir");
            javaStubDir.mkdirs();
            compiler.setJavaStubDirectory(javaStubDir);
            dest = mirahDir.getPath();
        } catch (IOException ex){
            
        }
        compiler.setDestinationDirectory(new File(dest));
        compiler.setDiagnostics(diag);
        
        List<String> paths = new ArrayList<String>();
        if ( !"".equals(srcClassPathStr) ){
            paths.add(srcClassPathStr);
        }
        if ( !"".equals(buildClassPathStr)){
            paths.add(buildClassPathStr);
        }
        if ( !"".equals(compileClassPathStr)){
            paths.add(compileClassPathStr);
        }
        
        StringBuilder classPath = new StringBuilder();
        for ( String path : paths ){
            classPath.append(path);
            classPath.append(File.pathSeparator);
            
        }
        
        
        String macroPath = new StringBuilder()
                .append(classPath.toString())
                .append(buildDir.getPath())
                .append(File.separator)
                .append("mirah_tmp") 
                .append(File.separator)
                .append("macros")
                .append(File.separator)
                .append("classes")
                .append(File.pathSeparator)
                .toString()
                ;
        
        String cp = ".";
        if ( classPath.length() >= 1 ){
            cp = classPath.toString().substring(0, classPath.length()-1);
        }
        compiler.setClassPath(macroPath+File.pathSeparator+cp);
        
        compiler.setMacroClassPath(
                macroPath.substring(0, macroPath.length()-1)
        );
        DocumentDebugger debugger = new DocumentDebugger();
        
        compiler.setDebugger(debugger);
        
        
        
        ClassPath bootClassPath = ClassPath.getClassPath(src, ClassPath.BOOT);
        String  bootClassPathStr = "";
        if ( bootClassPath != null ) {
            bootClassPathStr = bootClassPath.toString();
        }
        if ( !"".equals(bootClassPathStr)){
            compiler.setBootClassPath(bootClassPathStr);
        }
        String srcText = content;
        FileObject fakeFileRoot = getRoot(src);
        String relPath = FileUtil.getRelativePath(fakeFileRoot, src);
        relPath = relPath.substring(0, relPath.lastIndexOf("."));
        compiler.addFakeFile(relPath, srcText);
        FileChangeAdapter fileChangeListener = null;
        try {
            
        
            
            compiler.compile(new String[0]);
            if ( mirahDir != null ){
                for (FileObject compileRoot : compileClassPath.getRoots()){
                    if ( !compileRoot.getPath().endsWith(".jar") && compileRoot.isFolder() && !mirahDir.equals(compileRoot)){
                        copyIfChanged(new File(mirahDir.getPath()), new File(compileRoot.getPath()), new File(mirahDir.getPath()));
                    }
                }
            }
            
            
        } catch ( Exception ex){
            ex.printStackTrace();
        } 
        
        synchronized(documentDebuggers){
            
            Document doc = snapshot.getSource().getDocument(true);
            
            if ( debugger.resolvedTypes.size() > 0 ){
                debugger.compiler = compiler.getMirahc();
                
                documentDebuggers.put(doc, debugger);
                fireOnParse(doc);
            } 
        }
        
        
       
        
        
    }
    

     private FileObject getRoot(FileObject file){
        Project project = FileOwnerQuery.getOwner(file);
        Sources sources = ProjectUtils.getSources(project);
        for (SourceGroup sourceGroup : sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA)) {
            FileObject root = sourceGroup.getRootFolder();
            if ( FileUtil.isParentOf(root, file) || root.equals(file)){
                return root;
            }
        }
        return null;
    }

    @Override
    public Result getResult (Task task) {
        return new NBMirahParserResult (snapshot, diag);
    }

    @Override
    public void cancel () {
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
        
    }

    
    

    @Override
    public void removeChangeListener (ChangeListener changeListener) {
    }

    
    public static class NBMirahParserResult extends Result {

        private MirahParseDiagnostics diagnostics;
        private boolean valid = true;

        NBMirahParserResult (
                Snapshot snapshot, 
                MirahParseDiagnostics diagnostics) {
            super (snapshot);
            this.diagnostics = diagnostics;
        }

        public MirahParseDiagnostics getDiagnostics(){
            return diagnostics;
        }

        @Override
        protected void invalidate () {
            valid = false;
        }
    }
    
    public static class MirahParseDiagnostics extends SimpleDiagnostics {
        
        static class SyntaxError {
            SyntaxError(Diagnostic.Kind k, String pos, String msg){
                kind = k;
                position = pos;
                message = msg;
            }
            Diagnostic.Kind kind;
            String position;
            String message;
        }
        
        
        private List<SyntaxError> errors = new ArrayList<SyntaxError>();
        
        public MirahParseDiagnostics(){
            super(false);
        }
        
        @Override
        public int errorCount() {
            return super.errorCount();
        }

        @Override
        public void log(Diagnostic.Kind kind, String position, String message) {
            
            super.log(kind, position, message); 
            if ( !"ERROR_TO_PREVENT_COMPILING".equals(message)){
                errors.add(new SyntaxError(kind, position, message));
            }
        }
        
        public List<SyntaxError> getErrors(){
            return errors;
        }
    }
    
    public static class DocumentDebugger implements DebuggerInterface {
        public static class PositionType {
            public  int startPos, endPos;
            public  ResolvedType type;
            public Node node;
            
            
            public String toString(){
                if ( type == null ){
                    return "["+startPos+","+endPos+"]";
                } else {
                    return "["+type.name() + " "+startPos+","+endPos+"]";
                }
            }
            
            
        }
        
        final private TreeSet<PositionType> leftEdges;
        final private TreeSet<PositionType> rightEdges;
        final private HashMap<Node,ResolvedType> resolvedTypes = 
                new HashMap<Node,ResolvedType>();
        
        public Mirahc compiler;
        
        public DocumentDebugger(){
            
            leftEdges = new TreeSet<PositionType>(
                    new Comparator<PositionType>(){

                @Override
                public int compare(PositionType o1, PositionType o2) {
                    if ( o1.startPos < o2.startPos ) return -1;
                    else if (o2.startPos < o1.startPos ) return 1;
                    else if ( o1.endPos < o2.endPos ) return -1;
                    else if ( o2.endPos < o1.endPos ) return 1;
                    else return 0;
                }
                
            });
            rightEdges = new TreeSet<PositionType>(
                    new Comparator<PositionType>(){

                @Override
                public int compare(PositionType o1, PositionType o2) {
                    if ( o1.endPos < o2.endPos ) return -1;
                    else if ( o2.endPos < o1.endPos ) return 1;
                    else if ( o1.startPos < o2.startPos ) return -1;
                    else if ( o2.startPos < o1.startPos ) return 1;
                    else return 0;
                }
                
            });
        }
        

        public PositionType findNearestPositionOccurringAfter(int pos){
            PositionType t = new PositionType();
            t.startPos = pos;
            return leftEdges.ceiling(t);
        }
        
        public PositionType findNearestPositionOccuringBefore(int pos){
            PositionType t = new PositionType();
            t.endPos = pos;
            return rightEdges.lower(t);
        }
        
        public ResolvedType getType(Node node){
            return resolvedTypes.get(node);
        }
        
        public SortedSet<PositionType> findPositionsWithRightEdgeInRange(
                int start, 
                int end ){
            PositionType p1 = new PositionType();
            p1.endPos = start;
            p1.startPos = 0;
            
            PositionType p2 = new PositionType();
            p2.endPos = end;
            p2.startPos = end;
            
            SortedSet<PositionType> o1 = rightEdges.subSet(p1, p2);
            return o1;
        }
        
        public int countNodes(){
            return rightEdges.size();
        }
        
        public Node firstNode(){
            return leftEdges.first().node;
        }
        
        
        @Override
        public void parsedNode(Node node) {
            
        }

        @Override
        public void enterNode(Context cntxt, Node node, boolean bln) {
            
            
            
        }

        @Override
        public void exitNode(Context cntxt, final Node node, TypeFuture tf) {
            tf.onUpdate(new TypeListener(){

                @Override
                public void updated(TypeFuture tf, ResolvedType rt) {
                    if ( !tf.isResolved() ){
                        return;
                    }
                    if ( node.position() == null ){
                        return;
                    }
                    PositionType t = new PositionType();
                    t.startPos = node.position().startChar();
                    t.endPos = node.position().endChar();
                    t.type = rt;
                    t.node = node;
                    if ( leftEdges.contains(t)){
                        leftEdges.remove(t);
                    }
                    if ( rightEdges.contains(t)){
                        rightEdges.remove(t);
                    }
                    leftEdges.add(t);
                    rightEdges.add(t);
                    
                    resolvedTypes.put(node, rt);
                    
                    
                    
                }
                
            });
        }
        
        
        
        
        @Override
        public void inferenceError(Context cntxt, Node node, TypeFuture tf) {
            
        }
        
    }
    
    private static String nodeToString(Node n){
        if ( n == null || n.position() == null ){
            if ( n != null ){
                return ""+n;
            }
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Node ").append(n)
                .append(n.position().startLine())
                .append(".")
                .append(n.position().startColumn())
                .append(":")
                .append(n.position().startChar())
                .append("-")
                .append(n.position().endLine())
                .append(".")
                .append(n.position().endColumn())
                .append(":")
                .append(n.position().endChar())
                .append(" # ");
        
       return sb.toString();
                
    }
    
    
}
