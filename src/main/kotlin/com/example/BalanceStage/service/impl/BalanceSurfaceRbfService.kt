package com.example.BalanceStage.service.impl

import com.example.BalanceStage.service.RbfSurfaceVisualizationService
import javafx.application.Platform
import javafx.stage.Stage
import org.springframework.stereotype.Service
import plot.BalanceSurfaceApp

/**
 * BalanceSurfaceApp을 래핑한 RBF 표면 시각화 서비스
 *
 * BalanceSurfaceApp이 제공하는 고급 기능들:
 * - RBF (Radial Basis Function) 가우시안 보간
 * - 무지개 색상 히트맵 + 등고선 텍스처 매핑
 * - Z-Up 좌표계
 * - 궤도 컨트롤 (마우스 드래그/줌)
 * - PCA 평면 기반 높이 계산
 *
 * @see plot.BalanceSurfaceApp
 */
@Service
class BalanceSurfaceRbfService : RbfSurfaceVisualizationService {

    override fun showRbfSurfaceViewer(points: Array<DoubleArray>) {
        if (points.size < 3) {
            throw IllegalArgumentException("RBF 보간을 위해서는 최소 3개 이상의 포인트가 필요합니다")
        }

        // JavaFX Application Thread에서 실행
        Platform.runLater {
            try {
                val app = BalanceSurfaceApp()
                app.setPoints(points)

                val stage = Stage()
                app.start(stage)

                println("✓ RBF 3D 표면 뷰어 창이 열렸습니다 (${points.size}개 포인트)")
            } catch (e: Exception) {
                System.err.println("✗ RBF 뷰어 실행 실패: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
