package com.example.pennykeeper.utils.ai

import com.example.pennykeeper.BuildConfig
import com.example.pennykeeper.data.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale

class OpenRouterInferenceSvc(private val categoryRepository: CategoryRepository) {
    private val client = OkHttpClient()
    private val apiKey = BuildConfig.OPENROUTER_API_KEY
    private val baseUrl = "https://openrouter.ai/api/v1/chat/completions"
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())

    suspend fun getResponse(question: String, context: String): String = withContext(Dispatchers.IO) {
        try {
            makeRequest(question, context)
        } catch (e: IOException) {
            println("Failed to get response: ${e.message}")
            generateGenericResponse(context)
        }
    }

    private suspend fun makeRequest(question: String, context: String): String {
        val isGreeting = isGreeting(question)
        val isAnalysisQuery = !isGreeting && shouldProvideDetailedAnalysis(question, context)

        val systemPrompt = """You are a helpful financial assistant. Your role is to:
            1. Analyze financial data and provide insights
            2. Give specific, actionable advice
            3. Be concise but ensure complete sentences
            4. Use factual data from the provided context
            5. Format numbers as currency when discussing money
            6. For greetings, keep response under 2 sentences
            7. For analysis, limit to 3-4 key points
            8. Always complete your thoughts and sentences
            """.trimIndent()

        val userPrompt = when {
            isGreeting -> """Respond with exactly two sentences:
                1. A friendly greeting
                2. Ask how you can help with their finances
                Example: "Hello! How can I help you with your finances today?"
                """.trimIndent()
            isAnalysisQuery -> buildString {
                append("Financial Data:\n")
                append(context)
                append("\n\nUser Question: ")
                append(question)
                append("\n\nProvide a detailed analysis including:")
                append("\n1. Direct answer to the question")
                append("\n2. Key spending patterns")
                append("\n3. Notable insights")
                append("\n4. Brief recommendations")
            }
            else -> buildString {
                append("Context: ")
                append(context)
                append("\n\nQuestion: ")
                append(question)
                append("\n\nProvide a brief, friendly response focused on the specific question.")
            }
        }

        val jsonBody = JSONObject().apply {
            put("model", "meta-llama/llama-3.2-1b-instruct:free")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
            put("temperature", when {
                isGreeting -> 0.7
                isAnalysisQuery -> 0.3
                else -> 0.5
            })
            put("max_tokens", when {
                isGreeting -> 150
                isAnalysisQuery -> 1000
                else -> 250
            })
        }

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://pennykeeper.app")
            .addHeader("X-Title", "PennyKeeper")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response")

        return try {
            val jsonResponse = JSONObject(responseBody)

            val content = when {
                jsonResponse.has("candidates") -> {
                    jsonResponse
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getString("content")
                        .trim()
                }
                jsonResponse.has("choices") -> {
                    jsonResponse
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim()
                }
                else -> {
                    throw IOException("Unexpected response format")
                }
            }

            if (content.isBlank()) {
                throw IOException("Empty response content")
            }

            if (!isAnalysisQuery) {
                content
            } else {
                val expenses = parseFinancialData(context)
                formatAnalysisResponse(content, expenses)
            }
        } catch (e: Exception) {
            throw IOException("Failed to parse response: ${e.message}\nResponse body: $responseBody")
        }
    }


    private fun formatAnalysisResponse(aiResponse: String, expenses: Map<String, Double>): String {
        val total = expenses.values.sum()

        return buildString {
            append("💰 Financial Analysis\n")
            append("─".repeat(20))
            append("\n\n")

            // show the AI's response
            append(aiResponse)
            append("\n\n")

            // add the structured data
            append("📊 Quick Stats:\n")
            append("• Total: ${currencyFormatter.format(total)}\n")
            append("• Transactions: ${expenses.size}\n")

            if (expenses.isNotEmpty()) {
                val avgSpending = total / expenses.size
                append("• Average: ${currencyFormatter.format(avgSpending)}\n")

                // Show top categories by spending
                append("\nTop Categories:\n")
                expenses.entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .forEach { (category, amount) ->
                        val percentage = (amount / total * 100).toInt()
                        append("• ${category.capitalize()}: ${currencyFormatter.format(amount)} ($percentage%)\n")
                    }
            }
        }
    }

    private suspend fun parseFinancialData(context: String): Map<String, Double> {
        val expenses = mutableMapOf<String, Double>()

        try {
            // Get all categories from the database using coroutines
            val dbCategories = withContext(Dispatchers.IO) {
                categoryRepository.categories.first().map { it.name.lowercase() }
            }

            // Split into lines and clean up
            val lines = context.lowercase()
                .lines()
                .filter { it.isNotEmpty() }
                // Ignore summary lines
                .filter { !it.contains("total spending") && !it.contains("average") }

            lines.forEach { line ->
                try {
                    // Look for categories from database
                    val category = dbCategories.find { category ->
                        line.contains(category, ignoreCase = true)
                    }

                    // Find dollar sign using regex
                    val dollarAmount = "\\$\\s*([0-9]+(?:\\.[0-9]{2})?)"
                        .toRegex()
                        .find(line)
                        ?.groupValues
                        ?.get(1)
                        ?.toDoubleOrNull()

                    if (dollarAmount != null) {
                        val expenseCategory = category ?: withContext(Dispatchers.IO) {
                            // If no category matched, get the default category
                            categoryRepository.getDefaultCategory()?.name?.lowercase() ?: "other"
                        }

                        println("Found valid expense: $expenseCategory = $dollarAmount") // Debug log

                        // Add to existing amount if category already exists
                        expenses[expenseCategory] = expenses.getOrDefault(expenseCategory, 0.0) + dollarAmount
                    }
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return expenses
    }

    private suspend fun generateGenericResponse(context: String): String {
        val expenses = parseFinancialData(context)
        val total = expenses.values.sum()

        return buildString {
            appendLine("📊 Basic Analysis")
            appendLine("────────────────")
            appendLine("• Total spending: ${currencyFormatter.format(total)}")
            appendLine("• Number of categories: ${expenses.size}")
            if (expenses.isNotEmpty()) {
                val avgSpending = total / expenses.size
                appendLine("• Average transaction: ${currencyFormatter.format(avgSpending)}")
                val highestEntry = expenses.maxByOrNull { it.value }
                highestEntry?.let {
                    appendLine("• Largest expense: ${currencyFormatter.format(it.value)} (${it.key})")
                }

                appendLine("\nCategory Breakdown:")
                expenses.forEach { (category, amount) ->
                    val percentage = (amount / total * 100).toInt()
                    appendLine("• ${category.capitalize()}: ${currencyFormatter.format(amount)} ($percentage%)")
                }
            }
        }
    }

    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private fun shouldProvideDetailedAnalysis(question: String, context: String): Boolean {
        val analysisKeywords = setOf(
            "analyze", "analysis", "pattern", "spend", "expense", "cost",
            "budget", "money", "transaction", "trend", "breakdown", "report",
            "summary", "total", "how much", "where", "what", "when", "break down"
        )

        val greetings = setOf(
            "hi", "hello", "hey", "test", "help", "thanks", "thank you"
        )

        val questionLower = question.lowercase().trim()

        if (greetings.any { questionLower.contains(it) }) {
            return false
        }

        val containsAnalysisKeyword = analysisKeywords.any { keyword ->
            analysisKeywords.any { word ->
                levenshteinDistance(questionLower, word) <= 2
            }
        }

        val hasFinancialData = context.contains("$") ||
                context.contains("dollar") ||
                context.contains("spent")

        return hasFinancialData && containsAnalysisKeyword
    }

    /**
     * Handles typo using levenshtein distance -
     * adapted code from https://gist.github.com/ademar111190/34d3de41308389a0d0d8
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1) { it }
        var lastValue = 0

        for (i in s1.indices) {
            costs[0] = i + 1
            lastValue = i

            for (j in s2.indices) {
                val newValue = minOf(
                    costs[j + 1] + 1,
                    costs[j] + 1,
                    lastValue + if (s1[i] == s2[j]) 0 else 1
                )
                costs[j] = lastValue
                lastValue = newValue
            }
            costs[s2.length] = lastValue
        }
        return costs[s2.length]
    }

    private fun isGreeting(question: String): Boolean {
        val greetings = setOf(
            "hi", "hello", "hey", "test", "help", "thanks", "thank you",
            "good morning", "good afternoon", "good evening"
        )
        val questionLower = question.lowercase().trim().replace(Regex("[!.,?]"), "")
        return greetings.any { greeting ->
            questionLower == greeting ||
                    questionLower.startsWith("$greeting ") ||
                    questionLower.endsWith(" $greeting")
        }
    }
}