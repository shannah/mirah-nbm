/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.cc;

import ca.weblite.netbeans.mirah.lexer.MirahLexer;
import ca.weblite.netbeans.mirah.lexer.MirahParser;
import ca.weblite.netbeans.mirah.lexer.MirahParser.DocumentDebugger;
import ca.weblite.netbeans.mirah.lexer.MirahParser.DocumentDebugger.PositionType;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.util.Exceptions;

/**
 *
 * @author shannah
 */
@MimeRegistration(mimeType="text/x-mirah", service=CompletionProvider.class)
public class MirahCodeCompleter implements CompletionProvider {
    private static final Logger LOG = Logger.getLogger(MirahCodeCompleter.class.getCanonicalName());
    @Override
    public CompletionTask createTask(int queryType, final JTextComponent jtc) {
        LOG.warning("TextComponent is "+jtc);
        if ( queryType != CompletionProvider.COMPLETION_QUERY_TYPE){
            return null;
        }
        final int caretPosition = jtc.getCaretPosition();
        final DocumentDebugger dbg = MirahParser.getDocumentDebugger(jtc.getDocument());
        final Document doc = jtc.getDocument();
        return new AsyncCompletionTask(new AsyncCompletionQuery(){

            @Override
            protected void query(CompletionResultSet crs, Document dcmnt, int caretOffset) {
                
                if ( dbg != null ){
                    PositionType pt = dbg.findNearestPositionOccuringBefore(caretPosition);
                    if ( pt != null ){
                        String className = pt.type.name();
                        try {
                            Class clz = Class.forName(className);
                            Method[] methods = clz.getMethods();
                            for ( int i=0; i<methods.length; i++){
                                crs.addItem(new MirahCompletionItem(methods[i].getName(), caretPosition));
                            }
                        } catch (ClassNotFoundException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
                
                crs.addItem(new MirahCompletionItem("Hello()",caretPosition ));
                crs.addItem(new MirahCompletionItem("World()",caretPosition));
                crs.finish();
            }
            
        });
    }

    @Override
    public int getAutoQueryTypes(JTextComponent jtc, String string) {
        return 0;
    }
    
}
