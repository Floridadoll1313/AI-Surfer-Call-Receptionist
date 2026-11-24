package com.example.ai

import com.example.data.local.FaqEntity
import com.example.data.local.ProductEntity

data class ReceptionistResult(
  val responseText: String,
  val summary: String,
  val urgency: String, // LOW, MEDIUM, HIGH, URGENT_VIP
  val topic: String, // PRODUCT_INQUIRY, BUSINESS_INFO, LESSON_BOOKING, VIP_DROP, GENERAL
  val requiresImmediateEscalation: Boolean,
  val escalationReason: String = ""
)

object SurferAiReceptionist {

  private const val SYSTEM_PROMPT = """
You are Kai, the official Ocean Tide Drop AI Surfer Receptionist.
You represent Ocean Tide Drop AI Surfer (https://otdaisurfer.surf) — "Ride the Wave 🌊 Grow with AI".
We help businesses discover where AI can create the biggest impact and implement practical, high-converting AI systems:
- Free AI Wave Check™ (Free 5-question diagnostic at https://otdaisurfer.surf/wave-check)
- AI Opportunity Report™ (Prioritizes business AI opportunities)
- Wave Starter™ ($497 focused implementation sprint: choose Wave Scout for lead generation, Sales Rider for 24/7 sales, Content Creator for marketing assets, or Customer Care Cove for automated support. Stripe checkout: https://buy.stripe.com/aFa8wP7JN3500Uy1tt4gg0b)
- AEO Blueprint™ ($1,997 AI search visibility across ChatGPT, Perplexity, Gemini, Claude)
- Automation Architect™ ($1,997 connecting business tools and automated workflows)
- Wave Builder Package ($1,997 unified AI growth system)
- Big Kahuna™ / Tsunami Growth ($3,997 high-touch, multi-agent business transformation)
- GitHub Repository: https://github.com/otdaisurfer/AISurfer- (powers otdaisurfer.surf)
- Strategy Booking: https://calendly.com/oceantidedrop/new-meeting | Email: oceantidedropservice@gmail.com
Your tone: Authentic, stoked, warm coastal surfer energy combined with sharp, attentive business advisory hospitality. Speak with enthusiasm ("Aloha!", "Stoked to help you catch the AI wave!", "Right on!").
You must answer ALL business questions and ALL product questions thoroughly and accurately.
"""

  suspend fun respondToCall(
    callerName: String,
    callerNumber: String,
    callerQuestion: String,
    products: List<ProductEntity>,
    faqs: List<FaqEntity>,
    config: com.example.integrations.ChatGptConfig? = null,
    returningContext: String = ""
  ): ReceptionistResult {
    return processCallerQuestion(
      callerName = callerName,
      callerNumber = callerNumber,
      question = callerQuestion,
      products = products,
      faqs = faqs,
      openAiApiKey = config?.apiKey,
      modelName = config?.model ?: "gpt-4o-mini",
      returningContext = returningContext,
      useOpenAi = config?.isEnabled ?: true
    )
  }

  suspend fun processCallerQuestion(
    callerName: String,
    callerNumber: String,
    question: String,
    products: List<ProductEntity>,
    faqs: List<FaqEntity>,
    openAiApiKey: String? = null,
    modelName: String = "gpt-4o-mini",
    returningContext: String = "",
    useOpenAi: Boolean = true
  ): ReceptionistResult {
    // 1. Try ChatGPT if enabled and configured via override or AI Studio Secrets (BuildConfig.OPENAI_API_KEY)
    val effectiveOpenAiKey = if (!useOpenAi) {
      null
    } else {
      openAiApiKey?.takeIf { it.isNotBlank() } ?: OpenAiClient.configuredBuildConfigKey()
    }
    if (!effectiveOpenAiKey.isNullOrBlank() && effectiveOpenAiKey.startsWith("sk-")) {
      try {
        val productContext = products.joinToString("\n") {
          "- ${it.name} ($${it.price}): ${it.description} [Category: ${it.category}, Level: ${it.surfLevel}]"
        }
        val faqContext = faqs.joinToString("\n") {
          "- Q: ${it.question} | A: ${it.answer}"
        }
        val vipBlock = if (returningContext.isNotBlank()) "\n$returningContext\n" else ""

        val prompt = """
$SYSTEM_PROMPT

Current Business FAQ & Details:
$faqContext

Current Products & Services:
$productContext

Incoming Caller: $callerName ($callerNumber)$vipBlock
Caller Question: "$question"

Respond in valid JSON with exact keys:
{
  "reply": "<warm, accurate answer as Kai to speak to the caller>",
  "mainTopic": "<concise 2-5 word topic>",
  "actionItems": "<1-2 concise follow-up action items for the team>",
  "sentiment": "<Positive | Neutral | Excited VIP | Urgent / Frustrated>"
}
""".trimIndent()

        val request = ChatCompletionRequest(
          model = modelName,
          messages = listOf(
            ChatMessage(role = "system", content = SYSTEM_PROMPT),
            ChatMessage(role = "user", content = prompt)
          ),
          temperature = 0.5
        )

        val response = OpenAiClient.api.createChatCompletion(
          authorization = "Bearer $effectiveOpenAiKey",
          request = request
        )

        val rawContent = response.choices?.firstOrNull()?.message?.content
        if (!rawContent.isNullOrBlank()) {
          val parsed = parseOpenAiSummaryJson(rawContent, callerName, question)
          val classification = classifyCallerIntent(question)
          val aiFlagged = classification.isImmediate ||
            parsed.reply.lowercase().contains("escalat") ||
            parsed.sentiment.contains("Urgent", ignoreCase = true)
          val reason = classification.reason.ifBlank {
            if (aiFlagged) "AI Receptionist flagged conversation for priority crew follow-up." else ""
          }
          return ReceptionistResult(
            responseText = parsed.reply,
            summary = "Topic: ${parsed.mainTopic} | Action Items: ${parsed.actionItems} | Sentiment: ${parsed.sentiment}",
            urgency = if (aiFlagged && classification.urgency != "URGENT_VIP") "URGENT_VIP" else classification.urgency,
            topic = classification.topic,
            requiresImmediateEscalation = aiFlagged,
            escalationReason = reason
          )
        }
      } catch (_: Exception) {
        // Fall through to Firebase AI Logic (Gemini) or built-in engine
      }
    }

    // 2. Try Firebase AI Logic (Gemini gemini-3.8-flash) when FirebaseApp is initialized
    val geminiInsight = GeminiAiService.generateStructuredCallInsight(
      callerName = callerName,
      callerNumber = callerNumber,
      question = question,
      products = products,
      faqs = faqs,
      returningContext = returningContext
    )
    if (geminiInsight != null) {
      val ruleClass = classifyCallerIntent(question)
      val flagged = geminiInsight.requiresEscalation || ruleClass.isImmediate
      val urgency = if (flagged) "URGENT_VIP" else geminiInsight.urgency
      val reason = geminiInsight.escalationReason.ifBlank { ruleClass.reason }
      return ReceptionistResult(
        responseText = geminiInsight.replyText,
        summary = geminiInsight.summaryFormatted,
        urgency = urgency,
        topic = ruleClass.topic,
        requiresImmediateEscalation = flagged,
        escalationReason = reason
      )
    }

    // 3. Built-in Ocean Tide Drop AI Surfer Knowledge Engine (offline/unit-test safe)
    return analyzeWithInternalEngine(callerName, callerNumber, question, products, faqs, returningContext)
  }

  private data class ParsedOpenAiResponse(
    val reply: String,
    val mainTopic: String,
    val actionItems: String,
    val sentiment: String
  )

  private fun parseOpenAiSummaryJson(raw: String, callerName: String, question: String): ParsedOpenAiResponse {
    return try {
      val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
      val json = org.json.JSONObject(cleaned)
      ParsedOpenAiResponse(
        reply = json.optString("reply", raw).trim(),
        mainTopic = json.optString("mainTopic", "Customer Inquiry").trim(),
        actionItems = json.optString("actionItems", "Review transcript & follow up with $callerName").trim(),
        sentiment = json.optString("sentiment", "Positive").trim()
      )
    } catch (_: Exception) {
      val cls = classifyCallerIntent(question)
      ParsedOpenAiResponse(
        reply = raw.trim(),
        mainTopic = cls.topic.replace('_', ' '),
        actionItems = "Follow up with $callerName on inquiry",
        sentiment = if (cls.isImmediate) "Urgent / Frustrated" else "Positive"
      )
    }
  }

  private fun analyzeWithInternalEngine(
    callerName: String,
    callerNumber: String,
    question: String,
    products: List<ProductEntity>,
    faqs: List<FaqEntity>,
    returningContext: String = ""
  ): ReceptionistResult {
    val q = question.lowercase()
    val classification = classifyCallerIntent(question)
    val returningGreeting = if (returningContext.isNotBlank()) "Welcome back to Ocean Tide Drop, $callerName! " else ""

    // Check specific product queries against otdaisurfer.surf catalog
    val matchedProduct = products.firstOrNull { prod ->
      val pName = prod.name.lowercase()
      val pCat = prod.category.lowercase()
      val pSlug = prod.slug.lowercase()
      (q.contains("wave check") || q.contains("diagnostic") || q.contains("free audit") || q.contains("free check")) && (pSlug == "wave-check" || pName.contains("wave check")) ||
      (q.contains("opportunity report") || q.contains("report")) && (pSlug.contains("report") || pName.contains("opportunity report")) ||
      (q.contains("wave starter") || q.contains("starter sprint") || (q.contains("497") && !q.contains("scout") && !q.contains("rider"))) && (pSlug == "wave-starter" || pName.contains("wave starter")) ||
      (q.contains("wave scout") || q.contains("scout") || q.contains("lead gen") || q.contains("prospect")) && (pSlug.contains("scout") || pName.contains("scout")) ||
      (q.contains("sales rider") || q.contains("rider") || q.contains("sales assistant") || q.contains("follow up")) && (pSlug.contains("rider") || pName.contains("sales rider")) ||
      (q.contains("content creator") || q.contains("content") || q.contains("copywriting") || q.contains("marketing asset")) && (pSlug.contains("content") || pName.contains("content creator")) ||
      (q.contains("customer care") || q.contains("cove") || q.contains("support flow") || q.contains("help desk")) && (pSlug.contains("cove") || pName.contains("customer care")) ||
      (q.contains("aeo") || q.contains("answer engine") || q.contains("ai search") || q.contains("perplexity")) && (pSlug.contains("aeo") || pName.contains("aeo")) ||
      (q.contains("automation blueprint") || q.contains("map workflow")) && (pSlug.contains("automation-blueprint") || pName.contains("automation blueprint")) ||
      (q.contains("automation architect") || q.contains("architect") || q.contains("connect tools")) && (pSlug.contains("automation-architect") || pName.contains("automation architect")) ||
      (q.contains("wave builder") || q.contains("1997") || q.contains("growth system")) && (pSlug.contains("builder") || pName.contains("wave builder")) ||
      (q.contains("big kahuna") || q.contains("tsunami") || q.contains("3997") || q.contains("transformation") || q.contains("vip")) && (pSlug.contains("kahuna") || pName.contains("big kahuna"))
    }

    // Check specific FAQ queries
    val matchedFaq = faqs.firstOrNull { faq ->
      val keywords = faq.tags.split(",").map { it.trim().lowercase() }
      keywords.any { q.contains(it) } || q.contains(faq.category.lowercase())
    }

    val coreResponseText = when {
      matchedProduct != null -> {
        val priceStr = if (matchedProduct.price == 0.0) "Free" else "$${"%.0f".format(matchedProduct.price)}"
        val ctaHint = if (matchedProduct.directUrl.isNotBlank()) " Check it out or buy at: ${matchedProduct.directUrl}." else ""
        "Aloha $callerName! Stoked to help you with ${matchedProduct.name} ($priceStr). " +
        "${matchedProduct.description} [${matchedProduct.surfLevel}].$ctaHint " +
        "I have logged this in our high-priority queue with our Ocean Tide Drop AI team so we can assist your business right away!"
      }
      matchedFaq != null -> {
        "Aloha $callerName! Ocean Tide Drop AI Surfer (otdaisurfer.surf) has got you covered! " +
        "${matchedFaq.answer} Need us to customize an AI workflow for your team? I've logged your request!"
      }
      q.contains("price") || q.contains("cost") || q.contains("how much") || q.contains("pricing") || q.contains("package") -> {
        "Aloha $callerName! Our offerings on https://otdaisurfer.surf start with the Free AI Wave Check™ ($0), Wave Starter™ focused implementation sprints at $497 (via Stripe checkout for Wave Scout, Sales Rider, Content Creator, or Customer Care Cove), Wave Builder packages at $1,997, and our full Big Kahuna™ / Tsunami Growth transformation at $3,997. Which stage would best accelerate your team?"
      }
      q.contains("wave check") || q.contains("audit") || q.contains("free") || q.contains("start") -> {
        "Stoked! You can take our Free 5-question AI Wave Check™ right now at https://otdaisurfer.surf/wave-check to diagnose where AI will have the highest immediate ROI for your operations!"
      }
      q.contains("book") || q.contains("meeting") || q.contains("call") || q.contains("strategy") || q.contains("schedule") -> {
        "Right on $callerName! You can book a direct AI Strategy Session on our Calendly at https://calendly.com/oceantidedrop/new-meeting, or I can have our lead strategist call you back within our priority response window!"
      }
      q.contains("stripe") || q.contains("buy") || q.contains("purchase") || q.contains("checkout") -> {
        "You can grab the Wave Starter™ $497 sprint directly via Stripe checkout at: https://buy.stripe.com/aFa8wP7JN3500Uy1tt4gg0b. It kicks off implementation immediately!"
      }
      q.contains("hour") || q.contains("open") || q.contains("time") -> {
        "Aloha! Ocean Tide Drop AI Surfer operates full digital AI client implementations Monday through Saturday, while our AI Receptionist monitors your cell phone calls 24/7 so your business never misses an opportunity."
      }
      q.contains("emergency") || q.contains("urgent") || q.contains("asap") || q.contains("escalat") -> {
        "Understood, $callerName! I've marked your call as URGENT ESCALATION. Our founders and lead engineers at Ocean Tide Drop AI Surfer have been notified via our priority dispatch alert. We're on it!"
      }
      q.contains("github") || q.contains("repo") || q.contains("repository") || q.contains("code") || q.contains("aisurfer") -> {
        "Aloha $callerName! Our official GitHub connected repository is otdaisurfer/AISurfer- (https://github.com/otdaisurfer/AISurfer-), powering our production web presence at https://otdaisurfer.surf!"
      }
      else -> {
        // Universal answer for any business/custom question
        "Aloha $callerName! Thanks for reaching out to Ocean Tide Drop AI Surfer (https://otdaisurfer.surf). " +
        "Regarding your question: \"$question\", our AI engineering team builds custom workflows, sales assistants, content engines, and answer engine SEO tailored to your business. " +
        "I have logged this in our priority queue and our crew will get in touch with you promptly!"
      }
    }
    val responseText = returningGreeting + coreResponseText

    val summary = generateSummary(callerName, question, responseText, classification)

    return ReceptionistResult(
      responseText = responseText,
      summary = summary,
      urgency = classification.urgency,
      topic = classification.topic,
      requiresImmediateEscalation = classification.isImmediate,
      escalationReason = classification.reason
    )
  }

  private data class IntentClassification(
    val urgency: String,
    val topic: String,
    val isImmediate: Boolean,
    val reason: String = ""
  )

  private fun classifyCallerIntent(question: String): IntentClassification {
    val q = question.lowercase()
    return when {
      q.contains("urgent") || q.contains("emergency") || q.contains("asap") || q.contains("escalat") ||
        q.contains("down") || q.contains("outage") || q.contains("timeout") || q.contains("broken") -> {
        IntentClassification(
          urgency = "URGENT_VIP",
          topic = "HIGH_PRIORITY",
          isImmediate = true,
          reason = "AI flagged urgent incident or explicit escalation request."
        )
      }
      q.contains("human") || q.contains("manager") || q.contains("supervisor") || q.contains("real person") ||
        q.contains("refund") || q.contains("complaint") || q.contains("cancel") -> {
        IntentClassification(
          urgency = "URGENT_VIP",
          topic = "CREW_HANDOFF",
          isImmediate = true,
          reason = "AI flagged caller request for human crew handoff or billing support."
        )
      }
      q.contains("big kahuna") || q.contains("tsunami") || q.contains("3997") || q.contains("transformation") || q.contains("enterprise") -> {
        IntentClassification(
          urgency = "URGENT_VIP",
          topic = "ENTERPRISE_AI",
          isImmediate = true,
          reason = "AI flagged high-value Big Kahuna™ / Enterprise VIP inquiry."
        )
      }
      q.contains("wave starter") || q.contains("497") || q.contains("stripe") || q.contains("buy") || q.contains("wave builder") || q.contains("1997") -> {
        IntentClassification(urgency = "HIGH", topic = "PURCHASE_INTENT", isImmediate = false)
      }
      q.contains("book") || q.contains("schedule") || q.contains("meeting") || q.contains("strategy") || q.contains("calendly") -> {
        IntentClassification(urgency = "HIGH", topic = "STRATEGY_BOOKING", isImmediate = false)
      }
      q.contains("wave check") || q.contains("audit") || q.contains("diagnostic") || q.contains("product") || q.contains("scout") || q.contains("rider") || q.contains("content") || q.contains("cove") || q.contains("aeo") || q.contains("price") -> {
        IntentClassification(urgency = "MEDIUM", topic = "PRODUCT_INQUIRY", isImmediate = false)
      }
      q.contains("hour") || q.contains("where") || q.contains("who") || q.contains("website") || q.contains("contact") || q.contains("email") -> {
        IntentClassification(urgency = "LOW", topic = "BUSINESS_INFO", isImmediate = false)
      }
      else -> {
        IntentClassification(urgency = "MEDIUM", topic = "GENERAL", isImmediate = false)
      }
    }
  }

  private fun generateSummary(
    callerName: String,
    question: String,
    answer: String,
    classification: IntentClassification
  ): String {
    val sentiment = when {
      classification.isImmediate && (question.contains("down", ignoreCase = true) || question.contains("urgent", ignoreCase = true) || question.contains("refund", ignoreCase = true)) ->
        "Urgent / Frustrated"
      classification.urgency == "URGENT_VIP" || classification.urgency == "HIGH" ->
        "Positive (High Intent)"
      else -> "Positive"
    }
    val actionItem = when (classification.topic) {
      "HIGH_PRIORITY", "CREW_HANDOFF" -> "Escalate immediately to oceantidedropservice@gmail.com and call $callerName back"
      "ENTERPRISE_AI" -> "Schedule Big Kahuna architecture call via Calendly"
      "PURCHASE_INTENT" -> "Confirm Wave Starter Stripe checkout & sprint onboarding"
      "STRATEGY_BOOKING" -> "Send Calendly strategy invite & prep audit"
      else -> "Crew follow-up on ${classification.topic.lowercase().replace('_', ' ')}"
    }
    val topicLabel = classification.topic.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    val cleanQ = question.take(70)
    return "Topic: $topicLabel (\"$cleanQ\") | Action Items: $actionItem | Sentiment: $sentiment"
  }
}
