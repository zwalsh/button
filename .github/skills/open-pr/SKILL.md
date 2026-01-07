---
name: open-pr
description: Guide for opening a pull request (PR) using git and the GitHub CLI. Use this when asked to open a PR for code changes.
---

To open a pull request (PR) for your code changes, follow these steps:

1. **Push your branch (if needed):**
   - If your branch is not yet on the remote, push it:
     ```sh
     git push -u origin <branch-name>
     ```

2. **Open the PR using the GitHub CLI:**
   - Use the `gh` CLI to open a pull request:
     ```sh
     gh pr create --base <target-branch> --head <branch-name> --title "<PR Title>" --body "<PR Description>"
     ```

3. **Write a useful PR description:**
   - Clearly explain what the PR does, why the changes are needed, and any context for reviewers.
   - Use paragraphs and bullet points for clarity. Separate sections with blank lines for readability.
   - Example:
     ```
     This PR implements feature X and fixes bug Y.

     - Adds new API endpoint for ...
     - Refactors ...

     Context:
     The previous implementation ...
     ```

**Tips:**
- Review your PR description for clarity and formatting before submitting.
- Reference related issues or PRs if applicable.
