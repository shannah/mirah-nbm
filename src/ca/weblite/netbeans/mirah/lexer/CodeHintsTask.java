/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah.lexer;

import ca.weblite.netbeans.mirah.ClassIndex;
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
import mirah.lang.ast.ClassDefinition;
import mirah.lang.ast.ClosureDefinition;
import mirah.lang.ast.MethodDefinition;
import mirah.lang.ast.Node;
import mirah.lang.ast.Package;
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
        
        processClosures(t, se);
        processClasses(t, se);
    }
    
    public void processClasses(Parser.Result t, SchedulerEvent se){
        FileObject sourceFile = t.getSnapshot().getSource().getFileObject();
        Document doc = t.getSnapshot().getSource().getDocument(false);
        SourceQuery q = new SourceQuery(doc);
        SourceQuery classes = q.findClass(null);
        List<ErrorDescription> errors = new ArrayList<ErrorDescription>();
        DocumentQuery dq = new DocumentQuery(doc);
        for ( Node node : classes){
            List<Fix> fixList = new ArrayList<Fix>();
            SourceQuery $ = new SourceQuery(doc, node);
            ClassDefinition clazz = (ClassDefinition)node;
            String fqn = String.valueOf(clazz.name().identifier());
            String packageName = dq.getPackageName(clazz.position().startChar());
            if ( packageName != null ){
                fqn = packageName+"."+fqn;
            }
            
            ClassQuery clsQuery = new ClassQuery(fqn, sourceFile, false);
            
            
            Set<Method> missingMethods = clsQuery.getUnimplementedMethodsRequiredByInterfaces();
            missingMethods.addAll(clsQuery.getAbstractMethods());
            
            if ( !missingMethods.isEmpty() ){
                
                ClassUnimplementedMethodsFix fix = new ClassUnimplementedMethodsFix();
                fix.doc = doc;
                fix.classDef = clazz;
                fix.methods = missingMethods;

                fixList.add(fix);
                ErrorDescription errorDescription =  ErrorDescriptionFactory.createErrorDescription(
                    Severity.HINT,
                    "Implement Abstract Methods",
                    fixList,
                    doc,
                    clazz.position().startLine()
                );

                errors.add(errorDescription);
            }
                
              
        }
        
        if ( !errors.isEmpty()){
            errorsOut.addAll(errors);
            //HintsController.setErrors(doc, "mirah", errors);
        }
        
    }
    
    
    public void processClosures(Parser.Result t, SchedulerEvent se){
        FileObject sourceFile = t.getSnapshot().getSource().getFileObject();
        Document doc = t.getSnapshot().getSource().getDocument(false);
        SourceQuery q = new SourceQuery(doc);
        SourceQuery closures = q.findClosures("*");
        List<ErrorDescription> errors = new ArrayList<ErrorDescription>();
        
        
        for ( Node node : closures){
            List<Fix> fixList = new ArrayList<Fix>();
            Set<Method> abstractMethodsToAdd = new HashSet<Method>();
            SourceQuery $ = new SourceQuery(doc, node);
            ClosureDefinition closure = (ClosureDefinition)node;
            ClassQuery superQuery = null;
            if ( closure.superclass() != null ){
                superQuery = new ClassQuery(closure.superclass().typeref().name(), sourceFile, true);
            }  else if ( closure.interfaces() != null && closure.interfaces_size() > 0 ){
                superQuery = new ClassQuery(closure.interfaces(0).typeref().name(), sourceFile, true);
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
            DocumentQuery q = new DocumentQuery(doc);
            int indent = q.getIndent(closure.position().startChar());
            int indentSize = IndentUtils.indentLevelSize(doc);
            indent+=indentSize;
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            CodeFormatter fmt = new CodeFormatter();
            fmt.indent(sb, indent);
            Set<String> requiredImports = new HashSet<String>();
            for ( Method m : methods ){
                sb.append(fmt.formatMethod(m, indent, indentSize));
                requiredImports.addAll(fmt.getRequiredImports(m));
            }
            int nextDoPos = q.getAfterNextDo(closure.position().startChar());
            if ( nextDoPos != -1 ){
                doc.insertString(nextDoPos, sb.toString(), new SimpleAttributeSet());
            }
            
            for ( String importCls : requiredImports ){
                q.addImport(importCls);
            }
            
            return null;
            
            
            
        }
      
  }
  
  private static class ClassUnimplementedMethodsFix implements Fix {

        Document doc;
        ClassDefinition classDef;
        Set<Method> methods;
        
      
        @Override
        public String getText() {
            return "Implement abstract methods";
        }

        @Override
        public ChangeInfo implement() throws Exception {
            DocumentQuery q = new DocumentQuery(doc);
            int indent = q.getIndent(classDef.position().startChar());
            int indentSize = IndentUtils.indentLevelSize(doc);
            indent+=indentSize;
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            CodeFormatter fmt = new CodeFormatter();
            fmt.indent(sb, indent);
            Set<String> requiredImports = new HashSet<String>();
            for ( Method m : methods ){
                sb.append(fmt.formatMethod(m, indent, indentSize));
                requiredImports.addAll(fmt.getRequiredImports(m));
            }
            
            int prevEndPos = q.getBeforePrevEnd(classDef.position().endChar());
            if ( prevEndPos != -1 ){
                doc.insertString(prevEndPos, sb.toString(), new SimpleAttributeSet());
            }
            
            for ( String importCls : requiredImports ){
                q.addImport(importCls);
            }
            
            return null;
            
            
            
        }
      
  }
    
    

}
