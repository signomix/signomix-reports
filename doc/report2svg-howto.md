# Report to SVG Chart Generation - How To Guide

This guide explains how to use the Report to SVG Chart API to generate SVG charts from report definitions.

## Overview

The API provides a REST endpoint that accepts a `ReportDefinition` (containing data query) and a `ChartDefinition` (containing chart configuration), fetches the data, and returns an SVG chart.

## API Endpoint

```
POST /api/reports/charts/from-report
```

**Content-Type:** `application/json`
**Accept:** `image/svg+xml`
**Authentication:** Required (Bearer token in `Authentication` header)

## Request Format

### Headers

```
Authentication: Bearer <your-auth-token>
Content-Type: application/json
Accept: image/svg+xml
```

### Body

The request body should contain both `chartDefinition` and `reportDefinition` objects:

```json
{
  "chartDefinition": {
    "title": "Chart Title",
    "xAxisName": "Time",
    "yAxisName": "Value",
    "xAxisUnit": "seconds",
    "yAxisUnit": "°C",
    "width": 800,
    "height": 600,
    "titleFontSize": 16,
    "chartFontSize": 12,
    "xAxisMin": 1721252572735,
    "xAxisMax": 1721252581735,
    "measurementNames": ["Temperature", "Humidity"],
    "lineColors": ["#FF0000", "#0000FF"],
    "fillColor": "#FFFFFF"
  },
  "reportDefinition": {
    "dql": "SELECT temperature, humidity FROM devices WHERE eui='123456' LIMIT 100",
    "name": "Temperature and Humidity Report",
    "description": "Shows temperature and humidity data for device 123456"
  }
}
```

## Report Definition Configuration

The `reportDefinition` object specifies what data to fetch:

### Required Fields

- **`dql`** (string, required): The Data Query Language query that defines what data to fetch
  - Example: `"SELECT temperature, humidity FROM devices WHERE eui='123456' LIMIT 100"`
  - Must be a valid DQL query (see [DQL Documentation](https://documentation.signomix.com/pl/development/dql.md))
  - DQL is a specialized query language for IoT data, not a subset of SQL

### Optional Fields

- **`name`** (string): Human-readable name for the report
- **`description`** (string): Description of what the report shows
- **`organization`** (integer): Organization ID for multi-tenant systems
- **`tenant`** (integer): Tenant ID for multi-tenant systems
- **`path`** (string): Path or category for report organization
- **`userLogin`** (string): User associated with the report
- **`team`** (string): Team associated with the report
- **`administrators`** (string): Administrators for the report

## Chart Definition Configuration

The `chartDefinition` object specifies how the chart should look and behave:

### Basic Configuration

- **`title`** (string): Chart title displayed at the top
- **`xAxisName`** (string): Label for the X-axis
- **`yAxisName`** (string): Label for the Y-axis
- **`xAxisUnit`** (string): Unit of measurement for X-axis (displayed in brackets)
- **`yAxisUnit`** (string): Unit of measurement for Y-axis (displayed in brackets)

### Size and Layout

- **`width`** (integer, default: 800): Chart width in pixels
- **`height`** (integer, default: 600): Chart height in pixels

### Font Configuration

- **`titleFontSize`** (integer): Font size for the chart title
- **`chartFontSize`** (integer, default: 12): Font size for axis labels and legend

### Axis Configuration

- **`xAxisMin`** (number): Minimum value for X-axis (timestamp in milliseconds)
- **`xAxisMax`** (number): Maximum value for X-axis (timestamp in milliseconds)

### Data Series Configuration

- **`measurementNames`** (array of strings): Names for each data series
  - Example: `["Temperature", "Humidity"]`
  - If not provided, series will be named "Series 1", "Series 2", etc.

- **`lineColors`** (array of strings): Colors for each data series in hex format
  - Example: `["#FF0000", "#0000FF", "#00FF00"]`
  - Default colors: `["#000000", "#0000FF", "#FF0000", "#00FF00", "#FFA500", "#800080"]`

- **`fillColor`** (string): Fill color for data points in hex format
  - Example: `"#FFFFFF"` (white fill)
  - If not specified, no fill is applied

## DQL Query Examples

For complete DQL syntax and capabilities, refer to the [official DQL Documentation](https://documentation.signomix.com/pl/development/dql.md).

### Basic Time Series Query
```
SELECT temperature FROM devices WHERE eui='123456' LIMIT 100
```

### Multiple Metrics Query
```
SELECT temperature, humidity FROM devices WHERE eui='123456' LIMIT 100
```

### Time Range Query
```
SELECT temperature FROM devices WHERE eui='123456' AND timestamp > 1721252572735 AND timestamp < 1721252581735
```

### Multiple Devices Query
```
SELECT temperature FROM devices WHERE eui IN ('123456', '789012') LIMIT 100
```

### Advanced DQL Features
```
SELECT temperature, humidity FROM devices WHERE eui='123456' AND timestamp > now()-1h LIMIT 100
```

## Response Format

### Success Response

- **Status Code:** `200 OK`
- **Content-Type:** `image/svg+xml`
- **Body:** SVG XML content representing the chart

Example response headers:
```
HTTP/1.1 200 OK
Content-Type: image/svg+xml
Content-Length: 12345
```

### Error Responses

- **401 Unauthorized**: Invalid or missing authentication token
- **400 Bad Request**: Missing or invalid DQL query
- **500 Internal Server Error**: Data fetching or chart generation failure

Error responses include a JSON body with details:
```json
{
  "error": "Error message describing what went wrong"
}
```

## Usage Examples

### Example 1: Simple Temperature Chart

```bash
curl -X POST \
  https://api.example.com/api/reports/charts/from-report \
  -H 'Authentication: Bearer your-auth-token' \
  -H 'Content-Type: application/json' \
  -H 'Accept: image/svg+xml' \
  -d '{
    "chartDefinition": {
      "title": "Device Temperature",
      "xAxisName": "Time",
      "yAxisName": "Temperature",
      "yAxisUnit": "°C",
      "width": 800,
      "height": 400
    },
    "reportDefinition": {
      "dql": "SELECT temperature FROM devices WHERE eui='"'"'123456'"'"' LIMIT 24"
    }
  }' \
  --output temperature-chart.svg
```

### Example 2: Multi-Metric Chart with Custom Colors

```bash
curl -X POST \
  https://api.example.com/api/reports/charts/from-report \
  -H 'Authentication: Bearer your-auth-token' \
  -H 'Content-Type: application/json' \
  -H 'Accept: image/svg+xml' \
  -d '{
    "chartDefinition": {
      "title": "Environmental Data",
      "xAxisName": "Time",
      "yAxisName": "Values",
      "width": 1000,
      "height": 600,
      "titleFontSize": 18,
      "measurementNames": ["Temperature", "Humidity"],
      "lineColors": ["#FF0000", "#0000FF"],
      "xAxisMin": 1721252572735,
      "xAxisMax": 1721252581735
    },
    "reportDefinition": {
      "dql": "SELECT temperature, humidity FROM devices WHERE eui='"'"'123456'"'"' LIMIT 100",
      "name": "Environmental Monitoring",
      "description": "Temperature and humidity data for device 123456"
    }
  }' \
  --output environmental-chart.svg
```

### Example 3: Multiple Devices Comparison

```bash
curl -X POST \
  https://api.example.com/api/reports/charts/from-report \
  -H 'Authentication: Bearer your-auth-token' \
  -H 'Content-Type: application/json' \
  -H 'Accept: image/svg+xml' \
  -d '{
    "chartDefinition": {
      "title": "Temperature Comparison",
      "xAxisName": "Time",
      "yAxisName": "Temperature",
      "yAxisUnit": "°C",
      "measurementNames": ["Device A", "Device B"],
      "lineColors": ["#008000", "#800080"]
    },
    "reportDefinition": {
      "dql": "SELECT temperature FROM devices WHERE eui IN ('"'"'123456'"'"', '"'"'789012'"'"') LIMIT 50"
    }
  }' \
  --output comparison-chart.svg
```

### Example 4: Minimal Chart - Lines Only (No Title, No Axes, No Legend)

This example shows how to generate a chart with only the data lines, no title, no axis labels, and no legend - useful for embedding in dashboards or when you want minimal visual clutter.

```bash
curl -X POST \
  https://api.example.com/api/reports/charts/from-report \
  -H 'Authentication: Bearer your-auth-token' \
  -H 'Content-Type: application/json' \
  -H 'Accept: image/svg+xml' \
  -d '{
    "chartDefinition": {
      "width": 600,
      "height": 300,
      "lineColors": ["#FF0000", "#0000FF"],
      "measurementNames": []
    },
    "reportDefinition": {
      "dql": "SELECT temperature, humidity FROM devices WHERE eui='"'"'123456'"'"' LIMIT 50"
    }
  }' \
  --output minimal-chart.svg
```

**Key points for minimal charts:**
- Omit `title`, `xAxisName`, `yAxisName`, `xAxisUnit`, `yAxisUnit` to hide axis labels
- Set empty `measurementNames` array to hide the legend
- Still specify `lineColors` to control the appearance of data series
- Adjust `width` and `height` for your specific use case

## Best Practices

### Query Optimization

1. **Limit Data Points**: Use `LIMIT` clause to restrict the number of data points
   - Good: `LIMIT 100`
   - Avoid: No limit on large datasets

2. **Time Range Filtering**: Use timestamp filters to get only relevant data
   - Example: `AND timestamp > 1721252572735 AND timestamp < 1721252581735`
   - Use relative time: `AND timestamp > now()-1h`

3. **Specific Metrics**: Only select the metrics you need to display
   - Good: `SELECT temperature, humidity`
   - Avoid: Selecting unnecessary metrics

4. **Device Filtering**: Use specific device EUIs rather than wildcards when possible
   - Good: `WHERE eui='123456'`
   - Consider: `WHERE eui IN ('123456', '789012')` for multiple specific devices

### Chart Design

1. **Appropriate Size**: Choose chart dimensions that fit your use case
   - Dashboard widgets: 400-600px width
   - Full reports: 800-1200px width

2. **Readable Fonts**: Ensure font sizes are legible
   - Title: 16-20px
   - Labels: 12-14px

3. **Color Contrast**: Use distinct colors for different series
   - Avoid similar colors for different metrics
   - Consider colorblind-friendly palettes

4. **Axis Labels**: Always provide clear axis labels with units
   - Example: `"Temperature (°C)"`

5. **Time Range**: Set appropriate axis limits for better visualization
   - Use `xAxisMin` and `xAxisMax` to focus on relevant time periods

6. **Minimal Charts**: For dashboard embeds or compact displays
   - Omit title and axis labels when they're not needed
   - Use empty `measurementNames` array to hide legend
   - Focus on clean data visualization with just the essential lines

### Performance Considerations

1. **Data Volume**: Large datasets (>1000 points) may impact performance
2. **Complex Queries**: Complex DQL queries may take longer to execute
3. **Chart Complexity**: Many series (>6) may reduce readability
4. **Caching**: Consider caching frequently used chart configurations

## Troubleshooting

### Common Issues

1. **401 Unauthorized**: 
   - Verify your authentication token is valid
   - Check token has not expired
   - Ensure token has proper permissions

2. **400 Bad Request**:
   - Verify `reportDefinition.dql` is present and not empty
   - Check DQL query syntax (refer to [DQL Documentation](https://documentation.signomix.com/pl/development/dql.md))
   - Ensure JSON format is valid

3. **500 Internal Server Error**:
   - Check if the DQL query is valid and executable
   - Verify the devices/metrics in the query exist
   - Check database connectivity
   - Review server logs for detailed error information

4. **Empty Chart**:
   - Verify the query returns data
   - Check time range filters are correct
   - Ensure device EUIs are valid
   - Test the DQL query separately using other reporting endpoints

5. **Malformed SVG**:
   - This is rare but can happen with very large datasets
   - Try reducing the number of data points
   - Check for special characters in metric names

## Integration Examples

### JavaScript/Fetch API

```javascript
async function generateChart() {
  const response = await fetch('https://api.example.com/api/reports/charts/from-report', {
    method: 'POST',
    headers: {
      'Authentication': 'Bearer your-auth-token',
      'Content-Type': 'application/json',
      'Accept': 'image/svg+xml'
    },
    body: JSON.stringify({
      chartDefinition: {
        title: "Temperature Chart",
        xAxisName: "Time",
        yAxisName: "Temperature",
        yAxisUnit: "°C",
        width: 800,
        height: 400
      },
      reportDefinition: {
        dql: "SELECT temperature FROM devices WHERE eui='123456' LIMIT 24"
      }
    })
  });
  
  if (response.ok) {
    const svg = await response.text();
    document.getElementById('chart-container').innerHTML = svg;
  } else {
    const error = await response.json();
    console.error('Error generating chart:', error);
  }
}
```

### Python/Requests

```python
import requests

url = "https://api.example.com/api/reports/charts/from-report"
headers = {
    "Authentication": "Bearer your-auth-token",
    "Content-Type": "application/json",
    "Accept": "image/svg+xml"
}

payload = {
    "chartDefinition": {
        "title": "Humidity Chart",
        "xAxisName": "Time",
        "yAxisName": "Humidity",
        "yAxisUnit": "%",
        "width": 800,
        "height": 400
    },
    "reportDefinition": {
        "dql": "SELECT humidity FROM devices WHERE eui='123456' LIMIT 24"
    }
}

response = requests.post(url, headers=headers, json=payload)

if response.status_code == 200:
    with open("humidity-chart.svg", "w") as f:
        f.write(response.text)
    print("Chart generated successfully")
else:
    print(f"Error: {response.status_code} - {response.text}")
```

## Limitations

1. **Maximum Series**: Up to 6 data series can be displayed (defined by `ChartDefinition.MAX_SERIES`)
2. **Data Volume**: Very large datasets may impact performance or fail to render
3. **Query Complexity**: Complex DQL queries may have execution time limits
4. **SVG Size**: Extremely large charts may produce very large SVG files
5. **DQL Features**: Not all DQL features may be supported for chart generation

## Future Enhancements

The API may be extended in the future to support:
- Additional chart types (bar charts, pie charts, etc.)
- More advanced styling options
- Template-based chart configurations
- Export to other formats (PNG, PDF, etc.)
- Real-time data streaming for live charts

## Additional Resources

- [DQL Documentation](https://documentation.signomix.com/pl/development/dql.md) - Complete reference for Data Query Language
- [Reporting API](https://documentation.signomix.com/pl/api/reporting.md) - Other reporting endpoints
- [Chart Configuration](https://documentation.signomix.com/pl/guides/chart-configuration.md) - Advanced chart customization options