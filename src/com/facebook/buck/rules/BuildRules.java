/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.rules;

import com.facebook.buck.io.DirectoryTraverser;
import com.facebook.buck.io.DirectoryTraversers;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.util.HumanReadableException;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import javax.annotation.Nullable;

public class BuildRules {

  private BuildRules() {
    // Utility class.
  }

  public static ImmutableSortedSet<BuildRule> toBuildRulesFor(
      BuildTarget invokingBuildTarget,
      BuildRuleResolver ruleResolver,
      Iterable<BuildTarget> buildTargets,
      boolean allowNonExistentRule) {
    ImmutableSortedSet.Builder<BuildRule> buildRules = ImmutableSortedSet.naturalOrder();

    for (BuildTarget target : buildTargets) {
      Optional<BuildRule> buildRule = ruleResolver.getRuleOptional(target);
      if (buildRule.isPresent()) {
        buildRules.add(buildRule.get());
      } else if (!allowNonExistentRule) {
        throw new HumanReadableException("No rule for %s found when processing %s",
            target, invokingBuildTarget.getFullyQualifiedName());
      }
    }

    return buildRules.build();
  }

  /**
   * Helper function for {@link BuildRule}s to create their lists of files for caching.
   */
  public static void addInputsToSortedSet(
      @Nullable Path pathToDirectory,
      ImmutableSortedSet.Builder<Path> inputsToConsiderForCachingPurposes,
      DirectoryTraverser traverser) {
    if (pathToDirectory == null) {
      return;
    }

    Set<Path> files;
    try {
      files = DirectoryTraversers.getInstance().findFiles(pathToDirectory.toString(), traverser);
    } catch (IOException e) {
      throw new RuntimeException("Exception while traversing " + pathToDirectory + ".", e);
    }

    inputsToConsiderForCachingPurposes.addAll(files);
  }

  public static Predicate<BuildRule> isBuildRuleWithTarget(final BuildTarget target) {
    return new Predicate<BuildRule>() {
      @Override
      public boolean apply(BuildRule input) {
        return input.getBuildTarget().equals(target);
      }
    };
  }

  /**
   * @return the set of {@code BuildRule}s exported by {@code ExportDependencies} from the given
   *     rules.
   */
  public static ImmutableSortedSet<BuildRule> getExportedRules(
      Iterable<? extends BuildRule> rules) {
    ImmutableSortedSet.Builder<BuildRule> exportedRules = ImmutableSortedSet.naturalOrder();
    for (ExportDependencies exporter : Iterables.filter(rules, ExportDependencies.class)) {
      exportedRules.addAll(exporter.getExportedDeps());
    }
    return exportedRules.build();
  }

}
