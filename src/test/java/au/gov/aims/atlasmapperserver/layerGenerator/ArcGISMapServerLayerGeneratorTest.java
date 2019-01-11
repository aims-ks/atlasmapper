package au.gov.aims.atlasmapperserver.layerGenerator;

import org.junit.Assert;
import org.junit.Test;

public class ArcGISMapServerLayerGeneratorTest {

    @Test
    public void testBaseGetJSONUrl() throws Exception {
        ArcGISMapServerLayerGenerator layerGenerator = new ArcGISMapServerLayerGenerator();

        String arcGISPath = null;
        String type = null;

        String jsonUrlStr = layerGenerator.getJSONUrl("http://maps2.six.nsw.gov.au/arcgis/rest/services", arcGISPath, type);
        Assert.assertEquals("http://maps2.six.nsw.gov.au/arcgis/rest/services?f=json&pretty=true", jsonUrlStr);

        jsonUrlStr = layerGenerator.getJSONUrl("http://maps2.six.nsw.gov.au/arcgis/rest/services/", arcGISPath, type);
        Assert.assertEquals("http://maps2.six.nsw.gov.au/arcgis/rest/services/?f=json&pretty=true", jsonUrlStr);

        jsonUrlStr = layerGenerator.getJSONUrl("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps", arcGISPath, type);
        Assert.assertEquals("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps?f=json&pretty=true", jsonUrlStr);

        jsonUrlStr = layerGenerator.getJSONUrl("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps/", arcGISPath, type);
        Assert.assertEquals("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps/?f=json&pretty=true", jsonUrlStr);
    }

    @Test
    public void testPathGetJSONUrl() throws Exception {
        ArcGISMapServerLayerGenerator layerGenerator = new ArcGISMapServerLayerGenerator();

        String arcGISPath = "sixmaps/Cadastre";
        String type = "MapServer";

        String jsonUrlStr = layerGenerator.getJSONUrl("http://maps2.six.nsw.gov.au/arcgis/rest/services", arcGISPath, type);
        Assert.assertEquals("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps/Cadastre/MapServer?f=json&pretty=true", jsonUrlStr);

        jsonUrlStr = layerGenerator.getJSONUrl("http://maps2.six.nsw.gov.au/arcgis/rest/services/", arcGISPath, type);
        Assert.assertEquals("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps/Cadastre/MapServer?f=json&pretty=true", jsonUrlStr);

        jsonUrlStr = layerGenerator.getJSONUrl("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps", arcGISPath, type);
        Assert.assertEquals("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps/Cadastre/MapServer?f=json&pretty=true", jsonUrlStr);

        jsonUrlStr = layerGenerator.getJSONUrl("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps/", arcGISPath, type);
        Assert.assertEquals("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps/Cadastre/MapServer?f=json&pretty=true", jsonUrlStr);
    }

    @Test
    public void testLayerGetJSONUrl() throws Exception {
        ArcGISMapServerLayerGenerator layerGenerator = new ArcGISMapServerLayerGenerator();

        String arcGISPath = "sixmaps/Cadastre";
        String type = "MapServer";
        String layerId = "0";

        String jsonUrlStr = layerGenerator.getJSONUrl("http://maps2.six.nsw.gov.au/arcgis/rest/services", arcGISPath, type, layerId);
        Assert.assertEquals("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps/Cadastre/MapServer/0?f=json&pretty=true", jsonUrlStr);

        jsonUrlStr = layerGenerator.getJSONUrl("http://maps2.six.nsw.gov.au/arcgis/rest/services/", arcGISPath, type, layerId);
        Assert.assertEquals("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps/Cadastre/MapServer/0?f=json&pretty=true", jsonUrlStr);

        jsonUrlStr = layerGenerator.getJSONUrl("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps", arcGISPath, type, layerId);
        Assert.assertEquals("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps/Cadastre/MapServer/0?f=json&pretty=true", jsonUrlStr);

        jsonUrlStr = layerGenerator.getJSONUrl("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps/", arcGISPath, type, layerId);
        Assert.assertEquals("http://maps2.six.nsw.gov.au/arcgis/rest/services/sixmaps/Cadastre/MapServer/0?f=json&pretty=true", jsonUrlStr);
    }
}
