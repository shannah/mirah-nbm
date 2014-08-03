/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah.cc;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import mirah.lang.ast.FieldDeclaration;
import mirah.lang.ast.Node;
import mirah.lang.ast.NodeFilter;
import mirah.lang.ast.NodeVisitor;
import mirah.lang.ast.SimpleNodeVisitor;
import mirah.lang.ast.SimpleString;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.lib.editor.codetemplates.api.CodeTemplate;
import org.netbeans.lib.editor.codetemplates.api.CodeTemplateManager;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;

/**
 *
 * @author shannah
 */
public class MirahPropertyCompletionItem implements CompletionItem {

    private int caretOffset;
    private int length;
    private FieldDeclaration property;
    private static ImageIcon fieldIcon = new ImageIcon(ImageUtilities.loadImage("ca/weblite/netbeans/mirah/1391571312_application-x-ruby.png"));
    private static Color fieldColor = Color.decode("0x0000B2");

    public MirahPropertyCompletionItem(FieldDeclaration m, int caretOffset, int len) {
        this.property = m;
        this.caretOffset = caretOffset;
        this.length = len;
        
    }

    private String getFieldName(){
        
        return this.property.name().identifier();
       
        //return "Some field";  
    }
    
    @Override
    public void defaultAction(JTextComponent jtc) {
        CodeTemplateManager mgr = CodeTemplateManager.get(jtc.getDocument());
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(getFieldName());
        /*
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
                */
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
    
    
    @Override
    public void processKeyEvent(KeyEvent ke) {

    }

    @Override
    public int getPreferredWidth(Graphics graphics, Font font) {
        return CompletionUtilities.getPreferredWidth("@"+getFieldName(), null, graphics, font);
    }

    @Override
    public void render(Graphics g, Font font, Color color, Color color1, int width, int height, boolean selected) {
        CompletionUtilities.renderHtml(fieldIcon, "@"+getFieldName(), null, g, font,
                (selected ? Color.white : fieldColor), width, height, selected);
    }

    @Override
    public CompletionTask createDocumentationTask() {
        return null;
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
        return getFieldName();
    }

    @Override
    public CharSequence getInsertPrefix() {
        return getFieldName();
    }

}
