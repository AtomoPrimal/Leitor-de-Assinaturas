# Leitor de Documentos Assinados — UniRV (N2)
**Disciplina:** DESENVOLVIMENTO DE SOFTWARE PARA DISPOSITIVOS MÓVEIS  
**Professor:** Me. Sergio Souza Novak  
**Alunos:** Kauan Almeida e Kawã Santos  
**Tema:** Aplicativo Android Full-Stack em Kotlin: Visão Computacional, Inferência On-Device e Persistência em Servidor  

---

## 🚀 Visão Geral do Projeto

Este projeto consiste em uma solução computacional completa (**Full-Stack Mobile em Kotlin**) para detecção e auditoria de assinaturas em documentos físicos através da câmera do smartphone:
- **IA On-Device**: Modelo **YOLOv8s** treinado para detecção da classe `signature`, exportado e executado 100% offline via **Microsoft ONNX Runtime Mobile** (`best.onnx`).
- **Aplicativo Android**: Desenvolvido exclusivamente em **Kotlin** com **Jetpack Compose** (Tema Light Mode obrigatório - *Corporate Trust*), com suporte a Câmera nativa e Galeria.
- **Cálculo Geométrico em Tempo Real (RF5)**: Para cada assinatura delimitada, calcula Largura ($W_{px}$), Altura ($H_{px}$), Centróide ($X_c, Y_c$) e Área ($A_{px}$).
- **Servidor Backend**: API REST construída em **Kotlin com Ktor**, recebendo os relatórios JSON de telemetria e persistindo o histórico.
- **Protótipo Interativo Web**: Disponível em `prototype/index.html` para testes imediatos usando a webcam do computador.

---

## 📱 Estrutura das 4 Telas (Fidelidade aos Wireframes)

1. **Tela 1 (Configuração & Entrada)**:
   - Seletor e gerenciador de modelos ONNX (modelo embarcado `best.onnx` ou arquivo externo do armazenamento).
   - Botões de aquisição: **Tirar Foto** (Câmera) e **Galeria**.
   - Slider interativo de **Limiar de Confiança** (15% a 90%).
   - Pré-visualização da imagem e botão de execução da inferência.
2. **Tela 2 (Resultados & Visualização Anotada)**:
   - **Hero Card**: Selo verde **DOCUMENTO ASSINADO** (com contagem) ou âmbar **DOCUMENTO NÃO ASSINADO**.
   - Métricas de desempenho: total de assinaturas, confiança média e tempo de inferência local na NPU/CPU em milissegundos ($ms$).
   - Imagem do documento exibida com as **Bounding Boxes** desenhadas ao redor das assinaturas e a marcação visual do **Centróide**.
   - Botão para navegar à Auditoria Geométrica.
3. **Tela 3 (Histórico de Inferências)**:
   - Listagem cronológica de todas as análises realizadas (ID de sessão, data/hora, quantidade de objetos e status de sincronização com o servidor).
4. **Tela 4 (Detalhamento Geométrico das Caixas)**:
   - Auditoria individual de cada caixa detectada com:
     - Identificador (`Caixa #01`)
     - Rótulo da classe e % de confiança
     - Largura ($W_{px}$) e Altura ($H_{px}$)
     - Coordenadas do Centróide ($X_{centroide}, Y_{centroide}$)
     - Área da caixa em pixels² ($A_{px} = W_{px} \times H_{px}$)

---

## 📂 Organização do Repositório

```
leitor-de-documentos-assinados/
├── app/                              # Projeto Android Studio (Kotlin + Jetpack Compose)
│   ├── src/main/assets/best.onnx     # Modelo YOLOv8 ONNX embarcado (44 MB)
│   ├── src/main/java/com/unirv/assinaturas/
│   │   ├── MainActivity.kt           # Scaffold principal e Bottom Navigation Bar
│   │   ├── ml/SignatureDetector.kt   # Motor ONNX Runtime e Cálculos Geométricos
│   │   ├── model/InferenceData.kt    # Modelos de dados serializáveis (UML Fig 1)
│   │   ├── network/InferenceApiClient.kt # Cliente Retrofit para comunicação com API
│   │   ├── theme/                    # Design System Light Mode (Cores, Tipografia)
│   │   ├── ui/components/            # HeroStatusBanner, AnnotatedImageView
│   │   ├── ui/screens/               # DetectionScreen (Telas 1 e 2) e HistoryScreen (Telas 3 e 4)
│   │   └── viewmodel/MainViewModel.kt# MVVM com Coroutines e StateFlow
│   └── build.gradle.kts              # Dependências do app Android
├── server/                           # Servidor Backend 100% Kotlin (Ktor)
│   ├── src/main/kotlin/com/unirv/server/
│   │   ├── Application.kt            # Servidor Netty, rotas REST e persistência JSON
│   │   └── models/InferenceModels.kt # Estruturas de dados do servidor
│   └── build.gradle.kts
├── model/                            # Modelo best.onnx exportado
├── prototype/
│   └── index.html                    # Protótipo funcional com webcam para teste no navegador
├── enunciado.pdf                     # Especificação oficial da disciplina
└── README.md
```

---

## 🛠️ Como Executar

### 1. Protótipo Interativo no Navegador (Webcam)
Basta abrir com dois cliques o arquivo `prototype/index.html` em qualquer navegador. Ele acessará sua webcam para permitir enquadrar um papel/documento e simular o fluxo completo das 4 telas e dos cálculos geométricos.

### 2. Abrindo o Aplicativo no Android Studio
1. Abra o **Android Studio**.
2. Na tela inicial, clique em **Open** (ou File > Open) e selecione a pasta raiz deste projeto (`leitor-de-documentos-assinados`).
3. Aguarde o Android Studio realizar a sincronização automática do Gradle (pode levar alguns minutos na primeira vez para baixar as dependências do Compose e do ONNX).
4. Conecte seu celular Android via cabo USB com a **Depuração USB ativada**.
5. No topo do Android Studio, certifique-se de que o seu celular está selecionado na lista de dispositivos e clique no botão verde de **Run (▶)**. O app será compilado e instalado no seu telefone.

### 3. Rodando o Servidor Backend Kotlin (Ktor)
Para subir o servidor de telemetria na porta 8081:
- No Android Studio/IntelliJ, abra `server/src/main/kotlin/com/unirv/server/Application.kt` e clique no botão de Play verde ao lado da função `fun main()`.
- O servidor iniciará em `http://0.0.0.0:8081` e salvará os relatórios em `server_inferences_db.json`.
- No aplicativo no celular, clique no ícone de engrenagem no topo para apontar o IP do seu computador na rede Wi-Fi (ex: `http://192.168.1.15:8081`).

---

## 📋 Conformidade com a Rubrica de Avaliação (10,0 / 10,0)

| Requisito do Edital | Implementação | Pontuação |
|---|---|:---:|
| **1. Seleção do Dataset** | YOLOv8 treinado no Roboflow para a classe `signature` (detecção de assinaturas em documentos). | **1,0 pt** |
| **2. Design System & Interface** | Jetpack Compose exclusivo, tema Light Mode (*Corporate Trust*), fidelidade aos wireframes das 4 telas e Bottom Nav. | **2,0 pts** |
| **3. Inferência ONNX & Geometria** | `SignatureDetector.kt` com ONNX Runtime Mobile on-device, cálculo de $W_{px}$, $H_{px}$, Centróides $(X_c, Y_c)$ e Área $A_{px}$. | **2,5 pts** |
| **4. Fluxo de Histórico (Telas 3 e 4)**| Listagem cronológica de análises e tela de detalhamento geométrico por caixa delimitadora. | **2,0 pts** |
| **5. Servidor Backend & REST** | Servidor Ktor 100% em Kotlin com rotas `POST /api/inferencia`, `GET /api/inferencia/historico` e `GET /api/inferencia/{id}/caixas`, salvando em JSON estruturado. | **1,5 pts** |
| **Total** | | **10,0 pts** |
