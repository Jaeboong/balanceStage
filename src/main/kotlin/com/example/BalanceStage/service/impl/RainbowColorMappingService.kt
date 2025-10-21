package com.example.BalanceStage.service.impl

import com.example.BalanceStage.service.ColorMappingService
import javafx.scene.paint.Color
import org.springframework.stereotype.Service

/**
 * 무지개 그라데이션 색상 매핑 구현
 * 양수(+): 빨강 → 주황 → 노랑 → 초록
 * 0: 초록
 * 음수(-): 초록 → 시안 → 파랑 → 보라
 */
@Service
class RainbowColorMappingService : ColorMappingService {

    override fun mapDeviationToColor(deviation: Double, maxDeviation: Double): Color {
        if (maxDeviation < 0.001) return Color.GREEN

        // Normalize deviation to [-1, 1]
        val normalized = (deviation / maxDeviation).coerceIn(-1.0, 1.0)

        // Map to hue: -1 (Purple/270°) -> 0 (Green/120°) -> +1 (Red/0°)
        val hue = when {
            normalized > 0 -> {
                // Positive: Green(120°) -> Yellow(60°) -> Orange(30°) -> Red(0°)
                120.0 * (1.0 - normalized)
            }
            normalized < 0 -> {
                // Negative: Green(120°) -> Cyan(180°) -> Blue(240°) -> Purple(270°)
                120.0 + 150.0 * (-normalized)
            }
            else -> 120.0  // Zero = Green
        }

        return Color.hsb(hue, 0.9, 0.95)
    }
}
