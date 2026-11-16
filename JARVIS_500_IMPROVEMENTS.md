# JARVIS — 500 конкретных улучшений

Это инженерный backlog, а не заявление, что все пункты уже реализованы. Каждый пункт требует реализации, теста и проверки на целевом Android-устройстве. Пункты про медицинскую тематику ограничены справочной помощью, а управление устройством — разрешениями Android.

## Language and Russian morphology
1. case-aware parsing
2. gender and number agreement
3. verb aspect recognition
4. tense normalization
5. imperative detection
6. colloquial speech normalization
7. speech disfluency cleanup
8. ё/е normalization
9. homophone disambiguation
10. spelling correction
11. punctuation correction
12. stress-aware pronunciation hints
13. declension generation
14. conjugation generation
15. phrase boundary detection
16. multi-clause parsing
17. negation scope
18. question type classification
19. politeness and register
20. idiom recognition
21. slang handling
22. abbreviation expansion
23. transliteration handling
24. mixed Russian-English parsing
25. contextual coreference

## English and multilingual
26. English morphology
27. English spelling correction
28. English punctuation
29. language auto-detection
30. code-switch parsing
31. cross-language retrieval
32. translation with context
33. idiom translation
34. technical terminology mapping
35. unit locale handling
36. date locale handling
37. number words parsing
38. speech locale switching
39. bilingual chat memory
40. language preference per chat
41. language fallback policy
42. proper noun preservation
43. transliteration search
44. pronunciation dictionary
45. multilingual tokenizer
46. Unicode normalization
47. Cyrillic-Latin lookalike detection
48. script-aware spellcheck
49. locale-specific formatting
50. translation quality tests

## Reasoning and dialogue
51. intent decomposition
52. multi-step planning
53. subgoal tracking
54. assumption listing
55. uncertainty estimation
56. contradiction detection
57. self-consistency checks
58. answer relevance scoring
59. clarifying question policy
60. context window summarization
61. dialogue state tracking
62. topic transition detection
63. reference resolution
64. follow-up understanding
65. alternative generation
66. pros-cons analysis
67. causal reasoning
68. constraint satisfaction
69. counterexample search
70. proof outline generation
71. step verification
72. calibrated confidence
73. answer abstention
74. non-repetitive response generation
75. response style adaptation

## Math and STEM
76. arithmetic parser
77. operator precedence
78. fraction arithmetic
79. decimal arithmetic
80. equation solving
81. linear systems
82. quadratic equations
83. inequality solving
84. percentages
85. ratios and proportions
86. unit conversion
87. dimensional analysis
88. geometry formulas
89. coordinate geometry
90. trigonometry basics
91. statistics summaries
92. probability basics
93. function evaluation
94. sequence patterns
95. symbolic simplification
96. step-by-step derivations
97. answer substitution checks
98. sign error detection
99. domain restriction checks
100. math regression corpus

## Russian language education
101. parts of speech analysis
102. sentence parsing
103. subject-predicate detection
104. morphological tagging
105. spelling rules database
106. unstressed vowel lookup
107. consonant alternation rules
108. prefix spelling rules
109. н/нн rule engine
110. не/ни disambiguation
111. punctuation rule engine
112. direct speech formatting
113. participial phrase parsing
114. gerund phrase parsing
115. complex sentence analysis
116. word formation analysis
117. synonym suggestions
118. antonym suggestions
119. paronym distinction
120. lexical meaning lookup
121. phraseological units
122. text style analysis
123. essay structure feedback
124. grammar exercise generator
125. school-level adaptation

## Knowledge graph
126. typed concept records
127. entity records
128. relation records
129. prerequisite links
130. cause-effect edges
131. part-whole edges
132. contrast edges
133. definition provenance
134. source licensing metadata
135. source freshness dates
136. confidence scores
137. review status
138. duplicate entity detection
139. alias resolution
140. full-text indexing
141. semantic retrieval interface
142. keyword retrieval fallback
143. knowledge versioning
144. incremental import
145. conflict tracking
146. source comparison
147. citation generation
148. knowledge export
149. offline retrieval tests
150. database integrity checks

## Science and general knowledge
151. physics concepts
152. chemistry concepts
153. biology concepts
154. astronomy concepts
155. geology concepts
156. ecology concepts
157. computer science concepts
158. mathematics concepts
159. history timelines
160. geography entities
161. civics concepts
162. economics concepts
163. engineering principles
164. scientific method
165. measurement standards
166. periodic table data
167. chemical formula parser
168. physical constants
169. biology taxonomy
170. solar system facts
171. energy transformations
172. weather concepts
173. materials properties
174. scientific misconception checks
175. age-appropriate explanations

## Programming assistant
176. code explanation
177. project scaffolding
178. multi-file generation
179. syntax diagnostics
180. compiler error parsing
181. stack trace analysis
182. bug localization
183. refactoring proposals
184. unit test generation
185. integration test planning
186. test execution reporting
187. API documentation lookup
188. dependency analysis
189. version compatibility checks
190. secure coding review
191. secret detection
192. input validation review
193. performance profiling guidance
194. memory leak analysis
195. concurrency review
196. Git diff generation
197. patch application preview
198. rollback support
199. code formatting
200. build reproducibility

## Android device control
201. app package resolution
202. launcher intent verification
203. deep link handling
204. explicit browser opening
205. permission rationale UI
206. runtime permission state tracking
207. notification permission flow
208. camera permission flow
209. microphone permission flow
210. overlay permission flow
211. Bluetooth permission flow
212. Wi-Fi settings handoff
213. VPN settings handoff
214. alarm scheduling
215. timer scheduling
216. notification creation
217. media session inspection
218. torch control verification
219. battery state reading
220. storage state reading
221. network state reading
222. foreground service policy
223. Android version capability map
224. action result receipts
225. permission denial recovery

## Media and video integrations
226. YouTube app detection
227. YouTube search deep links
228. YouTube video ID resolution
229. VK app detection
230. VK Video deep links
231. media title extraction
232. artist disambiguation
233. track exact-match ranking
234. playlist selection
235. recommendation request parsing
236. personalized feed capability detection
237. account authorization status
238. playback start verification
239. playback pause verification
240. resume-versus-new-track distinction
241. media session ownership
242. unsupported service fallback
243. search result confirmation
244. video age restriction handling
245. region availability reporting
246. link safety validation
247. duplicate result suppression
248. user source preference
249. media command history
250. media integration regression tests

## Chats and memory
251. new chat creation
252. chat title generation
253. chat rename
254. chat deletion confirmation
255. chat search
256. message search
257. per-chat context isolation
258. conversation persistence
259. crash-safe writes
260. chat export
261. chat import
262. pinned conversations
263. saved answers
264. user-approved memory
265. memory edit controls
266. memory deletion workflow
267. memory provenance
268. memory relevance ranking
269. memory expiry policy
270. private chat mode
271. attachment indexing
272. conversation summarization
273. cross-chat search opt-in
274. database migration tests
275. memory backup and restore

## Interface and accessibility
276. modern sidebar navigation
277. collapsible sidebar
278. chat list virtualization
279. message streaming UI
280. stop generation control
281. regenerate answer control
282. edit prompt control
283. copy message action
284. code block rendering
285. syntax highlighting
286. markdown rendering
287. math rendering
288. image attachment picker
289. file attachment picker
290. voice input affordance
291. voice output controls
292. loading state clarity
293. error state clarity
294. action status receipts
295. dark theme
296. light theme
297. dynamic font scaling
298. screen reader labels
299. landscape layout
300. touch target sizing

## Voice and audio
301. five voice profile selection
302. available voice enumeration
303. voice sample preview
304. persisted voice preference
305. speech rate control
306. speech pitch control
307. TTS language fallback
308. speech recognition lifecycle
309. partial transcription handling
310. noise-tolerant command cleanup
311. wake phrase opt-in
312. barge-in interruption
313. speak only final answer
314. avoid duplicate TTS
315. audio focus handling
316. media playback coexistence
317. Bluetooth headset support
318. voice activity timeout
319. speech retry policy
320. recognition confidence handling
321. offline voice capability detection
322. voice latency measurement
323. pronunciation exceptions
324. voice regression suite
325. user mute control

## Vision and files
326. camera capture flow
327. gallery image import
328. image metadata safety
329. on-device image labeling
330. OCR integration
331. document text extraction
332. PDF page parsing
333. table extraction
334. chart interpretation
335. image question routing
336. file type detection
337. file size limits
338. malware scan handoff
339. archive listing safety
340. attachment previews
341. temporary file cleanup
342. storage permission minimization
343. private file handling
344. image cache limits
345. vision confidence display
346. OCR language selection
347. document search index
348. file citation anchors
349. corrupt file recovery
350. vision regression tests

## Security and privacy
351. least privilege permissions
352. no hidden microphone capture
353. no hidden camera capture
354. no silent file upload
355. network request inventory
356. TLS-only endpoints
357. WebView safe browsing
358. URL allowlist for privileged actions
359. deep link validation
360. input sanitization
361. SQL parameterization
362. secure preference storage
363. sensitive log redaction
364. crash report privacy
365. dependency vulnerability scan
366. SBOM generation
367. APK signature verification
368. backup exclusion policy
369. clipboard disclosure
370. destructive action confirmation
371. account token encryption
372. token expiry handling
373. permission revocation handling
374. threat model documentation
375. security regression tests

## Performance and thermal safety
376. startup time benchmark
377. cold start profiling
378. memory peak benchmark
379. low-memory mode
380. thermal status monitoring
381. thermal slowdown policy
382. critical heat animation pause
383. background polling reduction
384. wake lock minimization
385. battery usage benchmark
386. CPU budget per subsystem
387. GPU overdraw inspection
388. frame time measurement
389. animation frame cap
390. cache size caps
391. bounded conversation history
392. database query profiling
393. lazy module initialization
394. model context length cap
395. model thread count cap
396. inference cancellation
397. idle model unload
398. low battery mode
399. device-specific performance profile
400. thermal regression tests

## Testing and reliability
401. unit test coverage
402. intent routing corpus
403. Russian spelling corpus
404. English spelling corpus
405. math answer corpus
406. media intent corpus
407. permission flow tests
408. chat persistence tests
409. database migration tests
410. UI navigation tests
411. accessibility tests
412. offline mode tests
413. network failure tests
414. timeout handling tests
415. retry backoff tests
416. crash reproduction harness
417. ANR monitoring
418. memory leak tests
419. thermal simulation tests
420. APK install smoke test
421. APK signature check
422. lint gate
423. dependency lock checks
424. release artifact checksum
425. device acceptance checklist

## Web and research
426. explicit search intent detection
427. search query rewriting
428. multi-source retrieval
429. source credibility ranking
430. date freshness ranking
431. duplicate source removal
432. claim-source mapping
433. citation formatting
434. page extraction
435. safe redirect handling
436. download confirmation
437. search cancellation
438. offline answer distinction
439. browser navigation history
440. bookmark management
441. domain permissions
442. cookie isolation policy
443. private browsing mode
444. site error recovery
445. network timeout handling
446. search result summarization
447. conflicting source reporting
448. source quote length control
449. research notes export
450. web integration tests

## Health and safety knowledge
451. medical terminology glossary
452. symptom education boundaries
453. emergency warning sign references
454. clinician question preparation
455. medicine name disambiguation
456. drug interaction caution routing
457. no diagnosis policy
458. no prescription generation
459. source date tracking
460. health source credibility
461. uncertainty wording
462. age-appropriate explanations
463. mental health support boundaries
464. first aid source verification
465. poison control routing by region
466. medical misinformation checks
467. units and reference ranges caution
468. lab term explanation
469. appointment summary drafting
470. privacy-first health memory
471. health mode opt-in
472. high-impact response review
473. medical answer citations
474. unsafe advice refusal tests
475. health regression corpus

## Product governance and maintainability
476. modular package boundaries
477. typed action contracts
478. dependency injection
479. structured event logging
480. feature flags
481. configuration validation
482. schema migration strategy
483. API compatibility layer
484. architecture decision records
485. technical debt register
486. release notes automation
487. semantic versioning
488. contribution guide
489. code ownership map
490. documentation generation
491. build cache strategy
492. CI parallelization
493. reproducible build environment
494. license compliance audit
495. third-party notices
496. feature usage metrics opt-in
497. crash-free session metric
498. quality score dashboard
499. roadmap traceability
500. definition of done per feature


**Итого:** 500 улучшений в 20 направлениях.

## Правило приёмки
Пункт считается готовым только при наличии реализации, автоматического теста или документированного ручного теста на устройстве, обработки отказов и оценки влияния на память, батарею и температуру. Увеличение количества пунктов или строк кода не считается ростом интеллекта.
