package com.example.data.service

import com.example.data.model.AIChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object AIEducationService {

    private val disclaimerText = "⚠️ MEDICAL DISCLAIMER: MediSync AI Assistant is an educational tool. It does NOT provide medical diagnoses, alter prescriptions, or replace direct consultation with your physician or pharmacist. Never double up on missed doses without medical approval."

    fun getInitialGreeting(): AIChatMessage {
        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        return AIChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            text = "Hello! I am your MediSync AI Educational Assistant. I can help explain medication instructions, common food interactions, what to do if a dose was missed, and summarize adherence reports.\n\n$disclaimerText",
            timestamp = timeStr,
            isMedicalDisclaimer = true
        )
    }

    fun processQuery(userText: String): AIChatMessage {
        val query = userText.trim().lowercase()
        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        val reply = when {
            query.contains("miss") || query.contains("forgot") -> {
                "💊 **Missed Dose Guidance (Educational):**\n\n" +
                "1. **Never Take a Double Dose:** As a general rule, never take two doses at once to make up for a missed dose unless specifically directed by your prescriber.\n" +
                "2. **Timing Window:** If you remember within 1–2 hours of your scheduled time, taking it immediately is usually standard. If it is already close to your next scheduled dose, skip the missed dose and resume your regular schedule.\n" +
                "3. **Critical Medications:** For cardiac medications (e.g., Lisinopril, Beta-blockers) or Insulin, consult your caregiver or call your pharmacy helpline directly.\n\n" +
                disclaimerText
            }
            query.contains("food") || query.contains("eat") || query.contains("meal") -> {
                "🍎 **Medication & Food Interactions:**\n\n" +
                "• **Metformin:** Always take with or immediately after meals to reduce gastrointestinal irritation and stomach upset.\n" +
                "• **Lisinopril / ACE Inhibitors:** Can generally be taken with or without food. Avoid salt substitutes high in potassium without physician review.\n" +
                "• **Atorvastatin (Statins):** Avoid consuming large quantities of grapefruit or grapefruit juice, which can increase drug concentration in your bloodstream.\n" +
                "• **Levothyroxine:** Must be taken on an empty stomach with a full glass of water, at least 30–60 minutes before breakfast.\n\n" +
                disclaimerText
            }
            query.contains("adherence") || query.contains("report") || query.contains("score") -> {
                "📊 **Adherence Explanation:**\n\n" +
                "Your current medication adherence score is calculated based on doses taken on-time versus scheduled doses. Maintaining an adherence score above 85% is clinically associated with significantly better outcomes for chronic conditions like hypertension and diabetes.\n" +
                "Tip: You can export a comprehensive 30-day compliance report from the Agency Dashboard or Reports tab for your doctor visit."
            }
            query.contains("lisinopril") -> {
                "🩺 **Lisinopril Overview:**\n\n" +
                "• **Class:** ACE (Angiotensin-Converting Enzyme) Inhibitor\n" +
                "• **Common Uses:** High blood pressure (hypertension) and heart failure management.\n" +
                "• **Key Advice:** Take at the same time each day. May cause a mild dry cough in some individuals. If you experience dizziness when standing up, rise slowly and notify your caregiver.\n\n" +
                disclaimerText
            }
            query.contains("metformin") -> {
                "🩺 **Metformin Overview:**\n\n" +
                "• **Class:** Biguanide anti-diabetic agent\n" +
                "• **Common Uses:** Improves blood glucose control in Type 2 Diabetes.\n" +
                "• **Key Advice:** Take with food to minimize stomach discomfort. Stay well hydrated. Avoid excessive alcohol intake.\n\n" +
                disclaimerText
            }
            query.contains("side effect") || query.contains("dizzy") || query.contains("nausea") -> {
                "⚠️ **Recognizing Adverse Reactions:**\n\n" +
                "Mild side effects can sometimes occur when starting or adjusting doses. However, if you experience:\n" +
                "• Swelling of the lips, tongue, face, or throat\n" +
                "• Severe dizziness or fainting\n" +
                "• Unexplained rash or hives\n\n" +
                "Please contact emergency services (911) or your healthcare provider immediately. Tap the red 'Emergency Call' button on your Home screen to connect with your primary caregiver."
            }
            else -> {
                "Thank you for your question about '${userText}'.\n\n" +
                "In MediSync AI, all medication schedules and instructions (such as route, food requirements, and dose times) are verified against your healthcare provider's registered orders.\n\n" +
                "You can ask me questions such as:\n" +
                "• 'What should I do if I miss a dose of Lisinopril?'\n" +
                "• 'Should Metformin be taken with food?'\n" +
                "• 'What foods interact with Statins?'\n" +
                "• 'How is my adherence score calculated?'\n\n" +
                disclaimerText
            }
        }

        return AIChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            text = reply,
            timestamp = timeStr,
            isMedicalDisclaimer = true
        )
    }
}
