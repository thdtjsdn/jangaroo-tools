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

import net.jangaroo.jooc.AnalyzeContext;
import net.jangaroo.jooc.JooSymbol;
import net.jangaroo.jooc.JsWriter;
import net.jangaroo.jooc.Scope;

import java.io.IOException;

/**
 * @author Andreas Gawecki
 */
public class BinaryOpExpr extends OpExpr {

  private Expr arg1;
  private Expr arg2;

  public BinaryOpExpr(Expr arg1, JooSymbol op, Expr arg2) {
    super(op);
    this.setArg1(arg1);
    this.setArg2(arg2);
  }

  @Override
  public void visit(AstVisitor visitor) {
    visitor.visitBinaryOpExpr(this);
  }

  @Override
  public void scope(final Scope scope) {
    getArg1().scope(scope);
    getArg2().scope(scope);
  }

  public void generateJsCode(JsWriter out) throws IOException {
    getArg1().generateCode(out, false);
    out.writeSymbol(getOp());
    getArg2().generateCode(out, false);
  }

  public void analyze(AstNode parentNode, AnalyzeContext context) {
    super.analyze(parentNode, context);
    getArg1().analyze(this, context);
    getArg2().analyze(this, context);
  }

  public JooSymbol getSymbol() {
    return getArg1().getSymbol();
  }

  public boolean isCompileTimeConstant() {
    return getArg1().isCompileTimeConstant() && getArg2().isCompileTimeConstant();
  }

  public Expr getArg1() {
    return arg1;
  }

  public void setArg1(Expr arg1) {
    this.arg1 = arg1;
  }

  public Expr getArg2() {
    return arg2;
  }

  public void setArg2(Expr arg2) {
    this.arg2 = arg2;
  }
}