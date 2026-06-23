import CoreLocation

@Observable
final class LocationManager: NSObject, CLLocationManagerDelegate {
    var currentLocation: CLLocationCoordinate2D?
    var authorizationStatus: CLAuthorizationStatus = .notDetermined
    var locationError: String?

    private let manager = CLLocationManager()

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    var isPermissionDenied: Bool {
        authorizationStatus == .denied || authorizationStatus == .restricted
    }

    func requestPermission() {
        locationError = nil
        manager.requestWhenInUseAuthorization()
    }

    func requestLocation() {
        locationError = nil
        manager.requestLocation()
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        currentLocation = locations.last?.coordinate
        locationError = nil
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        locationError = "Couldn't get your location. Check Settings > Privacy > Location Services."
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
        if manager.authorizationStatus == .authorizedWhenInUse || manager.authorizationStatus == .authorizedAlways {
            manager.requestLocation()
        } else if manager.authorizationStatus == .denied {
            locationError = "Location access denied. Enable it in Settings > Privacy > Location Services."
        }
    }
}
