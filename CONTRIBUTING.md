# Development workflow

- Keep changes focused on one feature or maintenance task.
- Use branch names that describe the work, such as `codex/add-client-module` or `codex/fix-event-validation`.
- Use Conventional Commit prefixes (`feat`, `fix`, `docs`, `test`, `chore`) and a short, imperative subject.
- Before proposing a commit, summarize the files changed and the checks run. Do not commit until the project owner approves.
- Prefer a failing test that describes the intended behavior before implementing a feature. Keep tests close to the module they cover.
- Update the root README when setup commands or project structure change.
