// Copyright (C) 2009 The Android Open Source Project
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

package com.google.gerrit.server.account;

import com.google.gerrit.reviewdb.Account;
import com.google.gerrit.reviewdb.AccountGroup;
import com.google.inject.Inject;

import java.util.Collections;
import java.util.Set;

public final class ToyRealm implements Realm {

	@Override
	public boolean allowsEdit(Account.FieldName field) {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public AuthRequest authenticate(AuthRequest who) throws AccountException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void onCreateAccount(AuthRequest who, Account account) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Set<AccountGroup.Id> groups(AccountState who) {
		return who.getInternalGroups();
	}

	@Override
	public Account.Id lookup(String accountName) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Set<AccountGroup.ExternalNameKey> lookupGroups(String name) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
