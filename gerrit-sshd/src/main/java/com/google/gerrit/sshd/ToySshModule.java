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

package com.google.gerrit.sshd;

import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gerrit.reviewdb.*;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.RemotePeer;
import com.google.gerrit.server.config.*;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.LocalDiskRepositoryManager;
import com.google.gerrit.server.git.WorkQueue;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.ssh.SshInfo;
import com.google.gerrit.sshd.args4j.*;
import com.google.gerrit.sshd.commands.ToyDefaultCommandModule;
import com.google.gerrit.util.cli.CmdLineParser;
import com.google.gerrit.util.cli.OptionHandlerFactory;
import com.google.gerrit.util.cli.OptionHandlerUtil;
import com.google.gwtjsonrpc.server.SignedToken;
import com.google.gwtorm.client.*;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;
import com.google.inject.servlet.RequestScoped;
import org.apache.sshd.common.KeyPairProvider;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.kohsuke.args4j.spi.OptionHandler;

import java.net.SocketAddress;

import static com.google.inject.Scopes.SINGLETON;

/** Configures standard dependencies for {@link com.google.gerrit.sshd.SshDaemon}. */
public class ToySshModule extends FactoryModule {
  @Override
  protected void configure() {
	  // bind(File.class).annotatedWith(SitePath.class).toInstance(new File("/tmp"));
	  // Already done in GerritServerConfigModule
	  /*

		//install(new GerritServerConfigModule());
		*/
	  SecurityUtils.setRegisterBouncyCastle(true);
	  bindScope(RequestScoped.class, SshScope.REQUEST);

	  final SystemConfig systemConfig = SystemConfig.create();
	  systemConfig.adminGroupId=new AccountGroup.Id(1);
	  systemConfig.registerEmailPrivateKey = SignedToken.generateRandomKey();


	  bind(GitRepositoryManager.class).to(LocalDiskRepositoryManager.class);
	  configureRequestScope();
	  configureCmdLineParser();
	install(ToyAccountCacheImpl.module());
	  bind(Realm.class).to(ToyRealm.class);
    install(SshKeyCacheImpl.module());
    bind(SshLog.class);
    bind(SshInfo.class).to(SshDaemon.class).in(SINGLETON);
    factory(DispatchCommand.Factory.class);
    factory(PeerDaemonUser.Factory.class);

    bind(DispatchCommandProvider.class).annotatedWith(Commands.CMD_ROOT)
        .toInstance(new DispatchCommandProvider("", Commands.CMD_ROOT));
    bind(CommandFactoryProvider.class);
    bind(CommandFactory.class).toProvider(CommandFactoryProvider.class);
    bind(WorkQueue.Executor.class).annotatedWith(StreamCommandExecutor.class)
        .toProvider(StreamCommandExecutorProvider.class).in(SINGLETON);
    bind(QueueProvider.class).to(CommandExecutorQueueProvider.class).in(SINGLETON);

    bind(PublickeyAuthenticator.class).to(ToyPubKeyAuth.class);
    bind(KeyPairProvider.class).toProvider(HostKeyProvider.class).in(SINGLETON);

    install(new ToyDefaultCommandModule());

    install(new LifecycleModule() {
      @Override
      protected void configure() {
        listener().to(SshLog.class);
        listener().to(SshDaemon.class);
      }
    });
  }

  private void configureRequestScope() {
    bind(SshScope.Context.class).toProvider(SshScope.ContextProvider.class);

    bind(SshSession.class).toProvider(SshScope.SshSessionProvider.class).in(
        SshScope.REQUEST);
    bind(SocketAddress.class).annotatedWith(RemotePeer.class).toProvider(
        SshRemotePeerProvider.class).in(SshScope.REQUEST);

    bind(CurrentUser.class).toProvider(SshCurrentUserProvider.class).in(
        SshScope.REQUEST);
    bind(IdentifiedUser.class).toProvider(SshIdentifiedUserProvider.class).in(
        SshScope.REQUEST);

    bind(WorkQueue.Executor.class).annotatedWith(CommandExecutor.class)
        .toProvider(CommandExecutorProvider.class).in(SshScope.REQUEST);

    // install(new GerritRequestModule());
  }

  private void configureCmdLineParser() {
    factory(CmdLineParser.Factory.class);

    // registerOptionHandler(Account.Id.class, AccountIdHandler.class);
    // registerOptionHandler(AccountGroup.Id.class, AccountGroupIdHandler.class);
    registerOptionHandler(PatchSet.Id.class, PatchSetIdHandler.class);
    registerOptionHandler(ProjectControl.class, ProjectControlHandler.class);
    registerOptionHandler(SocketAddress.class, SocketAddressHandler.class);
  }

  private <T> void registerOptionHandler(Class<T> type,
      Class<? extends OptionHandler<T>> impl) {
    final Key<OptionHandlerFactory<T>> key = OptionHandlerUtil.keyFor(type);

    final TypeLiteral<OptionHandlerFactory<T>> factoryType =
        new TypeLiteral<OptionHandlerFactory<T>>() {};

    final TypeLiteral<? extends OptionHandler<T>> implType =
        TypeLiteral.get(impl);

    bind(key).toProvider(FactoryProvider.newFactory(factoryType, implType));
  }
}
