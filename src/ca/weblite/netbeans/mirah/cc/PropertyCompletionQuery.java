/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.cc;

import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import mirah.impl.Tokens;
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.Node;
import org.mirah.typer.ResolvedType;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.openide.util.Exceptions;

/**
 *
 * @author shannah
 */
public class PropertyCompletionQuery extends AsyncCompletionQuery{
    final int tAtPos;
    
    public PropertyCompletionQuery(int tAtPos){
        this.tAtPos = tAtPos;
    }
    
    @Override
    protected void query(CompletionResultSet crs, Document doc, int caretOffset) {
        BaseDocument bdoc = (BaseDocument)doc;
        //System.out.println("In property completion query");
        MirahParser.DocumentDebugger dbg = MirahParser.getDocumentDebugger(doc);
                
        if ( dbg != null ){
            //System.out.println("Debugger is not null");
            try {
                int p = caretOffset-1;
                if ( p < 0 ){
                    return;
                }
                String lastChar = doc.getText(p, 1);

                TokenSequence<MirahTokenId> toks = mirahTokenSequence(doc, caretOffset, true);
                MirahTokenId tInstanceVar = MirahTokenId.get(Tokens.tInstVar.ordinal());
                MirahTokenId tDot = MirahTokenId.get(Tokens.tDot.ordinal());
                MirahTokenId tAt = MirahTokenId.get(Tokens.tAt.ordinal());
                
                MirahTokenId tId = MirahTokenId.get(Tokens.tIDENTIFIER.ordinal());
                MirahTokenId tNL = MirahTokenId.get(Tokens.tNL.ordinal());
                MirahTokenId tWS = MirahTokenId.WHITESPACE;
                
                Token<MirahTokenId> foundToken = null;
                int tokenStart = -1;
                int tokenLen = -1;
                
                //System.out.println("About to walk back the tree "+toks.offset()+" "+tAtPos);
                while ( toks.offset() >= tAtPos ){
                    //System.out.println("OFFSET "+toks.offset());
                    Token<MirahTokenId> tok = toks.token();
                    //System.out.println("TOKEN is "+tok.id().name());
                    if ( tok.id() == tInstanceVar ){
                        foundToken = tok;
                        tokenStart = toks.offset();
                        tokenLen = tok.length();
                        break;
                        
                    }
                    if ( tok.id() != tId && tok.id() != tAt ){
                        cancel(crs);
                        return;
                    }
                    if ( !toks.movePrevious() ){
                        cancel(crs);
                        return;
                    }
                }
                
                if ( foundToken == null ){
                    cancel(crs);
                    return;
                }
                cancel(crs);
                return;
                /*
                if ( foundToken.id() == tInstanceVar ){
                    String varName = doc.getText(tokenStart, tokenLen);
                    
                    Node foundNode = MirahCodeCompleter.findNode(dbg, tokenStart+tokenLen);

                    if ( foundNode == null ){
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
                        
                    }
                    
                    if ( foundNode != null ){
                        ClassDefinition cls = (ClassDefinition)foundNode.findAncestor(ClassDefinition.class);
                        System.out.println("Found class definition "+cls);

                    } else {
                        System.out.println("Could not find node");
                    }
                    
                }
                
                
                cancel(crs);
                        */
            } catch ( BadLocationException ble){
                cancel(crs);
            }
        } 
    }
    
    private void cancel(CompletionResultSet crs){
        crs.finish();
    }
    
    
    private static TokenSequence<MirahTokenId> mirahTokenSequence(Document doc, int caretOffset, boolean backwardBias) {
        BaseDocument bd = (BaseDocument)doc;
        bd.readLock();
        try {
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
            
            return null;
        } finally {
            bd.readUnlock();
        }
    }
    
}
