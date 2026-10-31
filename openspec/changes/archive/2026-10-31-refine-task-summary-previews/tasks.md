## 1. Overview and full lists

- [x] 1.1 Replace the expanded overview with three distinct white panels and bounded previews; keep task filtering inside the first panel.
- [x] 1.2 Add a task-only paginated destination and remove inline expansion controls from all previews.

## 2. Consistent secondary pages

- [x] 2.1 Give member information, responsible tasks and recent journals distinct panels and visibly separated rows.
- [x] 2.2 Apply white content panels to full task/journal/organization pages; retain member filters and parent state on return.

## 3. Verification

- [x] 3.1 Run Android build/unit checks, strict OpenSpec validation and whitespace checks.
- [x] 3.2 Verify real emulator navigation, panel/row boundaries, preview bounds and font/screen layout where the current authenticated session permits; record evidence and limits in verification.md.

## 4. Confirmed density and contact follow-up

- [x] 4.1 Move current work to Android-syz without changing the working tree contents.
- [x] 4.2 Compact task entries, show up to four direct reports, and remove redundant normal-state captions.
- [x] 4.3 Add the minimal authorized contact endpoint and OpenAPI schema, preserving candidate-list privacy; test access boundaries and real/null phones.
- [x] 4.4 Load and display member contact through the Android repository with loading, unavailable and retry states.
- [x] 4.5 Run Android/backend/contract/OpenSpec checks and inspect the updated emulator UI; record deployment and verification limits.
