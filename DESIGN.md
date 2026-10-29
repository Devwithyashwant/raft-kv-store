# Raft Consensus - Design Notes

## Why this project exists
A single-server database has no fault tolerance - if it crashes, data is
unavailable or lost. Running multiple servers fixes availability, but creates
a new problem: how do servers agree on data when networks drop messages and
servers crash at arbitrary times?

## The core problem (concrete example)
Two servers each independently accept a withdrawal of 80 from a 100-balance
account because neither knows about the other's action in time. Both end up
at balance=20, but the real answer should be -60 (overdrawn). This is a
"lost update" - a decision made with incomplete information that can't be
undone once it affects the real world.

## How Raft solves it
- Only one leader at a time accepts writes
- A write only succeeds once a majority of servers confirm it
- Leaders send heartbeats to followers; if heartbeats stop, followers assume
  the leader died and start an election
- Election timeouts are randomized per-server to avoid split votes (multiple
  candidates tying because they all started an election at the same instant)
- Only a candidate whose log is at least as up-to-date as a majority of
  voters can win an election (the "election restriction") - this guarantees
  the new leader always has every previously-committed write, because any
  two majorities out of the same cluster must overlap by at least one server
- Every message carries a "term" number (incremented each election). If a
  server ever sees a term higher than its own, it immediately defers - this
  is what corrects a stale leader that reconnects after being offline

## Log entries
Each entry stores the term it was created in, not just the command. Comparing
logs for "who is more up-to-date" means comparing by term first, then by
log length - this ensures entries from an old, now-invalid leader's term
always lose to entries from a legitimate, later term, even if the old log
happens to be longer.