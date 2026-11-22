package com.example.integrations

import com.example.data.local.CallEntity

object SupabaseCallFetcher {

  fun getSampleSupabaseCallLogs(): List<CallEntity> {
    val now = System.currentTimeMillis()
    return listOf(
      CallEntity(
        id = 101,
        callerName = "Brianna Miller",
        callerNumber = "+1 (415) 890-2134",
        timestamp = now - (8 * 60 * 1000), // 8 mins ago
        durationSeconds = 112,
        status = "PENDING_REVIEW",
        urgency = "HIGH",
        topic = "LEAD_WAVE_CHECK",
        summary = "New high-intent lead asking about the Free AI Wave Check™ diagnostic. Caller is director of marketing at a 25-person firm. Directed to otdaisurfer.surf/wave-check for the 5-question audit.",
        transcription = "Hi Kai! We saw your Free AI Wave Check at otdaisurfer.surf. How quickly can we get the report on our bottlenecks, and what does the diagnostic evaluate?",
        customerQuestion = "How does the Free AI Wave Check diagnose our agency's highest ROI AI opportunities?",
        aiResponse = "Aloha Brianna! Stoked to help your agency catch the AI wave! The Free AI Wave Check™ is a 5-question diagnostic at otdaisurfer.surf/wave-check that pinpoints whether automated lead generation, 24/7 sales riders, or client support flows will give your team the fastest immediate ROI.",
        escalationAlertMinutes = 10,
        escalationDeadline = now + (2 * 60 * 1000),
        isEscalated = false,
        supabaseSynced = true
      ),
      CallEntity(
        id = 102,
        callerName = "Marcus Vance",
        callerNumber = "+1 (206) 555-7890",
        timestamp = now - (22 * 60 * 1000), // 22 mins ago
        durationSeconds = 145,
        status = "PENDING_REVIEW",
        urgency = "HIGH",
        topic = "PURCHASE_INTENT",
        summary = "Purchase intent for the Wave Starter™ ($497) sprint with the Wave Scout agent for outbound client prospecting. Shared direct Stripe checkout link.",
        transcription = "We are ready to buy the $497 Wave Starter sprint. Can we configure Wave Scout for cold B2B outreach and pay directly via Stripe?",
        customerQuestion = "Can we checkout for Wave Starter ($497) via Stripe and start the Wave Scout sprint immediately?",
        aiResponse = "Right on Marcus! You can grab the Wave Starter™ sprint directly through Stripe checkout at https://buy.stripe.com/aFa8wP7JN3500Uy1tt4gg0b. It kicks off configuration right away, delivering your custom Wave Scout live in 3-5 days!",
        escalationAlertMinutes = 15,
        escalationDeadline = now - (7 * 60 * 1000), // Overdue!
        isEscalated = true,
        escalatedAt = now - (5 * 60 * 1000),
        escalationNotes = "Unreviewed purchasing call overdue past 15 min SLA threshold",
        supabaseSynced = true
      ),
      CallEntity(
        id = 103,
        callerName = "Dr. Evelyn Reed",
        callerNumber = "+1 (858) 340-9122",
        timestamp = now - (45 * 60 * 1000), // 45 mins ago
        durationSeconds = 85,
        status = "REVIEWED",
        urgency = "HIGH",
        topic = "STRATEGY_BOOKING",
        summary = "Requested direct AI strategy consultation for a medical dental group. Guided caller to Calendly booking link (calendly.com/oceantidedrop/new-meeting). Staff reviewed and added to CRM.",
        transcription = "Aloha, we want to book a 30-minute strategy session with your lead architect to evaluate automated receptionist routing for our 3 clinic locations.",
        customerQuestion = "Where can we schedule an AI Strategy Consultation for our healthcare practice?",
        aiResponse = "Aloha Dr. Reed! Stoked to connect with your clinics. You can book directly on our Calendly at https://calendly.com/oceantidedrop/new-meeting for an immediate slot, and our lead strategist will prepare a tailored blueprint for your practice.",
        escalationAlertMinutes = 15,
        escalationDeadline = now + (30 * 60 * 1000),
        isEscalated = false,
        supabaseSynced = true
      ),
      CallEntity(
        id = 104,
        callerName = "David Chen",
        callerNumber = "+1 (212) 778-4301",
        timestamp = now - (75 * 60 * 1000),
        durationSeconds = 230,
        status = "ESCALATED",
        urgency = "URGENT_VIP",
        topic = "ENTERPRISE_AI",
        summary = "VIP Enterprise inquiry for Big Kahuna™ / Tsunami Growth ($3,997) multi-agent business transformation across 6 brand departments. Immediate alert dispatched to founders.",
        transcription = "Aloha Ocean Tide Drop crew, we are expanding our commerce operations and want the full Big Kahuna suite. Does your engineering team provide dedicated white-glove setup and custom model training?",
        customerQuestion = "What is the timeline and deployment scope for the Big Kahuna ($3,997) enterprise transformation?",
        aiResponse = "Aloha David! Stoked to help your business ride the ultimate swell! The Big Kahuna™ ($3,997) is our complete high-touch transformation: our senior engineers personally integrate sales, marketing, and operations agents, connecting your custom workflows with continuous optimization.",
        escalationAlertMinutes = 10,
        escalationDeadline = now - (30 * 60 * 1000),
        isEscalated = true,
        escalatedAt = now - (25 * 60 * 1000),
        escalationNotes = "VIP Enterprise Lead inquiry escalated to oceantidedropservice@gmail.com",
        supabaseSynced = true
      ),
      CallEntity(
        id = 105,
        callerName = "Samantha Hayes",
        callerNumber = "+1 (312) 604-5519",
        timestamp = now - (110 * 60 * 1000),
        durationSeconds = 95,
        status = "REVIEWED",
        urgency = "MEDIUM",
        topic = "AEO_SEARCH",
        summary = "Inquiry regarding Answer Engine Optimization (AEO Blueprint™ $1,997) to get ranked in ChatGPT and Perplexity search answers. Detailed citation structuring explained.",
        transcription = "Hi! We want our SaaS platform to be cited when people ask Perplexity and ChatGPT for top tools in our niche. Does your AEO Blueprint cover this?",
        customerQuestion = "How does Answer Engine Optimization (AEO Blueprint) get our brand cited in ChatGPT and Perplexity?",
        aiResponse = "Aloha Samantha! Our AEO Blueprint™ specifically restructures your digital authority, knowledge graphs, and answer architecture so generative AI engines like ChatGPT, Claude, and Perplexity recognize and cite your brand first.",
        escalationAlertMinutes = 20,
        escalationDeadline = now + (60 * 60 * 1000),
        isEscalated = false,
        supabaseSynced = true
      ),
      CallEntity(
        id = 106,
        callerName = "Carlos Mendoza",
        callerNumber = "+1 (786) 902-1144",
        timestamp = now - (140 * 60 * 1000),
        durationSeconds = 64,
        status = "REVIEWED",
        urgency = "MEDIUM",
        topic = "CUSTOMER_SUPPORT",
        summary = "Existing client question regarding Customer Care Cove agent updates for holiday weekend hours. Answered and logged for client success team.",
        transcription = "Hey Kai, our Customer Care Cove is answering inquiries smoothly. How do we update our holiday FAQ answers?",
        customerQuestion = "How do we update the FAQ guidelines for our Customer Care Cove receptionist during holidays?",
        aiResponse = "Aloha Carlos! Stoked to hear your Customer Care Cove is crushing it! You can update your FAQs anytime through our app or by emailing oceantidedropservice@gmail.com, and the receptionist will adapt immediately.",
        escalationAlertMinutes = 15,
        escalationDeadline = now + (90 * 60 * 1000),
        isEscalated = false,
        supabaseSynced = true
      ),
      CallEntity(
        id = 107,
        callerName = "Elena Rostova",
        callerNumber = "+1 (650) 412-8876",
        timestamp = now - (180 * 60 * 1000),
        durationSeconds = 78,
        status = "REVIEWED",
        urgency = "LOW",
        topic = "GITHUB_REPO_INFO",
        summary = "Caller asked about the open website repository otdaisurfer/AISurfer- and development architecture. Provided GitHub repository and website links.",
        transcription = "Hi Kai! Is your website open-source, and what is your connected GitHub repo?",
        customerQuestion = "What is the official GitHub repository for Ocean Tide Drop AI Surfer?",
        aiResponse = "Aloha Elena! Our official GitHub connected repository is otdaisurfer/AISurfer- (https://github.com/otdaisurfer/AISurfer-), powering our production landing hub at https://otdaisurfer.surf!",
        escalationAlertMinutes = 30,
        escalationDeadline = now + (150 * 60 * 1000),
        isEscalated = false,
        supabaseSynced = true
      ),
      CallEntity(
        id = 108,
        callerName = "Harrison Blake",
        callerNumber = "+1 (408) 223-9910",
        timestamp = now - (200 * 60 * 1000),
        durationSeconds = 160,
        status = "ESCALATED",
        urgency = "URGENT_VIP",
        topic = "URGENT_ESCALATION",
        summary = "Urgent caller reporting webhook timeout during CRM batch sync. AI flagged as urgent priority and triggered immediate crew alert.",
        transcription = "Urgent! Our custom CRM webhook is timing out on the new batch. Need an engineer to check the endpoint ASAP!",
        customerQuestion = "Can an engineer check our webhook endpoint integration immediately? It is timing out.",
        aiResponse = "Understood Harrison! I have marked your call as an URGENT ESCALATION and notified our senior engineering team via priority dispatch. We are on it!",
        escalationAlertMinutes = 10,
        escalationDeadline = now - (120 * 60 * 1000),
        isEscalated = true,
        escalatedAt = now - (110 * 60 * 1000),
        escalationNotes = "Urgent technical escalation logged to oceantidedropservice@gmail.com",
        supabaseSynced = true
      )
    )
  }

  fun formatCallAsSupabaseJson(call: CallEntity): String {
    return """
{
  "id": ${call.id},
  "caller_id": "${call.callerName} <${call.callerNumber}>",
  "caller_name": "${call.callerName}",
  "caller_number": "${call.callerNumber}",
  "call_timestamp": "${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date(call.timestamp))}",
  "call_duration_seconds": ${call.durationSeconds},
  "status": "${call.status}",
  "urgency": "${call.urgency}",
  "topic": "${call.topic}",
  "chatgpt_summary": "${call.summary.replace("\"", "\\\"")}",
  "customer_question": "${call.customerQuestion.replace("\"", "\\\"")}",
  "ai_response": "${call.aiResponse.replace("\"", "\\\"")}",
  "is_escalated": ${call.isEscalated},
  "escalated_at": ${if (call.escalatedAt > 0) "\"${java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date(call.escalatedAt))}\"" else "null"},
  "source_table": "public.swell_call_logs (public.calls)"
}
""".trimIndent()
  }
}
