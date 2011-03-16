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

import com.google.gerrit.reviewdb.*;
import com.google.gerrit.server.cache.CacheModule;
import com.google.gerrit.server.config.AuthConfig;
import com.google.gwtorm.client.OrmException;
import com.google.gwtorm.client.SchemaFactory;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;

import java.util.Collections;
import java.util.Set;

import static com.google.inject.internal.Sets.newHashSet;

/** Caches important (but small) account state to avoid database hits. */
@Singleton
public class ToyAccountCacheImpl implements AccountCache {
	private AuthConfig auth;

	public static Module module() {
    return new CacheModule() {
      @Override
      protected void configure() {
        bind(AccountCache.class).to(ToyAccountCacheImpl.class);
      }
    };
  }

	@Inject
	public ToyAccountCacheImpl(AuthConfig auth) {

		this.auth = auth;
	}

  public AccountState get(final Account.Id accountId) {
	  Account account = new Account(accountId);
	  account.setUserName("fred");

	  AccountGroup.Id groupId=new AccountGroup.Id(1);

	  Set<AccountGroup.Id> actualGroups = newHashSet();
	  actualGroups.add(groupId);
	  return new AccountState(account, actualGroups, Collections.<AccountExternalId>emptySet());
  }

  @Override
  public AccountState getByUsername(String username) {
    return null;
  }

  public void evict(final Account.Id accountId) {
  }

  public void evictByUsername(String username) {
  }

}
