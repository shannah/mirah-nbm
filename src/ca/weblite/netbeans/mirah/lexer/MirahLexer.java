/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.lexer;


import java.util.logging.Logger;
import mirah.impl.MirahLexer.Input;
import mirah.impl.MirahParser;
import mirah.impl.Tokens;
import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 *
 * @author shannah
 */
public class MirahLexer implements Lexer<MirahTokenId>{
    private Object state;
    private mirah.impl.MirahLexer lexer;
    private LexerRestartInfo<MirahTokenId> info;
    private int lastPos = 0;
    private MirahParser parser;
    private boolean inClassDeclaration = false;
    private boolean inMethodDeclaration = false;
    private boolean inTypeHint = false;
    private int lastToken = -1;
    private int lastNonWhiteToken = -1;
    //private MirahLexerInput input;
    
    private static final Logger LOG = Logger.getLogger(MirahLexer.class.getCanonicalName());
    private org.mirah.mmeta.BaseParser.Token<mirah.impl.Tokens> tok = null;

    /*
    public static class MirahLexerInput implements Input {

        private int pos = 0;
        private final LexerInput input;
        private boolean eof = false;

        public MirahLexerInput(LexerInput input) {
            this.input = input;
        }

        @Override
        public int pos() {
            return pos;
        }

        @Override
        public int read() {
            if ( eof ){
                return -1;
            }
            int out = input.read();
            eof = out == -1;
            if ( !eof ) pos++;
            return out;

        }

        @Override
        public boolean consume(char c) {
            if (read() == c) {
                return true;
            }
            backup(1);
            return false;
        }

        @Override
        public boolean consume(String s) {
            int len = s.length();
            for (int i = 0; i < len; i++) {
                int c = read();
                if (s.charAt(i) != (char) c) {
                    if ( c == -1 ){
                        i--;
                    }
                    backup(i+1);
                    return false;
                }
            }
            return true;

        }

        @Override
        public void backup(int amount) {
            input.backup(amount);
            pos -= amount;
            if ( amount > 0 ){
                eof = false;
            }
            if (pos < 0) {
                pos = 0;
            }
        }

        @Override
        public void skip(int amount) {
            if ( !eof ){
                for (int i = 0; i < amount; i++) {

                    if (read() == -1) {
                        eof = false;
                        return;
                    }
                }
            }
        }

        @Override
        public boolean hasNext() {
            System.out.println("Has next ");
            return !eof;
           
        }

        @Override
        public void consumeLine() {
            if ( !eof ){
                System.out.println("In consumeLine");
                int c;
                while ((c = read()) != -1) {
                    if (((char) c) == '\n') {
                        eof = false;
                        return;
                    }
                }
                System.out.println("No newline found");
            }
        }

        @Override
        public int peek() {
            if ( eof ){
                return -1;
            } else {
                int result = read();
                backup(1);
                return result;
            }
        }

        @Override
        public int finishCodepoint() {
            backup(1);
            char c1 = (char) read();
            int size = 1;
            int i2;
            if ((i2 = read()) != -1) {
                size++;
            }
            if (size == 1) {
                return String.valueOf(c1).codePointAt(0);
            } else {
                return String.valueOf(new char[]{c1, (char) i2}).codePointAt(0);
            }

        }

        @Override
        public CharSequence readBack(int length) {
            //System.out.println("readBack");
            int p = pos;
            backup(length);
            int newP = pos;
            StringBuilder sb = new StringBuilder();
            for (int i = newP; i < p; i++) {
                sb.append((char) read());
            }
            return sb.toString();

        }
    }
    */

    MirahLexer (LexerRestartInfo<MirahTokenId> info, MirahParser parser) {
        this.info = info;
        this.parser = parser;
        //this.input = new MirahLexerInput(info.input());
        //this.lexer = new mirah.impl.MirahLexer(this.input);
        this.lexer = null;
        
        
        
    }
/*
    @Override
    public Token<MirahTokenId> nextToken() {
        System.out.println("In nextToken()");
        int start = input.pos();
        Tokens tokType = lexer.simpleLex();
        if ( tokType.ordinal() == Tokens.tEOF.ordinal()){
            System.out.println("EOF");
            return null;
        }
        int end = input.pos();
        int len = end-start;
        if ( len < 0 ){
            System.out.println("Negative len at "+start+":"+end);
        }
        int ordinal = tokType.ordinal();
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
        } else if ( ordinal == Tokens.tColon.ordinal() && (lastNonWhiteToken == Tokens.tIDENTIFIER.ordinal() || lastNonWhiteToken == Tokens.tRParen.ordinal()) ){
            inTypeHint = true;

        }

        MirahTokenId tokid = MirahLanguageHierarchy.getToken(ordinal);
        //int len = tok.endpos-tok.startpos;
        //lastPos = end;
        lastToken = tokType.ordinal();
        if ( tokType.ordinal() < Tokens.tEOF.ordinal() ){
            lastNonWhiteToken = tokType.ordinal();
        }
        //if ( tokid == null ){
        //    System.out.println("Token id is null");
        //    System.out.println(tokType.name());
        //}
        //if ( tokType == null ){
        //    System.out.println("Toktype is null");
        //}
        //System.out.println("TOken "+tokType.name()+" tokid  ordinal "+tokid);
        System.out.println("Exiting nextToken "+tokid.name()+" len "+len);
        if ( tokType.ordinal() == Tokens.tPartialComment.ordinal() ){
            state = new Integer(-1);
            //len--;
            //input.backup(1);
        } else {
            state = null;
        }
        Token<MirahTokenId> out =  info.tokenFactory().createToken(tokid,len);
        if ( out == null ){
            System.out.println("It is a null token");
            return out;
        } else {
            return out;
        }
        
        
    }
*/
    
    
    
    @Override
    public org.netbeans.api.lexer.Token<MirahTokenId> nextToken () {
        
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
            //System.out.println("Str is: "+str);
            lastPos = 0;
            //strLen = str.length();
            lexer = new mirah.impl.MirahLexer(str, str.toCharArray(), parser);
        }
        
        boolean alreadyStarted = true;
        if ( tok == null ){
            alreadyStarted = false;
            try {
                tok = lexer.lex(lastPos, false);
                
                
            } catch ( Exception npe ){
                //npe.printStackTrace();
                
                int len = 1;
                lastPos++;
                return info.tokenFactory().createToken(MirahLanguageHierarchy.getToken(Tokens.tWhitespace.ordinal()), len);
            }
            
        } 
        
        if ( Tokens.tEOF.ordinal() == tok.type.ordinal()){
            
            lexer = null;
            tok = null;
            return null;
        } 
        if ( alreadyStarted || tok.startpos == tok.pos ){
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
            } else if ( ordinal == Tokens.tColon.ordinal() && (lastNonWhiteToken == Tokens.tIDENTIFIER.ordinal() || lastNonWhiteToken == Tokens.tRParen.ordinal()) ){
                inTypeHint = true;
                
            }
            
            MirahTokenId tokid = MirahLanguageHierarchy.getToken(ordinal);
            int len = tok.endpos-tok.startpos;
            lastPos = tok.endpos;
            lastToken = tok.type.ordinal();
            if ( tok.type.ordinal() < Tokens.tEOF.ordinal() ){
                lastNonWhiteToken = tok.type.ordinal();
            }
            tok = null;
            
            //if ( lastPos == strLen ){
            //    len--;
            //}
            return info.tokenFactory().createToken(tokid,len);
        } else {
            int len = tok.startpos-tok.pos;
            lastPos = tok.startpos;
            return info.tokenFactory().createToken(MirahLanguageHierarchy.getToken(Tokens.tComment.ordinal()), len);
        }
        
        
        
        
    }
 
    
    public void release () {
        
    
    }

    @Override
    public Object state() {
        return state;
    }
}