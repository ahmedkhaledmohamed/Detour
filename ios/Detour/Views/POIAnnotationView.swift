import SwiftUI

struct DetourBadge: View {
    let poi: POIResult
    let isSelected: Bool

    var body: some View {
        Text(poi.detourFormatted)
            .font(.caption2.weight(.bold))
            .foregroundStyle(.white)
            .padding(.horizontal, 6)
            .padding(.vertical, 3)
            .background(badgeColor, in: Capsule())
            .scaleEffect(isSelected ? 1.2 : 1.0)
            .animation(.easeInOut(duration: 0.2), value: isSelected)
    }

    private var badgeColor: Color {
        switch poi.detourColor {
        case .green: return .green
        case .yellow: return .orange
        case .orange: return .orange.opacity(0.8)
        case .red: return .red
        }
    }
}
