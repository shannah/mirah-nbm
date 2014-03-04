/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.lexer;

import java.util.Collection;
import java.util.logging.Logger;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.ParserFactory;

/**
 *
 * @author shannah
 */
public class MirahParserFactory extends ParserFactory{
    private static final Logger LOG = Logger.getLogger(MirahParserFactory.class.getCanonicalName());
    
    public MirahParserFactory(){
        
    }
    
    @Override
    public Parser createParser(Collection<Snapshot> clctn) {
        return new MirahParser();
    }
    
}
