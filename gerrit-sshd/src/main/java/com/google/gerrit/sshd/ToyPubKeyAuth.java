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

package com.google.gerrit.sshd;

import com.google.gerrit.server.IdentifiedUser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import java.security.PublicKey;

/**
 * Authenticates by public key really permissively
 */
@Singleton
class ToyPubKeyAuth implements PublickeyAuthenticator {
private final IdentifiedUser.GenericFactory userFactory;


  @Inject
  ToyPubKeyAuth(IdentifiedUser.GenericFactory uf) {
    userFactory = uf;
  }
	@Override
	public boolean authenticate(String username, PublicKey publicKey, ServerSession serverSession) {
		final SshSession sd = serverSession.getAttribute(SshSession.KEY);
		sd.authenticationSuccess(username, userFactory.create(username));
		return true;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
