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

import net.jangaroo.jooc.CodeGenerator;
import net.jangaroo.jooc.JooSymbol;
import net.jangaroo.jooc.JsWriter;

import java.io.IOException;

/**
 * @author Andreas Gawecki
 */
public class Parameters extends CommaSeparatedList<Parameter> {

  public Parameters getTail() {
    return (Parameters) super.getTail();
  }

  public Parameters(Parameter param, JooSymbol symComma, Parameters tail) {
    super(param, symComma, tail);
  }

  public Parameters(Parameter param) {
    this(param, null, null);
  }

  @Override
  public void visit(AstVisitor visitor) {
    visitor.visitParameters(this);
  }

  protected void generateTailAsJsCode(JsWriter out) throws IOException {
    if (!super.getTail().getHead().isRest()) {
      super.generateTailAsJsCode(out);
    } else {
      out.beginCommentWriteSymbol(getSymComma());
      super.getTail().generateJsCode(out);
      out.endComment();
    }
  }

  public CodeGenerator getParameterInitializerCodeGenerator() {
    return new CodeGenerator() {
      public void generateCode(JsWriter out, boolean generateApi) throws IOException {
        // first pass: generate conditionals and count parameters.
        int cnt = 0;
        StringBuilder code = new StringBuilder();
        for (Parameters parameters = Parameters.this; parameters!=null; parameters = parameters.getTail()) {
          Parameter param = parameters.getHead();
          if (param.isRest()) {
            break;
          }
          if (param.hasInitializer()) {
            code.insert(0,"if(arguments.length<"+(cnt+1)+"){");
          }
          ++cnt;
        }
        out.write(code.toString());
        // second pass: generate initializers and rest param code.
        for (Parameters parameters = Parameters.this; parameters!=null; parameters = parameters.getTail()) {
          Parameter param = parameters.getHead();
          if (param.isRest()) {
            param.generateRestParamCode(out, cnt);
            break;
          }
          if (param.hasInitializer()) {
            param.generateBodyInitializerCode(out, generateApi);
            out.write("}");
          }
        }
      }

      @Override
      public void generateJsCode(JsWriter out) throws IOException {
        generateCode(out, false);
      }

      @Override
      public void generateAsApiCode(JsWriter out) throws IOException {
        generateCode(out, true);
      }
    };
  }

  public String getRestParamName() {
    if (getHead().isRest()) {
      return getHead().getName();
    }
    return super.getTail() ==null ? null : getTail().getRestParamName();
  }

  public int getOtherParamCount() {
    if (getHead().isRest()) {
      return 0;
    }
    int tailOtherParamCount = super.getTail() ==null ? -1 : getTail().getOtherParamCount();
    return tailOtherParamCount==-1 ? tailOtherParamCount : 1+tailOtherParamCount;
  }
}