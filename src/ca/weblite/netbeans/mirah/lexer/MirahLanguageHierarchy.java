/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.lexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import mirah.impl.MirahParser;
import mirah.impl.Tokens;
import mirah.impl.Tokens.*;
import org.netbeans.spi.lexer.LanguageHierarchy;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 *
 * @author shannah
 */
public class MirahLanguageHierarchy extends LanguageHierarchy<MirahTokenId>{
    
    public static final int CLASS_DECLARATION = 1000;
    public static final int METHOD_DECLARATION = 1001;
    public static final int TYPE_HINT = 1002;
    
    private static Tokens[] literals = {
        Tokens.tFloat,
        Tokens.tTrue,
        Tokens.tFalse,
        Tokens.tInteger
    };
    
    
    
    
    
    private static Tokens[] keywords = {
        Tokens.tDef,
        Tokens.tDo,
        Tokens.tImplements,
        Tokens.tBEGIN,
        Tokens.tBegin,
        Tokens.tEND,
        Tokens.tClass,
        Tokens.tDo,
        Tokens.tIf,
        Tokens.tImport,
        Tokens.tDefmacro,
        Tokens.tCase,
        Tokens.tElse,
        Tokens.tElsif,
        Tokens.tEnd,
        Tokens.tIn,
        Tokens.tRescue,
        Tokens.tRetry,
        Tokens.tReturn,
        Tokens.tBreak,
        Tokens.tClass,
        Tokens.tFalse,
        Tokens.tTrue,
        Tokens.tOr,
        Tokens.tAnd,
        Tokens.tUnless,
        Tokens.tUntil,
        Tokens.tFor,
        Tokens.tEnsure,
        //Tokens.tDef,
        //Tokens.tDefmacro,
        Tokens.tDefined,
        Tokens.tNil,
        Tokens.tNot,
        Tokens.tPackage,
        Tokens.tRaise,
        Tokens.tMacro,
        Tokens.tRedo,
        Tokens.tSelf,
        Tokens.tSuper,
        Tokens.tInterface
        
        
        
           
    };
    private static final Logger LOG =
    Logger.getLogger(MirahLanguageHierarchy.class.getCanonicalName());
    private static List<MirahTokenId>  tokens;
    private static Map<Integer,MirahTokenId>
                                    idToToken;
    private static Set<Tokens> keywordSet = new HashSet<Tokens>();
    private static Set<Tokens> literalSet = new HashSet<Tokens>();

    private static void init () {
        
        keywordSet.addAll(Arrays.asList(keywords));
        literalSet.addAll(Arrays.asList(literals));
        
        Tokens[] toks = new Tokens[]{
            Tokens.tBEGIN, 
            Tokens.tEND, 
            Tokens.t__ENCODING__, 
            Tokens.t__FILE__, 
            Tokens.t__LINE__, 
            Tokens.tAlias, 
            Tokens.tAnd, 
            Tokens.tBegin, 
            Tokens.tBreak, 
            Tokens.tCase, 
            Tokens.tClass, 
            Tokens.tDef, 
            Tokens.tDefined, 
            Tokens.tDefmacro, 
            Tokens.tDo, 
            Tokens.tElse, 
            Tokens.tElsif, 
            Tokens.tEnd, 
            Tokens.tEnsure, 
            Tokens.tFalse, 
            Tokens.tFor, 
            Tokens.tIf, 
            Tokens.tImplements, 
            Tokens.tImport, 
            Tokens.tIn, 
            Tokens.tInterface, 
            Tokens.tMacro, 
            Tokens.tModule, 
            Tokens.tNext, 
            Tokens.tNil, 
            Tokens.tNot, 
            Tokens.tOr, 
            Tokens.tPackage, 
            Tokens.tRaise, 
            Tokens.tRedo, 
            Tokens.tRescue, 
            Tokens.tRetry, 
            Tokens.tReturn, 
            Tokens.tSelf, Tokens.tSuper, Tokens.tThen, Tokens.tTrue, Tokens.tUndef, Tokens.tUnless, Tokens.tUntil, 
            Tokens.tWhen, Tokens.tWhile, Tokens.tYield, Tokens.tClassVar, Tokens.tInstVar, Tokens.tCONSTANT, 
            Tokens.tIDENTIFIER, 
            Tokens.tFID, Tokens.tNL, Tokens.tSemi, Tokens.tSlash, Tokens.tDQuote, Tokens.tSQuote, Tokens.tColons, 
            Tokens.tColon, Tokens.tDot, Tokens.tDots, Tokens.tLParen, Tokens.tRParen, Tokens.tLBrack, Tokens.tRBrack, 
            Tokens.tLBrace, Tokens.tRBrace, Tokens.tAt, Tokens.tBang, Tokens.tPlus, Tokens.tMinus, Tokens.tCaret, 
            Tokens.tTilde, Tokens.tPercent, Tokens.tNMatch, Tokens.tMatch, Tokens.tNE, Tokens.tLT, Tokens.tLE, 
            Tokens.tLEG, Tokens.tGT, Tokens.tGE, Tokens.tEEQ, Tokens.tEEEQ, 
            Tokens.tLShift, Tokens.tLLShift, Tokens.tRShift, Tokens.tEQ, Tokens.tAndEq, Tokens.tOrEq, 
            Tokens.tOpAssign, Tokens.tQuestion, Tokens.tDigit, Tokens.tInteger, Tokens.tFloat, Tokens.tBacktick, 
            Tokens.tDollar, Tokens.tInstVarBacktick, Tokens.tClassVarBacktick, Tokens.tComma, Tokens.tStar, Tokens.tStars, 
            Tokens.tAmper, Tokens.tAmpers, Tokens.tPipe, Tokens.tPipes, Tokens.tRocket, Tokens.tCharacter, Tokens.tEscape, 
            Tokens.tStringContent, Tokens.tStrEvBegin, Tokens.tRegexBegin, Tokens.tRegexEnd, Tokens.tHereDocId, 
            Tokens.tHereDocBegin, Tokens.tHereDocEnd, Tokens.tUNKNOWN, Tokens.tEOF, Tokens.tComment, Tokens.tJavaDoc, Tokens.tWhitespace, Tokens.tPartialComment};
        
        tokens = new ArrayList<MirahTokenId>();
        for ( Tokens t : toks ){
            
            MirahTokenId tok = new MirahTokenId(t.name(), getTokCategory(t), t.ordinal());
            
            tokens.add(tok);
        }
        tokens.add(new MirahTokenId("Class Declaration", "class-declaration", CLASS_DECLARATION));
        tokens.add(new MirahTokenId("Method Declaration", "method-declaration", METHOD_DECLARATION ));
        tokens.add(new MirahTokenId("Type Hint", "type-hint", TYPE_HINT));
        //tokens.add(new MirahTokenId("WHITESPACE","whitespace", 999 ));
        
        idToToken = new HashMap<Integer, MirahTokenId> ();
        for (MirahTokenId token : tokens)
            idToToken.put (token.ordinal (), token);
    }
    
    private static String getTokCategory(Tokens t){
        
        if ( CLASS_DECLARATION == t.ordinal()){
            return "class-declaration";
        } else if ( METHOD_DECLARATION == t.ordinal()){
            return "method-declaration";
        } else if ( TYPE_HINT == t.ordinal()){
            return "type-hint";
        } 
        else if ( keywordSet.contains(t)){
            return "keyword";
        } else if ( Tokens.tIDENTIFIER.equals(t)){
            return "identifier";
        } else if ( literalSet.contains(t)){
            return "literal";
        } else if ( Tokens.tCharacter.equals(t) || Tokens.tSQuote.equals(t)) {
            return "character";
        } else if ( Tokens.tInstVar.equals(t)){
            return "instance-var";
        } else if ( Tokens.tClassVar.equals(t)){
            return "class-var";
        } else if ( Tokens.tStringContent.equals(t) || Tokens.tDQuote.equals(t)){
            return "string";
        } else if ( Tokens.tComment.equals(t) || Tokens.tJavaDoc.equals(t)){
            return "comment";
        } else {
            return "unknown";
        }
    }

    static synchronized MirahTokenId getToken (int id) {
        if (idToToken == null)
            init ();
        return idToToken.get (id);
    }

    protected synchronized Collection<MirahTokenId> createTokenIds () {
        if (tokens == null)
            init ();
        return tokens;
    }

    protected synchronized Lexer<MirahTokenId> createLexer (LexerRestartInfo<MirahTokenId> info) {
        
        return new MirahLexer (info, new MirahParser());
    }

    protected String mimeType () {
        return "text/x-mirah";
    }
    
    
}
