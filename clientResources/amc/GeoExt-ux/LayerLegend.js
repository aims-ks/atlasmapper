// Update call setText on the element index 0... which can be the delete button...
// This patch loop through the legend items to find the label, rather than
// taking the first one and assuming that it is the label.
GeoExt.LayerLegend.prototype.update = function() {
	var title = this.getLayerTitle(this.layerRecord);
	this.items.each(function(item) {
		if (item.getXType() === 'label' && typeof(item.setText) === 'function') {
			// Label found
			if (item.text !== title) {
				item.setText(title);
			}
			// Stop the iteration
			return false;
		}
	});
}
