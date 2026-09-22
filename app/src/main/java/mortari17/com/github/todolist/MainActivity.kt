package mortari17.com.github.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import mortari17.com.github.todolist.navigation.AppNavigation
import mortari17.com.github.todolist.ui.theme.TodolistTheme
import mortari17.com.github.todolist.viewmodel.TarefaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodolistTheme {
                val viewModel: TarefaViewModel = viewModel(
                    factory = TarefaViewModel.factory(applicationContext)
                )
                AppNavigation(viewModel = viewModel)
            }
        }
    }
}
