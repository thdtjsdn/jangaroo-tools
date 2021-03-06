/*
 * Copyright 2008 CoreMedia AG
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either 
 * express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */

package net.jangaroo.joodoc;

import net.jangaroo.jooc.NodeImplBase;
import net.jangaroo.jooc.TypeRelation;
import net.jangaroo.jooc.MemberDeclaration;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Type;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.AnnotationDesc;

import java.lang.reflect.Modifier;

public class MemberDocImpl extends DocImpl implements MemberDoc {

  public MemberDocImpl(NodeImplBase node) {
    super(node);
  }

  public int modifierSpecifier() {
    int modifier = -1;
    if (isPublic())
      modifier = Modifier.PUBLIC;
    else if (isProtected())
      modifier=Modifier.PROTECTED;
    else if (isPrivate())
      modifier=Modifier.PRIVATE;
    if (isStatic())
      modifier |= Modifier.STATIC;
    if (isAbstract())
      modifier |= Modifier.ABSTRACT;
    return modifier;
  }

  protected MemberDeclaration getMemberDeclaration() {
    return ((MemberDeclaration)declaration);
  }

  public boolean isFinal() {
    return getMemberDeclaration().isFinal();
  }

  public boolean isPackagePrivate() {
    return !isProtected() && !isPublic() && !isPrivate();
  }

  public boolean isPrivate() {
    return getMemberDeclaration().isPrivate();
  }

  public boolean isProtected() {
    return getMemberDeclaration().isProtected();
  }

  public boolean isPublic() {
    return getMemberDeclaration().isPublic();
  }

  public boolean isStatic() {
    return getMemberDeclaration().isStatic();
  }

  public boolean isAbstract() {
    return false;
  }

  public ClassDoc containingClass() {
    return (ClassDoc) DocMap.getDoc(getMemberDeclaration().getClassDeclaration());
  }

  public PackageDoc containingPackage() {
    return (PackageDoc) DocMap.getDoc(getMemberDeclaration().getClassDeclaration().getPackageDeclaration());
  }

  public String modifiers() {
    return (isPublic()?"public ":"")+
           (isStatic()?"static ":"")+
           (isFinal()?"final ":"")+
           (isAbstract()?"abstract ":"")
        ;
  }

  public String qualifiedName() {
    String[] qual=getMemberDeclaration().getQualifiedName();
    return Util.getQualifiedName(qual);
  }

  public String name() {
    return getMemberDeclaration().getName();
  }

  public boolean isIncluded() {
    return isPublic() || isProtected();
  }

  public Type getType() {
    TypeRelation optTypeRelation = getMemberDeclaration().getOptTypeRelation();
    if (optTypeRelation!=null)
      return (Type) (DocMap.getDoc(optTypeRelation.getType()));
    return TypeImpl.ANY;
  }

  public boolean isSynthetic() {
    return false;
  }

  public AnnotationDesc[] annotations() {
    return new AnnotationDesc[0];  //To change body of implemented methods use File | Settings | File Templates.
  }
}
