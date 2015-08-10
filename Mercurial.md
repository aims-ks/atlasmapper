**IMPORTANT NOTE: The images used in this page are temporary and they may be copyrighted. I will change them as soon as I find how to upload images to the project.**

## A guide to use Mercurial ##

### Installing ###
On Ubuntu and other Debian distribution, you may want to use the Mercurial repository to be sure to have the latest version of mercurial:
```
$ sudo add-apt-repository ppa:mercurial-ppa/releases
$ sudo apt-get update
$ sudo apt-get install mercurial
```

### Check-out ###
Check-out the project:
```
$ hg clone https://code.google.com/p/atlasmapper/
$ cd atlasmapper
```

### Commit ###

Add/remove files to the version control system (needed after adding or removing files)
```
$ hg addremove
```

Commit changes to the local server (can be done with NetBeans)
```
$ hg commit -m "Commit message"
```

### Check-out the Wiki ###

Checkout a local copy of the wiki
```
$ hg clone --insecure https://lafond.gael@wiki.atlasmapper.googlecode.com/hg/
```
NOTE: "insecure" (do not verify server certificate) is needed because the domain name is not the same as the one in the certificate (the project name is not in the certificate).

### Commit the Wiki ###

Add/remove files to the version control system (needed after adding or removing files)
```
$ hg addremove
```

Commit the changes to the local copy of the Wiki
```
$ hg commit -m "Commit message"
```

### Push the Wiki ###

Push the local copy of the Wiki to the live server
```
$ hg push --insecure https://lafond.gael@wiki.atlasmapper.googlecode.com/hg/
```

### Sharing modifications ###
A bundle is a file containing all committed modifications made on a project. They are useful to share changes without having to push the changes to the remote server.
To create a bundle file:
```
// Change directory to the project directory (where the .hg folder is)
$ cd projects/atlasmapper
// Commit, to be sure your bundle file will include your latest changes
$ hg commit -m 'Last commit'
// Create the bundle
$ hg bundle ../bundle.hg
```

### Pull ###
Get changes from the live server
```
$ hg pull https://code.google.com/p/atlasmapper/
```
Or use the default from your mercurial configuration (projectfolder/.hg/hgrc)
```
$ hg pull
```

### Push ###
Commit changes to the live server
```
$ hg push https://code.google.com/p/atlasmapper/
```
user: _Your google ID, the one used to log into google (without the @gmail.com)_<br>
password: <i>Can be found at the URL <a href='https://code.google.com/hosting/settings'>https://code.google.com/hosting/settings</a></i>

Or use the default from your mercurial configuration (projectfolder/.hg/hgrc)<br>
<pre><code>$ hg push<br>
</code></pre>
password: <i>Can be found at the URL <a href='https://code.google.com/hosting/settings'>https://code.google.com/hosting/settings</a></i>

<h3>Ignore</h3>
Ignore files (avoid committing some files)<br>
First: list the status of the repository to see which files are new<br>
<pre><code>$ hg status<br>
</code></pre>
NOTE: The new files are tags with a "?". If the file is already tracked, you will have to "forget" it first.<br>
<br>
Add the file <i>foo</i> to the .hgignore (executed at the root of the project)<br>
<pre><code>$ echo 'foo' &gt;&gt; .hgignore<br>
</code></pre>
The file .hgignore already contains the needed header<br>
<br>
To add more complex patterns, you can edit the .hgignore file and add a regular expression in the regexp section.<br>
<br>
You can test if the file really get ignored by requesting the status of the repository again<br>
<pre><code>$ hg status<br>
</code></pre>

Commit your modified .hgignore file if you also want others to ignore the same file.<br>
<pre><code>$ hg commit -m 'Add ignored files'<br>
</code></pre>

For more info about .hgignore syntax, see: <a href='http://www.selenic.com/mercurial/hgignore.5.html'>http://www.selenic.com/mercurial/hgignore.5.html</a>

<h4>Intellij users</h4>
Intellij do not parse the .hgignore file. The file has to be ignored using the IDE.<br>
<br>
A bug report as been submitted to JetBrains concerning this issue: <a href='http://youtrack.jetbrains.net/issue/IDEA-65229?query=by'>http://youtrack.jetbrains.net/issue/IDEA-65229?query=by</a>

<h3>Tag</h3>
To push the source of a new AtlasMapper version, it is important to tag it in prior to push the new source code.<br>
<br>
Tag a version (For example: 1.0-rc2)<br>
<pre><code>$ hg tag 1.0-rc2<br>
$ hg push<br>
</code></pre>

<h2>Branch</h2>
We are using the "Named Branches" method.<br>
<img src='http://stevelosh.com/media/images/blog/2009/08/branch-named.png' /><br>
References:<br>
<a href='http://stevelosh.com/blog/2009/08/a-guide-to-branching-in-mercurial/'>http://stevelosh.com/blog/2009/08/a-guide-to-branching-in-mercurial/</a><br>
<a href='http://hgbook.red-bean.com/read/managing-releases-and-branchy-development.html'>http://hgbook.red-bean.com/read/managing-releases-and-branchy-development.html</a>

Switch the working directory to a previous tag / changelog (to create a new branch from a old state)<br>
<pre><code>$ hg update -c revision<br>
</code></pre>
NOTE: The revision can be a revision number, a revision hash, a tag, a bookmark or a branch name. "-c" check if no uncommitted changes are present<br>
<br>
Branch the working directory<br>
<pre><code>$ hg branch [branchname]<br>
</code></pre>

Creating the new branch on the remote server<br>
<pre><code>$ hg commit<br>
$ hg push --new-branch<br>
</code></pre>
password: <i>Can be found at the URL <a href='https://code.google.com/hosting/settings'>https://code.google.com/hosting/settings</a></i>

NOTE: If for some reason the branch refuse to get created on the server after executing "hg push --new-branch" (if it don't ask for the password, it's not a good sign), try to commit and run the command "hg push --new-branch" again. All changes make in the new branch prior to the creation of the new branch on the server may have to be rollbacked!<br>
<br>
You are not working in the new branch. Make changes and commit normally.<br>
<br>
<h3>Rename / close a branch</h3>
Renaming "v0.1" to "0.1.x":<br>
<pre><code>$ hg update -c v0.1<br>
$ hg branch 0.1.x<br>
$ hg commit -m 'Branch renamed from v0.1 to 0.1.x'<br>
$ hg push --new-branch<br>
</code></pre>

The old branch "v0.1" has become inactive. It has to be closed:<br>
<pre><code>$ hg update -c v0.1<br>
$ hg commit --close-branch -m 'Branch renamed to 0.1.x'<br>
$ hg push<br>
$ hg update -c 0.1.x<br>
</code></pre>

<h3>Switch back to the trunk</h3>
<pre><code>$ hg update -c default<br>
</code></pre>

<h2>Merge</h2>
Linux and Mac users: Merging works great with "meld", a graphical diff software:<br>
<a href='http://meld.sourceforge.net/'>http://meld.sourceforge.net/</a><br>
<img src='http://meld.sourceforge.net/meld_preview.png' /><br>
The actual file is the one in the centre. Use arrow from right or left to choose the appropriate change. When the merge is done, activate the file in the centre by clicking on it, save it and quit Meld.<br>
<br>
How to merge changes from <i>branchname</i> to the trunk<br>
<br>
1. Switch the working directory to the trunk (if it's not already the case)<br>
<pre><code>$ hg update -c default<br>
</code></pre>
2. Merge the trunk with the branch, to create a new changelog in the trunk with 2 parents: <i>branchname</i> and trunk<br>
<pre><code>$ hg merge [branchname]<br>
$ hg commit<br>
$ hg push<br>
</code></pre>

Any branch can be merge with any other branch. Note that mercurial merges are NOT symmetrical.<br>
Trunk merge with <i>branchname</i> =/= <i>branchname</i> merge with Trunk