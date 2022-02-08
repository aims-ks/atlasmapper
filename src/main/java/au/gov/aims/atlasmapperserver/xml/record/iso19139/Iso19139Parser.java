/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2012 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.gov.au>
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
package au.gov.aims.atlasmapperserver.xml.record.iso19139;

import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import au.gov.aims.atlasmapperserver.xml.record.AbstractMetadataParser;
import au.gov.aims.atlasmapperserver.xml.record.MetadataDocument;

import javax.xml.parsers.SAXParser;
import java.io.File;
import java.util.logging.Logger;

// Basic SAX example, to have something to starts with.
// http://www.mkyong.com/java/how-to-read-xml-file-in-java-sax-parser/
public class Iso19139Parser extends AbstractMetadataParser {
    private static final Logger LOGGER = Logger.getLogger(Iso19139Parser.class.getName());

    public Iso19139Parser(ThreadLogger logger) {
        super(logger);
    }

    @Override
    public MetadataDocument parseFile(File file, String location)
    throws Exception {

        if (file == null || !file.exists()) {
            return null;
        }

        SAXParser saxParser = getSAXParser();

        Iso19139Document doc = new Iso19139Document(location);
        Iso19139Handler handler = new Iso19139Handler(doc);

        saxParser.parse(file, handler);
        doc.afterParse();

        return doc.isEmpty() ? null : doc;
    }
}
