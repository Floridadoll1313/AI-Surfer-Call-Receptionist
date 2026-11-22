// Ocean Tide Drop AI Surfer - Node.js Backend API & Live Dashboard Server
import http from "node:http";

const PORT = Number(process.env.APP_PORT || (process.env.PORT && process.env.PORT !== "8080" ? process.env.PORT : 3000));

// Persistent in-memory state mirroring Android Room + Supabase public.calls/products/faqs
let escalationTriggerConfig = {
  isEnabled: true,
  alertChannel: "BOTH", // NOTIFICATION | EMAIL | BOTH
  targetEmail: "oceantidedropservice@gmail.com",
  webhookUrl: "",
  autoOpenEmailComposer: true,
  escalateOnHighUrgency: false
};

let escalationDispatches = [];

let calls = [
  {
    id: 1,
    callerName: "Elena Vance",
    callerNumber: "+1 (808) 555-0142",
    timestamp: Date.now() - 1000 * 60 * 3,
    durationSeconds: 48,
    status: "PENDING_REVIEW",
    urgency: "HIGH",
    topic: "PURCHASE_INTENT",
    customerQuestion: "Aloha! Can we start with the $497 Wave Starter Sprint using Sales Rider for 24/7 inbound leads?",
    aiResponse:
      "Aloha Elena! Stoked to help you with Wave Starter™ Sprint ($497). It deploys Sales Rider for always-on lead qualification and follow-up within 3-5 days. Direct Stripe checkout: https://buy.stripe.com/aFa8wP7JN3500Uy1tt4gg0b.",
    summary:
      "Elena Vance inquired about $497 Wave Starter Sprint (Sales Rider). AI provided Stripe checkout & queued for crew.",
    transcription:
      "Caller: \"Aloha! Can we start with the $497 Wave Starter Sprint using Sales Rider?\"\nKai: \"Aloha Elena! Stoked to help you with Wave Starter™ Sprint ($497)...\"",
    escalationAlertMinutes: 10,
    escalationDeadline: Date.now() + 1000 * 60 * 7,
    isEscalated: false,
    escalatedAt: 0,
    escalationNotes: ""
  },
  {
    id: 2,
    callerName: "Marcus Thorne",
    callerNumber: "+1 (808) 555-0199",
    timestamp: Date.now() - 1000 * 60 * 12,
    durationSeconds: 72,
    status: "ESCALATED",
    urgency: "URGENT_VIP",
    topic: "ENTERPRISE_AI",
    customerQuestion:
      "Urgent VIP: We want the $3,997 Big Kahuna Tsunami Growth transformation across marketing, sales, and support ASAP.",
    aiResponse:
      "Aloha Marcus! Right on — Big Kahuna™ / Tsunami Growth ($3,997) brings strategy, AEO visibility, automation, and multi-agent AI together. Because this is an urgent enterprise inquiry, I've flagged it for immediate escalation to our founders!",
    summary:
      "VIP Big Kahuna ($3,997) multi-agent transformation inquiry flagged as URGENT_VIP.",
    transcription:
      "Caller: \"Urgent VIP: We want the $3,997 Big Kahuna Tsunami Growth transformation ASAP.\"\nKai: \"Aloha Marcus! Big Kahuna™ / Tsunami Growth ($3,997)...\"",
    escalationAlertMinutes: 3,
    escalationDeadline: Date.now() - 1000 * 60 * 9,
    isEscalated: true,
    escalatedAt: Date.now() - 1000 * 60 * 9,
    escalationNotes:
      "AI flagged high-value Big Kahuna™ / Enterprise VIP inquiry. Dispatched alert to oceantidedropservice@gmail.com."
  },
  {
    id: 3,
    callerName: "Liam Brooks",
    callerNumber: "+1 (310) 555-0174",
    timestamp: Date.now() - 1000 * 60 * 35,
    durationSeconds: 41,
    status: "REVIEWED",
    urgency: "MEDIUM",
    topic: "PRODUCT_INQUIRY",
    customerQuestion: "How does the Free AI Wave Check diagnostic work?",
    aiResponse:
      "Stoked you asked, Liam! Our Free AI Wave Check™ is a 5-question diagnostic at https://otdaisurfer.surf/wave-check that pinpoints your biggest business AI bottleneck and first practical move.",
    summary:
      "Liam Brooks asked about the Free AI Wave Check diagnostic. AI shared otdaisurfer.surf/wave-check.",
    transcription:
      "Caller: \"How does the Free AI Wave Check diagnostic work?\"\nKai: \"Stoked you asked, Liam! Our Free AI Wave Check™...\"",
    escalationAlertMinutes: 10,
    escalationDeadline: Date.now() - 1000 * 60 * 25,
    isEscalated: false,
    escalatedAt: 0,
    escalationNotes: ""
  }
];

let products = [
  {
    id: 1,
    name: "Free AI Wave Check™",
    price: 0.0,
    category: "Business AI Opportunity Check",
    description:
      "Answer five quick questions to see your strongest AI opportunity, practical first move, and the implementation path that fits your business.",
    popularBadge: "Start Here",
    surfLevel: "Stage: Discover (Free)",
    ctaText: "Start Free Wave Check",
    directUrl: "https://otdaisurfer.surf/wave-check",
    slug: "wave-check"
  },
  {
    id: 2,
    name: "AI Opportunity Report™",
    price: 0.0,
    category: "Business AI Strategy",
    description:
      "Turn scattered AI possibilities into a prioritized list of the opportunities most likely to create measurable business value.",
    popularBadge: "Core Diagnosis",
    surfLevel: "Stage: Diagnose (Free Tier)",
    ctaText: "Explore Opportunities",
    directUrl: "https://otdaisurfer.surf/members/products/ai-opportunity-report",
    slug: "ai-opportunity-report"
  },
  {
    id: 3,
    name: "Wave Starter™ Sprint",
    price: 497.0,
    category: "AI Implementation Sprint",
    description:
      "A focused implementation sprint to turn your clearest AI opportunity into a working business system (Wave Scout, Sales Rider, Content Creator, or Customer Care Cove).",
    popularBadge: "Best Seller",
    surfLevel: "Stage: Implement ($497)",
    ctaText: "Buy Wave Starter ($497)",
    directUrl: "https://buy.stripe.com/aFa8wP7JN3500Uy1tt4gg0b",
    slug: "wave-starter"
  },
  {
    id: 4,
    name: "Wave Scout™",
    price: 497.0,
    category: "Lead Generation AI",
    description:
      "Identify prospects, research buying signals, and organize opportunities so your sales team spends less time hunting and more time closing.",
    popularBadge: "Lead Generation",
    surfLevel: "Stage: Implement (Wave Starter)",
    ctaText: "Ride with Wave Scout",
    directUrl: "https://otdaisurfer.surf/members/products/wave-scout",
    slug: "wave-scout"
  },
  {
    id: 5,
    name: "Sales Rider™",
    price: 497.0,
    category: "AI Sales Assistant",
    description:
      "Turn leads into conversations and conversations into opportunities with an always-on sales assistant that responds instantly and organizes follow-up.",
    popularBadge: "Sales Conversion",
    surfLevel: "Stage: Implement (Wave Starter)",
    ctaText: "Meet Sales Rider",
    directUrl: "https://otdaisurfer.surf/members/products/sales-rider",
    slug: "sales-rider"
  },
  {
    id: 6,
    name: "Content Creator™",
    price: 497.0,
    category: "AI Marketing Engine",
    description:
      "Generate strategic social posts, emails, blogs, campaigns, offers, and marketing assets while keeping your message and tone strictly aligned.",
    popularBadge: "Marketing Engine",
    surfLevel: "Stage: Implement (Wave Starter)",
    ctaText: "Create with Content Creator",
    directUrl: "https://otdaisurfer.surf/members/products/content-creator",
    slug: "content-creator"
  },
  {
    id: 7,
    name: "Customer Care Cove™",
    price: 497.0,
    category: "AI Customer Support",
    description:
      "Automate helpful customer support while keeping the human touch. Creates faster, more consistent answers for routine customer questions and service requests.",
    popularBadge: "Customer Support",
    surfLevel: "Stage: Implement (Wave Starter)",
    ctaText: "Design Support Flow",
    directUrl: "https://otdaisurfer.surf/members/products/customer-care-cove",
    slug: "customer-care-cove"
  },
  {
    id: 8,
    name: "AEO Blueprint™",
    price: 1997.0,
    category: "AI Search Strategy",
    description:
      "Build a practical roadmap for becoming more visible, understandable, and authoritative across AI-powered search engines (ChatGPT, Perplexity, Gemini, Claude).",
    popularBadge: "Answer Engine SEO",
    surfLevel: "Stage: Plan (Wave Builder)",
    ctaText: "Build the Blueprint",
    directUrl: "https://otdaisurfer.surf/members/products/aeo-blueprint",
    slug: "aeo-blueprint"
  },
  {
    id: 9,
    name: "Automation Architect™",
    price: 1997.0,
    category: "AI Business Automation",
    description:
      "Connect processes, tools, data, and AI agents into practical automated workflows that eliminate repetitive handoffs and scale your business.",
    popularBadge: "Ops Automation",
    surfLevel: "Stage: Implement (Wave Builder)",
    ctaText: "Automate the Work",
    directUrl: "https://otdaisurfer.surf/members/products/automation-architect",
    slug: "automation-architect"
  },
  {
    id: 10,
    name: "Wave Builder Package",
    price: 1997.0,
    category: "Growth System Package",
    description:
      "A larger build for businesses ready to connect AI visibility, lead flow, follow-up, and automation into one practical, unified growth system.",
    popularBadge: "Growth System",
    surfLevel: "Stage: Plan & Build ($1,997)",
    ctaText: "Request Strategy Call",
    directUrl: "https://calendly.com/oceantidedrop/new-meeting",
    slug: "wave-builder"
  },
  {
    id: 11,
    name: "Big Kahuna™ / Tsunami Growth",
    price: 3997.0,
    category: "AI Growth Architect",
    description:
      "Bring strategy, visibility, automation, AI agents, workflows, and growth opportunities together into one complete, high-touch AI transformation experience.",
    popularBadge: "VIP Transformation",
    surfLevel: "Stage: Transform ($3,997)",
    ctaText: "Talk With AI Surfer",
    directUrl: "https://calendly.com/oceantidedrop/new-meeting",
    slug: "big-kahuna"
  }
];

let faqs = [
  {
    id: 1,
    category: "What We Do",
    question: "What is Ocean Tide Drop AI Surfer and how do you help businesses?",
    answer:
      "Ocean Tide Drop AI Surfer ('Ride the Wave 🌊 Grow with AI' - https://otdaisurfer.surf) helps businesses identify high-leverage AI opportunities and implements custom AI sales assistants, lead generators, content engines, support agents, answer-engine SEO, and end-to-end automation systems."
  },
  {
    id: 2,
    category: "Free Diagnostic",
    question: "How does the Free AI Wave Check™ work?",
    answer:
      "The Free AI Wave Check is a 5-question diagnostic at https://otdaisurfer.surf/wave-check that pinpoints your biggest bottleneck (lead response, consistent content, repetitive operations) and gives you a practical first move with a recommended implementation path."
  },
  {
    id: 3,
    category: "Pricing & Packages",
    question: "What are your core packages and pricing?",
    answer:
      "Wave Starter ($497 focused sprint via Stripe checkout), Wave Builder ($1,997 multi-tool integration, AEO Blueprint & Automation Architect), and Tsunami Growth / Big Kahuna ($3,997 complete multi-agent transformation)."
  },
  {
    id: 4,
    category: "Strategy & Booking",
    question: "How can I book an AI Strategy Call or get in touch?",
    answer:
      "Schedule directly via Calendly at https://calendly.com/oceantidedrop/new-meeting, email oceantidedropservice@gmail.com, or explore https://otdaisurfer.surf."
  },
  {
    id: 5,
    category: "AI Receptionist & SLA",
    question: "How does the AI Phone Receptionist & Call Escalation work?",
    answer:
      "Kai monitors incoming phone inquiries 24/7, answers product and business questions, and triggers instant notification & email escalation alerts to oceantidedropservice@gmail.com whenever the AI flags urgency or SLA thresholds elapse."
  }
];

let chatGptConfig = {
  apiKey: "",
  model: "gpt-4o-mini"
};

let supabaseConfig = {
  projectUrl: "https://kbgujyuxdopx.supabase.co",
  anonKey: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.oceantidedrop_mock_anon_key"
};

let gitHubConfig = {
  repoName: "AISurfer-",
  repoOwner: "otdaisurfer",
  repoUrl: "https://github.com/otdaisurfer/AISurfer-",
  websiteUrl: "https://otdaisurfer.surf",
  branch: "main",
  lastDeploymentStatus: "Connected: otdaisurfer/AISurfer- (Live on otdaisurfer.surf)"
};

// AI Intent Classification & Response Logic (mirrors SurferAiReceptionist.kt)
function classifyCallerIntent(question) {
  const q = (question || "").toLowerCase();
  if (
    q.includes("urgent") || q.includes("emergency") || q.includes("asap") ||
    q.includes("escalat") || q.includes("down") || q.includes("outage") ||
    q.includes("timeout") || q.includes("broken")
  ) {
    return {
      urgency: "URGENT_VIP",
      topic: "HIGH_PRIORITY",
      requiresImmediateEscalation: true,
      escalationReason: "AI flagged urgent incident or explicit escalation request."
    };
  }
  if (
    q.includes("human") || q.includes("manager") || q.includes("supervisor") ||
    q.includes("real person") || q.includes("refund") || q.includes("complaint") || q.includes("cancel")
  ) {
    return {
      urgency: "URGENT_VIP",
      topic: "CREW_HANDOFF",
      requiresImmediateEscalation: true,
      escalationReason: "AI flagged caller request for human crew handoff or billing support."
    };
  }
  if (
    q.includes("big kahuna") || q.includes("tsunami") || q.includes("3997") ||
    q.includes("transformation") || q.includes("enterprise")
  ) {
    return {
      urgency: "URGENT_VIP",
      topic: "ENTERPRISE_AI",
      requiresImmediateEscalation: true,
      escalationReason: "AI flagged high-value Big Kahuna™ / Enterprise VIP inquiry."
    };
  }
  if (q.includes("wave starter") || q.includes("497") || q.includes("stripe") || q.includes("buy") || q.includes("wave builder") || q.includes("1997")) {
    return { urgency: "HIGH", topic: "PURCHASE_INTENT", requiresImmediateEscalation: false, escalationReason: "" };
  }
  if (q.includes("book") || q.includes("schedule") || q.includes("meeting") || q.includes("strategy") || q.includes("calendly")) {
    return { urgency: "HIGH", topic: "STRATEGY_BOOKING", requiresImmediateEscalation: false, escalationReason: "" };
  }
  if (q.includes("wave check") || q.includes("audit") || q.includes("diagnostic") || q.includes("product") || q.includes("scout") || q.includes("rider") || q.includes("content") || q.includes("cove") || q.includes("aeo") || q.includes("price")) {
    return { urgency: "MEDIUM", topic: "PRODUCT_INQUIRY", requiresImmediateEscalation: false, escalationReason: "" };
  }
  if (q.includes("hour") || q.includes("where") || q.includes("who") || q.includes("website") || q.includes("contact") || q.includes("email")) {
    return { urgency: "LOW", topic: "BUSINESS_INFO", requiresImmediateEscalation: false, escalationReason: "" };
  }
  return { urgency: "MEDIUM", topic: "GENERAL", requiresImmediateEscalation: false, escalationReason: "" };
}

function generateAiSurferResponse(callerName = "Inquirer", question = "") {
  const q = question.toLowerCase();
  const classification = classifyCallerIntent(question);

  const matchedProd = products.find(p =>
    q.includes(p.name.toLowerCase()) ||
    (q.includes("wave check") && p.slug === "wave-check") ||
    (q.includes("wave starter") && p.slug === "wave-starter") ||
    (q.includes("scout") && p.slug === "wave-scout") ||
    (q.includes("rider") && p.slug === "sales-rider") ||
    (q.includes("content") && p.slug === "content-creator") ||
    (q.includes("cove") && p.slug === "customer-care-cove") ||
    (q.includes("aeo") && p.slug === "aeo-blueprint") ||
    (q.includes("architect") && p.slug === "automation-architect") ||
    ((q.includes("big kahuna") || q.includes("tsunami") || q.includes("3997")) && p.slug === "big-kahuna")
  );

  const matchedFaq = faqs.find(f =>
    q.includes(f.category.toLowerCase()) ||
    f.question.toLowerCase().split(" ").some(word => word.length > 4 && q.includes(word))
  );

  let responseText = "";
  if (matchedProd) {
    const priceLabel = matchedProd.price === 0 ? "Free" : `$${matchedProd.price.toFixed(0)}`;
    responseText = `Aloha ${callerName}! Stoked to help you with ${matchedProd.name} (${priceLabel}). ${matchedProd.description} [${matchedProd.surfLevel}]. Check it out at ${matchedProd.directUrl}. I've logged this for our Ocean Tide Drop AI team!`;
  } else if (matchedFaq) {
    responseText = `Aloha ${callerName}! Ocean Tide Drop AI Surfer (otdaisurfer.surf) has got you covered! ${matchedFaq.answer}`;
  } else if (q.includes("github") || q.includes("repo") || q.includes("aisurfer")) {
    responseText = `Aloha ${callerName}! Our official GitHub connected repository is otdaisurfer/AISurfer- (https://github.com/otdaisurfer/AISurfer-), powering https://otdaisurfer.surf!`;
  } else if (q.includes("price") || q.includes("cost") || q.includes("rate")) {
    responseText = `Aloha ${callerName}! Offerings on https://otdaisurfer.surf start with Free AI Wave Check™ ($0), Wave Starter™ sprints at $497 (Stripe checkout), Wave Builder at $1,997, and Big Kahuna™ / Tsunami Growth at $3,997.`;
  } else {
    responseText = `Aloha ${callerName}! Thanks for reaching out to Ocean Tide Drop AI Surfer (https://otdaisurfer.surf). Regarding "${question}", our AI engineering team builds custom workflows, sales assistants, content engines, and answer engine SEO. Logged for crew follow-up!`;
  }

  return {
    responseText,
    urgency: classification.urgency,
    topic: classification.topic,
    requiresImmediateEscalation: classification.requiresImmediateEscalation,
    escalationReason: classification.escalationReason,
    summary: `Caller ${callerName} asked: "${question.slice(0, 80)}...". Kai provided answer & logged for crew review.`
  };
}

function sendJson(res, statusCode, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET,POST,DELETE,OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type"
  });
  res.end(body);
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let raw = "";
    req.on("data", chunk => {
      raw += chunk;
      if (raw.length > 1024 * 1024) {
        reject(new Error("Payload too large"));
      }
    });
    req.on("end", () => {
      if (!raw.trim()) {
        resolve({});
        return;
      }
      try {
        resolve(JSON.parse(raw));
      } catch {
        reject(new Error("Invalid JSON body"));
      }
    });
    req.on("error", reject);
  });
}

function sweepOverdueEscalations() {
  const now = Date.now();
  calls.forEach(c => {
    if (c.status === "PENDING_REVIEW" && now >= c.escalationDeadline && !c.isEscalated) {
      c.status = "ESCALATED";
      c.isEscalated = true;
      c.escalatedAt = now;
      c.escalationNotes = `SLA timeout (${c.escalationAlertMinutes} min) elapsed! Alert dispatched via ${escalationTriggerConfig.alertChannel} to ${escalationTriggerConfig.targetEmail}.`;
    }
  });
}

const server = http.createServer(async (req, res) => {
  try {
    if (req.method === "OPTIONS") {
      res.writeHead(204, {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET,POST,DELETE,OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type"
      });
      res.end();
      return;
    }

    const url = new URL(req.url || "/", `http://${req.headers.host || "localhost"}`);

    if (url.pathname === "/health" && req.method === "GET") {
      return sendJson(res, 200, { status: "ok", runtime: "node", port: PORT });
    }

    if (url.pathname === "/api/calls" && req.method === "GET") {
      sweepOverdueEscalations();
      const statusFilter = url.searchParams.get("status");
      const filtered = statusFilter && statusFilter !== "ALL"
        ? calls.filter(c => statusFilter === "URGENT_VIP" ? c.urgency === "URGENT_VIP" : c.status === statusFilter)
        : calls;
      return sendJson(res, 200, filtered);
    }

    if (url.pathname === "/api/simulate-call" && req.method === "POST") {
      const body = await readJsonBody(req);
      const { callerName = "Ocean Caller", callerNumber = "+1 (555) 019-9283", question = "", durationSeconds = 32 } = body;
      const ai = generateAiSurferResponse(callerName, question);

      const shouldAutoEscalate =
        escalationTriggerConfig.isEnabled &&
        (ai.requiresImmediateEscalation || (escalationTriggerConfig.escalateOnHighUrgency && ai.urgency === "HIGH"));

      const alertMinutes = ai.urgency === "URGENT_VIP" ? 3 : 10;
      const now = Date.now();
      const newCall = {
        id: now,
        callerName,
        callerNumber,
        timestamp: now,
        durationSeconds,
        status: shouldAutoEscalate ? "ESCALATED" : "PENDING_REVIEW",
        urgency: ai.urgency,
        topic: ai.topic,
        customerQuestion: question,
        aiResponse: ai.responseText,
        summary: ai.summary,
        transcription: `Caller: "${question}"\nKai (AI Receptionist): "${ai.responseText}"`,
        escalationAlertMinutes: alertMinutes,
        escalationDeadline: now + alertMinutes * 60 * 1000,
        isEscalated: shouldAutoEscalate,
        escalatedAt: shouldAutoEscalate ? now : 0,
        escalationNotes: shouldAutoEscalate
          ? `${ai.escalationReason || "AI flagged for escalation."} Alert dispatched via ${escalationTriggerConfig.alertChannel} to ${escalationTriggerConfig.targetEmail}.`
          : ""
      };

      calls.unshift(newCall);
      return sendJson(res, 200, newCall);
    }

    if (url.pathname === "/api/mark-reviewed" && req.method === "POST") {
      const { id } = await readJsonBody(req);
      const call = calls.find(c => c.id === id);
      if (!call) return sendJson(res, 404, { error: "Call not found" });
      call.status = "REVIEWED";
      call.isEscalated = false;
      return sendJson(res, 200, { success: true, call });
    }

    if (url.pathname === "/api/mark-pending" && req.method === "POST") {
      const { id } = await readJsonBody(req);
      const call = calls.find(c => c.id === id);
      if (!call) return sendJson(res, 404, { error: "Call not found" });
      call.status = "PENDING_REVIEW";
      call.isEscalated = false;
      call.escalationDeadline = Date.now() + call.escalationAlertMinutes * 60 * 1000;
      return sendJson(res, 200, { success: true, call });
    }

    if ((url.pathname === "/api/escalate" || url.pathname === "/api/trigger-escalation") && req.method === "POST") {
      const { id, reason } = await readJsonBody(req);
      const call = calls.find(c => c.id === id);
      if (!call) return sendJson(res, 404, { error: "Call not found" });
      call.status = "ESCALATED";
      call.isEscalated = true;
      call.escalatedAt = Date.now();
      call.escalationNotes =
        reason ||
        `Escalation triggered via ${escalationTriggerConfig.alertChannel} to ${escalationTriggerConfig.targetEmail}.`;
      return sendJson(res, 200, { success: true, call, config: escalationTriggerConfig });
    }

    if (url.pathname === "/api/delete-call" && req.method === "POST") {
      const { id } = await readJsonBody(req);
      calls = calls.filter(c => c.id !== id);
      return sendJson(res, 200, { success: true });
    }

    if (url.pathname === "/api/escalation-config" && req.method === "GET") {
      return sendJson(res, 200, escalationTriggerConfig);
    }

    if (url.pathname === "/api/escalation-config" && req.method === "POST") {
      const body = await readJsonBody(req);
      escalationTriggerConfig = { ...escalationTriggerConfig, ...body };
      return sendJson(res, 200, { success: true, escalationTriggerConfig });
    }

    if (url.pathname === "/api/escalations" && req.method === "GET") {
      return sendJson(res, 200, escalationDispatches);
    }

    if (url.pathname === "/api/escalations/dispatch" && req.method === "POST") {
      const body = await readJsonBody(req);
      const {
        callId = null,
        callerName = "Ocean Caller",
        callerNumber = "",
        urgency = "URGENT_VIP",
        topic = "HIGH_PRIORITY",
        summary = "",
        customerQuestion = "",
        escalationReason = "AI flagged call for escalation",
        targetEmail = escalationTriggerConfig.targetEmail,
        webhookUrl = escalationTriggerConfig.webhookUrl
      } = body;

      const dispatchRecord = {
        dispatchId: `esc_${Date.now()}`,
        dispatchedAt: new Date().toISOString(),
        callId,
        callerName,
        callerNumber,
        urgency,
        topic,
        summary,
        customerQuestion,
        escalationReason,
        targetEmail,
        webhookForwarded: false,
        webhookStatus: "LOGGED_LOCAL"
      };

      const trimmedHook = (webhookUrl || "").trim();
      if (trimmedHook.startsWith("https://") || trimmedHook.startsWith("http://")) {
        try {
          const slackCompatiblePayload = {
            text: `🚨 *Ocean Tide Drop AI Escalation [${urgency}]*\n*Caller:* ${callerName} (${callerNumber})\n*Topic:* ${topic}\n*Reason:* ${escalationReason}\n*Summary:* ${summary}\n*Crew Email:* ${targetEmail}`,
            event: "ocean_surfer_escalation",
            callId,
            callerName,
            callerNumber,
            urgency,
            topic,
            summary,
            customerQuestion,
            escalationReason,
            targetEmail,
            dispatchedAt: dispatchRecord.dispatchedAt
          };
          const hookRes = await fetch(trimmedHook, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(slackCompatiblePayload)
          });
          dispatchRecord.webhookForwarded = hookRes.ok;
          dispatchRecord.webhookStatus = `HTTP_${hookRes.status}`;
        } catch (hookErr) {
          dispatchRecord.webhookForwarded = false;
          dispatchRecord.webhookStatus = `ERROR: ${hookErr?.message || "Webhook unreachable"}`;
        }
      }

      escalationDispatches.unshift(dispatchRecord);
      if (escalationDispatches.length > 100) escalationDispatches.length = 100;
      return sendJson(res, 200, { success: true, dispatch: dispatchRecord });
    }

    if (url.pathname === "/api/receptionist-query" && req.method === "POST") {
      const { callerName = "Inquirer", question = "" } = await readJsonBody(req);
      const ai = generateAiSurferResponse(callerName, question);
      return sendJson(res, 200, ai);
    }

    if (url.pathname === "/api/products" && req.method === "GET") {
      return sendJson(res, 200, products);
    }

    if (url.pathname === "/api/products" && req.method === "POST") {
      const newProd = await readJsonBody(req);
      newProd.id = Date.now();
      products.push(newProd);
      return sendJson(res, 200, newProd);
    }

    if (url.pathname === "/api/faqs" && req.method === "GET") {
      return sendJson(res, 200, faqs);
    }

    if (url.pathname === "/api/faqs" && req.method === "POST") {
      const newFaq = await readJsonBody(req);
      newFaq.id = Date.now();
      faqs.push(newFaq);
      return sendJson(res, 200, newFaq);
    }

    if (url.pathname === "/api/chatgpt-config" && req.method === "POST") {
      const body = await readJsonBody(req);
      chatGptConfig = { ...chatGptConfig, ...body };
      return sendJson(res, 200, { success: true, chatGptConfig });
    }

    if (url.pathname === "/api/supabase-config" && req.method === "POST") {
      const body = await readJsonBody(req);
      supabaseConfig = { ...supabaseConfig, ...body };
      return sendJson(res, 200, { success: true, supabaseConfig });
    }

    if (url.pathname === "/api/deploy-website" && req.method === "POST") {
      gitHubConfig.lastDeploymentStatus = `Connected & Rebuilt: ${gitHubConfig.repoOwner}/${gitHubConfig.repoName} -> ${gitHubConfig.websiteUrl}`;
      return sendJson(res, 200, { success: true, gitHubConfig });
    }

    // Serve HTML dashboard for root or browser navigation
    res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
    res.end(getAppHtml());
  } catch (err) {
    console.error("Backend request error:", err);
    sendJson(res, 400, { error: err?.message || "Unexpected backend error" });
  }
});

server.on("error", err => {
  console.error("Backend server error:", err.message);
});

server.listen(PORT, () => {
  console.log(`🌊 Ocean Tide Drop AI Surfer Node backend running on http://localhost:${PORT}`);
});

function getAppHtml() {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Ocean Tide Drop AI Surfer — Call Dashboard</title>
  <style>
    :root {
      --ocean-abyss: #03141F;
      --ocean-deep: #062335;
      --ocean-card: #0F3E5C;
      --surf-turquoise: #00E5FF;
      --surf-cyan: #00B4D8;
      --surf-teal: #0077B6;
      --golden-sun: #FFB703;
      --sunset-orange: #FB8500;
      --coral-alert: #FF4D6D;
      --sea-green: #06D6A0;
      --sand-primary: #F1FAEE;
      --sand-secondary: #90E0EF;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { background: var(--ocean-abyss); color: var(--sand-primary); font-family: system-ui, -apple-system, sans-serif; }
    .status-bar { background: #020D14; border-bottom: 1px solid rgba(0,229,255,0.2); padding: 10px 20px; display: flex; justify-content: space-between; align-items: center; font-size: 0.8rem; }
    .container { max-width: 1000px; margin: 0 auto; padding: 24px 16px; }
    .card { background: var(--ocean-deep); border: 1px solid rgba(0,119,182,0.4); border-radius: 14px; padding: 18px; margin-bottom: 16px; }
    .btn { padding: 8px 14px; border-radius: 8px; border: none; font-weight: 700; cursor: pointer; font-size: 0.82rem; }
    .btn-primary { background: var(--surf-turquoise); color: var(--ocean-abyss); }
    .btn-secondary { background: var(--ocean-card); color: var(--sand-secondary); border: 1px solid rgba(0,180,216,0.3); }
    .btn-alert { background: var(--coral-alert); color: #fff; }
    .btn-success { background: var(--sea-green); color: var(--ocean-abyss); }
    .chips { display: flex; gap: 8px; flex-wrap: wrap; margin: 10px 0; }
    .chip.active { background: var(--surf-turquoise); color: var(--ocean-abyss); }
  </style>
</head>
<body>
  <div class="status-bar">
    <div>🟢 <strong>KAI AI RECEPTIONIST</strong> • Guarding <a href="https://otdaisurfer.surf" style="color:var(--surf-turquoise)">otdaisurfer.surf</a> • Repo: <strong>otdaisurfer/AISurfer-</strong></div>
    <div id="statusCounts">Loading telemetry...</div>
  </div>
  <div class="container">
    <h1 style="margin-bottom:14px;">📞 AI Call Dashboard &amp; Escalation Triggers</h1>
    <div class="card">
      <strong>Status Filters</strong>
      <div class="chips">
        <button class="btn btn-secondary chip active" onclick="setStatusFilter('ALL', this)">All</button>
        <button class="btn btn-secondary chip" onclick="setStatusFilter('PENDING_REVIEW', this)">Pending</button>
        <button class="btn btn-secondary chip" onclick="setStatusFilter('ESCALATED', this)">Escalated</button>
        <button class="btn btn-secondary chip" onclick="setStatusFilter('REVIEWED', this)">Reviewed</button>
        <button class="btn btn-secondary chip" onclick="setStatusFilter('URGENT_VIP', this)">Urgent VIP</button>
      </div>
    </div>
    <div id="callsList"></div>
  </div>
  <script>
    let currentStatus = 'ALL';
    async function loadCalls() {
      const res = await fetch('/api/calls?status=' + encodeURIComponent(currentStatus));
      const list = await res.json();
      const pending = list.filter(c => c.status === 'PENDING_REVIEW').length;
      const escalated = list.filter(c => c.status === 'ESCALATED' || c.isEscalated).length;
      document.getElementById('statusCounts').innerText = escalated + ' Escalated • ' + pending + ' Pending';
      document.getElementById('callsList').innerHTML = list.map(c => \`
        <div class="card" style="border-left: 4px solid \${c.status === 'ESCALATED' ? 'var(--coral-alert)' : c.status === 'PENDING_REVIEW' ? 'var(--sunset-orange)' : 'var(--sea-green)'}">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <strong>\${c.callerName} (\${c.callerNumber})</strong>
            <span>\${c.status} • \${c.urgency}</span>
          </div>
          <p style="margin:8px 0;color:var(--sand-secondary)">\${c.summary}</p>
          \${c.escalationNotes ? \`<div style="color:var(--coral-alert);font-size:0.85rem;margin-bottom:8px;">⚠️ \${c.escalationNotes}</div>\` : ''}
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <button class="btn btn-success" onclick="actionCall('/api/mark-reviewed', \${c.id})">Mark Reviewed</button>
            <button class="btn btn-alert" onclick="actionCall('/api/trigger-escalation', \${c.id})">Trigger Escalation Alert</button>
          </div>
        </div>
      \`).join('');
    }
    function setStatusFilter(s, el) {
      currentStatus = s;
      document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
      el.classList.add('active');
      loadCalls();
    }
    async function actionCall(url, id) {
      await fetch(url, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({ id }) });
      loadCalls();
    }
    loadCalls();
  </script>
</body>
</html>`;
}
