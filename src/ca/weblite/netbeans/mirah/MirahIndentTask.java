package ca.weblite.netbeans.mirah;



import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import mirah.impl.Tokens;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.editor.indent.api.IndentUtils;
import org.netbeans.modules.editor.indent.spi.Context;
import org.netbeans.modules.editor.indent.spi.ExtraLock;
import org.netbeans.modules.editor.indent.spi.IndentTask;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author shannah
 */
public class MirahIndentTask implements IndentTask  {
    private Context context;
    private static final Logger LOG = Logger.getLogger(MirahIndentTask.class.getCanonicalName());
    public MirahIndentTask(Context ctx){
        this.context = ctx;
        
    }

    @Override
    public void reindent() throws BadLocationException {
        
        if ( context.startOffset() <= 0 ){
            return;
        }
        int indentSize = IndentUtils.indentLevelSize(context.document());
        int prevLineStart = context.lineStartOffset(context.startOffset()-1);
        int prevIndent = context.lineIndent(prevLineStart);
        int currLineStart = context.lineStartOffset(context.startOffset());
        int currLineEnd = currLineStart;
        while ( currLineEnd < context.document().getLength() && !"\n".equals(context.document().getText(currLineEnd, 1))){
            currLineEnd++;
        }
        
        
        int prevLineLen = currLineStart - prevLineStart-1;
        if ( prevLineLen < 0){
            prevLineLen = 0;
        }
        
        int currLineLen = currLineEnd-currLineStart;
        if ( currLineLen < 0 ){
            currLineLen = 0;
        }
        //String prevLine = context.document().getText(prevLineStart, prevLineLen);
        //String currLine = context.document().getText(currLineStart, currLineEnd-currLineStart);
        
        
        
        // Two questions:
        // 1. Do we need to adjust the indent of the previous line.
        // 2. Do we need to adjust the indent of the current line.
        
        TokenSequence<MirahTokenId> toks = mirahTokenSequence(context.document(), context.caretOffset(), true);
        // Check previous line for else
        
        MirahTokenId tElse = MirahTokenId.get(Tokens.tElse.ordinal());
        MirahTokenId tElsIf = MirahTokenId.get(Tokens.tElsif.ordinal());
        MirahTokenId tIf = MirahTokenId.get(Tokens.tIf.ordinal());
        //int index = toks.index();
        int changePrevIndent = -1;
        while ( toks.offset() > prevLineStart ){
            Token<MirahTokenId> curr = toks.token();
            if ( curr.id() == tElse || curr.id() == tElsIf ){
                // We have an else... find the matching if
                Token<MirahTokenId> curr2 = toks.token();
                while ( tIf != curr2.id() && toks.movePrevious() ){
                    curr2 = toks.token();
                }
                if ( tIf == curr2.id() ){
                    int ifStartLineOffset = context.lineStartOffset(toks.offset());
                    changePrevIndent = context.lineIndent(ifStartLineOffset);
                    break;
                    
                }
            }
            if ( !toks.movePrevious() ){
                break;
            }
        }
        
        Position prevLineStartPos = context.document().createPosition(prevLineStart);
        Position currLineStartPos = context.document().createPosition(currLineStart);
        Position currLineEndPos = context.document().createPosition(currLineEnd);
        prevIndent = context.lineIndent(prevLineStartPos.getOffset());
        if ( changePrevIndent  >= 0  && prevIndent != changePrevIndent){
            context.modifyIndent(prevLineStartPos.getOffset(), changePrevIndent);
        }
        
        
        prevLineStart = prevLineStartPos.getOffset();
        currLineStart = currLineStartPos.getOffset();
        currLineEnd = currLineEndPos.getOffset();
        
        prevLineLen = currLineStart - prevLineStart-1;
        if ( prevLineLen < 0){
            prevLineLen = 0;
        }
        
        currLineLen = currLineEnd-currLineStart;
        if ( currLineLen < 0 ){
            currLineLen = 0;
        }
        
        //toks.moveIndex(index);
        
        
        //StringBuilder sb = new StringBuilder();
        //int pos = currLineStart;
        //while ( pos < context.document().getLength() ){
        //    String ch = context.document().getText(pos++, 1);
        //    if ( "\n".equals(ch)){
        //        break;
        //    }
        //    sb.append(ch);
       // }
       // String currLine = sb.toString();
        
        int indent = prevIndent;
        
        
        MirahTokenId tEnd = MirahTokenId.get(Tokens.tEnd.ordinal());
        Set<MirahTokenId> openers = new HashSet<MirahTokenId>();
        openers.add(MirahTokenId.get(Tokens.tClass.ordinal()));
        openers.add(MirahTokenId.get(Tokens.tBegin.ordinal()));
        openers.add(MirahTokenId.get(Tokens.tIf.ordinal()));
        openers.add(MirahTokenId.get(Tokens.tDo.ordinal()));
        openers.add(MirahTokenId.get(Tokens.tDef.ordinal()));
        openers.add(MirahTokenId.get(Tokens.tInterface.ordinal()));
        
        LinkedList<MirahTokenId> foundOpeners = new LinkedList<MirahTokenId>();
        boolean equalsEncountered = false;
        
        //int numOpeners = 0;
        toks = mirahTokenSequence(context.document(), prevLineStart, false);
        
        while ( toks.offset() < currLineStart ){
            Token<MirahTokenId> tok = toks.token();
            if ( tok.id().ordinal() == Tokens.tEQ.ordinal()){
                equalsEncountered = true;
            }
            
            
            if ( tok.id() == tEnd ){
                //numOpeners--;
                if ( !foundOpeners.isEmpty()){
                    foundOpeners.pop();
                }
            } else if ( openers.contains(tok.id()) &&
                    !(tok.id().ordinal() == Tokens.tIf.ordinal() && equalsEncountered)
                    ){
                //numOpeners++;
                foundOpeners.push(tok.id());
            }
            toks.moveNext(); 
        }
        
        if ( !foundOpeners.isEmpty()){
            indent += foundOpeners.size()*indentSize;
        } 
        
        toks = mirahTokenSequence(context.document(), context.caretOffset(), false);
        
        
        
        Set<MirahTokenId> closers = new HashSet<MirahTokenId>();
        closers.add(tEnd);
        closers.add(MirahTokenId.get(Tokens.tElse.ordinal()));
        closers.add(MirahTokenId.get(Tokens.tElsif.ordinal()));
        closers.add(MirahTokenId.get(Tokens.tWhen.ordinal()));
        toks.move(currLineStart);
        toks.moveNext();
        while ( toks.offset() < currLineEnd ){
            if ( closers.contains(toks.token().id())){
                indent -= indentSize;
                break;
            }
            toks.moveNext();
        }
        
        if ( indent >= 0 ){
            context.modifyIndent(currLineStart, indent);
        }
        
        
        
        
        
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
    }
    
    
   

    @Override
    public ExtraLock indentLock() {
        return null;
    }
    
    
    
}
