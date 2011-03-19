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

package com.google.gerrit.httpd;

import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.TransferConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.http.server.resolver.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.pack.PackConfig;
import org.eclipse.jgit.transport.UploadPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Serves Git repositories over HTTP. */
@Singleton
public class ProjectServlet extends GitServlet {
  private static final Logger log =
      LoggerFactory.getLogger(ProjectServlet.class);

  private static final String ATT_CONTROL = "Project";

  static class Module extends AbstractModule {
    @Override
    protected void configure() {
      bind(Resolver.class);
      bind(Upload.class);
    }
  }

  static String getProject(HttpServletRequest req)
      throws ServiceNotEnabledException {
    String pc = (String) req.getAttribute(ATT_CONTROL);
    if (pc == null) {
      log.error("No " + ATT_CONTROL + " in request", new Exception("here"));
      throw new ServiceNotEnabledException();
    }
    return pc;
  }

  private final Provider<String> urlProvider;

  @Inject
  ProjectServlet(final Resolver resolver, final Upload upload,
      @CanonicalWebUrl @Nullable Provider<String> urlProvider) {
    this.urlProvider = urlProvider;

    setRepositoryResolver(resolver);
    setAsIsFileService(AsIsFileService.DISABLED);
    setUploadPackFactory(upload);
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    serveRegex("^/(.*?)/?$").with(new HttpServlet() {
      @Override
      protected void doGet(HttpServletRequest req, HttpServletResponse rsp)
          throws IOException {
        String pc;
        try {
          pc = getProject(req);
        } catch (ServiceNotEnabledException e) {
          rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
          return;
        }
        StringBuilder r = new StringBuilder();
        r.append(urlProvider.get());
        rsp.sendRedirect(r.toString());
      }
    });
  }

  static class Resolver implements RepositoryResolver {
    private final GitRepositoryManager manager;
    @Inject
    Resolver(GitRepositoryManager manager) {
      this.manager = manager;
    }

    @Override
    public Repository open(HttpServletRequest req, String projectName)
        throws RepositoryNotFoundException, ServiceNotAuthorizedException,
        ServiceNotEnabledException {
      if (projectName.endsWith(".git")) {
        // Be nice and drop the trailing ".git" suffix, which we never keep
        // in our database, but clients might mistakenly provide anyway.
        //
        projectName = projectName.substring(0, projectName.length() - 4);
      }

      if (projectName.startsWith("/")) {
        // Be nice and drop the leading "/" if supplied by an absolute path.
        // We don't have a file system hierarchy, just a flat namespace in
        // the database's Project entities. We never encode these with a
        // leading '/' but users might accidentally include them in Git URLs.
        //
        projectName = projectName.substring(1);
      }
		
      req.setAttribute(ATT_CONTROL, projectName);

      return manager.openRepository(projectName);
    }
  }

  static class Upload implements UploadPackFactory {
    private final PackConfig packConfig;

    @Inject
    Upload(final TransferConfig tc) {
      this.packConfig = tc.getPackConfig();
    }

    @Override
    public UploadPack create(HttpServletRequest req, Repository repo)
        throws ServiceNotEnabledException, ServiceNotAuthorizedException {

      // The Resolver above already checked READ access for us.
      //
      UploadPack up = new UploadPack(repo);
      up.setPackConfig(packConfig);
      return up;
    }
  }

}
