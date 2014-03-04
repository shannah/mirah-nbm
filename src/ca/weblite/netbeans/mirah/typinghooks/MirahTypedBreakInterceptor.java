/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.typinghooks;

import java.awt.event.ActionEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.TextAction;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.editor.mimelookup.MimeRegistrations;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.editor.indent.api.Indent;
import org.netbeans.spi.editor.typinghooks.TypedBreakInterceptor;
import org.openide.util.Lookup;

/**
 *
 * @author shannah
 */
public class MirahTypedBreakInterceptor implements TypedBreakInterceptor{
    private boolean isJavadocTouched = false;
    
    
    
    @Override
    public boolean beforeInsert(Context cntxt) throws BadLocationException {
        return false;
    }

    @Override
    public void insert(MutableContext context) throws BadLocationException {
        int dotPos = context.getCaretOffset();
        Document doc = context.getDocument();

        BaseDocument baseDoc = (BaseDocument) context.getDocument();
        if (MirahTypingCompletion.isAddRightBrace(baseDoc, dotPos)) {
            boolean insert[] = {true};
            int end = MirahTypingCompletion.getRowOrBlockEnd(baseDoc, dotPos, insert);
            if (insert[0]) {
                doc.insertString(end, "}", null); // NOI18N
                Indent.get(doc).indentNewLine(end);
            }
            context.getComponent().getCaret().setDot(dotPos);
        } else if ( MirahTypingCompletion.isAddEnd(baseDoc, dotPos)){
            boolean insert[] = {true};
            int end = MirahTypingCompletion.getRowOrBlockEnd(baseDoc, dotPos, insert);
            if (insert[0]) {
                
                doc.insertString(end, "end", null); // NOI18N
                //Indent.get(doc).reindent(end+1);
                Indent.get(doc).indentNewLine(end);
                
                
            }
            context.getComponent().getCaret().setDot(dotPos);
        } else if (MirahTypingCompletion.blockCommentCompletion(context)) {
            blockCommentComplete(doc, dotPos, context);
        }
        isJavadocTouched = MirahTypingCompletion.javadocBlockCompletion(context);
        if (isJavadocTouched) {
            blockCommentComplete(doc, dotPos, context);
        }
    }
    private void blockCommentComplete(Document doc, int dotPos, MutableContext context) throws BadLocationException {
        // note that the formater will add one line of javadoc
        doc.insertString(dotPos, "*/", null); // NOI18N
        Indent.get(doc).indentNewLine(dotPos);
        context.getComponent().getCaret().setDot(dotPos);
    }


       

    @Override
    public void afterInsert(Context context) throws BadLocationException {
        /*if (isJavadocTouched) {
                Lookup.Result<TextAction> res = MimeLookup.getLookup(MimePath.parse("text/x-javadoc")).lookupResult(TextAction.class); // NOI18N
                ActionEvent newevt = new ActionEvent(context.getComponent(), ActionEvent.ACTION_PERFORMED, "fix-javadoc"); // NOI18N
                for (TextAction action : res.allInstances()) {
                    action.actionPerformed(newevt);
                }
                isJavadocTouched = false;
            }*/
    }

    @Override
    public void cancelled(Context cntxt) {
        
    }
    
    
  
    @MimeRegistration(mimeType = "text/x-mirah", service = TypedBreakInterceptor.Factory.class)
    public static class MirahFactory implements TypedBreakInterceptor.Factory {

        @Override
        public TypedBreakInterceptor createTypedBreakInterceptor(MimePath mimePath) {
            return new MirahTypedBreakInterceptor();
        }
    }
    
}
