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

package com.google.gerrit.sshd;

import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;

@Singleton
class SshIdentifiedUserProvider implements Provider<IdentifiedUser> {
  private final Provider<SshSession> session;
  private final IdentifiedUser.GenericFactory factory;

  @Inject
  SshIdentifiedUserProvider(Provider<SshSession> s,
      IdentifiedUser.GenericFactory f) {
    session = s;
    factory = f;
  }

  @Override
  public IdentifiedUser get() {
    final CurrentUser user = session.get().getCurrentUser();
    if (user instanceof IdentifiedUser) {
      return factory.create(((IdentifiedUser) user).getUserName());
    }
    throw new ProvisionException("Not signed in SshIdentifiedUserProvider");
  }
}
