/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.cc;

import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import mirah.impl.Tokens;
import mirah.lang.ast.FieldDeclaration;
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
    private String filter;
    List<FieldDeclaration> matches = new ArrayList<FieldDeclaration>();
    
    public PropertyCompletionQuery(int tAtPos){
        this.tAtPos = tAtPos;
    }
    /*
     @Override
    protected boolean canFilter(JTextComponent component) {
        
        int currentPos = component.getCaretPosition();
        if ( currentPos <= tAtPos){
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
            filter = component.getText(tAtPos, currentPos-tAtPos).trim();
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        return filter != null;
    }

    @Override
    protected void filter(CompletionResultSet resultSet) {
        for ( FieldDeclaration field : matches){
            if ( field.name().identifier().toLowerCase().indexOf(filter.toLowerCase()) == 0 ){
                resultSet.addItem(new MirahPropertyCompletionItem(field, tAtPos, filter.length()));
            }

        }
        resultSet.finish();

    }

   */
    
    @Override
    protected void query(CompletionResultSet crs, Document doc, int caretOffset) {
        BaseDocument bdoc = (BaseDocument)doc;
        MirahParser.DocumentDebugger dbg = MirahParser.getDocumentDebugger(doc);
        if ( dbg != null ){
            try {
                int p = caretOffset-1;
                if ( p < 0 ){
                    return;
                }
                String lastChar = doc.getText(p, 1);

                TokenSequence<MirahTokenId> toks = mirahTokenSequence(doc, caretOffset, true);
                MirahTokenId tInstanceVar = MirahTokenId.get(Tokens.tInstVar.ordinal());
                MirahTokenId tClassVar = MirahTokenId.get(Tokens.tClassVar.ordinal());
                MirahTokenId tDot = MirahTokenId.get(Tokens.tDot.ordinal());
                MirahTokenId tAt = MirahTokenId.get(Tokens.tAt.ordinal());
                
                MirahTokenId tId = MirahTokenId.get(Tokens.tIDENTIFIER.ordinal());
                MirahTokenId tNL = MirahTokenId.get(Tokens.tNL.ordinal());
                MirahTokenId tWS = MirahTokenId.WHITESPACE;
                int bol = MirahCodeCompleter.getBeginningOfLine(doc, caretOffset);
                int eol = MirahCodeCompleter.getEndOfLine(doc, caretOffset);
                
                Token<MirahTokenId> foundToken = null;
                int tokenStart = -1;
                int tokenLen = -1;
                filter = null;
                boolean isClassVar = false;
                while ( toks.offset() >= tAtPos ){
                    Token<MirahTokenId> tok = toks.token();
                    if ( tok.id() == tInstanceVar ){
                        foundToken = tok;
                        tokenStart = toks.offset();
                        tokenLen = tok.length();
                        filter = doc.getText(tokenStart+1, caretOffset-tokenStart-1);
                        break;
                        
                    }
                    
                    if ( tok.id() == tClassVar ){
                        isClassVar = true;
                        foundToken = tok;
                        tokenStart = toks.offset();
                        tokenLen = tok.length();
                        filter = doc.getText(tokenStart+2, caretOffset-tokenStart-2);
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
                    
                    if ( tok.id() == tAt ){
                        foundToken = tok;
                        tokenStart = tAtPos+1; // Not sure why we need to do +1.  May be a bug in lexer
                        tokenLen = 1;
                        if ( "@".equals(doc.getText(tokenStart-1,1)) ){
                            isClassVar = true;
                            tokenStart--;
                            tokenLen = 2;
                        }
                        
                        break;
                    }
                }
                
                if ( foundToken == null ){
                    cancel(crs);
                    return;
                }
                
                FieldDeclaration[] fields = MirahCodeCompleter.findFields(dbg, tAtPos, isClassVar);
                if ( fields.length == 0 ){

                    Source src = Source.create(doc);
                    MirahParser parser = new MirahParser();
                    try {
                        Snapshot snapshot = src.createSnapshot();
                        String text = snapshot.getText().toString();
                        StringBuilder sb = new StringBuilder();
                        sb.append(text.substring(0, tokenStart));
                        for ( int i=tokenStart; i<eol; i++){
                            sb.append(' ');
                        }
                        sb.append(text.substring(eol));

                        parser.reparse(snapshot, sb.toString());

                    } catch (ParseException ex){
                        Exceptions.printStackTrace(ex);
                    }

                    dbg = MirahParser.getDocumentDebugger(doc);
                    //printNodes(dbg.compiler.compiler(), rightEdgeFinal);
                    fields = MirahCodeCompleter.findFields(dbg, tAtPos, isClassVar);
                    
                    
                }
                
                if ( fields.length == 0 ){
                    cancel(crs);
                } else {

                    matches.clear();
                    for ( FieldDeclaration dec : fields ){
                        if ( filter == null || dec.name().identifier().startsWith(filter)){
                            crs.addItem(new MirahPropertyCompletionItem(dec, tokenStart, tokenLen, isClassVar));
                        }
                        matches.add(dec);
                    }
                    crs.finish();
                }
                
                return;
            } catch ( BadLocationException ble){
                cancel(crs);
            } 
        } else {
            cancel(crs);
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
                    //bd.readUnlock();
                    return javaInnerTS;
                }
            }
            
            return null;
        } finally {
            bd.readUnlock();
             
        }
    }
    
}
