/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.lexer;


import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import javax.swing.text.Document;
import javax.tools.Diagnostic;
import mirah.lang.ast.Call;
import mirah.lang.ast.Node;
import org.mirah.jvm.mirrors.debug.ConsoleDebugger;
import org.mirah.jvm.mirrors.debug.DebugController;
import org.mirah.jvm.mirrors.debug.DebuggerInterface;
import org.mirah.mmeta.SyntaxError;
import org.mirah.tool.MirahCompiler;
import org.mirah.tool.MirahTool;
import org.mirah.tool.Mirahc;
import org.mirah.typer.ResolvedType;
import org.mirah.typer.TypeFuture;
import org.mirah.typer.TypeListener;
import org.mirah.util.Context;
import org.mirah.util.SimpleDiagnostics;
import org.netbeans.api.java.classpath.ClassPath;
import static org.netbeans.api.java.classpath.ClassPath.COMPILE;
import org.netbeans.api.project.Project;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shannah
 */
public class MirahParser extends Parser {
    private static WeakHashMap<Document,DocumentDebugger> documentDebuggers = new WeakHashMap<Document,DocumentDebugger>();
    private Snapshot snapshot;
    private MirahParseDiagnostics diag;
    private static final Logger LOG = Logger.getLogger(MirahParser.class.getCanonicalName());
    
    public static DocumentDebugger getDocumentDebugger(Document doc){
        return documentDebuggers.get(doc);
    }
    
    @Override
    public void parse(Snapshot snapshot, Task task, SourceModificationEvent sme) throws ParseException {
         
         this.snapshot = snapshot;
         
        //mirahParser = new mirah.impl.MirahParser ();
        //mirahParser.parse(snapshot.getText().toString());
        diag = new MirahParseDiagnostics();
        Mirahc compiler = new Mirahc();
        
        //Project proj = snapshot.getSource().getFileObject().getLookup().lookup(Project.class);
        FileObject src = snapshot.getSource().getFileObject();
        LOG.warning("Source file is "+src);
        ClassPath compileClassPath = ClassPath.getClassPath(src, ClassPath.COMPILE);
        //LOG.warning("Project directory is "+proj.getProjectDirectory().getName());
        LOG.warning("Parsing classpath is "+compileClassPath.toString());
        
        ClassPath buildClassPath = ClassPath.getClassPath(src, ClassPath.EXECUTE);
        LOG.warning("Execute classapth is "+buildClassPath.toString());
        
        ClassPath srcClassPath = ClassPath.getClassPath(src, ClassPath.SOURCE);
        LOG.warning("Src classapth is "+srcClassPath.toString());
        
        compiler.setDestination(buildClassPath.toString());
        compiler.setDiagnostics(diag);
        
        List<String> paths = new ArrayList<String>();
        if ( !"".equals(srcClassPath.toString()) ){
            paths.add(srcClassPath.toString());
        }
        if ( !"".equals(buildClassPath.toString())){
            paths.add(buildClassPath.toString());
        }
        if ( !"".equals(compileClassPath.toString())){
            paths.add(compileClassPath.toString());
        }
        
        StringBuilder classPath = new StringBuilder();
        for ( String path : paths ){
            classPath.append(path);
            classPath.append(File.pathSeparator);
            
        }
        
        String cp = ".";
        if ( classPath.length() >= 1 ){
            cp = classPath.toString().substring(0, classPath.length()-1);
        }
        LOG.warning("The effective compile classpath is "+classPath.toString());
        compiler.setClasspath(cp);
        DocumentDebugger debugger = new DocumentDebugger();
        compiler.setDebugger(debugger);
        synchronized(documentDebuggers){
            documentDebuggers.put(sme.getModifiedSource().getDocument(false), debugger);
        }
        
        
        
        
        ClassPath bootClassPath = ClassPath.getClassPath(src, ClassPath.BOOT);
        if ( !"".equals(bootClassPath.toString())){
            compiler.setBootClasspath(bootClassPath.toString());
        }
        
        compiler.addFakeFile(src.getPath(), snapshot.getText().toString());
        
        compiler.compile(new String[0]);
        
       
        
        
        
        
        
        //try {
        //    javaParser.CompilationUnit ();
        //} catch (org.simplejava.jccparser.ParseException ex) {
        //    Logger.getLogger (SJParser.class.getName()).log (Level.WARNING, null, ex);
       // }
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

        NBMirahParserResult (Snapshot snapshot, MirahParseDiagnostics diagnostics) {
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
            if (super.errorCount() == 0 ){
                super.log(Diagnostic.Kind.ERROR,  "0", "ERROR_TO_PREVENT_COMPILING");
            }
            return super.errorCount();
        }

        @Override
        public void log(Diagnostic.Kind kind, String position, String message) {
            super.log(kind, position, message); //To change body of generated methods, choose Tools | Templates.
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
            
            
            
            
        }
        
        final private TreeSet<PositionType> leftEdges;
        final private TreeSet<PositionType> rightEdges;
        
        public DocumentDebugger(){
            leftEdges = new TreeSet<PositionType>(new Comparator<PositionType>(){

                @Override
                public int compare(PositionType o1, PositionType o2) {
                    if ( o1.startPos < o2.startPos ) return -1;
                    else if (o2.startPos < o1.startPos ) return 1;
                    else return 0;
                }
                
            });
            rightEdges = new TreeSet<PositionType>(new Comparator<PositionType>(){

                @Override
                public int compare(PositionType o1, PositionType o2) {
                    if ( o1.endPos < o2.endPos ) return -1;
                    else if ( o2.endPos < o1.endPos ) return 1;
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
            return rightEdges.floor(t);
        }
        
        @Override
        public void parsedNode(Node node) {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void enterNode(Context cntxt, Node node, boolean bln) {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            
        }

        @Override
        public void exitNode(Context cntxt, final Node node, TypeFuture tf) {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            LOG.warning("Position of node "+node+" is "+node.position());
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
                    leftEdges.add(t);
                    rightEdges.add(t);
                }
                
            });
        }

        @Override
        public void inferenceError(Context cntxt, Node node, TypeFuture tf) {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    
    
}
