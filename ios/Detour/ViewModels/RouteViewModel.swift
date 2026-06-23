import SwiftUI
import MapKit
import CoreLocation

@Observable
final class RouteViewModel {
    var originQuery = ""
    var destinationQuery = ""

    var originCoordinate: CLLocationCoordinate2D?
    var destinationCoordinate: CLLocationCoordinate2D?

    var originName: String?
    var destinationName: String?

    var originSuggestions: [MKLocalSearchCompletion] = []
    var destinationSuggestions: [MKLocalSearchCompletion] = []

    var route: MKRoute?
    var poiResults: [POIResult] = []
    var selectedPOI: POIResult?
    var searchQuery = "coffee"
    var isLoading = false
    var errorMessage: String?

    var routeDurationFormatted: String? {
        guard let route else { return nil }
        let minutes = Int(route.expectedTravelTime / 60)
        if minutes >= 60 {
            return "\(minutes / 60)h \(minutes % 60)min"
        }
        return "\(minutes) min"
    }

    var routeDistanceFormatted: String? {
        guard let route else { return nil }
        let km = route.distance / 1000
        if km >= 10 {
            return String(format: "%.0f km", km)
        }
        return String(format: "%.1f km", km)
    }

    var isSearchReady: Bool {
        originCoordinate != nil && destinationCoordinate != nil
    }

    private let originCompleter = SearchCompleterDelegate()
    private let destinationCompleter = SearchCompleterDelegate()

    init() {
        originCompleter.onUpdate = { [weak self] results in
            self?.originSuggestions = results
        }
        destinationCompleter.onUpdate = { [weak self] results in
            self?.destinationSuggestions = results
        }
    }

    func updateOriginQuery(_ query: String) {
        originQuery = query
        originCoordinate = nil
        originName = nil
        originCompleter.completer.queryFragment = query
    }

    func updateDestinationQuery(_ query: String) {
        destinationQuery = query
        destinationCoordinate = nil
        destinationName = nil
        destinationCompleter.completer.queryFragment = query
    }

    func selectOrigin(_ completion: MKLocalSearchCompletion) {
        originQuery = completion.title
        originSuggestions = []
        originCompleter.completer.queryFragment = ""

        Task {
            if let coordinate = await resolveCoordinate(for: completion) {
                await MainActor.run {
                    self.originCoordinate = coordinate
                    self.originName = completion.title
                }
            }
        }
    }

    func selectDestination(_ completion: MKLocalSearchCompletion) {
        destinationQuery = completion.title
        destinationSuggestions = []
        destinationCompleter.completer.queryFragment = ""

        Task {
            if let coordinate = await resolveCoordinate(for: completion) {
                await MainActor.run {
                    self.destinationCoordinate = coordinate
                    self.destinationName = completion.title
                }
            }
        }
    }

    func useCurrentLocation(_ coordinate: CLLocationCoordinate2D) {
        originQuery = "Current Location"
        originCoordinate = coordinate
        originName = "Current Location"
        originSuggestions = []
    }

    func search() {
        guard let origin = originCoordinate, let destination = destinationCoordinate else { return }
        isLoading = true
        errorMessage = nil
        route = nil
        poiResults = []
        selectedPOI = nil

        Task {
            do {
                // Local route for polyline rendering
                let dirRequest = MKDirections.Request()
                dirRequest.source = MKMapItem(placemark: MKPlacemark(coordinate: origin))
                dirRequest.destination = MKMapItem(placemark: MKPlacemark(coordinate: destination))
                dirRequest.transportType = .automobile

                async let directionsTask = MKDirections(request: dirRequest).calculate()

                // Backend search for POIs along route
                async let searchTask = APIService.search(
                    origin: (origin.latitude, origin.longitude),
                    destination: (destination.latitude, destination.longitude),
                    query: searchQuery
                )

                let directionsResponse = try await directionsTask
                let searchResponse = try await searchTask

                await MainActor.run {
                    self.route = directionsResponse.routes.first
                    self.poiResults = searchResponse.results
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                    self.isLoading = false
                }
            }
        }
    }

    func clear() {
        originQuery = ""
        destinationQuery = ""
        originCoordinate = nil
        destinationCoordinate = nil
        originName = nil
        destinationName = nil
        originSuggestions = []
        destinationSuggestions = []
        route = nil
        poiResults = []
        selectedPOI = nil
        errorMessage = nil
    }

    func swapOriginDestination() {
        swap(&originQuery, &destinationQuery)
        swap(&originCoordinate, &destinationCoordinate)
        swap(&originName, &destinationName)
    }

    private func resolveCoordinate(for completion: MKLocalSearchCompletion) async -> CLLocationCoordinate2D? {
        let request = MKLocalSearch.Request(completion: completion)
        let search = MKLocalSearch(request: request)
        do {
            let response = try await search.start()
            return response.mapItems.first?.placemark.coordinate
        } catch {
            return nil
        }
    }
}

private final class SearchCompleterDelegate: NSObject, MKLocalSearchCompleterDelegate {
    let completer = MKLocalSearchCompleter()
    var onUpdate: (([MKLocalSearchCompletion]) -> Void)?

    override init() {
        super.init()
        completer.delegate = self
        completer.resultTypes = [.address, .pointOfInterest]
    }

    func completerDidUpdateResults(_ completer: MKLocalSearchCompleter) {
        onUpdate?(completer.results)
    }

    func completer(_ completer: MKLocalSearchCompleter, didFailWithError error: Error) {
        onUpdate?([])
    }
}
