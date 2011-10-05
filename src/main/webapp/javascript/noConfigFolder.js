/**
 * This page is displayer when the application is not configured properly
 * It show an error message with solutions to fix the problem.
 */
Ext.onReady(function() {
	Ext.QuickTips.init();

	var unixDemoDataDir = '/path/to/config/folder/atlasmapper';
	var windowsDemoDataDir = 'C:\\path\\to\\config\\folder\\atlasmapper';

	var actualValue = '';
	if (dataDirIsDefined) {
		actualValue = '<p>The current value of the variable is:<br>\n\
				<pre class="code">'+dataDirPropertyValue+'</pre>\n\
				This folder is: ' + (dataDirIsWritable ?
					'<span class="success">Writable</span>' :
					'<span class="error">Not writable OR can not be created</span>') +
				'</p>';
	} else {
		actualValue = '<p>The variable is <span class="error">not defined</span>.';
	}

	// This just creates a window to wrap the login form.
	// The login object is passed to the items collection.
	Ext.create('Ext.container.Viewport', {
		layout: 'fit',
		title: 'AtlasMapper server',
		closable: false,
		border: false,
		defaults: {
			width: '100%', // Accordion has height problem when no width is specified...
			xtype: 'panel'
		},
		items: {
			layout: 'border',
			padding: 20,
			items: [
				{
					region: 'north',
					split: true,
					bodyPadding: 10,
					html:'<div class="messageWindow">\n\
							<h1>Welcome to the AtlasMapper</h1>\n\
							<p>You have <b>successfully deployed the <i>AtlasMapper</i> War</b>. You are now one step away \
								before starting to use the application. You now have to set the location \
								of the configuration folder. This folder is used by the <i>AtlasMapper server</i> \
								(this application) to store its configuration. The application can not work \
								properly without this folder.\n\
							</p>\n\
							<p>The folder must give write access to the user used to \
								start up the web server. When using <i>Tomcat</i> as a web server, that user \
								is usually called <i>tomcat</i>. If the folder \
								do not exists, the application will try to create it.\n\
							</p>\n\
							<p>The path to the configuration folder has to be set with the variable \
								<b>'+dataDirProperty+'</b>.\n\
								'+actualValue+'\n\
								There is three different ways to set this variable. Choose the one that \
								suit the best your server installation:\n\
								<b>Note:</b> The path used in the documentation bellow is <i>'+unixDemoDataDir+'</i> for Linux \
								and <i>'+windowsDemoDataDir+'</i> for windows. It is recommanded to change this path to the \
								folder you want.\n\
							</p>\n\
						</div>'
				}, {
					region: 'center',
					border: false,
					xtype: 'tabpanel',
					defaults: {
						border: false,
						xtype: 'tabpanel',
						defaults: {
							bodyPadding: 10,
							autoScroll: true,
							xtype: 'panel'
						}
					},
					items: [
						{
							title: 'UNIX',
							items: [
								{
									title: 'Servlet context parameter',
									html:'<div class="messageWindow">\n\
											<p>\n\
												<b>Scope:</b> Visible to the <b><i>AtlasMapper server</i> only</b><br/>\n\
												<b>Apply to:</b> Deployed Web App<br/>\n\
												<b>Java details:</b> <i>ServletContext.getInitParameter(String)</i> is used to retrieved the variable<br/>\n\
											</p>\n\
											<p>\n\
												Add the variable to the Web Archive (undeployed War file).<br/>\n\
												<ol>\n\
													<!-- 1. -->\n\
													<li>Edit the file <i>web.xml</i>\n\
														<pre class="code">/path/to/webapps/atlasmapper/WEB-INF/web.xml</pre>\n\
													</li>\n\
													<!-- 2. -->\n\
													<li>Add the <i>'+dataDirProperty+'</i> variable to the <i>&lt;web-app&gt;</i> tag.\n\
														<pre class="code">&lt;web-app&gt;\n	...\n	&lt;context-param&gt;\n		&lt;param-name&gt;'+dataDirProperty+'&lt;/param-name&gt;\n		&lt;param-value&gt;'+unixDemoDataDir+'&lt;/param-value&gt;\n	&lt;/context-param&gt;\n	...\n&lt;/web-app&gt;</pre>\n\
													</li>\n\
													<!-- 3. -->\n\
													<li>Start or Restart the web server</li>\n\
												</ol>\n\
											</p>\n\
										</div>'
								}, {
									title: 'Java system property',
									html:'<div class="messageWindow">\n\
											<p>\n\
												<b>Scope:</b> Visible to <b>all webapps</b>, not just the <i>AtlasMapper server</i><br/>\n\
												<b>Apply to:</b> Web server configuration<br/>\n\
												<b>Java details:</b> <i>System.getProperty(String)</i> is used to retrieved the variable<br/>\n\
											</p>\n\
											<p>\n\
												<ul>\n\
													<li>\n\
														<b>Tomcat</b>\n\
														<ol>\n\
															<!-- 1. -->\n\
															<li>Create or edit the file <i>setenv.sh</i>\n\
																<pre class="code">/path/to/tomcat/bin/setenv.sh</pre>\n\
															</li>\n\
															<!-- 2. -->\n\
															<li>Add the <i>'+dataDirProperty+'</i> variable to <i>CATALINA_OPTS</i>\n\
																<pre class="code">#!/bin/sh\nCATALINA_OPTS="-D'+dataDirProperty+'='+unixDemoDataDir+'"\nexport CATALINA_OPTS</pre>\n\
															</li>\n\
															<!-- 3. -->\n\
															<li>Start or Restart <i>tomcat</i></li>\n\
														</ol>\n\
													</li>\n\
												</ul>\n\
												<ul>\n\
													<li>\n\
														<b>Glassfish</b>\n\
														<ol>\n\
															<!-- 1. -->\n\
															<li>Edit the file <i>domain.xml</i>\n\
																<pre class="code">domains/<<domain>>/config/domain.xml</pre>\n\
															</li>\n\
															<!-- 2. -->\n\
															<li>Add the <i>'+dataDirProperty+'</i> variable to <i>&lt;java-config&gt;</i>\n\
																<pre class="code">...\n&lt;java-config&gt;\n	...\n	&lt;jvm-options&gt;-D'+dataDirProperty+'='+unixDemoDataDir+'&lt;/jvm-options&gt;\n&lt;/java-config&gt;\n...</pre>\n\
															</li>\n\
															<!-- 3. -->\n\
															<li>Start or Restart <i>Glassfish</i></li>\n\
														</ol>\n\
													</li>\n\
												</ul>\n\
											</p>\n\
										</div>'
								}, {
									title: 'Global environment variable',
									html:'<div class="messageWindow">\n\
											<p>\n\
												<b>Scope:</b> Visible to <b>all applications run by the user</b>, not just the <i>AtlasMapper server</i><br/>\n\
												<b>Apply to:</b> User environment variables<br/>\n\
												<b>Java details:</b> <i>System.getenv(String)</i> is used to retrieved the variable<br/>\n\
											</p>\n\
											<p>\n\
												Add the variable to the operating system environment variables.<br/>\n\
												To temporarily set the variable:<br/>\n\
												<ol>\n\
													<!-- 1. -->\n\
													<li>Set the environment variable before starting the web server:\n\
														<pre class="code">$ export '+dataDirProperty+'='+unixDemoDataDir+'</pre>\n\
													</li>\n\
													<!-- 2. -->\n\
													<li>Start the web server from the same environment</li>\n\
												</ol>\n\
												To permanently set the variable:<br/>\n\
												<ol>\n\
													<!-- 1. -->\n\
													<li>Edit a user bash script such as <i>.bash_profile</i> or <i>.bashrc</i>:\n\
														<pre class="code">~/.bashrc</pre>\n\
													</li>\n\
													<!-- 2. -->\n\
													<li>Add the environment variable at the beggining of the file\n\
														<pre class="code">export '+dataDirProperty+'='+unixDemoDataDir+'</pre>\n\
													</li>\n\
													<!-- 3. -->\n\
													<li>Start or Restart the web server</li>\n\
												</ol>\n\
											</p>\n\
										</div>'
								}
							]
						}, {
							title: 'Windows',
							items: [
								{
									title: 'Servlet context parameter',
									html:'<div class="messageWindow">\n\
											<p>\n\
												<b>Scope:</b> Visible to the <b><i>AtlasMapper server</i> only</b><br/>\n\
												<b>Apply to:</b> Deployed Web App<br/>\n\
												<b>Java details:</b> <i>ServletContext.getInitParameter(String)</i> is used to retrieved the variable<br/>\n\
											</p>\n\
											<p>\n\
												Add the variable to the Web Archive (undeployed War file).<br/>\n\
												<ol>\n\
													<!-- 1. -->\n\
													<li>Edit the file <i>web.xml</i>\n\
														<pre class="code">C:\\path\\to\\webapps\\atlasmapper\\WEB-INF\\web.xml</pre>\n\
													</li>\n\
													<!-- 2. -->\n\
													<li>Add the <i>'+dataDirProperty+'</i> variable to the <i>&lt;web-app&gt;</i> tag.\n\
														<pre class="code">&lt;web-app&gt;\n	...\n	&lt;context-param&gt;\n		&lt;param-name&gt;'+dataDirProperty+'&lt;/param-name&gt;\n		&lt;param-value&gt;'+windowsDemoDataDir+'&lt;/param-value&gt;\n	&lt;/context-param&gt;\n	...\n&lt;/web-app&gt;</pre>\n\
													</li>\n\
													<!-- 3. -->\n\
													<li>Start or Restart the web server</li>\n\
												</ol>\n\
											</p>\n\
										</div>'
								}, {
									title: 'Java system property',
									html:'<div class="messageWindow">\n\
											<p>\n\
												<b>Scope:</b> Visible to <b>all webapps</b>, not just the <i>AtlasMapper server</i><br/>\n\
												<b>Apply to:</b> Web server configuration<br/>\n\
												<b>Java details:</b> <i>System.getProperty(String)</i> is used to retrieved the variable<br/>\n\
											</p>\n\
											<p>\n\
												<ul>\n\
													<li>\n\
														<b>Tomcat</b>\n\
														<ol>\n\
															<!-- 1. -->\n\
															<li>Create or edit the file <i>setenv.bat</i>\n\
																<pre class="code">C:\\path\\to\\tomcat\\bin\\setenv.bat</pre>\n\
															</li>\n\
															<!-- 2. -->\n\
															<li>Add the <i>'+dataDirProperty+'</i> variable to <i>CATALINA_OPTS</i>\n\
																<pre class="code">@echo off\nset CATALINA_OPTS="-D'+dataDirProperty+'='+windowsDemoDataDir+'"</pre>\n\
															</li>\n\
															<!-- 3. -->\n\
															<li>Start or Restart <i>tomcat</i></li>\n\
														</ol>\n\
													</li>\n\
												</ul>\n\
												<ul>\n\
													<li>\n\
														<b>Glassfish</b>\n\
														<ol>\n\
															<!-- 1. -->\n\
															<li>Edit the file <i>domain.xml</i>\n\
																<pre class="code">domains\\<<domain>>\\config\\domain.xml</pre>\n\
															</li>\n\
															<!-- 2. -->\n\
															<li>Add the <i>'+dataDirProperty+'</i> variable to <i>&lt;java-config&gt;</i>\n\
																<pre class="code">...\n&lt;java-config&gt;\n	...\n	&lt;jvm-options&gt;-D'+dataDirProperty+'='+windowsDemoDataDir+'&lt;/jvm-options&gt;\n&lt;/java-config&gt;\n...</pre>\n\
															</li>\n\
															<!-- 3. -->\n\
															<li>Start or Restart <i>Glassfish</i></li>\n\
														</ol>\n\
													</li>\n\
												</ul>\n\
											</p>\n\
										</div>'
								}, {
									title: 'Global environment variable',
									html:'<div class="messageWindow">\n\
											<p>\n\
												<b>Scope:</b> Visible to <b>all applications on the computer</b>, not just the <i>AtlasMapper server</i><br/>\n\
												<b>Apply to:</b> Operating system environment variables<br/>\n\
												<b>Java details:</b> <i>System.getenv(String)</i> is used to retrieved the variable<br/>\n\
											</p>\n\
											<p>\n\
												Add the variable to the operating system environment variables.<br/>\n\
												<ol>\n\
													<!-- 1 -->\n\
													<li>Open the <i>Environmental Variables</i> window\n\
														<ul>\n\
															<li>Windows 7:<br/>\n\
																<ol>\n\
																	<li>Select <i>Start</i>and right click <i>Computer</i></li>\n\
																	<li>Click <i>Advance System</i> setting on <i>System properties</i> window</li>\n\
																	<li>Select <i>Advance</i> tab</li>\n\
																	<li>Click <i>Environmental Variables</i> button</li>\n\
																</ol>\n\
															</li>\n\
															<li>Windows XP:<br/>\n\
																<ol>\n\
																	<li>Select Start, select Control panel</li>\n\
																	<li>In the opened Control panel, select Performance and Maintenance (it is at the bottom)</li>\n\
																	<li>In the Performance and Maintenance window, select System</li>\n\
																	<li>Select <i>Advance</i> tab</li>\n\
																	<li>On your computer desktop, right click the <i>My Computer</i> icon and select <i>Properties</i>.</li>\n\
																	<li>On the <i>System Properties</i> window select <i>Advance</i> tab.</li>\n\
																	<li>Click <i>Environmental Variables</i> button</li>\n\
																</ol>\n\
															</li>\n\
															<li>Windows 2000:<br/>\n\
																<ol>\n\
																	<li>On your computer desktop, right click the <i>My Computer</i> icon and select <i>Properties</i>.</li>\n\
																	<li>On the <i>System Properties</i> window select <i>Advance</i> tab.</li>\n\
																	<li>Click <i>Environmental Variables</i> button</li>\n\
																</ol>\n\
															</li>\n\
														</ul>\n\
													</li>\n\
													<!-- 2 -->\n\
													<li>Click <i>New</i> button in <i>System variables</i> frame at the bottom of the <i>Environment Variables</i> Window.</li>\n\
													<!-- 3 -->\n\
													<li>\n\
														Enter the variable:\n\
														<pre class="code">Variable Name: '+dataDirProperty+'\nVariable Value: '+windowsDemoDataDir+'</pre>\n\
													</li>\n\
													<!-- 4 -->\n\
													<li>Close the windows\n\
														<ul>\n\
															<li>Click <i>OK</i> button on <i>Edit System Variable</i> window.</li>\n\
															<li>Click <i>OK</i> button on <i>Environment Variables</i> window.</li>\n\
															<li>Click <i>OK</i> button on <i>Properties</i> window.</li>\n\
														</ul>\n\
													</li>\n\
													<!-- 5 -->\n\
													<li>Start or Restart the web server</li>\n\
												</ol>\n\
											</p>\n\
										</div>'
								}
							]
						}
					]
				}
			]
		}
	});
});
