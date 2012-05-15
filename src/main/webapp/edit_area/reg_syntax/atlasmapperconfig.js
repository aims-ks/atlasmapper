/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.org.au>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

editAreaLoader.load_syntax["atlasmapperconfig"] = {
	'DISPLAY_NAME': 'AtlasMapper config',
	'COMMENT_SINGLE': {1: '//'},
	'COMMENT_MULTI': {'/*': '*/'},
	'QUOTEMARKS': {1:"'", 2:'"'},
	'KEYWORD_CASE_SENSITIVE': true,
	'KEYWORDS': {
 		'layers': [
			'kmlUrl', 'title', 'path', 'description', 'olParams', 'olOptions',
			'dataSourceId', 'layerBoundingBox', 'cached',
			'wmsQueryable', 'wmsVersion', 'isBaseLayer', 'hasLegend',
			'legendUrl', 'legendGroup', 'legendTitle', 'infoHtmlUrls',
			'aliasIds', 'serverUrls', 'wmsRequestMimeType',
			'wmsFeatureRequestLayers', 'wmsTransectable', 'default',
			'styles', 'options', 'name',
			'type', 'mandatory', 'defaultValue', 'selected'
		],
		// Layers can override any data source entry
		'dataSources': [
			'featureRequestsUrl', 'serverUrls', 'webCacheUrl', 'legendUrl',
			'dataSourceName', 'webCacheSupportedParameters', 'dataSourceType',
			'legendParameters', 'wmsVersion'
		],
		'modules': [
			'Tree', 'Info', 'type', 'tabs', 'defaultContent', 'startingTab'
		],
		'keywords': [
			'options', 'description', 'true', 'false'
		],
		'main': [
			'modules', 'clientId', 'clientName', 'defaultLayers', 'projection',
			'layerInfoServiceUrl', 'proxyUrl', 'version', 'startingLocation',
			'dataSources'
		]
	},
	'OPERATORS': [],
	'DELIMITERS': [
		'[', ']', '{', '}'
	],
	// NOTE: Avoid Italic and/or Bold, it cause ghosting problem with many browsers
	'STYLES': {
		'COMMENTS': 'color: #AAAAAA;',
		'KEYWORDS': {
			'main': 'color: #FF0000;', // Red - Main config keyword should not be used
			'layers': 'color: #0000FF;', // Blue
			'dataSources': 'color: #3333FF;', // Blue (similar to layers)
			'modules': 'color: #FFB442;', // Orange
			'keywords' : 'color: #147F00;' // Green
		},
		'OPERATORS': 'color: #FF00FF;',
		'DELIMITERS': 'color: #0038E1;'
	}
	/* Auto completion is not implemented now.
	,
	'AUTO_COMPLETION':  {
		"default": {	// the name of this definition group. It's posisble to have different rules inside the same definition file
			"REGEXP": { "before_word": "[^a-zA-Z0-9_]|^",	// \\s|\\.|
				"possible_words_letters": "[a-zA-Z0-9_]+",
				"letter_after_word_must_match": "[^a-zA-Z0-9_]|$",
				"prefix_separator": "\\."
			},
			"CASE_SENSITIVE": true,
			"MAX_TEXT_LENGTH": 100,		// the maximum length of the text being analyzed before the cursor position
			"KEYWORDS": {
				'': [	// the prefix of thoses items
					// / **
					//  * 0 : the keyword the user is typing
					//  * 1 : (optionnal) the string inserted in code ("{@}" being the new position of the cursor, "§" beeing the equivalent to the value the typed string indicated if the previous )
					//  * 		If empty the keyword will be displayed
					//  * 2 : (optionnal) the text that appear in the suggestion box (if empty, the string to insert will be displayed)
					//  * /
					['Array', '§()', ''],
					['alert', '§({@})', 'alert(String message)'],
					['document'],
					['window']
				],
				'window' : [
					['location'],
					['document'],
					['scrollTo', 'scrollTo({@})', 'scrollTo(Int x,Int y)']
				],
				'location' : [
					 ['href']
				]
			}
		}
	}
	*/
};
