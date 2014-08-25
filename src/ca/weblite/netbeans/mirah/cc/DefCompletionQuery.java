/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.cc;

import ca.weblite.netbeans.mirah.lexer.ClassQuery;
import ca.weblite.netbeans.mirah.lexer.MirahLanguageHierarchy;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import mirah.impl.Tokens;
import mirah.lang.ast.Block;
import mirah.lang.ast.Cast;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.ClosureDefinition;
import mirah.lang.ast.Constant;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import org.mirah.tool.MirahCompiler;
import org.mirah.typer.ProxyNode;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author shannah
 */
public class DefCompletionQuery extends AsyncCompletionQuery {
    boolean parsed = false;
    int tries = 0;
    
    Object lock = new Object();
    String filter = null;
    Class currentType = null;
    boolean isStatic;

    final int initialOffset;

    public DefCompletionQuery(int initOff){
        this.initialOffset = initOff;
        
    }


    private String getThisClass(Document doc, int offset){
        BaseDocument bdoc = (BaseDocument)doc;
        MirahParser.DocumentDebugger dbg = MirahParser.getDocumentDebugger(doc);
        if ( dbg == null ){
            return null;
        }
        Node n = dbg.findNearestPositionOccuringBefore(offset).node;
        
        while ( n!= null ){
            
            if ( n instanceof ClosureDefinition ){
                ClosureDefinition cd = (ClosureDefinition)n;
                return cd.superclass().typeref().name();
                
            } else if ( n instanceof ClassDefinition ){
                ClassDefinition cd = (ClassDefinition)n;
                return dbg.getType(n).name();
               
            }
            n = n.parent();
            
        }
        
        return null;
        
        
    }
   
    
   


    @Override
    protected void query(final CompletionResultSet crs, final Document doc, final int caretOffset) {
        
        try {

            BaseDocument bdoc = (BaseDocument)doc;
            bdoc.readLock();
            TokenHierarchy<?> hi = TokenHierarchy.get(doc);
            bdoc.readUnlock();
            if ( crs.isFinished() || this.isTaskCancelled()){
                if ( !crs.isFinished() ){
                    crs.finish();
                }
                return;
            }
            
            String thisClassName = getThisClass(doc, caretOffset);
            if ( thisClassName == null ){
                crs.finish();
                return;
            }
            FileObject fileObject = NbEditorUtilities.getFileObject(doc);
            Class thisClass = MirahCodeCompleter.findClass(fileObject, thisClassName);

            if ( thisClass == null){
                crs.finish();
                return;
            }


            TokenSequence<MirahTokenId> toks = MirahCodeCompleter.mirahTokenSequence(doc, caretOffset, true);
            int bol = MirahCodeCompleter.getBeginningOfLine(doc, caretOffset);
            int eol = MirahCodeCompleter.getEndOfLine(doc, caretOffset);
            int defOffset = -1;

            while ( toks.movePrevious() ){
                if ( toks.token().id().ordinal() == Tokens.tDef.ordinal() ){
                    defOffset = toks.token().offset(hi);
                    break;
                }
            }

            if ( defOffset < 0 ){
                crs.finish();
                return;
            }

            filter = "";
            
            Class cls = thisClass;
            if ( cls.getSuperclass() != null ){
                cls = cls.getSuperclass();
            }
            
            ClassQuery cq = new ClassQuery(cls);
            
            for ( Method m : cq.getAccessibleMethods(thisClass)){

                //int modifiers = m.getModifiers();
                //if ( Modifier.isPrivate(modifiers) ){
                //    continue;
                //}
                
                if ( m.getName().startsWith(filter) && !Modifier.isStatic(m.getModifiers()) ){
                    crs.addItem(new MirahDefCompletionItem(m, defOffset, eol-defOffset));
                }
            }

            if ( !crs.isFinished() ){
                crs.finish();
            }
        } finally {
            if ( !crs.isFinished() ){
                crs.finish();
            }
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

    
    
}
