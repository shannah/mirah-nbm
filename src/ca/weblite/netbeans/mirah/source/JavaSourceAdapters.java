/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.weblite.netbeans.mirah.source;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;

/**
 *
 * @author shannah
 */
public class JavaSourceAdapters {
    
    static public Element createMethodElement(Method m){
        return new MethodElement(m);
    }
    
    static public Element createClassElement(Class c){
        return new ClassElement(c);
    }
    
    static public Element createPackageElement(Package p){
        return new PackageElement(p);
    }
    
    static Name createName(final String name){
        return new Name(){

            @Override
            public boolean contentEquals(CharSequence cs) {
                return name.equals(cs);
            }

            @Override
            public int length() {
                return name.length();
            }

            @Override
            public char charAt(int index) {
                return name.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return name.subSequence(start, end);
            }
            
        };
    }
    
    static class MethodElement implements Element {
        Method m;
        
        MethodElement(Method m){
            this.m = m;
        }
        
        @Override
        public TypeMirror asType() {
            return null;
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.METHOD;
        }

        @Override
        public List<? extends AnnotationMirror> getAnnotationMirrors() {
            return new ArrayList<AnnotationMirror>();
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            return null;
        }

        @Override
        public Set<Modifier> getModifiers() {
            Set<Modifier> out = new HashSet<Modifier>();
                int modifiers = m.getModifiers();
                if ( java.lang.reflect.Modifier.isAbstract(modifiers)){
                    out.add(Modifier.ABSTRACT);
                }
                if ( java.lang.reflect.Modifier.isFinal(modifiers)){
                    out.add(Modifier.FINAL);
                }
                if ( java.lang.reflect.Modifier.isNative(modifiers)){
                    out.add(Modifier.NATIVE);
                }
                if ( java.lang.reflect.Modifier.isPrivate(modifiers)){
                    out.add(Modifier.PRIVATE);
                }
                if ( java.lang.reflect.Modifier.isProtected(modifiers)){
                    out.add(Modifier.PROTECTED);
                }
                
                if ( java.lang.reflect.Modifier.isPublic(modifiers)){
                    out.add(Modifier.PUBLIC);
                }
                if ( java.lang.reflect.Modifier.isStatic(modifiers)){
                    out.add(Modifier.STATIC);
                }
                if ( java.lang.reflect.Modifier.isSynchronized(modifiers)){
                    out.add(Modifier.SYNCHRONIZED);
                }
                return out;
        }

        @Override
        public Name getSimpleName() {
            return createName(m.getName());
        }

        @Override
        public Element getEnclosingElement() {
            return createClassElement(m.getDeclaringClass());
        }

        @Override
        public List<? extends Element> getEnclosedElements() {
           return new ArrayList<Element>();
        }

        @Override
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return null;
        }
        
    }
    
    static class ClassElement implements Element {
        Class c;
        ClassElement(Class c){
            this.c = c;
        }

        @Override
        public TypeMirror asType() {
            return null;
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.CLASS;
        }

        @Override
        public List<? extends AnnotationMirror> getAnnotationMirrors() {
            return new ArrayList<AnnotationMirror>();
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            return null;
        }

        @Override
        public Set<Modifier> getModifiers() {
            Set<Modifier> out = new HashSet<Modifier>();
            int modifiers = c.getModifiers();
            if ( java.lang.reflect.Modifier.isAbstract(modifiers)){
                out.add(Modifier.ABSTRACT);
            }
            if ( java.lang.reflect.Modifier.isFinal(modifiers)){
                out.add(Modifier.FINAL);
            }
            
            if ( java.lang.reflect.Modifier.isPrivate(modifiers)){
                out.add(Modifier.PRIVATE);
            }
            if ( java.lang.reflect.Modifier.isProtected(modifiers)){
                out.add(Modifier.PROTECTED);
            }

            if ( java.lang.reflect.Modifier.isPublic(modifiers)){
                out.add(Modifier.PUBLIC);
            }
            if ( java.lang.reflect.Modifier.isStatic(modifiers)){
                out.add(Modifier.STATIC);
            }
            
            return out;
        }

        @Override
        public Name getSimpleName() {
            return createName(c.getSimpleName());
        }

        @Override
        public Element getEnclosingElement() {
            if ( c.getEnclosingMethod() != null ){
                return createMethodElement(c.getEnclosingMethod());
            //} else if ( c.getEnclosingConstructor() != null){
            //    return createMethodElement(c.getEnclosingConstructor());
            } else if ( c.getEnclosingClass() != null){
                return createClassElement(c.getEnclosingClass());
            } else if ( c.getPackage() != null ){
                return createPackageElement(c.getPackage());
            }
            return null;
            
        }

        @Override
        public List<? extends Element> getEnclosedElements() {
            List<Element> out = new ArrayList<Element>();
            for ( Method m : c.getDeclaredMethods()){
                out.add(createMethodElement(m));
            }
            for ( Class cls : c.getDeclaredClasses()){
                out.add(createClassElement(cls));
            }
            return out;
        }

        @Override
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            for ( Element e : getEnclosedElements()){
                v.visit(e);
            }
            return null;
        }
        
        
    }
    
  
    
    static class PackageElement implements Element {
        Package p;

        PackageElement(Package p){
            this.p = p;
        }
        
        @Override
        public TypeMirror asType() {
            return null;
        }

        @Override
        public ElementKind getKind() {
            return ElementKind.PACKAGE;
        }

        @Override
        public List<? extends AnnotationMirror> getAnnotationMirrors() {
            return new ArrayList<AnnotationMirror>();
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            return null;
        }

        @Override
        public Set<Modifier> getModifiers() {
            Set<Modifier> out = new HashSet<Modifier>();
            
            return out;
        }

        @Override
        public Name getSimpleName() {
            return createName(p.getName());
        }

        @Override
        public Element getEnclosingElement() {
            return null;
        }

        @Override
        public List<? extends Element> getEnclosedElements() {
            return new ArrayList<Element>();
        }

        @Override
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            return null;
        }
    }
}
