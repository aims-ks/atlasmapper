Define extra options to be added in the options panel of this layer.
Example:

```
"ea_ea:bluemarble" : {
	"options": [
		{
			"name": "comment",
			"title": "Comment",
			"type": "textarea"
		}
	]
}
```

**WARNING** this attribute is under development and may be unstable or change considerably between versions.

List of available attributes for this field:

| **Attribute** | **Type** | **Description** |
|:--------------|:---------|:----------------|
| name          | String   | The parameter's name, added to the request. |
| title         | String   | The displayed name for this option. It's shown as a label for the new input. |
| type          | String   | The ExtJS xtype for the input.<br />List of available types:<ul><li><b>checkbox</b></li><li><b>combo</b>: for dropdown. Not supported yet.</li><li><b>displayfield</b>: to display a readonly value.</li><li><b>hidden</b>: to send a value to the server without showing it to the user.</li><li><b>numberfield</b></li><li><b>sliderfield</b></li><li><b>textarea</b></li><li><b>textfield</b></li><li><b>timefield</b></li><li><b>datefield</b>: For date in format d/m/Y</li><li><b>ux-datefield</b>: For date displayed in format d/m/Y but sent to the server in format Y-m-d</li><li><b>ux-ncdatetimefield</b>: For date and time displayed in format d/m/Y and saved in the ISO format expected by NCWMS server.</li><li><b>ux-ncplotpanel</b>: To add time series and transect drawing features on NCWMS layers. NOTE: The name field is ignored with this type since it do not change the layer URL.</li></ul> |
| mandatory     | Boolean  | true to force the user to fill the option with a value, false otherwise. (default value: false) |
| defaultValue  | String   | Value pre-entered in the field. |