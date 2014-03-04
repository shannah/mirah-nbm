package ca.weblite.netbeans.mirah;



import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;
import mirah.impl.Tokens;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.editor.indent.spi.Context;
import org.netbeans.modules.editor.indent.spi.Context.Region;
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
        int prevLineStart = context.lineStartOffset(context.startOffset()-1);
        int prevIndent = context.lineIndent(prevLineStart);
        int currLineStart = context.lineStartOffset(context.startOffset());
        int currLineEnd = currLineStart;
        while ( currLineEnd < context.document().getLength() && !"\n".equals(context.document().getText(currLineEnd, 1))){
            currLineEnd++;
        }
        
        
        
        String prevLine = context.document().getText(prevLineStart, currLineStart-prevLineStart-1);
        String currLine = context.document().getText(currLineStart, currLineEnd-currLineStart);
        
        
        
        // Two questions:
        // 1. Do we need to adjust the indent of the previous line.
        // 2. Do we need to adjust the indent of the current line.
        
        TokenSequence<MirahTokenId> toks = mirahTokenSequence(context.document(), context.caretOffset(), true);
        // Check previous line for else
        
        MirahTokenId tElse = MirahTokenId.get(Tokens.tElse.ordinal());
        MirahTokenId tElsIf = MirahTokenId.get(Tokens.tElsif.ordinal());
        MirahTokenId tIf = MirahTokenId.get(Tokens.tIf.ordinal());
        int index = toks.index();
        int changePrevIndent = -1;
        while ( toks.offset() > prevLineStart ){
            System.out.println("LOOP 1");
            Token<MirahTokenId> curr = toks.token();
            if ( curr.id() == tElse || curr.id() == tElsIf ){
                // We have an else... find the matching if
                Token<MirahTokenId> curr2 = toks.token();
                while ( tIf != curr2.id() && toks.movePrevious() ){
                    System.out.println("LOOP 2");
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
        
        if ( changePrevIndent  >= 0 ){
            System.out.println("Changing offset of else to "+changePrevIndent);
            context.modifyIndent(prevLineStartPos.getOffset(), changePrevIndent);
        }
        
        
        prevLineStart = prevLineStartPos.getOffset();
        currLineStart = currLineStartPos.getOffset();
        currLineEnd = currLineEndPos.getOffset();
        
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
        
        int numOpeners = 0;
        toks = mirahTokenSequence(context.document(), context.caretOffset(), true);
        while ( toks.offset() > prevLineStart ){
            System.out.println("LOOP 3");
            Token<MirahTokenId> tok = toks.token();
            if ( tok.id() == tEnd ){
                numOpeners--;
            } else if ( openers.contains(tok.id())){
                numOpeners++;
            }
            toks.movePrevious(); 
        }
        
        if ( numOpeners > 0){
            indent += 4;
        } 
        
        toks = mirahTokenSequence(context.document(), context.caretOffset(), false);
        
        
        
        Set<MirahTokenId> closers = new HashSet<MirahTokenId>();
        closers.add(tEnd);
        closers.add(MirahTokenId.get(Tokens.tElse.ordinal()));
        closers.add(MirahTokenId.get(Tokens.tElsif.ordinal()));
        closers.add(MirahTokenId.get(Tokens.tWhen.ordinal()));
        
        while ( toks.offset() < currLineEnd ){
            System.out.println("LOOPS 4");
            if ( closers.contains(toks.token().id())){
                indent -= 4;
                break;
            }
            toks.moveNext();
        }
        
        context.modifyIndent(currLineStart, indent);
        
        
        
        
        
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
    
    
    public void reindentold() throws BadLocationException {
        if ( context.startOffset() <= 0 ){
            return;
        }
        int prevLineStart = context.lineStartOffset(context.startOffset()-1);
        int prevIndent = context.lineIndent(prevLineStart);
        
        context.modifyIndent(context.lineStartOffset(context.startOffset()), prevIndent);
        System.out.println("Start offset "+context.startOffset()+" end "+context.endOffset());
        
        
        Document doc = context.document();
        int currLineStart = context.lineStartOffset(context.startOffset());
        int prevLineEnd = currLineStart -1;
        int len = prevLineEnd-prevLineStart;
        String prevLine = "";
        if ( len > 0 ){
            prevLine = doc.getText(prevLineStart, len );
        }
        int pos = context.startOffset();
        String c = "";
        while ( !"\n".equals(c)){
            try {
                c = doc.getText(++pos, 1);
            } catch ( BadLocationException ble){
                pos--;
            }
        }
        int currLineEnd = pos;
        if ( currLineEnd < context.startOffset() ){
            currLineEnd = context.startOffset();
        }
        String currLine = doc.getText(context.startOffset(), currLineEnd-context.startOffset()).trim();
        System.out.println("Curr ("+context.startOffset()+" - "+currLineEnd+") line is "+currLine);
        LOG.warning("PRevious ("+currLineStart+" - "+currLineEnd+") line was ["+prevLine+"]");
        String prevLineTrimmed = prevLine.trim();
        
        //if ( "end".equals(currLine) && prevIndent >=4){
        //    
        //    context.modifyIndent(context.startOffset(), prevIndent);
        //    
        //}
        
        
        
        if ( (prevLineTrimmed.matches("^(.*)\\bdo( \\|.*\\|){0,1}$")
            ||
             prevLineTrimmed.matches("^\\bclass .*$")
            ||
             prevLineTrimmed.matches("^\\bdef\\b.*$"))
                &&
             !prevLineTrimmed.matches("^.*\bend\b")
                /*&&
             !"end".equals(currLine)*/
            
        ){
        
            
            
            //LOG.warning("Indenting");
            context.modifyIndent(context.lineStartOffset(context.startOffset()), prevIndent+4);
            // Check the next line indent
            //LOG.warning("End offset now "+context.endOffset());
            //if ( doc.getLength() <= context.endOffset()+1 ){
            //    //LOG.warning("Inserting String at the END of document to increase the size");
            //    doc.insertString(doc.getLength(), "\n\n", new SimpleAttributeSet());

            //}
            //LOG.warning("End offset after "+context.endOffset());
            //LOG.warning("Getting next line start");
            /*
            int nextLineStart = context.lineStartOffset(context.caretOffset()+1);
            LOG.warning("Getting next line indent");
            int nextLineIndent = context.lineIndent(nextLineStart);
            LOG.warning("Gotten lext line indent");
            String nextLinePrefix = "";
            try {
                nextLinePrefix = doc.getText(nextLineStart+nextLineIndent, 3);
            } catch (BadLocationException ble ){
                
            }
            LOG.warning("Next line prefix is "+nextLinePrefix);
            if ( nextLineIndent > prevIndent || (nextLineIndent == prevIndent && "end".equals(nextLinePrefix))){
                // We don't need to do anything here
                return;
            }
            try {
                if ( doc.getLength() <= context.caretOffset()+1 ){
                    LOG.warning("Inserting String at the END of document to increase the size");
                    doc.insertString(doc.getLength(), "\n\n", new SimpleAttributeSet());
                    
                }
                LOG.warning("Doc len: "+doc.getLength()+" offset "+context.endOffset()+1);
                doc.insertString(context.caretOffset()+1, "end\n",  new SimpleAttributeSet());
                context.modifyIndent(nextLineStart, prevIndent);
            } catch ( BadLocationException ble){
                LOG.warning("Bad location exception "+ble.getMessage());
            }
            //int nextLineStart = context.lineStartOffset(context.endOffset()+1);
            */
            
            
        }
        
        
        
    }

    @Override
    public ExtraLock indentLock() {
        return null;
    }
    
    
    
}
