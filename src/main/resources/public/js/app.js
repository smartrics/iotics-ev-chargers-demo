document.addEventListener('DOMContentLoaded', function() {
    var map = L.map('map').setView([51.505, -0.09], 13);
    var markers = [];
    var infoBox = document.getElementById('info-box');

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors'
    }).addTo(map);

    map.on('click', function(e) {
        executeQuery(e.latlng.lat, e.latlng.lng, calculateRadiusForCurrentView());
    });

    function executeQuery(lat, lon, radius) {
        document.getElementById('search-overlay').style.display = 'flex';
        map.off('click');

        fetch(`/markers?lat=${lat}&lon=${lon}&radius=${radius}`)
            .then(response => response.json())
            .then(data => {
                clearMarkers();
                data.forEach(markerData => {
                    var marker = L.marker([markerData.latitude, markerData.longitude])
                                  .addTo(map)
                                  .bindTooltip(markerData.label);
                    markers.push(marker);

                    marker.on('click', function(e) {
                        displayInfo(markerData);
                    });
                });
            })
            .catch(error => console.error('Error fetching markers:', error))
            .finally(() => {
                document.getElementById('search-overlay').style.display = 'none';
                map.on('click', function(e) {
                    executeQuery(e.latlng.lat, e.latlng.lng, calculateRadiusForCurrentView());
                });
            });
    }

    function displayInfo(data) {
        infoBox.style.display = 'block';
        infoBox.innerHTML = `<p>${data.type}</p>
                             <h4>${data.label}</h4>
                             <p>${data.info1}</p>
                             <p>${data.info2}</p>
                             <p>${data.info3}</p>`;
    }

    function clearMarkers() {
        markers.forEach(marker => map.removeLayer(marker));
        markers = [];
    }

    function calculateRadiusForCurrentView() {
        var bounds = map.getBounds();
        var center = bounds.getCenter();
        var northEast = bounds.getNorthEast();
        return center.distanceTo(northEast) / 2 / 1000; // Convert meters to kilometers
    }
});
