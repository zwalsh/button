---
name: open-pr
description: Guide for opening a pull request (PR) using git and the GitHub CLI. Use this when asked to open a PR for code changes.
---

To open a pull request (PR), follow these steps:

1. **Push the current branch (if needed):**
   - If your branch is not yet on the remote, push it:
     ```sh
     git push -u origin <branch-name>
     ```

2. **Gather context:**
   - Review the branch's diff vs `main` to understand key changes and technical details.
   - Make sure to understand what the PR does and why the changes are needed. Ask for input if anything is unclear.

3. **Write a useful PR title description:**
   - Generate a succinct title that explains the change.
   - Generate a description that is easy to read. Use paragraphs and bullet points for clarity. Separate sections with blank lines for readability.
   - In general, include an up-front "Summary", a "Context" section, a "Details" section and a "Testing" section.

4. **Open the PR using the GitHub CLI:**
   - Use the `gh` CLI to open a pull request. Example:
     ```sh
     gh pr create --base main --head <branch-name> --title "Add frontend tests for floatingPresserPositions.js" --body "## Summary

     This PR adds comprehensive frontend tests for floating presser positions.

     ## Context
     We are adding tests to all frontend code in the project. 

     ## Details
     - Verifies pill initialization, velocity, and target position calculations
     - Covers edge cases for random jitter, damping, and pill placement in the container
     - Ensures robust frontend behavior for dynamic presser movement and interaction

     ## Testing
     No testing needed when adding test cases."
     ```