/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.cc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.Action;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.queries.JavadocForBinaryQuery;
import org.netbeans.spi.editor.completion.CompletionDocumentation;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author shannah
 */
public class MethodCompletionDocumentation implements CompletionDocumentation {

    private MirahMethodCompletionItem item;
    private URL url;
    private boolean urlFound=false;
    private String text;
    
    
    public MethodCompletionDocumentation(MirahMethodCompletionItem item){
        this.item = item;
        
        
    }
    
    
    @Override
    public String getText() {
        URL url = getURL();
        if ( url == null ){
            return null;
        }
        if ( text == null ){
            text = HTMLJavadocParser.getJavadocText(
                    url,
                    false,
                    false);
        }
        return text;
        
    }

    
    private String getURLText(URL url){
        return HTMLJavadocParser.getJavadocText(
                    url,
                    false,
                    false);
        
    }
    
    /**
     * This is more a less the reverse function for Class.getName()
     */
    private static String decodeTypes(final String encodedType) {
        String DELIMITER = ",";

        DELIMITER = DELIMITER + "%20";
       

        StringBuilder sb = new StringBuilder("");
        boolean nextIsAnArray = false;

        for (int i = 0; i < encodedType.length(); i++) {
            char c = encodedType.charAt(i);

            if (c == '[') {
                nextIsAnArray = true;
                continue;
            } else if (c == 'Z') {
                sb.append("boolean");
            } else if (c == 'B') {
                sb.append("byte");
            } else if (c == 'C') {
                sb.append("char");
            } else if (c == 'D') {
                sb.append("double");
            } else if (c == 'F') {
                sb.append("float");
            } else if (c == 'I') {
                sb.append("int");
            } else if (c == 'J') {
                sb.append("long");
            } else if (c == 'S') {
                sb.append("short");
            } else if (c == 'L') { // special case reference
                i++;
                int semicolon = encodedType.indexOf(";", i);
                String typeName = encodedType.substring(i, semicolon);
                typeName = typeName.replace('/', '.');

                sb.append(typeName);
               

                i = semicolon;
            }

            if (nextIsAnArray) {
                sb.append("[]");
                nextIsAnArray = false;
            }

            if (i < encodedType.length() - 1) {
                sb.append(DELIMITER);
            }
        }
        return sb.toString();
    }

    
    private static String getMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName());
        sb.append("(");
        Class[] types = method.getParameterTypes();
        for (int i=0; i<types.length; i++){
            sb.append(types[i].getName());
            if ( i < types.length -1){
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
        

    }

    
    
    @Override
    public URL getURL() {
        if ( !urlFound ){
            urlFound = true;
            
            String className = item.getMethod().getDeclaringClass().getName();
            String classNamePath = className.replace(".", "/");

            ClassPath[] classPaths = new ClassPath[]{
                ClassPath.getClassPath(item.getFile(), ClassPath.SOURCE),
                ClassPath.getClassPath(item.getFile(), ClassPath.EXECUTE),
                ClassPath.getClassPath(item.getFile(), ClassPath.COMPILE),
                ClassPath.getClassPath(item.getFile(), ClassPath.BOOT)
            };

            String classFilePath = classNamePath+".class";
            //FileObject fo = null;
            //for ( ClassPath cp : classPaths ){
            //    fo = cp.findResource(classFilePath);
            //    if ( fo != null ){
            //        break;
            //    }
            //}

            //if ( fo == null ){
            //    return null;
            //}
            String javadocClassPath = classNamePath + ".html#"+getMethodSignature(item.getMethod()); // NOI18N
            URL htmlFile = null;
            for ( ClassPath cp : classPaths){
                for ( FileObject rootFO : cp.getRoots()){
                    for ( URL jdocRoot : JavadocForBinaryQuery.findJavadoc(rootFO.toURL()).getRoots()){
                        try {
                            htmlFile = new URL(jdocRoot + javadocClassPath);
                            InputStream is = null;
                            try {
                                is = htmlFile.openStream();
                            } catch (IOException ex){
                                htmlFile = null;
                            } finally {
                                try {
                                    is.close();
                                } catch ( Exception ex){}
                            }
                        } catch (MalformedURLException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                        if ( htmlFile != null ){
                            break;
                        }
                    }
                    if ( htmlFile != null ){
                        break;
                    }

                }
                if ( htmlFile != null ){
                    break;
                }
            }
            if ( htmlFile == null ){
                return null;
            }
            url = htmlFile;
            if ( url != null && getURLText(url) == null ){
                url = null;
            }
        }
        
        return url;
        
        
    }

    @Override
    public CompletionDocumentation resolveLink(String string) {
        return this;
    }

    @Override
    public Action getGotoSourceAction() {
        return null;
    }
    
}
