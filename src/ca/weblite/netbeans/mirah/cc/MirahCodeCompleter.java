/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.cc;

import ca.weblite.netbeans.mirah.lexer.MirahLexer;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahParser.DocumentDebugger;
import ca.weblite.netbeans.mirah.lexer.MirahParser.DocumentDebugger.PositionType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import mirah.lang.ast.Call;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Next;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeFilter;
import mirah.lang.ast.NodeList;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.SimpleNodeVisitor;
import mirah.lang.ast.SimpleString;
import mirah.lang.ast.TypeRef;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author shannah
 */
@MimeRegistration(mimeType="text/x-mirah", service=CompletionProvider.class)
public class MirahCodeCompleter implements CompletionProvider {
    private static final Logger LOG = Logger.getLogger(MirahCodeCompleter.class.getCanonicalName());
    @Override
    public CompletionTask createTask(int queryType, final JTextComponent jtc) {
        if ( queryType != CompletionProvider.COMPLETION_QUERY_TYPE){
            return null;
        }
        
        try {
            int caretOffset = jtc.getCaretPosition();
        
            int p = caretOffset-1;
            if ( p < 0 ){
                return null;
            }
            String lastChar = jtc.getDocument().getText(p, 1);
            while ( p > 0 && lastChar.trim().isEmpty()){
                p--;
                lastChar = jtc.getDocument().getText(p, 1);
            }
            if ( !".".equals(lastChar) ){
                return null;
            }
        } catch ( BadLocationException ble){
            return null;
        }
        
        return new AsyncCompletionTask(new AsyncCompletionQuery(){
            
            int tries = 0;
            Object lock = new Object();
            @Override
            protected void query(final CompletionResultSet crs, final Document doc, final int caretOffset) {
                if ( crs.isFinished() || this.isTaskCancelled()){
                    return;
                }
                tries++;
                DocumentDebugger dbg = MirahParser.getDocumentDebugger(doc);
                
                
                if ( dbg != null ){
                    try {
                        int p = caretOffset-1;
                        if ( p < 0 ){
                            return;
                        }
                        String lastChar = doc.getText(p, 1);
                        while ( p > 0 && lastChar.trim().isEmpty()){
                            p--;
                            lastChar = doc.getText(p, 1);
                        }
                        if ( ".".equals(lastChar) ){
                            // Find right edge of last node before '.'
                            int rightEdge = p-1;
                            String tmp = doc.getText(rightEdge, 1);
                            while ( rightEdge > 0 && tmp.trim().isEmpty()){
                                rightEdge--;
                                tmp = doc.getText(rightEdge, 1);
                            }
                            rightEdge++;
                            LOG.warning("Position is "+rightEdge);
                            LOG.warning("Num nodes "+dbg.countNodes());
                            if ( dbg.countNodes() > 0 ){
                                for( Object node : dbg.compiler.compiler().getParsedNodes() ){
                                    if ( node instanceof Node ){
                                        LOG.warning(nodeToString((Node)node));
                                        ((Node)node).accept(new NodeScanner(){

                                            @Override
                                            public Object visitNext(Next node, Object arg) {
                                                LOG.warning(nodeToString(node));
                                                if ( node != null ){
                                                    Node orig = node.originalNode();
                                                    if ( orig != null && orig instanceof SimpleString ){
                                                        SimpleString ss = (SimpleString)orig;
                                                        TypeRef tr = ss.typeref();
                                                        if ( tr != null ){
                                                            LOG.warning("Type ref is "+tr.name());
                                                        }
                                                    }
                                                }
                                                return super.visitNext(node, arg); //To change body of generated methods, choose Tools | Templates.
                                            }

                                            @Override
                                            public Object visitSimpleString(SimpleString node, Object arg) {
                                                if ( node != null ){
                                                    TypeRef tr = node.typeref();
                                                    if ( tr != null ){
                                                        LOG.warning("SS Type ref is "+tr.name());
                                                    }
                                                    
                                                }
                                                return super.visitSimpleString(node, arg); //To change body of generated methods, choose Tools | Templates.
                                            }

                                            @Override
                                            public Object visitCall(Call node, Object arg) {
                                                if ( node != null ){
                                                    TypeRef tr = node.typeref();
                                                    if ( tr != null ){
                                                        LOG.warning("Call Type ref is "+tr.name());
                                                    }
                                                    
                                                }
                                                return super.visitCall(node, arg); //To change body of generated methods, choose Tools | Templates.
                                            }

                                            
                                            
                                            @Override
                                            public boolean enterDefault(Node node, Object arg) {
                                                if ( node != null ){
                                                    LOG.warning(nodeToString(node));
                                                }
                                                return super.enterDefault(node, arg); //To change body of generated methods, choose Tools | Templates.
                                            }

                                            
                                            
                                            
                                            
                                            
                                            
                                            
                                            
                                        }, null);
                                        //walkTree((Node)node);
                                        
                                    }
                                };
                            
                            }
                            SortedSet<PositionType> positions = dbg.findPositionsWithRightEdgeInRange(rightEdge, rightEdge);
                            if ( positions.isEmpty() && tries < 3 ){
                                LOG.warning("Trying code completion for "+tries+" time");
                                MirahParser.addCallback(doc, new Runnable(){
                                    public void run(){
                                        LOG.warning("Running in addCallback "+tries);
                                        query(crs, doc, caretOffset);
                                        synchronized(lock){
                                            lock.notifyAll();
                                        }
                                    }
                                });
                                synchronized (lock){
                                    try {
                                        lock.wait(1000);
                                        
                                    } catch (InterruptedException ex) {
                                        Exceptions.printStackTrace(ex);
                                    }
                                }
                                if ( crs.isFinished() ){
                                    return;
                                }
                            }
                            for ( PositionType pt : positions ){
                                FileObject fileObject = NbEditorUtilities.getFileObject(doc);
                                Class cls = findClass(fileObject, pt.type.name());
                                if ( cls != null ){
                                    for ( Method m : cls.getMethods()){
                                        crs.addItem(new MirahMethodCompletionItem(m, caretOffset));
                                    }
                                }
                            }
                        }
                    } catch ( BadLocationException ble ){
                        ble.printStackTrace();
                    }
                }
                crs.finish();
                
            }

        }, jtc);
        

        
            
        
        
        
    }

    @Override
    public int getAutoQueryTypes(JTextComponent jtc, String string) {
        return 0;
    }
    
    private Class findClass(FileObject o, String name){
        ClassPath[] paths = new ClassPath[]{
            ClassPath.getClassPath(o, ClassPath.SOURCE),
            ClassPath.getClassPath(o, ClassPath.EXECUTE),
            ClassPath.getClassPath(o, ClassPath.COMPILE),
            ClassPath.getClassPath(o, ClassPath.BOOT)
        };
        
        for ( int i=0; i<paths.length; i++){
            ClassPath cp = paths[i];
            try {
                Class c = cp.getClassLoader(true).loadClass(name);
                if ( c != null ){
                    return c;
                }
            } catch ( ClassNotFoundException ex){
                
            }
        }
        return null;
    }
    
    private static void walkTree(Node node){
        LOG.warning(nodeToString(node));
        NodeFilter f = new NodeFilter(){

            @Override
            public boolean matchesNode(Node node) {
                
                return true;
            }
            
        };
        
        LOG.warning("About to find children");
        //List out = new ArrayList();
        List nodes = node.findChildren(f);
        LOG.warning(nodes.size()+" children found");
        for ( Object o : nodes ){
            if ( o instanceof Node ){
                walkTree((Node)o);
            }
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
