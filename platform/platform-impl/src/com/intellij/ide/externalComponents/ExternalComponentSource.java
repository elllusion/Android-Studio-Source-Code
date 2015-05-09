/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.ide.externalComponents;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Interface for classes that can provide information on and updates for installed components.
 */
public interface ExternalComponentSource {
  /**
   * Retrieve information on the updates that this source can provide.
   * @param indicator A {@link ProgressIndicator} that can be updated to show progress, or can be used to cancel the process.
   * @return A Collection of {@link UpdatableExternalComponent}s representing the available updates.
   */
  @NotNull
  Collection<UpdatableExternalComponent> getAvailableVersions(@Nullable ProgressIndicator indicator);

  /**
   * Retrieve information on currently installed components.
   * @return A Collection of currently installed {@link UpdatableExternalComponent}s.
   */
  @NotNull
  Collection<UpdatableExternalComponent> getCurrentVersions();

  /**
   * Install updates for the given {@link UpdatableExternalComponent}s.
   * @param request
   */
  void installUpdates(@NotNull Collection<UpdatableExternalComponent> request);

  /**
   * Gets a human-readable name for this source.
   * @return The name.
   */
  @NotNull
  String getName();

  @NotNull
  Collection<? extends Pair<String,String>> getStatuses();
}
