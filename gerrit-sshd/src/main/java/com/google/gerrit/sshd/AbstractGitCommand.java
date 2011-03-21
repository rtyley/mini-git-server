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
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.sshd.SshScope.Context;
import com.google.inject.Inject;
import org.apache.sshd.server.Environment;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.kohsuke.args4j.Argument;

import java.io.IOException;

public abstract class AbstractGitCommand extends BaseCommand {
  @Argument(index = 0, metaVar = "PROJECT.git", required = true, usage = "project name")
  private String repoPath;

  protected String projectName() {
	  String projectName = repoPath;

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

	  return projectName;
  }

  @Inject
  private GitRepositoryManager repoManager;

  @Inject
  private SshSession session;

  @Inject
  private SshScope.Context context;

  @Inject
  private IdentifiedUser user;

  @Inject
  private IdentifiedUser.GenericFactory userFactory;

  protected Repository repo;

  @Override
  public void start(final Environment env) {
    Context ctx = context.subContext(newSession(), context.getCommandLine());
    final Context old = SshScope.set(ctx);
    try {
      startThread(new ProjectCommandRunnable() {
        @Override
        public void executeParseCommand() throws Exception {
          parseCommandLine();
        }

        @Override
        public void run() throws Exception {
          AbstractGitCommand.this.service();
        }

        @Override
        public String getProjectName() {
		  return projectName();
        }
      });
    } finally {
      SshScope.set(old);
    }
  }

  private SshSession newSession() {
    return new SshSession(session, session.getRemoteAddress(), userFactory
        .create(user.getUserName()));
  }

  private void service() throws IOException, Failure {

    try {
      repo = repoManager.openRepository(projectName());
    } catch (RepositoryNotFoundException e) {
      throw new Failure(1, "fatal: '" + projectName() + "': not a git archive", e);
    }

    try {
      runImpl();
    } finally {
      repo.close();
    }
  }

  protected abstract void runImpl() throws IOException, Failure;
}
