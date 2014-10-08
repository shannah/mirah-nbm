/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah;

import ca.weblite.netbeans.mirah.lexer.MirahParser.NBMirahParserResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.StructureItem;
import org.netbeans.modules.csl.api.StructureScanner;
import org.netbeans.modules.csl.spi.ParserResult;

/**
 *
 * @author shannah
 */
public class MirahStructureAnalyzer implements StructureScanner {

    @Override
    public List<? extends StructureItem> scan(ParserResult pr) {
        NBMirahParserResult res = (NBMirahParserResult)pr;
        
        ArrayList<StructureItem> out = new ArrayList<StructureItem>();
        for ( NBMirahParserResult.Block block : res.getBlocks()){
            out.add(new MirahStructureItem(res.getSnapshot(), block));
        }
        return out;
    }

    @Override
    public Map<String, List<OffsetRange>> folds(ParserResult pr) {
        Map<String,List<OffsetRange>> out =  new HashMap<String,List<OffsetRange>>();
        NBMirahParserResult res = (NBMirahParserResult)pr;
        ArrayList<OffsetRange> ranges = new ArrayList<OffsetRange>();
        for ( NBMirahParserResult.Block block : res.getBlocks()){
            ranges.add(new OffsetRange(block.getOffset(), block.getOffset()+block.getLength()));
        }
        out.put("codeblocks", ranges);
        return out;
        
    }

    @Override
    public Configuration getConfiguration() {
        return new Configuration(true, true);
    }
    
}
