/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah;

import ca.weblite.netbeans.mirah.lexer.MirahParser.NBMirahParserResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.ImageIcon;
import org.netbeans.modules.csl.api.ElementHandle;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.HtmlFormatter;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.StructureItem;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shannah
 */
public class MirahStructureItem implements StructureItem {

    Snapshot snapshot;
    NBMirahParserResult.Block item;
    List<MirahStructureItem> children = null;
    
    public MirahStructureItem(Snapshot snapshot, NBMirahParserResult.Block item){
        this.snapshot = snapshot;
        this.item = item;
    }
    
    @Override
    public String getName() {
        return ""+item.getDescription();
    }

    @Override
    public String getSortText() {
        return getName();
    }

    @Override
    public String getHtml(HtmlFormatter hf) {
        return getName();
    }

    @Override
    public ElementHandle getElementHandle() {
        return new MirahElementHandle(item, snapshot);
    }

    @Override
    public ElementKind getKind() {
        return item.getKind();
    }

    @Override
    public Set<Modifier> getModifiers() {
        return Collections.emptySet();
    }

    @Override
    public boolean isLeaf() {
        return getNestedItems().isEmpty();
    }

    @Override
    public List<? extends StructureItem> getNestedItems() {
        if ( children == null ){
            children = new ArrayList<MirahStructureItem>();
            for (NBMirahParserResult.Block child : item.getChildren()){
                children.add(new MirahStructureItem(snapshot, child));
            }
        }
        return children;
    }

    @Override
    public long getPosition() {
        return item.getOffset();
    }

    @Override
    public long getEndPosition() {
        return item.getOffset()+item.getLength();
    }

    @Override
    public ImageIcon getCustomIcon() {
        return null;
    }
    
    
    class MirahElementHandle implements ElementHandle {

        Snapshot snapshot;
        NBMirahParserResult.Block item;

        private MirahElementHandle(NBMirahParserResult.Block item, Snapshot snapshot) {
            this.snapshot = snapshot;
            this.item = item;
        }
        
        @Override
        public FileObject getFileObject() {
            return snapshot.getSource().getFileObject();
        }

        @Override
        public String getMimeType() {
            return "text/x-mirah";
        }

        @Override
        public String getName() {
            return ""+item.getDescription();
        }

        @Override
        public String getIn() {
            return getName();
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.METHOD;
        }

        @Override
        public Set<Modifier> getModifiers() {
            return Collections.emptySet();
        }

        @Override
        public boolean signatureEquals(ElementHandle eh) {
            if ( !(eh instanceof MirahElementHandle) ) return false;
            if ( eh.getName().equals(this.getName()) ) return true;
            return false;
        }

        @Override
        public OffsetRange getOffsetRange(ParserResult pr) {
            return new OffsetRange( item.getOffset(), item.getOffset() + item.getLength() );
        }
        
    }
}
