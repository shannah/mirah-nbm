/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.typinghooks;

import javax.swing.text.BadLocationException;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.editor.mimelookup.MimeRegistrations;
import org.netbeans.spi.editor.typinghooks.TypedTextInterceptor;

/**
 *
 * @author shannah
 */
public class MirahTypedTextInterceptor implements TypedTextInterceptor {
private int caretPosition = -1;

        @Override
        public boolean beforeInsert(Context context) throws BadLocationException {
            return false;
        }

        @Override
        public void insert(MutableContext context) throws BadLocationException {
            char insertedChar = context.getText().charAt(0);
            switch(insertedChar) {
                case '(':
                case '[':
                    //if (TypingCompletion.isCompletionSettingEnabled())
                        MirahTypingCompletion.completeOpeningBracket(context);
                    break;
                case ')':
                case ']':
                    //if (TypingCompletion.isCompletionSettingEnabled())
                        caretPosition = MirahTypingCompletion.skipClosingBracket(context);
                    break;
                //case ';':
                    //if (TypingCompletion.isCompletionSettingEnabled())
                //        caretPosition = TypingCompletion.moveOrSkipSemicolon(context);
                //    break;
                case '\"':
                case '\'':
                    //if (MirahTypingCompletion.isCompletionSettingEnabled())
                        caretPosition = MirahTypingCompletion.completeQuote(context);
                    break;
            }
        }

        @Override
        public void afterInsert(Context context) throws BadLocationException { 
            if (caretPosition != -1) {
                context.getComponent().setCaretPosition(caretPosition);
                caretPosition = -1;
            }
        }

        @Override
        public void cancelled(Context context) {
        }

        @MimeRegistrations({
            @MimeRegistration(mimeType = "text/x-mirah", service = TypedTextInterceptor.Factory.class)
        })
        public static class Factory implements TypedTextInterceptor.Factory {

            @Override
            public TypedTextInterceptor createTypedTextInterceptor(MimePath mimePath) {
                return new MirahTypedTextInterceptor();
            }
        }
}
