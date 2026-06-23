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
        case .green: return Color(red: 0.2, green: 0.7, blue: 0.3)
        case .yellow: return Color(red: 0.9, green: 0.7, blue: 0.1)
        case .orange: return Color(red: 0.95, green: 0.5, blue: 0.1)
        case .red: return Color(red: 0.9, green: 0.2, blue: 0.2)
        }
    }
}
