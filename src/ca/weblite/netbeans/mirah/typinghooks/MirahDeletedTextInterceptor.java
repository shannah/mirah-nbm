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
import org.netbeans.spi.editor.typinghooks.DeletedTextInterceptor;

/**
 *
 * @author shannah
 */
public class MirahDeletedTextInterceptor implements DeletedTextInterceptor{
    
        @Override
        public boolean beforeRemove(DeletedTextInterceptor.Context context) throws BadLocationException {
            return false;
        }

        @Override
        public void remove(DeletedTextInterceptor.Context context) throws BadLocationException {  
            //System.out.println("In remove()");
            char removedChar = context.getText().charAt(0);
            switch(removedChar) {
                case '(':
                case '[':
                    if (MirahTypingCompletion.isCompletionSettingEnabled())
                        MirahTypingCompletion.removeBrackets(context);
                    break;
                case '\"':
                case '\'':
                    if (MirahTypingCompletion.isCompletionSettingEnabled())
                        MirahTypingCompletion.removeCompletedQuote(context);
                    break;
            }
        }

        @Override
        public void afterRemove(DeletedTextInterceptor.Context context) throws BadLocationException {
        }

        @Override
        public void cancelled(DeletedTextInterceptor.Context context) {
        }

        @MimeRegistrations({
            @MimeRegistration(mimeType = "text/x-mirah", service = DeletedTextInterceptor.Factory.class)
            
        })
        public static class Factory implements DeletedTextInterceptor.Factory {

            @Override
            public DeletedTextInterceptor createDeletedTextInterceptor(MimePath mimePath) {
                return new MirahDeletedTextInterceptor();
            }
        }
}
