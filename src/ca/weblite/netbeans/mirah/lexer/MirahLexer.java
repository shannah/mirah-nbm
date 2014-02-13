/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.lexer;


import java.util.logging.Logger;
import mirah.impl.MirahParser;
import mirah.impl.Tokens;
import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 *
 * @author shannah
 */
public class MirahLexer implements Lexer<MirahTokenId>{
    private mirah.impl.MirahLexer lexer;
    private LexerRestartInfo<MirahTokenId> info;
    private int lastPos = 0;
    private MirahParser parser;
    private boolean inClassDeclaration = false;
    private boolean inMethodDeclaration = false;
    private boolean inTypeHint = false;
    private int lastToken = -1;
    
    private static final Logger LOG = Logger.getLogger(MirahLexer.class.getCanonicalName());
    private org.mirah.mmeta.BaseParser.Token<mirah.impl.Tokens> tok = null;


    MirahLexer (LexerRestartInfo<MirahTokenId> info, MirahParser parser) {
        //LOG.warning("IN the mirah lexer hooohooo");
        this.info = info;
        this.parser = parser;
        this.lexer = null;
        
    
        //JavaCharStream stream = new JavaCharStream (info.input ());
        //javaParserTokenManager = new JavaParserTokenManager (stream);
    }

    @Override
    public org.netbeans.api.lexer.Token<MirahTokenId> nextToken () {
        //LOG.warning("In the nextToken");
        //Token token = javaParserTokenManager.getNextToken ();
        //if (info.input ().readLength () < 1) return null;
        if ( this.lexer == null ){
            int c;
            StringBuilder sb = new StringBuilder();
            
            while ( true ){
                c = info.input().read();
                if ( c == -1 ){
                    break;
                } else {
                    sb.append((char)c);
                }
            }
            String str = sb.toString();
            //LOG.warning("String is "+str);
            lastPos = 0;
            lexer = new mirah.impl.MirahLexer(str, str.toCharArray(), parser);
        }
        
        boolean alreadyStarted = true;
        if ( tok == null ){
            alreadyStarted = false;
            
            tok = lexer.lex(lastPos);
            
        } 
        
        if ( Tokens.tEOF.equals(tok.type)){
            
            lexer = null;
            tok = null;
            return null;
        } 
        if ( alreadyStarted || tok.startpos == tok.pos ){
            //LOG.warning("Tok type "+tok.type);
            //LOG.warning("TOk is "+tok.type.name()+", "+tok.type.ordinal());
            //try {
                //LOG.warning("TOK TEXT is "+tok.text());
            //} catch ( Exception ex){
                //LOG.warning("Null getting tok.text() "+ex.getMessage());
            //}
            //LOG.warning("Start "+tok.startpos+" pos: "+tok.pos+", endpos: "+tok.endpos);
            int ordinal = tok.type.ordinal();
            if ( inClassDeclaration && ordinal == Tokens.tCONSTANT.ordinal()){
                inClassDeclaration = false;
                ordinal = MirahLanguageHierarchy.CLASS_DECLARATION;
            } else if ( ordinal == Tokens.tClass.ordinal() || ordinal == Tokens.tInterface.ordinal() ){
                inClassDeclaration = true;
            } else if ( inMethodDeclaration && ordinal == Tokens.tIDENTIFIER.ordinal()){
                inMethodDeclaration = false;
                ordinal = MirahLanguageHierarchy.METHOD_DECLARATION;
            } else if ( ordinal == Tokens.tDef.ordinal() ){
                inMethodDeclaration = true;
            }
            
            if ( inTypeHint && (ordinal == Tokens.tCONSTANT.ordinal() || Tokens.tIDENTIFIER.ordinal() == ordinal) ){
                inTypeHint = false;
                ordinal = MirahLanguageHierarchy.TYPE_HINT;
            } else if ( ordinal == Tokens.tColon.ordinal() && (lastToken == Tokens.tIDENTIFIER.ordinal() || lastToken == Tokens.tRParen.ordinal()) ){
                inTypeHint = true;
                
            }
            
            MirahTokenId tokid = MirahLanguageHierarchy.getToken(ordinal);
            //LOG.warning("MTok is "+tokid);
            int len = tok.endpos-tok.startpos;
            lastPos = tok.endpos;
            //LOG.warning("Length is "+len);
            lastToken = tok.type.ordinal();
            tok = null;
            return info.tokenFactory ().createToken (tokid,len);
        } else {
            int len = tok.startpos-tok.pos;
            lastPos = tok.startpos;
            return info.tokenFactory().createToken(MirahLanguageHierarchy.getToken(999), len);
        }
        
        
        
        
    }

    
    public void release () {
        
    
    }

    @Override
    public Object state() {
        return null;
    }
}
