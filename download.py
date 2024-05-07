import os
import requests

# Base URL of the API
base_url = "https://api.openchargemap.io/v3/poi/"

# Compose query string parameters using environment variables
query_params = {
    'output'       : 'json',
    'client'       : 'demo-smartrics',
    'countrycode'  : 'GB',
    'verbose'      : 'true',
    'locationanme' : 'Cambridge',
    'latitude'     : '52.197121',
    'longitude'    : '0.132275',
    'distance'     : '35',
    'distanceunit' : 'miles',
    'maxresults'   : '10000',
    'key'          :'579bbe16-0416-4bc2-ab24-f86b5da56975'
    # Add more parameters as needed
}

# Build the full URL with query string
url_with_query = base_url + '?' + '&'.join([f"{key}={value}" for key, value in query_params.items() if value])

# Specify the output file path
output_file_path = f"data{query_params["maxresults"]}_{query_params["countrycode"]}_{query_params["distance"]}_{query_params["distanceunit"]}_{query_params["locationanme"]}.{query_params["output"]}"

# Download data from the API
response = requests.get(url_with_query)

# Check if the request was successful (status code 200)
if response.status_code == 200:
    with open(output_file_path, 'wb') as f:
        f.write(response.content)
    print("Data downloaded successfully.")
else:
    print(f"Error downloading data. Status code: {response.status_code}")
