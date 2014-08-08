/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.lexer;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;


/**
 *
 * @author shannah
 */
public class CodeHintsTaskFactory extends TaskFactory {

    private static final Logger LOG = Logger.getLogger(CodeHintsTask.class.getCanonicalName());
    
    
    public CodeHintsTaskFactory(){
        
    }
    @Override
    public Collection<? extends SchedulerTask> create(Snapshot snpsht) {
        return Collections.singleton(new CodeHintsTask());
    }
    
}
