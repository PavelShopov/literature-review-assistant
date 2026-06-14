# Persistent Project Memory Skill

## Purpose

Maintain long-term project continuity between agents, sessions, and tasks using shared documentation files.

## Required Files 

* `MEMORY.md` → shared project state
* `TASKS.md` → actionable todo list
* `DECISIONS.md` → architecture decisions
* `HANDOFF.md` → unfinished work + next steps

---

## Mandatory Workflow

Before starting ANY task:

1. Read:

   * `MEMORY.md`
   * `TASKS.md`
   * `HANDOFF.md`
2. Understand:

   * current architecture
   * existing implementation
   * pending tasks
   * constraints and conventions

After completing ANY meaningful task:

1. Update `MEMORY.md`
2. Update task status in `TASKS.md`
3. Record architectural changes in `DECISIONS.md`
4. Write handoff notes in `HANDOFF.md`

---

## MEMORY.md Format

```md
# Project Memory

## Current Status
Short summary of current project state

## Architecture
Important architecture decisions

## Active Components
- component name
- responsibility

## Known Issues
- issue
- workaround

## Recent Changes
- timestamped updates

## Next Recommended Steps
- next task
```

---

## Agent Rules

* Never begin implementation before reading memory files
* Never overwrite important historical context
* Prefer appending concise updates
* Keep documentation synchronized with code changes
* Use memory files as the single source of truth
* Avoid duplicate work by checking previous entries
* Leave clean handoff notes for future agents
* When spawning sub-agents, include relevant memory excerpts

---

## Coordination Behavior

Agents should behave like members of a persistent engineering team:

* maintain continuity
* preserve context
* document reasoning
* communicate through shared state
* minimize repeated analysis
* optimize for long-term maintainability
