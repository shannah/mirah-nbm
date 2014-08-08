/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah.lexer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import mirah.lang.ast.ClosureDefinition;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Node;
import org.netbeans.modules.editor.indent.api.IndentUtils;

import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shannah
 */
class CodeHintsTask extends ParserResultTask {

    private static final Logger LOG = Logger.getLogger(CodeHintsTask.class.getCanonicalName());

    private List<ErrorDescription> errorsOut;
    
    public CodeHintsTask() {
       this.errorsOut = new ArrayList<ErrorDescription>();
    }

   public List<ErrorDescription> getErrors(){
       return errorsOut;
   }
    
    
    @Override
    public void run(Parser.Result t, SchedulerEvent se) {
        
        FileObject sourceFile = t.getSnapshot().getSource().getFileObject();
        Document doc = t.getSnapshot().getSource().getDocument(false);
        SourceQuery q = new SourceQuery(doc);
        //System.out.println("Looking for lambdas..");
        SourceQuery closures = q.findClosures("*");
        List<ErrorDescription> errors = new ArrayList<ErrorDescription>();
        
        
        for ( Node node : closures){
            List<Fix> fixList = new ArrayList<Fix>();
            Set<Method> abstractMethodsToAdd = new HashSet<Method>();
            SourceQuery $ = new SourceQuery(doc, node);
            ClosureDefinition closure = (ClosureDefinition)node;
            ClassQuery superQuery = null;
            if ( closure.superclass() != null ){
                superQuery = new ClassQuery(closure.superclass().typeref().name(), sourceFile);
            }  else if ( closure.interfaces() != null && closure.interfaces_size() > 0 ){
                superQuery = new ClassQuery(closure.interfaces(0).typeref().name(), sourceFile);
            }
            
            if ( superQuery != null ){
                Set<Method> abstractMethods = superQuery.getAbstractMethods();
                Map<String,Method> methodMap = new HashMap<String,Method>();
                for ( Method m : abstractMethods ){
                    methodMap.put(ClassQuery.getMethodId(m), m);
                }
                SourceQuery implementedMethods = $.findMethods("*");
                for ( Node n : implementedMethods ){
                    MethodDefinition md = (MethodDefinition)n;
                    methodMap.remove(q.getMethodId(md));
                    
                }
                
                if ( !methodMap.isEmpty() ){
                    // There are some abstract methods that need to be implemented
                    abstractMethodsToAdd.addAll(methodMap.values());
                    
                    ClosureAbstractMethodsFix fix = new ClosureAbstractMethodsFix();
                    fix.doc = doc;
                    fix.closure = closure;
                    fix.methods = abstractMethodsToAdd;
                    
                    fixList.add(fix);
                    ErrorDescription errorDescription =  ErrorDescriptionFactory.createErrorDescription(
                        Severity.HINT,
                        "Implement Abstract Methods",
                        fixList,
                        doc,
                        closure.position().startLine()
                    );

                    errors.add(errorDescription);
                }
                
            }   
        }
        
        if ( !errors.isEmpty()){
            errorsOut.addAll(errors);
            //HintsController.setErrors(doc, "mirah", errors);
        }
        
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    @Override
    public void cancel() {

    }
    
    
  private static class ClosureAbstractMethodsFix implements Fix {

        Document doc;
        ClosureDefinition closure;
        Set<Method> methods;
        
      
        @Override
        public String getText() {
            return "Implement abstract methods";
        }

        @Override
        public ChangeInfo implement() throws Exception {
            //System.out.println("IMplementing fix....");
            //System.out.println(methods);
            DocumentQuery q = new DocumentQuery(doc);
            int indent = q.getIndent(closure.position().startChar());
            //System.out.println("Indent is "+indent);
            
            int indentSize = IndentUtils.indentLevelSize(doc);
            indent+=indentSize;
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            CodeFormatter fmt = new CodeFormatter();
            Set<String> requiredImports = new HashSet<String>();
            for ( Method m : methods ){
                //System.out.println("Formatting method "+m);
                //System.out.println("Formatted "+fmt.formatMethod(m, indent, indentSize));
                sb.append(fmt.formatMethod(m, indent, indentSize));
                requiredImports.addAll(fmt.getRequiredImports(m));
            }
            //System.out.println(sb.toString());
            //System.out.println("Looking for next do from "+closure.position().startChar());
            int nextDoPos = q.getAfterNextDo(closure.position().startChar());
            //System.out.println("Next do pos is "+nextDoPos);
            if ( nextDoPos != -1 ){
                doc.insertString(nextDoPos, sb.toString(), new SimpleAttributeSet());
            }
            
            for ( String importCls : requiredImports ){
                q.addImport(importCls);
            }
            
            return null;
            
            
            
        }
      
  }
    
    

}
