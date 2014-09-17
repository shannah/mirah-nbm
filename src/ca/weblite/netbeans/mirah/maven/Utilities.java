/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.maven;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shannah
 */
public class Utilities {
    public static void performPOMModelOperations(FileObject pom, List<ModelOperation> operations) throws XmlPullParserException, IOException{
        InputStream is = null;
        Model model = null;
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            is = pom.getInputStream();
            model = reader.read(is);
        } finally {
            try {
                if ( is != null ){
                    is.close();
                }
            } catch ( Throwable t){}
        }
        
        if ( model == null ){
            throw new IOException("Failed to read model.");
        }
        for ( ModelOperation o : operations ){
            o.performOperation(model);
        }
        OutputStream os = null;
        MavenXpp3Writer writer = new MavenXpp3Writer();
        
        try {
            os = pom.getOutputStream();
            writer.write(os, model);
        } finally {
            try {
                if ( os != null ){
                    os.close();
                }
            } catch ( Exception ex){}
        }
        
        
        
    }
}
