package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Property
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class RoomInspectionReport(
    val overallScore: Int,
    val cleanliness: String,
    val lighting: String,
    val ventilation: String,
    val furnitureCondition: String,
    val visibleDamage: String,
    val concerns: List<String>,
    val observations: String,
    val disclaimer: String = "Notice: This AI analysis provides automated visual guidance only based on observable image pixels. It is not an engineering structural inspection or certified property audit."
)

data class LeaseAnalysisReport(
    val rent: String,
    val securityDeposit: String,
    val noticePeriod: String,
    val lockInPeriod: String,
    val maintenanceAndHiddenCharges: String,
    val restrictionsAndCurfew: String,
    val refundConditions: String,
    val keyClauses: List<String>,
    val cautionaryTerms: List<String>,
    val summary: String,
    val disclaimer: String = "Legal Disclaimer: This automated summary is for informational guidance only and does not constitute formal legal counsel or tenancy representation."
)

class AiService private constructor() {

    companion object {
        private const val TAG = "StayFinderAI"
        private const val GEMINI_MODEL = "gemini-2.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        @Volatile
        private var instance: AiService? = null

        fun getInstance(): AiService {
            return instance ?: synchronized(this) {
                instance ?: AiService().also { instance = it }
            }
        }
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    val isConfigured: Boolean
        get() = try {
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
        } catch (e: Exception) {
            false
        }

    /**
     * AI Room Inspector: Analyzes real room image pixels.
     */
    suspend fun inspectRoomImage(bitmap: Bitmap): Result<RoomInspectionReport> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("Gemini API key is not configured. Please set GEMINI_API_KEY in the AI Studio Secrets panel.")
            )
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val prompt = """
                You are StayFinder's visual room inspection analyzer.
                Analyze this photo of a rental room/property objectively based strictly on visible pixels.
                Do not hallucinate or invent features that cannot be seen.

                Return a strictly formatted JSON object with the following keys:
                {
                  "overallScore": <integer between 1 and 10>,
                  "cleanliness": "<brief assessment>",
                  "lighting": "<natural/artificial lighting assessment>",
                  "ventilation": "<windows/airflow indicators observed>",
                  "furnitureCondition": "<beds, desks, wardrobes visible condition>",
                  "visibleDamage": "<any visible wall cracks, dampness, peeling paint, or 'None detected'>",
                  "concerns": ["<specific observable concern 1>", "<concern 2>"],
                  "observations": "<2-3 sentence balanced visual summary>"
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val url = "$BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Gemini call failed with code ${response.code}: $errorBody")
                return@withContext Result.failure(Exception("AI analysis failed: HTTP ${response.code}"))
            }

            val responseText = response.body?.string() ?: ""
            val jsonRoot = JSONObject(responseText)
            val candidates = jsonRoot.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
            val textOutput = parts?.optJSONObject(0)?.optString("text") ?: "{}"

            val parsed = JSONObject(textOutput)
            val concernsList = mutableListOf<String>()
            val concernsArray = parsed.optJSONArray("concerns")
            if (concernsArray != null) {
                for (i in 0 until concernsArray.length()) {
                    concernsList.add(concernsArray.getString(i))
                }
            }

            val report = RoomInspectionReport(
                overallScore = parsed.optInt("overallScore", 7),
                cleanliness = parsed.optString("cleanliness", "Moderate cleanliness observed"),
                lighting = parsed.optString("lighting", "Adequate ambient lighting"),
                ventilation = parsed.optString("ventilation", "Standard window ventilation visible"),
                furnitureCondition = parsed.optString("furnitureCondition", "Standard condition"),
                visibleDamage = parsed.optString("visibleDamage", "None clearly visible in photo"),
                concerns = if (concernsList.isNotEmpty()) concernsList else listOf("Check plumbing and switches in person during physical visit"),
                observations = parsed.optString("observations", "The room appears generally well-maintained based on the uploaded image.")
            )
            Result.success(report)
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting room image", e)
            Result.failure(e)
        }
    }

    /**
     * AI Lease Analyzer: Analyzes real agreement or rental contract text.
     */
    suspend fun analyzeLeaseText(leaseText: String): Result<LeaseAnalysisReport> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("Gemini API key is not configured. Please set GEMINI_API_KEY in the AI Studio Secrets panel.")
            )
        }

        try {
            val prompt = """
                You are StayFinder's Lease & Tenancy Agreement Analyzer.
                Carefully analyze this rental agreement text. Extract what the document explicitly states, and highlight any unusual, restrictive, or punitive clauses.
                Do not fabricate facts not in the document.

                Document text:
                \"\"\"
                $leaseText
                \"\"\"

                Return a strictly formatted JSON object with the following keys:
                {
                  "rent": "<monthly rent and due date from document or 'Not specified'>",
                  "securityDeposit": "<deposit amount and refund timeline or 'Not specified'>",
                  "noticePeriod": "<notice required by tenant/landlord or 'Not specified'>",
                  "lockInPeriod": "<minimum lock-in duration or 'Not specified'>",
                  "maintenanceAndHiddenCharges": "<electricity, water, maintenance, cleaning fees stated>",
                  "restrictionsAndCurfew": "<guest rules, gates, noise, non-veg restrictions>",
                  "refundConditions": "<deductions for repainting, deep cleaning, wear-and-tear>",
                  "keyClauses": ["<key clause 1>", "<key clause 2>"],
                  "cautionaryTerms": ["<unusual or risky term 1>", "<unusual term 2>"],
                  "summary": "<clear 2-3 sentence executive summary for the tenant>"
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val url = "$BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("AI analysis failed: HTTP ${response.code}"))
            }

            val responseText = response.body?.string() ?: ""
            val jsonRoot = JSONObject(responseText)
            val candidates = jsonRoot.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
            val textOutput = parts?.optJSONObject(0)?.optString("text") ?: "{}"

            val parsed = JSONObject(textOutput)

            val keyClauses = mutableListOf<String>()
            val kcArray = parsed.optJSONArray("keyClauses")
            if (kcArray != null) {
                for (i in 0 until kcArray.length()) keyClauses.add(kcArray.getString(i))
            }

            val cautionary = mutableListOf<String>()
            val cArray = parsed.optJSONArray("cautionaryTerms")
            if (cArray != null) {
                for (i in 0 until cArray.length()) cautionary.add(cArray.getString(i))
            }

            val report = LeaseAnalysisReport(
                rent = parsed.optString("rent", "Not specified in snippet"),
                securityDeposit = parsed.optString("securityDeposit", "Not specified in snippet"),
                noticePeriod = parsed.optString("noticePeriod", "Not specified"),
                lockInPeriod = parsed.optString("lockInPeriod", "None mentioned"),
                maintenanceAndHiddenCharges = parsed.optString("maintenanceAndHiddenCharges", "Standard utilities"),
                restrictionsAndCurfew = parsed.optString("restrictionsAndCurfew", "Standard house rules"),
                refundConditions = parsed.optString("refundConditions", "Subject to inspection on move-out"),
                keyClauses = if (keyClauses.isNotEmpty()) keyClauses else listOf("Term of agreement", "Rent payment schedule"),
                cautionaryTerms = cautionary,
                summary = parsed.optString("summary", "The document outlines standard tenancy clauses.")
            )
            Result.success(report)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing lease", e)
            Result.failure(e)
        }
    }

    /**
     * AI Accommodation Assistant:
     * User query -> filter REAL Firestore properties -> pass actual property data to AI -> answer.
     * NEVER invents fictional properties!
     * If no matching real properties exist in Firestore, replies:
     * "I couldn't find a matching StayFinder property in the available listings."
     */
    suspend fun askAccommodationAssistant(
        userQuery: String,
        realFirestoreProperties: List<Property>
    ): String = withContext(Dispatchers.IO) {
        if (realFirestoreProperties.isEmpty()) {
            return@withContext "I couldn't find a matching StayFinder property in the available listings. Currently, there are no properties registered in your database."
        }

        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Local algorithmic matching against real Firestore properties when API key is not yet set
            return@withContext performLocalPropertyMatching(userQuery, realFirestoreProperties)
        }

        try {
            // Summarize the real properties from Firestore to pass into AI
            val propertySummaries = StringBuilder()
            realFirestoreProperties.take(20).forEachIndexed { index, p ->
                propertySummaries.append(
                    """
                    Property #${index + 1}:
                    - Name: ${p.name}
                    - Type: ${p.type} (${p.subType})
                    - City: ${p.city}
                    - Area / Locality: ${p.area}, ${p.locality}
                    - Landmark: ${p.landmark}
                    - Monthly Rent: ₹${p.monthlyRent}
                    - Security Deposit: ₹${p.securityDeposit}
                    - Available Beds: ${p.availableBeds} (Vacancies: ${p.vacancies})
                    - Furnished: ${p.furnishedStatus}
                    - Amenities: ${p.amenities.filterValues { it }.keys.joinToString(", ")}
                    - Sharing: ${p.sharingOptions.filterValues { it }.keys.joinToString(", ")}
                    - Rating: ${p.rating}

                    """.trimIndent()
                )
            }

            val prompt = """
                You are StayFinder's Accommodation Assistant.
                CRITICAL DIRECTIVE: You MUST ONLY recommend properties from the exact list of REAL properties provided below.
                NEVER make up, hallucinate, or assume properties that are not listed here.
                If NO properties in the list match what the user is asking for (e.g. city, budget, type, or amenities), you MUST explicitly state:
                "I couldn't find a matching StayFinder property in the available listings."

                REAL PROPERTIES IN STAYFINDER FIRESTORE:
                $propertySummaries

                USER QUESTION:
                "$userQuery"

                Provide a helpful, precise answer referencing only real properties from the list above. Mention the property name, rent, location, and relevant amenities.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                })
            }

            val url = "$BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext performLocalPropertyMatching(userQuery, realFirestoreProperties)
            }

            val responseText = response.body?.string() ?: ""
            val jsonRoot = JSONObject(responseText)
            val candidates = jsonRoot.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
            val answer = parts?.optJSONObject(0)?.optString("text")?.trim() ?: ""

            if (answer.isNotBlank()) answer else performLocalPropertyMatching(userQuery, realFirestoreProperties)
        } catch (e: Exception) {
            Log.e(TAG, "Assistant error", e)
            performLocalPropertyMatching(userQuery, realFirestoreProperties)
        }
    }

    private fun performLocalPropertyMatching(query: String, properties: List<Property>): String {
        val q = query.lowercase()
        val matching = properties.filter { p ->
            p.city.lowercase().contains(q) ||
            p.area.lowercase().contains(q) ||
            p.locality.lowercase().contains(q) ||
            p.landmark.lowercase().contains(q) ||
            p.subType.lowercase().contains(q) ||
            p.name.lowercase().contains(q) ||
            p.description.lowercase().contains(q) ||
            (q.contains("wifi") && p.amenities["wifi"] == true) ||
            (q.contains("ac") && p.amenities["ac"] == true) ||
            (q.contains("food") && p.amenities["food"] == true)
        }

        if (matching.isEmpty()) {
            return "I couldn't find a matching StayFinder property in the available listings."
        }

        val sb = StringBuilder("Here are matching StayFinder properties from your database:\n\n")
        matching.take(3).forEach { p ->
            sb.append("• **${p.name}** in ${p.area}, ${p.city}\n")
            sb.append("  Rent: ₹${p.monthlyRent}/month | Type: ${p.subType}\n")
            sb.append("  Vacancies: ${p.vacancies} beds available\n\n")
        }
        return sb.toString().trim()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
