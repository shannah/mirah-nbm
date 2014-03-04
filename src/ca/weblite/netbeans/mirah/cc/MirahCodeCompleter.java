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
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.awt.EventQueue;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import mirah.impl.Tokens;
import mirah.lang.ast.Call;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.Constant;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Next;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeFilter;
import mirah.lang.ast.NodeList;
import mirah.lang.ast.NodeScanner;
import mirah.lang.ast.Position;
import mirah.lang.ast.SimpleNodeVisitor;
import mirah.lang.ast.SimpleString;
import mirah.lang.ast.TypeName;
import mirah.lang.ast.TypeRef;
import org.mirah.tool.MirahCompiler;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.editor.completion.Completion;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;

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
                ((Node)node).accept(new NodeScanner(){

                    @Override
                    public boolean enterDefault(Node node, Object arg) {
                        if ( node != null ){
                            
                            Position nodePos = node.position();
                            ResolvedType type = dbg.getType(node);
                            if ( type != null && nodePos != null && nodePos.endChar() == rightEdge ){
                                foundNode[0] = node;
                            } else if ( nodePos != null && nodePos.endChar() == rightEdge ){
                                
                            } else {
                                
                            }
                        } 
                        return super.enterDefault(node, arg); //To change body of generated methods, choose Tools | Templates.
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
            String filter = null;
            Class currentType = null;
            boolean isStatic;

            
            @Override
            protected boolean canFilter(JTextComponent component) {
                if ( currentType == null ){
                    return false;
                }
                int currentPos = component.getCaretPosition();
                if ( currentPos <= initialOffset){
                    return false;
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
                try {
                    filter = component.getText(initialOffset, currentPos-initialOffset).trim();
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }
                return filter != null;
            }

            @Override
            protected void filter(CompletionResultSet resultSet) {
                for ( Method m : currentType.getMethods()){
                    if ( m.getName().toLowerCase().indexOf(filter.toLowerCase()) == 0 && isStatic == Modifier.isStatic(m.getModifiers()) ){
                        resultSet.addItem(new MirahMethodCompletionItem(m, initialOffset, filter.length()));
                    }
                    
                }
                resultSet.finish();
                
            }
            
            
            
            
            
           
            
            
            @Override
            protected void query(final CompletionResultSet crs, final Document doc, final int caretOffset) {
                BaseDocument bdoc = (BaseDocument)doc;
                if ( crs.isFinished() || this.isTaskCancelled()){
                    return;
                }
                
                if ( caretOffset < initialOffset ){
                    crs.finish();
                    Completion.get().hideAll();
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
                        
                        TokenSequence<MirahTokenId> toks = mirahTokenSequence(doc, caretOffset, true);
                        
                        MirahTokenId tDot = MirahTokenId.get(Tokens.tDot.ordinal());
                        MirahTokenId tId = MirahTokenId.get(Tokens.tIDENTIFIER.ordinal());
                        MirahTokenId tNL = MirahTokenId.get(Tokens.tNL.ordinal());
                        MirahTokenId tWS = MirahTokenId.WHITESPACE;
                        
                        // Will hold the "dot" token that we want to do code
                        // completion for
                        Token<MirahTokenId> dotToken = null;
                        
                        // Will hold the subject token (i.e. the last token before 
                        // the dot that we will be checking for a type
                        Token<MirahTokenId> subjectToken = null;
                        Token<MirahTokenId> thisTok = toks.token();
                        if ( thisTok != null ){
                            MirahTokenId thisTokType = thisTok.id();
                            
                            Token<MirahTokenId> prevTok = null;
                            MirahTokenId prevTokType = null;
                            if ( toks.movePrevious() ){
                                prevTok = toks.token();
                                prevTokType = prevTok.id();
                                toks.moveNext();
                            }
                            
                            if ( tId.equals(thisTokType) && tDot.equals(prevTokType) ){
                                
                                filter = toks.token().text().toString();
                                dotToken = prevTok;
                                toks.movePrevious();
                                
                            } else if (tDot.equals(thisTokType)){
                                filter = "";
                                dotToken = thisTok;
                                
                            }
                            
                        }
                        
                        if ( dotToken == null ){
                            crs.finish();
                            Completion.get().hideAll();
                            return;
                        }
                        
                        // Now to find the subject token.
                        while ( toks.movePrevious() ){
                            Token<MirahTokenId> tok = toks.token();
                            if ( tWS.equals(tok.id())||tNL.equals(tok.id())){
                                // 
                            } else {
                                subjectToken = tok;
                                break;
                            }
                        }
                        
                        
                        
                        if ( dotToken == null || subjectToken == null ){
                            crs.finish();
                            Completion.get().hideAll();
                            return;
                        }
                        
                        bdoc.readLock();
                        TokenHierarchy<?> hi = TokenHierarchy.get(doc);
                        bdoc.readUnlock();
                        
                        int bol = getBeginningOfLine(doc, caretOffset);
                        int eol = getEndOfLine(doc, caretOffset);
                        int dotPos = dotToken.offset(hi);
                       
                        
                        
                        Node foundNode = findNode(dbg, subjectToken.offset(hi)+subjectToken.length());
                        
                        ResolvedType type = null;
                        if ( foundNode != null ){
                            type = dbg.getType(foundNode);
                            
                            Node c = foundNode;
                            while ( c != null ){
                                
                                c = c.parent();
                            }
                            
                        }
                        
                        if ( foundNode == null || type == null ){
                            Source src = Source.create(doc);
                            MirahParser parser = new MirahParser();
                            try {
                                Snapshot snapshot = src.createSnapshot();
                                String text = snapshot.getText().toString();
                                StringBuilder sb = new StringBuilder();
                                sb.append(text.substring(0, dotPos));
                                for ( int i=dotPos; i<eol; i++){
                                    sb.append(' ');
                                }
                                sb.append(text.substring(eol));
                                
                                parser.reparse(snapshot, sb.toString());
                                
                            } catch (ParseException ex){
                                Exceptions.printStackTrace(ex);
                            }
                            
                            dbg = MirahParser.getDocumentDebugger(doc);
                            //printNodes(dbg.compiler.compiler(), rightEdgeFinal);
                            foundNode = findNode(dbg, subjectToken.offset(hi)+subjectToken.length());
                            if ( foundNode != null ){
                                type = dbg.getType(foundNode);
                            }
                            
                        }
                        
                        if ( foundNode != null ){
                                
                            type = dbg.getType(foundNode);

                            if ( type != null ){
                                
                                
                                FileObject fileObject = NbEditorUtilities.getFileObject(doc);
                                Class cls = findClass(fileObject, dbg.getType(foundNode).name());
                                currentType = cls;
                                
                                isStatic = foundNode instanceof Constant;
                                if ( cls != null ){
                                    
                                    if ( isStatic && filter == null || "new".startsWith(filter)){
                                        for ( Constructor c : cls.getConstructors()){
                                            crs.addItem(new MirahConstructorCompletionItem(c, caretOffset, filter.length()));
                                        }
                                    }
                                    for ( Method m : cls.getMethods()){
                                        if ( m.getName().startsWith(filter) && isStatic == Modifier.isStatic(m.getModifiers())){
                                            crs.addItem(new MirahMethodCompletionItem(m, caretOffset, filter.length()));
                                        }
                                    }
                                }
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
            ClassPath.getClassPath(o, ClassPath.BOOT),
            
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
        NodeFilter f = new NodeFilter(){

            @Override
            public boolean matchesNode(Node node) {
                
                return true;
            }
            
        };
        
        List nodes = node.findChildren(f);
       
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
    
    
    private static TokenSequence<MirahTokenId> mirahTokenSequence(Document doc, int caretOffset, boolean backwardBias) {
        BaseDocument bd = (BaseDocument)doc;
        bd.readLock();
        TokenHierarchy<?> hi = TokenHierarchy.get(doc);
        List<TokenSequence<?>> tsList = hi.embeddedTokenSequences(caretOffset, backwardBias);
        // Go from inner to outer TSes
        for (int i = tsList.size() - 1; i >= 0; i--) {
            TokenSequence<?> ts = tsList.get(i);
            if (ts.languagePath().innerLanguage() == MirahTokenId.getLanguage()) {
                TokenSequence<MirahTokenId> javaInnerTS = (TokenSequence<MirahTokenId>) ts;
                bd.readUnlock();
                return javaInnerTS;
            }
        }
        bd.readUnlock();
        return null;
    }
    
    private static int getEndOfLine(Document doc, int caretOffset){
        BaseDocument bd = (BaseDocument)doc;
        bd.readLock();
        
        TokenHierarchy<?> hi = TokenHierarchy.get(doc);
        TokenSequence<MirahTokenId> toks = mirahTokenSequence(doc, caretOffset, false);
        MirahTokenId eol = MirahTokenId.get(Tokens.tNL.ordinal());
        MirahTokenId eof = MirahTokenId.get(Tokens.tEOF.ordinal());
        while ( !eol.equals(toks.token().id()) && !eof.equals(toks.token().id())){
            if ( !toks.moveNext() ){
                break;
            }
        }
        int off = toks.token().offset(hi);
        bd.readUnlock();
        return off;
        
    }
    
    private static int getBeginningOfLine(Document doc, int caretOffset){
        BaseDocument bd = (BaseDocument)doc;
        bd.readLock();
        
        TokenHierarchy<?> hi = TokenHierarchy.get(doc);
        TokenSequence<MirahTokenId> toks = mirahTokenSequence(doc, caretOffset, true);
        MirahTokenId eol = MirahTokenId.get(Tokens.tNL.ordinal());
        //MirahTokenId eof = MirahTokenId.get(Tokens.tEOF.ordinal());
        while ( !eol.equals(toks.token().id())){
            if ( !toks.movePrevious() ){
                break;
            }
        }
        int off = toks.token().offset(hi)+toks.token().length();
        bd.readUnlock();
        return off;
    }
    
    
    
    
    
    
    
    
}
