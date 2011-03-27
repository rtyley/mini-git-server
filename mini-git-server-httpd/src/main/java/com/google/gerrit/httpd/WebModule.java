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

package com.google.gerrit.httpd;

import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.RemotePeer;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.FactoryModule;
import com.google.gerrit.server.config.GerritRequestModule;
import com.google.gerrit.server.ssh.SshInfo;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.ServletModule;

import javax.annotation.Nullable;
import java.net.SocketAddress;

public class WebModule extends FactoryModule {
  private final Provider<SshInfo> sshInfoProvider;
  private final boolean wantSSL;

  @Inject
  WebModule(final Provider<SshInfo> sshInfoProvider,
      @CanonicalWebUrl @Nullable final String canonicalUrl,
      final Injector creatingInjector) {
    this.sshInfoProvider = sshInfoProvider;
    this.wantSSL = canonicalUrl != null && canonicalUrl.startsWith("https:");
  }

  @Override
  protected void configure() {
    install(new ServletModule() {
      @Override
      protected void configureServlets() {
        filter("/*").through(RequestCleanupFilter.class);
      }
    });

    if (wantSSL) {
      install(new RequireSslFilter.Module());
    }

    install(new UrlModule());
    install(new GerritRequestModule());
    install(new ProjectServlet.Module());

    bind(SshInfo.class).toProvider(sshInfoProvider);

    bind(SocketAddress.class).annotatedWith(RemotePeer.class).toProvider(
        HttpRemotePeerProvider.class).in(RequestScoped.class);

    bind(IdentifiedUser.class).toProvider(HttpIdentifiedUserProvider.class).in(
        RequestScoped.class);
  }
}
