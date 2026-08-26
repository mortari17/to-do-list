# ToDoList — Android (Kotlin + Jetpack Compose)

## Descrição do Projeto

Aplicativo Android de lista de tarefas (To-Do List) que permite ao usuário **cadastrar**, **visualizar**, **editar**, **concluir** e **excluir** tarefas, com os dados persistidos localmente no dispositivo através do Room.

A aplicação segue uma arquitetura em camadas (UI → ViewModel → Repository → DAO/Banco de dados), separando responsabilidades e permitindo que a interface reaja automaticamente às mudanças de estado, sem necessidade de recarregar a tela manualmente.

## Tecnologias Utilizadas

- **Kotlin** — linguagem principal do projeto.
- **Jetpack Compose** — construção de interface declarativa (telas de Lista e Formulário).
- **Room** — persistência local dos dados em banco SQLite, com acesso via DAO.
- **Coroutines / Flow** — operações assíncronas e observação reativa da lista de tarefas (`Flow` → `StateFlow`).
- **ViewModel** — retenção de estado, sobrevivendo a mudanças de configuração (ex.: rotação de tela).
- **Navigation Compose** — navegação entre as telas de Lista e Formulário em um único `NavHost`.

## Arquitetura

```
MainActivity
     |
     v
AppNavigation (NavHost)
     |
     |-- ListaTarefasScreen
     |-- FormularioTarefaScreen
     |
     v
TarefaViewModel
     |
     v
TarefaRepository
     |
     v
TarefaDao -- TarefaDatabase (Room)
```

## Componentes e Responsabilidades

### TarefaRepository

Camada responsável por intermediar o acesso aos dados, isolando o restante do app de detalhes de implementação do Room.

- `tarefas: Flow<List<Tarefa>>` — fluxo reativo vindo diretamente de `TarefaDao.listarTodas()`.
- `inserir()`, `atualizar()`, `deletar()` — funções `suspend` que delegam a chamada ao DAO.

**Responsabilidade única:** abstrair a fonte de dados. Se a origem mudasse (ex.: API remota), apenas o Repository precisaria ser alterado — ViewModel e UI não seriam impactados.

### TarefaViewModel

Camada que guarda e gerencia o estado da UI, seguindo o padrão MVVM.

- Converte o `Flow` do repositório em `StateFlow` via `stateIn`, usando `SharingStarted.WhileSubscribed(5_000)` — o fluxo permanece ativo por 5 segundos após a UI parar de observar, evitando reprocessamento em pequenas mudanças de tela.
- Expõe as ações `inserir`, `atualizar` e `deletar`, cada uma disparando uma coroutine em `viewModelScope` (cancelada automaticamente se o ViewModel for destruído).
- Fornece uma `factory` (companion object) que monta manualmente o `TarefaRepository` a partir do `TarefaDao` — não há framework de injeção de dependência (como Hilt); a criação é manual via `ViewModelProvider.Factory`.

O ViewModel não sabe como os dados são obtidos — apenas consome o Repository e disponibiliza estado e ações para a UI.

### ListaTarefasScreen — observação de estado e ações

- Coleta o estado com `collectAsStateWithLifecycle()`, garantindo recomposição automática a cada nova emissão do `StateFlow`, respeitando o ciclo de vida da tela.
- Renderiza a lista em uma `LazyColumn`, exibindo cada item como um `Card` (Checkbox + título + descrição + botão de excluir).
- Ações do usuário não alteram estado diretamente na tela — chamam funções do ViewModel:
  - Marcar/desmarcar `Checkbox` → `viewModel.atualizar(...)`
  - Ícone de lixeira → `viewModel.deletar(...)`
  - Clique no Card ou no botão "+" → apenas navega, repassado pela `AppNavigation`
- Toda a UI está isolada em `ListaTarefasContent`, um Composable "burro" que recebe apenas dados e callbacks — facilita reuso em `@Preview` sem depender do ViewModel.

### FormularioTarefaScreen — cadastro vs. edição

| Condição | Comportamento |
|---|---|
| `tarefaId == 0` | Nova tarefa — formulário abre vazio (`isEdicao = false`) |
| `tarefaId != 0` | Edição — busca a tarefa na lista observada e pré-preenche os campos (`isEdicao = true`) |

- Ao salvar (`onSalvar`), a mesma lógica de ID decide a ação: `viewModel.inserir(...)` ou `viewModel.atualizar(...)`, preservando o `id` original em edições.
- Após salvar, a navegação retorna automaticamente para a lista.
- O título da `TopAppBar` muda dinamicamente: "Nova Tarefa" ou "Editar Tarefa".

### AppNavigation — rotas e passagem de ID

| Rota | Tela | Observação |
|---|---|---|
| `lista` | `ListaTarefasScreen` | Tela inicial (`startDestination`) |
| `formulario/{tarefaId}` | `FormularioTarefaScreen` | Recebe o ID da tarefa como argumento de rota |

- Nova tarefa → `navController.navigate("formulario/0")` — o ID `0` sinaliza "criação".
- Editar → `navController.navigate("formulario/$id")` — passa o ID real da tarefa.
- O argumento é extraído com `backStackEntry.arguments?.getString("tarefaId")?.toInt() ?: 0`.
- O botão de voltar chama `navController.popBackStack()`, retornando sem empilhar uma nova instância.

### MainActivity — criação da ViewModel e início da navegação

1. No `onCreate`, chama `setContent { }`, envolvendo a UI no `TodolistTheme`.
2. Obtém a `TarefaViewModel` via `viewModel(factory = TarefaViewModel.factory(applicationContext))`, que monta o `TarefaDatabase`, extrai o `TarefaDao` e injeta no `TarefaRepository`.
3. Passa essa mesma instância para `AppNavigation(viewModel = viewModel)` — garantindo que Lista e Formulário compartilhem o mesmo ViewModel (por isso o formulário consegue localizar a tarefa em edição).

## Como Executar o Projeto

1. Clone ou baixe o repositório (extraia o `.zip`, se necessário).
2. Abra o Android Studio, clique em **Open** e selecione a pasta raiz do projeto (a que contém `build.gradle.kts` e `settings.gradle.kts`).
3. Aguarde o Gradle Sync finalizar (as dependências são baixadas automaticamente na primeira vez).
4. Configure um emulador Android ou conecte um dispositivo físico com depuração USB habilitada.
5. Clique em **Run** (ou `Shift + F10`) para compilar e instalar o app.

### Requisitos mínimos

- Android Studio atualizado
- JDK 21
- `minSdk` 24 / `targetSdk` 36

## Evidências
•	Tela inicial com a lista de tarefas em execução.
•	Cadastro de uma nova tarefa.
•	Tarefa cadastrada aparecendo na lista.
•	Edição de uma tarefa existente.
•	Tarefa marcada como concluída.
•	Exclusão de uma tarefa.
•	Navegação entre a lista e o formulário.
•	Build ou execução do projeto sem erros.

<img width="318" height="692" alt="Screenshot 2026-08-26 093319" src="https://github.com/user-attachments/assets/e5f6ee23-8b69-41c1-974b-e673795506ec" />
<img width="318" height="694" alt="Screenshot 2026-08-26 093337" src="https://github.com/user-attachments/assets/1234a9f2-dbe1-4a25-83b6-dbd2ce2b47a4" />
<img width="315" height="693" alt="Screenshot 2026-08-26 093407" src="https://github.com/user-attachments/assets/d70502d9-6966-4802-b523-e3ed7098367e" />
<img width="308" height="694" alt="Screenshot 2026-08-26 093420" src="https://github.com/user-attachments/assets/641b7259-a4b6-4a3d-92e2-50c6fe0d803d" />
<img width="323" height="688" alt="Screenshot 2026-08-26 093437" src="https://github.com/user-attachments/assets/8a51e1fe-2aac-4793-9388-7898c73fbbf1" />
<img width="311" height="690" alt="Screenshot 2026-08-26 093450" src="https://github.com/user-attachments/assets/d604998f-e997-4a95-943c-144fe3c07d6a" />
<img width="313" height="689" alt="Screenshot 2026-08-26 093716" src="https://github.com/user-attachments/assets/81251705-50bb-4b56-aace-38586194efe1" />
<img width="442" height="139" alt="Screenshot 2026-08-26 093736" src="https://github.com/user-attachments/assets/7bb2f561-e5bf-4180-ba21-6caeb8dafd0f" />


