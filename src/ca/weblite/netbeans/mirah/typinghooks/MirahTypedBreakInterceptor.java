/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.typinghooks;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.editor.mimelookup.MimeRegistrations;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.editor.indent.api.Indent;
import org.netbeans.spi.editor.typinghooks.TypedBreakInterceptor;

/**
 *
 * @author shannah
 */
public class MirahTypedBreakInterceptor implements TypedBreakInterceptor{

    
    
    
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
            } else {
                //if (TypingCompletion.blockCommentCompletion(context)) {
                //    blockCommentComplete(doc, dotPos, context);
                //}
                //isJavadocTouched = TypingCompletion.javadocBlockCompletion(context);
                //if (isJavadocTouched) {
                //    blockCommentComplete(doc, dotPos, context);
                //}
            }
    }

    @Override
    public void afterInsert(Context cntxt) throws BadLocationException {
        
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
