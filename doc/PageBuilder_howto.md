# PageBuilder - Developer Guide

## Overview

`PageBuilder` is a Java class that generates responsive HTML dashboard pages from JSON dashboard definitions. It creates layouts that adapt between 1-column mobile views and 10-column desktop views using Bootstrap 5.

## Table of Contents

- [Basic Usage](#basic-usage)
- [JSON Dashboard Definition Format](#json-dashboard-definition-format)
- [Responsive Grid System](#responsive-grid-system)
- [Column Configuration](#column-configuration)
- [Widget Configuration](#widget-configuration)
- [Mobile-Specific Settings](#mobile-specific-settings)
- [Advanced Features](#advanced-features)
- [Customization](#customization)
- [Error Handling](#error-handling)
- [Examples](#examples)

## Basic Usage

### Minimum Requirements

```java
import com.signomix.reports.domain.dashboard.PageBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;

// Basic usage with default 10 columns
try {
    String dashboardJson = "{\"title\":\"My Dashboard\",\"widgets\":[],\"items\":[]}";
    String html = PageBuilder.buildPage(dashboardJson);
    
    // Use the generated HTML
    System.out.println(html);
    // Or save to file, return in API response, etc.
} catch (JsonProcessingException e) {
    // Handle JSON parsing errors
    e.printStackTrace();
}

// Usage with 12 columns
try {
    String dashboardJson = "{\"title\":\"My Dashboard\",\"widgets\":[],\"items\":[]}";
    String html = PageBuilder.buildPage(dashboardJson, 12);
    
    // Use the generated HTML
    System.out.println(html);
} catch (JsonProcessingException e) {
    // Handle JSON parsing errors
    e.printStackTrace();
}
```

### Loading from File

```java
import java.nio.file.Files;
import java.nio.file.Paths;

String dashboardJson = new String(Files.readAllBytes(
    Paths.get("path/to/dashboard-definition.json")));
String html = PageBuilder.buildPage(dashboardJson);
```

## JSON Dashboard Definition Format

The dashboard definition JSON must contain the following structure:

```json
{
  "title": "Dashboard Title",
  "widgets": [
    {
      "title": "Widget Title",
      "name": "widget_id",
      "mobile_size": "1",
      "mobile_position": 0,
      "channel": "data_channel",
      "type": "widget_type",
      "config": "{}"
    }
  ],
  "items": [
    {
      "_el10": {
        "x": 0,
        "y": 0,
        "w": 2,
        "h": 1
      }
    }
  ]
}
```

### Required Fields

- `title`: Dashboard title (string)
- `widgets`: Array of widget definitions
- `items`: Array of layout items (must match widgets by index)

### Widget Fields

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| `title` | string | Widget display title | Optional (falls back to `name` or `channel`) |
| `name` | string | Widget identifier | Optional |
| `mobile_size` | string | Mobile visibility/height ("0" = hidden, ">0" = rows) | Optional (defaults to "1") |
| `mobile_position` | number | Custom mobile row position | Optional |
| `channel` | string | Data channel name | Optional |
| `type` | string | Widget type | Optional |
| `config` | string | Widget configuration JSON | Optional |

### Item Fields (Positioning)

Each item in the `items` array corresponds to a widget by index and contains:

```json
{
  "_el10" or "_el12": {
    "x": 0,      // Column position (0-9 for 10 columns, 0-11 for 12 columns)
    "y": 0,      // Row position
    "w": 2,      // Width in columns (1-10 or 1-12)
    "h": 1       // Height in rows (1 or greater)
  }
}
```

#### Widget Height Support

**New Feature**: Widgets can now occupy multiple rows by setting `h > 1`.

When a widget has `h > 1`, the PageBuilder creates a nested grid structure:
- The widget occupies `h` consecutive rows in the grid
- A nested grid is created inside the widget card
- The first row contains the widget title and content
- Additional rows (`h-1` rows) are created as empty space fillers

**Example**:
```json
{
  "_el10": {
    "x": 0,
    "y": 0,
    "w": 4,
    "h": 3  // This widget will occupy 3 rows vertically
  }
}
```

This creates a widget that spans 4 columns and 3 rows, with:
- Row 1: Widget title and content
- Row 2: Empty space
- Row 3: Empty space

**Use Cases**:
- Large charts that need more vertical space
- Dashboard headers or footers
- Multi-section widgets with multiple data views
- Creating visual hierarchy in your dashboard

## Responsive Grid System

### Desktop Layout (≥768px width)

- **Configurable column grid system** (10 or 12 columns)
- Uses custom CSS classes: `col-{columns}-{width}` (e.g., `col-10-1` to `col-10-10` or `col-12-1` to `col-12-12`)
- Widgets are positioned according to `_el{columns}.x`, `_el{columns}.y`, `_el{columns}.w`, `_el{columns}.h`
- Widgets are sorted by row (`y`) then column (`x`)

### Mobile Layout (<768px width)

- **1-column full-width layout**
- Each widget takes full width
- Vertical stacking based on `mobile_position` or original index
- Widgets with `mobile_size="0"` are hidden

### Bootstrap Breakpoints

- `d-none d-md-block`: Desktop-only content
- `d-md-none`: Mobile-only content
- `col-12`: Full-width columns on mobile

## Column Configuration

The PageBuilder supports two column configurations: 10 columns (default) and 12 columns.

### Using 10 Columns (Default)

```java
// Default behavior - uses 10 columns
String html = PageBuilder.buildPage(dashboardJson);

// Explicit 10 columns
String html = PageBuilder.buildPage(dashboardJson, 10);
```

### Using 12 Columns

```java
// Use 12 columns
String html = PageBuilder.buildPage(dashboardJson, 12);
```

### Choosing Between 10 and 12 Columns

| Factor | 10 Columns | 12 Columns |
|--------|-----------|-----------|
| **Grid precision** | Less precise (10% increments) | More precise (8.33% increments) |
| **Layout flexibility** | Good for simple layouts | Better for complex layouts |
| **Widget sizing** | Limited to multiples of 10% | More granular sizing options |
| **Use case** | Quick dashboards, simple layouts | Detailed dashboards, precise layouts |

**Recommendation**:
- Use **10 columns** for simple dashboards or when you want simpler calculations
- Use **12 columns** for more complex layouts requiring finer control over widget widths

## Widget Configuration

### Title Resolution

Widget titles are extracted in this priority order:
1. `title` field
2. `name` field  
3. `channel` field
4. Falls back to "Widget" if all are empty

### Mobile Visibility

- `mobile_size="0"`: Widget hidden on mobile (but visible on desktop)
- `mobile_size="1"` or higher: Widget visible on mobile
- `mobile_size` value determines height in mobile rows

### Mobile Positioning

- `mobile_position`: Custom row position on mobile
- Widgets are sorted by `mobile_position` (ascending)
- Widgets without `mobile_position` are sorted by original index
- Allows reordering widgets specifically for mobile view

## Advanced Features

### Custom CSS Styling

The generated HTML includes default styling that can be overridden:

```css
.widget-card {
    border: 1px solid #ddd;
    border-radius: 8px;
    padding: 15px;
    height: 100%;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.widget-title {
    font-weight: bold;
    margin-bottom: 10px;
    font-size: 1.1rem;
}
```

### XSS Protection

All widget titles and dashboard titles are automatically HTML-escaped to prevent XSS attacks:

```java
private static String escapeHtml(String text) {
    return text.replace("&", "&amp;")
              .replace("<", "&lt;")
              .replace(">", "&gt;")
              .replace("\"", "&quot;")
              .replace("'", "&#39;");
}
```

### Bootstrap Integration

The generated HTML includes:
- Bootstrap 5 CSS from CDN
- Bootstrap 5 JS bundle from CDN
- Responsive meta tag for proper mobile rendering
- Viewport settings for optimal display

## Customization

### Overriding Default Styles

Add custom CSS to override default styling:

```java
// Add this to your JSON processing before calling buildPage
String customCss = "
    <style>
        .widget-card { background-color: #f8f9fa; }
        .widget-title { color: #0d6efd; }
    </style>
";

String html = PageBuilder.buildPage(dashboardJson);
html = html.replace("</head>", customCss + "</head>");
```

### Adding Custom JavaScript

```java
String customJs = "
    <script>
        console.log('Dashboard loaded');
        // Add custom widget behavior here
    </script>
";

String html = PageBuilder.buildPage(dashboardJson);
html = html.replace("</body>", customJs + "</body>");
```

### Using Different Bootstrap Version

Modify the CDN URLs in the generated HTML:

```java
String html = PageBuilder.buildPage(dashboardJson);
html = html.replace("bootstrap@5.3.0", "bootstrap@5.2.0");
```

## Error Handling

### JSON Parsing Errors

```java
try {
    String html = PageBuilder.buildPage(invalidJson);
} catch (JsonProcessingException e) {
    // Handle malformed JSON
    System.err.println("Invalid dashboard JSON: " + e.getMessage());
    // Return error page or default dashboard
}
```

### Missing or Invalid Data

The PageBuilder handles missing data gracefully:
- Missing `title` → Uses "Dashboard" as default
- Missing widget `title` → Falls back to `name` or `channel`
- Missing positioning data → Uses defaults (x=0, y=0, w=1, h=1)
- Invalid `mobile_size` → Treats as "1" (visible)

### Empty Dashboard

If `widgets` or `items` arrays are empty, generates empty grid sections but still produces valid HTML structure.

## Examples

### Simple Dashboard

```java
String simpleDashboard = "{"
    + "\"title\":\"Simple Dashboard\","
    + "\"widgets\":["
    + "    {\"title\":\"Temperature\", \"mobile_size\":\"1\"},"
    + "    {\"title\":\"Humidity\", \"mobile_size\":\"1\"}"
    + "],"
    + "\"items\":["
    + "    {\"_el10\":{\"x\":0,\"y\":0,\"w\":2,\"h\":1}},"
    + "    {\"_el10\":{\"x\":3,\"y\":0,\"w\":2,\"h\":1}}"
    + "]"
    + "}";

String html = PageBuilder.buildPage(simpleDashboard);
```

### Dashboard with 12 Columns

```java
String twelveColumnDashboard = "{"
    + "\"title\":\"12-Column Dashboard\","
    + "\"widgets\":["
    + "    {\"title\":\"Main Content\", \"mobile_size\":\"1\"},"
    + "    {\"title\":\"Sidebar\", \"mobile_size\":\"1\"}"
    + "],"
    + "\"items\":["
    + "    {\"_el12\":{\"x\":0,\"y\":0,\"w\":9,\"h\":1}},"  // 9 columns wide
    + "    {\"_el12\":{\"x\":9,\"y\":0,\"w\":3,\"h\":1}}"   // 3 columns wide
    + "]"
    + "}";

String html = PageBuilder.buildPage(twelveColumnDashboard, 12);
```

### Complex Dashboard with Mobile Customization

```java
String complexDashboard = "{"
    + "\"title\":\"Advanced Dashboard\","
    + "\"widgets\":["
    + "    {\"title\":\"Main Chart\", \"mobile_size\":\"2\", \"mobile_position\":0},"
    + "    {\"title\":\"Side Panel\", \"mobile_size\":\"0\"},"  // Hidden on mobile
    + "    {\"title\":\"Status\", \"mobile_size\":\"1\", \"mobile_position\":1}"
    + "],"
    + "\"items\":["
    + "    {\"_el10\":{\"x\":0,\"y\":0,\"w\":6,\"h\":2}},"  // Main Chart - 6 columns wide, 2 rows tall
    + "    {\"_el10\":{\"x\":6,\"y\":0,\"w\":4,\"h\":2}},"  // Side Panel - 4 columns wide, 2 rows tall
    + "    {\"_el10\":{\"x\":0,\"y\":2,\"w\":3,\"h\":1}}"   // Status - 3 columns wide, 1 row tall
    + "]"
    + "}";

String html = PageBuilder.buildPage(complexDashboard);
```

### Complex Dashboard with 12 Columns

```java
String complexTwelveDashboard = "{"
    + "\"title\":\"Advanced 12-Column Dashboard\","
    + "\"widgets\":["
    + "    {\"title\":\"Main Chart\", \"mobile_size\":\"2\", \"mobile_position\":0},"
    + "    {\"title\":\"Side Panel\", \"mobile_size\":\"0\"},"  // Hidden on mobile
    + "    {\"title\":\"Status\", \"mobile_size\":\"1\", \"mobile_position\":1}"
    + "],"
    + "\"items\":["
    + "    {\"_el12\":{\"x\":0,\"y\":0,\"w\":8,\"h\":2}},"  // Main Chart - 8 columns wide, 2 rows tall
    + "    {\"_el12\":{\"x\":8,\"y\":0,\"w\":4,\"h\":2}},"  // Side Panel - 4 columns wide, 2 rows tall
    + "    {\"_el12\":{\"x\":0,\"y\":2,\"w\":4,\"h\":1}}"   // Status - 4 columns wide, 1 row tall
    + "]"
    + "}";

String html = PageBuilder.buildPage(complexTwelveDashboard, 12);
```


### Dashboard with Tall Widgets (Using Height Feature)

```java
String tallWidgetsDashboard = "{"
    + "\"title\":\"Dashboard with Tall Widgets\","
    + "\"widgets\":["
    + "    {\"title\":\"Header\", \"mobile_size\":\"1\"},"
    + "    {\"title\":\"Large Chart\", \"mobile_size\":\"1\"},"
    + "    {\"title\":\"Summary\", \"mobile_size\":\"1\"}"
    + "],"
    + "\"items\":["
    + "    {\"_el10\":{\"x\":0,\"y\":0,\"w\":12,\"h\":1}},"  // Header - full width, 1 row
    + "    {\"_el10\":{\"x\":0,\"y\":1,\"w\":8,\"h\":3}},"  // Large Chart - 8 columns, 3 rows tall
    + "    {\"_el10\":{\"x\":8,\"y\":1,\"w\":4,\"h\":2}}"   // Summary - 4 columns, 2 rows tall
    + "]"
    + "}";

String html = PageBuilder.buildPage(tallWidgetsDashboard);
```

**Note**: The Large Chart widget (h=3) will occupy 3 rows vertically, creating a nested grid with 3 rows inside its card. The Summary widget (h=2) will occupy 2 rows with a nested grid containing 2 rows.
## Integration with Existing Systems

### REST API Endpoint

```java
@Path("/api/dashboard")
public class DashboardResource {
    
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getDashboard(@QueryParam("id") String dashboardId) {
        try {
            String dashboardJson = dashboardService.getDashboardDefinition(dashboardId);
            return PageBuilder.buildPage(dashboardJson);
        } catch (JsonProcessingException e) {
            return "<html><body><h1>Error loading dashboard</h1></body></html>";
        }
    }
}
```

### File Generation

```java
public void generateDashboardFiles() {
    List<String> dashboardIds = dashboardService.getAllDashboardIds();
    
    for (String id : dashboardIds) {
        try {
            String dashboardJson = dashboardService.getDashboardDefinition(id);
            String html = PageBuilder.buildPage(dashboardJson);
            
            Files.write(
                Paths.get("output/dashboard-" + id + ".html"),
                html.getBytes()
            );
        } catch (Exception e) {
            log.error("Failed to generate dashboard " + id, e);
        }
    }
}
```

## Best Practices

### Performance Optimization

1. **Cache Generated HTML**: Store generated HTML to avoid reprocessing
2. **Use CDN for Bootstrap**: Already implemented for fast loading
3. **Minify HTML**: For production use, minify the generated HTML

### Security

1. **Validate Input JSON**: Ensure JSON comes from trusted sources
2. **Sanitize Widget Content**: The built-in `escapeHtml()` handles titles
3. **Use HTTPS**: Especially important when loading external resources

### Mobile Optimization

1. **Limit Mobile Widgets**: Use `mobile_size="0"` for less important widgets
2. **Custom Mobile Order**: Use `mobile_position` for optimal mobile UX
3. **Test on Devices**: Verify layout on various screen sizes

## Troubleshooting

### Common Issues

**Issue**: Widgets not appearing on desktop
- **Cause**: Missing or invalid `_el10` positioning data
- **Solution**: Ensure all items have valid `_el10.x`, `_el10.y`, `_el10.w`, `_el10.h`

**Issue**: Widgets not appearing on mobile  
- **Cause**: `mobile_size="0"` or missing mobile data
- **Solution**: Set `mobile_size="1"` or higher for mobile visibility

**Issue**: Widgets in wrong order on mobile
- **Cause**: Missing `mobile_position` values
- **Solution**: Add `mobile_position` or ensure widgets are in correct order in array

**Issue**: JSON parsing errors
- **Cause**: Malformed JSON structure
- **Solution**: Validate JSON using a JSON validator before processing

### Debugging Tips

1. **Inspect Generated HTML**: Save to file and examine structure
2. **Browser Developer Tools**: Check responsive behavior at different breakpoints
3. **Enable Bootstrap Grid Debugging**: Add temporary borders to visualize grid
4. **Log Widget Data**: Add debugging to see how widgets are being processed

## Future Enhancements

The PageBuilder can be extended to support:
- **Dynamic Widget Content**: Replace "Widget content" placeholder with actual data
- **Widget Types**: Different rendering for charts, tables, gauges, etc.
- **Theming**: Support for light/dark mode
- **Localization**: Multi-language support
- **Interactive Features**: Add JavaScript for widget interactions

## Support

For issues or questions about PageBuilder:
- Check the example dashboard definition in `doc/dashboard_defiinition_example1.json`
- Review the source code in `src/main/java/com/signomix/reports/domain/dashboard/PageBuilder.java`
- Examine the test file for usage examples

## License

This component is part of the Signomix Reports project and is subject to its licensing terms.
### Dashboard with Tall Widgets (Using Height Feature)

```java
String tallWidgetsDashboard = "{"
    + "\"title\":\"Dashboard with Tall Widgets\","
    + "\"widgets\":["
    + "    {\"title\":\"Header\", \"mobile_size\":\"1\"},"
    + "    {\"title\":\"Large Chart\", \"mobile_size\":\"1\"},"
    + "    {\"title\":\"Summary\", \"mobile_size\":\"1\"}"
    + "],"
    + "\"items\":["
    + "    {\"_el10\":{\"x\":0,\"y\":0,\"w\":12,\"h\":1}},"  // Header - full width, 1 row
    + "    {\"_el10\":{\"x\":0,\"y\":1,\"w\":8,\"h\":3}},"  // Large Chart - 8 columns, 3 rows tall
    + "    {\"_el10\":{\"x\":8,\"y\":1,\"w\":4,\"h\":2}}"   // Summary - 4 columns, 2 rows tall
    + "\]"
    + "}";

String html = PageBuilder.buildPage(tallWidgetsDashboard);
```

**Note**: The Large Chart widget (h=3) will occupy 3 rows vertically, creating a nested grid with 3 rows inside its card. The Summary widget (h=2) will occupy 2 rows with a nested grid containing 2 rows.
