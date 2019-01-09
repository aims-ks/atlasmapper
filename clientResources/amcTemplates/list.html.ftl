<!DOCTYPE html>
<!--
 *  This file is part of AtlasMapper server and clients.
 *
 *  Copyright (C) 2011 Australian Institute of Marine Science
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
-->
<html>

<!-- Generated with AtlasMapper version ${version!} -->
<head>
    <title>${clientName!} layers</title>
    <link rel="icon" type="image/png" href="resources/favicon.png?atlasmapperVer=${version}" />
    <meta http-equiv="content-type" content="text/html;charset=utf-8" />

    <link rel="stylesheet" type="text/css" href="resources/css/styles.css?atlasmapperVer=${version}" />
    <!--[if lte IE 6 ]>
        <link rel="stylesheet" type="text/css" href="resources/css/styles-ie6.css?atlasmapperVer=${version}" />
    <![endif]-->

    <!--[if IE]>
        <script type="text/javascript" src="modules/Utils/ECMAScriptPatch.js"></script>
    <![endif]-->

    <style>
        /* https://css-tricks.com/snippets/css/a-guide-to-flexbox/ */
        div.layerContainer {
            display: flex;
            flex-wrap: wrap;
            justify-content: space-between;
        }

        div.layerBlock {
            width: ${layerBoxWidth!202}px;
            font-size: 0.8em;
            margin: 1em;
        }
        div.layerBlock .image {
            border: 1px solid #000000;
            background-image: url('resources/images/loading-small.gif');
            background-repeat: no-repeat;
            background-position: center;
        }

        h2 {
            background-color: #CCCCCC;
            text-align: center;
            font-size: 4em;
        }
        h2 span {
            font-size: 0.6em;
        }
    </style>

    <script type="text/javascript">
        /**
         * Get the list of all element of class "image"
         * and start "lazy" the image loading.
         * @param parent The parent element containing elements with class "image".
         *   Use "document" to trigger the load of images of all elements.
         */
        function loadImages(parent) {
            var imageDivElements = parent.getElementsByClassName("image");
            var imageDivElementArray = [];
            for (var i=0; i<imageDivElements.length; i++) {
                imageDivElementArray.push(imageDivElements[i]);
            }
            loadImage(imageDivElementArray);
        }

        /**
         * Load the images of the first "image" element.
         * Wait until all images are loaded before processing the next one.
         * @param imageDivElementArray Array of remaining HTML elements having the class "image".
         */
        function loadImage(imageDivElementArray) {
            if (imageDivElementArray && imageDivElementArray.length > 0) {
                var imageObjs = [];

                // Get the first "image" element
                var imageDivElement = imageDivElementArray.shift();

                // Get the background image src, if any.
                // Register the image in an Image object to monitor its loading state.
                // JavaScript magic! "data-atlasmapper-background-image" becomes "dataset.atlasmapperBackgroundImage"
                var backgroundImageSrc = imageDivElement.dataset.atlasmapperBackgroundImage;
                if (backgroundImageSrc) {
                    // Create an Image instance to monitor its loading state.
                    var backgroundImage = new Image();
                    backgroundImage.src = backgroundImageSrc;
                    imageObjs.push(backgroundImage);
                    imageDivElement.style.backgroundImage = "url('" + backgroundImageSrc + "')";
                } else {
                    imageDivElement.style.backgroundImage = "none";
                }

                // Get all the layer image src, if any.
                // Register the images in Image objects to monitor their loading state.
                // NOTE: Each "image" div contain only one layer, but we could add more in the future
                //   (using CSS float or something similar).
                var layerImageElements = imageDivElement.getElementsByTagName("img");
                if (layerImageElements && layerImageElements.length > 0) {
                    for (var i=0; i<layerImageElements.length; i++) {
                        var layerImageElement = layerImageElements[i];
                        // JavaScript magic! "data-atlasmapper-src" becomes "dataset.atlasmapperSrc"
                        var layerImageSrc = layerImageElement.dataset.atlasmapperSrc;
                        if (layerImageSrc) {
                            // Create an Image instance to monitor its loading state.
                            var layerImage = new Image();
                            layerImage.src = layerImageSrc;
                            imageObjs.push(layerImage);
                            layerImageElement.src = layerImageSrc;
                        }
                    }
                }

                // Every time an monitored image is loaded,
                // check the state of all monitored images to know if we are ready to proceed to the next
                // "image" from the "imageDivElementArray" list.
                if (imageObjs.length > 0) {
                    for (var i=0; i<imageObjs.length; i++) {
                        imageObjs[i].onload = function() {
                            checkLoadingImages(imageDivElementArray, imageObjs);
                        }
                    }
                } else {
                    // There is no image to load in this element. Load the next one immediately.
                    loadImage(imageDivElementArray);
                }
            }
        }

        /**
         * Trigger the next image load if all the Image objects of "imageObjs" are loaded.
         * @param imageDivElementArray Array of remaining HTML elements having the class "image".
         * @param imageObjs Array of loading Image objects.
         */
        function checkLoadingImages(imageDivElementArray, imageObjs) {
            var fullyLoaded = true;
            for (var i=0; i<imageObjs.length; i++) {
                if (!imageObjs[i].complete) {
                    fullyLoaded = false;
                }
            }

            if (fullyLoaded) {
                loadImage(imageDivElementArray);
            }
        }

        window.onload = function() { loadImages(document); };
    </script>
</head>

<body id="list">
    ${listPageHeader!}

    <#if layers??>
        <#list layers?keys as dataSourceName>
            <h2>${dataSourceName} <span>(${layers[dataSourceName]?size} layers)</span></h2>

            <div class="layerContainer">
                <#list layers[dataSourceName] as layer>
                    <div class="layerBlock">
                        <#if layer["imageUrl"]??>
                            <div class="image" style="width:${layer["imageWidth"]!200}px; height:${layer["imageHeight"]!180}px;" data-atlasmapper-background-image="${layer["baseLayerUrl"]!}">
                                <#if layer["mapUrl"]??>
                                    <a href="${layer["mapUrl"]!}" target="_blank"><img alt="${layer["title"]!"Untitled"}" src="resources/images/blank.gif" data-atlasmapper-src="${layer["imageUrl"]!}" style="border: none" /></a>
                                <#else>
                                    <img alt="${layer["title"]!"Untitled"}" src="resources/images/blank.gif" data-atlasmapper-src="${layer["imageUrl"]!}" style="border: none" />
                                </#if>
                            </div>
                        </#if>
                        <!-- ${layer["id"]!} -->
                        ${layer["title"]!"Untitled"}
                    </div>
                </#list>
            </div>
        </#list>
    </#if>

    ${listPageFooter!}
</body>

</html>
