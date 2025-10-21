package com.example.BalanceStage.service

/**
 * RBF 보간 기반 3D 표면 시각화 서비스 인터페이스
 * SRP: RBF 보간 기반 3D 서페이스 뷰어 창 띄우기만 담당
 */
interface RbfSurfaceVisualizationService {
    /**
     * RBF 보간을 사용한 3D 표면 뷰어를 별도 창으로 표시
     * @param points 포인트 리스트 [(x, y, z, deviation), ...]
     * @throws IllegalArgumentException 포인트가 3개 미만일 경우
     */
    fun showRbfSurfaceViewer(points: Array<DoubleArray>)
}
