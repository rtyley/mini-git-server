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

package com.google.gerrit.server;

import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.Realm;
import com.google.gerrit.server.config.AuthConfig;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.inject.Inject;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.util.SystemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.Nullable;

/** An authenticated user. */
public class IdentifiedUser extends CurrentUser {
  /** Create an IdentifiedUser, ignoring any per-request state. */
  @Singleton
  public static class GenericFactory {
    private final AuthConfig authConfig;
    private final Provider<String> canonicalUrl;
    private final Realm realm;
    private final AccountCache accountCache;

    @Inject
    GenericFactory(final AuthConfig authConfig,
        final @CanonicalWebUrl Provider<String> canonicalUrl,
        final Realm realm, final AccountCache accountCache) {
      this.authConfig = authConfig;
      this.canonicalUrl = canonicalUrl;
      this.realm = realm;
      this.accountCache = accountCache;
    }

    public IdentifiedUser create(final Account.Id id) {
      return create(AccessPath.UNKNOWN, null, id);
    }

    public IdentifiedUser create(Provider<ReviewDb> db, Account.Id id) {
      return new IdentifiedUser(AccessPath.UNKNOWN, authConfig, canonicalUrl,
          realm, accountCache, null, db, id);
    }

    public IdentifiedUser create(AccessPath accessPath,
        Provider<SocketAddress> remotePeerProvider, Account.Id id) {
      return new IdentifiedUser(accessPath, authConfig, canonicalUrl, realm,
          accountCache, remotePeerProvider, null, id);
    }
  }

  /**
   * Create an IdentifiedUser, relying on current request state.
   * <p>
   * Can only be used from within a module that has defined request scoped
   * {@code @RemotePeer SocketAddress} and {@code ReviewDb} providers.
   */
  @Singleton
  public static class RequestFactory {
    private final AuthConfig authConfig;
    private final Provider<String> canonicalUrl;
    private final Realm realm;
    private final AccountCache accountCache;

    @Inject
    RequestFactory(final AuthConfig authConfig,
        final @CanonicalWebUrl Provider<String> canonicalUrl,
        final Realm realm, final AccountCache accountCache) {
      this.authConfig = authConfig;
      this.canonicalUrl = canonicalUrl;
      this.realm = realm;
      this.accountCache = accountCache;

    }

    public IdentifiedUser create(final AccessPath accessPath,
        final Account.Id id) {
      return new IdentifiedUser(accessPath, authConfig, canonicalUrl, realm,
          accountCache, id);
    }
  }

  private static final Logger log =
      LoggerFactory.getLogger(IdentifiedUser.class);

  private final Provider<String> canonicalUrl;
  private final Realm realm;
  private final AccountCache accountCache;

  private final Account.Id accountId;

  private AccountState state;

  private IdentifiedUser(final AccessPath accessPath,
      final AuthConfig authConfig, final Provider<String> canonicalUrl,
      final Realm realm, final AccountCache accountCache, final Account.Id id) {
    super(accessPath, authConfig);
    this.canonicalUrl = canonicalUrl;
    this.realm = realm;
    this.accountCache = accountCache;
    this.accountId = id;
  }

  private AccountState state() {
    if (state == null) {
      state = accountCache.get(getAccountId());
    }
    return state;
  }

  /** The account identity for the user. */
  public Account.Id getAccountId() {
    return accountId;
  }

  /** @return the user's user name; null if one has not been selected/assigned. */
  public String getUserName() {
    return state().getUserName();
  }

  public Account getAccount() {
    return state().getAccount();
  }

  public PersonIdent newRefLogIdent() {
    return newRefLogIdent(new Date(), TimeZone.getDefault());
  }

  public PersonIdent newRefLogIdent(final Date when, final TimeZone tz) {
    final Account ua = getAccount();

    String name = ua.getFullName();
    if (name == null || name.isEmpty()) {
      name = ua.getPreferredEmail();
    }
    if (name == null || name.isEmpty()) {
      name = "Anonymous Coward";
    }

    String user = getUserName();
    if (user == null) {
      user = "";
    }
    user = user + "|" + "account-" + ua.getId().toString();

    String host = null;
    if (remotePeerProvider != null) {
      final SocketAddress remotePeer = remotePeerProvider.get();
      if (remotePeer instanceof InetSocketAddress) {
        final InetSocketAddress sa = (InetSocketAddress) remotePeer;
        final InetAddress in = sa.getAddress();

        host = in != null ? in.getCanonicalHostName() : sa.getHostName();
      }
    }
    if (host == null || host.isEmpty()) {
      host = "unknown";
    }

    return new PersonIdent(name, user + "@" + host, when, tz);
  }

  public PersonIdent newCommitterIdent(final Date when, final TimeZone tz) {
    final Account ua = getAccount();
    String name = ua.getFullName();
    String email = ua.getPreferredEmail();

    if (email == null || email.isEmpty()) {
      // No preferred email is configured. Use a generic identity so we
      // don't leak an address the user may have given us, but doesn't
      // necessarily want to publish through Git records.
      //
      String user = getUserName();
      if (user == null || user.isEmpty()) {
        user = "account-" + ua.getId().toString();
      }

      String host;
      if (canonicalUrl.get() != null) {
        try {
          host = new URL(canonicalUrl.get()).getHost();
        } catch (MalformedURLException e) {
          host = SystemReader.getInstance().getHostname();
        }
      } else {
        host = SystemReader.getInstance().getHostname();
      }

      email = user + "@" + host;
    }

    if (name == null || name.isEmpty()) {
      final int at = email.indexOf('@');
      if (0 < at) {
        name = email.substring(0, at);
      } else {
        name = "Anonymous Coward";
      }
    }

    return new PersonIdent(name, email, when, tz);
  }

  @Override
  public String toString() {
    return "IdentifiedUser[account " + getAccountId() + "]";
  }
}
