// Copyright (C) 2010 The Android Open Source Project
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
import com.google.gerrit.server.config.*;
import com.google.inject.TypeLiteral;
import com.google.inject.internal.Sets;

import java.util.Set;

import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.internal.Sets.newHashSet;

public class ToyAccessControlModule extends FactoryModule {
  @Override
  protected void configure() {
//    bind(new TypeLiteral<Set<AccountGroup.Id>>() {}) //
//        .annotatedWith(ProjectCreatorGroups.class) //
//        .toProvider(ProjectCreatorGroupsProvider.class).in(SINGLETON);
//
//    bind(new TypeLiteral<Set<AccountGroup.Id>>() {}) //
//        .annotatedWith(ProjectOwnerGroups.class) //
//        .toProvider(ProjectOwnerGroupsProvider.class).in(SINGLETON);
//
	Set<AccountGroup.Id> uploadersAndDownloaders = newHashSet();
	uploadersAndDownloaders.add(new AccountGroup.Id(1));
    bind(new TypeLiteral<Set<AccountGroup.Id>>() {}) //
        .annotatedWith(GitUploadPackGroups.class) //
        .toInstance(uploadersAndDownloaders);

    bind(new TypeLiteral<Set<AccountGroup.Id>>() {}) //
        .annotatedWith(GitReceivePackGroups.class) //
        .toInstance(uploadersAndDownloaders);

    factory(ProjectControl.AssistedFactory.class);
  }
}
