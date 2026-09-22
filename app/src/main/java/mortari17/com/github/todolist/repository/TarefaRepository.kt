package mortari17.com.github.todolist.repository

import mortari17.com.github.todolist.data.Tarefa
import mortari17.com.github.todolist.data.TarefaDao
import kotlinx.coroutines.flow.Flow

class TarefaRepository(private val dao: TarefaDao) {

    val tarefas: Flow<List<Tarefa>> = dao.listarTodas()

    suspend fun inserir(tarefa: Tarefa) = dao.inserir(tarefa)

    suspend fun atualizar(tarefa: Tarefa) = dao.atualizar(tarefa)

    suspend fun deletar(tarefa: Tarefa) = dao.deletar(tarefa)
}