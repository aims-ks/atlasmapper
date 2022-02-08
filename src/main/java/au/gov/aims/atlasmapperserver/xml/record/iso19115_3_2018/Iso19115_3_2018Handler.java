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

import au.gov.aims.atlasmapperserver.Utils;
import au.gov.aims.atlasmapperserver.xml.record.MetadataDocument;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Iso19115_3_2018Handler extends DefaultHandler {
    private static final Logger LOGGER = Logger.getLogger(Iso19115_3_2018Handler.class.getName());

    private static final String GEX_BBOX_CONTAINER = "gex:EX_GeographicBoundingBox";
    private static final String GEX_BBOX_WEST_CONTAINER = "gex:westBoundLongitude";
    private static final String GEX_BBOX_EAST_CONTAINER = "gex:eastBoundLongitude";
    private static final String GEX_BBOX_SOUTH_CONTAINER = "gex:southBoundLatitude";
    private static final String GEX_BBOX_NORTH_CONTAINER = "gex:northBoundLatitude";
    private static final String GCO_BBOX_DECIMAL = "gco:Decimal";

    private static final String CIT_LINK_CONTAINER = "cit:CI_OnlineResource";

    private static final String CIT_LINK_URL_CONTAINER = "cit:linkage";
    private static final String MRI_LINK_URL_STRING = "mri:URL";
    private static final String GCO_LINK_URL_STRING = "gco:CharacterString";

    private static final String CIT_LINK_PROTOCOL_CONTAINER = "cit:protocol";
    private static final String GCO_LINK_PROTOCOL_STRING = "gco:CharacterString";

    private static final String CIT_LINK_NAME_CONTAINER = "cit:name";
    private static final String GCO_LINK_NAME_STRING = "gco:CharacterString";

    private static final String CIT_LINK_DESCRIPTION_CONTAINER = "cit:description";
    private static final String GCO_LINK_DESCRIPTION_STRING = "gco:CharacterString";

    private static final String CIT_LINK_APPLICATION_PROFILE_CONTAINER = "cit:applicationProfile";
    private static final String GCO_LINK_APPLICATION_PROFILE_STRING = "gco:CharacterString";

    private Iso19115_3_2018Document doc;
    private StringBuilder collectedChars;
    private String west, east, south, north;

    private MetadataDocument.Link currentLink;
    private Stack<String> xmlPath;
    // Marker to highlight important paths, without having to check the current path every time.
    private XMLPathMarker xmlPathMarker;

    public Iso19115_3_2018Handler(Iso19115_3_2018Document doc) {
        super();
        this.doc = doc;
        this.xmlPath = new Stack<String>();

        this.xmlPathMarker = null;
    }

    @Override
    public void startElement(
            String uri,
            String localName,
            String qName,
            Attributes attributes) throws SAXException {

        this.xmlPath.push(qName);
        this.xmlPathMarker = XMLPathMarker.get(this.xmlPath);

        if (this.xmlPathMarker != null) {
            this.collectedChars = new StringBuilder();

            if (XMLPathMarker.BBOXES_MDB.equals(this.xmlPathMarker)) {

                if (GEX_BBOX_CONTAINER.equalsIgnoreCase(qName)) {
                    this.west = null; this.east = null; this.south = null; this.north = null;
                }

            } else if (XMLPathMarker.LINKS_MDB.equals(this.xmlPathMarker)) {

                if (CIT_LINK_CONTAINER.equalsIgnoreCase(qName)) {
                    this.currentLink = new MetadataDocument.Link();
                }
            }
        }
    }

    @Override
    public void endElement(
            String uri,
            String localName,
            String qName) throws SAXException {

        if (this.xmlPathMarker != null) {
            String previousQName = (this.xmlPath.size() < 2 ? null : this.xmlPath.get(this.xmlPath.size() - 2));

            if (XMLPathMarker.ABSTRACT_MDB.equals(this.xmlPathMarker)) {
                this.doc.setAbstract(this.collectedChars.toString());

            } else if (XMLPathMarker.BBOXES_MDB.equals(this.xmlPathMarker)) {
                /*
                <gex:EX_GeographicBoundingBox>
                    <gex:westBoundLongitude>
                        <gco:Decimal>122.2115</gco:Decimal>
                    </gex:westBoundLongitude>
                    <gex:eastBoundLongitude>
                        <gco:Decimal>122.2115</gco:Decimal>
                    </gex:eastBoundLongitude>
                    <gex:southBoundLatitude>
                        <gco:Decimal>-18.0158</gco:Decimal>
                    </gex:southBoundLatitude>
                    <gex:northBoundLatitude>
                        <gco:Decimal>-18.0158</gco:Decimal>
                    </gex:northBoundLatitude>
                </gex:EX_GeographicBoundingBox>
                */

                if (GEX_BBOX_WEST_CONTAINER.equalsIgnoreCase(previousQName)
                        && GCO_BBOX_DECIMAL.equalsIgnoreCase(qName)) {

                    this.west = this.collectedChars.toString();

                } else if (GEX_BBOX_EAST_CONTAINER.equalsIgnoreCase(previousQName)
                        && GCO_BBOX_DECIMAL.equalsIgnoreCase(qName)) {

                    this.east = this.collectedChars.toString();

                } else if (GEX_BBOX_SOUTH_CONTAINER.equalsIgnoreCase(previousQName)
                        && GCO_BBOX_DECIMAL.equalsIgnoreCase(qName)) {

                    this.south = this.collectedChars.toString();

                } else if (GEX_BBOX_NORTH_CONTAINER.equalsIgnoreCase(previousQName)
                        && GCO_BBOX_DECIMAL.equalsIgnoreCase(qName)) {

                    this.north = this.collectedChars.toString();

                } else if (GEX_BBOX_CONTAINER.equalsIgnoreCase(qName)) {
                    if (this.west != null && this.east != null && this.south != null && this.north != null) {
                        try {
                            Double west = Double.valueOf(this.west);
                            Double east = Double.valueOf(this.east);
                            Double south = Double.valueOf(this.south);
                            Double north = Double.valueOf(this.north);

                            if (west.equals(east) && south.equals(north)) {
                                // It's a point
                                this.doc.addPoint(new MetadataDocument.Point(west, north));
                            } else {
                                // It's a bbox
                                MetadataDocument.Polygon polygon = new MetadataDocument.Polygon();
                                polygon.addPoint(new MetadataDocument.Point(west, north));
                                polygon.addPoint(new MetadataDocument.Point(east, north));
                                polygon.addPoint(new MetadataDocument.Point(east, south));
                                polygon.addPoint(new MetadataDocument.Point(west, south));

                                this.doc.addPolygon(polygon);
                            }
                        } catch(Exception ex) {
                            LOGGER.log(Level.WARNING, "Can not parse the bounding box: [{0}, {1}, {2}, {3}]: {4}", new String[]{
                                this.west, this.east, this.south, this.north, Utils.getExceptionMessage(ex)
                            });
                            LOGGER.log(Level.FINE, "Stack trace:", ex);
                        }
                    }

                    this.west = null; this.east = null; this.south = null; this.north = null;
                }

            } else if (XMLPathMarker.POLYGONS_MDB.equals(this.xmlPathMarker)) {
                /*
                lat,lon,elevation lat,lon,elevation etc.
                <gml:coordinates>
                    122.2115,-18.0158,0 121.9344,-15.0683,0 121.9669,-13.6502,0 121.9654,-13.6815,0 122.0005,-13.7115,0 122.0398,-13.7039,0 122.0347,-13.7721,0 122.0585,-13.6669,0 122.0209,-13.6215,0 122.0320,-13.6327,0 122.0219,-13.6227,0 122.0283,-13.6267,0 122.0265,-13.6294,0 121.8303,-16.2410,0 122.2170,-18.0027,0 121.8303,-16.2410,0 122.0265,-13.6294,0 122.0283,-13.6267,0 122.0219,-13.6227,0 122.0320,-13.6327,0 122.0209,-13.6215,0 122.0585,-13.6669,0 122.0347,-13.7721,0 122.0398,-13.7039,0 122.0005,-13.7115,0 121.9654,-13.6815,0 121.9669,-13.6502,0 121.9344,-15.0683,0 122.2115,-18.0158,0
                </gml:coordinates>
                */
                String coords = this.collectedChars.toString();
                if (Utils.isNotBlank(coords)) {
                    String[] coordArray = coords.split("\\s+");
                    if (coordArray != null && coordArray.length > 0) {
                        try {
                            MetadataDocument.Polygon polygon = new MetadataDocument.Polygon();
                            for (String coord : coordArray) {
                                String[] coordParts = coord.split(",");
                                if (coordParts != null && coordParts.length > 0) {
                                    Double[] parsedCoords = new Double[coordParts.length];
                                    for (int i=0; i<coordParts.length; i++) {
                                        parsedCoords[i] = Double.valueOf(coordParts[i]);
                                    }

                                    if (parsedCoords.length == 3) {
                                        polygon.addPoint(parsedCoords[1], parsedCoords[0], parsedCoords[2]);
                                    } else if (parsedCoords.length == 2) {
                                        polygon.addPoint(parsedCoords[1], parsedCoords[0]);
                                    }
                                }
                            }
                            this.doc.addPolygon(polygon);
                        } catch(Exception ex) {
                            LOGGER.log(Level.WARNING, "Can not parse the polygon: [{0}]: {1}",
                                    new String[]{ coords, Utils.getExceptionMessage(ex) });
                            LOGGER.log(Level.FINE, "Stack trace:", ex);
                        }
                    }
                }

            } else if (XMLPathMarker.LINKS_MDB.equals(this.xmlPathMarker)) {
                /*
                <cit:CI_OnlineResource>
                    <cit:linkage>
                        <mri:URL>http://mest.aodn.org.au:80/geonetwork/srv/en/metadata.show?uuid=87263960-92f0-4836-b8c5-8486660ddfe0</mri:URL>
                    </cit:linkage>
                    <cit:protocol>
                        <gco:CharacterString>WWW:LINK-1.0-http--metadata-URL</gco:CharacterString>
                    </cit:protocol>
                    <cit:name gco:nilReason="missing">
                        <gco:CharacterString/>
                    </cit:name>
                    <cit:description>
                        <gco:CharacterString>Point of truth URL of this metadata record</gco:CharacterString>
                    </cit:description>
                </cit:CI_OnlineResource>
                */

                if (
                    // <cit:linkage><mri:URL>
                    (CIT_LINK_URL_CONTAINER.equalsIgnoreCase(previousQName) && MRI_LINK_URL_STRING.equalsIgnoreCase(qName)) ||
                    // <cit:linkage><gco:CharacterString>
                    (CIT_LINK_URL_CONTAINER.equalsIgnoreCase(previousQName) && GCO_LINK_URL_STRING.equalsIgnoreCase(qName))
                ) {

                    this.currentLink.setUrl(this.collectedChars.toString());

                } else if (CIT_LINK_PROTOCOL_CONTAINER.equalsIgnoreCase(previousQName)
                        && GCO_LINK_PROTOCOL_STRING.equalsIgnoreCase(qName)) {

                    this.currentLink.setProtocolStr(this.collectedChars.toString());

                } else if (CIT_LINK_NAME_CONTAINER.equalsIgnoreCase(previousQName)
                        && GCO_LINK_NAME_STRING.equalsIgnoreCase(qName)) {

                    this.currentLink.setName(this.collectedChars.toString());

                } else if (CIT_LINK_DESCRIPTION_CONTAINER.equalsIgnoreCase(previousQName)
                        && GCO_LINK_DESCRIPTION_STRING.equalsIgnoreCase(qName)) {

                    this.currentLink.setDescription(this.collectedChars.toString());

                } else if (CIT_LINK_APPLICATION_PROFILE_CONTAINER.equalsIgnoreCase(previousQName)
                        && GCO_LINK_APPLICATION_PROFILE_STRING.equalsIgnoreCase(qName)) {

                    this.currentLink.setApplicationProfile(this.collectedChars.toString());

                } else if (CIT_LINK_CONTAINER.equalsIgnoreCase(qName)) {
                    this.doc.addLink(this.currentLink);
                    this.currentLink = null;
                }
            }
        }

        // The current QName is not needed (kept for debugging), but the call to the pop method is mandatory...
        String currentQName = this.xmlPath.pop();
        this.xmlPathMarker = XMLPathMarker.get(this.xmlPath);

        /*
        // Log any anomaly in the balance of the document.
        // NOTE: This never happen since an unbalanced document trigger an exception.
        if (!currentQName.equalsIgnoreCase(qName)) {
            LOGGER.log(Level.WARNING, "The document [{0}] has a unbalanced tag:\n# Path: {1}\n# Opening tag: {2}\n# Closing tag: {3}",
                    new String[]{this.doc.getUri(), this.xmlPath.toString(), qName, currentQName});
        }
        */
    }

    @Override
    public void characters(
            char ch[],
            int start,
            int length) throws SAXException {

        if (this.xmlPathMarker != null && this.collectedChars != null) {
            this.collectedChars.append(ch, start, length);
        }
    }

    private enum XMLPathMarker {
        ABSTRACT_MDB (new String[]{"mdb:MD_Metadata", "mdb:identificationInfo", "mri:MD_DataIdentification", "mri:abstract", "gco:CharacterString"}),
        BBOXES_MDB (new String[]{"mdb:MD_Metadata", "mdb:identificationInfo", "mri:MD_DataIdentification", "mri:extent", "gex:EX_Extent", "gex:geographicElement", "gex:EX_GeographicBoundingBox"}),
        POLYGONS_MDB (new String[]{"mdb:MD_Metadata", "mdb:identificationInfo", "mri:MD_DataIdentification", "mri:extent", "gex:EX_Extent", "gex:geographicElement", "gex:EX_BoundingPolygon", "gex:polygon", "gml:Polygon", "gml:exterior", "gml:LinearRing", "gml:coordinates"}),
        LINKS_MDB (new String[]{"mdb:MD_Metadata", "mdb:distributionInfo", "mrd:MD_Distribution", "mrd:transferOptions", "mrd:MD_DigitalTransferOptions", "mrd:onLine", "cit:CI_OnlineResource"});

        private final String[] path;

        XMLPathMarker(String[] path) {
            this.path = path;
        }

        public static XMLPathMarker get(List<String> xmlPath) {
            if (xmlPath == null || xmlPath.isEmpty()) {
                return null;
            }

            XMLPathMarker[] markers = XMLPathMarker.values();
            for (XMLPathMarker marker : markers) {
                boolean inPath = true;
                if (xmlPath.size() < marker.path.length) {
                    inPath = false;
                } else {
                    for (int i=0, len=marker.path.length; inPath && i<len; i++) {
                        if (!marker.path[i].equalsIgnoreCase(xmlPath.get(i))) {
                            inPath = false;
                        }
                    }
                }

                if (inPath) {
                    return marker;
                }
            }

            return null;
        }
    }
}
