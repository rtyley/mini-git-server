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

import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.Stage.PRODUCTION;

import com.google.gerrit.lifecycle.LifecycleManager;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gerrit.reviewdb.TrackingId;
import com.google.gerrit.server.config.*;
import com.google.gerrit.server.schema.DataSourceProvider;
import com.google.gerrit.server.schema.DatabaseModule;
import com.google.gerrit.sshd.SshModule;
import com.google.gerrit.sshd.ToySshModule;
import com.google.gerrit.sshd.commands.MasterCommandModule;
import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.spi.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

/** Configures the web application environment for Gerrit Code Review. */
public class WebAppInitializer extends GuiceServletContextListener {
  private static final Logger log =
      LoggerFactory.getLogger(WebAppInitializer.class);

  private File sitePath;
  private Injector cfgInjector;
  private Injector sysInjector;
  private Injector webInjector;
  private Injector sshInjector;
  private LifecycleManager manager;

  private synchronized void init() {
    if (manager == null) {
      final String path = System.getProperty("gerrit.site_path");
      if (path != null) {
        sitePath = new File(path);
      }

      cfgInjector = createCfgInjector();
      sysInjector = createSysInjector();
      sshInjector = createSshInjector();
      // webInjector = createWebInjector();

      // Push the Provider<HttpServletRequest> down into the canonical
      // URL provider. Its optional for that provider, but since we can
      // supply one we should do so, in case the administrator has not
      // setup the canonical URL in the configuration file.
      //
      // Note we have to do this manually as Guice failed to do the
      // injection here because the HTTP environment is not visible
      // to the core server modules.
      //
      sysInjector.getInstance(HttpCanonicalWebUrlProvider.class)
          .setHttpServletRequest(
              webInjector.getProvider(HttpServletRequest.class));

      manager = new LifecycleManager();
      manager.add(sysInjector);
      manager.add(sshInjector);
      //manager.add(webInjector);
    }
  }

  private Injector createCfgInjector() {
    final List<Module> modules = new ArrayList<Module>();
    if (sitePath == null) {
      // If we didn't get the site path from the system property
      // we need to get it from the database, as that's our old
      // method of locating the site path on disk.
      //
      modules.add(new AbstractModule() {
        @Override
        protected void configure() {
          bind(File.class).annotatedWith(SitePath.class).toProvider(
              SitePathFromSystemConfigProvider.class).in(SINGLETON);
        }
      });
      modules.add(new GerritServerConfigModule());
    }


	  
	 System.out.println("Fungle createCfgInjector");
    //modules.add(new AuthConfigModule());
    return Guice.createInjector(PRODUCTION, modules);
  }

  private Injector createSysInjector() {
    final List<Module> modules = new ArrayList<Module>();
    modules.add(cfgInjector.getInstance(ToyGerritGlobalModule.class));
    modules.add(new CanonicalWebUrlModule() {
      @Override
      protected Class<? extends Provider<String>> provider() {
        return HttpCanonicalWebUrlProvider.class;
      }
    });
    // modules.add(new MasterNodeStartup());
    return cfgInjector.createChildInjector(modules);
  }

  private Injector createSshInjector() {
    final List<Module> modules = new ArrayList<Module>();
    modules.add(new ToySshModule());
    return sysInjector.createChildInjector(modules);
  }

  private Injector createWebInjector() {
    final List<Module> modules = new ArrayList<Module>();
    modules.add(sshInjector.getInstance(WebModule.class));
    return sysInjector.createChildInjector(modules);
  }

  @Override
  protected Injector getInjector() {
    init();
    // return webInjector;
	return sshInjector;
  }

  @Override
  public void contextInitialized(final ServletContextEvent event) {
    super.contextInitialized(event);
	System.out.println("WAI contextInitialized - "+event);
    init();
    manager.start();
  }

  @Override
  public void contextDestroyed(final ServletContextEvent event) {
	System.out.println("WAI contextDestroyed - "+event);
    if (manager != null) {
      manager.stop();
      manager = null;
    }
    super.contextDestroyed(event);
  }
}
