ToDoList — Android (Kotlin + Jetpack Compose)
📌 Descrição do projeto

Aplicativo Android de lista de tarefas (To-Do List), desenvolvido em Kotlin com Jetpack Compose. O objetivo da aplicação é permitir que o usuário cadastre, visualize, edite,
marque como concluída e exclua tarefas, com os dados persistidos localmente no dispositivo através do Room.

A aplicação segue uma arquitetura em camadas (UI → ViewModel → Repository → DAO/Banco de dados),
separando responsabilidades e permitindo que a interface reaja automaticamente às mudanças de estado,
sem necessidade de recarregar a tela manualmente.

🛠️ Tecnologias utilizadas
Kotlin — linguagem principal do projeto.
Jetpack Compose — construção de interface declarativa (telas de lista e formulário).
Room — persistência local dos dados em banco SQLite, com acesso via DAO.
Coroutines / Flow — operações assíncronas (inserir, atualizar, deletar) e observação reativa da lista de tarefas (Flow → StateFlow).
ViewModel — retenção de estado e ponte entre a UI e o repositório, sobrevivendo a mudanças de configuração (ex.: rotação de tela).
Navigation Compose — navegação entre as telas de Lista e Formulário dentro de um único NavHost.
🏗️ Arquitetura
MainActivity
     │
     ▼
AppNavigation (NavHost)
     │
     ├── ListaTarefasScreen  ──┐
     │                         │ observam
     └── FormularioTarefaScreen┘
                    │
                    ▼
            TarefaViewModel
                    │
                    ▼
            TarefaRepository
                    │
                    ▼
         TarefaDao ── TarefaDatabase (Room)
         
TarefaRepository:    

É a camada responsável por intermediar o acesso aos dados, isolando o restante do app de detalhes de implementação do Room. Ele expõe:

tarefas: Flow<List<Tarefa>> — o fluxo reativo vindo diretamente do TarefaDao.listarTodas(), repassado sem alterações.
inserir(tarefa), atualizar(tarefa) e deletar(tarefa) — funções suspend que apenas delegam a chamada ao DAO.

Sua responsabilidade é puramente de abstração da fonte de dados: se no futuro a origem dos dados mudasse (por exemplo, para uma API remota), apenas o Repository precisaria ser alterado — nem o ViewModel nem a UI seriam impactados.

TarefaViewModel:

É a camada que guarda e gerencia o estado da UI, seguindo o padrão MVVM. Principais responsabilidades:

Converte o Flow<List<Tarefa>> do repositório em um StateFlow<List<Tarefa>> através de stateIn, usando SharingStarted.WhileSubscribed(5_000) — ou seja, o fluxo continua ativo por 5 segundos após a UI parar de observar, evitando reprocessamento desnecessário em pequenas mudanças de tela (como rotação).
Expõe as ações inserir, atualizar e deletar, cada uma disparando uma coroutine no viewModelScope (o que garante que a chamada seja cancelada automaticamente se o ViewModel for destruído).
Fornece uma factory (companion object) que monta manualmente o TarefaRepository a partir do TarefaDao, já que o projeto não utiliza um framework de injeção de dependência (como Hilt) — a criação é feita manualmente via ViewModelProvider.Factory.

Em resumo: o ViewModel não sabe como os dados são obtidos, apenas consome o Repository e disponibiliza estado e ações para a UI.

Como ListaTarefasScreen observa o estado e dispara ações
A tela coleta o estado com viewModel.tarefas.collectAsStateWithLifecycle(), o que garante que a lista de tarefas (List<Tarefa>) seja recomposta automaticamente sempre que o StateFlow do ViewModel emitir uma nova lista — e que a coleta respeite o ciclo de vida da tela (pausando quando ela não está visível).
A lista é renderizada em uma LazyColumn, exibindo cada tarefa em um TarefaItem (Card com Checkbox, título, descrição e botão de excluir).
Ações do usuário disparam eventos que não alteram estado diretamente na tela — eles chamam funções do ViewModel:
Marcar/desmarcar o Checkbox → viewModel.atualizar(tarefa.copy(concluida = ...)).
Clicar no ícone de lixeira → viewModel.deletar(tarefa).
Clicar no Card (ou no botão de "+") → não altera dados, apenas navega (onEditarTarefa / onNovaTarefa), repassados pela AppNavigation.
A tela em si é "burra": todo o conteúdo visual está isolado em ListaTarefasContent, um Composable que recebe apenas dados e callbacks (facilita reuso em @Preview sem depender do ViewModel).
Como FormularioTarefaScreen diferencia cadastro e edição
A tela recebe um tarefaId: Int vindo da navegação.
Se tarefaId == 0, é tratado como nova tarefa (isEdicao = false), e o formulário abre vazio.
Se tarefaId != 0, a tela busca a tarefa correspondente na lista observada do ViewModel (tarefas.find { it.id == tarefaId }) e usa seus dados (titulo, descricao) para pré-preencher os campos — configurando isEdicao = true.
Ao salvar (onSalvar), a mesma lógica de ID decide a ação:
tarefaId == 0 → viewModel.inserir(Tarefa(...)) (cria uma nova tarefa).
tarefaId != 0 → viewModel.atualizar(tarefaExistente.copy(...)) (atualiza a tarefa existente, preservando o id e os demais campos).
Após salvar, a navegação retorna automaticamente para a lista (onVoltar()).
O título da TopAppBar também muda dinamicamente ("Nova Tarefa" ou "Editar Tarefa") conforme isEdicao.
Rotas configuradas em AppNavigation e passagem do ID da tarefa

O NavHost define duas rotas, com "lista" como tela inicial:

Rota	Tela	Observação
"lista"	ListaTarefasScreen	Tela inicial (startDestination)
"formulario/{tarefaId}"	FormularioTarefaScreen	Recebe o ID da tarefa como argumento de rota
Para nova tarefa, a navegação chama navController.navigate("formulario/0"), ou seja, o próprio ID 0 funciona como sinalizador de "criação".
Para editar, a lista chama navController.navigate("formulario/$id"), passando o ID real da tarefa clicada.
Dentro da rota do formulário, o argumento é extraído com backStackEntry.arguments?.getString("tarefaId")?.toInt() ?: 0 e repassado como parâmetro tarefaId para a FormularioTarefaScreen.
O botão de voltar do formulário chama navController.popBackStack(), retornando para a tela anterior (a lista) sem empilhar uma nova instância.
Como a MainActivity cria a ViewModel e inicia a navegação
No onCreate, a Activity chama setContent { } para definir a UI em Compose, envolvendo tudo no TodolistTheme.
A TarefaViewModel é obtida via viewModel(factory = TarefaViewModel.factory(applicationContext)) — usando a factory definida no próprio ViewModel, que monta o TarefaDatabase, extrai o TarefaDao e injeta no TarefaRepository.
Essa mesma instância de TarefaViewModel é passada para AppNavigation(viewModel = viewModel), garantindo que tanto a tela de lista quanto a de formulário compartilhem o mesmo ViewModel — por isso o formulário consegue acessar a lista de tarefas para localizar a tarefa em edição.

▶️ Como executar o projeto
Clone ou baixe o repositório e extraia o .zip, se necessário.
Abra o Android Studio e selecione Open, apontando para a pasta raiz do projeto (a que contém build.gradle.kts e settings.gradle.kts).
Aguarde o Gradle Sync finalizar (pode baixar dependências automaticamente na primeira vez).
Certifique-se de ter um emulador Android configurado (ou um dispositivo físico conectado via USB com depuração habilitada).
Clique em Run ▶ (ou Shift + F10) para compilar e instalar o app.
Requisitos mínimos
Android Studio atualizado.
JDK 21.
minSdk 24 / targetSdk 36.

🖼️ Evidências
•	Tela inicial com a lista de tarefas em execução.
•	Cadastro de uma nova tarefa.
•	Tarefa cadastrada aparecendo na lista.
•	Edição de uma tarefa existente.
•	Tarefa marcada como concluída.
•	Exclusão de uma tarefa.
•	Navegação entre a lista e o formulário.
•	Build ou execução do projeto sem erros.
<img width="498" height="389" alt="Screenshot 2026-05-11 150727" src="https://github.com/user-attachments/assets/a6af168c-229f-4df1-9048-4f415aa9131c" />
<img width="1105" height="472" alt="Screenshot 2026-05-11 164021" src="https://github.com/user-attachments/assets/71691c85-15b4-4343-b00f-60df46daa0b8" />
<img width="575" height="40" alt="Screenshot 2026-05-12 151236" src="https://github.com/user-attachments/assets/4db12da3-495c-45fb-a45b-3fecab3c82ca" />
<img width="1494" height="48" alt="Screenshot 2026-05-19 184027" src="https://github.com/user-attachments/assets/b5d9d04e-d548-41e6-98f1-12dc5071815e" />
<img width="613" height="555" alt="Screenshot 2026-05-25 153221" src="https://github.com/user-attachments/assets/42174b91-deae-460a-b494-5e863191046e" />
<img width="318" height="692" alt="Screenshot 2026-08-26 093319" src="https://github.com/user-attachments/assets/54c5315a-a79a-4ea4-bb5a-797f09f6c204" />
<img width="318" height="694" alt="Screenshot 2026-08-26 093337" src="https://github.com/user-attachments/assets/70bcc658-b170-45a1-89c6-11dfbc44dfa7" />
<img width="315" height="693" alt="Screenshot 2026-08-26 093407" src="https://github.com/user-attachments/assets/23623bf5-c682-42d2-abed-5454cb170a63" />
<img width="308" height="694" alt="Screenshot 2026-08-26 093420" src="https://github.com/user-attachments/assets/33feb053-f17b-43b4-a4dd-4837809ce004" />
<img width="323" height="688" alt="Screenshot 2026-08-26 093437" src="https://github.com/user-attachments/assets/8ca78cc3-812d-48fd-b0a0-dde4b4ba47f0" />
<img width="311" height="690" alt="Screenshot 2026-08-26 093450" src="https://github.com/user-attachments/assets/25627266-dea3-4b28-8026-b16306e61d44" />
<img width="313" height="689" alt="Screenshot 2026-08-26 093716" src="https://github.com/user-attachments/assets/67cfd44f-3914-47a2-a6ef-55c0174d4aa3" />
<img width="442" height="139" alt="Screenshot 2026-08-26 093736" src="https://github.com/user-attachments/assets/1346e1d7-cff8-43ed-bed9-888c4a90de35" />

