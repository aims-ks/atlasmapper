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

window["Atlas"] = window["Atlas"] || {};
window["Atlas"]["Utils"] = window["Atlas"]["Utils"] || {};

/**
 * Apply some basic wiki format:
 * Styles:
 *     Bold: *expression*
 *     Italic: /expression/
 *     Underline: _expression_
 *     Strike: -expression-
 * NOTE: Styles can be nested, but not overlapping
 *     Example:
 *         _*Bold and underlined*_ (Valid)
 *         *_Bold and underlined*_ (Invalid, only the first style, bold, is applied)
 * Headers:
 *     Header (1st level): ^==Heading 1==$
 *     Header (2nd level): ^===Heading 2===$
 *     Header (3rd level): ^====Heading 3====$
 *     Header (4th level): ^=====Heading 4=====$
 *     Header (5th level): ^======Heading 5======$
 *     Header (6th level): ^=======Heading 6=======$
 * Bullet list:
 *     ^* First element$
 *     ^** Sub element$
 *     ^* 2nd element$
 * Numbered list:
 *     ^# First element$ (appear as "1. First element")
 *     ^## Sub element$  (appear as "    1. Sub element")
 *     ^# 2nd element$   (appear as "2. 2nd element")
 * URL:
 *     Format: [[url|label]]. The label is optional, and it may contains wiki format.
 *     Example:
 *         [[http://www.google.com/|Google]]
 *         [[http://google.com/]]
 *         [[page2.html|*Page /2/*]]
 */
Atlas.Utils.WikiFormater = OpenLayers.Class({
	MAX_URL_LENGTH: 40,

	initialize: function() {
	},

	/**
	 * Apply some basic wiki format:
	 * Bold: *expression*
	 * Italic: /expression/
	 * Underline: _expression_
	 * Strike: -expression-
	 * Header (1st level): ^==Heading 1==$
	 * Header (2nd level): ^===Heading 2===$
	 * Header (3rd level): ^====Heading 3====$
	 * Header (4th level): ^=====Heading 4=====$
	 * Header (5th level): ^======Heading 5======$
	 * Header (6th level): ^=======Heading 6=======$
	 * Bullet list:
	 *     ^* First element$
	 *     ^** Sub element$
	 *     ^* 2nd element$
	 * Numbered list:
	 *     ^# First element$ (appear as "1. First element")
	 *     ^## Sub element$  (appear as "    1. Sub element")
	 *     ^# 2nd element$   (appear as "2. 2nd element")
	 * URL: [[http://google.com/|Google]]
	 *
	 * See: https://github.com/lahdekorpi/Wiky.php/blob/master/wiky.inc.php
	 *
	 * @param input String, using some Wiki format syntax.
	 * @return HTML String.
	 */
	// Recursive method that treat the Wiki document as a tree.
	// This method is faster than using regex to parse the document
	// multiple times and easier to maintain than the streaming
	// solution. It is also the only one that handle the tag balancing
	// without having to implement any complex validation.
	// NOTE: If tags are crossing, the first one is implemented;
	//     For example, the string:
	//         *bold /italic close bold* close italic/
	//     Will result in:
	//         <b>bold /italic close bold</b> close italic/
	format: function(input) {
		if (!input || typeof(input) !== 'string') { return input; }

		// Normalize input, to deal only with "\n" for new lines.
		input = input.replace("\r\n", "\n").replace("\r", "\n");

		var output = '', currentChar = '', htmlChunk = '';

		for (var i=0, len=input.length; i<len; i++) {
			currentChar = input.charAt(i); // IE 7 and earlier do not support input[i]
			htmlChunk = '';

			// ********************
			// * New lines (<br>) *
			// ********************
			if (currentChar === '\n') {
				htmlChunk += '<br/>\n';
			}

			// *******************************
			// * Styles (<b>, <i>, <u>, <s>) *
			// *******************************
			if (this._isStyleChar(currentChar)) {
				var openTag = this._getStyleTag(input, i);
				if (openTag && openTag.open) {
					// Look for its close tag
					var closeTag = this._findCloseTag(input, openTag);
					if (closeTag) {
						htmlChunk += openTag.tag + this.format(input.substring(openTag.index+1, closeTag.index)) + closeTag.tag;
						i = closeTag.index;
						currentChar = i < len ? input.charAt(i) : '';
					}
				}
			}

			// *************
			// * Wiki URLs *
			// *************
			if (currentChar === '[' && i+1 < len && input.charAt(i+1) === '[') {
				var index = i, inURL = true, isURL = false;
				// Find the index of the last URL chars
				// NOTE: URLs can not contains URL, so the first closing tag is for the current URL.
				while (inURL && index < len) {
					// '\n' is not allow in Wiki URL
					if (input.charAt(index) === '\n') {
						inURL = false;
					}
					if (input.charAt(index+1) !== ']' && input.charAt(index) === ']' && input.charAt(index-1) === ']') {
						inURL = false;
						isURL = true;
					}
					if (inURL) {
						index++;
					}
				}
				if (isURL) {
					var htmlURL, wikiURL = input.substring(i+2, index-1);
					var urlStr, filename, label, wikiURLParts = wikiURL.split('|');

					var type = 'URL';
					if (wikiURLParts.length >= 2) {
						if (wikiURLParts[0] === 'IMG') {
							// Remove IMG
							type = wikiURLParts.shift();
							urlStr = wikiURLParts.shift();
							label = wikiURLParts.join('|');
						} else if (wikiURLParts[0] === 'DOWNLOAD') {
							// Remove DOWNLOAD
							type = wikiURLParts.shift();
							if (wikiURLParts.length >= 2) {
								urlStr = wikiURLParts.shift();
								filename = wikiURLParts.shift();
								label = this.format(wikiURLParts.join('|'));
								if (!label) {
									label = filename;
								}
							} else {
								urlStr = wikiURLParts[0];
								filename = urlStr;

								var lastSlashIndex = filename.lastIndexOf('/');
								var questionMarkIndex = filename.indexOf('?');
								if (questionMarkIndex < 0) { questionMarkIndex = filename.length; }

								if (lastSlashIndex+1 < questionMarkIndex) {
									filename = filename.substring(lastSlashIndex+1, questionMarkIndex);
								}
								label = filename;
							}
						} else {
							urlStr = wikiURLParts.shift();
							label = this.format(wikiURLParts.join('|'));
						}
					} else {
						label = this._truncateURLForDisplay(wikiURL);
						urlStr = wikiURL;
					}

					if (type === 'IMG') {
						htmlChunk += '<img src="'+urlStr+'" alt="'+label+'" title="'+label+'"/>';
					} else if (type === 'DOWNLOAD') {
						// NOTE: A[DOWNLOAD] is a HTML5 attribute. It's ignored if the browser do not support it.
						//     http://www.w3.org/html/wg/drafts/html/master/links.html#downloading-resources
						htmlChunk += '<a href="'+urlStr+'" download="'+filename+'" target="_blank">'+label+'</a>';
					} else {
						htmlChunk += '<a href="'+urlStr+'" target="_blank">'+label+'</a>';
					}
					i = index;
					currentChar = i < len ? input.charAt(i) : '';
				}
			}

			// ******************************
			// * Complete URLs (http://...) *
			// ******************************
			if (this._isCompleteURL(input, i)) {
				var urlStr = this._toURL(input, i);
				if (urlStr) {
					var label = this._truncateURLForDisplay(urlStr);
					htmlChunk += '<a href="'+urlStr+'" target="_blank">'+label+'</a>';
					i += urlStr.length-1;
					currentChar = i < len ? input.charAt(i) : '';
				}
			}

			// ****************************
			// * Incomplete URLs (www...) *
			// ****************************
			if (this._isIncompleteURL(input, i)) {
				var urlStr = this._toURL(input, i);
				if (urlStr) {
					var label = this._truncateURLForDisplay(urlStr);
					htmlChunk += '<a href="http://'+urlStr+'" target="_blank">'+label+'</a>';
					i += urlStr.length-1;
					currentChar = i < len ? input.charAt(i) : '';
				}
			}

			// ***********************************
			// * Headers (<h1>, <h2>, ..., <h6>) *
			// ***********************************
			if (currentChar === '=' && (i <= 0 || input.charAt(i-1) === '\n')) {
				// Collect open tag ("\n=====")
				var index = i, openTag = '=', closeTag = '',
					lineStartIndex = i, lineEndIndex;
				while (index+1 < len && input.charAt(index+1) === '=') {
					index++;
					openTag += '=';
				}

				// Collect close tag ("=====\n")
				while (index+1 < len && input.charAt(index+1) !== '\n') {
					index++;
					if (input.charAt(index) === '=') {
						closeTag += '=';
					} else {
						// reset
						closeTag = '';
					}
				}
				lineEndIndex = index;

				var headerTagNumber = Math.min(openTag.length, closeTag.length) - 1;
				if (headerTagNumber >= 1 && headerTagNumber <= 6) {
					htmlChunk += '<h'+headerTagNumber+'>' +
							this.format(input.substring(
									lineStartIndex + headerTagNumber + 1,
									lineEndIndex - headerTagNumber)) +
							'</h'+headerTagNumber+'>\n';

					// lineEndIndex   => last '='
					// lineEndIndex+1 => the '\n'
					i = lineEndIndex+1;
					currentChar = i < len ? input.charAt(i) : '';
				}
			}

			// ***************
			// * Bullet list *
			// ***************
			if (this._isListLine(input, i)) {
				// Collect all lines that define the list
				var index = i, inList = true;
				while (inList && index < len) {
					if (index > 0 && input.charAt(index-1) === '\n') {
						// The cursor is at the beginning of a new line.
						// It's time to check if the line is still part
						// of the bullet list.
						inList = this._isListLine(input, index);
					}
					if (inList) {
						index++;
					}
				}
				var listBlock = input.substring(i, index);

				htmlChunk += this._createHTMLList(this._createListObjFromWikiFormat(listBlock));
				i = index-1;
				currentChar = i < len ? input.charAt(i) : '';
			}

			// *****************
			// * Numbered list *
			// *****************
			if (this._isListLine(input, i, true)) {
				// Collect all lines that define the list
				var index = i, inList = true;
				while (inList && index < len) {
					if (index > 0 && input.charAt(index-1) === '\n') {
						// The cursor is at the beginning of a new line.
						// It's time to check if the line is still part
						// of the bullet list.
						inList = this._isListLine(input, index, true);
					}
					if (inList) {
						index++;
					}
				}
				var listBlock = input.substring(i, index);

				htmlChunk += this._createHTMLList(this._createListObjFromWikiFormat(listBlock, true), true);
				i = index-1;
				currentChar = i < len ? input.charAt(i) : '';
			}

			// Default
			if (htmlChunk == '') {
				htmlChunk = currentChar;
			}

			output += htmlChunk;
		}

		return output;
	},


	/**
	 * @param char
	 * @return {Boolean}
	 * @private
	 */
	_isStyleChar: function(char) {
		return char === '*' || char === '/' || char === '_' || char === '-';
	},

	/**
	 * @param input
	 * @param index
	 * @param numbered
	 * @return {Boolean}
	 * @private
	 */
	_isListLine: function(input, index, numbered) {
		var len = input.length, bulletChar = numbered ? '#' : '*';
		if (input.charAt(index) !== bulletChar || (index > 0 && input.charAt(index-1) !== '\n')) {
			return false;
		}
		while (index < len && input.charAt(index) === bulletChar) {
			index++;
		}
		// It is a list only if the next character after the stars is a white space.
		return /\s/.test(input.charAt(index));
	},

	/**
	 * @param input
	 * @param index
	 * @return {
	 *     type: '*',  // '*', '/', '_' or '-'
	 *     index: 123, // Index of the character in the input string
	 *     open: true, // Open or close tag
	 *     tag: '<b>'  // Equivalent HTML tag, either open or close.
	 * }
	 * @private
	 */
	_getStyleTag: function(input, index) {
		var tag = {
			type: input.charAt(index),
			index: index
		};

		// Delimiter: Allow caracter before the style char, to be considered as a style char.
		//     Example: "This is *important* " => "important" is considered as bold because it's surrounded by spaces.
		//         "end of -sentence-." => "sentence" is striked out because has a space before and a period after.
		//         "value1|*value2*" => "value2" is bold because it has a pipe before and a end of string at the end.
		//             The pipe and brakets chars are mostly used to detect style inside element, like in a link label,
		//             to not accidently consider the label style end with the current style end.
		//var styleDelimiterRegex = /[\s\.,\|\[\]]/;
		var styleDelimiterRegex = /[^\w:]/;

		// Check if the sequence start with white space
		var len = input.length, startIndex = index, endIndex = index;
		while (startIndex-1 >= 0 && this._isStyleChar(input.charAt(startIndex-1))) {
			startIndex--;
		}
		while (endIndex+1 < len && this._isStyleChar(input.charAt(endIndex+1))) {
			endIndex++;
		}

		if ((startIndex-1 < 0 || styleDelimiterRegex.test(input.charAt(startIndex-1))) &&
				(endIndex+1 >= len || !styleDelimiterRegex.test(input.charAt(endIndex+1)))) {
			tag.open = true;
		} else if ((startIndex-1 < 0 || !styleDelimiterRegex.test(input.charAt(startIndex-1))) &&
				(endIndex+1 >= len || styleDelimiterRegex.test(input.charAt(endIndex+1)))) {
			tag.open = false;
		} else {
			return null;
		}

		if (tag.type === '*') {
			tag.tag = tag.open ? '<b>' : '</b>';
		} else if (tag.type === '/') {
			tag.tag = tag.open ? '<i>' : '</i>';
		} else if (tag.type === '_') {
			tag.tag = tag.open ? '<u>' : '</u>';
		} else if (tag.type === '-') {
			tag.tag = tag.open ? '<s>' : '</s>';
		} else {
			return null;
		}

		return tag;
	},

	/**
	 * @param input
	 * @param openTag
	 * @return {*}
	 * @private
	 */
	_findCloseTag: function(input, openTag) {
		var tag, closeTag, currentChar = '';
		for (var i=openTag.index+1, len=input.length; i<len; i++) {
			currentChar = input.charAt(i);
			if (this._isStyleChar(currentChar)) {
				tag = this._getStyleTag(input, i);
				if (tag && tag.type === openTag.type) {
					if (tag.open) {
						// Find the close tag for the new open tag
						closeTag = this._findCloseTag(input, tag);
						if (!closeTag) {
							return null; // unbalanced
						}
						// Continue to look from close this tag.
						i = closeTag.index+1;
					} else {
						// This is the close tag we were looking for.
						return tag;
					}
				}
			}
		}
	},

	/**
	 * Create a list object from a Wiki string
	 * Wiki String:
	 * * Item A
	 * * Item B
	 * ** Item B1
	 * ** Item B2
	 * * Item C
	 * ** Item C1
	 * *** Item C11
	 * * Item D
	 *
	 * List:
	 * [
	 *     {
	 *         value: 'Item A',
	 *         children: []
	 *     }, {
	 *         value: 'Item B',
	 *         children: [
	 *             }
	 *                 value: 'Item B1',
	 *                 children: []
	 *             }, {
	 *                 value: 'Item B2',
	 *                 children: []
	 *             }
	 *         ]
	 *     }, {
	 *         value: 'Item C',
	 *         children: [
	 *             }
	 *                 value: 'Item C1',
	 *                 children: [
	 *                     }
	 *                         value: 'Item C11',
	 *                         children: []
	 *                     }
	 *                 ]
	 *             }
	 *         ]
	 *     }, {
	 *         value: 'Item D',
	 *         children: []
	 *     }
	 * ]
	 * @param listStr
	 * @param numbered
	 * @return [*] Array of list item, containing a value and a array of children.
	 * @private
	 */
	_createListObjFromWikiFormat: function(listStr, numbered) {
		var bulletChar = numbered ? '#' : '*';
		var valueRegex = numbered ? /^#+\s+/ : /^\*+\s+/;

		// Split on '\r' (Mac), '\n' (UNIX) or '\r\n' (Windows)
		var bulletListItems = listStr.replace(/[\n\r]+/g, '\n').split('\n');

		var bulletListObj = [];
		for (var i=0, len=bulletListItems.length; i<len; i++) {
			var itemStr = bulletListItems[i];

			var value = itemStr.replace(valueRegex, '');
			var index = 0;
			var listPtr = bulletListObj;
			while (itemStr.charAt(index) === bulletChar) {
				index++;
				if (listPtr.length === 0) {
					listPtr.push({
						children: []
					});
				}
				listPtr = listPtr[listPtr.length-1].children;
			}
			listPtr.push({
				value: value,
				children: []
			});
		}

		// Get ride of the root
		return bulletListObj[0].children;
	},

	/**
	 * Create a HTML list for a list object
	 * @param list
	 * @param numbered
	 * @return String
	 * @private
	 */
	_createHTMLList: function(list, numbered) {
		var listTagName = numbered ? 'ol' : 'ul';

		var htmlList = '<'+listTagName+'>\n';
		for (var i=0, len=list.length; i<len; i++) {
			htmlList += '<li>' + this.format(list[i].value);
			if (list[i].children && list[i].children.length > 0) {
				htmlList += '\n' + this._createHTMLList(list[i].children, numbered);
			}
			htmlList += '</li>\n';
		}
		htmlList += '</'+listTagName+'>\n';

		return htmlList;
	},

	/**
	 * @param url
	 * @return String
	 * @private
	 */
	_truncateURLForDisplay: function(url) {
		var maxUrlLength = this.MAX_URL_LENGTH || 40;

		if (maxUrlLength == 1) {
			return ".";
		}

		if (maxUrlLength == 2) {
			return "..";
		}

		if (maxUrlLength > 0 && maxUrlLength < url.length) {
			var beginningLength = Math.round((maxUrlLength-3) * 3.0/4);
			var endingLength = maxUrlLength - beginningLength - 3; // 3 is for the "..."
			if (beginningLength > 1 && endingLength == 0) {
				beginningLength--;
				endingLength = 1;
			}
			return url.substring(0, beginningLength) + "..." + url.substring(url.length - endingLength);
		}

		return url;
	},

	/**
	 * If it starts with (sftp|ftp|http|https|file)://
	 * @param input
	 * @param i
	 * @return {Boolean}
	 * @private
	 */
	_isCompleteURL: function(input, i) {
		var inputChunk = input.substr(i, 10);
		return /^(sftp|ftp|http|https|file):\/\//.test(inputChunk);
	},
	/**
	 * If it starts with www.(Something)
	 * @param input
	 * @param i
	 * @return {Boolean}
	 * @private
	 */
	_isIncompleteURL: function(input, i) {
		var inputChunk = input.substr(i, 5);
		return /^www\.\S/.test(inputChunk);
	},

	/**
	 * @param input
	 * @param i
	 * @return {String}
	 * @private
	 */
	_toURL: function(input, i) {
		var urlCharRegex = /[a-zA-Z0-9\$\-_\.\+\!\*'\(\),;\/\?:@=&#%]/,
			urlEndingCharRegex = /[a-zA-Z0-9\/]/,
			len = input.length, index = i;

		while (index < len && urlCharRegex.test(input.charAt(index))) {
			index++;
		}
		index--;
		while (index >= 0 && !urlEndingCharRegex.test(input.charAt(index))) {
			index--;
		}

		return input.substring(i, index+1);
	}
});
