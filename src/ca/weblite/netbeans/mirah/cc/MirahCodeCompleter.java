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
import java.util.SortedSet;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.filesystems.FileObject;
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
        

        // We should be looking for a method or field
        return new AsyncCompletionTask(new AsyncCompletionQuery(){

            @Override
            protected void query(CompletionResultSet crs, Document doc, int caretOffset) {
                DocumentDebugger dbg = MirahParser.getDocumentDebugger(doc);
                if ( dbg != null ){
                    try {

                        int p = caretOffset-1;
                        if ( p < 0 ){
                            return;
                        }
                        String lastChar = doc.getText(p, 1);
                        while ( p > 0 && lastChar.trim().isEmpty()){
                            p--;
                            lastChar = doc.getText(p, 1);
                        }


                        if ( ".".equals(lastChar) ){

                            // Find right edge of last node before '.'
                            int rightEdge = p-1;
                            String tmp = doc.getText(rightEdge, 1);
                            while ( rightEdge > 0 && tmp.trim().isEmpty()){
                                rightEdge--;
                                tmp = doc.getText(rightEdge, 1);
                            }

                            rightEdge++;

                            LOG.warning("CARET OFFSET "+caretOffset);
                            //PositionType pt = dbg.findNearestPositionOccuringBefore(caretPosition);
                            LOG.warning("Inside async query");
                            SortedSet<PositionType> positions = dbg.findPositionsWithRightEdgeInRange(rightEdge, rightEdge);
                            LOG.warning("Found set "+positions);
                            for ( PositionType pt : positions ){
                                FileObject fileObject = NbEditorUtilities.getFileObject(doc);
                                LOG.warning("File Object for document "+fileObject.getPath());
                                Class cls = findClass(fileObject, pt.type.name());
                                if ( cls != null ){
                                    for ( Method m : cls.getMethods()){
                                        crs.addItem(new MirahMethodCompletionItem(m, caretOffset));
                                    }
                                }

                               // crs.addItem(new MirahCompletionItem(p.type.name(), caretPosition));
                            }



                            
                        }
                    } catch ( BadLocationException ble ){
                        ble.printStackTrace();
                    }
                }
                crs.finish();
            }

        }, jtc);
        

        
            
        
        
        
    }

    @Override
    public int getAutoQueryTypes(JTextComponent jtc, String string) {
        return 0;
    }
    
    private Class findClass(FileObject o, String name){
        ClassPath[] paths = new ClassPath[]{
            ClassPath.getClassPath(o, ClassPath.SOURCE),
            ClassPath.getClassPath(o, ClassPath.EXECUTE),
            ClassPath.getClassPath(o, ClassPath.COMPILE),
            ClassPath.getClassPath(o, ClassPath.BOOT)
        };
        
        for ( int i=0; i<paths.length; i++){
            ClassPath cp = paths[i];
            try {
                Class c = cp.getClassLoader(true).loadClass(name);
                if ( c != null ){
                    return c;
                }
            } catch ( ClassNotFoundException ex){
                
            }
        }
        return null;
    }
    
    
    
}
