/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.editor.indent.spi.Context;
import org.netbeans.modules.editor.indent.spi.IndentTask;

/**
 *
 * @author shannah
 */
@MimeRegistration(mimeType="text/x-mirah",service=IndentTask.Factory.class)
public class MirahIndentTaskFactory implements IndentTask.Factory {

    @Override
    public IndentTask createTask(Context cntxt) {
        return new MirahIndentTask(cntxt);
    }
    
}
