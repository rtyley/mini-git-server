mini-git-server
===============

Pure-Java WAR capable of hosting git repos and exposing them with git+ssh.
Basically a copy of Gerrit (http://code.google.com/p/gerrit/) - with all the review-related
functionality stripped away and the dependency on a database removed (making it entirely filesystem based).

I use it for integration-testing my git client.

Just as with Gerrit, the location of the server site is passed to the war using the 'gerrit.site_path' system
property (will probably rename that at some point), and the format of the directory structure and the
[gerrit.site_path]/etc/gerrit.config file is just the same. My gerrit.config file is just:

	[gerrit]
		basePath = "repos"

...and which means the server will expect to find all the repos in [gerrit.site_path]/repos

