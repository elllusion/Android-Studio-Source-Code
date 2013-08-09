/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.ide.util.gotoByName;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.ProjectScope;
import org.jetbrains.annotations.NotNull;

public class DefaultFileNavigationContributor implements ChooseByNameContributor, DumbAware {

  @Override
  @NotNull
  public String[] getNames(Project project, boolean includeNonProjectItems) {
    return FilenameIndex.getAllFilenames(project);
  }

  @Override
  @NotNull
  public NavigationItem[] getItemsByName(String name, final String pattern, Project project, boolean includeNonProjectItems) {
    final boolean includeDirs = pattern.endsWith("/") || pattern.endsWith("\\");
    return FilenameIndex.getFilesByName(project, name,
                                        includeNonProjectItems ? ProjectScope.getAllScope(project) : ProjectScope.getProjectScope(project),
                                        includeDirs);
  }
}
