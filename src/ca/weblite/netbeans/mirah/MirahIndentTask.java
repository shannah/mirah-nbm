package ca.weblite.netbeans.mirah;



import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;
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
        int prevLineStart = context.lineStartOffset(context.startOffset()-1);
        int prevIndent = context.lineIndent(prevLineStart);
        context.modifyIndent(context.lineStartOffset(context.startOffset()), prevIndent);
        
        Document doc = context.document();
        int currLineStart = context.lineStartOffset(context.startOffset());
        int prevLineEnd = currLineStart -1;
        int len = prevLineEnd-prevLineStart;
        String prevLine = "";
        if ( len > 0 ){
            prevLine = doc.getText(prevLineStart, len );
        }
        //LOG.warning("PRevious line was ["+prevLine+"]");
        String prevLineTrimmed = prevLine.trim();
        if ( (prevLineTrimmed.matches("^(.*)\\bdo( \\|.*\\|){0,1}$")
            ||
             prevLineTrimmed.matches("^\\bclass .*$")
            ||
             prevLineTrimmed.matches("^\\bdef\\b.*$"))
                &&
             !prevLineTrimmed.matches("^.*\bend\b")
            
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
