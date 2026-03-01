package com.ma.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.ma.app.data.database.AppDatabase
import com.ma.app.data.repository.NodeRepository
import com.ma.app.ui.navigation.MaNavigation
import com.ma.app.ui.theme.MaTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: NodeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar repositorio
        val database = AppDatabase.getDatabase(applicationContext)
        repository = NodeRepository(database.nodeDao())

        setContent {
            MaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    MaNavigation(
                        navController = navController,
                        repository = repository
                    )
                }
            }
        }
    }
}
