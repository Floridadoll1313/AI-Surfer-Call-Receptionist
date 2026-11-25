package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.room.migration.Migration

@Database(
  entities = [CallEntity::class, ProductEntity::class, FaqEntity::class, WaveAuditLeadEntity::class],
  version = 4,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun callDao(): CallDao
  abstract fun productDao(): ProductDao
  abstract fun faqDao(): FaqDao
  abstract fun waveAuditLeadDao(): WaveAuditLeadDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    private val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE calls ADD COLUMN receivingLine TEXT NOT NULL DEFAULT 'PHONE_1'")
      }
    }

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "ocean_tide_surfer_database"
        )
        .addMigrations(MIGRATION_3_4)
        .fallbackToDestructiveMigration()
        .addCallback(DatabaseCallback())
        .build()
        INSTANCE = instance
        instance
      }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          CoroutineScope(Dispatchers.IO).launch {
            populateInitialData(database)
          }
        }
      }

      override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        db.execSQL("UPDATE products SET price = 297.0, surfLevel = 'Stage: Diagnose (\$297)', directUrl = 'https://buy.stripe.com/14A4gz2pt0WSfPs1tt4gg0f' WHERE slug = 'ai-opportunity-report' AND price = 0.0 AND surfLevel = 'Stage: Diagnose (Free Tier)'")
        db.execSQL("UPDATE products SET price = 497.0, surfLevel = 'Stage: Plan (\$497)' WHERE slug = 'aeo-blueprint' AND price = 1997.0 AND surfLevel = 'Stage: Plan (Wave Builder)'")
        INSTANCE?.let { database ->
          CoroutineScope(Dispatchers.IO).launch {
            upgradeDataIfLegacy(database)
          }
        }
      }
    }

    suspend fun populateInitialData(database: AppDatabase) {
      val productDao = database.productDao()
      if (productDao.getCount() == 0) {
        productDao.insertProducts(initialProducts)
      }

      val faqDao = database.faqDao()
      if (faqDao.getCount() == 0) {
        faqDao.insertFaqs(initialFaqs)
      }

    }

    suspend fun upgradeDataIfLegacy(database: AppDatabase) {
      val productDao = database.productDao()
      // If legacy surfboard placeholder exists, overwrite with official products from otdaisurfer.surf
      if (productDao.findOldSurfboardSample() != null || productDao.getCount() == 0) {
        productDao.deleteAllProducts()
        productDao.insertProducts(initialProducts)
        val faqDao = database.faqDao()
        faqDao.deleteAllFaqs()
        faqDao.insertFaqs(initialFaqs)
      }

    }

    suspend fun forceSyncOfficialProducts(database: AppDatabase) {
      val productDao = database.productDao()
      productDao.deleteAllProducts()
      productDao.insertProducts(initialProducts)
      val faqDao = database.faqDao()
      faqDao.deleteAllFaqs()
      faqDao.insertFaqs(initialFaqs)
    }

    val initialProducts = listOf(
      ProductEntity(
        id = 1,
        name = "Free AI Wave Check™",
        price = 0.0,
        category = "Business AI Opportunity Check",
        description = "Answer five quick questions to see your strongest AI opportunity, practical first move, and the implementation path that fits your business.",
        inStock = true,
        popularBadge = "Start Here",
        surfLevel = "Stage: Discover (Free)",
        ctaText = "Start Free Wave Check",
        directUrl = "https://otdaisurfer.surf/wave-check",
        slug = "wave-check"
      ),
      ProductEntity(
        id = 2,
        name = "AI Opportunity Report™",
        price = 297.0,
        category = "Business AI Strategy",
        description = "Turn scattered AI possibilities into a prioritized list of the opportunities most likely to create measurable business value.",
        inStock = true,
        popularBadge = "Core Diagnosis",
        surfLevel = "Stage: Diagnose (\$297)",
        ctaText = "Explore Opportunities",
        directUrl = "https://buy.stripe.com/14A4gz2pt0WSfPs1tt4gg0f",
        slug = "ai-opportunity-report"
      ),
      ProductEntity(
        id = 3,
        name = "Wave Starter™ Sprint",
        price = 497.0,
        category = "AI Implementation Sprint",
        description = "A focused implementation sprint to turn your clearest AI opportunity into a working business system. Includes build & deploy of Wave Scout, Sales Rider, Content Creator, or Customer Care Cove.",
        inStock = true,
        popularBadge = "Best Seller",
        surfLevel = "Stage: Implement ($497)",
        ctaText = "Buy Wave Starter ($497)",
        directUrl = "https://buy.stripe.com/aFa8wP7JN3500Uy1tt4gg0b",
        slug = "wave-starter"
      ),
      ProductEntity(
        id = 4,
        name = "Wave Scout™",
        price = 497.0,
        category = "Lead Generation AI",
        description = "Identify prospects, research buying signals, and organize opportunities so your sales team spends less time hunting and more time closing.",
        inStock = true,
        popularBadge = "Lead Generation",
        surfLevel = "Stage: Implement (Wave Starter)",
        ctaText = "Ride with Wave Scout",
        directUrl = "https://otdaisurfer.surf/members/products/wave-scout",
        slug = "wave-scout"
      ),
      ProductEntity(
        id = 5,
        name = "Sales Rider™",
        price = 497.0,
        category = "AI Sales Assistant",
        description = "Turn leads into conversations and conversations into opportunities with an always-on sales assistant that responds instantly and organizes follow-up.",
        inStock = true,
        popularBadge = "Sales Conversion",
        surfLevel = "Stage: Implement (Wave Starter)",
        ctaText = "Meet Sales Rider",
        directUrl = "https://otdaisurfer.surf/members/products/sales-rider",
        slug = "sales-rider"
      ),
      ProductEntity(
        id = 6,
        name = "Content Creator™",
        price = 497.0,
        category = "AI Marketing Engine",
        description = "Generate strategic social posts, emails, blogs, campaigns, offers, and marketing assets while keeping your message and tone strictly aligned.",
        inStock = true,
        popularBadge = "Marketing Engine",
        surfLevel = "Stage: Implement (Wave Starter)",
        ctaText = "Create with Content Creator",
        directUrl = "https://otdaisurfer.surf/members/products/content-creator",
        slug = "content-creator"
      ),
      ProductEntity(
        id = 7,
        name = "Customer Care Cove™",
        price = 497.0,
        category = "AI Customer Support",
        description = "Automate helpful customer support while keeping the human touch. Creates faster, more consistent answers for routine customer questions and service requests.",
        inStock = true,
        popularBadge = "Customer Support",
        surfLevel = "Stage: Implement (Wave Starter)",
        ctaText = "Design Support Flow",
        directUrl = "https://otdaisurfer.surf/members/products/customer-care-cove",
        slug = "customer-care-cove"
      ),
      ProductEntity(
        id = 8,
        name = "AEO Blueprint™",
        price = 497.0,
        category = "AI Search Strategy",
        description = "Build a practical roadmap for becoming more visible, understandable, and authoritative across AI-powered search engines (ChatGPT, Perplexity, Gemini, Claude).",
        inStock = true,
        popularBadge = "Answer Engine SEO",
        surfLevel = "Stage: Plan (\$497)",
        ctaText = "Build the Blueprint",
        directUrl = "https://otdaisurfer.surf/members/products/aeo-blueprint",
        slug = "aeo-blueprint"
      ),
      ProductEntity(
        id = 9,
        name = "Automation Blueprint™",
        price = 1997.0,
        category = "AI Workflow Strategy",
        description = "Map repetitive work into AI-powered workflows that reduce manual effort, connect your business tools, and make operations scalable.",
        inStock = true,
        popularBadge = "Workflow Strategy",
        surfLevel = "Stage: Plan (Wave Builder)",
        ctaText = "Map Your Workflows",
        directUrl = "https://otdaisurfer.surf/members/products/automation-blueprint",
        slug = "automation-blueprint"
      ),
      ProductEntity(
        id = 10,
        name = "Automation Architect™",
        price = 1997.0,
        category = "AI Business Automation",
        description = "Connect processes, tools, data, and AI agents into practical automated workflows that eliminate repetitive handoffs and scale your business.",
        inStock = true,
        popularBadge = "Ops Automation",
        surfLevel = "Stage: Implement (Wave Builder)",
        ctaText = "Automate the Work",
        directUrl = "https://otdaisurfer.surf/members/products/automation-architect",
        slug = "automation-architect"
      ),
      ProductEntity(
        id = 11,
        name = "Wave Builder Package",
        price = 1997.0,
        category = "Growth System Package",
        description = "A larger build for businesses ready to connect AI visibility, lead flow, follow-up, and automation into one practical, unified growth system.",
        inStock = true,
        popularBadge = "Growth System",
        surfLevel = "Stage: Plan & Build ($1,997)",
        ctaText = "Request Strategy Call",
        directUrl = "https://calendly.com/oceantidedrop/new-meeting",
        slug = "wave-builder"
      ),
      ProductEntity(
        id = 12,
        name = "Big Kahuna™ / Tsunami Growth",
        price = 3997.0,
        category = "AI Growth Architect",
        description = "Bring strategy, visibility, automation, AI agents, workflows, and growth opportunities together into one complete, high-touch AI transformation experience.",
        inStock = true,
        popularBadge = "VIP Transformation",
        surfLevel = "Stage: Transform ($3,997)",
        ctaText = "Talk With AI Surfer",
        directUrl = "https://calendly.com/oceantidedrop/new-meeting",
        slug = "big-kahuna"
      )
    )

    val initialFaqs = listOf(
      FaqEntity(
        id = 1,
        category = "What We Do",
        question = "What is Ocean Tide Drop AI Surfer and how do you help businesses?",
        answer = "Ocean Tide Drop AI Surfer ('Ride the Wave 🌊 Grow with AI' - https://otdaisurfer.surf) helps businesses identify high-leverage AI opportunities and implements custom AI sales assistants, lead generators, content engines, support agents, answer-engine SEO, and end-to-end automation systems.",
        tags = "about, what we do, services, agency, ai surfer, overview"
      ),
      FaqEntity(
        id = 2,
        category = "Free Diagnostic",
        question = "How does the Free AI Wave Check™ work?",
        answer = "The Free AI Wave Check is a 5-question diagnostic at https://otdaisurfer.surf/wave-check that pinpoints your biggest bottleneck (lead response, consistent content, repetitive operations) and gives you a practical first move with a recommended implementation path.",
        tags = "wave check, free audit, diagnosis, start, assessment"
      ),
      FaqEntity(
        id = 3,
        category = "Pricing & Packages",
        question = "What are your core packages and pricing?",
        answer = "We offer transparent pricing without stale tiers: Wave Starter ($497 focused implementation sprint with direct Stripe checkout for Wave Scout, Sales Rider, Content Creator, or Customer Care Cove), Wave Builder ($1,997 for multi-tool integration, AEO Blueprint & Automation Architect), and Tsunami Growth / Big Kahuna ($3,997 for complete multi-agent transformation).",
        tags = "pricing, cost, price, packages, $497, $1997, $3997, tiers"
      ),
      FaqEntity(
        id = 4,
        category = "Wave Starter",
        question = "What is included in the $497 Wave Starter™ Sprint?",
        answer = "Wave Starter ($497) is a rapid, focused build to deploy your clearest AI win: either Wave Scout (lead generation AI), Sales Rider (AI sales assistant), Content Creator (AI marketing engine), or Customer Care Cove (automated customer care). You can purchase directly via Stripe at https://buy.stripe.com/aFa8wP7JN3500Uy1tt4gg0b.",
        tags = "wave starter, $497, sprint, buy, stripe, sales rider, wave scout"
      ),
      FaqEntity(
        id = 5,
        category = "Strategy & Booking",
        question = "How can I book an AI Strategy Call or get in touch?",
        answer = "You can schedule a strategy session directly via Calendly at https://calendly.com/oceantidedrop/new-meeting, email us at oceantidedropservice@gmail.com, or explore live interactive tools on https://otdaisurfer.surf.",
        tags = "book, call, contact, calendly, email, strategy, meeting"
      ),
      FaqEntity(
        id = 6,
        category = "AI Receptionist & SLA",
        question = "How does the AI Phone Receptionist & Call Escalation work?",
        answer = "Our AI Receptionist ('Kai') fields cell phone inquiries 24/7, answers questions about our AI catalog and packages, qualifies caller interest, and logs every call. Any high-priority inquiry triggers a prompt notification and email escalation to oceantidedropservice@gmail.com within our 3 to 10 minute SLA.",
        tags = "receptionist, phone, call, kai, hours, sla, escalation, emergency"
      )
    )
  }
}
