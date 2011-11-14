/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.gov.aims.atlasmapperserver;

import au.gov.aims.atlasmapperserver.annotation.ConfigField;

/**
 *
 * @author glafond
 */
public class LayerStyleConfig extends AbstractConfig implements Comparable<LayerStyleConfig> {
	@ConfigField
	private String name;

	@ConfigField
	private String title;

	@ConfigField
	private String description;

	@ConfigField(name="default", getter="isDefault", setter="setDefault")
	private Boolean _default;

	public LayerStyleConfig(ConfigManager configManager) {
		super(configManager);
	}

	@Override
	public void setJSONObjectKey(String key) {
		if (Utils.isBlank(this.name)) {
			this.name = key;
		}
	}

	public Boolean isDefault() {
		return _default;
	}

	public void setDefault(Boolean _default) {
		this._default = _default;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	// Order by title, name
	// Null at the end
	public int compareTo(LayerStyleConfig o) {
		if (this == o) {
			return 0;
		}
		// Null at the end
		if (o == null) {
			return 1;
		}

		int cmp = this.title == null ? (o.title == null ? 0 : -1) : this.title.compareTo(o.title);
		if (cmp == 0) {
			cmp = this.name == null ? (o.name == null ? 0 : -1) : this.name.compareTo(o.name);
		}

		return cmp;
	}

	@Override
	public String toString() {
		return "LayerStyleConfig{\n" +
				(Utils.isBlank(name) ? "" :        "	name=" + name + "\n") +
				(Utils.isBlank(title) ? "" :       "	title=" + title + "\n") +
				(Utils.isBlank(description) ? "" : "	description=" + description + "\n") +
				(_default == null ? "" :           "	default=" + _default + "\n") +
			'}';
	}
}
