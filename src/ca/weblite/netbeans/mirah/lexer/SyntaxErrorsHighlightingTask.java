/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah.lexer;

import ca.weblite.netbeans.mirah.ImportFixList;
import ca.weblite.netbeans.mirah.lexer.MirahParser.MirahParseDiagnostics.SyntaxError;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.Document;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;

import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.RequestProcessor;

/**
 *
 * @author shannah
 */
class SyntaxErrorsHighlightingTask extends ParserResultTask {

    private static final Logger LOG = Logger.getLogger(SyntaxErrorsHighlightingTask.class.getCanonicalName());

    public SyntaxErrorsHighlightingTask() {
       
    }

    @Override
    public void run(Parser.Result t, SchedulerEvent se) {
        MirahParser.NBMirahParserResult result = (MirahParser.NBMirahParserResult) t;
        if ( result == null || result.getMirahDiagnostics() == null ){
            return;
        }
        List<SyntaxError> syntaxErrors = result.getMirahDiagnostics().getErrors();
        Snapshot snapshot = result.getSnapshot();
        if ( snapshot == null ){
            return;
        }
        Source source = snapshot.getSource();
        if ( source == null ){
            return;
        }
        
        Document document = source.getDocument(false);
        if ( document == null ){
            return;
        }
        List<ErrorDescription> errors = new ArrayList<ErrorDescription>();
        for (SyntaxError syntaxError : syntaxErrors) {
            String message = syntaxError.message;
            int line =-1;
            try {
                
                if ( syntaxError.position != null ){
                    //System.out.println("Syntax error position is not null: "+syntaxError.position);
                    String[] pieces = syntaxError.position.substring(0, syntaxError.position.lastIndexOf(":")).split(":");
                    //System.out.println(Arrays.toString(pieces));
                    String file = pieces[0];
                    File f = new File(file);
                    
                    // Sometimes the error seems to cite a line in the mirah core source
                    // files instead of the file that was being parsed.  check
                    // for that here
                    if ( source.getFileObject() != null && !source.getFileObject().getName().equals(f.getName()) && !source.getFileObject().getNameExt().equals(f.getName())){
                        //System.out.println(source.getFileObject().getName()+" -- "+f.getName());
                    } else {
                        int idx = pieces.length;
                        while (--idx >0 && line == -1 ){
                            if ( pieces[idx].trim().length() == 0){
                                continue;
                            }
                            try {
                                line = Integer.parseInt(pieces[idx]);
                            } catch (NumberFormatException nfe){}
                            
                        }
                    }
                }  else {
                    //System.out.println("Syntax error position is null");
                }
                if ( line == -1 ){
                    
                    // If we're here then it may be an inference error
                    // Let's look through the document to see if we can find any
                    // class references that don't have an import
                    DocumentQuery q = new DocumentQuery(document);
                    SourceQuery sq = new SourceQuery(document);
                    List<Token<MirahTokenId>> lambdas = q.findLambdaTypes();
                    for ( Token<MirahTokenId> tok : lambdas ){
                        String className = String.valueOf(tok.text());
                        String fqn = sq.getFQN(className, tok.offset(TokenHierarchy.get(document)));
                        if ( q.requiresImport(fqn)){
                            message = "cannot find class "+className;
                        }
                    }
                    
                    List<Token<MirahTokenId>> constants = q.findConstants();
                    for ( Token<MirahTokenId> tok : constants ){
                        String className = String.valueOf(tok.text());
                        String fqn = sq.getFQN(className, tok.offset(TokenHierarchy.get(document)));
                        if ( q.requiresImport(fqn)){
                            message = "cannot find class "+className;
                        }
                    }
                    
                    line = 1;
                }
                
            } catch (NumberFormatException ex){
                ex.printStackTrace();
            }
            
            if (line <= 0) {
                continue;
            }
            
            
            
                    
            ErrorDescription errorDescription = null;
            if ( message.toLowerCase().contains("cannot find class")){
                
                
                
                
               
                //List<Fix> imports = new ArrayList<Fix>();
                Pattern p = Pattern.compile("cannot find class ([a-zA-Z][a-zA-Z0-9\\.\\$]*)", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(message);
                
                if ( m.find() ){
                    
                    String className = m.group(1);
                    if ( className.indexOf(".") != -1 ){
                        int pos = className.lastIndexOf(".");
                        if ( pos < className.length()-1 ){
                            pos++;
                        }
                        className = className.substring(pos);
                        
                    }
                    
                    ImportFixList importFixes = new ImportFixList(source, className);
                    errorDescription = ErrorDescriptionFactory.createErrorDescription(
                        Severity.ERROR,
                        message,
                        importFixes,
                        document,
                        line
                    );

                    RequestProcessor rp = new RequestProcessor(SyntaxErrorsHighlightingTask.class);
                    
                    rp.submit(importFixes);
                    

                }
            }
                
            if ( errorDescription == null ){
                errorDescription = ErrorDescriptionFactory.createErrorDescription(
                        Severity.ERROR,
                        message, 
                        document,
                        line);
            }
            
            errors.add(errorDescription);
        }
        
        CodeHintsTask codeHints = new CodeHintsTask();
        codeHints.run(t, se);
        errors.addAll(codeHints.getErrors());
        HintsController.setErrors(document, "mirah", errors);
        
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
    
    
    
    

}
