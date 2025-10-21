package com.example.BalanceStage.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import plot.geom.DefaultDistanceCalculator
import plot.geom.DistanceCalculator
import plot.geom.PcaPlaneEstimator
import plot.geom.PlaneEstimator

/**
 * Geometry 관련 빈 설정
 * plot.geom 패키지의 기능들을 Spring 컨텍스트에 등록
 */
@Configuration
class GeometryConfig {

    @Bean
    fun planeEstimator(): PlaneEstimator {
        return PcaPlaneEstimator()
    }

    @Bean
    fun distanceCalculator(): DistanceCalculator {
        return DefaultDistanceCalculator()
    }
}
