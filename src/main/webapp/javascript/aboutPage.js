
Ext.onReady(function() {
	var frameset = new Frameset();
	frameset.setContentTitle('About');
	frameset.addContentDescription('About the Atlas Mapper');
	frameset.render(document.body);

	var htmlContent = Ext.create('Ext.panel.Panel', {
		bodyStyle: 'padding:5px 5px 0',
		border: false,

		html: '<table style="width: 100%">\n' +
				'	<tr>\n' +
				'		<th style="width: 16em">Name</th>\n' +
				'		<td>'+name+'</td>\n' +
				'	</tr>\n' +
				'	<tr>\n' +
				'		<th>Description</th>\n' +
				'		<td><pre style="white-space: pre-wrap;">'+description+'</pre></td>\n' +
				'	</tr>\n' +
				'	<tr>\n' +
				'		<th>Version</th>\n' +
				'		<td>'+version+'</td>\n' +
				'	</tr>\n' +
				'	<tr>\n' +
				'		<th>Project URL</th>\n' +
				'		<td><a href="'+url+'" target="_blank">'+url+'</td>\n' +
				'	</tr>\n' +
				'	<tr>\n' +
				'		<th>'+dataDirProperty+'</th>\n' +
				'		<td>'+dataDirPropertyValue+'</td>\n' +
				'	</tr>\n' +
				'	<tr>\n' +
				'		<th>License</th>\n' +
				'		<td><a href="http://www.gnu.org/copyleft/gpl.html" target="_blank">GPLv3</a></td>\n' +
				'	</tr>\n' +
				'	<tr>\n' +
				'		<th>Dependencies</th>\n' +
				'		<td>\n' +
				'			<p>\n' +
				'				AtlasMapper Server:\n' +
				'			</p>\n' +
				'			<table>\n' +
				'				<tr>\n' +
				'					<th style="width: 10em"><a href="http://geotools.org/" target="_blank">GeoTools</a></th>\n' +
				'					<td style="width: 8em">Java</td>\n' +
				'					<td style="width: 18em"><a href="http://www.gnu.org/licenses/lgpl-2.1.html" target="_blank">LGPL</a></td>\n' +
				'				</tr>\n' +
				'				<tr>\n' +
				'					<th><a href="http://freemarker.sourceforge.net/" target="_blank">FreeMarker</a></th>\n' +
				'					<td>Java</td>\n' +
				'					<td><a href="http://freemarker.sourceforge.net/docs/app_license.html" target="_blank">BSD-style</a></td>\n' +
				'				</tr>\n' +
				'				<tr>\n' +
				'					<th><a href="http://json.org/java/" target="_blank">JSON</a></th>\n' +
				'					<td>Java</td>\n' +
				'					<td><a href="http://www.json.org/license.html" target="_blank">JSON.org</a></td>\n' +
				'				</tr>\n' +
				'				<tr>\n' +
				'					<th><a href="http://www.sencha.com/products/extjs/" target="_blank">ExtJS 4.0.2</a></th>\n' +
				'					<td>Javascript</td>\n' +
				'					<td><a href="http://www.gnu.org/copyleft/gpl.html" target="_blank">GPLv3</a></td>\n' +
				'				</tr>\n' +
				'				<tr>\n' +
				'					<th><a href="http://www.cdolivet.com/editarea/" target="_blank">EditArea</a></th>\n' +
				'					<td>Javascript</td>\n' +
				'					<td>Released under <a href="http://www.cdolivet.com/editarea/editarea/docs/license.html" target="_blank">multiple licenses</a>:\n' +
				'						<ul>\n' +
				'							<li><a href="http://www.gnu.org/copyleft/lesser.html" target="_blank">LGPL</a></li>\n' +
				'							<li><a href="http://www.apache.org/licenses/LICENSE-2.0" target="_blank">Apache</a></li>\n' +
				'							<li><a href="http://www.opensource.org/licenses/bsd-license.php" target="_blank">BSD</a></li>\n' +
				'						</ul>\n' +
				'					</td>\n' +
				'				</tr>\n' +
				'			</table>\n' +
				'			<p>\n' +
				'				AtlasMapper Clients:\n' +
				'			</p>\n' +
				'			<table>\n' +
				'				<tr>\n' +
				'					<th style="width: 10em"><a href="http://openlayers.org/" target="_blank">OpenLayers 2.11</a></th>\n' +
				'					<td style="width: 8em">Javascript</td>\n' +
				'					<td style="width: 18em"><a href="http://svn.openlayers.org/trunk/openlayers/license.txt" target="_blank">FreeBSD</a></td>\n' +
				'				</tr>\n' +
				'				<tr>\n' +
				'					<th><a href="http://geoext.org/" target="_blank">GeoExt 1.0</a></th>\n' +
				'					<td>Javascript</td>\n' +
				'					<td><a href="http://trac.geoext.org/wiki/license" target="_blank">BSD</a></td>\n' +
				'				</tr>\n' +
				'				<tr>\n' +
				'					<th><a href="http://www.sencha.com/products/extjs/" target="_blank">ExtJS 3.3.0</a></th>\n' +
				'					<td>Javascript</td>\n' +
				'					<td><a href="http://www.gnu.org/copyleft/gpl.html" target="_blank">GPLv3</a></td>\n' +
				'				</tr>\n' +
				'			</table>\n' +
				'		</td>\n' +
				'	</tr>\n' +
				'</table>'
	});

	frameset.addContent(htmlContent);
});
