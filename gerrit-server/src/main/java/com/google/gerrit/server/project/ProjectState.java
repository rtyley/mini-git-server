// Copyright (C) 2008 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.server.project;

import com.google.gerrit.reviewdb.AccountGroup;
import com.google.gerrit.reviewdb.ApprovalCategory;
import com.google.gerrit.reviewdb.Project;
import com.google.gerrit.reviewdb.RefRight;
import com.google.gerrit.server.AnonymousUser;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.WildProjectName;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/** Cached information on a project. */
public class ProjectState {
  public interface Factory {
    ProjectState create(Project project, Collection<RefRight> localRights);
  }

  private final AnonymousUser anonymousUser;
  private final Project.NameKey wildProject;
  private final ProjectCache projectCache;
  private final ProjectControl.AssistedFactory projectControlFactory;

  private final Project project;
  private final Collection<RefRight> localRights;
  private final Set<AccountGroup.Id> owners;

  private volatile Collection<RefRight> inheritedRights;

  @Inject
  protected ProjectState(final AnonymousUser anonymousUser,
      final ProjectCache projectCache,
      @WildProjectName final Project.NameKey wildProject,
      final ProjectControl.AssistedFactory projectControlFactory,
      @Assisted final Project project,
      @Assisted Collection<RefRight> rights) {
    this.anonymousUser = anonymousUser;
    this.projectCache = projectCache;
    this.wildProject = wildProject;
    this.projectControlFactory = projectControlFactory;

    if (wildProject.equals(project.getNameKey())) {
      rights = new ArrayList<RefRight>(rights);
      for (Iterator<RefRight> itr = rights.iterator(); itr.hasNext();) {
        if (!itr.next().getApprovalCategoryId().canBeOnWildProject()) {
          itr.remove();
        }
      }
      rights = Collections.unmodifiableCollection(rights);
    }

    this.project = project;
    this.localRights = rights;

    final HashSet<AccountGroup.Id> groups = new HashSet<AccountGroup.Id>();
    for (final RefRight right : rights) {
      if (ApprovalCategory.OWN.equals(right.getApprovalCategoryId())
          && right.getMaxValue() > 0) {
        groups.add(right.getAccountGroupId());
      }
    }
    owners = Collections.unmodifiableSet(groups);
  }

  public Project getProject() {
    return project;
  }

  private Collection<RefRight> getWildProjectRights() {
    final ProjectState s = projectCache.get(wildProject);
    return s != null ? s.getLocalRights() : Collections.<RefRight> emptyList();
  }


  /** Is this the special wild project which manages inherited rights? */
  public boolean isSpecialWildProject() {
    return project.getNameKey().equals(wildProject);
  }

  public ProjectControl controlForAnonymousUser() {
    return controlFor(anonymousUser);
  }

  public ProjectControl controlFor(final CurrentUser user) {
    return projectControlFactory.create(user, this);
  }

}
