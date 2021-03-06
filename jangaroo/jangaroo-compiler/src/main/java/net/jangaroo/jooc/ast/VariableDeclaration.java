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

package net.jangaroo.jooc.ast;

import net.jangaroo.utils.AS3Type;
import net.jangaroo.jooc.JooSymbol;
import net.jangaroo.jooc.Jooc;
import net.jangaroo.jooc.Scope;
import net.jangaroo.jooc.sym;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andreas Gawecki
 * @author Frank Wienberg
 */
public class VariableDeclaration extends TypedIdeDeclaration {
  private static final Map<AS3Type, String> DEFAULT_VALUE_BY_TYPE = new HashMap<AS3Type, String>(10);

  private JooSymbol optSymConstOrVar;
  private Initializer optInitializer;
  private VariableDeclaration optNextVariableDeclaration;
  private JooSymbol optSymSemicolon;

  private VariableDeclaration previousVariableDeclaration;

  public VariableDeclaration(JooSymbol[] modifiers,
                             JooSymbol optSymConstOrVar,
                             Ide ide,
                             TypeRelation optTypeRelation,
                             Initializer optInitializer,
                             VariableDeclaration optNextVariableDeclaration,
                             JooSymbol optSymSemicolon) {
    // inherit modifiers of first declaration to those following this declaration
    super(modifiers, ide, optTypeRelation);
    this.optSymConstOrVar = optSymConstOrVar;
    this.optInitializer = optInitializer;
    this.optNextVariableDeclaration = optNextVariableDeclaration;
    this.optSymSemicolon = optSymSemicolon;
    if (optNextVariableDeclaration != null) {
      optNextVariableDeclaration.previousVariableDeclaration = this;
      if (optSymSemicolon != null) {
        optNextVariableDeclaration.setInheritedModifiers(modifiers);
      }
    }
  }

  @Override
  public void visit(AstVisitor visitor) throws IOException {
    visitor.visitVariableDeclaration(this);
  }

  protected int getAllowedModifiers() {
    return MODIFIERS_SCOPE | MODIFIER_STATIC;
  }

  public VariableDeclaration(JooSymbol symConstOrVar,
                             Ide ide,
                             TypeRelation optTypeRelation,
                             Initializer optInitializer,
                             VariableDeclaration optNextVariableDeclaration,
                             JooSymbol optSymSemicolon) {
    this(new JooSymbol[0], symConstOrVar, ide, optTypeRelation, optInitializer, optNextVariableDeclaration, optSymSemicolon);
  }

  public VariableDeclaration(JooSymbol symConstOrVar,
                             Ide ide,
                             TypeRelation optTypeRelation,
                             Initializer optInitializer,
                             VariableDeclaration optNextVariableDeclaration) {
    this(symConstOrVar, ide, optTypeRelation, optInitializer, optNextVariableDeclaration, null);
  }

  public VariableDeclaration(JooSymbol symConstOrVar,
                             Ide ide,
                             TypeRelation optTypeRelation,
                             Initializer optInitializer) {
    this(symConstOrVar, ide, optTypeRelation, optInitializer, null);
  }

  public VariableDeclaration(JooSymbol symConstOrVar,
                             Ide ide,
                             TypeRelation optTypeRelation) {
    this(symConstOrVar, ide, optTypeRelation, null, null, null);
  }

  @Override
  protected void setInheritedModifiers(final JooSymbol[] modifiers) {
    super.setInheritedModifiers(modifiers);
    if (getOptNextVariableDeclaration() != null) {
      getOptNextVariableDeclaration().setInheritedModifiers(modifiers);
    }
  }

  @Override
  public void setClassMember(boolean classMember) {
    super.setClassMember(classMember);
    if (getOptNextVariableDeclaration() != null) {
      getOptNextVariableDeclaration().setClassMember(classMember);
    }
  }

  public boolean isCompileTimeConstant() {
    return isConst() && (getOptInitializer() == null || getOptInitializer().getValue().isCompileTimeConstant());
  }

  public void analyze(AstNode parentNode) {
    super.analyze(parentNode);
    if (getOptInitializer() == null && isConst()) {
      Jooc.warning(getOptSymConstOrVar(), "constant should be initialized");
    }
    if (getOptInitializer() != null) {
      getOptInitializer().analyze(this);
    }
    if (getOptNextVariableDeclaration() != null) {
      getOptNextVariableDeclaration().analyze(this);
    }
    if (isClassMember() && !isStatic() && getOptInitializer() != null && !getOptInitializer().getValue().isCompileTimeConstant()) {
      getClassDeclaration().addFieldWithInitializer(this);
    }
  }

  static {
    DEFAULT_VALUE_BY_TYPE.put(AS3Type.BOOLEAN, "false");
    DEFAULT_VALUE_BY_TYPE.put(AS3Type.INT, "0");
    DEFAULT_VALUE_BY_TYPE.put(AS3Type.NUMBER, "NaN");
    DEFAULT_VALUE_BY_TYPE.put(AS3Type.UINT, "0");
    DEFAULT_VALUE_BY_TYPE.put(AS3Type.ANY, "undefined");
  }

  public static String getDefaultValue(TypeRelation typeRelation) {
    AS3Type type = typeRelation == null ? AS3Type.ANY : AS3Type.typeByName(typeRelation.getType().getSymbol().getText());
    String emptyValue = DEFAULT_VALUE_BY_TYPE.get(type);
    if (emptyValue == null) {
      emptyValue = "null";
    }
    return emptyValue;
  }

  boolean allowDuplicates(Scope scope) {
    // todo It is "worst practice" to redeclare local variables in AS3, make this configurable:
    return !isClassMember();
  }

  @Override
  public void scope(final Scope scope) {
    super.scope(scope);
    if (getOptInitializer() != null) {
      getOptInitializer().scope(scope);
    }
    if (getOptNextVariableDeclaration() != null) {
      getOptNextVariableDeclaration().scope(scope);
    }
  }

  public boolean hasPreviousVariableDeclaration() {
    return previousVariableDeclaration != null;
  }

  protected VariableDeclaration getPreviousVariableDeclaration() {
    return previousVariableDeclaration;
  }

  protected VariableDeclaration getFirstVariableDeclaration() {
    VariableDeclaration firstVariableDeclaration = this;
    while (firstVariableDeclaration.hasPreviousVariableDeclaration()) {
      firstVariableDeclaration = firstVariableDeclaration.getPreviousVariableDeclaration();
    }
    return firstVariableDeclaration;
  }

  @Override
  public int getModifiers() {
    return hasPreviousVariableDeclaration()
            ? getFirstVariableDeclaration().getModifiers()
            : super.getModifiers();
  }

  public boolean isConst() {
    VariableDeclaration firstVariableDeclaration = getFirstVariableDeclaration();
    return firstVariableDeclaration.getOptSymConstOrVar() != null && firstVariableDeclaration.getOptSymConstOrVar().sym == sym.CONST;
  }

  public JooSymbol getOptSymConstOrVar() {
    return optSymConstOrVar;
  }

  public Initializer getOptInitializer() {
    return optInitializer;
  }

  public VariableDeclaration getOptNextVariableDeclaration() {
    return optNextVariableDeclaration;
  }

  public JooSymbol getOptSymSemicolon() {
    return optSymSemicolon;
  }

}
