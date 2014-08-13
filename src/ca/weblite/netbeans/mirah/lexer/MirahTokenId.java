/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.lexer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import mirah.impl.Tokens;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.TokenId;

/**
 *
 * @author shannah
 */
public class MirahTokenId implements TokenId {
    
    public static final MirahTokenId RPAREN = get(Tokens.tRParen.ordinal());
    public static final MirahTokenId LPAREN = get(Tokens.tLParen.ordinal());
    public static final MirahTokenId RBRACK = get(Tokens.tRBrack.ordinal());
    public static final MirahTokenId LBRACK = get(Tokens.tLBrack.ordinal());
    public static final MirahTokenId RBRACE = get(Tokens.tRBrace.ordinal());
    public static final MirahTokenId LBRACE = get(Tokens.tLBrace.ordinal());
    public static final MirahTokenId WHITESPACE = get(Tokens.tWhitespace.ordinal());
    public static final MirahTokenId STRING_LITERAL = get(Tokens.tStringContent.ordinal());
    public static final MirahTokenId CHAR_LITERAL = get(Tokens.tCharacter.ordinal());
    public static final MirahTokenId SQUOTE = get(Tokens.tSQuote.ordinal());
    public static final MirahTokenId DQUOTE = get(Tokens.tDQuote.ordinal());
    public static final MirahTokenId NL = get(Tokens.tNL.ordinal());
    public static final Set<MirahTokenId> WHITESPACE_AND_COMMENTS = new HashSet<MirahTokenId>(Arrays.asList(new MirahTokenId[]{
       get(Tokens.tWhitespace),
       get(Tokens.tComment),
       get(Tokens.tJavaDoc)
    }));
    
    
    public static enum Enum {
        RPAREN,
        LPAREN,
        RBRACK,
        LBRACK,
        LBRACE,
        RBRACE,
        WHITESPACE,
        STRING_LITERAL,
        CHAR_LITERAL,
        SQUOTE,
        DQUOTE
    }
    
    private static final Map<MirahTokenId,Enum> enumMap = new HashMap<MirahTokenId,Enum>();
    static {
        enumMap.put(RPAREN, Enum.RPAREN);
        enumMap.put(LPAREN, Enum.LPAREN);
        enumMap.put(RBRACK, Enum.RBRACK);
        enumMap.put(LBRACK, Enum.LBRACK);
        enumMap.put(RBRACE, Enum.RBRACE);
        enumMap.put(LBRACE, Enum.LBRACE);
        enumMap.put(WHITESPACE, Enum.WHITESPACE);
        enumMap.put(STRING_LITERAL, Enum.STRING_LITERAL);
        enumMap.put(CHAR_LITERAL, Enum.CHAR_LITERAL);
        enumMap.put(SQUOTE, Enum.SQUOTE);
        enumMap.put(DQUOTE, Enum.DQUOTE);
    }
    
    public static final Set<MirahTokenId> CONTINUATION_TOKENS = set(
            Tokens.tOr,
            Tokens.tAnd,
            Tokens.tDo,
            Tokens.tAmper,
            Tokens.tColon,
            Tokens.tColons,
            Tokens.tDot,
            Tokens.tGE,
            Tokens.tGT,
            Tokens.tLE,
            Tokens.tLT,
            Tokens.tOpAssign,
            Tokens.tOrEq,
            Tokens.tAndEq,
            Tokens.tLLShift,
            Tokens.tLShift,
            Tokens.tRShift,
            Tokens.tPlus,
            Tokens.tPipes,
            Tokens.tPipe,
            Tokens.tPercent,
            Tokens.tMinus,
            Tokens.tStar,
            Tokens.tStars,
            Tokens.tNE,
            Tokens.tIn,
            Tokens.tSlash,
            Tokens.tComma,
            Tokens.tLParen,
            Tokens.tLBrack,
            Tokens.tLBrace
    );
    
    private final String        name;
    private final String        primaryCategory;
    private final int           id;
    
     private static final Language<MirahTokenId> language = new MirahLanguageHierarchy ().language ();

    public static final Language<MirahTokenId> getLanguage () {
        
        return language;
    }
    
    MirahTokenId (
        String                  name,
        String                  primaryCategory,
        int                     id
    ) {
        this.name = name;
        this.primaryCategory = primaryCategory;
        this.id = id;
    }

    @Override
    public String primaryCategory () {
        return primaryCategory;
    }

    @Override
    public int ordinal () {
        return id;
    }

    @Override
    public String name () {
        return name;
    }
    
    public static MirahTokenId get(int id){
        return MirahLanguageHierarchy.getToken(id);
    }
    
    public static MirahTokenId get(Tokens tok){
        return MirahTokenId.get(tok.ordinal());
    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof MirahTokenId) ){
            return false;
            
        }
        MirahTokenId o = (MirahTokenId)obj;
        if ( o == null ){
            return false;
        }
        return o.ordinal() == this.ordinal();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + this.id;
        return hash;
    }
    
    public Enum asEnum(){
        return enumMap.get(this);
    }
    
    
    public static Set<MirahTokenId> set(Tokens... toks){
        HashSet<MirahTokenId> out = new HashSet<MirahTokenId>();
        for ( Tokens t : toks ){
            out.add(MirahTokenId.get(t));
        }
        return out;
    }
    
    
    
}
