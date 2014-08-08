/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah.cc;

import ca.weblite.netbeans.mirah.lexer.DocumentQuery;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;
import mirah.impl.Tokens;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.lib.editor.codetemplates.api.CodeTemplate;
import org.netbeans.lib.editor.codetemplates.api.CodeTemplateManager;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;

/**
 *
 * @author shannah
 */
public class MirahDefCompletionItem implements CompletionItem {

    
    
    private int caretOffset;
    private int length;
    private Method method;
    private static ImageIcon fieldIcon = new ImageIcon(ImageUtilities.loadImage("ca/weblite/netbeans/mirah/1391571312_application-x-ruby.png"));
    private static Color fieldColor = Color.decode("0x0000B2");

    public MirahDefCompletionItem(Method m, int caretOffset, int len) {
        this.method = m;
        this.caretOffset = caretOffset;
        this.length = len;
        
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
        
        try {
            ((BaseDocument)doc).readLock();
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
        } finally {
            ((BaseDocument)doc).readUnlock();
        }
    }
    
    public List<String> getImports(Document doc) throws Exception {
        DocumentQuery q = new DocumentQuery(doc);
        return q.getImports();
       
    }
    
    public void addImport(Document doc, String fullClassName) throws Exception {
            
            //Document doc = source.getDocument(true);
            TokenHierarchy<?> hi = TokenHierarchy.get(doc);
            int caretOffset = 0;
            TokenSequence<MirahTokenId> seq = mirahTokenSequence(doc, caretOffset, false);
            
            // Find the first package or import and place the import after that.
            MirahTokenId PKG = MirahTokenId.get(Tokens.tPackage.ordinal());
            MirahTokenId IMPORT = MirahTokenId.get(Tokens.tImport.ordinal());
            MirahTokenId EOL = MirahTokenId.get(Tokens.tNL.ordinal());
            
            
            
            int pos = 0;
            Token pkg = null;
            Token firstImport = null;
            
            do {
                Token curr = seq.token();
                MirahTokenId currTok = (MirahTokenId)curr.id();
                if ( PKG.equals(currTok) ){
                    pkg = curr;
                } else if ( IMPORT.equals(currTok)){
                    firstImport = curr;
                }
                
            } while ( seq.moveNext());
            
            
            if ( firstImport != null ){
                
                doc.insertString(firstImport.offset(hi), "import "+fullClassName+"\n", new SimpleAttributeSet());
            } else if ( pkg != null ){
                seq.move(pkg.offset(hi));
                while ( seq.moveNext() ){
                    if ( EOL.equals(seq.token().id())){
                        doc.insertString(seq.token().offset(hi), "\nimport "+fullClassName+"\n", new SimpleAttributeSet());
                        break;
                    }
                }
            } else {
                doc.insertString(0, "import "+fullClassName+"\n", new SimpleAttributeSet());
            }
            
            
            
        }
    
    
    @Override
    public void defaultAction(JTextComponent jtc) {
        CodeTemplateManager mgr = CodeTemplateManager.get(jtc.getDocument());
        
        CodeTemplate tpl = mgr.createTemporary(formatMethod(method));
        
        
        try {
            StyledDocument doc = (StyledDocument) jtc.getDocument();
            if ( length > 0 ){
                doc.remove(caretOffset, length);
            }
            tpl.insert(jtc);
            
            // Now to add the imports.
            
            List primitives = Arrays.asList(new String[]{
                "boolean","int","short","long","char","float","double"
            });
            
            
            Set<String> classNames = new HashSet<String>();
            for ( Class paramType : method.getParameterTypes()){
                if ( paramType.getName().startsWith("java.lang.")){
                    continue;
                }
                
                classNames.add(paramType.getName());
            }
            
            Class returnType = method.getReturnType();
            if ( !returnType.getName().startsWith("java.lang.")){
                classNames.add(returnType.getName());
            }
            try {
                classNames.removeAll(getImports(doc));
                
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
            classNames.removeAll(primitives);
           
            if ( !classNames.isEmpty()){
                for ( String className : classNames ){
                    try {
                        addImport(doc, className);
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
            
            
            //doc.insertString(caretOffset, tpl., null);
            //This statement will close the code completion box:
            Completion.get().hideAll();
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    private String formatMethod(Method m){
        StringBuilder sb = new StringBuilder();
       if ( Modifier.isPrivate(m.getModifiers()) ){
           sb.append("private ");
       } else if (  Modifier.isProtected(m.getModifiers()) ){
           sb.append("protected ");
       } else if ( Modifier.isPublic(m.getModifiers()) ){
           // No need to prepend any modifier for public
       } else {
           sb.append("package_private ");
       }
       
       sb.append("def ");
       sb.append(m.getName());
       Class[] args = m.getParameterTypes();
       if ( args.length > 0 ){
           sb.append("(");
       }
       for ( int i=0; i<args.length; i++){
           sb
                   .append("arg")
                   .append(i)
                   .append(":")
                   .append(args[i].getSimpleName());
           if ( i < args.length-1 ){
               sb.append(",");
           } else {
               sb.append(")");
           }
       }
       sb.append(" : ");
       sb.append(m.getReturnType().getSimpleName());
       
       return sb.toString();
        
        
    }
    
    private String formatMethodForList(Method m){
       StringBuilder sb = new StringBuilder();
       if ( Modifier.isPrivate(m.getModifiers()) ){
           sb.append("private ");
       } else if (  Modifier.isProtected(m.getModifiers()) ){
           sb.append("protected ");
       } else if ( Modifier.isPublic(m.getModifiers()) ){
           // No need to prepend any modifier for public
       } else {
           sb.append("package_private ");
       }
       sb.append(m.getName());
       Class[] args = m.getParameterTypes();
       if ( args.length > 0 ){
           sb.append("(");
       }
       for ( int i=0; i<args.length; i++){
           sb
                   .append("arg")
                   .append(i)
                   .append(":")
                   .append(args[i].getSimpleName());
           if ( i < args.length-1 ){
               sb.append(",");
           } else {
               sb.append(")");
           }
       }
       sb.append(" : ");
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
        return method.getName();
    }

    @Override
    public CharSequence getInsertPrefix() {
        return method.getName();
    }

}
