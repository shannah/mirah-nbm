/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.lexer;

import java.util.ArrayList;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import mirah.impl.Tokens;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.openide.util.Exceptions;

/**
 *
 * @author shannah
 */
public class DocumentQuery {
    
    Document doc;
    
    public DocumentQuery(Document doc){
        this.doc = doc;
    }
    
    public int getEOL(int offset){
        
        int out = offset;
        try {
            
            int len = doc.getLength();
            while ( out >=0 && out < len && !"\n".equals(doc.getText(out, 1)) ){
                out++;
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        return out;
    }
    
    public int getBOL(int offset){
        int out = offset;
        try {
            
            int len = doc.getLength();
            while ( out >=0 && out < len && !"\n".equals(doc.getText(out, 1)) ){
                out--;
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        try {
            if ( "\n".equals(doc.getText(out, 1) )){
                out++;
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        return out;
    }
    
    public int getIndent(int offset){
        try {
            int bol = getBOL(offset);
            int eol = getEOL(offset);
            int i = bol;
            int len = doc.getLength();
            while ( i > 0 && i<eol && " ".equals(doc.getText(i, 1))){
                i++;
            }
            return i-bol;
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        return 0;
    }
    
    public List<Token<MirahTokenId>> findLambdaTypes(){
        List<Token<MirahTokenId>> out = new ArrayList<Token<MirahTokenId>>();
        TokenHierarchy<?> hi = TokenHierarchy.get(doc);
        int caretOffset = 0;
        TokenSequence<MirahTokenId> seq = getTokens(caretOffset, false);
        
        while ( seq.moveNext() ){
            
            if ( seq.token().id().ordinal() == Tokens.tIDENTIFIER.ordinal() 
                    && "lambda".equals(String.valueOf(seq.token().text()))){
                while ( seq.moveNext() ){
                    if ( seq.token().id().ordinal() == Tokens.tCONSTANT.ordinal() ){
                        out.add(seq.token());
                        break;
                    }
                }
            }
        }
        return out;
    }
    
    
    public void addImport(String fqn) throws BadLocationException{
        if ( requiresImport(fqn) ){
            TokenHierarchy<?> hi = TokenHierarchy.get(doc);
            int caretOffset = 0;
            TokenSequence<MirahTokenId> seq = mirahTokenSequence(doc, caretOffset, false);
            
            // Find the first package or import and place the import after that.
            MirahTokenId PKG = MirahTokenId.get(Tokens.tPackage.ordinal());
            MirahTokenId IMPORT = MirahTokenId.get(Tokens.tImport.ordinal());
            MirahTokenId EOL = MirahTokenId.get(Tokens.tNL.ordinal());
            
            
            
            int pos = 0;
            Token pkg = null;
            Token firstImport = null;
            
            do {
                Token curr = seq.token();
                MirahTokenId currTok = (MirahTokenId)curr.id();
                if ( PKG.equals(currTok) ){
                    pkg = curr;
                } else if ( IMPORT.equals(currTok)){
                    firstImport = curr;
                }
                
            } while ( seq.moveNext());
            
            
            if ( firstImport != null ){
                try {
                    doc.insertString(firstImport.offset(hi), "import "+fqn+"\n", new SimpleAttributeSet());
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }
            } else if ( pkg != null ){
                seq.move(pkg.offset(hi));
                while ( seq.moveNext() ){
                    if ( EOL.equals(seq.token().id())){
                        doc.insertString(seq.token().offset(hi), "\nimport "+fqn+"\n", new SimpleAttributeSet());
                        break;
                    }
                }
            } else {
                doc.insertString(0, "import "+fqn+"\n", new SimpleAttributeSet());
            }
        }
            
            
    }
    
    public List<String> getImports() {
        //Document doc = source.getDocument(true);
            TokenHierarchy<?> hi = TokenHierarchy.get(doc);
            int caretOffset = 0;
            TokenSequence<MirahTokenId> seq = getTokens(caretOffset, false);
            
            // Find the first package or import and place the import after that.
            MirahTokenId PKG = MirahTokenId.get(Tokens.tPackage.ordinal());
            MirahTokenId IMPORT = MirahTokenId.get(Tokens.tImport.ordinal());
            MirahTokenId EOL = MirahTokenId.get(Tokens.tNL.ordinal());
            
            
            
            int pos = 0;
            Token pkg = null;
            Token firstImport = null;
            List<String> out = new ArrayList<String>();
            do {
                Token curr = seq.token();
                MirahTokenId currTok = (MirahTokenId)curr.id();
                if ( IMPORT.equals(currTok)){
                    while ( seq.moveNext() && seq.token().id().ordinal() != Tokens.tIDENTIFIER.ordinal()){
                        
                    }
                    StringBuilder sb = new StringBuilder();
                    do {
                        sb.append(seq.token().text());
                    } while ( 
                            (
                                seq.token().id().ordinal() == Tokens.tDot.ordinal() 
                                || seq.token().id().ordinal() == Tokens.tIDENTIFIER.ordinal()
                                || seq.token().id().ordinal() == Tokens.tStar.ordinal()
                                
                            ) && seq.moveNext()
                            
                    );
                    out.add(sb.toString());
                }
                
            } while ( seq.moveNext());
            
            return out;
    }
   
    public boolean requiresImport(String fqn){
        String pkg = "";
        String simpleName = fqn;
        if ( fqn.indexOf(".") != -1 ){
            pkg = fqn.substring(0, fqn.lastIndexOf("."));
            simpleName = fqn.substring(pkg.length()+1);
        }
        
        String pkgGlob = pkg+".*";
        
        List<String> imports = getImports();
        for ( String line : imports ){
            if ( pkgGlob.equals(line)){
                return false;
            }
            if ( fqn.equals(line)){
                return false;
            }
        }
        return true;
    }
    
    
    public TokenSequence<MirahTokenId> getTokens(int caretOffset, boolean backwardBias){
        return mirahTokenSequence(doc, caretOffset, backwardBias);
    }
    
    
    
    public int getAfterNextDo(int caretOffset){
        //System.out.println("Doc len is "+doc.getLength());
        TokenSequence<MirahTokenId> seq = getTokens(caretOffset, true);
        while ( seq.moveNext() ){
            //System.out.println("Tok is "+seq.token());
            if ( seq.token().id().ordinal() == Tokens.tDo.ordinal() ){
                //System.out.println("Found do... checking if there is something after");
                if ( seq.moveNext() ){
                    return seq.token().offset(TokenHierarchy.get(doc));
                } else {
                    return -1;
                }
            }
        }
        return -1;
    }
    
    
    /**
     * Get token sequence positioned over a token.
     *
     * @param doc
     * @param caretOffset
     * @param backwardBias
     * @return token sequence positioned over a token that "contains" the offset
     * or null if the document does not contain any java token sequence or the
     * offset is at doc-or-section-start-and-bwd-bias or
     * doc-or-section-end-and-fwd-bias.
     */
    private static TokenSequence<MirahTokenId> mirahTokenSequence(Document doc, int caretOffset, boolean backwardBias) {
        BaseDocument bdoc = (BaseDocument)doc;
        bdoc.readLock();
        try {
            TokenHierarchy<?> hi = TokenHierarchy.get(doc);
            List<TokenSequence<?>> tsList = hi.embeddedTokenSequences(caretOffset, backwardBias);
            //System.out.println(tsList+" off "+caretOffset+" back: "+backwardBias);
            // Go from inner to outer TSes
            for (int i = tsList.size() - 1; i >= 0; i--) {
                TokenSequence<?> ts = tsList.get(i);
                if (ts.languagePath().innerLanguage() == MirahTokenId.getLanguage()) {
                    TokenSequence<MirahTokenId> javaInnerTS = (TokenSequence<MirahTokenId>) ts;
                    return javaInnerTS;
                }
            }
            return null;
        } finally {
            bdoc.readUnlock();
        }
    }
}
