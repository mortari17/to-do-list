package mortari17.com.github.todolist.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mortari17.com.github.todolist.ui.theme.FormularioTarefaScreen
import mortari17.com.github.todolist.ui.theme.ListaTarefasScreen
import mortari17.com.github.todolist.viewmodel.TarefaViewModel

@Composable
fun AppNavigation(viewModel: TarefaViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "lista") {
        composable("lista") {
            ListaTarefasScreen(
                viewModel = viewModel,
                onNovaTarefa = { navController.navigate("formulario/0") },
                onEditarTarefa = { id -> navController.navigate("formulario/$id") }
            )
        }
        composable("formulario/{tarefaId}") { backStackEntry ->
            val tarefaId = backStackEntry.arguments?.getString("tarefaId")?.toInt() ?: 0
            FormularioTarefaScreen(
                viewModel = viewModel,
                tarefaId = tarefaId,
                onVoltar = { navController.popBackStack() }
            )
        }
    }
}