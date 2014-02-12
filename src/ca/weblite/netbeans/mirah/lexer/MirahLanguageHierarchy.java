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
import java.util.List;
import java.util.Map;
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
    
    private static Tokens[] keywords = {
        Tokens.tDef,
        Tokens.tDo,
        Tokens.tImplements,
        Tokens.tBEGIN,
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
        Tokens.tIn
        
        
           
    };
    private static final Logger LOG =
    Logger.getLogger(MirahLanguageHierarchy.class.getCanonicalName());
    private static List<MirahTokenId>  tokens;
    private static Map<Integer,MirahTokenId>
                                    idToToken;

    private static void init () {
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
            Tokens.tHereDocBegin, Tokens.tHereDocEnd, Tokens.tUNKNOWN, Tokens.tEOF};
        
        
        
        tokens = /*Arrays.<MirahTokenId> asList (new MirahTokenId[] {
            
        });*/
        tokens = new ArrayList<MirahTokenId>();
        LOG.warning("About to do tokens");
        for ( Tokens t : toks ){
            LOG.warning("Creating token "+t.ordinal());
            MirahTokenId tok = new MirahTokenId(t.name(), getTokCategory(t), t.ordinal());
            LOG.warning("Token created... now adding");
            tokens.add(tok);
        }
        tokens.add(new MirahTokenId("WHITESPACE","whitespace", 999 ));
        
        idToToken = new HashMap<Integer, MirahTokenId> ();
        for (MirahTokenId token : tokens)
            idToToken.put (token.ordinal (), token);
    }
    
    private static String getTokCategory(Tokens t){
        if ( Arrays.asList(keywords).contains(t)){
            return "keyword";
        } else  {
            return "character";
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
