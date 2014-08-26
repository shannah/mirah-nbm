/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.typinghooks;

import ca.weblite.netbeans.mirah.lexer.DocumentQuery;
import ca.weblite.netbeans.mirah.lexer.MirahLanguageHierarchy;
import ca.weblite.netbeans.mirah.lexer.MirahTokenId;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import mirah.impl.Tokens;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.lexer.PartType;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.lib.editor.util.swing.DocumentUtilities;
import org.netbeans.modules.editor.indent.api.Indent;
import org.netbeans.modules.editor.indent.api.IndentUtils;
import org.netbeans.spi.editor.typinghooks.DeletedTextInterceptor;
import org.netbeans.spi.editor.typinghooks.TypedBreakInterceptor;
import org.netbeans.spi.editor.typinghooks.TypedTextInterceptor;

/**
 *
 * @author shannah
 */
public class MirahTypingCompletion {
   
    private static final Logger LOG = Logger.getLogger(MirahTypingCompletion.class.getCanonicalName());
    /**
     * Returns true if bracket completion is enabled in options.
     */
    static boolean isCompletionSettingEnabled() {
        //Preferences prefs = MimeLookup.getLookup(JavaKit.JAVA_MIME_TYPE).lookup(Preferences.class);
        //return prefs.getBoolean(SimpleValueNames.COMPLETION_PAIR_CHARACTERS, false);
        return true;
    }
    
    private static int tokenBalance(Document doc, MirahTokenId leftTokenId) {
        TokenBalance tb = TokenBalance.get(doc);
        
        MirahTokenId tEnd = MirahTokenId.get(Tokens.tEnd.ordinal());
        
        tb.addTokenPair(MirahTokenId.getLanguage(), MirahTokenId.get(Tokens.tLParen.ordinal()), MirahTokenId.get(Tokens.tRParen.ordinal()));
        tb.addTokenPair(MirahTokenId.getLanguage(), MirahTokenId.get(Tokens.tLBrace.ordinal()), MirahTokenId.get(Tokens.tRBrace.ordinal()));
        tb.addTokenPair(MirahTokenId.getLanguage(), MirahTokenId.get(Tokens.tLBrack.ordinal()), MirahTokenId.get(Tokens.tRBrack.ordinal()));
        tb.addTokenPair(MirahTokenId.getLanguage(), MirahTokenId.get(Tokens.tDo.ordinal()), tEnd);
        tb.addTokenPair(MirahTokenId.getLanguage(), MirahTokenId.get(Tokens.tDef.ordinal()), tEnd);
        tb.addTokenPair(MirahTokenId.getLanguage(), MirahTokenId.get(Tokens.tClass.ordinal()), tEnd);
        tb.addTokenPair(MirahTokenId.getLanguage(), MirahTokenId.get(Tokens.tInterface.ordinal()), tEnd);
        tb.addTokenPair(MirahTokenId.getLanguage(), MirahTokenId.get(Tokens.tIf.ordinal()), tEnd);
        tb.addTokenPair(MirahTokenId.getLanguage(), MirahTokenId.get(Tokens.tWhile.ordinal()), tEnd);
        tb.addTokenPair(MirahTokenId.getLanguage(), MirahTokenId.get(Tokens.tCase.ordinal()), tEnd);
        tb.addTokenPair(MirahTokenId.getLanguage(), MirahTokenId.get(Tokens.tBegin.ordinal()), tEnd);
        //tb.addTokenPair(MirahTokenId.getLanguage(), MirahTokenId.get(Tokens.tDo.ordinal()), MirahTokenId.get(Tokens.tEnd.ordinal()));
        
        
        int balance = tb.balance(MirahTokenId.getLanguage(), leftTokenId);
        assert (balance != Integer.MAX_VALUE);
        
        return balance;
    }
    
    
    
    /**
     * Returns position of the first unpaired closing paren/brace/bracket from the caretOffset
     * till the end of caret row. If there is no such element, position after the last non-white
     * character on the caret row is returned.
     */
    static int getRowOrBlockEnd(BaseDocument doc, int caretOffset, boolean[] insert) throws BadLocationException {
        
        int rowEnd = org.netbeans.editor.Utilities.getRowLastNonWhite(doc, caretOffset);
        if (rowEnd == -1 || caretOffset >= rowEnd+1) {
            return caretOffset;
        }
        rowEnd += 1;
        int parenBalance = 0;
        int braceBalance = 0;
        int bracketBalance = 0;
        TokenSequence<MirahTokenId> ts = mirahTokenSequence(doc, caretOffset, false);
        if (ts == null) {
            return caretOffset;
        }
        
        while (ts.offset() < rowEnd) {
            final int id = ts.token().id().ordinal();
            if ( id == Tokens.tLParen.ordinal()) {
                parenBalance++;
            } else if ( id == Tokens.tRParen.ordinal()){
                parenBalance--;
            } else if ( id == Tokens.tLBrace.ordinal()){
                braceBalance++;
            } else if ( id == Tokens.tRBrace.ordinal()){
                braceBalance--;
            } else if ( id == Tokens.tLBrack.ordinal()){
                bracketBalance++;
            } else if ( id == Tokens.tRBrack.ordinal()){
                bracketBalance--;
            }
            
            if (!ts.moveNext()) {
                break;
            }
        }

        if ( parenBalance > 0 || bracketBalance > 0 || braceBalance > 0 ){
            doc.insertString(rowEnd+1, "\n", null);
            return rowEnd+1;
        }
        return rowEnd;
    }
    
    private static TokenSequence<MirahTokenId> mirahTokenSequence(TypedTextInterceptor.MutableContext context, boolean backwardBias) {
        return mirahTokenSequence(context.getDocument(), context.getOffset(), backwardBias);
    }

    private static TokenSequence<MirahTokenId> mirahTokenSequence(DeletedTextInterceptor.Context context, boolean backwardBias) {
        return mirahTokenSequence(context.getDocument(), context.getOffset(), backwardBias);
    }
    
       private static TokenSequence<MirahTokenId> mirahTokenSequence(TypedBreakInterceptor.Context context, boolean backwardBias) {
        return mirahTokenSequence(context.getDocument(), context.getCaretOffset(), backwardBias);
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
    private static TokenSequence<MirahTokenId> mirahTokenSequence(Document doc, int caretOffset, boolean backwardBias) {
        ((BaseDocument)doc).readLock();
        try {
            TokenHierarchy<?> hi = TokenHierarchy.get(doc);
            List<TokenSequence<?>> tsList = hi.embeddedTokenSequences(caretOffset, backwardBias);
            // Go from inner to outer TSes
            for (int i = tsList.size() - 1; i >= 0; i--) {
                TokenSequence<?> ts = tsList.get(i);
                if (ts.languagePath().innerLanguage() == MirahTokenId.getLanguage()) {
                    TokenSequence<MirahTokenId> javaInnerTS = (TokenSequence<MirahTokenId>) ts;
                    return javaInnerTS;
                }
            }
            return null;
        } finally {
            ((BaseDocument)doc).readUnlock();
        }
    }
    
    
    static boolean isAddEnd(BaseDocument doc, int caretOffset) throws BadLocationException {
        
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
        
        int indentSize = IndentUtils.indentLevelSize(doc);
        DocumentQuery dq = new DocumentQuery(doc);
        if ( caretOffset > 0 ){
            TokenSequence<MirahTokenId> seq = dq.getTokens(caretOffset-1, false);
            if ( DocumentQuery.findPrevious(
                    seq, 
                    null, 
                    null,
                    MirahTokenId.get(Tokens.tNL)
            )){
                int bol = seq.offset();
                seq.moveNext();
                
                // See if the if statement is the first statement on 
                // the line... only then do we add an auto end
                if ( DocumentQuery.findNext(
                        seq,
                        null,//MirahTokenId.WHITESPACE_AND_COMMENTS,
                        MirahTokenId.get(Tokens.tNL),
                        MirahTokenId.get(Tokens.tIf)
                        )){
                    int ifOffset = seq.token().offset(TokenHierarchy.get(doc));
                    
                    MirahTokenId ifPrefixToken = null;
                    int ifPrefixOffset = -1;
                    while ( seq.movePrevious() && seq.offset() > bol ){
                        if ( !MirahTokenId.WHITESPACE_AND_COMMENTS.
                                contains(seq.token().id())){
                            ifPrefixToken = seq.token().id();
                            ifPrefixOffset = seq.offset();
                            break;
                        }
                    }
                    if ( ifPrefixToken != null && 
                            !assignmentTokens.contains(ifPrefixToken)){
                        return false;
                    }
                    
                    int indent = dq.getIndent(ifOffset);
                    //System.out.println("Indent is "+indent);
                    // Look for an end
                    if ( DocumentQuery.findNext( 
                            seq, 
                            null, 
                            MirahTokenId.get(Tokens.tNL), 
                            MirahTokenId.get(Tokens.tEnd))){
                        //System.out.println("Found end on this line");
                        return false;
                    }
                    
                    seq.move(ifOffset);
                    seq.moveNext();
                    DocumentQuery.consumeLine(seq);
                    seq.moveNext();
                    if ( seq.token() != null ){
                        int nextIndent = dq.getIndent(seq.token().offset(TokenHierarchy.get(doc)));
                        //System.out.println("Next indent is"+nextIndent);
                        if ( nextIndent != indent+indentSize ){
                            return true;
                        } else {
                            return false;
                        }
                        
                    }
                    // We now have an if statement
                    // Let's see if the next line is already indented
                    
                    
                    
                    return true;
                }
            }
            
        }
        
        //System.out.println("isAddEnd: "+doc.getText(bol, eol-bol));
        
        
        if ( caretOffset <= 1){
            return false;
        }
        int prevBOL = dq.getBOL(caretOffset-1);
        
        int nextBOL = dq.getBOL(caretOffset);
        if ( dq.getIndent(prevBOL) < dq.getIndent(nextBOL)){
            return false;
        }
        
        
        
        
        MirahTokenId[] starts = new MirahTokenId[]{
             MirahTokenId.get(Tokens.tDo.ordinal()),
             MirahTokenId.get(Tokens.tClass.ordinal()),
             MirahTokenId.get(Tokens.tInterface.ordinal()),
             MirahTokenId.get(Tokens.tDef.ordinal()),
             //MirahTokenId.get(Tokens.tIf.ordinal()),
             MirahTokenId.get(Tokens.tCase.ordinal()),
             MirahTokenId.get(Tokens.tWhile.ordinal()),
             MirahTokenId.get(Tokens.tBegin.ordinal()),
             MirahTokenId.get(Tokens.tUnless.ordinal())
        };
        boolean foundStart = false;
        for ( int i=0; i<starts.length; i++){
            if (tokenBalance(doc, starts[i]) > 0) {
                foundStart = true;
            }
        }
        if ( !foundStart ){
            return false;
        }
        //if (tokenBalance(doc, MirahTokenId.get(Tokens.tDo.ordinal())) <= 0) {
        //    return false;
       // }
        int caretRowStartOffset = org.netbeans.editor.Utilities.getRowStart(doc, caretOffset);
        TokenSequence<MirahTokenId> ts = mirahTokenSequence(doc, caretOffset, true);
        if (ts == null) {
            return false;
        }
        boolean first = true;
        
        MirahTokenId WHITESPACE = MirahTokenId.get(Tokens.tWhitespace.ordinal());
        //MirahTokenId LINE_COMMENT = MirahTokenId.get(Tokens.t)
        MirahTokenId LBRACE = MirahTokenId.get(Tokens.tLBrace.ordinal());
        
        do {
            if (ts.offset() < caretRowStartOffset) {
                return false;
            }
            for (int i=0; i<starts.length; i++){
                if ( ts.token().id().equals(starts[i])){
                    return true;
                }
            }
            //if ( ts.token().id().equals(LBRACE)){
            //    return true;
            //}
            
            
            first = false;
        } while (ts.movePrevious());
        return false;
    }
    
    /**
     * Resolve whether pairing right curly should be added automatically
     * at the caret position or not.
     * <br>
     * There must be only whitespace or line comment or block comment
     * between the caret position
     * and the left brace and the left brace must be on the same line
     * where the caret is located.
     * <br>
     * The caret must not be "contained" in the opened block comment token.
     *
     * @param doc document in which to operate.
     * @param caretOffset offset of the caret.
     * @return true if a right brace '}' should be added
     *  or false if not.
     */
    static boolean isAddRightBrace(BaseDocument doc, int caretOffset) throws BadLocationException {
        if (tokenBalance(doc, MirahTokenId.get(Tokens.tLBrace.ordinal())) <= 0) {
            return false;
        }
        int caretRowStartOffset = org.netbeans.editor.Utilities.getRowStart(doc, caretOffset);
        TokenSequence<MirahTokenId> ts = mirahTokenSequence(doc, caretOffset, true);
        if (ts == null) {
            return false;
        }
        boolean first = true;
        
        MirahTokenId WHITESPACE = MirahTokenId.get(Tokens.tWhitespace.ordinal());
        //MirahTokenId LINE_COMMENT = MirahTokenId.get(Tokens.t)
        MirahTokenId LBRACE = MirahTokenId.get(Tokens.tLBrace.ordinal());
        
        do {
            if (ts.offset() < caretRowStartOffset) {
                return false;
            }
            
            if ( ts.token().id().equals(LBRACE)){
                return true;
            }
            
            
            first = false;
        } while (ts.movePrevious());
        return false;
    }
    
    /**
     * Check for various conditions and possibly add a pairing bracket.
     *
     * @param context
     * @throws BadLocationException
     */
    static void completeOpeningBracket(TypedTextInterceptor.MutableContext context) throws BadLocationException {
        if (isStringOrComment(mirahTokenSequence(context, false).token().id())) {
            return;
        }
        char chr = context.getDocument().getText(context.getOffset(), 1).charAt(0);
        if (chr == ')' || chr == ',' || chr == '\"' || chr == '\'' || chr == ' ' || chr == ']' || chr == '}' || chr == '\n' || chr == '\t' || chr == ';') {
            char insChr = context.getText().charAt(0);
            context.setText("" + insChr + matching(insChr) , 1);  // NOI18N
        }
    }
    
    
    /**
     * Returns for an opening bracket or quote the appropriate closing
     * character.
     */
    private static char matching(char bracket) {
        switch (bracket) {
            case '(':
                return ')';
            case '[':
                return ']';
            case '\"':
                return '\"'; // NOI18N
            case '\'':
                return '\'';
            default:
                return ' ';
        }
    }
    
      private static MirahTokenId.Enum matching(MirahTokenId.Enum id) {
        switch (id) {
            case LPAREN:
                return MirahTokenId.Enum.RPAREN;
            case LBRACK:
                return MirahTokenId.Enum.RBRACK;
            case RPAREN:
                return MirahTokenId.Enum.LPAREN;
            case RBRACK:
                return MirahTokenId.Enum.LBRACK;
            default:
                return null;
        }
    }
    
    private static boolean isStringOrComment(MirahTokenId id) {
        return id.ordinal() == Tokens.tStringContent.ordinal();
    }
    
    /**
     * Check for various conditions and possibly skip a closing bracket.
     *
     * @param context
     * @return relative caretOffset change
     * @throws BadLocationException
     */
    static int skipClosingBracket(TypedTextInterceptor.MutableContext context) throws BadLocationException {
        TokenSequence<MirahTokenId> javaTS = mirahTokenSequence(context, false);
        if (javaTS == null || (javaTS.token().id().ordinal() != Tokens.tRParen.ordinal()) && javaTS.token().id().ordinal() != Tokens.tRBrack.ordinal() || isStringOrComment(javaTS.token().id())) {
            return -1;
        }

        MirahTokenId.Enum bracketId = bracketCharToId(context.getText().charAt(0));
        if (isSkipClosingBracket(context, javaTS, bracketId)) {
            context.setText("", 0);  // NOI18N
            return context.getOffset() + 1;
        }
        return -1;
    }
    
    private static MirahTokenId.Enum bracketCharToId(char bracket) {
        switch (bracket) {
            case '(':
                return MirahTokenId.Enum.LPAREN;
            case ')':
                return MirahTokenId.Enum.RPAREN;
            case '[':
                return MirahTokenId.Enum.LBRACK;
            case ']':
                return MirahTokenId.Enum.RBRACK;
            case '{':
                return MirahTokenId.Enum.LBRACE;
            case '}':
                return MirahTokenId.Enum.RBRACE;
            default:
                throw new IllegalArgumentException("Not a bracket char '" + bracket + '\'');  // NOI18N
        }
    }
    
    private static Set<MirahTokenId> STOP_TOKENS_FOR_SKIP_CLOSING_BRACKET = new HashSet<MirahTokenId>();
    static {
        STOP_TOKENS_FOR_SKIP_CLOSING_BRACKET.add(MirahTokenId.LBRACE);
        STOP_TOKENS_FOR_SKIP_CLOSING_BRACKET.add(MirahTokenId.RBRACE);
    }

    
    
    private static boolean isSkipClosingBracket(TypedTextInterceptor.MutableContext context, TokenSequence<MirahTokenId> javaTS, MirahTokenId.Enum rightBracketId) {
        if (context.getOffset() == context.getDocument().getLength()) {
            return false;
        }

        boolean skipClosingBracket = false;

        if (javaTS != null && javaTS.token().id().asEnum() == rightBracketId) {
            MirahTokenId.Enum leftBracketId = matching(rightBracketId);
            // Skip all the brackets of the same type that follow the last one
            do {
                if (STOP_TOKENS_FOR_SKIP_CLOSING_BRACKET.contains(javaTS.token().id())
                        || (javaTS.token().id() == MirahTokenId.WHITESPACE && javaTS.token().text().toString().contains("\n"))) {  // NOI18N
                    while (javaTS.token().id().asEnum() != rightBracketId) {
                        boolean isPrevious = javaTS.movePrevious();
                        if (!isPrevious) {
                            break;
                        }
                    }
                    break;
                }
            } while (javaTS.moveNext());

            // token var points to the last bracket in a group of two or more right brackets
            // Attempt to find the left matching bracket for it
            // Search would stop on an extra opening left brace if found
            int braceBalance = 0; // balance of '{' and '}'
            int bracketBalance = -1; // balance of the brackets or parenthesis
            int numOfSemi = 0;
            boolean finished = false;
            while (!finished && javaTS.movePrevious()) {
                MirahTokenId tokId = javaTS.token().id();
                if ( tokId == null ){
                    continue;
                }
                MirahTokenId.Enum id = tokId.asEnum();
                if ( id == null ){
                    continue;
                }
                switch (id) {
                    case LPAREN:
                    case LBRACK:
                        if (id == leftBracketId) {
                            bracketBalance++;
                            if (bracketBalance == 1) {
                                if (braceBalance != 0) {
                                    // Here the bracket is matched but it is located
                                    // inside an unclosed brace block
                                    // e.g. ... ->( } a()|)
                                    // which is in fact illegal but it's a question
                                    // of what's best to do in this case.
                                    // We chose to leave the typed bracket
                                    // by setting bracketBalance to 1.
                                    // It can be revised in the future.
                                    bracketBalance = 2;
                                }
                                finished = javaTS.offset() < context.getOffset();
                            }
                        }
                        break;

                    case RPAREN:
                    case RBRACK:
                        if (id == rightBracketId) {
                            bracketBalance--;
                        }
                        break;

                    case LBRACE:
                        braceBalance++;
                        if (braceBalance > 0) { // stop on extra left brace
                            finished = true;
                        }
                        break;

                    case RBRACE:
                        braceBalance--;
                        break;

                    //case SEMICOLON:
                    //    numOfSemi++;
                    //    break;
                }
            }

            if (bracketBalance == 1 && numOfSemi < 2) {
                finished = false;
                while (!finished && javaTS.movePrevious()) {
                    switch (javaTS.token().id().asEnum()) {
                        case WHITESPACE:
                        //case LINE_COMMENT:
                        //case BLOCK_COMMENT:
                        //case JAVADOC_COMMENT:
                            break;
                        //case FOR:
                        //    bracketBalance--;
                        default:
                            finished = true;
                            break;
                    }
                }
            }

            skipClosingBracket = bracketBalance != 1;
        }
        return skipClosingBracket;

    }
    
    /**
     * Called to insert either single bracket or bracket pair. 
     *
     * @param context
     * @return relative caretOffset change
     * @throws BadLocationException
     */
    static int completeQuote(TypedTextInterceptor.MutableContext context) throws BadLocationException {
        if (isEscapeSequence(context)) {
            return -1;
        }
        // Examine token id at the caret offset
        TokenSequence<MirahTokenId> javaTS = mirahTokenSequence(context, true);
        MirahTokenId.Enum id = (javaTS != null) ? javaTS.token().id().asEnum() : null;


        // If caret within comment return false
        if ( id != null ){
            
        }
        boolean caretInsideToken = (id != null)
                && (javaTS.offset() + javaTS.token().length() >= context.getOffset()
                || javaTS.token().partType() == PartType.START);
        

        boolean completablePosition = isQuoteCompletablePosition(context);
        boolean insideString = caretInsideToken
                && (id == MirahTokenId.Enum.STRING_LITERAL || id == MirahTokenId.Enum.CHAR_LITERAL || id == MirahTokenId.Enum.SQUOTE || id == MirahTokenId.Enum.DQUOTE);
        
        int lastNonWhite = org.netbeans.editor.Utilities.getRowLastNonWhite((BaseDocument) context.getDocument(), context.getOffset());
        // eol - true if the caret is at the end of line (ignoring whitespaces)
        boolean eol = lastNonWhite < context.getOffset();
        
        if (insideString) {
            if (eol) {
                return -1;
            } else {
                //#69524
                char chr = context.getDocument().getText(context.getOffset(), 1).charAt(0);
                if (chr == context.getText().charAt(0)) {
                    //#83044
                    if (context.getOffset() > 0) {
                        javaTS.move(context.getOffset() - 1);
                        if (javaTS.moveNext()) {
                            id = javaTS.token().id().asEnum();
                            if (id == MirahTokenId.Enum.STRING_LITERAL || id == MirahTokenId.Enum.CHAR_LITERAL || (chr == '\'' && id == MirahTokenId.Enum.SQUOTE) || (chr == '"' && id == MirahTokenId.Enum.DQUOTE) ) {
                                context.setText("", 0); // NOI18N
                                return context.getOffset() + 1;
                            }
                        }
                    }
                }
            }
        }

        if ((completablePosition && !insideString) || eol) {
            context.setText(context.getText() + context.getText(), 1);
        }
        return -1;
    }
    
    private static boolean isEscapeSequence(TypedTextInterceptor.MutableContext context) throws BadLocationException {
        if (context.getOffset() <= 0) {
            return false;
        }

        char[] previousChars;
        for (int i = 2; context.getOffset() - i >= 0; i += 2) {
            previousChars = context.getDocument().getText(context.getOffset() - i, 2).toCharArray();
            if (previousChars[1] != '\\') {
                return false;
            }
            if (previousChars[0] != '\\') {
                return true;
            }
        }
        return context.getDocument().getText(context.getOffset() - 1, 1).charAt(0) == '\\';
    }
    
    private static boolean isQuoteCompletablePosition(TypedTextInterceptor.MutableContext context) throws BadLocationException {
        if (context.getOffset() == context.getDocument().getLength()) {
            return true;
        } else {
            for (int i = context.getOffset(); i < context.getDocument().getLength(); i++) {
                char chr = context.getDocument().getText(i, 1).charAt(0);
                if (chr == '\n') {
                    break;
                }
                if (!Character.isWhitespace(chr)) {
                    return (chr == ')' || chr == ',' || chr == '+' || chr == '}' || chr == ';');
                }

            }
            return false;
        }
    }
    
    
    /**
     * Check for various conditions and possibly remove two brackets.
     *
     * @param context
     * @throws BadLocationException
     */
    static void removeBrackets(DeletedTextInterceptor.Context context) throws BadLocationException {
        int caretOffset = context.isBackwardDelete() ? context.getOffset() - 1 : context.getOffset();
        TokenSequence<MirahTokenId> ts = mirahTokenSequence(context.getDocument(), caretOffset, false);
        if (ts == null) {
            return;
        }

        switch (ts.token().id().asEnum()) {
            case RPAREN:
                if (tokenBalance(context.getDocument(), MirahTokenId.LPAREN) != 0) {
                    context.getDocument().remove(caretOffset, 1);
                }
                break;
            case RBRACK:
                if (tokenBalance(context.getDocument(), MirahTokenId.LBRACK) != 0) {
                    context.getDocument().remove(caretOffset, 1);
                }
                break;
        }
    }
    
    /**
     * Check for various conditions and possibly remove two quotes.
     *
     * @param context
     * @throws BadLocationException
     */
    static void removeCompletedQuote(DeletedTextInterceptor.Context context) throws BadLocationException {
        
        TokenSequence<MirahTokenId> ts = mirahTokenSequence(context, false);
        
        if (ts == null) {
            return;
        }
        char removedChar = context.getText().charAt(0);
        int caretOffset = context.isBackwardDelete() ? context.getOffset() - 1 : context.getOffset();
        if (removedChar == '\"' || removedChar == '\'') {
            if (ts.token().id().asEnum() == MirahTokenId.Enum.STRING_LITERAL && ts.offset() == caretOffset) {
                context.getDocument().remove(caretOffset, 1);
            }
        } else if (removedChar == '\'') {
            if (ts.token().id().asEnum() == MirahTokenId.Enum.CHAR_LITERAL && ts.offset() == caretOffset) {
                context.getDocument().remove(caretOffset, 1);
            }
        }
    }
    
     static boolean blockCommentCompletion(TypedBreakInterceptor.Context context) {
        return blockCommentCompletionImpl(context, false);
    }

    static boolean javadocBlockCompletion(TypedBreakInterceptor.Context context) {
        return blockCommentCompletionImpl(context, true);
    }

    private static boolean blockCommentCompletionImpl(TypedBreakInterceptor.Context context, boolean javadoc) {
            TokenSequence<MirahTokenId> ts = mirahTokenSequence(context, false);
            if (ts == null) {
                return false;
            }
            int dotPosition = context.getCaretOffset();
            ts.move(dotPosition);
            if (!((ts.moveNext() || ts.movePrevious()) && (ts.token().id() == MirahTokenId.WHITESPACE) || ts.token().id() == MirahTokenId.NL)) {
                
               return false;
            }

            int jdoffset = dotPosition - (javadoc ? 3 : 2);
            if (jdoffset >= 0) {
                //CharSequence content = org.netbeans.lib.editor.util.swing.DocumentUtilities.getText(context.getDocument());
                CharSequence content = DocumentUtilities.getText(context.getDocument());
                if (isOpenBlockComment(content, dotPosition - 1, javadoc) && !isClosedBlockComment(content, dotPosition) && isAtRowEnd(content, dotPosition)) {
                    return true;
                }
            }
        return false;
    }
    
    private static boolean isOpenBlockComment(CharSequence content, int pos, boolean javadoc) {
        for (int i = pos; i >= 0; i--) {
            char c = content.charAt(i);
            if (c == '*' && (javadoc ? i - 2 >= 0 && content.charAt(i - 1) == '*' && content.charAt(i - 2) == '/' : i - 1 >= 0 && content.charAt(i - 1) == '/')) {
                // matched /*
                return true;
            } else if (c == '\n') {
                // no javadoc, matched start of line
                return false;
            } else if (c == '/' && i - 1 >= 0 && content.charAt(i - 1) == '*') {
                // matched javadoc enclosing tag
                return false;
            }
        }

        return false;
    }

    private static boolean isClosedBlockComment(CharSequence txt, int pos) {
        int length = txt.length();
        int quotation = 0;
        for (int i = pos; i < length; i++) {
            char c = txt.charAt(i);
            if (c == '*' && i < length - 1 && txt.charAt(i + 1) == '/') {
                if (quotation == 0 || i < length - 2) {
                    return true;
                }
                // guess it is not just part of some text constant
                boolean isClosed = true;
                for (int j = i + 2; j < length; j++) {
                    char cc = txt.charAt(j);
                    if (cc == '\n') {
                        break;
                    } else if (cc == '"' && j < length - 1 && txt.charAt(j + 1) != '\'') {
                        isClosed = false;
                        break;
                    }
                }

                if (isClosed) {
                    return true;
                }
            } else if (c == '/' && i < length - 1 && txt.charAt(i + 1) == '*') {
                // start of another comment block
                return false;
            } else if (c == '\n') {
                quotation = 0;
            } else if (c == '"' && i < length - 1 && txt.charAt(i + 1) != '\'') {
                quotation = ++quotation % 2;
            }
        }

        return false;
    }
    
     private static boolean isAtRowEnd(CharSequence txt, int pos) {
        int length = txt.length();
        for (int i = pos; i < length; i++) {
            char c = txt.charAt(i);
            if (c == '\n') {
                return true;
            }
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }
     
      
    

}
