import Foundation
import CoreLocation

struct POIResult: Identifiable, Codable, Hashable {
    let placeId: String
    let name: String
    let address: String
    let lat: Double
    let lng: Double
    let detourSeconds: Int
    let detourFormatted: String
    let rating: Double
    let userRatingCount: Int
    let isOpenNow: Bool
    let types: [String]
    let photoReference: String?

    var id: String { placeId }

    var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: lat, longitude: lng)
    }

    var detourColor: DetourColor {
        let minutes = detourSeconds / 60
        if minutes < 3 { return .green }
        if minutes < 7 { return .yellow }
        if minutes < 15 { return .orange }
        return .red
    }

    enum DetourColor: String {
        case green, yellow, orange, red
    }
}

struct SearchResponse: Codable {
    let results: [POIResult]
    let route: RouteInfo
}

struct RouteInfo: Codable {
    let encodedPolyline: String
    let durationSeconds: Int
    let distanceMeters: Int
}
