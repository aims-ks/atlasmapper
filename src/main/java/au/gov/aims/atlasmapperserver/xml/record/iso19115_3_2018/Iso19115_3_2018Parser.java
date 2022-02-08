/*
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2022 Australian Institute of Marine Science
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
package au.gov.aims.atlasmapperserver.xml.record.iso19115_3_2018;

import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import au.gov.aims.atlasmapperserver.xml.record.AbstractMetadataParser;
import au.gov.aims.atlasmapperserver.xml.record.MetadataDocument;

import javax.xml.parsers.SAXParser;
import java.io.File;

public class Iso19115_3_2018Parser extends AbstractMetadataParser {
    public Iso19115_3_2018Parser(ThreadLogger logger) {
        super(logger);
    }

    @Override
    public MetadataDocument parseFile(File file, String location)
    throws Exception {

        if (file == null || !file.exists()) {
            return null;
        }

        SAXParser saxParser = getSAXParser();

        Iso19115_3_2018Document doc = new Iso19115_3_2018Document(location);
        Iso19115_3_2018Handler handler = new Iso19115_3_2018Handler(doc);

        saxParser.parse(file, handler);
        doc.afterParse();

        return doc.isEmpty() ? null : doc;
    }
}
