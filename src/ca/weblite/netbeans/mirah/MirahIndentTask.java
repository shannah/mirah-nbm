package ca.weblite.netbeans.mirah;



import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.StyledDocument;
import mirah.impl.Tokens;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.editor.indent.api.IndentUtils;
import org.netbeans.modules.editor.indent.spi.Context;
import org.netbeans.modules.editor.indent.spi.ExtraLock;
import org.netbeans.modules.editor.indent.spi.IndentTask;
import ca.weblite.netbeans.mirah.lexer.DocumentQuery;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author shannah
 */
public class MirahIndentTask implements IndentTask  {
    private Context context;
    private static final Logger LOG = 
            Logger.getLogger(MirahIndentTask.class.getCanonicalName());
    public MirahIndentTask(Context ctx){
        this.context = ctx;
        
    }
    
    @Override
    public void reindent() throws BadLocationException {
        //System.out.println("Reindenting");
        /*
        1. Single Indent This Line
            - Last line is def, if, else, elsif,  begin, do, rescue, class, 
            interface
        2. Double Indent This Line
            - Last line is hanging def, if, elsif, rescue, class, or enterface
        3. Single un-indent last line
            - Last line was a finished double-indent from previous hanging def, 
                if, elsif, rescue, class or interface
            - Last line was rescue, end, or ensure and was -- change indent to 
                match opener
        4. 
        
        
        */
        if ( context.startOffset() <= 0 ){
            return;
        }
        int indentSize = IndentUtils.indentLevelSize(context.document());
        
        int prevLineStart = context.lineStartOffset(context.startOffset()-1);
        int prevIndent = context.lineIndent(prevLineStart);
        int currLineStart = context.lineStartOffset(context.startOffset());
        int currLineEnd = currLineStart;
        while ( currLineEnd < context.document().getLength() && 
                !"\n".equals(context.document().getText(currLineEnd, 1))){
            currLineEnd++;
        }
        int prevLineLen = currLineStart - prevLineStart-1;
        if ( prevLineLen < 0){
            prevLineLen = 0;
        }
        
        int prevLineEnd = prevLineStart+prevLineLen;
        
        int currLineLen = currLineEnd-currLineStart;
        if ( currLineLen < 0 ){
            currLineLen = 0;
        }
        
        javax.swing.text.Element paragraph = 
                ((StyledDocument)context.document()).
                        getParagraphElement(currLineStart);
        javax.swing.text.Element lastParagraph = 
                ((StyledDocument)context.document()).
                        getParagraphElement(prevLineStart);
        
        int indent = prevIndent;
        
        DocumentQuery dq = new DocumentQuery(context.document());
        
        TokenSequence<MirahTokenId> seq = dq.getTokens(prevLineStart, false);
        
        // First  non-white token
        MirahTokenId firstNonWhiteToken = null;
        int firstNonWhiteOffset = -1;
        while ( seq.offset() < prevLineEnd && seq.token() != null){
            MirahTokenId curr = seq.token().id();
            if ( !MirahTokenId.WHITESPACE_AND_COMMENTS.contains(curr)){
                firstNonWhiteToken = curr;
                firstNonWhiteOffset = seq.offset();
                break;
            }
            seq.moveNext();
        }
        
        
        Set<MirahTokenId> conditionals = MirahTokenId.set(
                Tokens.tIf,
                Tokens.tElsif,
                Tokens.tUnless,
                Tokens.tWhile,
                Tokens.tUnless
                
        );
        
        
        Set<MirahTokenId> blockBeginTokens = MirahTokenId.set(
                Tokens.tClass,
                Tokens.tInterface,
                Tokens.tDef,
                Tokens.tIf,
                Tokens.tBegin,
                Tokens.tBEGIN,
                Tokens.tUnless,
                Tokens.tWhile
                
        );
        
        Set<MirahTokenId> ifOrBegin = MirahTokenId.set(
                Tokens.tIf,
                Tokens.tUnless,
                Tokens.tBegin,
                Tokens.tBEGIN,
                Tokens.tWhile
        );
        
        MirahTokenId lastIfOrBeginToken = null;
        
        int lastIfOrBeginPos = -1;
        int prevPos = seq.offset();
        while ( seq.offset() < prevLineEnd && seq.token() != null){
            MirahTokenId curr = seq.token().id();
            if ( conditionals.contains(curr)){
                lastIfOrBeginToken = curr;
                lastIfOrBeginPos = seq.offset();
                break;
            }
            seq.moveNext();
        }
        MirahTokenId lastIfOrBeginPrefixToken = null;
        if ( lastIfOrBeginPos != -1 ){
            seq.move(lastIfOrBeginPos);
            while ( seq.movePrevious() && seq.offset() >= prevLineStart){
                if ( !MirahTokenId.
                        WHITESPACE_AND_COMMENTS.contains(seq.token().id())){
                    lastIfOrBeginPrefixToken = seq.token().id();
                    break;
                }
            }
        }
        
        Set<MirahTokenId> assignmentTokens = MirahTokenId.set(
                Tokens.tOpAssign,
                Tokens.tOrEq,
                Tokens.tAndEq,
                Tokens.tPipes,
                Tokens.tPlus,
                Tokens.tMinus,
                Tokens.tEEEQ,
                Tokens.tEEQ,
                Tokens.tGE,
                Tokens.tLT,
                Tokens.tIn,
                Tokens.tGT,
                Tokens.tLE,
                Tokens.tAmpers,
                Tokens.tNE,
                Tokens.tQuestion,
                Tokens.tEQ
        );
        
        if ( lastIfOrBeginPrefixToken != null &&
                assignmentTokens.contains(lastIfOrBeginPrefixToken)){
            firstNonWhiteToken = lastIfOrBeginToken;
            seq.move(lastIfOrBeginPos-1);
            seq.moveNext();
        }
        
        // If the first non-white token of the line is a "begin" token like "if"
        // or "class", then we will do an indent on the next line, as long
        // as the begin isn't ended on the same line.
        if ( firstNonWhiteToken != null && 
                blockBeginTokens.contains(firstNonWhiteToken)){
            // This is a def
            int parenMatch = 0;
            int brackMatch = 0;
            int braceMatch = 0;
            int saveOffset = seq.offset();
            MirahTokenId lastNonWhiteToken = null;
            while ( seq.offset() < prevLineEnd && seq.token() != null ){
                int ordinal = seq.token().id().ordinal();
                if ( ordinal == Tokens.tLBrace.ordinal()){
                    braceMatch++;
                } else if ( ordinal == Tokens.tLBrack.ordinal()){
                    brackMatch++;
                } else if ( ordinal == Tokens.tLParen.ordinal()){
                    parenMatch++;
                } else if ( ordinal == Tokens.tRBrace.ordinal()){
                    braceMatch--;
                } else if ( ordinal == Tokens.tRBrack.ordinal()){
                    brackMatch--;
                } else if ( ordinal == Tokens.tRParen.ordinal()){
                    parenMatch--;
                }
                
                if ( !MirahTokenId.WHITESPACE_AND_COMMENTS.
                        contains(seq.token().id())){
                    lastNonWhiteToken = seq.token().id();
                }
                seq.moveNext();
            }
            
            Set<MirahTokenId> continuers = MirahTokenId.CONTINUATION_TOKENS;
            
            if ( parenMatch > 0 || brackMatch > 0 || braceMatch > 0 || 
                    continuers.contains(lastNonWhiteToken) ){
                
                indent += indentSize*2;
            } else {
                
                indent += indentSize;
            }
        } else {
            MirahTokenId lastNonWhite = 
                    dq.lastNonWhiteTokenOfLine(prevLineStart);
            Set<MirahTokenId> continuers = MirahTokenId.CONTINUATION_TOKENS;
            if ( !continuers.contains(lastNonWhite)){
                // The last line is not itself a continuation... check
                // if the previous line was.
                javax.swing.text.Element par = 
                        ((StyledDocument)context.document()).
                                getParagraphElement(prevLineStart-1);
                javax.swing.text.Element rootPar = null;
                while ( par != null ){
                    lastNonWhite = 
                            dq.lastNonWhiteTokenOfLine(par.getStartOffset());
                    
                    if ( continuers.contains(lastNonWhite)){
                        rootPar = par;
                        int newOff = par.getStartOffset()-1;
                        if ( newOff >=0 ){
                            par = ((StyledDocument)context.document()).
                                    getParagraphElement(newOff);
                        } else {
                            par = null;
                        }
                    } else {
                        par = null;
                    }
                }
                
                if ( rootPar != null ){
                    // We found the root of the continuation.
                    int rootLineStart = 
                            context.lineStartOffset(rootPar.getStartOffset());
                    int rootLineIndent = 
                            context.lineIndent(rootLineStart);
                    MirahTokenId firstTokInRoot = 
                            dq.firstNonWhiteToken(rootLineStart);
                    if ( blockBeginTokens.contains(firstTokInRoot)){
                        indent = rootLineIndent+indentSize;
                    } else {
                        indent = rootLineIndent;
                    }
                }
            } else {
                // This line is a continuation
                // Check and see if the previous line was also a continuation
                // The last line is not itself a continuation... check
                // if the previous line was.
                javax.swing.text.Element par = 
                        ((StyledDocument)context.document()).
                                getParagraphElement(prevLineStart-1);
                javax.swing.text.Element rootPar = null;
                while ( par != null ){
                    lastNonWhite = 
                            dq.lastNonWhiteTokenOfLine(par.getStartOffset());
                    
                    if ( continuers.contains(lastNonWhite)){
                        rootPar = par;
                        int newOff = par.getStartOffset()-1;
                        if ( newOff >=0 ){
                            par = ((StyledDocument)context.document()).
                                    getParagraphElement(newOff);
                        } else {
                            par = null;
                        }
                    } else {
                        par = null;
                    }
                }
                
                if ( rootPar == null ){
                    // The prev line is the original continuation
                    indent = prevIndent+indentSize;
                }
                
                
            }
        }
        
        
        
       
        Set<MirahTokenId> closers = MirahTokenId.set(
                Tokens.tElse, 
                Tokens.tElsif, 
                Tokens.tWhen, 
                Tokens.tEnd, 
                Tokens.tRescue, 
                Tokens.tEnsure
        );
        seq = dq.getTokens(currLineStart, false);
        
        firstNonWhiteToken = null;
         while ( seq.offset() < currLineEnd ){
            if ( !MirahTokenId.WHITESPACE_AND_COMMENTS.
                    contains(seq.token().id())){
                firstNonWhiteToken = seq.token().id();
                break;
            }
            seq.moveNext();
        }
        
         
        Set<MirahTokenId> ifChildren = 
                MirahTokenId.set(Tokens.tElsif, Tokens.tElse);
        Set<MirahTokenId> exceptionTokens = 
                MirahTokenId.set(Tokens.tRescue, Tokens.tEnsure);
        
         
        if ( firstNonWhiteToken != null && 
                firstNonWhiteToken.ordinal() == Tokens.tEnd.ordinal()){
            Set<MirahTokenId> begins = MirahTokenId.set(
                    Tokens.tDef, 
                    Tokens.tIf, 
                    Tokens.tClass, 
                    Tokens.tInterface, 
                    Tokens.tDo, 
                    Tokens.tBegin,
                    Tokens.tUnless,
                    Tokens.tWhile
            );
            
            int balance = 0;
            while ( seq.offset() > 0 && seq.token() != null ){
                MirahTokenId tok = seq.token().id();
                if ( tok.ordinal() == Tokens.tEnd.ordinal() ){
                    balance++;
                } else if ( begins.contains(tok)){
                    balance--;
                    if ( balance <= 0 ){
                        int off = seq.offset();
                        //if ( off > 0 ) off--;
                        indent = context.lineIndent(
                                context.lineStartOffset(off)
                        );
                        break;
                    }
                }
                seq.movePrevious();
            }
        } else if ( firstNonWhiteToken != null && 
                ifChildren.contains(firstNonWhiteToken)){
            Set<MirahTokenId> begins = MirahTokenId.set(Tokens.tIf);
            
            int balance = 0;
            while ( seq.offset() > 0 && seq.token() != null ){
                MirahTokenId tok = seq.token().id();
                if ( tok.ordinal() == Tokens.tEnd.ordinal() ){
                    balance++;
                } else if ( begins.contains(tok)){
                    balance--;
                    if ( balance < 0 ){
                        indent = context.lineIndent(seq.offset());
                        break;
                    }
                }
                seq.movePrevious();
            }
        } else if ( firstNonWhiteToken != null && 
                exceptionTokens.contains(firstNonWhiteToken)){
            Set<MirahTokenId> begins = MirahTokenId.set(
                    Tokens.tDef, 
                    Tokens.tIf, 
                    Tokens.tClass, 
                    Tokens.tInterface, 
                    Tokens.tDo, 
                    Tokens.tBegin
            );
            
            int balance = 0;
            while ( seq.offset() > 0 && seq.token() != null ){
                MirahTokenId tok = seq.token().id();
                if ( tok.ordinal() == Tokens.tEnd.ordinal() ){
                    balance++;
                } else if ( begins.contains(tok)){
                    balance--;
                    if ( balance < 0 ){
                        indent = context.lineIndent(seq.offset());
                        break;
                    }
                }
                seq.movePrevious();
            }
        }
        
        if ( indent >= 0 ){
            context.modifyIndent(currLineStart, indent);
        }
        
        
        // Now let's modify the indentation of the previous line
        MirahTokenId prevFirstNonWhite = dq.firstNonWhiteToken(prevLineStart);
        Set<MirahTokenId> reorgs = MirahTokenId.set(
                Tokens.tEnd, 
                Tokens.tRescue, 
                Tokens.tEnsure, 
                Tokens.tElse,
                Tokens.tElsif
        );
        Set<MirahTokenId> ifEnders = 
                MirahTokenId.set(Tokens.tElse, Tokens.tElsif);
        Set<MirahTokenId> exceptionEnds = 
                MirahTokenId.set(Tokens.tRescue, Tokens.tEnsure);
        Set<MirahTokenId> ifUnlessTokens = 
                MirahTokenId.set(Tokens.tIf, Tokens.tUnless);
        boolean changePrevLineIndent = false;
        int changePrevLineIndentTo = 0;
        if ( prevFirstNonWhite != null && 
                prevFirstNonWhite.ordinal() == Tokens.tEnd.ordinal()){
            // Let's find the matching line
            int startOff = prevLineStart;
            if ( startOff > 0 ) startOff--;
            seq = dq.getTokens(startOff, false);
            Set<MirahTokenId> openers = MirahTokenId.set(
                    Tokens.tDo, 
                    Tokens.tIf, 
                    Tokens.tDef, 
                    Tokens.tBegin, 
                    Tokens.tClass, 
                    Tokens.tInterface,
                    Tokens.tUnless
            );
            int balance = 0;
            while ( seq.token() != null && seq.offset() > 0 ){
                int ord = seq.token().id().ordinal();
                if ( ord == Tokens.tEnd.ordinal()){
                    balance++;
                } else if ( openers.contains(seq.token().id())){
                    if ( ord == Tokens.tIf.ordinal() ){
                        // If statements don't necessarily have a close block..
                        // only if the "if" is the first nonwhite of the line
                        MirahTokenId firstNonWhiteOfIfLine = 
                                dq.firstNonWhiteToken(
                                        context.lineStartOffset(seq.offset())
                                );
                        if ( firstNonWhiteOfIfLine != null && 
                                firstNonWhiteOfIfLine.ordinal() == 
                                Tokens.tIf.ordinal() ){
                            balance--;
                        } else {
                            // See if the if is prefixed
                            int offset = seq.offset();
                            int bol = context.lineStartOffset(offset);
                            MirahTokenId ifPrefixToken = null;
                            while ( seq.movePrevious() && seq.offset() > bol ){
                                if ( !MirahTokenId.WHITESPACE_AND_COMMENTS.
                                        contains(seq.token().id())){
                                    ifPrefixToken = seq.token().id();
                                    break;
                                }
                            }
                            seq.move(offset+1);
                            seq.movePrevious();
                            
                            if ( ifPrefixToken != null && 
                                    assignmentTokens.contains(ifPrefixToken)){
                                balance--;
                            }
                        }
                    } else {
                        balance--;
                    }
                    if ( balance < 0 ){
                        // We found the line that we need to match it with
                        changePrevLineIndentTo = 
                                context.lineIndent(
                                        context.lineStartOffset(seq.offset())
                                );
                        changePrevLineIndent = true;
                        break;
                        
                    }
                }
                seq.movePrevious();
            }
        } else if ( prevFirstNonWhite != null && 
                ifEnders.contains(prevFirstNonWhite) ){
            int startOff = prevLineStart;
            if ( startOff > 0 ) startOff--;
            seq = dq.getTokens(startOff, false);
            Set<MirahTokenId> openers = MirahTokenId.set(Tokens.tIf, Tokens.tUnless);
            int balance = 0;
            while ( seq.token() != null && seq.offset() > 0 ){
                int ord = seq.token().id().ordinal();
                if ( ord == Tokens.tEnd.ordinal()){
                    balance++;
                } else if ( openers.contains(seq.token().id())){
                    //if ( ord == Tokens.tIf.ordinal() ){
                    // If statements don't necessarily have a close block..
                    // only if the "if" is the first nonwhite of the line
                    MirahTokenId firstNonWhiteOfIfLine = 
                            dq.firstNonWhiteToken(
                                    context.lineStartOffset(seq.offset())
                            );
                    if ( firstNonWhiteOfIfLine != null && 
                            openers.contains(firstNonWhiteOfIfLine) ){
                        balance--;
                    } else {
                        // See if the if is prefixed
                        int offset = seq.offset();
                        int bol = context.lineStartOffset(offset);
                        MirahTokenId ifPrefixToken = null;
                        while ( seq.movePrevious() && seq.offset() > bol ){
                            if ( !MirahTokenId.WHITESPACE_AND_COMMENTS.
                                    contains(seq.token().id())){
                                ifPrefixToken = seq.token().id();
                                break;
                            }
                        }
                        seq.move(offset+1);
                        seq.movePrevious();

                        if ( ifPrefixToken != null && 
                                assignmentTokens.contains(ifPrefixToken)){
                            balance--;
                        }
                    }
                    //} else {
                    //    balance--;
                    //}
                    if ( balance < 0 ){
                        // We found the line that we need to match it with
                        changePrevLineIndentTo = 
                                context.lineIndent(
                                        context.lineStartOffset(seq.offset())
                                );
                        changePrevLineIndent = true;
                        break;
                        
                    }
                }
                seq.movePrevious();
            }
        } else if ( prevFirstNonWhite != null && 
                exceptionEnds.contains(prevFirstNonWhite)){
            int startOff = prevLineStart;
            if ( startOff > 0 ) startOff--;
            seq = dq.getTokens(startOff, false);
            Set<MirahTokenId> openers = MirahTokenId.set(Tokens.tBegin);
            int balance = 0;
            while ( seq.token() != null && seq.offset() > 0 ){
                int ord = seq.token().id().ordinal();
                if ( ord == Tokens.tEnd.ordinal()){
                    balance++;
                } else if ( openers.contains(seq.token().id())){
                    //if ( ord == Tokens.tIf.ordinal() ){
                    // If statements don't necessarily have a close block.. 
                    // only if the "if" is the first nonwhite of the line
                    MirahTokenId firstNonWhiteOfIfLine = 
                            dq.firstNonWhiteToken(
                                    context.lineStartOffset(seq.offset())
                            );
                    if ( firstNonWhiteOfIfLine != null && 
                            openers.contains(firstNonWhiteOfIfLine) ){
                        balance--;
                    } else {
                        // See if the if is prefixed
                        int offset = seq.offset();
                        int bol = context.lineStartOffset(offset);
                        MirahTokenId ifPrefixToken = null;
                        while ( seq.movePrevious() && seq.offset() > bol ){
                            if ( !MirahTokenId.WHITESPACE_AND_COMMENTS.
                                    contains(seq.token().id())){
                                ifPrefixToken = seq.token().id();
                                break;
                            }
                        }
                        seq.move(offset+1);
                        seq.movePrevious();

                        if ( ifPrefixToken != null && 
                                assignmentTokens.contains(ifPrefixToken)){
                            balance--;
                        }
                    }
                    //} else {
                    //    balance--;
                    //}
                    if ( balance < 0 ){
                        // We found the line that we need to match it with
                        changePrevLineIndentTo = context.lineIndent(
                                context.lineStartOffset(seq.offset())
                        );
                        changePrevLineIndent = true;
                        break;
                        
                    }
                }
                seq.movePrevious();
            }
        }
        
        if ( changePrevLineIndent ){
            context.modifyIndent(prevLineStart, changePrevLineIndentTo);
        }
        
        
        
        
        
    }
    
    public void reindent_old() throws BadLocationException {
        
        if ( context.startOffset() <= 0 ){
            return;
        }
        int indentSize = IndentUtils.indentLevelSize(context.document());
        int prevLineStart = context.lineStartOffset(context.startOffset()-1);
        int prevIndent = context.lineIndent(prevLineStart);
        int currLineStart = context.lineStartOffset(context.startOffset());
        int currLineEnd = currLineStart;
        while ( currLineEnd < context.document().getLength() && 
                !"\n".equals(context.document().getText(currLineEnd, 1))){
            currLineEnd++;
        }
        
        
        int prevLineLen = currLineStart - prevLineStart-1;
        if ( prevLineLen < 0){
            prevLineLen = 0;
        }
        
        int currLineLen = currLineEnd-currLineStart;
        if ( currLineLen < 0 ){
            currLineLen = 0;
        }
       
        
        // Two questions:
        // 1. Do we need to adjust the indent of the previous line.
        // 2. Do we need to adjust the indent of the current line.
        
        TokenSequence<MirahTokenId> toks = mirahTokenSequence(
                context.document(), 
                context.caretOffset(), 
                true
        );
        // Check previous line for else
        
        MirahTokenId tElse = MirahTokenId.get(Tokens.tElse.ordinal());
        MirahTokenId tElsIf = MirahTokenId.get(Tokens.tElsif.ordinal());
        MirahTokenId tIf = MirahTokenId.get(Tokens.tIf.ordinal());
        //int index = toks.index();
        int changePrevIndent = -1;
        while ( toks.offset() > prevLineStart ){
            Token<MirahTokenId> curr = toks.token();
            if ( curr.id() == tElse || curr.id() == tElsIf ){
                // We have an else... find the matching if
                Token<MirahTokenId> curr2 = toks.token();
                while ( tIf != curr2.id() && toks.movePrevious() ){
                    curr2 = toks.token();
                }
                if ( tIf == curr2.id() ){
                    int ifStartLineOffset = 
                            context.lineStartOffset(toks.offset());
                    changePrevIndent = context.lineIndent(ifStartLineOffset);
                    break;
                    
                }
            }
            if ( !toks.movePrevious() ){
                break;
            }
        }
        
        Position prevLineStartPos = 
                context.document().createPosition(prevLineStart);
        Position currLineStartPos = 
                context.document().createPosition(currLineStart);
        Position currLineEndPos = 
                context.document().createPosition(currLineEnd);
        prevIndent = context.lineIndent(prevLineStartPos.getOffset());
        if ( changePrevIndent  >= 0  && prevIndent != changePrevIndent){
            context.modifyIndent(
                    prevLineStartPos.getOffset(), 
                    changePrevIndent
            );
        }
        
        
        prevLineStart = prevLineStartPos.getOffset();
        currLineStart = currLineStartPos.getOffset();
        currLineEnd = currLineEndPos.getOffset();
        
        prevLineLen = currLineStart - prevLineStart-1;
        if ( prevLineLen < 0){
            prevLineLen = 0;
        }
        
        currLineLen = currLineEnd-currLineStart;
        if ( currLineLen < 0 ){
            currLineLen = 0;
        }
        
        int indent = prevIndent;
        
        
        MirahTokenId tEnd = MirahTokenId.get(Tokens.tEnd.ordinal());
        Set<MirahTokenId> openers = new HashSet<MirahTokenId>();
        openers.add(MirahTokenId.get(Tokens.tClass.ordinal()));
        openers.add(MirahTokenId.get(Tokens.tBegin.ordinal()));
        openers.add(MirahTokenId.get(Tokens.tIf.ordinal()));
        openers.add(MirahTokenId.get(Tokens.tDo.ordinal()));
        openers.add(MirahTokenId.get(Tokens.tDef.ordinal()));
        openers.add(MirahTokenId.get(Tokens.tInterface.ordinal()));
        
        LinkedList<MirahTokenId> foundOpeners = new LinkedList<MirahTokenId>();
        boolean equalsEncountered = false;
        
        //int numOpeners = 0;
        toks = mirahTokenSequence(context.document(), prevLineStart, false);
        
        while ( toks.offset() < currLineStart ){
            Token<MirahTokenId> tok = toks.token();
            if ( tok.id().ordinal() == Tokens.tEQ.ordinal()){
                equalsEncountered = true;
            }
            
            
            if ( tok.id() == tEnd ){
                //numOpeners--;
                if ( !foundOpeners.isEmpty()){
                    foundOpeners.pop();
                }
            } else if ( openers.contains(tok.id()) &&
                    !(tok.id().ordinal() == Tokens.tIf.ordinal() && 
                    equalsEncountered)
                    ){
                foundOpeners.push(tok.id());
            }
            toks.moveNext(); 
        }
        
        if ( !foundOpeners.isEmpty()){
            indent += foundOpeners.size()*indentSize;
        } 
        
        toks = mirahTokenSequence(
                context.document(), 
                context.caretOffset(), 
                false
        );
        
        
        
        Set<MirahTokenId> closers = new HashSet<MirahTokenId>();
        closers.add(tEnd);
        closers.add(MirahTokenId.get(Tokens.tElse.ordinal()));
        closers.add(MirahTokenId.get(Tokens.tElsif.ordinal()));
        closers.add(MirahTokenId.get(Tokens.tWhen.ordinal()));
        toks.move(currLineStart);
        toks.moveNext();
        while ( toks.offset() < currLineEnd ){
            if ( closers.contains(toks.token().id())){
                indent -= indentSize;
                break;
            }
            toks.moveNext();
        }
        
        if ( indent >= 0 ){
            context.modifyIndent(currLineStart, indent);
        }
        
        
        
        
        
    }
    
    
    
    /**
     * Get token sequence positioned over a token.
     *
     * @param doc
     * @param caretOffset
     * @param backwardBias
     * @return token sequence positioned over a token that "contains" the offset
     * or null if the document does not contain any java token sequence or the
     * offset is at doc-or-section-start-and-bwd-bias or
     * doc-or-section-end-and-fwd-bias.
     */
    private static TokenSequence<MirahTokenId> mirahTokenSequence(
            Document doc, 
            int caretOffset, 
            boolean backwardBias) {
        TokenHierarchy<?> hi = TokenHierarchy.get(doc);
        List<TokenSequence<?>> tsList = 
                hi.embeddedTokenSequences(caretOffset, backwardBias);
        // Go from inner to outer TSes
        for (int i = tsList.size() - 1; i >= 0; i--) {
            TokenSequence<?> ts = tsList.get(i);
            if (ts.languagePath().innerLanguage() == 
                    MirahTokenId.getLanguage()) {
                TokenSequence<MirahTokenId> javaInnerTS = 
                        (TokenSequence<MirahTokenId>) ts;
                return javaInnerTS;
            }
        }
        return null;
    }
    
    
   

    @Override
    public ExtraLock indentLock() {
        return null;
    }
    
    
    
}
