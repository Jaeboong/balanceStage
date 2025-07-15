package com.example.BalanceStage

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.application.Platform
import javafx.scene.Scene
import javafx.stage.Stage
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import kotlin.system.exitProcess

@SpringBootApplication
class BalanceStageApplication : Application() {

	companion object {
		lateinit var applicationContext: ConfigurableApplicationContext
		
		@JvmStatic
		fun main(args: Array<String>) {
			// Start Spring Boot context first
			applicationContext = runApplication<BalanceStageApplication>(*args)
			
			// Then launch JavaFX
			Application.launch(BalanceStageApplication::class.java, *args)
		}
	}

	override fun start(primaryStage: Stage) {
		try {
			val fxmlLoader = FXMLLoader(javaClass.getResource("/balanceMasterUI_v240.fxml"))
			
			// Set controller factory to use Spring beans
			fxmlLoader.setControllerFactory { clazz ->
				applicationContext.getBean(clazz)
			}
			
			val scene = Scene(fxmlLoader.load(), 1330.0, 750.0)
			primaryStage.title = "Balance Master v2.4.0 - Spring Boot Edition"
			primaryStage.scene = scene
			primaryStage.show()
			
			// Handle window close event
			primaryStage.setOnCloseRequest {
				Platform.exit()
				applicationContext.close()
				exitProcess(0)
			}
			
		} catch (e: Exception) {
			e.printStackTrace()
			Platform.exit()
			applicationContext.close()
			exitProcess(1)
		}
	}
}
