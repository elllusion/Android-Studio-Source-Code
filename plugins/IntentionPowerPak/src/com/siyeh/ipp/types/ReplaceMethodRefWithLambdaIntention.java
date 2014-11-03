/*
 * Copyright 2011 Bas Leijdekkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ipp.types;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.RedundantCastUtil;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.text.UniqueNameGenerator;
import com.siyeh.ipp.base.Intention;
import com.siyeh.ipp.base.PsiElementPredicate;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ReplaceMethodRefWithLambdaIntention extends Intention {
  private static final Logger LOG = Logger.getInstance("#" + ReplaceMethodRefWithLambdaIntention.class.getName());

  @NotNull
  @Override
  protected PsiElementPredicate getElementPredicate() {
    return new MethodRefPredicate();
  }

  @Override
  protected void processIntention(@NotNull PsiElement element) throws IncorrectOperationException {
    final PsiMethodReferenceExpression referenceExpression = PsiTreeUtil.getParentOfType(element, PsiMethodReferenceExpression.class);
    LOG.assertTrue(referenceExpression != null);
    final PsiElement resolve = referenceExpression.resolve();
    final boolean isReceiver = resolve instanceof PsiMethod && PsiMethodReferenceUtil.hasReceiver(referenceExpression, (PsiMethod)resolve);
    final PsiParameter[] psiParameters = resolve instanceof PsiMethod ? ((PsiMethod)resolve).getParameterList().getParameters() : null;
    final PsiType functionalInterfaceType = referenceExpression.getFunctionalInterfaceType();
    final PsiClassType.ClassResolveResult functionalInterfaceResolveResult = PsiUtil.resolveGenericsClassInType(functionalInterfaceType);
    final PsiMethod interfaceMethod = LambdaUtil.getFunctionalInterfaceMethod(functionalInterfaceType);
    LOG.assertTrue(interfaceMethod != null);
    final PsiSubstitutor psiSubstitutor = LambdaUtil.getSubstitutor(interfaceMethod, functionalInterfaceResolveResult);
    final StringBuilder buf = new StringBuilder("(");
    LOG.assertTrue(functionalInterfaceType != null);
    buf.append(functionalInterfaceType.getCanonicalText()).append(")(");
    final PsiParameterList parameterList = interfaceMethod.getParameterList();
    final PsiParameter[] parameters = parameterList.getParameters();

    final Map<PsiParameter, String> map = new HashMap<PsiParameter, String>();
    final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
    final JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(element.getProject());
    final String paramsString = StringUtil.join(parameters, new Function<PsiParameter, String>() {
      @Override
      public String fun(PsiParameter parameter) {
        final int parameterIndex = parameterList.getParameterIndex(parameter);
        String baseName;
        if (isReceiver && parameterIndex == 0) {
          final SuggestedNameInfo nameInfo = codeStyleManager.suggestVariableName(VariableKind.PARAMETER, null, null, psiSubstitutor.substitute(parameter.getType()));
          baseName = nameInfo.names.length > 0 ? nameInfo.names[0] : parameter.getName();
        }
        else {
          final String initialName = psiParameters != null ? psiParameters[parameterIndex - (isReceiver ? 1 : 0)].getName() : parameter.getName();
          baseName = codeStyleManager.variableNameToPropertyName(initialName, VariableKind.PARAMETER);
        } 

        if (baseName != null) {
          String parameterName = nameGenerator.generateUniqueName(codeStyleManager.suggestUniqueVariableName(baseName, referenceExpression, true));
          map.put(parameter, parameterName);
          return parameterName;
        }
        return "";
      }
    }, ", ");
    buf.append(paramsString);
    buf.append(") -> ");


    final JavaResolveResult resolveResult = referenceExpression.advancedResolve(false);
    final PsiElement resolveElement = resolveResult.getElement();
    if (resolveElement instanceof PsiMember) {
      boolean needBraces = interfaceMethod.getReturnType() == PsiType.VOID && !(resolveElement instanceof PsiMethod && ((PsiMethod)resolveElement).getReturnType() == PsiType.VOID);

      if (needBraces) {
        buf.append("{");
      }
      final PsiElement qualifier = referenceExpression.getQualifier();
      PsiClass containingClass = null;
      if (resolveElement instanceof PsiMethod) {
        containingClass = ((PsiMember)resolveElement).getContainingClass();
        LOG.assertTrue(containingClass != null);
      } else if (resolveElement instanceof PsiClass) {
        containingClass = (PsiClass)resolveElement;
      }

      final boolean onArrayRef =
        JavaPsiFacade.getElementFactory(element.getProject()).getArrayClass(PsiUtil.getLanguageLevel(element)) == containingClass;

      final PsiElement referenceNameElement = referenceExpression.getReferenceNameElement();
      if (isReceiver){
        buf.append(map.get(parameters[0])).append(".");
      } else {
        if (!(referenceNameElement instanceof PsiKeyword)) {
          if (qualifier instanceof PsiTypeElement) {
            final PsiJavaCodeReferenceElement referenceElement = ((PsiTypeElement)qualifier).getInnermostComponentReferenceElement();
            LOG.assertTrue(referenceElement != null);
            buf.append(referenceElement.getReferenceName()).append(".");
          }
          else if (qualifier != null && !(qualifier instanceof PsiThisExpression && ((PsiThisExpression)qualifier).getQualifier() == null)) {
            buf.append(qualifier.getText()).append(".");
          }
        }
      } 

      //new or method name
      buf.append(referenceExpression.getReferenceName());

      if (referenceNameElement instanceof PsiKeyword) {
        //class name
        buf.append(" ");
        if (onArrayRef) {
          if (qualifier instanceof PsiTypeElement) {
            final PsiType type = ((PsiTypeElement)qualifier).getType();
            int dim = type.getArrayDimensions();
            buf.append(type.getDeepComponentType().getCanonicalText());
            buf.append("[");
            buf.append(map.get(parameters[0]));
            buf.append("]");
            while (--dim > 0) {
              buf.append("[]");
            }
          }
        } else {
          buf.append(((PsiMember)resolveElement).getName());

          final PsiSubstitutor substitutor = resolveResult.getSubstitutor();

          LOG.assertTrue(containingClass != null);
          if (containingClass.hasTypeParameters() && !PsiUtil.isRawSubstitutor(containingClass, substitutor)) {
            buf.append("<").append(StringUtil.join(containingClass.getTypeParameters(), new Function<PsiTypeParameter, String>() {
              @Override
              public String fun(PsiTypeParameter parameter) {
                final PsiType psiType = substitutor.substitute(parameter);
                LOG.assertTrue(psiType != null);
                return psiType.getCanonicalText();
              }
            }, ", ")).append(">");
          }
        }
      }

      if (!onArrayRef || isReceiver) {
        //param list
        buf.append("(");
        boolean first = true;
        for (int i = isReceiver ? 1 : 0; i < parameters.length; i++) {
          PsiParameter parameter = parameters[i];
          if (!first) {
            buf.append(", ");
          } else {
            first = false;
          }
          buf.append(map.get(parameter));
        }
        buf.append(")");
      }

      if (needBraces) {
        buf.append(";}");
      }
    }


    final PsiTypeCastExpression typeCastExpression = (PsiTypeCastExpression)referenceExpression
      .replace(JavaPsiFacade.getElementFactory(element.getProject()).createExpressionFromText(buf.toString(), referenceExpression));
    if (RedundantCastUtil.isCastRedundant(typeCastExpression)) {
      final PsiExpression operand = typeCastExpression.getOperand();
      LOG.assertTrue(operand != null);
      typeCastExpression.replace(operand);
    }
  }

  private static class MethodRefPredicate implements PsiElementPredicate {
    @Override
    public boolean satisfiedBy(PsiElement element) {
      final PsiMethodReferenceExpression methodReferenceExpression = PsiTreeUtil.getParentOfType(element, PsiMethodReferenceExpression.class);
      if (methodReferenceExpression != null) {
        final PsiType interfaceType = methodReferenceExpression.getFunctionalInterfaceType();
        if (interfaceType != null &&
            LambdaUtil.getFunctionalInterfaceMethod(interfaceType) != null &&
            methodReferenceExpression.resolve() != null) {
          return true;
        }
      }
      return false;
    }
  }
}
