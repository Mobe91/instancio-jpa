Doing a release
==========

* Make sure you have GPG installed and the executable is available on PATH
* Make sure your Maven settings.xml has credentials for the server `sonatype-nexus-staging` configured
* Make sure your Maven settings.xml has a profile called `mobecker-release` with the property `gpg.passphrase`
* Edit the `README.md` and update the property `version.instancio-jpa` to the latest released version
* Prepare a local Maven release via `./mvnw -P "mobecker-release,release" release:clean release:prepare` (make sure you use JDK 11 because the checkstyle version we use is not compatible with JDK 8)
* Actually deploy the release with `./mvnw -P "mobecker-release,release" release:perform`
* Goto https://s01.oss.sonatype.org/ and login. In *Build Promotion* click on *Staging Repositories* then scroll down and find the repository named *commobecker-*
* Click on the repository, then click *Close* and *Confirm*. Wait until the operation has completed.
* Commit the changes and push the branch `git push origin`, as well as the created tag `git push origin TAG`
* Go back to Sonatype, click *Refresh* and finally click *Release* and *Confirm*
* Create Tweet about new version
