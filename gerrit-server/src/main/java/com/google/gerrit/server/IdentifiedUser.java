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

import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.inject.Inject;
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
import java.util.Date;
import java.util.TimeZone;

/** An authenticated user. */
public class IdentifiedUser extends CurrentUser {
	private final String username;

	/** Create an IdentifiedUser, ignoring any per-request state. */
  @Singleton
  public static class GenericFactory {
    public IdentifiedUser create(String username) {
      return new IdentifiedUser(username);
    }
  }

  private static final Logger log =
      LoggerFactory.getLogger(IdentifiedUser.class);

  private IdentifiedUser(String username) {
	  this.username = username;
  }

  /** @return the user's user name; null if one has not been selected/assigned. */
  public String getUserName() {
    return username;
  }

  public PersonIdent newRefLogIdent() {
    return newRefLogIdent(new Date(), TimeZone.getDefault());
  }

  public PersonIdent newRefLogIdent(final Date when, final TimeZone tz) {
    String user = getUserName();

    String host = "unknown";

    return new PersonIdent(user, user + "@" + host, when, tz);
  }

  public PersonIdent newCommitterIdent(final Date when, final TimeZone tz) {

      String user = getUserName();

      String host= "unknown";

      String email = user + "@" + host;

    return new PersonIdent(user, email, when, tz);
  }

  @Override
  public String toString() {
    return "IdentifiedUser[" + getUserName() + "]";
  }
}
