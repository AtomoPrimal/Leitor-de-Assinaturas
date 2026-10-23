package com.unirv.server

import com.unirv.server.models.InferencePayload
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Collections

fun main() {
    embeddedServer(Netty, port = 8081, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    // Persistência em memória e arquivo JSON estruturado (RF7)
    val storageFile = File("server_inferences_db.json")
    val jsonSerializer = Json { prettyPrint = true }
    val database = Collections.synchronizedList(mutableListOf<InferencePayload>())

    // Carrega dados salvos anteriormente se o arquivo existir
    if (storageFile.exists()) {
        try {
            val content = storageFile.readText()
            val savedList = jsonSerializer.decodeFromString<List<InferencePayload>>(content)
            database.addAll(savedList)
            println("✓ Carregadas ${savedList.size} inferências do banco JSON local.")
        } catch (e: Exception) {
            println("Aviso ao carregar banco JSON: ${e.message}")
        }
    }

    fun persistDatabase() {
        try {
            val jsonContent = jsonSerializer.encodeToString(database.toList())
            storageFile.writeText(jsonContent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    routing {
        get("/") {
            call.respondText(
                "API do Leitor de Assinaturas UniRV - Online na porta 8081!\n" +
                "Rotas disponíveis:\n" +
                "POST /api/inferencia\n" +
                "GET /api/inferencia/historico\n" +
                "GET /api/inferencia/{id}/caixas"
            )
        }

        // RF6 & RF7: Salva nova inferência
        post("/api/inferencia") {
            try {
                val payload = call.receive<InferencePayload>()
                // Remove duplicatas se houver
                database.removeIf { it.sessionId == payload.sessionId }
                database.add(0, payload)
                persistDatabase()

                println("→ [POST /api/inferencia] Recebida sessão: ${payload.sessionId} com ${payload.totalObjects} caixas.")
                call.respond(HttpStatusCode.Created, mapOf("status" to "success", "id_sessao" to payload.sessionId))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Erro ao processar JSON")))
            }
        }

        // RF7 & RF8: Retorna lista de histórico
        get("/api/inferencia/historico") {
            val history = database.toList()
            println("[GET /api/inferencia/historico] Returned ${history.size} sessions.")
            call.respond(history)
        }

        // RF7 & RF8: Retorna caixas de uma sessão específica
        get("/api/inferencia/{id}/caixas") {
            val sessionId = call.parameters["id"]
            val found = database.find { it.sessionId == sessionId }
            if (found != null) {
                println("[GET /api/inferencia/$sessionId/caixas] Returned ${found.boundingBoxes.size} boxes.")
                call.respond(found.boundingBoxes)
            } else {
                println("[GET /api/inferencia/$sessionId/caixas] Session not found.")
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Sessão não encontrada: $sessionId"))
            }
        }

        // Salva evidência visual (foto do documento) para histórico e base de retreinamento
        post("/api/inferencia/{id}/imagem") {
            val sessionId = call.parameters["id"] ?: "temp"
            try {
                val uploadsDir = File("uploads").apply { mkdirs() }
                val destFile = File(uploadsDir, "$sessionId.jpg")
                val bytes = call.receive<ByteArray>()
                destFile.writeBytes(bytes)
                println("✓ [POST /api/inferencia/$sessionId/imagem] Foto salva com sucesso em uploads/$sessionId.jpg (${bytes.size / 1024} KB).")
                call.respond(HttpStatusCode.Created, mapOf("status" to "image_saved", "path" to destFile.name))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Erro ao salvar imagem")))
            }
        }
    }
}
