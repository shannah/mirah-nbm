/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.lexer;

import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.TokenId;

/**
 *
 * @author shannah
 */
public class MirahTokenId implements TokenId {
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
}
