package com.example.BalanceStage.service

import javafx.scene.shape.MeshView
import javafx.scene.shape.TriangleMesh

/**
 * 3D 메쉬 생성 팩토리 인터페이스
 * SRP: 3D 메쉬 객체 생성만 담당
 */
interface MeshFactory {
    /**
     * 평면 메쉬 생성
     * @param width 너비
     * @param height 높이
     * @return TriangleMesh
     */
    fun createPlaneMesh(width: Double, height: Double): TriangleMesh

    /**
     * 평면 메쉬뷰 생성 (반투명, 회색)
     * @param width 너비
     * @param height 높이
     * @param normalVector 평면의 법선 벡터 [nx, ny, nz]
     * @return MeshView
     */
    fun createPlaneMeshView(
        width: Double,
        height: Double,
        normalVector: DoubleArray
    ): MeshView
}
