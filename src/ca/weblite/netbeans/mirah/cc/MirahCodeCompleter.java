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
import java.awt.EventQueue;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
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
import mirah.lang.ast.Position;
import mirah.lang.ast.SimpleNodeVisitor;
import mirah.lang.ast.SimpleString;
import mirah.lang.ast.TypeRef;
import org.mirah.tool.MirahCompiler;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.editor.completion.Completion;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.classpath.ClassPath;

import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
//import org.netbeans.modules.parsing.impl.Utilities;

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
    
    private Node findNode(final DocumentDebugger dbg, final int rightEdge){
        final Node[] foundNode = new Node[1];
        for( Object node : dbg.compiler.compiler().getParsedNodes() ){
            if ( node instanceof Node ){
                //LOG.warning(nodeToString((Node)node));
                ((Node)node).accept(new NodeScanner(){

                    @Override
                    public boolean enterDefault(Node node, Object arg) {
                        if ( node != null ){
                            
                            Position nodePos = node.position();
                            ResolvedType type = dbg.getType(node);
                            if ( type != null && nodePos != null && nodePos.endChar() == rightEdge ){
                                LOG.warning("Visibing simple string "+nodeToString(node)+" with type "+type);
                                LOG.warning("Set as found node as it is currently nearest");
                                foundNode[0] = node;
                            } else if ( nodePos != null && nodePos.endChar() == rightEdge ){
                                LOG.warning("Found node but no type info yet "+nodeToString(node));
                            } else {
                                //LOG.warning("Not set as found node because it wasn't nearest or node was null");
                            }
                        } 
                        return super.enterDefault(node, arg); //To change body of generated methods, choose Tools | Templates.
                    }

                    
                    
                    @Override
                    public Object exitDefault(Node node, Object arg) {
                        
                        return super.exitDefault(node, arg); //To change body of generated methods, choose Tools | Templates.
                    }



                }, null);
                //walkTree((Node)node);

            }
        };
        return foundNode[0];
    }
    
    @Override
    public CompletionTask createTask(int queryType, final JTextComponent jtc) {
        if ( queryType != CompletionProvider.COMPLETION_QUERY_TYPE){
            return null;
        }
        final int initialOffset = jtc.getCaretPosition();
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
            boolean parsed = false;
            int tries = 0;
            Object lock = new Object();

            /*
            @Override
            protected boolean canFilter(JTextComponent component) {
                int currentPos = component.getCaretPosition();
                if ( currentPos == initialOffset){
                    return true;
                }
                
                try {
                    char c = component.getText(currentPos, 1).charAt(0);
                    switch ( c){
                        case '.':
                        case ':':
                        case '(':
                        case ';':
                        
                            return false;
                    }
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }
                return true;
            }
            */
            
            
           
            
            
            @Override
            protected void query(final CompletionResultSet crs, final Document doc, final int caretOffset) {
                if ( crs.isFinished() || this.isTaskCancelled()){
                    return;
                }
                
                if ( caretOffset < initialOffset ){
                    crs.finish();
                    Completion.get().hideAll();
                }
                
                
                tries++;
                DocumentDebugger dbg = MirahParser.getDocumentDebugger(doc);
                //LOG.warning("Debugger is "+dbg);
                
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
                            LOG.warning("In query");
                            LOG.warning("Thread id "+Thread.currentThread().getId());
                            // Find right edge of last node before '.'
                            int rightEdge = p-1;
                            String tmp = doc.getText(rightEdge, 1);
                            while ( rightEdge > 0 && tmp.trim().isEmpty()){
                                rightEdge--;
                                tmp = doc.getText(rightEdge, 1);
                            }
                            rightEdge++;
                            final int rightEdgeFinal = rightEdge;
                            //LOG.warning("Position is "+rightEdge);
                            Node foundNode = findNode(dbg, rightEdge);
                            if ( foundNode != null ){
                                LOG.warning("Found node "+nodeToString(foundNode)+" and type is "+dbg.getType(foundNode));
                            }
                            int i = 0;
                            
                            ResolvedType type = null;
                            if ( foundNode != null ){
                                type = dbg.getType(foundNode);
                            }
                            if ( (foundNode == null || type == null) && tries++ < 10){
                                Source src = Source.create(doc);
                                
                                LOG.warning(src.createSnapshot().getText().toString());
                                LOG.warning("Not found so we're going to reparse the document");
                                try {
                                    Snapshot snapshot = src.createSnapshot();
                                    
                                    MirahParser parser = new MirahParser();
                                    parser.reparse(snapshot, p, 1, "");
                                } catch (ParseException ex) {
                                    LOG.warning("There was a parse exception "+ex.getMessage());
                                    Exceptions.printStackTrace(ex);
                                }
                               
                                
                                
                                dbg = MirahParser.getDocumentDebugger(doc);
                                //printNodes(dbg.compiler.compiler(), rightEdgeFinal);
                                foundNode = findNode(dbg, rightEdge);
                                if ( foundNode != null ){
                                    type = dbg.getType(foundNode);
                                }
                                LOG.warning("Node is "+nodeToString(foundNode)+" after attempt "+i+" type "+type);
                                
                                
                            }
                            
                            
                            
                            
                            if ( foundNode != null ){
                                
                                type = dbg.getType(foundNode);
                                
                                if ( type != null ){
                                    //LOG.warning("Node was found "+nodeToString(foundNode));
                                    FileObject fileObject = NbEditorUtilities.getFileObject(doc);
                                    Class cls = findClass(fileObject, dbg.getType(foundNode).name());
                                    if ( cls != null ){
                                        for ( Method m : cls.getMethods()){
                                            crs.addItem(new MirahMethodCompletionItem(m, caretOffset));
                                        }
                                    }
                                }
                            } else {
                                //LOG.warning("Node was not found ");
                            }
                        }
                    } catch ( BadLocationException ble ){
                        ble.printStackTrace();
                    }
                }
                if ( !crs.isFinished() ){
                    crs.finish();
                }
                
                
                
            }

            private void printNodes(MirahCompiler compiler, int rightEdgeFinal) {
                for ( Object o : compiler.getParsedNodes()){
                    if ( o instanceof Node && o != null ){
                        Node node = (Node)o;
                        
                        node.accept(new NodeScanner(){

                            @Override
                            public boolean enterDefault(Node node, Object arg) {
                                //LOG.warning("PRINTNODES: "+nodeToString(node));
                                return super.enterDefault(node, arg); //To change body of generated methods, choose Tools | Templates.
                            }
                            
                        }, null);
                        
                    }
                }
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
