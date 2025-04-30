import pandas as pd
import geopandas as gpd
from shapely.geometry import LineString
import folium
import math
import matplotlib.pyplot as plt  # For color mapping

# Read line segment data from an Excel file
input_file = "D:/2024SS/Masterarbeit/line_segments.xlsx"  #  Change this to actual Excel file path
df = pd.read_excel(input_file)

# Ensure the required columns are present
# The file must have 'from_x', 'from_y', 'to_x', 'to_y' columns
required_columns = {"from_x", "from_y", "to_x", "to_y"}
if not required_columns.issubset(df.columns):
    raise ValueError(f"The input file is missing required columns: {required_columns}")

# Standardize each line segment so that the smaller point comes first
def normalize_row(row):
    p1 = (row["from_x"], row["from_y"])
    p2 = (row["to_x"], row["to_y"])
    return tuple(sorted([p1, p2]))

df["segment_key"] = df.apply(normalize_row, axis=1)

# Count the occurrence of each unique line segment
count_series = df["segment_key"].value_counts()
df["count"] = df["segment_key"].map(count_series)

# Create a GeoDataFrame with unique line segments
df["geometry"] = df.apply(lambda row: LineString([row["segment_key"][0], row["segment_key"][1]]), axis=1)
gdf = gpd.GeoDataFrame(df.drop_duplicates("segment_key"), geometry="geometry", crs="EPSG:25832")

# Reproject coordinates to WGS84 (longitude/latitude)
gdf = gdf.to_crs("EPSG:4326")

# Calculate the center point of all segments
center = gdf.unary_union.centroid
center_lat, center_lon = center.y, center.x

# Create a folium map centered on the calculated center
m = folium.Map(location=[center_lat, center_lon], zoom_start=14)

# Normalize usage counts to determine line width
min_count = gdf["count"].min()
max_count = gdf["count"].max()

def scale_weight(x, min_w=2, max_w=5):
    if max_count == min_count:
        return (min_w + max_w) / 2
    # Use square root scaling to emphasize high-traffic segments
    norm = (x - min_count) / (max_count - min_count)
    norm = math.sqrt(norm)  # Nonlinear scaling
    return min_w + norm * (max_w - min_w)

# Use matplotlib's colormap to generate colors (e.g., from red to purple)
colormap = plt.get_cmap("inferno")  # You can also choose other colormaps like "Reds"

def get_color(x):
    if max_count == min_count:
        norm_value = 0.5
    else:
        norm_value = (x - min_count) / (max_count - min_count)
    rgba = colormap(norm_value)
    return f"#{int(rgba[0]*255):02x}{int(rgba[1]*255):02x}{int(rgba[2]*255):02x}"

# Plot each line segment on the map
for idx, row in gdf.iterrows():
    line = row["geometry"]
    usage_count = row["count"]
    weight = scale_weight(usage_count)
    color = get_color(usage_count)
    coords = [(lat, lon) for lon, lat in line.coords]  # Switch (lon, lat) to (lat, lon) for folium
    folium.PolyLine(coords, color=color, weight=weight, tooltip=f"number: {usage_count}").add_to(m)

# Save the map to an HTML file
output_file = "D:/2024SS/Masterarbeit/roller_map.html" #  Change this to actual file path
m.save(output_file)
print(f"The map has been saved to {output_file}")