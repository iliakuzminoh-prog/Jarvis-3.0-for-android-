# JARVIS Next-Generation Product Specification

Status: engineering specification. Items are requirements, not claims that they are already implemented.

## Product contract
JARVIS is a personal assistant with four cooperating systems:
1. A language model for open-ended reasoning, writing, explanation, planning and code generation.
2. A structured knowledge system for verified facts, concepts, relations, procedures, worked examples and references.
3. A deterministic device-action system with permission gates, execution receipts and rollback/confirmation where possible.
4. A conversation workspace with multiple persistent chats, search, history, attachments and a collapsible left navigation rail.

A phrase matcher or a large list of canned answers is not an AI model. It must never be described as one.

## Required user experience
- Standard AI chat layout: collapsible left sidebar; new chat; persistent chat history; rename/delete/search chats; main conversation; composer with text, microphone, image/file attachment; send/stop controls.
- Sidebar sections: Chats, New chat, Saved items, Tools, Settings. A separate user-managed Websites area stores bookmarks and opens them in the in-app browser.
- Conversations have independent message history and context. Starting a new chat must not leak the previous chat's context.
- Answers must be generated from the request and context. Do not rotate canned replies to fake variation.
- Show when an answer is based on local knowledge, model reasoning, web sources, or an executed device action.
- Stream model output where the runtime supports it; allow stop/regenerate/edit-and-resubmit/copy.
- Accessibility: keyboard-safe layout, large touch targets, dynamic text sizing, screen-reader labels, landscape support.

## Language and reasoning
- Russian and English input/output, mixed-language utterances, morphology, cases, inflections, colloquial forms, speech recognition normalization and spelling correction.
- Intent classification must precede answer generation. Device commands must never be answered as unrelated knowledge questions.
- A spelling request such as "Как правильно написать по-русски женидьба?" must identify the task as orthography, suggest "женитьба", explain the spelling, and give an example.
- For uncertain spelling, ask for a sentence or report uncertainty; never invent a rule.
- Open-ended reasoning, summarization, translation, comparison, planning, text generation and multi-step task decomposition.
- Math engine with expression parsing, equations, units, fractions, percentages, geometry and step-by-step verification.
- Programming assistant: explain, generate, refactor, debug, review, write tests, inspect logs and produce complete files/patches. Generated code must be labelled untested until actually built/run. Never execute arbitrary generated shell/code without explicit approval and a sandbox.
- Health/medical mode is informational only: explain terminology and general evidence, help prepare questions for a clinician, and identify emergency warning signs. It must not diagnose, prescribe, change medication, or claim to replace a doctor.

## Knowledge system: not a word list
Use a versioned local database with typed records:
- Concept: definition, aliases, domain, level, prerequisites, examples, misconceptions.
- Entity: name, attributes, identifiers, synonyms and references.
- Relation: typed edges such as part-of, causes, depends-on, contrasts-with, derived-from.
- Procedure: preconditions, ordered steps, expected result, failure cases and safety notes.
- WorkedExample: prompt, solution steps, final answer, verification method and difficulty.
- Formula: expression, variable definitions, units, assumptions, domain restrictions and derivation.
- CodeExample: language, task, source, code, tests, expected output, version constraints and security notes.
- SpellingEntry: canonical form, common errors, grammatical forms, rule, examples and confidence.
- Source: title, publisher, URL/identifier, licence, date checked and exact claim supported.
- ConversationMemory: user-approved preferences and project facts, separated from chat transcript and searchable/deletable.

Every factual entry must have provenance, version, confidence and review status. Do not bulk-fill the database with fabricated text. Import only legally usable sources; store attribution and licence. Index by full text and aliases; use relation traversal and retrieval ranking, not substring matching alone.

## Model and hardware
- The model is the reasoning engine; the knowledge database is grounding and retrieval. Neither replaces the other.
- Provide a LocalModelProvider interface and a separately downloadable quantized model/runtime. Do not bundle a multi-gigabyte model inside a small APK.
- A 2 GB RAM budget is not enough to promise a large assistant comparable to cloud systems. Android OS, UI, speech and model runtime all need memory. Benchmark on the actual target device before choosing model size/context length.
- If a smaller on-device model cannot answer reliably, state its limitation. No hidden server calls: network use must be explicit and visible.

## Device actions and integrations
Every action follows: parse intent -> resolve target -> check permission -> show confirmation for consequential actions -> execute -> verify observable result -> return receipt.
- Open app/site, explicit web search, YouTube/VK search and media selection.
- Distinguish "open recommendations" from "choose and play one item from my personalized recommendations". The latter requires supported account integration and user authorization; merely opening the app is not success.
- Song request with title must search/select that exact title. Generic "play music" may ask for a source/preference; never claim a named song started when a prior paused session merely resumed.
- Alarm/timer/notification actions must use Android APIs and report the scheduled identifier/time.
- Browser actions must use a real WebView or external intent and return actual navigation success/failure.
- VPN, Bluetooth, Wi-Fi and system settings are constrained by Android security APIs. Use official settings or approved service integrations; do not pretend arbitrary apps can be controlled.
- Health, messaging, file access, camera and notifications each require their own permission only when the feature is invoked.

## Safety and trust
- Never say "done", "playing", "sent", "saved" or "enabled" until the system verifies the action.
- If verification is unavailable, say exactly what was opened/requested and what remains unverified.
- Keep private data local by default. No silent upload of contacts, files, microphone audio, notifications or chat history.
- Medical, financial and other high-impact outputs need appropriate uncertainty, sources and human oversight.
- Generated commands are proposals until user-approved; destructive actions require a clear confirmation.

## Release acceptance tests
1. Orthography: typo example produces correct form, rule and example; casual "как дела" does not trigger spellcheck.
2. Conversation: "что такое фотосинтез?" gets a subject answer, never a generic greeting or browser launch.
3. Media: YouTube/VK recommendations are routed to the correct service; title-based song request preserves exact title; no false playback success.
4. Math: tests include multi-operation precedence, parentheses, fractions, negative values, decimals, equations and invalid input.
5. Chats: create, rename, search, delete, reopen after process death; chat contexts remain isolated.
6. Permissions: each permission is requested at feature use; denial is handled without crashing or claiming success.
7. Browser: open a real URL, navigate back/forward, report failures, preserve user-selected bookmarks.
8. Programming: generate a source file plus tests; compile it in CI; show actual diagnostics.
9. Knowledge: retrieve concepts, relations, procedures, examples and source provenance, not only matching words.
10. Device benchmark: cold start, memory peak, response latency, battery and offline behavior measured on the target phone.

## Current known gap
The existing application is a native Android Activity with a small rule-based knowledge base, local command handlers and a floating overlay. It is not yet the requested full chat product, general-purpose LLM, comprehensive knowledge graph or verified multi-service automation system. This specification prevents future progress reports from presenting planned work as completed.
