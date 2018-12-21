package au.gov.aims.atlasmapperserver.xml.TC211;

import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class GeoNetworkUrlBuilderTest {

    @Test
    public void testStraitFormardUrls() throws Exception {
        GeoNetworkUrlBuilder geoNetworkUrlBuilder;
        URL craftedLegacyUrl;
        URL crafted2_10Url;

        // Strait forward
        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?id=44003"));
        Assert.assertTrue("The URL is not valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
        craftedLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
        Assert.assertEquals("http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?id=44003", craftedLegacyUrl.toString());
        crafted2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
        Assert.assertEquals("http://www.cmar.csiro.au/geonetwork/srv/eng/xml_iso19139.mcp?id=44003&styleSheet=xml_iso19139.mcp.xsl", crafted2_10Url.toString());

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?uuid=urn:cmar.csiro.au:dataset:13028"));
        Assert.assertTrue("The URL is not valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
        craftedLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
        Assert.assertEquals("http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?uuid=urn%3Acmar.csiro.au%3Adataset%3A13028", craftedLegacyUrl.toString());
        crafted2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
        Assert.assertEquals("http://www.cmar.csiro.au/geonetwork/srv/eng/xml_iso19139.mcp?uuid=urn%3Acmar.csiro.au%3Adataset%3A13028&styleSheet=xml_iso19139.mcp.xsl", crafted2_10Url.toString());
    }

    @Test
    public void testModernGeoNetworkUrls() throws Exception {
        GeoNetworkUrlBuilder geoNetworkUrlBuilder;
        URL craftedLegacyUrl;
        URL crafted2_10Url;

        // Used in example
        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("https://www.domain.com/records/metadata/geonetwork/srv/eng/xml_iso19139.mcp?uuid=1a46774e-a3ac-4982-b08b-94ce1ad8d45c&styleSheet=xml_iso19139.mcp.xsl"));
        Assert.assertTrue("The URL is not valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
        craftedLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
        Assert.assertEquals("https://www.domain.com/records/metadata/geonetwork/srv/en/iso19139.xml?uuid=1a46774e-a3ac-4982-b08b-94ce1ad8d45c", craftedLegacyUrl.toString());
        crafted2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
        Assert.assertEquals("https://www.domain.com/records/metadata/geonetwork/srv/eng/xml_iso19139.mcp?uuid=1a46774e-a3ac-4982-b08b-94ce1ad8d45c&styleSheet=xml_iso19139.mcp.xsl", crafted2_10Url.toString());

        // Missing StyleSheet parameter
        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("https://www.domain.com/records/metadata/geonetwork/srv/eng/xml_iso19139.mcp?uuid=1a46774e-a3ac-4982-b08b-94ce1ad8d45c"));
        Assert.assertTrue("The URL is not valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
        craftedLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
        Assert.assertEquals("https://www.domain.com/records/metadata/geonetwork/srv/en/iso19139.xml?uuid=1a46774e-a3ac-4982-b08b-94ce1ad8d45c", craftedLegacyUrl.toString());
        crafted2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
        Assert.assertEquals("https://www.domain.com/records/metadata/geonetwork/srv/eng/xml_iso19139.mcp?uuid=1a46774e-a3ac-4982-b08b-94ce1ad8d45c&styleSheet=xml_iso19139.mcp.xsl", crafted2_10Url.toString());
    }

    @Test
    public void testUnusualUrlParameters() throws Exception {
        GeoNetworkUrlBuilder geoNetworkUrlBuilder;
        URL craftedLegacyUrl;
        URL crafted2_10Url;

        // Unusual parameter order
        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?currTab=full&potato=20&uuid=urn:cmar.csiro.au:dataset:13028&ignoreme=false"));
        Assert.assertTrue("The URL is not valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
        craftedLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
        Assert.assertEquals("http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?uuid=urn%3Acmar.csiro.au%3Adataset%3A13028", craftedLegacyUrl.toString());
        crafted2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
        Assert.assertEquals("http://www.cmar.csiro.au/geonetwork/srv/eng/xml_iso19139.mcp?uuid=urn%3Acmar.csiro.au%3Adataset%3A13028&styleSheet=xml_iso19139.mcp.xsl", crafted2_10Url.toString());
    }

    @Test
    public void testDifferentPort() throws Exception {
        GeoNetworkUrlBuilder geoNetworkUrlBuilder;
        URL craftedLegacyUrl;
        URL crafted2_10Url;

        // Different schema / port
        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("https://www.cmar.csiro.au:8443/geonetwork/srv/en/metadata.show?id=44003"));
        Assert.assertTrue("The URL is not valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
        craftedLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
        Assert.assertEquals("https://www.cmar.csiro.au:8443/geonetwork/srv/en/iso19139.xml?id=44003", craftedLegacyUrl.toString());
        crafted2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
        Assert.assertEquals("https://www.cmar.csiro.au:8443/geonetwork/srv/eng/xml_iso19139.mcp?id=44003&styleSheet=xml_iso19139.mcp.xsl", crafted2_10Url.toString());
    }

    @Test
    public void testMixedIDAndUUID() throws Exception {
        GeoNetworkUrlBuilder geoNetworkUrlBuilder;
        URL craftedLegacyUrl;
        URL crafted2_10Url;

        // ID or UUID mixed with other parameters
        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?uuid=urn:cmar.csiro.au:dataset:13028&currTab=full"));
        Assert.assertTrue("The URL is not valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
        craftedLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
        Assert.assertEquals("http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?uuid=urn%3Acmar.csiro.au%3Adataset%3A13028", craftedLegacyUrl.toString());
        crafted2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
        Assert.assertEquals("http://www.cmar.csiro.au/geonetwork/srv/eng/xml_iso19139.mcp?uuid=urn%3Acmar.csiro.au%3Adataset%3A13028&styleSheet=xml_iso19139.mcp.xsl", crafted2_10Url.toString());

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?id=44003&currTab=full"));
        Assert.assertTrue("The URL is not valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
        craftedLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
        Assert.assertEquals("http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?id=44003", craftedLegacyUrl.toString());
        crafted2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
        Assert.assertEquals("http://www.cmar.csiro.au/geonetwork/srv/eng/xml_iso19139.mcp?id=44003&styleSheet=xml_iso19139.mcp.xsl", crafted2_10Url.toString());

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?currTab=full&id=44003"));
        Assert.assertTrue("The URL is not valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
        craftedLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
        Assert.assertEquals("http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?id=44003", craftedLegacyUrl.toString());
        crafted2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
        Assert.assertEquals("http://www.cmar.csiro.au/geonetwork/srv/eng/xml_iso19139.mcp?id=44003&styleSheet=xml_iso19139.mcp.xsl", crafted2_10Url.toString());

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?currTab=full&id=44003&uuid=urn%3Acmar.csiro.au%3Adataset%3A13028"));
        Assert.assertTrue("The URL is not valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
        craftedLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
        Assert.assertEquals("http://www.cmar.csiro.au/geonetwork/srv/en/iso19139.xml?uuid=urn%3Acmar.csiro.au%3Adataset%3A13028", craftedLegacyUrl.toString());
        crafted2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
        Assert.assertEquals("http://www.cmar.csiro.au/geonetwork/srv/eng/xml_iso19139.mcp?uuid=urn%3Acmar.csiro.au%3Adataset%3A13028&styleSheet=xml_iso19139.mcp.xsl", crafted2_10Url.toString());
    }

    @Test
    public void testMissingIDAndUUID() throws Exception {
        GeoNetworkUrlBuilder geoNetworkUrlBuilder;
        URL craftedLegacyUrl;
        URL crafted2_10Url;

        // Parameter ID / UUID missing
        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?currTab=full&not-uuid=urn:cmar.csiro.au:dataset:13028"));
        Assert.assertFalse("The broken URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
        craftedLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
        Assert.assertNull(craftedLegacyUrl);
        crafted2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
        Assert.assertNull(crafted2_10Url);

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.cmar.csiro.au/geonetwork/srv/en/metadata.show?currTab=full&not-id=44003"));
        Assert.assertFalse("The broken URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
        craftedLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
        Assert.assertNull(craftedLegacyUrl);
        crafted2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
        Assert.assertNull(crafted2_10Url);

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.google.com"));
        Assert.assertFalse("The broken URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
        craftedLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
        Assert.assertNull(craftedLegacyUrl);
        crafted2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
        Assert.assertNull(crafted2_10Url);
    }

    @Test
    public void testNotQuiteGeoNetworkMestUrl() throws Exception {
        GeoNetworkUrlBuilder geoNetworkUrlBuilder;
        URL craftedLegacyUrl;
        URL crafted2_10Url;

        // ID / UUID present, in a URL that do not quite looks like a GeoNetwork URL
        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.google.com?id=44003"));
        Assert.assertFalse("The broken URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
        craftedLegacyUrl = geoNetworkUrlBuilder.craftGeoNetworkLegacyMestUrl();
        Assert.assertNull(craftedLegacyUrl);
        crafted2_10Url = geoNetworkUrlBuilder.craftGeoNetwork2_10MestUrl();
        Assert.assertNull(crafted2_10Url);
    }

    @Test
    public void testUnusableUrls() throws Exception {
        GeoNetworkUrlBuilder geoNetworkUrlBuilder;

        // Ensure stability (no exception) - Do not really care about the output...
        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(null);
        Assert.assertFalse("NULL URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.google.com/"));
        Assert.assertFalse("The broken URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.google.com/id=12"));
        Assert.assertFalse("The broken URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.google.com/?id=12"));
        Assert.assertFalse("The broken URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.google.com/?"));
        Assert.assertFalse("The broken URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.google.com/?&"));
        Assert.assertFalse("The broken URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.google.com/?&id=12"));
        Assert.assertFalse("The broken URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.google.com/?&/"));
        Assert.assertFalse("The broken URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.google.com/?&/id=12"));
        Assert.assertFalse("The broken URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.google.com//?&/"));
        Assert.assertFalse("The broken URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());

        geoNetworkUrlBuilder = new GeoNetworkUrlBuilder(new URL("http://www.google.com//?id=12&/"));
        Assert.assertFalse("The broken URL is valid: " + geoNetworkUrlBuilder.getOriginalUrl(), geoNetworkUrlBuilder.isValidGeoNetworkUrl());
    }
}
