# StudyHub --- Study Buddy AI

## Comprehensive Product Enhancement & Implementation Master Prompt

**Purpose:** Upgrade the existing Study Buddy experience into a
reliable, adaptive, interactive, premium-quality AI learning companion.
This document is an implementation brief for an AI coding agent working
inside the existing StudyHub codebase.

------------------------------------------------------------------------

# 0. Mission, Scope, and Non-Negotiable Rules

You are acting as a senior full-stack engineer, AI product architect,
educational technology specialist, UX designer, accessibility reviewer,
and QA engineer.

Your task is to inspect and improve the **existing Study Buddy feature
inside StudyHub**. Do not create a separate demo, a standalone
replacement application, or a disconnected prototype. Implement
improvements in the current project using its existing architecture and
design system wherever possible.

StudyHub is already a **universal education platform**. Do not redesign
the platform around one academic group, examination, country, or
subject. Study Buddy must work for learners across subjects, academic
levels, curricula, and goals.

The current Study Buddy interface already has a promising premium
direction: a chat-based experience, teal accents, a friendly assistant
identity, rounded response surfaces, structured information cards,
suggested follow-ups, study-planning UI, and interactive knowledge
checks. Preserve and refine this direction rather than replacing it
without reason.

The main objective is to evolve Study Buddy from an answer-generating
chatbot into a reliable learning system that can explain, guide, assess,
personalize, help students practice, and support continued learning.

## Non-negotiable requirements

1.  **Inspect before changing.** Review the codebase, framework,
    architecture, existing AI integration, streaming, rendering,
    persistence, profiles, UI components, and tests before implementing
    major changes.
2.  **Fix real defects, not just appearance.** The visible LaTeX
    rendering issue is a high-priority bug. Investigate and correct its
    actual cause.
3.  **Do not break existing working functionality.** Preserve current
    navigation, chat history, profile integration, message flow,
    existing controls, and correct layout behavior.
4.  **Do not make the UI universal by hardcoding one subject.**
    Personalization must be driven by the actual question, available
    student context, and relevant learning records.
5.  **Do not fake functionality.** A button, checkbox, quiz, timer, save
    action, or progress indicator must perform the advertised action.
    Never show success if the operation failed or never occurred.
6.  **Do not fabricate learning data.** Study time, completion, scores,
    mastery, deadlines, citations, and historical activity must come
    from actual records or be clearly labelled estimates.
7.  **Do not force every answer into the same card template.** Choose an
    answer presentation appropriate to the task.
8.  **Do not add dependencies or rewrite architecture casually.** Prefer
    existing libraries and established project patterns. Add
    dependencies only when justified and compatible.
9.  **Do not execute arbitrary model-generated HTML or code.** Validate
    structured AI output and render only approved component types.
10. **Implement and verify.** Do not stop after describing a plan,
    producing mockups, or changing CSS. Run available tests, builds, and
    targeted checks, and report honestly what was verified.

------------------------------------------------------------------------

# 1. Initial Codebase Audit

Before implementation, inspect the existing project and produce a
concise internal understanding of the following areas. Then proceed with
implementation rather than waiting for approval unless an actual blocker
or destructive decision requires clarification.

## 1.1 Application architecture

Identify:

-   Frontend framework and version.
-   Language and type system.
-   Routing and state-management conventions.
-   Existing component library and design tokens.
-   Styling approach and responsive breakpoints.
-   AI provider, model configuration, and request flow.
-   Whether responses are streamed or returned as complete messages.
-   Current Markdown renderer and any math-rendering library.
-   Message and conversation data models.
-   Database and persistence layer.
-   Authentication and profile access patterns.
-   File/image upload capabilities.
-   Existing calendar, analytics, focus timer, voice, or notification
    integrations.
-   Existing tests, linting, formatting, and build commands.

Use the project's established conventions. Do not assume a framework or
database before inspecting it.

## 1.2 Existing functionality inventory

Identify the current implementation and status of:

-   Chat composer and send action.
-   Message rendering.
-   Markdown formatting.
-   Mathematical notation.
-   Copy and regenerate actions.
-   Suggested follow-ups.
-   Interactive quizzes.
-   Checklist components.
-   Study plans and scheduling.
-   Start Focus Session.
-   Plan My Day.
-   Study progress metrics.
-   Student profile context.
-   Chat history and persistence.
-   Loading, streaming, and error states.
-   Mobile responsiveness and scrolling.

Classify important items as working, partially working, broken, or not
implemented. Fix broken existing behavior before adding competing
implementations.

## 1.3 Change strategy

-   Reuse existing components and APIs where possible.
-   Prefer small, well-scoped changes over a full rewrite.
-   Keep AI prompting, structured output validation, rendering,
    persistence, and UI interactions separated by responsibility.
-   Avoid duplicate components that solve the same problem.
-   Keep secrets and provider credentials on the appropriate secure side
    of the architecture.
-   Do not expose private profile or learning data to unrelated
    requests.
-   Do not migrate or delete user data without a justified, safe
    migration strategy.

------------------------------------------------------------------------

# 2. Priority P0 --- Fix LaTeX and Mathematical Rendering

The current app sometimes shows mathematical source such as
`\(\vec{A} \cdot \vec{B} = |A||B| \cos\theta\)` as literal text rather
than formatted mathematics. This must be treated as a real rendering
defect, not merely a prompt-writing problem.

## 2.1 Trace the entire pipeline

Investigate how mathematical content travels through:

1.  AI response generation.
2.  Streaming chunks or complete response payload.
3.  Markdown normalization and parsing.
4.  Mathematical delimiter recognition.
5.  Math rendering.
6.  Reactivity/rerendering and final presentation.

Determine whether the problem is caused by unsupported delimiters,
incorrect escaping, Markdown parsing order, streaming partials,
component lifecycle, CSS, or a missing/misconfigured renderer. Do not
assume the cause without inspection.

## 2.2 Select and integrate a proper renderer

Use a reliable compatible solution such as KaTeX or MathJax, based on
the project's framework, existing dependencies, security requirements,
and rendering needs.

Support the math delimiters the application intends to accept, including
as appropriate:

-   Inline: `\( ... \)`
-   Display: `\[ ... \]`
-   Display: `$$ ... $$`

If the existing app or provider uses other supported conventions,
normalize them safely and consistently.

Support:

-   Fractions and nested fractions.
-   Superscripts and subscripts.
-   Greek letters and standard symbols.
-   Vectors, dot products, cross products, magnitudes, and vector
    notation.
-   Roots and radicals.
-   Matrices, determinants, cases, and aligned equations.
-   Limits, derivatives, integrals, sums, and products.
-   Trigonometric expressions.
-   Systems of equations and multiline derivations.
-   Chemistry equations and common scientific notation where supported.
-   Mathematical expressions embedded in English, Bangla, or
    mixed-language paragraphs.
-   Equations inside quiz questions, answers, worked solutions, and
    generated study materials.

## 2.3 Parsing and rendering rules

-   Parse mathematical notation without corrupting ordinary prose.
-   Do not interpret code examples as mathematics.
-   Avoid unsafe arbitrary HTML rendering.
-   Sanitize supported Markdown/HTML according to the existing security
    model.
-   Handle escaped delimiters and malformed expressions gracefully.
-   If one expression fails, preserve the rest of the answer.
-   Preserve source content or show a safe fallback for invalid
    expressions rather than silently deleting symbols.
-   Avoid global string replacements that remove backslashes or damage
    LaTeX syntax.
-   Ensure mathematical rendering works in every supported AI response
    component, not just the main text bubble.

## 2.4 Streaming behavior

The model may send a formula in several incomplete chunks. The UI must
not permanently display malformed math because the first chunk is
incomplete.

-   Handle incomplete delimiters while streaming.
-   Render stable content progressively where practical.
-   Reprocess the final completed message after streaming finishes.
-   Avoid repeatedly remounting the entire response.
-   Preserve scroll position and interaction state when the response
    updates.
-   Prevent excessive re-rendering on long responses.

## 2.5 Mobile math layout

-   Prevent long equations from expanding the viewport horizontally.
-   Allow suitable horizontal scrolling for oversized display equations.
-   Keep inline equations readable within paragraphs.
-   Avoid clipping fractions, superscripts, matrices, and equation
    labels.
-   Verify rendering at narrow mobile widths and larger displays.

## 2.6 LaTeX acceptance tests

Create tests or a reproducible test page covering:

1.  Inline math in a paragraph.
2.  A standalone display equation.
3.  A multi-step derivation.
4.  Fractions, roots, Greek symbols, vectors, and matrices.
5.  A long equation on a narrow screen.
6.  Equations inside an interactive quiz.
7.  Equations delivered as streaming chunks.
8.  A completed response that rerenders after another message is sent.
9.  Code containing backslashes and dollar signs.
10. Malformed or unsupported LaTeX.
11. Bangla prose containing English mathematical notation.
12. A response containing both Markdown and multiple math expressions.

**Acceptance criterion:** Supported mathematics renders correctly
throughout Study Buddy and does not appear as raw LaTeX source in
ordinary successful responses.

------------------------------------------------------------------------

# 3. Priority P0 --- Answer Reliability and Educational Quality

The presentation layer must not make inaccurate answers look
authoritative.

## 3.1 Response behavior

-   Answer the user's actual question before offering tangential
    suggestions.
-   Adapt detail to the question and requested level.
-   Keep straightforward factual answers concise.
-   Use deeper explanations for complex learning tasks.
-   Explain reasoning when it improves learning.
-   Clearly distinguish facts, interpretations, assumptions, estimates,
    and uncertain claims.
-   Do not invent user profile details, exam requirements, official
    questions, sources, or learning activity.
-   Do not label generated practice questions as official past questions
    unless their provenance has been verified.
-   Do not claim external verification unless a suitable source or
    retrieval mechanism was actually used.
-   Respect the user's chosen language and requested explanation style.

## 3.2 Optional answer verification

Where useful, provide an action such as **Check this answer** or
**Review my solution**.

The verification process should:

-   Re-examine the answer or calculation.
-   Check intermediate steps and consistency.
-   Identify possible errors or assumptions.
-   Correct errors when found.
-   Explain uncertainty rather than guaranteeing perfection.

This is an additional review layer, not a promise that the answer is
infallible.

## 3.3 Sources and evidence

When the application has access to reliable retrieval or source
integrations, use them for questions that genuinely need current or
source-specific information.

-   Cite only sources actually retrieved or available.
-   Distinguish generated explanations from sourced claims.
-   Avoid fabricated URLs, quotations, citations, or official exam
    patterns.
-   If source verification is unavailable, communicate limitations
    honestly.
-   Do not overload ordinary explanations with citations that do not
    improve the answer.

------------------------------------------------------------------------

# 4. Priority P1 --- Dynamic, Question-Aware Response UI

The current interface already uses structured cards, insight panels,
quizzes, checklists, and suggested actions. Preserve this visual
identity, but make response composition context-aware.

## 4.1 Principle: choose the right interface for the task

The same response template must not be applied to every question. Select
the minimum set of components that makes the answer clearer and more
useful.

Examples:

-   A simple definition: concise text and a short example.
-   A complex concept: structured explanation, key terms, and examples.
-   A mathematical problem: rendered equation, numbered solution steps,
    and a final answer.
-   A quiz: interactive question, selectable options, feedback, and
    explanation.
-   A comparison: a comparison table or structured list.
-   A study plan: schedule, session durations, task checkboxes, and
    progress.
-   An essay request: outline, writing draft, and revision actions.
-   A coding question: syntax-highlighted code, explanation, and copy
    action.
-   A history question: chronological structure or timeline where
    useful.
-   A document summary: overview, key points, section structure, and
    follow-up actions.
-   A diagram-based question: a suitable diagram or visualization if
    supported.
-   A language-learning question: examples, corrections, and optional
    practice.

These are adaptive examples, not fixed templates or discipline-specific
hardcoding.

## 4.2 Use a validated response model

Where appropriate, define or refine a typed response representation that
separates content from presentation.

Potential content types include:

-   Text/Markdown.
-   Mathematical content.
-   Callout or insight.
-   Step-by-step explanation.
-   Table/comparison.
-   Quiz/assessment.
-   Flashcards.
-   Study plan.
-   Checklist.
-   Code.
-   Diagram or visualization configuration.
-   Contextual follow-up actions.

Follow the project's current architecture. Do not invent a large schema
if a smaller extension of the existing model is sufficient.

Requirements:

-   Validate the AI response before rendering.
-   Allow only approved component types and supported fields.
-   Validate data types, required fields, and allowed options.
-   Handle missing, invalid, and unknown component types gracefully.
-   Preserve the useful textual answer if structured rendering fails.
-   Never execute arbitrary model-provided HTML, JavaScript, component
    names, or code.
-   Keep response generation and UI rendering independently testable.

## 4.3 Visual hierarchy and restraint

-   Do not wrap every paragraph in a card.
-   Use cards for distinct content, not decoration.
-   Use tables for genuine comparisons.
-   Use lists for steps or grouped items.
-   Use math blocks for equations.
-   Use interactive controls only when they have real behavior.
-   Avoid excessive borders, nested panels, repeated icons, redundant
    headings, and oversized whitespace.
-   Avoid empty or incomplete cards.
-   Avoid layout shifts when streaming content arrives.
-   Keep short answers short and detailed answers navigable.

## 4.4 Reusable components

Where consistent with the existing architecture, use reusable components
for responsibilities such as:

-   Main response renderer.
-   Markdown/rich text renderer.
-   Mathematical renderer.
-   Quiz renderer.
-   Flashcard renderer.
-   Study-plan renderer.
-   Table renderer.
-   Code renderer.
-   Visualization renderer.
-   Follow-up suggestions.
-   Message actions.

These are suggested responsibilities, not mandatory filenames. Reuse
existing components rather than creating duplicates.

------------------------------------------------------------------------

# 5. Priority P1 --- Make Existing Interactive Elements Actually Work

The interface already contains quiz options, checkboxes, suggested
follow-ups, planning actions, and message controls. Improve their
behavior rather than merely adding more visual components.

## 5.1 Quiz interactions

Implement a complete quiz lifecycle:

1.  Render the question and its answer choices.
2.  Support the correct selection model for the question.
3.  Validate input on submission or use immediate feedback when
    intentionally designed.
4.  Show the result and explain the correct answer.
5.  Explain the underlying misconception after an incorrect answer where
    useful.
6.  Allow retry, reveal, or continuation according to the exercise type.
7.  Prevent accidental duplicate submissions where relevant.
8.  Persist attempts when supported by the data model.
9.  Update progress only after a real recorded result.
10. Preserve the student's selected state when the UI rerenders.

Support suitable question types: single-choice MCQs, multiple-select,
true/false, short answer, flashcards, and open-ended responses where the
application can evaluate them appropriately.

Do not force every discipline or learning task into MCQs.

## 5.2 Checklists and study tasks

Distinguish among:

-   A temporary checklist inside a response.
-   A saved task.
-   A completed study activity.
-   A record that contributes to persistent progress.

Make the intended behavior explicit. Persist saved tasks using existing
infrastructure. Show completion feedback only after the state update
succeeds.

## 5.3 Contextual actions

Potential actions include:

-   Explain more simply.
-   Show another example.
-   Test my understanding.
-   Generate practice questions.
-   Summarize the key points.
-   Create flashcards.
-   Start a focus session.
-   Plan my study day.
-   Revise my mistakes.
-   Continue learning.

Only show actions relevant to the current answer. A button must submit
the correct contextual request or perform the corresponding application
operation.

## 5.4 Message actions

Where present or appropriate, support:

-   Copy answer.
-   Regenerate response.
-   Retry failed response.
-   Ask a contextual follow-up.
-   Save an answer or resource if supported.
-   Provide feedback if supported.

Provide clear success and error feedback. Ensure controls remain usable
on mobile and are accessible to assistive technologies.

------------------------------------------------------------------------

# 6. Priority P1 --- Adaptive Tutor Mode

Turn Study Buddy into a tutor that can guide understanding and practice,
not just provide finished answers.

## 6.1 Adjustable teaching styles

Support different response modes where appropriate:

-   Simple beginner explanation.
-   Detailed conceptual explanation.
-   Step-by-step solution.
-   Example-first explanation.
-   Formal or advanced explanation.
-   Quick revision.
-   Exam-oriented explanation when requested or relevant.
-   Guided practice.
-   Direct answer.

The student should be able to change styles without starting a
completely unrelated conversation.

## 6.2 Adaptive teaching loop

Where appropriate:

1.  Explain the concept.
2.  Offer a short comprehension check.
3.  Evaluate the student's response.
4.  Identify the specific misunderstanding.
5.  Provide a hint or alternative explanation.
6.  Offer another practice item.
7.  Update learning records if the interaction is recorded.

Do not force a quiz after every response. Straightforward questions
should still receive straightforward answers.

## 6.3 Socratic mode

Offer an optional guided mode where the tutor helps students solve a
problem through hints and questions rather than immediately revealing
the full solution.

A possible flow:

-   Understand what the question asks.
-   Identify relevant information.
-   Select a method.
-   Complete the next step.
-   Check the result.
-   Reveal a worked solution when requested or appropriate.

Keep this mode optional and respect students who explicitly ask for a
direct solution.

## 6.4 Detecting understanding

Do not assume that one correct answer proves mastery or one wrong answer
proves a broad weakness.

Use multiple relevant observations over time where available. Track the
specific concept or skill involved. Be transparent that mastery
estimates are estimates, especially when data is limited.

------------------------------------------------------------------------

# 7. Priority P1 --- Persistent Learning Memory and Personalization

Use existing StudyHub profile and learning records to improve future
tutoring. Study Buddy must remain capable of answering questions outside
the student's primary academic track.

## 7.1 Useful learning context

Where available and relevant, use:

-   Completed lessons and topics.
-   Quiz attempts and results.
-   Repeated mistakes.
-   Saved notes or learning materials.
-   Stated learning goals.
-   Preferred explanation style and language.
-   Current study plans.
-   Revision history.

Do not invent missing profile fields or activity. If there is
insufficient context, provide a sensible default or ask one short
clarifying question when necessary.

## 7.2 Context boundaries

-   The current question takes priority over assumptions based on
    profile data.
-   Profile context must personalize, not restrict, learning.
-   A student studying one subject may ask about any other supported
    subject.
-   Do not mention profile details unnecessarily in every answer.
-   Do not treat an AI inference as a verified fact.
-   Do not expose one student's information in another student's
    conversation.
-   Follow authentication, authorization, retention, and privacy rules
    already established in the application.

## 7.3 Student controls

Where suitable controls exist or can be added consistently, allow
students to inspect or correct their learning preferences and relevant
saved learning records.

Do not store unnecessary sensitive information. Keep memory focused on
educational utility.

------------------------------------------------------------------------

# 8. Priority P2 --- Smart Revision and Mistake Notebook

Build revision around actual learning activity rather than generic
reminders.

## 8.1 Revision queue

Provide an appropriate view of items that need review, such as:

-   Topics due for revision.
-   Incorrect quiz questions.
-   Concepts the student marked as difficult.
-   Flashcards awaiting review.
-   Incomplete learning tasks.

If no records exist, show an honest empty state and a relevant action to
begin learning.

## 8.2 Spaced repetition

Introduce a configurable spaced-repetition strategy where it is
appropriate.

Use real events such as recall performance, quiz results, and review
ratings to determine future review timing. Avoid claiming that a
specific schedule guarantees retention.

Support different content types, including:

-   Vocabulary and definitions.
-   Mathematical formulas.
-   Scientific concepts.
-   Historical facts and events.
-   Language rules.
-   Accounting principles.
-   Essay structures and key arguments.
-   Other material that benefits from repeated retrieval practice.

## 8.3 Mistake notebook

Allow relevant mistakes to be saved and revisited.

Each record may include:

-   The original question.
-   The student's answer.
-   The correct answer or accepted solution.
-   A concise explanation of the mistake.
-   The relevant topic.
-   The date and attempt information, where available.
-   A retry action.

Do not save every mistake automatically if doing so creates noise. Use
meaningful criteria or student controls.

## 8.4 Revision actions

Where supported, include:

-   Revise today's due items.
-   Practice my mistakes.
-   Create flashcards from this explanation.
-   Generate a short revision quiz.
-   Review this concept again later.

Persist the relevant review state and update it only when a review
actually occurs.

------------------------------------------------------------------------

# 9. Priority P2 --- Goal-Aware Study Planning and Focus Sessions

Improve the existing study plan and focus-session experience.

## 9.1 Study plan generation

Use relevant information such as:

-   The student's stated goal.
-   Subjects and topics.
-   Available time.
-   Daily target if configured.
-   Upcoming deadlines or exams if known.
-   Topic difficulty and priority.
-   Completed work.
-   Existing scheduled activities.

Do not assume every student has the same study target or schedule. If
key details are missing, use reasonable defaults or ask a brief question
when necessary.

Create plans with:

-   Clear tasks.
-   Realistic duration estimates.
-   Breaks where appropriate.
-   A sensible order.
-   A visible overall goal.
-   Flexible options for rescheduling.

## 9.2 Plan execution

Where existing infrastructure supports it:

-   Save and reopen plans.
-   Mark sessions complete.
-   Track planned versus actual duration.
-   Reschedule unfinished tasks.
-   Adjust remaining sessions after delays.
-   Add eligible events to an integrated calendar.
-   Resume an incomplete plan.

Do not claim calendar synchronization or persistent saving unless
implemented and successful.

## 9.3 Focus sessions

If the current Focus Session action is available, verify that it
genuinely works.

Potential behavior includes:

-   Start, pause, resume, and end a session.
-   Show elapsed or remaining time.
-   Associate the session with a selected task when appropriate.
-   Record actual completed duration.
-   Update study metrics from real records.
-   Handle leaving and returning to the screen gracefully.

Use the existing app's timer and persistence architecture. Do not create
conflicting timers or claim an active session exists when it does not.

## 9.4 Progress analytics

Where sufficient recorded data exists, show:

-   Planned versus completed study time.
-   Completed tasks.
-   Revision activity.
-   Quiz accuracy.
-   Topic practice history.
-   Progress toward stated goals.
-   Trends over time.

Clearly label estimates. Do not fabricate mastery percentages, scores,
study time, or completed sessions.

------------------------------------------------------------------------

# 10. Priority P2 --- Photo-Based Questions and Document Learning

If the current AI integration supports image input, or a compatible
integration can be added safely, improve photo-based learning.

## 10.1 Supported use cases

Examples include:

-   Textbook questions.
-   Handwritten exercises.
-   Diagrams and charts.
-   Worksheets.
-   Notes.
-   Passages and reading comprehension.
-   Accounting calculations.
-   Scientific notation and equations.
-   Other supported educational images.

## 10.2 Expected workflow

1.  Accept a supported upload.
2.  Show a preview and allow removal or replacement.
3.  Process the image through a compatible model or existing service.
4.  Identify the relevant text, symbols, diagram, or question.
5.  Solve or explain the requested task.
6.  Show reasoning appropriate to the request.
7.  Offer follow-up practice where useful.

When content is unclear, explicitly identify the uncertainty and ask for
a clearer image or confirmation. Do not invent unreadable words,
symbols, labels, or missing diagram information.

## 10.3 Documents and notes

If file/document support is available, consider allowing students to:

-   Summarize a document.
-   Explain a selected passage.
-   Extract key concepts.
-   Generate practice questions.
-   Create flashcards.
-   Build a revision guide.
-   Navigate by section or heading.

Respect file size, supported formats, authorization, and privacy
requirements. Avoid presenting a fake upload flow that does not actually
process the uploaded content.

------------------------------------------------------------------------

# 11. Priority P2 --- Visual Learning Lab

Use visual explanations where they provide genuine educational value.

## 11.1 Potential visualization types

-   Mathematical graphs and geometry.
-   Physics diagrams and motion.
-   Vector operations.
-   Circuits and wave behavior.
-   Biology diagrams and processes.
-   Chemistry structures and reaction representations.
-   Historical timelines.
-   Geography maps where relevant.
-   Business processes.
-   Economic charts.
-   Literature relationship maps.
-   Programming flowcharts.

These are examples, not a requirement to include visuals in every
answer.

## 11.2 Genuine interaction

Where feasible, allow students to manipulate relevant variables and
observe the result.

Examples:

-   Change a graph parameter.
-   Adjust a physics variable.
-   Reveal diagram labels.
-   Step through a process.
-   Compare different scenarios.

Use actual interactive components or visualization libraries for
interactive behavior. Do not substitute a static image when the feature
promises user-controlled interaction.

## 11.3 Accuracy and fallback

-   Ensure diagrams are appropriate to the subject.
-   Avoid unsupported or misleading visual claims.
-   Provide textual explanations and accessible alternatives.
-   Handle unsupported visualization types gracefully.
-   Keep mobile performance and readability in mind.

------------------------------------------------------------------------

# 12. Priority P3 --- Optional Voice Tutor

Evaluate existing capabilities before adding voice infrastructure.

If compatible services are available, consider:

-   Speech-to-text question input.
-   Text-to-speech explanations.
-   Optional spoken conversation.
-   Pause, resume, and interruption controls.
-   Transcripts.
-   Language and pronunciation support.
-   Oral-practice workflows.

Voice must remain optional. The text experience should work
independently.

Test supported languages, latency, noisy input, device compatibility,
and failure states. Be transparent when a feature depends on a service
or permission that is unavailable.

------------------------------------------------------------------------

# 13. Premium Mobile UI/UX Refinement

Preserve the existing Study Buddy design identity and improve usability
rather than adding decoration for its own sake.

## 13.1 Existing chat layout

Check:

-   Header hierarchy and status.
-   User/assistant message distinction.
-   Readability of long answers.
-   Teal accents and existing design tokens.
-   Message-level actions.
-   Contextual suggestion chips.
-   Loading and error states.
-   Composer usability.
-   Scrolling through long messages.
-   Responsive layout for equations, tables, diagrams, and code.

Keep the established "Grounded AI" label consistent with the current
design.

Do not reintroduce changes to already-correct keyboard behavior or
unrelated functionality. Avoid unnecessary modifications outside the
agreed scope.

## 13.2 Responsive behavior

Test on narrow mobile screens and larger displays.

Prevent:

-   Horizontal page overflow.
-   Clipped equations.
-   Unreadable tables.
-   Oversized cards.
-   Overlapping controls.
-   Buttons that are difficult to tap.
-   Important content hidden behind fixed UI.
-   Large scroll jumps during streaming.
-   Excessive layout shifts as components load.

Use appropriate touch targets, semantic controls, keyboard
accessibility, readable contrast, and reduced-motion support where
practical.

## 13.3 Performance

-   Avoid unnecessary rerendering of long messages.
-   Memoize expensive calculations when appropriate.
-   Avoid repeatedly processing unchanged mathematical content.
-   Keep streaming updates responsive.
-   Avoid excessive network requests for simple UI interactions.
-   Handle long conversations without severe performance degradation.
-   Preserve message state and scroll behavior where possible.

Do not introduce complicated performance optimizations without evidence
that they are needed.

------------------------------------------------------------------------

# 14. Contextual Follow-Ups

Follow-up suggestions should be specific to the current answer and help
the student take a useful next step.

Examples:

After a mathematical solution: - Explain a specific step. - Solve a
similar problem. - Test my understanding. - Show a common mistake.

After a history explanation: - Create a timeline. - Compare the major
causes. - Quiz me on key events. - Summarize the important arguments.

After a writing task: - Improve clarity. - Review grammar. - Create an
outline. - Provide constructive feedback.

After a study plan: - Adjust the duration. - Start the first session. -
Reschedule unfinished tasks. - Generate a revision checklist.

Do not show all actions after every answer. Avoid generic, repetitive
chips that add little value.

When a suggestion is selected, preserve the relevant conversational
context and execute the intended action.

------------------------------------------------------------------------

# 15. AI Integration, Structured Responses, and Safety

## 15.1 Separate responsibilities

Keep the following responsibilities distinct where the current
architecture permits:

-   Request construction and relevant context selection.
-   AI generation and streaming.
-   Structured response validation.
-   Markdown and math rendering.
-   Interactive component state.
-   Persistence and analytics.
-   Error handling and logging.

Avoid a single giant component that handles every aspect of the
experience.

## 15.2 Safe rendering

-   Treat model output as untrusted input.
-   Render only supported component types.
-   Validate structured data.
-   Sanitize permitted rich content.
-   Never execute arbitrary model-generated JavaScript.
-   Do not dynamically import arbitrary component names from AI output.
-   Prevent invalid output from crashing the conversation.
-   Provide a readable fallback when structured rendering fails.

## 15.3 Context management

-   Include only relevant profile and learning context.
-   Avoid sending unnecessary personal data to the model.
-   Keep different users' data isolated.
-   Preserve relevant context across follow-ups.
-   Avoid blindly including the entire chat history if it becomes
    inefficient or irrelevant.
-   Summarize prior context carefully where needed, without inventing
    details.

## 15.4 Error handling

Provide clear behavior for:

-   Network failure.
-   AI provider errors.
-   Rate limits.
-   Empty or malformed responses.
-   Interrupted streaming.
-   Upload failures.
-   Invalid structured output.
-   Unsupported components.
-   Persistence failures.

Keep user-facing messages helpful and concise. Log appropriate
diagnostic details without exposing secrets or sensitive content.

------------------------------------------------------------------------

# 16. Testing and Acceptance Criteria

Create or extend automated tests using the existing project testing
setup. If no suitable test framework exists, use the lightest compatible
approach rather than introducing a large testing stack without reason.

## 16.1 Mathematical rendering

-   Inline and display formulas render correctly.
-   Complex expressions work.
-   Invalid expressions fail gracefully.
-   Streaming math is handled.
-   Code is not accidentally parsed as math.
-   Mobile layout does not overflow.
-   All relevant response components support math.

## 16.2 Dynamic response rendering

-   Plain text renders normally.
-   Markdown headings, lists, and tables work.
-   Math, quiz, code, and study-plan components render correctly.
-   Unknown or malformed structured content falls back safely.
-   No arbitrary model-generated executable content is accepted.
-   Short responses do not acquire unnecessary cards.

## 16.3 Interactive controls

-   Quiz selections and submissions work.
-   Correct/incorrect feedback is accurate relative to the supplied
    answer key.
-   Retry and continuation behave as intended.
-   Checklists update correctly.
-   Saved state persists where expected.
-   Suggested follow-ups submit the correct contextual request.
-   Copy and regenerate actions work.
-   Failed operations show appropriate errors.

## 16.4 Learning and persistence

-   Learning records reflect actual activity.
-   Study tasks persist when intended.
-   Revision scheduling uses recorded events.
-   Analytics do not invent values.
-   Profile information is accessed with correct authorization.
-   One user's data cannot appear in another user's conversation.

## 16.5 Mobile and accessibility

-   No page-level horizontal overflow.
-   Equations and tables remain usable.
-   Controls have accessible labels.
-   Focus and keyboard interaction work where applicable.
-   Text contrast and readability are appropriate.
-   Streaming does not make the page unusable.

## 16.6 Regression checks

-   Existing navigation continues to work.
-   Chat history remains available.
-   Existing profile integration is preserved.
-   Existing working features are not silently removed.
-   Build, lint, and tests pass where available.
-   Any known failures are reported honestly.

------------------------------------------------------------------------

# 17. Implementation Roadmap

Prioritize the work in this order. Adjust the sequence only when the
codebase reveals a genuine dependency.

## Phase 1 --- Stabilize the foundation

1.  Audit the current implementation.
2.  Reproduce and fix LaTeX rendering.
3.  Verify Markdown and streaming compatibility.
4.  Improve error handling and fallback behavior.
5.  Make existing quizzes, checklists, and message actions functional.
6.  Add regression tests for critical behavior.

**Exit condition:** Existing Study Buddy answers render correctly, core
controls work, and the changes do not break established functionality.

## Phase 2 --- Dynamic response experience

1.  Improve response-type detection and structured output validation.
2.  Implement or refine reusable renderers.
3.  Match the UI to the task.
4.  Improve contextual follow-ups.
5.  Polish mobile behavior and long-answer performance.

**Exit condition:** Different kinds of questions produce appropriate,
readable response layouts without forcing a universal card template.

## Phase 3 --- Learning intelligence

1.  Implement or refine adaptive tutoring behavior.
2.  Use relevant learning history.
3.  Add useful mistake tracking.
4.  Implement smart revision.
5.  Improve study planning and real progress tracking.

**Exit condition:** The app can use actual learning activity to
personalize future support without fabricating progress.

## Phase 4 --- Advanced learning tools

1.  Improve photo-based question solving.
2.  Add or refine document learning if supported.
3.  Build high-value interactive visualizations.
4.  Add voice support only when suitable integrations are available.
5.  Expand analytics and accessibility checks.

**Exit condition:** Advanced tools work end-to-end, have graceful
fallbacks, and do not compromise the core chat experience.

------------------------------------------------------------------------

# 18. Execution Instructions for the Coding Agent

Follow this workflow:

### Step 1 --- Inspect

Read the relevant files and trace the current Study Buddy response
pipeline. Identify the actual cause of the LaTeX bug and map the
existing components and integrations.

### Step 2 --- Plan internally

Create a concise implementation checklist ordered by priority and
dependency. Do not replace working code without a reason.

### Step 3 --- Implement the highest-priority fixes

Start with LaTeX, response reliability, existing interactions, and
regression safety. Make real code changes rather than returning only
recommendations.

### Step 4 --- Integrate the improvements

Build on the existing architecture. Keep components reusable, types
explicit where supported, and state/persistence responsibilities clear.

### Step 5 --- Verify

Run the available test suite, linting, type checks, and production build
where feasible. Test the critical paths and inspect the resulting diff.

### Step 6 --- Report honestly

At completion, provide a concise report covering:

-   What was changed.
-   Which files or modules were affected.
-   What caused the LaTeX issue.
-   How the issue was fixed.
-   Which features are fully functional.
-   Which features remain partial or depend on unavailable integrations.
-   Tests/build checks that passed.
-   Any failures, assumptions, or remaining limitations.

Do not claim a feature is complete unless it has been implemented and
verified. Do not conceal failing tests. Do not finish by returning
another prompt instead of performing the available implementation work.

------------------------------------------------------------------------

# Definition of Success

The upgraded Study Buddy should:

1.  Render supported mathematical notation correctly.
2.  Produce accurate, relevant, readable answers.
3.  Select response layouts appropriate to the actual question.
4.  Make visible interactive controls genuinely functional.
5.  Adapt teaching and practice to the learner where evidence supports
    it.
6.  Use learning memory and progress responsibly.
7.  Provide useful revision and study-planning workflows.
8.  Support image/document learning and visualizations where the
    technical stack allows.
9.  Preserve the current premium visual identity and existing working
    features.
10. Remain responsive, secure, accessible, and reliable on mobile.
11. Avoid fabricated progress, fake integrations, unsafe rendering, and
    unsupported claims.
12. Be verified through actual implementation and testing.

**Final principle:** Build a coherent learning experience, not a
collection of decorative AI cards. Prioritize correctness, genuine
interactivity, useful personalization, and reliable implementation over
feature count alone.
