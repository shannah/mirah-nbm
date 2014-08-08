/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.lexer;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author shannah
 */
public class CodeFormatter {
    public String formatMethod(Method m, int indent, int numSpacesPerIndent ){
        StringBuilder sb = new StringBuilder();
        //indent(sb, indent);
        sb.append("def ").append(m.getName());
        int i=0;
        int len;
        if ( ( len = m.getParameterTypes().length) > 0 ){
            
            sb.append("(");
            for ( Class pType : m.getParameterTypes()){
                sb
                        .append("arg")
                        .append(i++)
                        .append(":")
                        .append(pType.getSimpleName());
                if ( i<len){
                    sb.append(", ");
                }
            }
            sb.append(")");
        }
        sb.append(":");
        Class returnType;
        if ( (returnType = m.getReturnType()) != null ){
            sb.append(returnType.getSimpleName());
        } else {
            sb.append("void");
        }
        sb.append("\n");
        indent(sb, indent);
        indent(sb, numSpacesPerIndent);
        sb.append("\n");
        indent(sb, indent);
        sb.append("end\n");
        indent(sb, indent);
        
        return sb.toString();
    }
    
    public Set<String> getRequiredImports(Method m){
        Set<String> out = new HashSet<String>();
        for ( Class ptype : m.getParameterTypes()){
            String name = ptype.getName();
            if ( name.startsWith("java.lang.")){
                continue;
            }
            if ( ptype.isPrimitive() ){
                continue;
            }
            out.add(ptype.getName());
        }
        Class retType = m.getReturnType();
        if ( retType != null
                && !retType.getName().startsWith("java.lang.")
                && !retType.isPrimitive()){
            out.add(retType.getName());
        }
        
        return out;
        
    }
    
    public void indent(StringBuilder sb, int indent){
        for ( int i=0; i< indent; i++){
            sb.append(" ");
        }
    }
}
