/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah;

import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;
import mirah.impl.Tokens;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.LazyFixList;
import org.openide.filesystems.FileObject;
import org.openide.util.RequestProcessor;

/**
 *
 * @author shannah
 */
public class ImportFixList  implements LazyFixList, Runnable {
    
    private Source source;
    private String className;
    private PropertyChangeSupport pcs;
    private List<Fix> fixes = new ArrayList<Fix>();
    private boolean computed = false;
    
    public ImportFixList(Source source, String className){
        pcs = new PropertyChangeSupport(this);
        this.source = source;
        this.className = className;
        
        
        
    }
    
    
    

    @Override
    public boolean probablyContainsFixes() {
        synchronized(fixes){
            return !fixes.isEmpty();
        }
    }

    @Override
    public List<Fix> getFixes() {
        synchronized(fixes){
            return Collections.unmodifiableList(fixes);
        }
    }

    @Override
    public boolean isComputed() {
        return computed;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener pl) {
        pcs.addPropertyChangeListener(pl);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener pl) {
        pcs.removePropertyChangeListener(pl);
    }

    @Override
    public void run() {
        System.err.println("In ImportFixList.run()");
        FileObject fo = source.getFileObject();
        ClassIndex idx = new ClassIndex();
        ClassIndex.CompoundQuery q = new ClassIndex.CompoundQuery(0);

        ClassPath[] classPaths = new ClassPath[]{
            ClassPath.getClassPath(fo, ClassPath.SOURCE),
            ClassPath.getClassPath(fo, ClassPath.EXECUTE),
            ClassPath.getClassPath(fo, ClassPath.COMPILE),
            ClassPath.getClassPath(fo, ClassPath.BOOT)
        };
        int priority = 10;
        for ( ClassPath cp : classPaths){
            
            q.addQuery(new ClassIndex.ClassPathQuery(priority--, className, "", cp));
        }

        ClassIndex.Future results = new ClassIndex.Future(){

            @Override
            protected void resultsAdded() {
                synchronized(fixes){
                    fixes.clear();

                }
                Set<String> matches = new HashSet<String>();
                matches.addAll(this.getMatches());
                for ( String match : matches ){
                    Fix fix = new ImportFix(match);
                    synchronized(fixes){
                        fixes.add(fix);
                    }
                    pcs.firePropertyChange(LazyFixList.PROP_FIXES, fixes, fixes);

                }
            }

        };
        
        idx.findClass(q, results);
        
        computed = true;
        pcs.firePropertyChange(LazyFixList.PROP_COMPUTED, false, true);
        
    }
    
    class ImportFix implements Fix{
        private String fullClassName;
        ImportFix(String fqn){
            if ( fqn.indexOf(".") == 0 ){
                fqn = fqn.substring(1);
            }
            this.fullClassName = fqn;
        }

        @Override
        public String getText() {
            return "Add import "+fullClassName;
        }

        @Override
        public ChangeInfo implement() throws Exception {
            
            Document doc = source.getDocument(true);
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
            
            
            return null;
        }
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
    
}
