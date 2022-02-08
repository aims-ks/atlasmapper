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
package au.gov.aims.atlasmapperserver.xml.record;

import au.gov.aims.atlasmapperserver.thread.ThreadLogger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;

public abstract class AbstractMetadataParser {
    protected ThreadLogger logger;

    public AbstractMetadataParser(ThreadLogger logger) {
        this.logger = logger;
    }

    public abstract MetadataDocument parseFile(File file, String location) throws Exception;

    public static SAXParser getSAXParser() throws SAXException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        // Disabling DTD loading & validation
        // Without those 2 lines, initialising XML files takes ages (about 10 minutes for 500kb, down to a few ms with those lines)
        factory.setFeature("http://apache.org/xml/features/validation/schema", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return factory.newSAXParser();
    }
}
