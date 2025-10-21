package com.example.BalanceStage.util

import com.example.BalanceStage.controller.PointData
import javafx.animation.AnimationTimer
import kotlin.math.abs
import kotlin.random.Random

/**
 * 실시간 밸런스 시뮬레이터
 * 점들이 평균 평면으로 점진적으로 수렴하는 과정을 시각화
 */
class BalanceSimulator(
    private val points: MutableList<PointData>,
    private val updateCallback: () -> Unit,
    private val statusCallback: (String) -> Unit
) {
    private var animationTimer: AnimationTimer? = null
    private var targetZ: Double = 0.0
    private var iteration = 0
    private var lastPhysicsUpdate = 0L  // 물리 업데이트 시간
    private var lastRenderUpdate = 0L   // 렌더 업데이트 시간
    private val random = Random(System.currentTimeMillis())

    companion object {
        private const val RENDER_INTERVAL_NS = 16_666_666L    // 렌더링: 60fps
        private const val PHYSICS_INTERVAL_NS = 100_000_000L  // 물리: 100ms (10fps)
        private const val CONVERGENCE_RATE = 0.015            // 1.5% 수렴 (느리게)
        private const val NOISE_AMPLITUDE = 0.008             // 노이즈 감소
        private const val COMPLETION_THRESHOLD = 0.005        // 정밀하게
        private const val MAX_ITERATIONS = 300                // 충분한 시간
    }

    /**
     * 시뮬레이션 시작
     * 현재 점들의 평균 Z를 목표 평면으로 설정
     */
    fun start() {
        if (points.isEmpty()) {
            statusCallback("❌ 시뮬레이션: 포인트가 없습니다")
            return
        }

        // 목표 평면 = 현재 점들의 평균 Z
        targetZ = points.map { it.z }.average()
        iteration = 0
        lastPhysicsUpdate = 0L
        lastRenderUpdate = 0L

        statusCallback(String.format("⚙ 밸런스 시뮬레이션 시작 (목표 Z=%.3f mm)", targetZ))

        animationTimer = object : AnimationTimer() {
            override fun handle(now: Long) {
                // 물리: 100ms마다 Z값 변경 (느린 수렴)
                if (lastPhysicsUpdate == 0L || now - lastPhysicsUpdate >= PHYSICS_INTERVAL_NS) {
                    lastPhysicsUpdate = now
                    applyBalanceStep()  // Z값만 변경
                    checkCompletion()
                }

                // 렌더링: 16ms마다 (PCA 재계산 + 등고선 렌더링)
                if (lastRenderUpdate == 0L || now - lastRenderUpdate >= RENDER_INTERVAL_NS) {
                    lastRenderUpdate = now
                    updateCallback()  // PCA 재계산 + 렌더링
                }
            }
        }
        animationTimer?.start()
    }

    /**
     * 한 스텝의 밸런스 조정 적용
     * - Ease-out: 처음엔 빠르고 점점 느려지는 자연스러운 수렴
     * - 랜덤 노이즈 추가 (실제 기계 시뮬레이션)
     */
    private fun applyBalanceStep() {
        // 진행률 계산 (0.0 ~ 1.0)
        val progress = iteration.toDouble() / MAX_ITERATIONS

        // Ease-out: 처음엔 빠르고 점점 느려짐
        val dynamicRate = CONVERGENCE_RATE * (1.0 - progress * 0.7)

        points.forEach { p ->
            val diff = targetZ - p.z
            val step = diff * dynamicRate + (random.nextDouble() - 0.5) * NOISE_AMPLITUDE
            p.z += step
        }
        iteration++

        // 현재 잔류 편차 계산 및 상태 출력
        val avgDeviation = points.map { abs(it.z - targetZ) }.average()
        statusCallback(String.format("평형 중... 잔류편차: %.4f mm (반복: %d)", avgDeviation, iteration))
    }

    /**
     * 완료 조건 체크
     * - 평균 편차 < 0.01mm
     * - 또는 100회 이상 반복
     */
    private fun checkCompletion() {
        val avgDeviation = points.map { abs(it.z - targetZ) }.average()

        if (avgDeviation < COMPLETION_THRESHOLD) {
            stop()
            statusCallback(String.format("✓ 평형 완료! 최종 편차: %.4f mm (%d회 반복)", avgDeviation, iteration))
        } else if (iteration >= MAX_ITERATIONS) {
            stop()
            statusCallback(String.format("⏸ 시뮬레이션 종료 (최대 반복 도달, 편차: %.4f mm)", avgDeviation))
        }
    }

    /**
     * 시뮬레이션 중지
     */
    fun stop() {
        animationTimer?.stop()
        animationTimer = null
    }

    /**
     * 현재 실행 중인지 확인
     */
    fun isRunning(): Boolean {
        return animationTimer != null
    }
}
