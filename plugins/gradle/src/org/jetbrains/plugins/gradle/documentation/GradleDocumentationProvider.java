/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.documentation;

import com.intellij.codeInsight.javadoc.JavaDocUtil;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.gradle.util.GradleDocumentationBundle;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladislav.Soroka
 * @since 8/29/13
 */
public class GradleDocumentationProvider implements DocumentationProvider {

  @Nullable
  @Override
  public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
    return null;
  }

  @Nullable
  @Override
  public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
    List<String> result = new ArrayList<String>();
    return result.isEmpty() ? null : result;
  }

  @Nullable
  @Override
  public String generateDoc(PsiElement element, PsiElement originalElement) {
    String result = null;

    PsiFile file = element.getContainingFile();
    if (file == null || !file.getName().endsWith(GradleConstants.EXTENSION)) {
      return null;
    }

    if (element instanceof GrLiteral) {
      GrLiteral grLiteral = (GrLiteral)element;
      PsiElement stmt = PsiTreeUtil.findFirstParent(grLiteral, new Condition<PsiElement>() {
        @Override
        public boolean value(PsiElement psiElement) {
          return psiElement instanceof GrCall;
        }
      });
      if (stmt instanceof GrCall) {
        GrCall grCall = (GrCall)stmt;
        PsiMethod psiMethod = grCall.resolveMethod();
        if (psiMethod != null && psiMethod.getContainingClass() != null) {
          //noinspection ConstantConditions
          String qualifiedName = psiMethod.getContainingClass().getQualifiedName();
          if (grLiteral.getParent() instanceof GrNamedArgument) {
            GrNamedArgument namedArgument = (GrNamedArgument)grLiteral.getParent();
            String key = StringUtil.join(new String[]{
              "gradle.documentation",
              qualifiedName,
              psiMethod.getName(),
              namedArgument.getLabelName(),
              String.valueOf(grLiteral.getValue()),
            }, "."
            );

            String bndMsg = GradleDocumentationBundle.messageOrDefault(key, "");
            result = bndMsg.isEmpty() ? null : bndMsg;
          }
        }
      }
    }
    return result;
  }

  @Nullable
  @Override
  public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
    return null;
  }

  @Nullable
  @Override
  public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
    return JavaDocUtil.findReferenceTarget(psiManager, link, context);
  }
}