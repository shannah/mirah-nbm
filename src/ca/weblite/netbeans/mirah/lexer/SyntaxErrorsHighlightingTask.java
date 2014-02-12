/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.netbeans.mirah.lexer;

import ca.weblite.netbeans.mirah.lexer.MirahParser.MirahParseDiagnostics.SyntaxError;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.text.Document;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;

/**
 *
 * @author shannah
 */
class SyntaxErrorsHighlightingTask extends ParserResultTask {

    private static final Logger LOG = Logger.getLogger(SyntaxErrorsHighlightingTask.class.getCanonicalName());

    public SyntaxErrorsHighlightingTask() {
        LOG.warning("In SyntaxErrorsHighlightingTask constructor");
    }

    @Override
    public void run(Parser.Result t, SchedulerEvent se) {
        MirahParser.NBMirahParserResult result = (MirahParser.NBMirahParserResult) t;
        List<SyntaxError> syntaxErrors = result.getDiagnostics().getErrors();
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
                LOG.warning("About to parse line from ["+syntaxError.position+"]");
                String[] pieces = syntaxError.position.substring(0, syntaxError.position.lastIndexOf(":")).split(":");
                
                line = Integer.parseInt(pieces[pieces.length-1]);
                LOG.warning("Parse error found on line "+line);
            } catch (NumberFormatException ex){
                ex.printStackTrace();
            }
            
            if (line <= 0) {
                continue;
            }
            ErrorDescription errorDescription = ErrorDescriptionFactory.createErrorDescription(
                    Severity.ERROR,
                    message,
                    document,
                    line);
            errors.add(errorDescription);
        }
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
