/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah;


import ca.weblite.netbeans.mirah.support.api.MirahExtender;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.spi.editor.document.OnSaveTask;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shannah
 */
public class MirahOnSaveTask implements OnSaveTask {

    Context context;
    
    private MirahOnSaveTask(Context context){
        this.context = context;
    }
    
    @Override
    public void performTask() {
        
        FileObject fo = NbEditorUtilities.getFileObject(context.getDocument());
        Project proj = FileOwnerQuery.getOwner(fo);
        
         if (!MirahExtender.isActive(proj)) {
            MirahExtender.activate(proj);
        }
        System.out.println("About to check if mirah is current");
        
        if (!MirahExtender.isCurrent(proj)){
            System.out.println("Mirah is not current");
            MirahExtender.update(proj);
        }
    }

    @Override
    public void runLocked(Runnable r) {
        performTask();
    }

    @Override
    public boolean cancel() {
        System.out.println("Cancelling....");
        return false;
    }
    
    
    @MimeRegistration(mimeType="text/x-mirah", service=OnSaveTask.Factory.class, position=1500)
    public static class Factory implements OnSaveTask.Factory {

        @Override
        public OnSaveTask createTask(Context cntxt) {
            
            return new MirahOnSaveTask(cntxt);
        }
        
    }
    
   
    
    
}
