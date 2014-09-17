/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.cc;

import ca.weblite.netbeans.mirah.lexer.DocumentQuery;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import mirah.impl.Tokens;
import mirah.lang.ast.Constant;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeScanner;
import org.mirah.tool.MirahCompiler;
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
public class MethodCompletionQuery extends AsyncCompletionQuery {
    boolean parsed = false;
    int tries = 0;
    Object lock = new Object();
    String filter = null;
    Class currentType = null;
    boolean isStatic;
    FileObject file;
    final int initialOffset;

    public MethodCompletionQuery(int initOff, FileObject file){
        this.initialOffset = initOff;
        this.file = file;
    }


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
        //System.out.println("Filter "+filter);
        for ( Method m : currentType.getMethods()){
            if ( m.getName().toLowerCase().indexOf(filter.toLowerCase()) == 0 && isStatic == Modifier.isStatic(m.getModifiers()) ){
                resultSet.addItem(new MirahMethodCompletionItem(file, m, initialOffset, filter.length(), currentType));
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
        MirahParser.DocumentDebugger dbg = MirahParser.getDocumentDebugger(doc);

        if ( dbg != null ){
            try {
                int p = caretOffset-1;
                if ( p < 0 ){
                    return;
                }
                String lastChar = doc.getText(p, 1);

                TokenSequence<MirahTokenId> toks = MirahCodeCompleter.mirahTokenSequence(doc, caretOffset, true);

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

                int bol = MirahCodeCompleter.getBeginningOfLine(doc, caretOffset);
                int eol = MirahCodeCompleter.getEndOfLine(doc, caretOffset);
                int dotPos = dotToken.offset(hi);



                Node foundNode = MirahCodeCompleter.findNode(dbg, subjectToken.offset(hi)+subjectToken.length());

                ResolvedType type = null;
                if ( foundNode != null ){
                    type = dbg.getType(foundNode);
                    
                    if ( type == null ){
                        DocumentQuery dq = new DocumentQuery(bdoc);
                        TokenSequence<MirahTokenId> seq = dq.getTokens(foundNode.position().endChar(), true);
                        String typeName = dq.guessType(seq, file);
                        //System.out.println("Type name guessed to be "+typeName);
                    }
                    //Node c = foundNode;
                    //while ( c != null ){
                    //
                    //    c = c.parent();
                    //}

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
                    foundNode = MirahCodeCompleter.findNode(dbg, subjectToken.offset(hi)+subjectToken.length());
                    if ( foundNode != null ){
                        type = dbg.getType(foundNode);
                    }

                }

                if ( foundNode != null ){

                    type = dbg.getType(foundNode);

                    if ( type != null ){


                        FileObject fileObject = NbEditorUtilities.getFileObject(doc);
                        Class cls = MirahCodeCompleter.findClass(fileObject, dbg.getType(foundNode).name());
                        currentType = cls;

                        isStatic = foundNode instanceof Constant;
                        if ( cls != null ){
                            if ( isStatic && filter == null || "new".startsWith(filter)){
                                for ( Constructor c : cls.getConstructors()){
                                    crs.addItem(new MirahConstructorCompletionItem(c, caretOffset-filter.length(), filter.length()));
                                }
                            }
                            for ( Method m : cls.getMethods()){
                                if ( m.getName().startsWith(filter) && isStatic == Modifier.isStatic(m.getModifiers())){
                                    crs.addItem(new MirahMethodCompletionItem(fileObject, m, caretOffset-filter.length(), filter.length(), cls));
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

    
    
}
