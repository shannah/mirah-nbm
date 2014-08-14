/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah.cc;

import ca.weblite.netbeans.mirah.lexer.ClassQuery;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.lib.editor.codetemplates.api.CodeTemplate;
import org.netbeans.lib.editor.codetemplates.api.CodeTemplateManager;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;

/**
 *
 * @author shannah
 */
public class MirahMethodCompletionItem implements CompletionItem {

    private int caretOffset;
    private int length;
    private Method method;
    private static ImageIcon fieldIcon = new ImageIcon(ImageUtilities.loadImage("ca/weblite/netbeans/mirah/1391571312_application-x-ruby.png"));
    private static Color fieldColor = Color.decode("0x0000B2");
    private FileObject file;
    final private Class cls;

    public MirahMethodCompletionItem(FileObject file, Method m, int caretOffset, int len, Class cls) {
        this.method = m;
        this.caretOffset = caretOffset;
        this.length = len;
        this.file = file; 
        this.cls = cls;
        
    }

    
    Method getMethod(){
        return method;
    }
    
    FileObject getFile(){
        return file;
    }
    
    @Override
    public void defaultAction(JTextComponent jtc) {
        CodeTemplateManager mgr = CodeTemplateManager.get(jtc.getDocument());
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName());
        Class[] ptypes = method.getParameterTypes();
        
        if ( ptypes.length > 0 ){
            sb.append("(");
            for ( int i=0; i<ptypes.length; i++){
                sb.append("${PARAM");
                sb.append(i);
                sb.append(" instanceof=\"");
                sb.append(ptypes[i].getCanonicalName());
                sb.append("\" default=\"nil\"}");
                if ( i < ptypes.length-1 ){
                    sb.append(", ");
                }
            }
            sb.append(")");
            
        }
        String tplStr = sb.toString();
        
        CodeTemplate tpl = mgr.createTemporary(tplStr);
        
        
        try {
            StyledDocument doc = (StyledDocument) jtc.getDocument();
            if ( length > 0 ){
                doc.remove(caretOffset, length);
            }
            tpl.insert(jtc);
            //doc.insertString(caretOffset, tpl., null);
            //This statement will close the code completion box:
            Completion.get().hideAll();
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    private String formatMethod(Method m){
        ClassQuery cq = new ClassQuery(cls);
        
        StringBuilder sb = new StringBuilder();
        sb.append(m.getName());
        Class[] ptypes = m.getParameterTypes();
        if ( ptypes.length == 0 ){
            return sb.toString();
        }
        sb.append("(");
        List<String> pnames = null;
        try {
            pnames = cq.getParameterNames(method, file);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        int i=0;
        for ( Class c : ptypes ){
            if ( pnames != null && pnames.size() > i){
                sb.append(pnames.get(i)).append(":");
            }
            i++;
            sb.append(c.getSimpleName());
            sb.append(",");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append(")");
        return sb.toString();
        
        
    }
    
    private String formatMethodForList(Method m){
        ClassQuery cq = new ClassQuery(cls);
        StringBuilder sb = new StringBuilder();
        sb.append(m.getName());
        Class[] ptypes = m.getParameterTypes();
        List<String> pnames = null;
        try {
            pnames = cq.getParameterNames(method, file);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        int i=0;
        if ( ptypes.length > 0 ){
            sb.append("(");
            for ( Class c : ptypes ){
                 if ( pnames != null && pnames.size() > i){
                    sb.append(pnames.get(i)).append(":");
                }
                i++;
                sb.append(c.getSimpleName());
                sb.append(", ");
            }
            sb.delete(sb.length()-2, sb.length()-1);
            sb.append(")");
        }
        sb.append(":");
        sb.append(m.getReturnType().getSimpleName());
        return sb.toString();
    }

    @Override
    public void processKeyEvent(KeyEvent ke) {

    }

    @Override
    public int getPreferredWidth(Graphics graphics, Font font) {
        return CompletionUtilities.getPreferredWidth(formatMethodForList(method), null, graphics, font);
    }

    @Override
    public void render(Graphics g, Font font, Color color, Color color1, int width, int height, boolean selected) {
        CompletionUtilities.renderHtml(fieldIcon, formatMethodForList(method), null, g, font,
                (selected ? Color.white : fieldColor), width, height, selected);
    }

    @Override
    public CompletionTask createDocumentationTask() {
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int i) {
                MethodCompletionDocumentation doc = new MethodCompletionDocumentation(MirahMethodCompletionItem.this);
                // Get the text in this query to cache it so that it doesn't block the UI thread.
                doc.getText();
                completionResultSet.setDocumentation(doc);
                completionResultSet.finish();
            }
        });
    }

    @Override
    public CompletionTask createToolTipTask() {
        return null;
    }

    @Override
    public boolean instantSubstitution(JTextComponent jtc) {
        return false;
    }

    @Override
    public int getSortPriority() {
        return 0;
    }

    @Override
    public CharSequence getSortText() {
        return method.getName();
    }

    @Override
    public CharSequence getInsertPrefix() {
        return method.getName();
    }

}
