---
Evidence status: completed-external-evidence
Repository: IMG-LTD/MMPay
Ruleset source: governance/github-rulesets/v1-tags.json
Ruleset source sha256: cc6dc2d1152a0a307598f67b476da4ac9530f1c8e6d83d19c42ae1cc162b168b
Ruleset target: refs/tags/v1.*
Ruleset enforcement: active
Deletion protection: enabled
Non-fast-forward protection: enabled
Creation protection: enabled
Bypass actors: none
Remote ruleset evidence URL: https://github.com/IMG-LTD/MMPay/rules/16623567
Remote ruleset observed at: 2026-05-20T04:42:31Z
---

# v1 Tag Ruleset Evidence

The repository-level ruleset id 16623567 was created against
IMG-LTD/MMPay through the GitHub Rulesets API and now governs every
ref matching refs/tags/v1.*. The serialized intent in
governance/github-rulesets/v1-tags.json is the source of truth for
the ruleset; its sha256 hash above pins the exact bytes that were
posted to the API and matches the file currently checked into the
release commit.

The ruleset enforces three rules with active enforcement and an empty
bypass actors list: deletion (cannot delete a v1 tag), non-fast-forward
(cannot rewrite a v1 tag's history), and creation (only the workflow
that ran the release-gate can introduce a new v1 tag). Once the
v1.0.0 GA tag is published this ruleset prevents any subsequent
mutation of the line.

Live confirmation is available by running
`scripts/governance/verify-v1-tag-ruleset-evidence.sh` with
`MMPAY_RULESET_LIVE_CHECK=true` set in the environment, which queries
`gh api /repos/IMG-LTD/MMPay/rulesets/16623567` and asserts both the
active enforcement state and the empty bypass actors invariant.
