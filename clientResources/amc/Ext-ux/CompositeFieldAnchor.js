/*
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
 */

Ext.namespace("Ext.ux");
Ext.namespace("Ext.ux.form");

/**
 * This class is basically a Ext.form.CompositeField, with 'anchor'
 * layout instead of 'hbox'.
 * It is used to display the datetime fields (date field + time field)
 * one above the other, in the same component, so they stay together.
 */
Ext.ux.form.CompositeFieldAnchor = Ext.extend(Ext.form.CompositeField, {
	initComponent: function() {
		var labels = [],
			items  = this.items,
			item;

		for (var i=0, j = items.length; i < j; i++) {
			item = items[i];

			labels.push(item.fieldLabel);

			//apply any defaults
			Ext.applyIf(item, this.defaults);

			//apply default margins to each item except the last
			if (!(i == j - 1 && this.skipLastItemMargin)) {
				Ext.applyIf(item, {margins: this.defaultMargins});
			}
		}

		this.fieldLabel = this.fieldLabel || this.buildLabel(labels);

		/**
		 * @property fieldErrors (attribute of Ext.form.CompositeField)
		 * @type Ext.util.MixedCollection
		 * MixedCollection of current errors on the Composite's subfields. This is used internally to track when
		 * to show and hide error messages at the Composite level. Listeners are attached to the MixedCollection's
		 * add, remove and replace events to update the error icon in the UI as errors are added or removed.
		 */
		this.fieldErrors = new Ext.util.MixedCollection(true, function(item) {
			return item.field;
		});

		this.fieldErrors.on({
			scope  : this,
			add    : this.updateInvalidMark,
			remove : this.updateInvalidMark,
			replace: this.updateInvalidMark
		});

		// Call initComponent of the grand-parent
		Ext.form.CompositeField.superclass.initComponent.apply(this, arguments);

		this.innerCt = new Ext.Container({
			// The layout is the only modification to the Ext.form.CompositeField initComponent method
			layout  : 'anchor',
			items   : this.items,
			cls     : 'x-form-composite',
			defaultMargins: '0 3 0 0'
		});

		var fields = this.innerCt.findBy(function(c) {
			return c.isFormField;
		}, this);

		/**
		 * @property items
		 * @type Ext.util.MixedCollection
		 * Internal collection of all of the subfields in this Composite
		 */
		this.items = new Ext.util.MixedCollection();
		this.items.addAll(fields);
	}
});

Ext.reg('compositefieldanchor', Ext.ux.form.CompositeFieldAnchor);
