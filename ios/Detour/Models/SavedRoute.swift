import Foundation
import CoreLocation

struct SavedRoute: Identifiable, Codable, Hashable {
    let id: String
    var name: String
    let originLat: Double
    let originLng: Double
    let originName: String
    let destinationLat: Double
    let destinationLng: Double
    let destinationName: String
    var defaultCategory: String
    let createdAt: Date

    var originCoordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: originLat, longitude: originLng)
    }

    var destinationCoordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: destinationLat, longitude: destinationLng)
    }

    static func create(
        name: String,
        origin: CLLocationCoordinate2D,
        originName: String,
        destination: CLLocationCoordinate2D,
        destinationName: String,
        defaultCategory: String = "coffee"
    ) -> SavedRoute {
        SavedRoute(
            id: UUID().uuidString,
            name: name,
            originLat: origin.latitude,
            originLng: origin.longitude,
            originName: originName,
            destinationLat: destination.latitude,
            destinationLng: destination.longitude,
            destinationName: destinationName,
            defaultCategory: defaultCategory,
            createdAt: Date()
        )
    }
}

final class SavedRoutesStore {
    static let shared = SavedRoutesStore()
    private let key = "savedRoutes"
    private let maxRoutes = 5

    func load() -> [SavedRoute] {
        guard let data = UserDefaults.standard.data(forKey: key),
              let routes = try? JSONDecoder().decode([SavedRoute].self, from: data)
        else { return [] }
        return routes
    }

    func save(_ route: SavedRoute) {
        var routes = load()
        routes.removeAll { $0.id == route.id }
        routes.insert(route, at: 0)
        if routes.count > maxRoutes { routes = Array(routes.prefix(maxRoutes)) }
        if let data = try? JSONEncoder().encode(routes) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }

    func delete(_ route: SavedRoute) {
        var routes = load()
        routes.removeAll { $0.id == route.id }
        if let data = try? JSONEncoder().encode(routes) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }
}

struct RecentSearch: Identifiable, Codable, Hashable {
    let id: String
    let originName: String
    let originLat: Double
    let originLng: Double
    let destinationName: String
    let destinationLat: Double
    let destinationLng: Double
    let category: String
    let timestamp: Date

    var originCoordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: originLat, longitude: originLng)
    }
    var destinationCoordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: destinationLat, longitude: destinationLng)
    }
}

final class RecentSearchStore {
    static let shared = RecentSearchStore()
    private let key = "recentSearches"
    private let maxEntries = 10

    func load() -> [RecentSearch] {
        guard let data = UserDefaults.standard.data(forKey: key),
              let entries = try? JSONDecoder().decode([RecentSearch].self, from: data)
        else { return [] }
        return entries
    }

    func add(originName: String, origin: CLLocationCoordinate2D,
             destinationName: String, destination: CLLocationCoordinate2D,
             category: String) {
        var entries = load()
        entries.removeAll {
            $0.originName == originName && $0.destinationName == destinationName && $0.category == category
        }
        let entry = RecentSearch(
            id: UUID().uuidString,
            originName: originName,
            originLat: origin.latitude, originLng: origin.longitude,
            destinationName: destinationName,
            destinationLat: destination.latitude, destinationLng: destination.longitude,
            category: category,
            timestamp: Date()
        )
        entries.insert(entry, at: 0)
        if entries.count > maxEntries { entries = Array(entries.prefix(maxEntries)) }
        if let data = try? JSONEncoder().encode(entries) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }
}
