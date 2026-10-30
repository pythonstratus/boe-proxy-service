I'll check a couple of Claude Code details first so the prompt and model advice are accurate.

**Short answer on models:** use **Claude Sonnet 5.5** to build the memory. It reads and summarizes code well and costs far less than Fable. Move up to **Opus 5.5** only if the codebase is very large or Sonnet's output feels thin. **Haiku 5.5** is fine for quick indexing, but I wouldn't let it write the memory you'll later debug against, because it's more likely to miss subtle logic. Keep **Fable** for the debugging sessions themselves.

In Claude Code, "memory" means `CLAUDE.md` files, which load automatically at the start of each session. Anything a `CLAUDE.md` loads is paid for on every Fable turn, so the prompt below splits the output in two:

- **Short `CLAUDE.md` files** that always load. They hold the essentials.
- **Detailed reference docs** that Claude opens only when it needs them.

### The prompt

Open a terminal on the other computer in the folder that contains both `entity-service` and `entity-ui`. Run `claude --model sonnet`, then paste this:

```
You are building a persistent "codebase memory" for the Entity application so that future sessions (including debugging sessions on a different model) can work from it instead of re-reading the code. This task is READ-ONLY for source code: do not modify, format, or refactor any existing file. The only files you create or edit are the memory files listed in Phase 3. If CLAUDE.md files already exist, merge into them rather than overwriting.

CONTEXT
- This folder contains two parts: entity-service/ (backend) and entity-ui/ (frontend).
- The memory will be used mainly for DEBUGGING. Favor what helps trace bugs (data flow, contracts, config, error handling, state, edge cases) over general description.

PHASE 0 - RECON (keep it cheap)
1. Record the git commit hash and branch. Check whether the two folders share one git repo or are separate repos.
2. Identify languages, frameworks, build tools and versions from manifests (package.json, pom.xml, build.gradle, *.csproj, pyproject/requirements, go.mod, etc.).
3. Get a file inventory with counts per directory. Skip anything git-ignored, plus node_modules, dist, build, target, bin, obj, .next, coverage, vendor, lockfiles, minified or bundled files, generated code, binary assets and large fixtures.
4. Read any existing README, docs, CLAUDE.md or AGENTS.md first. Treat them as claims to verify, not as facts.

PHASE 1 - DEEP ANALYSIS (in parallel)
Delegate entity-service and entity-ui to two separate subagents so raw file contents stay out of the main context. The subagents must not edit any files. Each returns a structured written report, not file dumps, with a file path for every claim.

Backend subagent (entity-service):
- Entry point, startup sequence, dependency wiring.
- Module map: what each package or folder is responsible for.
- Every API endpoint: method, path, handler (file:symbol), request/response shape, validation, auth requirement, status codes, error responses.
- Data model: entities/tables, relationships, migrations, repository/ORM patterns, transactions.
- Business rules per domain: state transitions, side effects.
- External integrations (DBs, queues, caches, third-party APIs) and their configuration.
- Configuration: config files, profiles, every environment variable and its default.
- Error handling and logging: exception mapping, log format and location.
- Security: authentication/authorization flow, CORS, token handling.
- Tests: framework, location, how to run, what is and isn't covered.
- Build/run/test commands, verified from scripts and manifests, not guessed.

Frontend subagent (entity-ui):
- Entry point; routing table (route -> page/component file).
- Component hierarchy for the main screens; shared components.
- State management: stores/contexts, what lives where, how it updates.
- API client layer: base URL resolution, every backend call (method, path, payload, where it's called from), interceptors, auth token handling, error handling and retries.
- Forms and validation; data transformations between API and UI.
- Configuration and env vars (build-time vs run-time).
- Build/run/test commands; test setup.

Both subagents also list "Risk areas / suspected bugs" noticed while reading, with file paths, marked UNVERIFIED. Examples: swallowed exceptions, missing null checks, race conditions, inconsistent field names, TODO/FIXME/HACK comments, dead code.

PHASE 2 - INTEGRATION ANALYSIS (main session)
Using both reports, open specific files only where needed to confirm:
- API contract map: each UI call -> matching backend endpoint. Flag mismatches in path, method, field names, types, required fields, enum values, date formats, auth headers or error shape.
- Trace the 3-5 most important end-to-end flows. Go from UI action -> API -> service -> persistence -> response -> UI state update, with file:symbol at each hop.
- Environment wiring between the two parts: ports, proxies, base URLs, CORS origins.

PHASE 3 - WRITE THE MEMORY FILES
Placement: if the two folders are separate git repos with no shared parent repo, put each folder's reference docs inside that folder (<folder>/docs/claude-memory/) so they travel with that repo. In that case, keep only cross-cutting files at the top level. Otherwise use this layout:

1. ./CLAUDE.md (under 100 lines):
   - What the system is and how the two parts connect.
   - How to run both locally.
   - Top 10 gotchas.
   - A "How to use this memory" note: which reference doc to open for which kind of task, and that when memory and code disagree, the code wins and the memory should be corrected.
   - Refer to docs by plain path in backticks. Do NOT use @-imports, because an import loads the full file into every session.
2. entity-service/CLAUDE.md (under 150 lines): backend stack, commands, module map, conventions, where to find what, backend gotchas.
3. entity-ui/CLAUDE.md (under 150 lines): the same for the frontend.
4. docs/claude-memory/ (read on demand, so these can be detailed):
   - architecture.md: system diagram (Mermaid), components, request lifecycle.
   - backend-reference.md: endpoints table, data model, services, config/env vars.
   - frontend-reference.md: routes, components, state, API client, config/env vars.
   - api-contract.md: UI call <-> endpoint map with flagged mismatches.
   - flows.md: the end-to-end traces from Phase 2.
   - debugging-guide.md: how to run each part with debug logging, where logs go, example curl requests, common failure modes, and the UNVERIFIED risk list.

Rules for all files:
- Every factual claim cites a file path, plus a symbol or line where useful. Label anything inferred as "Inferred".
- Never invent. If something can't be determined, write "Unknown" and say where you looked.
- Use tables and terse bullets, not prose. Use only short code snippets.
- Each file starts with a header: date generated, commit hash(es), model used, scope covered, and anything skipped.

PHASE 4 - VERIFY
- Spot-check at least 10 specific claims (endpoints, paths, env vars, commands) against the code and fix any errors.
- Confirm every file path mentioned in the memory exists.
- Run wc -l on the CLAUDE.md files. Trim any over their limit by moving detail into docs/claude-memory/.

FINAL REPORT (short, in chat)
- Files written, with line counts.
- What was skipped or left Unknown.
- Top 5 risk areas from the debugging guide.
- A reminder to commit the memory files.
```

When it finishes, review the files and commit them. For debugging, start `claude --model fable` from the same folder; the `CLAUDE.md` files load on their own.

### Other suggestions

- **Commit the memory to git.** That's what makes it portable between your computers. Claude Code's automatic memory is stored only on the machine where it was created.
- **Refresh it instead of rebuilding it.** The commit hash in each file's header lets a cheap Sonnet session update only what changed. Use this prompt:
  ```
  Refresh the codebase memory. Read the commit hash in the header of ./CLAUDE.md, run git diff --stat <hash>..HEAD, and re-analyze only the changed files. Update only the affected sections of the CLAUDE.md files and docs/claude-memory/, update the header hashes and dates, spot-check the changed claims against the code, and list what you updated. Do not modify source code.
  ```
- **Pick the model when you start a session, not partway through.** Switching models with `/model` mid-conversation throws away the prompt cache, so the whole context gets billed again.
- **Use one fresh session per bug.** Start each with `/clear` and point Fable at the relevant doc (for example `docs/claude-memory/flows.md`) so it doesn't re-explore the code.
- **Keep generated code out of reach.** Git-ignored folders are already skipped. For generated or vendored code that is checked in, add `Read` deny rules to `.claude/settings.json`.
- **Let a cheaper model do Fable's file searching.** In a Fable session, Claude Code's built-in Explore subagent already does its searching on Opus. To push that searching down to Haiku, define your own project subagent named `Explore` with `model: haiku`.

I can also package the build and refresh prompts as two skill files, so on the other computer you'd just type `/build-code-memory` or `/refresh-code-memory`.

Sources:
- [How Claude remembers your project (memory)](https://code.claude.com/docs/en/memory.md)
- [Create custom subagents](https://code.claude.com/docs/en/sub-agents.md)
- [Set up Claude Code in a monorepo or large codebase](https://code.claude.com/docs/en/large-codebases.md)
- [Prompt caching](https://code.claude.com/docs/en/prompt-caching.md)

Yes. Use **Opus 4.6** to build the memory. It's a strong code reader and well suited to this job; it just costs more than Sonnet would have. The prompt works as written, and only the launch command changes:

```
claude --model claude-opus-4-6
```

If that ID isn't accepted on that machine, start `claude` and pick Opus 4.6 from the `/model` menu before pasting the prompt. Don't rely on the plain `opus` alias, because it may point to a newer Opus that your account doesn't have.

I'd still keep **Haiku** away from the initial build. The analysis subagents write the substance of the memory, and Haiku is more likely to miss subtle logic, which is exactly what you need when debugging. By default the subagents use the same model as the main session, so they'll run on Opus 4.6 too.

Haiku is useful in two places:

- **Small refreshes.** When the diff since the last snapshot is a handful of files, run the refresh prompt with Haiku. For large changes or refactors, use Opus 4.6.
- **File searching during Fable sessions.** The tip from before still applies: define a project subagent named `Explore` with `model: haiku`, and Fable's file searching runs on Haiku instead of Opus.

Since Opus is now the most expensive part of the build, the token-saving advice matters more: skip the generated and vendored folders, and refresh incrementally instead of rebuilding.
